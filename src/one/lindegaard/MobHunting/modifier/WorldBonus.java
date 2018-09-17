package one.lindegaard.MobHunting.modifier;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import one.lindegaard.MobHunting.DamageInformation;
import one.lindegaard.MobHunting.HuntData;
import one.lindegaard.MobHunting.MobHunting;

public class WorldBonus implements IModifier {

	@Override
	public String getName() {
		return MobHunting.getInstance().getMessages().getString("bonus.world.name");
	}

	@Override
	public double getMultiplier(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {

		if (MobHunting.getInstance().getConfigManager().worldMultiplier.containsKey(killer.getWorld().getName())) {
			try {
				return Double.valueOf(
						MobHunting.getInstance().getConfigManager().worldMultiplier.get(killer.getWorld().getName()));
			} catch (Exception e) {
				return 1;
			}
		} else
			return 1;
	}

	@Override
	public boolean doesApply(Entity deadEntity, Player killer, HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (MobHunting.getInstance().getConfigManager().worldMultiplier.containsKey(killer.getWorld().getName())) {
			try {
				return Double.valueOf(MobHunting.getInstance().getConfigManager().worldMultiplier
						.get(killer.getWorld().getName())) != 1;
			} catch (Exception e) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting " + ChatColor.RED
						+ "Error in World multiplier for word:" + killer.getWorld().getName());

				return false;
			}
		} else
			return false;
	}

}
