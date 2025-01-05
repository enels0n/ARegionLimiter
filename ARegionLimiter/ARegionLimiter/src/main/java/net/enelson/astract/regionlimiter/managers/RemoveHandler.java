package net.enelson.astract.regionlimiter.managers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import net.enelson.astract.regionlimiter.ARegionLimiter;
import net.enelson.astract.regionlimiter.utils.Message;

public class RemoveHandler implements Listener {

	private static final String[] REGION_COMMANDS = {"/region", "/regions", "/rg", 
            "/worldguard:region", "/worldguard:regions", "/worldguard:rg"};
	private static final String[] DELETE_COMMANDS = {"remove", "rem", "delete", "del"};
	
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
		
		e.setCancelled(true);
		Player player = e.getPlayer();
		
        // Переменные для хранения значений флага и аргументов
        String world = player.getWorld().getName();
        String region = null;

        // Обработка аргументов и поиск флага world
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-w") || args[i].equalsIgnoreCase("--world")) {
                if (i + 1 < args.length) {
                    // Извлекаем следующее значение как имя мира
                    world = args[i + 1].replace("\"", ""); // Убираем кавычки, если они есть
                    i++; // Пропускаем следующее значение, так как оно уже использовано
                }
            } else if (region == null) {
                // Устанавливаем регион, если он еще не установлен
                region = args[i];
            }
        }

        // Если регион не был установлен из аргументов, вы можете здесь обработать это
        if (region == null) {
            player.sendMessage(Message.NAME_NOT_ENTERED.getMessageWithPlaceholders(player));
            return; // Или другое обработка, если регион не задан
        }

        World bukkitWorld = Bukkit.getWorld(world);

		if (bukkitWorld == null) {
			//регион в указанном мире не существует
			player.sendMessage(Message.INVALID_WORLD.getMessageWithPlaceholders(player)
					.replaceAll("%world%", world));
			return;
		}
        
		LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(bukkitWorld));
		
		ProtectedRegion protectedRegion = regions.getRegion(region);

		if (protectedRegion == null) {
			//регион в указанном мире не существует
			player.sendMessage(Message.INVALID_REGION.getMessageWithPlaceholders(player)
					.replaceAll("%region%", region)
					.replaceAll("%world%", world));
			return;
		}
		
		if(!protectedRegion.isOwner(localPlayer) && !player.isOp() && !player.isPermissionSet("aregionlimiter.bypass")) {
			//не овнер
			player.sendMessage(Message.NO_PERM_REMOVE.getMessageWithPlaceholders(player));
			return;
		}
		
		String parent = protectedRegion.getFlag(ARegionLimiter.getInstance().getParentFlag());
		if(parent != null) {
			ProtectedRegion parentRegion = regions.getRegion(parent);
			if(parentRegion == null) {
				if(player.isOp() || player.isPermissionSet("aregionlimiter.bypass")) {
					player.sendMessage("Ок. Удаляем.");
					regions.removeRegion(protectedRegion.getId());
				}
				else {
					player.sendMessage("Что-то пошло не так. Обратитесь к администратору.");
				}
				return;
			}
			
	        List<String> vars = new ArrayList<>(Arrays.asList(parentRegion.getFlag(ARegionLimiter.getInstance().getChildsFlag()).split(", ")));

			// Удаляем нужный элемент
			vars.remove(region);
			
			// Преобразуем обратно в строку
			String newFlag = vars.size() > 0 ? String.join(", ", vars) : "null";
			parentRegion.setFlag(ARegionLimiter.getInstance().getChildsFlag(), newFlag);
			
			regions.removeRegion(protectedRegion.getId());
			player.sendMessage(Message.REMOVE_REGION_CHILD.getMessageWithPlaceholders(player)
					.replaceAll("%region%", protectedRegion.getId()));
			return;
		}
		
		String childs = protectedRegion.getFlag(ARegionLimiter.getInstance().getChildsFlag());
		if(childs != null && !childs.equals("null")) {
			List<String> childrenList = Arrays.asList(protectedRegion.getFlag(ARegionLimiter.getInstance().getChildsFlag()).split(", "));
			for(String child : childrenList) {
				regions.removeRegion(child);
			}
			regions.removeRegion(protectedRegion.getId());
			
			String children = String.join(", ", childrenList);
			player.sendMessage(Message.REMOVE_REGION_PARENT.getMessageWithPlaceholders(player)
					.replaceAll("%region%", protectedRegion.getId())
					.replaceAll("%children%", children));
			return;
		}
		regions.removeRegion(protectedRegion.getId());
		player.sendMessage(Message.REMOVE_REGION.getMessageWithPlaceholders(player)
				.replaceAll("%region%", protectedRegion.getId()));
	}
	
	private boolean isRegionCommand(final String[] args) {
		return Arrays.stream(REGION_COMMANDS).anyMatch(cmd -> args[0].equalsIgnoreCase(cmd)) && args.length >= 3;
	}

	private boolean isClaimArg(final String[] args) {
		return Arrays.stream(DELETE_COMMANDS).anyMatch(cmd -> args[0].equalsIgnoreCase(cmd)) && args.length >= 2;
	}
}
