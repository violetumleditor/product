package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.horstmann.violet.framework.swingextension.DragScrollSupport;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * Vertical list of tool buttons, with optional headers and grids. A graph tools bar shows several of them (selection tool,
 * diagram tools, common tools), each in its own side bar section.
 */
public class GraphToolsBarPanel extends JPanel
{

    private static final int GRID_COLUMNS = 4;
    private static final int GRID_GAP = 2;
    private static final int HEADER_ALPHA = 170;
    private static final int HEADER_LINE_ALPHA = 45;
    private static final int HEADER_TOP_GAP = 8;
    private static final int HEADER_BOTTOM_GAP = 3;
    private static final int HEADER_SIDE_GAP = 8;
    private static final int HEADER_LINE_GAP = 8;

    public GraphToolsBarPanel(GraphToolsBar graphToolsBar)
    {
        super(new GridBagLayout());
        this.graphToolsBar = graphToolsBar;
        setOpaque(true);
        setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
        this.graphToolsBar.addListener(new IGraphToolsBarListener()
        {
            @Override
            public void toolSelectionChanged(GraphTool selectedTool)
            {
                for (GraphToolsBarButton aButton : buttons)
                {
                    aButton.setSelected(aButton.getTool().equals(selectedTool));
                }
                refreshCategoryButtons();
            }
        });
        setUI(new GraphToolsBarPanelUI());
    }

    /**
     * Keeps the custom UI, and takes the colors of the new theme, when the look and feel changes
     */
    @Override
    public void updateUI()
    {
        if (getUI() instanceof GraphToolsBarPanelUI)
        {
            setUI(new GraphToolsBarPanelUI());
            return;
        }
        super.updateUI();
    }

    /**
     * Adds a small title above a group of tools, if this group isn't empty
     *
     * @param title
     * @param tools of the group
     */
    public void addHeader(String title, List<GraphTool> tools)
    {
        if (title == null || tools.isEmpty())
        {
            return;
        }
        addRow(new HeaderLabel(title));
    }

    /**
     * Adds one row (icon and label) per tool
     *
     * @param tools
     */
    public void addTools(List<GraphTool> tools)
    {
        for (GraphTool aTool : tools)
        {
            addRow(createButton(aTool));
        }
    }

    /**
     * Adds tools as a grid of icons with a short label below each one, on as many rows as needed
     *
     * @param tools
     * @param shortLabels one per tool; the full label stays in the tool tip
     */
    public void addToolGrid(List<GraphTool> tools, List<String> shortLabels)
    {
        if (tools.isEmpty())
        {
            return;
        }
        JPanel grid = new JPanel(new GridLayout(0, GRID_COLUMNS, GRID_GAP, GRID_GAP));
        grid.setOpaque(false);
        for (int i = 0; i < tools.size(); i++)
        {
            GraphToolsBarButton button = createButton(tools.get(i));
            button.setText(shortLabels.get(i));
            button.setTextBelowIcon();
            grid.add(button);
        }
        addRow(grid);
    }

    /**
     * Adds one icon per tool, without text: the label stays in the tool tip. For the narrow column shown when the side bar is
     * reduced.
     *
     * @param tools
     */
    public void addIconTools(List<GraphTool> tools)
    {
        for (GraphTool aTool : tools)
        {
            GraphToolsBarButton button = createButton(aTool);
            button.setTextVisible(false);
            button.setPreferredSize(ICON_BUTTON_SIZE);
            addRow(button);
        }
    }

    /**
     * Adds a group of tools under a header which collapses it: a grid of icons with the tool name below each one, denser than
     * one row per tool when the diagram has many kinds of elements
     *
     * @param title
     * @param tools of the group
     * @param isExpanded true to show the tools, false to show the header only
     */
    public void addCategory(String title, List<GraphTool> tools, boolean isExpanded)
    {
        if (tools.isEmpty())
        {
            return;
        }
        JPanel grid = createTileGrid(tools);
        grid.setVisible(isExpanded);
        addRow(new CategoryHeader(title, tools.size(), grid));
        addRow(grid);
    }

