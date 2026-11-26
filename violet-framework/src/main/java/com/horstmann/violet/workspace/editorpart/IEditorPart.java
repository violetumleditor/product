package com.horstmann.violet.workspace.editorpart;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import com.horstmann.violet.product.diagram.abstracts.IGraph;

/**
 * Defines the editor behaviour (an editor is something embedding an IGraph)
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public interface IEditorPart
{

    /**
     * Tells the editor what the current mouse gesture changes, so that it may draw the rest of the diagram once instead of at
     * each step (see {@link IGraph#getChangingElements(java.util.Collection, java.util.Collection)}). Both may be empty for a
     * gesture that changes nothing in the graph (a link or a lasso followed by the mouse). Called at each step, only the first
     * call of a gesture counts; the editor forgets it when the mouse is released.
     *
     * @param nodes the nodes moved or resized
     * @param edges the edges whose path or texts are moved
     */
    public default void startMoving(java.util.Collection<? extends com.horstmann.violet.product.diagram.abstracts.node.INode> nodes,
            java.util.Collection<? extends com.horstmann.violet.product.diagram.abstracts.edge.IEdge> edges)
    {
        // Nothing by default: everything is drawn at each step
    }

    /**
     * Returns the graph handled by the editor
     */
    public abstract IGraph getGraph();

    /**
     * Removes the selected node or edges.
     */
    public abstract void removeSelected();

    /**
     * Changes the zoom of this editor. The zoom is 1 by default and is multiplied by the fourth root of 2 for each positive step or
     * divided by it for each negative step. The Swing component then fires a {@link #ZOOM_PROPERTY} property change. The middle of
     * the view stays where it is on screen.
     *
     * @param steps the number of steps by which to change the zoom. A positive value zooms in, a negative value zooms out.
     */
    public abstract void changeZoom(int steps);

    /**
     * Changes the zoom as {@link #changeZoom(int)} does, around a given point.
     *
     * @param steps the number of steps by which to change the zoom
     * @param center the point of the Swing component (a mouse event's...) which stays where it is on screen
     */
    public abstract void changeZoom(int steps, Point2D center);

    /**
     * Sets the zoom back to 100 %, the middle of the view staying where it is on screen, as {@link #changeZoom(int)} does.
     */
    public abstract void resetZoom();

    /**
     * Name of the property change fired by {@link #getSwingComponent()} when the zoom factor changes
     */
    public static final String ZOOM_PROPERTY = "zoom";

    /**
     * @return current zoom factor
     */
    public double getZoomFactor();
    
    /**
     * @return the grid used to keep elements aligned
     */
    public IGrid getGrid();
    
    /**
     * @param point on the Swing component (a mouse event's...)
     * @return the same point on the graph. The component's origin is not the graph's: the drawing area extends in all
     *         directions, so also to negative graph coordinates.
     */
    public Point2D toGraphPoint(Point2D point);

    /**
     * @param point on the graph
     * @return the same point on the Swing component
     */
    public Point2D toComponentPoint(Point2D point);

    /**
     * @return the part of the graph's plane covered by the Swing component, on the graph
     */
    public Rectangle2D getDrawingArea();

    /**
     * Name of the property change fired by {@link #getSwingComponent()} when the graph point at its origin changes
     */
    public static final String DRAWING_AREA_PROPERTY = "drawingArea";

    /**
     * @param isFrozen true to keep the drawing area as it is for now (a node waits off screen while dragged from the side bar)
     */
    public void setDrawingAreaFrozen(boolean isFrozen);

    /**
     * @return true while the drawing area is kept as it is
     */
    public boolean isDrawingAreaFrozen();

    /**
     * Scrolls just enough to show a part of the graph, with some room around. Doesn't scroll on an axis where it is
     * larger than the view.
     *
     * @param boundsOnGraph what to show
     */
    public void ensureVisible(Rectangle2D boundsOnGraph);

    /**
     * @return the awt object displaying this editor part
     */
    public JComponent getSwingComponent();
    
    /**
     * @return object that manages node and edges selection
     */
    public IEditorPartSelectionHandler getSelectionHandler();
    
    /**
     * @return manager used to declare new editor behaviors and how to send events between behaviors
     */
    public IEditorPartBehaviorManager getBehaviorManager();
    
   

}