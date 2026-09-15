package com.geeta.sidebarmanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import net.runelite.client.ui.PluginPanel;

public class SidebarManagerPanel extends PluginPanel
{
    private static final int SCROLL_MARGIN = 40;
    private static final int SCROLL_AMOUNT = 12;

    private final SidebarManager sidebarManager;
    private final JPanel itemPanel = new JPanel();
    private final Timer scrollTimer;

    private SidebarItem draggedItem;
    private JScrollPane outerScrollPane;
    private int scrollDirection;

    public SidebarManagerPanel(SidebarManager sidebarManager)
    {
        this.sidebarManager = sidebarManager;

        setLayout(new BorderLayout());

        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.Y_AXIS));

        scrollTimer = new Timer(40, event -> autoScroll());

        JButton showAllButton = new JButton("Show All");
        showAllButton.addActionListener(event -> sidebarManager.showAllItems());

        JButton resetOrderButton = new JButton("Reset Order");
        resetOrderButton.addActionListener(event -> sidebarManager.resetItemOrder());

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        headerButtons.add(showAllButton);
        headerButtons.add(resetOrderButton);

        JPanel header = new JPanel(new BorderLayout());
        header.add(headerButtons, BorderLayout.CENTER);

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.add(header, BorderLayout.CENTER);
        headerContainer.add(new JSeparator(), BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);
        add(itemPanel, BorderLayout.CENTER);
    }

    public void refresh()
    {
        itemPanel.removeAll();

        List<SidebarItem> items = sidebarManager.getItems();

        for (SidebarItem item : items)
        {
            addItem(item);
        }

        itemPanel.revalidate();
        itemPanel.repaint();
    }

    private void addItem(SidebarItem item)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.putClientProperty("sidebarItem", item);
        row.setTransferHandler(createTransferHandler(item));
        setNormalBorder(row);

        boolean visible = sidebarManager.isVisible(item);

        JLabel dragHandle = new JLabel("☰");
        dragHandle.setToolTipText("Drag to reorder");

        dragHandle.addMouseMotionListener(new java.awt.event.MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent event)
            {
                findOuterScrollPane();
                row.getTransferHandler().exportAsDrag(row, event, TransferHandler.MOVE);
            }
        });

        JLabel name = new JLabel(item.getName());

        if (!visible)
        {
            name.setForeground(Color.RED);
        }

        row.add(dragHandle, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);

        if (!sidebarManager.isManagerItem(item))
        {
            JButton button = new JButton(visible ? "Hide" : "Show");

            button.addActionListener(event ->
            {
                if (sidebarManager.isVisible(item))
                {
                    sidebarManager.hideItem(item);
                }
                else
                {
                    sidebarManager.showItem(item);
                }

                refresh();
            });

            row.add(button, BorderLayout.EAST);
        }

        itemPanel.add(row);
    }

    private TransferHandler createTransferHandler(SidebarItem item)
    {
        return new TransferHandler()
        {
            @Override
            protected java.awt.datatransfer.Transferable createTransferable(JComponent component)
            {
                draggedItem = item;
                return new StringSelection(item.getName());
            }

            @Override
            public int getSourceActions(JComponent component)
            {
                return MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support)
            {
                if (draggedItem == null || !support.isDrop())
                {
                    stopAutoScroll();
                    clearDropIndicators();
                    return false;
                }

                updateAutoScroll(support);

                Component component = support.getComponent();

                if (!(component instanceof JPanel))
                {
                    clearDropIndicators();
                    return false;
                }

                JPanel targetRow = (JPanel) component;
                Object value = targetRow.getClientProperty("sidebarItem");

                if (!(value instanceof SidebarItem))
                {
                    clearDropIndicators();
                    return false;
                }

                SidebarItem targetItem = (SidebarItem) value;

                if (targetItem == draggedItem)
                {
                    clearDropIndicators();
                    return false;
                }

                Point point = support.getDropLocation().getDropPoint();
                boolean insertBefore = point.y < targetRow.getHeight() / 2;

                showDropIndicator(targetRow, insertBefore);
                return true;
            }

            @Override
            public boolean importData(TransferSupport support)
            {
                if (!canImport(support))
                {
                    stopAutoScroll();
                    clearDropIndicators();
                    return false;
                }

                JPanel targetRow = (JPanel) support.getComponent();
                SidebarItem targetItem = (SidebarItem) targetRow.getClientProperty("sidebarItem");

                List<SidebarItem> items = sidebarManager.getItems();
                int draggedIndex = items.indexOf(draggedItem);
                int targetIndex = items.indexOf(targetItem);

                Point point = support.getDropLocation().getDropPoint();
                boolean insertBefore = point.y < targetRow.getHeight() / 2;

                if (!insertBefore)
                {
                    targetIndex++;
                }

                if (draggedIndex < targetIndex)
                {
                    targetIndex--;
                }

                targetIndex = Math.max(0, Math.min(targetIndex, items.size() - 1));

                SidebarItem itemToMove = draggedItem;
                draggedItem = null;

                stopAutoScroll();
                clearDropIndicators();

                sidebarManager.moveItem(itemToMove, targetIndex);
                return true;
            }

            @Override
            protected void exportDone(JComponent source, java.awt.datatransfer.Transferable data, int action)
            {
                draggedItem = null;
                stopAutoScroll();
                clearDropIndicators();
            }
        };
    }

    private void findOuterScrollPane()
    {
        if (outerScrollPane != null)
        {
            return;
        }

        Container parent = getParent();

        while (parent != null)
        {
            if (parent instanceof JScrollPane)
            {
                outerScrollPane = (JScrollPane) parent;
                return;
            }

            parent = parent.getParent();
        }
    }

    private void updateAutoScroll(TransferHandler.TransferSupport support)
    {
        findOuterScrollPane();

        if (outerScrollPane == null)
        {
            return;
        }

        Component component = support.getComponent();
        Point point = support.getDropLocation().getDropPoint();
        Point viewportPoint = SwingUtilities.convertPoint(component, point, outerScrollPane.getViewport());

        int viewportHeight = outerScrollPane.getViewport().getHeight();

        if (viewportPoint.y < SCROLL_MARGIN)
        {
            startAutoScroll(-1);
        }
        else if (viewportPoint.y > viewportHeight - SCROLL_MARGIN)
        {
            startAutoScroll(1);
        }
        else
        {
            stopAutoScroll();
        }
    }

    private void startAutoScroll(int direction)
    {
        scrollDirection = direction;

        if (!scrollTimer.isRunning())
        {
            scrollTimer.start();
        }
    }

    private void stopAutoScroll()
    {
        scrollDirection = 0;

        if (scrollTimer.isRunning())
        {
            scrollTimer.stop();
        }
    }

    private void autoScroll()
    {
        if (outerScrollPane == null || scrollDirection == 0)
        {
            return;
        }

        javax.swing.JScrollBar scrollBar = outerScrollPane.getVerticalScrollBar();
        int oldValue = scrollBar.getValue();
        int newValue = oldValue + scrollDirection * SCROLL_AMOUNT;

        newValue = Math.max(scrollBar.getMinimum(), Math.min(newValue, scrollBar.getMaximum() - scrollBar.getVisibleAmount()));
        scrollBar.setValue(newValue);

        if (scrollBar.getValue() == oldValue)
        {
            stopAutoScroll();
        }
    }

    private void showDropIndicator(JPanel targetRow, boolean insertBefore)
    {
        clearDropIndicators();

        Color lineColor = getForeground();

        Border padding = BorderFactory.createEmptyBorder(insertBefore ? 2 : 4, 6, insertBefore ? 4 : 2, 6);
        Border line = BorderFactory.createMatteBorder(insertBefore ? 2 : 0, 0, insertBefore ? 0 : 2, 0, lineColor);

        targetRow.setBorder(BorderFactory.createCompoundBorder(line, padding));
        targetRow.repaint();
    }

    private void clearDropIndicators()
    {
        for (Component component : itemPanel.getComponents())
        {
            if (component instanceof JPanel)
            {
                JPanel row = (JPanel) component;
                setNormalBorder(row);
            }
        }

        itemPanel.repaint();
    }

    private void setNormalBorder(JPanel row)
    {
        row.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
    }
}