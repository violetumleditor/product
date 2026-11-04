package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import com.horstmann.violet.framework.theme.ThemeManager;

public class GraphToolsBarPanel extends JPanel
{

    private static final int NAVIGATION_EDGE_HEIGHT = 14;

    public GraphToolsBarPanel(GraphToolsBar umlToolsPanel)
    {
        super();
        this.graphToolsPanel = umlToolsPanel;
        this.nodeButtons = getToggleButtons(this.graphToolsPanel.getNodeTools());
        this.edgeButtons = getToggleButtons(this.graphToolsPanel.getEdgeTools());

        this.buttonsPanel = new JPanel(new java.awt.GridLayout(0, 1));
        this.buttonsPanel.setOpaque(true);
        this.buttonsPanel.setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
        this.buttonsPanel.addMouseWheelListener(getScrollWheelListener());

        this.topIndicatorPanel = new NavigationIndicatorPanel(true);
        this.bottomIndicatorPanel = new NavigationIndicatorPanel(false);

        setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
        setLayout(new BorderLayout());
        add(this.topIndicatorPanel, BorderLayout.NORTH);
        add(this.buttonsPanel, BorderLayout.CENTER);
        add(this.bottomIndicatorPanel, BorderLayout.SOUTH);

        addMouseWheelListener(getScrollWheelListener());
        this.graphToolsPanel.addListener(getGraphToolsPanelListener());

        refreshVisibleButtons();
    }

    private IGraphToolsBarListener getGraphToolsPanelListener()
    {
        if (this.listener == null)
        {
            this.listener = new IGraphToolsBarListener()
            {
                @Override
                public void toolSelectionChanged(GraphTool selectedTool)
                {
                    for (GraphToolsBarButton aButton : nodeButtons)
                    {
                        if (aButton.getTool().equals(selectedTool))
                        {
                            setSelectedButton(aButton);
                            ensureButtonVisible(aButton);
                            return;
                        }
                    }
                    for (GraphToolsBarButton aButton : edgeButtons)
                    {
                        if (aButton.getTool().equals(selectedTool))
                        {
                            setSelectedButton(aButton);
                            ensureButtonVisible(aButton);
                            return;
                        }
                    }
                }
            };
        }
        return this.listener;
    }

    private List<GraphToolsBarButton> getToggleButtons(List<GraphTool> tools)
    {
        List<GraphToolsBarButton> buttons = new ArrayList<GraphToolsBarButton>();
        for (GraphTool aTool : tools)
        {
            final GraphToolsBarButton button = new GraphToolsBarButton(aTool);
            initializeButton(button);
            buttons.add(button);
        }
        return buttons;
    }

