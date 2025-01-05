package net.enelson.astract.regionlimiter.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.enelson.astract.regionlimiter.commands.subcommands.ReloadCommand;

public class CommandManager implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if (args.length == 0) {
			return false;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			new ReloadCommand(sender);
		}
		return false;
	}
}
