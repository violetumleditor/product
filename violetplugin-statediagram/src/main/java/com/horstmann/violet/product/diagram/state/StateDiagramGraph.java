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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * An UML state diagram.
 */
public class StateDiagramGraph extends AbstractGraph
{
    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(StateDiagramConstant.STATE_DIAGRAM_STRINGS, Locale.getDefault());
        
        CircularInitialStateNode circularInitialStateNode = new CircularInitialStateNode();
        circularInitialStateNode.setToolTip(rs.getString("node1.tooltip"));
        NODE_PROTOTYPES.add(circularInitialStateNode);
        
        StateNode stateNode = new StateNode();
        stateNode.setToolTip(rs.getString("node0.tooltip"));
        NODE_PROTOTYPES.add(stateNode);
        
        CircularFinalStateNode circularFinalStateNode = new CircularFinalStateNode();
        circularFinalStateNode.setToolTip(rs.getString("node2.tooltip"));
        NODE_PROTOTYPES.add(circularFinalStateNode);
        
        NoteNode noteNode = new NoteNode();
        noteNode.setToolTip(rs.getString("node3.tooltip"));
        NODE_PROTOTYPES.add(noteNode);

        ImageNode imageNode = new ImageNode();
        imageNode.setToolTip(rs.getString("image.tooltip"));
        NODE_PROTOTYPES.add(imageNode);

        ContainerNode containerNode = new ContainerNode();
        containerNode.setToolTip(rs.getString("node4.tooltip"));
        containerNode.enableToolIcon();
        NODE_PROTOTYPES.add(containerNode);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);

        StateTransitionEdge stateTransitionEdge = new StateTransitionEdge();
        stateTransitionEdge.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(stateTransitionEdge);
        
        GenericEdge genericEdge = new GenericEdge();
        genericEdge.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(genericEdge);
    }

}
