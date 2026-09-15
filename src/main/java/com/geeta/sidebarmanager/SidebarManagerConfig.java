package com.geeta.sidebarmanager;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("sidebarmanager")
public interface SidebarManagerConfig extends Config
{
	@Range(
			min = 16,
			max = 48
	)
	@ConfigItem(
			keyName = "iconSize",
			name = "Icon Size",
			description = "Changes the size of the RuneLite sidebar plugin icons",
			position = 0
	)
	default int iconSize()
	{
		return 16;
	}

	@Range(
			min = 0,
			max = 16
	)
	@ConfigItem(
			keyName = "iconSpacing",
			name = "Icon Spacing",
			description = "Changes the spacing around sidebar plugin icons",
			position = 1
	)
	default int iconSpacing()
	{
		return 2;
	}

	@Range(
			min = 0,
			max = 20
	)
	@ConfigItem(
			keyName = "sidebarPadding",
			name = "Sidebar Padding",
			description = "Changes the padding around sidebar tabs",
			position = 2
	)
	default int sidebarPadding()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "scrollableSidebar",
			name = "Scrollable Sidebar",
			description = "Keeps plugin icons in a single column and scrolls instead of creating additional columns",
			position = 3
	)
	default boolean scrollableSidebar()
	{
		return false;
	}

	@ConfigItem(
			keyName = "showPluginNames",
			name = "Show Plugin Names",
			description = "Shows the plugin name next to each sidebar icon",
			position = 4
	)
	default boolean showPluginNames()
	{
		return false;
	}

	@ConfigItem(
			keyName = "nameAlignment",
			name = "Plugin Name Alignment",
			description = "Controls the alignment of plugin names",
			position = 5
	)
	default NameAlignment nameAlignment()
	{
		return NameAlignment.LEFT;
	}


	@ConfigItem(
			keyName = "hiddenItems",
			name = "Hidden Items",
			description = "Stores hidden sidebar items",
			hidden = true
	)
	default String hiddenItems()
	{
		return "";
	}

	@ConfigItem(
			keyName = "itemOrder",
			name = "Item Order",
			description = "Stores the custom sidebar item order",
			hidden = true
	)
	default String itemOrder()
	{
		return "";
	}
	enum NameAlignment
	{
		LEFT,
		CENTER,
		RIGHT
	}
}