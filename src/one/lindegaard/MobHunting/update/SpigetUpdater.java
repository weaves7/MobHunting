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
	private static String currentJarFile = "";
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

	public static String getCurrentJarFile() {
		return currentJarFile;
	}

	public static void setCurrentJarFile(String name) {
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
		boolean succes = false;
		final String OS = System.getProperty("os.name");
		if (OS.indexOf("Win") >= 0) {
			succes = spigetUpdate.downloadUpdate();
			if (succes) {
				File downloadedJar = new File("plugins/update/MobHunting-" + newDownloadVersion + ".jar");
				File newJar = new File("plugins/update/MobHunting.jar");
				if (newJar.exists())
					newJar.delete();
				downloadedJar.renameTo(newJar);
				return true;
			}
		} else {
			if (updateAvailable != UpdateStatus.RESTART_NEEDED)
				succes = spigetUpdate.downloadUpdate();
			if (succes) {
				File currentJar = new File("plugins/" + getCurrentJarFile());
				File disabledJar = new File("plugins/" + getCurrentJarFile() + ".old");
				int count = 0;
				while (disabledJar.exists() && count++ < 100) {
					disabledJar = new File("plugins/" + getCurrentJarFile() + ".old" + count);
				}
				if (!disabledJar.exists()) {
					currentJar.renameTo(disabledJar);

					File downloadedJar = new File(
							"plugins/MobHunting/update/MobHunting-" + newDownloadVersion + ".jar");
					File newJar = new File("plugins/MobHunting-" + newDownloadVersion + ".jar");
					downloadedJar.renameTo(newJar);
					updateAvailable = UpdateStatus.RESTART_NEEDED;
					return true;
				}
			}
		}
		return false;
	}

	public void checkForUpdate(final CommandSender sender, boolean updateCheck, final boolean silent) {

		if (updateCheck) {
			if (!silent)
				Bukkit.getConsoleSender().sendMessage(
						ChatColor.GOLD + "[MobHunting] " + Messages.getString("mobhunting.commands.update.check"));

			if (updateAvailable != UpdateStatus.RESTART_NEEDED) {
				spigetUpdate = new SpigetUpdate(plugin, 3582);
				spigetUpdate.setVersionComparator(VersionComparator.SEM_VER);
				spigetUpdate.setUserAgent("MobHunting/" + plugin.getDescription().getVersion());

				spigetUpdate.checkForUpdate(new UpdateCallback() {

					@Override
					public void updateAvailable(String newVersion, String downloadUrl, boolean hasDirectDownload) {
						//// A new version is available
						Bukkit.getConsoleSender()
								.sendMessage(ChatColor.RED + "A new version is available: " + newVersion);
						updateAvailable = UpdateStatus.AVAILABLE;
						newDownloadVersion = newVersion;
						sender.sendMessage(ChatColor.GREEN + "[MobHunting] "
								+ Messages.getString("mobhunting.commands.update.version-found"));
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
}
