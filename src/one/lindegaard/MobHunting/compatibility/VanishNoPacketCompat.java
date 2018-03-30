package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.kitteh.vanish.VanishPlugin;

import one.lindegaard.MobHunting.MobHunting;

public class VanishNoPacketCompat implements Listener {

	private static VanishPlugin mPlugin;
	private static boolean supported = false;

	public VanishNoPacketCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with VanishNoPacket is disabled in config.yml");
		} else {

			mPlugin = (VanishPlugin) Bukkit.getServer().getPluginManager()
					.getPlugin(CompatPlugin.VanishNoPacket.getName());

			Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			Bukkit.getConsoleSender()
					.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with VanishNoPacket ("
							+ getVanishNoPacket().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static VanishPlugin getVanishNoPacket() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationVanishNoPacket;
	}

	public static boolean isVanishedModeEnabled(Player player) {
		if (isSupported())
			return getVanishNoPacket().getManager().isVanished(player);
		return false;
	}

}
