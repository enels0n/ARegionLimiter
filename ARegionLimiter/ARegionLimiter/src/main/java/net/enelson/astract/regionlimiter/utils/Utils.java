package net.enelson.astract.regionlimiter.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.region.RegionCommands;

import net.enelson.astract.regionlimiter.ARegionLimiter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.platform.PlayerAdapter;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("deprecation")
public class Utils {
	public static Actor wrapAsPrivileged(CommandSender sender, boolean showMessages) {
	    Actor actor = sender instanceof Player 
	            ? WorldGuardPlugin.inst().wrapPlayer((Player) sender) 
	            : WorldGuardPlugin.inst().wrapCommandSender(sender);

	    return (Actor) Proxy.newProxyInstance(
	            actor.getClass().getClassLoader(),
	            actor.getClass().getInterfaces(),
	            (proxy, method, args) -> {
	                // Используем switch с текстовыми переменными
	                return switch (method.getName()) {
	                    case "print", "printRaw", "printDebug", "printError", "printInfo" -> {
	                        if (showMessages) {
	                            yield method.invoke(actor, args);
	                        } else {
	                            yield null;
	                        }
	                    }
	                    case "hasPermission" -> true;
	                    case "checkPermission" -> null;
	                    default -> method.invoke(actor, args);
	                };
	            }
	    );
	}
	
	public static Set<Character> getFlagCommandValueFlags() {
		try {
			Method method = RegionCommands.class.getMethod("flag", CommandContext.class, Actor.class);
			Command annotation = method.getAnnotation(Command.class);
			char[] flags = annotation.flags().toCharArray();
			Set<Character> valueFlags = new HashSet<>();
			for (int i = 0; i < flags.length; ++i) {
				if ((flags.length > (i + 1)) && (flags[i + 1] == ':')) {
					valueFlags.add(flags[i]);
					++i;
				}
			}
			return valueFlags;
		} catch (Throwable t) {
			t.printStackTrace();
			return Collections.emptySet();
		}
	}
	
	public static boolean hasPermission(Player player, String permission) {
		LuckPerms lp = ARegionLimiter.getInstance().getLuckPerms();
		if(lp != null) {
			PlayerAdapter<Player> adapter = lp.getPlayerAdapter(Player.class);
			CachedPermissionData permissionData = adapter.getPermissionData(player);
			return permissionData.checkPermission(permission).asBoolean();
		}
		else {
			return player.isPermissionSet(permission);
		}
	}
	
	public static void debug(Player player, String message) {
		if(!ARegionLimiter.getInstance().getConfig().getBoolean("debug"))
			return;
		if(player != null)
			message = message.replaceAll("%player%", player.getName());
		Bukkit.getLogger().info("[ARegionLimiter's Debug] " + message);
	}
}
