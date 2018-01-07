package one.lindegaard.MobHunting.update;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.update.spiget.SpigetUpdate;
import org.inventivetalent.update.spiget.UpdateCallback;
import org.inventivetalent.update.spiget.comparator.VersionComparator;

import one.lindegaard.MobHunting.Messages;
import one.lindegaard.MobHunting.MobHunting;

public class SpigetUpdater {

	private MobHunting plugin;

	public SpigetUpdater(MobHunting plugin) {
		this.plugin = plugin;
	}

	private SpigetUpdate spigetUpdate = null;
	private UpdateStatus updateAvailable = UpdateStatus.UNKNOWN;
	private String currentJarFile = "";
	private String newDownloadVersion = "";

	public SpigetUpdate getSpigetUpdate() {
		return spigetUpdate;
	}

	public UpdateStatus getUpdateAvailable() {
		return updateAvailable;
	}

	public void setUpdateAvailable(UpdateStatus b) {
		updateAvailable = b;
	}

	public String getCurrentJarFile() {
		return currentJarFile;
	}

	public void setCurrentJarFile(String name) {
		currentJarFile = name;
	}

	public String getNewDownloadVersion() {
		return newDownloadVersion;
	}

	public void setNewDownloadVersion(String newDownloadVersion) {
		this.newDownloadVersion = newDownloadVersion;
	}

