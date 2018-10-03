package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class WorldGuardCompat {

	private static boolean supported = false;
	private static WorldGuardPlugin mPlugin;

	public WorldGuardCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with WorldGuard is disabled in config.yml");
		} else {
			mPlugin = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldGuard.getName());
			if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with WorldGuard (" + mPlugin.getDescription().getVersion() + ")");
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of WorldGuard (" + mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Mobhunting does only support 7.0.0 beta 1 and newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static WorldGuardPlugin getWorldGuardPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationWorldGuard;
	}

}
