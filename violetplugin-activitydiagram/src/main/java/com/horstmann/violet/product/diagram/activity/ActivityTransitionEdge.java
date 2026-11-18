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
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;

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
        if (SynchronizationBarNode.class.isInstance(getStartNode()) || SynchronizationBarNode.class.isInstance(getEndNode())) {
        	if (node.equals(getStartNode())) {
        		Point2D p1 = node.getLocationOnGraph();
        		Point2D p2 = getEndNode().getLocationOnGraph();
        		if (p1.getY() < p2.getY()) {
        			return Direction.NORTH;
        		}
       		    if (p1.getY() > p2.getY()) {
        			return Direction.SOUTH;
        		}
        	}
        	if (node.equals(getEndNode())) {
        		Point2D p1 = node.getLocationOnGraph();
        		Point2D p2 = getStartNode().getLocationOnGraph();
        		if (p1.getY() < p2.getY()) {
        			return Direction.NORTH;
        		}
       		    if (p1.getY() > p2.getY()) {
        			return Direction.SOUTH;
        		}
        	}
        }
        if (isRoutedFromDecision())
        {
            return getDirectionFromDecision(node);
        }
        return super.getDirection(node);
    }

    /**
     * @return true for an automatic transition leaving a decision node, whose route depends on where its end node lies
     */
    private boolean isRoutedFromDecision()
    {
        INode start = getStartNode();
        INode end = getEndNode();
        return DecisionNode.class.isInstance(start) && end != null && !start.equals(end)
                && BentStyle.AUTO.equals(getBentStyle());
    }

    /**
     * Chooses the vertex of the decision the transition leaves from, with the other branches of the decision, and the side of
     * the end node it reaches. The automatic path then follows: HV from a side vertex to the top or bottom of an end node above
     * or below, HVH to an end node on the same row, VHV from the bottom or top vertex.
     */
    private Direction getDirectionFromDecision(INode node)
    {
        DecisionVertexChooser chooser = new DecisionVertexChooser((DecisionNode) getStartNode());
        return chooser.getDirection(this, node);
    }

}
