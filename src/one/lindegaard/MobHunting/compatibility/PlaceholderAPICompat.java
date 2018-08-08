package one.lindegaard.MobHunting.compatibility;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.clip.placeholderapi.PlaceholderAPI;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.placeholder.MobHuntingPlaceholderHook;
import one.lindegaard.MobHunting.placeholder.PlaceHolderData;
import one.lindegaard.MobHunting.placeholder.PlaceHolderManager;

public class PlaceholderAPICompat {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private static PlaceHolderManager mPlaceHolderManager;

	// https://www.spigotmc.org/resources/placeholderapi.6245/
	// https://www.spigotmc.org/wiki/hooking-into-placeholderapi/

	public PlaceholderAPICompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with PlaceholderAPI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.PlaceholderAPI.getName());
			if (mPlugin.getDescription().getVersion().compareTo("2.0.6") >= 0) {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
								+ "Enabling compatibility with PlaceholderAPI (" + mPlugin.getDescription().getVersion()
								+ ").");
				new MobHuntingPlaceholderHook(MobHunting.getInstance()).hook();
				mPlaceHolderManager = new PlaceHolderManager(MobHunting.getInstance());
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of PlaceholderAPI (" + mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting, please upgrade to 2.0.6 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public PlaceholderAPI getPlaceholderAPI() {
		return (PlaceholderAPI) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationPlaceholderAPI;
	}

	public static HashMap<UUID, PlaceHolderData> getPlaceHolders() {
		return mPlaceHolderManager.getPlaceHolders();
	}

	public static void shutdown() {
		mPlaceHolderManager.shutdown();
	}

	public static String setPlaceholders(Player player, String messages_with_placeholders) {
		if (isSupported())
			return PlaceholderAPI.setPlaceholders(player, messages_with_placeholders);
		return messages_with_placeholders;
	}

}
