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

package com.horstmann.violet.product.diagram.object;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * An UML-style object diagram that shows object references.
 */
public class ObjectDiagramGraph extends AbstractGraph
{
    
    @Override
    public boolean addNode(INode newNode, Point2D p)
    {
        INode foundNode = findNode(p);
        if ((foundNode == null || foundNode instanceof ContainerNode || foundNode instanceof FreeTextNode)
                && newNode.getClass().isAssignableFrom(FieldNode.class)) {
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

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(ObjectDiagramConstant.OBJECT_DIAGRAM_STRINGS, Locale.getDefault());

        ObjectNode node0 = new ObjectNode();
        node0.setToolTip(rs.getString("node0.tooltip"));
        NODE_PROTOTYPES.add(node0);

        FieldNode node1 = new FieldNode();
        node1.setToolTip(rs.getString("node1.tooltip"));
        MultiLineString fn = new MultiLineString();
        fn.setText("name");
        node1.setName(fn);
        MultiLineString fv = new MultiLineString();
        fv.setText("value");
        node1.setValue(fv);
        NODE_PROTOTYPES.add(node1);

        NoteNode node2 = new NoteNode();
        node2.setToolTip(rs.getString("node2.tooltip"));
        NODE_PROTOTYPES.add(node2);

        ImageNode imageNode = new ImageNode();
        imageNode.setToolTip(rs.getString("image.tooltip"));
        NODE_PROTOTYPES.add(imageNode);

        ContainerNode node3 = new ContainerNode();
        node3.setToolTip(rs.getString("node3.tooltip"));
        node3.enableToolIcon();
        NODE_PROTOTYPES.add(node3);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);

        ObjectReferenceEdge reference = new ObjectReferenceEdge();
        reference.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(reference);

        ObjectRelationshipEdge association = new ObjectRelationshipEdge();
        association.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(association);

        GenericEdge genericEdge = new GenericEdge();
        genericEdge.setToolTip(rs.getString("edge2.tooltip"));
        EDGE_PROTOTYPES.add(genericEdge);
    }

}
