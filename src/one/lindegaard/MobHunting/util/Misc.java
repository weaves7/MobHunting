package one.lindegaard.MobHunting.util;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;

import one.lindegaard.MobHunting.MobHunting;

public class Misc {
	public static boolean isAxe(ItemStack item) {
		return item != null && (item.getType() == Material.DIAMOND_AXE || item.getType() == Material.GOLDEN_AXE
				|| item.getType() == Material.IRON_AXE || item.getType() == Material.STONE_AXE
				|| item.getType() == Material.WOODEN_AXE);
	}

	public static boolean isSword(ItemStack item) {
		return item != null && (item.getType() == Material.DIAMOND_SWORD || item.getType() == Material.GOLDEN_SWORD
				|| item.getType() == Material.IRON_SWORD || item.getType() == Material.STONE_SWORD
				|| item.getType() == Material.WOODEN_SWORD);
	}

	public static boolean isPick(ItemStack item) {
		return item != null && (item.getType() == Material.DIAMOND_PICKAXE || item.getType() == Material.GOLDEN_PICKAXE
				|| item.getType() == Material.IRON_PICKAXE || item.getType() == Material.STONE_PICKAXE
				|| item.getType() == Material.WOODEN_PICKAXE);
	}

	public static boolean isBow(ItemStack item) {
		return item != null && (item.getType() == Material.BOW);
	}

	public static boolean isTrident(ItemStack item) {
		return item != null && (item.getType() == Material.TRIDENT);
	}

	public static boolean isUnarmed(ItemStack item) {
		return (item == null || item.getType() == Material.AIR);
	}

	public static boolean isSign(Block block) {
		if (isMC113OrNewer())
			return block.getType() == Material.SIGN || block.getType() == Material.WALL_SIGN;
		else
			return block.getType() == Material.LEGACY_SIGN || block.getType() == Material.LEGACY_SIGN_POST;
	}

	public static boolean isSign(Material material) {
		if (isMC113OrNewer())
			return material == Material.SIGN || material == Material.WALL_SIGN;
		else
			return material == Material.LEGACY_SIGN || material == Material.LEGACY_SIGN_POST;
	}

	public static boolean isSkull(Material material) {
		if (isMC113OrNewer())
			return material == Material.PLAYER_HEAD || material == Material.PLAYER_WALL_HEAD
					|| material == Material.SKELETON_SKULL || material == Material.SKELETON_WALL_SKULL
					|| material == Material.WITHER_SKELETON_SKULL || material == Material.WITHER_SKELETON_WALL_SKULL
					|| material == Material.CREEPER_HEAD || material == Material.CREEPER_WALL_HEAD
					|| material == Material.DRAGON_HEAD || material == Material.DRAGON_WALL_HEAD;
		else
			return material == Material.LEGACY_SKULL || material == Material.LEGACY_SKULL_ITEM;
	}

	public static Map<String, Object> toMap(Location loc) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("X", loc.getX());
		map.put("Y", loc.getY());
		map.put("Z", loc.getZ());

		map.put("Yaw", (double) loc.getYaw());
		map.put("Pitch", (double) loc.getPitch());

		if (loc.getWorld() != null)
			map.put("W", loc.getWorld().getUID().toString());

