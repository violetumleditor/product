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

import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;

/**
 * An edge that joins two call nodes. Typically, call edges are used in sequence diagram to represent calls between entities (call
 * nodes).
 */
public class CallEdge extends MessageEdge
{
    /**
     * Default constructor
     */
    public CallEdge()
    {
        setSignal(false);
    }

    /**
     * Gets the signal property.
     * 
     * @return true if this is an edge that represents an asynchronus signal
     */
    public boolean isSignal()
    {
        return signal;
    }

    /**
     * Sets the signal property.
     * 
     * @param newValue true if this is an edge that represents an asynchronus signal
     */
    public void setSignal(boolean newValue)
    {
        signal = newValue;

    }

    @Override
    public ArrowHead getEndArrowHead()
    {
        if (signal)
        {
            return ArrowHead.V;
        }
        return ArrowHead.BLACK_TRIANGLE;
    }

    /**
     * A self call is a loop on the right of its bars
     */
    @Override
    public ArrayList<Point2D> getPoints()
    {
        ArrayList<Point2D> points = super.getPoints();
        if (isSelfCall())
        {
            Point2D start = points.get(0);
            Point2D end = points.get(1);
            double loopX = end.getX() + LOOP_GAP;
            points.add(1, new Point2D.Double(loopX, start.getY()));
            points.add(2, new Point2D.Double(loopX, end.getY()));
        }
        return points;
    }

    private boolean isSelfCall()
    {
        INode start = getStartNode();
        INode end = getEndNode();
        return start instanceof ActivationBarNode && end instanceof ActivationBarNode
                && ((ActivationBarNode) start).getImplicitParameter() == ((ActivationBarNode) end).getImplicitParameter();
    }

    /** Indicate if the node represents an asynchonus signal */
    private boolean signal;

    /** Horizontal gap used to connected two activation bars on the same lifeline */
    private static final int LOOP_GAP = 15;
}