    /**
     * Adds tools as a grid of icons with the tool name below each one
     *
     * @param tools
     */
    public void addTiles(List<GraphTool> tools)
    {
        if (tools.isEmpty())
        {
            return;
        }
        if (this.tileGrid == null)
        {
            this.tileGrid = createTileGrid(tools);
            addRow(this.tileGrid);
            return;
        }
        for (GraphTool aTool : tools)
        {
            this.tileGrid.add(createTile(aTool));
        }
        revalidate();
    }

    private JPanel createTileGrid(List<GraphTool> tools)
    {
        JPanel grid = new JPanel(new TileGridLayout(TILE_COLUMNS, GRID_GAP));
        grid.setOpaque(false);
        for (GraphTool aTool : tools)
        {
            grid.add(createTile(aTool));
        }
        return grid;
    }

    private GraphToolsBarButton createTile(GraphTool tool)
    {
        GraphToolsBarButton button = createButton(tool);
        button.setText(getWrappedLabel(tool.getLabel()));
        button.setTextBelowIcon();
        return button;
    }

    /**
     * @return the label centered, on several lines if it is longer than a tile
     */
    private String getWrappedLabel(String label)
    {
        String text = label == null ? "" : label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='text-align:center;width:" + TILE_LABEL_WIDTH + "px'>" + text + "</div></html>";
    }

    /**
     * Shows only the tools whose name contains a text, whatever the case and the accents
     *
     * @param text typed by the user, empty to show all the tools
     * @return the number of tools shown
     */
    public int setFilter(String text)
    {
        String filter = normalize(text);
        int count = 0;
        for (GraphToolsBarButton aButton : this.buttons)
        {
            boolean isMatching = normalize(aButton.getTool().getLabel()).contains(filter);
            aButton.setVisible(isMatching);
            if (isMatching)
            {
                count++;
            }
        }
        revalidate();
        repaint();
        return count;
    }

