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

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A generic connection, available in every diagram, that links any node to any node (itself
 * included), whatever the diagram rules (see AbstractGraph.connect). All its look is set in its properties: start, middle and end labels, line style,
 * arrow heads, bent and angle styles.
 *
 * It replaces the former note edge: its defaults (dotted, straight, no arrow) draw like it, and
 * the old "noteEdge" elements of the diagram files are read as generic edges.
 */
public class GenericEdge extends SegmentedLineEdge
{
    public GenericEdge()
    {
        super();
        setLineStyle(LineStyle.DOTTED);
        setBentStyle(BentStyle.STRAIGHT);
    }

    /**
     * Name of the element written by the former NoteEdge class in the diagram files.
     */
    public static final String LEGACY_NOTE_EDGE_TAG = "noteEdge";
}
