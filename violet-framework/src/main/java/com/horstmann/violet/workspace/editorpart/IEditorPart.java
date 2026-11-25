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
     * Returns the graph handled by the editor
     */
    public abstract IGraph getGraph();

    /**
     * Removes the selected node or edges.
     */
    public abstract void removeSelected();

    /**
     * Changes the zoom of this editor. The zoom is 1 by default and is multiplied by the fourth root of 2 for each positive step or
     * divided by it for each negative step. The Swing component then fires a {@link #ZOOM_PROPERTY} property change.
     *
     * @param steps the number of steps by which to change the zoom. A positive value zooms in, a negative value zooms out.
     */
    public abstract void changeZoom(int steps);

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
     * Scrolls just enough to show a part of the graph, with some room around. Does nothing if it is larger than the view.
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