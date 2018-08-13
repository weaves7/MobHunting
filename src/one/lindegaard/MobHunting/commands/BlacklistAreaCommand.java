package one.lindegaard.MobHunting.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibHelper;
import one.lindegaard.MobHunting.grinding.Area;

public class BlacklistAreaCommand implements ICommand {

	private MobHunting plugin;

	public BlacklistAreaCommand(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "blacklistarea";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "mobhunting.blacklist";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.GREEN + " [add|remove]" + ChatColor.WHITE
				+ " - to blacklist an area." };
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("mobhunting.commands.blacklistarea.description");
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
	public boolean onCommand(CommandSender sender, String label, String[] args) {
		Location loc = ((Player) sender).getLocation();

		if (args.length == 0) {
			if (plugin.getGrindingManager().isGrindingArea(loc)) {
				plugin.getMessages().senderSendMessage(sender, ChatColor.GREEN
						+ plugin.getMessages().getString("mobhunting.commands.blacklistarea.isblacklisted"));
				Area area = plugin.getGrindingManager().getGrindingArea(loc);
				if (ProtocolLibCompat.isSupported())
					ProtocolLibHelper.showGrindingArea((Player) sender, area, loc);
			} else
				plugin.getMessages().senderSendMessage(sender, ChatColor.RED
						+ plugin.getMessages().getString("mobhunting.commands.blacklistarea.notblacklisted"));
		} else if (args.length == 1) {
			if (args[0].equalsIgnoreCase("remove")) {
				plugin.getGrindingManager().unBlacklistArea(loc);
				plugin.getMessages().senderSendMessage(sender, ChatColor.GREEN
						+ plugin.getMessages().getString("mobhunting.commands.blacklistarea.remove.done"));
			} else if (args[0].equalsIgnoreCase("add")) {
				Area area = new Area(loc, plugin.getConfigManager().grindingDetectionRange, 0);
				plugin.getGrindingManager().blacklistArea(area);
				plugin.getMessages().senderSendMessage(sender,
						ChatColor.GREEN + plugin.getMessages().getString("mobhunting.commands.blacklistarea.done"));
				if (ProtocolLibCompat.isSupported())
					ProtocolLibHelper.showGrindingArea((Player) sender, area, loc);
			} else
				return false;
		} else
			return false;

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		ArrayList<String> items = new ArrayList<String>();
		if (args.length == 1) {
			if (items.isEmpty()) {
				items.add("add");
				items.add("remove");
				items.add("");
			}
		}

		if (!args[args.length - 1].trim().isEmpty()) {
			String match = args[args.length - 1].trim().toLowerCase();
			Iterator<String> it = items.iterator();
			while (it.hasNext()) {
				String name = it.next();
				if (!name.toLowerCase().startsWith(match))
					it.remove();
			}
		}
		return items;
	}

}
