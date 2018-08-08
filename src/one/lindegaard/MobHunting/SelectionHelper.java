package one.lindegaard.MobHunting;

import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.compatibility.WorldEditCompat;

public class SelectionHelper {
	private static WeakHashMap<Player, Entry<Location, Location>> mPoints = new WeakHashMap<Player, Entry<Location, Location>>();

	public static Location getPointA(Player player) throws IllegalArgumentException {
		if (needsCommands()) {
			Entry<Location, Location> existing = mPoints.get(player);
			if (existing == null || existing.getKey() == null)
				throw new IllegalArgumentException(
						MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.point1-unset"));

			if (!existing.getKey().getWorld().equals(player.getWorld()))
				throw new IllegalArgumentException(
						MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.point1-unset"));

			return existing.getKey();
		} else
			return new Location(player.getWorld(), WorldEditCompat.getPointA(player).getX(),
					WorldEditCompat.getPointA(player).getY(), WorldEditCompat.getPointA(player).getZ());
	}

	public static Location getPointB(Player player) throws IllegalArgumentException {
		if (needsCommands()) {
			Entry<Location, Location> existing = mPoints.get(player);
			if (existing == null || existing.getValue() == null)
				throw new IllegalArgumentException(
						MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.point2-unset"));

			if (!existing.getValue().getWorld().equals(player.getWorld()))
				throw new IllegalArgumentException(
						MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.point2-unset"));

			return existing.getValue();
		} else
			return new Location(player.getWorld(), WorldEditCompat.getPointB(player).getX(),
					WorldEditCompat.getPointB(player).getY(), WorldEditCompat.getPointB(player).getZ());
	}

	public static boolean needsCommands() {
		return !Bukkit.getPluginManager().isPluginEnabled("WorldEdit"); //$NON-NLS-1$
	}

	public static void setPointA(Player player, Location location) {
		Entry<Location, Location> existing = mPoints.get(player);
		if (existing == null)
			mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(location, null));
		else {
			if (existing.getValue() != null && !existing.getValue().getWorld().equals(location.getWorld()))
				mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(location, null));
			else
				mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(location, existing.getValue()));
		}
	}

	public static void setPointB(Player player, Location location) {
		Entry<Location, Location> existing = mPoints.get(player);
		if (existing == null)
			mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(null, location));
		else {
			if (existing.getKey() != null && !existing.getKey().getWorld().equals(location.getWorld()))
				mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(null, location));
			else
				mPoints.put(player, new AbstractMap.SimpleEntry<Location, Location>(existing.getKey(), location));
		}
	}
}
