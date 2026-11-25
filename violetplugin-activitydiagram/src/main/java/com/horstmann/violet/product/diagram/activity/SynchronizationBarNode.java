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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;

/**
 * A synchronization bar node in an activity diagram (fork or join), horizontal or vertical. A horizontal bar spans the nodes
 * it is connected to from left to right and its transitions reach its top or bottom side; a vertical one spans them from top
 * to bottom and its transitions reach its left or right side.
 */
public class SynchronizationBarNode extends RectangularNode
{

    /**
     * Creates a horizontal bar
     */
    public SynchronizationBarNode()
    {
        this(false);
    }

    /**
     * @param isVertical true for a vertical bar, false for a horizontal one
     */
    public SynchronizationBarNode(boolean isVertical)
    {
        super();
        // Left null for a horizontal bar, so that its file stays the same as before vertical bars existed
        this.vertical = isVertical ? Boolean.TRUE : null;
    }

    /**
     * @return true for a vertical bar, false for a horizontal one (no setter: the orientation comes from the tool, not from
     *         the properties dialog)
     */
    public boolean isVertical()
    {
        return Boolean.TRUE.equals(this.vertical);
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && this != e.getEndNode();
    }

    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        Point2D defaultConnectionPoint = getDefaultConnectionPoint(e);
        if (!ActivityTransitionEdge.class.isInstance(e))
        {
            return defaultConnectionPoint;
        }
        INode start = e.getStartNode();
        INode end = e.getEndNode();
        INode other;
        if (this == start)
        {
            other = end;
        }
        else if (this == end)
        {
            other = start;
        }
        else
        {
            return defaultConnectionPoint;
        }
        Point2D otherConnectionPoint = getOtherConnectionPoint(e, other);
        if (isVertical())
        {
            // On the left or right side, at the height of the other end
            return keepOffAdaptedEdges(e, new Point2D.Double(defaultConnectionPoint.getX(), otherConnectionPoint.getY()),
                    Collections.emptyList());
        }
        // On the top or bottom side, in front of the other end
        return keepOffAdaptedEdges(e, new Point2D.Double(otherConnectionPoint.getX(), defaultConnectionPoint.getY()),
                Collections.emptyList());
    }

    /**
     * @return the point where the edge reaches the other node. Between two bars, the starting bar aligns on the default point
     *         of the ending one, and the ending one on the starting one, so that each doesn't ask the other forever.
     */
    private Point2D getOtherConnectionPoint(IEdge e, INode other)
    {
        if (other instanceof SynchronizationBarNode && this == e.getStartNode())
        {
            return ((SynchronizationBarNode) other).getDefaultConnectionPoint(e);
        }
        return other.getConnectionPoint(e);
    }

    /**
     * @return the connection point of a rectangular node, on the side the edge comes from
     */
    private Point2D getDefaultConnectionPoint(IEdge e)
    {
        return getConnectionPointOnSide(e);
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        double length = DEFAULT_LENGTH;
        List<INode> connectedNodes = getConnectedNodes();
        if (connectedNodes.size() > 0)
        {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (INode n : connectedNodes)
            {
                Rectangle2D b = n.getBounds();
                min = Math.min(min, isVertical() ? b.getMinY() : b.getMinX());
                max = Math.max(max, isVertical() ? b.getMaxY() : b.getMaxX());
            }
            length = max - min;
        }
        length = EXTRA_LENGTH + length + EXTRA_LENGTH;
        if (isVertical())
        {
            return new Rectangle2D.Double(location.getX(), location.getY(), THICKNESS, length);
        }
        return new Rectangle2D.Double(location.getX(), location.getY(), length, THICKNESS);
    }

    @Override
    public Point2D getLocation()
    {
        Point2D defaultLocation = super.getLocation();
        double x = defaultLocation.getX();
        double y = defaultLocation.getY();
        List<INode> connectedNodes = getConnectedNodes();
        if (connectedNodes.size() > 0)
        {
            // The bar starts just before the first connected node, along its length
            double min = Double.MAX_VALUE;
            for (INode n : connectedNodes)
            {
                Point2D p = n.getLocation();
                min = Math.min(min, isVertical() ? p.getY() : p.getX());
            }
            if (isVertical())
            {
                y = min - EXTRA_LENGTH;
            }
            else
            {
                x = min - EXTRA_LENGTH;
            }
        }
        return new Point2D.Double(x, y);
    }

    /**
     * @return nodes which are connected (with edges) to this node, other bars excepted: their extent depends on their own
     *         connected nodes, and two bars connected together would otherwise measure each other forever
     */
    private List<INode> getConnectedNodes()
    {
        List<INode> connectedNodes = new ArrayList<INode>();
        // needs to contain all incoming and outgoing edges
        for (IEdge e : getConnectedEdges())
        {
            INode other = null;
            if (e.getStartNode() == this)
            {
                other = e.getEndNode();
            }
            else if (e.getEndNode() == this)
            {
                other = e.getStartNode();
            }
            if (other != null && !(other instanceof SynchronizationBarNode))
            {
                connectedNodes.add(other);
            }
        }
        return connectedNodes;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Perform drawing
        g2.setColor(getBorderColor());
        g2.fill(getShape());

        // Restore first color
        g2.setColor(oldColor);
    }

    /**
     * @see java.lang.Object#clone()
     */
    @Override
    public SynchronizationBarNode clone()
    {
        return (SynchronizationBarNode) super.clone();
    }

    /** True for a vertical bar, null for a horizontal one (saved only for a vertical bar) */
    private Boolean vertical;

    private static int DEFAULT_LENGTH = 100;
    private static int THICKNESS = 4;
    private static int EXTRA_LENGTH = 12;
}
