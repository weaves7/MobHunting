package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import one.lindegaard.MobHunting.MobHunting;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.api.EntityManager;

public class StackMobCompat implements Listener {

	// https://www.spigotmc.org/resources/stackmob.29999/

	private static boolean supported = false;
	private static Plugin mPlugin;

	public StackMobCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with StackMob is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.StackMob.getName());
			if (mPlugin.getDescription().getVersion().compareTo("2.0.9") >= 0) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with StackMob (" + mPlugin.getDescription().getVersion() + ").");
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				supported = true;
			} else {
				Bukkit.getServer().getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
								+ "Your current version of StackMob (" + mPlugin.getDescription().getVersion()
								+ ") is not supported by MobHunting, please upgrade to 2.0.9 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************

	public static boolean isSupported() {
		return supported;
	}

	private static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationStackMob;
	}

	public static EntityManager getEntityManager() {
		return new EntityManager((StackMob) mPlugin);
	}

	public static boolean isStackedMob(Entity entity) {
		if (isSupported()) {
			return getEntityManager().isStackedEntity(entity);
		}
		return false;
	}

	public static int getStackSize(Entity entity) {
		return getEntityManager().getStackedEntity(entity).getSize();
	}

	public static boolean killHoleStackOnDeath(Entity entity) {
		return mPlugin.getConfig().getBoolean("kill-all.enabled");
	}

	public static boolean isGrindingStackedMobsAllowed() {
		return MobHunting.getInstance().getConfigManager().isGrindingStackedMobsAllowed;
	}

}
