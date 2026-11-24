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

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.DiamondNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A decision node in an activity diagram.
 */
public class DecisionNode extends DiamondNode
{

    @Override
    public void setGraph(IGraph g)
    {
        super.setGraph(g);
        convertFormerCondition();
    }

    /**
     * In older files, the text was the "condition", drawn inside the diamond which grew with it: it becomes the name, and the
     * diamond of standard size takes the center of the former one
     */
    private void convertFormerCondition()
    {
        if (this.condition == null)
        {
            return;
        }
        Rectangle2D formerBounds = getFormerBounds(this.condition);
        setName(this.condition);
        this.condition = null;
        Rectangle2D bounds = getBounds();
        Point2D newLocation = new Point2D.Double(formerBounds.getCenterX() - bounds.getWidth() / 2, formerBounds.getCenterY()
                - bounds.getHeight() / 2);
        setLocation(getGraph().getGridSticker().snap(newLocation));
    }

    /**
     * @param text the text inside the diamond
     * @return the bounds the diamond had around it
     */
    private Rectangle2D getFormerBounds(MultiLineString text)
    {
        Rectangle2D b = text.getBoundsWithMargins();
        double halfTextWidth = Math.max(FORMER_TEXT_WIDTH, b.getWidth()) / 2;
        double halfTextHeight = Math.max(FORMER_TEXT_HEIGHT, b.getHeight()) / 2;
        double halfWidth = halfTextWidth + Math.tan(Math.toRadians(60)) * halfTextHeight;
        double halfHeight = halfTextHeight + Math.tan(Math.toRadians(30)) * halfTextWidth;
        Point2D location = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), halfWidth * 2, halfHeight * 2);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public DecisionNode clone()
    {
        DecisionNode cloned = (DecisionNode) super.clone();
        if (this.condition != null)
        {
            cloned.condition = this.condition.clone();
        }
        return cloned;
    }

    /** Only set by the reader of an older file, until the node gets its graph; never saved */
    private MultiLineString condition;

    private static final double FORMER_TEXT_WIDTH = 30;

    private static final double FORMER_TEXT_HEIGHT = 20;

}
