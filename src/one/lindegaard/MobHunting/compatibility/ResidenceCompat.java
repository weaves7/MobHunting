package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import one.lindegaard.MobHunting.MobHunting;

public class ResidenceCompat {

	private static Plugin mPlugin;
	private static boolean supported = false;

	// https://www.spigotmc.org/resources/residence-1-7-10-up-to-1-11.11480/

	public ResidenceCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with Residence is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Residence.getName());

			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Enabling compatibility with Residence (" + mPlugin.getDescription().getVersion() + ").");
			supported = true;
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
		return MobHunting.getInstance().getConfigManager().enableIntegrationResidence;
	}

	public static boolean isProtected(Player player) {
		if (supported) {
			return ResidenceHelper.isProtected(player);
		}
		return false;
	}

}
