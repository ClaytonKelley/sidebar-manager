package com.geeta.sidebarmanager;

import java.awt.Component;
import javax.swing.Icon;

public class SidebarItem
{
    private final String name;
    private final Component component;
    private final Icon originalIcon;
    private final String tooltip;
    private final int originalIndex;

    public SidebarItem(
            String name,
            Component component,
            Icon originalIcon,
            String tooltip,
            int originalIndex)
    {
        this.name = name;
        this.component = component;
        this.originalIcon = originalIcon;
        this.tooltip = tooltip;
        this.originalIndex = originalIndex;
    }

    public String getName()
    {
        return name;
    }

    public Component getComponent()
    {
        return component;
    }

    public Icon getOriginalIcon()
    {
        return originalIcon;
    }

    public String getTooltip()
    {
        return tooltip;
    }

    public int getOriginalIndex()
    {
        return originalIndex;
    }
}