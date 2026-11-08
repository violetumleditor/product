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

package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * One field made of a formatting toolbar and the text it formats: a single rounded border, the toolbar as a header on the window
 * color, a hairline under it, and the border in the accent color while the text has the focus. Colors are read when painting, so
 * that it follows the theme.
 */
class RichTextBox extends JPanel
{
    /**
     * @param toolbar header, not opaque
     * @param content text area, under the toolbar
     * @param focusOwner component whose focus colors the border
     */
    RichTextBox(JComponent toolbar, JComponent content, final JComponent focusOwner)
    {
        super(new BorderLayout());
        this.toolbar = toolbar;
        this.focusOwner = focusOwner;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        toolbar.setOpaque(false);
        add(toolbar, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        focusOwner.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getColor("Panel.background", getBackground()));
            g2.fill(getShape());
            int y = toolbar.getY() + toolbar.getHeight() - 1;
            g2.setColor(getColor("Component.borderColor", Color.GRAY));
            g2.fillRect(0, y, getWidth(), 1);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Paints the border over the children, so that their corners are rounded too.
     */
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D shape = getShape();
            Area corners = new Area(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
            corners.subtract(new Area(shape));
            g2.setColor(getParentBackground());
            g2.fill(corners);
            boolean isFocused = focusOwner.isFocusOwner();
            g2.setColor(isFocused ? getColor("Component.focusColor", Color.BLUE) : getColor("Component.borderColor", Color.GRAY));
            g2.setStroke(new BasicStroke(1));
            g2.draw(shape);
        }
        finally
        {
            g2.dispose();
        }
    }

    private RoundRectangle2D getShape()
    {
        return new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, ARC, ARC);
    }

    private Color getParentBackground()
    {
        for (Container parent = getParent(); parent != null; parent = parent.getParent())
        {
            if (parent.isOpaque() || !(parent instanceof JComponent))
            {
                return parent.getBackground();
            }
        }
        return getColor("Panel.background", getBackground());
    }

    private static Color getColor(String key, Color fallback)
    {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    private final Component toolbar;
    private final JComponent focusOwner;

    private static final int ARC = 8;
}
