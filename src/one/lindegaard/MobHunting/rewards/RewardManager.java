package one.lindegaard.MobHunting.rewards;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Mule;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.PolarBear;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.gestern.gringotts.Configuration;
import org.gestern.gringotts.currency.Denomination;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.storage.PlayerSettings;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.CustomMobsCompat;
import one.lindegaard.MobHunting.compatibility.GringottsCompat;
import one.lindegaard.MobHunting.compatibility.HerobrineCompat;
import one.lindegaard.MobHunting.compatibility.MyPetCompat;
import one.lindegaard.MobHunting.compatibility.MysteriousHalloweenCompat;
import one.lindegaard.MobHunting.compatibility.MythicMobsCompat;
import one.lindegaard.MobHunting.compatibility.SmartGiantsCompat;
import one.lindegaard.MobHunting.compatibility.TARDISWeepingAngelsCompat;
import one.lindegaard.MobHunting.mobs.ExtendedMobManager;
import one.lindegaard.MobHunting.mobs.MinecraftMob;
import one.lindegaard.MobHunting.util.Misc;

@SuppressWarnings("deprecation")
public class RewardManager {

	private MobHunting plugin;
	private File file;
	private YamlConfiguration config = new YamlConfiguration();

	private Economy mEconomy;
	private PickupRewards pickupRewards;

	private HashMap<Integer, Double> droppedMoney = new HashMap<Integer, Double>();
	private HashMap<UUID, Reward> placedMoney_Reward = new HashMap<UUID, Reward>();
	private HashMap<UUID, Location> placedMoney_Location = new HashMap<UUID, Location>();

	public RewardManager(MobHunting plugin) {
		this.plugin = plugin;
		file = new File(plugin.getDataFolder(), "rewards.yml");
		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		if (economyProvider == null) {
			Bukkit.getLogger().severe(plugin.getMessages().getString(plugin.getName().toLowerCase() + ".hook.econ"));
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}

		mEconomy = economyProvider.getProvider();

		plugin.getMessages().debug("MobHunting is using %s as Economy Provider", mEconomy.getName());
		plugin.getMessages().debug("Number of Economy Providers = %s",
				Bukkit.getServicesManager().getRegistrations(Economy.class).size());
		if (Bukkit.getServicesManager().getRegistrations(Economy.class).size() > 1) {
			for (RegisteredServiceProvider<Economy> registation : Bukkit.getServicesManager()
					.getRegistrations(Economy.class)) {
				plugin.getMessages().debug("Provider name=%s", registation.getProvider().getName());
			}
		}

		pickupRewards = new PickupRewards(plugin);

		Bukkit.getPluginManager().registerEvents(new RewardListeners(plugin), plugin);
		if (Misc.isMC18OrNewer())
			Bukkit.getPluginManager().registerEvents(new MoneyMergeEventListener(plugin), plugin);

		if (Misc.isMC112OrNewer() && eventDoesExists())
			Bukkit.getPluginManager().registerEvents(new EntityPickupItemEventListener(pickupRewards), plugin);
		else
			Bukkit.getPluginManager().registerEvents(new PlayerPickupItemEventListener(pickupRewards), plugin);
		loadAllStoredRewards();

		if (plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency)
			new BagOfGoldSign(plugin);

	}

	private boolean eventDoesExists() {
		try {
			@SuppressWarnings({ "rawtypes", "unused" })
			Class cls = Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}

	}

	public Economy getEconomy() {
		return mEconomy;
	}

	public HashMap<Integer, Double> getDroppedMoney() {
		return droppedMoney;
	}

	public HashMap<UUID, Reward> getLocations() {
		return placedMoney_Reward;
	}

