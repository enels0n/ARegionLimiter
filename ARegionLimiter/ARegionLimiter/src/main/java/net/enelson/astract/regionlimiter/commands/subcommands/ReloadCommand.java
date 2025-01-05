package net.enelson.astract.regionlimiter.commands.subcommands;

import org.bukkit.command.CommandSender;

import net.enelson.astract.regionlimiter.ARegionLimiter;


public class ReloadCommand {
	public ReloadCommand(CommandSender sender) {
		if(sender.isOp()) {
			ARegionLimiter.getInstance().reloadConfig();
			sender.sendMessage("The plugin has been reloaded.");
			return;
		}
	}
}
