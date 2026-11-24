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

package com.horstmann.violet.product.diagram.activity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * An activity node in an activity diagram.
 */
public class ActivityNode extends RectangularNode
{
    /**
     * Construct an action node with a default size
     */
    public ActivityNode()
    {
        name = new MultiLineString();
    }

    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        // Backup current color;
        Color oldColor = g2.getColor();
        // Draw shape and content
        Shape shape = getShape();
        g2.setColor(getBackgroundColor());
        g2.fill(shape);
        g2.setColor(getBorderColor());
        g2.draw(shape);
        g2.setColor(getTextColor());
        name.draw(g2, getBounds());
        // Restore first color
        g2.setColor(oldColor);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        return new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ARC_SIZE, ARC_SIZE);
    }

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D nameBounds = name.getBoundsWithMargins();
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = Math.max(nameBounds.getWidth(), DEFAULT_WIDTH);
        double h = Math.max(nameBounds.getHeight(), DEFAULT_HEIGHT);
        Rectangle2D.Double globalBounds = new Rectangle2D.Double(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(globalBounds);
        return snappedBounds;
    }

    /**
     * Sets the name property value.
     * 
     * @param newValue the new action name
     */
    public void setName(MultiLineString newValue)
    {
        name = newValue;
    }

    /**
     * Gets the name property value.
     * 
     * @param the action name
     */
    public MultiLineString getName()
    {
        return name;
    }

    @Override
    public ActivityNode clone()
    {
        ActivityNode cloned = (ActivityNode) super.clone();
        cloned.name = name.clone();
        return cloned;
    }

    private MultiLineString name;

    private static int ARC_SIZE = 20;
    private static int DEFAULT_WIDTH = 80;
    private static int DEFAULT_HEIGHT = 60;
}
