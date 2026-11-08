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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.UIManager;

import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * Small icons showing the values of the edge properties in the property editor. They are drawn in the text color of their button,
 * or in the accent color when it is selected, read when painting so that they follow the theme.
 */
public final class ChoiceIcons
{
    private ChoiceIcons()
    {
    }

    /**
     * @param arrowHead to show
     * @param atStart true to point to the left, as at the start of an edge
     * @return the icon
     */
    public static Icon arrowHead(final ArrowHead arrowHead, final boolean atStart)
    {
        return new ChoiceIcon()
        {
            @Override
            protected void paint(Graphics2D g2)
            {
                double y = HEIGHT / 2.0;
                Point2D tip = new Point2D.Double(atStart ? 3 : WIDTH - 3, y);
                Point2D tail = new Point2D.Double(atStart ? WIDTH - 2 : 2, y);
                // The line stops at the base of closed heads, which are not filled
                double base = arrowHead == ArrowHead.V ? 0 : arrowHead.getArrowBaseLength(ARROW_SCALE);
                double lineEnd = atStart ? tip.getX() + base : tip.getX() - base;
                g2.draw(new Line2D.Double(tail.getX(), y, lineEnd, y));
                GeneralPath path = arrowHead.getPath(tail, tip, ARROW_SCALE);
                if (arrowHead == ArrowHead.BLACK_DIAMOND || arrowHead == ArrowHead.BLACK_TRIANGLE)
                {
                    g2.fill(path);
                }
                g2.draw(path);
            }
        };
    }

    /**
     * @param lineStyle to show
     * @return the icon
     */
    public static Icon lineStyle(final LineStyle lineStyle)
    {
        return new ChoiceIcon()
        {
            @Override
            protected void paint(Graphics2D g2)
            {
                g2.setStroke(lineStyle.getStroke(STROKE_WIDTH));
                g2.draw(new Line2D.Double(3, HEIGHT / 2.0, WIDTH - 3, HEIGHT / 2.0));
            }
        };
    }

    /**
     * @param bentStyle to show
     * @return the icon
     */
    public static Icon bentStyle(final BentStyle bentStyle)
    {
        return new ChoiceIcon()
        {
            @Override
            protected void paint(Graphics2D g2)
            {
                double l = 3, r = WIDTH - 3, t = 3, b = HEIGHT - 3, cx = WIDTH / 2.0, cy = HEIGHT / 2.0;
                double[] points;
                if (bentStyle == BentStyle.STRAIGHT) points = new double[] { l, t, r, b };
                else if (bentStyle == BentStyle.FREE) points = new double[] { l, t, l + 6, b - 1, r - 6, t + 1, r, b };
                else if (bentStyle == BentStyle.HV) points = new double[] { l, t, r, t, r, b };
                else if (bentStyle == BentStyle.VH) points = new double[] { l, t, l, b, r, b };
                else if (bentStyle == BentStyle.VHV) points = new double[] { l, t, l, cy, r, cy, r, b };
                else points = new double[] { l, t, cx, t, cx, b, r, b };
                Path2D.Double path = new Path2D.Double();
                path.moveTo(points[0], points[1]);
                for (int i = 2; i < points.length; i += 2)
                {
                    path.lineTo(points[i], points[i + 1]);
                }
                g2.draw(path);
                if (bentStyle == BentStyle.AUTO)
                {
                    g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 8));
                    g2.drawString("A", (float) r - 6, (float) b);
                }
            }
        };
    }

    /**
     * @param angleStyle to show
     * @return the icon
     */
    public static Icon angleStyle(final AngleStyle angleStyle)
    {
        return new ChoiceIcon()
        {
            @Override
            protected void paint(Graphics2D g2)
            {
                double radius = Math.min(10, angleStyle.getRadius() / 3.0);
                double x = 7, top = 3, bottom = HEIGHT - 2, right = WIDTH - 3;
                Path2D.Double path = new Path2D.Double();
                path.moveTo(x, bottom);
                path.lineTo(x, top + radius);
                path.quadTo(x, top, x + radius, top);
                path.lineTo(right, top);
                g2.draw(path);
            }
        };
    }

    /**
     * Base of the icons: sets the color, the stroke and the antialiasing.
     */
    private abstract static class ChoiceIcon implements Icon
    {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.translate(x, y);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setColor(getColor(c));
                g2.setStroke(new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                paint(g2);
            }
            finally
            {
                g2.dispose();
            }
        }

        private Color getColor(Component c)
        {
            return getIconColor(c);
        }

        /**
         * Paints the icon in a WIDTH x HEIGHT area at the origin
         *
         * @param g2 graphics with color and stroke set
         */
        protected abstract void paint(Graphics2D g2);

        @Override
        public int getIconWidth()
        {
            return WIDTH;
        }

        @Override
        public int getIconHeight()
        {
            return HEIGHT;
        }
    }

    /**
     * @param c component showing an icon
     * @return the color of the icon: the accent on a selected button, grey when disabled, the text color otherwise
     */
    static Color getIconColor(Component c)
    {
        if (!c.isEnabled())
        {
            Color disabled = UIManager.getColor("Label.disabledForeground");
            return disabled != null ? disabled : Color.GRAY;
        }
        if (c instanceof AbstractButton && ((AbstractButton) c).isSelected())
        {
            Color accent = UIManager.getColor("Component.focusColor");
            if (accent != null) return accent;
        }
        return c.getForeground();
    }

    private static final int WIDTH = 24;
    private static final int HEIGHT = 14;
    private static final float STROKE_WIDTH = 1.3f;
    private static final float ARROW_SCALE = 0.4f;
}
