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
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
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

class AchievePlayTimeRunnableTest {

	private static final UUID FIRST_UUID = UUID.randomUUID();
	private static final UUID SECOND_UUID = UUID.randomUUID();

	private final YamlConfiguration mainConfig = new YamlConfiguration();
	private final AchievementMap achievementMap = new AchievementMap();
	private final CacheManager cacheManager = mock(CacheManager.class);
	private final Player firstPlayer = mock(Player.class);
	private final Player secondPlayer = mock(Player.class);
	private final EntityScheduler firstScheduler = mock(EntityScheduler.class);
	private final EntityScheduler secondScheduler = mock(EntityScheduler.class);

	private MockedStatic<Bukkit> bukkit;
	private AchievePlayTimeRunnable underTest;

	@BeforeEach
	void setUp() {
		bukkit = mockStatic(Bukkit.class);
		bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
		bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(firstPlayer, secondPlayer));
		FoliaHelper.init(mock(AdvancedAchievements.class));

		underTest = new AchievePlayTimeRunnable(mainConfig, achievementMap, cacheManager);
		underTest.extractConfigurationParameters();

		preparePlayer(firstPlayer, FIRST_UUID, firstScheduler);
		preparePlayer(secondPlayer, SECOND_UUID, secondScheduler);
	}

	@AfterEach
	void tearDown() {
		bukkit.close();
	}

	@Test
	void shouldScheduleOneTaskPerPlayerOnOwningRegionWithoutTouchingPlayerState() {
		underTest.run();

		ArgumentCaptor<Runnable> firstTask = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Runnable> secondTask = ArgumentCaptor.forClass(Runnable.class);
		verify(firstScheduler, times(1)).execute(any(), firstTask.capture(), any(), anyLong());
		verify(secondScheduler, times(1)).execute(any(), secondTask.capture(), any(), anyLong());

		verify(firstPlayer, never()).getGameMode();
		verify(firstPlayer, never()).getWorld();
		verify(secondPlayer, never()).getGameMode();
		verify(secondPlayer, never()).getWorld();
		verify(cacheManager, never()).getAndIncrementStatisticAmount(any(NormalAchievements.class), any(UUID.class),
				anyInt());

		firstTask.getValue().run();
		secondTask.getValue().run();

		verify(cacheManager).getAndIncrementStatisticAmount(eq(NormalAchievements.PLAYEDTIME), eq(FIRST_UUID), anyInt());
		verify(cacheManager).getAndIncrementStatisticAmount(eq(NormalAchievements.PLAYEDTIME), eq(SECOND_UUID), anyInt());
	}

	@Test
	void shouldRunCapturedTaskForItsOwnPlayerOnly() {
		underTest.run();

		ArgumentCaptor<Runnable> firstTask = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Runnable> secondTask = ArgumentCaptor.forClass(Runnable.class);
		verify(firstScheduler).execute(any(), firstTask.capture(), any(), anyLong());
		verify(secondScheduler).execute(any(), secondTask.capture(), any(), anyLong());

		firstTask.getValue().run();

		verify(cacheManager).getAndIncrementStatisticAmount(eq(NormalAchievements.PLAYEDTIME), eq(FIRST_UUID), anyInt());
		verify(cacheManager, never()).getAndIncrementStatisticAmount(any(NormalAchievements.class), eq(SECOND_UUID),
				anyInt());

		secondTask.getValue().run();

		verify(cacheManager).getAndIncrementStatisticAmount(eq(NormalAchievements.PLAYEDTIME), eq(SECOND_UUID), anyInt());
	}

	private void preparePlayer(Player player, UUID uuid, EntityScheduler scheduler) {
		World world = mock(World.class);
		when(world.getName()).thenReturn("world");
		when(player.getUniqueId()).thenReturn(uuid);
		when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
		when(player.getWorld()).thenReturn(world);
		when(player.getScheduler()).thenReturn(scheduler);
	}
}
