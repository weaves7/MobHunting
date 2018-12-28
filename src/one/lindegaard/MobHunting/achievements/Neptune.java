package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class Neptune implements Achievement, Listener {

	private MobHunting plugin;

	public Neptune(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.neptune.name");
	}

	@Override
	public String getID() {
		return "neptune";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.neptune.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialNeptune;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onKillCompleted(MobHuntKillEvent event) {
		if (event.getDamageInfo().getWeapon().getType() == Material.TRIDENT
				&& !event.getDamageInfo().isMeleWeapenUsed()
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0) {
			double dist = event.getDamageInfo().getAttackerPosition().distance(event.getKilledEntity().getLocation());
			if (dist >= 20) {
				plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
						plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
			}
		}
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialNeptuneCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialNeptuneCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return new ItemStack(Material.TRIDENT);
	}
}
