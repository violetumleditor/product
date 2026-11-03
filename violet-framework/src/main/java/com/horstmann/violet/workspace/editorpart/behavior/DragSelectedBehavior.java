package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

public class DragSelectedBehavior extends AbstractEditorPartBehavior {

    public DragSelectedBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar) {
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getClickCount() > 1) {
            return;
        }
        if (event.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        if (BehaviorUtils.isCursorOnResizePoint(editorPart, event)) {
        	return;
        }
        if (BehaviorUtils.isCursorOnCropPoint(editorPart, event)) {
        	return;
        }
        
        GraphTool selectedTool = this.selectionHandler.getSelectedTool();
        if (IEdge.class.isInstance(selectedTool.getNodeOrEdge())) {
            return;
        }
        double zoom = editorPart.getZoomFactor();
        final Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        if (isMouseOnNode(mousePoint)) {
            changeSelectedElementIfNeeded(mousePoint);
            this.draggedNodes = getNodesToDrag();
            this.isReadyForDragging = true;
            this.lastMousePoint = mousePoint;
            this.initialCursor = this.editorPart.getSwingComponent().getCursor();
            this.editorPart.getSwingComponent().setCursor(this.dragCursor);
        }
    }

    private boolean isMouseOnNode(Point2D mouseLocation) {
        if (isLocked) {
            return false;
        }
        INode node = this.graph.findNode(mouseLocation);
        if (node == null) {
            return false;
        }
        return true;
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (!isReadyForDragging) {
            return;
        }
        double zoom = editorPart.getZoomFactor();
        Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        // TODO :
        // behaviorManager.fireOnElementsDragged(selectionHandler.getSelectedNodes(),
        // selectionHandler.getSelectedEdges());
        List<INode> selectedNodes = this.draggedNodes;
        if (selectedNodes.isEmpty()) {
        	return;
        }
        INode lastNode = selectedNodes.get(selectedNodes.size() - 1);

        Rectangle2D bounds = lastNode.getBounds();
        double dx = mousePoint.getX() - lastMousePoint.getX();
        double dy = mousePoint.getY() - lastMousePoint.getY();

        // we don't want to drag node into negative coordinates
        // particularly with multiple selection, we might never be
        // able to get them back.
        for (INode n : selectedNodes)
            bounds.add(n.getBounds());
        dx = Math.max(dx, -bounds.getX());
        dy = Math.max(dy, -bounds.getY());

        boolean isAtLeastOneNodeMoved = false;
        IGridSticker gridSticker = graph.getGridSticker();
        for (INode n : selectedNodes) {
            if (selectedNodes.contains(n.getParent())) continue; // parents are responsible for translating their
            // children
            Point2D currentNodeLocation = n.getLocation();
            Point2D futureNodeLocation = new Point2D.Double(currentNodeLocation.getX() + dx, currentNodeLocation.getY() + dy);
            Point2D fixedFutureNodeLocation = gridSticker.snap(futureNodeLocation);
            if (!currentNodeLocation.equals(fixedFutureNodeLocation)) {
                n.setLocation(fixedFutureNodeLocation);
                isAtLeastOneNodeMoved = true;
            }
        }

        // Drag transition points on edges
        Iterator<IEdge> iterOnEdges = graph.getAllEdges().iterator();
        while (iterOnEdges.hasNext()) {
            IEdge e = (IEdge) iterOnEdges.next();
            INode startingNode = e.getStartNode();
            INode endinNode = e.getEndNode();
            if (selectedNodes.contains(startingNode) && selectedNodes.contains(endinNode)) {
                ITransitionPoint[] transitionPoints = e.getTransitionPoints();
                for (ITransitionPoint aTransitionPoint : transitionPoints) {
                    double newTransitionPointLocationX = aTransitionPoint.getX() + dx;
                    double newTransitionPointLocationY = aTransitionPoint.getY() + dy;
                    Point2D p = new Point2D.Double(newTransitionPointLocationX, newTransitionPointLocationY);
                    p = gridSticker.snap(p);
                    aTransitionPoint.setX(p.getX());
                    aTransitionPoint.setY(p.getY());
                }
                e.setTransitionPoints(transitionPoints);
            }
        }

        // Save mouse location for next dragging sequence
        if (isAtLeastOneNodeMoved) {
            Point2D snappedMousePoint = gridSticker.snap(mousePoint);
            if (!snappedMousePoint.equals(lastMousePoint)) {
                editorPart.getSwingComponent().invalidate();
                editorPart.getSwingComponent().repaint();
            }
            lastMousePoint = snappedMousePoint;
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
    	if (this.editorPart.getSwingComponent().getCursor().equals(this.dragCursor)) {
    		this.editorPart.getSwingComponent().setCursor(this.initialCursor);
    	}
        this.lastMousePoint = null;
        this.isReadyForDragging = false;
        this.initialCursor = null;
        this.draggedNodes = new ArrayList<>();
    }

    /**
     * Selected nodes plus the nodes located inside selected containers. Computed when
     * the drag starts so that a moving container doesn't catch nodes on its way.
     */
    private List<INode> getNodesToDrag() {
        Set<INode> nodesToDrag = new LinkedHashSet<>();
        List<INode> selectedNodes = this.selectionHandler.getSelectedElements().stream().filter(e -> INode.class.isInstance(e)).map(n -> (INode) n).toList();
        for (INode aNode : selectedNodes) {
            if (aNode instanceof ContainerNode) {
                nodesToDrag.addAll(((ContainerNode) aNode).getContainedNodes());
            }
            nodesToDrag.add(aNode);
        }
        return new ArrayList<>(nodesToDrag);
    }

    private void changeSelectedElementIfNeeded(Point2D mouseLocation) {
        IEdge edge = this.graph.findEdge(mouseLocation);
        if (edge != null) {
            // We don't want to drag edges
            return;
        }
        INode node = this.graph.findNode(mouseLocation);
        if (node == null) {
            return;
        }
        List<INode> selectedNodes = this.selectionHandler.getSelectedElements().stream().filter(e -> INode.class.isInstance(e)).map(n -> (INode) n).toList();
        if (!selectedNodes.contains(node)) {
            this.selectionHandler.clearSelection();
            this.selectionHandler.addSelectedElement(node);
        }
    }


    public static void lock() {
        isLocked = true;
    }

    public static void unlock() {
        isLocked = false;
    }


    private IGraph graph;

    private Point2D lastMousePoint = null;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPart editorPart;

    private boolean isReadyForDragging = false;

    private List<INode> draggedNodes = new ArrayList<>();

    private Cursor initialCursor = null;

    private Cursor dragCursor = new Cursor(Cursor.HAND_CURSOR);


    private static volatile boolean isLocked = false;
}
