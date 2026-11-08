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

package com.horstmann.violet.framework.propertyeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * A panel drawn like a text field (background, thin rounded border), holding small buttons in the property editor. Colors are read
 * when painting, so that it follows the theme.
 */
public class RoundedFieldPanel extends JPanel
{
    /**
     * @param layout of the content
     * @param padding between the border and the content
     */
    public RoundedFieldPanel(LayoutManager layout, int padding)
    {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC);
            Color background = UIManager.getColor("TextField.background");
            if (background != null)
            {
                g2.setColor(background);
                g2.fill(shape);
            }
            Color border = UIManager.getColor("Component.borderColor");
            g2.setColor(border != null ? border : Color.GRAY);
            g2.setStroke(new BasicStroke(1));
            g2.draw(shape);
        }
        finally
        {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private static final int ARC = 8;
}
