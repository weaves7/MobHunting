package one.lindegaard.MobHunting.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.storage.DataStoreManager;
import one.lindegaard.MobHunting.storage.PlayerSettings;

public class LearnCommand implements ICommand {

	private MobHunting plugin;

	public LearnCommand(MobHunting plugin) {
		this.plugin = plugin;
	}

	// Used case
	// /mh learn - args.length = 0 || arg[0]=""

	@Override
	public String getName() {
		return "learn";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "learnmode" };
	}

	@Override
	public String getPermission() {
		return "mobhunting.learn";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.WHITE + " - to enable/disable learningmode.",
				ChatColor.GOLD + label + ChatColor.GREEN + " playername" + ChatColor.WHITE
						+ " - to enable/disable learningmode for a specific player." };
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("mobhunting.commands.learn.description");
	}

	@Override
	public boolean canBeConsole() {
		return false;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {

		if (args.length == 0) {
			togglePlayerLearningMode((Player) sender);
			return true;
		} else if (args.length == 1) {
			DataStoreManager ds = plugin.getDataStoreManager();
			Player player = (Player) ds.getPlayerByName(args[0]);
			if (player != null) {
				if (sender.hasPermission("mobhunting.learn.other") || sender instanceof ConsoleCommandSender) {
					togglePlayerLearningMode(player);
				} else {
					plugin.getMessages().senderSendMessage(sender,
							ChatColor.RED + "You dont have permission " + ChatColor.AQUA + "'mobhunting.learn.other'");
				}
				return true;
			} else {
				plugin.getMessages().senderSendMessage(sender, ChatColor.RED + "Player " + args[0] + " is not online.");
				return false;
			}
		}
		return false;
	}

	private void togglePlayerLearningMode(Player player) {
		PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(player);
		ps.setLearningMode(!ps.isLearningMode());
		plugin.getPlayerSettingsManager().setPlayerSettings(player, ps);
		if (ps.isLearningMode())
			player.sendMessage(
					plugin.getMessages().getString("mobhunting.commands.learn.disabled", "player", player.getName()));
		else
			player.sendMessage(
					plugin.getMessages().getString("mobhunting.commands.learn.enabled", "player", player.getName()));
	}

}
