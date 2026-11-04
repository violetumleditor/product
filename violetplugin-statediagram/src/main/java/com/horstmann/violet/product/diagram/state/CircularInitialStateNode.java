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

package com.horstmann.violet.product.diagram.state;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.EllipticalNode;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * An initial or final node (bull's eye) in a state or activity diagram.
 */
public class CircularInitialStateNode extends EllipticalNode
{

	public CircularInitialStateNode() {
		super();
		setBorderColor(DEFAULT_BORDER_COLOR);
	}
	
	
    @Override
    public Rectangle2D getBounds()
    {
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = DEFAULT_DIAMETER;
        double h = DEFAULT_DIAMETER;
        Rectangle2D currentBounds = new Rectangle2D.Double(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(currentBounds);
        return snappedBounds;
    }

    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Draw circle
        g2.setColor(getBorderColor());
        Rectangle2D bounds = getBounds();
        Ellipse2D circle = new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g2.fill(circle);

        // Restore first color
        g2.setColor(oldColor);
    }

    /**
     * Kept for compatibility with old versions
     * 
     * @param dummy
     */
    public void setFinal(boolean dummy)
    {
        // Nothing to do
    }

    /** default node diameter */
    private static int DEFAULT_DIAMETER = 20;
    
    /** Default color */
    private static Color DEFAULT_BORDER_COLOR = ColorChoice.PASTEL_GREY.getBorderColor();

}
