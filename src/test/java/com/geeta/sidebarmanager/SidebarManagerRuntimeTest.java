package com.geeta.sidebarmanager;

import java.awt.image.BufferedImage;
import java.awt.event.ContainerListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** Exercises isolated Swing components, without launching RuneLite or generating game input. */
public class SidebarManagerRuntimeTest
{
    private final Map<String, String> settings = new HashMap<>();
    private final AtomicReference<Throwable> queuedFailure = new AtomicReference<>();
    private final ImageIcon icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
    private JTabbedPane sidebar;
    private SidebarManager manager;
    private JPanel bank;
    private JPanel notes;
    private JPanel managerPanel;
    private boolean scrollable;
    private int configWrites;
    private ContainerListener[] originalContainerListeners;

    @Before
    public void setUp() throws Exception
    {
        onEdt(() ->
        {
            sidebar = new JTabbedPane(JTabbedPane.RIGHT);
            originalContainerListeners = sidebar.getContainerListeners();
            bank = new JPanel();
            notes = new JPanel();
            managerPanel = new JPanel();
            sidebar.insertTab(null, icon, bank, "Bank Value History", 0);
            sidebar.insertTab(null, icon, notes, "Notes", 1);
            sidebar.insertTab(null, icon, managerPanel, "Sidebar Manager", 2);
            sidebar.setSelectedIndex(-1);
            SidebarManagerConfig config = new SidebarManagerConfig()
            {
                @Override
                public boolean showPluginNames()
                {
                    return true;
                }

                @Override
                public boolean scrollableSidebar()
                {
                    return scrollable;
                }
            };
            manager = new SidebarManager(config, null)
            {
                @Override
                JTabbedPane findSidebar()
                {
                    return sidebar;
                }

                @Override
                String getStoredValue(String key)
                {
                    return settings.get(key);
                }

                @Override
                void setStoredValue(String key, String value)
                {
                    configWrites++;
                    if (value == null)
                    {
                        settings.remove(key);
                    }
                    else
                    {
                        settings.put(key, value);
                    }
                    // Match ConfigManager's synchronous event notification and deferred UI update.
                    if ("hiddenItems".equals(key))
                    {
                        applyHiddenSettings();
                    }
                    else if ("itemOrder".equals(key))
                    {
                        applyOrderSettings();
                    }
                }
            };
            manager.start();
        });
        drainEdt();
    }

    @After
    public void tearDown() throws Exception
    {
        onEdt(manager::stop);
        drainEdt();
    }

    @Test
    public void replacementCannotResurrectOldComponent() throws Exception
    {
        JPanel replacement = new JPanel();
        SidebarItem[] oldItem = new SidebarItem[1];
        onEdt(() ->
        {
            oldItem[0] = item("Bank Value History");
            changePlugin(() -> sidebar.remove(bank));
        });
        drainEdt();
        onEdt(() -> assertNull(item("Bank Value History")));
        onEdt(() -> changePlugin(() -> sidebar.insertTab(null, icon, replacement, "Bank Value History", 0)));
        drainEdt();
        onEdt(() ->
        {
            assertSame(replacement, item("Bank Value History").getComponent());
            assertEquals("Bank Value History", sidebar.getTitleAt(sidebar.indexOfComponent(replacement)));
            manager.showItem(oldItem[0]);
            manager.hideItem(item("Bank Value History"));
        });
        drainEdt();
        onEdt(() -> manager.showItem(item("Bank Value History")));
        drainEdt();
        onEdt(() ->
        {
            assertEquals(3, sidebar.getTabCount());
            assertEquals(-1, sidebar.indexOfComponent(bank));
            assertEquals(1, countTabs("Bank Value History"));
        });
    }

    @Test
    public void removingHiddenPluginDoesNotResurrectItOnShowAllOrShutdown() throws Exception
    {
        onEdt(() -> manager.hideItem(item("Bank Value History")));
        drainEdt();
        onEdt(() -> changePlugin(() -> sidebar.remove(bank)));
        drainEdt();
        onEdt(() ->
        {
            assertNull(item("Bank Value History"));
            assertEquals("Bank Value History", settings.get("hiddenItems"));
            manager.showAllItems();
        });
        drainEdt();
        onEdt(manager::stop);
        drainEdt();
        onEdt(() ->
        {
            assertEquals(-1, sidebar.indexOfComponent(bank));
            assertEquals(2, sidebar.getTabCount());
            assertSame(notes, sidebar.getComponentAt(0));
            assertSame(managerPanel, sidebar.getComponentAt(1));
        });
    }

    @Test
    public void newPluginUsesNativeInsertionIndexWithHiddenTabsAndKeepsSelection() throws Exception
    {
        JPanel added = new JPanel();
        onEdt(() ->
        {
            manager.moveItem(item("Sidebar Manager"), 0);
            manager.hideItem(item("Bank Value History"));
            sidebar.setSelectedComponent(notes);
        });
        drainEdt();
        int writesBefore = configWrites;
        onEdt(() -> changePlugin(() -> sidebar.insertTab(null, icon, added, "RS-Bingo", 3)));
        drainEdt();
        onEdt(() ->
        {
            assertNotNull(item("RS-Bingo"));
            assertEquals("RS-Bingo", sidebar.getTitleAt(sidebar.indexOfComponent(added)));
            assertSame(notes, sidebar.getSelectedComponent());
            assertEquals(-1, sidebar.indexOfComponent(bank));
            assertEquals(Arrays.asList("Sidebar Manager", "Notes", "RS-Bingo"), visibleNames());
            assertEquals(writesBefore, configWrites);
            manager.resetItemOrder();
        });
        drainEdt();
        onEdt(() -> assertEquals(Arrays.asList("Notes", "Sidebar Manager", "RS-Bingo"), visibleNames()));
    }

