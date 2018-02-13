package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;

import one.lindegaard.MobHunting.MobHunting;

public class FactionsCompat {

	private static Plugin mPlugin;

	// https://www.massivecraft.com/factions-develop

	public FactionsCompat() {
		mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Factions.getName());
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.getInstance().getConfigManager().disableIntegrationFactions;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getInstance().getConfigManager().disableIntegrationFactions;
	}

	// PERMANENT: A permanent faction will never be deleted. (no)
	// PEACEFUL: Allways in truce with other factions (no)
	// INFPOWER: This flag gives the faction infinite power. (no)
	// POWERLOSS: Is power lost on death in this territory? (yes)
	// PVP: Can you PVP in territory? (yes)
	// FRIENDLYFIRE: Can friends hurt eachother here? (no)
	// MONSTERS: Can monsters spawn in this territory? (yes)
	// EXPLOSIONS: Can explosions occur in this territory? (yes)
	// FIRESPREAD: Can fire spread in territory? (yes)
	// ENDERGRIEF: Can endermen grief in this territory? (no

	public static boolean isPVPAllowed(Player player) {
		MPlayer mplayer = MPlayer.get(player);
		Faction faction = mplayer.getFaction();
		return faction.getFlag(MFlag.ID_FRIENDLYFIRE);
	}

	public static boolean isMonstersAllowedToSpawn(Player player) {
		MPlayer mplayer = MPlayer.get(player);
		Faction faction = mplayer.getFaction();
		return faction.getFlag(MFlag.ID_MONSTERS);
	}

	public static boolean isInSafeZone(Player player) {
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(player.getLocation()));
		return FactionColl.get().getSafezone().equals(faction);
	}

	public static boolean isInWilderness(Player player) {
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(player.getLocation()));
		return FactionColl.get().getNone().equals(faction);
	}

	public static boolean isInWarZone(Player player) {
		Faction faction = BoardColl.get().getFactionAt(PS.valueOf(player.getLocation()));
		return FactionColl.get().getWarzone().equals(faction);
	}
}
