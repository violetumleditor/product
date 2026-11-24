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

package com.horstmann.violet.product.diagram.abstracts.property;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import com.horstmann.violet.framework.util.SerializableEnumeration;

/**
 * This class defines arrow heads of various shapes.
 */
public class ArrowHead extends SerializableEnumeration
{

    /**
     * Draws the arrowhead of a line of the default width.
     *
     * @param g2 the graphics context
     * @param p a point on the axis of the arrow head
     * @param q the end point of the arrow head
     */
    public void draw(Graphics2D g2, Point2D p, Point2D q)
    {
        draw(g2, p, q, 1.0f);
    }

    /**
     * Draws the arrowhead of a line of the given width. Its visible tip is the end point, outline included.
     *
     * @param g2 the graphics context
     * @param p a point on the axis of the arrow head, usually the other end of the last segment
     * @param q the end point of the arrow head
     * @param lineWidth the width of the line
     */
    public void draw(Graphics2D g2, Point2D p, Point2D q, float lineWidth)
    {
        GeneralPath path = getPath(p, q, lineWidth);
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        float strokeWidth = (float) getOutlineWidth(p, q, lineWidth);
        if (lineWidth <= 1 && oldStroke instanceof BasicStroke)
        {
            // The tool bar sets a stroke of one pixel whatever the scale of its icon
            strokeWidth = ((BasicStroke) oldStroke).getLineWidth();
        }
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (this == BLACK_DIAMOND || this == BLACK_TRIANGLE)
        {
            g2.fill(path);
        }
        else if (this == DIAMOND || this == TRIANGLE)
        {
            g2.setColor(Color.WHITE);
            g2.fill(path);
        }
        g2.setColor(oldColor);
        g2.draw(path);
        g2.setStroke(oldStroke);
    }

    /**
     * Gets the path of the arrowhead of a line of the default width.
     *
     * @param p a point on the axis of the arrow head
     * @param q the end point of the arrow head
     * @return the path
     */
    public GeneralPath getPath(Point2D p, Point2D q)
    {
        return getPath(p, q, 1.0f);
    }

    /**
     * Gets the path of the arrowhead of a line of the given width: the middle of its outline.
     *
     * @param p a point on the axis of the arrow head
     * @param q the end point of the arrow head
     * @param lineWidth the width of the line
     * @return the path
     */
    public GeneralPath getPath(Point2D p, Point2D q, float lineWidth)
    {
        GeneralPath path = new GeneralPath();
        if (this == NONE || this == HALF_V) return path;
        double outline = getOutlineWidth(p, q, lineWidth);
        double tip = outline / 2;
        double length = getTotalLength(p, q, lineWidth) - outline;
        double base = tip + length;
        double angle = Math.atan2(q.getY() - p.getY(), q.getX() - p.getX());
        if (this == V)
        {
            double halfHeight = length * HALF_HEIGHT;
            moveTo(path, q, angle, base, -halfHeight);
            lineTo(path, q, angle, tip, 0);
            lineTo(path, q, angle, base, halfHeight);
        }
        else if (this == TRIANGLE || this == BLACK_TRIANGLE)
        {
            double halfHeight = length * HALF_HEIGHT;
            moveTo(path, q, angle, tip, 0);
            lineTo(path, q, angle, base, -halfHeight);
            lineTo(path, q, angle, base, halfHeight);
            path.closePath();
        }
        else
        {
            double middle = tip + length / 2;
            moveTo(path, q, angle, tip, 0);
            lineTo(path, q, angle, middle, -length / 2);
            lineTo(path, q, angle, base, 0);
            lineTo(path, q, angle, middle, length / 2);
            path.closePath();
        }
        return path;
    }

    private void moveTo(GeneralPath path, Point2D q, double angle, double back, double side)
    {
        path.moveTo(q.getX() - back * Math.cos(angle) - side * Math.sin(angle),
                q.getY() - back * Math.sin(angle) + side * Math.cos(angle));
    }

    private void lineTo(GeneralPath path, Point2D q, double angle, double back, double side)
    {
        path.lineTo(q.getX() - back * Math.cos(angle) - side * Math.sin(angle),
                q.getY() - back * Math.sin(angle) + side * Math.cos(angle));
    }

    /**
     * The length grows slower than the line width, and never takes more than half of the last segment. Heads whose outline has the
     * width of the line are a bit longer, so that they stay hollow, and the black diamond is as long as the white one.
     *
     * @return the length of the head along its axis, outline included
     */
    private double getTotalLength(Point2D p, Point2D q, float lineWidth)
    {
        double length = 7 + 2.7 * lineWidth;
        if (this == V || this == BLACK_TRIANGLE)
        {
            length = 7 + 2 * lineWidth;
        }
        else if (this == TRIANGLE)
        {
            // As long as the other arrows for a thin line
            length = 6.3 + 2.7 * lineWidth;
        }
        return Math.min(length, p.distance(q) / 2);
    }

    /**
     * @return the width of the outline: the one of the line, but thin around a black triangle, which is filled
     */
    private double getOutlineWidth(Point2D p, Point2D q, float lineWidth)
    {
        double outline = this == BLACK_TRIANGLE ? Math.min(lineWidth, 1.5) : lineWidth;
        // On a short segment, a hollow head has to stay hollow
        return Math.min(outline, getTotalLength(p, q, lineWidth) / (this == V ? 2.5 : 3));
    }

    /**
     * Returns how far from the end point the line has to stop, so that it doesn't show beyond the head nor inside a hollow one. The
     * line of an open head goes up to its tip.
     *
     * @param p a point on the axis of the arrow head
     * @param q the end point of the arrow head
     * @param lineWidth the width of the line
     * @return the distance, 0 without head
     */
    public double getLineCut(Point2D p, Point2D q, float lineWidth)
    {
        if (this == NONE || this == HALF_V) return 0;
        double outline = getOutlineWidth(p, q, lineWidth);
        // The end of the line is square: it extends by half of its width
        if (this == V) return outline;
        return getTotalLength(p, q, lineWidth) - outline / 2;
    }

    /** Same opening for the open, white and black arrows, in proportion to their length */
    private static final double HALF_HEIGHT = 0.5;

    /** Array head type : this head has no shape */
    public static final ArrowHead NONE = new ArrowHead();

    /** Array head type : this head is a triangle */
    public static final ArrowHead TRIANGLE = new ArrowHead();

    /** Array head type : this head is a black filled triangle */
    public static final ArrowHead BLACK_TRIANGLE = new ArrowHead();

    /** Array head type : this head is a V */
    public static final ArrowHead V = new ArrowHead();

    /** Array head type : this head is a half V */
    public static final ArrowHead HALF_V = new ArrowHead();

    /** Array head type : this head is a diamond */
    public static final ArrowHead DIAMOND = new ArrowHead();

    /** Array head type : this head is black filled diamond */
    public static final ArrowHead BLACK_DIAMOND = new ArrowHead();

    /** Internal Java UID */
    private static final long serialVersionUID = -3824887997763775890L;

}
