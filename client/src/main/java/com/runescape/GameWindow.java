package com.runescape;

import com.runescape.util.SystemUtils;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.ColorConvertOp;
import java.awt.color.ColorSpace;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * I fear the day technology will surpass our human interaction. The world will have a generation of idiots. -Albert Einstein
 * Date: 6/5/2015
 * Time: 1:58 PM
 *
 * @author Galkon
 */
public class GameWindow extends JFrame implements ActionListener {

    private Color RL_DARKER = new Color(16, 8, 10);
    private Color RL_DARK = new Color(28, 11, 15);
    private Color RL_DARKER_HOVER = new Color(56, 20, 29);
    private Color RL_TAB_ACTIVE = new Color(80, 28, 40);
    private Color RL_TEXT = new Color(224, 204, 210);
    private Color RL_TEXT_SUBTLE = new Color(165, 136, 145);
    private Color RL_BORDER = new Color(10, 4, 6);
    private Color RL_GLOSS = new Color(255, 255, 255, 20);
    private static final int RL_PLUGIN_PANEL_WIDTH = 225;
    private static final int RL_SCROLLBAR_WIDTH = 17;
    private static final int RL_PANEL_PADDING = 6;
    private static final int RL_TAB_HEIGHT = 26;
    private static final int RL_SIDEBAR_CLOSED_WIDTH = 34;
    private static final int RL_SIDEBAR_CONTENT_WIDTH = RL_PLUGIN_PANEL_WIDTH + RL_SCROLLBAR_WIDTH;
    private static final int RL_SIDEBAR_OPEN_WIDTH = RL_SIDEBAR_CONTENT_WIDTH + RL_SIDEBAR_CLOSED_WIDTH;

    private static GameWindow instance;
    private final Applet appletInstance;

    private JPanel rlSidebarContainer;
    private JTabbedPane rlSidebarTabs;
    private JButton rlSidebarToggleButton;
    private boolean rlSidebarOpen = true;
    private int rlSidebarLastSelectedTab = 0;
    private int rlSidebarHoverTab = -1;
    private ImageIcon rlSidebarOpenIcon;
    private ImageIcon rlSidebarCloseIcon;
    private ImageIcon rlPluginsTabIcon;
    private ImageIcon rlConfigIcon;
    private ImageIcon rlBackIcon;
    private ImageIcon rlSwitcherOnIcon;
    private ImageIcon rlSwitcherOffIcon;
    private ImageIcon rlThemesTabIcon;
    private String currentTheme = "Crimson";

    public GameWindow(Applet applet) {
        if (!SystemUtils.isMac()) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        this.setTitle(Configuration.CLIENT_NAME);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setFocusTraversalKeysEnabled(false);
        this.getContentPane().setBackground(Color.BLACK);
        this.getContentPane().setLayout(new BorderLayout());

        appletInstance = applet;
        appletInstance.init();
        appletInstance.setMinimumSize(new Dimension(765, 503));
        appletInstance.setPreferredSize(((Client) appletInstance).frameDimension());
        this.getContentPane().add(appletInstance, BorderLayout.CENTER);

        if (appletInstance instanceof Client) {
            this.getContentPane().add(createRuneLiteSidebar((Client) appletInstance), BorderLayout.EAST);
        }

        this.pack();

        Insets insets = this.getInsets();
        int minWidth = appletInstance.getMinimumSize().width + RL_SIDEBAR_CLOSED_WIDTH + insets.left + insets.right;
        int minHeight = appletInstance.getMinimumSize().height + insets.top + insets.bottom;
        this.setMinimumSize(new Dimension(minWidth, minHeight));

        this.setVisible(true);
        this.requestFocus();
        this.toFront();

        this.setLocationRelativeTo(null);

        setInstance(this);
    }

    public static GameWindow getInstance() {
        return instance;
    }

    public static void setInstance(GameWindow instance) {
        GameWindow.instance = instance;
    }

