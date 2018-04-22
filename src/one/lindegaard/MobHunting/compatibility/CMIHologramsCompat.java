package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.HologramManager;

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
			if (mPlugin.getDescription().getVersion().compareTo("7.6.2.0") >= 0) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with CMI (" + mPlugin.getDescription().getVersion() + ").");
				supported = true;
			} else
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
						+ "Your current version of CMI (" + mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Mobhunting does only support version 7.6.2.0 or newer.");

		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static CMI getCMIPlugin() {
		return (CMI) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationHolograms;
	}

	public static HologramManager getHologramManager() {
		return getCMIPlugin().getHologramManager();
	}

}
