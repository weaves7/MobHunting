package one.lindegaard.MobHunting.compatibility;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;

public class EssentialsCompat implements Listener {

	private static Essentials mPlugin;
	private static boolean supported = false;

	public EssentialsCompat() {
		if (isDisabledInConfig()) {
			Bukkit.getLogger().info("[MobHunting] Compatibility with Essentials is disabled in config.yml");
		} else {
			mPlugin = (Essentials) Bukkit.getPluginManager().getPlugin(CompatPlugin.Essentials.getName());

			Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			Bukkit.getLogger().info("[MobHunting] Enabling compatibility with Essentials ("
					+ getEssentials().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static Essentials getEssentials() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.getInstance().getConfigManager().disableIntegrationEssentials;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getInstance().getConfigManager().disableIntegrationEssentials;
	}

	public static boolean isGodModeEnabled(Player player) {
		if (isSupported()) {
			User user = getEssentials().getUser(player);
			return user.isGodModeEnabled();
		}
		return false;
	}

	public static boolean isVanishedModeEnabled(Player player) {
		if (isSupported()) {
			User user = getEssentials().getUser(player);
			return user.isVanished();
		}
		return false;
	}

	public static double getBalance(Player player) {
		double bal = mPlugin.getOfflineUser(player.getName()).getMoney().doubleValue();
		return bal;
	}

	public static double getEssentialsBalance(OfflinePlayer offlinePlayer) {
		if (supported) {
			String uuid = EssentialsCompat.getEssentials().getOfflineUser(offlinePlayer.getName()).getConfigUUID()
					.toString();
			File datafolder = EssentialsCompat.getEssentials().getDataFolder();
			if (datafolder.exists()) {
				File configfile = new File(datafolder + "/userdata/" + uuid + ".yml");
				if (configfile.exists()) {
					YamlConfiguration config = new YamlConfiguration();
					try {
						config.load(configfile);
					} catch (IOException | InvalidConfigurationException e) {
						e.printStackTrace();
						return 0;
					}
					return Double.valueOf(config.getString("money"));
				}
			}
		}
		return 0;
	}

	public static void setEssentialsBalance(OfflinePlayer offlinePlayer, double amount) {
		if (supported) {
			String uuid = EssentialsCompat.getEssentials().getOfflineUser(offlinePlayer.getName()).getConfigUUID()
					.toString();
			File datafolder = EssentialsCompat.getEssentials().getDataFolder();
			if (datafolder.exists()) {
				File configfile = new File(datafolder + "/userdata/" + uuid + ".yml");
				if (configfile.exists()) {
					YamlConfiguration config = new YamlConfiguration();
					try {
						config.load(configfile);
						config.set("money", String.valueOf(amount));
						config.save(configfile);
						Messages.debug("updated essentials balance to %s", amount);
					} catch (IOException | InvalidConfigurationException e) {
						e.printStackTrace();
						return;
					}
				}
			}
		}
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	// @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	// private void onPlayerJoin(PlayerJoinEvent event) {
	// final Player player = event.getPlayer();
	// Messages.debug("ESS Balance=%s", getEssBalance(player));
	// }

}
