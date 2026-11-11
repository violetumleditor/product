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

package com.horstmann.violet.product.diagram.sequence;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * An edge that joins two call nodes.
 */
public class ReturnEdge extends SegmentedLineEdge
{

	@Override
	public boolean isTransitionPointsSupported() {
		return false;
	}

    /**
     * A return goes back to the caller, so its tool bar icon points to the left, unlike the call one.
     */
    @Override
    public boolean isToolIconReversed()
    {
        return true;
    }

    @Override
    public ArrowHead getEndArrowHead()
    {
        return ArrowHead.V;
    }

    @Override
    public LineStyle getLineStyle()
    {
        return LineStyle.DOTTED;
    }

    @Override
    public Line2D getConnectionPoints()
    {
        ArrayList<Point2D> points = getPoints();
        Point2D p1 = points.get(0);
        Point2D p2 = points.get(points.size() - 1);
        return new Line2D.Double(p1, p2);
    }

    @Override
    public ArrayList<Point2D> getPoints()
    {
        INode endingNode = getEndNode();
        INode startingNode = getStartNode();
        return getPointsForNodesOnDifferentLifeLines(startingNode, endingNode);
    }

    private ArrayList<Point2D> getPointsForNodesOnDifferentLifeLines(INode startingNode, INode endingNode)
    {
        Point2D startingPoint = startingNode.getConnectionPoint(this);
        Point2D endingPoint = endingNode.getConnectionPoint(this);
        ArrayList<Point2D> a = new ArrayList<Point2D>();
        a.add(startingPoint);
        a.add(endingPoint);
        return a;
    }

}
