package one.lindegaard.MobHunting.compatibility;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.magmaguy.elitemobs.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteMobEntity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import com.magmaguy.elitemobs.EliteMobs;
import com.magmaguy.elitemobs.MetadataHandler;
import com.magmaguy.elitemobs.EntityTracker;
import com.magmaguy.elitemobs.config.ConfigValues;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.mobs.MobPlugin;
import one.lindegaard.MobHunting.rewards.RewardData;

public class EliteMobsCompat implements Listener {

	// https://www.spigotmc.org/resources/%E2%9A%94elitemobs%E2%9A%94.40090/

	private static boolean supported = false;
	private static Plugin mPlugin;
	private static HashMap<String, RewardData> mMobRewardData = new HashMap<String, RewardData>();
	private static File file = new File(MobHunting.getInstance().getDataFolder(), "EliteMobs-rewards.yml");
	private static YamlConfiguration config = new YamlConfiguration();
	public static final String MH_ELITEMOBS = "MH:ELITEMOBS";

	public EliteMobsCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getLogger().info(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with EliteMobs is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.EliteMobs.getName());

			if (mPlugin.getDescription().getVersion().compareTo("6.7.0") >= 0) {
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				Bukkit.getConsoleSender()
						.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
								+ "Enabling Compatibility with EliteMobs ("
								+ getEliteMobs().getDescription().getVersion() + ")");

				supported = true;

