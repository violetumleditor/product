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

package com.horstmann.violet.product.diagram.abstracts.property;

import java.awt.BasicStroke;
import java.awt.Stroke;

import com.horstmann.violet.framework.util.SerializableEnumeration;

/**
 * This class defines line styles of various shapes.
 */
public class LineStyle extends SerializableEnumeration
{
    /**
     * Private constructor. Note that this class is nearly construct as a singleton.
     */
    private LineStyle()
    {
    }

    /**
     * Gets a stroke with which to draw this line style (default width 1).
     * 
     * @return the stroke object that strokes this line style
     */
    public Stroke getStroke()
    {
        return getStroke(1.0f);
    }

    /**
     * Gets a stroke with the given width. For dotted lines, the dash
     * pattern is scaled proportionally to the width.
     * 
     * @param width the desired line width in pixels
     * @return the stroke object that strokes this line style at the given width
     */
    public Stroke getStroke(float width)
    {
        if (width < 1.0f) width = 1.0f;
        if (this == DOTTED)
        {
            float[] dash = new float[]
            {
                    3.0f * width,
                    3.0f * width
            };
            return new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        }
        if (this == DASHED)
        {
            float[] dash = new float[]
            {
                    8.0f * width,
                    5.0f * width
            };
            return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
        }
        return new BasicStroke(width);
    }

    /** The unique solid linestyle instance */
    public static final LineStyle SOLID = new LineStyle();

    /** The unique dotted linestyle instance */
    public static final LineStyle DOTTED = new LineStyle();

    /**
     * The unique dashed linestyle instance (long dashes, as a BPMN message flow). Not offered by the line style editor: it is
     * given by the edges that need it.
     */
    public static final LineStyle DASHED = new LineStyle();

}
