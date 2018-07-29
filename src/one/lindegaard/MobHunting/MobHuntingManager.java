package one.lindegaard.MobHunting;

import com.gmail.nossr50.datatypes.skills.SkillType;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.MobHunting.bounty.Bounty;
import one.lindegaard.MobHunting.bounty.BountyStatus;
import one.lindegaard.MobHunting.compatibility.*;
import one.lindegaard.MobHunting.events.BountyKillEvent;
import one.lindegaard.MobHunting.events.MobHuntEnableCheckEvent;
import one.lindegaard.MobHunting.events.MobHuntKillEvent;
import one.lindegaard.MobHunting.grinding.Area;
import one.lindegaard.MobHunting.mobs.ExtendedMob;
import one.lindegaard.MobHunting.mobs.MinecraftMob;
import one.lindegaard.MobHunting.modifier.*;
import one.lindegaard.MobHunting.placeholder.PlaceHolderData;
import one.lindegaard.MobHunting.rewards.CustomItems;
import one.lindegaard.MobHunting.update.SpigetUpdater;
import one.lindegaard.MobHunting.util.Misc;

import org.bukkit.*;
import org.bukkit.command.CommandException;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MobHuntingManager implements Listener {

	private MobHunting plugin;
	private final String SPAWNER_BLOCKED = "MH:SpawnerBlocked";

	private static WeakHashMap<LivingEntity, DamageInformation> mDamageHistory = new WeakHashMap<>();
	private Set<IModifier> mHuntingModifiers = new HashSet<>();

	/**
	 * Constructor for MobHuntingManager
	 *
	 * @param instance
	 */
	public MobHuntingManager(MobHunting instance) {
		this.plugin = instance;
		registerHuntingModifiers();
		Bukkit.getServer().getPluginManager().registerEvents(this, instance);
	}

	/**
	 * Gets the DamageInformation for a LivingEntity
	 *
	 * @param entity
	 * @return
	 */
	public DamageInformation getDamageInformation(Entity entity) {
		return mDamageHistory.get(entity);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		setHuntEnabled(player, true);
		if (player.hasPermission("mobhunting.update") && plugin.getConfigManager().updateCheck) {
			new BukkitRunnable() {
				@Override
				public void run() {
					new SpigetUpdater(plugin).checkForUpdate(player, true);
				}
			}.runTaskLater(plugin, 20L);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		HuntData data = new HuntData(player);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + "" + ChatColor.ITALIC
					+ plugin.getMessages().getString("mobhunting.killstreak.ended"));
		}
		data.setKillStreak(0);
		data.putHuntDataToPlayer(player);
	}

	/**
	 * Set if MobHunting is allowed for the player
	 *
	 * @param player
	 * @param enabled
	 *            = true : means the MobHunting is allowed
	 */
	public void setHuntEnabled(Player player, boolean enabled) {
		player.setMetadata("MH:enabled", new FixedMetadataValue(plugin, enabled));
	}

	/**
	 * Checks if MobHunting is enabled for the player
	 *
	 * @param player
	 * @return true if MobHunting is enabled for the player, false if not.
	 */
	public boolean isHuntEnabled(Player player) {
		if (CitizensCompat.isNPC(player))
			return false;

		if (!player.hasMetadata("MH:enabled")) {
			plugin.getMessages().debug("KillBlocked %s: Player doesnt have MH:enabled", player.getName());
			return false;
		}

		List<MetadataValue> values = player.getMetadata("MH:enabled");

		// Use the first value that matches the required type
		boolean enabled = false;
		for (MetadataValue value : values) {
			if (value.value() instanceof Boolean)
				enabled = value.asBoolean();
		}

		if (enabled && !player.hasPermission("mobhunting.enable")) {
			plugin.getMessages().debug("KillBlocked %s: Player doesnt have permission mobhunting.enable",
					player.getName());
			return false;
		}

		if (!enabled) {
			plugin.getMessages().debug("KillBlocked %s: MH:enabled is false", player.getName());
			return false;
		}

		MobHuntEnableCheckEvent event = new MobHuntEnableCheckEvent(player);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isEnabled())
			plugin.getMessages().debug("KillBlocked %s: Plugin cancelled check", player.getName());
		return event.isEnabled();
	}

	private void registerHuntingModifiers() {
		mHuntingModifiers.add(new BonusMobBonus());
		mHuntingModifiers.add(new BrawlerBonus());
		if (ConquestiaMobsCompat.isSupported())
			mHuntingModifiers.add(new ConquestiaBonus());
		if (LorinthsRpgMobsCompat.isSupported())
			mHuntingModifiers.add(new LorinthsBonus());
		mHuntingModifiers.add(new CoverBlown());
		mHuntingModifiers.add(new CriticalModifier());
		mHuntingModifiers.add(new DifficultyBonus());
		if (FactionsHelperCompat.isSupported())
			mHuntingModifiers.add(new FactionWarZoneBonus());
		mHuntingModifiers.add(new FlyingPenalty());
		mHuntingModifiers.add(new FriendleFireBonus());
		if (plugin.getConfigManager().grindingDetectionEnabled)
			mHuntingModifiers.add(new GrindingPenalty());
		mHuntingModifiers.add(new HappyHourBonus());
		mHuntingModifiers.add(new MountedBonus());
		mHuntingModifiers.add(new ProSniperBonus());
		mHuntingModifiers.add(new RankBonus());
		mHuntingModifiers.add(new ReturnToSenderBonus());
		mHuntingModifiers.add(new ShoveBonus());
		mHuntingModifiers.add(new SneakyBonus());
		mHuntingModifiers.add(new SniperBonus());
		if (MobStackerCompat.isSupported() || StackMobCompat.isSupported())
			mHuntingModifiers.add(new StackedMobBonus());
		mHuntingModifiers.add(new Undercover());
		if (CrackShotCompat.isSupported())
			mHuntingModifiers.add(new CrackShotPenalty());
		if (InfernalMobsCompat.isSupported())
			mHuntingModifiers.add(new InfernalMobBonus());
	}

	/**
	 * Check if MobHunting is allowed in world
	 *
	 * @param world
	 * @return true if MobHunting is allowed.
	 */
	public boolean isHuntEnabledInWorld(World world) {
		if (world != null)
			for (String worldName : plugin.getConfigManager().disabledInWorlds) {
				if (world.getName().equalsIgnoreCase(worldName))
					return false;
			}

		return true;
	}

	/**
	 * Checks if the player has permission to kill the mob
	 *
	 * @param player
	 * @param mob
	 * @return true if the player has permission to kill the mob
	 */
	public boolean hasPermissionToKillMob(Player player, LivingEntity mob) {
		String permission_postfix = "*";
		if (TARDISWeepingAngelsCompat.isWeepingAngelMonster(mob)) {
			permission_postfix = TARDISWeepingAngelsCompat.getWeepingAngelMonsterType(mob).name();
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages()
						.debug("Permission mobhunting.mobs." + permission_postfix + " not set, defaulting to True.");
				return true;
			}
		} else if (MythicMobsCompat.isMythicMob(mob)) {
			permission_postfix = MythicMobsCompat.getMythicMobType(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages()
						.debug("Permission mobhunting.mobs." + permission_postfix + " not set, defaulting to True.");
				return true;
			}
		} else if (CitizensCompat.isSentryOrSentinelOrSentries(mob)) {
			permission_postfix = "npc-" + CitizensCompat.getNPCId(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages()
						.debug("Permission mobhunting.mobs.'" + permission_postfix + "' not set, defaulting to True.");
				return true;
			}
		} else if (CustomMobsCompat.isCustomMob(mob)) {
			permission_postfix = CustomMobsCompat.getCustomMobType(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages()
						.debug("Permission mobhunting.mobs.'" + permission_postfix + "' not set, defaulting to True.");
				return true;
			}
		} else if (MysteriousHalloweenCompat.isMysteriousHalloween(mob)) {
			permission_postfix = "npc-" + MysteriousHalloweenCompat.getMysteriousHalloweenType(mob);
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages()
						.debug("Permission mobhunting.mobs.'" + permission_postfix + "' not set, defaulting to True.");
				return true;
			}
		} else {
			permission_postfix = mob.getType().toString();
			if (player.isPermissionSet("mobhunting.mobs." + permission_postfix))
				return player.hasPermission("mobhunting.mobs." + permission_postfix);
			else {
				plugin.getMessages().debug("Permission 'mobhunting.mobs.*' or 'mobhunting.mobs." + permission_postfix
						+ "' not set, defaulting to True.");
				return true;
			}
		}
	}

	// ************************************************************************************
	// EVENTS
	// ************************************************************************************
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerDeath(PlayerDeathEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()) || !isHuntEnabled(event.getEntity()))
			return;

		Player killed = event.getEntity();

		HuntData data = new HuntData(killed);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue((Player) event.getEntity(), ChatColor.RED + ""
					+ ChatColor.ITALIC + plugin.getMessages().getString("mobhunting.killstreak.ended"));
		}
		plugin.getMessages().debug("%s died - Killstreak ended", killed.getName());
		data.resetKillStreak(killed);

		if (CitizensCompat.isNPC(killed))
			return;

		EntityDamageEvent lastDamageCause = killed.getLastDamageCause();
		if (lastDamageCause instanceof EntityDamageByEntityEvent) {
			Entity damager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
			Player killer = null;
			LivingEntity mob = null;

			if (damager instanceof Player)
				killer = (Player) damager;
			else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player)
				killer = (Player) ((Projectile) damager).getShooter();
			else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof LivingEntity)
				mob = (LivingEntity) ((Projectile) damager).getShooter();
			else if (damager instanceof LivingEntity)
				mob = (LivingEntity) damager;
			else if (damager instanceof Projectile) {
				if (((Projectile) damager).getShooter() != null)
					plugin.getMessages().debug("%s was killed by a %s shot by %s", killed.getName(), damager.getName(),
							((Projectile) damager).getShooter().toString());
				else
					plugin.getMessages().debug("%s was killed by a %s", killed.getName(), damager.getName());
			}

			plugin.getMessages().debug("%s was killed by a %s", killed.getName(), damager.getName());
			if (damager instanceof Projectile)
				plugin.getMessages().debug("and shooter was %s", ((Projectile) damager).getShooter().toString());

			// MobArena
			if (MobArenaCompat.isPlayingMobArena((Player) killed) && !plugin.getConfigManager().mobarenaGetRewards) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing MobArena.", killed.getName());
				return;
				// PVPArena
			} else if (PVPArenaCompat.isPlayingPVPArena((Player) killed)
					&& !plugin.getConfigManager().pvparenaGetRewards) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing PvpArena.", killed.getName());
				return;
				// BattleArena
			} else if (BattleArenaCompat.isPlayingBattleArena((Player) killed)) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing BattleArena.", killed.getName());
				return;
			}

			if (mob != null) {
				double playerKilledByMobPenalty = 0;

				playerKilledByMobPenalty = plugin.getRewardManager().getPlayerKilledByMobPenalty(killed,
						event.getDrops());

				if (playerKilledByMobPenalty != 0) {
					if (BagOfGoldCompat.isSupported()) {
						//TODO: cleanup here
						event.getDrops().add(new ItemStack(Material.DIRT));
						plugin.getMessages().debug("penalty=%s", playerKilledByMobPenalty);
						BagOfGold.getAPI().getEconomyManager().removeMoneyFromBalance(killed, playerKilledByMobPenalty);
						//BagOfGold.getAPI().getEconomyManager().withdrawPlayer(killed, playerKilledByMobPenalty);
					} else if (plugin.getConfigManager().dropMoneyOnGroundUseAsCurrency) {
						plugin.getRewardManager().withdrawPlayer(killed, playerKilledByMobPenalty);
					} else {
						plugin.getRewardManager().getEconomy().withdrawPlayer(killed, playerKilledByMobPenalty);
					}
					boolean killed_muted = false;
					if (plugin.getPlayerSettingsManager().containsKey(killed))
						killed_muted = plugin.getPlayerSettingsManager().getPlayerSettings(killed).isMuted();
					if (!killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue(killed,
								ChatColor.RED + "" + ChatColor.ITALIC
										+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
												plugin.getRewardManager().format(playerKilledByMobPenalty), "money",
												plugin.getRewardManager().format(playerKilledByMobPenalty)));
					}
					plugin.getMessages().debug("%s lost %s for being killed by a %s", killed.getName(),
							plugin.getRewardManager().format(playerKilledByMobPenalty), mob.getName());
				} else {
					plugin.getMessages().debug("There is NO penalty for being killed by a %s", mob.getName());
				}

			} else if (killer != null && BagOfGoldCompat.isSupported()) {
				PlayerBalance ps = BagOfGold.getAPI().getPlayerBalanceManager().getPlayerBalance(killed);
				double balance = ps.getBalance() + ps.getBalanceChanges();
				if (balance != 0) {
					plugin.getMessages().debug("%s dropped %s because of his death, killed by %s", killed.getName(),
							plugin.getRewardManager().format(balance), killer.getName());
					BagOfGold.getAPI().getEconomyManager().removeMoneyFromBalance(killed, balance);

				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		if (!isHuntEnabledInWorld(event.getEntity().getWorld()) || !isHuntEnabled((Player) event.getEntity()))
			return;

		Player player = (Player) event.getEntity();
		HuntData data = new HuntData(player);
		if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
			plugin.getMessages().playerActionBarMessageQueue(player, ChatColor.RED + "" + ChatColor.ITALIC
					+ plugin.getMessages().getString("mobhunting.killstreak.ended"));
			plugin.getMessages().debug("%s was hit - Killstreak ended", player.getName());
		}
		data.resetKillStreak(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onSkeletonShoot(ProjectileLaunchEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;

		if (event.getEntity() instanceof Arrow) {
			if (event.getEntity().getShooter() instanceof Skeleton) {
				Skeleton shooter = (Skeleton) event.getEntity().getShooter();
				if (shooter.getTarget() instanceof Player && isHuntEnabled((Player) shooter.getTarget())
						&& ((Player) shooter.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = null;
					info = mDamageHistory.get(shooter);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) shooter.getTarget());
					info.setAttackerPosition(shooter.getTarget().getLocation().clone());
					mDamageHistory.put(shooter, info);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onFireballShoot(ProjectileLaunchEvent event) {
		if (!isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;

		if (event.getEntity() instanceof Fireball) {
			if (event.getEntity().getShooter() instanceof Blaze) {
				Blaze blaze = (Blaze) event.getEntity().getShooter();
				if (blaze.getTarget() instanceof Player && isHuntEnabled((Player) blaze.getTarget())
						&& ((Player) blaze.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = mDamageHistory.get(blaze);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) blaze.getTarget());
					info.setAttackerPosition(blaze.getTarget().getLocation().clone());
					mDamageHistory.put(blaze, info);
				}
			} else if (event.getEntity().getShooter() instanceof Wither) {
				Wither wither = (Wither) event.getEntity().getShooter();
				if (wither.getTarget() instanceof Player && isHuntEnabled((Player) wither.getTarget())
						&& ((Player) wither.getTarget()).getGameMode() != GameMode.CREATIVE) {
					DamageInformation info = null;
					info = mDamageHistory.get(wither);
					if (info == null)
						info = new DamageInformation();
					info.setTime(System.currentTimeMillis());
					info.setAttacker((Player) wither.getTarget());
					info.setAttackerPosition(wither.getTarget().getLocation().clone());
					mDamageHistory.put(wither, info);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMobDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof LivingEntity) || !isHuntEnabledInWorld(event.getEntity().getWorld()))
			return;// ok
		Entity damager = event.getDamager();
		Entity damaged = event.getEntity();

		// check if damager or damaged is Sentry / Sentinel. Only Sentry gives a
		// reward.
		if (CitizensCompat.isNPC(damaged) && !CitizensCompat.isSentryOrSentinelOrSentries(damaged))
			return;

		if (WorldGuardCompat.isSupported()
				&& !WorldGuardHelper.isAllowedByWorldGuard(damager, damaged, DefaultFlag.MOB_DAMAGE, true)) {
			return;
		}

		if (damager instanceof Player && (PreciousStonesCompat.isMobDamageProtected((Player) damager)
				|| PreciousStonesCompat.isPVPProtected((Player) damager)))
			return;

		if (CrackShotCompat.isSupported() && CrackShotCompat.isCrackShotUsed(damaged)) {
			return;
		}

		DamageInformation info = null;
		info = mDamageHistory.get(damaged);
		if (info == null)
			info = new DamageInformation();

		info.setTime(System.currentTimeMillis());

		Player cause = null;
		ItemStack weapon = null;

		if (damager instanceof Player) {
			cause = (Player) damager;
		}

		boolean projectile = false;
		if (damager instanceof Projectile) {
			if (((Projectile) damager).getShooter() instanceof Player)
				cause = (Player) ((Projectile) damager).getShooter();

			if (damager instanceof ThrownPotion)
				weapon = ((ThrownPotion) damager).getItem();

			info.setIsMeleWeaponUsed(false);
			projectile = true;

			if (CrackShotCompat.isCrackShotProjectile((Projectile) damager)) {
				info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon((Projectile) damager));
			}

		} else
			info.setIsMeleWeaponUsed(true);

		if (MyPetCompat.isMyPet(damager)) {
			cause = MyPetCompat.getMyPetOwner(damaged);
			info.setIsMeleWeaponUsed(false);
			info.setIsMyPetAssist(true);
		} else if (damager instanceof Wolf && ((Wolf) damager).isTamed()
				&& ((Wolf) damager).getOwner() instanceof Player) {
			cause = (Player) ((Wolf) damager).getOwner();
			info.setIsMeleWeaponUsed(false);
			info.setIsMyPetAssist(true);
		}

		if (weapon == null && cause != null) {
			if (Misc.isMC19OrNewer() && projectile) {
				PlayerInventory pi = cause.getInventory();
				if (pi.getItemInMainHand().getType() == Material.BOW)
					weapon = pi.getItemInMainHand();
				else
					weapon = pi.getItemInOffHand();
			} else {
				weapon = cause.getItemInHand();
			}
			if (CrackShotCompat.isCrackShotWeapon(weapon)) {
				info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon(weapon));
				plugin.getMessages().debug("%s used a CrackShot weapon: %s", cause.getName(),
						info.getCrackShotWeaponUsed());
			}
		}

		if (weapon != null)
			info.setWeapon(weapon);

		// Take note that a weapon has been used at all
		if (info.getWeapon() != null && (Misc.isSword(info.getWeapon()) || Misc.isAxe(info.getWeapon())
				|| Misc.isPick(info.getWeapon()) || info.isCrackShotWeaponUsed() || projectile))
			info.setHasUsedWeapon(true);

		if (cause != null) {
			if (cause != info.getAttacker()) {
				info.setAssister(info.getAttacker());
				info.setLastAssistTime(info.getLastAttackTime());
			}

			info.setLastAttackTime(System.currentTimeMillis());

			info.setAttacker(cause);
			if (cause.isFlying() && !cause.isInsideVehicle())
				info.setWasFlying(true);

			info.setAttackerPosition(cause.getLocation().clone());

			if (!info.isPlayerUndercover())
				if (DisguisesHelper.isDisguised(cause)) {
					if (DisguisesHelper.isDisguisedAsAgresiveMob(cause)) {
						plugin.getMessages().debug("[MobHunting] %s was under cover - diguised as an agressive mob",
								cause.getName());
						info.setPlayerUndercover(true);
					} else
						plugin.getMessages().debug("[MobHunting] %s was under cover - diguised as an passive mob",
								cause.getName());
					if (plugin.getConfigManager().removeDisguiseWhenAttacking) {
						DisguisesHelper.undisguiseEntity(cause);
						// if (cause instanceof Player)
						plugin.getMessages().playerActionBarMessageQueue(cause, ChatColor.GREEN + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("bonus.undercover.message", "cause", cause.getName()));
						if (damaged instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue((Player) damaged,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.undercover.message", "cause", cause.getName()));
						}
					}
				}

			if (!info.isMobCoverBlown())
				if (DisguisesHelper.isDisguised(damaged)) {
					if (DisguisesHelper.isDisguisedAsAgresiveMob(damaged)) {
						plugin.getMessages().debug("[MobHunting] %s Cover blown, diguised as an agressive mob",
								damaged.getName());
						info.setMobCoverBlown(true);
					} else
						plugin.getMessages().debug("[MobHunting] %s Cover Blown, diguised as an passive mob",
								damaged.getName());
					if (plugin.getConfigManager().removeDisguiseWhenAttacked) {
						DisguisesHelper.undisguiseEntity(damaged);
						if (damaged instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue((Player) damaged,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.coverblown.message", "damaged", damaged.getName()));
						}
						if (cause instanceof Player) {
							plugin.getMessages().playerActionBarMessageQueue(cause,
									ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
											.getString("bonus.coverblown.message", "damaged", damaged.getName()));
						}
					}
				}

			mDamageHistory.put((LivingEntity) damaged, info);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	private void onMobDeath(EntityDeathEvent event) {
		LivingEntity killed = event.getEntity();

		Player killer = event.getEntity().getKiller();
		ExtendedMob mob = plugin.getExtendedMobManager().getExtendedMobFromEntity(killed);
		if (mob.getMob_id() == 0) {
			return;
		}

		// Grinding Farm detections
		if (plugin.getConfigManager().grindingDetectionEnabled && plugin.getConfigManager().detectFarms
				&& !plugin.getGrindingManager().isGrindingDisabledInWorld(event.getEntity().getWorld())) {
			if (killed.getLastDamageCause() != null) {
				if (!plugin.getGrindingManager().isWhitelisted(killed.getLocation())) {
					if (killed.getLastDamageCause().getCause() == DamageCause.FALL) {
						plugin.getMessages().debug("===================== Farm detection =======================");
						plugin.getGrindingManager().registerDeath(killed);
						if (plugin.getConfigManager().detectNetherGoldFarms
								&& plugin.getGrindingManager().isNetherGoldXPFarm(killed)) {
							cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnNetherGoldFarms,
									plugin.getConfigManager().disableNaturalXPDropsOnNetherGoldFarms);
							if (getPlayer(killer, killed) != null) {
								if ((plugin.getPlayerSettingsManager().containsKey(getPlayer(killer, killed))
										&& plugin.getPlayerSettingsManager()
												.getPlayerSettings(getPlayer(killer, killed)).isLearningMode())
										|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist")
										|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist.show"))
									ProtocolLibHelper.showGrindingArea(getPlayer(killer, killed),
											new Area(killed.getLocation(),
													plugin.getConfigManager().rangeToSearchForGrinding,
													plugin.getConfigManager().numberOfDeathsWhenSearchingForGringding),
											killed.getLocation());
								plugin.getMessages().learn(getPlayer(killer, killed),
										plugin.getMessages().getString("mobhunting.learn.grindingfarm"));
							}
							plugin.getMessages().debug("================== Farm detection Ended (1)=================");
							return;
						}
						if (plugin.getConfigManager().detectOtherFarms
								&& plugin.getGrindingManager().isOtherFarm(killed)) {
							cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnOtherFarms,
									plugin.getConfigManager().disableNaturalXPDropsOnOtherFarms);
							if (getPlayer(killer, killed) != null) {
								if ((plugin.getPlayerSettingsManager().containsKey(getPlayer(killer, killed))
										&& plugin.getPlayerSettingsManager()
												.getPlayerSettings(getPlayer(killer, killed)).isLearningMode())
										|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist.show")
										|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist"))
									ProtocolLibHelper.showGrindingArea(getPlayer(killer, killed),
											new Area(killed.getLocation(),
													plugin.getConfigManager().rangeToSearchForGrinding,
													plugin.getConfigManager().numberOfDeathsWhenSearchingForGringding),
											killed.getLocation());
								plugin.getMessages().learn(getPlayer(killer, killed),
										plugin.getMessages().getString("mobhunting.learn.grindingfarm"));
							}
							return;
						}
						plugin.getMessages().debug("================== Farm detection Ended (2)=================");
					} else if (killed.getLastDamageCause().getCause() == DamageCause.ENTITY_ATTACK) {
						// plugin.getMessages().debug("A mob died in an attack:
						// (%s,%s,%s in %s)",
						// killed.getLocation().getX(),
						// killed.getLocation().getY(),
						// killed.getLocation().getZ(),
						// killed.getWorld().getName());
					}
				} else {
					// plugin.getMessages().debug("A mob died in a whitelisted
					// area: (%s,%s,%s in %s)",
					// killed.getLocation().getX(), killed.getLocation().getY(),
					// killed.getLocation().getZ(),
					// killed.getWorld().getName());
				}
			} else {
				// plugin.getMessages().debug("The %s (%s) died without a
				// damageCause.",
				// mob.getName(), mob.getMobPlugin().getName());
				return;
			}
		}

		DamageInformation info = mDamageHistory.get(killed);
		if (info == null) {
			info = new DamageInformation();
		}

		// Killer is not a player and not a MyPet and CrackShot not used.
		if (killer == null && !MyPetCompat.isKilledByMyPet(killed) && !info.isCrackShotWeaponUsed()) {
			return;
		}

		if (killed != null && (killed.getType() == EntityType.UNKNOWN || killed.getType() == EntityType.ARMOR_STAND)) {
			return;
		}

		plugin.getMessages().debug("======================== New kill ==========================");

		// Check if the mob was killed by MyPet and assisted_kill is disabled.
		if (killer == null && MyPetCompat.isKilledByMyPet(killed) && plugin.getConfigManager().enableAssists == false) {
			Player owner = MyPetCompat.getMyPetOwner(killed);
			plugin.getMessages().debug("KillBlocked: %s - Assisted kill is disabled", owner.getName());
			plugin.getMessages().learn(owner,
					plugin.getMessages().getString("mobhunting.learn.assisted-kill-is-disabled"));
			plugin.getMessages().debug("======================= kill ended (1)======================");
			return;
		}

		// Write killer name to Server Log
		if (killer != null)
			plugin.getMessages().debug("%s killed a %s (%s)@(%s:%s,%s,%s)", killer.getName(), mob.getMobName(),
					mob.getMobPlugin().getName(), killer.getWorld().getName(), (int) killer.getLocation().getBlockX(),
					(int) killer.getLocation().getBlockY(), (int) killer.getLocation().getBlockZ());
		else if (MyPetCompat.isKilledByMyPet(killed))
			plugin.getMessages().debug("%s owned by %s killed a %s (%s)@(%s:%s,%s,%s)",
					MyPetCompat.getMyPet(killed).getName(), MyPetCompat.getMyPetOwner(killed).getName(),
					mob.getMobName(), mob.getMobPlugin().getName(),
					MyPetCompat.getMyPetOwner(killed).getWorld().getName(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockX(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockY(),
					(int) MyPetCompat.getMyPetOwner(killed).getLocation().getBlockZ());
		else if (info.isCrackShotWeaponUsed()) {
			if (killer == null) {
				killer = info.getCrackShotPlayer();
				if (killer != null)
					plugin.getMessages().debug("%s killed a %s (%s) using a %s@(%s:%s,%s,%s)", killer.getName(),
							mob.getMobName(), mob.getMobPlugin().getName(), info.getCrackShotWeaponUsed(),
							killer.getWorld().getName(), (int) killer.getLocation().getBlockX(),
							(int) killer.getLocation().getBlockY(), (int) killer.getLocation().getBlockZ());
				else
					plugin.getMessages().debug("No killer was stored in the Damageinformation");
			}
		}

		// Killer is a NPC
		if (killer != null && CitizensCompat.isNPC(killer)) {
			plugin.getMessages().debug("KillBlocked: Killer is a Citizen NPC (ID:%s).",
					CitizensCompat.getNPCId(killer));
			plugin.getMessages().debug("======================= kill ended (2)======================");
			return;
		}

		// Player killed a Stacked Mob
		if (MobStackerCompat.isStackedMob(killed)) {
			if (plugin.getConfigManager().getRewardFromStackedMobs) {
				if (getPlayer(killer, killed) != null) {
					plugin.getMessages().debug("%s killed a stacked mob (%s) No=%s",
							getPlayer(killer, killed).getName(), mob.getMobName(),
							MobStackerCompat.getStackSize(killed));
					if (MobStackerCompat.killHoleStackOnDeath(killed) && MobStackerCompat.multiplyLoot()) {
						plugin.getMessages().debug("Pay reward for no x mob");
					} else {
						// pay reward for one mob, if config allows
						plugin.getMessages().debug("Pay reward for one mob");
					}
				}
			} else {
				plugin.getMessages().debug("KillBlocked: Rewards from StackedMobs is disabled in Config.yml");
				plugin.getMessages().debug("======================= kill ended (3)======================");
				return;
			}
		} else

		// Player killed a Citizens2 NPC
		if (getPlayer(killer, killed) != null && CitizensCompat.isNPC(killed)
				&& CitizensCompat.isSentryOrSentinelOrSentries(killed)) {
			plugin.getMessages().debug("%s killed a Sentinel, Sentries or a Sentry npc-%s (name=%s)",
					getPlayer(killer, killed).getName(), CitizensCompat.getNPCId(killed), mob.getMobName());
		}

		// WorldGuard Compatibility
		if (WorldGuardCompat.isSupported()) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)) {
				Player player = getPlayer(killer, killed);
				if (!WorldGuardHelper.isAllowedByWorldGuard(killer, killed, DefaultFlag.MOB_DAMAGE, true)) {
					plugin.getMessages().debug("KillBlocked: %s is hiding in WG region with mob-damage=DENY",
							killer.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.mob-damage-flag"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					plugin.getMessages().debug("======================= kill ended (4)======================");
					return;
				} else if (!WorldGuardHelper.isAllowedByWorldGuard(killer, killed, WorldGuardHelper.getMobHuntingFlag(),
						true)) {
					plugin.getMessages().debug("KillBlocked: %s is in a protected region mobhunting=DENY",
							killer.getName());
					plugin.getMessages().learn(player,
							plugin.getMessages().getString("mobhunting.learn.mobhunting-deny"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					plugin.getMessages().debug("======================= kill ended (5)======================");
					return;
				}
			}
		}

		if (PreciousStonesCompat.isMobDamageProtected(getPlayer(killer, killed))) {
			Player player = getPlayer(killer, killed);
			plugin.getMessages().debug("KillBlocked: %s is hiding in PreciousStone Field with prevent-mob-damage flag",
					player.getName());
			plugin.getMessages().learn(player,
					plugin.getMessages().getString("mobhunting.learn.prevent-mob-damage-flag"));
			cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
					plugin.getConfigManager().disableNatualXPDrops);
			plugin.getMessages().debug("======================= kill ended (5.5)======================");
			return;
		}

		// Factions Compatibility - no reward when player are in SafeZone
		if (FactionsHelperCompat.isSupported()) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)) {
				Player player = getPlayer(killer, killed);
				if (FactionsHelperCompat.isInSafeZone(player)) {
					plugin.getMessages().debug("KillBlocked: %s is hiding in Factions SafeZone", player.getName());
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.factions-no-rewards-in-safezone"));
					cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
							plugin.getConfigManager().disableNatualXPDrops);
					plugin.getMessages().debug("======================= kill ended (6)======================");
					return;
				}
			}
		}

		// Towny Compatibility - no reward when player are in a protected town
		if (TownyCompat.isSupported()) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)
					&& !(killed instanceof Player)) {
				Player player = getPlayer(killer, killed);
				if (plugin.getConfigManager().disableRewardsInHomeTown && TownyCompat.isInHomeTome(player)) {
					plugin.getMessages().debug("KillBlocked: %s is hiding in his home town", player.getName());
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.towny-no-rewards-in-home-town"));
					cancelDrops(event, plugin.getConfigManager().disableNaturallyRewardsInHomeTown,
							plugin.getConfigManager().disableNaturallyRewardsInHomeTown);
					plugin.getMessages().debug("======================= kill ended (7)======================");
					return;
				}
			}
		}

		// Residence Compatibility - no reward when player are in a protected
		// residence
		if (ResidenceCompat.isSupported()) {
			if ((killer != null || MyPetCompat.isMyPet(killer)) && !CitizensCompat.isNPC(killer)
					&& !(killed instanceof Player)) {
				Player player = getPlayer(killer, killed);
				if (plugin.getConfigManager().disableRewardsInHomeResidence && ResidenceCompat.isProtected(player)) {
					plugin.getMessages().debug("KillBlocked: %s is hiding in a protected residence", player.getName());
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.residence-no-rewards-in-protected-area"));
					cancelDrops(event, plugin.getConfigManager().disableNaturallyRewardsInProtectedResidence,
							plugin.getConfigManager().disableNaturallyRewardsInProtectedResidence);
					plugin.getMessages().debug("======================= kill ended (8)======================");
					return;
				}
			}
		}

		// MobHunting is Disabled in World
		if (!isHuntEnabledInWorld(event.getEntity().getWorld())) {
			if (WorldGuardCompat.isSupported()) {
				if (!CitizensCompat.isNPC(killer)) {
					if (WorldGuardHelper.isAllowedByWorldGuard(killer, killed, WorldGuardHelper.getMobHuntingFlag(),
							false)) {
						plugin.getMessages().debug("KillBlocked %s: Mobhunting disabled in world '%s'",
								getPlayer(killer, killed).getName(), getPlayer(killer, killed).getWorld().getName());
						plugin.getMessages().learn(getPlayer(killer, killed),
								plugin.getMessages().getString("mobhunting.learn.disabled"));
						plugin.getMessages().debug("======================= kill ended (9)======================");
						return;
					} else {
						plugin.getMessages().debug("KillBlocked %s: Mobhunting disabled in world '%s'",
								getPlayer(killer, killed).getName(), getPlayer(killer, killed).getWorld().getName());
						plugin.getMessages().learn(getPlayer(killer, killed),
								plugin.getMessages().getString("mobhunting.learn.disabled"));
						plugin.getMessages().debug("======================= kill ended (10)======================");
						return;
					}
				} else {
					plugin.getMessages()
							.debug("KillBlocked: killer is null and killer was not a MyPet or NPC Sentinel Guard.");
					plugin.getMessages().debug("======================= kill ended (11)=====================");
					return;
				}
			} else {
				// MobHunting is NOT allowed in this world,
				plugin.getMessages().debug("KillBlocked %s: Mobhunting disabled in world '%s'",
						getPlayer(killer, killed).getName(), getPlayer(killer, killed).getWorld().getName());
				plugin.getMessages().learn(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.learn.disabled"));
				plugin.getMessages().debug("======================= kill ended (12)=====================");
				return;
			}
		}

		// Handle Muted mode
		boolean killer_muted = false;
		boolean killed_muted = false;
		if (getPlayer(killer, killed) instanceof Player
				&& plugin.getPlayerSettingsManager().containsKey((Player) getPlayer(killer, killed)))
			killer_muted = plugin.getPlayerSettingsManager().getPlayerSettings(getPlayer(killer, killed)).isMuted();
		if (killed instanceof Player && plugin.getPlayerSettingsManager().containsKey((Player) killed))
			killed_muted = plugin.getPlayerSettingsManager().getPlayerSettings((Player) killed).isMuted();

		// Player died while playing a Minigame: MobArena, PVPArena,
		// BattleArena, Suicide, PVP, penalty when Mobs kills player
		if (killed instanceof Player) {
			// MobArena
			if (MobArenaCompat.isPlayingMobArena((Player) killed) && !plugin.getConfigManager().mobarenaGetRewards) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing MobArena.", mob.getMobName());
				plugin.getMessages().learn(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.learn.mobarena"));
				plugin.getMessages().debug("======================= kill ended (13)=====================");
				return;

				// PVPArena
			} else if (PVPArenaCompat.isPlayingPVPArena((Player) killed)
					&& !plugin.getConfigManager().pvparenaGetRewards) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing PvpArena.", mob.getMobName());
				plugin.getMessages().learn(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.learn.pvparena"));
				plugin.getMessages().debug("======================= kill ended (14)=====================");
				return;

				// BattleArena
			} else if (BattleArenaCompat.isPlayingBattleArena((Player) killed)) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing BattleArena.", mob.getMobName());
				plugin.getMessages().learn(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.learn.battlearena"));
				plugin.getMessages().debug("======================= kill ended (15)=====================");
				return;

				// MiniGamesLib
			} else if (MinigamesLibCompat.isPlayingMinigame((Player) killed)) {
				plugin.getMessages().debug("KillBlocked: %s was killed while playing a MiniGame.", mob.getMobName());
				plugin.getMessages().learn(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.learn.minigameslib"));
				plugin.getMessages().debug("======================= kill ended (16)=====================");
				return;

				//
			} else if (PreciousStonesCompat.isPVPProtected(getPlayer(killer, killed))) {
				Player player = getPlayer(killer, killed);
				plugin.getMessages().debug("KillBlocked: %s is hiding in PreciousStone Field with prevent-pvp flag",
						player.getName());
				plugin.getMessages().learn(player, plugin.getMessages().getString("mobhunting.learn.prevent-pvp-flag"));
				plugin.getMessages().debug("======================= kill ended (16.5)======================");
				return;
			} else if (killer != null) {
				if (killed.equals(killer)) {
					// Suicide
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.suiside"));
					plugin.getMessages().debug("KillBlocked: Suiside not allowed (Killer=%s, Killed=%s)",
							killer.getName(), killed.getName());
					plugin.getMessages().debug("======================= kill ended (17)======================");
					return;
					// PVP
				} else if (!plugin.getConfigManager().pvpAllowed) {
					// PVP
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.nopvp"));
					plugin.getMessages().debug(
							"KillBlocked: Rewards for PVP kill is not allowed in config.yml. %s killed %s.",
							getPlayer(killer, killed).getName(), mob.getMobName());
					plugin.getMessages().debug("======================= kill ended (18)=====================");
					return;
				}
			}
		}

		// Player killed a mob while playing a minigame: MobArena, PVPVArena,
		// BattleArena
		// Player is in Godmode or Vanished
		// Player permission to Hunt (and get rewards)
		if (MobArenaCompat.isPlayingMobArena(getPlayer(killer, killed))
				&& !plugin.getConfigManager().mobarenaGetRewards) {
			plugin.getMessages().debug("KillBlocked: %s is currently playing MobArena.",
					getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.mobarena"));
			plugin.getMessages().debug("======================= kill ended (19)=====================");
			return;
		} else if (PVPArenaCompat.isPlayingPVPArena(getPlayer(killer, killed))
				&& !plugin.getConfigManager().pvparenaGetRewards) {
			plugin.getMessages().debug("KillBlocked: %s is currently playing PvpArena.",
					getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.pvparena"));
			plugin.getMessages().debug("======================= kill ended (20)=====================");
			return;
		} else if (BattleArenaCompat.isPlayingBattleArena(getPlayer(killer, killed))) {
			plugin.getMessages().debug("KillBlocked: %s is currently playing BattleArena.",
					getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.battlearena"));
			plugin.getMessages().debug("======================= kill ended (21)=====================");
			return;
		} else if (EssentialsCompat.isGodModeEnabled(getPlayer(killer, killed))) {
			plugin.getMessages().debug("KillBlocked: %s is in God mode", getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.godmode"));
			cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
					plugin.getConfigManager().disableNatualXPDrops);
			plugin.getMessages().debug("======================= kill ended (22)=====================");
			return;
		} else if (EssentialsCompat.isVanishedModeEnabled(getPlayer(killer, killed))) {
			plugin.getMessages().debug("KillBlocked: %s is in Vanished mode", getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.vanished"));
			plugin.getMessages().debug("======================= kill ended (23)=====================");
			return;
		} else if (VanishNoPacketCompat.isVanishedModeEnabled(getPlayer(killer, killed))) {
			plugin.getMessages().debug("KillBlocked: %s is in Vanished mode", getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.vanished"));
			plugin.getMessages().debug("======================= kill ended (24)=====================");
			return;
		}

		if (!hasPermissionToKillMob(getPlayer(killer, killed), killed)) {
			plugin.getMessages().debug("KillBlocked: %s has not permission to kill %s.",
					getPlayer(killer, killed).getName(), mob.getMobName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.no-permission", "killed-mob", mob.getMobName()));
			plugin.getMessages().debug("======================= kill ended (25a)=====================");
			return;
		}

		if (!plugin.getRewardManager().getMobEnabled(killed)) {
			plugin.getMessages().debug("KillBlocked: %s is disabled in config.yml", mob.getMobName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.mob-disabled", "killed-mob", mob.getMobName()));
			plugin.getMessages().debug("======================= kill ended (25b)=====================");
			return;
		}

		// Mob Spawner / Egg / Egg Dispenser detection
		if (plugin.getConfigManager().grindingDetectionEnabled && event.getEntity().hasMetadata(SPAWNER_BLOCKED)) {
			if (!plugin.getGrindingManager().isWhitelisted(event.getEntity().getLocation())) {
				if (killed != null) {
					plugin.getMessages().debug(
							"KillBlocked %s(%d): Mob has MH:blocked meta (probably spawned from a mob spawner, an egg or a egg-dispenser )",
							event.getEntity().getType(), killed.getEntityId());
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.mobspawner", "killed", mob.getMobName()));
					cancelDrops(event,
							plugin.getConfigManager().disableNaturallyDroppedItemsFromMobSpawnersEggsAndDispensers,
							plugin.getConfigManager().disableNaturallyDroppedXPFromMobSpawnersEggsAndDispensers);
				}
				plugin.getMessages().debug("======================= kill ended (26)======================");
				return;
			}
		}

		// MobHunting is disabled for the player
		if (!isHuntEnabled(getPlayer(killer, killed))) {
			plugin.getMessages().debug("KillBlocked: %s Hunting is disabled for player",
					getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.huntdisabled"));
			plugin.getMessages().debug("======================= kill ended (27)======================");
			return;
		}

		// The player is in Creative mode
		if (getPlayer(killer, killed).getGameMode() == GameMode.CREATIVE) {
			plugin.getMessages().debug("KillBlocked: %s is in creative mode", getPlayer(killer, killed).getName());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.creative"));
			cancelDrops(event, plugin.getConfigManager().tryToCancelNaturalDropsWhenInCreative,
					plugin.getConfigManager().tryToCancelXPDropsWhenInCreative);
			plugin.getMessages().debug("======================= kill ended (28)======================");
			return;
		}

		// Calculate basic the reward
		double cash = plugin.getRewardManager().getBaseKillPrize(killed);
		if (plugin.mRand.nextDouble() > plugin.getRewardManager().getMoneyChance(killed))
			cash = 0;
		double basic_prize = cash;
		plugin.getMessages().debug("Basic Prize=%s for killing a %s", plugin.getRewardManager().format(cash),
				mob.getMobName());

		// There is no reward and no penalty for this kill
		if (basic_prize == 0 && plugin.getRewardManager().getKillCommands(killed).isEmpty()
				&& !plugin.getRewardManager().getHeadDropHead(killed)) {
			plugin.getMessages().debug(
					"KillBlocked %s(%d): There is no reward and no penalty for this Mob/Player and is not counted as kill/achievement.",
					mob.getMobName(), killed.getEntityId());
			plugin.getMessages().learn(getPlayer(killer, killed),
					plugin.getMessages().getString("mobhunting.learn.no-reward", "killed", mob.getMobName()));
			plugin.getMessages().debug("======================= kill ended (29)=====================");
			return;
		}

		// add a multiplier for killing an EliteMob
		if (EliteMobsCompat.isEliteMobs(killed)) {
			int level = EliteMobsCompat.getEliteMobsLevel(killed);
			double mul = 1;
			if (level >= 50)
				mul = plugin.getConfigManager().elitemobMultiplier * (1 + (level - 50) / (400 - 50));
			if (level >= 400)
				mul = plugin.getConfigManager().elitemobMultiplier;
			plugin.getMessages().debug("A level %s %s EliteMob was killed by %s. Multiplier is %s", level,
					mob.getMobName(), getPlayer(killer, killed), mul);
			cash = cash * mul;
		}
		
		// Update DamageInformation
		if (killed instanceof LivingEntity && mDamageHistory.containsKey((LivingEntity) killed)) {
			info = mDamageHistory.get(killed);
			if (System.currentTimeMillis() - info.getTime() > plugin.getConfigManager().assistTimeout * 1000)
				info = null;
			// else
			// else if (killer == null)
			// killer = info.getAttacker();
		}
		if (info == null) {
			info = new DamageInformation();
			info.setTime(System.currentTimeMillis());
			info.setLastAttackTime(info.getTime());
			if (killer != null) {
				info.setAttacker(getPlayer(killer, killed));
				info.setAttackerPosition(getPlayer(killer, killed).getLocation());
				@SuppressWarnings("deprecation")
				ItemStack weapon = killer.getItemInHand();
				if (!weapon.equals(new ItemStack(Material.AIR))) {
					info.setHasUsedWeapon(true);
					if (CrackShotCompat.isCrackShotWeapon(weapon)) {
						info.setCrackShotWeapon(CrackShotCompat.getCrackShotWeapon(weapon));
						plugin.getMessages().debug("%s used a CrackShot weapon: %s", killer.getName(),
								CrackShotCompat.getCrackShotWeapon(weapon));
					}
				}
			}
		}

		// Check if the kill was within the time limit on both kills and
		// assisted kills
		if (((System.currentTimeMillis() - info.getLastAttackTime()) > plugin.getConfigManager().killTimeout * 1000)
				&& (info.isWolfAssist() && ((System.currentTimeMillis()
						- info.getLastAttackTime()) > plugin.getConfigManager().assistTimeout * 1000))) {
			plugin.getMessages().debug("KillBlocked %s: Last damage was too long ago (%s sec.)",
					getPlayer(killer, killed).getName(),
					(System.currentTimeMillis() - info.getLastAttackTime()) / 1000);
			plugin.getMessages().debug("======================= kill ended (30)=====================");
			return;
		}

		// MyPet killed a mob - Assister is the Owner
		if (MyPetCompat.isKilledByMyPet(killed) && plugin.getConfigManager().enableAssists == true) {
			info.setAssister(MyPetCompat.getMyPetOwner(killed));
			plugin.getMessages().debug("MyPetAssistedKill: Pet owned by %s killed a %s", info.getAssister().getName(),
					mob.getMobName());
		}

		if (info.getWeapon() == null)
			info.setWeapon(new ItemStack(Material.AIR));

		// Player or killed Mob is disguised
		if (!info.isPlayerUndercover())
			if (DisguisesHelper.isDisguised(getPlayer(killer, killed))) {
				if (DisguisesHelper.isDisguisedAsAgresiveMob(getPlayer(killer, killed))) {
					info.setPlayerUndercover(true);
				} else if (plugin.getConfigManager().removeDisguiseWhenAttacking) {
					DisguisesHelper.undisguiseEntity(getPlayer(killer, killed));
					if (getPlayer(killer, killed) != null && !killer_muted) {
						plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages().getString(
										"bonus.undercover.message", "cause", getPlayer(killer, killed).getName()));
					}
					if (killed instanceof Player && !killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue((Player) killed,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages().getString(
										"bonus.undercover.message", "cause", getPlayer(killer, killed).getName()));
					}
				}
			}
		if (!info.isMobCoverBlown())
			if (DisguisesHelper.isDisguised(killed)) {
				if (DisguisesHelper.isDisguisedAsAgresiveMob(killed)) {
					info.setMobCoverBlown(true);
				}
				if (plugin.getConfigManager().removeDisguiseWhenAttacked) {
					DisguisesHelper.undisguiseEntity(killed);
					if (killed instanceof Player && !killed_muted) {
						plugin.getMessages().playerActionBarMessageQueue((Player) killed,
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.coverblown.message", "damaged", mob.getMobName()));
					}
					if (getPlayer(killer, killed) != null && !killer_muted) {
						plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
								ChatColor.GREEN + "" + ChatColor.ITALIC + plugin.getMessages()
										.getString("bonus.coverblown.message", "damaged", mob.getMobName()));
					}
				}
			}

		HuntData data = new HuntData(getPlayer(killer, killed));
		if (getPlayer(killer, killed) != null) {
			if (cash != 0 && plugin.getConfigManager().grindingDetectionEnabled
					&& (!plugin.getGrindingManager().isGrindingArea(getPlayer(killer, killed).getLocation())
							|| plugin.getGrindingManager().isWhitelisted(getPlayer(killer, killed).getLocation()))) {
				// Killstreak
				if (killed instanceof Slime) {
					// Tiny Slime and MagmaCube do no damage or very little
					// damage, so Killstreak is
					// not achieved if the mob is small
					Slime slime = (Slime) killed;
					if (slime.getSize() != 1)
						data.handleKillstreak(plugin, getPlayer(killer, killed));
				} else if (killed instanceof MagmaCube) {
					MagmaCube magmaCube = (MagmaCube) killed;
					if (magmaCube.getSize() != 1)
						data.handleKillstreak(plugin, getPlayer(killer, killed));
				} else
					data.handleKillstreak(plugin, getPlayer(killer, killed));
			} else {
				// Killstreak ended. Players started to kill 4 chicken and the
				// one mob to gain 4 x prize
				if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
					plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.RED + ""
							+ ChatColor.ITALIC + plugin.getMessages().getString("mobhunting.killstreak.ended"));
				}
				// plugin.getMessages().debug("%s - Killstreak ended",
				// player.getName());
				data.resetKillStreak(getPlayer(killer, killed));
			}
		} else {
			plugin.getMessages().debug("======================= kill ended (31)=====================");
			return;
		}

		// Record kills that are still within a small area
		Location loc = killed.getLocation();

		// Grinding detection
		if (plugin.getConfigManager().grindingDetectionEnabled
				&& !(cash == 0 && plugin.getRewardManager().getKillCommands(killed).isEmpty())) {
			// Check if the location is marked as a Grinding Area. Whitelist
			// overrules blacklist.

			Area detectedGrindingArea = plugin.getGrindingManager().getGrindingArea(loc);
			if (detectedGrindingArea == null)
				// Check if Players HuntData contains this Grinding Area.
				detectedGrindingArea = data.getPlayerSpecificGrindingArea(loc);
			else {
				if (!plugin.getGrindingManager().isWhitelisted(detectedGrindingArea.getCenter())) {
					if (plugin.getGrindingManager().isGrindingArea(detectedGrindingArea.getCenter()))
						if (plugin.getPlayerSettingsManager().getPlayerSettings(killer).isLearningMode()
								|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist")
								|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist.show"))
							ProtocolLibHelper.showGrindingArea(killer, detectedGrindingArea, killed.getLocation());
					plugin.getMessages().debug("Grinding detected : %s", getPlayer(killer, killed));
					plugin.getMessages().learn(getPlayer(killer, killed),
							plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
					plugin.getMessages().debug("======================= kill ended (32)=====================");
					return;
				}
			}

			plugin.getMessages().debug("Checking if player is grinding within a range of %s blocks",
					data.getcDampnerRange());
			if (!plugin.getGrindingManager().isWhitelisted(loc)) {

				if (detectedGrindingArea != null) {
					data.setLastKillAreaCenter(null);
					data.setDampenedKills(data.getDampenedKills() + 1);
					if (data.getDampenedKills() >= (isSlimeOrMagmaCube(killed) ? 2 : 1)
							* plugin.getConfigManager().grindingDetectionNumberOfDeath) {
						if (plugin.getConfigManager().blacklistPlayerGrindingSpotsServerWorldWide)
							plugin.getGrindingManager().registerKnownGrindingSpot(detectedGrindingArea);
						cancelDrops(event, plugin.getConfigManager().disableNaturalItemDropsOnPlayerGrinding,
								plugin.getConfigManager().disableNaturalXPDropsOnPlayerGrinding);
						plugin.getMessages().debug(
								"DampenedKills reached the limit %s, no rewards paid. Grinding Spot registered.",
								(isSlimeOrMagmaCube(killed) ? 2 : 1)
										* plugin.getConfigManager().grindingDetectionNumberOfDeath);
						if (plugin.getPlayerSettingsManager().getPlayerSettings(getPlayer(killer, killed))
								.isLearningMode() || getPlayer(killer, killed).hasPermission("mobhunting.blacklist")
								|| getPlayer(killer, killed).hasPermission("mobhunting.blacklist.show"))
							ProtocolLibHelper.showGrindingArea(getPlayer(killer, killed), detectedGrindingArea, loc);
						plugin.getMessages().learn(getPlayer(killer, killed),
								plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
						plugin.getMessages().debug("1)Dampenedkilles=%s", data.getDampenedKills());
						plugin.getMessages().debug("======================= kill ended (33)======================");
						return;
					} else {
						plugin.getMessages().debug("DampenedKills=%s", data.getDampenedKills());
					}
				} else {
					if (data.getLastKillAreaCenter() != null) {
						if (loc.getWorld().equals(data.getLastKillAreaCenter().getWorld())) {
							if (loc.distance(data.getLastKillAreaCenter()) < data.getcDampnerRange()
									&& !plugin.getGrindingManager().isWhitelisted(loc)) {
								if (!MobStackerCompat.isSupported() || (MobStackerCompat.isStackedMob(killed)
										&& !MobStackerCompat.isGrindingStackedMobsAllowed())) {
									data.setDampenedKills(data.getDampenedKills() + 1);
									plugin.getMessages().debug("DampenedKills=%s", data.getDampenedKills());
									if (data.getDampenedKills() >= (isSlimeOrMagmaCube(killed) ? 2 : 1)
											* plugin.getConfigManager().grindingDetectionNumberOfDeath / 2) {
										plugin.getMessages().debug(
												"Warning: Grinding detected. Killings too close, adding 1 to DampenedKills.");
										plugin.getMessages().learn(getPlayer(killer, killed),
												plugin.getMessages().getString("mobhunting.learn.grindingnotallowed"));
										plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
												ChatColor.RED + plugin.getMessages()
														.getString("mobhunting.grinding.detected"));
										data.recordGrindingArea();
										cancelDrops(event, plugin.getConfigManager().disableNaturalItemDrops,
												plugin.getConfigManager().disableNatualXPDrops);
									}
								}
							} else {
								data.setLastKillAreaCenter(loc.clone());
								plugin.getMessages().debug(
										"Kill not within %s blocks from previous kill. DampenedKills reset to 0",
										data.getcDampnerRange());
								data.setDampenedKills(0);
							}
						} else {
							data.setLastKillAreaCenter(loc.clone());
							plugin.getMessages().debug("Kill in new world. DampenedKills reset to 0");
							data.setDampenedKills(0);
						}
					} else {
						data.setLastKillAreaCenter(loc.clone());
						plugin.getMessages().debug("Last Kill Area Center was null. DampenedKills reset to 0");
						data.setDampenedKills(0);
					}
				}
				// }

				if (data.getDampenedKills() > (isSlimeOrMagmaCube(killed) ? 2 : 1)
						* plugin.getConfigManager().grindingDetectionNumberOfDeath / 2 + 4
						&& !plugin.getGrindingManager().isWhitelisted(loc)) {
					if (data.getKillstreakLevel() != 0 && data.getKillstreakMultiplier() != 1) {
						plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
								ChatColor.RED + plugin.getMessages().getString("mobhunting.killstreak.lost"));
					}
					plugin.getMessages().debug("KillStreak reset to 0");
					data.setKillStreak(0);
				}
				data.putHuntDataToPlayer(getPlayer(killer, killed));
			} else {
				plugin.getMessages().debug("Area is whitelisted. Grinding not detected.");
			}
		} else {
			plugin.getMessages().debug("Grinding detection is disabled in config.yml");
		}

		// Apply the modifiers to Basic reward
		EntityDamageByEntityEvent lastDamageCause = null;
		if (killed.getLastDamageCause() instanceof EntityDamageByEntityEvent)
			lastDamageCause = (EntityDamageByEntityEvent) killed.getLastDamageCause();
		double multipliers = 1.0;
		ArrayList<String> modifiers = new ArrayList<String>();
		// only add modifiers if the killer is the player.
		for (IModifier mod : mHuntingModifiers) {
			if (mod.doesApply(killed, getPlayer(killer, killed), data, info, lastDamageCause)) {
				double amt = mod.getMultiplier(killed, getPlayer(killer, killed), data, info, lastDamageCause);
				if (amt != 1.0) {
					modifiers.add(mod.getName());
					multipliers *= amt;
					data.addModifier(mod.getName(), amt);
					plugin.getMessages().debug("Multiplier: %s = %s", mod.getName(), amt);
				}
			}
		}
		data.setReward(cash);
		data.putHuntDataToPlayer(getPlayer(killer, killed));

		plugin.getMessages().debug("Killstreak=%s, level=%s, multiplier=%s ", data.getKillStreak(),
				data.getKillstreakLevel(), data.getKillstreakMultiplier());
		multipliers *= data.getKillstreakMultiplier();

		String extraString = "";

		// Only display the multiplier if its not 1
		if (Math.abs(multipliers - 1) > 0.05)
			extraString += String.format("x%.1f", multipliers);

		// Add on modifiers
		for (String modifier : modifiers)
			extraString += ChatColor.WHITE + " * " + modifier;

		cash *= multipliers;

		cash = Misc.ceil(cash);

		// Handle Bounty Kills
		double reward = 0;
		if (plugin.getConfigManager().enablePlayerBounties && killed instanceof Player) {
			plugin.getMessages().debug("This was a PVP kill (killed=%s), number of bounties=%s", killed.getName(),
					plugin.getBountyManager().getAllBounties().size());
			OfflinePlayer wantedPlayer = (OfflinePlayer) killed;
			String worldGroupName = plugin.getWorldGroupManager().getCurrentWorldGroup(getPlayer(killer, killed));
			if (plugin.getBountyManager().hasOpenBounties(wantedPlayer)) {
				BountyKillEvent bountyEvent = new BountyKillEvent(worldGroupName, getPlayer(killer, killed),
						wantedPlayer, plugin.getBountyManager().getOpenBounties(worldGroupName, wantedPlayer));
				Bukkit.getPluginManager().callEvent(bountyEvent);
				if (bountyEvent.isCancelled()) {
					plugin.getMessages().debug("KillBlocked %s: BountyKillEvent was cancelled",
							(killer != null ? killer : info.getAssister()).getName());
					plugin.getMessages().debug("======================= kill ended (34)=====================");
					return;
				}
				Set<Bounty> bounties = plugin.getBountyManager().getOpenBounties(worldGroupName, wantedPlayer);
				for (Bounty b : bounties) {
					reward += b.getPrize();
					OfflinePlayer bountyOwner = b.getBountyOwner();
					plugin.getBountyManager().delete(b);
					if (bountyOwner != null && bountyOwner.isOnline()) {
						plugin.getMessages().playerActionBarMessageQueue(Misc.getOnlinePlayer(bountyOwner),
								plugin.getMessages().getString("mobhunting.bounty.bounty-claimed", "killer",
										getPlayer(killer, killed).getName(), "prize",
										plugin.getRewardManager().format(b.getPrize()), "money",
										plugin.getRewardManager().format(b.getPrize()), "killed", killed.getName()));
					}
					b.setStatus(BountyStatus.completed);
					plugin.getDataStoreManager().updateBounty(b);
				}
				plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
						plugin.getMessages().getString("mobhunting.moneygain-for-killing", "prize",
								plugin.getRewardManager().format(reward), "money",
								plugin.getRewardManager().format(reward), "killed", killed.getName()));
				plugin.getMessages().debug("Bounty: %s got %s for killing %s", getPlayer(killer, killed).getName(),
						reward, killed.getName());
				plugin.getRewardManager().depositPlayer(getPlayer(killer, killed), reward);
				// plugin.getMessages().debug("RecordCash: %s killed a %s (%s)
				// Cash=%s",
				// killer.getName(), mob.getName(),
				// mob.getMobPlugin().name(), cash);
				// plugin.getDataStoreManager().recordCash(killer, mob,
				// killed.hasMetadata("MH:hasBonus"), cash);

			} else {
				plugin.getMessages().debug("There is no Bounty on %s", killed.getName());
			}
		}

		cash = Misc.round(cash);
		plugin.getMessages().debug("Reward rounded to %s", cash);
		
		// Check if there is a reward for this kill
		if (cash >= plugin.getConfigManager().minimumReward || cash <= -plugin.getConfigManager().minimumReward
				|| !plugin.getRewardManager().getKillCommands(killed).isEmpty()
				|| (killer != null && McMMOCompat.isSupported() && plugin.getConfigManager().enableMcMMOLevelRewards)
				|| plugin.getRewardManager().getHeadDropHead(killed)) {

			// Remember: Handle MobHuntKillEvent and Record Hunt Achievement is
			// done using
			// EighthsHuntAchievement.java (onKillCompleted)
			MobHuntKillEvent event2 = new MobHuntKillEvent(data, info, killed, getPlayer(killer, killed));
			Bukkit.getPluginManager().callEvent(event2);
			// Check if Event is cancelled before paying the reward
			if (event2.isCancelled()) {
				plugin.getMessages().debug("KillBlocked %s: MobHuntKillEvent was cancelled",
						getPlayer(killer, killed).getName());
				plugin.getMessages().debug("======================= kill ended (35)=====================");
				return;
			}

			// Record the kill in the Database
			if (info.getAssister() == null || plugin.getConfigManager().enableAssists == false) {
				plugin.getMessages().debug("RecordKill: %s killed a %s (%s) Cash=%s",
						getPlayer(killer, killed).getName(), mob.getMobName(), mob.getMobPlugin().name(),
						plugin.getRewardManager().format(cash));
				plugin.getDataStoreManager().recordKill(getPlayer(killer, killed), mob,
						killed.hasMetadata("MH:hasBonus"), cash);
			} else {
				if (MyPetCompat.isKilledByMyPet(killed))
					plugin.getMessages().debug("RecordAssistedKill: %s killed a %s (%s) Cash=%s",
							getPlayer(killer, killed).getName() + "/" + MyPetCompat.getMyPet(killed).getName(),
							mob.getMobName(), mob.getMobPlugin().name(), plugin.getRewardManager().format(cash));

				else
					plugin.getMessages().debug("RecordAssistedKill: %s killed a %s (%s) Cash=%s",
							getPlayer(killer, killed).getName() + "/" + info.getAssister().getName(), mob.getMobName(),
							mob.getMobPlugin().name(), plugin.getRewardManager().format(cash));
				plugin.getDataStoreManager().recordAssist(getPlayer(killer, killed), killer, mob,
						killed.hasMetadata("MH:hasBonus"), cash);
			}
		} else {
			plugin.getMessages().debug("KillBlocked %s: There is now reward for killing a %s",
					getPlayer(killer, killed).getName(), mob.getMobName());
			plugin.getMessages().debug("======================= kill ended (36)=====================");
			return;
		}

		String worldname = getPlayer(killer, killed).getWorld().getName();
		String killerpos = getPlayer(killer, killed).getLocation().getBlockX() + " "
				+ getPlayer(killer, killed).getLocation().getBlockY() + " "
				+ getPlayer(killer, killed).getLocation().getBlockZ();
		String killedpos = killed.getLocation().getBlockX() + " " + killed.getLocation().getBlockY() + " "
				+ killed.getLocation().getBlockZ();

		// send a message to the player
		if (!plugin.getRewardManager().getKillMessage(killed).trim().isEmpty() && !killer_muted) {
			String message = ChatColor.GREEN + plugin.getRewardManager().getKillMessage(killed).trim()
					.replaceAll("\\{player\\}", getPlayer(killer, killed).getName())
					.replaceAll("\\{killer\\}", getPlayer(killer, killed).getName())
					.replaceAll("\\{killed\\}", mob.getFriendlyName())
					// .replaceAll("{prize}",
					// plugin.getRewardManager().format(cash))
					.replaceAll("\\{prize\\}", plugin.getRewardManager().format(cash))
					.replaceAll("\\{world\\}", worldname).replaceAll("\\{killerpos\\}", killerpos)
					.replaceAll("\\{killedpos\\}", killedpos)
					.replaceAll("\\{rewardname\\}", plugin.getConfigManager().dropMoneyOnGroundSkullRewardName);
			if (killed instanceof Player)
				message = message.replaceAll("\\{killed_player\\}", killed.getName()).replaceAll("\\{killed\\}",
						killed.getName());
			else
				message = message.replaceAll("\\{killed_player\\}", mob.getMobName()).replaceAll("\\{killed\\}",
						mob.getMobName());
			plugin.getMessages().debug("Description to be send:" + message);
			getPlayer(killer, killed).sendMessage(message);
		}
		
		// Pay the money reward to killer/player and assister
		if ((cash >= plugin.getConfigManager().minimumReward) || (cash <= -plugin.getConfigManager().minimumReward)) {

			// Handle reward on PVP kill. (Robbing)
			boolean robbing = killer != null && killed instanceof Player && !CitizensCompat.isNPC(killed)
					&& plugin.getConfigManager().pvpAllowed && plugin.getConfigManager().robFromVictim;
			if (robbing) {
				plugin.getMessages().debug("PVP kill reward is '%s'", plugin.getConfigManager().pvpKillMoney);
				plugin.getRewardManager().withdrawPlayer((Player) killed, cash);
				// plugin.getMessages().debug("RecordCash: %s killed a %s (%s)
				// Cash=%s",
				// killer.getName(), mob.getName(),
				// mob.getMobPlugin().name(), cash);
				// plugin.getDataStoreManager().recordCash(killer, mob,
				// killed.hasMetadata("MH:hasBonus"), -cash);
				if (!killed_muted)
					killed.sendMessage(ChatColor.RED + "" + ChatColor.ITALIC
							+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
									plugin.getRewardManager().format(cash), "money",
									plugin.getRewardManager().format(cash)));
				plugin.getMessages().debug("%s lost %s", killed.getName(), plugin.getRewardManager().format(cash));
			}

			// Reward/Penalty for assisted kill
			if (info.getAssister() == null || plugin.getConfigManager().enableAssists == false) {
				if (cash >= plugin.getConfigManager().minimumReward) {
					if (plugin.getConfigManager().dropMoneyOnGroup) {
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(killer, killed, killed.getLocation(),
								cash);
					} else {
						plugin.getRewardManager().depositPlayer(killer, cash);
						// plugin.getMessages().debug("RecordCash: %s killed a
						// %s (%s)
						// Cash=%s", killer.getName(), mob.getName(),
						// mob.getMobPlugin().name(), cash);
						// plugin.getDataStoreManager().recordCash(killer,
						// mob, killed.hasMetadata("MH:hasBonus"), cash);
						plugin.getMessages().debug("%s got a reward (%s)", killer.getName(),
								plugin.getRewardManager().format(cash));
					}
				} else if (cash <= -plugin.getConfigManager().minimumReward) {
					plugin.getRewardManager().withdrawPlayer(killer, -cash);
					// plugin.getMessages().debug("RecordCash: %s killed a %s
					// (%s) Cash=%s",
					// killer.getName(), mob.getName(),
					// mob.getMobPlugin().name(), cash);
					// plugin.getDataStoreManager().recordCash(killer, mob,
					// killed.hasMetadata("MH:hasBonus"), cash);
					plugin.getMessages().debug("%s got a penalty (%s)", killer.getName(),
							plugin.getRewardManager().format(cash));
				}
			} else {
				cash = Misc.round(cash / 2);
				if (cash >= plugin.getConfigManager().minimumReward) {
					if (plugin.getConfigManager().dropMoneyOnGroup) {
						if (MyPetCompat.isKilledByMyPet(killed))
							plugin.getMessages().debug("1)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									getPlayer(killer, killed).getName(), MyPetCompat.getMyPet(killed).getName(),
									plugin.getRewardManager().format(cash));
						else if (CitizensCompat.isNPC(killer))
							plugin.getMessages().debug("2)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									getPlayer(killer, killed).getName(), killer.getName(),
									plugin.getRewardManager().format(cash));
						else
							plugin.getMessages().debug("3)%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									getPlayer(killer, killed).getName(), getKillerName(killer, killed),
									plugin.getRewardManager().format(cash));
						plugin.getRewardManager().dropMoneyOnGround_RewardManager(getPlayer(killer, killed), killed,
								killed.getLocation(), cash);
					} else {
						plugin.getRewardManager().depositPlayer(info.getAssister(), cash);

						if (!MyPetCompat.isKilledByMyPet(killed) && !CitizensCompat.isNPC(killer))
							onAssist(getPlayer(killer, killed), killer, killed, info.getLastAssistTime());
						if (MyPetCompat.isKilledByMyPet(killed))
							plugin.getMessages().debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									getPlayer(killer, killed).getName(), MyPetCompat.getMyPet(killed).getName(),
									plugin.getRewardManager().format(cash));
						else
							plugin.getMessages().debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)",
									getPlayer(killer, killed).getName(), getKillerName(killer, killed),
									plugin.getRewardManager().format(cash));
					}
				} else if (cash <= -plugin.getConfigManager().minimumReward) {
					plugin.getRewardManager().withdrawPlayer(getPlayer(killer, killed), -cash);
					if (!MyPetCompat.isKilledByMyPet(killed) && !CitizensCompat.isNPC(killer))
						onAssist(info.getAssister(), killer, killed, info.getLastAssistTime());
					plugin.getMessages().debug("%s was assisted by %s. Reward/Penalty is only ½ (%s)",
							getPlayer(killer, killed).getName(), getKillerName(killer, killed),
							plugin.getRewardManager().format(cash));
				}
			}

			// Tell the player that he got the reward/penalty, unless muted
			if (!killer_muted)

				if (extraString.trim().isEmpty()) {
					if (cash >= plugin.getConfigManager().minimumReward) {
						if (!plugin.getConfigManager().dropMoneyOnGroup) {
							plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.GREEN
									+ "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain", "prize",
											plugin.getRewardManager().format(cash), "money",
											plugin.getRewardManager().format(cash), "killed", mob.getFriendlyName()));
						} else
							plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.GREEN
									+ "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.drop", "prize",
											plugin.getRewardManager().format(cash), "money",
											plugin.getRewardManager().format(cash), "killed", mob.getFriendlyName()));
					} else if (cash <= -plugin.getConfigManager().minimumReward) {
						plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.RED + ""
								+ ChatColor.ITALIC
								+ plugin.getMessages().getString("mobhunting.moneylost", "prize",
										plugin.getRewardManager().format(cash), "money",
										plugin.getRewardManager().format(cash), "killed", mob.getFriendlyName()));
					}

				} else {
					if (cash >= plugin.getConfigManager().minimumReward) {
						if (!plugin.getConfigManager().dropMoneyOnGroup) {
							plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.GREEN
									+ "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.bonuses", "basic_prize",
											plugin.getRewardManager().format(basic_prize), "prize",
											plugin.getRewardManager().format(cash), "money",
											plugin.getRewardManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getRewardManager().format(multipliers), "killed",
											mob.getFriendlyName()));
						} else
							plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed), ChatColor.GREEN
									+ "" + ChatColor.ITALIC
									+ plugin.getMessages().getString("mobhunting.moneygain.bonuses.drop", "basic_prize",
											plugin.getRewardManager().format(basic_prize), "prize",
											plugin.getRewardManager().format(cash), "money",
											plugin.getRewardManager().format(cash), "bonuses", extraString.trim(),
											"multipliers", plugin.getRewardManager().format(multipliers), "killed",
											mob.getFriendlyName()));
					} else if (cash <= -plugin.getConfigManager().minimumReward) {
						plugin.getMessages().playerActionBarMessageQueue(getPlayer(killer, killed),
								ChatColor.RED + "" + ChatColor.ITALIC
										+ plugin.getMessages().getString("mobhunting.moneylost.bonuses", "basic_prize",
												plugin.getRewardManager().format(basic_prize), "prize",
												plugin.getRewardManager().format(cash), "money",
												plugin.getRewardManager().format(cash), "bonuses", extraString.trim(),
												"multipliers", multipliers, "killed", mob.getFriendlyName()));
					}
				}
		} else
			plugin.getMessages().debug("The money reward was 0 or less than %s  (Bonuses=%s)",
					getPlayer(killer, killed).getName(), plugin.getConfigManager().minimumReward, extraString);

		// McMMO Level rewards
		if (killer != null && McMMOCompat.isSupported() && plugin.getConfigManager().enableMcMMOLevelRewards
				&& data.getDampenedKills() < 10 && !CrackShotCompat.isCrackShotUsed(killed)) {

			SkillType skilltype = null;
			if (Misc.isAxe(info.getWeapon()))
				skilltype = SkillType.AXES;
			else if (Misc.isSword(info.getWeapon()))
				skilltype = SkillType.SWORDS;
			else if (Misc.isBow(info.getWeapon()))
				skilltype = SkillType.ARCHERY;
			else if (Misc.isUnarmed(info.getWeapon()))
				skilltype = SkillType.UNARMED;

			if (skilltype != null) {
				double chance = plugin.mRand.nextDouble();
				plugin.getMessages().debug("If %s<%s %s will get a McMMO Level for %s", chance,
						plugin.getRewardManager().getMcMMOChance(killed), killer.getName(), skilltype.getName());

				if (chance < plugin.getRewardManager().getMcMMOChance(killed)) {
					int level = plugin.getRewardManager().getMcMMOLevel(killed);
					McMMOCompat.addLevel(killer, skilltype.getName(), level);
					plugin.getMessages().debug("%s was rewarded with %s McMMO Levels for %s", killer.getName(),
							plugin.getRewardManager().getMcMMOLevel(killed), skilltype.getName());
					killer.sendMessage(plugin.getMessages().getString("mobhunting.mcmmo.skilltype_level", "mcmmo_level",
							level, "skilltype", skilltype));
				}
			}
		}

		// Run console commands as a reward
		if (data.getDampenedKills() < 10) {
			Iterator<HashMap<String, String>> itr = plugin.getRewardManager().getKillCommands(killed).iterator();
			while (itr.hasNext()) {
				HashMap<String, String> cmd = itr.next();
				String perm = cmd.getOrDefault("permission", "");
				if (perm.isEmpty() || getPlayer(killer, killed).hasPermission(perm)) {
					double random = plugin.mRand.nextDouble();
					if (random < Double.valueOf(cmd.get("chance"))) {
						String commandCmd = cmd.getOrDefault("cmd", "");
						if (commandCmd != null) {
							commandCmd = commandCmd.replaceAll("\\{player\\}", getPlayer(killer, killed).getName())
									.replaceAll("\\{killer\\}", getPlayer(killer, killed).getName())
									.replaceAll("\\{killed\\}", mob.getFriendlyName())
									.replaceAll("\\{world\\}", worldname)
									.replaceAll("\\{prize\\}", plugin.getRewardManager().format(cash))
									.replaceAll("\\{killerpos\\}", killerpos).replaceAll("\\{killedpos\\}", killedpos)
									.replaceAll("\\{rewardname\\}",
											plugin.getConfigManager().dropMoneyOnGroundSkullRewardName);
							if (killed instanceof Player)
								commandCmd = commandCmd.replaceAll("\\{killed_player\\}", killed.getName())
										.replaceAll("\\{killed\\}", killed.getName());
							else
								commandCmd = commandCmd.replaceAll("\\{killed_player\\}", mob.getMobName())
										.replaceAll("\\{killed\\}", mob.getMobName());
							plugin.getMessages().debug("Command to be run:" + commandCmd);
							if (!commandCmd.isEmpty()) {
								String str = commandCmd;
								do {
									if (str.contains("|")) {
										int n = str.indexOf("|");
										try {
											Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
													str.substring(0, n));
										} catch (CommandException e) {
											Bukkit.getConsoleSender()
													.sendMessage(ChatColor.RED
															+ "[MobHunting][ERROR] Could not run cmd:\""
															+ str.substring(0, n) + "\" when Mob:" + mob.getMobName()
															+ " was killed by " + getPlayer(killer, killed).getName());
											Bukkit.getConsoleSender()
													.sendMessage(ChatColor.RED + "Command:" + str.substring(0, n));
										}
										str = str.substring(n + 1, str.length());
									}
								} while (str.contains("|"));
								try {
									Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), str);
								} catch (CommandException e) {
									Bukkit.getConsoleSender()
											.sendMessage(ChatColor.RED + "[MobHunting][ERROR] Could not run cmd:\""
													+ str + "\" when Mob:" + mob.getMobName() + " was killed by "
													+ getPlayer(killer, killed).getName());
									Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Command:" + str);
								}
							}
						}
						MessageType messageType = MessageType.valueOf((cmd == null || cmd.get("message_type") == null)
								? "Chat" : cmd.getOrDefault("message_type", "Chat"));
						String message = cmd.getOrDefault("message", "");
						if (message != null && !killer_muted) {
							plugin.getMessages().playerSendMessageAt(getPlayer(killer, killed),
									message.replaceAll("\\{player\\}", getPlayer(killer, killed).getName())
											.replaceAll("\\{killer\\}", getPlayer(killer, killed).getName())
											.replaceAll("\\{killed\\}", mob.getFriendlyName())
											.replaceAll("\\{world\\}", worldname)
											.replaceAll("\\{prize\\}", plugin.getRewardManager().format(cash))
											.replaceAll("\\{killerpos\\}", killerpos)
											.replaceAll("\\{killedpos\\}", killedpos).replaceAll("\\{rewardname\\}",
													plugin.getConfigManager().dropMoneyOnGroundSkullRewardName),
									messageType);
						}
					} else
						plugin.getMessages().debug(
								"The command did not run because random number (%s) was bigger than chance (%s)",
								random, cmd.get("chance"));
				} else {
					plugin.getMessages().debug("%s has not permission (%s) to run command: %s",
							getPlayer(killer, killed).getName(), cmd.get("permission"), cmd.get("cmd"));
				}
			}

			// Update PlaceHolderData
			if (PlaceholderAPICompat.isSupported()) {
				if (info.getAssister() == null) {
					PlaceHolderData p = PlaceholderAPICompat.getPlaceHolders()
							.get(getPlayer(killer, killed).getUniqueId());
					p.setTotal_kills(p.getTotal_kills() + 1);
					PlaceholderAPICompat.getPlaceHolders().put(getPlayer(killer, killed).getUniqueId(), p);
				} else {
					PlaceHolderData p = PlaceholderAPICompat.getPlaceHolders()
							.get(getPlayer(killer, killed).getUniqueId());
					p.setTotal_assists(p.getTotal_assists() + 1);
					PlaceholderAPICompat.getPlaceHolders().put(getPlayer(killer, killed).getUniqueId(), p);
				}
			}

		}

		// drop a head if allowed
		if (plugin.getRewardManager().getHeadDropHead(killed)) {
			double random = plugin.mRand.nextDouble();
			if (random < plugin.getRewardManager().getHeadDropChance(killed)) {
				MinecraftMob minecraftMob = MinecraftMob.getMinecraftMobType(killed);
				if (minecraftMob == MinecraftMob.PvpPlayer) {
					ItemStack head = new CustomItems(plugin).getPlayerHead(killed.getUniqueId(), 1,
							plugin.getRewardManager().getHeadValue(killed));
					getPlayer(killer, killed).getWorld().dropItem(killed.getLocation(), head);
				} else {
					ItemStack head = new CustomItems(plugin).getCustomHead(minecraftMob, mob.getFriendlyName(), 1,
							plugin.getRewardManager().getHeadValue(killed), minecraftMob.getPlayerUUID());
					getPlayer(killer, killed).getWorld().dropItem(killed.getLocation(), head);
				}
				plugin.getMessages().debug("%s killed a %s and a head was dropped", killer.getName(), killed.getName());
				if (!plugin.getRewardManager().getHeadDropMessage(killed).isEmpty())
					plugin.getMessages().playerSendMessage(killer,
							ChatColor.GREEN + plugin.getRewardManager().getHeadDropMessage(killed)
									.replaceAll("\\{player\\}", getPlayer(killer, killed).getName())
									.replaceAll("\\{killer\\}", getPlayer(killer, killed).getName())
									.replaceAll("\\{killed\\}", mob.getFriendlyName())
									.replaceAll("\\{prize\\}", plugin.getRewardManager().format(cash))
									.replaceAll("\\{world\\}", worldname).replaceAll("\\{killerpos\\}", killerpos)
									.replaceAll("\\{killedpos\\}", killedpos).replaceAll("\\{rewardname\\}",
											plugin.getConfigManager().dropMoneyOnGroundSkullRewardName));
			} else {
				plugin.getMessages().debug("Did not drop a head: random(%s)>chance(%s)", random,
						plugin.getRewardManager().getHeadDropChance(killed));
			}
		}

		plugin.getMessages().debug("======================= kill ended (37)=====================");
	}

	private boolean isSlimeOrMagmaCube(Entity entity) {
		return entity instanceof Slime || entity instanceof MagmaCube;
	}

	/**
	 * Get the Player or the MyPet owner (Player)
	 *
	 * @param killer
	 *            - the player who killed the mob
	 * @param killed
	 *            - the mob which died
	 * @return the Player or return null when killer is not a player and killed
	 *         not killed by a MyPet.
	 */
	private Player getPlayer(Player killer, Entity killed) {
		if (killer != null)
			return killer;

		Player owner = MyPetCompat.getMyPetOwner(killed);
		if (owner != null)
			return owner;

		DamageInformation damageInformation = mDamageHistory.get(killed);
		if (damageInformation != null && damageInformation.isCrackShotWeaponUsed())
			return damageInformation.getAttacker();

		plugin.getMessages().debug("Name was not found: Killer=%s, killed=%s", killer, killed);

		return null;

	}

	private String getKillerName(Player killer, Entity killed) {
		if (killer != null)
			return killer.getName();
		if (MyPetCompat.isKilledByMyPet(killed))
			return MyPetCompat.getMyPet(killed).getName();
		else
			return "";
	}

	private void cancelDrops(EntityDeathEvent event, boolean items, boolean xp) {
		if (items) {
			plugin.getMessages().debug("Removing naturally dropped items");
			event.getDrops().clear();
		}
		if (xp) {
			plugin.getMessages().debug("Removing naturally dropped XP");
			event.setDroppedExp(0);
		}
	}

	private void onAssist(Player player, Player killer, LivingEntity killed, long time) {
		if (!plugin.getConfigManager().enableAssists
				|| (System.currentTimeMillis() - time) > plugin.getConfigManager().assistTimeout * 1000)
			return;

		double multiplier = plugin.getConfigManager().assistMultiplier;
		double ks = 1.0;
		if (plugin.getConfigManager().assistAllowKillstreak) {
			HuntData data = new HuntData(player);
			ks = data.handleKillstreak(plugin, player);
		}

		multiplier *= ks;
		double cash = 0;
		if (killed instanceof Player)
			cash = plugin.getRewardManager().getBaseKillPrize(killed) * multiplier / 2;
		else
			cash = plugin.getRewardManager().getBaseKillPrize(killed) * multiplier;

		if ((cash >= plugin.getConfigManager().minimumReward) || (cash <= -plugin.getConfigManager().minimumReward)) {
			ExtendedMob mob = plugin.getExtendedMobManager().getExtendedMobFromEntity(killed);
			if (mob.getMob_id() == 0) {
				Bukkit.getLogger().warning("Unknown Mob:" + mob.getMobName() + " from plugin " + mob.getMobPlugin());
				Bukkit.getLogger().warning("Please report this to developer!");
				return;
			}
			// plugin.getDataStoreManager().recordAssist(player, killer,
			// mob, killed.hasMetadata("MH:hasBonus"), cash);
			if (cash >= 0)
				plugin.getRewardManager().depositPlayer(player, cash);
			else
				plugin.getRewardManager().withdrawPlayer(player, -cash);
			// plugin.getMessages().debug("RecordCash: %s killed a %s (%s)
			// Cash=%s",
			// killer.getName(), mob.getName(),
			// mob.getMobPlugin().name(), cash);
			// plugin.getDataStoreManager().recordCash(killer, mob,
			// killed.hasMetadata("MH:hasBonus"), cash);
			plugin.getMessages().debug("%s got a on assist reward (%s)", player.getName(),
					plugin.getRewardManager().format(cash));

			if (ks != 1.0)
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.GREEN + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("mobhunting.moneygain.assist", "prize",
										plugin.getRewardManager().format(cash), "money",
										plugin.getRewardManager().format(cash)));
			else {
				plugin.getMessages().playerActionBarMessageQueue(player,
						ChatColor.GREEN + "" + ChatColor.ITALIC
								+ plugin.getMessages().getString("mobhunting.moneygain.assist.bonuses", "prize",
										plugin.getRewardManager().format(cash), "money",
										plugin.getRewardManager().format(cash), "bonuses", String.format("x%.1f", ks)));
			}
		} else
			plugin.getMessages().debug("KillBlocked %s: Reward was less than %s.", killer.getName(),
					plugin.getConfigManager().minimumReward);
		;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void bonusMobSpawn(CreatureSpawnEvent event) {
		// Bonus Mob can't be Citizens and MyPet
		if (CitizensCompat.isNPC(event.getEntity()) || MyPetCompat.isMyPet(event.getEntity()))
			return;

		if (event.getEntityType() == EntityType.ENDER_DRAGON)
			return;

		if (event.getEntityType() == EntityType.CREEPER)
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(event.getEntity()) == 0
						&& plugin.getRewardManager().getKillCommands(event.getEntity()).isEmpty())
				|| event.getSpawnReason() != SpawnReason.NATURAL)
			return;

		if (plugin.mRand.nextDouble() * 100 < plugin.getConfigManager().bonusMobChance) {
			plugin.getParticleManager().attachEffect(event.getEntity(), Effect.MOBSPAWNER_FLAMES);
			if (plugin.mRand.nextBoolean())
				event.getEntity()
						.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 3));
			else
				event.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
			event.getEntity().setMetadata("MH:hasBonus", new FixedMetadataValue(plugin, true));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void spawnerMobSpawn(CreatureSpawnEvent event) {
		// Citizens and MyPet can't be spawned from Spawners and eggs
		if (CitizensCompat.isNPC(event.getEntity()) || MyPetCompat.isMyPet(event.getEntity()))
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(event.getEntity()) == 0)
						&& plugin.getRewardManager().getKillCommands(event.getEntity()).isEmpty())
			return;

		if (event.getSpawnReason() == SpawnReason.SPAWNER || event.getSpawnReason() == SpawnReason.SPAWNER_EGG
				|| event.getSpawnReason() == SpawnReason.DISPENSE_EGG) {
			if (plugin.getConfigManager().disableMoneyRewardsFromMobSpawnersEggsAndDispensers)
				if (plugin.getConfigManager().grindingDetectionEnabled
						&& !plugin.getGrindingManager().isWhitelisted(event.getEntity().getLocation()))
					event.getEntity().setMetadata(SPAWNER_BLOCKED, new FixedMetadataValue(plugin, true));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void reinforcementMobSpawn(CreatureSpawnEvent event) {

		if (event.getSpawnReason() != SpawnReason.REINFORCEMENTS)
			return;

		LivingEntity mob = event.getEntity();

		if (CitizensCompat.isNPC(mob) && !CitizensCompat.isSentryOrSentinelOrSentries(mob))
			return;

		if (!isHuntEnabledInWorld(event.getLocation().getWorld())
				|| (plugin.getRewardManager().getBaseKillPrize(mob) <= 0)
						&& plugin.getRewardManager().getKillCommands(mob).isEmpty())
			return;

		event.getEntity().setMetadata("MH:reinforcement", new FixedMetadataValue(plugin, true));

	}

	public Set<IModifier> getHuntingModifiers() {
		return mHuntingModifiers;
	}

	public WeakHashMap<LivingEntity, DamageInformation> getDamageHistory() {
		return mDamageHistory;
	}
}