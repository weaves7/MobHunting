package one.lindegaard.MobHunting.compatibility;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;

import one.lindegaard.MobHunting.leaderboard.HologramLeaderboard;

public class CMIHologramsHelper {

	public static void createHologram(HologramLeaderboard board) {
		//MobHunting.getAPI().getMessages().debug("Hologram=%s @ %s", board.getHologramName(),board.getLocation().toString());
		CMIHologram hologram = new CMIHologram(board.getHologramName(), board.getLocation());
		//MobHunting.getAPI().getMessages().debug("new Hologram=%s",hologram.getName());
		CMIHologramsCompat.
		getHologramManager()
				.addHologram(
						hologram);
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
		CMIHologramsCompat.
		getHologramManager()
		.removeHolo(hologram);
	}

	public static void hideHologram(CMIHologram hologram) {
		CMIHologramsCompat.
		getHologramManager()
		.hideHoloForAllPlayers(hologram);
	}

}
