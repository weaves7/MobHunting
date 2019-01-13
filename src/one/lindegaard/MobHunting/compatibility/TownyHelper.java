package one.lindegaard.MobHunting.compatibility;

import java.util.List;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import one.lindegaard.MobHunting.MobHunting;

public class TownyHelper {

	public static boolean isInHomeTown(Player player) {
		if (TownyCompat.isSupported()) {
			Resident resident;
			Town homeTown = null;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (NotRegisteredException ex) {
				// MobHunting.getInstance().getMessages().debug("Could not find the Resident (%s)",
				// player.getName());
				return false;
			}

			if (resident != null) {
				try {
					homeTown = resident.getTown();
				} catch (NotRegisteredException e) {
					// MobHunting.getInstance().getMessages().debug("%s has no town", player.getName());
					return false;
				}
			}

			TownBlock tb = TownyUniverse.getTownBlock(player.getLocation());
			if (tb != null) {
				// Location is within a town
				try {
					MobHunting.getInstance().getMessages().debug("%s is in a town (%s)", player.getName(), tb.getTown().getName());
				} catch (NotRegisteredException e) {
				}
			} else {
				// MobHunting.getInstance().getMessages().debug("The player is not in a town");
				return false;
			}

			try {
				// Check if the town is the residents town.
				List<Resident> residents = tb.getTown().getResidents();
				if (residents.contains(resident) || tb.getTown().equals(homeTown)) {
					// check if town is protected against mob damage
					TownyPermission p1 = homeTown.getPermissions();
					Boolean protected_mob = p1.mobs;
					MobHunting.getInstance().getMessages().debug("%s is in his HomeTown. Mob spawns:%s", player.getName(),
							protected_mob ? "On" : "Off");
					return true;
				} else {
					// MobHunting.getInstance().getMessages().debug("%s is not in his home town",
					// player.getName());
					return false;
				}
			} catch (NotRegisteredException e) {
				return false;
			}
		}
		return false;
	}

	public static boolean isInAnyTomn(Player player) {
		if (TownyCompat.isSupported()) {
			TownBlock tb = TownyUniverse.getTownBlock(player.getLocation());
			if (tb != null) {
				// Location is within a town
				return true;
			} else {
				// MobHunting.getInstance().getMessages().debug("The player is not in a town");
				return false;
			}
		}
		return false;
	}

}
