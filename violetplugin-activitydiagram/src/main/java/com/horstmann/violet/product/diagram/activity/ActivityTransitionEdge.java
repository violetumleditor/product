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

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;

/**
 * An edge that is shaped like a line with up to three segments with an arrowhead
 */
public class ActivityTransitionEdge extends SegmentedLineEdge
{


    @Override
    public ArrowHead getEndArrowHead()
    {
        return ArrowHead.V;
    }
    
    @Override
    public Direction getDirection(INode node)
    {
        Direction directionAcrossBar = getDirectionAcrossBar(node);
        if (directionAcrossBar != null)
        {
            return directionAcrossBar;
        }
        Direction direction = new ActivityRouteChooser(this).getDirection(node);
        return direction != null ? direction : super.getDirection(node);
    }

    /**
     * A transition reaches a horizontal synchronization bar by its top or bottom side, a vertical one by its left or right side,
     * and leaves the node at its other end by the side facing the bar.
     * 
     * @return the direction of the transition at the given node when it starts or ends at a synchronization bar, null otherwise
     */
    private Direction getDirectionAcrossBar(INode node)
    {
        INode start = getStartNode();
        INode end = getEndNode();
        if (start == null || end == null || start.equals(end))
        {
            return null;
        }
        INode other = node.equals(start) ? end : start;
        SynchronizationBarNode bar = null;
        if (node instanceof SynchronizationBarNode)
        {
            bar = (SynchronizationBarNode) node;
        }
        else if (other instanceof SynchronizationBarNode)
        {
            bar = (SynchronizationBarNode) other;
        }
        if (bar == null)
        {
            return null;
        }
        Point2D p1 = node.getLocationOnGraph();
        Point2D p2 = other.getLocationOnGraph();
        if (bar.isVertical())
        {
            if (p1.getX() < p2.getX())
            {
                return Direction.WEST;
            }
            if (p1.getX() > p2.getX())
            {
                return Direction.EAST;
            }
            return null;
        }
        if (p1.getY() < p2.getY())
        {
            return Direction.NORTH;
        }
        if (p1.getY() > p2.getY())
        {
            return Direction.SOUTH;
        }
        return null;
    }

}
