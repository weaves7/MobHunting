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
import org.bukkit.GameMode;
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
import one.lindegaard.MobHunting.Messages;
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
			Bukkit.getLogger().severe(Messages.getString(plugin.getName().toLowerCase() + ".hook.econ"));
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}

		mEconomy = economyProvider.getProvider();

		Messages.debug("MobHunting is using %s as Economy Provider", mEconomy.getName());
		Messages.debug("Number of Economy Providers = %s",
				Bukkit.getServicesManager().getRegistrations(Economy.class).size());
		if (Bukkit.getServicesManager().getRegistrations(Economy.class).size() > 1) {
			for (RegisteredServiceProvider<Economy> registation : Bukkit.getServicesManager()
					.getRegistrations(Economy.class)) {
				Messages.debug("Provider name=%s", registation.getProvider().getName());
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
		if (BagOfGoldCompat.isSupported())
			return mEconomy.format(Misc.round(amount));
		else
			return Misc.format(amount);
	}

	public double getBalance(OfflinePlayer offlinePlayer) {
		if (BagOfGoldCompat.isSupported())
			return mEconomy.getBalance(offlinePlayer);
		else if (offlinePlayer.isOnline()) {
			Player player = (Player) offlinePlayer;
			double amountInInventory = 0;
			for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
				ItemStack is = player.getInventory().getItem(slot);
				if (Reward.isReward(is)) {
					Reward reward = Reward.getReward(is);
					if (reward.isBagOfGoldReward())
						amountInInventory = amountInInventory + reward.getMoney();
				}
			}
			return amountInInventory;
		} else {
			return 0;
		}
	}

	public boolean has(OfflinePlayer offlinePlayer, double amount) {
		return mEconomy.has(offlinePlayer, amount);
	}

	public boolean addBagOfGoldPlayer_RewardManager(Player player, double amount) {
		//if (player.getGameMode() != GameMode.SURVIVAL){
		//	Messages.debug("Player is not in Survival mode, adjusting amount to 0");
		//	amount = 0;
		//}
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
							? plugin.getRewardManager().format(rewardInSlot.getMoney())
							: rewardInSlot.getDisplayname() + " ("
									+ plugin.getRewardManager().format(rewardInSlot.getMoney()) + ")";
					im.setDisplayName(
							ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + displayName);
					is.setItemMeta(im);
					is.setAmount(1);
					Messages.debug("Added %s to item in slot %s, new value is %s (addBagOfGoldPlayer_RewardManager)",
							plugin.getRewardManager().format(amount), slot,
							plugin.getRewardManager().format(rewardInSlot.getMoney()));
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
							new ItemStack(Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem)), plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
							amount, UUID.fromString(Reward.MH_REWARD_ITEM_UUID), null);
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
		}
		return false;
	}

	public double removeBagOfGoldPlayer_RewardManager(Player player, double amount) {
		//if (player.getGameMode() != GameMode.SURVIVAL){
		//	Messages.debug("Player is not in Survival mode, adjusting amount to 0");
		//	amount = 0;
		//}
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
						is = customItems.getCustomtexture(reward.getRewardUUID(),
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
						+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
								? plugin.getRewardManager().format(money)
								: Reward.getReward(is).getDisplayname() + " (" + plugin.getRewardManager().format(money)
										+ ")"));
				item.setCustomNameVisible(true);
			}
		}
		if (item != null)
			Messages.debug("%s was dropped on the ground as item %s (# of rewards=%s)", format(money),
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
					Messages.debug("Saving a reward placed as a block.");
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
				Messages.debug("Deleted %s rewards from the rewards.yml file", deleted);
				File file_copy = new File(MobHunting.getInstance().getDataFolder(), "rewards.yml.old");
				Files.copy(file.toPath(), file_copy.toPath(), StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				config.save(file);
			}
			if (n > 0) {
				Messages.debug("Loaded %s rewards from the rewards.yml file", n);
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
				|| plugin.getConfigManager().mobKillsPlayerPenalty.equals("")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.equals("0%")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.equals("0")
				|| plugin.getConfigManager().mobKillsPlayerPenalty.isEmpty()) {
			return 0;
		} else if (plugin.getConfigManager().mobKillsPlayerPenalty.contains(":")) {
			String[] str1 = plugin.getConfigManager().mobKillsPlayerPenalty.split(":");
			double prize = (plugin.getMobHuntingManager().mRand.nextDouble()
					* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
			return Misc.round(prize);
		} else if (plugin.getConfigManager().mobKillsPlayerPenalty.endsWith("%")) {
			double prize = Math.floor(Double
					.valueOf(plugin.getConfigManager().mobKillsPlayerPenalty.substring(0,
							plugin.getConfigManager().mobKillsPlayerPenalty.length() - 1))
					* plugin.getRewardManager().getBalance(playerToBeRobbed) / 100);
			return Misc.round(prize);
		} else if (plugin.getConfigManager().mobKillsPlayerPenalty.contains(":")) {
			String[] str1 = plugin.getConfigManager().mobKillsPlayerPenalty.split(":");
			double prize2 = (plugin.getMobHuntingManager().mRand.nextDouble()
					* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
			return Misc.round(Double.valueOf(prize2));
		} else
			return Double.valueOf(plugin.getConfigManager().mobKillsPlayerPenalty);
	}

	public double getRandomPrice(String str) {
		if (str == null || str.equals("") || str.isEmpty()) {
			Bukkit.getServer().getConsoleSender().sendMessage(ChatColor.RED + "[MobHunting] [WARNING]" + ChatColor.RESET
					+ " The random_bounty_prize is not set in config.yml. Please set the prize to 0 or a positive number.");
			return 0;
		} else if (str.contains(":")) {
			String[] str1 = str.split(":");
			double prize = (plugin.getMobHuntingManager().mRand.nextDouble()
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
			Messages.debug("TARDISWeepingAngel %s has no reward data",
					TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).getName());
			return 0;

		} else if (MythicMobsCompat.isMythicMob(mob)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(mob)))
				return getPrice(mob, MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(mob))
						.getRewardPrize());
			Messages.debug("MythicMob %s has no reward data", MythicMobsCompat.getMythicMobType(mob));
			return 0;

		} else if (CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(mob);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return getPrice(mob, CitizensCompat.getMobRewardData().get(key).getRewardPrize());
			}
			Messages.debug("Citizens mob %s has no reward data", npc.getName());
			return 0;

		} else if (CustomMobsCompat.isCustomMob(mob)) {
			if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(mob)))
				return getPrice(mob, CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(mob))
						.getRewardPrize());
			Messages.debug("CustomMob %s has no reward data", CustomMobsCompat.getCustomMobType(mob));
			return 0;

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()))
				return getPrice(mob, MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()).getRewardPrize());
			Messages.debug("MysteriousHalloween %s has no reward data",
					MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name());
			return 0;

		} else if (SmartGiantsCompat.isSmartGiants(mob)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(mob)))
				return getPrice(mob, SmartGiantsCompat.getMobRewardData()
						.get(SmartGiantsCompat.getSmartGiantsMobType(mob)).getRewardPrize());
			Messages.debug("SmartGiantsS %s has no reward data", SmartGiantsCompat.getSmartGiantsMobType(mob));
			return 0;

		} else if (MyPetCompat.isMyPet(mob)) {
			Messages.debug("Tried to find a prize for a MyPet: %s (Owner=%s)", MyPetCompat.getMyPet(mob),
					MyPetCompat.getMyPetOwner(mob));
			return getPrice(mob, plugin.getConfigManager().wolfPrize);

		} else if (HerobrineCompat.isHerobrineMob(mob)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(mob)))
				return getPrice(mob, HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(mob))
						.getRewardPrize());
			Messages.debug("Herobrine mob %s has no reward data", HerobrineCompat.getHerobrineMobType(mob));
			return 0;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return getPrice(mob, plugin.getConfigManager().parrotPrize);
				else if (mob instanceof Illusioner)
					return getPrice(mob, plugin.getConfigManager().illusionerPrize);

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return getPrice(mob, plugin.getConfigManager().llamaPrize);
				else if (mob instanceof Vex)
					return getPrice(mob, plugin.getConfigManager().vexPrize);
				else if (mob instanceof Vindicator)
					return getPrice(mob, plugin.getConfigManager().vindicatorPrize);
				else if (mob instanceof Evoker)
					return getPrice(mob, plugin.getConfigManager().evokerPrize);
				else if (mob instanceof Donkey)
					return getPrice(mob, plugin.getConfigManager().donkeyPrize);
				else if (mob instanceof Mule)
					return getPrice(mob, plugin.getConfigManager().mulePrize);
				else if (mob instanceof SkeletonHorse)
					return getPrice(mob, plugin.getConfigManager().skeletonhorsePrize);
				else if (mob instanceof ZombieHorse)
					return getPrice(mob, plugin.getConfigManager().zombiehorsePrize);
				else if (mob instanceof Stray)
					return getPrice(mob, plugin.getConfigManager().strayPrize);
				else if (mob instanceof Husk)
					return getPrice(mob, plugin.getConfigManager().huskPrize);
				else if (mob instanceof ZombieVillager)
					return getPrice(mob, plugin.getConfigManager().zombieVillagerPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return getPrice(mob, plugin.getConfigManager().nitwitPrize);

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return getPrice(mob, plugin.getConfigManager().polarBearPrize);
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return getPrice(mob, plugin.getConfigManager().strayPrize);
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return getPrice(mob, plugin.getConfigManager().huskPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return getPrice(mob, plugin.getConfigManager().villagerPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return getPrice(mob, plugin.getConfigManager().priestPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return getPrice(mob, plugin.getConfigManager().butcherPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return getPrice(mob, plugin.getConfigManager().blacksmithPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return getPrice(mob, plugin.getConfigManager().librarianPrize);
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return getPrice(mob, plugin.getConfigManager().farmerPrize);

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return getPrice(mob, plugin.getConfigManager().shulkerPrize);

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return getPrice(mob, plugin.getConfigManager().elderGuardianPrize);
				else if (mob instanceof Guardian)
					return getPrice(mob, plugin.getConfigManager().guardianPrize);
				else if (mob instanceof Endermite)
					return getPrice(mob, plugin.getConfigManager().endermitePrize);
				else if (mob instanceof Rabbit)
					if (((Rabbit) mob).getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY)
						return getPrice(mob, plugin.getConfigManager().killerrabbitPrize);
					else
						return getPrice(mob, plugin.getConfigManager().rabbitPrize);

			// Minecraft 1.7.10 and older entities
			if (mob instanceof Player) {
				if (plugin.getConfigManager().pvpKillPrize.trim().endsWith("%")) {
					Messages.debug("PVP kill reward is '%s'", plugin.getConfigManager().pvpKillPrize);
					double prize = Math.floor(Double
							.valueOf(plugin.getConfigManager().pvpKillPrize.substring(0,
									plugin.getConfigManager().pvpKillPrize.length() - 1))
							* plugin.getRewardManager().getBalance((Player) mob) / 100);
					return Misc.round(prize);
				} else if (plugin.getConfigManager().pvpKillPrize.contains(":")) {
					String[] str1 = plugin.getConfigManager().pvpKillPrize.split(":");
					double prize2 = (plugin.getMobHuntingManager().mRand.nextDouble()
							* (Double.valueOf(str1[1]) - Double.valueOf(str1[0])) + Double.valueOf(str1[0]));
					return Misc.round(Double.valueOf(prize2));
				} else
					return Double.valueOf(plugin.getConfigManager().pvpKillPrize);
			} else if (mob instanceof Blaze)
				return getPrice(mob, plugin.getConfigManager().blazePrize);
			else if (mob instanceof Creeper)
				return getPrice(mob, plugin.getConfigManager().creeperPrize);
			else if (mob instanceof Silverfish)
				return getPrice(mob, plugin.getConfigManager().silverfishPrize);
			else if (mob instanceof Enderman)
				return getPrice(mob, plugin.getConfigManager().endermanPrize);
			else if (mob instanceof Giant)
				return getPrice(mob, plugin.getConfigManager().giantPrize);
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return getPrice(mob, plugin.getConfigManager().skeletonPrize);
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return getPrice(mob, plugin.getConfigManager().witherSkeletonPrize);
			else if (mob instanceof CaveSpider)
				return getPrice(mob, plugin.getConfigManager().caveSpiderPrize);
			else if (mob instanceof Spider)
				return getPrice(mob, plugin.getConfigManager().spiderPrize);
			else if (mob instanceof Witch)
				return getPrice(mob, plugin.getConfigManager().witchPrize);
			else if (mob instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				if (((PigZombie) mob).isBaby())
					return Misc.round(getPrice(mob, plugin.getConfigManager().zombiePigmanPrize)
							* plugin.getConfigManager().babyMultiplier);
				else
					return getPrice(mob, plugin.getConfigManager().zombiePigmanPrize);
			else if (mob instanceof Zombie)
				if (((Zombie) mob).isBaby())
					return Misc.round(getPrice(mob, plugin.getConfigManager().zombiePrize)
							* plugin.getConfigManager().babyMultiplier);
				else
					return getPrice(mob, plugin.getConfigManager().zombiePrize);
			else if (mob instanceof Ghast)
				return getPrice(mob, plugin.getConfigManager().ghastPrize);
			else if (mob instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return getPrice(mob, plugin.getConfigManager().magmaCubePrize) * ((MagmaCube) mob).getSize();
			else if (mob instanceof Slime)
				return getPrice(mob, plugin.getConfigManager().slimeTinyPrize) * ((Slime) mob).getSize();
			else if (mob instanceof EnderDragon)
				return getPrice(mob, plugin.getConfigManager().enderdragonPrize);
			else if (mob instanceof Wither)
				return getPrice(mob, plugin.getConfigManager().witherPrize);
			else if (mob instanceof IronGolem)
				return getPrice(mob, plugin.getConfigManager().ironGolemPrize);

			// Passive mobs
			else if (mob instanceof Bat)
				return getPrice(mob, plugin.getConfigManager().batPrize);
			else if (mob instanceof Chicken)
				return getPrice(mob, plugin.getConfigManager().chickenPrize);
			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return getPrice(mob, plugin.getConfigManager().mushroomCowPrize);
				else
					return getPrice(mob, plugin.getConfigManager().cowPrize);
			else if (mob instanceof Horse)
				return getPrice(mob, plugin.getConfigManager().horsePrize);
			else if (mob instanceof Ocelot)
				return getPrice(mob, plugin.getConfigManager().ocelotPrize);
			else if (mob instanceof Pig)
				return getPrice(mob, plugin.getConfigManager().pigPrize);
			else if (mob instanceof Sheep)
				return getPrice(mob, plugin.getConfigManager().sheepPrize);
			else if (mob instanceof Snowman)
				return getPrice(mob, plugin.getConfigManager().snowmanPrize);
			else if (mob instanceof Squid)
				return getPrice(mob, plugin.getConfigManager().squidPrize);
			else if (mob instanceof Villager)
				return getPrice(mob, plugin.getConfigManager().villagerPrize);
			else if (mob instanceof Wolf) {
				return getPrice(mob, plugin.getConfigManager().wolfPrize);
			} else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return getPrice(mob, plugin.getConfigManager().rawFishPrize);
				} else if (is.getData().getData() == (byte) 1) {
					return getPrice(mob, plugin.getConfigManager().rawSalmonPrize);
				} else if (is.getData().getData() == (byte) 2) {
					return getPrice(mob, plugin.getConfigManager().clownfishPrize);
				} else if (is.getData().getData() == (byte) 3) {
					return getPrice(mob, plugin.getConfigManager().pufferfishPrize);
				}
			}
		}
		// Messages.debug("Mobhunting could not find the prize for killing this
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
			double prize = (plugin.getMobHuntingManager().mRand.nextDouble()
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
	public String getKillConsoleCmd(Entity mob) {
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(mob)) {
			if (TARDISWeepingAngelsCompat.getMobRewardData()
					.containsKey(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()))
				return TARDISWeepingAngelsCompat.getMobRewardData()
						.get(TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name()).getConsoleRunCommand();
			return "";

		} else if (MythicMobsCompat.isMythicMob(mob)) {
			if (MythicMobsCompat.getMobRewardData().containsKey(MythicMobsCompat.getMythicMobType(mob)))
				return MythicMobsCompat.getMobRewardData().get(MythicMobsCompat.getMythicMobType(mob))
						.getConsoleRunCommand();
			return "";

		} else if (CitizensCompat.isNPC(mob) && CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(mob);
			String key = String.valueOf(npc.getId());
			if (CitizensCompat.getMobRewardData().containsKey(key)) {
				return CitizensCompat.getMobRewardData().get(key).getConsoleRunCommand();
			}
			return "";

		} else if (CustomMobsCompat.isCustomMob(mob)) {
			if (mob.hasMetadata(CustomMobsCompat.MH_CUSTOMMOBS)) {
				List<MetadataValue> data = mob.getMetadata(CustomMobsCompat.MH_CUSTOMMOBS);
				for (MetadataValue value : data)
					if (value.value() instanceof RewardData)
						return ((RewardData) value.value()).getConsoleRunCommand();
			} else if (CustomMobsCompat.getMobRewardData().containsKey(CustomMobsCompat.getCustomMobType(mob)))
				return CustomMobsCompat.getMobRewardData().get(CustomMobsCompat.getCustomMobType(mob))
						.getConsoleRunCommand();
			return "";

		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			if (MysteriousHalloweenCompat.getMobRewardData()
					.containsKey(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()))
				return MysteriousHalloweenCompat.getMobRewardData()
						.get(MysteriousHalloweenCompat.getMysteriousHalloweenType(mob).name()).getConsoleRunCommand();
			return "";

		} else if (SmartGiantsCompat.isSmartGiants(mob)) {
			if (SmartGiantsCompat.getMobRewardData().containsKey(SmartGiantsCompat.getSmartGiantsMobType(mob)))
				return SmartGiantsCompat.getMobRewardData().get(SmartGiantsCompat.getSmartGiantsMobType(mob))
						.getConsoleRunCommand();
			return "";

		} else if (HerobrineCompat.isHerobrineMob(mob)) {
			if (HerobrineCompat.getMobRewardData().containsKey(HerobrineCompat.getHerobrineMobType(mob)))
				return HerobrineCompat.getMobRewardData().get(HerobrineCompat.getHerobrineMobType(mob))
						.getConsoleRunCommand();
			return "";

		} else if (MyPetCompat.isMyPet(mob)) {
			return plugin.getConfigManager().wolfCmd;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return plugin.getConfigManager().parrotCmd;
				else if (mob instanceof Illusioner)
					return plugin.getConfigManager().illusionerCmd;

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return plugin.getConfigManager().llamaCmd;
				else if (mob instanceof Vex)
					return plugin.getConfigManager().vexCmd;
				else if (mob instanceof Vindicator)
					return plugin.getConfigManager().vindicatorCmd;
				else if (mob instanceof Evoker)
					return plugin.getConfigManager().evokerCmd;
				else if (mob instanceof Donkey)
					return plugin.getConfigManager().donkeyCmd;
				else if (mob instanceof Mule)
					return plugin.getConfigManager().muleCmd;
				else if (mob instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonhorseCmd;
				else if (mob instanceof ZombieHorse)
					return plugin.getConfigManager().zombiehorseCmd;
				else if (mob instanceof Stray)
					return plugin.getConfigManager().strayCmd;
				else if (mob instanceof Husk)
					return plugin.getConfigManager().huskCmd;
				else if (mob instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitCmd;

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return plugin.getConfigManager().polarBearCmd;
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayCmd;
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianCmd;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerCmd;

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return plugin.getConfigManager().shulkerCmd;

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return plugin.getConfigManager().elderGuardianCmd;
				else if (mob instanceof Guardian)
					return plugin.getConfigManager().guardianCmd;
				else if (mob instanceof Endermite)
					return plugin.getConfigManager().endermiteCmd;
				else if (mob instanceof Rabbit)
					if ((((Rabbit) mob).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerrabbitCmd;
					else
						return plugin.getConfigManager().rabbitCmd;

			if (mob instanceof Player)
				return plugin.getConfigManager().pvpKillCmd;
			else if (mob instanceof Blaze)
				return plugin.getConfigManager().blazeCmd;
			else if (mob instanceof Creeper)
				return plugin.getConfigManager().creeperCmd;
			else if (mob instanceof Silverfish)
				return plugin.getConfigManager().silverfishCmd;
			else if (mob instanceof Enderman)
				return plugin.getConfigManager().endermanCmd;
			else if (mob instanceof Giant)
				return plugin.getConfigManager().giantCmd;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonCmd;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonCmd;
			else if (mob instanceof Spider)
				if (mob instanceof CaveSpider)
					// CaveSpider is a sub class of Spider
					return plugin.getConfigManager().caveSpiderCmd;
				else
					return plugin.getConfigManager().spiderCmd;
			else if (mob instanceof Witch)
				return plugin.getConfigManager().witchCmd;
			else if (mob instanceof Zombie)
				if (mob instanceof PigZombie)
					return plugin.getConfigManager().zombiePigmanCmd;
				else
					return plugin.getConfigManager().zombieCmd;
			else if (mob instanceof Ghast)
				return plugin.getConfigManager().ghastCmd;
			else if (mob instanceof MagmaCube)
				// Magmacube is an instance of slime and must be checked before
				// the Slime itself
				return plugin.getConfigManager().magmaCubeCmd;
			else if (mob instanceof Slime)
				return plugin.getConfigManager().slimeCmd;
			else if (mob instanceof EnderDragon)
				return plugin.getConfigManager().enderdragonCmd;
			else if (mob instanceof Wither)
				return plugin.getConfigManager().witherCmd;
			else if (mob instanceof IronGolem)
				return plugin.getConfigManager().ironGolemCmd;

			// Passive mobs
			else if (mob instanceof Bat)
				return plugin.getConfigManager().batCmd;
			else if (mob instanceof Chicken)
				return plugin.getConfigManager().chickenCmd;

			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					return plugin.getConfigManager().mushroomCowCmd;
				else
					return plugin.getConfigManager().cowCmd;
			else if (mob instanceof Horse)
				return plugin.getConfigManager().horseCmd;
			else if (mob instanceof Ocelot)
				return plugin.getConfigManager().ocelotCmd;
			else if (mob instanceof Pig)
				return plugin.getConfigManager().pigCmd;
			else if (mob instanceof Sheep)
				return plugin.getConfigManager().sheepCmd;
			else if (mob instanceof Snowman)
				return plugin.getConfigManager().snowmanCmd;
			else if (mob instanceof Squid)
				return plugin.getConfigManager().squidCmd;
			else if (mob instanceof Villager)
				return plugin.getConfigManager().villagerCmd;
			else if (mob instanceof Wolf)
				return plugin.getConfigManager().wolfCmd;
			else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishCmd;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonCmd;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishCmd;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishCmd;
				}
			}

		}
		return "";
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
			return plugin.getConfigManager().wolfCmdDesc;

		} else {
			if (Misc.isMC112OrNewer())
				if (mob instanceof Parrot)
					return plugin.getConfigManager().parrotCmdDesc;
				else if (mob instanceof Illusioner)
					return plugin.getConfigManager().illusionerCmdDesc;

			if (Misc.isMC111OrNewer())
				if (mob instanceof Llama)
					return plugin.getConfigManager().llamaCmdDesc;
				else if (mob instanceof Vex)
					return plugin.getConfigManager().vexCmdDesc;
				else if (mob instanceof Vindicator)
					return plugin.getConfigManager().vindicatorCmdDesc;
				else if (mob instanceof Evoker)
					return plugin.getConfigManager().evokerCmdDesc;
				else if (mob instanceof Donkey)
					return plugin.getConfigManager().donkeyCmdDesc;
				else if (mob instanceof Mule)
					return plugin.getConfigManager().muleCmdDesc;
				else if (mob instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonhorseCmdDesc;
				else if (mob instanceof ZombieHorse)
					return plugin.getConfigManager().zombiehorseCmdDesc;
				else if (mob instanceof Stray)
					return plugin.getConfigManager().strayCmdDesc;
				else if (mob instanceof Husk)
					return plugin.getConfigManager().huskCmdDesc;
				else if (mob instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitCmdDesc;

			if (Misc.isMC110OrNewer())
				if (mob instanceof PolarBear)
					return plugin.getConfigManager().polarBearCmdDesc;
				else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayCmdDesc;
				else if (mob instanceof Zombie && ((Zombie) mob).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianCmdDesc;
				else if (mob instanceof Villager && ((Villager) mob).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerCmdDesc;

			if (Misc.isMC19OrNewer())
				if (mob instanceof Shulker)
					return plugin.getConfigManager().shulkerCmdDesc;

			if (Misc.isMC18OrNewer())
				if (mob instanceof Guardian && ((Guardian) mob).isElder())
					return plugin.getConfigManager().elderGuardianCmdDesc;
				else if (mob instanceof Guardian)
					return plugin.getConfigManager().guardianCmdDesc;
				else if (mob instanceof Endermite)
					return plugin.getConfigManager().endermiteCmdDesc;
				else if (mob instanceof Rabbit)
					if ((((Rabbit) mob).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerrabbitCmdDesc;
					else
						return plugin.getConfigManager().rabbitCmdDesc;

			// MC1.7 or older
			if (mob instanceof Player)
				return plugin.getConfigManager().pvpKillCmdDesc;
			else if (mob instanceof Blaze)
				return plugin.getConfigManager().blazeCmdDesc;
			else if (mob instanceof Creeper)
				return plugin.getConfigManager().creeperCmdDesc;
			else if (mob instanceof Silverfish)
				return plugin.getConfigManager().silverfishCmdDesc;
			else if (mob instanceof Enderman)
				return plugin.getConfigManager().endermanCmdDesc;
			else if (mob instanceof Giant)
				return plugin.getConfigManager().giantCmdDesc;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonCmdDesc;
			else if (mob instanceof Skeleton && ((Skeleton) mob).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonCmdDesc;
			else if (mob instanceof CaveSpider)
				// CaveSpider is a Subclass of Spider
				return plugin.getConfigManager().caveSpiderCmdDesc;
			else if (mob instanceof Spider)
				return plugin.getConfigManager().spiderCmdDesc;
			else if (mob instanceof Witch)
				return plugin.getConfigManager().witchCmdDesc;
			else if (mob instanceof PigZombie)
				// PigZombie is a subclass of Zombie
				return plugin.getConfigManager().zombiePigmanCmdDesc;
			else if (mob instanceof Zombie)
				return plugin.getConfigManager().zombieCmdDesc;
			else if (mob instanceof Ghast)
				return plugin.getConfigManager().ghastCmdDesc;
			else if (mob instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeCmdDesc;
			else if (mob instanceof Slime)
				return plugin.getConfigManager().slimeCmdDesc;
			else if (mob instanceof EnderDragon)
				return plugin.getConfigManager().enderdragonCmdDesc;
			else if (mob instanceof Wither)
				return plugin.getConfigManager().witherCmdDesc;
			else if (mob instanceof IronGolem)
				return plugin.getConfigManager().ironGolemCmdDesc;

			// Passive mobs
			else if (mob instanceof Bat)
				return plugin.getConfigManager().batCmdDesc;
			else if (mob instanceof Chicken)
				return plugin.getConfigManager().chickenCmdDesc;
			else if (mob instanceof Cow)
				if (mob instanceof MushroomCow)
					// MushroomCow is a subclass of Cow
					return plugin.getConfigManager().mushroomCowCmdDesc;
				else
					return plugin.getConfigManager().cowCmdDesc;
			else if (mob instanceof Horse)
				return plugin.getConfigManager().horseCmdDesc;
			else if (mob instanceof Ocelot)
				return plugin.getConfigManager().ocelotCmdDesc;
			else if (mob instanceof Pig)
				return plugin.getConfigManager().pigCmdDesc;
			else if (mob instanceof Sheep)
				return plugin.getConfigManager().sheepCmdDesc;
			else if (mob instanceof Snowman)
				return plugin.getConfigManager().snowmanCmdDesc;
			else if (mob instanceof Squid)
				return plugin.getConfigManager().squidCmdDesc;
			else if (mob instanceof Villager)
				return plugin.getConfigManager().villagerCmdDesc;
			else if (mob instanceof Wolf)
				return plugin.getConfigManager().wolfCmdDesc;
			else if (mob instanceof Item && ((Item) mob).getItemStack().getType() == Material.RAW_FISH) {
				ItemStack is = ((Item) mob).getItemStack();
				if (is.getData().getData() == (byte) 0) {
					return plugin.getConfigManager().rawFishCmdDesc;
				} else if (is.getData().getData() == (byte) 1) {
					return plugin.getConfigManager().rawSalmonCmdDesc;
				} else if (is.getData().getData() == (byte) 2) {
					return plugin.getConfigManager().clownfishCmdDesc;
				} else if (is.getData().getData() == (byte) 3) {
					return plugin.getConfigManager().pufferfishCmdDesc;
				}
			}

		}
		return "";
	}

	public double getCmdRunChance(Entity killed) {
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
					return plugin.getConfigManager().vexCmdRunChance;
				else if (killed instanceof Vindicator)
					return plugin.getConfigManager().vindicatorCmdRunChance;
				else if (killed instanceof Evoker)
					return plugin.getConfigManager().evokerCmdRunChance;
				else if (killed instanceof Donkey)
					return plugin.getConfigManager().donkeyCmdRunChance;
				else if (killed instanceof Mule)
					return plugin.getConfigManager().muleCmdRunChance;
				else if (killed instanceof SkeletonHorse)
					return plugin.getConfigManager().skeletonhorseCmdRunChance;
				else if (killed instanceof ZombieHorse)
					return plugin.getConfigManager().zombiehorseCmdRunChance;
				else if (killed instanceof Stray)
					return plugin.getConfigManager().strayCmdRunChance;
				else if (killed instanceof Husk)
					return plugin.getConfigManager().huskCmdRunChance;
				else if (killed instanceof ZombieVillager)
					return plugin.getConfigManager().zombieVillagerCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NITWIT)
					return plugin.getConfigManager().nitwitCmdRunChance;

			if (Misc.isMC110OrNewer())
				if (killed instanceof PolarBear)
					return plugin.getConfigManager().polarBearCmdRunChance;
				else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.STRAY)
					return plugin.getConfigManager().strayCmdRunChance;
				else if (killed instanceof Zombie && ((Zombie) killed).getVillagerProfession() == Profession.HUSK)
					return plugin.getConfigManager().huskCmdRunChance;

				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.NORMAL)
					return plugin.getConfigManager().villagerCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.PRIEST)
					return plugin.getConfigManager().priestCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BUTCHER)
					return plugin.getConfigManager().butcherCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.BLACKSMITH)
					return plugin.getConfigManager().blacksmithCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.LIBRARIAN)
					return plugin.getConfigManager().librarianCmdRunChance;
				else if (killed instanceof Villager && ((Villager) killed).getProfession() == Profession.FARMER)
					return plugin.getConfigManager().farmerCmdRunChance;

			if (Misc.isMC19OrNewer())
				if (killed instanceof Shulker)
					return plugin.getConfigManager().shulkerCmdRunChance;

			if (Misc.isMC18OrNewer())
				if (killed instanceof Guardian && ((Guardian) killed).isElder())
					return plugin.getConfigManager().elderGuardianCmdRunChance;
				else if (killed instanceof Guardian)
					return plugin.getConfigManager().guardianCmdRunChance;
				else if (killed instanceof Endermite)
					return plugin.getConfigManager().endermiteCmdRunChance;
				else if (killed instanceof Rabbit)
					if ((((Rabbit) killed).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY)
						return plugin.getConfigManager().killerrabbitCmdRunChance;
					else
						return plugin.getConfigManager().rabbitCmdRunChance;

			// MC1.7 or older
			if (killed instanceof Player) {
				return plugin.getConfigManager().pvpKillCmdRunChance;
			} else if (killed instanceof Blaze)
				return plugin.getConfigManager().blazeCmdRunChance;
			else if (killed instanceof Creeper)
				return plugin.getConfigManager().creeperCmdRunChance;
			else if (killed instanceof Silverfish)
				return plugin.getConfigManager().silverfishCmdRunChance;
			else if (killed instanceof Enderman)
				return plugin.getConfigManager().endermanCmdRunChance;
			else if (killed instanceof Giant)
				return plugin.getConfigManager().giantCmdRunChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.NORMAL)
				return plugin.getConfigManager().skeletonCmdRunChance;
			else if (killed instanceof Skeleton && ((Skeleton) killed).getSkeletonType() == SkeletonType.WITHER)
				return plugin.getConfigManager().witherSkeletonCmdRunChance;
			else if (killed instanceof CaveSpider)
				// CaveSpider is a subclass of Spider
				return plugin.getConfigManager().caveSpiderRunChance;
			else if (killed instanceof Spider)
				return plugin.getConfigManager().spiderCmdRunChance;
			else if (killed instanceof Witch)
				return plugin.getConfigManager().witchCmdRunChance;
			else if (killed instanceof PigZombie)
				// PigZombie is a subclass of Zombie.
				return plugin.getConfigManager().zombiepigmanCmdRunChance;
			else if (killed instanceof Zombie)
				return plugin.getConfigManager().zombieCmdRunChance;
			else if (killed instanceof Ghast)
				return plugin.getConfigManager().ghastCmdRunChance;
			else if (killed instanceof MagmaCube)
				// MagmaCube is a subclass of Slime
				return plugin.getConfigManager().magmaCubeCmdRunChance;
			else if (killed instanceof Slime)
				return plugin.getConfigManager().slimeCmdRunChance;
			else if (killed instanceof EnderDragon)
				return plugin.getConfigManager().enderdragonCmdRunChance;
			else if (killed instanceof Wither)
				return plugin.getConfigManager().witherCmdRunChance;
			else if (killed instanceof IronGolem)
				return plugin.getConfigManager().ironGolemCmdRunChance;

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
			Integer prize = plugin.getMobHuntingManager().mRand.nextInt(Integer.valueOf(str1[1]))
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

	public boolean isCmdGointToBeExcuted(Entity killed) {
		double randomDouble = plugin.getMobHuntingManager().mRand.nextDouble();
		double runChanceDouble = getCmdRunChance(killed);
		Messages.debug("Command will be run if chance: %s > %s (random number)", runChanceDouble, randomDouble);
		if (killed instanceof Player)
			return randomDouble < plugin.getConfigManager().pvpKillCmdRunChance;
		else
			return !getKillConsoleCmd(killed).equals("") && randomDouble < runChanceDouble;
	}

}
