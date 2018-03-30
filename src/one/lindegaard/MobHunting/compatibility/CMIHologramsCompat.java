package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import com.Zrips.CMI.CMI;

import one.lindegaard.MobHunting.MobHunting;

public class CMIHologramsCompat {

	private static Plugin mPlugin;
	private static boolean supported = false;

	// https://www.spigotmc.org/resources/cmi-ranks-kits-portals-essentials-mysql-sqlite-bungeecord.3742/

	public CMIHologramsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with CMI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.CMI.getName());

			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Enabling compatibility with CMI (" + mPlugin.getDescription().getVersion() + ").");

			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public CMI getCMIPlugin() {
		return (CMI) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationHolograms;
	}

	public String getManager() {
		return "test";
	}

	// public static HologramManager getHologramManager() {
	// return ((HologramPlugin) mPlugin).getHologramManager();
	// }

}
