package com.geeta.sidebarmanager;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SidebarManagerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SidebarManagerPlugin.class);
		RuneLite.main(args);
	}
}