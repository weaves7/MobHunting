package one.lindegaard.MobHunting.achievements;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.MobArenaCompat;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class Creepercide extends AbstractSkullAchievement implements Listener {

	private MobHunting plugin;

	public Creepercide(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.creepercide.name");
	}

	@Override
	public String getID() {
		return "creepercide";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.creepercide.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialCreepercide;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onKill(MobHuntKillEvent event) {
		if (!(event.getKilledEntity() instanceof Creeper)
				|| !plugin.getMobHuntingManager().isHuntEnabledInWorld(event.getKilledEntity().getWorld()))
			return;

		if (plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) <= 0)
			return;

		Creeper killed = (Creeper) event.getKilledEntity();

		if (!(killed.getLastDamageCause() instanceof EntityDamageByEntityEvent))
			return;

		EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) killed.getLastDamageCause();

		if (damage.getDamager() instanceof Creeper) {
			Player initiator = null;

			if (((Creeper) event.getKilledEntity()).getTarget() instanceof Player)
				initiator = (Player) ((Creeper) event.getKilledEntity()).getTarget();
			else {
				DamageInformation a, b;
				a = plugin.getMobHuntingManager().getDamageInformation(killed);
				b = plugin.getMobHuntingManager().getDamageInformation((Creeper) damage.getDamager());

				if (a != null)
					initiator = a.getAttacker();

				if (b != null && initiator == null)
					initiator = b.getAttacker();
			}

			if (initiator != null && plugin.getMobHuntingManager().isHuntEnabled(initiator)) {
				// Check if player (initiator) is playing MobArena.
				if (MobArenaCompat.isPlayingMobArena((Player) initiator)
						&& !plugin.getConfigManager().mobarenaGetRewards) {
					plugin.getMessages().debug("AchiveBlocked: CreeperCide was achieved while %s was playing MobArena.",
							initiator.getName());
					plugin.getMessages().learn(initiator, plugin.getMessages().getString("mobhunting.learn.mobarena"));
				} else
					plugin.getAchievementManager().awardAchievement("creepercide", initiator,
							plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
			}
		}
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialCreepercideCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialCreepercideCmdDesc;
	}
}