	public void hourlyUpdateCheck(final CommandSender sender, boolean updateCheck, final boolean silent) {
		long seconds = MobHunting.getInstance().getConfigManager().checkEvery;
		if (seconds < 900) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.RED
					+ "[MobHunting][Warning] check_every in your config.yml is too low. A low number can cause server crashes. The number is raised to 900 seconds = 15 minutes.");
			seconds = 900;
		}
		if (updateCheck) {
			new BukkitRunnable() {
				@Override
				public void run() {
					checkForUpdate(sender, true, false);
				}
			}.runTaskTimer(MobHunting.getInstance(), 0L, seconds * 20L);
		}
	}

	public boolean downloadAndUpdateJar() {
		new BukkitRunnable() {
			int count = 0;
			boolean succes = spigetUpdate.downloadUpdate();
			final String OS = System.getProperty("os.name");

			@Override
			public void run() {
				if (count++ > 10) {
					Bukkit.getConsoleSender().sendMessage(
							ChatColor.RED + "[MobHunting] No updates found. (No response from server after 10s)");
					this.cancel();
				} else {
					// Wait for the response
					if (succes) {
						if (OS.indexOf("Win") >= 0) {
							File downloadedJar = new File("plugins/update/" + currentJarFile);
							File newJar = new File("plugins/update/MobHunting-" + newDownloadVersion + ".jar");
							if (newJar.exists())
								newJar.delete();
							downloadedJar.renameTo(newJar);
						} else {
							if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
								File currentJar = new File("plugins/" + currentJarFile);
								File disabledJar = new File("plugins/" + currentJarFile + ".old");
								int count = 0;
								while (disabledJar.exists() && count++ < 100) {
									disabledJar = new File("plugins/" + currentJarFile + ".old" + count);
								}
								if (!disabledJar.exists()) {
									currentJar.renameTo(disabledJar);
									File downloadedJar = new File("plugins/update/" + currentJarFile);
									File newJar = new File("plugins/MobHunting-" + newDownloadVersion + ".jar");
									downloadedJar.renameTo(newJar);
									Messages.debug("Moved plugins/update/" + currentJarFile + " to plugins/MobHunting-"
											+ newDownloadVersion + ".jar");
									updateAvailable = UpdateStatus.RESTART_NEEDED;
								}
							}
						}
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 20L, 20L);
		return true;
	}

	public void checkForUpdate(final CommandSender sender, boolean updateCheck, final boolean silent) {

		if (updateCheck) {
			if (!silent)
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + Messages.getString("mobhunting.commands.update.check"));
			if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
				spigetUpdate = new SpigetUpdate(plugin, 3582);
				spigetUpdate.setVersionComparator(VersionComparator.SEM_VER);
				spigetUpdate.setUserAgent("MobHunting-" + plugin.getDescription().getVersion());

				spigetUpdate.checkForUpdate(new UpdateCallback() {

					@Override
					public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
						//// A new version is available
						updateAvailable = UpdateStatus.AVAILABLE;
						newDownloadVersion = newVersion;
						sender.sendMessage(ChatColor.GREEN + "[MobHunting] " + Messages
								.getString("mobhunting.commands.update.version-found", "newversion", newVersion));
						if (plugin.getConfigManager().autoupdate) {
							downloadAndUpdateJar();
							sender.sendMessage(ChatColor.GREEN + "[MobHunting] "
									+ Messages.getString("mobhunting.commands.update.complete"));
						} else
							sender.sendMessage(ChatColor.GREEN + "[MobHunting] "
									+ Messages.getString("mobhunting.commands.update.help"));
					}

					@Override
					public void upToDate() {
						//// Plugin is up-to-date
						if (!silent)
							sender.sendMessage(ChatColor.GOLD + "[MobHunting] "
									+ Messages.getString("mobhunting.commands.update.no-update"));
					}
				});
			}
		}
	}

	/**
	public static UpdateStatus isSnapShotNewerVersion() {
		// Version format on Bukkit.org: "MobHunting Vn.n.n"
		// Version format in jar file: "n.n.n" | "n.n.n-SNAPSHOT-Bn"

		int updateCheck = 0, pluginCheck = 0;
		boolean snapshot = false;
		// Check to see if the latest file is newer that this one
		//String[] split = Updater.getBukkitUpdate().getVersionName().split(" V");
		// Only do this if the format is what we expect
		String[] split = new String[2];
		if (split.length == 2) {
			// Need to escape the period in the regex expression
			String[] updateVer = split[1].split("\\.");
			// Check the version #'smy
			String[] pluginVerSNAPSHOT = MobHunting.getInstance().getDescription().getVersion().split("\\-");
			if (pluginVerSNAPSHOT.length > 1)
				snapshot = pluginVerSNAPSHOT[1].equals("SNAPSHOT");
			if (snapshot)
				Messages.debug("You are using a development version (%s)",
						MobHunting.getInstance().getDescription().getVersion());
			String[] pluginVer = pluginVerSNAPSHOT[0].split("\\.");
			// Run through major, minor, sub
			for (int i = 0; i < Math.max(updateVer.length, pluginVer.length); i++) {
				try {
					updateCheck = 0;
					if (i < updateVer.length) {
						updateCheck = Integer.valueOf(updateVer[i]);
					}
					pluginCheck = 0;
					if (i < pluginVer.length) {
						pluginCheck = Integer.valueOf(pluginVer[i]);
					}
					if (updateCheck > pluginCheck) {
						return UpdateStatus.AVAILABLE;
					} else if (updateCheck < pluginCheck)
						return UpdateStatus.NOT_AVAILABLE;
				} catch (Exception e) {
					MobHunting.getInstance().getLogger().warning("Could not determine update's version # ");
					MobHunting.getInstance().getLogger().warning(
							"Installed plugin version: " + MobHunting.getInstance().getDescription().getVersion());
					MobHunting.getInstance().getLogger()
							.warning("Newest version on Bukkit.org: " + Updater.getBukkitUpdate().getVersionName());
					return UpdateStatus.UNKNOWN;
				}
			}
		} else {
			MobHunting.getInstance().getLogger().warning("Could not determine update's version # ");
			MobHunting.getInstance().getLogger()
					.warning("Installed plugin version: " + MobHunting.getInstance().getDescription().getVersion());
			MobHunting.getInstance().getLogger()
					.warning("Newest version on Bukkit.org: " + Updater.getBukkitUpdate().getVersionName());
			return UpdateStatus.UNKNOWN;
		}
		if ((updateCheck == pluginCheck && snapshot))
			return UpdateStatus.AVAILABLE;
		else
			return UpdateStatus.NOT_AVAILABLE;
	}**/
}
