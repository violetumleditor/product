/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.framework.swingextension;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicPanelUI;

/**
 * UI for CustomToggleButton
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class CustomToggleButtonGraphToolUI extends BasicPanelUI
{

    /**
     * Default constructor
     * 
     * @param selectedColor
     * @param unselectedColor
     */
    public CustomToggleButtonGraphToolUI(Color selectedColor, Color unselectedColor)
    {
        this.unselectedColor = unselectedColor;
        this.selectedColor = selectedColor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.plaf.basic.BasicPanelUI#installDefaults(javax.swing.JPanel)
     */
    protected void installDefaults(JPanel p)
    {
        p.setOpaque(true);

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.plaf.ComponentUI#paint(java.awt.Graphics, javax.swing.JComponent)
     */
    public void paint(Graphics g, JComponent c)
    {
        if (c instanceof CustomToggleButton)
        {
            CustomToggleButton button = (CustomToggleButton) c;
            paintUnselectedBg(g, c);
            if (button.isSelected())
            {
                paintPill(g, c, selectedColor);
            }
            else if (button.isRollover())
            {
                paintPill(g, c, ROLLOVER_OVERLAY_COLOR);
            }
            if (button.isSeparatorAbove())
            {
                g.setColor(SEPARATOR_COLOR);
                g.drawLine(SEPARATOR_SIDE_GAP, 0, c.getWidth() - SEPARATOR_SIDE_GAP, 0);
            }
        }
        super.paint(g, c);
    }

    /**
     * Paints the rounded flat background of a selected or hovered button
     *
     * @param g
     * @param c
     * @param color
     */
    private void paintPill(Graphics g, JComponent c, Color color)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillRoundRect(0, PILL_VERTICAL_GAP, c.getWidth(), c.getHeight() - 2 * PILL_VERTICAL_GAP, PILL_ARC, PILL_ARC);
        g2.dispose();
    }

    /**
     * Paints background for state set as 'NOT selected'
     * 
     * @param g
     * @param c
     */
    private void paintUnselectedBg(Graphics g, JComponent c)
    {
        Graphics2D g2 = (Graphics2D) g;
        Color currentColor = g2.getColor();
        g2.setColor(unselectedColor);
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());
        g2.setColor(currentColor);
    }

    private static final int PILL_ARC = 10;
    private static final int PILL_VERTICAL_GAP = 1;
    private static final int SEPARATOR_SIDE_GAP = 6;
    private static final Color SEPARATOR_COLOR = new Color(0, 0, 0, 28);
    private static final Color ROLLOVER_OVERLAY_COLOR = new Color(128, 128, 128, 40);

    private Color unselectedColor;
    private Color selectedColor;

}
