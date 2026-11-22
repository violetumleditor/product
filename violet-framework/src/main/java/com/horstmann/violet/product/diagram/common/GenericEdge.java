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
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
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
     * @return the generic edges of the common tools, in their order: line, arrow, double arrow, dotted line, bent arrow. All
     *         are routed automatically; the bent arrow, the only one drawn bent in the tool bar, also has rounded corners, to
     *         show that links can have some. Each one has its tool tip and its short label. Static, as the diagrams build
     *         their prototypes in a static block.
     */
    public static List<IEdge> createToolPrototypes()
    {
        ResourceBundle rs = ResourceBundle.getBundle(STRINGS, Locale.getDefault());
        List<IEdge> prototypes = new ArrayList<IEdge>();
        prototypes.add(createToolPrototype(rs, "line", LineStyle.SOLID, AngleStyle.RAW, ArrowHead.NONE, ArrowHead.NONE));
        prototypes.add(createToolPrototype(rs, "arrow", LineStyle.SOLID, AngleStyle.RAW, ArrowHead.NONE, ArrowHead.V));
        prototypes.add(createToolPrototype(rs, "doublearrow", LineStyle.SOLID, AngleStyle.RAW, ArrowHead.V, ArrowHead.V));
        prototypes.add(createToolPrototype(rs, "dotted", LineStyle.DOTTED, AngleStyle.RAW, ArrowHead.NONE, ArrowHead.NONE));
        GenericEdge bent = createToolPrototype(rs, "bent", LineStyle.SOLID, AngleStyle.ROUNDED2, ArrowHead.NONE, ArrowHead.V);
        bent.bentToolIcon = true;
        prototypes.add(bent);
        return prototypes;
    }

    private static GenericEdge createToolPrototype(ResourceBundle rs, String key, LineStyle lineStyle, AngleStyle angleStyle,
            ArrowHead startArrowHead, ArrowHead endArrowHead)
    {
        GenericEdge edge = new GenericEdge();
        edge.setLineStyle(lineStyle);
        edge.setBentStyle(BentStyle.AUTO);
        edge.setAngleStyle(angleStyle);
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
     * The bent arrow of the common tools goes down in its tool icon, else its bends wouldn't show. The other ones, also
     * automatic, stay horizontal, so they draw straight.
     */
    @Override
    public double getToolIconSlope()
    {
        return this.bentToolIcon ? BENT_TOOL_ICON_SLOPE : 0;
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

    /** True for the bent arrow of the common tools, whose tool icon goes down. Not stored in the diagram files */
    private transient boolean bentToolIcon;
}
