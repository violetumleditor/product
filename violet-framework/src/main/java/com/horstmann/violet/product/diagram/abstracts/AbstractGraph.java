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

package com.horstmann.violet.product.diagram.abstracts;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.node.IMovableLabelNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.common.MarkerNode;

/**
 * A graph consisting of selectable nodes and edges.
 */
public abstract class AbstractGraph implements Serializable, Cloneable, IGraph
{
    /**
     * Constructs a graph with no nodes or edges.
     */
    public AbstractGraph()
    {
        nodes = new ArrayList<INode>();
        edges = new ArrayList<IEdge>();
    }

    @Override
    public INode findNode(Point2D p)
    {
        GeometryCache.begin();
        try
        {
            MarkerNode marker = findMarker(p);
            return marker != null ? marker : findNodeUnderMarkers(p);
        }
        finally
        {
            GeometryCache.end();
        }
    }

    /**
     * @return the most recent marker stroke passing at this point, or null
     */
    private MarkerNode findMarker(Point2D p)
    {
        for (INode n : getAllNodes())
        {
            if (n instanceof MarkerNode && ((MarkerNode) n).isOnStroke(p))
            {
                return (MarkerNode) n;
            }
        }
        return null;
    }

    @Override
    public INode findNodeUnderMarkers(Point2D p)
    {
        GeometryCache.begin();
        try
        {
            return findNodeUnderMarkersInPass(p);
        }
        finally
        {
            GeometryCache.end();
        }
    }

