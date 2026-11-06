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
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.NoteNode;

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
        // Free texts are on top : they are found first
        // Containers are in background : they are only found if no other node is at this point
        INode foundNode = null;
        ContainerNode smallestContainer = null;
        for (INode n : getAllNodes())
        {
            Point2D locationOnGraph = n.getLocationOnGraph();
            Rectangle2D bounds = n.getBounds();
            Rectangle2D boundsToCheck = new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(),
                    bounds.getHeight());
            if (!boundsToCheck.contains(p))
            {
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
        for (IEdge e : edges)
        {
            if (e.contains(p)) return e;
        }
        return null;
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
            aContainer.draw(g2);
        }

        int count = 0;
        int z = 0;
        while (count < nodes.size())
        {
            for (INode n : nodes)
            {

                if (n.getZ() == z)
                {
                    if (n instanceof ContainerNode)
                    {
                        // Already drawn
                    }
                    else if (n instanceof NoteNode || n instanceof FreeTextNode)
                    {
                        specialNodes.add(n);
                    }
                    else
                    {
                        Stroke savedStroke = g2.getStroke();
                        if (n instanceof AbstractNode && ((AbstractNode) n).getBorderWidth() > 1)
                        {
                            g2.setStroke(new BasicStroke(((AbstractNode) n).getBorderWidth()));
                        }
                        n.draw(g2);
                        g2.setStroke(savedStroke);
                    }
                    count++;
                }
            }
            z++;
        }

        for (int i = 0; i < edges.size(); i++)
        {
            IEdge e = (IEdge) edges.get(i);
            e.draw(g2);
        }
        // Special nodes are always drawn upon other elements
        for (INode n : specialNodes)
        {
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
        Rectangle2D r = null;
        for (INode n : nodes)
        {
            Rectangle2D b = n.getBounds();
            if (r == null) r = b;
            else r.add(b);
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
    public Collection<INode> getAllNodes()
    {
        List<INode> fifo = new ArrayList<INode>();
        List<INode> allNodes = new ArrayList<INode>();
        fifo.addAll(nodes);
        allNodes.addAll(nodes);
        while (!fifo.isEmpty())
        {
            INode nodeToInspect = fifo.remove(0);
            List<INode> children = nodeToInspect.getChildren();
            fifo.addAll(children);
            allNodes.addAll(children);
        }
        // Let's have children first
        Collections.reverse(allNodes);
        return Collections.unmodifiableCollection(allNodes);
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

        // Case 1 : Note, container, free text and image nodes always attached to the graph
        if (newNode instanceof NoteNode || newNode instanceof ContainerNode || newNode instanceof FreeTextNode
                || newNode instanceof ImageNode)
        {
            newNode.setLocation(adjustedPoint);
            nodes.add(newNode);
            return true;
        }
        // Case 2 : attached to an existing node (containers and free texts never have children)
        INode potentialParentNode = findNode(p);
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
        // Step 0 : containers and free texts never accept edges
        if (start instanceof ContainerNode || end instanceof ContainerNode || start instanceof FreeTextNode
                || end instanceof FreeTextNode)
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
        if (start.addConnection(e))
        {
            e.setId(new Id());
            edges.add(e);
            return true;
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

    private ArrayList<INode> nodes;
    private ArrayList<IEdge> edges;
    private transient IGridSticker gridSticker;
    @SuppressWarnings("unused")
    private transient Rectangle2D minBounds;
}
