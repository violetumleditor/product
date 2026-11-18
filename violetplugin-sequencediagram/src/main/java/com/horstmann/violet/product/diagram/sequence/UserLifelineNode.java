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

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * A lifeline of a user (an actor) in a sequence diagram: a stick man with the name below it, instead of the object box.
 */
public class UserLifelineNode extends LifelineNode
{
    /**
     * Construct a user lifeline with a default size
     */
    public UserLifelineNode()
    {
        getName().setUnderlined(false);
    }

    @Override
    protected double getTopRectangleHeight()
    {
        return FIGURE_HEIGHT + getName().getBounds().getHeight();
    }

    @Override
    protected double getTopRectangleWidth()
    {
        return Math.max(super.getTopRectangleWidth(), FIGURE_WIDTH);
    }

    /**
     * Draws the stick man then the name below it
     */
    @Override
    protected void drawTop(Graphics2D g2, Rectangle2D top)
    {
        double neckX = top.getCenterX();
        double neckY = top.getY() + GAP_ABOVE + HEAD_SIZE;
        Ellipse2D head = new Ellipse2D.Double(neckX - HEAD_SIZE / 2.0, neckY - HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
        GeneralPath body = new GeneralPath();
        double hipY = neckY + BODY_SIZE;
        body.moveTo(neckX, neckY);
        body.lineTo(neckX, hipY);
        body.moveTo(neckX - ARMS_SIZE / 2.0, neckY + BODY_SIZE / 3.0);
        body.lineTo(neckX + ARMS_SIZE / 2.0, neckY + BODY_SIZE / 3.0);
        double dx = LEG_SIZE / Math.sqrt(2);
        body.moveTo(neckX - dx, hipY + dx);
        body.lineTo(neckX, hipY);
        body.lineTo(neckX + dx, hipY + dx);
        g2.setColor(getBackgroundColor());
        g2.fill(head);
        g2.setColor(getBorderColor());
        g2.draw(head);
        g2.draw(body);
        Rectangle2D nameBox = new Rectangle2D.Double(top.getX(), top.getY() + FIGURE_HEIGHT, top.getWidth(), top.getHeight()
                - FIGURE_HEIGHT);
        g2.setColor(getTextColor());
        getName().draw(g2, nameBox);
    }

    @Override
    public UserLifelineNode clone()
    {
        return (UserLifelineNode) super.clone();
    }

    /** Stick man : width */
    private static final int FIGURE_WIDTH = 48;
    /** Stick man : space above the head */
    private static final int GAP_ABOVE = 4;
    /** Stick man : head size */
    private static final int HEAD_SIZE = FIGURE_WIDTH * 4 / 12;
    /** Stick man : body size */
    private static final int BODY_SIZE = FIGURE_WIDTH * 5 / 12;
    /** Stick man : leg size */
    private static final int LEG_SIZE = FIGURE_WIDTH * 5 / 12;
    /** Stick man : arm size */
    private static final int ARMS_SIZE = FIGURE_WIDTH * 6 / 12;
    /** Stick man : height, with the space above the head and below the feet */
    private static final int FIGURE_HEIGHT = GAP_ABOVE + HEAD_SIZE + BODY_SIZE + (int) Math.ceil(LEG_SIZE / Math.sqrt(2)) + GAP_ABOVE;
}
