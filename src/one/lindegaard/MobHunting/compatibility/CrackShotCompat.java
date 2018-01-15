package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.shampaggon.crackshot.CSUtility;
import com.shampaggon.crackshot.events.WeaponDamageEntityEvent;
import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.MobHunting;

public class CrackShotCompat implements Listener {

	private static Plugin mPlugin;
	private static boolean supported = false;
	// https://dev.bukkit.org/projects/crackshot
	// API: https://github.com/Shampaggon/CrackShot/wiki/Hooking-into-CrackShot

	public CrackShotCompat() {
		if (isDisabledInConfig()) {
			Bukkit.getLogger().info("[MobHunting] Compatibility with CrackShot is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.CrackShot.getName());

			if (mPlugin.getDescription().getVersion().compareTo("0.98.5") >= 0) {

				Bukkit.getConsoleSender().sendMessage("[MobHunting] Enabling compatibility with CrackShot ("
						+ mPlugin.getDescription().getVersion() + ")");

				supported = true;

				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());

			} else {
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.RED + "[MobHunting] Your current version of CrackShot ("
								+ mPlugin.getDescription().getVersion()
								+ ") has no API implemented. Please update to V0.98.5 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.getInstance().getConfigManager().disableIntegrationCrackShot;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getInstance().getConfigManager().disableIntegrationCrackShot;
	}

	public static boolean isCrackShotWeapon(ItemStack itemStack) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(itemStack) != null;
		}
		return false;
	}

	public static String getCrackShotWeapon(ItemStack itemStack) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(itemStack);
		}
		return null;
	}

	public static boolean isCrackShotProjectile(Projectile Projectile) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(Projectile) != null;
		}
		return false;
	}

	public static String getCrackShotWeapon(Projectile Projectile) {
		if (isSupported()) {
			CSUtility cs = new CSUtility();
			return cs.getWeaponTitle(Projectile);
		}
		return null;
	}

	public static boolean isCrackShotUsed(Entity entity) {
		if (MobHunting.getInstance().getMobHuntingManager().getDamageHistory().containsKey(entity))
			return MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity).getCrackShotWeaponUsed() != null 
					&& !MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(entity).getCrackShotWeaponUsed()
							.isEmpty();
		return false;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void onWeaponDamageEntityEvent(WeaponDamageEntityEvent event) {
		if (event.getVictim() instanceof LivingEntity) {
			DamageInformation info = MobHunting.getInstance().getMobHuntingManager().getDamageHistory().get(event.getVictim());
			if (info == null)
				info = new DamageInformation();
			info.setTime(System.currentTimeMillis());
			info.setAttacker(event.getPlayer());
			info.setAttackerPosition(event.getPlayer().getLocation().clone());
			info.setCrackShotWeapon(getCrackShotWeapon(event.getPlayer().getItemInHand()));
			info.setCrackShotPlayer(event.getPlayer());
			MobHunting.getInstance().getMobHuntingManager().getDamageHistory().put((LivingEntity) event.getVictim(), info);
		}
	}

}
