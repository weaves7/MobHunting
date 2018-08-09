package one.lindegaard.MobHunting.mobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.CustomMobsCompat;
import one.lindegaard.MobHunting.compatibility.EliteMobsCompat;
import one.lindegaard.MobHunting.compatibility.HerobrineCompat;
import one.lindegaard.MobHunting.compatibility.InfernalMobsCompat;
import one.lindegaard.MobHunting.compatibility.MysteriousHalloweenCompat;
import one.lindegaard.MobHunting.compatibility.MythicMobsCompat;
import one.lindegaard.MobHunting.compatibility.SmartGiantsCompat;
import one.lindegaard.MobHunting.compatibility.TARDISWeepingAngelsCompat;
import one.lindegaard.MobHunting.storage.DataStoreException;

public class ExtendedMobManager {

	private MobHunting plugin;
	
	private HashMap<Integer, ExtendedMob> mobs = new HashMap<Integer, ExtendedMob>();

	public ExtendedMobManager(MobHunting plugin) {
		this.plugin=plugin;
		updateExtendedMobs();
	}

	public void updateExtendedMobs() {
		plugin.getStoreManager().insertMissingVanillaMobs();
		if (CitizensCompat.isSupported())
			plugin.getStoreManager().insertMissingCitizensMobs();
		if (MythicMobsCompat.isSupported())
			plugin.getStoreManager().insertMissingMythicMobs();
		if (CustomMobsCompat.isSupported())
			plugin.getStoreManager().insertCustomMobs();
		if (TARDISWeepingAngelsCompat.isSupported())
			plugin.getStoreManager().insertTARDISWeepingAngelsMobs();
		if (MysteriousHalloweenCompat.isSupported())
			plugin.getStoreManager().insertMysteriousHalloweenMobs();
		if (SmartGiantsCompat.isSupported())
			plugin.getStoreManager().insertSmartGiants();
		if (HerobrineCompat.isSupported())
			plugin.getStoreManager().insertHerobrineMobs();
		if (EliteMobsCompat.isSupported())
			plugin.getStoreManager().insertEliteMobs();
		// Not needed
		// if (InfernalMobsCompat.isSupported())
		// plugin.getStoreManager().insertInfernalMobs();

		Set<ExtendedMob> set = new HashSet<ExtendedMob>();

		try {
			set = (HashSet<ExtendedMob>) plugin.getStoreManager().loadMobs();
		} catch (DataStoreException e) {
			Bukkit.getLogger().severe("[MobHunting] Could not load data from mh_Mobs");
			e.printStackTrace();
		}

		int n = 0;
		Iterator<ExtendedMob> mobset = set.iterator();
		while (mobset.hasNext()) {
			ExtendedMob mob = (ExtendedMob) mobset.next();
			switch (mob.getMobPlugin()) {
			case MythicMobs:
				if (!MythicMobsCompat.isSupported() || !MythicMobsCompat.isEnabledInConfig()
						|| !MythicMobsCompat.isMythicMob(mob.getMobtype()))
					continue;
				break;

			case CustomMobs:
				if (!CustomMobsCompat.isSupported() || !CustomMobsCompat.isEnabledInConfig())
					continue;
				break;

			case TARDISWeepingAngels:
				if (!TARDISWeepingAngelsCompat.isSupported() || !TARDISWeepingAngelsCompat.isEnabledInConfig())
					continue;
				break;

			case Citizens:
				if (!CitizensCompat.isSupported() || !CitizensCompat.isEnabledInConfig()
						|| !CitizensCompat.isSentryOrSentinelOrSentries(mob.getMobtype()))
					continue;
				break;

			case MysteriousHalloween:
				if (!MysteriousHalloweenCompat.isSupported() || !MysteriousHalloweenCompat.isEnabledInConfig())
					continue;
				break;

			case SmartGiants:
				if (!SmartGiantsCompat.isSupported() || !SmartGiantsCompat.isEnabledInConfig())
					continue;
				break;

			case InfernalMobs:
				if (!InfernalMobsCompat.isSupported() || !InfernalMobsCompat.isEnabledInConfig())
					continue;
				break;
				
			case Herobrine:
				if (!HerobrineCompat.isSupported()|| !HerobrineCompat.isEnabledInConfig())
					continue;
				break;
				
			case EliteMobs:
				if (!EliteMobsCompat.isSupported()|| !EliteMobsCompat.isEnabledInConfig())
					continue;
				break;

			case Minecraft:
				break;

			default:
				ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
				console.sendMessage(ChatColor.RED + "[MobHunting] Missing PluginType: " + mob.getMobPlugin().getName()
						+ " in ExtendedMobManager.");
				continue;
			}
			if (!mobs.containsKey(mob.getMob_id())) {
				n++;
				mobs.put(mob.getMob_id(), mob);
			}
		}
		plugin.getMessages().debug("%s mobs was loaded into memory. Total mobs=%s", n, mobs.size());
	}

