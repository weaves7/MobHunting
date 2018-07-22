package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;

public class Electrifying implements Achievement, Listener {

	private MobHunting plugin;

	public Electrifying(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.electrifying.name");
	}

	@Override
	public String getID() {
		return "electrifying";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.electrifying.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialCharged;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (event.getKilledEntity() instanceof Creeper && ((Creeper) event.getKilledEntity()).isPowered()
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialChargedCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialChargedCmdDesc;
	}

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getSymbol() {
		//TODO: Best Material?
		//ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1);
		ItemStack skull = new ItemStack(Material.CREEPER_HEAD);
		SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
		skullMeta.setOwner("MHF_Creeper");
		skull.setItemMeta(skullMeta);
		return skull;
	}
}
