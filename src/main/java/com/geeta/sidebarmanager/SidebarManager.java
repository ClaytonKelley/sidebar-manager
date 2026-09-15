package com.geeta.sidebarmanager;

import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import net.runelite.client.util.ImageUtil;

public class SidebarManager
{
    private static final String DEFAULT_STYLE =
            "tabInsets: 2,5,2,5; " +
                    "variableSize: true; " +
                    "deselectable: true; " +
                    "tabHeight: 26";

    private final SidebarManagerConfig config;

    private final List<Icon> originalIcons =
            new ArrayList<>();

    private JTabbedPane sidebar;

    private ChangeListener sidebarChangeListener;

    @Inject
    public SidebarManager(
            SidebarManagerConfig config)
    {
        this.config = config;
    }

    public void start()
    {
        SwingUtilities.invokeLater(() ->
        {
            sidebar = findSidebar();

            if (sidebar == null)
            {
                System.out.println(
                        "Could not find RuneLite sidebar"
                );
                return;
            }

            captureOriginalIcons();
            applySidebarSettings();
            installSidebarListener();

            SwingUtilities.invokeLater(
                    this::updateCollapsedWidth
            );
        });
    }

    public void stop()
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

            originalIcons.clear();
            sidebar = null;
        });
    }

    public void applySettings()
    {
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

    private void captureOriginalIcons()
    {
        originalIcons.clear();

        for (int i = 0;
             i < sidebar.getTabCount();
             i++)
        {
            originalIcons.add(
                    sidebar.getIconAt(i)
            );
        }
    }

    private void applySidebarSettings()
    {
        if (sidebar == null)
        {
            return;
        }

        updateLayout();
        updateStyle();

        for (int i = 0;
             i < sidebar.getTabCount();
             i++)
        {
            updateTab(i);
        }

        sidebar.revalidate();
        sidebar.repaint();

        SwingUtilities.invokeLater(
                this::updateCollapsedWidth
        );
    }

    private void updateTab(int index)
    {
        if (index < originalIcons.size())
        {
            Icon originalIcon =
                    originalIcons.get(index);

            if (originalIcon != null)
            {
                if (config.iconSize() ==
                        originalIcon.getIconWidth() &&
                        config.iconSize() ==
                                originalIcon.getIconHeight())
                {
                    sidebar.setIconAt(
                            index,
                            originalIcon
                    );
                }
                else
                {
                    BufferedImage sourceIcon =
                            iconToBufferedImage(
                                    originalIcon
                            );

                    BufferedImage resizedIcon =
                            ImageUtil.resizeImage(
                                    sourceIcon,
                                    config.iconSize(),
                                    config.iconSize()
                            );

                    sidebar.setIconAt(
                            index,
                            new ImageIcon(
                                    resizedIcon
                            )
                    );
                }
            }
        }

        if (config.showPluginNames())
        {
            String name =
                    sidebar.getToolTipTextAt(index);

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

    private BufferedImage iconToBufferedImage(
            Icon icon)
    {
        BufferedImage image =
                new BufferedImage(
                        icon.getIconWidth(),
                        icon.getIconHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );

        Graphics2D graphics =
                image.createGraphics();

        icon.paintIcon(
                null,
                graphics,
                0,
                0
        );

        graphics.dispose();

        return image;
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

            for (int i = 0;
                 i < sidebar.getTabCount();
                 i++)
            {
                String text =
                        sidebar.getToolTipTextAt(i);

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
        clearSidebarSizeConstraint();

        for (int i = 0;
             i < sidebar.getTabCount();
             i++)
        {
            sidebar.setTitleAt(
                    i,
                    null
            );

            if (i < originalIcons.size())
            {
                sidebar.setIconAt(
                        i,
                        originalIcons.get(i)
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
}