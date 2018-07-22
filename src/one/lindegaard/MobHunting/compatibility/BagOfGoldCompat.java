package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import net.citizensnpcs.api.CitizensAPI;
import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.EconomyManager;
import one.lindegaard.BagOfGold.PlayerSettingsManager;
import one.lindegaard.BagOfGold.bank.BankManager;
import one.lindegaard.BagOfGold.storage.DataStoreManager;

public class BagOfGoldCompat {

	private BagOfGold mPlugin;
	private static boolean supported = false;

	public BagOfGoldCompat() {
		mPlugin = (BagOfGold) Bukkit.getPluginManager().getPlugin(CompatPlugin.BagOfGold.getName());

		if (mPlugin.getDescription().getVersion().compareTo("2.0.0") >= 0) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with BagOfGold ("
							+ getBagOfGoldAPI().getDescription().getVersion() + ")");
			supported = true;
		} else {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
							+ "Your current version of BagOfGold (" + mPlugin.getDescription().getVersion()
							+ ") is outdated. Please upgrade to 2.0.0 or newer.");
			Bukkit.getPluginManager().disablePlugin(mPlugin);
		}

	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public BagOfGold getBagOfGoldAPI() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public String getBagOfGoldFormat() {
		return BagOfGold.getInstance().getConfigManager().numberFormat;
	}

	public EconomyManager getEconomyManager() {
		return BagOfGold.getInstance().getEconomyManager();
	}

	public PlayerSettingsManager getPlayerSettingsManager() {
		return BagOfGold.getInstance().getPlayerSettingsManager();
	}

	public DataStoreManager getDataStoreManager() {
		return BagOfGold.getInstance().getDataStoreManager();
	}

	public BankManager getBankManager() {
		return BagOfGold.getInstance().getBankManager();
	}

	public static boolean isNPC(Integer id) {
		if (isSupported())
			return CitizensAPI.getNPCRegistry().getById(id) != null;
		return false;
	}

}
