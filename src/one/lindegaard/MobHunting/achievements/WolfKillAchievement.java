package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.MobArenaCompat;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;
import one.lindegaard.MobHunting.mobs.ExtendedMob;
import one.lindegaard.MobHunting.mobs.MinecraftMob;
import one.lindegaard.MobHunting.mobs.MobPlugin;

public class WolfKillAchievement implements ProgressAchievement, Listener {

	private MobHunting plugin;

	public WolfKillAchievement(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.fangmaster.name");
	}

	@Override
	public String getID() {
		return "fangmaster";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.fangmaster.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialFangMaster;
	}

	@Override
	public int getNextLevel() {
		return 500;
	}

	@Override
	public String inheritFrom() {
		return null;
	}

	@Override
	public String nextLevelId() {
		return null;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onWolfKillMob(MobHuntKillEvent event) {
		if (!plugin.getMobHuntingManager().isHuntEnabledInWorld(event.getKilledEntity().getWorld())
				|| !(event.getKilledEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent)
				|| (plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) <= 0))
			return;

		EntityDamageByEntityEvent dmg = (EntityDamageByEntityEvent) event.getKilledEntity().getLastDamageCause();

		if (!(dmg.getDamager() instanceof Wolf))
			return;

		Wolf killer = (Wolf) dmg.getDamager();

		if (killer.isTamed() && killer.getOwner() instanceof OfflinePlayer) {
			Player owner = ((OfflinePlayer) killer.getOwner()).getPlayer();

			if (owner != null && plugin.getMobHuntingManager().isHuntEnabled(owner)) {
				if (MobArenaCompat.isPlayingMobArena((Player) owner)
						&& !plugin.getConfigManager().mobarenaGetRewards) {
					plugin.getMessages().debug("AchiveBlocked: FangMaster was achieved while %s was playing MobArena.",
							owner.getName());
					plugin.getMessages().learn(owner, plugin.getMessages().getString("mobhunting.learn.mobarena"));
				} else
					plugin.getAchievementManager().awardAchievementProgress(this, owner,
							plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()), 1);
			}
		}

	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialFangMasterCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialFangMasterCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return new ItemStack(Material.STRING);
	}

	@Override
	public ExtendedMob getExtendedMob() {
		return new ExtendedMob(MobPlugin.Minecraft, MinecraftMob.Wolf.name());
	}
}
