package com.horstmann.violet.application.menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.behavior.CutCopyPasteBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.IEditorPartBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.SideBar;

/**
 * Compact tool bar shown at the right of the menu bar. As the menu bar is shared by all the diagrams, each button acts on the
 * active workspace. When the window is too narrow, the least useful groups of buttons go into a "more" menu, one after the
 * other (see {@link #setCollapseLevel(int)}); undo and redo always stay.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class MenuBarToolBar extends JPanel
{

    public MenuBarToolBar(MainFrame mainFrame)
    {
        super(new FlowLayout(FlowLayout.RIGHT, FLOW_GAP, 0));
        ResourceBundleInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        setOpaque(false);
        Consumer<IWorkspace> undo = workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::undo);
        Consumer<IWorkspace> redo = workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::redo);
        Consumer<IWorkspace> cut = workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::cut);
        Consumer<IWorkspace> copy = workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::copy);
        Consumer<IWorkspace> paste = workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::paste);
        Consumer<IWorkspace> delete = workspace -> workspace.getEditorPart().removeSelected();
        Consumer<IWorkspace> zoomOut = workspace -> workspace.getEditorPart().changeZoom(-1);
        Consumer<IWorkspace> zoomIn = workspace -> workspace.getEditorPart().changeZoom(1);

        add(createButton(this.undoTooltip, ToolShapes.UNDO, undo));
        add(createButton(this.redoTooltip, ToolShapes.REDO, redo));

        CollapsibleGroup cutGroup = new CollapsibleGroup();
        cutGroup.add(createSeparator());
        cutGroup.addAction(this.cutTooltip, ToolShapes.CUT, cut);
        CollapsibleGroup copyGroup = new CollapsibleGroup();
        copyGroup.addAction(this.copyTooltip, ToolShapes.COPY, copy);
        CollapsibleGroup pasteGroup = new CollapsibleGroup();
        pasteGroup.addAction(this.pasteTooltip, ToolShapes.PASTE, paste);
        CollapsibleGroup deleteGroup = new CollapsibleGroup();
        deleteGroup.addAction(this.deleteTooltip, ToolShapes.DELETE, delete);

        CollapsibleGroup zoomGroup = new CollapsibleGroup();
        zoomGroup.add(createSeparator());
        zoomGroup.addAction(this.zoomOutTooltip, ToolShapes.ZOOM_OUT, zoomOut);
        zoomGroup.add(getZoomLabel());
        zoomGroup.addAction(this.zoomInTooltip, ToolShapes.ZOOM_IN, zoomIn);

        CollapsibleGroup themeGroup = new CollapsibleGroup();
        themeGroup.add(createSeparator());
        themeGroup.add(getThemeSwitch());
        themeGroup.addMenuItem(this::createThemeMenuItem);

        CollapsibleGroup fullScreenGroup = new CollapsibleGroup();
        fullScreenGroup.add(getFullScreenSeparator());
        fullScreenGroup.add(getFullScreenButton());
        fullScreenGroup.addMenuItem(this::createFullScreenMenuItem);
        // Its visibility also depends on the support of full screen
        fullScreenGroup.setVisibilityApplier(isCollapsed -> getFullScreenButton().setCollapsed(isCollapsed));
        fullScreenGroup.setAvailability(() -> getFullScreenButton().isSupported());

        add(getMoreButton());
        getMoreButton().setVisible(false);

        getDisplayedGroups().addAll(Arrays.asList(cutGroup, copyGroup, pasteGroup, deleteGroup, zoomGroup, themeGroup, fullScreenGroup));
        getCollapseOrder().addAll(Arrays.asList(themeGroup, fullScreenGroup, zoomGroup, deleteGroup, pasteGroup, copyGroup, cutGroup));
    }

    /**
     * Prevents the menu bar from stretching this panel, so that the free space goes to the horizontal glue before it.
     */
    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    /**
     * Prevents the menu bar from shrinking this panel, which would cut its last buttons off: the title shrinks instead, and the
     * buttons which don't fit go into the "more" menu.
     */
    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    /**
     * @return the number of groups of buttons which can go into the "more" menu
     */
    public int getMaxCollapseLevel()
    {
        return getCollapseOrder().size();
    }

    /**
     * Moves groups of buttons into the "more" menu, the least useful first: theme, full screen, zoom, delete, paste, copy, cut.
     *
     * @param level number of groups moved, 0 to show all the buttons
     */
    public void setCollapseLevel(int level)
    {
        List<CollapsibleGroup> collapseOrder = getCollapseOrder();
        for (int i = 0; i < collapseOrder.size(); i++)
        {
            collapseOrder.get(i).setCollapsed(i < level);
        }
        getMoreButton().setVisible(level > 0);
    }

    /**
     * @param level as given to {@link #setCollapseLevel(int)}
     * @return the preferred width of this tool bar at this level, without changing anything
     */
    public int getPreferredWidth(int level)
    {
        Insets insets = getInsets();
        int width = insets.left + insets.right + FLOW_GAP;
        for (Component aComponent : getComponents())
        {
            if (isShownAtLevel(aComponent, level))
            {
                width += aComponent.getPreferredSize().width + FLOW_GAP;
            }
        }
        return width;
    }

    private boolean isShownAtLevel(Component component, int level)
    {
        if (component == getMoreButton())
        {
            return level > 0;
        }
        List<CollapsibleGroup> collapseOrder = getCollapseOrder();
        for (int i = 0; i < collapseOrder.size(); i++)
        {
            CollapsibleGroup aGroup = collapseOrder.get(i);
            if (aGroup.contains(component))
            {
                return i >= level && aGroup.isAvailable();
            }
        }
        return true;
    }

    /**
     * Shows the zoom of this diagram, and follows its changes
     *
     * @param workspace the active diagram, or null if none is opened
     */
    public void setActiveWorkspace(IWorkspace workspace)
    {
        JComponent editorComponent = workspace != null ? workspace.getEditorPart().getSwingComponent() : null;
        if (editorComponent == this.zoomSource)
        {
            return;
        }
        if (this.zoomSource != null)
        {
            this.zoomSource.removePropertyChangeListener(IEditorPart.ZOOM_PROPERTY, getZoomListener());
        }
        this.zoomSource = editorComponent;
        if (editorComponent != null)
        {
            editorComponent.addPropertyChangeListener(IEditorPart.ZOOM_PROPERTY, getZoomListener());
            getZoomLabel().showZoom(workspace.getEditorPart().getZoomFactor());
        }
        else
        {
            getZoomLabel().setText("");
        }
    }

    private ZoomLabel getZoomLabel()
    {
        if (this.zoomLabel == null)
        {
            this.zoomLabel = new ZoomLabel();
        }
        return this.zoomLabel;
    }

    private ThemeSwitch getThemeSwitch()
    {
        if (this.themeSwitch == null)
        {
            this.themeSwitch = new ThemeSwitch();
        }
        return this.themeSwitch;
    }

    private FullScreenButton getFullScreenButton()
    {
        if (this.fullScreenButton == null)
        {
            this.fullScreenButton = new FullScreenButton();
        }
        return this.fullScreenButton;
    }

    /**
     * @return the separator before the full screen button, hidden with it
     */
    private JSeparator getFullScreenSeparator()
    {
        if (this.fullScreenSeparator == null)
        {
            this.fullScreenSeparator = createSeparator();
        }
        return this.fullScreenSeparator;
    }

    /**
     * @return the button which shows the actions that don't fit in the menu bar
     */
    private JButton getMoreButton()
    {
        if (this.moreButton == null)
        {
            this.moreButton = new ToolButton(new ShapeIcon(ToolShapes.MORE, ICON_SIZE));
            this.moreButton.setToolTipText(this.moreTooltip);
            this.moreButton.addActionListener(event -> showMoreMenu());
        }
        return this.moreButton;
    }

    private List<CollapsibleGroup> getDisplayedGroups()
    {
        if (this.displayedGroups == null)
        {
            this.displayedGroups = new ArrayList<CollapsibleGroup>();
        }
        return this.displayedGroups;
    }

    private List<CollapsibleGroup> getCollapseOrder()
    {
        if (this.collapseOrder == null)
        {
            this.collapseOrder = new ArrayList<CollapsibleGroup>();
        }
        return this.collapseOrder;
    }

    /**
     * Shows the actions of the hidden buttons, in the order of the tool bar, below the "more" button and aligned on its right
     */
    private void showMoreMenu()
    {
        JPopupMenu popup = new JPopupMenu();
        for (CollapsibleGroup aGroup : getDisplayedGroups())
        {
            if (aGroup.isCollapsed() && aGroup.isAvailable())
            {
                aGroup.addMenuItemsTo(popup);
            }
        }
        if (popup.getComponentCount() == 0)
        {
            return;
        }
        JButton button = getMoreButton();
        popup.show(button, button.getWidth() - popup.getPreferredSize().width, button.getHeight());
    }

    private JMenuItem createMenuItem(String text, Shape shape, Runnable action)
    {
        JMenuItem menuItem = new JMenuItem(text, new ShapeIcon(shape, ICON_SIZE));
        menuItem.addActionListener(event -> action.run());
        return menuItem;
    }

    private JMenuItem createThemeMenuItem()
    {
        boolean isDark = ThemeManager.getInstance().getTheme().isDark();
        return createMenuItem(isDark ? this.lightThemeTooltip : this.darkThemeTooltip, isDark ? ToolShapes.SUN : ToolShapes.MOON,
                // Later, as the look and feel can't change safely during the events of this menu
                () -> SwingUtilities.invokeLater(() -> {
                    this.mainFrame.toggleTheme();
                    getThemeSwitch().refreshState();
                }));
    }

    private JMenuItem createFullScreenMenuItem()
    {
        boolean isFullScreen = this.mainFrame.isFullScreen();
        return createMenuItem(isFullScreen ? this.exitFullScreenTooltip : this.enterFullScreenTooltip,
                isFullScreen ? ToolShapes.EXIT_FULL_SCREEN : ToolShapes.FULL_SCREEN, () -> {
                    // Right away: a browser accepts full screen only during a user gesture
                    this.mainFrame.toggleFullScreen();
                    getFullScreenButton().refreshState();
                });
    }

    /**
     * @return the listener which shows the zoom changes of the active diagram. Always the same instance, so that it can be removed.
     */
    private PropertyChangeListener getZoomListener()
    {
        if (this.zoomListener == null)
        {
            this.zoomListener = event -> getZoomLabel().showZoom((Double) event.getNewValue());
        }
        return this.zoomListener;
    }

    private JButton createButton(String tooltip, Shape shape, Consumer<IWorkspace> action)
    {
        JButton button = new ToolButton(new ShapeIcon(shape, ICON_SIZE));
        button.setToolTipText(tooltip);
        button.addActionListener(event -> runOnActiveWorkspace(action));
        return button;
    }

    private void runOnActiveWorkspace(Consumer<IWorkspace> action)
    {
        IWorkspace workspace = this.mainFrame.getActiveWorkspace();
        if (workspace != null)
        {
            action.accept(workspace);
        }
    }

    private JSeparator createSeparator()
    {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Color foreground = ThemeManager.getInstance().getTheme().getMenubarForegroundColor();
        separator.setForeground(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 90));
        separator.setPreferredSize(new Dimension(9, BUTTON_SIZE.height));
        return separator;
    }

    private static <T extends IEditorPartBehavior> void withBehavior(IWorkspace workspace, Class<T> type, Consumer<T> action)
    {
        IEditorPartBehaviorManager behaviorManager = workspace.getEditorPart().getBehaviorManager();
        List<T> found = behaviorManager.getBehaviors(type);
        if (found.size() == 1)
        {
            action.accept(found.get(0));
        }
    }

    /**
     * Components of this tool bar which go into the "more" menu together, with the menu items replacing them there
     */
    private class CollapsibleGroup
    {
        /**
         * Adds a component to this tool bar, in this group
         */
        void add(Component component)
        {
            MenuBarToolBar.this.add(component);
            this.components.add(component);
        }

        /**
         * Adds a button to this tool bar, and the matching item to the "more" menu
         */
        void addAction(String text, Shape shape, Consumer<IWorkspace> action)
        {
            add(createButton(text, shape, action));
            addMenuItem(() -> createMenuItem(text, shape, () -> runOnActiveWorkspace(action)));
        }

        /**
         * @param menuItemFactory builds the item when the "more" menu opens, so that it shows the current state
         */
        void addMenuItem(Supplier<JMenuItem> menuItemFactory)
        {
            this.menuItemFactories.add(menuItemFactory);
        }

        void addMenuItemsTo(JPopupMenu popup)
        {
            for (Supplier<JMenuItem> aFactory : this.menuItemFactories)
            {
                popup.add(aFactory.get());
            }
        }

        boolean contains(Component component)
        {
            return this.components.contains(component);
        }

        void setCollapsed(boolean isCollapsed)
        {
            this.isCollapsed = isCollapsed;
            if (this.visibilityApplier != null)
            {
                this.visibilityApplier.accept(isCollapsed);
                return;
            }
            for (Component aComponent : this.components)
            {
                aComponent.setVisible(!isCollapsed);
            }
        }

        boolean isCollapsed()
        {
            return this.isCollapsed;
        }

        /**
         * @param visibilityApplier shows or hides the components instead of this group, given true when they go into the menu
         */
        void setVisibilityApplier(Consumer<Boolean> visibilityApplier)
        {
            this.visibilityApplier = visibilityApplier;
        }

        /**
         * @param availability tells whether these actions can be used at all
         */
        void setAvailability(BooleanSupplier availability)
        {
            this.availability = availability;
        }

        boolean isAvailable()
        {
            return this.availability == null || this.availability.getAsBoolean();
        }

        private final List<Component> components = new ArrayList<Component>();
        private final List<Supplier<JMenuItem>> menuItemFactories = new ArrayList<Supplier<JMenuItem>>();
        private boolean isCollapsed = false;
        private Consumer<Boolean> visibilityApplier;
        private BooleanSupplier availability;
    }

    /**
     * Flat button showing a rounded background on hover, whatever the current theme is.
     */
    private static class ToolButton extends JButton
    {
        ToolButton(Icon icon)
        {
            super(icon);
            setPreferredSize(BUTTON_SIZE);
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            // Keeps the keyboard focus on the diagram
            setFocusable(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            if (getModel().isRollover())
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color foreground = getForeground();
                int alpha = getModel().isPressed() ? 60 : 30;
                g2.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Zoom of the active diagram as a percentage, with a fixed width so that the buttons around it don't move.
     */
    private static class ZoomLabel extends JLabel
    {
        ZoomLabel()
        {
            super("", SwingConstants.CENTER);
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
        }

        void showZoom(double zoomFactor)
        {
            setText(Math.round(zoomFactor * 100) + "%");
        }

        @Override
        public Dimension getPreferredSize()
        {
            // Only grows beyond 999%, which is rare
            FontMetrics metrics = getFontMetrics(getFont());
            int width = Math.max(metrics.stringWidth(USUAL_WIDEST_TEXT), metrics.stringWidth(getText()));
            return new Dimension(width, BUTTON_SIZE.height);
        }

        private static final String USUAL_WIDEST_TEXT = "000%";
    }

    /**
     * Switch between a sun and a moon, which toggles the light and dark themes.
     */
    private class ThemeSwitch extends JToggleButton
    {
        ThemeSwitch()
        {
            setPreferredSize(new Dimension(2 * (SWITCH_ICON_SIZE + SWITCH_GAP) + TRACK_WIDTH, BUTTON_SIZE.height));
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setFocusable(false);
            refreshState();
            // Later, as the look and feel can't change safely during the events of this button
            addActionListener(event -> SwingUtilities.invokeLater(() -> {
                MenuBarToolBar.this.mainFrame.toggleTheme();
                refreshState();
            }));
        }

        void refreshState()
        {
            boolean isDark = ThemeManager.getInstance().getTheme().isDark();
            setSelected(isDark);
            setToolTipText(isDark ? MenuBarToolBar.this.lightThemeTooltip : MenuBarToolBar.this.darkThemeTooltip);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int iconY = (getHeight() - SWITCH_ICON_SIZE) / 2;
            int trackX = SWITCH_ICON_SIZE + SWITCH_GAP;
            int trackY = (getHeight() - TRACK_HEIGHT) / 2;
            boolean isDark = isSelected();
            new ShapeIcon(ToolShapes.SUN, SWITCH_ICON_SIZE, getForegroundColor(isDark ? INACTIVE_ALPHA : 255)).paintIcon(this, g2, 0, iconY);
            new ShapeIcon(ToolShapes.MOON, SWITCH_ICON_SIZE, getForegroundColor(isDark ? 255 : INACTIVE_ALPHA)).paintIcon(this, g2,
                    trackX + TRACK_WIDTH + SWITCH_GAP, iconY);
            g2.setColor(getForegroundColor(getModel().isRollover() ? TRACK_ROLLOVER_ALPHA : TRACK_ALPHA));
            g2.fillRoundRect(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);
            int knobSize = TRACK_HEIGHT - 2 * KNOB_GAP;
            int knobX = isDark ? trackX + TRACK_WIDTH - KNOB_GAP - knobSize : trackX + KNOB_GAP;
            g2.setColor(getForeground());
            g2.fillOval(knobX, trackY + KNOB_GAP, knobSize, knobSize);
            g2.dispose();
        }

        private Color getForegroundColor(int alpha)
        {
            Color foreground = getForeground();
            return new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), alpha);
        }
    }

    /**
     * Enters or leaves full screen. Its icon follows the state, which can also change without it (Escape key in a browser),
     * and it is hidden, with its separator, where full screen isn't possible (iPhone browsers) or when it went into the "more"
     * menu.
     */
    private class FullScreenButton extends ToolButton
    {
        FullScreenButton()
        {
            super(new ShapeIcon(ToolShapes.FULL_SCREEN, ICON_SIZE));
            // Called right away: a browser accepts full screen only during a user gesture
            addActionListener(event -> {
                MenuBarToolBar.this.mainFrame.toggleFullScreen();
                refreshState();
            });
            refreshState();
            Timer stateFollower = new Timer(FULL_SCREEN_POLLING_DELAY, event -> refreshState());
            stateFollower.start();
        }

        /**
         * @param isCollapsed true when this button went into the "more" menu
         */
        void setCollapsed(boolean isCollapsed)
        {
            this.isCollapsed = isCollapsed;
            refreshVisibility();
        }

        /**
         * @return true if full screen is possible here, as last checked
         */
        boolean isSupported()
        {
            return this.isSupported;
        }

        void refreshState()
        {
            MainFrame frame = MenuBarToolBar.this.mainFrame;
            this.isSupported = frame.isFullScreenSupported();
            refreshVisibility();
            boolean isFullScreen = frame.isFullScreen();
            if (this.isFullScreenShown != null && this.isFullScreenShown == isFullScreen)
            {
                return;
            }
            this.isFullScreenShown = isFullScreen;
            setIcon(new ShapeIcon(isFullScreen ? ToolShapes.EXIT_FULL_SCREEN : ToolShapes.FULL_SCREEN, ICON_SIZE));
            setToolTipText(isFullScreen ? MenuBarToolBar.this.exitFullScreenTooltip : MenuBarToolBar.this.enterFullScreenTooltip);
        }

        private void refreshVisibility()
        {
            boolean isShown = this.isSupported && !this.isCollapsed;
            if (isVisible() != isShown)
            {
                setVisible(isShown);
                getFullScreenSeparator().setVisible(isShown);
            }
        }

        /** State shown by the icon, null before the first refresh */
        private Boolean isFullScreenShown;
        private boolean isSupported = true;
        private boolean isCollapsed = false;
    }

    private static final int FLOW_GAP = 2;
    private static final int FULL_SCREEN_POLLING_DELAY = 500;
    private static final int SWITCH_ICON_SIZE = 16;
    private static final int SWITCH_GAP = 5;
    private static final int TRACK_WIDTH = 30;
    private static final int TRACK_HEIGHT = 16;
    private static final int KNOB_GAP = 3;
    private static final int TRACK_ALPHA = 70;
    private static final int TRACK_ROLLOVER_ALPHA = 110;
    private static final int INACTIVE_ALPHA = 130;
    private static final int ICON_SIZE = 18;
    private static final Dimension BUTTON_SIZE = new Dimension(28, 26);

    private final MainFrame mainFrame;
    private ZoomLabel zoomLabel;
    private ThemeSwitch themeSwitch;
    private FullScreenButton fullScreenButton;
    private JSeparator fullScreenSeparator;
    private JButton moreButton;
    /** Groups of buttons in the order of the tool bar */
    private List<CollapsibleGroup> displayedGroups;
    /** The same groups, in the order they go into the "more" menu */
    private List<CollapsibleGroup> collapseOrder;
    private PropertyChangeListener zoomListener;
    /** Editor of the active diagram, whose zoom is shown */
    private JComponent zoomSource;

    @ResourceBundleBean(key = "undo.tooltip")
    private String undoTooltip;
    @ResourceBundleBean(key = "redo.tooltip")
    private String redoTooltip;
    @ResourceBundleBean(key = "cut.tooltip")
    private String cutTooltip;
    @ResourceBundleBean(key = "copy.tooltip")
    private String copyTooltip;
    @ResourceBundleBean(key = "paste.tooltip")
    private String pasteTooltip;
    @ResourceBundleBean(key = "delete.tooltip")
    private String deleteTooltip;
    @ResourceBundleBean(key = "zoomout.tooltip")
    private String zoomOutTooltip;
    @ResourceBundleBean(key = "zoomin.tooltip")
    private String zoomInTooltip;
    @ResourceBundleBean(key = "theme.dark.tooltip")
    private String darkThemeTooltip;
    @ResourceBundleBean(key = "theme.light.tooltip")
    private String lightThemeTooltip;
    @ResourceBundleBean(key = "fullscreen.enter.tooltip")
    private String enterFullScreenTooltip;
    @ResourceBundleBean(key = "fullscreen.exit.tooltip")
    private String exitFullScreenTooltip;
    @ResourceBundleBean(key = "menubar.more.tooltip")
    private String moreTooltip;

}
