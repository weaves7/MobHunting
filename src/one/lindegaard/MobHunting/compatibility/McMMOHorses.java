package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.blueskullgames.horserpg.HorseRPG;
import com.blueskullgames.horserpg.RPGHorse;

import one.lindegaard.MobHunting.MobHunting;

public class McMMOHorses {

	// https://www.spigotmc.org/resources/mcmmohorses.46301/
	
	private static HorseRPG mPlugin;
	private static boolean supported = false;

	public McMMOHorses() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with McMMOHorses is disabled in config.yml");
		} else {
			mPlugin = (HorseRPG) Bukkit.getPluginManager().getPlugin(CompatPlugin.McMMOHorses.getName());

			Bukkit.getConsoleSender()
					.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with McMMOHorses ("
							+ getMcMMOHorses().getDescription().getVersion() + ")");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static HorseRPG getMcMMOHorses() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationMcMMOHorses;
	}

	public static RPGHorse getHorse(Entity entity) {
		if (isSupported()) {
			return HorseRPG.hSpawnedHorses.get(entity);
		} else
			return null;
	}

	public static boolean isMcMMOHorse(Entity entity) {
		if (isSupported()) {
			return HorseRPG.hSpawnedHorses.containsKey(entity);
		} else
			return false;
	}
	
	public static boolean isMcMMOHorseOwner(Entity entity, Player player) {
		if (isSupported()) {
			return HorseRPG.hSpawnedHorses.get(entity).owner.equalsIgnoreCase(player.getName());
		} else
			return false;
	}

	public static boolean isGodmode(Entity entity) {
		if (isSupported()) {
			return HorseRPG.hSpawnedHorses.get(entity).godmode;
		} else
			return false;
	}

	public static boolean isPermanentDeath() {
		if (isSupported()) {
			return HorseRPG.permanentDeath;
		} else
			return false;
	}
	
	public static RPGHorse getCurrentHorse(Player player) {
		if (isSupported()) {
			return HorseRPG.pCurrentHorse.get(player);
		} else
			return null;
	}
}
