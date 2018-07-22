package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.SmartGiantsCompat;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class DavidAndGoliath implements Achievement, Listener {

	private MobHunting plugin;

	public DavidAndGoliath(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.davidandgoliath.name");
	}

	@Override
	public String getID() {
		return "davidandgoliath";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.davidandgoliath.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().davidAndGoliat;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (SmartGiantsCompat.isSmartGiants(event.getKilledEntity())
				&& event.getDamageInfo().getWeapon().getType() == Material.STONE_BUTTON
				&& !(plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) == 0
						&& plugin.getRewardManager().getKillCommands(event.getKilledEntity()).isEmpty()))
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().davidAndGoliatCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().davidAndGoliatCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		//TODO: Best head?????
		return new ItemStack(Material.PLAYER_HEAD);
	}

}
