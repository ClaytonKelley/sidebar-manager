package com.geeta.sidebarmanager;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
		name = "Sidebar Manager",
		description = "Customize and organize the RuneLite Plugin Sidebar"
)
public class SidebarManagerPlugin extends Plugin
{
	private static final int DEFAULT_ICON_SIZE = 16;

	private static final String DEFAULT_STYLE =
			"tabInsets: 2,5,2,5; " +
					"variableSize: true; " +
					"deselectable: true; " +
					"tabHeight: 26";

	@Inject
	private SidebarManagerConfig config;

	@Inject
	private ClientUI clientUI;

	private JTabbedPane sidebar;

	private ChangeListener sidebarChangeListener;

	private final List<NavigationButton> navigationButtons =
			new ArrayList<>();

	@Provides
	SidebarManagerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SidebarManagerConfig.class);
	}

	@Override
	protected void startUp()
	{
		SwingUtilities.invokeLater(() ->
		{
			sidebar = findSidebar();

			if (sidebar == null)
			{
				System.out.println("Could not find RuneLite sidebar");
				return;
			}

			applySidebarSettings();
			installSidebarListener();

			SwingUtilities.invokeLater(
					this::updateCollapsedWidth
			);
		});
	}

	@Override
	protected void shutDown()
	{
		if (sidebar == null)
		{
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			removeSidebarListener();

			sidebar.setPreferredSize(null);
			sidebar.setMinimumSize(null);
			sidebar.setMaximumSize(null);

			restoreRuneLiteSidebar();

			sidebar = null;
			navigationButtons.clear();
		});
	}

	private JTabbedPane findSidebar()
	{
		for (Frame frame : Frame.getFrames())
		{
			JTabbedPane found =
					findTabbedPane(frame);

			if (found != null)
			{
				return found;
			}
		}

		return null;
	}

	private JTabbedPane findTabbedPane(
			Container container)
	{
		for (Component component :
				container.getComponents())
		{
			if (component instanceof JTabbedPane)
			{
				JTabbedPane tabbedPane =
						(JTabbedPane) component;

				if (tabbedPane.getTabPlacement()
						== JTabbedPane.RIGHT)
				{
					return tabbedPane;
				}
			}

			if (component instanceof Container)
			{
				JTabbedPane result =
						findTabbedPane(
								(Container) component
						);

				if (result != null)
				{
					return result;
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private void loadNavigationButtons()
	{
		navigationButtons.clear();

		try
		{
			Field sidebarEntriesField =
					ClientUI.class.getDeclaredField(
							"sidebarEntries"
					);

			sidebarEntriesField.setAccessible(true);

			Set<NavigationButton> sidebarEntries =
					(Set<NavigationButton>)
							sidebarEntriesField.get(clientUI);

			navigationButtons.addAll(
					sidebarEntries
			);
		}
		catch (ReflectiveOperationException e)
		{
			System.out.println(
					"Failed to access RuneLite sidebar entries: "
							+ e.getMessage()
			);
		}
	}

	private void applySidebarSettings()
	{
		if (sidebar == null)
		{
			return;
		}

		loadNavigationButtons();

		updateLayout();
		updateStyle();

		int count = Math.min(
				sidebar.getTabCount(),
				navigationButtons.size()
		);

		for (int i = 0; i < count; i++)
		{
			updateTab(
					i,
					navigationButtons.get(i)
			);
		}

		sidebar.revalidate();
		sidebar.repaint();

		SwingUtilities.invokeLater(
				this::updateCollapsedWidth
		);
	}

	private void updateTab(
			int index,
			NavigationButton navButton)
	{
		BufferedImage originalIcon =
				navButton.getIcon();

		if (originalIcon != null)
		{
			BufferedImage resizedIcon =
					ImageUtil.resizeImage(
							originalIcon,
							config.iconSize(),
							config.iconSize()
					);

			sidebar.setIconAt(
					index,
					new ImageIcon(resizedIcon)
			);
		}

		if (config.showPluginNames())
		{
			String name =
					navButton.getTooltip();

			if (name == null)
			{
				name = "";
			}

			sidebar.setTitleAt(
					index,
					name
			);
		}
		else
		{
			sidebar.setTitleAt(
					index,
					null
			);
		}
	}

	private void updateLayout()
	{
		if (config.scrollableSidebar())
		{
			sidebar.setTabLayoutPolicy(
					JTabbedPane.SCROLL_TAB_LAYOUT
			);
		}
		else
		{
			sidebar.setTabLayoutPolicy(
					JTabbedPane.WRAP_TAB_LAYOUT
			);
		}
	}

	private void updateStyle()
	{
		int vertical =
				config.iconSpacing();

		int horizontal =
				config.sidebarPadding();

		String alignment;

		switch (config.nameAlignment())
		{
			case RIGHT:
				alignment = "trailing";
				break;

			case CENTER:
				alignment = "center";
				break;

			case LEFT:
			default:
				alignment = "leading";
				break;
		}

		String style =
				"tabInsets: "
						+ vertical + ","
						+ horizontal + ","
						+ vertical + ","
						+ horizontal + "; "
						+ "variableSize: true; "
						+ "deselectable: true; "
						+ "tabHeight: "
						+ (config.iconSize()
						+ (vertical * 2))
						+ "; "
						+ "tabAlignment: "
						+ alignment;

		sidebar.putClientProperty(
				FlatClientProperties.STYLE,
				style
		);
	}

	private void installSidebarListener()
	{
		if (sidebarChangeListener != null)
		{
			return;
		}

		sidebarChangeListener = event ->
				SwingUtilities.invokeLater(
						this::updateCollapsedWidth
				);

		sidebar.addChangeListener(
				sidebarChangeListener
		);
	}

	private void removeSidebarListener()
	{
		if (sidebarChangeListener == null)
		{
			return;
		}

		sidebar.removeChangeListener(
				sidebarChangeListener
		);

		sidebarChangeListener = null;
	}

	private void updateCollapsedWidth()
	{
		if (sidebar == null)
		{
			return;
		}

		if (!config.scrollableSidebar())
		{
			clearSidebarSizeConstraint();
			refreshParentLayout();
			return;
		}

		if (sidebar.getSelectedIndex() != -1)
		{
			clearSidebarSizeConstraint();
			refreshParentLayout();
			return;
		}

		int tabWidth =
				calculateTabStripWidth();

		if (tabWidth <= 0)
		{
			return;
		}

		Dimension current =
				sidebar.getSize();

		int height = current.height;

		if (height <= 0)
		{
			height =
					sidebar.getPreferredSize().height;
		}

		Dimension collapsedSize =
				new Dimension(
						tabWidth,
						height
				);

		sidebar.setPreferredSize(
				collapsedSize
		);

		sidebar.setMinimumSize(
				new Dimension(
						tabWidth,
						0
				)
		);

		sidebar.setMaximumSize(
				new Dimension(
						tabWidth,
						Integer.MAX_VALUE
				)
		);

		refreshParentLayout();
	}

	private int calculateTabStripWidth()
	{
		int maxWidth = 0;

		for (int i = 0;
			 i < sidebar.getTabCount();
			 i++)
		{
			Rectangle bounds =
					sidebar.getBoundsAt(i);

			if (bounds != null &&
					bounds.width > maxWidth)
			{
				maxWidth = bounds.width;
			}
		}

		if (maxWidth > 0)
		{
			return maxWidth;
		}

		int iconWidth =
				config.iconSize();

		int padding =
				config.sidebarPadding() * 2;

		int estimatedWidth =
				iconWidth + padding + 4;

		if (config.showPluginNames())
		{
			int widestText = 0;

			for (NavigationButton navButton :
					navigationButtons)
			{
				String text =
						navButton.getTooltip();

				if (text == null)
				{
					continue;
				}

				int textWidth =
						sidebar
								.getFontMetrics(
										sidebar.getFont()
								)
								.stringWidth(text);

				if (textWidth > widestText)
				{
					widestText =
							textWidth;
				}
			}

			estimatedWidth +=
					widestText + 8;
		}

		return estimatedWidth;
	}

	private void clearSidebarSizeConstraint()
	{
		sidebar.setPreferredSize(null);
		sidebar.setMinimumSize(null);
		sidebar.setMaximumSize(null);
	}

	private void refreshParentLayout()
	{
		if (sidebar == null)
		{
			return;
		}

		sidebar.revalidate();
		sidebar.repaint();

		Container parent =
				sidebar.getParent();

		while (parent != null)
		{
			parent.revalidate();
			parent.repaint();

			parent = parent.getParent();
		}
	}

	private void restoreRuneLiteSidebar()
	{
		loadNavigationButtons();

		clearSidebarSizeConstraint();

		int count = Math.min(
				sidebar.getTabCount(),
				navigationButtons.size()
		);

		for (int i = 0; i < count; i++)
		{
			NavigationButton navButton =
					navigationButtons.get(i);

			sidebar.setTitleAt(
					i,
					null
			);

			BufferedImage originalIcon =
					navButton.getIcon();

			if (originalIcon != null)
			{
				BufferedImage resizedIcon =
						ImageUtil.resizeImage(
								originalIcon,
								DEFAULT_ICON_SIZE,
								DEFAULT_ICON_SIZE
						);

				sidebar.setIconAt(
						i,
						new ImageIcon(resizedIcon)
				);
			}
		}

		sidebar.setTabLayoutPolicy(
				JTabbedPane.WRAP_TAB_LAYOUT
		);

		sidebar.putClientProperty(
				FlatClientProperties.STYLE,
				DEFAULT_STYLE
		);

		sidebar.revalidate();
		sidebar.repaint();
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

		SwingUtilities.invokeLater(() ->
		{
			if (sidebar == null)
			{
				return;
			}

			clearSidebarSizeConstraint();
			applySidebarSettings();
		});
	}
}