    private void initializeButton(final GraphToolsBarButton button)
    {
        button.addMouseWheelListener(getScrollWheelListener());
        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                setSelectedButton(button);
                notifyMouseEvent(button, event);
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                notifyMouseEvent(button, event);
            }
        });
        button.addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent event)
            {
                notifyMouseEvent(button, event);
            }
        });
    }

    private GraphToolsBarButton getSelectedButton()
    {
        for (GraphToolsBarButton button : this.nodeButtons)
        {
            if (button.isSelected())
            {
                return button;
            }
        }
        for (GraphToolsBarButton button : this.edgeButtons)
        {
            if (button.isSelected())
            {
                return button;
            }
        }
        return this.nodeButtons.get(0);
    }

    public List<GraphToolsBarButton> getNodeButtons()
    {
        return this.nodeButtons;
    }

    public List<GraphToolsBarButton> getEdgeButtons()
    {
        return this.edgeButtons;
    }

    public JPanel getButtonsPanel()
    {
        return this.buttonsPanel;
    }

    public void selectNextButton()
    {
        int nextPos = 0;
        GraphToolsBarButton selectedButton = getSelectedButton();
        int posForNodes = this.nodeButtons.indexOf(selectedButton);
        if (posForNodes >= 0)
        {
            nextPos = posForNodes + 1;
            if (nextPos < this.nodeButtons.size())
            {
                setSelectedButton(this.nodeButtons.get(nextPos));
            }
            if (nextPos >= this.nodeButtons.size() && this.edgeButtons.size() > 0)
            {
                setSelectedButton(this.edgeButtons.get(0));
            }
            return;
        }
        int posForEdges = this.edgeButtons.indexOf(selectedButton);
        if (posForEdges >= 0)
        {
            nextPos = posForEdges + 1;
            if (nextPos < this.edgeButtons.size())
            {
                setSelectedButton(this.edgeButtons.get(nextPos));
            }
        }
    }

    public void selectPreviousButton()
    {
        int previousPos = 0;
        GraphToolsBarButton selectedButton = getSelectedButton();
        int posForNodes = this.nodeButtons.indexOf(selectedButton);
        if (posForNodes >= 0)
        {
            previousPos = posForNodes - 1;
            if (previousPos >= 0)
            {
                setSelectedButton(this.nodeButtons.get(previousPos));
            }
            return;
        }
        int posForEdges = this.edgeButtons.indexOf(selectedButton);
        if (posForEdges >= 0)
        {
            previousPos = posForEdges - 1;
            if (previousPos >= 0)
            {
                setSelectedButton(this.edgeButtons.get(previousPos));
            }
            if (previousPos < 0 && this.nodeButtons.size() > 0)
            {
                setSelectedButton(this.nodeButtons.get(this.nodeButtons.size() - 1));
            }
        }
    }

    private void setSelectedButton(GraphToolsBarButton selectedButton)
    {
        GraphToolsBarButton previousSelectedButton = getSelectedButton();
        for (GraphToolsBarButton button : this.nodeButtons)
        {
            if (button != selectedButton)
            {
                button.setSelected(false);
            }
            if (button == selectedButton)
            {
                button.setSelected(true);
                int pos = this.nodeButtons.indexOf(button);
                this.graphToolsPanel.setSelectedTool(this.graphToolsPanel.getNodeTools().get(pos));
            }
        }
        for (GraphToolsBarButton button : this.edgeButtons)
        {
            if (button != selectedButton)
            {
                button.setSelected(false);
            }
            if (button == selectedButton)
            {
                button.setSelected(true);
                int pos = this.edgeButtons.indexOf(button);
                this.graphToolsPanel.setSelectedTool(this.graphToolsPanel.getEdgeTools().get(pos));
            }
        }
        if (previousSelectedButton != null)
        {
            previousSelectedButton.repaint();
        }
        selectedButton.repaint();
        revalidate();
        repaint();
    }

    private void notifyMouseEvent(GraphToolsBarButton selectedButton, MouseEvent event)
    {
        for (GraphToolsBarButton button : this.nodeButtons)
        {
            if (button == selectedButton)
            {
                int pos = this.nodeButtons.indexOf(button);
                this.graphToolsPanel.notifyMouseEvent(this.graphToolsPanel.getNodeTools().get(pos), event);
            }
        }
        for (GraphToolsBarButton button : this.edgeButtons)
        {
            if (button == selectedButton)
            {
                int pos = this.edgeButtons.indexOf(button);
                this.graphToolsPanel.notifyMouseEvent(this.graphToolsPanel.getEdgeTools().get(pos), event);
            }
        }
    }

    private MouseWheelListener getScrollWheelListener()
    {
        if (this.scrollWheelListener == null)
        {
            this.scrollWheelListener = new MouseWheelListener()
            {
                @Override
                public void mouseWheelMoved(MouseWheelEvent event)
                {
                    boolean isCtrl = (event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                    if (isCtrl)
                    {
                        return;
                    }
                    if (event.getWheelRotation() > 0)
                    {
                        selectNextButton();
                    }
                    if (event.getWheelRotation() < 0)
                    {
                        selectPreviousButton();
                    }
                    event.consume();
                }
            };
        }
        return this.scrollWheelListener;
    }

    private void scrollUp()
    {
        setFirstVisibleIndex(this.firstVisibleIndex - 1);
    }

    private void scrollDown()
    {
        setFirstVisibleIndex(this.firstVisibleIndex + 1);
    }

    private void setFirstVisibleIndex(int newIndex)
    {
        int boundedIndex = Math.max(0, Math.min(newIndex, getMaximumFirstVisibleIndex()));
        if (boundedIndex == this.firstVisibleIndex)
        {
            return;
        }
        this.firstVisibleIndex = boundedIndex;
        refreshVisibleButtons();
    }

    private void ensureButtonVisible(GraphToolsBarButton button)
    {
        int index = getAllButtons().indexOf(button);
        if (index < 0)
        {
            return;
        }
        if (index < this.firstVisibleIndex)
        {
            setFirstVisibleIndex(index);
            return;
        }
        int lastVisibleIndex = this.firstVisibleIndex + GraphToolsBar.MAX_VISIBLE_ITEMS - 1;
        if (index > lastVisibleIndex)
        {
            setFirstVisibleIndex(index - GraphToolsBar.MAX_VISIBLE_ITEMS + 1);
        }
    }

    private void refreshVisibleButtons()
    {
        this.buttonsPanel.removeAll();
        List<GraphToolsBarButton> allButtons = getAllButtons();
        int start = Math.min(this.firstVisibleIndex, allButtons.size());
        int end = Math.min(start + GraphToolsBar.MAX_VISIBLE_ITEMS, allButtons.size());
        for (int index = start; index < end; index++)
        {
            this.buttonsPanel.add(allButtons.get(index));
        }
        this.buttonsPanel.revalidate();
        this.buttonsPanel.repaint();
        updateIndicatorVisibility();
        revalidate();
        repaint();
    }

    private void updateIndicatorVisibility()
    {
        this.topIndicatorPanel.setIndicatorVisible(hasToolBeforeVisibleRange());
        this.bottomIndicatorPanel.setIndicatorVisible(hasToolAfterVisibleRange());
    }

    private List<GraphToolsBarButton> getAllButtons()
    {
        if (this.allButtons == null)
        {
            this.allButtons = new ArrayList<GraphToolsBarButton>(this.nodeButtons.size() + this.edgeButtons.size());
            this.allButtons.addAll(this.nodeButtons);
            this.allButtons.addAll(this.edgeButtons);
        }
        return this.allButtons;
    }

    private int getMaximumFirstVisibleIndex()
    {
        return Math.max(0, getAllButtons().size() - GraphToolsBar.MAX_VISIBLE_ITEMS);
    }

    private boolean hasToolBeforeVisibleRange()
    {
        return getVisibleStartIndex() > 0;
    }

    private boolean hasToolAfterVisibleRange()
    {
        return getVisibleEndExclusiveIndex() < getAllButtons().size();
    }

    private int getVisibleStartIndex()
    {
        return Math.min(this.firstVisibleIndex, getAllButtons().size());
    }

    private int getVisibleEndExclusiveIndex()
    {
        return Math.min(getVisibleStartIndex() + GraphToolsBar.MAX_VISIBLE_ITEMS, getAllButtons().size());
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = super.getPreferredSize();
        int visibleButtonCount = Math.min(getAllButtons().size(), GraphToolsBar.MAX_VISIBLE_ITEMS);
        int buttonHeight = getReferenceButtonHeight();
        int buttonWidth = getReferenceButtonWidth();
        int height = visibleButtonCount * buttonHeight + (NAVIGATION_EDGE_HEIGHT * 2);
        int width = Math.max(size.width, buttonWidth);
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMaximumSize()
    {
        Dimension preferredSize = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, preferredSize.height);
    }

    private int getReferenceButtonHeight()
    {
        List<GraphToolsBarButton> allButtons = getAllButtons();
        if (allButtons.isEmpty())
        {
            return 0;
        }
        return allButtons.get(0).getPreferredSize().height;
    }

    private int getReferenceButtonWidth()
    {
        int width = 0;
        for (GraphToolsBarButton button : getAllButtons())
        {
            width = Math.max(width, button.getPreferredSize().width);
        }
        return width;
    }

    private class NavigationIndicatorPanel extends JPanel
    {
        private final boolean top;
        private boolean indicatorVisible;

        NavigationIndicatorPanel(boolean top)
        {
            this.top = top;
            setOpaque(false);
            addMouseWheelListener(getScrollWheelListener());
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent event)
                {
                    if (!indicatorVisible)
                    {
                        return;
                    }
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event)
                {
                    if (!hovered)
                    {
                        return;
                    }
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent event)
                {
                    if (!indicatorVisible)
                    {
                        return;
                    }
                    if (NavigationIndicatorPanel.this.top)
                    {
                        scrollUp();
                    }
                    else
                    {
                        scrollDown();
                    }
                }
            });
        }

        void setIndicatorVisible(boolean visible)
        {
            if (this.indicatorVisible == visible)
            {
                return;
            }
            this.indicatorVisible = visible;
            if (!visible)
            {
                this.hovered = false;
            }
            repaint();
        }

        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(10, NAVIGATION_EDGE_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (!this.indicatorVisible)
            {
                return;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color indicatorColor = getHighContrastIndicatorColor();
                g2.setColor(indicatorColor);
                paintFilledTriangle(g2, this.top);
            }
            finally
            {
                g2.dispose();
            }
        }

        private Color getHighContrastIndicatorColor()
        {
            Color background = GraphToolsBarPanel.this.getBackground();
            if (background == null)
            {
                return Color.DARK_GRAY;
            }
            int luminance = (background.getRed() * 299 + background.getGreen() * 587 + background.getBlue() * 114) / 1000;
            return luminance >= 140 ? Color.DARK_GRAY : Color.WHITE;
        }

        private void paintFilledTriangle(Graphics2D graphics2D, boolean isUp)
        {
            int triangleWidth = Math.max(12, Math.min(getWidth() - 34, 24));
            int triangleHeight = Math.max(5, NAVIGATION_EDGE_HEIGHT - 7);
            int centerX = getWidth() / 2;
            int halfWidth = triangleWidth / 2;
            int centerY = getHeight() / 2;

            Path2D triangle = new Path2D.Double();
            if (isUp)
            {
                int apexY = centerY - triangleHeight / 2;
                int baseY = centerY + triangleHeight / 2;
                triangle.moveTo(centerX, apexY);
                triangle.lineTo(centerX - halfWidth, baseY);
                triangle.lineTo(centerX + halfWidth, baseY);
            }
            else
            {
                int apexY = centerY + triangleHeight / 2;
                int baseY = centerY - triangleHeight / 2;
                triangle.moveTo(centerX, apexY);
                triangle.lineTo(centerX - halfWidth, baseY);
                triangle.lineTo(centerX + halfWidth, baseY);
            }
            triangle.closePath();
            graphics2D.fill(triangle);
        }

        private boolean hovered;
    }

    private IGraphToolsBarListener listener;
    private GraphToolsBar graphToolsPanel;
    private List<GraphToolsBarButton> nodeButtons;
    private List<GraphToolsBarButton> edgeButtons;
    private List<GraphToolsBarButton> allButtons;
    private JPanel buttonsPanel;
    private NavigationIndicatorPanel topIndicatorPanel;
    private NavigationIndicatorPanel bottomIndicatorPanel;
    private MouseWheelListener scrollWheelListener;
    private int firstVisibleIndex;
}
