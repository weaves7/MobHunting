package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class InFighting implements Achievement, Listener {

	private MobHunting plugin;

	public InFighting(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return Messages.getString("achievements.infighting.name");
	}

	@Override
	public String getID() {
		return "infighting";
	}

	@Override
	public String getDescription() {
		return Messages.getString("achievements.infighting.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialInfighting;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onKill(MobHuntKillEvent event) {
		if (!(event.getKilledEntity() instanceof Skeleton)
				|| !plugin.getMobHuntingManager().isHuntEnabledInWorld(event.getKilledEntity().getWorld())
				|| plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) <= 0)
			return;

		Skeleton killed = (Skeleton) event.getKilledEntity();

		if (!(killed.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return;

		EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) killed.getLastDamageCause();

		if (damage.getDamager() instanceof Arrow && ((Arrow) damage.getDamager()).getShooter() instanceof Skeleton) {

			Skeleton skele = (Skeleton) ((Arrow) damage.getDamager()).getShooter();

			if (killed.getTarget() == skele && skele.getTarget() == killed) {
				DamageInformation a, b;
				a = plugin.getMobHuntingManager().getDamageInformation(killed);
				b = plugin.getMobHuntingManager().getDamageInformation(skele);

				Player initiator = null;
				if (a != null)
					initiator = a.getAttacker();

				if (b != null && initiator == null)
					initiator = b.getAttacker();

				if (initiator != null && plugin.getMobHuntingManager().isHuntEnabled(initiator))
					plugin.getAchievementManager().awardAchievement(this, initiator,
							plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
			}
		}
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialInfightingCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialInfightingCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return new ItemStack(Material.IRON_SWORD);
	}
}
