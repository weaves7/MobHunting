package one.lindegaard.MobHunting.achievements;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CreeperBoxing extends AbstractSkullAchievement implements Listener {

	private MobHunting plugin;

	public CreeperBoxing(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.creeperboxing.name");
	}

	@Override
	public String getID() {
		return "creeperboxing";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.creeperboxing.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialCreeperPunch;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (event.getKilledEntity() instanceof Creeper && !event.getDamageInfo().hasUsedWeapon()
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialCreeperPunchCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialCreeperPunchCmdDesc;
	}

}