    private INode findNodeUnderMarkersInPass(Point2D p)
    {
        // Free texts are on top : they are found first
        // Containers are in background : they are only found if no other node is at this point
        // The text a node draws outside its bounds comes between both
        INode foundNode = null;
        INode foundByLabel = null;
        ContainerNode smallestContainer = null;
        for (INode n : getAllNodes())
        {
            if (n instanceof MarkerNode)
            {
                continue;
            }
            Point2D locationOnGraph = n.getLocationOnGraph();
            Rectangle2D bounds = n.getBounds();
            Rectangle2D boundsToCheck = new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(),
                    bounds.getHeight());
            if (!boundsToCheck.contains(p))
            {
                Rectangle2D labelBounds = n.getOutsideLabelBounds();
                if (foundByLabel == null && labelBounds != null && labelBounds.contains(p))
                {
                    foundByLabel = n;
                }
                continue;
            }
            if (n instanceof FreeTextNode)
            {
                return n;
            }
            if (!(n instanceof ContainerNode))
            {
                foundNode = foundNode == null ? n : foundNode;
                continue;
            }
            ContainerNode aContainer = (ContainerNode) n;
            if (smallestContainer == null || aContainer.getArea() < smallestContainer.getArea())
            {
                smallestContainer = aContainer;
            }
        }
        if (foundNode == null)
        {
            foundNode = foundByLabel;
        }
        return foundNode != null ? foundNode : smallestContainer;
    }

    @Override
    public INode findNode(Id id)
    {
        for (INode n : getAllNodes())
        {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    @Override
    public IEdge findEdge(Point2D p)
    {
        GeometryCache.begin();
        try
        {
            for (IEdge e : edges)
            {
                if (isNear(e, p) && e.contains(p) && !isLineOnNodeLabel(e, p) && !isLineUnderMarker(e, p)) return e;
            }
            return null;
        }
        finally
        {
            GeometryCache.end();
        }
    }

    /**
     * @return false when the point is far from the edge, which then needs no exact (and costly) hit test; always true without
     *         the geometry cache, so that its bounds aren't computed each time
     */
    private boolean isNear(IEdge e, Point2D p)
    {
        if (!GeometryCache.isActive())
        {
            return true;
        }
        Rectangle2D bounds = e.getBounds();
        return bounds == null || new Rectangle2D.Double(bounds.getX() - HIT_MARGIN, bounds.getY() - HIT_MARGIN,
                bounds.getWidth() + 2 * HIT_MARGIN, bounds.getHeight() + 2 * HIT_MARGIN).contains(p);
    }

    /**
     * A marker stroke is upon the edges, so it is taken rather than them; their transition points can still be dragged
     */
    private boolean isLineUnderMarker(IEdge e, Point2D p)
    {
        return e.findTransitionPoint(p) == null && findMarker(p) != null;
    }

    /**
     * The text of a node crossed by a line (a flow leaving the bottom of an event) is taken rather than this line, so that it can
     * be dragged away; the texts of the edge and its transition points stay first
     */
    private boolean isLineOnNodeLabel(IEdge e, Point2D p)
    {
        if (e instanceof IMovableLabelElement && ((IMovableLabelElement) e).findLabel(p) != null)
        {
            return false;
        }
        if (e.findTransitionPoint(p) != null)
        {
            return false;
        }
        INode node = findNode(p);
        return node instanceof IMovableLabelNode && ((IMovableLabelNode) node).findLabel(p) != null;
    }

    @Override
    public IEdge findEdge(Id id)
    {
        for (IEdge e : edges)
        {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }
    
    


    /**
     * Lists the nodes in the order they are painted : the most recent ones upon the oldest ones, as {@link #findNode(Point2D)}
     * finds them first. Children come before their parent, which paints them again upon its background.
     *
     * @return all the nodes, from the bottom to the top
     */
    private List<INode> getNodesFromBottomToTop()
    {
        List<INode> orderedNodes = new ArrayList<INode>();
        for (INode aNode : this.nodes)
        {
            addChildrenThenNode(aNode, orderedNodes);
        }
        return orderedNodes;
    }

    private void addChildrenThenNode(INode node, List<INode> orderedNodes)
    {
        for (INode aChild : node.getChildren())
        {
            addChildrenThenNode(aChild, orderedNodes);
        }
        orderedNodes.add(node);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        draw(g2, null);
    }

    @Override
    public void draw(Graphics2D g2, Predicate<Object> isDrawn)
    {
        GeometryCache.begin();
        try
        {
            drawInPass(g2, isDrawn);
        }
        finally
        {
            GeometryCache.end();
        }
    }

    /**
     * @return the part of the graph to paint, enlarged by a margin, or null to paint everything (an export, an image without
     *         clip, or the optimizations turned off)
     */
    private Rectangle2D getCullingArea(Graphics2D g2)
    {
        if (!GeometryCache.isActive())
        {
            return null;
        }
        Rectangle clip = g2.getClipBounds();
        if (clip == null)
        {
            return null;
        }
        return new Rectangle2D.Double(clip.getX() - CULLING_MARGIN, clip.getY() - CULLING_MARGIN,
                clip.getWidth() + 2 * CULLING_MARGIN, clip.getHeight() + 2 * CULLING_MARGIN);
    }

    /**
     * @return true if the node may paint something in the area (or there is no area): the node itself, or its text outside
     */
    private boolean isInArea(INode n, Rectangle2D area)
    {
        if (area == null)
        {
            return true;
        }
        Point2D locationOnGraph = n.getLocationOnGraph();
        Rectangle2D bounds = n.getBounds();
        if (area.intersects(locationOnGraph.getX(), locationOnGraph.getY(), Math.max(1, bounds.getWidth()),
                Math.max(1, bounds.getHeight())))
        {
            return true;
        }
        Rectangle2D labelBounds = n.getOutsideLabelBounds();
        return labelBounds != null && area.intersects(labelBounds);
    }

    /**
     * @return true if the edge may paint something in the area (or there is no area)
     */
    private boolean isInArea(IEdge e, Rectangle2D area)
    {
        if (area == null)
        {
            return true;
        }
        Rectangle2D bounds = e.getBounds();
        return bounds == null || area.intersects(bounds.getX(), bounds.getY(), Math.max(1, bounds.getWidth()),
                Math.max(1, bounds.getHeight()));
    }

    private void drawInPass(Graphics2D g2, Predicate<Object> isDrawn)
    {
        Rectangle2D area = getCullingArea(g2);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<INode> specialNodes = new ArrayList<INode>();
        Collection<INode> nodes = getNodesFromBottomToTop();

        // Containers are always drawn in background, the smallest ones upon the largest ones
        List<ContainerNode> containerNodes = new ArrayList<ContainerNode>();
        for (INode n : nodes)
        {
            if (n instanceof ContainerNode)
            {
                containerNodes.add((ContainerNode) n);
            }
        }
        containerNodes.sort(Comparator.comparingDouble(ContainerNode::getArea).reversed());
        for (ContainerNode aContainer : containerNodes)
        {
            if ((isDrawn == null || isDrawn.test(aContainer)) && isInArea(aContainer, area))
            {
                aContainer.draw(g2);
            }
        }

        // By increasing Z, in the bottom to top order at the same Z (a stable sort): one pass instead of one per Z
        List<INode> nodesByZ = new ArrayList<INode>(nodes);
        nodesByZ.sort(Comparator.comparingInt(INode::getZ));
        for (INode n : nodesByZ)
        {
            if (n instanceof ContainerNode)
            {
                // Already drawn
            }
            else if (n instanceof NoteNode || n instanceof FreeTextNode || n instanceof MarkerNode)
            {
                specialNodes.add(n);
            }
            else if ((isDrawn == null || isDrawn.test(n)) && isInArea(n, area))
            {
                Stroke savedStroke = g2.getStroke();
                if (n instanceof AbstractNode && ((AbstractNode) n).getBorderWidth() > 1)
                {
                    g2.setStroke(new BasicStroke(((AbstractNode) n).getBorderWidth()));
                }
                n.draw(g2);
                g2.setStroke(savedStroke);
            }
        }

        for (int i = 0; i < edges.size(); i++)
        {
            IEdge e = (IEdge) edges.get(i);
            if ((isDrawn == null || isDrawn.test(e)) && isInArea(e, area))
            {
                e.draw(g2);
            }
        }
        // Special nodes are always drawn upon other elements
        for (INode n : specialNodes)
        {
            if ((isDrawn != null && !isDrawn.test(n)) || !isInArea(n, area))
            {
                continue;
            }
            // Translate g2 if node has parent
            Point2D nodeLocationOnGraph = n.getLocationOnGraph();
            Point2D nodeLocation = n.getLocation();
            Point2D g2Location = new Point2D.Double(nodeLocationOnGraph.getX() - nodeLocation.getX(), nodeLocationOnGraph.getY()
                    - nodeLocation.getY());
            g2.translate(g2Location.getX(), g2Location.getY());
            Stroke savedSpecialStroke = g2.getStroke();
            if (n instanceof AbstractNode && ((AbstractNode) n).getBorderWidth() > 1)
            {
                g2.setStroke(new BasicStroke(((AbstractNode) n).getBorderWidth()));
            }
            n.draw(g2);
            g2.setStroke(savedSpecialStroke);
            // Restore g2 original location
            g2.translate(-g2Location.getX(), -g2Location.getY());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.product.diagram.abstracts.IGraph#getBounds()
     */
    public Rectangle2D getClipBounds()
    {
        GeometryCache.begin();
        try
        {
            return getClipBoundsInPass();
        }
        finally
        {
            GeometryCache.end();
        }
    }

    private Rectangle2D getClipBoundsInPass()
    {
        Rectangle2D r = null;
        for (INode n : nodes)
        {
            Rectangle2D b = n.getBounds();
            if (r == null) r = b;
            else r.add(b);
            Rectangle2D labelBounds = n.getOutsideLabelBounds();
            if (labelBounds != null) r.add(labelBounds);
        }
        for (IEdge e : edges)
        {
            r.add(e.getBounds());
        }
        return r == null ? new Rectangle2D.Double() : new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public void setBounds(Rectangle2D newValue)
    {
        minBounds = newValue;
    }

    @Override
    public abstract List<INode> getNodePrototypes();

    @Override
    public abstract List<IEdge> getEdgePrototypes();

    @Override
    public List<IEdge> getEdgePrototypes(INode start, INode end)
    {
        List<IEdge> possibleEdges = new ArrayList<IEdge>();
        if (!isCommonNode(start) && !isCommonNode(end))
        {
            for (IEdge aPrototype : getEdgePrototypes())
            {
                if (!(aPrototype instanceof GenericEdge) && isAllowedByRules(aPrototype, start, end))
                {
                    possibleEdges.add(aPrototype);
                }
            }
        }
        if (possibleEdges.isEmpty())
        {
            for (IEdge aPrototype : getEdgePrototypes())
            {
                if (aPrototype instanceof GenericEdge)
                {
                    possibleEdges.add(aPrototype);
                }
            }
        }
        return possibleEdges;
    }

    @Override
    public Collection<INode> getAllNodes()
    {
        // Asked by each lookup of a node, and by some nodes while they are drawn: listed once per pass over the graph
        @SuppressWarnings("unchecked")
        Collection<INode> cachedNodes = (Collection<INode>) GeometryCache.get(this, ALL_NODES_CACHE_KEY);
        if (cachedNodes != null)
        {
            return cachedNodes;
        }
        ArrayDeque<INode> fifo = new ArrayDeque<INode>(nodes);
        List<INode> allNodes = new ArrayList<INode>(nodes);
        while (!fifo.isEmpty())
        {
            INode nodeToInspect = fifo.poll();
            List<INode> children = nodeToInspect.getChildren();
            fifo.addAll(children);
            allNodes.addAll(children);
        }
        // Let's have children first
        Collections.reverse(allNodes);
        Collection<INode> result = Collections.unmodifiableCollection(allNodes);
        GeometryCache.put(this, ALL_NODES_CACHE_KEY, result);
        return result;
    }

    @Override
    public Collection<IEdge> getAllEdges()
    {
        return Collections.unmodifiableCollection(edges);
    }

    @Override
    public boolean addNode(INode newNode, Point2D p)
    {
        newNode.setId(new Id());
        newNode.setGraph(this);

        // Calculate location offset for cropped nodes (e.g. ImageNode)
        // so that the visible top-left corner matches the paste point 'p'.
        Point2D locationOnGraph = newNode.getLocationOnGraph();
        Point2D rawLocation = newNode.getLocation();
        double offsetX = locationOnGraph.getX() - rawLocation.getX();
        double offsetY = locationOnGraph.getY() - rawLocation.getY();
        Point2D adjustedPoint = new Point2D.Double(p.getX() - offsetX, p.getY() - offsetY);

        // Case 1 : Note, container, free text, image and marker nodes always attached to the graph
        if (newNode instanceof NoteNode || newNode instanceof ContainerNode || newNode instanceof FreeTextNode
                || newNode instanceof ImageNode || newNode instanceof MarkerNode)
        {
            newNode.setLocation(adjustedPoint);
            nodes.add(newNode);
            return true;
        }
        // Case 2 : attached to an existing node (containers and free texts never have children)
        INode potentialParentNode = findNodeUnderMarkers(p);
        if (potentialParentNode != null && !(potentialParentNode instanceof ContainerNode)
                && !(potentialParentNode instanceof FreeTextNode))
        {
            Point2D parentLocationOnGraph = potentialParentNode.getLocationOnGraph();
            Point2D relativeLocation = new Point2D.Double(adjustedPoint.getX() - parentLocationOnGraph.getX(), adjustedPoint.getY()
                    - parentLocationOnGraph.getY());
            return potentialParentNode.addChild(newNode, relativeLocation);
        }
        // Case 3 : attached directly to the graph
        newNode.setLocation(adjustedPoint);
        newNode.setParent(null);
        nodes.add(newNode);
        return true;
    }

    /**
     * Same cases as {@link #addNode(INode, Point2D)}: a common element always goes on the graph, another node into the node found
     * there if it accepts it as a child ({@link INode#canAddChild(INode)}), else on the graph when nothing (or a grouping area, or a
     * free text) is there. A diagram refusing some nodes on the graph itself overrides both methods.
     */
    @Override
    public boolean canAddNode(INode newNode, Point2D p)
    {
        if (newNode instanceof NoteNode || newNode instanceof ContainerNode || newNode instanceof FreeTextNode
                || newNode instanceof ImageNode || newNode instanceof MarkerNode)
        {
            return true;
        }
        INode potentialParentNode = findNodeUnderMarkers(p);
        if (potentialParentNode != null && !(potentialParentNode instanceof ContainerNode)
                && !(potentialParentNode instanceof FreeTextNode))
        {
            return potentialParentNode.canAddChild(newNode);
        }
        return true;
    }

    /**
     * For the nodes this diagram lets change their parent ({@link #isMovedByDrop(INode)}): the node under the point, in the order
     * of {@link #findNode(Point2D)} (children first), that accepts the dragged node as a child; null if there is none.
     */
    @Override
    public INode findNewParentOnDrop(INode draggedNode, Point2D p)
    {
        if (!isMovedByDrop(draggedNode))
        {
            return null;
        }
        return findNodeTakingChild(draggedNode, p);
    }

    /**
     * @param node a node of this graph
     * @return true if the node, dragged and dropped on a node that accepts it as a child, moves into it (see
     *         {@link #findNewParentOnDrop(INode, Point2D)}); false by default
     */
    protected boolean isMovedByDrop(INode node)
    {
        return false;
    }

    private INode findNodeTakingChild(INode child, Point2D p)
    {
        for (INode aNode : getAllNodes())
        {
            if (aNode instanceof ContainerNode || aNode instanceof FreeTextNode || isSameOrDescendant(aNode, child))
            {
                continue;
            }
            Point2D locationOnGraph = aNode.getLocationOnGraph();
            Rectangle2D bounds = aNode.getBounds();
            Rectangle2D boundsOnGraph = new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(),
                    bounds.getHeight());
            if (boundsOnGraph.contains(p) && aNode.canAddChild(child))
            {
                return aNode;
            }
        }
        return null;
    }

    private boolean isSameOrDescendant(INode node, INode ancestor)
    {
        for (INode aNode = node; aNode != null; aNode = aNode.getParent())
        {
            if (aNode == ancestor)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void changeParent(INode node, INode newParent, int rank)
    {
        INode oldParent = node.getParent();
        if (oldParent != null)
        {
            oldParent.removeChild(node);
        }
        else
        {
            this.nodes.remove(node);
        }
        if (newParent != null)
        {
            newParent.addChild(node, rank < 0 ? newParent.getChildren().size() : Math.min(rank, newParent.getChildren().size()));
            return;
        }
        node.setParent(null);
        node.setGraph(this);
        this.nodes.add(rank < 0 ? this.nodes.size() : Math.min(rank, this.nodes.size()), node);
    }

    @Override
    public int getRank(INode node)
    {
        INode parent = node.getParent();
        return parent == null ? this.nodes.indexOf(node) : parent.getChildren().indexOf(node);
    }

    @Override
    public void removeNode(INode... nodesToRemove)
    {
        // Step 1a : Remove nodes directly attach to the graph
        for (INode aNodeToRemove : nodesToRemove)
        {
            if (this.nodes.contains(aNodeToRemove))
            {
                this.nodes.remove(aNodeToRemove);
            }
        }
        // Step 1b : Remove nodes attach to other nodes as children
        for (INode aNode : getAllNodes())
        {
            for (INode aNodeToRemove : nodesToRemove)
            {
                List<INode> children = aNode.getChildren();
                if (children.contains(aNodeToRemove))
                {
                    aNode.removeChild(aNodeToRemove);
                }
            }
        }
        // Step 2 : Disconnect edges
        List<IEdge> edgesToRemove = new ArrayList<IEdge>();
        Collection<INode> allNodes = getAllNodes();
        for (IEdge anEdge : this.edges)
        {
            INode startingNode = anEdge.getStartNode();
            INode endingNode = anEdge.getEndNode();
            boolean isEdgeStillConnected = (allNodes.contains(startingNode) && allNodes.contains(endingNode));
            if (!isEdgeStillConnected)
            {
                edgesToRemove.add(anEdge);
            }
        }
        IEdge[] edgesToRemoveAsArray = edgesToRemove.toArray(new IEdge[edgesToRemove.size()]);
        removeEdge(edgesToRemoveAsArray);
    }

    @Override
    public boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation)
    {
        // Step 0 : markers are never linked, containers and free texts only accept generic edges
        if (start instanceof MarkerNode || end instanceof MarkerNode)
        {
            return false;
        }
        boolean isGenericEdge = (e instanceof GenericEdge);
        if (!isGenericEdge && (start instanceof ContainerNode || end instanceof ContainerNode
                || start instanceof FreeTextNode || end instanceof FreeTextNode))
        {
            return false;
        }
        // Step 1 : find if nodes exist
        Collection<INode> allNodes = getAllNodes();
        if (start != null && !allNodes.contains(start))
        {
            addNode(start, start.getLocation());
        }
        if (end != null && !allNodes.contains(end))
        {
            addNode(end, end.getLocation());
        }
        e.setStartNode(start);
        e.setStartLocation(startLocation);
        e.setEndNode(end);
        e.setEndLocation(endLocation);
        // Step 2 : a generic edge links any nodes, the other ones follow the rules of their starting node
        boolean isAccepted = isGenericEdge ? (start != null && end != null) : start.addConnection(e);
        if (isAccepted)
        {
            e.setId(new Id());
            edges.add(e);
            return true;
        }
        return false;
    }

    /**
     * The rules of each type of diagram: which kinds of edges can go from which kind of node to which other one. The generic edge
     * and the elements common to all diagrams are left out.
     *
     * @return empty by default: only the generic edge is then possible
     */
    protected List<ConnectionRule> getConnectionRules()
    {
        return Collections.emptyList();
    }

    private boolean isAllowedByRules(IEdge prototype, INode start, INode end)
    {
        for (ConnectionRule aRule : getConnectionRules())
        {
            if (aRule.allows(prototype, start, end))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true for an element common to all diagrams, which only a generic edge links
     */
    private boolean isCommonNode(INode node)
    {
        for (Class<?> aClass : COMMON_NODE_CLASSES)
        {
            if (aClass.isInstance(node))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeEdge(IEdge... edgesToRemove)
    {
        for (IEdge anEdgeToRemove : edgesToRemove)
        {
            INode startingNode = anEdgeToRemove.getStartNode();
            INode endingNode = anEdgeToRemove.getEndNode();
            startingNode.removeConnection(anEdgeToRemove);
            endingNode.removeConnection(anEdgeToRemove);
            this.edges.remove(anEdgeToRemove);
        }
    }

    @Override
    @Transient
    public IGridSticker getGridSticker()
    {
        if (this.gridSticker == null)
        {
            return new IGridSticker()
            {
                @Override
                public Rectangle2D snap(Rectangle2D r)
                {
                    return r;
                }

                @Override
                public Point2D snap(Point2D p)
                {
                    return p;
                }
            };
        }
        return this.gridSticker;
    }

    @Override
    public void setGridSticker(IGridSticker positionCorrector)
    {
        this.gridSticker = positionCorrector;
    }

    /** The elements common to all diagrams, in the order of the tool bar: only a generic edge links them */
    /** What {@link #getAllNodes()} remembers in a {@link GeometryCache} pass */
    private static final Object ALL_NODES_CACHE_KEY = new Object();

    /**
     * Room kept around the painted part when choosing what to paint, in graph units: what an element paints beyond its bounds
     * (thick borders, arrow heads, halos) stays well inside it
     */
    private static final double CULLING_MARGIN = 40;

    /** Room around an edge where its exact hit test is still made (it finds the edge up to a few pixels from its line) */
    private static final double HIT_MARGIN = 20;

    public static final List<Class<? extends INode>> COMMON_NODE_CLASSES = Collections.unmodifiableList(
            Arrays.<Class<? extends INode>> asList(NoteNode.class, FreeTextNode.class, ImageNode.class, ContainerNode.class,
                    MarkerNode.class));

    private ArrayList<INode> nodes;
    private ArrayList<IEdge> edges;
    private transient IGridSticker gridSticker;
    @SuppressWarnings("unused")
    private transient Rectangle2D minBounds;
}
