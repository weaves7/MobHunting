package one.lindegaard.MobHunting.compatibility;

import org.bukkit.entity.Player;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.regions.Region;

import one.lindegaard.MobHunting.MobHunting;

public class WorldEditHelper {

	public WorldEditHelper() {
		// TODO Auto-generated constructor stub
	}

	public static Vector getPointA(Player player) throws IllegalArgumentException {
		if (WorldEditCompat.isSupported())
			throw new IllegalArgumentException("WorldEdit is not present");

		com.sk89q.worldedit.world.World wor = WorldEditCompat.getWorldEdit().getSession(player).getSelectionWorld();
		Region sel = null;
		try {
			sel = WorldEditCompat.getWorldEdit().getSession(player).getSelection(wor);
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
		if (WorldEditCompat.isSupported())
			throw new IllegalArgumentException("WorldEdit is not present");

		com.sk89q.worldedit.world.World wor = WorldEditCompat.getWorldEdit().getSession(player).getSelectionWorld();
		Region sel = null;
		try {
			sel = WorldEditCompat.getWorldEdit().getSession(player).getSelection(wor);
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
}
