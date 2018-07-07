package one.lindegaard.MobHunting;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;

import one.lindegaard.MobHunting.compatibility.ActionAnnouncerCompat;
import one.lindegaard.MobHunting.compatibility.ActionBarAPICompat;
import one.lindegaard.MobHunting.compatibility.ActionbarCompat;
import one.lindegaard.MobHunting.compatibility.BarAPICompat;
import one.lindegaard.MobHunting.compatibility.BattleArenaCompat;
import one.lindegaard.MobHunting.compatibility.BossBarAPICompat;
import one.lindegaard.MobHunting.compatibility.CMICompat;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.ConquestiaMobsCompat;
import one.lindegaard.MobHunting.compatibility.CrackShotCompat;
import one.lindegaard.MobHunting.compatibility.CustomMobsCompat;
import one.lindegaard.MobHunting.compatibility.DisguiseCraftCompat;
import one.lindegaard.MobHunting.compatibility.EliteMobsCompat;
import one.lindegaard.MobHunting.compatibility.EssentialsCompat;
import one.lindegaard.MobHunting.compatibility.ExtraHardModeCompat;
import one.lindegaard.MobHunting.compatibility.FactionsHelperCompat;
import one.lindegaard.MobHunting.compatibility.FactionsHelperCompat.FactionsVersion;
import one.lindegaard.MobHunting.compatibility.GringottsCompat;
import one.lindegaard.MobHunting.compatibility.HerobrineCompat;
import one.lindegaard.MobHunting.compatibility.HologramsCompat;
import one.lindegaard.MobHunting.compatibility.HolographicDisplaysCompat;
import one.lindegaard.MobHunting.compatibility.IDisguiseCompat;
import one.lindegaard.MobHunting.compatibility.InfernalMobsCompat;
import one.lindegaard.MobHunting.compatibility.LibsDisguisesCompat;
import one.lindegaard.MobHunting.compatibility.LorinthsRpgMobsCompat;
import one.lindegaard.MobHunting.compatibility.MinigamesCompat;
import one.lindegaard.MobHunting.compatibility.MinigamesLibCompat;
import one.lindegaard.MobHunting.compatibility.MobArenaCompat;
import one.lindegaard.MobHunting.compatibility.MobStackerCompat;
import one.lindegaard.MobHunting.compatibility.MyPetCompat;
import one.lindegaard.MobHunting.compatibility.MysteriousHalloweenCompat;
import one.lindegaard.MobHunting.compatibility.MythicMobsCompat;
import one.lindegaard.MobHunting.compatibility.PVPArenaCompat;
import one.lindegaard.MobHunting.compatibility.PreciousStonesCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ResidenceCompat;
import one.lindegaard.MobHunting.compatibility.SmartGiantsCompat;
import one.lindegaard.MobHunting.compatibility.StackMobCompat;
import one.lindegaard.MobHunting.compatibility.TARDISWeepingAngelsCompat;
import one.lindegaard.MobHunting.compatibility.TitleAPICompat;
import one.lindegaard.MobHunting.compatibility.TitleManagerCompat;
import one.lindegaard.MobHunting.compatibility.TownyCompat;
import one.lindegaard.MobHunting.compatibility.VanishNoPacketCompat;
import one.lindegaard.MobHunting.compatibility.WorldEditCompat;
import one.lindegaard.MobHunting.compatibility.WorldGuardCompat;
import org.bstats.bukkit.Metrics;

public class MetricsManager {

	private MobHunting plugin;
	private boolean started = false;

	private Metrics bStatsMetrics;

	public MetricsManager(MobHunting plugin) {
		this.plugin = plugin;
	}

