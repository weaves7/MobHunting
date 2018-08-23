package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.comphenix.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.EnumWrappers.Particle;

import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.grinding.Area;

public class ProtocolLibShowGrindingAreaThread implements Runnable {
	
	public Player player;
	public Area grindingArea;
	public Location killedLocation;
	
	public ProtocolLibShowGrindingAreaThread( Player p, Area a, Location loc) {
		player=p;
		grindingArea=a;
		killedLocation=loc;
	}

	@Override
	public void run() {
		WrapperPlayServerWorldParticles wpwp = new WrapperPlayServerWorldParticles();
		
		if (killedLocation != null) {
			wpwp.setParticleType(Particle.CLOUD);
			wpwp.setNumberOfParticles(1);
			wpwp.setOffsetX(0);
			wpwp.setOffsetY(0);
			wpwp.setOffsetZ(0);
			wpwp.setLongDistance(false);
			wpwp.setParticleData(0);
			wpwp.setX((float) (killedLocation.getBlockX() + 0.5));
			wpwp.setZ((float) (killedLocation.getBlockZ() + 0.5));
			for (int n = 0; n < 10; n++) {
				wpwp.setY((float) (killedLocation.getBlockY() + 0.2 + 0.2 * n));
				MobHunting.getAPI().getMessages().debug("ProtocolLib: SendPacket to %s",player.getName());
				if (player!=null & player.isOnline())
				wpwp.sendPacket(player);
			}
		}
	}
	
/**
		// Grinding Area
		if (grindingArea != null) {
			// Show center of grinding area
			wpwp.setParticleType(Particle.FLAME);
			wpwp.setNumberOfParticles(3);
			wpwp.setOffsetX(0);
			wpwp.setOffsetY(0);
			wpwp.setOffsetZ(0);
			wpwp.setX((float) (grindingArea.getCenter().getBlockX() + 0.5));
			wpwp.setZ((float) (grindingArea.getCenter().getBlockZ() + 0.5));
			for (int n = 0; n < 10; n++) {
				wpwp.setY((float) (grindingArea.getCenter().getBlockY() + 0.2 + 0.1 * n));
				wpwp.sendPacket(player);
			}

			// Circle around the grinding area
			wpwp.setParticleType(Particle.FLAME);
			wpwp.setY((float) (grindingArea.getCenter().getBlockY() + 0.2));
			wpwp.setOffsetY(0);
			for (int n = 0; n < 360; n = n
					+ (int) (45 / MobHunting.getInstance().getConfigManager().grindingDetectionRange)) {
				wpwp.setX((float) (grindingArea.getCenter().getBlockX() + 0.5
						+ Math.cos(n) * MobHunting.getInstance().getConfigManager().grindingDetectionRange));
				wpwp.setZ((float) (grindingArea.getCenter().getBlockZ() + 0.5
						+ Math.sin(n) * MobHunting.getInstance().getConfigManager().grindingDetectionRange));
				wpwp.sendPacket(player);
			}
		}
	**/
	
}

