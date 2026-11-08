package com.horstmann.violet.workspace.editorpart;

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
     * Grows drawing area
     */
    public void growDrawingArea();
    
    /**
     * Clips drawing area
     */
    public void clipDrawingArea();

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