	public void start() {
		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
			public void run() {
				try {
					// make a URL to MCStats.org
					URL url = new URL("https://www.bstats.org/");
					if (!started && HttpTools.isHomePageReachable(url)) {
						startBStatsMetrics();
						plugin.getMessages().debug("Metrics reporting to Https://bstats.org has started.");
						started= true;
					} else {
						plugin.getMessages().debug("https://bstats.org/ seems to be down");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}, 100, 72000);
	}

	private void startBStatsMetrics() {
		bStatsMetrics = new Metrics(plugin);

		bStatsMetrics.addCustomChart(
				new Metrics.SimplePie("database_used_for_mobhunting", () -> plugin.getConfigManager().databaseType));
		bStatsMetrics.addCustomChart(new Metrics.SimplePie("language", () -> plugin.getConfigManager().language));

		bStatsMetrics.addCustomChart(
				new Metrics.SimplePie("economy_plugin", () -> plugin.getRewardManager().getEconomy().getName()));

		bStatsMetrics.addCustomChart(
				new Metrics.AdvancedPie("protection_plugin_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("WorldGuard", Integer.valueOf(WorldGuardCompat.isSupported() ? 1 : 0));
						valueMap.put("Factions", Integer
								.valueOf(FactionsHelperCompat.factionsVersion == FactionsVersion.FACTIONS ? 1 : 0));
						valueMap.put("FactionsUUID", Integer.valueOf(
								FactionsHelperCompat.factionsVersion == FactionsVersion.FACTIONS_UUID ? 1 : 0));
						valueMap.put("Towny", Integer.valueOf(TownyCompat.isSupported() ? 1 : 0));
						valueMap.put("Residence", Integer.valueOf(ResidenceCompat.isSupported() ? 1 : 0));
						valueMap.put("PreciousStones", Integer.valueOf(PreciousStonesCompat.isSupported() ? 1 : 0));
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new Metrics.AdvancedPie("minigame_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("MobArena", MobArenaCompat.isSupported() ? 1 : 0);
						valueMap.put("Minigames", MinigamesCompat.isSupported() ? 1 : 0);
						valueMap.put("MinigamesLib", MinigamesLibCompat.isSupported() ? 1 : 0);
						valueMap.put("PVPArena", PVPArenaCompat.isSupported() ? 1 : 0);
						valueMap.put("BattleArena", BattleArenaCompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics.addCustomChart(
				new Metrics.AdvancedPie("disguise_plugin_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("pgDev.bukkit.DisguiseCraft.disguise.DisguiseType");
							valueMap.put("DisguiseCraft", DisguiseCraftCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("de.robingrether.idisguise.disguise.DisguiseType");
							valueMap.put("iDisguise", IDisguiseCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("me.libraryaddict.disguise.disguisetypes.DisguiseType");
							valueMap.put("LibsDisguises", LibsDisguisesCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						valueMap.put("VanishNoPacket", VanishNoPacketCompat.isSupported() ? 1 : 0);
						valueMap.put("Essentials", EssentialsCompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new Metrics.AdvancedPie("other_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("Citizens", CitizensCompat.isSupported() ? 1 : 0);
						valueMap.put("Gringotts", GringottsCompat.isSupported() ? 1 : 0);
						valueMap.put("MyPet", MyPetCompat.isSupported() ? 1 : 0);
						valueMap.put("WorldEdit", WorldEditCompat.isSupported() ? 1 : 0);
						valueMap.put("ProtocolLib", ProtocolLibCompat.isSupported() ? 1 : 0);
						valueMap.put("ExtraHardMode", ExtraHardModeCompat.isSupported() ? 1 : 0);
						valueMap.put("CrackShot", CrackShotCompat.isSupported() ? 1 : 0);
						valueMap.put("CMI", CMICompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics.addCustomChart(new Metrics.AdvancedPie("special_mobs", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("MythicMobs", MythicMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("TARDISWeepingAngels", TARDISWeepingAngelsCompat.isSupported() ? 1 : 0);
				valueMap.put("MobStacker", MobStackerCompat.isSupported() ? 1 : 0);
				valueMap.put("CustomMobs", CustomMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("ConquestiaMobs", ConquestiaMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("LorinthsRpgMobs", LorinthsRpgMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("StackMob", StackMobCompat.isSupported() ? 1 : 0);
				valueMap.put("MysteriousHalloween", MysteriousHalloweenCompat.isSupported() ? 1 : 0);
				valueMap.put("SmartGiants", SmartGiantsCompat.isSupported() ? 1 : 0);
				valueMap.put("InfernalMobs", InfernalMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("Herobrine", HerobrineCompat.isSupported() ? 1 : 0);
				valueMap.put("EliteMobs", EliteMobsCompat.isSupported() ? 1 : 0);
				return valueMap;
			}

		}));

		bStatsMetrics.addCustomChart(new Metrics.AdvancedPie("titlemanagers", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("BossBarAPI", BossBarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("TitleAPI", TitleAPICompat.isSupported() ? 1 : 0);
				valueMap.put("BarAPI", BarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("TitleManager", TitleManagerCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBar", ActionbarCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBarAPI", ActionBarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("ActionAnnouncer", ActionAnnouncerCompat.isSupported() ? 1 : 0);
				valueMap.put("Holograms", HologramsCompat.isSupported() ? 1 : 0);
				valueMap.put("Holographic Display", HolographicDisplaysCompat.isSupported() ? 1 : 0);
				valueMap.put("CMIHolograms", CMICompat.isSupported() ? 1 : 0);
				return valueMap;
			}

		}));

		bStatsMetrics.addCustomChart(new Metrics.AdvancedPie("mobhunting_usage", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("Leaderboards", plugin.getLeaderboardManager().getWorldLeaderBoards().size());
				valueMap.put("Holographic Leaderboards",
						plugin.getLeaderboardManager().getHologramManager().getHolograms().size());
				valueMap.put("MasterMobHunters", CitizensCompat.getMasterMobHunterManager().getAll().size());
				valueMap.put("PlayerBounties", plugin.getConfigManager().enablePlayerBounties
						? plugin.getBountyManager().getAllBounties().size() : 0);
				return valueMap;
			}

		}));
	}

}
