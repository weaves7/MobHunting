package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import one.lindegaard.MobHunting.MobHunting;

public class FactionsHelperCompat {

	private static boolean supported = false;
	private static Plugin mPlugin;

	private final String PREFIX = ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET;

	public enum FactionsVersion {
		NOT_DETECTED, FACTIONS_UUID, FACTIONS
	};

	public static FactionsVersion factionsVersion = FactionsVersion.NOT_DETECTED;

	public FactionsHelperCompat() {
		ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
		if (isDisabledInConfig()) {
			console.sendMessage(PREFIX + "Compatibility with Factions/FactionsUUID is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Factions.getName());
			if (mPlugin.getDescription().getVersion().compareTo("1.6.9.6") >= 0) {
				try {
					@SuppressWarnings({ "rawtypes", "unused" })
					Class cls = Class.forName("com.massivecraft.factions.entity.BoardColl");
					console.sendMessage(PREFIX + "Enabling compatibility with Factions ("
							+ mPlugin.getDescription().getVersion() + ")");
					factionsVersion = FactionsVersion.FACTIONS;
					supported = true;
				} catch (ClassNotFoundException e) {
					console.sendMessage(PREFIX + ChatColor.RED + "Your version of Factions ("
							+ mPlugin.getDescription().getVersion()
							+ ") is not complatible with this version of MobHunting, please upgrade.");
				}
				// Bukkit.getPluginManager().registerEvents(new FactionsCompat(),
				// MobHunting.getInstance());

			} else if (mPlugin.getDescription().getVersion().startsWith("1.6.9.5-U")) {
				console.sendMessage(PREFIX + "Enabling compatibility with FactionUUID ("
						+ mPlugin.getDescription().getVersion() + ")");
				factionsVersion = FactionsVersion.FACTIONS_UUID;
				supported = true;
				// Bukkit.getPluginManager().registerEvents(new FactionsUUIDCompat(),
				// MobHunting.getInstance());

			} else {
				console.sendMessage(PREFIX + ChatColor.RED + "Factions is outdated. Integration will be disabled");
				return;
			}
		}
	}

	public static boolean isSupported() {
		return supported;
	}

	public static void setSupported(boolean status) {
		supported = status;
	}

	public static FactionsVersion getFactionsVersion() {
		return factionsVersion;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.getInstance().getConfigManager().disableIntegrationFactions;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getInstance().getConfigManager().disableIntegrationFactions;
	}

	public static boolean isInSafeZone(Player player) {
		if (supported) {
			switch (factionsVersion) {
			case FACTIONS:
				return FactionsCompat.isInSafeZone(player);
			case FACTIONS_UUID:
				return FactionsUUIDCompat.isInSafeZone(player);
			case NOT_DETECTED:
				return false;
			}
		}
		return false;
	}

	public static boolean isInWildernessZone(Player player) {
		if (supported) {
			switch (factionsVersion) {
			case FACTIONS:
				return FactionsCompat.isInWilderness(player);
			case FACTIONS_UUID:
				return FactionsUUIDCompat.isInWilderness(player);
			case NOT_DETECTED:
				return false;
			}
		}
		return false;
	}

	public static boolean isInWarZone(Player player) {
		if (supported) {
			switch (factionsVersion) {
			case FACTIONS:
				return FactionsCompat.isInWarZone(player);
			case FACTIONS_UUID:
				return FactionsUUIDCompat.isInWarZone(player);
			case NOT_DETECTED:
				return false;
			}
		}
		return false;
	}

}
