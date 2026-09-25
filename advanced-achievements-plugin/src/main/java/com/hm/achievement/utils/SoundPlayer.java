package com.hm.achievement.utils;

import java.util.Locale;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Class in charge of player sounds when players successfully complete some actions.
 *
 * @author Pyves
 */
@Singleton
public class SoundPlayer {

	private final Logger logger;

	@Inject
	public SoundPlayer(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Plays a sound provided via configuration. If the sound is invalid, this method falls back to the provided
	 * fallback.
	 *
	 * @param player
	 * @param providedSound
	 * @param fallbackSound
	 */
	public void play(Player player, String providedSound, String fallbackSound) {
		Sound sound = resolveSound(providedSound);
		if (sound == null) {
			logger.warning("Sound " + providedSound + " is invalid, using default instead.");
			sound = resolveSound(fallbackSound);
		}
		if (sound != null) {
			player.playSound(player.getLocation(), sound, 1, 0.7f);
		}
	}

	/**
	 * Resolves a sound from its configuration name: either an enum constant name or a registry name from the default
	 * configuration and the project wiki (lower case, with underscores or dots, optionally namespaced). The former enum
	 * validity check could not be kept as Sound is an interface in Minecraft 26.x rather than an enum.
	 *
	 * @param name
	 * @return the resolved sound, or null if the name is invalid
	 */
	private Sound resolveSound(String name) {
		if (name == null) {
			return null;
		}
		try {
			return Sound.valueOf(name);
		} catch (IllegalArgumentException e) {
			// Not an enum constant name, try the other forms below.
		}
		try {
			return Sound.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			// Not a lower case enum constant name either, try the registry names below.
		}
		String registryName = name.toLowerCase(Locale.ROOT);
		Sound sound = fromRegistry(registryName);
		// Registry keys mix underscores and dots, so the wiki form with dots as separators is also tried.
		return sound != null ? sound : fromRegistry(registryName.replace('_', '.'));
	}

	private Sound fromRegistry(String name) {
		try {
			NamespacedKey key = NamespacedKey.fromString(name);
			return key == null ? null : Registry.SOUNDS.get(key);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}
