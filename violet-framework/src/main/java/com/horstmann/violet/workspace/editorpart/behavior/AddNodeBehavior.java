package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBarMouseListener;

public class AddNodeBehavior extends AbstractEditorPartBehavior implements IGraphToolsBarMouseListener
{
	private static final int OUTSIDE_SCREEN_POSITION = -1000;

    public AddNodeBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
    	this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
        this.graphToolsBar = graphToolsBar;
        
        this.isDraggingFromTools = false;
        this.isDraggingFromGraph = false;
        this.draggedNodeFromTools = null;
        graphToolsBar.addMouseListener(this);
    }
    
    @Override
    public void onMouseDragged(MouseEvent event) {
    	this.isDraggingFromGraph = true;
    }
    
    
    @Override
    public void onMouseReleased(MouseEvent event)
    {
    	if (this.isDraggingFromGraph)
    	{
    		this.isDraggingFromGraph = false;
    		return;
    	}
    	
    	this.draggedNodeFromTools = null;
    	
        if (event.getClickCount() > 1)
        {
            return;
        }
        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool()))
        {
            return;
        }
        GraphTool selectedTool = this.selectionHandler.getSelectedTool();
        if (!INode.class.isInstance(selectedTool.getNodeOrEdge()))
        {
            return;
        }
        if (AddContainerByFrameBehavior.isContainerTool(selectedTool) && BehaviorUtils.isCursorOnSelectedNode(editorPart, event))
        {
            return; // a click on the selected container (e.g. just created) only keeps it selected
        }

        double zoom = editorPart.getZoomFactor();
        final Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        INode prototype = (INode) selectedTool.getNodeOrEdge();
        INode nodeUnderCursor = graph.findNode(mousePoint);
        if (nodeUnderCursor != null && nodeUnderCursor.getClass().equals(prototype.getClass()))
        {
            // A click on a node of the tool's own kind selects it (done on press by DragSelectedBehavior) instead of
            // stacking a new one upon it: notes, free texts and images are always added, whatever lies under them
            return;
        }
        IGridSticker gridSticker = graph.getGridSticker();
        Point2D newNodeLocation = gridSticker.snap(mousePoint);
        INode newNode = (INode) prototype.clone();
        newNode.setId(new Id());
        
        boolean added = addNodeAtPoint(newNode, newNodeLocation);
        if (added)
        {
            selectionHandler.setSelectedElement(newNode);
            if (AddContainerByFrameBehavior.isContainerTool(selectedTool))
            {
                graphToolsBar.reset(); // back to the selection tool once a container is created
            }

//            if (!KeyModifierUtil.isCtrl(event)) {
//	            selectionHandler.setSelectedTool(GraphTool.SELECTION_TOOL);
//	            graphToolsBar.setSelectedTool(GraphTool.SELECTION_TOOL);
//	            graphToolsBar.getAWTComponent().invalidate();
//            }
            
            editorPart.getSwingComponent().invalidate();
        }
    }

    /**
     * Adds a new at a precise location
     * 
     * @param newNode to be added
     * @param location
     * @return true if the node_old has been added
     */
    public boolean addNodeAtPoint(INode newNode, Point2D location)
    {
        boolean isAdded = false;
        this.behaviorManager.fireBeforeAddingNodeAtPoint(newNode, location);
        try
        {
            if (graph.addNode(newNode, location))
            {
                newNode.incrementRevision();
                isAdded = true;
            }
        }
        finally
        {
            this.behaviorManager.fireAfterAddingNodeAtPoint(newNode, location);
        }
        return isAdded;
    }

	@Override
	public void onMouseToolClicked(GraphTool selectedTool)
	{
		Object obj = selectedTool.getNodeOrEdge();
		if (obj instanceof INode)
		{
			this.isDraggingFromTools = true;
	        INode prototype = (INode) selectedTool.getNodeOrEdge();
	        this.draggedNodeFromTools = (INode) prototype.clone();
	        this.draggedNodeFromTools.setId(new Id());
	        
	        
			double zoom = editorPart.getZoomFactor();
	        final Point2D initialLocation = new Point2D.Double(OUTSIDE_SCREEN_POSITION / zoom, OUTSIDE_SCREEN_POSITION / zoom);
	        IGridSticker gridSticker = graph.getGridSticker();
	        Point2D newNodeLocation = gridSticker.snap(initialLocation);
			
	        boolean added = addNodeAtPoint(this.draggedNodeFromTools, newNodeLocation);
	        if (added)
	        {
	            selectionHandler.setSelectedElement(this.draggedNodeFromTools);
	            editorPart.getSwingComponent().invalidate();
	        }
		}
	}

	@Override
	public void onMouseToolDragged(MouseEvent event)
	{
		if (this.isDraggingFromTools) {
			MouseEvent outEvent = SwingUtilities.convertMouseEvent((Component)event.getSource(), event, editorPart.getSwingComponent());
			
			moveDraggedNode(outEvent);
		}
	}

	@Override
	public void onMouseToolReleased(MouseEvent event)
	{
		this.isDraggingFromTools = false;
		if (this.draggedNodeFromTools != null) {
			MouseEvent outEvent = SwingUtilities.convertMouseEvent((Component)event.getSource(), event, editorPart.getSwingComponent());
            
			if (outEvent.getX() < 0 || outEvent.getY() < 0) {
				this.graph.removeNode(this.draggedNodeFromTools);
				selectionHandler.removeElementFromSelection(this.draggedNodeFromTools);
				editorPart.getSwingComponent().invalidate();
				editorPart.getSwingComponent().repaint();
			} else {
				moveDraggedNode(outEvent);
				graphToolsBar.reset();
			}
			
	        this.draggedNodeFromTools = null;
		}
	}
	
	private void moveDraggedNode(MouseEvent convertedEvent) {
		double zoom = editorPart.getZoomFactor();
        final Point2D mousePoint = new Point2D.Double(convertedEvent.getX() / zoom, convertedEvent.getY() / zoom);
        IGridSticker gridSticker = graph.getGridSticker();
        Point2D newNodeLocation = gridSticker.snap(mousePoint);
		
        draggedNodeFromTools.setLocation(newNodeLocation);
        
        selectionHandler.setSelectedElement(this.draggedNodeFromTools);
        editorPart.getSwingComponent().invalidate();
        editorPart.getSwingComponent().repaint();
	}
	
    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPartBehaviorManager behaviorManager;

    private IGraphToolsBar graphToolsBar;

    private boolean isDraggingFromTools;
    
    private boolean isDraggingFromGraph;
    
    private INode draggedNodeFromTools;
}
