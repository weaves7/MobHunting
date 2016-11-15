package one.lindegaard.MobHunting.storage;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.bounty.Bounty;
import one.lindegaard.MobHunting.bounty.BountyStatus;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.CustomMobsCompat;
import one.lindegaard.MobHunting.compatibility.MythicMobsCompat;
import one.lindegaard.MobHunting.compatibility.TARDISWeepingAngelsCompat;
import one.lindegaard.MobHunting.mobs.PluginManager;
import one.lindegaard.MobHunting.mobs.MobPlugin;
import one.lindegaard.MobHunting.mobs.ExtendedMob;
import one.lindegaard.MobHunting.mobs.MinecraftMob;

public abstract class DatabaseDataStore implements IDataStore {
	/**
	 * Connection to the Database
	 */
	protected Connection mConnection;

	/**
	 * Args: player id
	 */
	protected PreparedStatement mSavePlayerStats;

	/**
	 * Args: player uuid
	 */
	protected PreparedStatement[] mGetPlayerData;

	/**
	 * Args: player name
	 */
	protected PreparedStatement mGetPlayerUUID;

	/**
	 * Args: player name, player uuid
	 */
	protected PreparedStatement mUpdatePlayerName;

	/**
	 * Args: player uuid
	 */
	protected PreparedStatement mUpdatePlayerSettings;

	/**
	 * Args: player uuid
	 */
	protected PreparedStatement mInsertPlayerData;

	/**
	 * Args: Player OfflinePLayer
	 */
	protected PreparedStatement mGetBounties;

	/**
	 * Args: Bounty
	 */
	protected PreparedStatement mInsertBounty;

	/**
	 * Args: Bounty
	 */
	protected PreparedStatement mUpdateBounty;

	/**
	 * Args: Bounty ID
	 */
	protected PreparedStatement mDeleteBounty;

	/**
	 * Args: player player_id
	 */
	protected PreparedStatement mGetPlayerByPlayerId;

	/**
	 * Establish initial connection to Database
	 */
	protected abstract Connection setupConnection() throws SQLException, DataStoreException;

	/**
	 * Setup / Create database version 2 tables for MobHunting
	 */
	protected abstract void setupV2Tables(Connection connection) throws SQLException;

	/**
	 * Setup / Create database version 3 tables for MobHunting
	 */
	protected abstract void setupV3Tables(Connection connection) throws SQLException;

	/**
	 * Setup / Setup Triggers for V3 Database Layout
	 */
	protected abstract void setupTriggerV3(Connection connection) throws SQLException;

	/**
	 * Open a connection to the Database and prepare a statement for executing.
	 * 
	 * @param connection
	 * @param preparedConnectionType
	 * @throws SQLException
	 */
	protected abstract void openPreparedStatements(Connection connection, PreparedConnectionType preparedConnectionType)
			throws SQLException;

	public enum PreparedConnectionType {
		SAVE_PLAYER_STATS, LOAD_ARCHIEVEMENTS, SAVE_ACHIEVEMENTS, UPDATE_PLAYER_NAME, GET1PLAYER, GET2PLAYERS, GET5PLAYERS, GET10PLAYERS, GET_PLAYER_UUID, INSERT_PLAYER_DATA, UPDATE_PLAYER_SETTINGS, GET_BOUNTIES, INSERT_BOUNTY, UPDATE_BOUNTY, DELETE_BOUNTY, GET_PLAYER_BY_PLAYER_ID, LOAD_MOBS, INSERT_MOBS, UPDATE_MOBS
	};

