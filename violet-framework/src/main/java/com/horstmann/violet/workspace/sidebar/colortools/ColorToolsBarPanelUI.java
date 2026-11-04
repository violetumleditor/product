package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

public class ColorToolsBarPanelUI extends PanelUI
{

    /**
     * Default constructor
     * 
     * @param colorToolsPanel
     */
    public ColorToolsBarPanelUI(ColorToolsBarPanel colorToolsPanel)
    {
        this.colorToolsPanel = colorToolsPanel;
    }

    @Override
    public void installUI(JComponent c)
    {
        c.removeAll();
        this.colorToolsPanel.setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
        this.colorToolsPanel.add(getPanel());
    }

    /**
     * @return the main panel
     */
    private JPanel getPanel()
    {
        if (this.panel == null)
        {
            this.panel = new JPanel();
            this.panel.setOpaque(false);
            this.panel.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.panel.setPreferredSize(new Dimension(215, 100));
            FlowLayout layout = new FlowLayout(FlowLayout.LEFT, 10, 10);
            this.panel.setLayout(layout);
            for (ColorTool aColorButton : getColorToolList())
            {
                this.panel.add(aColorButton);
            }
        }
        return this.panel;
    }

    private List<ColorTool> getColorToolList()
    {
        if (this.colorToolList == null)
        {
            this.colorToolList = new ArrayList<ColorTool>();
            for (ColorChoice aChoice : ColorChoice.PALETTE)
            {
                ColorTool aColorTool = getColorTool(aChoice);
                this.colorToolList.add(aColorTool);
            }
        }
        return this.colorToolList;
    }

    private ColorTool getColorTool(final ColorChoice colorChoice)
    {
        final ColorTool aColorTool = new ColorTool(colorChoice);
        aColorTool.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                aColorTool.setBorderPaintable(true);
                aColorTool.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
            	aColorTool.setBorderPaintable(false);
            	aColorTool.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorToolsPanel.fireColorChoiceChanged(colorChoice);
            }
        });
        return aColorTool;
    }

    private class ColorTool extends JLabel
    {

        public ColorTool(ColorChoice colorChoice)
        {
            this.colorChoice = colorChoice;
            setPreferredSize(new Dimension(20, 20));
        }

        @Override
        public void paint(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;
            Color oldColor = g2.getColor();
            Color backgroundColor = colorChoice.getBackgroundColor();
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (this.isBorderPaintable)
            {
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            g2.setColor(oldColor);
        }

        public void setBorderPaintable(boolean isBorderPaintable)
        {
            this.isBorderPaintable = isBorderPaintable;
        }

        private boolean isBorderPaintable = false;
        private ColorChoice colorChoice;
    }
    

    /**
     * Component(s panel
     */
    private JPanel panel;

    private ColorToolsBarPanel colorToolsPanel;

    private List<ColorTool> colorToolList;
    
}
