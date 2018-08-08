package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.regions.Region;

import one.lindegaard.MobHunting.MobHunting;

public class WorldEditCompat {
	private static WorldEditPlugin mPlugin;
	private static boolean supported = false;

	public WorldEditCompat() {
		mPlugin = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin(CompatPlugin.WorldEdit.getName());
		if (mPlugin.getDescription().getVersion().compareTo("7.0.0") >= 0) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Enabling compatibility with WorldEdit (" + mPlugin.getDescription().getVersion() + ")");
			supported = true;
		} else {
			Bukkit.getConsoleSender()
					.sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
							+ "Your current version of WorldEdit (" + mPlugin.getDescription().getVersion()
							+ ") is not supported by MobHunting. Mobhunting does only support 7.0.0 and newer.");
		}
	}

	public static WorldEditPlugin getWorldEdit() {
		return mPlugin;
	}

	public static Vector getPointA(Player player) throws IllegalArgumentException {
		if (supported)
			throw new IllegalArgumentException("WorldEdit is not present");

		com.sk89q.worldedit.world.World wor = mPlugin.getSession(player).getSelectionWorld();
		Region sel = null;
		try {
			sel = mPlugin.getSession(player).getSelection(wor);
		} catch (IncompleteRegionException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		if (sel == null)
			throw new IllegalArgumentException(
					MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.no-select"));

		if (!(sel instanceof CuboidSelection))
			throw new IllegalArgumentException(
					MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.select-type"));

		return sel.getMinimumPoint();
	}

	public static Vector getPointB(Player player) throws IllegalArgumentException {
		if (supported)
			throw new IllegalArgumentException("WorldEdit is not present");

		com.sk89q.worldedit.world.World wor = mPlugin.getSession(player).getSelectionWorld();
		Region sel = null;
		try {
			sel = mPlugin.getSession(player).getSelection(wor);
		} catch (IncompleteRegionException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		if (sel == null)
			throw new IllegalArgumentException(
					MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.no-select"));

		if (!(sel instanceof CuboidSelection))
			throw new IllegalArgumentException(
					MobHunting.getInstance().getMessages().getString("mobhunting.commands.select.select-type"));

		return sel.getMaximumPoint();
	}

	public static boolean isSupported() {
		return supported;
	}
}
