package com.hm.achievement.utils;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.hm.achievement.AdvancedAchievements;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Utility class to abstract Folia/Paper threaded region scheduler calls. Ensures tasks run on the appropriate scheduler
 * (global region, entity region, or async).
 *
 * @author Assistant
 */
public final class FoliaHelper {

	private static AdvancedAchievements plugin;

	private FoliaHelper() {
		// Utility class.
	}

	public static void init(AdvancedAchievements plugin) {
		FoliaHelper.plugin = plugin;
	}

	public static AdvancedAchievements getPlugin() {
		return plugin;
	}

	/**
	 * Runs a task on the global region scheduler (formerly "main thread").
	 */
	public static void runOnGlobal(Runnable runnable) {
		if (plugin == null) {
			runnable.run();
			return;
		}
		Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
	}

	/**
	 * Runs a delayed task on the global region scheduler.
	 *
	 * @param delayTicks delay in server ticks
	 * @param runnable code to execute
	 */
	public static ScheduledTask runLaterOnGlobal(long delayTicks, Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runnable.run(), delayTicks);
	}

	/**
	 * Runs a repeating task on the global region scheduler.
	 *
	 * @param initialDelayTicks initial delay in server ticks
	 * @param periodTicks period in server ticks
	 * @param runnable code to execute
	 */
	public static ScheduledTask runTimerOnGlobal(long initialDelayTicks, long periodTicks, Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> runnable.run(), initialDelayTicks,
				periodTicks);
	}

	/**
	 * Runs a task on the player's own region scheduler. This is the correct way to interact with a specific player on
	 * Folia.
	 */
	public static void runOnPlayer(Player player, Runnable runnable) {
		if (plugin == null) {
			runnable.run();
			return;
		}
		player.getScheduler().execute(plugin, runnable, null, 0L);
	}

	/**
	 * Runs a delayed task on the player's own region scheduler.
	 */
	public static ScheduledTask runLaterOnPlayer(Player player, long delayTicks, Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		return player.getScheduler().runDelayed(plugin, task -> runnable.run(), null, delayTicks);
	}

	/**
	 * Runs a task asynchronously (not tied to any region).
	 */
	public static void runAsync(Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
	}

	/**
	 * Runs a delayed asynchronous task.
	 *
	 * @param delay delay amount
	 * @param unit time unit
	 */
	public static ScheduledTask runLaterAsync(long delay, TimeUnit unit, Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		return Bukkit.getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), delay, unit);
	}

	/**
	 * Runs a repeating asynchronous task.
	 *
	 * @param initialDelay initial delay
	 * @param period repeat period
	 * @param unit time unit
	 */
	public static ScheduledTask runTimerAsync(long initialDelay, long period, TimeUnit unit, Runnable runnable) {
		if (plugin == null) {
			throw new IllegalStateException("FoliaHelper not initialised");
		}
		return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> runnable.run(), initialDelay, period, unit);
	}
}
