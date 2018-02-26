package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.MobHunting;

import com.connorlinfoot.actionbarapi.ActionBarAPI;

public class ActionBarAPICompat {

	private static ActionBarAPI mPlugin;
	private static boolean supported = false;

	// https://www.spigotmc.org/resources/actionbarapi-1-8-1-9-1-10.1315/

	public ActionBarAPICompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage("[MobHunting] Compatibility with ActionBarAPI is disabled in config.yml");
		} else {
			mPlugin = (ActionBarAPI) Bukkit.getPluginManager().getPlugin(CompatPlugin.ActionBarApi.getName());

			Bukkit.getConsoleSender().sendMessage("[MobHunting] Enabling compatibility with ActionBarAPI ("
					+ getActionBarAPI().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public ActionBarAPI getActionBarAPI() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationActionBarAPI;
	}

	public static void setMessage(Player player, String text) {
		if (supported) {

			ActionBarAPI.sendActionBar(player, text);

			// ActionBarAPI.sendActionBar(player,"Action Bar Message",
			// duration);
		}
	}

}
