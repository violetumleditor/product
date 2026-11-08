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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Supplier;

import javax.swing.Icon;
import javax.swing.text.StyleConstants;

/**
 * Outline icons of the text formatting toolbar, drawn on a 24x24 grid like the menu icons, in the color of their button (the accent
 * when selected), so that they stay sharp and follow the theme.
 */
final class TextFormatIcons
{
    private TextFormatIcons()
    {
    }

    static Icon bold()
    {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(7, 5);
        path.lineTo(12.5, 5);
        path.append(new Arc2D.Double(9, 5, 7, 7, 90, -180, Arc2D.OPEN), true);
        path.lineTo(7, 12);
        path.closePath();
        path.moveTo(7, 12);
        path.lineTo(13.5, 12);
        path.append(new Arc2D.Double(10, 12, 7, 7, 90, -180, Arc2D.OPEN), true);
        path.lineTo(7, 19);
        path.closePath();
        return new FormatIcon(path, 2.6f);
    }

    static Icon italic()
    {
        Path2D.Double path = new Path2D.Double();
        line(path, 19, 4, 10, 4);
        line(path, 14, 20, 5, 20);
        line(path, 15, 4, 9, 20);
        return new FormatIcon(path, STROKE);
    }

    static Icon underline()
    {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(7, 4);
        path.lineTo(7, 10);
        path.append(new Arc2D.Double(7, 5, 10, 10, 180, 180, Arc2D.OPEN), true);
        path.lineTo(17, 4);
        line(path, 5, 20, 19, 20);
        return new FormatIcon(path, STROKE);
    }

    /**
     * @param alignment StyleConstants.ALIGN_LEFT, ALIGN_CENTER or ALIGN_RIGHT
     * @return lines of text aligned as shown by their ends
     */
    static Icon alignment(int alignment)
    {
        double[][] lines;
        if (alignment == StyleConstants.ALIGN_CENTER) lines = new double[][] { { 3, 21 }, { 7, 17 }, { 5, 19 } };
        else if (alignment == StyleConstants.ALIGN_RIGHT) lines = new double[][] { { 3, 21 }, { 9, 21 }, { 6, 21 } };
        else lines = new double[][] { { 3, 21 }, { 3, 15 }, { 3, 18 } };
        Path2D.Double path = new Path2D.Double();
        for (int i = 0; i < lines.length; i++)
        {
            line(path, lines[i][0], 6 + 6 * i, lines[i][1], 6 + 6 * i);
        }
        return new FormatIcon(path, STROKE);
    }

    /**
     * @param color the current text color, or null if it is none of the palette
     * @return a letter A underlined by a bar of the current color
     */
    static Icon textColor(final Supplier<Color> color)
    {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(7, 15);
        path.lineTo(12, 3);
        path.lineTo(17, 15);
        line(path, 8.7, 11, 15.3, 11);
        return new FormatIcon(path, STROKE)
        {
            @Override
            protected void paintMore(Graphics2D g2, Component c)
            {
                Shape bar = new RoundRectangle2D.Double(4, 18, 16, 4, 1.5, 1.5);
                Color value = color.get();
                if (value != null)
                {
                    g2.setColor(TextColorButton.toDisplayedColor(value));
                    g2.fill(bar);
                }
                else
                {
                    g2.setStroke(new BasicStroke(1));
                    g2.draw(bar);
                }
            }
        };
    }

    /**
     * @return a small down arrow, telling that a button opens a menu
     */
    static Icon chevron()
    {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(7, 10);
        path.lineTo(12, 15);
        path.lineTo(17, 10);
        return new FormatIcon(path, 2.2f, 10);
    }

    /**
     * @param icon of a button opening a menu
     * @return the icon followed by a small down arrow
     */
    static Icon withChevron(final Icon icon)
    {
        final Icon chevron = chevron();
        return new Icon()
        {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y)
            {
                icon.paintIcon(c, g, x, y);
                chevron.paintIcon(c, g, x + icon.getIconWidth(), y + (icon.getIconHeight() - chevron.getIconHeight()) / 2);
            }

            @Override
            public int getIconWidth()
            {
                return icon.getIconWidth() + chevron.getIconWidth();
            }

            @Override
            public int getIconHeight()
            {
                return icon.getIconHeight();
            }
        };
    }

    private static void line(Path2D path, double x1, double y1, double x2, double y2)
    {
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
    }

    /**
     * An outline on the 24x24 grid, shown at 16 pixels
     */
    private static class FormatIcon implements Icon
    {
        FormatIcon(Shape shape, float stroke)
        {
            this(shape, stroke, SIZE);
        }

        FormatIcon(Shape shape, float stroke, int size)
        {
            this.shape = shape;
            this.stroke = stroke;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.translate(x, y);
                g2.scale(size / 24.0, size / 24.0);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(ChoiceIcons.getIconColor(c));
                g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(shape);
                paintMore(g2, c);
            }
            finally
            {
                g2.dispose();
            }
        }

        /**
         * Paints more on the 24x24 grid
         *
         * @param g2 graphics set as for the outline
         * @param c component showing the icon
         */
        protected void paintMore(Graphics2D g2, Component c)
        {
        }

        @Override
        public int getIconWidth()
        {
            return size;
        }

        @Override
        public int getIconHeight()
        {
            return size;
        }

        private final Shape shape;
        private final float stroke;
        private final int size;
    }

    private static final int SIZE = 16;
    private static final float STROKE = 1.9f;
}
