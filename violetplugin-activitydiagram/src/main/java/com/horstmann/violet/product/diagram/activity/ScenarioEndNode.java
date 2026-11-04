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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.EllipticalNode;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A final node (bull's eye) in an activity diagram.
 */
public class ScenarioEndNode extends EllipticalNode
{

	public ScenarioEndNode() {
		super();
		setBorderColor(DEFAULT_BORDER_COLOR);
		setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
	}
	
	
    @Override
    public Rectangle2D getBounds()
    {
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = DEFAULT_DIAMETER + 2 * DEFAULT_GAP;
        double h = DEFAULT_DIAMETER + 2 * DEFAULT_GAP;
        Rectangle2D currentBounds = new Rectangle2D.Double(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(currentBounds);
        return snappedBounds;
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null && this != e.getEndNode();
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Draw circles
        Ellipse2D circle = new Ellipse2D.Double(getBounds().getX(), getBounds().getY(), getBounds().getWidth(), getBounds()
                .getHeight());

        Rectangle2D bounds = getBounds();
        Ellipse2D inside = new Ellipse2D.Double(bounds.getX() + DEFAULT_GAP, bounds.getY() + DEFAULT_GAP, bounds.getWidth() - 2
                * DEFAULT_GAP, bounds.getHeight() - 2 * DEFAULT_GAP);
        g2.setColor(getBackgroundColor());
        g2.fill(circle);
        g2.setColor(getBorderColor());
        g2.fill(inside);
        g2.draw(circle);

        // Restore first color
        g2.setColor(oldColor);
    }

    @Override
    public ScenarioEndNode clone()
    {
        return (ScenarioEndNode) super.clone();
    }

    /** default node diameter */
    private static int DEFAULT_DIAMETER = 14;

    /** default gap between the main circle and the ring for a final node */
    private static int DEFAULT_GAP = 3;

    /** Default colors */
    private static Color DEFAULT_BORDER_COLOR = ColorChoice.PASTEL_GREY.getBorderColor();
    private static Color DEFAULT_BACKGROUND_COLOR = ColorChoice.PASTEL_GREY.getBackgroundColor();

}