				loadEliteMobsMobsData();
				saveEliteMobsData();

			} else {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
						+ "Your current version of EliteMobs (" + mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Please update EliteMobs to version 6.7.0 or newer.");
			}
		}

	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public EliteMobs getEliteMobs() {
		return (EliteMobs) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEliteMobs(Entity entity) {
		if (isSupported())
			return EntityTracker.isEliteMob(entity);
		return false;
	}

	public static boolean isEliteMobs_Kraken(Entity entity) {
		if (isSupported()) {
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.KRAKEN)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isEliteMobs_Balrog(Entity entity) {
		if (isSupported())
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.BALROG)) {
				return true;
			}
		return false;
	}

	public static boolean isEliteMobs_Fae(Entity entity) {
		if (isSupported())
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.FAE)) {
				return true;
			}
		return false;
	}

	public static boolean isEliteMobs_TheReturned(Entity entity) {
		if (isSupported())
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.THE_RETURNED)) {
				return true;
			}
		return false;
	}

	public static boolean isEliteMobs_TreasureGoblin(Entity entity) {
		if (isSupported())
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.TREASURE_GOBLIN)) {
				return true;
			}
		return false;
	}

	public static boolean isEliteMobs_ZombieKing(Entity entity) {
		if (isSupported())
			if (EntityTracker.getEliteMobEntity(entity).getName().equals(EliteBoss.ZOMBIE_KING)) {
				return true;
			}
		return false;
	}

	public static enum Mobs {
		Kraken(EliteBoss.KRAKEN), Balrog(EliteBoss.BALROG), Fae(EliteBoss.FAE), TheReturned(EliteBoss.THE_RETURNED), TreasureGoblin(
						EliteBoss.TREASURE_GOBLIN), EliteMob(EliteBoss.ELITE_MOB);

		private String name;

		private Mobs(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	};

	public static Mobs getEliteMobsType(Entity entity) {
		if (isEliteMobs_Kraken(entity))
			return Mobs.Kraken;
		else if (isEliteMobs_Balrog(entity))
			return Mobs.Balrog;
		else if (isEliteMobs_Fae(entity))
			return Mobs.Fae;
		else if (isEliteMobs_TheReturned(entity))
			return Mobs.TheReturned;
		else if (isEliteMobs_TreasureGoblin(entity)) {
			return Mobs.TreasureGoblin;
		}
		else
			return Mobs.EliteMob;
	}

	//Seems to never be used.
	public static String getName(Entity entity) {
		System.out.println("Called getName");
		if (isEliteMobs_Kraken(entity))
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.kraken");
		else if (isEliteMobs_Balrog(entity))
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.balrog");
		else if (isEliteMobs_Fae(entity))
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.fae");
		else if (isEliteMobs_TheReturned(entity))
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.the_returned");
		else if (isEliteMobs_TreasureGoblin(entity))
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.treasure_goblin");
		else
			return MobHunting.getInstance().getMessages().getString("mobs.EliteMobs.elitemob");
	}

	public static int getEliteMobsLevel(Entity entity) {
		if (isEliteMobs(entity)) {
			EliteMobEntity eliteMobEntity = EntityTracker.getEliteMobEntity(entity);
			return eliteMobEntity.getLevel();
		}
		return 0;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationEliteMobs;
	}

	public static HashMap<String, RewardData> getMobRewardData() {
		return mMobRewardData;
	}

	public static int getProgressAchievementLevel1(String mobtype) {
		return mMobRewardData.get(mobtype).getAchivementLevel1();
	}

	// **************************************************************************
	// LOAD & SAVE
	// **************************************************************************
	public static void loadEliteMobsMobsData() {
		try {
			if (!file.exists()) {
				for (Mobs monster : Mobs.values()) {
					mMobRewardData.put(monster.name(),
							new RewardData(MobPlugin.EliteMobs, monster.name(), monster.getName(), true, "10:20", 1,
									"You killed an EliteMob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
					saveEliteMobsData(monster.name());
					MobHunting.getInstance().getStoreManager().insertEliteMobs(monster.name());
				}
				MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
				return;
			}

			config.load(file);
			for (String key : config.getKeys(false)) {
				ConfigurationSection section = config.getConfigurationSection(key);
				RewardData mob = new RewardData();
				mob.read(section);
				mob.setMobType(key);
				mMobRewardData.put(key, mob);
				MobHunting.getInstance().getStoreManager().insertEliteMobs(key);
			}
			MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
			MobHunting.getInstance().getMessages().debug("Loaded %s EliteMobs", mMobRewardData.size());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

	}

	//Seems to never be used.
	public static void loadEliteMobsData(String key) {
		try {
			if (!file.exists()) {
				return;
			}

			config.load(file);
			ConfigurationSection section = config.getConfigurationSection(key);
			RewardData mob = new RewardData();
			mob.read(section);
			mob.setMobType(key);
			mMobRewardData.put(key, mob);
			MobHunting.getInstance().getStoreManager().insertEliteMobs(key);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static void saveEliteMobsData() {
		try {
			config.options().header("This a extra MobHunting config data for the EliteMobs on your server.");

			if (mMobRewardData.size() > 0) {

				int n = 0;
				for (String str : mMobRewardData.keySet()) {
					ConfigurationSection section = config.createSection(str);
					mMobRewardData.get(str).save(section);
					n++;
				}

				if (n != 0) {
					MobHunting.getInstance().getMessages().debug("Saving Mobhunting extra EliteMobs data.");
					config.save(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveEliteMobsData(String key) {
		try {
			if (mMobRewardData.containsKey(key)) {
				ConfigurationSection section = config.createSection(key);
				mMobRewardData.get(key).save(section);
				MobHunting.getInstance().getMessages().debug("Saving extra EliteMobs data for mob=%s (%s)", key,
						mMobRewardData.get(key).getMobName());
				config.save(file);
			} else {
				MobHunting.getInstance().getMessages().debug("ERROR! EliteMobs ID (%s) is not found in mMobRewardData",
						key);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
//THIS IS LIKELY BROKEN!!!!!
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onEliteMobsSpawnEvent(EntitySpawnEvent event) {

		Entity entity = event.getEntity();

		if (EntityTracker.isEliteMob(event.getEntity())) {

			Mobs monster = getEliteMobsType(entity);

			if (mMobRewardData != null && !mMobRewardData.containsKey(monster.name())) {
				MobHunting.getInstance().getMessages().debug("New EliteMob found=%s", monster.name());
				mMobRewardData.put(monster.name(),
						new RewardData(MobPlugin.EliteMobs, monster.name(), monster.getName(), true, "40:60", 1,
								"You killed an EliteMob", new ArrayList<HashMap<String, String>>(), 1, 0.02));
				saveEliteMobsData(monster.name());
				MobHunting.getInstance().getStoreManager().insertEliteMobs(monster.name());
				// Update mob loaded into memory
				MobHunting.getInstance().getExtendedMobManager().updateExtendedMobs();
				MobHunting.getInstance().getMessages().injectMissingMobNamesToLangFiles();
			}

			event.getEntity().setMetadata(MH_ELITEMOBS,
					new FixedMetadataValue(mPlugin, mMobRewardData.get(monster.name())));
		}
	}

	//THESE STRINGS MAY NOT BE CORRECT DUE TO FORMATTING OR COLOR CODES!!!
	public static class EliteBoss{
		public static final String KRAKEN = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Name of mob in Kraken event"));
		public static final String BALROG = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Name of mob in Balrog event"));
		public static final String FAE = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Name of mob in Fae event"));
		public static final String TREASURE_GOBLIN = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Name of mob in Treasure Goblin events"));
		public static final String THE_RETURNED = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Dead moon names.Name of The Returned"));
		public static final String ZOMBIE_KING = ChatColor.translateAlternateColorCodes('&',ConfigValues.eventsConfig.getString("Dead moon names.Name of the Zombie King"));
		public static final String ELITE_MOB = "EliteMob";

	}
}
