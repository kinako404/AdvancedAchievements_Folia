package com.hm.achievement.advancement;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.UnsafeValues;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.config.AchievementMap;
import com.hm.achievement.domain.Achievement;
import com.hm.achievement.domain.Achievement.AchievementBuilder;
import com.hm.achievement.gui.GUIItems;
import com.hm.achievement.gui.OrderedCategory;
import com.hm.achievement.utils.FoliaHelper;
import com.hm.achievement.utils.NamespacedPluginMock;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

@ExtendWith(MockitoExtension.class)
class AdvancementManagerTest {

	@Mock
	private GUIItems guiItems;
	private AdvancedAchievements plugin;
	@Mock
	private Logger logger;
	@Mock
	private UnsafeValues unsafeValues;
	@Mock
	private GlobalRegionScheduler globalRegionScheduler;
	@Mock
	private ScheduledTask generationTask;
	@Mock
	private CommandSender feedback;

	private YamlConfiguration mainConfig;
	private AchievementMap achievementMap;
	private AdvancementManager underTest;

	@BeforeEach
	void setUp() {
		plugin = NamespacedPluginMock.create();
		FoliaHelper.init(plugin);
		mainConfig = new YamlConfiguration();
		mainConfig.set("RegisterAdvancementDescriptions", true);
		mainConfig.set("HideAdvancements", false);
		mainConfig.set("ShowAdvancementToasts", true);
		mainConfig.set("RootAdvancementTitle", "Advanced Achievements");
		mainConfig.set("AdvancementsBackground", "minecraft:block/purple_concrete");
		mainConfig.set("AdvancementGenerationPerTick", 1);
		achievementMap = new AchievementMap();
		underTest = new AdvancementManager(mainConfig, guiItems, achievementMap, plugin, logger, Set.of());
		underTest.extractConfigurationParameters();
	}

	@Test
	void forceRegenerationShouldRemoveReloadThenLoadInOrder() {
		NamespacedKey rootKey = key(AdvancementManager.ADVANCED_ACHIEVEMENTS_PARENT);
		NamespacedKey childKey = key("place_5_stone");
		Advancement oldRoot = advancement(rootKey);
		Advancement oldChild = advancement(childKey);
		configureSingleAchievement();

		// Reproduce recovery after the broken command already deleted the persisted files.
		when(unsafeValues.removeAdvancement(rootKey)).thenReturn(false);
		when(unsafeValues.removeAdvancement(childKey)).thenReturn(false);
		Advancement loadedRoot = mock(Advancement.class);
		Advancement loadedChild = mock(Advancement.class);
		when(unsafeValues.loadAdvancement(eq(rootKey), anyString())).thenReturn(loadedRoot);
		when(unsafeValues.loadAdvancement(eq(childKey), anyString())).thenReturn(loadedChild);
		ArgumentCaptor<Consumer<ScheduledTask>> scheduled = scheduledTaskCaptor();
		Runnable complete = mock(Runnable.class);

		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			configureBukkit(bukkit);
			bukkit.when(Bukkit::advancementIterator)
					.thenReturn(java.util.List.of(oldRoot, oldChild).iterator(), Collections.emptyIterator());

			underTest.generateAdvancementsIncremental(true, feedback, complete);
			Runnable tick = scheduledTick(scheduled);

			tick.run();
			verify(unsafeValues).removeAdvancement(rootKey);
			verify(unsafeValues, never()).loadAdvancement(eq(rootKey), anyString());
			bukkit.verify(Bukkit::reloadData, never());

			tick.run();
			verify(unsafeValues).removeAdvancement(childKey);
			verify(unsafeValues, never()).loadAdvancement(eq(rootKey), anyString());

			tick.run();
			bukkit.verify(Bukkit::reloadData);
			verify(unsafeValues, never()).loadAdvancement(eq(rootKey), anyString());

			tick.run();
			verify(unsafeValues).loadAdvancement(eq(rootKey), anyString());
			verify(unsafeValues, never()).loadAdvancement(eq(childKey), anyString());

			tick.run();
			verify(unsafeValues).loadAdvancement(eq(childKey), anyString());
		}

