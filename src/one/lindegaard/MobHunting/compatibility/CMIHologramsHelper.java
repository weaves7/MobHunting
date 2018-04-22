package one.lindegaard.MobHunting.compatibility;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;

import one.lindegaard.MobHunting.leaderboard.HologramLeaderboard;

public class CMIHologramsHelper {

	public static void createHologram(HologramLeaderboard board) {
		Location loc = board.getLocation().subtract(0, 1, 0);
		CMIHologram hologram = new CMIHologram(board.getHologramName(), loc );
		CMICompat.getHologramManager().addHologram(hologram);
	}

	public static void addTextLine(CMIHologram hologram, String text) {
		List<String> lines = hologram.getLinesAsList();
		lines.add(lines.size(), text);
		hologram.setLines(lines);
	}

	public static void removeLine(CMIHologram hologram, int i) {
		List<String> lines = hologram.getLinesAsList();
		if (lines.size() > i)
			lines.remove(i);
		hologram.setLines(lines);
	}

	public static void editTextLine(CMIHologram hologram, String text, int i) {
		List<String> lines = hologram.getLinesAsList();
		if (lines.size() > i)
			lines.remove(i);
		lines.add(i, text);
		hologram.setLines(lines);
	}

	public static void addItemLine(CMIHologram hologram, ItemStack itemstack) {
		// hologram.addLine(new ItemLine(hologram, itemstack));
	}

	public static void deleteHologram(CMIHologram hologram) {
		CMICompat.getHologramManager().removeHolo(hologram);
	}

	public static void hideHologram(CMIHologram hologram) {
		CMICompat.getHologramManager().hideHoloForAllPlayers(hologram);
	}

}
