package one.lindegaard.MobHunting.modifier;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.HuntData;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.LorinthsRpgMobsCompat;

public class LorinthsBonus implements IModifier {

	@Override
	public String getName() {
		return Messages.getString("bonus.lorinthsrpgmobs.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		Messages.debug("LorinthsRpgMobs total multiplier = %s", Math.pow(
				MobHunting.getInstance().getConfigManager().mulitiplierPerLevel, LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity)-1));
		return Math.pow(MobHunting.getInstance().getConfigManager().mulitiplierPerLevel,
				LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity)-1);
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		Messages.debug("%s killed a LorinthsRpgMobs %s level %s", killer.getName(), deadEntity.getType(),
				LorinthsRpgMobsCompat.getLorinthsRpgMobsLevel(deadEntity));
		return deadEntity.hasMetadata(LorinthsRpgMobsCompat.MH_LORINTHS_RPG_MOBS);
	}

}
