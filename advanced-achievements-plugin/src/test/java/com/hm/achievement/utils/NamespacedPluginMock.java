package com.hm.achievement.utils;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.mockito.Mockito;

import com.hm.achievement.AdvancedAchievements;

import net.kyori.adventure.key.Namespaced;

/**
 * Creates plugin mocks able to build namespaced keys: from Minecraft 26.x, NamespacedKey derives the namespace of keys
 * created from a plugin from Namespaced#namespace rather than from Plugin#getName.
 */
public final class NamespacedPluginMock {

	public static AdvancedAchievements create() {
		AdvancedAchievements plugin = mock(AdvancedAchievements.class,
				Mockito.withSettings().extraInterfaces(Namespaced.class));
		lenient().when(plugin.getName()).thenReturn("AdvancedAchievements");
		lenient().when(((Namespaced) plugin).namespace()).thenReturn("advancedachievements");
		return plugin;
	}

	private NamespacedPluginMock() {
	}
}
