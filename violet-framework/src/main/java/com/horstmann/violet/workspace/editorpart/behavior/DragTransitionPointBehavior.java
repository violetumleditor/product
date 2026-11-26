package com.horstmann.violet.workspace.editorpart.behavior;

import java.util.stream.Collectors;
import java.util.Collections;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

public class DragTransitionPointBehavior extends AbstractEditorPartBehavior
{

    public DragTransitionPointBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.selectionHandler = editorPart.getSelectionHandler();
        this.graphToolsBar = graphToolsBar;
        this.behaviorManager = editorPart.getBehaviorManager();
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        if (event.getClickCount() > 1)
        {
            return;
        }
        if (event.getButton() != MouseEvent.BUTTON1)
        {
            return;
        }
        if (BehaviorUtils.isCursorOnResizePoint(editorPart, event)) {
        	return;
        }
        if (!isPrerequisitesOK())
        {
        	return;
        }
        if (!isSelectedToolOK())
        {
            return;
        }
        final Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        isReadyForDragging = true;
        // As transition points are added on dragging action but with the mouse
        // location saved on onMousePressed, we look for transition points from
        // this first mouse location
        firstMousePoint = mousePoint;
        lastMousePoint = mousePoint;
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (!isReadyForDragging)
        {
            return;
        }
        if (this.edgeTransitionPointToDrag == null)
        {
            // As transition points are added on dragging action, we look for transition points from
            // on dragging event too
            this.edgeTransitionPointToDrag = getEdgeTransitionPointToDrag();
            if (this.edgeTransitionPointToDrag != null)
            {
                startUndoRedoCapture();
            }
        }
        if (this.edgeTransitionPointToDrag == null)
        {
            return;
        }
        Point2D mousePoint = editorPart.toGraphPoint(event.getPoint());
        double dx = mousePoint.getX() - lastMousePoint.getX();
        double dy = mousePoint.getY() - lastMousePoint.getY();
        IGridSticker gridSticker = editorPart.getGraph().getGridSticker();

        // Drag specific transition point on selected edge
        if (this.edgeTransitionPointToDrag != null)
        {
            double newTransitionPointLocationX = this.edgeTransitionPointToDrag.getX() + dx;
            double newTransitionPointLocationY = this.edgeTransitionPointToDrag.getY() + dy;
            Point2D newTransitionPoint = new Point2D.Double(newTransitionPointLocationX, newTransitionPointLocationY);
            newTransitionPoint = gridSticker.snap(newTransitionPoint);
            // The editor draws the rest of the diagram once, not at each step
            editorPart.startMoving(Collections.<INode> emptyList(), selectionHandler.getSelectedElements().stream()
                    .filter(IEdge.class::isInstance)
                    .map(IEdge.class::cast).collect(Collectors.toList()));
            this.edgeTransitionPointToDrag.setX(newTransitionPoint.getX());
            this.edgeTransitionPointToDrag.setY(newTransitionPoint.getY());
            // Save mouse location for next dragging sequence
            Point2D snappedMousePoint = gridSticker.snap(mousePoint);
            if (!snappedMousePoint.equals(lastMousePoint)) {
                editorPart.getSwingComponent().invalidate();
                editorPart.getSwingComponent().repaint();
            }
            lastMousePoint = snappedMousePoint;
            return;
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        stopUndoRedoCapture();
        firstMousePoint = null;
        lastMousePoint = null;
        isReadyForDragging = false;
        selectedEdge = null;
        edgeTransitionPointToDrag = null;
    }

    private IEdge getSelectedEdge()
    {
        if (this.selectedEdge == null)
        {
        	List<IEdge> selectedEdges = selectionHandler.getSelectedElements().stream().filter(e -> IEdge.class.isInstance(e)).map(e -> (IEdge) e).toList();
        	if (selectedEdges.size() == 1) {
        		this.selectedEdge = selectedEdges.get(0);
        	}
        }
        return this.selectedEdge;
    }    
    
    private boolean isPrerequisitesOK()
    {
        if (getSelectedEdge() == null)
        {
            return false;
        }
        if (getSelectedEdge().isTransitionPointsSupported())
        {
            return true;
        }
        return false;
    }

    private boolean isSelectedToolOK()
    {
        if (getSelectedEdge() == null)
        {
            return false;
        }
        GraphTool selectedTool = this.graphToolsBar.getSelectedTool();
        if (GraphTool.SELECTION_TOOL.equals(selectedTool))
        {
            return true;
        }
        if (selectedTool.getNodeOrEdge() != null && selectedTool.getNodeOrEdge().getClass().isInstance(getSelectedEdge()))
        {
            return true;
        }
        return false;
    }

    private ITransitionPoint getEdgeTransitionPointToDrag()
    {
        if (getSelectedEdge() == null)
        {
            return null;
        }
        if (!getSelectedEdge().isTransitionPointsSupported())
        {
            return null;
        }
        return getSelectedEdge().findTransitionPoint(this.firstMousePoint);
    }

    private void startUndoRedoCapture()
    {
        if (getSelectedEdge() == null)
        {
            return;
        }
        this.behaviorManager.fireBeforeChangingTransitionPointsOnEdge(getSelectedEdge());
    }

    private void stopUndoRedoCapture()
    {
        if (getSelectedEdge() == null)
        {
            return;
        }
        this.behaviorManager.fireAfterChangingTransitionPointsOnEdge(getSelectedEdge());
    }

    private IEditorPartBehaviorManager behaviorManager;

    private Point2D firstMousePoint = null;

    private Point2D lastMousePoint = null;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPart editorPart;

    private IGraphToolsBar graphToolsBar;

    private boolean isReadyForDragging = false;

    private ITransitionPoint edgeTransitionPointToDrag = null;

    private IEdge selectedEdge = null;
}
