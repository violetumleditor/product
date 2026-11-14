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