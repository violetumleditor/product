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

package com.horstmann.violet.product.diagram.usecase;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.EllipticalNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A use case node in a use case diagram.
 */
public class UseCaseNode extends EllipticalNode
{
    /**
     * Construct a use case node with a default size
     */
    public UseCaseNode()
    {
        name = new MultiLineString();
    }

    @Override
    public Rectangle2D getBounds()
    {
        double aspectRatio = DEFAULT_WIDTH / DEFAULT_HEIGHT;
        Rectangle2D b = name.getBoundsWithMargins();
        double bw = b.getWidth();
        double bh = b.getHeight();
        double minWidth = Math.sqrt(bw * bw + aspectRatio * aspectRatio * bh * bh);
        double minHeight = minWidth / aspectRatio;
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = Math.max(minWidth, DEFAULT_WIDTH);
        double h = Math.max(minHeight, DEFAULT_HEIGHT);
        Rectangle2D currentBounds = new Rectangle2D.Double(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(currentBounds);
        return snappedBounds;
    }

    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        // if use case node is atatched to an actor node, we force connection point to cardianl points
        if (e.getStartNode().getClass().isAssignableFrom(ActorNode.class) || e.getEndNode().getClass().isAssignableFrom(ActorNode.class))
        {

        }

        return super.getConnectionPoint(e);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Draw shape and text
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

    /**
     * Sets the name property value.
     * 
     * @param newValue the new use case name
     */
    public void setName(MultiLineString newValue)
    {
        name = newValue;
    }

    /**
     * Gets the name property value.
     * 
     * @param the use case name
     */
    public MultiLineString getName()
    {
        return name;
    }

    @Override
    public UseCaseNode clone()
    {
        UseCaseNode cloned = (UseCaseNode) super.clone();
        cloned.name = name.clone();
        return cloned;
    }

    private MultiLineString name;

    private static int DEFAULT_WIDTH = 110;
    private static int DEFAULT_HEIGHT = 40;
}
