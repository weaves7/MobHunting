package one.lindegaard.MobHunting;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.rewards.CustomItems;
import one.lindegaard.MobHunting.storage.DataStoreException;
import one.lindegaard.MobHunting.storage.IDataCallback;
import one.lindegaard.MobHunting.storage.PlayerSettings;

public class PlayerSettingsManager implements Listener {

	private HashMap<UUID, PlayerSettings> mPlayerSettings = new HashMap<UUID, PlayerSettings>();

	private MobHunting plugin;

	/**
	 * Constructor for the PlayerSettingsmanager
	 */
	PlayerSettingsManager(MobHunting plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Get playerSettings from memory
	 * 
	 * @param offlinePlayer
	 * @return PlayerSettings
	 */
	public PlayerSettings getPlayerSettings(OfflinePlayer offlinePlayer) {
		if (mPlayerSettings.containsKey(offlinePlayer.getUniqueId()))
			return mPlayerSettings.get(offlinePlayer.getUniqueId());
		else {
			try {
				PlayerSettings ps = plugin.getStoreManager().loadPlayerSettings(offlinePlayer);
				return ps;
			} catch (DataStoreException | SQLException e) {
				Messages.debug("%s is not in the database. (Has played before=%s)", offlinePlayer.getName(),
						offlinePlayer.hasPlayedBefore());
				return new PlayerSettings(offlinePlayer, 0);
			}
		}

	}

	/**
	 * Store playerSettings in memory
	 * 
	 * @param playerSettings
	 */
	public void setPlayerSettings(OfflinePlayer player, PlayerSettings playerSettings) {
		mPlayerSettings.put(player.getUniqueId(), playerSettings);
	}

	/**
	 * Remove PlayerSettings from Memory
	 * 
	 * @param player
	 */
	public void removePlayerSettings(OfflinePlayer player) {
		Messages.debug("Removing %s from player settings cache", player.getName());
		mPlayerSettings.remove(player.getUniqueId());
	}

	/**
	 * Read PlayerSettings From database into Memory when player joins
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (containsKey(player))
			Messages.debug("Using cached playersettings for %s. Balance=%s", player.getName(),
					plugin.getRewardManager().getBalance(player));
		else {
			load(player);
		}
	}

	/**
	 * Write PlayerSettings to Database when Player Quit and remove PlayerSettings
	 * from memory
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		save(player);
	}

	/**
	 * Load PlayerSettings asynchronously from Database
	 * 
	 * @param player
	 */
	public void load(final OfflinePlayer player) {
		plugin.getDataStoreManager().requestPlayerSettings(player, new IDataCallback<PlayerSettings>() {

			@Override
			public void onCompleted(PlayerSettings ps) {
				if (ps.isMuted())
					Messages.debug("%s isMuted()", player.getName());
				if (ps.isLearningMode())
					Messages.debug("%s is in LearningMode()", player.getName());
				mPlayerSettings.put(player.getUniqueId(), ps);
				// get Balance to check if balance in DB is the same as in
				// player inventory
				double balance = plugin.getRewardManager().getBalance(player);
				Messages.debug("%s balance=%s", player.getName(), balance);

				if (ps.getTexture()==null || ps.getTexture().equals("")) {
					Messages.debug("Store %s skin in MobHunting Skin Cache" , player.getName());
					new CustomItems(plugin).getPlayerHead(player.getUniqueId(), 1, 0);
				}
			}

			@Override
			public void onError(Throwable error) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[MobHunting][ERROR] " + player.getName()
						+ " is new, creating user in database.");
				mPlayerSettings.put(player.getUniqueId(),
						new PlayerSettings(player,
								BagOfGoldCompat.isSupported()
										? BagOfGold.getInstance().getConfigManager().startingBalance
										: 0));
			}
		});
	}

	/**
	 * Write PlayerSettings to Database
	 * 
	 * @param player
	 */
	public void save(final OfflinePlayer player) {
		plugin.getDataStoreManager().updatePlayerSettings(player, getPlayerSettings(player).isLearningMode(),
				getPlayerSettings(player).isMuted());
	}

	/**
	 * Test if PlayerSettings contains data for Player
	 * 
	 * @param player
	 * @return true if player exists in PlayerSettings in Memory
	 */
	public boolean containsKey(final OfflinePlayer player) {
		return mPlayerSettings.containsKey(player.getUniqueId());
	}

}
