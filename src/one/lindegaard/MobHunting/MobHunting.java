package one.lindegaard.MobHunting;

import java.io.File;
import java.util.Random;

import one.lindegaard.MobHunting.achievements.*;
import one.lindegaard.MobHunting.bounty.BountyManager;
import one.lindegaard.MobHunting.bounty.WorldGroup;
import one.lindegaard.MobHunting.commands.BountyCommand;
import one.lindegaard.MobHunting.commands.CheckGrindingCommand;
import one.lindegaard.MobHunting.commands.ClearGrindingCommand;
import one.lindegaard.MobHunting.commands.CommandDispatcher;
import one.lindegaard.MobHunting.commands.DatabaseCommand;
import one.lindegaard.MobHunting.commands.DebugCommand;
import one.lindegaard.MobHunting.commands.HappyHourCommand;
import one.lindegaard.MobHunting.commands.HeadCommand;
import one.lindegaard.MobHunting.commands.HologramCommand;
import one.lindegaard.MobHunting.commands.LeaderboardCommand;
import one.lindegaard.MobHunting.commands.LearnCommand;
import one.lindegaard.MobHunting.commands.MoneyCommand;
import one.lindegaard.MobHunting.commands.AchievementsCommand;
import one.lindegaard.MobHunting.commands.BlacklistAreaCommand;
import one.lindegaard.MobHunting.commands.MuteCommand;
import one.lindegaard.MobHunting.commands.RegionCommand;
import one.lindegaard.MobHunting.commands.ReloadCommand;
import one.lindegaard.MobHunting.commands.SelectCommand;
import one.lindegaard.MobHunting.commands.TopCommand;
import one.lindegaard.MobHunting.commands.UpdateCommand;
import one.lindegaard.MobHunting.commands.VersionCommand;
import one.lindegaard.MobHunting.commands.WhitelistAreaCommand;
import one.lindegaard.MobHunting.compatibility.*;
import one.lindegaard.MobHunting.config.ConfigManagerOld;
import one.lindegaard.MobHunting.config.ConfigManager;
import one.lindegaard.MobHunting.grinding.GrindingManager;
import one.lindegaard.MobHunting.leaderboard.LeaderboardManager;
import one.lindegaard.MobHunting.mobs.ExtendedMobManager;
import one.lindegaard.MobHunting.rewards.RewardManager;
import one.lindegaard.MobHunting.storage.DataStoreException;
import one.lindegaard.MobHunting.storage.DataStoreManager;
import one.lindegaard.MobHunting.storage.IDataStore;
import one.lindegaard.MobHunting.storage.MySQLDataStore;
import one.lindegaard.MobHunting.storage.SQLiteDataStore;
import one.lindegaard.MobHunting.update.SpigetUpdater;
import one.lindegaard.MobHunting.util.Misc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import io.chazza.advancementapi.AdvancementManager;

public class MobHunting extends JavaPlugin {

	// Constants
	private final static String pluginName = "mobhunting";

	private static MobHunting instance;
	public Random mRand = new Random();
	private File mFile = new File(getDataFolder(), "config.yml");

	private Messages mMessages;
	private ConfigManagerOld mConfig0;
	private ConfigManager mConfig;
	private RewardManager mRewardManager;
	private MobHuntingManager mMobHuntingManager;
	private FishingManager mFishingManager;
	private GrindingManager mGrindingManager;
	private LeaderboardManager mLeaderboardManager;
	private AchievementManager mAchievementManager;
	private BountyManager mBountyManager;
	private ParticleManager mParticleManager = new ParticleManager();
	private MetricsManager mMetricsManager;
	private PlayerSettingsManager mPlayerSettingsManager;
	private WorldGroup mWorldGroupManager;
	private ExtendedMobManager mExtendedMobManager;
	private IDataStore mStore;
	private DataStoreManager mStoreManager;
	private AdvancementManager mAdvancementManager;
	private CommandDispatcher mCommandDispatcher;
	private CompatibilityManager mCompatibilityManager;
	private SpigetUpdater mSpigetUpdater;

	private boolean mInitialized = false;

	@Override
	public void onLoad() {

		instance = this;

		mMessages = new Messages(this);

		// Check what happen if WorldGuard is installed and register MobHuting
		// Flag
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (wg != null)
			WorldGuardHelper.registerFlag();
	}

