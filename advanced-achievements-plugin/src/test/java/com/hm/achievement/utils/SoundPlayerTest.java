package com.hm.achievement.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SoundPlayerTest {

	@Mock
	private Logger logger;

	@Mock
	private Player player;

	private Location location;

	@BeforeEach
	void setUp() {
		// Sound names are resolved through the server's sound registry.
		MockBukkit.mock();
		location = new Location(null, 0, 0, 0);
		lenient().when(player.getLocation()).thenReturn(location);
	}

	@AfterEach
	void tearDown() {
		MockBukkit.unmock();
	}

	@Test
	void shouldUseProvidedSoundIfValid() {
		SoundPlayer underTest = new SoundPlayer(logger);

		underTest.play(player, "ENTITY_FIREWORK_ROCKET_BLAST", "SOME_FALLBACK");

		verify(player).playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.7f);
	}

	@Test
	void shouldUseLowerCaseEnumNameIfValid() {
		SoundPlayer underTest = new SoundPlayer(logger);

		underTest.play(player, "entity_player_levelup", "SOME_FALLBACK");

		verify(player).playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.7f);
	}

	@Test
	void shouldUseRegistryNameIfValid() {
		SoundPlayer underTest = new SoundPlayer(logger);

		underTest.play(player, "minecraft:entity.player.levelup", "SOME_FALLBACK");

		ArgumentCaptor<Sound> soundCaptor = ArgumentCaptor.forClass(Sound.class);
		verify(player).playSound(eq(location), soundCaptor.capture(), eq(1.0f), eq(0.7f));
		assertEquals(Sound.ENTITY_PLAYER_LEVELUP.getKey(), soundCaptor.getValue().getKey());
	}

	@Test
	void shouldUseFallbackSoundIfProvidedInvalid() {
		SoundPlayer underTest = new SoundPlayer(logger);

		underTest.play(player, "INVALID", "ENTITY_FIREWORK_ROCKET_BLAST");

		verify(player).playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 0.7f);
	}

	@Test
	void shouldNotPlayAnythingIfBothSoundsAreInvalid() {
		SoundPlayer underTest = new SoundPlayer(logger);

		underTest.play(player, "INVALID", "ALSO_INVALID");

		verifyNoInteractions(player);
	}
}
