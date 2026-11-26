package com.horstmann.violet.product.diagram.abstracts;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

public interface IGraph
{

    /**
     * Gets the node types of a particular graph type.
     * 
     * @return a list of node prototypes
     */
    public abstract List<INode> getNodePrototypes();


    /**
     * Gets the edge types of a particular graph type.
     * 
     * @return a list of edge prototypes
     */
    public abstract List<IEdge> getEdgePrototypes();

    /**
     * Gives the categories the side bar groups the prototypes by, for a type of diagram which has many of them. The prototypes
     * left out of every category go to two default ones: elements and links.
     *
     * @return the categories in display order, empty (the default) to only get the two default ones
     */
    public default List<ToolCategory> getToolCategories()
    {
        return List.of();
    }

    /**
     * Gives the kinds of edges that can link two nodes of this graph, as defined by each type of diagram (see
     * {@code AbstractGraph.getConnectionRules()}). The generic edge is only given when no edge specific to the diagram fits, or
     * for an element common to all diagrams (note, free text, image, grouping area). Used by the link handle, which offers these
     * kinds of edges.
     *
     * @param start the start node
     * @param end the end node, maybe the start node itself
     * @return edge prototypes, among {@link #getEdgePrototypes()} and in the same order
     */
    public abstract List<IEdge> getEdgePrototypes(INode start, INode end);


    /**
     * Gets ALL the nodes of this graph.
     * 
     * @return an unmodifiable collection of the nodes
     */
    public abstract Collection<INode> getAllNodes();


    /**
     * Gets ALL the edges of this graph.
     * 
     * @return an unmodifiable collection of the edges
     */
    public abstract Collection<IEdge> getAllEdges();


    /**
     * Removes one or more edges from this graph.
     * 
     * @param edgesToRemove
     */
    public abstract void removeEdge(IEdge... edgesToRemove);
    
    /**
     * Removes one or more nodes from this graph.
     * 
     * @param nodesToRemove
     */
    public abstract void removeNode(INode... nodesToRemove);    
    
    /**
     * Adds a node to the graph so that the top left corner of the bounding rectangle is at the given point.
     * This method is called by a decoder when reading a data file.
     * 
     * @param n the node to add
     * @param p the desired location
     */
    public abstract boolean addNode(INode n, Point2D p);

    /**
     * Tells, without changing anything, whether {@link #addNode(INode, Point2D)} would accept this node at this point: as a child
     * of the node found there if it takes children, or on the graph itself. Used to offer only the kinds of nodes that can be added
     * on a spot ("+" button).
     *
     * @param n the node to add, not on the graph
     * @param p the desired location
     * @return true if the node would be added
     */
    public abstract boolean canAddNode(INode n, Point2D p);

    /**
     * @return the room kept above the elements on the drawing area, in pixels
     */
    public default int getTopMargin()
    {
        return DEFAULT_MARGIN;
    }

    /** Room kept around the elements on the drawing area, in pixels */
    public static final int DEFAULT_MARGIN = 40;

    /**
     * Tells which node a node of this graph, dragged alone and dropped at this point, moves into (an activation bar into a lifeline,
     * a class into a package). Side-effect free. By default, a dragged node never changes its parent.
     *
     * @param draggedNode the node being dragged, already on this graph
     * @param p the drop point (graph coordinates)
     * @return the node that takes it as a child (it accepts it, see {@link INode#canAddChild(INode)}, and is neither the node nor
     *         one of its descendants); null (or its current parent) to leave it where it is
     */
    public default INode findNewParentOnDrop(INode draggedNode, Point2D p)
    {
        return null;
    }

    /**
     * Moves a node of this graph into another node, keeping its location relative to its parent. Undoing such a move (see
     * {@link #findNewParentOnDrop}) may put a node back on the graph itself.
     *
     * @param node the node to move
     * @param newParent the node that takes it as a child; null for the graph itself (a top-level node, when undoing)
     * @param rank its rank among its new siblings (bottom to top), clamped; -1 to put it last (on top)
     */
    public default void changeParent(INode node, INode newParent, int rank)
    {
        if (newParent == null)
        {
            throw new UnsupportedOperationException("This graph has no top-level nodes list");
        }
        newParent.addChild(node, rank < 0 ? newParent.getChildren().size() : Math.min(rank, newParent.getChildren().size()));
    }

