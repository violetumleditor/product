package com.horstmann.violet.workspace.editorpart.behavior;

import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import java.util.Map;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
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
        boolean isContainerTool = AddContainerByFrameBehavior.isContainerTool(selectedTool);
        if (isContainerTool && !BehaviorUtils.isCursorOnSelectedNode(editorPart, event)) {
            return; // the container tool draws a frame instead (see AddContainerByFrameBehavior)
        }
        final Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        if (isMouseOnNode(mousePoint)) {
            if (!isContainerTool) {
                // With the container tool, the press is on the selection (maybe upon a node inside a selected container),
                // which is dragged as it is
                changeSelectedElementIfNeeded(mousePoint);
            }
            this.draggedNodes = getNodesToDrag();
            this.draggedTransitionPoints = BehaviorUtils.getTransitionPointsToMove(this.graph, this.draggedNodes,
                    this.selectionHandler.getSelectedElements());
            this.wantedLocations.clear();
            this.isReadyForDragging = true;
            this.newParentOnDrop = null;
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
        Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        // TODO :
        // behaviorManager.fireOnElementsDragged(selectionHandler.getSelectedNodes(),
        // selectionHandler.getSelectedEdges());
        List<INode> selectedNodes = this.draggedNodes;
        if (selectedNodes.isEmpty()) {
        	return;
        }

        double dx = mousePoint.getX() - lastMousePoint.getX();
        double dy = mousePoint.getY() - lastMousePoint.getY();

        // Nodes stay in the positive coordinates of their parent; the diagram itself extends in all directions
        dx = Math.max(dx, BehaviorUtils.getMinMoveX(selectedNodes));
        dy = Math.max(dy, BehaviorUtils.getMinMoveY(selectedNodes));

        Map<SegmentedLineEdge, Point2D> adaptedEdges = BehaviorUtils.getAdaptedEdgesToMove(this.graph, selectedNodes);
        boolean isAtLeastOneNodeMoved = false;
        IGridSticker gridSticker = graph.getGridSticker();
        for (INode n : selectedNodes) {
            if (selectedNodes.contains(n.getParent())) continue; // parents are responsible for translating their
            // children
            // A child may be kept elsewhere than asked by its parent (e.g. off its siblings): it follows the mouse again later
            Point2D currentNodeLocation = this.wantedLocations.getOrDefault(n, n.getLocation());
            Point2D futureNodeLocation = new Point2D.Double(currentNodeLocation.getX() + dx, currentNodeLocation.getY() + dy);
            Point2D fixedFutureNodeLocation = gridSticker.snap(futureNodeLocation);
            if (!currentNodeLocation.equals(fixedFutureNodeLocation)) {
                n.setLocation(fixedFutureNodeLocation);
                if (n.getParent() != null) {
                    this.wantedLocations.put(n, fixedFutureNodeLocation);
                }
                isAtLeastOneNodeMoved = true;
            }
        }

        BehaviorUtils.followNodes(adaptedEdges);

        // Transition points of the edges between dragged nodes, and the selected ones (lasso), move with them
        for (ITransitionPoint aTransitionPoint : this.draggedTransitionPoints) {
            Point2D p = gridSticker.snap(new Point2D.Double(aTransitionPoint.getX() + dx, aTransitionPoint.getY() + dy));
            if (p.getX() != aTransitionPoint.getX() || p.getY() != aTransitionPoint.getY()) {
                aTransitionPoint.setX(p.getX());
                aTransitionPoint.setY(p.getY());
                isAtLeastOneNodeMoved = true;
            }
        }

        updateNewParentOnDrop(mousePoint);

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
        if (this.newParentOnDrop != null) {
            // Undone by UndoRedoOnDragBehavior, which runs after this behavior
            BehaviorUtils.moveIntoNewParent(this.graph, this.draggedNodes.get(0), this.newParentOnDrop);
            this.newParentOnDrop = null;
            editorPart.getSwingComponent().invalidate();
            editorPart.getSwingComponent().repaint();
        }
    	if (this.editorPart.getSwingComponent().getCursor().equals(this.dragCursor)) {
    		this.editorPart.getSwingComponent().setCursor(this.initialCursor);
    	}
        this.lastMousePoint = null;
        this.isReadyForDragging = false;
        this.initialCursor = null;
        this.draggedNodes = new ArrayList<>();
        this.wantedLocations.clear();
        this.draggedTransitionPoints = new ArrayList<>();
    }

    /**
     * A node dragged alone may move into another node, if the graph says so ({@link IGraph#findNewParentOnDrop}): that node is
     * outlined until the release.
     */
    private void updateNewParentOnDrop(Point2D mousePoint) {
        INode newParent = null;
        if (this.draggedNodes.size() == 1) {
            INode draggedNode = this.draggedNodes.get(0);
            newParent = this.graph.findNewParentOnDrop(draggedNode, mousePoint);
            newParent = (newParent == draggedNode.getParent()) ? null : newParent;
        }
        if (newParent != this.newParentOnDrop) {
            this.newParentOnDrop = newParent;
            editorPart.getSwingComponent().repaint();
        }
    }

    @Override
    public void onPaint(Graphics2D g2) {
        if (this.newParentOnDrop == null) {
            return;
        }
        BehaviorUtils.drawDropTarget(g2, this.newParentOnDrop);
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

    /** Locations asked by the mouse for the dragged children, which may differ from where they are shown */
    private final Map<INode, Point2D> wantedLocations = new HashMap<>();

    /** Transition points moved with the dragged nodes, see {@link BehaviorUtils#getTransitionPointsToMove} */
    private List<ITransitionPoint> draggedTransitionPoints = new ArrayList<>();

    private Cursor initialCursor = null;

    /** Node the dragged node moves into if released now, null if it stays where it is */
    private INode newParentOnDrop = null;

    private Cursor dragCursor = new Cursor(Cursor.HAND_CURSOR);


    private static volatile boolean isLocked = false;
}
