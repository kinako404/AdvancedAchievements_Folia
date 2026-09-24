package com.hm.achievement.runnable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.config.AchievementMap;
import com.hm.achievement.db.CacheManager;
import com.hm.achievement.utils.FoliaHelper;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;

class AchieveDistanceRunnableTest {

	private static final UUID FIRST_UUID = UUID.randomUUID();
	private static final UUID SECOND_UUID = UUID.randomUUID();

	private final YamlConfiguration mainConfig = new YamlConfiguration();
	private final AchievementMap achievementMap = new AchievementMap();
	private final CacheManager cacheManager = mock(CacheManager.class);
	private final Player firstPlayer = mock(Player.class);
	private final Player secondPlayer = mock(Player.class);
	private final EntityScheduler firstScheduler = mock(EntityScheduler.class);
	private final EntityScheduler secondScheduler = mock(EntityScheduler.class);
	private final World firstWorld = mock(World.class);
	private final World secondWorld = mock(World.class);
	private final Location firstPreviousLocation = new Location(firstWorld, 0, 0, 0);
	private final Location firstCurrentLocation = new Location(firstWorld, 0, 0, 10);
	private final Location secondLocation = new Location(secondWorld, 0, 0, 0);

	private MockedStatic<Bukkit> bukkit;
	private AchieveDistanceRunnable underTest;

	@BeforeEach
	void setUp() {
		bukkit = mockStatic(Bukkit.class);
		bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(firstPlayer, secondPlayer));
		FoliaHelper.init(mock(AdvancedAchievements.class));

		underTest = new AchieveDistanceRunnable(mainConfig, achievementMap, cacheManager, Set.of());
		underTest.extractConfigurationParameters();

		when(firstWorld.getName()).thenReturn("world");
		when(firstWorld.getUID()).thenReturn(UUID.randomUUID());
		when(secondWorld.getName()).thenReturn("world");
		when(secondWorld.getUID()).thenReturn(UUID.randomUUID());
		preparePlayer(firstPlayer, FIRST_UUID, firstScheduler, firstWorld, firstPreviousLocation);
		preparePlayer(secondPlayer, SECOND_UUID, secondScheduler, secondWorld, secondLocation);
	}

	@AfterEach
	void tearDown() {
		bukkit.close();
	}

	@Test
	void shouldScheduleOneTaskPerPlayerOnOwningRegionWithoutReadingLocationInline() {
		underTest.run();

		ArgumentCaptor<Runnable> firstTask = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Runnable> secondTask = ArgumentCaptor.forClass(Runnable.class);
		verify(firstScheduler, times(1)).execute(any(), firstTask.capture(), any(), anyLong());
		verify(secondScheduler, times(1)).execute(any(), secondTask.capture(), any(), anyLong());

		verify(firstPlayer, never()).getLocation();
		verify(secondPlayer, never()).getLocation();
		verify(cacheManager, never()).getAndIncrementStatisticAmount(any(NormalAchievements.class), any(UUID.class),
				anyInt());
	}

	@Test
	void shouldUpdateDistanceForOwningPlayerWhenRegionTaskRuns() {
		when(firstPlayer.getLocation()).thenReturn(firstCurrentLocation);
		underTest.updateLocation(FIRST_UUID, firstPreviousLocation);

		underTest.run();

		ArgumentCaptor<Runnable> firstTask = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Runnable> secondTask = ArgumentCaptor.forClass(Runnable.class);
		verify(firstScheduler).execute(any(), firstTask.capture(), any(), anyLong());
		verify(secondScheduler).execute(any(), secondTask.capture(), any(), anyLong());

		firstTask.getValue().run();

		verify(cacheManager).getAndIncrementStatisticAmount(NormalAchievements.DISTANCEFOOT, FIRST_UUID, 10);
		verify(cacheManager, never()).getAndIncrementStatisticAmount(any(NormalAchievements.class), eq(SECOND_UUID),
				anyInt());
		verify(secondPlayer, never()).getLocation();
	}

	private void preparePlayer(Player player, UUID uuid, EntityScheduler scheduler, World world, Location location) {
		when(player.getUniqueId()).thenReturn(uuid);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(player.getWorld()).thenReturn(world);
		when(player.getLocation()).thenReturn(location);
		when(player.getScheduler()).thenReturn(scheduler);
	}
}