    /**
     * @param node a node of this graph
     * @return its rank among its siblings: the children of its parent, or the top-level nodes of the graph
     */
    public default int getRank(INode node)
    {
        INode parent = node.getParent();
        return parent == null ? -1 : parent.getChildren().indexOf(node);
    }


    /**
     * Adds an edge to this graph. 
     * 
     * @param e the new edge to add (don't forget to populate it with the following nodes and points!)
     * @param start the start node of the edge
     * @param startLocation the point inside the start node where the edge begins
     * @param end the end node of the edge
     * @param endLocation the point inside the end node where the edge ends
     * @param list a points for edge that supports free path 
     * @return isOK as true if successfully connected
     */
    public abstract boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation);


    /**
     * Finds a node by its id. This internal method should only be used by network features (for the moment because
     * node ids are still generated automatically)
     * 
     * @param id 
     * @return the found node or null if no one found
     */
    public abstract INode findNode(Id id);


    /**
     * Finds a node containing the given point.
     * 
     * @param p a point
     * @return a node containing p or null if no nodes contain p
     */
    public abstract INode findNode(Point2D p);

    /**
     * Same as {@link #findNode(Point2D)}, leaving out the marker strokes drawn upon the other elements: for what takes a child, a
     * link or a text.
     *
     * @param p a point
     * @return a node containing p or null if no nodes contain p
     */
    public default INode findNodeUnderMarkers(Point2D p)
    {
        return findNode(p);
    }

    /**
     * Finds an adge by its id. This internal method should only be used by network features (for the moment because
     * edge ids are still generated automatically)
     * 
     * @param id
     * @return the found edge or null if no one found
     */
    public abstract IEdge findEdge(Id id);


    /**
     * Finds an edge containing the given point.
     * 
     * @param p a point
     * @return an edge containing p or null if no edges contain p
     */
    public abstract IEdge findEdge(Point2D p);

    /**
     * Draws the graph
     * 
     * @param g2 the graphics context
     */
    public abstract void draw(Graphics2D g2);

    /**
     * Draws only some elements, in the order {@link #draw(Graphics2D)} draws them: while something is dragged, the editor draws
     * the elements that don't change once in an image, then only those that change at each step. Only called when
     * {@link #getChangingElements(java.util.Collection, java.util.Collection)} returned a filter.
     *
     * @param g2 the graphics context
     * @param isDrawn tells which nodes and edges to draw
     */
    public default void draw(Graphics2D g2, java.util.function.Predicate<Object> isDrawn)
    {
        draw(g2);
    }

    /**
     * Tells which elements may change while the given ones are changed by the mouse (moved, resized, their segments, transition
     * points or texts moved), all the others staying exactly as they are, so that they may be drawn once in an image.
     *
     * @param nodes the nodes the user moves or resizes
     * @param edges the edges whose path or texts the user moves
     * @return a filter accepting the elements that may change, or null when anything may change (everything is then drawn at
     *         each step)
     */
    public default java.util.function.Predicate<Object> getChangingElements(java.util.Collection<INode> nodes,
            java.util.Collection<IEdge> edges)
    {
        return null;
    }


    /**
     * Gets the smallest rectangle enclosing the graph
     * 
     * @return the bounding rectangle
     */
    public abstract Rectangle2D getClipBounds();
    
    
    /**
     * Sets desired bound
     * 
     * @param newValue
     */
    public abstract void setBounds(Rectangle2D newValue);
    
    
    /**
     * Returns the corrector used to stick points on the grid
     */
    public IGridSticker getGridSticker();

    /**
     * Sets the corrector used to stick points on the grid
     * 
     * @param newGrid
     */
    public void setGridSticker(IGridSticker newCorrector);
    
    
}