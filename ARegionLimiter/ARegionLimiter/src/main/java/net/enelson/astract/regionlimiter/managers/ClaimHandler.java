package net.enelson.astract.regionlimiter.managers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.region.RegionCommands;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.flags.Flag;

import net.enelson.astract.regionlimiter.ARegionLimiter;
import net.enelson.astract.regionlimiter.utils.Message;
import net.enelson.astract.regionlimiter.utils.Utils;

import org.bukkit.event.Listener;

@SuppressWarnings("deprecation")
public class ClaimHandler implements Listener {

	private String[] REGION_COMMANDS = {"/region", "/regions", "/rg", 
            "/worldguard:region", "/worldguard:regions", "/worldguard:rg"};
	private String[] DIMENSIONS = {"x", "y", "z"};
	private RegionCommands regionCommands = new RegionCommands(WorldGuard.getInstance());
	private Set<Character> flagCommandValueFlags = Utils.getFlagCommandValueFlags();
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onCommand(final PlayerCommandPreprocessEvent e) {
		if(e.isCancelled())
			return;
		
		String[] args = e.getMessage().split(" ");

		if (!isRegionCommand(args))
			return;

		args = Arrays.copyOfRange(args, 1, args.length);
		if (!isClaimArg(args))
			return;

		
		Player player = e.getPlayer();

		Utils.debug(player, "%player% initiated the creation of a region.");
		
		LocalSession session = ARegionLimiter.getInstance().getWorldEdit().getSession(player);
		Region selection;
		try {
			selection = session.getSelection(session.getSelectionWorld());
		} catch (IncompleteRegionException ex) {
			Utils.debug(player, "An error occurred while retrieving the WorldEdit session");
			return;
		}

		if (player.hasPermission("aregionlimiter.bypass") || player.isOp()) {
			Utils.debug(player, "%player% is op or has bypass permission.");
			return;
		}

		e.setCancelled(true);
		
		String name = args[1];
		Pattern pattern = Pattern.compile("[a-zA-Z0-9_]{3,20}");
		if(!pattern.matcher(name).matches() || name.equalsIgnoreCase("__global__")) {
			player.sendMessage(Message.INVALID_NAME.getMessageWithPlaceholders(player));
			return;
		}
		
		if (!(selection instanceof CuboidRegion)) {
			player.sendMessage(Message.UNKNOWN_REGION_TYPE.getMessageWithPlaceholders(player));
			return;
		}

		CuboidRegion cuboid = (CuboidRegion) selection;
		Utils.debug(player, "%player%'s selection size is [X: " + cuboid.getWidth() + ", Y: " + cuboid.getHeight() + ", Z: " + cuboid.getLength() + "]");
		
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(cuboid.getWorld());
		ApplicableRegionSet set = regions.getApplicableRegions(cuboid.getPos1());
		ApplicableRegionSet set2 = regions.getApplicableRegions(cuboid.getPos2());
		
		ProtectedCuboidRegion testRegion = new ProtectedCuboidRegion("testRegion", cuboid.getPos1(), cuboid.getPos2());
		for(ProtectedRegion region : regions.getApplicableRegions(testRegion)) {
			Utils.debug(player, "Checking if is unown region \"" + region.getId() + "\"");
			if (!region.isOwner(localPlayer)) {
				player.sendMessage(Message.ANOTHER_REGION_AFFECTED.getMessageWithPlaceholders(player));
				return;
			}
		}
		
		ClaimType type = ClaimType.IN_GLOBAL;
		ProtectedRegion ownRegion = null;
		
		for (ProtectedRegion region : set) {
			if (region.getFlag(ARegionLimiter.getInstance().getParentFlag()) != null
					|| region.getFlag(ARegionLimiter.getInstance().getChildsFlag()) == null) {
				continue;
			}

			if (containsRegion(set2, region)) {
				type = ClaimType.IN_OWN_REGION;
				ownRegion = region;
				break;
			}
		}
		

		Utils.debug(player, "ClaimType is " + type.name());

		Map<Param, Integer> sizes = getSizes(type, player);

        if (!checkDimensionLimits(player, cuboid, sizes)) {
            return;
        }
        
		long count = countRegions(localPlayer, regions, type);

	    if (!checkRegionLimits(player, args, count, sizes, type, ownRegion, regions)) {
	        return;
	    }

		String newRegionId = generateRegionId(type, name, ownRegion);
		
		if(regions.getRegion(newRegionId) != null) {
			player.sendMessage(Message.REGION_EXIST.getMessageWithPlaceholders(player));
			return;
		}

		ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion(newRegionId, cuboid.getPos1(), cuboid.getPos2());
		newRegion.getOwners().addPlayer(localPlayer);

		regions.addRegion(newRegion);
		if(type == ClaimType.IN_OWN_REGION) {
			String flag = ownRegion.getFlag(ARegionLimiter.getInstance().getChildsFlag());
			
			flag = (flag == null || flag.equals("null")) ? newRegion.getId() : flag + ", " + newRegion.getId();
			ownRegion.setFlag(ARegionLimiter.getInstance().getChildsFlag(), flag);
			newRegion.setFlag(ARegionLimiter.getInstance().getParentFlag(), ownRegion.getId());
			newRegion.setPriority(ownRegion.getPriority()+1);

			for (Entry<Flag<?>, String> entry : ARegionLimiter.getInstance().getChildAutoFlags().entrySet()) {
				try {
					setFlag(Utils.wrapAsPrivileged(player, false), cuboid.getWorld().getName(), newRegion, entry.getKey(), entry.getValue());
				} catch (CommandException ex) {
					ex.printStackTrace();
				}
			}
		}
		else {
			newRegion.setFlag(ARegionLimiter.getInstance().getChildsFlag(), "null");

			for (Entry<Flag<?>, String> entry : ARegionLimiter.getInstance().getParentAutoFlags().entrySet()) {
				try {
					setFlag(Utils.wrapAsPrivileged(player, false), cuboid.getWorld().getName(), newRegion, entry.getKey(), entry.getValue());
				} catch (CommandException ex) {
					ex.printStackTrace();
				}
			}
		}
		
		try {
			regions.save();
		} catch (StorageException e1) {
			e1.printStackTrace();
		}
		
		String message = type == ClaimType.IN_GLOBAL ? Message.SUCCESS_IN_GLOBAL.getMessageWithPlaceholders(player)
				: Message.SUCCESS_IN_OWN_REGION.getMessageWithPlaceholders(player);
		player.sendMessage(message
				.replaceAll("%region%", newRegion.getId()));
	}

