package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.framework.util.GrabberUtils;
import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.ICroppableNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.IResizableNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

public class SelectByClickBehavior extends AbstractEditorPartBehavior
{

    public SelectByClickBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        resetEventAttributes();
        
        if (event.getClickCount() > 1) {
            return;
        }
        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (!GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool())) {
            return;
        }
        if (BehaviorUtils.isCursorOnResizePoint(editorPart, event) || BehaviorUtils.isCursorOnCropPoint(editorPart, event)) {
            return; // half of a handle lies outside its node: keeps the selection, which ResizeNodeBehavior / CropNodeBehavior need
        }
        Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        boolean isCtrl = KeyModifierUtil.isCtrl(event);
        boolean isOnNodeOrEdge = isMouseOnNodeOrEdge(mousePoint);
        if (!isOnNodeOrEdge && !isCtrl)
        {
            resetSelectedElements();
            return;
        }
        if (isOnNodeOrEdge && !isCtrl)
        {
            processSelection(mousePoint, true);
            return;
        }
        if (isOnNodeOrEdge && isCtrl)
        {
            processSelection(mousePoint, false);
            return;
        }
    }

    
    @Override
    public void onMouseMoved(MouseEvent event)
    {
        updateHoveredTransitionPoint(event);
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        this.isDragGesture = true;
    }
    
    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (event.getClickCount() > 1) {
            return;
        }
        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (this.isDragGesture) {
            return;
        }
        if (KeyModifierUtil.isCtrl(event)) {
            processSelectionInConflictWithDraggingEvents(false);
        } else {
            processSelectionInConflictWithDraggingEvents(true);
        }
        this.editorPart.getSwingComponent().invalidate();
    }

    private void resetEventAttributes()
    {
        this.isDragGesture = false;
        this.unprocessedSelectedElements.clear();
    }

    
    private boolean isMouseOnNodeOrEdge(Point2D mouseLocation)
    {
        INode node = this.graph.findNode(mouseLocation);
        IEdge edge = this.graph.findEdge(mouseLocation);
        if (node == null && edge == null)
        {
            return false;
        }
        return true;
    }

    private void resetSelectedElements()
    {
        this.selectionHandler.clearSelection();
    }
    
    
    /**
     * Here, we add or remove the selected node_old or edge to the global selection. Under the wood, we can't remove anything.
     * This can only made on 'mouse released' to avoid conflict to any dragging event.
     * 
     * @param mouseLocation
     * @param isResetSelectionFirst
     */
    private void processSelection(Point2D mouseLocation, boolean isResetSelectionFirst)
    {
        INode node = this.graph.findNode(mouseLocation);
        IEdge edge = this.graph.findEdge(mouseLocation);
        ITransitionPoint transitionPoint = null;
        if (edge != null) {
        	transitionPoint = edge.findTransitionPoint(mouseLocation);
        }

        if (edge != null) {
        	node = null;
        }
        
        if (isResetSelectionFirst) {
        	if (edge != null && !this.selectionHandler.isElementAlreadySelected(edge)) {
        		resetSelectedElements();
        	}
        	if (transitionPoint != null && !this.selectionHandler.isElementAlreadySelected(transitionPoint)) {
        		resetSelectedElements();
        	}
        	if (node != null && !this.selectionHandler.isElementAlreadySelected(node)) {
        		resetSelectedElements();
        	}
        }
        
        if (edge != null)
        {
        	if (this.selectionHandler.isElementAlreadySelected(edge))
        	{
        		// This edge will be removed only on mouse button released
        		// to avoid conflicts with dragging events
        		this.unprocessedSelectedElements.add(edge);
        	}
        	else
        	{
        		this.selectionHandler.addSelectedElement(edge);
                List<IEdge> selectedEdges = selectionHandler.getSelectedElements().stream().filter(e -> IEdge.class.isInstance(e)).map(e -> (IEdge) e).toList();
        		if (selectedEdges.size() == 1) {
        			this.behaviorManager.fireOnEdgeSelected(edge);
        		}
        	}
        }
        
        if (transitionPoint != null) {
        	if (this.selectionHandler.isElementAlreadySelected(transitionPoint))
        	{
        		// This transition point will be removed only on mouse button released
        		// to avoid conflicts with dragging events
        		this.unprocessedSelectedElements.add(transitionPoint);
        	}
        	else
        	{
        		this.selectionHandler.addSelectedElement(transitionPoint);
        		this.behaviorManager.fireOnEdgeSelected(edge);
        	}
        }
        
        if (node != null)
        {
            if (this.selectionHandler.isElementAlreadySelected(node))
            {
                // This node_old will be removed only on mouse button released
                // to avoid conflicts with dragging events
                this.unprocessedSelectedElements.add(node);
            }
            else
            {
                this.selectionHandler.addSelectedElement(node);
                List<INode> selectedNodes = selectionHandler.getSelectedElements().stream().filter(e -> INode.class.isInstance(e)).map(n -> (INode) n).toList();
                if (selectedNodes.size() == 1) {
                	this.behaviorManager.fireOnNodeSelected(node);
                }
            }
        }
    }
    
    
    
    /**
     * We process node or edges on 'mouse released' event because it's not possible to remove node or edges
     * from selection on 'mouse pressed' because it can be part of a dragging intention from the user. So,
     * unprocessed elements are removed from selection only on 'mouse released' if the user is pressing CTRL.
     * They are set as unique selection if the user isn't pressing CTRL.
     * 
     * @param isResetSelectionFirst
     */
    private void processSelectionInConflictWithDraggingEvents(boolean isResetSelectionFirst) {
        if (this.unprocessedSelectedElements.isEmpty()) {
            return;
        }
        if (isResetSelectionFirst) {
            resetSelectedElements();

            for (ISelectable selectable : this.unprocessedSelectedElements) {
            	this.selectionHandler.addSelectedElement(selectable);
            }
        }
        if (!isResetSelectionFirst) {
            for (ISelectable selectable : this.unprocessedSelectedElements) {
            	this.selectionHandler.removeElementFromSelection(selectable);
            }
        }
    }
    
    @Override
    public void onPaint(Graphics2D g2)
    {
        double zoom = editorPart.getZoomFactor();
        // Draw child selection points: round handles for the transition points of the edges, gray squares for the child nodes
    	for (ISelectable element : selectionHandler.getSelectedElements()) {
    		List<ISelectable> selectableChildren = element.getSelectableChildren();
    		for (ISelectable child : selectableChildren) {
    			if (child instanceof ITransitionPoint) {
    				boolean isSelected = this.selectionHandler.isElementAlreadySelected(child);
    				GrabberUtils.drawTransitionPointHandle(g2, ((ITransitionPoint) child).toPoint2D(), zoom, isSelected, false);
    				continue;
    			}
    			List<Point2D> childPoints = child.getSelectionPoints();
    			childPoints.forEach(p -> GrabberUtils.drawGrayGrabber(g2, p));
    		}
        }
        // Draw the transition point under the mouse cursor larger, in a halo (also on an edge not selected yet)
        if (this.hoveredTransitionPoint != null)
        {
            boolean isSelected = this.selectionHandler.isElementAlreadySelected(this.hoveredTransitionPoint);
            GrabberUtils.drawTransitionPointHandle(g2, this.hoveredTransitionPoint.toPoint2D(), zoom, isSelected, true);
        }
        // Draw selection/resize handles (purple) and crop handles (teal)
    	for (ISelectable element : selectionHandler.getSelectedElements()) {
    		if (element instanceof IResizableNode) {
    			// Corner resize handles
    			IResizableNode resizable = (IResizableNode) element;
    			resizable.getResizableDragPoints().forEach((direction, rect) -> {
    				Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
    				GrabberUtils.drawResizeHandle(g2, center, zoom, false);
    			});
    		} else {
    			// Regular purple squares for non-resizable nodes/edges
    			List<Point2D> selectionPoints = element.getSelectionPoints();
    			selectionPoints.forEach(p -> GrabberUtils.drawPurpleGrabber(g2, p));
    		}
    		if (element instanceof ICroppableNode) {
    			// Edge crop handles (teal)
    			ICroppableNode croppable = (ICroppableNode) element;
    			croppable.getCropDragPoints().forEach((direction, rect) -> {
    				Point2D center = new Point2D.Double(rect.getCenterX(), rect.getCenterY());
    				GrabberUtils.drawTealCropGrabber(g2, center, direction);
    			});
    		}
        }
    }

    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;
    
    private IEditorPartBehaviorManager behaviorManager;
    
    private IGraphToolsBar graphToolsBar;
    
    private boolean isDragGesture = false;
    
    private List<ISelectable> unprocessedSelectedElements = new ArrayList<>();

    /** The transition point currently under the mouse cursor (or {@code null}). */
    private ITransitionPoint hoveredTransitionPoint = null;

    /**
     * Detects whether the mouse is hovering over a transition point (of a selected edge first, else of the edge a click would
     * select) and triggers a repaint so it is drawn larger.
     */
    private void updateHoveredTransitionPoint(MouseEvent event)
    {
        if (!GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool()))
        {
            if (this.hoveredTransitionPoint != null)
            {
                this.hoveredTransitionPoint = null;
                this.editorPart.getSwingComponent().repaint();
            }
            return;
        }
        Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        ITransitionPoint found = null;
        for (ISelectable element : selectionHandler.getSelectedElements())
        {
            if (element instanceof IEdge)
            {
                IEdge edge = (IEdge) element;
                found = edge.findTransitionPoint(mousePoint);
                if (found != null) break;
            }
        }
        IEdge edgeUnderMouse = found == null ? this.graph.findEdge(mousePoint) : null;
        if (edgeUnderMouse != null)
        {
            found = edgeUnderMouse.findTransitionPoint(mousePoint);
        }
        if (found != this.hoveredTransitionPoint)
        {
            this.hoveredTransitionPoint = found;
            this.editorPart.getSwingComponent().repaint();
        }
    }
}
