package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.util.GrabberUtils;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.IRotatableNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Turns a node ({@link IRotatableNode}, a free text) around its center: while it alone is selected, a small circular arrow, tied to it by a thin dashed line, lies off
 * its top (off its bottom once its top faces the right, where the pencil and the link handle stand), and dragging it turns the node
 * by steps of 15 degrees, any angle with Shift. The angle is shown beside the handle while it turns. One drag is one undoable
 * edit.
 * <p>
 * Its size on screen doesn't depend on the zoom. It consumes the events of its gesture, so it comes before the selection, the
 * lasso and the drag.
 */
public class RotateNodeBehavior extends AbstractEditorPartBehavior
{

    public RotateNodeBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar, IGraphFile graphFile)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.selectionHandler = editorPart.getSelectionHandler();
        this.graphToolsBar = graphToolsBar;
        this.graphFile = graphFile;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.node = null;
        this.isDragging = false;
        if (event.getButton() != MouseEvent.BUTTON1 || !isOnHandle(event))
        {
            return;
        }
        IRotatableNode aNode = getRotatableNode();
        this.node = aNode;
        this.rotationBefore = aNode.getRotation();
        this.locationBefore = aNode.getLocation();
        this.center = aNode.getRotationCenter();
        this.isHandleBelow = isHandleBelow(aNode.getRotationDegrees());
        this.pressAngle = getMouseAngle(event);
        event.consume();
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.node == null)
        {
            if (!this.isDragging)
            {
                this.isDragging = true; // something else is dragged: no handle meanwhile
                repaint();
            }
            return;
        }
        event.consume();
        double before = this.rotationBefore == null ? 0 : this.rotationBefore;
        double degrees = before + getMouseAngle(event) - this.pressAngle;
        if (!event.isShiftDown())
        {
            degrees = Math.round(degrees / STEP) * STEP;
        }
        // The editor draws the rest of the diagram once, not at each step
        this.editorPart.startMoving(Collections.<INode> singletonList(this.node), Collections.<IEdge> emptyList());
        this.node.setRotation(degrees);
        this.editorPart.getSwingComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        repaint();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.node == null)
        {
            if (this.isDragging)
            {
                this.isDragging = false;
                repaint();
            }
            return;
        }
        event.consume();
        IRotatableNode turnedNode = this.node;
        Double rotationAfter = turnedNode.getRotation();
        Point2D locationAfter = turnedNode.getLocation();
        Double before = this.rotationBefore;
        Point2D locationBefore = this.locationBefore;
        this.node = null;
        repaint();
        if (Objects.equals(before, rotationAfter))
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
                turnedNode.setRotation(before);
                turnedNode.setLocation(locationBefore); // exactly where it was, whatever the rounding of the center
            }

            @Override
            public void redo() throws CannotRedoException
            {
                super.redo();
                turnedNode.setRotation(rotationAfter);
                turnedNode.setLocation(locationAfter);
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
        boolean isOnHandle = isOnHandle(event);
        JComponent component = this.editorPart.getSwingComponent();
        if (isOnHandle)
        {
            if (!this.toolTip.equals(component.getToolTipText()))
            {
                component.setToolTipText(this.toolTip);
            }
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            event.consume(); // the behaviors after this one leave the cursor and the tool tip as they are
        }
        else if (this.isHovered)
        {
            component.setToolTipText(null);
            component.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
        if (isOnHandle != this.isHovered)
        {
            this.isHovered = isOnHandle;
            repaint();
        }
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        IRotatableNode aNode = this.node != null ? this.node : getRotatableNode();
        if (aNode == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        boolean isBelow = this.node != null ? this.isHandleBelow : isHandleBelow(aNode.getRotationDegrees());
        Point2D anchor = getHandleCenter(aNode, isBelow, 0);
        Point2D handle = getHandleCenter(aNode, isBelow, STEM / zoom);
        GrabberUtils.drawRotationHandle(g2, anchor, handle, zoom, this.node != null || this.isHovered);
        if (this.node != null)
        {
            drawAngle(g2, handle, aNode.getRotationDegrees(), zoom);
        }
    }

    /**
     * Draws the angle in a violet pill beside the handle, its size the same on screen whatever the zoom
     */
    private void drawAngle(Graphics2D g2, Point2D handle, double degrees, double zoom)
    {
        String text = (degrees == Math.rint(degrees) ? String.valueOf((long) degrees) : String.format("%.1f", degrees)) + "°";
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 11);
        GlyphVector glyphs = font.createGlyphVector(new FontRenderContext(null, true, true), text);
        Rectangle2D textBounds = glyphs.getLogicalBounds();
        double padding = 5;
        double width = (textBounds.getWidth() + 2 * padding) / zoom;
        double height = (textBounds.getHeight() + 2) / zoom;
        double x = handle.getX() + LABEL_GAP / zoom;
        double y = handle.getY() - height / 2;
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(LABEL_COLOR);
        g2.fill(new RoundRectangle2D.Double(x, y, width, height, height, height));
        AffineTransform toGraph = AffineTransform.getTranslateInstance(x + padding / zoom, y + (1 - textBounds.getY()) / zoom);
        toGraph.scale(1 / zoom, 1 / zoom);
        Shape outline = toGraph.createTransformedShape(glyphs.getOutline());
        g2.setColor(Color.WHITE);
        g2.fill(outline);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * @return the single selected node that can turn, when its handle may be shown; null otherwise
     */
    private IRotatableNode getRotatableNode()
    {
        if (this.isDragging || isEditingText() || !isHandleTool(this.graphToolsBar.getSelectedTool()))
        {
            return null;
        }
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (selectedElements.size() != 1 || !(selectedElements.get(0) instanceof IRotatableNode))
        {
            return null;
        }
        IRotatableNode aNode = (IRotatableNode) selectedElements.get(0);
        Rectangle2D area = this.editorPart.getDrawingArea();
        Point2D center = aNode.getRotationCenter();
        if (center.getX() < area.getMinX() || center.getY() < area.getMinY())
        {
            return null; // node picked in the side bar, still off screen until it is dragged onto the diagram
        }
        return aNode;
    }

    /**
     * As the pencil button, with the selection tool and with a node tool, which stays selected once its node is created
     */
    private boolean isHandleTool(GraphTool tool)
    {
        return GraphTool.SELECTION_TOOL.equals(tool) || (tool != null && tool.getNodeOrEdge() instanceof INode);
    }

    private boolean isEditingText()
    {
        for (EditTextInPlaceBehavior behavior : this.editorPart.getBehaviorManager().getBehaviors(EditTextInPlaceBehavior.class))
        {
            if (behavior.isEditing())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * The handle stands off the top of the node, unless its top faces the right, where the pencil and the link handle stand
     * (EditButtonBehavior, AddEdgeByDragBehavior), i.e. turned by more than 45 degrees and less than 135: then off its bottom,
     * which faces the left
     */
    private boolean isHandleBelow(double degrees)
    {
        return Math.sin(Math.toRadians(degrees)) > 0.7;
    }

    /**
     * @param distance from the middle of the top (or bottom) side of the node, outwards
     * @return the point at that distance, on the graph
     */
    private Point2D getHandleCenter(IRotatableNode aNode, boolean isBelow, double distance)
    {
        double radians = Math.toRadians(aNode.getRotationDegrees());
        double side = isBelow ? -1 : 1;
        // The top of the node, (0, -1), turned clockwise on screen
        double dx = side * Math.sin(radians);
        double dy = -side * Math.cos(radians);
        double length = aNode.getUnrotatedHeight() / 2 + distance;
        Point2D aCenter = aNode.getRotationCenter();
        return new Point2D.Double(aCenter.getX() + dx * length, aCenter.getY() + dy * length);
    }

    private boolean isOnHandle(MouseEvent event)
    {
        IRotatableNode aNode = getRotatableNode();
        if (aNode == null)
        {
            return false;
        }
        Point2D handle = getHandleCenter(aNode, isHandleBelow(aNode.getRotationDegrees()), STEM / this.editorPart.getZoomFactor());
        return event.getPoint().distance(this.editorPart.toComponentPoint(handle)) <= HIT_RADIUS;
    }

    /**
     * @return the direction from the center of the node to the mouse, in degrees, clockwise on screen
     */
    private double getMouseAngle(MouseEvent event)
    {
        Point2D mouse = this.editorPart.toGraphPoint(event.getPoint());
        return Math.toDegrees(Math.atan2(mouse.getY() - this.center.getY(), mouse.getX() - this.center.getX()));
    }

    private void repaint()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    private final IEditorPart editorPart;

    private final IEditorPartSelectionHandler selectionHandler;

    private final IGraphToolsBar graphToolsBar;

    private final IGraphFile graphFile;

    @ResourceBundleBean(key = "rotate.handle.tooltip")
    private String toolTip;

    /** The node being turned, null between two gestures */
    private IRotatableNode node;

    private Double rotationBefore;

    private Point2D locationBefore;

    /** Center of the node at the press, which doesn't move while it turns */
    private Point2D center;

    /** Where the handle stood at the press: it stays on that side during the gesture */
    private boolean isHandleBelow;

    /** Direction of the mouse at the press, in degrees */
    private double pressAngle;

    /** True while something else is dragged: the handle is hidden */
    private boolean isDragging;

    /** True while the mouse is on the handle, drawn larger */
    private boolean isHovered;

    /** Length on screen of the line between the node and the center of the handle, in pixels */
    private static final double STEM = 26;

    /** Distance on screen from the center of the handle at which it is grabbed, in pixels */
    private static final double HIT_RADIUS = 14;

    /** Distance on screen from the center of the handle to the angle shown while turning, in pixels */
    private static final double LABEL_GAP = 16;

    /** Angle step without Shift, in degrees */
    private static final double STEP = 15;

    private static final Color LABEL_COLOR = new Color(123, 79, 214);
}
