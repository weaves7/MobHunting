package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import de.Keyle.MyPet.MyPetPlugin;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.entity.MyPetBukkitEntity;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent;
import de.Keyle.MyPet.api.event.MyPetInventoryActionEvent.Action;
import de.Keyle.MyPet.api.event.MyPetPickupItemEvent;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.rewards.Reward;

public class MyPetCompat implements Listener {
	private static boolean supported = false;
	private static MyPetPlugin mPlugin;

	public MyPetCompat() {
		if (MobHunting.getInstance().getConfigManager().disableIntegrationMyPet) {
			Bukkit.getLogger().info("[MobHunting] Compatibility with MyPet is disabled in config.yml");
		} else {
			mPlugin = (MyPetPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.MyPet.getName());
			Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
			Bukkit.getLogger().info("[MobHunting] Enabling compatibility with MyPet ("
					+ getMyPetPlugin().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************

	public static boolean isSupported() {
		return supported;
	}

	public static MyPetPlugin getMyPetPlugin() {
		return mPlugin;
	}

	public static boolean isMyPet(Entity entity) {
		if (isSupported())
			return entity instanceof MyPetBukkitEntity;
		return false;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getInstance().getConfigManager().disableIntegrationMyPet;
	}

	public static boolean isKilledByMyPet(Entity entity) {
		if (isSupported() && (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
			EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();
			if (dmg != null && (dmg.getDamager() instanceof MyPetBukkitEntity))
				return true;
		}
		return false;
	}

	public static MyPetBukkitEntity getMyPet(Entity entity) {
		EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();

		if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
			return null;

		MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();

		return killer;
	}

	public static Player getMyPetOwner(Entity entity) {

		if (!(entity.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return null;

		EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) entity.getLastDamageCause();

		if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
			return null;

		MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();

		if (killer.getOwner() == null)
			return null;

		return killer.getOwner().getPlayer();
	}

	public void getInv(Player player) {
		// mPlugin.getMyPetManager().getMyPet(player).getSkilltree();
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMyPetKillMob(EntityDeathEvent event) {
		if (!MobHunting.getInstance().getMobHuntingManager().isHuntEnabledInWorld(event.getEntity().getWorld())
				|| !(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return;

		EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
		if (dmg == null || !(dmg.getDamager() instanceof MyPetBukkitEntity))
			return;

		MyPetBukkitEntity killer = (MyPetBukkitEntity) dmg.getDamager();
		if (killer.getOwner() != null) {
			Player owner = killer.getOwner().getPlayer();
			if (owner != null && MobHunting.getInstance().getMobHuntingManager().isHuntEnabled(owner))
				MobHunting.getInstance().getAchievementManager().awardAchievementProgress("fangmaster", owner,
						MobHunting.getInstance().getExtendedMobManager().getExtendedMobFromEntity(event.getEntity()),
						1);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	private void onMyPetInventoryActionEvent(MyPetInventoryActionEvent event) {
		if (event.getAction() == Action.Pickup)
			Messages.debug("MyPetInventoryActionEvent=%s", event.getAction().name());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	private void onMyPetPickupItem(MyPetPickupItemEvent event) {
		if (event.isCancelled())
			return;

		Item item = event.getItem();
		Player player = event.getOwner().getPlayer();
		MyPet pet = event.getPet();

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			MobHunting.getInstance().getMessages().playerActionBarMessage(player,
					Messages.getString("mobhunting.reward.mypet_pickup", "rewardname",
							ChatColor.valueOf(MobHunting.getInstance().getConfigManager().dropMoneyOnGroundTextColor)
									+ reward.getDisplayname(),
							"petname", pet.getPetName(), "money",
							MobHunting.getInstance().getRewardManager().getEconomy().format(reward.getMoney())));
			Messages.debug("%s owned by %s picked up %s %s.", pet.getPetName(), player.getName(),
					MobHunting.getInstance().getRewardManager().getEconomy().format(reward.getMoney()),
					reward.getDisplayname());
			if (reward.isBagOfGoldReward() || reward.isItemReward()) {
				if (!MobHunting.getInstance().getConfigManager().dropMoneyOnGroundUseAsCurrency) {
					event.setCancelled(true);
					item.remove();
					MobHunting.getInstance().getRewardManager().depositPlayer(player,reward.getMoney());
				}
			}
		}
	}

}
