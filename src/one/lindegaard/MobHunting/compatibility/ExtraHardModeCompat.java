package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.extrahardmode.ExtraHardMode;
import com.extrahardmode.config.RootConfig;

import one.lindegaard.MobHunting.MobHunting;

public class ExtraHardModeCompat implements Listener {

	private static Plugin mPlugin;
	private static boolean supported = false;

	// https://www.spigotmc.org/resources/extra-hard-mode.19673/

	public ExtraHardModeCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with ExtraHardMode is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.ExtraHardMode.getName());

			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Enabling compatibility with ExtraHardMode (" + mPlugin.getDescription().getVersion() + ").");
			if (!MobHunting.getInstance().getConfigManager().difficultyMultiplier
					.containsKey("difficulty.multiplier.extrahard")) {
				MobHunting.getInstance().getMessages().debug("Adding extrahard difficulty to config.yml");
				MobHunting.getInstance().getConfigManager().difficultyMultiplier.put("difficulty.multiplier.extrahard",
						"2.5");
				MobHunting.getInstance().getConfigManager().saveConfig();
			}
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledForWorld(World world) {
		if (isSupported()) {
			for (String worldName : ((ExtraHardMode) mPlugin).getModuleForClass(RootConfig.class).getEnabledWorlds())
				if (worldName.equalsIgnoreCase("@all") || world.getName().equalsIgnoreCase(worldName))
					return true;
		}
		return false;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationExtraHardMode;
	}

}