    @Test
    public void batchedPluginChangesWaitForAllQueuedNavigationChanges() throws Exception
    {
        JPanel replacement = new JPanel();
        JPanel added = new JPanel();
        onEdt(() -> manager.hideItem(item("Bank Value History")));
        drainEdt();
        onEdt(() ->
        {
            changePlugin(() -> sidebar.remove(bank));
            changePlugin(() -> sidebar.insertTab(null, icon, replacement, "Bank Value History", 0));
            changePlugin(() -> sidebar.insertTab(null, icon, added, "RS-Bingo", 3));
        });
        drainEdt();
        onEdt(() ->
        {
            assertSame(replacement, item("Bank Value History").getComponent());
            assertEquals(-1, sidebar.indexOfComponent(bank));
            assertEquals(-1, sidebar.indexOfComponent(replacement));
            assertEquals(4, manager.getItems().size());
            assertNotNull(item("RS-Bingo"));
            manager.showAllItems();
        });
        drainEdt();
        onEdt(() -> assertEquals(1, countTabs("Bank Value History")));
    }

    @Test
    public void externalChangesOutsideLifecycleEventsAreTracked() throws Exception
    {
        JPanel replacement = new JPanel();
        onEdt(() ->
        {
            sidebar.remove(bank);
            sidebar.insertTab(null, icon, replacement, "Bank Value History", 0);
        });
        drainEdt();
        onEdt(() ->
        {
            assertSame(replacement, item("Bank Value History").getComponent());
            assertEquals("Bank Value History", sidebar.getTitleAt(0));
            assertEquals(3, manager.getItems().size());
        });
    }

    @Test
    public void collapsedScrollableSidebarStaysDeselectedAcrossLifecycleChanges() throws Exception
    {
        onEdt(() ->
        {
            scrollable = true;
            manager.applySettings();
        });
        drainEdt();
        onEdt(() -> changePlugin(() -> sidebar.remove(bank)));
        drainEdt();
        onEdt(() ->
        {
            assertEquals(-1, sidebar.getSelectedIndex());
            assertTrue(sidebar.isPreferredSizeSet());
            sidebar.setSelectedComponent(notes);
        });
        drainEdt();
        onEdt(() ->
        {
            assertFalse(sidebar.isPreferredSizeSet());
            assertFalse(sidebar.isMinimumSizeSet());
            assertFalse(sidebar.isMaximumSizeSet());
        });
    }

    @Test
    public void savedOrderRetainsDisabledPluginWhenOtherItemsAreReordered() throws Exception
    {
        onEdt(() -> manager.moveItem(item("Notes"), 0));
        drainEdt();
        onEdt(() -> changePlugin(() -> sidebar.remove(bank)));
        drainEdt();
        onEdt(() -> manager.moveItem(item("Sidebar Manager"), 0));
        drainEdt();
        onEdt(() -> assertEquals("Sidebar Manager\nBank Value History\nNotes", settings.get("itemOrder")));
        JPanel replacement = new JPanel();
        onEdt(() -> changePlugin(() -> sidebar.insertTab(null, icon, replacement, "Bank Value History", 0)));
        drainEdt();
        onEdt(() -> assertEquals(Arrays.asList("Sidebar Manager", "Bank Value History", "Notes"), visibleNames()));
    }

    @Test
    public void shutdownCancelsPendingRefreshAndRemovesListeners() throws Exception
    {
        JPanel added = new JPanel();
        onEdt(() ->
        {
            manager.hideItem(item("Bank Value History"));
            changePlugin(() -> sidebar.insertTab(null, icon, added, "RS-Bingo", 3));
            manager.stop();
        });
        drainEdt();
        onEdt(() ->
        {
            assertTrue(manager.getItems().isEmpty());
            assertEquals(4, sidebar.getTabCount());
            assertArrayEquals(originalContainerListeners, sidebar.getContainerListeners());
            assertEquals(JTabbedPane.WRAP_TAB_LAYOUT, sidebar.getTabLayoutPolicy());
            for (int index = 0; index < sidebar.getTabCount(); index++)
            {
                assertNull(sidebar.getTitleAt(index));
                assertSame(icon, sidebar.getIconAt(index));
            }
        });
    }

    private void changePlugin(Runnable navigationChange)
    {
        // ClientToolbar enqueues first; PluginManager posts PluginChanged before returning to EDT.
        SwingUtilities.invokeLater(() ->
        {
            try
            {
                navigationChange.run();
            }
            catch (Throwable failure)
            {
                queuedFailure.compareAndSet(null, failure);
            }
        });
        manager.onPluginChanged();
    }

    private SidebarItem item(String name)
    {
        for (SidebarItem item : manager.getItems())
        {
            if (name.equals(item.getName()))
            {
                return item;
            }
        }
        return null;
    }

    private int countTabs(String name)
    {
        int count = 0;
        for (String visibleName : visibleNames())
        {
            if (name.equals(visibleName))
            {
                count++;
            }
        }
        return count;
    }

    private List<String> visibleNames()
    {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < sidebar.getTabCount(); index++)
        {
            names.add(sidebar.getToolTipTextAt(index));
        }
        return names;
    }

    private void drainEdt() throws Exception
    {
        // Drain navigation changes, deferred refreshes, then queued layout callbacks. No sleeps.
        for (int pass = 0; pass < 5; pass++)
        {
            onEdt(() -> { });
        }
        Throwable failure = queuedFailure.getAndSet(null);
        if (failure != null)
        {
            throw new AssertionError("Queued navigation change failed", failure);
        }
    }

    private void onEdt(Runnable action) throws Exception
    {
        SwingUtilities.invokeAndWait(action);
    }
}
