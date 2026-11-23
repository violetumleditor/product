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

package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;

/**
 * A class that assumes that an edge can yield its shape and then takes advantage of the fact that containment testing can be done
 * by stroking the shape with a fat stroke. NOTE: Ideally, you should be able to draw the same shape that is used for containment
 * testing. However, in JDK 1.4, BasicStroke.createStrokedShape returns shitty-looking shapes.
 */
public abstract class ShapeEdge extends AbstractEdge
{
    /**
     * Returns the path that should be stroked to draw this edge. The path does not include arrow tips or labels.
     * 
     * @return a path along the edge
     */
    public abstract Shape getShape();

    @Override
    public Rectangle2D getBounds()
    {
        return getShape().getBounds();
    }

    public boolean contains(Point2D aPoint)
    {
        // the end points may contain small nodes, so don't
        // match them
        Line2D conn = getConnectionPoints();
        if (aPoint.distance(conn.getP1()) <= END_POINT_DISTANCE || aPoint.distance(conn.getP2()) <= END_POINT_DISTANCE) return false;
        // A line is thin: it is found up to HIT_DISTANCE from it, but never inside the nodes it links, which keep their clicks
        if (isInsideLinkedNode(aPoint)) return false;

        Shape p = getShape();
        BasicStroke fatStroke = new BasicStroke((float) (2 * HIT_DISTANCE), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Shape fatPath = fatStroke.createStrokedShape(p);
        return fatPath.contains(aPoint);
    }

    /**
     * @return true if the point lies in the start or the end node of this edge, a grouping area excepted (an edge can run inside
     *         it, as it is a background)
     */
    private boolean isInsideLinkedNode(Point2D aPoint)
    {
        for (INode aNode : new INode[] { getStartNode(), getEndNode() })
        {
            if (aNode == null || aNode instanceof ContainerNode)
            {
                continue;
            }
            Point2D locationOnGraph = aNode.getLocationOnGraph();
            Rectangle2D bounds = aNode.getBounds();
            Rectangle2D boundsOnGraph = new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(),
                    bounds.getHeight());
            if (boundsOnGraph.contains(aPoint))
            {
                return true;
            }
        }
        return false;
    }

    /** Distance from the line under which a point is on the edge */
    private static final double HIT_DISTANCE = 6;

    /** Distance from an end point under which a point is never on the edge */
    private static final double END_POINT_DISTANCE = 3;
}
