package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Creates a container by drawing its frame with the mouse (click + drag), like the lasso selection, when the container tool is
 * selected.
 * <p>
 * A simple click (without drag) is left to {@link AddNodeBehavior}, which creates the container at its default size. As
 * {@link AddNodeBehavior} ignores any click followed by a drag, a frame too small to be meant as such also creates a container
 * at its default size, so that a slight move of the mouse during the click never loses the creation. Either way, the selection
 * tool is selected again once the container is created.
 * </p>
 * With this tool, a press on a selected node (such as the container just created) drags it with {@link DragSelectedBehavior}.
 * Anywhere else, {@link DragSelectedBehavior} stays idle, so that a frame can start upon another node or inside a larger
 * container.
 *
 * @author Alexandre de Pellegrin
 */
public class AddContainerByFrameBehavior extends AbstractEditorPartBehavior
{

    public AddContainerByFrameBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
    }

    /**
     * @return true if the tool creates containers, i.e. if frames can be drawn with it
     */
    public static boolean isContainerTool(GraphTool tool)
    {
        return tool != null && tool.getNodeOrEdge() instanceof ContainerNode;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        reset();
        if (event.getClickCount() > 1 || event.getButton() != MouseEvent.BUTTON1)
        {
            return;
        }
        if (KeyModifierUtil.isCtrl(event))
        {
            return; // Ctrl + drag scrolls the diagram (see DragGraphBehavior)
        }
        if (!isContainerTool(this.selectionHandler.getSelectedTool()))
        {
            return;
        }
        if (BehaviorUtils.isCursorOnResizePoint(editorPart, event) || BehaviorUtils.isCursorOnCropPoint(editorPart, event))
        {
            return;
        }
        if (BehaviorUtils.isCursorOnSelectedNode(editorPart, event))
        {
            return; // drags the selection, as the container just created (see DragSelectedBehavior)
        }
        this.startPoint = graph.getGridSticker().snap(toGraphPoint(event));
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.startPoint == null)
        {
            return;
        }
        Point2D snappedPoint = graph.getGridSticker().snap(toGraphPoint(event));
        if (snappedPoint.equals(this.endPoint))
        {
            return;
        }
        this.endPoint = snappedPoint;
        repaint();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.startPoint == null || this.endPoint == null)
        {
            reset(); // no drag : simple click handled by AddNodeBehavior
            return;
        }
        GraphTool selectedTool = this.selectionHandler.getSelectedTool();
        Rectangle2D frame = getFrame();
        Point2D location = frame != null ? new Point2D.Double(frame.getX(), frame.getY()) : this.startPoint;
        reset();
        if (!isContainerTool(selectedTool))
        {
            return;
        }
        ContainerNode newNode = ((ContainerNode) selectedTool.getNodeOrEdge()).clone();
        newNode.setId(new Id());
        if (frame != null)
        {
            newNode.setPreferredSize(new Rectangle2D.Double(0, 0, frame.getWidth(), frame.getHeight()));
        }
        if (addNodeAtPoint(newNode, location))
        {
            selectionHandler.setSelectedElement(newNode);
            graphToolsBar.reset(); // back to the selection tool once the container is created
        }
        repaint();
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        Rectangle2D frame = getFrame();
        if (frame == null)
        {
            return;
        }
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(FRAME_COLOR);
        g2.setStroke(FRAME_STROKE);
        g2.draw(frame);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Adds the node and notifies the other behaviors (undo/redo, ...) as AddNodeBehavior does
     */
    private boolean addNodeAtPoint(ContainerNode newNode, Point2D location)
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

    /**
     * @return the frame being drawn, or null if there is none or if it is too small to be meant as a frame
     */
    private Rectangle2D getFrame()
    {
        if (this.startPoint == null || this.endPoint == null)
        {
            return null;
        }
        double x1 = this.startPoint.getX();
        double y1 = this.startPoint.getY();
        double x2 = this.endPoint.getX();
        double y2 = this.endPoint.getY();
        Rectangle2D frame = new Rectangle2D.Double(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
        if (frame.getWidth() < MIN_FRAME_SIZE || frame.getHeight() < MIN_FRAME_SIZE)
        {
            return null;
        }
        return frame;
    }

    private Point2D toGraphPoint(MouseEvent event)
    {
        double zoom = editorPart.getZoomFactor();
        return new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
    }

    private void reset()
    {
        this.startPoint = null;
        this.endPoint = null;
    }

    private void repaint()
    {
        editorPart.getSwingComponent().invalidate();
        editorPart.getSwingComponent().repaint();
    }

    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPartBehaviorManager behaviorManager;

    private IGraphToolsBar graphToolsBar;

    /** Corners of the frame being drawn (graph coordinates, snapped on the grid) */
    private Point2D startPoint;

    private Point2D endPoint;

    /** Under this width or height (graph coordinates), the frame is not meant as such */
    private static final double MIN_FRAME_SIZE = 10;

    private static final Color FRAME_COLOR = new Color(0.7f, 0.4f, 0.7f);

    private static final Stroke FRAME_STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
            new float[] { 4, 3 }, 0);
}
