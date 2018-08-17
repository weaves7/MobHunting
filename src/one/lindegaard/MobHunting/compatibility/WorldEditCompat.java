package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import one.lindegaard.MobHunting.MobHunting;

public class WorldEditCompat {
	private static WorldEditPlugin mPlugin;
	private static boolean supported = false;

	public WorldEditCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with WorldEdit is disabled in config.yml");
		} else {
			mPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldEdit.getName());
			if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with WorldEdit (" + mPlugin.getDescription().getVersion() + ")");
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of WorldEdit (" + mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting. Mobhunting does only support 7.0.0 and newer.");
			}
		}
	}

	public static WorldEditPlugin getWorldEdit() {
		return mPlugin;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationWorldEdit;
	}

	public static boolean isSupported() {
		return supported;
	}
}
