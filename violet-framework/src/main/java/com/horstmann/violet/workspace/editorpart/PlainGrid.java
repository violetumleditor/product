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

package com.horstmann.violet.workspace.editorpart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;

/**
 * A grid to which points and rectangles can be "snapped". The snapping operation moves a point to the nearest grid point.
 */
public class PlainGrid implements IGrid
{
    /**
     * Constructs a grid with its grid size.
     * 
     * @param snappingWidth the grid point distance in x-direction
     * @param snappingHeight the grid point distance in y-direction
     */
    public PlainGrid(IEditorPart editorPart)
    {
        this.editorPart = editorPart;
        this.snappingWidth = DEFAULT_GRID_SIZE;
        this.snappingHeight = DEFAULT_GRID_SIZE;
    }

    /**
     * @return the grid size according to the graph size, the editor size and its zoom factor (scaleX and scaleY)
     */
    private Rectangle2D.Double getBounds(Graphics2D g2)
    {
        Component editorPartComponent = editorPart.getSwingComponent();
        Dimension parentSize = editorPartComponent.getSize();
        return new Rectangle.Double(0, 0, (int) (parentSize.getWidth() / editorPart.getZoomFactor()), (int) (parentSize.getHeight() / editorPart.getZoomFactor()));
    }

    @Override
    public void paint(Graphics2D g2)
    {
        if (snappingWidth == 0 || snappingHeight == 0) return;
        Color oldColor = g2.getColor();
        Rectangle2D.Double bounds = getBounds(g2);
        g2.setColor(ThemeManager.getInstance().getTheme().getDiagramBackgroundColor());
        g2.fill(bounds);
        g2.setColor(ThemeManager.getInstance().getTheme().getGridColor());
        Stroke oldStroke = g2.getStroke();
        for (double x = bounds.getX(); x < bounds.getMaxX(); x += snappingWidth)
            g2.draw(new Line2D.Double(x, bounds.getY(), x, bounds.getMaxY()));
        for (double y = bounds.getY(); y < bounds.getMaxY(); y += snappingHeight)
            g2.draw(new Line2D.Double(bounds.getX(), y, bounds.getMaxX(), y));
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }
    
    

    @Override
    public IGridSticker getGridSticker()
    {
        return new IGridSticker()
        {
            
            @Override
            public Rectangle2D snap(Rectangle2D r)
            {
                return PlainGrid.this.snap(r);
            }
            
            @Override
            public Point2D snap(Point2D p)
            {
                return PlainGrid.this.snap(p);
            }
        };
    }


    private Point2D snap(Point2D p)
    {
        double x;
        if (snappingWidth == 0) x = p.getX();
        else x = Math.round(p.getX() / snappingWidth) * snappingWidth;
        double y;
        if (snappingHeight == 0) y = p.getY();
        else y = Math.round(p.getY() / snappingHeight) * snappingHeight;
        p.setLocation(x, y);
        return p;
    }

    private Rectangle2D snap(Rectangle2D r)
    {
        double x;
        double w;
        if (snappingWidth == 0)
        {
            x = r.getX();
            w = r.getWidth();
        }
        else
        {
            x = Math.round(r.getX() / snappingWidth) * snappingWidth;
            w = Math.ceil(r.getWidth() / (2 * snappingWidth)) * (2 * snappingWidth);
        }
        double y;
        double h;
        if (snappingHeight == 0)
        {
            y = r.getY();
            h = r.getHeight();
        }
        else
        {
            y = Math.round(r.getY() / snappingHeight) * snappingHeight;
            h = Math.ceil(r.getHeight() / (2 * snappingHeight)) * (2 * snappingHeight);
        }

        r.setFrame(x, y, w, h);
        return r;
    }

    /* (non-Javadoc)
     * @see com.horstmann.violet.product.diagram.abstracts.IGrid#getSnappingWidth()
     */
    @Override
    public double getSnappingWidth()
    {
        return snappingWidth;
    }

    /* (non-Javadoc)
     * @see com.horstmann.violet.product.diagram.abstracts.IGrid#getSnappingHeight()
     */
    @Override
    public double getSnappingHeight()
    {
        return snappingHeight;
    }

    private double snappingWidth;
    private double snappingHeight;
    public static final int DEFAULT_GRID_SIZE = 10;
    private IEditorPart editorPart;
}
