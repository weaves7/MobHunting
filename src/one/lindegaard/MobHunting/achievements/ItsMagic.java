package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class ItsMagic implements Achievement, Listener {

	private MobHunting plugin;

	public ItsMagic(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.itsmagic.name");
	}

	@Override
	public String getID() {
		return "itsmagic";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.itsmagic.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialItsMagic;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (event.getDamageInfo().getWeapon().getType() == Material.POTION
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialItsMagicCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialItsMagicCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return new ItemStack(Material.POTION);
	}
}