	private boolean isRegionCommand(final String[] args) {
		return Arrays.stream(REGION_COMMANDS).anyMatch(cmd -> args[0].equalsIgnoreCase(cmd)) && args.length > 1;
	}

	private boolean isClaimArg(final String[] cmd) {
		return cmd[0].equalsIgnoreCase("claim") && cmd.length > 1;
	}

	private boolean containsRegion(ApplicableRegionSet set, ProtectedRegion region) {
		for (ProtectedRegion rg : set) {
			if (rg == region)
				return true;
		}
		return false;
	}

	private Map<Param, Integer> getSizes(ClaimType type, Player player) {
		Map<Param, Integer> sizes = new HashMap<Param, Integer>();
		String group = this.getMainGroup(player, type.getPath());
		String world = player.getWorld().getName();

		sizes.put(Param.VERTEXPAND, getParamBoolean(type.getPath(), world, group, "vertexpand"));
		sizes.put(Param.MAX_COUNT, getParamInt(type.getPath(), world, group, "max-count"));

		for (String o : new String[] { "x", "y", "z" }) {
			sizes.put(Param.valueOf("MIN_"+o.toUpperCase()), getParamInt(type.getPath(), world, group, o + "-min"));
			sizes.put(Param.valueOf("MAX_"+o.toUpperCase()), getParamInt(type.getPath(), world, group, o + "-max"));
		}

		Utils.debug(player, "%player%'s max sizes is " + sizes);
		return sizes;
	}

