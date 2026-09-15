package com.geeta.sidebarmanager;

import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "Sidebar Manager",
		description = "Customize and organize the RuneLite Plugin Sidebar"
)
public class SidebarManagerPlugin extends Plugin
{
	@Inject
	private SidebarManager sidebarManager;

	@Provides
	SidebarManagerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SidebarManagerConfig.class);
	}

	@Override
	protected void startUp()
	{
		sidebarManager.start();
	}

	@Override
	protected void shutDown()
	{
		sidebarManager.stop();
	}

	@Subscribe
	public void onConfigChanged(
			ConfigChanged event)
	{
		if (!event.getGroup().equals(
				"sidebarmanager"))
		{
			return;
		}

		switch (event.getKey())
		{
			case "iconSize":
			case "iconSpacing":
			case "sidebarPadding":
			case "scrollableSidebar":
			case "showPluginNames":
			case "nameAlignment":
				break;

			default:
				return;
		}

		sidebarManager.applySettings();
	}
}