package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.Objects;

import javax.swing.JComponent;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IMovableLabelElement;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A text of an edge (start, middle or end, or the one of a state transition), or the text a node draws outside (a BPMN event or
 * gateway), can be dragged anywhere with the selection tool. The element keeps its offset from the automatic place of the text,
 * which follows the element: the text follows when the nodes move. Its top left corner goes on the
 * grid; released near its automatic place, it goes back there (no offset). One drag is one undoable property edit.
 * <p>
 * The move cursor shows over a text; while dragging, a dashed violet line ties the text to the point of the element it belongs to.
 * Comes before DragSelectedBehavior, which doesn't get the drags of a text.
 */
public class MoveLabelBehavior extends AbstractEditorPartBehavior
{

    public MoveLabelBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        boolean isOverText = isSelectionTool() && findTextElement(getPointOnGraph(event)) != null;
        JComponent component = this.editorPart.getSwingComponent();
        if (isOverText)
        {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            this.isCursorShown = true;
        }
        else if (this.isCursorShown)
        {
            component.setCursor(Cursor.getDefaultCursor());
            this.isCursorShown = false;
        }
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        reset();
        if (event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() != 1 || !isSelectionTool())
        {
            return;
        }
        Point2D point = getPointOnGraph(event);
        IMovableLabelElement element = findTextElement(point);
        if (element == null || BehaviorUtils.isCursorOnResizePoint(this.editorPart, event)
                || BehaviorUtils.isCursorOnCropPoint(this.editorPart, event))
        {
            return;
        }
        this.element = element;
        this.labelName = element.findLabel(point);
        this.pressPoint = point;
        this.pressScreenPoint = event.getPoint();
        this.originalOffset = element.getLabelOffset(this.labelName);
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.element == null)
        {
            return;
        }
        if (!this.isDragging && event.getPoint().distance(this.pressScreenPoint) < DRAG_THRESHOLD)
        {
            event.consume();
            return;
        }
        this.isDragging = true;
        Point2D point = getPointOnGraph(event);
        double x = point.getX() - this.pressPoint.getX() + (this.originalOffset != null ? this.originalOffset.getX() : 0);
        double y = point.getY() - this.pressPoint.getY() + (this.originalOffset != null ? this.originalOffset.getY() : 0);
        this.element.setLabelOffset(this.labelName, getOffset(x, y));
        refresh();
        event.consume();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.element != null && this.isDragging)
        {
            Point2D.Double newOffset = this.element.getLabelOffset(this.labelName);
            if (!Objects.equals(this.originalOffset, newOffset))
            {
                // Undo/redo, auto-save and repainting listen to these
                IEditorPartBehaviorManager behaviorManager = this.editorPart.getBehaviorManager();
                PropertyChangeEvent change = new PropertyChangeEvent(this.element,
                        this.element.getLabelOffsetPropertyName(this.labelName), this.originalOffset, newOffset);
                if (this.element instanceof IEdge)
                {
                    IEdge edge = (IEdge) this.element;
                    behaviorManager.fireBeforeEditingEdge(edge);
                    behaviorManager.fireWhileEditingEdge(edge, change);
                    behaviorManager.fireAfterEditingEdge(edge);
                }
                if (this.element instanceof INode)
                {
                    INode node = (INode) this.element;
                    behaviorManager.fireBeforeEditingNode(node);
                    behaviorManager.fireWhileEditingNode(node, change);
                    behaviorManager.fireAfterEditingNode(node);
                }
            }
        }
        reset();
        refresh();
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        if (this.element == null || !this.isDragging)
        {
            return;
        }
        Rectangle2D bounds = this.element.getLabelBounds(this.labelName);
        if (bounds == null)
        {
            return;
        }
        Point2D anchor = this.element.getLabelAnchor(this.labelName);
        Point2D closest = new Point2D.Double(Math.max(bounds.getMinX(), Math.min(anchor.getX(), bounds.getMaxX())),
                Math.max(bounds.getMinY(), Math.min(anchor.getY(), bounds.getMaxY())));
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(TIE_COLOR);
        g2.setStroke(TIE_STROKE);
        g2.draw(new Line2D.Double(anchor, closest));
        g2.draw(bounds);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * @param x horizontal offset from the automatic place, as dragged
     * @param y vertical offset, as dragged
     * @return the offset putting the top left corner of the text on the grid, or null near the automatic place
     */
    private Point2D.Double getOffset(double x, double y)
    {
        Rectangle2D defaultBounds = this.element.getDefaultLabelBounds(this.labelName);
        if (defaultBounds == null || Math.hypot(x, y) < BACK_HOME_PIXELS / this.editorPart.getZoomFactor())
        {
            return null;
        }
        Point2D corner = getGraph().getGridSticker().snap(new Point2D.Double(defaultBounds.getX() + x, defaultBounds.getY() + y));
        double dx = corner.getX() - defaultBounds.getX();
        double dy = corner.getY() - defaultBounds.getY();
        if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5)
        {
            return null;
        }
        return new Point2D.Double(dx, dy);
    }

    /**
     * @return the element whose text is under the point, or null
     */
    private IMovableLabelElement findTextElement(Point2D point)
    {
        // The element a click selects (an edge first), if its text is there
        Object found = getGraph().findEdge(point);
        if (found == null)
        {
            found = getGraph().findNode(point);
        }
        if (found instanceof IMovableLabelElement && ((IMovableLabelElement) found).findLabel(point) != null)
        {
            return (IMovableLabelElement) found;
        }
        return null;
    }

    private void reset()
    {
        this.element = null;
        this.labelName = null;
        this.pressPoint = null;
        this.pressScreenPoint = null;
        this.originalOffset = null;
        this.isDragging = false;
    }

    private void refresh()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    private Point2D getPointOnGraph(MouseEvent event)
    {
        double zoom = this.editorPart.getZoomFactor();
        return new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
    }

    private boolean isSelectionTool()
    {
        return this.graphToolsBar == null || GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool());
    }

    private IGraph getGraph()
    {
        return this.editorPart.getGraph();
    }

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    /** The element whose text is pressed, or null */
    private IMovableLabelElement element;

    /** Which text */
    private String labelName;

    private Point2D pressPoint;

    private java.awt.Point pressScreenPoint;

    private Point2D.Double originalOffset;

    private boolean isDragging;

    /** True if this behavior set the move cursor */
    private boolean isCursorShown;

    /** Distance on screen before a press on a text becomes a drag (a click selects, a double click edits) */
    private static final double DRAG_THRESHOLD = 3;

    /** Distance on screen from its automatic place under which a released text goes back there */
    private static final double BACK_HOME_PIXELS = 10;

    private static final Color TIE_COLOR = new Color(179, 102, 179);

    private static final Stroke TIE_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] { 4, 3 },
            0);
}
