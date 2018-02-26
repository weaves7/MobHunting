package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import one.lindegaard.MobHunting.MobHunting;

import org.gestern.gringotts.Gringotts;

public class GringottsCompat {

	// http://dev.bukkit.org/bukkit-plugins/gringotts/
	// Source code: https://github.com/MinecraftWars/Gringotts

	private static boolean supported = false;
	private static Gringotts mPlugin;

	public GringottsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getLogger().info("[MobHunting] Compatibility with Gringotts is disabled in config.yml");
		} else {
			mPlugin = (Gringotts) Bukkit.getPluginManager().getPlugin(CompatPlugin.Gringotts.getName());

			Bukkit.getLogger().info("[MobHunting] Enabling Compatibility with Gringotts ("
					+ getGringotts().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static Gringotts getGringotts() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationGringotts;
	}

}
