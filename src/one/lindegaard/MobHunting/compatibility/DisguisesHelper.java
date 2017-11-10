package one.lindegaard.MobHunting.compatibility;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.MobHunting;

public class DisguisesHelper {

	// ***************************************************************************
	// Integration to LibsDisguises, DisguiseCraft, IDisguise
	// ***************************************************************************

	/**
	 * isDisguised - checks if the player is disguised.
	 * 
	 * @param entity
	 * @return true when the player is disguised and false when the is not.
	 */
	public static boolean isDisguised(Entity entity) {
		if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(LibsDisguisesCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationLibsDisguises)
			return LibsDisguisesCompat.isDisguised((Player) entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(DisguiseCraftCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationDisguiseCraft)
			return DisguiseCraftCompat.isDisguised((Player) entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(IDisguiseCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationIDisguise)
			return IDisguiseCompat.isDisguised(entity);
		else {
			return false;
		}
	}

	/**
	 * isDisguisedAsAsresiveMob - checks if the player is disguised as a mob who
	 * attacks players.
	 * 
	 * @param entity
	 * @return true when the player is disguised as a mob who attacks players
	 *         and false when not.
	 */
	public static boolean isDisguisedAsAgresiveMob(Entity entity) {
		if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(LibsDisguisesCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationLibsDisguises)
			return LibsDisguisesCompat.isAggresiveDisguise(entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(DisguiseCraftCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationDisguiseCraft)
			return DisguiseCraftCompat.isAggresiveDisguise(entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(IDisguiseCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationIDisguise)
			return IDisguiseCompat.isAggresiveDisguise(entity);
		else {
			return false;
		}
	}

	/**
	 * isDisguisedAsPlayer - checks if the player is disguised as another
	 * player.
	 * 
	 * @param entity
	 * @return true when the player is disguised as another player, and false
	 *         when not.
	 */
	public static boolean isDisguisedAsPlayer(Entity entity) {
		if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(LibsDisguisesCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationLibsDisguises)
			return LibsDisguisesCompat.isPlayerDisguise((Player) entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(DisguiseCraftCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationDisguiseCraft)
			return DisguiseCraftCompat.isPlayerDisguise((Player) entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(IDisguiseCompat.class) && entity instanceof Player
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationIDisguise)
			return IDisguiseCompat.isPlayerDisguise((Player) entity);
		else {
			return false;
		}
	}

	public static void undisguiseEntity(Entity entity) {
		if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(LibsDisguisesCompat.class)
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationLibsDisguises)
			LibsDisguisesCompat.undisguiseEntity(entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(DisguiseCraftCompat.class)
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationDisguiseCraft)
			DisguiseCraftCompat.undisguisePlayer(entity);
		else if (MobHunting.getInstance().getCompatibilityManager().isPluginLoaded(IDisguiseCompat.class)
				&& !MobHunting.getInstance().getConfigManager().disableIntegrationIDisguise)
			IDisguiseCompat.undisguisePlayer(entity);
		else {

		}
	}
}
