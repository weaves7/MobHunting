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
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.event.block.Action;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.BagOfGoldCompat;
import one.lindegaard.MobHunting.compatibility.CitizensCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibCompat;
import one.lindegaard.MobHunting.compatibility.ProtocolLibHelper;
import one.lindegaard.MobHunting.mobs.MinecraftMob;
import one.lindegaard.MobHunting.util.Misc;

public class RewardListeners implements Listener {

	private MobHunting plugin;

	public RewardListeners(MobHunting plugin) {
		this.plugin = plugin;
	}

	private boolean isFakeReward(Item item) {
		ItemStack itemStack = item.getItemStack();
		return isFakeReward(itemStack);
	}

	private boolean isFakeReward(ItemStack itemStack) {

		if (itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName()
				&& itemStack.getItemMeta().getDisplayName()
						.contains(MobHunting.getAPI().getConfigManager().dropMoneyOnGroundSkullRewardName)) {
			if (!itemStack.getItemMeta().hasLore()) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropReward(PlayerDropItemEvent event) {
		if (event.isCancelled())
			return;

		Item item = event.getItemDrop();
		Player player = event.getPlayer();

		if (isFakeReward(item)) {
			player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
			return;
		}

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			double money = reward.getMoney();
			if (money == 0) {
				item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
						+ reward.getDisplayname());
				plugin.getRewardManager().getDroppedMoney().put(item.getEntityId(), money);
				plugin.getMessages().debug("%s dropped a %s (# of rewards left=%s)", player.getName(),
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
				if (!plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency
						&& !plugin.getConfigManager().dropMoneyOnGroup)
					plugin.getRewardManager().getEconomy().withdrawPlayer(player, money);

				plugin.getMessages().debug("%s dropped %s money. (# of rewards left=%s)", player.getName(),
						plugin.getRewardManager().format(money), plugin.getRewardManager().getDroppedMoney().size());
				plugin.getMessages().playerActionBarMessageQueue(player,
						plugin.getMessages().getString("mobhunting.moneydrop", "money",
								plugin.getRewardManager().format(money), "rewardname", ChatColor
										.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
												? plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim()
												: reward.getDisplayname())));
			}
			item.setCustomNameVisible(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onDespawnRewardEvent(ItemDespawnEvent event) {
		if (event.isCancelled())
			return;

		if (BagOfGoldCompat.isSupported() && BagOfGoldCompat.useAsEconomyAnEconomyPlugin())
			return;

		if (Reward.isReward(event.getEntity())) {
			if (plugin.getRewardManager().getDroppedMoney().containsKey(event.getEntity().getEntityId())) {
				plugin.getRewardManager().getDroppedMoney().remove(event.getEntity().getEntityId());
				if (event.getEntity().getLastDamageCause() != null)
					plugin.getMessages().debug("The reward was destroyed by %s",
							event.getEntity().getLastDamageCause().getCause());
				else
					plugin.getMessages().debug("The reward despawned (# of rewards left=%s)",
							plugin.getRewardManager().getDroppedMoney().size());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryPickupRewardEvent(InventoryPickupItemEvent event) {
		if (event.isCancelled())
			return;

		if (BagOfGoldCompat.isSupported() && BagOfGoldCompat.useAsEconomyAnEconomyPlugin())
			return;

		Item item = event.getItem();
		if (!item.hasMetadata(Reward.MH_REWARD_DATA))
			return;

		if (plugin.getConfigManager().denyHoppersToPickUpMoney
				&& event.getInventory().getType() == InventoryType.HOPPER) {
			// plugin.getMessages().debug("A %s tried to pick up the the reward,
			// but this is
			// disabled in config.yml",
			// event.getInventory().getType());
			event.setCancelled(true);
		} else {
			// plugin.getMessages().debug("The reward was picked up by %s",
			// event.getInventory().getType());
			if (plugin.getRewardManager().getDroppedMoney().containsKey(item.getEntityId()))
				plugin.getRewardManager().getDroppedMoney().remove(item.getEntityId());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerMoveOverRewardEvent(PlayerMoveEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();

		if (plugin.getRewardManager().canPickupMoney(player)) {
			Iterator<Entity> entityList = ((Entity) player).getNearbyEntities(1, 1, 1).iterator();
			while (entityList.hasNext() && plugin.getRewardManager().canPickupMoney(player)) {
				Entity entity = entityList.next();
				if (!(entity instanceof Item))
					continue;

				Item item = (Item) entity;

				if (isFakeReward(item)) {
					player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
					return;
				}

				if (Reward.isReward(item)) {
					if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId())) {
						Reward reward = Reward.getReward(item);
						if (reward.isBagOfGoldReward() || reward.isItemReward()) {
							double addedMoney = 0;
							if (reward.getMoney() != 0 && !plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency) {
								// If not Gringotts
								addedMoney = plugin.getRewardManager().depositPlayer(player, reward.getMoney()).amount;
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
											plugin.getMessages().debug(
													"Added %s to %s's item in slot %s, new value is %s (addBagOfGoldPlayer_RewardManager)",
													plugin.getRewardManager().format(reward.getMoney()),
													player.getName(), slot,
													plugin.getRewardManager().format(rewardInSlot.getMoney()));
											addedMoney = reward.getMoney();
											break;
										}
									}
								}
							}
							if (Misc.round(addedMoney) == Misc.round(reward.getMoney())) {
								plugin.getMessages().debug("Was able to pickup all the money");
								item.remove();
								if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId()))
									plugin.getRewardManager().getDroppedMoney().remove(entity.getEntityId());
								if (ProtocolLibCompat.isSupported())
									ProtocolLibHelper.pickupMoney(player, item);

								if (reward.getMoney() == 0) {
									plugin.getMessages().debug("%s picked up a %s (# of rewards left=%s)",
											player.getName(), reward.getDisplayname(),
											plugin.getRewardManager().getDroppedMoney().size());
								} else {
									plugin.getMessages().debug(
											"%s picked up a %s with a value:%s (# of rewards left=%s)(PickupRewards)",
											player.getName(), reward.getDisplayname(),
											plugin.getRewardManager().format(Misc.round(reward.getMoney())),
											plugin.getRewardManager().getDroppedMoney().size());
									plugin.getMessages().playerActionBarMessageQueue(player,
											plugin.getMessages().getString("mobhunting.moneypickup", "money",
													plugin.getRewardManager().format(reward.getMoney()), "rewardname",
													ChatColor.valueOf(
															plugin.getConfigManager().dropMoneyOnGroundTextColor)
															+ (plugin.getConfigManager().dropMoneyOnGroundItemtype
																	.equalsIgnoreCase("ITEM")
																			? plugin.getConfigManager().dropMoneyOnGroundSkullRewardName
																					.trim()
																			: reward.getDisplayname())));
								}
							} else if (Misc.round(addedMoney) < Misc.round(reward.getMoney())) {
								double rest = reward.getMoney() - addedMoney;
								plugin.getMessages().debug("Was not able to pick up %s money (remove Item)", rest);
								item.remove();

								if (plugin.getRewardManager().getDroppedMoney().containsKey(entity.getEntityId()))
									plugin.getRewardManager().getDroppedMoney().remove(entity.getEntityId());
								if (ProtocolLibCompat.isSupported())
									ProtocolLibHelper.pickupMoney(player, item);

								plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, null,
										player.getLocation(), rest);

							} else {
								plugin.getMessages().debug("someting else?????");
							}
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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
				plugin.getMessages().debug("The reward was hit by %s and removed. (# of rewards left=%s)",
						projectile.getType(), plugin.getRewardManager().getDroppedMoney().size());
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

		if (isFakeReward(is)) {
			player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this was a FAKE reward with no value");
			return;
		}

		if (Reward.isReward(is)) {
			Reward reward = Reward.getReward(is);
			if (event.getPlayer().getGameMode() != GameMode.SURVIVAL) {
				reward.setMoney(0);
				plugin.getMessages().learn(event.getPlayer(),
						plugin.getMessages().getString("mobhunting.learn.no-duplication"));
			}
			if (reward.getMoney() == 0)
				reward.setUniqueId(UUID.randomUUID());
			plugin.getMessages().debug("%s placed a reward block: %s", player.getName(),
					ChatColor.stripColor(reward.toString()));
			block.setMetadata(Reward.MH_REWARD_DATA, new FixedMetadataValue(plugin, reward));
			plugin.getRewardManager().getReward().put(reward.getUniqueUUID(), reward);
			plugin.getRewardManager().getLocations().put(reward.getUniqueUUID(), block.getLocation());
			plugin.getRewardManager().saveReward(reward.getUniqueUUID());
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
				is = customItems.getCustomtexture(reward.getRewardType(), reward.getDisplayname(),
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
						is = new ItemStack(Material.PLAYER_HEAD, 1);
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		Player player = (Player) event.getPlayer();
		Inventory inventory = event.getInventory();
		if (inventory.getType() == InventoryType.CRAFTING) {
			ItemStack helmet = player.getEquipment().getHelmet();

			if (isFakeReward(helmet)) {
				player.sendMessage(
						ChatColor.RED + "[MobHunting] WARNING, you have a reward on your head. It was removed.");
				event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
				return;
			}

			if (Reward.isReward(helmet)) {
				Reward reward = Reward.getReward(helmet);
				if (reward.isBagOfGoldReward()) {
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.rewards.no-helmet"));
					event.getPlayer().getEquipment().setHelmet(new ItemStack(Material.AIR));
					if (Misc.round(reward.getMoney()) != Misc
							.round(plugin.getRewardManager().addBagOfGoldPlayer(player, reward.getMoney())))
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(player, null, player.getLocation(),
								reward.getMoney());
				}
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
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ reward.getDisplayname());
			else
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(reward.getMoney())
										: reward.getDisplayname() + " ("
												+ plugin.getRewardManager().format(reward.getMoney()) + ")"));
		} else if (Misc.isMC113OrNewer()
				&& (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD)) {
			Skull skullState = (Skull) block.getState();
			OfflinePlayer owner = skullState.getOwningPlayer();
			if (owner != null && owner.getName() != null)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + owner.getName());
		} else if (block.getType() == Material.LEGACY_SKULL) {
			Skull skullState = (Skull) block.getState();
			switch (skullState.getSkullType()) {
			case PLAYER:
				if (Misc.isMC19OrNewer()) {
					OfflinePlayer owner = skullState.getOwningPlayer();
					if (owner != null && owner.getName() != null) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ owner.getName());
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else if (skullState.hasOwner()) {
					@SuppressWarnings("deprecation")
					String owner = skullState.getOwner();
					if (owner != null && !owner.equalsIgnoreCase("")) {
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + owner);
					} else
						plugin.getMessages().playerActionBarMessageQueue(player,
								ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
										+ plugin.getMessages().getString("mobhunting.reward.customtexture"));
				} else
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
									+ plugin.getMessages().getString("mobhunting.reward.steve"));
				break;
			case CREEPER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Creeper.name"));
				break;
			case SKELETON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Skeleton.name"));
				break;
			case WITHER:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Wither.name"));
				break;
			case ZOMBIE:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.Zombie.name"));
				break;
			case DRAGON:
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ plugin.getMessages().getString("mobs.EnderDragon.name"));
				break;
			default:
				break;
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInventoryClickReward(InventoryClickEvent event) {
		if (event.isCancelled() || event.getInventory() == null) {
			plugin.getMessages().debug("RewardListeners: Something cancelled the InventoryClickEvent");
			return;
		}

		if (CitizensCompat.isNPC(event.getWhoClicked()))
			return;

		ItemStack isCurrentSlot = event.getCurrentItem();
		ItemStack isCursor = event.getCursor();

		Player player = (Player) event.getWhoClicked();

		if (!(Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor))) {
			if (isFakeReward(isCurrentSlot)) {
				player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this is a FAKE reward. It was removed.");
				isCurrentSlot.setType(Material.AIR);
				return;
			}
			if (isFakeReward(isCursor)) {
				player.sendMessage(ChatColor.RED + "[MobHunting] WARNING, this is a FAKE reward. It was removed.");
				isCursor.setType(Material.AIR);
				return;
			}
			return;
		}

		InventoryAction action = event.getAction();
		SlotType slotType = event.getSlotType();

		// Inventory inventory = event.getInventory();
		// if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
		// plugin.getMessages().debug(
		// "action=%s, InventoryType=%s, slottype=%s, slotno=%s, current=%s,
		// cursor=%s, view=%s", action,
		// inventory.getType(), slotType, event.getSlot(),
		// isCurrentSlot == null ? "null" : isCurrentSlot.getType(),
		// isCursor == null ? "null" : isCursor.getType(),
		// event.getView().getType());
		// }

		if (action == InventoryAction.NOTHING)
			return;

		if (!(slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR || slotType == SlotType.OUTSIDE
				|| slotType == SlotType.RESULT || (slotType == SlotType.ARMOR && event.getSlot() == 39))) {
			if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
				Reward reward = Reward.isReward(isCurrentSlot) ? Reward.getReward(isCurrentSlot)
						: Reward.getReward(isCursor);
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.rewards.no-use",
						"rewardname", reward.getDisplayname()));
				plugin.getMessages().debug("RewardListerner: cancel 1");
				event.setCancelled(true);
				return;
			}
		}

		if (action == InventoryAction.CLONE_STACK || action == InventoryAction.UNKNOWN) {
			if (Reward.isReward(isCurrentSlot) || Reward.isReward(isCursor)) {
				Reward reward = Reward.isReward(isCurrentSlot) ? Reward.getReward(isCurrentSlot)
						: Reward.getReward(isCursor);
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.rewards.no-clone",
						"rewardname", reward.getDisplayname()));
				plugin.getMessages().debug("RewardListerner: cancel 2");
				event.setCancelled(true);
				return;
			}
		}

		if (action == InventoryAction.SWAP_WITH_CURSOR) {
			if (Reward.isReward(isCurrentSlot) && Reward.isReward(isCursor)) {
				event.setCancelled(true);
				ItemMeta imCurrent = isCurrentSlot.getItemMeta();
				ItemMeta imCursor = isCursor.getItemMeta();
				Reward reward1 = new Reward(imCurrent.getLore());
				Reward reward2 = new Reward(imCursor.getLore());
				if ((reward1.isBagOfGoldReward() || reward1.isItemReward())
						&& reward1.getRewardType().equals(reward2.getRewardType())) {
					if (reward2.getMoney() + reward2.getMoney() <= plugin.getConfigManager().limitPerBag) {
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
						event.setCurrentItem(isCursor);
						event.setCursor(isCurrentSlot);
						plugin.getMessages().debug("%s merged two rewards", player.getName());
					} else {
						double rest = reward1.getMoney() + reward2.getMoney() - plugin.getConfigManager().limitPerBag;
						reward2.setMoney(plugin.getConfigManager().limitPerBag);
						imCursor.setLore(reward2.getHiddenLore());
						imCursor.setDisplayName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(plugin.getConfigManager().limitPerBag)
										: reward2.getDisplayname() + " (" + plugin.getRewardManager()
												.format(plugin.getConfigManager().limitPerBag) + ")"));
						isCursor.setItemMeta(imCursor);

						reward1.setMoney(rest);
						imCurrent.setLore(reward1.getHiddenLore());
						imCurrent.setDisplayName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
								+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
										? plugin.getRewardManager().format(plugin.getConfigManager().limitPerBag)
										: reward1.getDisplayname() + " ("
												+ plugin.getRewardManager().format(reward1.getMoney()) + ")"));
						isCurrentSlot.setItemMeta(imCurrent);
						event.setCurrentItem(isCursor);
						event.setCursor(isCurrentSlot);
						plugin.getMessages().debug("%s merged two rewards", player.getName());
					}
				} else {
				}
			}

		} else if (action == InventoryAction.PICKUP_HALF) {
			if (isCursor.getType() == Material.AIR && Reward.isReward(isCurrentSlot)) {
				Reward reward = Reward.getReward(isCurrentSlot);
				if (reward.isBagOfGoldReward() || reward.isItemReward()) {
					double currentSlotMoney = Misc.round(reward.getMoney() / 2);
					double cursorMoney = Misc.round(reward.getMoney() - currentSlotMoney);
					if (cursorMoney >= plugin.getConfigManager().minimumReward) {
						event.setCancelled(true);
						reward.setMoney(currentSlotMoney);
						isCurrentSlot = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCurrentSlot.clone(),
								reward);
						event.setCurrentItem(isCurrentSlot);
						reward.setMoney(cursorMoney);
						isCursor = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCurrentSlot.clone(),
								reward);
						event.setCursor(isCursor);
						plugin.getMessages().debug("%s halfed a reward in two (%s,%s)", player.getName(),
								plugin.getRewardManager().format(currentSlotMoney),
								plugin.getRewardManager().format(cursorMoney));
					}
				} else if (reward.isKilledHeadReward() || reward.isKilledHeadReward()) {

				}
			}
		} else if (action == InventoryAction.COLLECT_TO_CURSOR) {
			if (Reward.isReward(isCursor)) {
				Reward cursor = Reward.getReward(isCursor);
				if (cursor.isBagOfGoldReward() || cursor.isItemReward()) {
					double saldo = Misc.floor(cursor.getMoney());
					for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
						ItemStack is = player.getInventory().getItem(slot);
						if (Reward.isReward(is)) {
							Reward reward = Reward.getReward(is);
							if ((reward.isBagOfGoldReward() || reward.isItemReward()) && reward.getMoney() > 0) {
								saldo = saldo + reward.getMoney();
								if (saldo <= plugin.getConfigManager().limitPerBag)
									player.getInventory().clear(slot);
								else {
									reward.setMoney(plugin.getConfigManager().limitPerBag);
									is = plugin.getRewardManager().setDisplayNameAndHiddenLores(is.clone(), reward);
									is.setAmount(1);
									// event.setCurrentItem(is);
									player.getInventory().clear(slot);
									player.getInventory().addItem(is);
									saldo = saldo - plugin.getConfigManager().limitPerBag;
								}
							}
						}
					}
					cursor.setMoney(saldo);
					isCursor = plugin.getRewardManager().setDisplayNameAndHiddenLores(isCursor.clone(), cursor);
					event.setCursor(isCursor);
				}
			}
		}
	}

}
