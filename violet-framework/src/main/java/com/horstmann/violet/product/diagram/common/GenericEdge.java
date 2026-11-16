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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A generic connection, available in every diagram, that links any node to any node (itself
 * included), whatever the diagram rules (see AbstractGraph.connect). All its look is set in its properties: start, middle and end labels, line style,
 * arrow heads, bent and angle styles.
 *
 * It replaces the former note edge: its defaults (dotted, straight, no arrow) draw like it, and
 * the old "noteEdge" elements of the diagram files are read as generic edges.
 *
 * The common tools offer it in several looks (see {@link #createToolPrototypes()}), all of this one class: the diagram files
 * only store the properties of each edge.
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
     * @return the generic edges of the common tools, in their order: line, arrow, double arrow, dotted line, bent arrow. Each
     *         one has its tool tip and its short label. Static, as the diagrams build their prototypes in a static block.
     */
    public static List<IEdge> createToolPrototypes()
    {
        ResourceBundle rs = ResourceBundle.getBundle(STRINGS, Locale.getDefault());
        List<IEdge> prototypes = new ArrayList<IEdge>();
        prototypes.add(createToolPrototype(rs, "line", LineStyle.SOLID, BentStyle.STRAIGHT, ArrowHead.NONE, ArrowHead.NONE));
        prototypes.add(createToolPrototype(rs, "arrow", LineStyle.SOLID, BentStyle.STRAIGHT, ArrowHead.NONE, ArrowHead.V));
        prototypes.add(createToolPrototype(rs, "doublearrow", LineStyle.SOLID, BentStyle.STRAIGHT, ArrowHead.V, ArrowHead.V));
        prototypes.add(createToolPrototype(rs, "dotted", LineStyle.DOTTED, BentStyle.STRAIGHT, ArrowHead.NONE, ArrowHead.NONE));
        prototypes.add(createToolPrototype(rs, "bent", LineStyle.SOLID, BentStyle.AUTO, ArrowHead.NONE, ArrowHead.V));
        return prototypes;
    }

    private static GenericEdge createToolPrototype(ResourceBundle rs, String key, LineStyle lineStyle, BentStyle bentStyle,
            ArrowHead startArrowHead, ArrowHead endArrowHead)
    {
        GenericEdge edge = new GenericEdge();
        edge.setLineStyle(lineStyle);
        edge.setBentStyle(bentStyle);
        edge.setStartArrowHead(startArrowHead);
        edge.setEndArrowHead(endArrowHead);
        edge.setToolTip(rs.getString(key + ".tooltip"));
        edge.shortToolLabel = rs.getString(key + ".short");
        return edge;
    }

    /**
     * @return the label shown under its icon in the common tools, or null if it isn't one of their prototypes
     */
    public String getShortToolLabel()
    {
        return this.shortToolLabel;
    }

    /**
     * A bent edge goes down in its tool icon, else its bends wouldn't show
     */
    @Override
    public double getToolIconSlope()
    {
        BentStyle bentStyle = getBentStyle();
        boolean isBent = BentStyle.HV.equals(bentStyle) || BentStyle.VH.equals(bentStyle) || BentStyle.HVH.equals(bentStyle)
                || BentStyle.VHV.equals(bentStyle) || BentStyle.AUTO.equals(bentStyle);
        return isBent ? BENT_TOOL_ICON_SLOPE : 0;
    }

    /**
     * Name of the element written by the former NoteEdge class in the diagram files.
     */
    public static final String LEGACY_NOTE_EDGE_TAG = "noteEdge";

    /** Tool tips and short labels of the common tools */
    private static final String STRINGS = "com.horstmann.violet.product.diagram.common.GenericEdgeStrings";

    private static final double BENT_TOOL_ICON_SLOPE = 0.5;

    /** Not stored in the diagram files, as the tool tip */
    private transient String shortToolLabel;
}