    public static void main(String[] args) {
        final Client client = new Client();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameWindow(client);
            }
        });
    }

    public void exit() {
        int confirm = JOptionPane.showOptionDialog(null, "Are you sure you want to exit", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (confirm == JOptionPane.YES_OPTION) {
            setTitle("Please wait, the client is closing...");
            System.exit(0);
        }
    }

    public int getFrameWidth() {
        Insets insets = this.getInsets();
        return getWidth() - (insets.left + insets.right);
    }

    public int getFrameHeight() {
        Insets insets = this.getInsets();
        return getHeight() - (insets.top + insets.bottom);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd == null) {
            return;
        }
    }

    private JPanel createRuneLiteSidebar(Client client) {
        applyTheme(currentTheme);
        rlSidebarOpenIcon = loadIcon("/com/runescape/ui/open_rs.png", 16, 16);
        rlSidebarCloseIcon = flipHorizontal(rlSidebarOpenIcon);
        rlPluginsTabIcon = loadIcon("/com/runescape/ui/plugins_tab.png", 16, 16);
        rlThemesTabIcon = loadIcon("/com/runescape/ui/config_icon.png", 16, 16);
        rlConfigIcon = loadIcon("/com/runescape/ui/config_edit_icon.png", 14, 14);
        rlBackIcon = loadIcon("/com/runescape/ui/config_back_icon.png", 14, 14);
        rlSwitcherOnIcon = loadIcon("/com/runescape/ui/switcher_on.png", 28, 14);
        rlSwitcherOffIcon = createSwitcherOffIcon(rlSwitcherOnIcon);

        JPanel sidebar = createGradientPanel(new BorderLayout(), lighten(RL_DARK, 8), darken(RL_DARKER, 4));
        sidebar.setBackground(RL_DARKER);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, RL_BORDER));
        sidebar.setPreferredSize(new Dimension(RL_SIDEBAR_OPEN_WIDTH, 0));
        rlSidebarContainer = sidebar;

        rlSidebarTabs = new JTabbedPane(JTabbedPane.RIGHT);
        rlSidebarTabs.setOpaque(true);
        rlSidebarTabs.setBackground(RL_DARKER);
        rlSidebarTabs.setBorder(BorderFactory.createEmptyBorder());
        rlSidebarTabs.setUI(new RuneLiteTabbedPaneUI());
        rlSidebarTabs.addTab("", rlPluginsTabIcon, wrapPluginPanel(createPluginsHubPanel(client)), "Plugins");
        rlSidebarTabs.addTab("", rlThemesTabIcon, wrapPluginPanel(createThemePanel(client)), "Themes");
        rlSidebarTabs.setSelectedIndex(0);
        rlSidebarTabs.addChangeListener(_e -> {
            int selected = rlSidebarTabs.getSelectedIndex();
            if (selected >= 0) {
                rlSidebarLastSelectedTab = selected;
                if (!rlSidebarOpen) {
                    setSidebarOpen(true);
                }
            }
            rlSidebarTabs.repaint();
        });
        rlSidebarTabs.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int tab = rlSidebarTabs.indexAtLocation(e.getX(), e.getY());
                if (tab != rlSidebarHoverTab) {
                    rlSidebarHoverTab = tab;
                    rlSidebarTabs.repaint();
                }
            }
        });
        rlSidebarTabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (rlSidebarHoverTab != -1) {
                    rlSidebarHoverTab = -1;
                    rlSidebarTabs.repaint();
                }
            }
        });

        JPanel toggleWrap = createGradientPanel(new BorderLayout(), lighten(RL_DARK, 10), darken(RL_DARKER, 2));
        toggleWrap.setLayout(new BoxLayout(toggleWrap, BoxLayout.X_AXIS));
        toggleWrap.setBackground(RL_DARKER);
        toggleWrap.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, RL_BORDER));
        toggleWrap.add(Box.createHorizontalGlue());
        rlSidebarToggleButton = new JButton(rlSidebarCloseIcon);
        rlSidebarToggleButton.setToolTipText("Close sidebar");
        styleToggleButton(rlSidebarToggleButton);
        rlSidebarToggleButton.addActionListener(_e -> setSidebarOpen(!rlSidebarOpen));
        toggleWrap.add(rlSidebarToggleButton);
        toggleWrap.add(Box.createRigidArea(new Dimension(4, 0)));

        sidebar.add(toggleWrap, BorderLayout.NORTH);
        sidebar.add(rlSidebarTabs, BorderLayout.CENTER);
        return sidebar;
    }

    private void setSidebarOpen(boolean open) {
        if (rlSidebarContainer == null || rlSidebarTabs == null || rlSidebarOpen == open) {
            return;
        }

        rlSidebarOpen = open;
        if (open) {
            rlSidebarTabs.setSelectedIndex(Math.max(0, Math.min(rlSidebarLastSelectedTab, rlSidebarTabs.getTabCount() - 1)));
            rlSidebarContainer.setPreferredSize(new Dimension(RL_SIDEBAR_OPEN_WIDTH, 0));
        } else {
            if (rlSidebarTabs.getSelectedIndex() >= 0) {
                rlSidebarLastSelectedTab = rlSidebarTabs.getSelectedIndex();
            }
            rlSidebarTabs.setSelectedIndex(-1);
            rlSidebarContainer.setPreferredSize(new Dimension(RL_SIDEBAR_CLOSED_WIDTH, 0));
        }

        int deltaWidth = open ? (RL_SIDEBAR_OPEN_WIDTH - RL_SIDEBAR_CLOSED_WIDTH) : -(RL_SIDEBAR_OPEN_WIDTH - RL_SIDEBAR_CLOSED_WIDTH);
        setSize(getWidth() + deltaWidth, getHeight());

        if (rlSidebarToggleButton != null) {
            rlSidebarToggleButton.setIcon(open ? rlSidebarCloseIcon : rlSidebarOpenIcon);
            rlSidebarToggleButton.setToolTipText(open ? "Close sidebar" : "Open sidebar");
        }

        rlSidebarContainer.revalidate();
        rlSidebarContainer.repaint();
    }

    private void rebuildSidebar() {
        if (!(appletInstance instanceof Client)) {
            return;
        }

        java.awt.Container content = getContentPane();
        if (rlSidebarContainer != null) {
            content.remove(rlSidebarContainer);
        }
        rlSidebarContainer = createRuneLiteSidebar((Client) appletInstance);
        content.add(rlSidebarContainer, BorderLayout.EAST);
        content.revalidate();
        content.repaint();
    }

    private JPanel createTileIndicatorsPanel(Client client) {
        JPanel panel = createPluginPanel("Tile Indicators", "Highlights destination and current tiles");

        JCheckBox enable = createCheckBox("Enable plugin", client.isTileIndicatorsPluginEnabled(),
                v -> client.setTileIndicatorsPluginEnabled(v));
        JCheckBox destination = createCheckBox("Destination tile", client.isTileIndicatorDestinationEnabled(),
                v -> client.setTileIndicatorDestinationEnabled(v));
        JCheckBox current = createCheckBox("Current tile", client.isTileIndicatorCurrentEnabled(),
                v -> client.setTileIndicatorCurrentEnabled(v));

        JButton destinationColor = createColorButton("Destination color", client.getTileIndicatorDestinationColor(),
                color -> client.setTileIndicatorDestinationColor(color));
        JButton currentColor = createColorButton("Current color", client.getTileIndicatorCurrentColor(),
                color -> client.setTileIndicatorCurrentColor(color));

        panel.add(enable);
        panel.add(destination);
        panel.add(current);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(destinationColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(currentColor);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createPluginsHubPanel(Client client) {
        CardLayout cards = new CardLayout();
        JPanel root = createGradientPanel(cards, lighten(RL_DARK, 6), darken(RL_DARKER, 2));
        root.setBackground(RL_DARK);

        JPanel listPanel = createPluginPanel("Plugins", "Manage plugin toggles and settings");
        JPanel settingsPanel = createPluginSettingsContainer();

        listPanel.add(createSectionLabel("Combat & Stats"));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Ammo",
                client::isAmmoPluginEnabled,
                client::setAmmoPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Ammo", createAmmoPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Boosts Information",
                client::isBoostsInfoPluginEnabled,
                client::setBoostsInfoPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Boosts Information", createBoostsInformationPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Boss Timers",
                client::isBossTimersPluginEnabled,
                client::setBossTimersPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Boss Timers", createBossTimersPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Item Stats",
                client::isItemStatsPluginEnabled,
                client::setItemStatsPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Item Stats", createItemStatsPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "NPC Aggression Timer",
                client::isNpcAggressionTimerPluginEnabled,
                client::setNpcAggressionTimerPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "NPC Aggression Timer", createNpcAggressionPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "NPC Indicators",
                client::isNpcIndicatorsPluginEnabled,
                client::setNpcIndicatorsPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "NPC Indicators", createNpcIndicatorsPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "XP Drops",
                client::isXpDropsPluginEnabled,
                client::setXpDropsPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "XP Drops", createXpDropsPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "XP Tracker",
                client::isXpTrackerPluginEnabled,
                client::setXpTrackerPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "XP Tracker", createXpTrackerPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        listPanel.add(createSectionLabel("Interface & Inventory"));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Idle Notifier",
                client::isIdleNotifierPluginEnabled,
                client::setIdleNotifierPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Idle Notifier", createIdleNotifierPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Inventory Tags",
                client::isInventoryTagsPluginEnabled,
                client::setInventoryTagsPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Inventory Tags", createInventoryTagsPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Item Charges",
                client::isItemChargesPluginEnabled,
                client::setItemChargesPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Item Charges", createItemChargesPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Loot Tracker",
                client::isLootTrackerPluginEnabled,
                client::setLootTrackerPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Loot Tracker", createLootTrackerPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Timers",
                client::isTimersPluginEnabled,
                client::setTimersPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Timers", createTimersPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        listPanel.add(createSectionLabel("Existing Custom"));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Tile Indicators",
                client::isTileIndicatorsPluginEnabled,
                client::setTileIndicatorsPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Tile Indicators", createTileIndicatorsPanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Visual Metronome",
                client::isVisualMetronomePluginEnabled,
                client::setVisualMetronomePluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Visual Metronome", createVisualMetronomePanel(client))));
        listPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        listPanel.add(createPluginListItem(
                "Shortest Path",
                client::isShortestPathPluginEnabled,
                client::setShortestPathPluginEnabled,
                () -> openPluginSettings(cards, root, settingsPanel, "Shortest Path", createShortestPathPanel(client))));
        listPanel.add(Box.createVerticalGlue());

        root.add(listPanel, "list");
        root.add(settingsPanel, "settings");
        cards.show(root, "list");
        return root;
    }

    private JPanel createThemePanel(Client client) {
        JPanel panel = createPluginPanel("Themes", "Quick sidebar color presets");
        panel.add(createThemeButton("Forest Green / Black", "Forest"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(createThemeButton("Yellow / Black", "Yellow"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(createThemeButton("Cyan / Black", "Cyan"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(createThemeButton("Indigo / Black", "Indigo"));
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(createThemeButton("Orange / Black", "Orange"));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(lighten(RL_TEXT, 18));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        return label;
    }

    private JPanel createAmmoPanel(Client client) {
        JPanel panel = createPluginPanel("Ammo", "Shows equipped ammo count with low warning");
        JCheckBox enable = createCheckBox("Enable plugin", client.isAmmoPluginEnabled(), client::setAmmoPluginEnabled);
        JLabel thresholdLabel = createSubtleLabel("Low ammo warning threshold");
        JSpinner threshold = new JSpinner(new SpinnerNumberModel(client.getAmmoLowWarningThreshold(), 1, 5000, 1));
        styleSpinner(threshold);
        threshold.addChangeListener(_e -> client.setAmmoLowWarningThreshold((Integer) threshold.getValue()));
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(thresholdLabel);
        panel.add(threshold);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createBoostsInformationPanel(Client client) {
        JPanel panel = createPluginPanel("Boosts Information", "Displays current combat boosts and drains");
        JCheckBox enable = createCheckBox("Enable plugin", client.isBoostsInfoPluginEnabled(), client::setBoostsInfoPluginEnabled);
        JCheckBox showCombat = createCheckBox("Show combat skills", client.isBoostsShowCombatSkills(), client::setBoostsShowCombatSkills);
        JCheckBox showNonCombat = createCheckBox("Show non-combat skills", client.isBoostsShowNonCombatSkills(), client::setBoostsShowNonCombatSkills);
        JCheckBox relative = createCheckBox("Use relative boosts", client.isBoostsUseRelativeBoosts(), client::setBoostsUseRelativeBoosts);
        JCheckBox notifyDrain = createCheckBox("Notify on drain threshold", client.isBoostsNotifyOnThreshold(), client::setBoostsNotifyOnThreshold);
        JLabel thresholdLabel = createSubtleLabel("Drain threshold");
        JSpinner threshold = new JSpinner(new SpinnerNumberModel(client.getBoostsThreshold(), 0, 50, 1));
        styleSpinner(threshold);
        threshold.addChangeListener(_e -> client.setBoostsThreshold((Integer) threshold.getValue()));
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(showCombat);
        panel.add(showNonCombat);
        panel.add(relative);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(notifyDrain);
        panel.add(thresholdLabel);
        panel.add(threshold);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createBossTimersPanel(Client client) {
        JPanel panel = createPluginPanel("Boss Timers", "Tracks boss respawn timers on kill");
        JCheckBox enable = createCheckBox("Enable plugin", client.isBossTimersPluginEnabled(), client::setBossTimersPluginEnabled);
        JLabel limitLabel = createSubtleLabel("Overlay rows to show");
        JSpinner limit = new JSpinner(new SpinnerNumberModel(client.getBossTimersOverlayLimit(), 1, 10, 1));
        styleSpinner(limit);
        limit.addChangeListener(_e -> client.setBossTimersOverlayLimit((Integer) limit.getValue()));
        JButton clear = createActionButton("Clear timers");
        JLabel status = createSubtleLabel("");
        Runnable refreshState = () -> {
            List<String> entries = client.getBossTimersSummary();
            status.setText(entries.isEmpty() ? "No active boss timers" : entries.get(0));
        };
        clear.addActionListener(_e -> {
            client.clearBossTimers();
            refreshState.run();
        });

        panel.add(enable);
        panel.add(createSubtleLabel("Uses RuneLite boss-name timing map."));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(limitLabel);
        panel.add(limit);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(clear);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(status);
        panel.add(Box.createVerticalGlue());
        refreshState.run();
        return panel;
    }

    private JPanel createItemStatsPanel(Client client) {
        JPanel panel = createPluginPanel("Item Stats", "Shows quick item stat hints on inventory hover");
        JCheckBox enable = createCheckBox("Enable plugin", client.isItemStatsPluginEnabled(), client::setItemStatsPluginEnabled);
        JCheckBox showId = createCheckBox("Show item ID", client.isItemStatsShowItemId(), client::setItemStatsShowItemId);
        JCheckBox showValue = createCheckBox("Show item value", client.isItemStatsShowItemValue(), client::setItemStatsShowItemValue);
        JCheckBox showActions = createCheckBox("Show primary action", client.isItemStatsShowActions(), client::setItemStatsShowActions);
        JCheckBox showGroundActions = createCheckBox("Show ground action", client.isItemStatsShowGroundActions(), client::setItemStatsShowGroundActions);
        JCheckBox showMembership = createCheckBox("Show members/team info", client.isItemStatsShowMembershipInfo(), client::setItemStatsShowMembershipInfo);
        JCheckBox showNoted = createCheckBox("Show note info", client.isItemStatsShowNoteInfo(), client::setItemStatsShowNoteInfo);
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(showId);
        panel.add(showValue);
        panel.add(showActions);
        panel.add(showGroundActions);
        panel.add(showMembership);
        panel.add(showNoted);
        panel.add(createSubtleLabel("Displays stackable/equipable info in a tooltip."));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createNpcAggressionPanel(Client client) {
        JPanel panel = createPluginPanel("NPC Aggression Timer", "10-tile style aggro timer helper");
        JCheckBox enable = createCheckBox("Enable plugin", client.isNpcAggressionTimerPluginEnabled(), client::setNpcAggressionTimerPluginEnabled);
        JLabel durationLabel = createSubtleLabel("Aggro duration (seconds)");
        JSpinner duration = new JSpinner(new SpinnerNumberModel(client.getNpcAggressionDurationSeconds(), 10, 3600, 10));
        styleSpinner(duration);
        duration.addChangeListener(_e -> client.setNpcAggressionDurationSeconds((Integer) duration.getValue()));
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(durationLabel);
        panel.add(duration);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createNpcIndicatorsPanel(Client client) {
        JPanel panel = createPluginPanel("NPC Indicators", "Highlights matching NPC names");
        JCheckBox enable = createCheckBox("Enable plugin", client.isNpcIndicatorsPluginEnabled(), client::setNpcIndicatorsPluginEnabled);
        JCheckBox highlightTile = createCheckBox("Highlight tiles", client.isNpcIndicatorsHighlightTile(), client::setNpcIndicatorsHighlightTile);
        JCheckBox highlightName = createCheckBox("Draw names", client.isNpcIndicatorsHighlightName(), client::setNpcIndicatorsHighlightName);
        JCheckBox highlightMinimap = createCheckBox("Mark minimap dots", client.isNpcIndicatorsHighlightMinimap(), client::setNpcIndicatorsHighlightMinimap);
        JCheckBox ignoreDead = createCheckBox("Ignore dead NPCs", client.isNpcIndicatorsIgnoreDead(), client::setNpcIndicatorsIgnoreDead);
        JCheckBox caseSensitive = createCheckBox("Case-sensitive matching", client.isNpcIndicatorsCaseSensitive(), client::setNpcIndicatorsCaseSensitive);
        JButton matchMode = createActionButton("Match mode: " + npcMatchModeLabel(client.getNpcIndicatorsMatchMode()));
        matchMode.addActionListener(_e -> {
            int next = (client.getNpcIndicatorsMatchMode() + 1) % 3;
            client.setNpcIndicatorsMatchMode(next);
            matchMode.setText("Match mode: " + npcMatchModeLabel(next));
        });
        JButton indicatorColor = createColorButton("Highlight color", client.getNpcIndicatorsColor(), client::setNpcIndicatorsColor);
        JButton setTargets = createActionButton("Set target NPC names");
        setTargets.addActionListener(_e -> {
            String value = JOptionPane.showInputDialog(this, "Comma-separated NPC names", client.getNpcIndicatorTargetsCsv());
            if (value != null) {
                client.setNpcIndicatorTargetsCsv(value);
            }
        });
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(highlightTile);
        panel.add(highlightName);
        panel.add(highlightMinimap);
        panel.add(ignoreDead);
        panel.add(caseSensitive);
        panel.add(matchMode);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(indicatorColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(setTargets);
        panel.add(createSubtleLabel("Example: abyssal demon, bloodveld"));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createXpDropsPanel(Client client) {
        JPanel panel = createPluginPanel("XP Drops", "Enables floating XP drops");
        JCheckBox enable = createCheckBox("Enable plugin", client.isXpDropsPluginEnabled(), client::setXpDropsPluginEnabled);
        panel.add(enable);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createXpTrackerPanel(Client client) {
        JPanel panel = createPluginPanel("XP Tracker", "Shows session XP and XP/hour");
        JCheckBox enable = createCheckBox("Enable plugin", client.isXpTrackerPluginEnabled(), client::setXpTrackerPluginEnabled);
        JCheckBox pauseLogout = createCheckBox("Pause on logout", client.isXpTrackerPauseOnLogout(), client::setXpTrackerPauseOnLogout);
        JLabel autoPauseLabel = createSubtleLabel("Auto pause after idle (minutes, 0 = disabled)");
        JSpinner autoPause = new JSpinner(new SpinnerNumberModel(client.getXpTrackerAutoPauseMinutes(), 0, 120, 1));
        styleSpinner(autoPause);
        autoPause.addChangeListener(_e -> client.setXpTrackerAutoPauseMinutes((Integer) autoPause.getValue()));
        JLabel summary = createSubtleLabel(client.getXpTrackerSessionSummary());
        JButton refresh = createActionButton("Refresh session stats");
        refresh.addActionListener(_e -> summary.setText(client.getXpTrackerSessionSummary()));
        JButton reset = createActionButton("Reset session");
        reset.addActionListener(_e -> {
            client.resetXpTrackerSession();
            summary.setText(client.getXpTrackerSessionSummary());
        });

        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(pauseLogout);
        panel.add(autoPauseLabel);
        panel.add(autoPause);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(refresh);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(reset);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(summary);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createIdleNotifierPanel(Client client) {
        JPanel panel = createPluginPanel("Idle Notifier", "Notifies when player has been idle");
        JCheckBox enable = createCheckBox("Enable plugin", client.isIdleNotifierPluginEnabled(), client::setIdleNotifierPluginEnabled);
        JLabel thresholdLabel = createSubtleLabel("Idle threshold (seconds)");
        JSpinner threshold = new JSpinner(new SpinnerNumberModel(client.getIdleNotifierThresholdSeconds(), 5, 1800, 5));
        styleSpinner(threshold);
        threshold.addChangeListener(_e -> client.setIdleNotifierThresholdSeconds((Integer) threshold.getValue()));
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(thresholdLabel);
        panel.add(threshold);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createInventoryTagsPanel(Client client) {
        JPanel panel = createPluginPanel("Inventory Tags", "Highlights selected inventory item IDs");
        JCheckBox enable = createCheckBox("Enable plugin", client.isInventoryTagsPluginEnabled(), client::setInventoryTagsPluginEnabled);
        JCheckBox caseSensitive = createCheckBox("Case-sensitive name tags", client.isInventoryTagsCaseSensitive(), client::setInventoryTagsCaseSensitive);
        JButton matchMode = createActionButton("Name match mode: " + inventoryMatchModeLabel(client.getInventoryTagsNameMatchMode()));
        matchMode.addActionListener(_e -> {
            int next = (client.getInventoryTagsNameMatchMode() + 1) % 3;
            client.setInventoryTagsNameMatchMode(next);
            matchMode.setText("Name match mode: " + inventoryMatchModeLabel(next));
        });
        JLabel groupLabel = createSubtleLabel("Active tag group (1-4)");
        JSpinner group = new JSpinner(new SpinnerNumberModel(client.getInventoryTagsSelectedGroup() + 1, 1, 4, 1));
        styleSpinner(group);
        Runnable syncGroup = () -> client.setInventoryTagsSelectedGroup((Integer) group.getValue() - 1);
        group.addChangeListener(_e -> syncGroup.run());
        JButton tagColor = createActionButton("Set active group color");
        tagColor.addActionListener(_e -> {
            syncGroup.run();
            int selected = client.getInventoryTagsSelectedGroup();
            Color chosen = JColorChooser.showDialog(this, "Choose tag color for group " + (selected + 1), client.getInventoryTagsGroupColor(selected));
            if (chosen != null) {
                client.setInventoryTagsGroupColor(selected, chosen);
            }
        });
        JButton setItems = createActionButton("Set active group item IDs");
        setItems.addActionListener(_e -> {
            syncGroup.run();
            int selected = client.getInventoryTagsSelectedGroup();
            String value = JOptionPane.showInputDialog(this, "Comma-separated item IDs", client.getInventoryTagsGroupItemIdsCsv(selected));
            if (value != null) {
                client.setInventoryTagsGroupItemIdsCsv(selected, value);
            }
        });
        JButton setNames = createActionButton("Set active group item names");
        setNames.addActionListener(_e -> {
            syncGroup.run();
            int selected = client.getInventoryTagsSelectedGroup();
            String value = JOptionPane.showInputDialog(this, "Comma-separated item names", client.getInventoryTagsGroupItemNamesCsv(selected));
            if (value != null) {
                client.setInventoryTagsGroupItemNamesCsv(selected, value);
            }
        });
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(groupLabel);
        panel.add(group);
        panel.add(caseSensitive);
        panel.add(matchMode);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(tagColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(setItems);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(setNames);
        panel.add(createSubtleLabel("Example: 11802, 12926, 995"));
        panel.add(createSubtleLabel("Name example: rune crossbow, amulet"));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private String npcMatchModeLabel(int mode) {
        switch (mode) {
            case 1:
                return "Exact";
            case 2:
                return "Starts With";
            default:
                return "Contains";
        }
    }

    private String inventoryMatchModeLabel(int mode) {
        switch (mode) {
            case 1:
                return "Exact";
            case 2:
                return "Starts With";
            default:
                return "Contains";
        }
    }

    private JPanel createItemChargesPanel(Client client) {
        JPanel panel = createPluginPanel("Item Charges", "Displays charge count from item names");
        JCheckBox enable = createCheckBox("Enable plugin", client.isItemChargesPluginEnabled(), client::setItemChargesPluginEnabled);
        JLabel lowThresholdLabel = createSubtleLabel("Low charge warning threshold");
        JSpinner lowThreshold = new JSpinner(new SpinnerNumberModel(client.getItemChargesLowWarningThreshold(), 0, 1000, 1));
        styleSpinner(lowThreshold);
        lowThreshold.addChangeListener(_e -> client.setItemChargesLowWarningThreshold((Integer) lowThreshold.getValue()));
        JCheckBox showZero = createCheckBox("Show zero charges", client.isItemChargesShowWhenZero(), client::setItemChargesShowWhenZero);
        JButton normalColor = createColorButton("Normal text color", client.getItemChargesTextColor(), client::setItemChargesTextColor);
        JButton lowColor = createColorButton("Low warning color", client.getItemChargesLowColor(), client::setItemChargesLowColor);
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(lowThresholdLabel);
        panel.add(lowThreshold);
        panel.add(showZero);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(normalColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(lowColor);
        panel.add(createSubtleLabel("Parses names ending with charge suffixes like (4), (3), etc."));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createLootTrackerPanel(Client client) {
        JPanel panel = createPluginPanel("Loot Tracker", "Tracks recent inventory gains");
        JCheckBox enable = createCheckBox("Enable plugin", client.isLootTrackerPluginEnabled(), client::setLootTrackerPluginEnabled);
        JLabel maxEntriesLabel = createSubtleLabel("Stored entries");
        JSpinner maxEntries = new JSpinner(new SpinnerNumberModel(client.getLootTrackerMaxEntries(), 1, 50, 1));
        styleSpinner(maxEntries);
        maxEntries.addChangeListener(_e -> client.setLootTrackerMaxEntries((Integer) maxEntries.getValue()));
        JCheckBox chatOutput = createCheckBox("Send loot to chat", client.isLootTrackerShowNpcKillChat(), client::setLootTrackerShowNpcKillChat);
        JLabel latest = createSubtleLabel("");
        Runnable refreshState = () -> {
            List<String> entries = client.getLootTrackerRecentEntries();
            latest.setText(entries.isEmpty() ? "No loot entries yet" : entries.get(0));
        };
        JButton refresh = createActionButton("Refresh entries");
        refresh.addActionListener(_e -> refreshState.run());
        JButton clear = createActionButton("Clear entries");
        clear.addActionListener(_e -> {
            client.clearLootTrackerEntries();
            refreshState.run();
        });

        panel.add(enable);
        panel.add(createSubtleLabel("Records positive inventory deltas as loot entries."));
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(maxEntriesLabel);
        panel.add(maxEntries);
        panel.add(chatOutput);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(refresh);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(clear);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(latest);
        panel.add(Box.createVerticalGlue());
        refreshState.run();
        return panel;
    }

    private JPanel createTimersPanel(Client client) {
        JPanel panel = createPluginPanel("Timers", "Enables server effect timer overlays");
        JCheckBox enable = createCheckBox("Enable plugin", client.isTimersPluginEnabled(), client::setTimersPluginEnabled);
        JCheckBox secondsOnly = createCheckBox("Show seconds only", client.isTimersShowSecondsOnly(), client::setTimersShowSecondsOnly);
        JCheckBox sortAscending = createCheckBox("Sort ascending time", client.isTimersSortAscending(), client::setTimersSortAscending);
        JCheckBox showSpriteId = createCheckBox("Show sprite id", client.isTimersShowSpriteId(), client::setTimersShowSpriteId);
        JLabel minSecondsLabel = createSubtleLabel("Minimum seconds to show");
        JSpinner minSeconds = new JSpinner(new SpinnerNumberModel(client.getTimersMinSeconds(), 0, 3600, 1));
        styleSpinner(minSeconds);
        minSeconds.addChangeListener(_e -> client.setTimersMinSeconds((Integer) minSeconds.getValue()));
        JLabel maxVisibleLabel = createSubtleLabel("Maximum visible timers");
        JSpinner maxVisible = new JSpinner(new SpinnerNumberModel(client.getTimersMaxVisible(), 1, 20, 1));
        styleSpinner(maxVisible);
        maxVisible.addChangeListener(_e -> client.setTimersMaxVisible((Integer) maxVisible.getValue()));
        JLabel offsetXLabel = createSubtleLabel("Overlay X offset");
        JSpinner offsetX = new JSpinner(new SpinnerNumberModel(client.getTimersOverlayOffsetX(), -1200, 1200, 5));
        styleSpinner(offsetX);
        offsetX.addChangeListener(_e -> client.setTimersOverlayOffsetX((Integer) offsetX.getValue()));
        JLabel offsetYLabel = createSubtleLabel("Overlay Y offset");
        JSpinner offsetY = new JSpinner(new SpinnerNumberModel(client.getTimersOverlayOffsetY(), -1200, 1200, 5));
        styleSpinner(offsetY);
        offsetY.addChangeListener(_e -> client.setTimersOverlayOffsetY((Integer) offsetY.getValue()));
        JButton timerColor = createColorButton("Timer text color", client.getTimersTextColor(), client::setTimersTextColor);
        JButton setIncludeSprites = createActionButton("Set include sprite IDs");
        setIncludeSprites.addActionListener(_e -> {
            String value = JOptionPane.showInputDialog(this, "Comma-separated sprite IDs to include (blank = all)", client.getTimersIncludeSpriteIdsCsv());
            if (value != null) {
                client.setTimersIncludeSpriteIdsCsv(value);
            }
        });
        JButton setExcludeSprites = createActionButton("Set exclude sprite IDs");
        setExcludeSprites.addActionListener(_e -> {
            String value = JOptionPane.showInputDialog(this, "Comma-separated sprite IDs to exclude", client.getTimersExcludeSpriteIdsCsv());
            if (value != null) {
                client.setTimersExcludeSpriteIdsCsv(value);
            }
        });
        panel.add(enable);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(secondsOnly);
        panel.add(sortAscending);
        panel.add(showSpriteId);
        panel.add(minSecondsLabel);
        panel.add(minSeconds);
        panel.add(maxVisibleLabel);
        panel.add(maxVisible);
        panel.add(offsetXLabel);
        panel.add(offsetX);
        panel.add(offsetYLabel);
        panel.add(offsetY);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(timerColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(setIncludeSprites);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(setExcludeSprites);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JButton createThemeButton(String label, String themeKey) {
        JButton button = createActionButton(label);
        button.addActionListener(_e -> {
            currentTheme = themeKey;
            rebuildSidebar();
            if (rlSidebarTabs != null && rlSidebarTabs.getTabCount() > 1) {
                rlSidebarTabs.setSelectedIndex(1);
            }
        });
        return button;
    }

    private void applyTheme(String themeKey) {
        switch (themeKey) {
            case "Forest":
                RL_DARKER = new Color(8, 14, 9);
                RL_DARK = new Color(12, 22, 14);
                RL_DARKER_HOVER = new Color(22, 46, 28);
                RL_TAB_ACTIVE = new Color(33, 70, 43);
                RL_TEXT = new Color(217, 232, 220);
                RL_TEXT_SUBTLE = new Color(142, 172, 149);
                RL_BORDER = new Color(4, 8, 5);
                RL_GLOSS = new Color(255, 255, 255, 16);
                break;
            case "Yellow":
                RL_DARKER = new Color(18, 16, 6);
                RL_DARK = new Color(28, 24, 8);
                RL_DARKER_HOVER = new Color(60, 52, 16);
                RL_TAB_ACTIVE = new Color(95, 84, 24);
                RL_TEXT = new Color(239, 233, 195);
                RL_TEXT_SUBTLE = new Color(190, 176, 112);
                RL_BORDER = new Color(10, 9, 3);
                RL_GLOSS = new Color(255, 255, 255, 16);
                break;
            case "Cyan":
                RL_DARKER = new Color(6, 14, 18);
                RL_DARK = new Color(8, 22, 28);
                RL_DARKER_HOVER = new Color(17, 50, 62);
                RL_TAB_ACTIVE = new Color(26, 78, 96);
                RL_TEXT = new Color(203, 233, 239);
                RL_TEXT_SUBTLE = new Color(130, 178, 190);
                RL_BORDER = new Color(3, 8, 10);
                RL_GLOSS = new Color(255, 255, 255, 16);
                break;
            case "Indigo":
                RL_DARKER = new Color(10, 8, 19);
                RL_DARK = new Color(16, 12, 33);
                RL_DARKER_HOVER = new Color(35, 27, 72);
                RL_TAB_ACTIVE = new Color(53, 42, 108);
                RL_TEXT = new Color(220, 215, 239);
                RL_TEXT_SUBTLE = new Color(159, 150, 195);
                RL_BORDER = new Color(5, 4, 11);
                RL_GLOSS = new Color(255, 255, 255, 18);
                break;
            case "Orange":
                RL_DARKER = new Color(20, 10, 5);
                RL_DARK = new Color(32, 16, 7);
                RL_DARKER_HOVER = new Color(68, 35, 14);
                RL_TAB_ACTIVE = new Color(106, 54, 23);
                RL_TEXT = new Color(240, 224, 208);
                RL_TEXT_SUBTLE = new Color(196, 163, 132);
                RL_BORDER = new Color(11, 5, 2);
                RL_GLOSS = new Color(255, 255, 255, 18);
                break;
            default:
                RL_DARKER = new Color(16, 8, 10);
                RL_DARK = new Color(28, 11, 15);
                RL_DARKER_HOVER = new Color(56, 20, 29);
                RL_TAB_ACTIVE = new Color(80, 28, 40);
                RL_TEXT = new Color(224, 204, 210);
                RL_TEXT_SUBTLE = new Color(165, 136, 145);
                RL_BORDER = new Color(10, 4, 6);
                RL_GLOSS = new Color(255, 255, 255, 20);
                break;
        }
    }

    private JPanel createPluginSettingsContainer() {
        JPanel settingsPanel = createGradientPanel(new BorderLayout(), lighten(RL_DARK, 6), darken(RL_DARKER, 2));
        settingsPanel.setBackground(RL_DARK);
        return settingsPanel;
    }

    private void openPluginSettings(CardLayout cards, JPanel root, JPanel settingsPanel, String title, JPanel pluginSettings) {
        settingsPanel.removeAll();
        settingsPanel.setBackground(RL_DARK);

        JPanel topBar = createGradientPanel(new BorderLayout(), lighten(RL_TAB_ACTIVE, 6), darken(RL_DARK, 2));
        topBar.setBackground(RL_DARK);
        topBar.setBorder(BorderFactory.createEmptyBorder(8, 6, 6, 6));

        JButton back = new JButton(rlBackIcon);
        back.setToolTipText("Back");
        styleIconButton(back, 22);
        back.addActionListener(_e -> cards.show(root, "list"));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(back, BorderLayout.WEST);
        left.add(titleLabel, BorderLayout.CENTER);
        topBar.add(left, BorderLayout.CENTER);

        settingsPanel.add(topBar, BorderLayout.NORTH);
        settingsPanel.add(pluginSettings, BorderLayout.CENTER);
        settingsPanel.revalidate();
        settingsPanel.repaint();
        cards.show(root, "settings");
    }

    private JPanel createPluginListItem(String name, BooleanSupplier enabledSupplier, Consumer<Boolean> enabledSetter, Runnable onConfigure) {
        JPanel row = createGradientPanel(new BorderLayout(4, 0), lighten(RL_DARK, 8), darken(RL_DARKER, 1));
        row.setBackground(RL_DARK);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 4)),
                BorderFactory.createEmptyBorder(2, 6, 2, 4)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(name);
        label.setForeground(Color.WHITE);
        row.add(label, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));

        JButton gear = new JButton(rlConfigIcon);
        gear.setToolTipText("Edit plugin configuration");
        styleIconButton(gear, 22);
        gear.addActionListener(_e -> onConfigure.run());

        JToggleButton toggle = new JToggleButton(rlSwitcherOffIcon);
        toggle.setSelectedIcon(rlSwitcherOnIcon);
        toggle.setSelected(enabledSupplier.getAsBoolean());
        toggle.setToolTipText(toggle.isSelected() ? "Disable plugin" : "Enable plugin");
        styleIconButton(toggle, 28);
        toggle.addActionListener(_e -> {
            enabledSetter.accept(toggle.isSelected());
            toggle.setToolTipText(toggle.isSelected() ? "Disable plugin" : "Enable plugin");
        });

        controls.add(gear);
        controls.add(Box.createRigidArea(new Dimension(4, 0)));
        controls.add(toggle);
        row.add(controls, BorderLayout.EAST);
        return row;
    }

    private JPanel createVisualMetronomePanel(Client client) {
        JPanel panel = createPluginPanel("Visual Metronome", "RuneLite-style game tick visualizer");

        JCheckBox enable = createCheckBox("Enable plugin", client.isVisualMetronomePluginEnabled(),
                v -> client.setVisualMetronomePluginEnabled(v));
        JCheckBox highlightTile = createCheckBox("Highlight true tile", client.isVisualMetronomeHighlightTrueTile(),
                v -> client.setVisualMetronomeHighlightTrueTile(v));

        JLabel cycleLabel = createSubtleLabel("Cycle length (ticks)");
        JSpinner cycleSpinner = new JSpinner(new SpinnerNumberModel(client.getVisualMetronomeCycleLength(), 1, 10, 1));
        cycleSpinner.addChangeListener(_e -> client.setVisualMetronomeCycleLength((Integer) cycleSpinner.getValue()));
        styleSpinner(cycleSpinner);

        JButton tickColor = createColorButton("Tick color", client.getVisualMetronomeTickColor(),
                color -> client.setVisualMetronomeTickColor(color));
        JButton tockColor = createColorButton("Tock color", client.getVisualMetronomeTockColor(),
                color -> client.setVisualMetronomeTockColor(color));

        panel.add(enable);
        panel.add(highlightTile);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(cycleLabel);
        panel.add(cycleSpinner);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(tickColor);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(tockColor);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createShortestPathPanel(Client client) {
        JPanel panel = createPluginPanel("Shortest Path", "Draws route to your selected destination tile");

        JCheckBox enable = createCheckBox("Enable plugin", client.isShortestPathPluginEnabled(),
                v -> client.setShortestPathPluginEnabled(v));
        JCheckBox manualTarget = createCheckBox("Use manual target", client.isShortestPathUseManualTarget(),
                v -> client.setShortestPathUseManualTarget(v));
        JToggleButton selectNextTile = createToggleActionButton("Select next clicked tile");
        selectNextTile.setSelected(client.isShortestPathSelectTargetMode());
        JButton useCurrentDestination = createActionButton("Use current destination");
        JButton clearManualTarget = createActionButton("Clear manual target");
        JButton pathColor = createColorButton("Path color", client.getShortestPathColor(),
                color -> client.setShortestPathColor(color));

        JLabel hint = createSubtleLabel("");

        Runnable refreshState = () -> {
            boolean pluginEnabled = client.isShortestPathPluginEnabled();
            boolean manualEnabled = pluginEnabled && manualTarget.isSelected();

            manualTarget.setEnabled(pluginEnabled);
            selectNextTile.setEnabled(manualEnabled);
            useCurrentDestination.setEnabled(pluginEnabled);
            clearManualTarget.setEnabled(pluginEnabled);
            pathColor.setEnabled(pluginEnabled);

            if (!pluginEnabled) {
                selectNextTile.setSelected(false);
                hint.setText("Enable plugin to preview a route");
                return;
            }

            selectNextTile.setSelected(client.isShortestPathSelectTargetMode());
            selectNextTile.setBackground(selectNextTile.isSelected() ? RL_TAB_ACTIVE : RL_DARK);
            if (client.isShortestPathSelectTargetMode()) {
                hint.setText("Click a tile in-game to set manual target");
            } else if (client.isShortestPathUseManualTarget()) {
                hint.setText("Manual target active");
            } else {
                hint.setText("Target source: current walk destination");
            }
        };

        enable.addActionListener(_e -> refreshState.run());
        manualTarget.addActionListener(_e -> refreshState.run());
        selectNextTile.addActionListener(_e -> {
            client.setShortestPathSelectTargetMode(selectNextTile.isSelected());
            refreshState.run();
        });
        useCurrentDestination.addActionListener(_e -> {
            client.useCurrentDestinationAsShortestPathTarget();
            manualTarget.setSelected(client.isShortestPathUseManualTarget());
            refreshState.run();
        });
        clearManualTarget.addActionListener(_e -> {
            client.clearShortestPathTarget();
            manualTarget.setSelected(client.isShortestPathUseManualTarget());
            refreshState.run();
        });

        panel.add(enable);
        panel.add(manualTarget);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(selectNextTile);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(useCurrentDestination);
        panel.add(Box.createRigidArea(new Dimension(0, 4)));
        panel.add(clearManualTarget);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(pathColor);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(hint);
        panel.add(Box.createVerticalGlue());
        refreshState.run();
        return panel;
    }

    private JPanel createPluginPanel(String titleText, String subtitleText) {
        JPanel panel = createGradientPanel(null, lighten(RL_DARK, 8), darken(RL_DARKER, 1));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(RL_DARK);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 2)),
                BorderFactory.createEmptyBorder(RL_PANEL_PADDING, RL_PANEL_PADDING, RL_PANEL_PADDING, RL_PANEL_PADDING)));

        JLabel title = new JLabel(titleText);
        title.setForeground(lighten(RL_TEXT, 20));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = createSubtleLabel(subtitleText);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JPanel wrapPluginPanel(JPanel pluginPanel) {
        JPanel northPanel = createGradientPanel(new BorderLayout(), lighten(RL_DARK, 6), darken(RL_DARKER, 2));
        northPanel.setBackground(RL_DARK);
        northPanel.add(pluginPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(northPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(RL_DARK);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapped = new JPanel(new BorderLayout());
        wrapped.setBackground(RL_DARK);
        wrapped.setPreferredSize(new Dimension(RL_SIDEBAR_CONTENT_WIDTH, 0));
        wrapped.add(scrollPane, BorderLayout.CENTER);
        return wrapped;
    }

    private JCheckBox createCheckBox(String text, boolean selected, Consumer<Boolean> onChange) {
        JCheckBox checkBox = new JCheckBox(text, selected);
        checkBox.setOpaque(false);
        checkBox.setForeground(RL_TEXT);
        checkBox.setFocusPainted(false);
        checkBox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        checkBox.setAlignmentX(LEFT_ALIGNMENT);
        checkBox.setBackground(new Color(0, 0, 0, 0));
        checkBox.addActionListener(_e -> onChange.accept(checkBox.isSelected()));
        return checkBox;
    }

    private JButton createColorButton(String label, Color initialColor, Consumer<Color> onChange) {
        JButton button = new JButton(label);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setForeground(RL_TEXT);
        button.setBackground(lighten(RL_DARK, 10));
        button.setBorder(BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 6)));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        button.setAlignmentX(LEFT_ALIGNMENT);

        JPanel swatch = new JPanel();
        swatch.setBackground(initialColor == null ? Color.WHITE : new Color(initialColor.getRed(), initialColor.getGreen(), initialColor.getBlue()));
        swatch.setPreferredSize(new Dimension(16, 16));
        swatch.setBorder(BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 8)));
        button.setLayout(new BorderLayout());
        JLabel textLabel = new JLabel(label);
        textLabel.setForeground(RL_TEXT);
        button.add(textLabel, BorderLayout.WEST);
        button.add(swatch, BorderLayout.EAST);

        button.addActionListener(_e -> {
            Color selected = JColorChooser.showDialog(this, "Choose " + label, swatch.getBackground());
            if (selected != null) {
                swatch.setBackground(selected);
                onChange.accept(selected);
            }
        });
        return button;
    }

    private JLabel createSubtleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(RL_TEXT_SUBTLE);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setForeground(RL_TEXT);
        button.setBackground(lighten(RL_DARK, 10));
        button.setBorder(BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 6)));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        button.setAlignmentX(LEFT_ALIGNMENT);
        return button;
    }

    private JToggleButton createToggleActionButton(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setUI(new BasicButtonUI());
        button.setHorizontalAlignment(JButton.LEFT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setForeground(RL_TEXT);
        button.setBackground(lighten(RL_DARK, 10));
        button.setBorder(BorderFactory.createLineBorder(lighten(RL_TAB_ACTIVE, 6)));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.addActionListener(_e -> button.setBackground(button.isSelected() ? RL_TAB_ACTIVE : RL_DARK));
        button.setBackground(button.isSelected() ? RL_TAB_ACTIVE : RL_DARK);
        return button;
    }

    private void styleIconButton(AbstractButton button, int width) {
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(width, 20));
        button.setMaximumSize(new Dimension(width, 20));
        button.setMinimumSize(new Dimension(width, 20));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setOpaque(true);
                button.setBackground(RL_DARKER_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setOpaque(false);
                button.setBackground(new Color(0, 0, 0, 0));
            }
        });
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setMaximumSize(new Dimension(70, 24));
        spinner.setPreferredSize(new Dimension(70, 24));
    }

    private final class RuneLiteTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(2, 5, 2, 5);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabAreaInsets = new Insets(2, 0, 2, 0);
            contentBorderInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
            return RL_TAB_HEIGHT;
        }

        @Override
        protected int calculateTabWidth(int tabPlacement, int tabIndex, java.awt.FontMetrics metrics) {
            return RL_SIDEBAR_CLOSED_WIDTH - 2;
        }

        @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            boolean hovered = tabIndex == rlSidebarHoverTab;
            Color top = isSelected ? lighten(RL_TAB_ACTIVE, 8) : (hovered ? lighten(RL_DARKER_HOVER, 4) : lighten(RL_DARK, 4));
            Color bottom = isSelected ? darken(RL_TAB_ACTIVE, 6) : (hovered ? darken(RL_DARKER_HOVER, 8) : darken(RL_DARK, 4));
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(x, y, top, x, y + h, bottom));
            g2.fillRect(x, y, w, h);
            g2.setColor(RL_GLOSS);
            g2.drawLine(x + 1, y + 1, x + w - 2, y + 1);
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            g.setColor(RL_BORDER);
            g.drawRect(x, y, w, h);
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            // RuneLite does not render default Swing focus ring on sidebar tabs.
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(RL_BORDER);
            int x = tabPane.getInsets().left;
            int y = tabPane.getInsets().top;
            int w = tabPane.getWidth() - tabPane.getInsets().left - tabPane.getInsets().right;
            int h = tabPane.getHeight() - tabPane.getInsets().top - tabPane.getInsets().bottom;
            g.drawRect(x, y, Math.max(0, w - 1), Math.max(0, h - 1));
        }
    }

    private void styleToggleButton(JButton button) {
        button.setFocusPainted(false);
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        button.setBackground(lighten(RL_DARK, 10));
        button.setForeground(RL_TEXT);
        button.setPreferredSize(new Dimension(23, 23));
        button.setMaximumSize(new Dimension(23, 23));
        button.setMinimumSize(new Dimension(23, 23));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(RL_DARKER_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(lighten(RL_DARK, 10));
            }
        });
    }

    private JPanel createGradientPanel(LayoutManager layout, Color topColor, Color bottomColor) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(RL_GLOSS);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    private Color lighten(Color color, int amount) {
        return new Color(
                Math.min(255, color.getRed() + amount),
                Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount),
                color.getAlpha());
    }

    private Color darken(Color color, int amount) {
        return new Color(
                Math.max(0, color.getRed() - amount),
                Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount),
                color.getAlpha());
    }

    private ImageIcon loadIcon(String resourcePath, int width, int height) {
        try {
            URL resource = GameWindow.class.getResource(resourcePath);
            if (resource == null) {
                return createFallbackIcon(width, height);
            }
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                return createFallbackIcon(width, height);
            }
            Image scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception ex) {
            return createFallbackIcon(width, height);
        }
    }

    private ImageIcon flipHorizontal(ImageIcon source) {
        if (source == null || source.getImage() == null) {
            return createFallbackIcon(16, 16);
        }

        int width = source.getIconWidth();
        int height = source.getIconHeight();
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = flipped.createGraphics();
        g.drawImage(source.getImage(), width, 0, -width, height, null);
        g.dispose();
        return new ImageIcon(flipped);
    }

    private ImageIcon createFallbackIcon(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(RL_TEXT);
        g.drawRect(1, 1, Math.max(1, width - 3), Math.max(1, height - 3));
        g.drawLine(3, height / 2, width - 4, height / 2);
        g.dispose();
        return new ImageIcon(image);
    }

    private ImageIcon createSwitcherOffIcon(ImageIcon source) {
        if (source == null || source.getImage() == null) {
            return createFallbackIcon(28, 14);
        }
        int width = source.getIconWidth();
        int height = source.getIconHeight();
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = src.createGraphics();
        g.drawImage(source.getImage(), 0, 0, null);
        g.dispose();

        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        BufferedImage gray = op.filter(src, null);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgba = gray.getRGB(x, y);
                int a = (rgba >>> 24) & 0xFF;
                int c = rgba & 0xFF;
                int dark = Math.max(0, (int) (c * 0.61f));
                out.setRGB(width - 1 - x, y, (a << 24) | (dark << 16) | (dark << 8) | dark);
            }
        }
        return new ImageIcon(out);
    }
}
