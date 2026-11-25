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

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.ConnectionRule;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.MarkerNode;
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
        if (newNode instanceof ActivationBarNode && putBack((ActivationBarNode) newNode))
        {
            return true;
        }
        return canAddNode(newNode, p) && super.addNode(newNode, p);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        new ActivationBarSpacing(this).apply();
        super.draw(g2);
        getDrawnBounds().clear();
        for (INode aNode : getAllNodes())
        {
            if (isParticipant(aNode))
            {
                Point2D locationOnGraph = aNode.getLocationOnGraph();
                Rectangle2D bounds = aNode.getBounds();
                getDrawnBounds().put(aNode, new Rectangle2D.Double(locationOnGraph.getX(),
                        locationOnGraph.getY(), bounds.getWidth(), bounds.getHeight()));
            }
        }
    }

    /**
     * Called on each press, so a drag starts again from the bars as they are then
     */
    @Override
    public INode findNode(Point2D p)
    {
        this.linkedBarsMover = null;
        return super.findNode(p);
    }

    /**
     * The bars linked to a dragged bar move with it, or hold it back
     */
    private void moveLinkedBars(ActivationBarNode draggedBar)
    {
        if (draggedBar.getParent() == null)
        {
            return;
        }
        if (this.linkedBarsMover == null || this.linkedBarsMover.getDraggedBar() != draggedBar)
        {
            if (!getDrawnBounds().containsKey(draggedBar))
            {
                return;
            }
            this.linkedBarsMover = new LinkedBarsMover(this, draggedBar, getDrawnBounds());
        }
        this.linkedBarsMover.follow();
    }

    /**
     * @return the bounds on graph of the bars and lifelines, as last drawn
     */
    private Map<INode, Rectangle2D> getDrawnBounds()
    {
        if (this.drawnBounds == null)
        {
            this.drawnBounds = new HashMap<>();
        }
        return this.drawnBounds;
    }

    @Override
    public Rectangle2D getClipBounds()
    {
        new ActivationBarSpacing(this).apply();
        return super.getClipBounds();
    }

    /**
     * A removed bar added again (undo, redo) goes back where it was in its parent, whatever lies at the point
     */
    private boolean putBack(ActivationBarNode bar)
    {
        INode parent = bar.getParent();
        if (bar.getRemovedRank() == null || parent == null || !getAllNodes().contains(parent))
        {
            return false;
        }
        int rank = Math.min(bar.getRemovedRank(), parent.getChildren().size());
        bar.setRemovedRank(null);
        return parent.addChild(bar, rank);
    }

    /**
     * A node removed with one it is in stays in it, so that it comes back with it
     */
    @Override
    public void removeNode(INode... nodesToRemove)
    {
        Messages messages = new Messages(this);
        List<INode> topNodes = new ArrayList<INode>();
        for (INode aNode : nodesToRemove)
        {
            if (Arrays.stream(nodesToRemove).noneMatch(n -> n != aNode && messages.isInside(aNode, n)))
            {
                topNodes.add(aNode);
            }
        }
        for (INode aNode : topNodes)
        {
            int rank = aNode.getParent() == null ? -1 : aNode.getParent().getChildren().indexOf(aNode);
            if (aNode instanceof ActivationBarNode && rank >= 0)
            {
                ((ActivationBarNode) aNode).setRemovedRank(rank);
            }
        }
        super.removeNode(topNodes.toArray(new INode[0]));
    }

    @Override
    public void removeEdge(IEdge... edgesToRemove)
    {
        for (IEdge anEdge : edgesToRemove)
        {
            if (anEdge instanceof MessageEdge)
            {
                ((MessageEdge) anEdge).rememberRemoval();
            }
            else if (isBetweenParticipants(anEdge.getStartNode(), anEdge.getEndNode()))
            {
                getRemovedGenericEdges().add(anEdge);
            }
        }
        super.removeEdge(edgesToRemove);
    }

    /**
     * Lifelines and activation bars are only linked by messages, never by generic edges. Those of older files stay, and come back
     * once removed (undo).
     */
    @Override
    public boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation)
    {
        boolean isPutBack = getRemovedGenericEdges().contains(e) && e.getStartNode() == start && e.getEndNode() == end;
        if (e instanceof GenericEdge && isBetweenParticipants(start, end) && !isPutBack)
        {
            return false;
        }
        return super.connect(e, start, startLocation, end, endLocation);
    }

    /**
     * @return true if both nodes are lifelines or activation bars
     */
    private boolean isBetweenParticipants(INode start, INode end)
    {
        return isParticipant(start) && isParticipant(end);
    }

    private boolean isParticipant(INode node)
    {
        return node instanceof LifelineNode || node instanceof ActivationBarNode;
    }

    /**
     * @return the generic edges of an older file, between lifelines or bars, that were removed
     */
    private Set<IEdge> getRemovedGenericEdges()
    {
        if (this.removedGenericEdges == null)
        {
            this.removedGenericEdges = Collections.newSetFromMap(new IdentityHashMap<>());
        }
        return this.removedGenericEdges;
    }

    /**
     * The lifelines stay at y = 0, close to the top
     */
    @Override
    public int getTopMargin()
    {
        return 10;
    }

    /**
     * An activation bar only goes into a lifeline or another activation bar, never on the graph itself
     */
    @Override
    public boolean canAddNode(INode newNode, Point2D p)
    {
        return (!(newNode instanceof ActivationBarNode) || isParticipant(findNodeUnderMarkers(p))) && super.canAddNode(newNode, p);
    }

    /**
     * A bar never changes its parent by drag, but the linked bars move with it. Called at each step of the drag of a node, never by undo
     * nor redo, which restore the linked bars by themselves.
     */
    @Override
    public INode findNewParentOnDrop(INode draggedNode, Point2D p)
    {
        if (draggedNode instanceof ActivationBarNode)
        {
            moveLinkedBars((ActivationBarNode) draggedNode);
        }
        return super.findNewParentOnDrop(draggedNode, p);
    }

    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    /**
     * Between two activation bars, the rules allow a call and a return: only one of them is offered, so it is drawn straight away. A
     * link towards the bar that called the starting one is its return, any other one is a call. A call refused by {@link CallRules}
     * is not offered, nor is a generic edge between lifelines and bars: nothing may be possible.
     */
    @Override
    public List<IEdge> getEdgePrototypes(INode start, INode end)
    {
        return super.getEdgePrototypes(start, end).stream().filter(e -> isPossible(e, start, end)).toList();
    }

    private boolean isPossible(IEdge prototype, INode start, INode end)
    {
        if (prototype instanceof GenericEdge)
        {
            return !isBetweenParticipants(start, end);
        }
        CallRules callRules = new CallRules(this);
        boolean isReturn = callRules.isReturnAcceptable(start, end);
        if (prototype instanceof ReturnEdge)
        {
            return isReturn;
        }
        // A bar that is not on the graph yet can be called
        boolean isBetweenBars = start instanceof ActivationBarNode && end instanceof ActivationBarNode && end.getParent() != null;
        return !isReturn && (!isBetweenBars || callRules.isCallAcceptable((ActivationBarNode) start, (ActivationBarNode) end));
    }

    @Override
    protected List<ConnectionRule> getConnectionRules()
    {
        return CONNECTION_RULES;
    }

    private transient Map<INode, Rectangle2D> drawnBounds;

    private transient Set<IEdge> removedGenericEdges;

    /** The current drag of a bar */
    private transient LinkedBarsMover linkedBarsMover;

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
        
        UserLifelineNode userLifelineNode = new UserLifelineNode();
        userLifelineNode.setToolTip(rs.getString("userlifeline.tooltip"));
        NODE_PROTOTYPES.add(userLifelineNode);

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

        NODE_PROTOTYPES.add(MarkerNode.createToolPrototype());
        
        CallEdge callEdge = new CallEdge();
        callEdge.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(callEdge);
        
        ReturnEdge returnEdge = new ReturnEdge();
        returnEdge.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(returnEdge);
        
        EDGE_PROTOTYPES.addAll(GenericEdge.createToolPrototypes());
    }

}
