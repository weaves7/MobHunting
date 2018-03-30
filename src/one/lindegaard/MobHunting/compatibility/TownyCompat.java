package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import one.lindegaard.MobHunting.MobHunting;

public class TownyCompat {

	private static Plugin mPlugin;
	private static boolean supported = false;

	// http://towny.palmergames.com/

	public TownyCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Compatibility with Towny in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Towny.getName());

			try {
				@SuppressWarnings({ "rawtypes", "unused" })
				Class cls = Class.forName("com.palmergames.bukkit.towny.object.TownyUniverse");
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with Towny (" + mPlugin.getDescription().getVersion() + ").");
				supported = true;
			} catch (ClassNotFoundException e) {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED + "Your version of Towny ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not complatible with this version of MobHunting, please upgrade.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationTowny;
	}

	public static boolean isInHomeTome(Player player) {
		if (supported) {
			return TownyHelper.isInHomeTome(player);
		}
		return false;
	}

}