		return map;
	}

	public static Location fromMap(Map<String, Object> map) {
		double x, y, z;
		float yaw, pitch;
		UUID world;

		x = (Double) map.get("X");
		y = (Double) map.get("Y");
		z = (Double) map.get("Z");

		yaw = (float) (double) (Double) map.get("Yaw");
		pitch = (float) (double) (Double) map.get("Pitch");

		if (map.containsKey("W")) {
			world = UUID.fromString((String) map.get("W"));
			return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
		} else
			return new Location(null, x, y, z, yaw, pitch);
	}

	// *******************************************************************
	// Version detection
	// *******************************************************************
	public static boolean isMC113() {
		return Bukkit.getBukkitVersion().contains("1.13");
	}

	public static boolean isMC112() {
		return Bukkit.getBukkitVersion().contains("1.12");
	}

	public static boolean isMC111() {
		return Bukkit.getBukkitVersion().contains("1.11");
	}

	public static boolean isMC110() {
		return Bukkit.getBukkitVersion().contains("1.10");
	}

	public static boolean isMC19() {
		return Bukkit.getBukkitVersion().contains("1.9");
	}

	public static boolean isMC18() {
		return Bukkit.getBukkitVersion().contains("1.8");
	}

	public static boolean isMC113OrNewer() {
		if (isMC113())
			return true;
		else if (isMC112() || isMC111() || isMC110() || isMC19() || isMC18())
			return false;
		return true;
	}

	public static boolean isMC112OrNewer() {
		if (isMC112())
			return true;
		else if (isMC111() || isMC110() || isMC19() || isMC18())
			return false;
		return true;
	}

	public static boolean isMC111OrNewer() {
		if (isMC111())
			return true;
		else if (isMC110() || isMC19() || isMC18())
			return false;
		return true;
	}

	public static boolean isMC110OrNewer() {
		if (isMC110())
			return true;
		else if (isMC19() || isMC18())
			return false;
		return true;
	}

	public static boolean isMC19OrNewer() {
		if (isMC19())
			return true;
		else if (isMC18())
			return false;
		return true;
	}

	// *******************************************************************
	// Version detection
	// *******************************************************************
	public static boolean isGlowstoneServer() {
		return Bukkit.getServer().getName().equalsIgnoreCase("Glowstone");
	}

	public static boolean isPaperServer() {
        return Bukkit.getServer().getName().equalsIgnoreCase("Paper")
                && Bukkit.getServer().getVersion().toLowerCase().contains("paper");
    }

	public static boolean isSpigotServer() {
		return Bukkit.getServer().getName().equalsIgnoreCase("CraftBukkit")
				&& Bukkit.getServer().getVersion().toLowerCase().contains("spigot");
	}

	public static boolean isCraftBukkitServer() {
		return Bukkit.getServer().getName().equalsIgnoreCase("CraftBukkit")
				&& Bukkit.getServer().getVersion().toLowerCase().contains("bukkit");
	}

	public static Player getOnlinePlayer(OfflinePlayer offlinePlayer) {
		for (Player player : getOnlinePlayers()) {
			if (player.getName().equals(offlinePlayer.getName()))
				return player;
		}
		return null;
	}

	public static String trimSignText(String string) {
		return string.length() > 15 ? string.substring(0, 14).trim() : string;
	}

	public static double round(double d) {
		return Math.round(d / MobHunting.getInstance().getConfigManager().rewardRounding)
				* MobHunting.getInstance().getConfigManager().rewardRounding;
	}

	public static double ceil(double d) {
		return Math.ceil(d / MobHunting.getInstance().getConfigManager().rewardRounding)
				* MobHunting.getInstance().getConfigManager().rewardRounding;
	}

	public static double floor(double d) {
		return Math.floor(d / MobHunting.getInstance().getConfigManager().rewardRounding)
				* MobHunting.getInstance().getConfigManager().rewardRounding;
	}

	public static final Block getTargetBlock(Player player, int range) {
		BlockIterator iter = new BlockIterator(player, range);
		Block lastBlock = iter.next();
		while (iter.hasNext()) {
			lastBlock = iter.next();
			if (lastBlock.getType() == Material.AIR) {
				continue;
			}
			break;
		}
		return lastBlock;
	}

	/**
	 * Gets the online player (backwards compatibility)
	 *
	 * @return number of players online
	 */
	public static int getOnlinePlayersAmount() {
		try {
			Method method = Server.class.getMethod("getOnlinePlayers");
			if (method.getReturnType().equals(Collection.class)) {
				return ((Collection<?>) method.invoke(Bukkit.getServer())).size();
			} else {
				return ((Player[]) method.invoke(Bukkit.getServer())).length;
			}
		} catch (Exception ex) {
			MobHunting.getInstance().getMessages().debug(ex.getMessage());
		}
		return 0;
	}

	/**
	 * Gets the online player (for backwards compatibility)
	 *
	 * @return all online players as a Java Collection, if return type of
	 *         Bukkit.getOnlinePlayers() is Player[] it will be converted to a
	 *         Collection.
	 */
	@SuppressWarnings({ "unchecked" })
	public static Collection<Player> getOnlinePlayers() {
		Method method;
		try {
			method = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
			Object players = method.invoke(null);
			Collection<Player> newPlayers;
			if (players instanceof Player[])
				newPlayers = Arrays.asList((Player[]) players);
			else
				newPlayers = (Collection<Player>) players;
			return newPlayers;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return Collections.emptyList();
	}

	public static String format(double money) {
		Locale locale = new Locale("en", "UK");
		String pattern = "0.#####";
		DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		decimalFormat.applyPattern(pattern);
		return decimalFormat.format(money);
	}

	public static boolean isUUID(String string) {
		try {
			UUID.fromString(string);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

}
