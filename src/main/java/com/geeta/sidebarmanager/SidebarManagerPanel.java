package com.geeta.sidebarmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;
import java.awt.Color;
import javax.swing.JSeparator;

public class SidebarManagerPanel extends PluginPanel
{
    private final SidebarManager sidebarManager;

    private final JPanel itemPanel =
            new JPanel();

    public SidebarManagerPanel(
            SidebarManager sidebarManager)
    {
        this.sidebarManager =
                sidebarManager;

        setLayout(
                new BorderLayout()
        );

        JLabel title =
                new JLabel(
                        "Sidebar Items",
                        SwingConstants.CENTER
                );

        title.setPreferredSize(
                new Dimension(
                        0,
                        32
                )
        );

        itemPanel.setLayout(
                new BoxLayout(
                        itemPanel,
                        BoxLayout.Y_AXIS
                )
        );

        JScrollPane scrollPane =
                new JScrollPane(
                        itemPanel
                );

        scrollPane.setBorder(null);

        JButton showAllButton = new JButton("Show All");
        showAllButton.addActionListener(event -> sidebarManager.showAllItems());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.CENTER);
        header.add(showAllButton, BorderLayout.EAST);

        JPanel headerContainer = new JPanel(new BorderLayout());
        headerContainer.add(header, BorderLayout.CENTER);
        headerContainer.add(new JSeparator(), BorderLayout.SOUTH);

        add(headerContainer, BorderLayout.NORTH);

        add(
                scrollPane,
                BorderLayout.CENTER
        );
    }

    public void refresh()
    {
        itemPanel.removeAll();

        List<SidebarItem> items =
                sidebarManager.getItems();

        for (SidebarItem item : items)
        {
            addItem(item);
        }

        itemPanel.revalidate();
        itemPanel.repaint();
    }

    private void addItem(
            SidebarItem item)
    {
        JPanel row =
                new JPanel(
                        new BorderLayout(
                                8,
                                0
                        )
                );

        row.setBorder(
                BorderFactory.createEmptyBorder(
                        4,
                        6,
                        4,
                        6
                )
        );

        boolean visible = sidebarManager.isVisible(item);

        JLabel name = new JLabel(item.getName());

        if (!visible)
        {
            name.setForeground(Color.RED);
        }

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

            row.add(
                    button,
                    BorderLayout.EAST
            );
        }

        itemPanel.add(row);
    }
}