	/**
	 * Initialize the connection. Must be called after Opening of initial
	 * connection. Open Prepared statements for batch processing large
	 * selections of players. Batches will be performed in batches of 10,5,2,1
	 */
	@Override
	public void initialize() throws DataStoreException {
		try {

			mConnection = setupConnection();
			mConnection.setAutoCommit(false);

			// Find current database version
			if (MobHunting.getConfigManager().databaseVersion == 0) {
				Statement statement = mConnection.createStatement();
				try {
					ResultSet rs = statement.executeQuery("SELECT MOB_ID FROM mh_Mobs LIMIT 0");
					rs.close();
					// The TABLE mh_Mobs created for V3 and does only contain
					// data after migration
					MobHunting.getConfigManager().databaseVersion = 3;
					MobHunting.getConfigManager().saveConfig();
				} catch (SQLException e1) {
					try {
						ResultSet rs = statement.executeQuery("SELECT UUID from mh_Players LIMIT 0");
						rs.close();
						// Player UUID is migrated in V2
						MobHunting.getConfigManager().databaseVersion = 2;
						MobHunting.getConfigManager().saveConfig();
					} catch (SQLException e2) {
						// database if from before Minecraft 1.7.9 R1 (No UUID)
						// = V1
						try {
							ResultSet rs = statement.executeQuery("SELECT PLAYER_ID from mh_Players LIMIT 0");
							rs.close();
							MobHunting.getConfigManager().databaseVersion = 1;
							MobHunting.getConfigManager().saveConfig();
						} catch (SQLException e3) {
							// DATABASE DOES NOT EXIST AT ALL, CREATE NEW EMPTY
							// V3 DATABASE
							MobHunting.getConfigManager().databaseVersion = 3;
							MobHunting.getConfigManager().saveConfig();
						}
					}
				}
				statement.close();
			}

			switch (MobHunting.getConfigManager().databaseVersion) {
			case 1:
				// create new V2 tables and migrate data.
				Bukkit.getLogger().info("[MobHunting] Database version " + MobHunting.getConfigManager().databaseVersion
						+ " detected. Migrating to V2");
				setupV2Tables(mConnection);
				MobHunting.getConfigManager().databaseVersion = 2;
				MobHunting.getConfigManager().saveConfig();
			case 2:
				// Create new V3 tables and migrate data;
				Bukkit.getLogger().info("[MobHunting] Database version " + MobHunting.getConfigManager().databaseVersion
						+ " detected. Migrating to V3");
				// setupV3Tables(mConnection);
				migrateDatabaseLayoutFromV2toV3(mConnection);
				setupTriggerV3(mConnection);
				MobHunting.getConfigManager().databaseVersion = 3;
				MobHunting.getConfigManager().saveConfig();
				break;
			case 3:
				Bukkit.getLogger().info("[MobHunting] Database version " + MobHunting.getConfigManager().databaseVersion
						+ " detected.");
				// DATABASE IS UPTODATE or NOT created => create new database
				setupV3Tables(mConnection);
				setupTriggerV3(mConnection);
				break;
			default: // not needed
				Bukkit.getLogger().info("[MobHunting] Database version " + MobHunting.getConfigManager().databaseVersion
						+ " detected.");
				setupV3Tables(mConnection);
				migrateDatabaseLayoutFromV2toV3(mConnection);
				setupTriggerV3(mConnection);
				MobHunting.getConfigManager().databaseVersion = 3;
				MobHunting.getConfigManager().saveConfig();
			}

			mGetPlayerData = new PreparedStatement[4];

			// Enable FOREIGN KEY for Sqlite database
			if (!MobHunting.getConfigManager().databaseType.equalsIgnoreCase("MySQL")) {
				Statement statement = mConnection.createStatement();
				statement.execute("PRAGMA foreign_keys = ON");
				statement.close();
			}

		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * Open connections for batch processing.
	 * 
	 * @throws SQLException
	 */
	protected void openPreparedGetPlayerStatements() throws SQLException {
		openPreparedStatements(mConnection, PreparedConnectionType.GET1PLAYER);
		openPreparedStatements(mConnection, PreparedConnectionType.GET2PLAYERS);
		openPreparedStatements(mConnection, PreparedConnectionType.GET5PLAYERS);
		openPreparedStatements(mConnection, PreparedConnectionType.GET10PLAYERS);
	}

	/**
	 * Close all opened connections for batch processing.
	 * 
	 * @throws SQLException
	 */
	protected void closePreparedGetPlayerStatements() throws SQLException {
		mGetPlayerData[0].close();
		mGetPlayerData[1].close();
		mGetPlayerData[2].close();
		mGetPlayerData[3].close();
	}

	/**
	 * Rollback of last transaction on Database.
	 * 
	 * @throws DataStoreException
	 */
	protected void rollback() throws DataStoreException {

		try {
			mConnection.rollback();
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * Shutdown: Commit and close database connection completely.
	 */
	@Override
	public void shutdown() throws DataStoreException {
		try {
			if (mConnection != null) {
				mConnection.commit();
				int n = 0;
				do {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					n++;
				} while (MobHunting.getDataStoreManager().isRunning() && n < 40);
				System.out.println("[MobHunting] Closing database connection.");
				mConnection.close();
			}
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * databaseFixLeaderboard - tries to fix inconsistens in the database. Will
	 * later be used for cleaning the database; deleting old data or so. This is
	 * not implemented yet.
	 */
	@Override
	public void databaseFixLeaderboard() throws SQLException {
		Statement statement = mConnection.createStatement();
		try {
			Messages.debug("Beginning cleaning of database");
			int result;
			result = statement.executeUpdate("DELETE FROM mh_Achievements WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_Achievements.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_Achievements", result);
			result = statement.executeUpdate("DELETE FROM mh_AllTime WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_AllTime.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_AllTime", result);
			result = statement.executeUpdate("DELETE FROM mh_Daily WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_Daily.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_Daily", result);
			result = statement.executeUpdate("DELETE FROM mh_Monthly WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_Monthly.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_Monthly", result);
			result = statement.executeUpdate("DELETE FROM mh_Weekly WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_Weekly.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_Weekly", result);
			result = statement.executeUpdate("DELETE FROM mh_Yearly WHERE PLAYER_ID NOT IN "
					+ "(SELECT PLAYER_ID FROM mh_Players " + "where mh_Yearly.PLAYER_ID=mh_Players.PLAYER_ID);");
			Messages.debug("%s rows was deleted from Mh_Yearly", result);
			statement.close();
			mConnection.commit();
			Bukkit.getLogger().info("MobHunting Database was cleaned");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ******************************************************************
	// V2 To V3 Database migration
	// ******************************************************************

	// Migrate from DatabaseLayout from V2 to V3
	public void migrateDatabaseLayoutFromV2toV3(Connection connection) throws SQLException {
		Bukkit.getLogger().info("[MobHunting] DATAMIGRATION FROM DATABASE LAYOUT V2 TO V3.");

		// rename old tables
		Statement create = connection.createStatement();
		// Check if tables are V3.
		try {
			ResultSet rs = create.executeQuery("SELECT MOB_ID from mh_Mobs LIMIT 0");
			rs.close();
		} catch (SQLException e) {
			// Tables are V2
			create.executeUpdate("ALTER TABLE mh_Daily RENAME TO mh_DailyV2");
			create.executeUpdate("ALTER TABLE mh_Weekly RENAME TO mh_WeeklyV2");
			create.executeUpdate("ALTER TABLE mh_Monthly RENAME TO mh_MonthlyV2");
			create.executeUpdate("ALTER TABLE mh_Yearly RENAME TO mh_YearlyV2");
			create.executeUpdate("ALTER TABLE mh_AllTime RENAME TO mh_AllTimeV2");
			create.executeUpdate("DROP TRIGGER IF EXISTS DailyInsert");
			create.executeUpdate("DROP TRIGGER IF EXISTS DailyUpdate");
		}
		create.close();

		// create new tables
		setupV3Tables(connection);

		// migrate data from old table3s to new tables.
		Statement statement = connection.createStatement();

		HashMap<String, Integer> mobs = new HashMap<>();
		try {
			ResultSet rs = statement.executeQuery("SELECT * FROM mh_Mobs");
			while (rs.next()) {
				mobs.put(rs.getString("MOBTYPE"), rs.getInt("MOB_ID"));
			}
			rs.close();
		} catch (Exception e) {
			Bukkit.getLogger().severe("Error while fetching Vanilla Mobs from mh_Mobs");
			e.printStackTrace();
		}

		try {
			int n = 0;
			ResultSet rs = statement.executeQuery(
					"SELECT * FROM mh_DailyV2" + " inner join mh_Players using (PLAYER_ID) WHERE NAME!=''");
			while (rs.next()) {
				int achievements = rs.getInt("ACHIEVEMENT_COUNT");
				Statement statement2 = connection.createStatement();
				for (MinecraftMob mob : MinecraftMob.values()) {
					String id = rs.getString("ID");
					int mob_id = mobs.get(mob.name());
					int player_id = rs.getInt("PLAYER_ID");
					int kills = rs.getInt(mob.name() + "_KILL");
					int assists = rs.getInt(mob.name() + "_ASSIST");
					if (kills > 0 || assists > 0 || achievements > 0) {
						String insertStr = "INSERT INTO mh_Daily VALUES (" + id + "," + mob_id + "," + player_id + ","
								+ achievements + "," + kills + "," + assists + ")";
						statement2.executeUpdate(insertStr);
						n++;
						achievements = 0;
					}
				}
				statement2.close();
				connection.commit();
			}
			rs.close();
			Bukkit.getLogger().info("[MobHunting] Migrated " + n + " records into mh_Daily.");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] Error while inserting data to new mh_Daily");
			e.printStackTrace();
		}

		try {
			int n = 0;
			ResultSet rs = statement.executeQuery(
					"SELECT * FROM mh_WeeklyV2" + " inner join mh_Players using (PLAYER_ID) WHERE NAME!=''");

			while (rs.next()) {
				int achievements = rs.getInt("ACHIEVEMENT_COUNT");
				Statement statement2 = connection.createStatement();
				for (MinecraftMob mob : MinecraftMob.values()) {
					String id = rs.getString("ID");
					int mob_id = mobs.get(mob.name());
					int player_id = rs.getInt("PLAYER_ID");
					int kills = rs.getInt(mob.name() + "_KILL");
					int assists = rs.getInt(mob.name() + "_ASSIST");
					if (kills > 0 || assists > 0 || achievements > 0) {
						String insertStr = "INSERT INTO mh_Weekly VALUES (" + id + "," + mob_id + "," + player_id + ","
								+ achievements + "," + kills + "," + assists + ")";
						statement2.executeUpdate(insertStr);
						n++;
						achievements = 0;
					}
				}
				statement2.close();
				connection.commit();
			}
			rs.close();
			Bukkit.getLogger().info("[MobHunting] Migrated " + n + " records into mh_Weekly.");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] Error while inserting data to new mh_Weekly");
			e.printStackTrace();
		}

		try {
			int n = 0;
			ResultSet rs = statement.executeQuery(
					"SELECT * FROM mh_MonthlyV2" + " inner join mh_Players using (PLAYER_ID) WHERE NAME!=''");

			while (rs.next()) {
				int achievements = rs.getInt("ACHIEVEMENT_COUNT");
				Statement statement2 = connection.createStatement();
				for (MinecraftMob mob : MinecraftMob.values()) {
					String id = rs.getString("ID");
					int mob_id = mobs.get(mob.name());
					int player_id = rs.getInt("PLAYER_ID");
					int kills = rs.getInt(mob.name() + "_KILL");
					int assists = rs.getInt(mob.name() + "_ASSIST");
					if (kills > 0 || assists > 0 || achievements > 0) {
						String insertStr = "INSERT INTO mh_Monthly VALUES (" + id + "," + mob_id + "," + player_id + ","
								+ achievements + "," + kills + "," + assists + ")";
						statement2.executeUpdate(insertStr);
						n++;
						achievements = 0;
					}
				}
				statement2.close();
				connection.commit();
			}
			rs.close();
			Bukkit.getLogger().info("[MobHunting] Migrated " + n + " records into mh_Monthly.");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] Error while inserting data to new mh_Monthly");
			e.printStackTrace();
		}

		try {
			int n = 0;
			ResultSet rs = statement.executeQuery(
					"SELECT * FROM mh_YearlyV2" + " inner join mh_Players using (PLAYER_ID) WHERE NAME!=''");

			while (rs.next()) {
				int achievements = rs.getInt("ACHIEVEMENT_COUNT");
				Statement statement2 = connection.createStatement();
				for (MinecraftMob mob : MinecraftMob.values()) {
					String id = rs.getString("ID");
					int mob_id = mobs.get(mob.name());
					int player_id = rs.getInt("PLAYER_ID");
					int kills = rs.getInt(mob.name() + "_KILL");
					int assists = rs.getInt(mob.name() + "_ASSIST");
					if (kills > 0 || assists > 0 || achievements > 0) {
						String insertStr = "INSERT INTO mh_Yearly VALUES (" + id + "," + mob_id + "," + player_id + ","
								+ achievements + "," + kills + "," + assists + ")";
						statement2.executeUpdate(insertStr);
						n++;
						achievements = 0;
					}
				}
				statement2.close();
				connection.commit();
			}
			rs.close();
			Bukkit.getLogger().info("[MobHunting] Migrated " + n + " records into mh_Yearly.");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] Error while inserting data to new mh_Yearly");
			e.printStackTrace();
		}

		try {
			int n = 0;
			ResultSet rs = statement.executeQuery(
					"SELECT * FROM mh_AllTimeV2" + " inner join mh_Players using (PLAYER_ID) WHERE NAME!=''");

			while (rs.next()) {
				int achievements = rs.getInt("ACHIEVEMENT_COUNT");
				Statement statement2 = connection.createStatement();
				for (MinecraftMob mob : MinecraftMob.values()) {
					int mob_id = mobs.get(mob.name());
					int player_id = rs.getInt("PLAYER_ID");
					int kills = rs.getInt(mob.name() + "_KILL");
					int assists = rs.getInt(mob.name() + "_ASSIST");
					if (kills > 0 || assists > 0 || achievements > 0) {
						String insertStr = "INSERT INTO mh_AllTime VALUES (" + mob_id + "," + player_id + ","
								+ achievements + "," + kills + "," + assists + ")";
						statement2.executeUpdate(insertStr);
						n++;
						achievements = 0;
					}
				}
				statement2.close();
				connection.commit();
			}
			rs.close();
			Bukkit.getLogger().info("[MobHunting] Migrated " + n + " records into mh_AllTime.");
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] Error while inserting data to new mh_AllTime");
			e.printStackTrace();
		}

		statement.close();
		connection.commit();
	}

	public int getMobIdFromExtendedMobType(String mob, int plugin_id) {
		int res = 0;
		try {
			Statement statement = mConnection.createStatement();
			ResultSet rs = statement.executeQuery(
					"SELECT MOB_ID from mh_Mobs WHERE PLUGIN_ID=" + plugin_id + " AND MOBTYPE='" + mob + "'");
			res = rs.getInt("MOB_ID");
			rs.close();
			statement.close();
		} catch (SQLException e) {
			Bukkit.getLogger().severe("[MobHunting] The ExtendedMobType " + mob + " was not found");
			e.printStackTrace();
		}
		return res;
	}

	@Override
	public void insertMissingVanillaMobs() {
		// Adding Vanilla Mobs to mh_Mobs
		int n = 0;
		try {
			Statement statement = mConnection.createStatement();
			for (MinecraftMob mob : MinecraftMob.values()) {
				int res = statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) " + "SELECT 0,'"
						+ mob.name() + "' " + "WHERE NOT EXISTS("
						+ "SELECT * FROM mh_Mobs WHERE PLUGIN_ID=0 AND MOBTYPE='" + mob.name() + "')");
				n = n + res;
			}
			if (n > 0)
				Bukkit.getLogger().info("[MobHunting] " + n + " Minecraft Vanilla Mobs was inserted to mh_Mobs");
			statement.close();
			mConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertMissingMythicMobs() {
		int n = 0;
		try {
			Statement statement = mConnection.createStatement();
			for (String mob : MythicMobsCompat.getMobRewardData().keySet())
				if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.MythicMobs) == 0) {

					statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (1,'" + mob + "')");
					n++;
				}
			if (n > 0)
				Bukkit.getLogger().info("[MobHunting] " + n + " MythicMobs was inserted to mh_Mobs");
			statement.close();
			mConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertMythicMobs(String mob) {
		if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.MythicMobs) == 0)
			try {
				Statement statement = mConnection.createStatement();
				statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (1,'" + mob + "')");
				Bukkit.getLogger().info("[MobHunting] MythicMobs MobType " + mob + " was inserted to mh_Mobs");
				statement.close();
				mConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void insertCitizensMobs() {
		int n = 0;
		try {
			Statement statement = mConnection.createStatement();
			for (String mob : CitizensCompat.getMobRewardData().keySet()) {
				if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.Citizens) == 0)

					statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (2,'" + mob + "')");
				n++;
			}
			if (n > 0)
				Bukkit.getLogger().info("[MobHunting] " + n + " Citizens NPC's was inserted to mh_Mobs");
			statement.close();
			mConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void insertCitizensMobs(String mob) {
		if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.Citizens) == 0)
			try {
				Statement statement = mConnection.createStatement();
				statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (2,'" + mob + "')");
				Bukkit.getLogger().info("[MobHunting] Citizens MobType " + mob + " was inserted to mh_Mobs");
				statement.close();
				mConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void insertTARDISWeepingAngelsMobs() {
		int n = 0;
		try {
			Statement statement = mConnection.createStatement();
			for (String mob : TARDISWeepingAngelsCompat.getMobRewardData().keySet())
				if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.TARDISWeepingAngels) == 0) {

					statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (3,'" + mob + "')");
					n++;
				}
			if (n > 0)
				Bukkit.getLogger().info("[MobHunting] " + n + " TARDISWeepingAngel mobs was inserted to mh_Mobs");
			statement.close();
			mConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertTARDISWeepingAngelsMobs(String mob) {
		if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.TARDISWeepingAngels) == 0)
			try {
				Statement statement = mConnection.createStatement();
				statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (3,'" + mob + "')");
				Bukkit.getLogger().info("[MobHunting] TARDISWeepingAngel MobType " + mob + " was inserted to mh_Mobs");
				statement.close();
				mConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void insertCustomMobs() {
		int n = 0;
		try {
			Statement statement = mConnection.createStatement();
			for (String mob : CustomMobsCompat.getMobRewardData().keySet())
				if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.CustomMobs) == 0) {

					statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (4,'" + mob + "')");
					n++;
				}
			if (n > 0)
				Bukkit.getLogger().info("[MobHunting] " + n + " CustomMobs was inserted to mh_Mobs");
			statement.close();
			mConnection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertCustomMobs(String mob) {
		if (MobHunting.getExtendedMobManager().getMobIdFromMobType(mob, MobPlugin.CustomMobs) == 0)
			try {
				Statement statement = mConnection.createStatement();
				statement.executeUpdate("INSERT INTO mh_Mobs (PLUGIN_ID, MOBTYPE) VALUES (4,'" + mob + "')");
				Bukkit.getLogger().info("[MobHunting] CustomMobs MobType " + mob + " was inserted to mh_Mobs");
				statement.close();
				mConnection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	}

	// ******************************************************************
	// Bounties
	// ******************************************************************
	@Override
	public Set<Bounty> loadBounties(OfflinePlayer offlinePlayer) throws DataStoreException {
		Set<Bounty> bounties = new HashSet<Bounty>();
		try {
			int playerId = getPlayerId(offlinePlayer);
			openPreparedStatements(mConnection, PreparedConnectionType.GET_BOUNTIES);
			mGetBounties.setInt(1, playerId);
			mGetBounties.setInt(2, playerId);

			ResultSet set = mGetBounties.executeQuery();

			while (set.next()) {
				Bounty b = new Bounty();
				b.setBountyOwnerId(set.getInt(1));
				b.setBountyOwner(getPlayerByPlayerId(set.getInt(1)));
				b.setMobtype(set.getString(2));
				b.setWantedPlayerId(set.getInt(3));
				b.setWantedPlayer(getPlayerByPlayerId(set.getInt(3)));
				b.setNpcId(set.getInt(4));
				b.setMobId(set.getString(5));
				b.setWorldGroup(set.getString(6));
				b.setCreatedDate(set.getLong(7));
				b.setEndDate(set.getLong(8));
				b.setPrize(set.getDouble(9));
				b.setMessage(set.getString(10));
				b.setStatus(BountyStatus.valueOf(set.getInt(11)));
				bounties.add(b);
			}
			set.close();
			mGetBounties.close();
			return (Set<Bounty>) bounties;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new DataStoreException(e);
		}
	};

	@Override
	public void insertBounty(Set<Bounty> bountyDataSet) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.INSERT_BOUNTY);
			for (Bounty bounty : bountyDataSet) {
				int bountyOwnerId = getPlayerId(bounty.getBountyOwner());
				int wantedPlayerId = getPlayerId(bounty.getWantedPlayer());
				mInsertBounty.setString(1, bounty.getMobtype());
				mInsertBounty.setInt(2, bountyOwnerId);
				mInsertBounty.setInt(3, wantedPlayerId);
				mInsertBounty.setInt(4, bounty.getNpcId());
				mInsertBounty.setString(5, bounty.getMobId());
				mInsertBounty.setString(6, bounty.getWorldGroup());
				mInsertBounty.setLong(7, bounty.getCreatedDate());
				mInsertBounty.setLong(8, bounty.getEndDate());
				mInsertBounty.setDouble(9, bounty.getPrize());
				mInsertBounty.setString(10, bounty.getMessage());
				mInsertBounty.setInt(11, bounty.getStatus().getValue());
				mInsertBounty.addBatch();
			}
			mInsertBounty.executeBatch();
			mInsertBounty.close();
			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}

	};

	@Override
	public void updateBounty(Set<Bounty> bountyDataSet) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.UPDATE_BOUNTY);
			for (Bounty bounty : bountyDataSet) {
				mUpdateBounty.setDouble(1, bounty.getPrize());
				mUpdateBounty.setString(2, bounty.getMessage());
				mUpdateBounty.setLong(3, bounty.getEndDate());
				mUpdateBounty.setInt(4, bounty.getStatus().getValue());
				mUpdateBounty.setInt(5, getPlayerId(bounty.getWantedPlayer()));
				mUpdateBounty.setInt(6, getPlayerId(bounty.getBountyOwner()));
				mUpdateBounty.setString(7, bounty.getWorldGroup());
				mUpdateBounty.addBatch();
			}
			mUpdateBounty.executeBatch();
			mUpdateBounty.close();
			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	};

	@Override
	public void deleteBounty(Set<Bounty> bounties) throws DataStoreException {
		Iterator<Bounty> itr = bounties.iterator();
		while (itr.hasNext()) {
			Bounty b = itr.next();
			b.setStatus(BountyStatus.deleted);
		}
		insertBounty(bounties);
	}

	@Override
	public void cancelBounty(Set<Bounty> bounties) throws DataStoreException {
		Iterator<Bounty> itr = bounties.iterator();
		while (itr.hasNext()) {
			Bounty b = itr.next();
			b.setStatus(BountyStatus.canceled);
		}
		insertBounty(bounties);
	}

	// ******************************************************************
	// Player Settings
	// ******************************************************************

	/**
	 * getPlayerSettings
	 * 
	 * @param offlinePlayer
	 *            :OfflinePlayer
	 * @return PlayerData
	 * @throws DataStoreException
	 * @throws SQLException
	 * 
	 */
	@Override
	public PlayerSettings getPlayerSettings(OfflinePlayer offlinePlayer) throws DataStoreException, SQLException {
		openPreparedGetPlayerStatements();
		mGetPlayerData[0].setString(1, offlinePlayer.getUniqueId().toString());
		ResultSet result = mGetPlayerData[0].executeQuery();
		if (result.next()) {
			PlayerSettings ps = new PlayerSettings(offlinePlayer, result.getBoolean("LEARNING_MODE"),
					result.getBoolean("MUTE_MODE"));
			int id = result.getInt("PLAYER_ID");
			if (id != 0)
				ps.setPlayerId(id);
			result.close();
			Messages.debug("Reading Playersettings from Database: %s", ps.toString());
			closePreparedGetPlayerStatements();
			return ps;
		}
		closePreparedGetPlayerStatements();
		throw new UserNotFoundException("User " + offlinePlayer.toString() + " is not present in database");
	}

	/**
	 * insertPlayerSettings to database
	 */
	@Override
	public void insertPlayerSettings(PlayerSettings playerSettings) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.INSERT_PLAYER_DATA);
			mInsertPlayerData.setString(1, playerSettings.getPlayer().getUniqueId().toString());
			mInsertPlayerData.setString(2, playerSettings.getPlayer().getName());
			mInsertPlayerData.setInt(3, playerSettings.isLearningMode() ? 1 : 0);
			mInsertPlayerData.setInt(4, playerSettings.isMuted() ? 1 : 0);
			mInsertPlayerData.addBatch();
			mInsertPlayerData.executeBatch();
			mInsertPlayerData.close();
			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	}

	@Override
	public void updatePlayerSettings(Set<PlayerSettings> playerDataSet) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.UPDATE_PLAYER_SETTINGS);
			for (PlayerSettings playerData : playerDataSet) {
				mUpdatePlayerSettings.setInt(1, playerData.isLearningMode() ? 1 : 0);
				mUpdatePlayerSettings.setInt(2, playerData.isMuted() ? 1 : 0);
				mUpdatePlayerSettings.setString(3, playerData.getPlayer().getUniqueId().toString());
				mUpdatePlayerSettings.addBatch();
			}
			mUpdatePlayerSettings.executeBatch();
			mUpdatePlayerSettings.close();
			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	}

	/**
	 * getPlayerIds
	 * 
	 * @param players
	 *            : A set of players: Set<OfflinePlayer>
	 * @return Map<UUID, Integer> a Map with all players UUID and player_ID in
	 *         the Database.
	 * @throws SQLException
	 */
	protected Map<UUID, Integer> getPlayerIdsNotUsed(Set<OfflinePlayer> players) throws SQLException {

		// Then select all players in batches of 10,5,2,1 into the HashMap ids
		// and return ids.
		int left = players.size();
		Iterator<OfflinePlayer> it = players.iterator();
		HashMap<UUID, Integer> ids = new HashMap<UUID, Integer>();
		ArrayList<OfflinePlayer> changedNames = new ArrayList<OfflinePlayer>();

		while (left > 0) {
			PreparedStatement statement;
			openPreparedGetPlayerStatements();
			int size = 0;
			if (left >= 10) {
				size = 10;
				statement = mGetPlayerData[3];
			} else if (left >= 5) {
				size = 5;
				statement = mGetPlayerData[2];
			} else if (left >= 2) {
				size = 2;
				statement = mGetPlayerData[1];
			} else {
				size = 1;
				statement = mGetPlayerData[0];
			}

			left -= size;

			ArrayList<OfflinePlayer> temp = new ArrayList<OfflinePlayer>(size);
			for (int i = 0; i < size; ++i) {
				OfflinePlayer player = it.next();
				temp.add(player);
				statement.setString(i + 1, player.getUniqueId().toString());
			}

			ResultSet results = statement.executeQuery();

			int index = 0;
			while (results.next()) {
				OfflinePlayer player = temp.get(index++);
				if (results.getString(1).equals(player.getUniqueId().toString())
						&& !results.getString(2).equals(player.getPlayer().getName())) {
					MobHunting.getInstance().getLogger()
							.warning("[MobHunting] Name change detected(1): " + results.getString(2) + " -> "
									+ player.getPlayer().getName() + " UUID=" + player.getUniqueId().toString());
					changedNames.add(player);
				}

				ids.put(UUID.fromString(results.getString(1)), results.getInt(3));
			}
			results.close();

			Iterator<OfflinePlayer> itr = changedNames.iterator();
			while (itr.hasNext()) {
				OfflinePlayer p = itr.next();
				Messages.debug("Updating playername in database and in memory (%s)", p.getName());
				updatePlayerName(p.getPlayer());
			}
		}
		closePreparedGetPlayerStatements();
		mConnection.commit();
		return ids;
	}

	/**
	 * getPlayerID. get the player ID and check if the player has change name
	 * 
	 * @param offlinePlayer
	 * @return PlayerID: int
	 * @throws SQLException
	 * @throws DataStoreException
	 */
	public int getPlayerId(OfflinePlayer offlinePlayer) throws SQLException, DataStoreException {
		if (offlinePlayer == null)
			return 0;
		int playerId = 0;
		PlayerSettings ps = MobHunting.getPlayerSettingsmanager().getPlayerSettings(offlinePlayer);
		if (ps != null)
			playerId = ps.getPlayerId();
		if (playerId == 0) {
			openPreparedGetPlayerStatements();
			mGetPlayerData[0].setString(1, offlinePlayer.getUniqueId().toString());
			ResultSet result = mGetPlayerData[0].executeQuery();
			ArrayList<OfflinePlayer> changedNames = new ArrayList<OfflinePlayer>();
			if (result.next()) {
				String name = result.getString(2);
				UUID uuid = UUID.fromString(result.getString(1));
				if (name != null && uuid != null)
					if (offlinePlayer.getUniqueId().equals(uuid) && !offlinePlayer.getName().equals(name)) {
						MobHunting.getInstance().getLogger().warning("[MobHunting] Name change detected(2): " + name
								+ " -> " + offlinePlayer.getName() + " UUID=" + offlinePlayer.getUniqueId().toString());
						changedNames.add(offlinePlayer);
					}
				playerId = result.getInt(3);
				result.close();
				Iterator<OfflinePlayer> itr = changedNames.iterator();
				while (itr.hasNext()) {
					OfflinePlayer p = itr.next();
					Messages.debug("Updating playername in database and in memory (%s)", p.getName());
					updatePlayerName(p.getPlayer());
				}
			}
			result.close();
			closePreparedGetPlayerStatements();
		}
		return playerId;
	}

	/**
	 * updatePlayerName - update the players name in the Database
	 * 
	 * @param offlinePlayer
	 *            : OfflinePlayer
	 * @throws SQLException
	 */
	protected void updatePlayerName(OfflinePlayer offlinePlayer) throws SQLException {
		openPreparedStatements(mConnection, PreparedConnectionType.UPDATE_PLAYER_NAME);
		try {
			mUpdatePlayerName.setString(1, offlinePlayer.getName());
			mUpdatePlayerName.setString(2, offlinePlayer.getUniqueId().toString());
			mUpdatePlayerName.executeUpdate();
			mUpdatePlayerName.close();
			mConnection.commit();
		} finally {
			mConnection.rollback();
		}
	}

	/**
	 * getPlayerByName - get the player
	 * 
	 * @param name
	 *            : String
	 * @return player
	 */
	@Override
	public OfflinePlayer getPlayerByName(String name) throws DataStoreException {
		if (name.equals("Random Bounty"))
			return null; // used for Random Bounties
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.GET_PLAYER_UUID);
			mGetPlayerUUID.setString(1, name);
			ResultSet set = mGetPlayerUUID.executeQuery();

			if (set.next()) {
				UUID uid = UUID.fromString(set.getString(1));
				set.close();
				mGetPlayerUUID.close();
				return Bukkit.getOfflinePlayer(uid);
			}
			mGetPlayerUUID.close();
			throw new UserNotFoundException("[MobHunting] User " + name + " is not present in database");
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * getPlayerByName - get the player
	 * 
	 * @param name
	 *            : String
	 * @return player
	 */
	@Override
	public OfflinePlayer getPlayerByPlayerId(int playerId) throws DataStoreException {
		if (playerId == 0)
			return null; // Used for Random Bounty
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.GET_PLAYER_BY_PLAYER_ID);
			mGetPlayerByPlayerId.setInt(1, playerId);
			ResultSet set = mGetPlayerByPlayerId.executeQuery();

			if (set.next()) {
				UUID uid = UUID.fromString(set.getString(1));
				set.close();
				mGetPlayerByPlayerId.close();
				return Bukkit.getOfflinePlayer(uid);
			}
			mGetPlayerByPlayerId.close();
			throw new UserNotFoundException("[MobHunting] PlayerId " + playerId + " is not present in database");
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	// ******************************************************************
	// ACHIEVEMENTS
	// ******************************************************************

	/**
	 * Args: player id
	 */
	protected PreparedStatement mLoadAchievements;

	/**
	 * Args: player id, achievement, date, progress
	 */
	protected PreparedStatement mSaveAchievement;

	/**
	 * loadAchievements - loading the achievements for one player into memory
	 * 
	 * @param OfflinePlayer
	 *            :
	 * @throws DataStoreException
	 */
	@Override
	public Set<AchievementStore> loadAchievements(OfflinePlayer player) throws DataStoreException {
		HashSet<AchievementStore> achievements = new HashSet<AchievementStore>();
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.LOAD_ARCHIEVEMENTS);
			int playerId = getPlayerId(player);
			if (playerId != 0) {
				mLoadAchievements.setInt(1, playerId);
				ResultSet set = mLoadAchievements.executeQuery();
				while (set.next()) {
					achievements.add(new AchievementStore(set.getString(1), player, set.getInt(3)));
				}
				set.close();
			}
			mLoadAchievements.close();
			return achievements;
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * saveAchievements - save all achievements to the Database
	 */
	@Override
	public void saveAchievements(Set<AchievementStore> achievements) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.SAVE_ACHIEVEMENTS);
			for (AchievementStore achievement : achievements) {
				mSaveAchievement.setInt(1, getPlayerId(achievement.player));
				mSaveAchievement.setString(2, achievement.id);
				mSaveAchievement.setDate(3, new Date(System.currentTimeMillis()));
				mSaveAchievement.setInt(4, achievement.progress);

				mSaveAchievement.addBatch();
			}
			mSaveAchievement.executeBatch();
			mSaveAchievement.close();

			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	}

	// ******************************************************************
	// MOBS
	// ******************************************************************

	/**
	 * Args: player id
	 */
	protected PreparedStatement mLoadMobs;

	/**
	 * Args: player id, achievement, date, progress
	 */
	protected PreparedStatement mInsertMobs;

	/**
	 * Args: player id, achievement, date, progress
	 */
	protected PreparedStatement mUpdateMobs;

	/**
	 * loadMobs - load all mobs from database into memory
	 */
	@Override
	public Set<ExtendedMob> loadMobs() throws DataStoreException {
		HashSet<ExtendedMob> mobs = new HashSet<ExtendedMob>();
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.LOAD_MOBS);
			ResultSet set = mLoadMobs.executeQuery();
			while (set.next()) {
				mobs.add(new ExtendedMob(set.getInt("MOB_ID"), PluginManager.valueOf(set.getInt("PLUGIN_ID")),
						set.getString("MOBTYPE")));
			}
			set.close();
			mLoadMobs.close();
			return mobs;
		} catch (SQLException e) {
			throw new DataStoreException(e);
		}
	}

	/**
	 * saveMobs - save all NEW mobs to the Database
	 */
	@Override
	public void insertMobs(Set<ExtendedMob> mobs) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.INSERT_MOBS);
			for (ExtendedMob mob : mobs) {
				mInsertMobs.setInt(1, mob.getMobPlugin().getId());
				mInsertMobs.setString(2, mob.getMobtype());

				mInsertMobs.addBatch();
			}
			mInsertMobs.executeBatch();
			mInsertMobs.close();

			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	}

	/**
	 * updateMobs - update all EXSISTING mobs in the Database
	 */
	@Override
	public void updateMobs(Set<ExtendedMob> mobs) throws DataStoreException {
		try {
			openPreparedStatements(mConnection, PreparedConnectionType.UPDATE_MOBS);
			for (ExtendedMob mob : mobs) {
				mUpdateMobs.setInt(1, mob.getMobPlugin().getId());
				mUpdateMobs.setString(2, mob.getMobtype());
				mUpdateMobs.setInt(3, mob.getMob_id());

				mUpdateMobs.addBatch();
			}
			mUpdateMobs.executeBatch();
			mUpdateMobs.close();

			mConnection.commit();
		} catch (SQLException e) {
			rollback();
			throw new DataStoreException(e);
		}
	}

}
