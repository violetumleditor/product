package com.horstmann.violet.workspace.editorpart.behavior;

import java.util.Collections;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.ISplittableNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * With the selection tool, dragging a separator of a split node (see {@link ISplittableNode}, the line between two lanes of a BPMN
 * pool...) moves it, the node selected or not. Only where nothing else lies over the separator: a node or an edge there is picked
 * as usual. The cursor tells the separator, and its direction, while the mouse hovers it. One drag is one undoable edit.
 * <p>
 * It consumes the events of its gesture, so it comes before the lasso, the selection and the drag.
 */
public class MoveSeparatorBehavior extends AbstractEditorPartBehavior
{

    public MoveSeparatorBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar, IGraphFile graphFile)
    {
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.graphToolsBar = graphToolsBar;
        this.graphFile = graphFile;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.node = null;
        if (event.getButton() != MouseEvent.BUTTON1 || event.isControlDown())
        {
            return;
        }
        ISplittableNode aNode = findNodeWithSeparatorAt(event);
        if (aNode == null)
        {
            return;
        }
        this.node = aNode;
        this.separatorIndex = findSeparatorIndex(aNode, event);
        this.isSeparatorVertical = isVertical(aNode.getSeparators().get(this.separatorIndex));
        this.positionsBefore = aNode.getSeparatorPositions();
        event.consume();
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.node == null)
        {
            return;
        }
        Point2D mousePoint = this.editorPart.toGraphPoint(event.getPoint());
        double x = snap(mousePoint.getX(), this.editorPart.getGrid().getSnappingWidth());
        double y = snap(mousePoint.getY(), this.editorPart.getGrid().getSnappingHeight());
        // The editor draws the rest of the diagram once, not at each step
        this.editorPart.startMoving(Collections.singletonList(this.node), Collections.<IEdge> emptyList());
        this.node.moveSeparator(this.separatorIndex, new Point2D.Double(x, y));
        this.editorPart.getSwingComponent().setCursor(getCursor(this.isSeparatorVertical));
        this.editorPart.getSwingComponent().repaint();
        event.consume();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.node == null)
        {
            return;
        }
        event.consume();
        ISplittableNode movedNode = this.node;
        String before = this.positionsBefore;
        String after = movedNode.getSeparatorPositions();
        this.node = null;
        updateCursor(event);
        if (Objects.equals(before, after))
        {
            return;
        }
        this.graphFile.setSaveRequired();
        List<UndoRedoCompoundBehavior> undoRedoBehaviors = this.editorPart.getBehaviorManager()
                .getBehaviors(UndoRedoCompoundBehavior.class);
        if (undoRedoBehaviors.size() != 1)
        {
            return;
        }
        UndoRedoCompoundBehavior undoRedoBehavior = undoRedoBehaviors.get(0);
        undoRedoBehavior.startHistoryCapture();
        CompoundEdit capturedEdit = undoRedoBehavior.getCurrentCapturedEdit();
        capturedEdit.addEdit(new AbstractUndoableEdit()
        {
            @Override
            public void undo() throws CannotUndoException
            {
                super.undo();
                movedNode.setSeparatorPositions(before);
            }

            @Override
            public void redo() throws CannotRedoException
            {
                super.redo();
                movedNode.setSeparatorPositions(after);
            }
        });
        undoRedoBehavior.stopHistoryCapture();
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.node != null)
        {
            event.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        updateCursor(event);
    }

    /**
     * Shows the resize cursor over a separator, and tells the behaviors after this one (resizing by the corners) to leave the
     * cursor as it is
     */
    private void updateCursor(MouseEvent event)
    {
        JComponent component = this.editorPart.getSwingComponent();
        ISplittableNode aNode = findNodeWithSeparatorAt(event);
        if (aNode != null)
        {
            Line2D separator = aNode.getSeparators().get(findSeparatorIndex(aNode, event));
            component.setCursor(getCursor(isVertical(separator)));
            event.consume();
            this.isCursorShown = true;
        }
        else if (this.isCursorShown)
        {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            this.isCursorShown = false;
        }
    }

    /**
     * @return the node, found where nothing else lies, that has a separator under the mouse; null if none
     */
    private ISplittableNode findNodeWithSeparatorAt(MouseEvent event)
    {
        if (!GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool()))
        {
            return null;
        }
        Point2D point = this.editorPart.toGraphPoint(event.getPoint());
        INode aNode = this.graph.findNode(point);
        if (!(aNode instanceof ISplittableNode) || this.graph.findEdge(point) != null)
        {
            return null;
        }
        ISplittableNode splittableNode = (ISplittableNode) aNode;
        return findSeparatorIndex(splittableNode, event) >= 0 ? splittableNode : null;
    }

    /**
     * @return the index of the separator of the node under the mouse; -1 if none
     */
    private int findSeparatorIndex(ISplittableNode splittableNode, MouseEvent event)
    {
        double zoom = this.editorPart.getZoomFactor();
        Point2D mousePoint = this.editorPart.toGraphPoint(event.getPoint());
        double x = mousePoint.getX();
        double y = mousePoint.getY();
        double tolerance = TOLERANCE / zoom;
        List<Line2D> separators = splittableNode.getSeparators();
        for (int i = 0; i < separators.size(); i++)
        {
            if (separators.get(i).ptSegDist(x, y) <= tolerance)
            {
                return i;
            }
        }
        return -1;
    }

    private boolean isVertical(Line2D separator)
    {
        return Math.abs(separator.getY2() - separator.getY1()) > Math.abs(separator.getX2() - separator.getX1());
    }

    private Cursor getCursor(boolean isVertical)
    {
        return Cursor.getPredefinedCursor(isVertical ? Cursor.E_RESIZE_CURSOR : Cursor.N_RESIZE_CURSOR);
    }

    private double snap(double value, double step)
    {
        return step > 0 ? Math.round(value / step) * step : value;
    }

    private final IEditorPart editorPart;

    private final IGraph graph;

    private final IGraphToolsBar graphToolsBar;

    private final IGraphFile graphFile;

    /** The node whose separator is being dragged, null between two gestures */
    private ISplittableNode node;

    private int separatorIndex;

    private boolean isSeparatorVertical;

    private String positionsBefore;

    private boolean isCursorShown;

    /** Distance to a separator, in pixels on screen, from which it is grabbed */
    private static final double TOLERANCE = 5;

}
