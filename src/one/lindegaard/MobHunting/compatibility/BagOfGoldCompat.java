package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import one.lindegaard.BagOfGold.BagOfGold;

public class BagOfGoldCompat {

	private static BagOfGold mPlugin;
	private static boolean supported = false;

	public BagOfGoldCompat() {
		mPlugin = (BagOfGold) Bukkit.getPluginManager().getPlugin(CompatPlugin.BagOfGold.getName());

		Plugin vPlugin = Bukkit.getPluginManager().getPlugin("Vault");
		if (vPlugin.getDescription().getVersion().compareTo("1.6.7") >= 0) {
			Bukkit.getLogger().info("[MobHunting] Enabling compatibility with BagOfGold ("
					+ getBagOfGoldAPI().getDescription().getVersion() + ")");
			supported = true;
		} else {
			ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
			console.sendMessage(ChatColor.RED + "[MobHunting/BagOfGold] Your current version of Vault ("
					+ mPlugin.getDescription().getVersion()
					+ ") is outdated. Please upgrade to 1.6.7 or newer if you want to use BagOfGold or uninstall BagOfGold");
		}

	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static BagOfGold getBagOfGoldAPI() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static double getStartingBalance() {
		return BagOfGold.getConfigManager().startingBalance;
	}

	public static String getBagOfGoldFormat() {
		return BagOfGold.getConfigManager().numberFormat;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

}
