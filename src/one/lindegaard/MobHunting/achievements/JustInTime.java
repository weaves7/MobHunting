package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class JustInTime implements Achievement, Listener {

	private MobHunting plugin;

	public JustInTime(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.justintime.name");
	}

	@Override
	public String getID() {
		return "justintime";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.justintime.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialJustInTime;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		// getTime() return world time in ticks. 0 ticks = 6:00 500=6:30
		// Zombies begin burning about 5:30 = 23500
		// player get a reward if he kills between 5:30 and 6:00.
		if (event.getKilledEntity().getWorld().getEnvironment().equals(Environment.NORMAL)
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0
				&& (event.getKilledEntity().getWorld().getTime() >= 23500
						&& event.getKilledEntity().getWorld().getTime() <= 24000)
				&& event.getKilledEntity().getFireTicks() > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialJustInTimeCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialJustInTimeCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		//TODO: right Material?
		return new ItemStack(Material.CLOCK);
	}
}