		InOrder loadOrder = inOrder(unsafeValues);
		loadOrder.verify(unsafeValues).loadAdvancement(eq(rootKey), anyString());
		loadOrder.verify(unsafeValues).loadAdvancement(eq(childKey), anyString());
		verify(generationTask).cancel();
		verify(complete).run();
	}

	@Test
	void startupGenerationShouldOnlyLoadMissingAdvancementsWithoutReloadingData() {
		NamespacedKey rootKey = key(AdvancementManager.ADVANCED_ACHIEVEMENTS_PARENT);
		NamespacedKey childKey = key("place_5_stone");
		configureSingleAchievement();
		Advancement existingRoot = mock(Advancement.class);
		when(unsafeValues.loadAdvancement(eq(childKey), anyString())).thenReturn(mock(Advancement.class));
		ArgumentCaptor<Consumer<ScheduledTask>> scheduled = scheduledTaskCaptor();

		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			configureBukkit(bukkit);
			bukkit.when(() -> Bukkit.getAdvancement(rootKey)).thenReturn(existingRoot);
			bukkit.when(() -> Bukkit.getAdvancement(childKey)).thenReturn(null);

			underTest.generateAdvancementsIncremental(false, null, null);
			scheduledTick(scheduled).run();

			bukkit.verify(Bukkit::advancementIterator, never());
			bukkit.verify(Bukkit::reloadData, never());
		}

		verify(unsafeValues, never()).removeAdvancement(rootKey);
		verify(unsafeValues, never()).loadAdvancement(eq(rootKey), anyString());
		verify(unsafeValues).loadAdvancement(eq(childKey), anyString());
		verify(generationTask).cancel();
	}

	@Test
	void generationFailureShouldCancelAndReportOnlyOnce() {
		NamespacedKey rootKey = key(AdvancementManager.ADVANCED_ACHIEVEMENTS_PARENT);
		when(guiItems.getOrderedAchievementItems()).thenReturn(Collections.emptyMap());
		IllegalArgumentException failure = new IllegalArgumentException("already exists");
		when(unsafeValues.loadAdvancement(eq(rootKey), anyString())).thenThrow(failure);
		ArgumentCaptor<Consumer<ScheduledTask>> scheduled = scheduledTaskCaptor();
		Runnable complete = mock(Runnable.class);

		try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
			configureBukkit(bukkit);
			bukkit.when(() -> Bukkit.getAdvancement(rootKey)).thenReturn(null);

			underTest.generateAdvancementsIncremental(false, feedback, complete);
			Runnable tick = scheduledTick(scheduled);
			tick.run();
			tick.run();

			underTest.generateAdvancementsIncremental(false, feedback, complete);
		}

		verify(unsafeValues).loadAdvancement(eq(rootKey), anyString());
		verify(generationTask).cancel();
		verify(logger).log(eq(Level.SEVERE), eq("Advancement generation failed while loading advancement " + rootKey + "."),
				eq(failure));
		verify(feedback).sendMessage("§cAdvancement generation failed. Check the server logs for details.");
		verify(complete, never()).run();
		verify(globalRegionScheduler, times(2)).runAtFixedRate(eq(plugin), scheduled.capture(), eq(1L), eq(1L));
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<Consumer<ScheduledTask>> scheduledTaskCaptor() {
		ArgumentCaptor<Consumer<ScheduledTask>> scheduled = ArgumentCaptor.forClass(Consumer.class);
		when(globalRegionScheduler.runAtFixedRate(eq(plugin), scheduled.capture(), eq(1L), eq(1L)))
				.thenReturn(generationTask);
		return scheduled;
	}

	/**
	 * Runs one iteration of the repeating generation task registered on the global region scheduler, exactly as the
	 * server would.
	 */
	private Runnable scheduledTick(ArgumentCaptor<Consumer<ScheduledTask>> scheduled) {
		Consumer<ScheduledTask> generationStep = scheduled.getValue();
		return () -> generationStep.accept(generationTask);
	}

	private void configureBukkit(MockedStatic<Bukkit> bukkit) {
		bukkit.when(Bukkit::getUnsafe).thenReturn(unsafeValues);
		bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(globalRegionScheduler);
		bukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
		bukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.1");
	}

	private void configureSingleAchievement() {
		Achievement achievement = new AchievementBuilder()
				.category(MultipleAchievements.PLACES)
				.subcategory("stone")
				.name("place_5_stone")
				.displayName("Stone Setter")
				.goal("Place 5 stone blocks.")
				.build();
		achievementMap.put(achievement);
		ItemStack item = mock(ItemStack.class);
		when(item.getType()).thenReturn(Material.STONE);
		Map<OrderedCategory, ItemStack> items = new LinkedHashMap<>();
		items.put(new OrderedCategory(1, MultipleAchievements.PLACES), item);
		when(guiItems.getOrderedAchievementItems()).thenReturn(items);
	}

	private Advancement advancement(NamespacedKey key) {
		Advancement advancement = mock(Advancement.class);
		when(advancement.getKey()).thenReturn(key);
		return advancement;
	}

	private NamespacedKey key(String value) {
		return new NamespacedKey(plugin, value);
	}
}
