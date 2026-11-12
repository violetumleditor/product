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

package com.horstmann.violet.product.diagram.activity;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.ConnectionRule;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * An UML activity diagram.
 */
public class ActivityDiagramGraph extends AbstractGraph
{
    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    @Override
    public boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation)
    {
        if (!ActivityTransitionEdge.class.isInstance(e))
        {
            return super.connect(e, start, startLocation, end, endLocation);
        }
        ActivityTransitionEdge transitionEdge = (ActivityTransitionEdge) e;
        if (DecisionNode.class.isInstance(start))
        {
            boolean isSyncBarAtEnd = SynchronizationBarNode.class.isInstance(end);
            if (isSyncBarAtEnd)
            {
                // For syn bar, we want to connect edge to north or south points
                transitionEdge.setBentStyle(BentStyle.AUTO);
            }
            if (!isSyncBarAtEnd)
            {
                // For all the other cases, decision node are connected from east or west
                transitionEdge.setBentStyle(BentStyle.HV);
            }
        }
        return super.connect(e, start, startLocation, end, endLocation);
    }

    @Override
    protected List<ConnectionRule> getConnectionRules()
    {
        return CONNECTION_RULES;
    }

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    /**
     * Start node type, end node type, edge types that can go from the first to the second, as in UML: control flows join actions,
     * decisions, fork/join bars and signals; nothing flows into the initial node nor out of the final node, and the initial node
     * doesn't go straight to the final one (an empty activity).
     */
    private static final List<ConnectionRule> CONNECTION_RULES = List.of(
            new ConnectionRule(ScenarioStartNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ScenarioStartNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ScenarioStartNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ScenarioStartNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ScenarioStartNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(ActivityNode.class, ScenarioEndNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(DecisionNode.class, ScenarioEndNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SynchronizationBarNode.class, ScenarioEndNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalSendingNode.class, ScenarioEndNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, ActivityNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, DecisionNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, SynchronizationBarNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, SignalSendingNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, SignalReceiptNode.class, ActivityTransitionEdge.class),
            new ConnectionRule(SignalReceiptNode.class, ScenarioEndNode.class, ActivityTransitionEdge.class));

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(ActivityDiagramConstant.ACTIVITY_DIAGRAM_STRINGS, Locale.getDefault());

        ScenarioStartNode node5 = new ScenarioStartNode();
        node5.setToolTip(rs.getString("node5.tooltip"));
        NODE_PROTOTYPES.add(node5);
        
        ActivityNode node0 = new ActivityNode();
        node0.setToolTip(rs.getString("node0.tooltip"));
        NODE_PROTOTYPES.add(node0);

        DecisionNode node1 = new DecisionNode();
        node1.setToolTip(rs.getString("node1.tooltip"));
        NODE_PROTOTYPES.add(node1);

        SynchronizationBarNode node2 = new SynchronizationBarNode();
        node2.setToolTip(rs.getString("node2.tooltip"));
        NODE_PROTOTYPES.add(node2);

        SignalSendingNode node3 = new SignalSendingNode();
        node3.setToolTip(rs.getString("node3.tooltip"));
        NODE_PROTOTYPES.add(node3);

        SignalReceiptNode node4 = new SignalReceiptNode();
        node4.setToolTip(rs.getString("node4.tooltip"));
        NODE_PROTOTYPES.add(node4);

        ScenarioEndNode node6 = new ScenarioEndNode();
        node6.setToolTip(rs.getString("node6.tooltip"));
        NODE_PROTOTYPES.add(node6);

        NoteNode node7 = new NoteNode();
        node7.setToolTip(rs.getString("node7.tooltip"));
        NODE_PROTOTYPES.add(node7);

        ImageNode imageNode = new ImageNode();
        imageNode.setToolTip(rs.getString("image.tooltip"));
        NODE_PROTOTYPES.add(imageNode);

        ContainerNode node8 = new ContainerNode();
        node8.setToolTip(rs.getString("node8.tooltip"));
        node8.enableToolIcon();
        NODE_PROTOTYPES.add(node8);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);

        ActivityTransitionEdge transition = new ActivityTransitionEdge();
        transition.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(transition);

        GenericEdge genericEdge = new GenericEdge();
        genericEdge.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(genericEdge);
    }

}
