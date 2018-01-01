package one.lindegaard.MobHunting.rewards;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.event.block.Action;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.storage.PlayerSettings;
import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibHelper;
import one.lindegaard.MobHunting.mobs.MinecraftMob;
import one.lindegaard.MobHunting.util.Misc;

public class RewardListeners implements Listener {

	private MobHunting plugin;

	public RewardListeners(MobHunting plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDropReward(PlayerDropItemEvent event) {
		if (event.isCancelled())
			return;

		Item item = event.getItemDrop();
		Player player = event.getPlayer();

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			double money = 0;
			if (player.getGameMode() == GameMode.SURVIVAL)
				money = reward.getMoney();
			else {
				ItemStack is = item.getItemStack();
				is = plugin.getRewardManager().setDisplayNameAndHiddenLores(is, reward.getDisplayname(), money,
						reward.getRewardUUID(), reward.getSkinUUID());
				item.setItemStack(is);
			}
			if (money == 0) {
				item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
						+ reward.getDisplayname());
				plugin.getRewardManager().getDroppedMoney().put(item.getEntityId(), money);
				Messages.debug("%s dropped a %s (# of rewards left=%s)", player.getName(),
						reward.getDisplayname() != null ? reward.getDisplayname()
								: plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
						plugin.getRewardManager().getDroppedMoney().size());
			} else {
				if (reward.isItemReward())
					item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
							+ plugin.getRewardManager().format(money));
				else
					item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
							+ reward.getDisplayname() + " (" + plugin.getRewardManager().format(money) + ")");

				plugin.getRewardManager().getDroppedMoney().put(item.getEntityId(), money);
				if (!plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency)
					plugin.getRewardManager().getEconomy().withdrawPlayer(player, money);

