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

package com.horstmann.violet.product.diagram.common;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A note node in a UML diagram.
 * 
 * FIXME : manage Z order
 * 
 * for (IEdge e : getGraph().getEdges()) { if (e.getStart() == this) { INode end = e.getEnd(); Point2D endPoint = end.getLocation();
 * INode n = getGraph().findNode(endPoint); if (n != end) end.setZ(n.getZ() + 1); } }
 * 
 */
public class NoteNode extends RectangularNode
{
    /**
     * Construct a note node with a default size and color
     */
    public NoteNode()
    {
        text = new MultiLineString();
        text.setJustification(MultiLineString.Justification.LEFT);
        setBackgroundColor(DEFAULT_BACKGROUND_COLOR);
        setBorderColor(DEFAULT_BORDER_COLOR);
    }

    @Override
    public int getZ()
    {
        // Ensures that this kind of nodes is always on top
        return INFINITE_Z_LEVEL;
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        if (e.getStartNode() == e.getEndNode())
        {
            return false;
        }
        return super.addConnection(e);
    }

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D b = text.getBounds();
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = Math.max(b.getWidth() + PADDING_LEFT + PADDING_RIGHT, DEFAULT_WIDTH);
        double h = Math.max(b.getHeight() + PADDING_TOP + PADDING_BOTTOM, DEFAULT_HEIGHT);
        Rectangle2D currentBounds = new Rectangle2D.Double(x, y, w, h);
        Rectangle2D snapperBounds = getGraph().getGridSticker().snap(currentBounds);
        return snapperBounds;
    }

    /**
     * Gets the value of the text property.
     * 
     * @return the text inside the note
     */
    public MultiLineString getText()
    {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param newValue the text inside the note
     */
    public void setText(MultiLineString newValue)
    {
        text = newValue;
    }


    @Override
    public void draw(Graphics2D g2)
    {
        // Backup current color;
        Color oldColor = g2.getColor();

        // Perform drawing
        g2.setColor(getBackgroundColor());
        Shape path = getShape();
        g2.fill(path);
        g2.setColor(getBorderColor());
        g2.draw(path);

        Rectangle2D bounds = getBounds();
        GeneralPath fold = new GeneralPath();
        fold.moveTo((float) (bounds.getMaxX() - FOLD_X), (float) bounds.getY());
        fold.lineTo((float) bounds.getMaxX() - FOLD_X, (float) bounds.getY() + FOLD_X);
        fold.lineTo((float) bounds.getMaxX(), (float) (bounds.getY() + FOLD_Y));
        fold.closePath();
        g2.setColor(Color.WHITE);
        g2.fill(fold);
        g2.setColor(getBorderColor());
        g2.draw(fold);

        g2.setColor(getTextColor());
        text.draw(g2, getTextBounds());

        // Restore first color
        g2.setColor(oldColor);
    }

    /**
     * The text sits in the top left corner, inside the margins: the right one keeps the first line out of the fold, and a
     * note made taller than its text (default size, grid) leaves the extra space at the bottom.
     * 
     * @return the rectangle given to the text
     */
    private Rectangle2D getTextBounds()
    {
        Rectangle2D bounds = getBounds();
        double width = bounds.getWidth() - PADDING_LEFT - PADDING_RIGHT;
        double height = text.getBounds().getHeight();
        return new Rectangle2D.Double(bounds.getX() + PADDING_LEFT, bounds.getY() + PADDING_TOP, width, height);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        GeneralPath path = new GeneralPath();
        path.moveTo((float) bounds.getX(), (float) bounds.getY());
        path.lineTo((float) (bounds.getMaxX() - FOLD_X), (float) bounds.getY());
        path.lineTo((float) bounds.getMaxX(), (float) (bounds.getY() + FOLD_Y));
        path.lineTo((float) bounds.getMaxX(), (float) bounds.getMaxY());
        path.lineTo((float) bounds.getX(), (float) bounds.getMaxY());
        path.closePath();
        return path;
    }

    @Override
    public NoteNode clone()
    {
        NoteNode cloned = (NoteNode) super.clone();
        cloned.text = text.clone();
        return cloned;
    }

    private MultiLineString text;

    private static int DEFAULT_WIDTH = 60;
    private static int DEFAULT_HEIGHT = 40;
    private static Color DEFAULT_BACKGROUND_COLOR = ColorChoice.PASTEL_YELLOW_ORANGE.getBackgroundColor();
    private static Color DEFAULT_BORDER_COLOR = ColorChoice.PASTEL_YELLOW_ORANGE.getBorderColor();
    private static int FOLD_X = 8;
    private static int FOLD_Y = 8;
    private static int INFINITE_Z_LEVEL = 10000;
    private static int PADDING_LEFT = 8;
    private static int PADDING_RIGHT = 8;
    private static int PADDING_TOP = 6;
    private static int PADDING_BOTTOM = 6;
}
