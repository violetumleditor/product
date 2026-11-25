package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

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
        if (DrawMarkerBehavior.isMarkerTool(selectedTool))
        {
            return; // only drawn freehand
        }
        if (AddContainerByFrameBehavior.isContainerTool(selectedTool) && BehaviorUtils.isCursorOnSelectedNode(editorPart, event))
        {
            return; // a click on the selected container (e.g. just created) only keeps it selected
        }

        final Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        INode prototype = (INode) selectedTool.getNodeOrEdge();
        INode nodeUnderCursor = graph.findNodeUnderMarkers(mousePoint);
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
		if (DrawMarkerBehavior.isMarkerTool(selectedTool))
		{
			return; // only drawn freehand
		}
		Object obj = selectedTool.getNodeOrEdge();
		if (obj instanceof INode)
		{
			this.isDraggingFromTools = true;
	        INode prototype = (INode) selectedTool.getNodeOrEdge();
	        this.draggedNodeFromTools = (INode) prototype.clone();
	        this.draggedNodeFromTools.setId(new Id());
	        
	        
			double zoom = editorPart.getZoomFactor();
	        Rectangle2D drawingArea = editorPart.getDrawingArea();
	        final Point2D initialLocation = new Point2D.Double(drawingArea.getX() + OUTSIDE_SCREEN_POSITION / zoom,
	                drawingArea.getY() + OUTSIDE_SCREEN_POSITION / zoom);
	        editorPart.setDrawingAreaFrozen(true); // it must not extend to the node waiting off screen
	        IGridSticker gridSticker = graph.getGridSticker();
	        Point2D newNodeLocation = gridSticker.snap(initialLocation);
			
	        boolean added = addNodeAtPoint(this.draggedNodeFromTools, newNodeLocation);
	        if (added)
	        {
	            selectionHandler.setSelectedElement(this.draggedNodeFromTools);
	            editorPart.getSwingComponent().invalidate();
	        }
	        else
	        {
	            // Refused on the graph itself (an activation bar only goes into a lifeline or another bar): kept aside, then added
	            // on release into the node under the mouse
	            this.pendingNodeFromTools = this.draggedNodeFromTools;
	            this.draggedNodeFromTools = null;
	            this.pendingDropPoint = null;
	        }
		}
	}

	@Override
	public void onMouseToolDragged(MouseEvent event)
	{
		if (this.isDraggingFromTools && this.pendingNodeFromTools != null) {
			this.pendingDropPoint = getDropPoint(event);
			editorPart.getSwingComponent().repaint();
			return;
		}
		if (this.isDraggingFromTools && this.draggedNodeFromTools != null) {
			MouseEvent outEvent = SwingUtilities.convertMouseEvent((Component)event.getSource(), event, editorPart.getSwingComponent());
			
			moveDraggedNode(outEvent);
		}
	}

	@Override
	public void onMouseToolReleased(MouseEvent event)
	{
		this.isDraggingFromTools = false;
		INode droppedNode = null;
		if (this.pendingNodeFromTools != null) {
			dropPendingNode(event);
		}
		if (this.draggedNodeFromTools != null) {
			MouseEvent outEvent = SwingUtilities.convertMouseEvent((Component)event.getSource(), event, editorPart.getSwingComponent());
            
			if (outEvent.getX() < 0 || outEvent.getY() < 0 || !isOverVisibleDiagram(event)) {
				this.graph.removeNode(this.draggedNodeFromTools);
				selectionHandler.removeElementFromSelection(this.draggedNodeFromTools);
				editorPart.getSwingComponent().invalidate();
				editorPart.getSwingComponent().repaint();
			} else {
				moveDraggedNode(outEvent);
				if (this.newParentOfDraggedNode != null) {
					// Into the node it is dropped on (a class into a package); undoing the addition removes it from there
					BehaviorUtils.moveIntoNewParent(graph, this.draggedNodeFromTools, this.newParentOfDraggedNode);
				}
				graphToolsBar.reset();
				droppedNode = this.draggedNodeFromTools;
			}
			this.newParentOfDraggedNode = null;
			
	        this.draggedNodeFromTools = null;
		}
		editorPart.setDrawingAreaFrozen(false);
		if (droppedNode != null) {
			editorPart.ensureVisible(BehaviorUtils.getBoundsOnGraph(droppedNode));
		}
	}
	
	/**
	 * @param event released on a tool button
	 * @return true if the mouse is over the diagram itself, not over a component in front of it (the side bar opened over the
	 *         diagram on a narrow window) nor beside it
	 */
	private boolean isOverVisibleDiagram(MouseEvent event) {
		Component diagram = editorPart.getSwingComponent();
		Component window = SwingUtilities.getWindowAncestor(diagram);
		if (window == null) {
			return true;
		}
		Point windowPoint = SwingUtilities.convertPoint((Component) event.getSource(), event.getPoint(), window);
		Component componentUnderMouse = SwingUtilities.getDeepestComponentAt(window, windowPoint.x, windowPoint.y);
		return componentUnderMouse != null && SwingUtilities.isDescendingFrom(componentUnderMouse, diagram);
	}

	/**
	 * Adds the node kept aside where it is released, inside the node under the mouse, if the graph accepts it there
	 */
	private void dropPendingNode(MouseEvent event) {
		INode newNode = this.pendingNodeFromTools;
		Point2D dropPoint = getDropPoint(event);
		this.pendingNodeFromTools = null;
		this.pendingDropPoint = null;
		if (dropPoint != null && graph.canAddNode(newNode, dropPoint) && addNodeAtPoint(newNode, dropPoint)) {
			selectionHandler.setSelectedElement(newNode);
			graphToolsBar.reset();
		}
		editorPart.getSwingComponent().invalidate();
		editorPart.getSwingComponent().repaint();
	}

	/**
	 * @param event on a tool button
	 * @return the snapped point of the diagram under the mouse, null if the mouse is not over the visible diagram
	 */
	private Point2D getDropPoint(MouseEvent event) {
		MouseEvent outEvent = SwingUtilities.convertMouseEvent((Component) event.getSource(), event, editorPart.getSwingComponent());
		if (outEvent.getX() < 0 || outEvent.getY() < 0 || !isOverVisibleDiagram(event)) {
			return null;
		}
		return graph.getGridSticker().snap(editorPart.toGraphPoint(outEvent.getPoint()));
	}

	/**
	 * While a node is dragged from the tools, the node that will take it; for a node kept aside, also its outline at the mouse
	 */
	@Override
	public void onPaint(Graphics2D g2) {
		if (this.draggedNodeFromTools != null && this.newParentOfDraggedNode != null) {
			BehaviorUtils.drawDropTarget(g2, this.newParentOfDraggedNode);
		}
		if (this.pendingNodeFromTools == null || this.pendingDropPoint == null) {
			return;
		}
		if (!graph.canAddNode(this.pendingNodeFromTools, this.pendingDropPoint)) {
			return;
		}
		INode target = graph.findNodeUnderMarkers(this.pendingDropPoint);
		if (target != null) {
			BehaviorUtils.drawDropTarget(g2, target);
		}
		Rectangle2D bounds = this.pendingNodeFromTools.getBounds();
		BehaviorUtils.drawDropOutline(g2, new Rectangle2D.Double(this.pendingDropPoint.getX(), this.pendingDropPoint.getY(),
				bounds.getWidth(), bounds.getHeight()));
	}

	private void moveDraggedNode(MouseEvent convertedEvent) {
        final Point2D mousePoint = editorPart.toGraphPoint(convertedEvent.getPoint());
        IGridSticker gridSticker = graph.getGridSticker();
        Point2D newNodeLocation = gridSticker.snap(mousePoint);
		
        draggedNodeFromTools.setLocation(newNodeLocation);
        INode newParent = graph.findNewParentOnDrop(draggedNodeFromTools, newNodeLocation);
        this.newParentOfDraggedNode = (newParent == draggedNodeFromTools.getParent()) ? null : newParent;
        
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

    /** Node the node dragged from the tools moves into if released now (a package), null if it stays on the graph */
    private INode newParentOfDraggedNode;

    /** Node dragged from the tools that the graph refused off screen, added on release (null otherwise) */
    private INode pendingNodeFromTools;

    /** Where the node kept aside would be dropped now, null when not over the diagram */
    private Point2D pendingDropPoint;
}