				Messages.debug("%s dropped %s money. (# of rewards left=%s)", player.getName(),
						plugin.getRewardManager().format(money), plugin.getRewardManager().getDroppedMoney().size());
				plugin.getMessages().playerActionBarMessage(player,
						Messages.getString("mobhunting.moneydrop", "money", plugin.getRewardManager().format(money),
								"rewardname", ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
												? plugin.getConfigManager().dropMoneyOnGroundSkullRewardName
														.trim()
												: reward.getDisplayname())));
				if (BagOfGoldCompat.isSupported() && player.getGameMode() == GameMode.SURVIVAL
						&& (reward.isBagOfGoldReward() || reward.isItemReward())) {
					BagOfGold.getInstance().getEconomyManager().removeMoneyFromBalance(player, money);
				}
			}
			item.setCustomNameVisible(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onDespawnRewardEvent(ItemDespawnEvent event) {
		if (event.isCancelled())
			return;

		if (Reward.isReward(event.getEntity())) {
			if (plugin.getRewardManager().getDroppedMoney().containsKey(event.getEntity().getEntityId())) {
				plugin.getRewardManager().getDroppedMoney().remove(event.getEntity().getEntityId());
				Messages.debug("The reward was lost - despawned (# of rewards left=%s)",
						plugin.getRewardManager().getDroppedMoney().size());
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryPickupRewardEvent(InventoryPickupItemEvent event) {
		if (event.isCancelled())
			return;

		Item item = event.getItem();
		if (!item.hasMetadata(Reward.MH_REWARD_DATA))
			return;

		if (plugin.getConfigManager().denyHoppersToPickUpMoney
				&& event.getInventory().getType() == InventoryType.HOPPER) {
			// Messages.debug("A %s tried to pick up the the reward, but this is
			// disabled in config.yml",
			// event.getInventory().getType());
			event.setCancelled(true);
		} else {
			// Messages.debug("The reward was picked up by %s",
			// event.getInventory().getType());
			if (plugin.getRewardManager().getDroppedMoney().containsKey(item.getEntityId()))
				plugin.getRewardManager().getDroppedMoney().remove(item.getEntityId());
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerMoveOverRewardEvent(PlayerMoveEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		if (player.getInventory().firstEmpty() == -1) {
			Iterator<Entity> entityList = ((Entity) player).getNearbyEntities(1, 1, 1).iterator();
			while (entityList.hasNext()) {
				Entity entity = entityList.next();
				if (!(entity instanceof Item))
					continue;

				Item item = (Item) entity;

				if (Reward.isReward(item)) {
					if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId())) {
						Reward reward = Reward.getReward(item);
						if (reward.isBagOfGoldReward() || reward.isItemReward()) {
							boolean done = false;
							if (BagOfGoldCompat.isSupported()) {
								if (player.getGameMode() == GameMode.SURVIVAL) {
									done = plugin.getRewardManager().addBagOfGoldPlayer_RewardManager(player,
											reward.getMoney());
									PlayerSettings ps = BagOfGold.getInstance().getPlayerSettingsManager()
											.getPlayerSettings(player);
									ps.setBalance(Misc.round(ps.getBalance() + reward.getMoney()));
									BagOfGold.getInstance().getPlayerSettingsManager().setPlayerSettings(player, ps);
									BagOfGold.getInstance().getDataStoreManager().updatePlayerSettings(player, ps);
									done = true;
								}
							} else if (reward.getMoney() != 0
									&& !plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency) {
								// If not Gringotts
								done = plugin.getRewardManager().depositPlayer(player, reward.getMoney())
										.transactionSuccess();
							} else {
								// Inventory is full , check if item is
								// inventory
								for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
									ItemStack is = player.getInventory().getItem(slot);
									if (Reward.isReward(is)) {
										Reward rewardInSlot = Reward.getReward(is);
										if ((rewardInSlot.isBagOfGoldReward() || rewardInSlot.isItemReward())) {
											rewardInSlot.setMoney(rewardInSlot.getMoney() + reward.getMoney());
											ItemMeta im = is.getItemMeta();
											im.setLore(rewardInSlot.getHiddenLore());
											String displayName = plugin.getConfigManager().dropMoneyOnGroundItemtype
													.equalsIgnoreCase("ITEM")
															? plugin.getRewardManager().format(rewardInSlot.getMoney())
															: rewardInSlot.getDisplayname() + " (" + plugin
																	.getRewardManager().format(rewardInSlot.getMoney())
																	+ ")";
											im.setDisplayName(ChatColor
													.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
													+ displayName);
											is.setItemMeta(im);
											is.setAmount(1);
											Messages.debug(
													"Added %s to item in slot %s, new value is %s (addBagOfGoldPlayer_RewardManager)",
													plugin.getRewardManager().format(reward.getMoney()), slot,
													plugin.getRewardManager().format(rewardInSlot.getMoney()));
											done = true;
											break;
										}
									}
								}
							}
							if (done) {
								item.remove();
								if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId()))
									plugin.getRewardManager().getDroppedMoney().remove(entity.getEntityId());
								if (ProtocolLibCompat.isSupported())
									ProtocolLibHelper.pickupMoney(player, item);

								if (reward.getMoney() == 0) {
									Messages.debug("%s picked up a %s (# of rewards left=%s)", player.getName(),
											reward.getDisplayname(),
											plugin.getRewardManager().getDroppedMoney().size());
								} else {
									Messages.debug(
											"%s picked up a %s with a value:%s (# of rewards left=%s)(PickupRewards)",
											player.getName(), reward.getDisplayname(),
											plugin.getRewardManager().format(Misc.round(reward.getMoney())),
											plugin.getRewardManager().getDroppedMoney().size());
									plugin.getMessages().playerActionBarMessage(player, Messages.getString(
											"mobhunting.moneypickup", "money",
											plugin.getRewardManager().format(reward.getMoney()), "rewardname",
											ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
													+ (plugin.getConfigManager().dropMoneyOnGroundItemtype
															.equalsIgnoreCase("ITEM")
																	? plugin.getConfigManager().dropMoneyOnGroundSkullRewardName
																			.trim()
																	: reward.getDisplayname())));
								}
							}
						}
					} else {
						Messages.debug("This reward is missing in DroppedRewards (size=%s)",
								plugin.getRewardManager().getDroppedMoney().size());
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onProjectileHitRewardEvent(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		Entity targetEntity = null;
		Iterator<Entity> nearby = projectile.getNearbyEntities(1, 1, 1).iterator();
		while (nearby.hasNext()) {
			targetEntity = nearby.next();
			if (Reward.isReward(targetEntity)) {
				if (plugin.getRewardManager().getDroppedMoney().containsKey(targetEntity.getEntityId()))
					plugin.getRewardManager().getDroppedMoney().remove(targetEntity.getEntityId());
				targetEntity.remove();
				Messages.debug("The reward was hit by %s and removed. (# of rewards left=%s)", projectile.getType(),
						plugin.getRewardManager().getDroppedMoney().size());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRewardBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		ItemStack is = event.getItemInHand();
		Block block = event.getBlockPlaced();
		if (Reward.isReward(is)) {
			Reward reward = Reward.getReward(is);
			if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
				reward.setMoney(0);
				plugin.getMessages().learn(event.getPlayer(), Messages.getString("mobhunting.learn.no-duplication"));
			}
			if (reward.getMoney() == 0)
				reward.setUniqueId(UUID.randomUUID());
			Messages.debug("Placed Reward Block:%s", reward.toString());
			block.setMetadata(Reward.MH_REWARD_DATA, new FixedMetadataValue(plugin, reward));
			plugin.getRewardManager().getLocations().put(reward.getUniqueUUID(), reward);
			plugin.getRewardManager().getReward().put(reward.getUniqueUUID(), block.getLocation());
			plugin.getRewardManager().saveReward(reward.getUniqueUUID());
			if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() || reward.isItemReward())
					&& player.getGameMode() == GameMode.SURVIVAL) {
				BagOfGold.getInstance().getEconomyManager().removeMoneyFromBalance(player, reward.getMoney());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRewardBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		CustomItems customItems = new CustomItems(plugin);

		Block block = event.getBlock();
		if (Reward.hasReward(block)) {
			Reward reward = Reward.getReward(block);
			block.getDrops().clear();
			block.setType(Material.AIR);
			block.removeMetadata(Reward.MH_REWARD_DATA, plugin);
			ItemStack is;
			if (reward.isBagOfGoldReward()) {
				is = customItems.getCustomtexture(reward.getRewardUUID(), reward.getDisplayname(),
						plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
						plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, reward.getMoney(),
						reward.getUniqueUUID(), reward.getSkinUUID());
			} else {
				MinecraftMob mob = (reward.getSkinUUID() != null)
						? MinecraftMob.getMinecraftMobType(reward.getSkinUUID())
						: MinecraftMob.getMinecraftMobType(reward.getDisplayname());
				if (mob != null) {
					is = customItems.getCustomHead(mob, reward.getDisplayname(), 1, reward.getMoney(),
							reward.getSkinUUID());
				} else {
					@SuppressWarnings("deprecation")
					OfflinePlayer player = Bukkit.getOfflinePlayer(reward.getDisplayname());
					if (player != null) {
						is = customItems.getPlayerHead(player.getUniqueId(), 1, reward.getMoney());
					} else {
						plugin.getLogger().warning("[MobHunting] The mobtype could not be detected from displayname:"
								+ reward.getDisplayname());
						is = new ItemStack(Material.SKULL_ITEM, 1);
					}
				}
			}
			Item item = block.getWorld().dropItemNaturally(block.getLocation(), is);

			String displayName = plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
					? plugin.getRewardManager().format(reward.getMoney())
					: (reward.getMoney() == 0 ? reward.getDisplayname()
							: reward.getDisplayname() + " (" + plugin.getRewardManager().format(reward.getMoney())
									+ ")");
			item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + displayName);
			item.setCustomNameVisible(true);
			item.setMetadata(Reward.MH_REWARD_DATA, new FixedMetadataValue(plugin, new Reward(reward.getHiddenLore())));

			if (plugin.getRewardManager().getLocations().containsKey(reward.getUniqueUUID()))
				plugin.getRewardManager().getLocations().remove(reward.getUniqueUUID());
			if (plugin.getRewardManager().getReward().containsKey(reward.getUniqueUUID()))
				plugin.getRewardManager().getReward().remove(reward.getUniqueUUID());
		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryMoveReward(InventoryMoveItemEvent event) {
		if (event.isCancelled())
			return;

		if (Reward.isReward(event.getItem())) {
			Messages.debug(
					"onInventoryMoveReward: A reward was moved. Initiator=%s, Source=%s, Destination=%s. The event was cancelled!",
					event.getInitiator().getType(), event.getSource().getType(), event.getDestination().getType());
			event.setCancelled(true);
		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		Inventory inventory = event.getInventory();
		if (inventory.getType() == InventoryType.CRAFTING) {
			ItemStack helmet = player.getEquipment().getHelmet();
			if (Reward.isReward(helmet)) {
				Reward reward = Reward.getReward(helmet);
				if (reward.isBagOfGoldReward()) {
					Messages.debug("%s is not allowed to wear a BagOfGold on the head", player.getName());
					plugin.getMessages().learn(player, Messages.getString("mobhunting.learn.rewards.no-helmet"));
					event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
					// if (BagOfGoldCompat.isSupported() && player.getGameMode()
					// == GameMode.SURVIVAL)
					if (!plugin.getRewardManager().addBagOfGoldPlayer_RewardManager(player, reward.getMoney()))

						// BagOfGold.getApi().getEconomyManager().addBagOfGoldPlayer_EconomyManager(player,
						// reward.getMoney());
						// else
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, null, player.getLocation(),
								reward.getMoney());
				}
			}
		}
		// ItemStack cursor = event.getView().getCursor();
		// if (Reward.isReward(event.getView().getCursor())) {
		// Reward reward = Reward.getReward(cursor);
		// if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() ||
		// reward.isItemReward())) {
		// Messages.debug("Closed inventory while cursor is a %s",
		// reward.getDisplayname());
		// BagOfGold.getApi().getEconomyManager().addMoneyToBalance(player,
		// reward.getMoney());
		// }
		// }
		if (BagOfGoldCompat.isSupported() && player.getGameMode() == GameMode.SURVIVAL) {
			PlayerSettings ps = BagOfGold.getApi().getPlayerSettingsManager().getPlayerSettings(player);
			double amountInInventory = 0;
			for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
				ItemStack is = player.getInventory().getItem(slot);
				if (Reward.isReward(is)) {
					Reward reward = Reward.getReward(is);
					if (reward.isBagOfGoldReward() || reward.isItemReward())
						amountInInventory = amountInInventory + reward.getMoney();
				}
			}
			if (Misc.round(amountInInventory) != Misc.round(ps.getBalance() + ps.getBalanceChanges())) {
				ps.setBalance(amountInInventory);
				ps.setBalanceChanges(0);
				BagOfGold.getApi().getPlayerSettingsManager().setPlayerSettings(player, ps);
				BagOfGold.getApi().getDataStoreManager().updatePlayerSettings(player, ps);
			}
			Messages.debug("%S closed inventory: new balance is %s", player.getName(),
					plugin.getRewardManager().getEconomy().getBalance(player));
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL)
	public void onInventoryClickReward(InventoryClickEvent event) {
		if (event.isCancelled() || event.getClickedInventory() == null)
			return;

		InventoryAction action = event.getAction();
		ItemStack isCurrentSlot = event.getCurrentItem();
		ItemStack isCursor = event.getCursor();
		Player player = (Player) event.getWhoClicked();
		Inventory inventory = event.getInventory();
		Inventory clickedInventory = event.getClickedInventory();

		if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
			Messages.debug("action=%s, ClickedinvType=%s, InventoryType=%s, slotno=%s, current=%s, cursor=%s", action,
					clickedInventory.getType(), inventory.getType(), event.getSlot(),
					isCurrentSlot == null ? "null" : isCurrentSlot.getType(),
					isCursor == null ? "null" : isCursor.getType());
		}

		if (action == InventoryAction.NOTHING)
			return;

		if ((inventory.getType() == InventoryType.FURNACE || inventory.getType() == InventoryType.ANVIL
				|| inventory.getType() == InventoryType.BEACON || inventory.getType() == InventoryType.BREWING
				|| inventory.getType() == InventoryType.CREATIVE || inventory.getType() == InventoryType.ENCHANTING
				|| inventory.getType() == InventoryType.WORKBENCH)
				&& (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor))) {
			Reward reward = Reward.isReward(isCurrentSlot) ? Reward.getReward(isCurrentSlot)
					: Reward.getReward(isCursor);
			plugin.getMessages().learn(player,
					Messages.getString("mobhunting.learn.rewards.no-use", "rewardname", reward.getDisplayname()));
			event.setCancelled(true);
			return;
		}

		if (player.getGameMode() != GameMode.SURVIVAL
				&& (Reward.isReward(isCursor) || Reward.isReward(isCurrentSlot))) {
			plugin.getMessages().learn(player, Messages.getString("mobhunting.learn.rewards.creative"));
			event.setCancelled(true);
			return;
		}

		if (action == InventoryAction.CLONE_STACK && (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor))) {
			Reward reward = Reward.isReward(isCurrentSlot) ? Reward.getReward(isCurrentSlot)
					: Reward.getReward(isCursor);
			plugin.getMessages().learn(player,
					Messages.getString("mobhunting.learn.rewards.no-clone", "rewardname", reward.getDisplayname()));
			event.setCancelled(true);
			return;
		}

		if ((Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor))
				&& clickedInventory.getType() == InventoryType.CRAFTING && event.getSlot() == 40) {
			Reward reward1 = Reward.getReward(isCurrentSlot);
			Reward reward2 = Reward.getReward(isCursor);
			Messages.debug("You cant place a BagofGold in the helmet slot : (%s,%s)", reward1 == null, reward2 == null);
			event.setCancelled(true);
			return;
		}

		if (action == InventoryAction.SWAP_WITH_CURSOR
				&& (isCurrentSlot.getType() == Material.SKULL_ITEM
						|| isCurrentSlot.getType() == Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem))
				&& isCurrentSlot != null && isCurrentSlot.getType() == isCursor.getType()) {
			if (Reward.isReward(isCurrentSlot) && Reward.isReward(isCursor)) {
				event.setCancelled(true);
				ItemMeta imCurrent = isCurrentSlot.getItemMeta();
				ItemMeta imCursor = isCursor.getItemMeta();
				Reward reward1 = new Reward(imCurrent.getLore());
				Reward reward2 = new Reward(imCursor.getLore());
				if ((reward1.isBagOfGoldReward() || reward1.isItemReward())
						&& reward1.getRewardUUID().equals(reward2.getRewardUUID())) {
					reward2.setMoney(reward1.getMoney() + reward2.getMoney());
					imCursor.setLore(reward2.getHiddenLore());
					imCursor.setDisplayName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
							+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
									? plugin.getRewardManager().format(reward2.getMoney())
									: reward2.getDisplayname() + " ("
											+ plugin.getRewardManager().format(reward2.getMoney()) + ")"));
					isCursor.setItemMeta(imCursor);
					isCurrentSlot.setAmount(0);
					isCurrentSlot.setType(Material.AIR);
					Messages.debug("%s merged two rewards", player.getName());
					if (BagOfGoldCompat.isSupported()) {
						// BagOfGold.getApi().getEconomyManager().removeMoneyFromBalance(player,
						// reward1.getMoney());
					}
				}
			}

		} else if (action == InventoryAction.PICKUP_HALF
				&& (clickedInventory.getType() == InventoryType.PLAYER
						|| clickedInventory.getType() == InventoryType.CRAFTING)
				&& isCursor.getType() == Material.AIR
				&& (isCurrentSlot.getType() == Material.SKULL_ITEM || isCurrentSlot.getType() == Material
						.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem))) {
			if (Reward.isReward(isCurrentSlot)) {
				Reward reward = Reward.getReward(isCurrentSlot);
				if (reward.isBagOfGoldReward() || reward.isItemReward()) {
					double currentSlotMoney = Misc.floor(reward.getMoney() / 2);
					double cursorMoney = Misc.round(reward.getMoney() - currentSlotMoney);
					if (currentSlotMoney >= plugin.getConfigManager().minimumReward) {
						event.setCancelled(true);

						isCurrentSlot = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCurrentSlot.clone(),
								reward.getDisplayname(), currentSlotMoney, reward.getRewardUUID(),
								reward.getSkinUUID());
						event.setCurrentItem(isCurrentSlot);

						isCursor = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCurrentSlot.clone(),
								reward.getDisplayname(), cursorMoney, reward.getRewardUUID(), reward.getSkinUUID());
						event.setCursor(isCursor);

						if (BagOfGoldCompat.isSupported()) {
							// BagOfGold.getApi().getEconomyManager().removeMoneyFromBalance(player,
							// cursorMoney);
						}

						Messages.debug("%s halfed a reward in two (%s,%s)", player.getName(),
								plugin.getRewardManager().format(currentSlotMoney),
								plugin.getRewardManager().format(cursorMoney));
					}
				}
			}
		} else if (action == InventoryAction.COLLECT_TO_CURSOR && Reward.isReward(isCursor)) {
			Reward cursor = Reward.getReward(isCursor);
			if (cursor.getMoney() > 0 && (cursor.isBagOfGoldReward() || cursor.isItemReward())) {
				double saldo = Misc.floor(cursor.getMoney());
				for (int slot = 0; slot < clickedInventory.getSize(); slot++) {
					ItemStack is = player.getInventory().getItem(slot);
					if (Reward.isReward(is)) {
						Reward reward = Reward.getReward(is);
						if (cursor.getRewardUUID().equals(reward.getRewardUUID()) && reward.getMoney() > 0) {
							saldo = saldo + reward.getMoney();
							player.getInventory().clear(slot);
						}
					}
				}
				for (int slot = 0; slot < inventory.getSize(); slot++) {
					ItemStack is = player.getInventory().getItem(slot);
					if (Reward.isReward(is)) {
						Reward reward = Reward.getReward(is);
						if (cursor.getRewardUUID().equals(reward.getRewardUUID()) && reward.getMoney() > 0) {
							saldo = saldo + reward.getMoney();
							player.getInventory().clear(slot);
						}
					}
				}

				isCursor = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCursor.clone(),
						cursor.getDisplayname(), saldo, cursor.getRewardUUID(), cursor.getSkinUUID());
				event.setCursor(isCursor);
				if (BagOfGoldCompat.isSupported()) {
					// BagOfGold.getApi().getEconomyManager().removeMoneyFromBalance(player,
					// saldo - cursor.getMoney());
				}
			}
		} else if (Reward.isReward(isCurrentSlot)
				&& (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_ONE
						|| action == InventoryAction.PICKUP_SOME || action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
				&& ((clickedInventory.getType() == InventoryType.PLAYER && event.getSlot() < 36
						|| event.getSlot() == 40) || clickedInventory.getType() == InventoryType.CRAFTING)) {
			Reward reward = Reward.getReward(isCurrentSlot);
			if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() || reward.isItemReward())) {
				Messages.debug("(1)Reward was moved from PlayerInventory (%s) to %s", inventory.getType(),
						event.getClickedInventory().getType());
				// BagOfGold.getApi().getEconomyManager().removeMoneyFromBalance(player,
				// reward.getMoney());
			}
		} else if ((action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE
				|| action == InventoryAction.PLACE_SOME || action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
				&& BagOfGoldCompat.isSupported() && Reward.isReward(isCursor)
				&& ((clickedInventory.getType() == InventoryType.PLAYER && event.getSlot() != 39)
						|| (clickedInventory.getType() == InventoryType.CRAFTING && event.getSlot() != 0))) {
			Reward reward = Reward.getReward(isCursor);
			if (reward.isBagOfGoldReward() || reward.isItemReward()) {
				Messages.debug("(2)Reward was moved from %s to PlayerInventory (%s)", inventory.getType(),
						clickedInventory.getType());
				// BagOfGold.getApi().getEconomyManager().addMoneyToBalance(player,
				// reward.getMoney());
			}
		} else if ((action == InventoryAction.MOVE_TO_OTHER_INVENTORY) && BagOfGoldCompat.isSupported()
				&& Reward.isReward(isCurrentSlot)
				&& ((clickedInventory.getType() == InventoryType.PLAYER && event.getSlot() == 39))) {
			Reward reward = Reward.getReward(isCurrentSlot);
			if (reward.isBagOfGoldReward() || reward.isItemReward()) {
				Messages.debug("(3)Reward was moved to %s from PlayerInventory (%s)", inventory.getType(),
						clickedInventory.getType());
				// BagOfGold.getApi().getEconomyManager().addMoneyToBalance(player,
				// reward.getMoney());
			}
		} else if (Reward.isReward(isCurrentSlot)
				&& (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_ONE)
				&& (event.getClickedInventory().getType() == InventoryType.DISPENSER
						|| clickedInventory.getType() == InventoryType.DROPPER
						|| clickedInventory.getType() == InventoryType.CHEST
						|| clickedInventory.getType() == InventoryType.ENDER_CHEST
						|| clickedInventory.getType() == InventoryType.SHULKER_BOX)) {
			Reward reward = Reward.getReward(isCurrentSlot);
			if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() || reward.isItemReward())) {
				Messages.debug("(4)Reward was moved from Inventory (%s) to %s", inventory.getType(),
						clickedInventory.getType());
			}
		} else if (Reward.isReward(isCurrentSlot) && (action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
				&& (event.getClickedInventory().getType() == InventoryType.DISPENSER
						|| clickedInventory.getType() == InventoryType.DROPPER
						|| clickedInventory.getType() == InventoryType.CHEST
						|| clickedInventory.getType() == InventoryType.ENDER_CHEST
						|| clickedInventory.getType() == InventoryType.SHULKER_BOX)
				&& clickedInventory.getType() == inventory.getType()) {
			Reward reward = Reward.getReward(isCurrentSlot);
			if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() || reward.isItemReward())) {
				Messages.debug("(5)Reward was moved from PlayerInventory (%s) to %s", inventory.getType(),
						clickedInventory.getType());
				// BagOfGold.getApi().getEconomyManager().addMoneyToBalance(player,
				// reward.getMoney());
			}
		} else if ((action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ALL_SLOT
				|| action == InventoryAction.DROP_ONE_CURSOR || action == InventoryAction.DROP_ONE_SLOT
				|| action == InventoryAction.UNKNOWN || action == InventoryAction.HOTBAR_MOVE_AND_READD
				|| action == InventoryAction.HOTBAR_SWAP) && Reward.isReward(isCursor)
				&& ((clickedInventory.getType() == InventoryType.PLAYER && event.getSlot() != 39)
						|| (clickedInventory.getType() == InventoryType.CRAFTING && event.getSlot() != 0))) {
			Reward reward = Reward.getReward(isCursor);
			if (BagOfGoldCompat.isSupported() && (reward.isBagOfGoldReward() || reward.isItemReward())) {

				Bukkit.getLogger()
						.warning("This action is not supportet. Action=" + action + ", ClickedinvType="
								+ clickedInventory.getType() + ", InventoryType=" + inventory.getType() + ", slotno="
								+ event.getSlot());
				event.setCancelled(true);

			}
		}
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (event.getClickedBlock() == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		if (Misc.isMC19OrNewer() && event.getHand() != EquipmentSlot.HAND)
			return;

		Player player = event.getPlayer();

		Block block = event.getClickedBlock();

		if (Reward.hasReward(block)) {
			Reward reward = Reward.getReward(block);
			if (reward.getMoney() == 0)
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ reward.getDisplayname());
			else
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(reward.getMoney())
										: reward.getDisplayname() + " ("
												+ plugin.getRewardManager().format(reward.getMoney()) + ")"));
		} else if (block.getType() == Material.SKULL) {
			Skull skullState = (Skull) block.getState();
			switch (skullState.getSkullType()) {
			case PLAYER:
				if (Misc.isMC19OrNewer()) {
					OfflinePlayer owner = skullState.getOwningPlayer();
					if (owner != null && owner.getName() != null) {
						plugin.getMessages().playerActionBarMessage(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ owner.getName());
					} else
						plugin.getMessages().playerActionBarMessage(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ Messages.getString("mobhunting.reward.customtexture"));
				} else if (skullState.hasOwner()) {
					@SuppressWarnings("deprecation")
					String owner = skullState.getOwner();
					if (!owner.equalsIgnoreCase("")) {
						plugin.getMessages().playerActionBarMessage(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + owner);
					} else
						plugin.getMessages().playerActionBarMessage(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ Messages.getString("mobhunting.reward.customtexture"));
				} else
					plugin.getMessages().playerActionBarMessage(player,
							ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
									+ Messages.getString("mobhunting.reward.steve"));
				break;
			case CREEPER:
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ Messages.getString("mobs.Creeper.name"));
				break;
			case SKELETON:
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ Messages.getString("mobs.Skeleton.name"));
				break;
			case WITHER:
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ Messages.getString("mobs.Wither.name"));
				break;
			case ZOMBIE:
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ Messages.getString("mobs.Zombie.name"));
				break;
			case DRAGON:
				plugin.getMessages().playerActionBarMessage(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ Messages.getString("mobs.EnderDragon.name"));
				break;
			default:
				break;
			}
		}
	}

}