	public HashMap<UUID, Location> getReward() {
		return placedMoney_Location;
	}

	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
		EconomyResponse result = mEconomy.depositPlayer(offlinePlayer, amount);
		if (!result.transactionSuccess() && offlinePlayer.isOnline())
			((Player) offlinePlayer).sendMessage(ChatColor.RED + "Unable to add money: " + result.errorMessage);
		return result;
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
		EconomyResponse result = mEconomy.withdrawPlayer(offlinePlayer, amount);
		if (!result.transactionSuccess() && offlinePlayer.isOnline())
			((Player) offlinePlayer).sendMessage(ChatColor.RED + "Unable to remove money: " + result.errorMessage);
		return result;
	}

	public String format(double amount) {
		if (plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency)
			return Misc.format(amount);
		else
			return getEconomy().format(amount);
	}

	public double getBalance(OfflinePlayer offlinePlayer) {
		if (BagOfGoldCompat.isSupported() || !plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency)
			return mEconomy.getBalance(offlinePlayer);
		else if (offlinePlayer.isOnline()) {
			return getAmountInInventory((Player) offlinePlayer);
		} else {
			return 0;
		}
	}

	public double getAmountInInventory(Player player) {
		double amountInInventory = 0;
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward reward = Reward.getReward(is);
				if (reward.isBagOfGoldReward() || reward.isItemReward())
					amountInInventory = amountInInventory + reward.getMoney();
			}
		}
		return amountInInventory;
	}

	public boolean addBagOfGoldPlayer_RewardManager(Player player, double amount) {
		boolean found = false;
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward rewardInSlot = Reward.getReward(is);
				if ((rewardInSlot.isBagOfGoldReward() || rewardInSlot.isItemReward())) {
					rewardInSlot.setMoney(rewardInSlot.getMoney() + amount);
					ItemMeta im = is.getItemMeta();
					im.setLore(rewardInSlot.getHiddenLore());
					String displayName = plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
							? format(rewardInSlot.getMoney())
							: rewardInSlot.getDisplayname() + " (" + format(rewardInSlot.getMoney()) + ")";
					im.setDisplayName(
							ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + displayName);
					is.setItemMeta(im);
					is.setAmount(1);
					plugin.getMessages().debug("Added %s to item in slot %s, new value is %s (addBagOfGoldPlayer_RewardManager)",
							format(amount), slot, format(rewardInSlot.getMoney()));
					found = true;
					break;
				}
			}
		}
		if (!found) {
			if (player.getInventory().firstEmpty() != -1) {
				ItemStack is;
				if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")) {
					is = setDisplayNameAndHiddenLores(
							new ItemStack(Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem)),
							plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(), amount,
							UUID.fromString(Reward.MH_REWARD_ITEM_UUID), null);
				} else {
					is = new CustomItems(plugin).getCustomtexture(UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID),
							plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
							plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
							plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, Misc.floor(amount),
							UUID.randomUUID(), UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID));
				}
				player.getInventory().addItem(is);
				return true;
			}
		} else
			return true;
		return false;
	}

	public double removeBagOfGoldPlayer_RewardManager(Player player, double amount) {
		MobHunting mPlugin = (MobHunting) Bukkit.getPluginManager().getPlugin("MobHunting");
		double taken = 0;
		double toBeTaken = Misc.floor(amount);
		CustomItems customItems = new CustomItems(mPlugin);
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward reward = Reward.getReward(is);
				if (reward.isBagOfGoldReward()) {
					double saldo = Misc.floor(reward.getMoney());
					if (saldo > toBeTaken) {
						reward.setMoney(saldo - toBeTaken);
						is = customItems.getCustomtexture(reward.getRewardType(),
								plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
								plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
								plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, saldo - toBeTaken,
								UUID.randomUUID(), reward.getSkinUUID());
						player.getInventory().setItem(slot, is);
						taken = taken + toBeTaken;
						toBeTaken = 0;
						return taken;
					} else {
						is.setItemMeta(null);
						is.setType(Material.AIR);
						is.setAmount(0);
						player.getInventory().setItem(slot, is);
						taken = taken + saldo;
						toBeTaken = toBeTaken - saldo;
						return taken;
					}
				}
			}
		}

		return amount;

	}

	public void dropMoneyOnGround_RewardManager(Player player, Entity killedEntity, Location location, double money) {
		Item item = null;
		money = Misc.ceil(money);
		if (GringottsCompat.isSupported()) {
			List<Denomination> denoms = Configuration.CONF.currency.denominations();
			int unit = Configuration.CONF.currency.unit;
			double rest = money;
			for (Denomination d : denoms) {
				ItemStack is = new ItemStack(d.key.type.getType(), 1);
				while (rest >= (d.value / unit)) {
					item = location.getWorld().dropItem(location, is);
					rest = rest - (d.value / unit);
				}
			}
		} else {
			ItemStack is;
			UUID uuid = null, skinuuid = null;
			if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("KILLED")) {
				MinecraftMob mob = MinecraftMob.getMinecraftMobType(killedEntity);
				uuid = UUID.fromString(Reward.MH_REWARD_KILLED_UUID);
				skinuuid = mob.getPlayerUUID();
				is = new CustomItems(plugin).getCustomHead(mob, mob.getFriendlyName(), 1, money, skinuuid);

			} else if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("SKULL")) {
				uuid = UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID);
				skinuuid = uuid;
				is = new CustomItems(plugin).getCustomtexture(uuid,
						plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
						plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
						plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, money, UUID.randomUUID(),
						skinuuid);

			} else if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("KILLER")) {
				uuid = UUID.fromString(Reward.MH_REWARD_KILLER_UUID);
				skinuuid = player.getUniqueId();
				is = new CustomItems(plugin).getPlayerHead(player.getUniqueId(), 1, money);

			} else { // ITEM
				uuid = UUID.fromString(Reward.MH_REWARD_ITEM_UUID);
				skinuuid = null;
				is = new ItemStack(Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem), 1);
			}

			item = location.getWorld().dropItem(location, is);
			getDroppedMoney().put(item.getEntityId(), money);
			item.setMetadata(Reward.MH_REWARD_DATA,
					new FixedMetadataValue(plugin,
							new Reward(
									plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM") ? ""
											: Reward.getReward(is).getDisplayname(),
									money, uuid, UUID.randomUUID(), skinuuid)));
			if (Misc.isMC18OrNewer()) {
				item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
						+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM") ? format(money)
								: Reward.getReward(is).getDisplayname() + " (" + format(money) + ")"));
				item.setCustomNameVisible(true);
			}
		}
		if (item != null)
			plugin.getMessages().debug("%s was dropped on the ground as item %s (# of rewards=%s)", format(money),
					plugin.getConfigManager().dropMoneyOnGroundItemtype, droppedMoney.size());
	}

	public void saveReward(UUID uuid) {
		try {
			config.options().header("This is the rewards placed as blocks. Do not edit this file manually!");
			if (placedMoney_Reward.containsKey(uuid)) {
				Location location = placedMoney_Location.get(uuid);
				if (location != null && location.getBlock().getType() == Material.SKULL) {
					Reward reward = placedMoney_Reward.get(uuid);
					ConfigurationSection section = config.createSection(uuid.toString());
					section.set("location", location);
					reward.save(section);
					plugin.getMessages().debug("Saving a reward placed as a block.");
					config.save(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadAllStoredRewards() {
		int n = 0;
		int deleted = 0;
		try {

			if (!file.exists())
				return;

			config.load(file);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		try {
			for (String key : config.getKeys(false)) {
				ConfigurationSection section = config.getConfigurationSection(key);
				Reward reward = new Reward();
				reward.read(section);
				Location location = (Location) section.get("location");
				if (location != null && location.getBlock().getType() == Material.SKULL) {
					location.getBlock().setMetadata(Reward.MH_REWARD_DATA,
							new FixedMetadataValue(MobHunting.getInstance(), new Reward(reward)));
					placedMoney_Reward.put(UUID.fromString(key), reward);
					placedMoney_Location.put(UUID.fromString(key), location);
					n++;
				} else {
					deleted++;
					config.set(key, null);
				}
			}
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		try {

			if (deleted > 0) {
				plugin.getMessages().debug("Deleted %s rewards from the rewards.yml file", deleted);
				File file_copy = new File(MobHunting.getInstance().getDataFolder(), "rewards.yml.old");
				Files.copy(file.toPath(), file_copy.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				config.save(file);
			}
			if (n > 0) {
				plugin.getMessages().debug("Loaded %s rewards from the rewards.yml file", n);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ItemStack setDisplayNameAndHiddenLores(ItemStack skull, String mDisplayName, double money, UUID rewardUUID,
			UUID skinUUID) {
		ItemMeta skullMeta = skull.getItemMeta();
		skullMeta.setLore(new ArrayList<String>(Arrays.asList("Hidden:" + mDisplayName, "Hidden:" + money,
				"Hidden:" + rewardUUID, money == 0 ? "Hidden:" : "Hidden:" + UUID.randomUUID(), "Hidden:" + skinUUID)));
		if (money == 0)
			skullMeta.setDisplayName(
					ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + mDisplayName);
		else
			skullMeta.setDisplayName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
					+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM") ? format(money)
							: mDisplayName + " (" + format(money) + ")"));
		skull.setItemMeta(skullMeta);
		return skull;
	}

	public double getPlayerKilledByMobPenalty(Player playerToBeRobbed) {
		if (plugin.getConfigManager().mobKillsPlayerPenalty == null
				|| plugin.getConfigManager().mobKillsPlayerPenalty.trim().equals("")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.trim().equals("0%")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.trim().equals("0")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.trim().isEmpty()) {
			return 0;
		} else if (plugin.getConfigManager().mobKillsPlayerPenalty.trim().contains(":")) {
			String[] str1 = plugin.getConfigManager().mobKillsPlayerPenalty.trim().split(":");
			double prize = (plugin.mRand.nextDouble()
					* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
			return Misc.round(prize);
		} else if (plugin.getConfigManager().mobKillsPlayerPenalty.trim().endsWith("%")) {
			double prize = 0;
			if (BagOfGoldCompat.isSupported()) {
				PlayerSettings ps = BagOfGold.getApi().getPlayerSettingsManager().getPlayerSettings(playerToBeRobbed);
				prize = Math.round(Double
						.valueOf(plugin.getConfigManager().mobKillsPlayerPenalty.trim().substring(0,
								plugin.getConfigManager().mobKillsPlayerPenalty.trim().length() - 1))
						* (ps.getBalance() + ps.getBalanceChanges()) / 100);
			} else {
				prize = Math.floor(Double
						.valueOf(plugin.getConfigManager().mobKillsPlayerPenalty.trim().substring(0,
								plugin.getConfigManager().mobKillsPlayerPenalty.trim().length() - 1))
						* getBalance(playerToBeRobbed) / 100);
			}
			return Misc.round(prize);
		} else
			return Double.valueOf(plugin.getConfigManager().mobKillsPlayerPenalty.trim());
	}

	public double getRandomPrice(String str) {
		if (str == null || str.equals("") || str.isEmpty()) {
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
					+ " The random_bounty_prize is not set in config.yml. Please set the prize to 0 or a positive number.");
			return 0;
		} else if (str.contains(":")) {
			String[] str1 = str.split(":");
			double prize = (plugin.mRand.nextDouble()
					* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
			return Misc.round(prize);
		} else
			return Double.valueOf(str);
	}

	/**
	 * Return the reward money for a given mob
	 * 
	 * @param mob
	 * @return value
	 */
	public double getBaseKillPrize(Entity mob) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(mob)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()))
				return getPrice(mob, TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()).getRewardPrize());
			plugin.getMessages().debug("TARDISWeepingAngel %s has no reward data",
					TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).getName());
			return 0;

		} else if (MythicMobsCompat.isMythicMob(mob)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(mob)))
				return getPrice(mob, MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(mob))
						.getRewardPrize());
			plugin.getMessages().debug("MythicMob %s has no reward data", MythicMobsCompat.getMythicMobType(mob));
			return 0;

		} else if (CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(mob);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return getPrice(mob, CitizensCompat.getMobRewardData().get(key).getRewardPrize());
			}
			plugin.getMessages().debug("Citizens mob %s has no reward data", npc.getName());
			return 0;

		} else if (CustomMobsCompat.isCustomMob(mob)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(mob)))
				return getPrice(mob, CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(mob))
						.getRewardPrize());
			plugin.getMessages().debug("CustomMob %s has no reward data", CustomMobsCompat.getCustomMobType(mob));
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()))
				return getPrice(mob, MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()).getRewardPrize());
			plugin.getMessages().debug("MysteriousHalloween %s has no reward data",
					MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name());
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(mob)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(mob)))
				return getPrice(mob, SmartGiantsCompat.getMobRewardData()
						.get(SmartGiantsCompat.getSmartGiantsMobType(mob)).getRewardPrize());
			plugin.getMessages().debug("SmartGiantsS %s has no reward data", SmartGiantsCompat.getSmartGiantsMobType(mob));
			return 0;

		} else if (MyPetCompat.isMyPet(mob)) {
			plugin.getMessages().debug("Tried to find a prize for a MyPet: %s (Owner=%s)", MyPetCompat.getMyPet(mob),
					MyPetCompat.getMyPetOwner(mob));
			return getPrice(mob, plugin.getConfigManager().wolfMoney);

		} else if (HerobrineCompat.isHerobrineMob(mob)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(mob)))
				return getPrice(mob, HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(mob))
						.getRewardPrize());
			plugin.getMessages().debug("Herobrine mob %s has no reward data", HerobrineCompat.getHerobrineMobType(mob));
			return 0;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return getPrice(mob, plugin.getConfigManager().parrotMoney);
				else if (mob instanceof Illusioner)
					return getPrice(mob, plugin.getConfigManager().illusionerMoney);

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return getPrice(mob, plugin.getConfigManager().llamaMoney);
				else if (mob instanceof Vex)
					return getPrice(mob, plugin.getConfigManager().vexMoney);
				else if (mob instanceof Vindicator)
					return getPrice(mob, plugin.getConfigManager().vindicatorMoney);
				else if (mob instanceof Evoker)
					return getPrice(mob, plugin.getConfigManager().evokerMoney);
				else if (mob instanceof Donkey)
					return getPrice(mob, plugin.getConfigManager().donkeyMoney);
				else if (mob instanceof Mule)
					return getPrice(mob, plugin.getConfigManager().muleMoney);
				else if (mob instanceof SkeletonHorse)
					return getPrice(mob, plugin.getConfigManager().skeletonHorseMoney);
				else if (mob instanceof ZombieHorse)
					return getPrice(mob, plugin.getConfigManager().zombieHorseMoney);
				else if (mob instanceof Stray)
					return getPrice(mob, plugin.getConfigManager().strayMoney);
				else if (mob instanceof Husk)
					return getPrice(mob, plugin.getConfigManager().huskMoney);
				else if (mob instanceof ZombieVillager)
					return getPrice(mob, plugin.getConfigManager().zombieVillagerMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return getPrice(mob, plugin.getConfigManager().nitwitMoney);

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return getPrice(mob, plugin.getConfigManager().polarBearMoney);
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return getPrice(mob, plugin.getConfigManager().strayMoney);
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return getPrice(mob, plugin.getConfigManager().huskMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return getPrice(mob, plugin.getConfigManager().villagerMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return getPrice(mob, plugin.getConfigManager().priestMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return getPrice(mob, plugin.getConfigManager().butcherMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return getPrice(mob, plugin.getConfigManager().blacksmithMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return getPrice(mob, plugin.getConfigManager().librarianMoney);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return getPrice(mob, plugin.getConfigManager().farmerMoney);

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return getPrice(mob, plugin.getConfigManager().shulkerMoney);

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return getPrice(mob, plugin.getConfigManager().elderGuardianMoney);
				else if (mob instanceof Guardian)
					return getPrice(mob, plugin.getConfigManager().guardianMoney);
				else if (mob instanceof Endermite)
					return getPrice(mob, plugin.getConfigManager().endermiteMoney);
				else if (mob instanceof Rabbit)
					if (((Rabbit) mob).getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY)
						return getPrice(mob, plugin.getConfigManager().killerRabbitMoney);
					else
						return getPrice(mob, plugin.getConfigManager().rabbitMoney);

			// Minecraft 1.7.10 and older entities
			if (mob instanceof Player) {
				if (plugin.getConfigManager().pvpKillMoney.trim().endsWith("%")) {
					double prize = 0;
					if (BagOfGoldCompat.isSupported()) {
						PlayerSettings ps = BagOfGold.getApi().getPlayerSettingsManager()
								.getPlayerSettings((Player) mob);
						prize = Math.round(Double
								.valueOf(plugin.getConfigManager().pvpKillMoney.trim().substring(0,
										plugin.getConfigManager().pvpKillMoney.trim().length() - 1))
								* (ps.getBalance() + ps.getBalanceChanges()) / 100);
					} else {
						prize = Math.round(Double
								.valueOf(plugin.getConfigManager().pvpKillMoney.trim().substring(0,
										plugin.getConfigManager().pvpKillMoney.trim().length() - 1))
								* getBalance((Player) mob) / 100);
					}
					return Misc.round(prize);
				} else if (plugin.getConfigManager().pvpKillMoney.contains(":")) {
					String[] str1 = plugin.getConfigManager().pvpKillMoney.split(":");
					double prize2 = (plugin.mRand.nextDouble()
							* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
					return Misc.round(Double.valueOf(prize2));
				} else
					return Double.valueOf(plugin.getConfigManager().pvpKillMoney.trim());
			} else if (mob instanceof Blaze)
				return getPrice(mob, plugin.getConfigManager().blazeMoney);
			else if (mob instanceof Creeper)
				return getPrice(mob, plugin.getConfigManager().creeperMoney);
			else if (mob instanceof Silverfish)
				return getPrice(mob, plugin.getConfigManager().silverfishMoney);
			else if (mob instanceof Enderman)
				return getPrice(mob, plugin.getConfigManager().endermanMoney);
			else if (mob instanceof Giant)
				return getPrice(mob, plugin.getConfigManager().giantMoney);
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return getPrice(mob, plugin.getConfigManager().skeletonMoney);
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return getPrice(mob, plugin.getConfigManager().witherSkeletonMoney);
			else if (mob instanceof CaveSpider)
				return getPrice(mob, plugin.getConfigManager().caveSpiderMoney);
			else if (mob instanceof Spider)
				return getPrice(mob, plugin.getConfigManager().spiderMoney);
			else if (mob instanceof Witch)
				return getPrice(mob, plugin.getConfigManager().witchMoney);
			else if (mob instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				if (((PigZombie) mob).isBaby())
					return Misc.round(getPrice(mob, plugin.getConfigManager().zombiePigmanMoney)
							* plugin.getConfigManager().babyMultiplier);
				else
					return getPrice(mob, plugin.getConfigManager().zombiePigmanMoney);
			else if (mob instanceof Zombie)
				if (((Zombie) mob).isBaby())
					return Misc.round(getPrice(mob, plugin.getConfigManager().zombieMoney)
							* plugin.getConfigManager().babyMultiplier);
				else
					return getPrice(mob, plugin.getConfigManager().zombieMoney);
			else if (mob instanceof Ghast)
				return getPrice(mob, plugin.getConfigManager().ghastMoney);
			else if (mob instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return getPrice(mob, plugin.getConfigManager().magmaCubeMoney) * ((MagmaCube) mob).getSize();
			else if (mob instanceof Slime)
				return getPrice(mob, plugin.getConfigManager().slimeMoney) * ((Slime) mob).getSize();
			else if (mob instanceof EnderDragon)
				return getPrice(mob, plugin.getConfigManager().enderDragonMoney);
			else if (mob instanceof Wither)
				return getPrice(mob, plugin.getConfigManager().witherMoney);
			else if (mob instanceof IronGolem)
				return getPrice(mob, plugin.getConfigManager().ironGolemMoney);

			// Passive mobs
			else if (mob instanceof Bat)
				return getPrice(mob, plugin.getConfigManager().batMoney);
			else if (mob instanceof Chicken)
				return getPrice(mob, plugin.getConfigManager().chickenMoney);
			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return getPrice(mob, plugin.getConfigManager().mushroomCowMoney);
				else
					return getPrice(mob, plugin.getConfigManager().cowPrize);
			else if (mob instanceof Horse)
				return getPrice(mob, plugin.getConfigManager().horseMoney);
			else if (mob instanceof Ocelot)
				return getPrice(mob, plugin.getConfigManager().ocelotMoney);
			else if (mob instanceof Pig)
				return getPrice(mob, plugin.getConfigManager().pigMoney);
			else if (mob instanceof Sheep)
				return getPrice(mob, plugin.getConfigManager().sheepMoney);
			else if (mob instanceof Snowman)
				return getPrice(mob, plugin.getConfigManager().snowmanMoney);
			else if (mob instanceof Squid)
				return getPrice(mob, plugin.getConfigManager().squidMoney);
			else if (mob instanceof Villager)
				return getPrice(mob, plugin.getConfigManager().villagerMoney);
			else if (mob instanceof Wolf) {
				return getPrice(mob, plugin.getConfigManager().wolfMoney);
			} else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return getPrice(mob, plugin.getConfigManager().rawFishMoney);
				} else if (is.getData().getData() == (byte) 1) {
					return getPrice(mob, plugin.getConfigManager().rawSalmonMoney);
				} else if (is.getData().getData() == (byte) 2) {
					return getPrice(mob, plugin.getConfigManager().clownfishMoney);
				} else if (is.getData().getData() == (byte) 3) {
					return getPrice(mob, plugin.getConfigManager().pufferfishMoney);
				}
			}
		}
		// plugin.getMessages().debug("Mobhunting could not find the prize for killing this
		// mob: %s (%s)",
		// ExtendedMobManager.getMobName(mob), mob.getType());
		return 0;
	}

	private double getPrice(Entity mob, String str) {
		if (str == null || str.equals("") || str.isEmpty()) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
							+ " The prize for killing a " + ExtendedMobManager.getMobName(mob)
							+ " is not set in config.yml. Please set the prize to 0 or a positive or negative number.");
			return 0;
		} else if (str.startsWith(":")) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
							+ " The prize for killing a " + ExtendedMobManager.getMobName(mob)
							+ " in config.yml has a wrong format. The prize can't start with \":\"");
			if (str.length() > 1)
				return getPrice(mob, str.substring(1, str.length()));
			else
				return 0;
		} else if (str.contains(":")) {
			String[] str1 = str.split(":");
			double prize = (plugin.mRand.nextDouble()
					* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
			return Misc.round(prize);
		} else
			return Double.valueOf(str);
	}

	/**
	 * Get the command to be run when the player kills a Mob.
	 * 
	 * @param mob
	 * @return a number of commands to be run in the console. Each command must be
	 *         separeted by a "|"
	 */
	public List<HashMap<String, String>> getKillCommands(Entity mob) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(mob)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()).getConsoleRunCommand();
			return new ArrayList<>();

		} else if (MythicMobsCompat.isMythicMob(mob)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(mob)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(mob))
						.getConsoleRunCommand();
			return new ArrayList<>();

		} else if (CitizensCompat.isNPC(mob) && CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(mob);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getConsoleRunCommand();
			}
			return new ArrayList<>();

		} else if (CustomMobsCompat.isCustomMob(mob)) {
			if (mob.hasMetadata(CustomMobsCompat.MH_CUSTOMMOBS)) {
				List<MetadataValue> data = mob.getMetadata(CustomMobsCompat.MH_CUSTOMMOBS);
				for (MetadataValue value : data)
					if (value.value() instanceof RewardData)
						return ((RewardData) value.value()).getConsoleRunCommand();
			} else if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(mob)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(mob))
						.getConsoleRunCommand();
			return new ArrayList<>();

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()).getConsoleRunCommand();
			return new ArrayList<>();

		} else if (SmartGiantsCompat.isSmartGiants(mob)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(mob)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(mob))
						.getConsoleRunCommand();
			return null;

		} else if (HerobrineCompat.isHerobrineMob(mob)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(mob)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(mob))
						.getConsoleRunCommand();
			return new ArrayList<>();

		} else if (MyPetCompat.isMyPet(mob)) {
			return plugin.getConfigManager().wolfCommands;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return plugin.getConfigManager().parrotCommands;
				else if (mob instanceof Illusioner)
					return plugin.getConfigManager().illusionerCommands;

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return plugin.getConfigManager().llamaCommands;
				else if (mob instanceof Vex)
					return plugin.getConfigManager().vexCommands;
				else if (mob instanceof Vindicator)
					return plugin.getConfigManager().vindicatorCommands;
				else if (mob instanceof Evoker)
					return plugin.getConfigManager().evokerCommands;
				else if (mob instanceof Donkey)
					return plugin.getConfigManager().donkeyCommands;
				else if (mob instanceof Mule)
					return plugin.getConfigManager().muleCommands;
				else if (mob instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonHorseCommands;
				else if (mob instanceof ZombieHorse)
					return plugin.getConfigManager().zombieHorseCommands;
				else if (mob instanceof Stray)
					return plugin.getConfigManager().strayCommands;
				else if (mob instanceof Husk)
					return plugin.getConfigManager().huskCommands;
				else if (mob instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitCommands;

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return plugin.getConfigManager().polarBearCommands;
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayCommands;
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianCommands;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerCommnds;

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return plugin.getConfigManager().shulkerCommands;

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return plugin.getConfigManager().elderGuardianCommands;
				else if (mob instanceof Guardian)
					return plugin.getConfigManager().guardianCommands;
				else if (mob instanceof Endermite)
					return plugin.getConfigManager().endermiteCommands;
				else if (mob instanceof Rabbit)
					if ((((Rabbit) mob).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerRabbitCommands;
					else
						return plugin.getConfigManager().rabbitCommads;

			if (mob instanceof Player)
				return plugin.getConfigManager().pvpCmdNew;
			else if (mob instanceof Blaze)
				//TODO: Change this
				return plugin.getConfigManager().blazeCommands;
			else if (mob instanceof Creeper)
				return plugin.getConfigManager().creeperCommands;
			else if (mob instanceof Silverfish)
				return plugin.getConfigManager().silverfishCommands;
			else if (mob instanceof Enderman)
				return plugin.getConfigManager().endermanCommands;
			else if (mob instanceof Giant)
				return plugin.getConfigManager().giantCommands;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonCommands;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonCommands;
			else if (mob instanceof Spider)
				if (mob instanceof CaveSpider)
					// CaveSpider is a sub class of Spider
					return plugin.getConfigManager().caveSpiderCommands;
				else
					return plugin.getConfigManager().spiderCommands;
			else if (mob instanceof Witch)
				return plugin.getConfigManager().witchCommands;
			else if (mob instanceof Zombie)
				if (mob instanceof PigZombie)
					return plugin.getConfigManager().zombiePigmanCommands;
				else
					return plugin.getConfigManager().zombieCommands;
			else if (mob instanceof Ghast)
				return plugin.getConfigManager().ghastCommands;
			else if (mob instanceof MagmaCube)
				// Magmacube is an instance of slime and must be checked before
				// the Slime itself
				return plugin.getConfigManager().magmaCubeCommands;
			else if (mob instanceof Slime)
				return plugin.getConfigManager().slimeCommands;
			else if (mob instanceof EnderDragon)
				return plugin.getConfigManager().enderDragonCommands;
			else if (mob instanceof Wither)
				return plugin.getConfigManager().witherCommands;
			else if (mob instanceof IronGolem)
				return plugin.getConfigManager().ironGolemCommands;

			// Passive mobs
			else if (mob instanceof Bat)
				return plugin.getConfigManager().batCommands;
			else if (mob instanceof Chicken)
				return plugin.getConfigManager().chickenCommands;

			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					return plugin.getConfigManager().mushroomCowCommands;
				else
					return plugin.getConfigManager().cowCmdNew;
			else if (mob instanceof Horse)
				return plugin.getConfigManager().horseCommands;
			else if (mob instanceof Ocelot)
				return plugin.getConfigManager().ocelotCommands;
			else if (mob instanceof Pig)
				return plugin.getConfigManager().pigCommands;
			else if (mob instanceof Sheep)
				return plugin.getConfigManager().sheepCommands;
			else if (mob instanceof Snowman)
				return plugin.getConfigManager().snowmanCommands;
			else if (mob instanceof Squid)
				return plugin.getConfigManager().squidCommands;
			else if (mob instanceof Villager)
				return plugin.getConfigManager().villagerCommands;
			else if (mob instanceof Wolf)
				return plugin.getConfigManager().wolfCommands;
			else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishCommands;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonCommands;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishCommands;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishCommands;
				}
			}

		}
		return new ArrayList<>();
	}

	/**
	 * Get the text to be send to the player describing the reward
	 * 
	 * @param mob
	 * @return String
	 */
	public String getKillRewardDescription(Entity mob) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(mob)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()).getRewardDescription();
			return "";

		} else if (MythicMobsCompat.isMythicMob(mob)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(mob)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(mob))
						.getRewardDescription();
			return "";

		} else if (CitizensCompat.isNPC(mob) && CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(mob);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getRewardDescription();
			}
			return "";

		} else if (CustomMobsCompat.isCustomMob(mob)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(mob)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(mob))
						.getRewardDescription();
			return "";

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()).getRewardDescription();
			return "";

		} else if (SmartGiantsCompat.isSmartGiants(mob)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(mob)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(mob))
						.getRewardDescription();
			return "";

		} else if (HerobrineCompat.isHerobrineMob(mob)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(mob)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(mob))
						.getRewardDescription();
			return "";

		} else if (MyPetCompat.isMyPet(mob)) {
			return plugin.getConfigManager().wolfMessage;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return plugin.getConfigManager().parrotMessage;
				else if (mob instanceof Illusioner)
					return plugin.getConfigManager().illusionerMessage;

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return plugin.getConfigManager().llamaMessage;
				else if (mob instanceof Vex)
					return plugin.getConfigManager().vexMessage;
				else if (mob instanceof Vindicator)
					return plugin.getConfigManager().vindicatorMessage;
				else if (mob instanceof Evoker)
					return plugin.getConfigManager().evokerMessage;
				else if (mob instanceof Donkey)
					return plugin.getConfigManager().donkeyMessage;
				else if (mob instanceof Mule)
					return plugin.getConfigManager().muleMessage;
				else if (mob instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonHorseMessage;
				else if (mob instanceof ZombieHorse)
					return plugin.getConfigManager().zombieHorseMessage;
				else if (mob instanceof Stray)
					return plugin.getConfigManager().strayMessage;
				else if (mob instanceof Husk)
					return plugin.getConfigManager().huskMessage;
				else if (mob instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitMessage;

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return plugin.getConfigManager().polarBearMessage;
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayMessage;
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianMessage;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerMessage;

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return plugin.getConfigManager().shulkerMessage;

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return plugin.getConfigManager().elderGuardianMessage;
				else if (mob instanceof Guardian)
					return plugin.getConfigManager().guardianMessge;
				else if (mob instanceof Endermite)
					return plugin.getConfigManager().endermiteMessage;
				else if (mob instanceof Rabbit)
					if ((((Rabbit) mob).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerRabbitMessage;
					else
						return plugin.getConfigManager().rabbitMessage;

			// MC1.7 or older
			if (mob instanceof Player)
				return plugin.getConfigManager().pvpKillMessage;
			else if (mob instanceof Blaze)
				return plugin.getConfigManager().blazeMessage;
			else if (mob instanceof Creeper)
				return plugin.getConfigManager().creeperMessage;
			else if (mob instanceof Silverfish)
				return plugin.getConfigManager().silverfishMessage;
			else if (mob instanceof Enderman)
				return plugin.getConfigManager().endermanMessage;
			else if (mob instanceof Giant)
				return plugin.getConfigManager().giantMessage;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonMessage;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonMessage;
			else if (mob instanceof CaveSpider)
				// CaveSpider is a Subclass of Spider
				return plugin.getConfigManager().caveSpiderMessage;
			else if (mob instanceof Spider)
				return plugin.getConfigManager().spiderMessage;
			else if (mob instanceof Witch)
				return plugin.getConfigManager().witchMessage;
			else if (mob instanceof PigZombie)
				// PigZombie is a subclass of Zombie
				return plugin.getConfigManager().zombiePigmanMessage;
			else if (mob instanceof Zombie)
				return plugin.getConfigManager().zombieMessage;
			else if (mob instanceof Ghast)
				return plugin.getConfigManager().ghastMessage;
			else if (mob instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeMessage;
			else if (mob instanceof Slime)
				return plugin.getConfigManager().slimeMessage;
			else if (mob instanceof EnderDragon)
				return plugin.getConfigManager().enderDragonMessage;
			else if (mob instanceof Wither)
				return plugin.getConfigManager().witherMessage;
			else if (mob instanceof IronGolem)
				return plugin.getConfigManager().ironGolemMessage;

			// Passive mobs
			else if (mob instanceof Bat)
				return plugin.getConfigManager().batMessage;
			else if (mob instanceof Chicken)
				return plugin.getConfigManager().chickenMessage;
			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return plugin.getConfigManager().mushroomCowMessage;
				else
					return plugin.getConfigManager().cowCmdDesc;
			else if (mob instanceof Horse)
				return plugin.getConfigManager().horseMessage;
			else if (mob instanceof Ocelot)
				return plugin.getConfigManager().ocelotMessage;
			else if (mob instanceof Pig)
				return plugin.getConfigManager().pigMessage;
			else if (mob instanceof Sheep)
				return plugin.getConfigManager().sheepMessage;
			else if (mob instanceof Snowman)
				return plugin.getConfigManager().snowmanMessage;
			else if (mob instanceof Squid)
				return plugin.getConfigManager().squidMessage;
			else if (mob instanceof Villager)
				return plugin.getConfigManager().villagerMessage;
			else if (mob instanceof Wolf)
				return plugin.getConfigManager().wolfMessage;
			else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishMessage;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonMessage;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishMessage;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishMessage;
				}
			}

		}
		return "";
	}

	public double getMoneyChance(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).getChance();
			return 0;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).getChance();
			return 0;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getChance();
			}
			return 0;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).getChance();
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).getChance();
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
						.getChance();
			return 0;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).getChance();
			return 0;

		} else if (MyPetCompat.isMyPet(killed)) {
			return plugin.getConfigManager().wolfCmdRunChance;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return plugin.getConfigManager().parrotCmdRunChance;
				else if (killed instanceof Illusioner)
					return plugin.getConfigManager().illusionerCmdRunChance;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return plugin.getConfigManager().llamaCmdRunChance;
				else if (killed instanceof Vex)
					return plugin.getConfigManager().vexMoneyChance;
				else if (killed instanceof Vindicator)
					return plugin.getConfigManager().vindicatorCmdRunChance;
				else if (killed instanceof Evoker)
					return plugin.getConfigManager().evokerCmdRunChance;
				else if (killed instanceof Donkey)
					return plugin.getConfigManager().donkeyCmdRunChance;
				else if (killed instanceof Mule)
					return plugin.getConfigManager().muleCmdRunChance;
				else if (killed instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonHorseCmdRunChance;
				else if (killed instanceof ZombieHorse)
					return plugin.getConfigManager().zombieHorseCmdRunChance;
				else if (killed instanceof Stray)
					return plugin.getConfigManager().strayMoneyChance;
				else if (killed instanceof Husk)
					return plugin.getConfigManager().huskMoneyChance;
				else if (killed instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitCmdRunChance;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return plugin.getConfigManager().polarBearMoneyChance;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayMoneyChance;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskMoneyChance;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherMoneyChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithMoneyChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerCmdRunChance;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return plugin.getConfigManager().shulkerMoneyChance;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return plugin.getConfigManager().elderGuardianMoneyChance;
				else if (killed instanceof Guardian)
					return plugin.getConfigManager().guardianMoneyChance;
				else if (killed instanceof Endermite)
					return plugin.getConfigManager().endermiteMoneyChance;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerRabbitMoneyChance;
					else
						return plugin.getConfigManager().rabbitCmdRunChance;

			// MC1.7 or older
			if (killed instanceof Player) {
				return plugin.getConfigManager().pvpCmdRunChance;
			} else if (killed instanceof Blaze)
				return plugin.getConfigManager().blazeMoneyChance;
			else if (killed instanceof Creeper)
				return plugin.getConfigManager().creeperMoneyChance;
			else if (killed instanceof Silverfish)
				return plugin.getConfigManager().silverfishMoneyChance;
			else if (killed instanceof Enderman)
				return plugin.getConfigManager().endermanMoneyChance;
			else if (killed instanceof Giant)
				return plugin.getConfigManager().giantMoneyChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonMoneyChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonMoneyChance;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return plugin.getConfigManager().caveSpiderMoneyChance;
			else if (killed instanceof Spider)
				return plugin.getConfigManager().spiderMoneyChance;
			else if (killed instanceof Witch)
				return plugin.getConfigManager().witchMoneyChance;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return plugin.getConfigManager().zombiePigmanMoneyChance;
			else if (killed instanceof Zombie)
				return plugin.getConfigManager().zombieMoneyChance;
			else if (killed instanceof Ghast)
				return plugin.getConfigManager().ghastMoneyChance;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeMoneyChance;
			else if (killed instanceof Slime)
				return plugin.getConfigManager().slimeMoneyChance;
			else if (killed instanceof EnderDragon)
				return plugin.getConfigManager().enderDragonMoneyChance;
			else if (killed instanceof Wither)
				return plugin.getConfigManager().witherMoneyChance;
			else if (killed instanceof IronGolem)
				return plugin.getConfigManager().ironGolemMoneyChance;

			// Passive mobs
			else if (killed instanceof Bat)
				return plugin.getConfigManager().batCmdRunChance;
			else if (killed instanceof Chicken)
				return plugin.getConfigManager().chickenCmdRunChance;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return plugin.getConfigManager().mushroomCowCmdRunChance;
				else
					return plugin.getConfigManager().cowCmdRunChance;
			else if (killed instanceof Horse)
				return plugin.getConfigManager().horseCmdRunChance;
			else if (killed instanceof Ocelot)
				return plugin.getConfigManager().ocelotCmdRunChance;
			else if (killed instanceof Pig)
				return plugin.getConfigManager().pigCmdRunChance;
			else if (killed instanceof Sheep)
				return plugin.getConfigManager().sheepCmdRunChance;
			else if (killed instanceof Snowman)
				return plugin.getConfigManager().snowmanCmdRunChance;
			else if (killed instanceof Squid)
				return plugin.getConfigManager().squidCmdRunChance;
			else if (killed instanceof Villager)
				return plugin.getConfigManager().villagerCmdRunChance;
			else if (killed instanceof Wolf)
				return plugin.getConfigManager().wolfCmdRunChance;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishCmdRunChance;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonCmdRunChance;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishCmdRunChance;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishCmdRunChance;
				}
			}
		}
		return 0;
	}

	public double getMcMMOChance(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name())
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed))
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getMcMMOSkillRewardChance();
			}
			return 0;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed))
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name())
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed))
						.getMcMMOSkillRewardChance();
			return 0;

		} else if (MyPetCompat.isMyPet(killed)) {
			return plugin.getConfigManager().wolfMcMMOSkillRewardChance;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return plugin.getConfigManager().parrotMcMMOSkillRewardChance;
				else if (killed instanceof Illusioner)
					return plugin.getConfigManager().illusionerMcMMOSkillRewardChance;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return plugin.getConfigManager().llamaMcMMOSkillRewardChance;
				else if (killed instanceof Vex)
					return plugin.getConfigManager().vexMcMMOSkillRewardChance;
				else if (killed instanceof Vindicator)
					return plugin.getConfigManager().vindicatorMcMMOSkillRewardChance;
				else if (killed instanceof Evoker)
					return plugin.getConfigManager().evokerMcMMOSkillRewardChance;
				else if (killed instanceof Donkey)
					return plugin.getConfigManager().donkeyMcMMOSkillRewardChance;
				else if (killed instanceof Mule)
					return plugin.getConfigManager().muleMcMMOSkillRewardChance;
				else if (killed instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonHorseMcMMOSkillRewardChance;
				else if (killed instanceof ZombieHorse)
					return plugin.getConfigManager().zombieHorseMcMMOSkillRewardChance;
				else if (killed instanceof Stray)
					return plugin.getConfigManager().strayMcMMOSkillRewardChance;
				else if (killed instanceof Husk)
					return plugin.getConfigManager().huskMcMMOSkillRewardChance;
				else if (killed instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitMcMMOSkillRewardChance;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return plugin.getConfigManager().polarBearMcMMOSkillRewardChance;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayMcMMOSkillRewardChance;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskMcMMOSkillRewardChance;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianMcMMOSkillRewardChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerMcMMOSkillRewardChance;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return plugin.getConfigManager().shulkerMcMMOSkillRewardChance;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return plugin.getConfigManager().elderGuardianMcMMOSkillRewardChance;
				else if (killed instanceof Guardian)
					return plugin.getConfigManager().guardianMcMMOSkillRewardChance;
				else if (killed instanceof Endermite)
					return plugin.getConfigManager().endermiteMcMMOSkillRewardChance;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerRabbitMcMMOSkillRewardChance;
					else
						return plugin.getConfigManager().rabbitMcMMOSkillRewardChance;

			// MC1.7 or older
			if (killed instanceof Player) {
				return plugin.getConfigManager().pvpPlayerMcMMOSkillRewardChance;
			} else if (killed instanceof Blaze)
				return plugin.getConfigManager().blazeMcMMOSkillRewardChance;
			else if (killed instanceof Creeper)
				return plugin.getConfigManager().creeperMcMMOSkillRewardChance;
			else if (killed instanceof Silverfish)
				return plugin.getConfigManager().silverfishMcMMOSkillRewardChance;
			else if (killed instanceof Enderman)
				return plugin.getConfigManager().endermanMcMMOSkillRewardChance;
			else if (killed instanceof Giant)
				return plugin.getConfigManager().giantMcMMOSkillRewardChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonMcMMOSkillRewardChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonMcMMOSkillRewardChance;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return plugin.getConfigManager().caveSpiderMcMMOSkillRewardChance;
			else if (killed instanceof Spider)
				return plugin.getConfigManager().spiderMcMMOSkillRewardChance;
			else if (killed instanceof Witch)
				return plugin.getConfigManager().witchMcMMOSkillRewardChance;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return plugin.getConfigManager().zombiePigManMcMMOSkillRewardChance;
			else if (killed instanceof Zombie)
				return plugin.getConfigManager().zombieMcMMOSkillRewardChance;
			else if (killed instanceof Ghast)
				return plugin.getConfigManager().ghastMcMMOSkillRewardChance;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeMcMMOSkillRewardChance;
			else if (killed instanceof Slime)
				return plugin.getConfigManager().slimeMcMMOSkillRewardChance;
			else if (killed instanceof EnderDragon)
				return plugin.getConfigManager().enderdragonMcMMOSkillRewardChance;
			else if (killed instanceof Wither)
				return plugin.getConfigManager().witherMcMMOSkillRewardChance;
			else if (killed instanceof IronGolem)
				return plugin.getConfigManager().ironGolemMcMMOSkillRewardChance;

			// Passive mobs
			else if (killed instanceof Bat)
				return plugin.getConfigManager().batMcMMOSkillRewardChance;
			else if (killed instanceof Chicken)
				return plugin.getConfigManager().chickenMcMMOSkillRewardChance;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return plugin.getConfigManager().mushroomCowMcMMOSkillRewardChance;
				else
					return plugin.getConfigManager().cowMcMMOSkillRewardChance;
			else if (killed instanceof Horse)
				return plugin.getConfigManager().horseMcMMOSkillRewardChance;
			else if (killed instanceof Ocelot)
				return plugin.getConfigManager().ocelotMcMMOSkillRewardChance;
			else if (killed instanceof Pig)
				return plugin.getConfigManager().pigMcMMOSkillRewardChance;
			else if (killed instanceof Sheep)
				return plugin.getConfigManager().sheepMcMMOSkillRewardChance;
			else if (killed instanceof Snowman)
				return plugin.getConfigManager().snowmanMcMMOSkillRewardChance;
			else if (killed instanceof Squid)
				return plugin.getConfigManager().squidMcMMOSkillRewardChance;
			else if (killed instanceof Villager)
				return plugin.getConfigManager().villagerMcMMOSkillRewardChance;
			else if (killed instanceof Wolf)
				return plugin.getConfigManager().wolfMcMMOSkillRewardChance;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawfishMcMMOSkillRewardChance;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawsalmonMcMMOSkillRewardChance;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishMcMMOSkillRewardChance;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishMcMMOSkillRewardChance;
				}
			}
		}
		return 0;
	}

	private int getMcMMOXP(Entity mob, String str) {
		if (str == null || str.equals("") || str.isEmpty()) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
							+ " The McMMO XP for killing a " + ExtendedMobManager.getMobName(mob)
							+ " is not set in config.yml. Please set the McMMO XP to 0 or a positive number.");
			return 0;
		} else if (str.startsWith(":")) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
							+ " The McMMO XP for killing a " + ExtendedMobManager.getMobName(mob)
							+ " in config.yml has a wrong format. The prize can't start with \":\"");
			if (str.length() > 1)
				return getMcMMOXP(mob, str.substring(1, str.length()));
			else
				return 0;
		} else if (str.contains(":")) {
			String[] str1 = str.split(":");
			Integer prize = plugin.mRand.nextInt(Integer.valueOf(str1[1]))
					+ Integer.valueOf(str1[0]);
			return prize;
		} else
			return Integer.valueOf(str);
	}

	public int getMcMMOLevel(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name())
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed))
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getMcMMOSkillRewardAmount();
			}
			return 0;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed))
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name())
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed))
						.getMcMMOSkillRewardAmount();
			return 0;

		} else if (MyPetCompat.isMyPet(killed)) {
			return getMcMMOXP(killed, plugin.getConfigManager().wolfMcMMOSkillRewardAmount);

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return getMcMMOXP(killed, plugin.getConfigManager().parrotMcMMOSkillRewardAmount);
				else if (killed instanceof Illusioner)
					return getMcMMOXP(killed, plugin.getConfigManager().illusionerMcMMOSkillRewardAmount);

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return getMcMMOXP(killed, plugin.getConfigManager().llamaMcMMOSkillRewardAmount);
				else if (killed instanceof Vex)
					return getMcMMOXP(killed, plugin.getConfigManager().vexMcMMOSkillRewardAmount);
				else if (killed instanceof Vindicator)
					return getMcMMOXP(killed, plugin.getConfigManager().vindicatorMcMMOSkillRewardAmount);
				else if (killed instanceof Evoker)
					return getMcMMOXP(killed, plugin.getConfigManager().evokerMcMMOSkillRewardAmount);
				else if (killed instanceof Donkey)
					return getMcMMOXP(killed, plugin.getConfigManager().donkeyMcMMOSkillRewardAmount);
				else if (killed instanceof Mule)
					return getMcMMOXP(killed, plugin.getConfigManager().muleMcMMOSkillRewardAmount);
				else if (killed instanceof SkeletonHorse)
					return getMcMMOXP(killed, plugin.getConfigManager().skeletonHorseMcMMOSkillRewardAmount);
				else if (killed instanceof ZombieHorse)
					return getMcMMOXP(killed, plugin.getConfigManager().zombieHorseMcMMOSkillRewardAmount);
				else if (killed instanceof Stray)
					return getMcMMOXP(killed, plugin.getConfigManager().strayMcMMOSkillRewardAmount);
				else if (killed instanceof Husk)
					return getMcMMOXP(killed, plugin.getConfigManager().huskMcMMOSkillRewardAmount);
				else if (killed instanceof ZombieVillager)
					return getMcMMOXP(killed, plugin.getConfigManager().zombieVillagerMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return getMcMMOXP(killed, plugin.getConfigManager().nitwitMcMMOSkillRewardAmount);

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return getMcMMOXP(killed, plugin.getConfigManager().polarBearMcMMOSkillRewardAmount);
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return getMcMMOXP(killed, plugin.getConfigManager().strayMcMMOSkillRewardAmount);
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return getMcMMOXP(killed, plugin.getConfigManager().huskMcMMOSkillRewardAmount);

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return getMcMMOXP(killed, plugin.getConfigManager().villagerMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return getMcMMOXP(killed, plugin.getConfigManager().priestMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return getMcMMOXP(killed, plugin.getConfigManager().butcherMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return getMcMMOXP(killed, plugin.getConfigManager().blacksmithMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return getMcMMOXP(killed, plugin.getConfigManager().librarianMcMMOSkillRewardAmount);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return getMcMMOXP(killed, plugin.getConfigManager().farmerMcMMOSkillRewardAmount);

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return getMcMMOXP(killed, plugin.getConfigManager().shulkerMcMMOSkillRewardAmount);

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return getMcMMOXP(killed, plugin.getConfigManager().elderGuardianMcMMOSkillRewardAmount);
				else if (killed instanceof Guardian)
					return getMcMMOXP(killed, plugin.getConfigManager().guardianMcMMOSkillRewardAmount);
				else if (killed instanceof Endermite)
					return getMcMMOXP(killed, plugin.getConfigManager().endermiteMcMMOSkillRewardAmount);
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return getMcMMOXP(killed, plugin.getConfigManager().killerRabbitMcMMOSkillRewardAmount);
					else
						return getMcMMOXP(killed, plugin.getConfigManager().rabbitMcMMOSkillRewardAmount);

			// MC1.7 or older
			if (killed instanceof Player) {
				return getMcMMOXP(killed, plugin.getConfigManager().pvpPlayerMcMMOSkillRewardAmount);
			} else if (killed instanceof Blaze)
				return getMcMMOXP(killed, plugin.getConfigManager().blazeMcMMOSkillRewardAmount);
			else if (killed instanceof Creeper)
				return getMcMMOXP(killed, plugin.getConfigManager().creeperMcMMOSkillRewardAmount);
			else if (killed instanceof Silverfish)
				return getMcMMOXP(killed, plugin.getConfigManager().silverfishMcMMOSkillRewardAmount);
			else if (killed instanceof Enderman)
				return getMcMMOXP(killed, plugin.getConfigManager().endermanMcMMOSkillRewardAmount);
			else if (killed instanceof Giant)
				return getMcMMOXP(killed, plugin.getConfigManager().giantMcMMOSkillRewardAmount);
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return getMcMMOXP(killed, plugin.getConfigManager().skeletonMcMMOSkillRewardAmount);
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return getMcMMOXP(killed, plugin.getConfigManager().witherSkeletonMcMMOSkillRewardAmount);
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return getMcMMOXP(killed, plugin.getConfigManager().caveSpiderMcMMOSkillRewardAmount);
			else if (killed instanceof Spider)
				return getMcMMOXP(killed, plugin.getConfigManager().spiderMcMMOSkillRewardAmount);
			else if (killed instanceof Witch)
				return getMcMMOXP(killed, plugin.getConfigManager().witchMcMMOSkillRewardAmount);
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return getMcMMOXP(killed, plugin.getConfigManager().zombiePigManMcMMOSkillRewardAmount);
			else if (killed instanceof Zombie)
				return getMcMMOXP(killed, plugin.getConfigManager().zombieMcMMOSkillRewardAmount);
			else if (killed instanceof Ghast)
				return getMcMMOXP(killed, plugin.getConfigManager().ghastMcMMOSkillRewardAmount);
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return getMcMMOXP(killed, plugin.getConfigManager().magmaCubeMcMMOSkillRewardAmount);
			else if (killed instanceof Slime)
				return getMcMMOXP(killed, plugin.getConfigManager().slimeMcMMOSkillRewardAmount);
			else if (killed instanceof EnderDragon)
				return getMcMMOXP(killed, plugin.getConfigManager().enderdragonMcMMOSkillRewardAmount);
			else if (killed instanceof Wither)
				return getMcMMOXP(killed, plugin.getConfigManager().witherMcMMOSkillRewardAmount);
			else if (killed instanceof IronGolem)
				return getMcMMOXP(killed, plugin.getConfigManager().ironGolemMcMMOSkillRewardAmount);

			// Passive mobs
			else if (killed instanceof Bat)
				return getMcMMOXP(killed, plugin.getConfigManager().batMcMMOSkillRewardAmount);
			else if (killed instanceof Chicken)
				return getMcMMOXP(killed, plugin.getConfigManager().chickenMcMMOSkillRewardAmount);
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return getMcMMOXP(killed, plugin.getConfigManager().mushroomCowMcMMOSkillRewardAmount);
				else
					return getMcMMOXP(killed, plugin.getConfigManager().cowMcMMOSkillRewardAmount);
			else if (killed instanceof Horse)
				return getMcMMOXP(killed, plugin.getConfigManager().horseMcMMOSkillRewardAmount);
			else if (killed instanceof Ocelot)
				return getMcMMOXP(killed, plugin.getConfigManager().ocelotMcMMOSkillRewardAmount);
			else if (killed instanceof Pig)
				return getMcMMOXP(killed, plugin.getConfigManager().pigMcMMOSkillRewardAmount);
			else if (killed instanceof Sheep)
				return getMcMMOXP(killed, plugin.getConfigManager().sheepMcMMOSkillRewardAmount);
			else if (killed instanceof Snowman)
				return getMcMMOXP(killed, plugin.getConfigManager().snowmanMcMMOSkillRewardAmount);
			else if (killed instanceof Squid)
				return getMcMMOXP(killed, plugin.getConfigManager().squidMcMMOSkillRewardAmount);
			else if (killed instanceof Villager)
				return getMcMMOXP(killed, plugin.getConfigManager().villagerMcMMOSkillRewardAmount);
			else if (killed instanceof Wolf)
				return getMcMMOXP(killed, plugin.getConfigManager().wolfMcMMOSkillRewardAmount);
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return getMcMMOXP(killed, plugin.getConfigManager().rawfishMcMMOSkillRewardAmount);
				} else if (is.getData().getData() == (byte) 1) {
					return getMcMMOXP(killed, plugin.getConfigManager().rawsalmonMcMMOSkillRewardAmount);
				} else if (is.getData().getData() == (byte) 2) {
					return getMcMMOXP(killed, plugin.getConfigManager().clownfishMcMMOSkillRewardAmount);
				} else if (is.getData().getData() == (byte) 3) {
					return getMcMMOXP(killed, plugin.getConfigManager().pufferfishMcMMOSkillRewardAmount);
				}
			}
		}
		return 0;
	}
	
	public boolean getMobEnabled(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).isMobEnabled();
			return false;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).isMobEnabled();
			return false;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).isMobEnabled();
			}
			return false;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).isMobEnabled();
			return false;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).isMobEnabled();
			return false;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
						.isMobEnabled();
			return false;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).isMobEnabled();
			return false;

		} else if (MyPetCompat.isMyPet(killed)) {
			return plugin.getConfigManager().wolfEnabled;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return plugin.getConfigManager().parrotEnabled;
				else if (killed instanceof Illusioner)
					return plugin.getConfigManager().illusionerEnabled;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return plugin.getConfigManager().llamaEnabled;
				else if (killed instanceof Vex)
					return plugin.getConfigManager().vexEnabled;
				else if (killed instanceof Vindicator)
					return plugin.getConfigManager().vindicatorEnabled;
				else if (killed instanceof Evoker)
					return plugin.getConfigManager().evokerEnabled;
				else if (killed instanceof Donkey)
					return plugin.getConfigManager().donkeyEnabled;
				else if (killed instanceof Mule)
					return plugin.getConfigManager().muleEnabled;
				else if (killed instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonHorseEnabled;
				else if (killed instanceof ZombieHorse)
					return plugin.getConfigManager().zombieHorseEnabled;
				else if (killed instanceof Stray)
					return plugin.getConfigManager().strayEnabled;
				else if (killed instanceof Husk)
					return plugin.getConfigManager().huskEnabled;
				else if (killed instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitEnabled;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return plugin.getConfigManager().polarBearEnabled;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayEnabled;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskEnabled;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianEnabled;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerEnabled;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return plugin.getConfigManager().shulkerEnabled;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return plugin.getConfigManager().elderGuardianEnabled;
				else if (killed instanceof Guardian)
					return plugin.getConfigManager().guardianEnabled;
				else if (killed instanceof Endermite)
					return plugin.getConfigManager().endermiteEnabled;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerRabbitEnabled;
					else
						return plugin.getConfigManager().rabbitEnabled;

			// MC1.7 or older
			if (killed instanceof Player) {
				return plugin.getConfigManager().pvpAllowed;
			} else if (killed instanceof Blaze)
				return plugin.getConfigManager().blazeEnabled;
			else if (killed instanceof Creeper)
				return plugin.getConfigManager().creeperEnabled;
			else if (killed instanceof Silverfish)
				return plugin.getConfigManager().silverfishEnabled;
			else if (killed instanceof Enderman)
				return plugin.getConfigManager().endermanEnabled;
			else if (killed instanceof Giant)
				return plugin.getConfigManager().giantEnabled;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonEnabled;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonEnabled;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return plugin.getConfigManager().caveSpiderEnabled;
			else if (killed instanceof Spider)
				return plugin.getConfigManager().spiderEnabled;
			else if (killed instanceof Witch)
				return plugin.getConfigManager().witchEnabled;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return plugin.getConfigManager().zombiePigmanEnabled;
			else if (killed instanceof Zombie)
				return plugin.getConfigManager().zombieEnabled;
			else if (killed instanceof Ghast)
				return plugin.getConfigManager().ghastEnabled;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeEnabled;
			else if (killed instanceof Slime)
				return plugin.getConfigManager().slimeEnabled;
			else if (killed instanceof EnderDragon)
				return plugin.getConfigManager().enderDragonEnabled;
			else if (killed instanceof Wither)
				return plugin.getConfigManager().witherEnabled;
			else if (killed instanceof IronGolem)
				return plugin.getConfigManager().ironGolemEnabled;

			// Passive mobs
			else if (killed instanceof Bat)
				return plugin.getConfigManager().batEnabled;
			else if (killed instanceof Chicken)
				return plugin.getConfigManager().chickenEnabled;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return plugin.getConfigManager().mushroomCowEnabled;
				else
					return plugin.getConfigManager().cowEnabled;
			else if (killed instanceof Horse)
				return plugin.getConfigManager().horseEnabled;
			else if (killed instanceof Ocelot)
				return plugin.getConfigManager().ocelotEnabled;
			else if (killed instanceof Pig)
				return plugin.getConfigManager().pigEnabled;
			else if (killed instanceof Sheep)
				return plugin.getConfigManager().sheepEnabled;
			else if (killed instanceof Snowman)
				return plugin.getConfigManager().snowmanEnabled;
			else if (killed instanceof Squid)
				return plugin.getConfigManager().squidEnabled;
			else if (killed instanceof Villager)
				return plugin.getConfigManager().villagerEnabled;
			else if (killed instanceof Wolf)
				return plugin.getConfigManager().wolfEnabled;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishEnabled;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonEnabled;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishEnabled;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishEnabled;
				}
			}
		}
		return false;
	}

	public boolean getHeadDropHead(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return false;
				//return TARDISWeepingAngelsCompat.getMobRewardData()
				//		.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).getMobEnabled();
			return false;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return false;
				//return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).isMobEnabled();
			return false;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return false;
				//return CitizensCompat.getMobRewardData().get(key).isMobEnabled();
			}
			return  false;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return  false;
				//return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).isMobEnabled();
			return  false;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return  false;
				//return MysteriousHalloweenCompat.getMobRewardData()
				//		.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).isMobEnabled();
			return  false;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return  false;
				//return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
				//		.isMobEnabled();
			return  false;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return  false;
				//return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).isMobEnabled();
			return  false;

		} else if (MyPetCompat.isMyPet(killed)) {
			return  plugin.getConfigManager().wolfHeadDropHead;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return  plugin.getConfigManager().parrotHeadDropHead;
				else if (killed instanceof Illusioner)
					return  plugin.getConfigManager().illusionerHeadDropHead;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return  plugin.getConfigManager().llamaHeadDropHead;
				else if (killed instanceof Vex)
					return  plugin.getConfigManager().vexHeadDropHead;
				else if (killed instanceof Vindicator)
					return  plugin.getConfigManager().vindicatorHeadDropHead;
				else if (killed instanceof Evoker)
					return  plugin.getConfigManager().evokerHeadDropHead;
				else if (killed instanceof Donkey)
					return  plugin.getConfigManager().donkeyHeadDropHead;
				else if (killed instanceof Mule)
					return  plugin.getConfigManager().muleHeadDropHead;
				else if (killed instanceof SkeletonHorse)
					return  plugin.getConfigManager().skeletonHorseHeadDropHead;
				else if (killed instanceof ZombieHorse)
					return  plugin.getConfigManager().zombieHorseHeadDropHead;
				else if (killed instanceof Stray)
					return  plugin.getConfigManager().strayHeadDropHead;
				else if (killed instanceof Husk)
					return  plugin.getConfigManager().huskHeadDropHead;
				else if (killed instanceof ZombieVillager)
					return  plugin.getConfigManager().zombieVillagerHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return  plugin.getConfigManager().nitwitHeadDropHead;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return  plugin.getConfigManager().polarBearHeadDropHead;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return  plugin.getConfigManager().strayHeadDropHead;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return  plugin.getConfigManager().huskHeadDropHead;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return  plugin.getConfigManager().villagerHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return  plugin.getConfigManager().priestHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return  plugin.getConfigManager().butcherHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return  plugin.getConfigManager().blacksmithHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return  plugin.getConfigManager().librarianHeadDropHead;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return  plugin.getConfigManager().farmerHeadDropHead;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return  plugin.getConfigManager().shulkerHeadDropHead;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return  plugin.getConfigManager().elderGuardianHeadDropHead;
				else if (killed instanceof Guardian)
					return  plugin.getConfigManager().guardianHeadDropHead;
				else if (killed instanceof Endermite)
					return  plugin.getConfigManager().endermiteHeadDropHead;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return  plugin.getConfigManager().killerRabbitHeadDropHead;
					else
						return  plugin.getConfigManager().rabbitHeadDropHead;

			// MC1.7 or older
			if (killed instanceof Player) {
				return  plugin.getConfigManager().pvpHeadDropHead;
			} else if (killed instanceof Blaze)
				return  plugin.getConfigManager().blazeHeadDropHead;
			else if (killed instanceof Creeper)
				return  plugin.getConfigManager().creeperHeadDropHead;
			else if (killed instanceof Silverfish)
				return  plugin.getConfigManager().silverfishHeadDropHead;
			else if (killed instanceof Enderman)
				return  plugin.getConfigManager().endermanHeadDropHead;
			else if (killed instanceof Giant)
				return  plugin.getConfigManager().giantHeadDropHead;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return  plugin.getConfigManager().skeletonHeadDropHead;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return  plugin.getConfigManager().witherSkeletonHeadDropHead;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return  plugin.getConfigManager().caveSpiderHeadDropHead;
			else if (killed instanceof Spider)
				return  plugin.getConfigManager().spiderHeadDropHead;
			else if (killed instanceof Witch)
				return  plugin.getConfigManager().witchHeadDropHead;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return  plugin.getConfigManager().zombiePigmanHeadDropHead;
			else if (killed instanceof Zombie)
				return  plugin.getConfigManager().zombieHeadDropHead;
			else if (killed instanceof Ghast)
				return  plugin.getConfigManager().ghastHeadDropHead;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return  plugin.getConfigManager().magmaCubeHeadDropHead;
			else if (killed instanceof Slime)
				return  plugin.getConfigManager().slimeHeadDropHead;
			else if (killed instanceof EnderDragon)
				return  plugin.getConfigManager().enderDragonHeadDropHead;
			else if (killed instanceof Wither)
				return  plugin.getConfigManager().witherHeadDropHead;
			else if (killed instanceof IronGolem)
				return  plugin.getConfigManager().ironGolemHeadDropHead;

			// Passive mobs
			else if (killed instanceof Bat)
				return  plugin.getConfigManager().batHeadDropHead;
			else if (killed instanceof Chicken)
				return  plugin.getConfigManager().chickenHeadDropHead;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return  plugin.getConfigManager().mushroomCowHeadDropHead;
				else
					return  plugin.getConfigManager().cowHeadDropHead;
			else if (killed instanceof Horse)
				return  plugin.getConfigManager().horseHeadDropHead;
			else if (killed instanceof Ocelot)
				return  plugin.getConfigManager().ocelotHeadDropHead;
			else if (killed instanceof Pig)
				return  plugin.getConfigManager().pigHeadDropHead;
			else if (killed instanceof Sheep)
				return  plugin.getConfigManager().sheepHeadDropHead;
			else if (killed instanceof Snowman)
				return  plugin.getConfigManager().snowmanHeadDropHead;
			else if (killed instanceof Squid)
				return  plugin.getConfigManager().squidHeadDropHead;
			else if (killed instanceof Villager)
				return  plugin.getConfigManager().villagerHeadDropHead;
			else if (killed instanceof Wolf)
				return  plugin.getConfigManager().wolfHeadDropHead;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return  plugin.getConfigManager().rawFishHeadDropHead;
				} else if (is.getData().getData() == (byte) 1) {
					return  plugin.getConfigManager().rawSalmonHeadDropHead;
				} else if (is.getData().getData() == (byte) 2) {
					return  plugin.getConfigManager().clownfishHeadDropHead;
				} else if (is.getData().getData() == (byte) 3) {
					return  plugin.getConfigManager().pufferfishHeadDropHead;
				}
			}
		}
		return false;
	}
	
	public double getHeadDropChance(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return 0;
				//return TARDISWeepingAngelsCompat.getMobRewardData()
				//		.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).getMobEnabled();
			return 0;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return 0;
				//return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).isMobEnabled();
			return 0;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return 0;
				//return CitizensCompat.getMobRewardData().get(key).isMobEnabled();
			}
			return  0;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return  0;
				//return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).isMobEnabled();
			return  0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return  0;
				//return MysteriousHalloweenCompat.getMobRewardData()
				//		.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).isMobEnabled();
			return  0;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return  0;
				//return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
				//		.isMobEnabled();
			return  0;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return  0;
				//return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).isMobEnabled();
			return  0;

		} else if (MyPetCompat.isMyPet(killed)) {
			return  plugin.getConfigManager().wolfHeadDropChance;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return  plugin.getConfigManager().parrotHeadDropChance;
				else if (killed instanceof Illusioner)
					return  plugin.getConfigManager().illusionerHeadDropChance;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return  plugin.getConfigManager().llamaHeadDropChance;
				else if (killed instanceof Vex)
					return  plugin.getConfigManager().vexHeadDropChance;
				else if (killed instanceof Vindicator)
					return  plugin.getConfigManager().vindicatorHeadDropChance;
				else if (killed instanceof Evoker)
					return  plugin.getConfigManager().evokerHeadDropChance;
				else if (killed instanceof Donkey)
					return  plugin.getConfigManager().donkeyHeadDropChance;
				else if (killed instanceof Mule)
					return  plugin.getConfigManager().muleHeadDropChance;
				else if (killed instanceof SkeletonHorse)
					return  plugin.getConfigManager().skeletonHorseHeadDropChance;
				else if (killed instanceof ZombieHorse)
					return  plugin.getConfigManager().zombieHorseHeadDropChance;
				else if (killed instanceof Stray)
					return  plugin.getConfigManager().strayHeadDropChance;
				else if (killed instanceof Husk)
					return  plugin.getConfigManager().huskHeadDropChance;
				else if (killed instanceof ZombieVillager)
					return  plugin.getConfigManager().zombieVillagerHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return  plugin.getConfigManager().nitwitHeadDropChance;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return  plugin.getConfigManager().polarBearHeadDropChance;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return  plugin.getConfigManager().strayHeadDropChance;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return  plugin.getConfigManager().huskHeadDropChance;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return  plugin.getConfigManager().villagerHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return  plugin.getConfigManager().priestHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return  plugin.getConfigManager().butcherHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return  plugin.getConfigManager().blacksmithHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return  plugin.getConfigManager().librarianHeadDropChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return  plugin.getConfigManager().farmerHeadDropChance;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return  plugin.getConfigManager().shulkerHeadDropChance;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return  plugin.getConfigManager().elderGuardianHeadDropChance;
				else if (killed instanceof Guardian)
					return  plugin.getConfigManager().guardianHeadDropChance;
				else if (killed instanceof Endermite)
					return  plugin.getConfigManager().endermiteHeadDropChance;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return  plugin.getConfigManager().killerRabbitHeadDropChance;
					else
						return  plugin.getConfigManager().rabbitHeadDropChance;

			// MC1.7 or older
			if (killed instanceof Player) {
				return  plugin.getConfigManager().pvpHeadDropChance;
			} else if (killed instanceof Blaze)
				return  plugin.getConfigManager().blazeHeadDropChance;
			else if (killed instanceof Creeper)
				return  plugin.getConfigManager().creeperHeadDropChance;
			else if (killed instanceof Silverfish)
				return  plugin.getConfigManager().silverfishHeadDropChance;
			else if (killed instanceof Enderman)
				return  plugin.getConfigManager().endermanHeadDropChance;
			else if (killed instanceof Giant)
				return  plugin.getConfigManager().giantHeadDropChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return  plugin.getConfigManager().skeletonHeadDropChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return  plugin.getConfigManager().witherSkeletonHeadDropChance;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return  plugin.getConfigManager().caveSpiderHeadDropChance;
			else if (killed instanceof Spider)
				return  plugin.getConfigManager().spiderHeadDropChance;
			else if (killed instanceof Witch)
				return  plugin.getConfigManager().witchHeadDropChance;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return  plugin.getConfigManager().zombiePigmanHeadDropChance;
			else if (killed instanceof Zombie)
				return  plugin.getConfigManager().zombieHeadDropChance;
			else if (killed instanceof Ghast)
				return  plugin.getConfigManager().ghastHeadDropChance;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return  plugin.getConfigManager().magmaCubeHeadDropChance;
			else if (killed instanceof Slime)
				return  plugin.getConfigManager().slimeHeadDropChance;
			else if (killed instanceof EnderDragon)
				return  plugin.getConfigManager().enderDragonHeadDropChance;
			else if (killed instanceof Wither)
				return  plugin.getConfigManager().witherHeadDropChance;
			else if (killed instanceof IronGolem)
				return  plugin.getConfigManager().ironGolemHeadDropChance;

			// Passive mobs
			else if (killed instanceof Bat)
				return  plugin.getConfigManager().batHeadDropChance;
			else if (killed instanceof Chicken)
				return  plugin.getConfigManager().chickenHeadDropChance;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return  plugin.getConfigManager().mushroomCowHeadDropChance;
				else
					return  plugin.getConfigManager().cowHeadDropChance;
			else if (killed instanceof Horse)
				return  plugin.getConfigManager().horseHeadDropChance;
			else if (killed instanceof Ocelot)
				return  plugin.getConfigManager().ocelotHeadDropChance;
			else if (killed instanceof Pig)
				return  plugin.getConfigManager().pigHeadDropChance;
			else if (killed instanceof Sheep)
				return  plugin.getConfigManager().sheepHeadDropChance;
			else if (killed instanceof Snowman)
				return  plugin.getConfigManager().snowmanHeadDropChance;
			else if (killed instanceof Squid)
				return  plugin.getConfigManager().squidHeadDropChance;
			else if (killed instanceof Villager)
				return  plugin.getConfigManager().villagerHeadDropChance;
			else if (killed instanceof Wolf)
				return  plugin.getConfigManager().wolfHeadDropChance;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return  plugin.getConfigManager().rawFishHeadDropChance;
				} else if (is.getData().getData() == (byte) 1) {
					return  plugin.getConfigManager().rawSalmonHeadDropChance;
				} else if (is.getData().getData() == (byte) 2) {
					return  plugin.getConfigManager().clownfishHeadDropChance;
				} else if (is.getData().getData() == (byte) 3) {
					return  plugin.getConfigManager().pufferfishHeadDropChance;
				}
			}
		}
		return 0;
	}
	
	public String getHeadDropMessage(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return "";
				//return TARDISWeepingAngelsCompat.getMobRewardData()
				//		.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).getMobEnabled();
			return "";

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return "";
				//return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).isMobEnabled();
			return "";

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return "";
				//return CitizensCompat.getMobRewardData().get(key).isMobEnabled();
			}
			return  "";

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return  "";
				//return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).isMobEnabled();
			return  "";

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return  "";
				//return MysteriousHalloweenCompat.getMobRewardData()
				//		.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).isMobEnabled();
			return  "";

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return  "";
				//return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
				//		.isMobEnabled();
			return  "";

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return  "";
				//return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).isMobEnabled();
			return  "";

		} else if (MyPetCompat.isMyPet(killed)) {
			return  plugin.getConfigManager().wolfHeadMessage;

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return  plugin.getConfigManager().parrotHeadMessage;
				else if (killed instanceof Illusioner)
					return  plugin.getConfigManager().illusionerHeadMessage;

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return  plugin.getConfigManager().llamaHeadMessage;
				else if (killed instanceof Vex)
					return  plugin.getConfigManager().vexHeadMessage;
				else if (killed instanceof Vindicator)
					return  plugin.getConfigManager().vindicatorHeadMessage;
				else if (killed instanceof Evoker)
					return  plugin.getConfigManager().evokerHeadMessage;
				else if (killed instanceof Donkey)
					return  plugin.getConfigManager().donkeyHeadMessage;
				else if (killed instanceof Mule)
					return  plugin.getConfigManager().muleHeadMessage;
				else if (killed instanceof SkeletonHorse)
					return  plugin.getConfigManager().skeletonHorseHeadMessage;
				else if (killed instanceof ZombieHorse)
					return  plugin.getConfigManager().zombieHorseHeadMessage;
				else if (killed instanceof Stray)
					return  plugin.getConfigManager().strayHeadMessage;
				else if (killed instanceof Husk)
					return  plugin.getConfigManager().huskHeadMessage;
				else if (killed instanceof ZombieVillager)
					return  plugin.getConfigManager().zombieVillagerHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return  plugin.getConfigManager().nitwitHeadMessage;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return  plugin.getConfigManager().polarBearHeadMessage;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return  plugin.getConfigManager().strayHeadMessage;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return  plugin.getConfigManager().huskHeadMessage;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return  plugin.getConfigManager().villagerHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return  plugin.getConfigManager().priestHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return  plugin.getConfigManager().butcherHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return  plugin.getConfigManager().blacksmithHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return  plugin.getConfigManager().librarianHeadMessage;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return  plugin.getConfigManager().farmerHeadMessage;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return  plugin.getConfigManager().shulkerHeadMessage;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return  plugin.getConfigManager().elderGuardianHeadMessage;
				else if (killed instanceof Guardian)
					return  plugin.getConfigManager().guardianHeadMessage;
				else if (killed instanceof Endermite)
					return  plugin.getConfigManager().endermiteHeadMessage;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return  plugin.getConfigManager().killerRabbitHeadMessage;
					else
						return  plugin.getConfigManager().rabbitHeadMessage;

			// MC1.7 or older
			if (killed instanceof Player) {
				return  plugin.getConfigManager().pvpHeadMessage;
			} else if (killed instanceof Blaze)
				return  plugin.getConfigManager().blazeHeadMessage;
			else if (killed instanceof Creeper)
				return  plugin.getConfigManager().creeperHeadMessage;
			else if (killed instanceof Silverfish)
				return  plugin.getConfigManager().silverfishHeadMessage;
			else if (killed instanceof Enderman)
				return  plugin.getConfigManager().endermanHeadMessage;
			else if (killed instanceof Giant)
				return  plugin.getConfigManager().giantHeadMessage;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return  plugin.getConfigManager().skeletonHeadMessage;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return  plugin.getConfigManager().witherSkeletonHeadMessage;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return  plugin.getConfigManager().caveSpiderHeadMessage;
			else if (killed instanceof Spider)
				return  plugin.getConfigManager().spiderHeadMessage;
			else if (killed instanceof Witch)
				return  plugin.getConfigManager().witchHeadMessage;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return  plugin.getConfigManager().zombiePigmanHeadMessage;
			else if (killed instanceof Zombie)
				return  plugin.getConfigManager().zombieHeadMessage;
			else if (killed instanceof Ghast)
				return  plugin.getConfigManager().ghastHeadMessage;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return  plugin.getConfigManager().magmaCubeHeadMessage;
			else if (killed instanceof Slime)
				return  plugin.getConfigManager().slimeHeadMessage;
			else if (killed instanceof EnderDragon)
				return  plugin.getConfigManager().enderDragonHeadMessage;
			else if (killed instanceof Wither)
				return  plugin.getConfigManager().witherHeadMessage;
			else if (killed instanceof IronGolem)
				return  plugin.getConfigManager().ironGolemHeadMessage;

			// Passive mobs
			else if (killed instanceof Bat)
				return  plugin.getConfigManager().batHeadMessage;
			else if (killed instanceof Chicken)
				return  plugin.getConfigManager().chickenHeadMessage;
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return  plugin.getConfigManager().mushroomCowHeadMessage;
				else
					return  plugin.getConfigManager().cowHeadMessage;
			else if (killed instanceof Horse)
				return  plugin.getConfigManager().horseHeadMessage;
			else if (killed instanceof Ocelot)
				return  plugin.getConfigManager().ocelotHeadMessage;
			else if (killed instanceof Pig)
				return  plugin.getConfigManager().pigHeadMessage;
			else if (killed instanceof Sheep)
				return  plugin.getConfigManager().sheepHeadMessage;
			else if (killed instanceof Snowman)
				return  plugin.getConfigManager().snowmanHeadMessage;
			else if (killed instanceof Squid)
				return  plugin.getConfigManager().squidHeadMessage;
			else if (killed instanceof Villager)
				return  plugin.getConfigManager().villagerHeadMessage;
			else if (killed instanceof Wolf)
				return  plugin.getConfigManager().wolfHeadMessage;
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return  plugin.getConfigManager().rawFishHeadMessage;
				} else if (is.getData().getData() == (byte) 1) {
					return  plugin.getConfigManager().rawSalmonHeadMessage;
				} else if (is.getData().getData() == (byte) 2) {
					return  plugin.getConfigManager().clownfishHeadMessage;
				} else if (is.getData().getData() == (byte) 3) {
					return  plugin.getConfigManager().pufferfishHeadMessage;
				}
			}
		}
		return "";
	}
	
	public double getHeadValue(Entity killed) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(killed)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()))
				return 0;
				//return TARDISWeepingAngelsCompat.getMobRewardData()
				//		.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(killed).name()).getMobEnabled();
			return 0;

		} else if (MythicMobsCompat.isMythicMob(killed)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(killed)))
				return 0;
				//return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(killed)).isMobEnabled();
			return 0;

		} else if (CitizensCompat.isNPC(killed) && CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			NPCRegistry registry = CitizensAPI.getNPCRegistry();
			NPC npc = registry.getNPC(killed);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return 0;
				//return CitizensCompat.getMobRewardData().get(key).isMobEnabled();
			}
			return 0;

		} else if (CustomMobsCompat.isCustomMob(killed)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(killed)))
				return 0;
				//return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(killed)).isMobEnabled();
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(killed)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()))
				return 0;
				//return MysteriousHalloweenCompat.getMobRewardData()
				//		.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(killed).name()).isMobEnabled();
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(killed)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(killed)))
				return 0;
				//return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(killed))
				//		.isMobEnabled();
			return 0;

		} else if (HerobrineCompat.isHerobrineMob(killed)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(killed)))
				return 0;
				//return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(killed)).isMobEnabled();
			return 0;

		} else if (MyPetCompat.isMyPet(killed)) {
			return getPrice(killed, plugin.getConfigManager().wolfHeadPrize);

		} else {
			if (Misc.isMC112OrNewer())
				if (killed instanceof Parrot)
					return getPrice(killed, plugin.getConfigManager().parrotHeadPrize);
				else if (killed instanceof Illusioner)
					return getPrice(killed, plugin.getConfigManager().illusionerHeadPrize);

			if (Misc.isMC111OrNewer())
				if (killed instanceof Llama)
					return getPrice(killed, plugin.getConfigManager().llamaHeadPrize);
				else if (killed instanceof Vex)
					return getPrice(killed, plugin.getConfigManager().vexHeadPrize);
				else if (killed instanceof Vindicator)
					return getPrice(killed, plugin.getConfigManager().vindicatorHeadPrize);
				else if (killed instanceof Evoker)
					return getPrice(killed, plugin.getConfigManager().evokerHeadPrize);
				else if (killed instanceof Donkey)
					return getPrice(killed, plugin.getConfigManager().donkeyHeadPrize);
				else if (killed instanceof Mule)
					return getPrice(killed, plugin.getConfigManager().muleHeadPrize);
				else if (killed instanceof SkeletonHorse)
					return getPrice(killed, plugin.getConfigManager().skeletonHorseHeadPrize);
				else if (killed instanceof ZombieHorse)
					return getPrice(killed, plugin.getConfigManager().zombieHorseHeadPrize);
				else if (killed instanceof Stray)
					return getPrice(killed, plugin.getConfigManager().strayHeadPrize);
				else if (killed instanceof Husk)
					return getPrice(killed, plugin.getConfigManager().huskHeadPrize);
				else if (killed instanceof ZombieVillager)
					return getPrice(killed, plugin.getConfigManager().zombieVillagerHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return getPrice(killed, plugin.getConfigManager().nitwitHeadPrize);

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return getPrice(killed, plugin.getConfigManager().polarBearHeadPrize);
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return getPrice(killed, plugin.getConfigManager().strayHeadPrize);
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return getPrice(killed, plugin.getConfigManager().huskHeadPrize);

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return getPrice(killed, plugin.getConfigManager().villagerHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return getPrice(killed, plugin.getConfigManager().priestHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return getPrice(killed, plugin.getConfigManager().butcherHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return getPrice(killed, plugin.getConfigManager().blacksmithHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return getPrice(killed, plugin.getConfigManager().librarianHeadPrize);
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return getPrice(killed, plugin.getConfigManager().farmerHeadPrize);

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return getPrice(killed, plugin.getConfigManager().shulkerHeadPrize);

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return getPrice(killed, plugin.getConfigManager().elderGuardianHeadPrize);
				else if (killed instanceof Guardian)
					return getPrice(killed, plugin.getConfigManager().guardianHeadPrize);
				else if (killed instanceof Endermite)
					return getPrice(killed, plugin.getConfigManager().endermiteHeadPrize);
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return getPrice(killed, plugin.getConfigManager().killerRabbitHeadPrize);
					else
						return getPrice(killed, plugin.getConfigManager().rabbitHeadPrize);

			// MC1.7 or older
			if (killed instanceof Player) {
				return getPrice(killed, plugin.getConfigManager().pvpHeadPrize);
			} else if (killed instanceof Blaze)
				return getPrice(killed, plugin.getConfigManager().blazeHeadPrize);
			else if (killed instanceof Creeper)
				return getPrice(killed, plugin.getConfigManager().creeperHeadPrize);
			else if (killed instanceof Silverfish)
				return getPrice(killed, plugin.getConfigManager().silverfishHeadPrize);
			else if (killed instanceof Enderman)
				return getPrice(killed, plugin.getConfigManager().endermanHeadPrize);
			else if (killed instanceof Giant)
				return getPrice(killed, plugin.getConfigManager().giantHeadPrize);
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return getPrice(killed, plugin.getConfigManager().skeletonHeadPrize);
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return getPrice(killed, plugin.getConfigManager().witherSkeletonHeadPrize);
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return getPrice(killed, plugin.getConfigManager().caveSpiderHeadPrize);
			else if (killed instanceof Spider)
				return getPrice(killed, plugin.getConfigManager().spiderHeadPrize);
			else if (killed instanceof Witch)
				return getPrice(killed, plugin.getConfigManager().witchHeadPrize);
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return getPrice(killed, plugin.getConfigManager().zombiePigmanHeadPrize);
			else if (killed instanceof Zombie)
				return getPrice(killed, plugin.getConfigManager().zombieHeadPrize);
			else if (killed instanceof Ghast)
				return getPrice(killed, plugin.getConfigManager().ghastHeadPrize);
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return getPrice(killed, plugin.getConfigManager().magmaCubeHeadPrize);
			else if (killed instanceof Slime)
				return getPrice(killed, plugin.getConfigManager().slimeHeadPrize);
			else if (killed instanceof EnderDragon)
				return getPrice(killed, plugin.getConfigManager().enderDragonHeadPrize);
			else if (killed instanceof Wither)
				return getPrice(killed, plugin.getConfigManager().witherHeadPrize);
			else if (killed instanceof IronGolem)
				return getPrice(killed, plugin.getConfigManager().ironGolemHeadPrize);

			// Passive mobs
			else if (killed instanceof Bat)
				return getPrice(killed, plugin.getConfigManager().batHeadPrize);
			else if (killed instanceof Chicken)
				return getPrice(killed, plugin.getConfigManager().chickenHeadPrize);
			else if (killed instanceof Cow)
				if (killed instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return getPrice(killed, plugin.getConfigManager().mushroomCowHeadPrize);
				else
					return getPrice(killed, plugin.getConfigManager().cowHeadPrize);
			else if (killed instanceof Horse)
				return getPrice(killed, plugin.getConfigManager().horseHeadPrize);
			else if (killed instanceof Ocelot)
				return getPrice(killed, plugin.getConfigManager().ocelotHeadPrize);
			else if (killed instanceof Pig)
				return getPrice(killed, plugin.getConfigManager().pigHeadPrize);
			else if (killed instanceof Sheep)
				return getPrice(killed, plugin.getConfigManager().sheepHeadPrize);
			else if (killed instanceof Snowman)
				return getPrice(killed, plugin.getConfigManager().snowmanHeadPrize);
			else if (killed instanceof Squid)
				return getPrice(killed, plugin.getConfigManager().squidHeadPrize);
			else if (killed instanceof Villager)
				return getPrice(killed, plugin.getConfigManager().villagerHeadPrize);
			else if (killed instanceof Wolf)
				return getPrice(killed, plugin.getConfigManager().wolfHeadPrize);
			else if (killed instanceof Item && ((Item) killed).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) killed).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return getPrice(killed, plugin.getConfigManager().rawFishHeadPrize);
				} else if (is.getData().getData() == (byte) 1) {
					return getPrice(killed, plugin.getConfigManager().rawSalmonHeadPrize);
				} else if (is.getData().getData() == (byte) 2) {
					return getPrice(killed, plugin.getConfigManager().clownfishHeadPrize);
				} else if (is.getData().getData() == (byte) 3) {
					return getPrice(killed, plugin.getConfigManager().pufferfishHeadPrize);
				}
			}
		}
		return 0;
	}

}
