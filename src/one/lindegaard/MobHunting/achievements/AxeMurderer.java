package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;
import one.lindegaard.MobHunting.util.Misc;

public class AxeMurderer implements Achievement, Listener {

	private MobHunting plugin;

	public AxeMurderer(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.axemurderer.name");
	}

	@Override
	public String getID() {
		return "axemurderer";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.axemurderer.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialAxeMurderer;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (Misc.isAxe(event.getDamageInfo().getWeapon())
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialAxeMurdererCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialAxeMurdererCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		//TODO: Best material
		return new ItemStack(Material.WOODEN_AXE);
	}

}