	public ExtendedMob getExtendedMobFromMobID(int i) {
		return mobs.get(i);
	}

	public HashMap<Integer, ExtendedMob> getAllMobs() {
		return mobs;
	}

	public int getMobIdFromMobTypeAndPluginID(String mobtype, MobPlugin mobPlugin) {
		Iterator<Entry<Integer, ExtendedMob>> mobset = mobs.entrySet().iterator();
		while (mobset.hasNext()) {
			ExtendedMob mob = (ExtendedMob) mobset.next().getValue();
			if (mob.getMobPlugin().equals(mobPlugin) && mob.getMobtype().equalsIgnoreCase(mobtype))
				return mob.getMob_id();
		}
		return 0;
	}

	public ExtendedMob getExtendedMobFromEntity(Entity entity) {
		int mob_id;
		MobPlugin mobPlugin;
		String mobtype;

		if (MythicMobsCompat.isMythicMob(entity)) {
			mobPlugin = MobPlugin.MythicMobs;
			mobtype = MythicMobsCompat.getMythicMobType(entity);
		} else if (CitizensCompat.isNPC(entity)) {
			mobPlugin = MobPlugin.Citizens;
			mobtype = String.valueOf(CitizensCompat.getNPCId(entity));
		} else if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(entity)) {
			mobPlugin = MobPlugin.TARDISWeepingAngels;
			mobtype = TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(entity).name();
		} else if (CustomMobsCompat.isCustomMob(entity)) {
			mobPlugin = MobPlugin.CustomMobs;
			mobtype = CustomMobsCompat.getCustomMobType(entity);
		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(entity)) {
			mobPlugin = MobPlugin.MysteriousHalloween;
			mobtype = MysteriousHalloweenCompat.getMysteriousHalloweenType(entity).name();
		} else if (SmartGiantsCompat.isSmartGiants(entity)) {
			mobPlugin = MobPlugin.SmartGiants;
			mobtype = SmartGiantsCompat.getSmartGiantsMobType(entity);
		} else if (InfernalMobsCompat.isInfernalMob(entity)) {
			mobPlugin = MobPlugin.InfernalMobs;
			MinecraftMob mob = MinecraftMob.getMinecraftMobType(entity);
			if (mob != null)
				mobtype = mob.name();
			else{
				plugin.getMessages().debug("unhandled entity %s", entity.getType());
				mobtype = "";
			}
		} else if (HerobrineCompat.isHerobrineMob(entity)){
			mobPlugin = MobPlugin.Herobrine;
			mobtype = HerobrineCompat.getHerobrineMobType(entity);
		} else if (EliteMobsCompat.isEliteMobs(entity)){
			mobPlugin = MobPlugin.EliteMobs;
			mobtype = EliteMobsCompat.getEliteMobsType(entity).name();
		} else {
			// StatType
			mobPlugin = MobPlugin.Minecraft;
			MinecraftMob mob = MinecraftMob.getMinecraftMobType(entity);
			if (mob != null)
				mobtype = mob.name();
			else
				mobtype = "";
		}
		mob_id = getMobIdFromMobTypeAndPluginID(mobtype, mobPlugin);
		return new ExtendedMob(mob_id, mobPlugin, mobtype);
	}

	// This is only used to get a "random" mob_id stored when an Achievement is
	// stored in mh_Daily
	public ExtendedMob getFirstMob() {
		int mob_id = mobs.keySet().iterator().next();
		return mobs.get(mob_id);
	}

	//public static String getMobName(Entity mob) {
	//		return mob.getName();
	//}

	public String getTranslatedName() {
		return "";
	};

}