	private String getMainGroup(Player player, String path) {
		String group = "default";
		for (String gr : ARegionLimiter.getInstance().getConfig().getConfigurationSection("limit." + path)
				.getKeys(false)) {
			boolean hasPerm = Utils.hasPermission(player, "aregionlimiter." + path + "." + gr);
			if (hasPerm)
				group = gr;
			Utils.debug(player, "Checking %player%'s perm \"aregionlimiter." + path + "." + gr + "\". Result: " + hasPerm);
		}
		
		Utils.debug(player, "%player%'s main group is " + group);
		return group;
	}

	private int getParamInt(String path, String world, String group, String parameter) {
		return ARegionLimiter.getInstance().getConfig().getInt(
				"limit-per-world." + path + "." + world + "." + group + "." + parameter,
				ARegionLimiter.getInstance().getConfig().getInt("limit." + path + "." + group + "." + parameter));
	}

	private int getParamBoolean(String path, String world, String group, String parameter) {
		return ARegionLimiter.getInstance().getConfig().getBoolean(
				"limit-per-world." + path + "." + world + "." + group + "." + parameter,
				ARegionLimiter.getInstance().getConfig().getBoolean("limit." + path + "." + group + "." + parameter)) ? 1 : 0;
	}
	
	private long countRegions(LocalPlayer localPlayer, RegionManager regions, ClaimType type) {
	    return regions.getRegions().values().stream().filter(region -> {
	        switch (type) {
	            case IN_GLOBAL:
	                return region.getOwners().contains(localPlayer) && region.getFlag(ARegionLimiter.getInstance().getChildsFlag()) != null;
	            case IN_OWN_REGION:
	                return region.getOwners().contains(localPlayer) && region.getFlag(ARegionLimiter.getInstance().getParentFlag()) != null;
	            default:
	                return false;
	        }
	    }).count();
	}
	
	private String generateRegionId(ClaimType type, String baseId, ProtectedRegion ownRegion) {
	    return (type == ClaimType.IN_OWN_REGION && ownRegion != null) ? ownRegion.getId() + "_" + baseId.toLowerCase() : baseId.toLowerCase();
	}
	
	private boolean checkRegionLimits(Player player, String[] args, long count, Map<Param, Integer> sizes, ClaimType type, ProtectedRegion ownRegion, RegionManager regions) {
	    if (sizes.get(Param.MAX_COUNT) >=0 && sizes.get(Param.MAX_COUNT) <= count) {
	        player.sendMessage(type == ClaimType.IN_GLOBAL ? Message.MAX_COUNT_IN_GLOBAL.getMessageWithPlaceholders(player) :
	                                                          Message.MAX_COUNT_IN_OWN_REGION.getMessageWithPlaceholders(player));
	        return false;
	    }
	    return true;
	}
	
    private boolean checkDimensionLimits(Player player, CuboidRegion cuboid, Map<Param, Integer> sizes) {
        for (String dimension : DIMENSIONS) {
            int size = 0;
            switch (dimension) {
                case "x": size = cuboid.getWidth(); break;
                case "y": size = cuboid.getHeight(); break;
                case "z": size = cuboid.getLength(); break;
            }

            int min = sizes.get(Param.valueOf("MIN_" + dimension.toUpperCase()));
            int max = sizes.get(Param.valueOf("MAX_" + dimension.toUpperCase()));
            
            if (min != 0 && size < min) {
                player.sendMessage(Message.valueOf("CLAIM_MIN_" + dimension.toUpperCase())
                        .getMessageWithPlaceholders(player).replace("%size%", Integer.toString(min)));
                return false;
            }

            if (max != 0 && size > max) {
                player.sendMessage(Message.valueOf("CLAIM_MAX_" + dimension.toUpperCase())
                        .getMessageWithPlaceholders(player).replace("%size%", Integer.toString(max)));
                return false;
            }
        }

        return true;
    }
    
	
	private <T> void setFlag(Actor actor, String world, ProtectedRegion region, Flag<T> flag, String value) throws CommandException {
		String command = String.format("flag %s -w %s %s %s", region.getId(), world, flag.getName(), value);
		CommandContext context = new CommandContext(command, flagCommandValueFlags);
		this.regionCommands.flag(context, actor);
	}
}
