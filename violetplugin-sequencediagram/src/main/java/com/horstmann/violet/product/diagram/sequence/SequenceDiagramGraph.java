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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.ConnectionRule;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * A UML sequence diagram.
 */
public class SequenceDiagramGraph extends AbstractGraph
{

    
    
    
    @Override
    public boolean addNode(INode newNode, Point2D p)
    {
    	INode foundNode = findNode(p);
        if ((foundNode == null || foundNode instanceof ContainerNode || foundNode instanceof FreeTextNode)
                && newNode.getClass().isAssignableFrom(ActivationBarNode.class)) {
            return false;
        }
        return super.addNode(newNode, p);
    }

    
    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    @Override
    protected List<ConnectionRule> getConnectionRules()
    {
        return CONNECTION_RULES;
    }

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    /**
     * Start node type, end node type, edge types that can go from the first to the second, as in UML: messages are sent during an
     * execution (activation bar), to another execution or to a lifeline (a call ending on a lifeline creates the activation bar it
     * ends on, or the object on its top); a reply goes back from an execution to the one that called it. Nothing leaves a lifeline
     * itself.
     */
    private static final List<ConnectionRule> CONNECTION_RULES = List.of(
            new ConnectionRule(ActivationBarNode.class, ActivationBarNode.class, CallEdge.class, ReturnEdge.class),
            new ConnectionRule(ActivationBarNode.class, LifelineNode.class, CallEdge.class));

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(SequenceDiagramConstant.SEQUENCE_DIAGRAM_STRINGS, Locale.getDefault());
        
        LifelineNode lifelineNode = new LifelineNode();
        lifelineNode.setToolTip(rs.getString("node0.tooltip"));
        NODE_PROTOTYPES.add(lifelineNode);
        
        ActivationBarNode activationBarNode = new ActivationBarNode();
        activationBarNode.setToolTip(rs.getString("node1.tooltip"));
        NODE_PROTOTYPES.add(activationBarNode);
        
        NoteNode noteNode = new NoteNode();
        noteNode.setToolTip(rs.getString("node2.tooltip"));
        NODE_PROTOTYPES.add(noteNode);

        ImageNode imageNode = new ImageNode();
        imageNode.setToolTip(rs.getString("image.tooltip"));
        NODE_PROTOTYPES.add(imageNode);

        ContainerNode containerNode = new ContainerNode();
        containerNode.setToolTip(rs.getString("node3.tooltip"));
        containerNode.enableToolIcon();
        NODE_PROTOTYPES.add(containerNode);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);
        
        CallEdge callEdge = new CallEdge();
        callEdge.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(callEdge);
        
        ReturnEdge returnEdge = new ReturnEdge();
        returnEdge.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(returnEdge);
        
        GenericEdge genericEdge = new GenericEdge();
        genericEdge.setToolTip(rs.getString("edge2.tooltip"));
        EDGE_PROTOTYPES.add(genericEdge);
    }

}