    private String normalize(String text)
    {
        if (text == null)
        {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }

    /**
     * Adds one button for a whole group of tools, for the narrow column shown when the side bar is reduced and the diagram has
     * too many tools for it. It shows the icon of the tool of the group used last, and opens the tools of the group, with their
     * names, beside it.
     *
     * @param category
     */
    public void addCategoryButton(GraphToolCategory category)
    {
        if (category.getTools().isEmpty())
        {
            return;
        }
        CategoryButton button = new CategoryButton(category);
        this.categoryButtons.add(button);
        addRow(button);
    }

    private void refreshCategoryButtons()
    {
        for (CategoryButton aButton : this.categoryButtons)
        {
            aButton.refresh();
        }
    }

    private void showCategoryPopup(final CategoryButton button)
    {
        GraphToolCategory category = button.getCategory();
        final JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        JLabel title = new JLabel(category.getName());
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setBorder(new EmptyBorder(POPUP_GAP, POPUP_GAP, POPUP_GAP, POPUP_GAP));
        int columns = Math.min(category.getTools().size(), POPUP_COLUMNS);
        JPanel grid = new JPanel(new GridLayout(0, columns, GRID_GAP, GRID_GAP));
        grid.setOpaque(false);
        for (final GraphTool aTool : category.getTools())
        {
            JButton choice = new JButton(getWrappedLabel(aTool.getLabel()), aTool.getIcon());
            choice.setToolTipText(aTool.getLabel());
            choice.getAccessibleContext().setAccessibleName(aTool.getLabel());
            choice.putClientProperty("JButton.buttonType", "toolBarButton");
            choice.setFocusable(false);
            choice.setVerticalTextPosition(SwingConstants.BOTTOM);
            choice.setHorizontalTextPosition(SwingConstants.CENTER);
            choice.setFont(choice.getFont().deriveFont(choice.getFont().getSize2D() - 1));
            choice.setMargin(new Insets(4, 2, 4, 2));
            choice.addActionListener(e -> {
                popup.setVisible(false);
                this.graphToolsBar.setSelectedTool(aTool);
            });
            grid.add(choice);
        }
        popup.add(title, BorderLayout.NORTH);
        popup.add(grid, BorderLayout.CENTER);
        popup.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                button.rememberClosingPress();
                // The click which opened the choice selected the button, whatever the tool
                SwingUtilities.invokeLater(GraphToolsBarPanel.this::refreshCategoryButtons);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
        popup.show(button, button.getWidth() + POPUP_GAP, 0);
    }

    /**
     * Adds a thin horizontal line between two groups of tools
     */
    public void addSeparatorLine()
    {
        addRow(new SeparatorLine());
    }

    /**
     * @return all buttons of this panel
     */
    public List<GraphToolsBarButton> getButtons()
    {
        return this.buttons;
    }

    private void addRow(Component c)
    {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = this.rowCount++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(c, gbc);
        revalidate();
    }

    private GraphToolsBarButton createButton(final GraphTool tool)
    {
        final GraphToolsBarButton button = new GraphToolsBarButton(tool);
        button.setSelected(tool.equals(this.graphToolsBar.getSelectedTool()));
        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                toolBeforePress = graphToolsBar.getSelectedTool();
                isPressCancelled = false;
                graphToolsBar.setSelectedTool(tool);
                graphToolsBar.notifyMouseEvent(tool, event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                if (isPressCancelled)
                {
                    return;
                }
                graphToolsBar.notifyMouseEvent(tool, event);
            }
        });
        button.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent event)
            {
                if (isPressCancelled)
                {
                    return;
                }
                DragScrollSupport scrollSupport = getDragScrollSupport(button);
                if (scrollSupport != null && scrollSupport.isScrollGesture())
                {
                    cancelPress(button, tool, event);
                    return;
                }
                if (scrollSupport != null && !scrollSupport.isDragLeftToComponents())
                {
                    return; // still a tap or a scroll: not yet a drag toward the diagram
                }
                graphToolsBar.notifyMouseEvent(tool, event);
            }
        });
        this.buttons.add(button);
        return button;
    }

    /**
     * @return what tells whether the finger drags the side bar to scroll it or drags the tool toward the diagram, null if the
     *         side bar can't be scrolled this way
     */
    private DragScrollSupport getDragScrollSupport(Component button)
    {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, button);
        if (scrollPane == null)
        {
            return null;
        }
        Object support = scrollPane.getClientProperty(DragScrollSupport.CLIENT_PROPERTY);
        return support instanceof DragScrollSupport ? (DragScrollSupport) support : null;
    }

    /**
     * Undoes what the press started once the gesture turns out to be a scroll: ends the drag toward the diagram in the side bar,
     * which removes the node it prepared, and selects the former tool again
     */
    private void cancelPress(Component button, GraphTool tool, MouseEvent event)
    {
        this.isPressCancelled = true;
        MouseEvent release = new MouseEvent(button, MouseEvent.MOUSE_RELEASED, event.getWhen(), event.getModifiersEx(), event.getX(),
                event.getY(), 1, false, MouseEvent.BUTTON1);
        this.graphToolsBar.notifyMouseEvent(tool, release);
        if (this.toolBeforePress != null)
        {
            this.graphToolsBar.setSelectedTool(this.toolBeforePress);
        }
    }

    /**
     * Title of a group of tools, smaller and lighter than the tools, followed by a thin line up to the right edge
     */
    private static class HeaderLabel extends JLabel
    {
        HeaderLabel(String text)
        {
            super(text);
            setBorder(new EmptyBorder(HEADER_TOP_GAP, HEADER_SIDE_GAP, HEADER_BOTTOM_GAP, HEADER_SIDE_GAP));
            updateHeaderFont();
        }

        @Override
        public void updateUI()
        {
            super.updateUI();
            updateHeaderFont();
        }

        private void updateHeaderFont()
        {
            ITheme theme = ThemeManager.getInstance().getTheme();
            if (theme != null && theme.getToggleButtonFont() != null)
            {
                Font font = theme.getToggleButtonFont();
                setFont(font.deriveFont(Font.PLAIN, font.getSize2D() - 1f));
            }
        }

        @Override
        public Color getForeground()
        {
            ITheme theme = ThemeManager.getInstance().getTheme();
            return theme != null ? theme.getContrastColor(HEADER_ALPHA) : super.getForeground();
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            ITheme theme = ThemeManager.getInstance().getTheme();
            if (theme == null)
            {
                return;
            }
            Insets insets = getInsets();
            FontMetrics metrics = g.getFontMetrics(getFont());
            int lineStart = insets.left + metrics.stringWidth(getText()) + HEADER_LINE_GAP;
            int lineEnd = getWidth() - insets.right;
            if (lineStart >= lineEnd)
            {
                return;
            }
            int textTop = insets.top + (getHeight() - insets.top - insets.bottom - metrics.getHeight()) / 2;
            int lineY = textTop + metrics.getAscent() - metrics.getAscent() * 2 / 5;
            g.setColor(theme.getContrastColor(HEADER_LINE_ALPHA));
            g.drawLine(lineStart, lineY, lineEnd, lineY);
        }
    }

    /**
     * Header of a group of tools: a click shows or hides the tools. It gives the number of tools, as they may be hidden.
     */
    private class CategoryHeader extends JPanel
    {
        CategoryHeader(String title, int toolCount, final Component tools)
        {
            super(new BorderLayout());
            this.tools = tools;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            this.chevronLabel.setBorder(new EmptyBorder(HEADER_TOP_GAP, HEADER_SIDE_GAP, HEADER_BOTTOM_GAP, 0));
            HeaderLabel countLabel = new HeaderLabel(String.valueOf(toolCount));
            countLabel.setBorder(new EmptyBorder(HEADER_TOP_GAP, 0, HEADER_BOTTOM_GAP, HEADER_SIDE_GAP));
            add(this.chevronLabel, BorderLayout.WEST);
            add(new HeaderLabel(title), BorderLayout.CENTER);
            add(countLabel, BorderLayout.EAST);
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    CategoryHeader.this.tools.setVisible(!CategoryHeader.this.tools.isVisible());
                    refreshChevron();
                    GraphToolsBarPanel.this.revalidate();
                    GraphToolsBarPanel.this.repaint();
                }
            });
            refreshChevron();
        }

        private void refreshChevron()
        {
            this.chevronLabel.setIcon(this.tools.isVisible() ? CHEVRON_DOWN : CHEVRON_RIGHT);
        }

        private final Component tools;
        private final JLabel chevronLabel = new JLabel();
    }

    /**
     * Button of a group of tools in the reduced side bar, marked by a small corner as it opens a choice
     */
    private class CategoryButton extends GraphToolsBarButton
    {
        CategoryButton(GraphToolCategory category)
        {
            super(category.getTools().get(0));
            this.category = category;
            this.shownTool = category.getTools().get(0);
            setTextVisible(false);
            setPreferredSize(ICON_BUTTON_SIZE);
            setToolTipText(category.getName());
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    // The choice closes on this press before the button gets it: the click that follows must not open it again
                    CategoryButton.this.isClickClosingChoice = e.getWhen() == CategoryButton.this.choiceClosingPressTime;
                    CategoryButton.this.isDraggingTool = false;
                    CategoryButton.this.isDragCancelled = false;
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    if (CategoryButton.this.isDraggingTool)
                    {
                        graphToolsBar.notifyMouseEvent(CategoryButton.this.shownTool, e);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (!CategoryButton.this.isClickClosingChoice && !CategoryButton.this.isDraggingTool)
                    {
                        showCategoryPopup(CategoryButton.this);
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    dragShownTool(e);
                }
            });
            refresh();
        }

        /**
         * Drags the tool shown by the button toward the diagram, like a tool button of the open side bar. The press is sent only
         * once the gesture turns out to be such a drag, so that a click which opens the choice leaves the selected tool as it is.
         */
        private void dragShownTool(MouseEvent event)
        {
            if (this.isDragCancelled)
            {
                return;
            }
            if (!this.isDraggingTool)
            {
                DragScrollSupport scrollSupport = getDragScrollSupport(this);
                if (scrollSupport != null && scrollSupport.isScrollGesture())
                {
                    this.isDragCancelled = true;
                    return;
                }
                if (scrollSupport != null && !scrollSupport.isDragLeftToComponents())
                {
                    return; // still a tap or a scroll: not yet a drag toward the diagram
                }
                this.isDraggingTool = true;
                graphToolsBar.setSelectedTool(this.shownTool);
                MouseEvent press = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, event.getWhen(), event.getModifiersEx(),
                        event.getX(), event.getY(), 1, false, MouseEvent.BUTTON1);
                graphToolsBar.notifyMouseEvent(this.shownTool, press);
            }
            graphToolsBar.notifyMouseEvent(this.shownTool, event);
        }

        /**
         * Remembers the time of the press that closes the choice, if it is made on this button
         */
        void rememberClosingPress()
        {
            AWTEvent event = EventQueue.getCurrentEvent();
            if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_PRESSED)
            {
                MouseEvent mouseEvent = (MouseEvent) event;
                if (SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), this))
                {
                    this.choiceClosingPressTime = mouseEvent.getWhen();
                }
            }
        }

        GraphToolCategory getCategory()
        {
            return this.category;
        }

        /**
         * Selected while the selected tool is one of the group, whose icon it then takes
         */
        void refresh()
        {
            GraphTool selectedTool = graphToolsBar.getSelectedTool();
            boolean isSelected = selectedTool != null && this.category.getTools().contains(selectedTool);
            if (isSelected)
            {
                this.shownTool = selectedTool;
                setIcon(selectedTool.getIcon());
            }
            setSelected(isSelected);
        }

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            ITheme theme = ThemeManager.getInstance().getTheme();
            if (theme == null)
            {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(theme.getToggleButtonSelectedBorderColor());
            int right = getWidth() - CORNER_MARK_GAP;
            int bottom = getHeight() - CORNER_MARK_GAP;
            g2.fillPolygon(new int[] { right, right, right - CORNER_MARK_SIZE }, new int[] { bottom - CORNER_MARK_SIZE, bottom,
                    bottom }, 3);
            g2.dispose();
        }

        /** Time of the last press on this button which closed its choice */
        private long choiceClosingPressTime = -1;
        /** Whether the current click closed the choice, so must not open it again */
        private boolean isClickClosingChoice = false;
        /** Whether the current gesture drags the shown tool toward the diagram */
        private boolean isDraggingTool = false;
        /** Whether the current gesture turned out to scroll the side bar */
        private boolean isDragCancelled = false;
        private final GraphToolCategory category;
        /** Tool whose icon the button shows: the one of the group used last, else the first one */
        private GraphTool shownTool;
    }

    /**
     * Line between two groups of icons, in the color of the group headers
     */
    private static class SeparatorLine extends JComponent
    {
        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(ICON_BUTTON_SIZE.width, SEPARATOR_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            ITheme theme = ThemeManager.getInstance().getTheme();
            if (theme == null)
            {
                return;
            }
            g.setColor(theme.getContrastColor(HEADER_LINE_ALPHA));
            int y = getHeight() / 2;
            g.drawLine(SEPARATOR_SIDE_GAP, y, getWidth() - 1 - SEPARATOR_SIDE_GAP, y);
        }
    }

    private static final Dimension ICON_BUTTON_SIZE = new Dimension(40, 36);
    private static final int SEPARATOR_HEIGHT = 9;
    private static final int SEPARATOR_SIDE_GAP = 6;
    /** Tiles per row in a group of tools */
    private static final int TILE_COLUMNS = 3;
    /** Width of a tile label, in pixels: a longer name goes on several lines */
    private static final int TILE_LABEL_WIDTH = 60;
    /** Tools per row in the choice opened by the button of a group: it stays narrower than a phone screen */
    private static final int POPUP_COLUMNS = 4;
    private static final int POPUP_GAP = 6;
    private static final int CORNER_MARK_SIZE = 6;
    private static final int CORNER_MARK_GAP = 3;
    private static final int CHEVRON_SIZE = 12;
    private static final Icon CHEVRON_RIGHT = new ShapeIcon(ToolShapes.CHEVRON_RIGHT, CHEVRON_SIZE);
    private static final Icon CHEVRON_DOWN = new ShapeIcon(
            AffineTransform.getRotateInstance(Math.PI / 2, 12, 12).createTransformedShape(ToolShapes.CHEVRON_RIGHT), CHEVRON_SIZE);

    private final GraphToolsBar graphToolsBar;
    private final List<GraphToolsBarButton> buttons = new ArrayList<GraphToolsBarButton>();
    private final List<CategoryButton> categoryButtons = new ArrayList<CategoryButton>();
    /** Grid filled by addTiles(), null until its first call */
    private JPanel tileGrid;
    private int rowCount = 0;
    /** Tool selected before the current press on a button, selected again if the gesture scrolls the side bar */
    private GraphTool toolBeforePress;
    /** True once the current gesture turned out to be a scroll: the button ignores its last events */
    private boolean isPressCancelled = false;

}
