package com.horstmann.violet.workspace.sidebar.thicknesstools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.PanelUI;

import com.horstmann.violet.framework.theme.ThemeManager;

public class ThicknessToolsBarPanelUI extends PanelUI
{

    public ThicknessToolsBarPanelUI(ThicknessToolsBarPanel thicknessToolsPanel)
    {
        this.thicknessToolsPanel = thicknessToolsPanel;
    }

    @Override
    public void installUI(JComponent c)
    {
        c.removeAll();
        this.thicknessToolsPanel.setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
        this.thicknessToolsPanel.add(getPanel());
    }

    private JPanel getPanel()
    {
        if (this.panel == null)
        {
            this.panel = new JPanel();
            this.panel.setOpaque(false);
            this.panel.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.panel.setPreferredSize(new Dimension(215, 32));
            FlowLayout layout = new FlowLayout(FlowLayout.LEFT, 3, 3);
            this.panel.setLayout(layout);
            for (ThicknessTool aTool : getThicknessToolList())
            {
                this.panel.add(aTool);
            }
        }
        return this.panel;
    }

    private List<ThicknessTool> getThicknessToolList()
    {
        if (this.thicknessToolList == null)
        {
            this.thicknessToolList = new ArrayList<ThicknessTool>();
            for (int thickness = ThicknessToolsBarPanel.MIN_THICKNESS; thickness <= ThicknessToolsBarPanel.MAX_THICKNESS; thickness++)
            {
                ThicknessTool aTool = getThicknessTool(thickness);
                this.thicknessToolList.add(aTool);
            }
        }
        return this.thicknessToolList;
    }

    private ThicknessTool getThicknessTool(final int thickness)
    {
        final ThicknessTool aTool = new ThicknessTool(thickness);
        aTool.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                aTool.setHovered(true);
                aTool.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                aTool.setHovered(false);
                aTool.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                thicknessToolsPanel.fireThicknessChanged(thickness);
            }
        });
        return aTool;
    }

    private class ThicknessTool extends JLabel
    {
        private static final int BAR_HEIGHT = 16;
        private static final int TOOL_HEIGHT = 26;
        private static final int TOOL_MAX_WIDTH = 18;
        private static final int CORNER_ARC = 8;

        public ThicknessTool(int thickness)
        {
            this.thickness = thickness;
            setPreferredSize(new Dimension(TOOL_MAX_WIDTH, TOOL_HEIGHT));
        }

        @Override
        public void paint(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();

            // Center the vertical bar
            int barWidth = this.thickness;
            int x = (getWidth() - barWidth) / 2;
            int y = (getHeight() - BAR_HEIGHT) / 2;

            // Draw the vertical bar (black)
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, barWidth, BAR_HEIGHT);

            // Draw hover border
            if (this.isHovered)
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getInstance().getTheme().getToggleButtonSelectedBorderColor());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_ARC, CORNER_ARC);
            }

            g2.dispose();
        }

        public void setHovered(boolean isHovered)
        {
            this.isHovered = isHovered;
        }

        private boolean isHovered = false;
        private int thickness;
    }

    private JPanel panel;
    private ThicknessToolsBarPanel thicknessToolsPanel;
    private List<ThicknessTool> thicknessToolList;
}