	@Override
	public void onEnable() {

		int config_version = ConfigManager.getConfigVersion(mFile);
		Bukkit.getConsoleSender().sendMessage(
				ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Your config version is " + config_version);
		switch (config_version) {
		case 0:
			mConfig0 = new ConfigManagerOld(this, mFile);
			if (mConfig0.loadConfig()) {
				if (mConfig.convertConfig(mConfig0)) {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Converting config.yml to new version 1 format");
					if (mConfig.backup)
						mConfig.backupConfig(mFile);
				}
			}
			break;
		case -2:
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Defect config.yml file. Deleted.");
		case -1:
			mConfig = new ConfigManager(this, mFile);
			if (!mConfig.loadConfig())
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Error could not load config.yml");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Creating new config.yml, version=" + mConfig.configVersion);
			break;
		default:
			mConfig = new ConfigManager(this, mFile);
			if (mConfig.loadConfig()) {
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "Existing config.yml loaded.");
				if (mConfig.backup)
					mConfig.backupConfig(mFile);
			} else
				throw new RuntimeException(getMessages().getString(pluginName + ".config.fail"));
			break;
		}
		mConfig.saveConfig();

		if (isbStatsEnabled())
			getMessages().debug("bStat is enabled");
		else {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "=====================WARNING=============================");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "The statistics collection is disabled. As developer I need the");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "statistics from bStats.org. The statistics is 100% anonymous.");
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.GOLD + "[MobHunting]" + ChatColor.RED + "https://bstats.org/plugin/bukkit/MobHunting");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "Please enable this in /plugins/bStats/config.yml and get rid of this");
			Bukkit.getConsoleSender().sendMessage(
					ChatColor.GOLD + "[MobHunting]" + ChatColor.RED + "message. Loading will continue in 15 sec.");
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ "=========================================================");
			long now = System.currentTimeMillis();
			while (System.currentTimeMillis() < now + 15000L) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}

		mWorldGroupManager = new WorldGroup(this);
		mWorldGroupManager.load();

		mRewardManager = new RewardManager(this);
		if (mRewardManager.getEconomy() == null)
			return;

		mGrindingManager = new GrindingManager(this);

		if (mConfig.databaseType.equalsIgnoreCase("mysql"))
			mStore = new MySQLDataStore(this);
		else
			mStore = new SQLiteDataStore(this);

		try {
			mStore.initialize();
		} catch (DataStoreException e) {
			e.printStackTrace();
			try {
				mStore.shutdown();
			} catch (DataStoreException e1) {
				e1.printStackTrace();
			}
			setEnabled(false);
			return;
		}

		mSpigetUpdater = new SpigetUpdater(this);
		mSpigetUpdater.setCurrentJarFile(this.getFile().getName());

		mStoreManager = new DataStoreManager(this, mStore);

		mPlayerSettingsManager = new PlayerSettingsManager(this);

		mCompatibilityManager = new CompatibilityManager(this);

		// Handle compatibility stuff
		mCompatibilityManager.registerPlugin(EssentialsCompat.class, CompatPlugin.Essentials);
		mCompatibilityManager.registerPlugin(BagOfGoldCompat.class, CompatPlugin.BagOfGold);
		mCompatibilityManager.registerPlugin(GringottsCompat.class, CompatPlugin.Gringotts);

		// Protection plugins
		mCompatibilityManager.registerPlugin(WorldEditCompat.class, CompatPlugin.WorldEdit);
		mCompatibilityManager.registerPlugin(WorldGuardCompat.class, CompatPlugin.WorldGuard);
		mCompatibilityManager.registerPlugin(HologramsCompat.class, CompatPlugin.Holograms);
		mCompatibilityManager.registerPlugin(HolographicDisplaysCompat.class, CompatPlugin.HolographicDisplays);
		mCompatibilityManager.registerPlugin(CMICompat.class, CompatPlugin.CMI);
		mCompatibilityManager.registerPlugin(FactionsHelperCompat.class, CompatPlugin.Factions);
		mCompatibilityManager.registerPlugin(TownyCompat.class, CompatPlugin.Towny);
		mCompatibilityManager.registerPlugin(ResidenceCompat.class, CompatPlugin.Residence);
		mCompatibilityManager.registerPlugin(PreciousStonesCompat.class, CompatPlugin.PreciousStones);

		// Other plugins
		mCompatibilityManager.registerPlugin(McMMOCompat.class, CompatPlugin.mcMMO);
		mCompatibilityManager.registerPlugin(ProtocolLibCompat.class, CompatPlugin.ProtocolLib);
		mCompatibilityManager.registerPlugin(MyPetCompat.class, CompatPlugin.MyPet);
		mCompatibilityManager.registerPlugin(BossShopCompat.class, CompatPlugin.BossShop);

		// Minigame plugins
		mCompatibilityManager.registerPlugin(MinigamesCompat.class, CompatPlugin.Minigames);
		mCompatibilityManager.registerPlugin(MinigamesLibCompat.class, CompatPlugin.MinigamesLib);
		mCompatibilityManager.registerPlugin(MobArenaCompat.class, CompatPlugin.MobArena);
		mCompatibilityManager.registerPlugin(PVPArenaCompat.class, CompatPlugin.PVPArena);
		mCompatibilityManager.registerPlugin(BattleArenaCompat.class, CompatPlugin.BattleArena);

		// Disguise and Vanish plugins
		mCompatibilityManager.registerPlugin(LibsDisguisesCompat.class, CompatPlugin.LibsDisguises);
		mCompatibilityManager.registerPlugin(DisguiseCraftCompat.class, CompatPlugin.DisguiseCraft);
		mCompatibilityManager.registerPlugin(IDisguiseCompat.class, CompatPlugin.iDisguise);
		mCompatibilityManager.registerPlugin(VanishNoPacketCompat.class, CompatPlugin.VanishNoPacket);

		// Plugins used for presentation information in the BossBar, ActionBar,
		// Title or Subtitle
		mCompatibilityManager.registerPlugin(BossBarAPICompat.class, CompatPlugin.BossBarApi);
		mCompatibilityManager.registerPlugin(TitleAPICompat.class, CompatPlugin.TitleAPI);
		mCompatibilityManager.registerPlugin(BarAPICompat.class, CompatPlugin.BarApi);
		mCompatibilityManager.registerPlugin(TitleManagerCompat.class, CompatPlugin.TitleManager);
		mCompatibilityManager.registerPlugin(ActionbarCompat.class, CompatPlugin.Actionbar);
		mCompatibilityManager.registerPlugin(ActionBarAPICompat.class, CompatPlugin.ActionBarApi);
		mCompatibilityManager.registerPlugin(ActionAnnouncerCompat.class, CompatPlugin.ActionAnnouncer);
		mCompatibilityManager.registerPlugin(PlaceholderAPICompat.class, CompatPlugin.PlaceholderAPI);

		// Plugins where the reward is a multiplier
		mCompatibilityManager.registerPlugin(StackMobCompat.class, CompatPlugin.StackMob);
		mCompatibilityManager.registerPlugin(MobStackerCompat.class, CompatPlugin.MobStacker);
		mCompatibilityManager.registerPlugin(ConquestiaMobsCompat.class, CompatPlugin.ConquestiaMobs);
		mCompatibilityManager.registerPlugin(LorinthsRpgMobsCompat.class, CompatPlugin.LorinthsRpgMobs);
		mCompatibilityManager.registerPlugin(EliteMobsCompat.class, CompatPlugin.EliteMobs);

		// ExtendedMob Plugins where special mobs are created
		mCompatibilityManager.registerPlugin(MythicMobsCompat.class, CompatPlugin.MythicMobs);
		mCompatibilityManager.registerPlugin(TARDISWeepingAngelsCompat.class, CompatPlugin.TARDISWeepingAngels);
		mCompatibilityManager.registerPlugin(CustomMobsCompat.class, CompatPlugin.CustomMobs);
		mCompatibilityManager.registerPlugin(MysteriousHalloweenCompat.class, CompatPlugin.MysteriousHalloween);
		mCompatibilityManager.registerPlugin(CitizensCompat.class, CompatPlugin.Citizens);
		mCompatibilityManager.registerPlugin(SmartGiantsCompat.class, CompatPlugin.SmartGiants);
		mCompatibilityManager.registerPlugin(InfernalMobsCompat.class, CompatPlugin.InfernalMobs);
		mCompatibilityManager.registerPlugin(HerobrineCompat.class, CompatPlugin.Herobrine);

		mCompatibilityManager.registerPlugin(ExtraHardModeCompat.class, CompatPlugin.ExtraHardMode);
		mCompatibilityManager.registerPlugin(CrackShotCompat.class, CompatPlugin.CrackShot);

		mExtendedMobManager = new ExtendedMobManager(this);

		// Register commands
		mCommandDispatcher = new CommandDispatcher(this, "mobhunt",
				getMessages().getString("mobhunting.command.base.description") + getDescription().getVersion());
		getCommand("mobhunt").setExecutor(mCommandDispatcher);
		getCommand("mobhunt").setTabCompleter(mCommandDispatcher);
		mCommandDispatcher.registerCommand(new AchievementsCommand(this));
		mCommandDispatcher.registerCommand(new BlacklistAreaCommand(this));
		mCommandDispatcher.registerCommand(new CheckGrindingCommand(this));
		mCommandDispatcher.registerCommand(new ClearGrindingCommand(this));
		mCommandDispatcher.registerCommand(new DatabaseCommand(this));
		mCommandDispatcher.registerCommand(new HeadCommand(this));
		mCommandDispatcher.registerCommand(new LeaderboardCommand(this));
		if (HolographicDisplaysCompat.isSupported() || HologramsCompat.isSupported() || CMICompat.isSupported())
			mCommandDispatcher.registerCommand(new HologramCommand(this));
		mCommandDispatcher.registerCommand(new LearnCommand(this));
		mCommandDispatcher.registerCommand(new MuteCommand(this));
		// moved to CitizensCompat
		// mCommandDispatcher.registerCommand(new NpcCommand(this));
		mCommandDispatcher.registerCommand(new ReloadCommand(this));
		if (WorldGuardCompat.isSupported())
			mCommandDispatcher.registerCommand(new RegionCommand(this));
		if (WorldEditCompat.isSupported())
			mCommandDispatcher.registerCommand(new SelectCommand(this));
		mCommandDispatcher.registerCommand(new TopCommand(this));
		mCommandDispatcher.registerCommand(new WhitelistAreaCommand(this));
		mCommandDispatcher.registerCommand(new UpdateCommand(this));
		mCommandDispatcher.registerCommand(new VersionCommand(this));
		mCommandDispatcher.registerCommand(new DebugCommand(this));
		if (mConfig.enablePlayerBounties)
			mCommandDispatcher.registerCommand(new BountyCommand(this));
		mCommandDispatcher.registerCommand(new HappyHourCommand(this));
		mCommandDispatcher.registerCommand(new MoneyCommand(this));

		mLeaderboardManager = new LeaderboardManager(this);

		mAchievementManager = new AchievementManager(this);

		mMobHuntingManager = new MobHuntingManager(this);
		if (mConfig.enableFishingRewards)
			mFishingManager = new FishingManager(this);

		if (mConfig.enablePlayerBounties)
			mBountyManager = new BountyManager(this);

		// Check for new MobHuntig updates using Spiget.org
		mSpigetUpdater.hourlyUpdateCheck(getServer().getConsoleSender(), mConfig.updateCheck, false);

		if (!Misc.isGlowstoneServer()) {
			mMetricsManager = new MetricsManager(this);
			mMetricsManager.start();
		}

		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				instance.getMessages().injectMissingMobNamesToLangFiles();
			}
		}, 20 * 5);

		// Handle online players when server admin do a /reload or /mh reload
		if (Misc.getOnlinePlayersAmount() > 0) {
			getMessages().debug("Reloading %s player settings from the database", Misc.getOnlinePlayersAmount());
			for (Player player : Misc.getOnlinePlayers()) {
				mPlayerSettingsManager.load(player);
				mAchievementManager.load(player);
				if (mConfig.enablePlayerBounties)
					mBountyManager.load(player);
				mMobHuntingManager.setHuntEnabled(player, true);
			}
		}

		getMessages().debug("Updating advancements");
		if (!getConfigManager().disableMobHuntingAdvancements && Misc.isMC112OrNewer()) {
			mAdvancementManager = new AdvancementManager(this);
			mAdvancementManager.getAdvancementsFromAchivements();

			Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					getMessages().debug("Reloading advancements");
					Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "minecraft:reload");
				}
			}, 20 * 15);

		}

		if (!Misc.isMC113OrNewer())
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting]" + ChatColor.RED
					+ " version +6.0.0 is only for Minecraft 1.13! You should downgrade to 5.x");

		// for (int i = 0; i < 5; i++)
		// getMessages().debug("Random uuid = %s", UUID.randomUUID());

		mInitialized = true;

	}

	@Override
	public void onDisable() {
		getMessages().debug("Disabling MobHunting initiated");

		if (!mInitialized)
			return;

		getMessages().debug("Shutdown LeaderBoardManager");
		mLeaderboardManager.shutdown();
		getMessages().debug("Shutdown AreaManager");
		mGrindingManager.saveData();
		if (PlaceholderAPICompat.isSupported()) {
			getMessages().debug("Shutdown PlaceHolderManager");
			PlaceholderAPICompat.shutdown();
		}
		getMobHuntingManager().getHuntingModifiers().clear();
		if (mConfig.enableFishingRewards)
			getFishingManager().getFishingModifiers().clear();

		try {
			getMessages().debug("Shutdown StoreManager");
			mStoreManager.shutdown();
			getMessages().debug("Shutdown Store");
			mStore.shutdown();
		} catch (DataStoreException e) {
			e.printStackTrace();
		}
		getMessages().debug("Shutdown CitizensCompat");
		CitizensCompat.shutdown();
		getMessages().debug("Shutdown WorldGroupManager");
		mWorldGroupManager.save();
		Bukkit.getConsoleSender()
				.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET + "MobHunting disabled.");
	}

	private boolean isbStatsEnabled() {
		File bStatsFolder = new File(instance.getDataFolder().getParentFile(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		return config.getBoolean("enabled", true);
	}

	// ************************************************************************************
	// Managers and handlers
	// ************************************************************************************
	public static MobHunting getInstance() {
		return instance;
	}

	public static MobHunting getAPI() {
		return instance;
	}

	public ConfigManager getConfigManager() {
		return mConfig;
	}

	/**
	 * Get the MessagesManager
	 * 
	 * @return
	 */
	public Messages getMessages() {
		return mMessages;
	}

	/**
	 * setMessages
	 * 
	 * @param messages
	 */
	public void setMessages(Messages messages) {
		mMessages = messages;
	}

	/**
	 * Gets the MobHuntingHandler
	 * 
	 * @return MobHuntingManager
	 */
	public MobHuntingManager getMobHuntingManager() {
		return mMobHuntingManager;
	}

	/**
	 * Get all Achievements for all players.
	 * 
	 * @return
	 */
	public AchievementManager getAchievementManager() {
		return mAchievementManager;
	}

	/**
	 * Gets the Store Manager
	 * 
	 * @return
	 */
	public IDataStore getStoreManager() {
		return mStore;
	}

	/**
	 * Gets the Database Store Manager
	 * 
	 * @return
	 */
	public DataStoreManager getDataStoreManager() {
		return mStoreManager;
	}

	/**
	 * Gets the LeaderboardManager
	 * 
	 * @return
	 */
	public LeaderboardManager getLeaderboardManager() {
		return mLeaderboardManager;
	}

	/**
	 * Get the BountyManager
	 * 
	 * @return
	 */
	public BountyManager getBountyManager() {
		return mBountyManager;
	}

	/**
	 * Get the AreaManager
	 * 
	 * @return
	 */
	public GrindingManager getGrindingManager() {
		return mGrindingManager;
	}

	/**
	 * Get all WorldGroups and their worlds
	 * 
	 * @return
	 */
	public WorldGroup getWorldGroupManager() {
		return mWorldGroupManager;
	}

	/**
	 * Get the PlayerSettingsManager
	 * 
	 * @return
	 */
	public PlayerSettingsManager getPlayerSettingsManager() {
		return mPlayerSettingsManager;
	}

	/**
	 * Get the RewardManager
	 * 
	 * @return
	 */
	public RewardManager getRewardManager() {
		return mRewardManager;
	}

	/**
	 * Get the ParticleManager
	 * 
	 * @return
	 */
	public ParticleManager getParticleManager() {
		return mParticleManager;
	}

	/**
	 * Get the MobManager
	 * 
	 * @return
	 */
	public ExtendedMobManager getExtendedMobManager() {
		return mExtendedMobManager;
	}

	/**
	 * Get the FishingManager
	 * 
	 * @return
	 */
	public FishingManager getFishingManager() {
		return mFishingManager;
	}

	/**
	 * Get the AdvancementManager
	 * 
	 * @return
	 */
	public AdvancementManager getAdvancementManager() {
		return mAdvancementManager;
	}

	public CommandDispatcher getCommandDispatcher() {
		return mCommandDispatcher;
	}

	public CompatibilityManager getCompatibilityManager() {
		return mCompatibilityManager;
	}

	public SpigetUpdater getSpigetUpdater() {
		return mSpigetUpdater;
	}

}
