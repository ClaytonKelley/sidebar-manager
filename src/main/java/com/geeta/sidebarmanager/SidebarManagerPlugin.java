package com.geeta.sidebarmanager;

import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;


@PluginDescriptor(
		name = "Sidebar Manager",
		description = "Customize and organize the RuneLite Plugin Sidebar"
)
public class SidebarManagerPlugin extends Plugin
{
	@Inject
	private SidebarManager sidebarManager;

	@Inject
	private ClientToolbar clientToolbar;

	private SidebarManagerPanel panel;

	private NavigationButton navigationButton;

	@Provides
	SidebarManagerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SidebarManagerConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new SidebarManagerPanel(sidebarManager);
		sidebarManager.setPanel(panel);

		BufferedImage icon = ImageUtil.loadImageResource(
				getClass(),
				"/sidebar_manager_icon.png"
		);

		navigationButton = NavigationButton.builder()
				.tooltip("Sidebar Manager")
				.icon(icon)
				.priority(10)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navigationButton);
		sidebarManager.start();
	}

	@Override
	protected void shutDown()
	{
		sidebarManager.stop();

		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(
					navigationButton
			);

			navigationButton = null;
		}

		panel = null;
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
				sidebarManager.applySettings();
				break;

			case "hiddenItems":
				sidebarManager.applyHiddenSettings();
				break;

			default:
				break;
		}

		//sidebarManager.applySettings();
	}
}