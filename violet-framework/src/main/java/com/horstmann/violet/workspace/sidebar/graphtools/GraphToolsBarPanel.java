package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
                graphToolsBar.setSelectedTool(tool);
                graphToolsBar.notifyMouseEvent(tool, event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                graphToolsBar.notifyMouseEvent(tool, event);
            }
        });
        button.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent event)
            {
                graphToolsBar.notifyMouseEvent(tool, event);
            }
        });
        this.buttons.add(button);
        return button;
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

    private final GraphToolsBar graphToolsBar;
    private final List<GraphToolsBarButton> buttons = new ArrayList<GraphToolsBarButton>();
    private int rowCount = 0;

}
