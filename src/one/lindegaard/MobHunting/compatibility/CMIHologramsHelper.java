package one.lindegaard.MobHunting.compatibility;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.CMIHologram;

import one.lindegaard.MobHunting.leaderboard.HologramLeaderboard;

public class CMIHologramsHelper {

	public static void createHologram(HologramLeaderboard board) {
		CMI.getInstance().getHologramManager()
				.addHologram(new CMIHologram(board.getHologramName(), board.getLocation()));
	}

	public static void addTextLine(CMIHologram hologram, String text) {
		List<String> lines = hologram.getLinesAsList();
		lines.add(lines.size()-1, text);
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
		//hologram.addLine(new ItemLine(hologram, itemstack));
	}

	public static void deleteHologram(CMIHologram hologram) {
		CMI.getInstance().getHologramManager().removeHolo(hologram);
	}

	public static void hideHologram(CMIHologram hologram) {
		CMI.getInstance().getHologramManager().hideHoloForAllPlayers(hologram);
	}

}
