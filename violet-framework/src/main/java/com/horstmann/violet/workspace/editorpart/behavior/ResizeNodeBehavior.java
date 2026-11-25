package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.horstmann.violet.framework.util.GrabberUtils;
import com.horstmann.violet.product.diagram.abstracts.node.CropInsets;
import com.horstmann.violet.product.diagram.abstracts.node.ICroppableNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.IResizableNode;
import com.horstmann.violet.product.diagram.abstracts.node.ResizeDirection;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

public class ResizeNodeBehavior extends AbstractEditorPartBehavior
{
    private static final double MIN_SIZE = 10.0;

    public ResizeNodeBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.selectionHandler = editorPart.getSelectionHandler();
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        this.isReadyForResizing = false;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isReadyForResizing = false;
        this.lastMouseOnGrid = null;
        this.activeDirection = BehaviorUtils.findResizeDirection(editorPart, event);
        if (this.activeDirection != null)
        {
            this.isReadyForResizing = true;
            List<IResizableNode> selectedNodes = selectedResizableNodes();
            if (!selectedNodes.isEmpty())
            {
                IResizableNode node0 = selectedNodes.get(0);
                this.initialBounds = effectiveBoundsForResize(node0);
                this.initialFullBounds = null;
                this.initialCropInsets = null;
                if (node0 instanceof ICroppableNode)
                {
                    this.initialFullBounds = ((ICroppableNode) node0).getUncroppedBounds();
                    CropInsets ci = ((ICroppableNode) node0).getCropInsets();
                    this.initialCropInsets = (ci != null) ? ci.clone() : new CropInsets();
                }
                if (node0 instanceof INode)
                {
                    this.initialLocation = ((INode) node0).getLocation();
                }
            }
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        if (event.isConsumed() && !isResizing)
        {
            // The mouse is on a separator of a split node (MoveSeparatorBehavior) or holds the navigation tool (DragGraphBehavior),
            // which shows its own cursor
            return;
        }
        List<IResizableNode> sNodes = selectedResizableNodes();
        setHoveredDirection(BehaviorUtils.findResizeDirection(editorPart, event));
        if (sNodes.size() == 1)
        {
            ResizeDirection direction = this.hoveredDirection;
            if (direction != null || isResizing)
            {
                int cursorType = direction != null
                        ? direction.getCursorType()
                        : (activeDirection != null ? activeDirection.getCursorType() : Cursor.SE_RESIZE_CURSOR);
                editorPart.getSwingComponent().setCursor(Cursor.getPredefinedCursor(cursorType));
                DragSelectedBehavior.lock();
            }
            else if (!BehaviorUtils.isCursorOnCropPoint(editorPart, event))
            {
                editorPart.getSwingComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                DragSelectedBehavior.unlock();
            }
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (!isReadyForResizing || activeDirection == null || initialBounds == null)
        {
            isResizing = false;
            return;
        }

        List<IResizableNode> selectedNodes = selectedResizableNodes();
        if (selectedNodes.isEmpty())
        {
            return;
        }

        IResizableNode node0 = selectedNodes.get(0);

        // Convert mouse from screen space to graph space, on the grid: the node is resized
        // grid step by grid step, and nothing is computed nor painted between two of them
        Point2D mouse = editorPart.getGraph().getGridSticker().snap(editorPart.toGraphPoint(event.getPoint()));
        if (mouse.equals(this.lastMouseOnGrid))
        {
            return;
        }
        this.lastMouseOnGrid = mouse;
        double mouseX = mouse.getX();
        double mouseY = mouse.getY();

        double initMinX = initialBounds.getMinX();
        double initMinY = initialBounds.getMinY();
        double initMaxX = initialBounds.getMaxX();
        double initMaxY = initialBounds.getMaxY();
        double initW    = initialBounds.getWidth();
        double initH    = initialBounds.getHeight();

        double newW;
        double newH;
        double desiredMinX = initMinX;
        double desiredMinY = initMinY;

        switch (activeDirection)
        {
            case SE: newW = mouseX - initMinX; newH = mouseY - initMinY;                                              break;
            case NW: newW = initMaxX - mouseX; newH = initMaxY - mouseY; desiredMinX = mouseX; desiredMinY = mouseY;  break;
            case NE: newW = mouseX - initMinX; newH = initMaxY - mouseY;                       desiredMinY = mouseY;  break;
            case SW: newW = initMaxX - mouseX; newH = mouseY  - initMinY; desiredMinX = mouseX;                       break;
            case N:  newW = initW;             newH = initMaxY - mouseY;                       desiredMinY = mouseY;  break;
            case S:  newW = initW;             newH = mouseY  - initMinY;                                             break;
            case E:  newW = mouseX - initMinX; newH = initH;                                                          break;
            case W:  newW = initMaxX - mouseX; newH = initH;              desiredMinX = mouseX;                       break;
            default: return;
        }

        // Clamp to minimum – re-anchor fixed edge when clamped
        if (newW < MIN_SIZE)
        {
            newW = MIN_SIZE;
            switch (activeDirection)
            {
                case NW: case SW: case W: desiredMinX = initMaxX - newW; break;
                default: break;
            }
        }
        if (newH < MIN_SIZE)
        {
            newH = MIN_SIZE;
            switch (activeDirection)
            {
                case NW: case NE: case N: desiredMinY = initMaxY - newH; break;
                default: break;
            }
        }

        // Apply size and position changes
        if (node0 instanceof ICroppableNode && initialFullBounds != null && initialCropInsets != null)
        {
            // newW/newH are the desired *visible* dimensions. The whole image and its crop
            // insets are scaled by one factor, so the visible part keeps its ratio: the
            // dragged corner reaches the farthest of the mouse coordinates.
            double scale = getUniformScale(newW / initW, newH / initH);
            scale = Math.max(scale, MIN_SIZE / Math.min(initW, initH));

            double newFullW = initialFullBounds.getWidth()  * scale;
            double newFullH = initialFullBounds.getHeight() * scale;

            // The node may still adjust its size (ratio kept), so read back the actual one
            node0.setPreferredSize(new Rectangle2D.Double(0, 0, newFullW, newFullH));
            Rectangle2D actualFullBounds = ((ICroppableNode) node0).getUncroppedBounds();
            double actualFullW = actualFullBounds.getWidth();
            double actualFullH = actualFullBounds.getHeight();

            double actualScaleX = actualFullW / initialFullBounds.getWidth();
            double actualScaleY = actualFullH / initialFullBounds.getHeight();

            double newCropL = initialCropInsets.getLeft()   * actualScaleX;
            double newCropR = initialCropInsets.getRight()  * actualScaleX;
            double newCropT = initialCropInsets.getTop()    * actualScaleY;
            double newCropB = initialCropInsets.getBottom() * actualScaleY;
            ((ICroppableNode) node0).setCropInsets(new CropInsets(newCropT, newCropL, newCropB, newCropR));

            // The corner opposite to the dragged one keeps its position
            double newVisW = actualFullW - newCropL - newCropR;
            double newVisH = actualFullH - newCropT - newCropB;
            double newVisMinX = isWestSide(activeDirection) ? initMaxX - newVisW : initMinX;
            double newVisMinY = isNorthSide(activeDirection) ? initMaxY - newVisH : initMinY;
            if (node0 instanceof INode)
            {
                ((INode) node0).setLocation(new Point2D.Double(newVisMinX - newCropL, newVisMinY - newCropT));
            }
        }
        else
        {
            Dimension snapped = snap(new Dimension((int) newW, (int) newH));

            // Reposition the node when pulling its top-left corner/edge
            if (node0 instanceof INode && initialLocation != null)
            {
                double dx = desiredMinX - initMinX;
                double dy = desiredMinY - initMinY;
                if (dx != 0.0 || dy != 0.0)
                {
                    ((INode) node0).setLocation(new Point2D.Double(
                            initialLocation.getX() + dx,
                            initialLocation.getY() + dy));
                }
            }

            node0.setPreferredSize(new Rectangle2D.Double(0, 0, snapped.getWidth(), snapped.getHeight()));
        }
        isResizing = true;
        editorPart.getSwingComponent().setCursor(
                Cursor.getPredefinedCursor(activeDirection.getCursorType()));
        editorPart.getSwingComponent().repaint();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        isResizing = false;
        isReadyForResizing = false;
        activeDirection = null;
        initialBounds = null;
        initialFullBounds = null;
        initialCropInsets = null;
        initialLocation = null;
        lastMouseOnGrid = null;
        DragSelectedBehavior.unlock();
    }

    /**
     * Draws the handle under the mouse, or the one being dragged, larger, over the handles drawn by SelectByClickBehavior
     */
    @Override
    public void onPaint(Graphics2D g2)
    {
        ResizeDirection direction = this.isReadyForResizing ? this.activeDirection : this.hoveredDirection;
        List<IResizableNode> nodes = selectedResizableNodes();
        if (direction == null || nodes.size() != 1)
        {
            return;
        }
        Rectangle2D zone = nodes.get(0).getResizableDragPoints().get(direction);
        if (zone != null)
        {
            GrabberUtils.drawResizeHandle(g2, new Point2D.Double(zone.getCenterX(), zone.getCenterY()), editorPart.getZoomFactor(), true);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setHoveredDirection(ResizeDirection direction)
    {
        if (direction == this.hoveredDirection)
        {
            return;
        }
        this.hoveredDirection = direction;
        editorPart.getSwingComponent().invalidate();
        editorPart.getSwingComponent().repaint();
    }

    private List<IResizableNode> selectedResizableNodes()
    {
        return this.selectionHandler.getSelectedElements().stream()
                .filter(e -> e instanceof IResizableNode)
                .map(n -> (IResizableNode) n)
                .toList();
    }

    /**
     * Returns the bounds to use as the resize baseline.
     * For croppable nodes the visible (cropped) bounds are used so that the
     * resize drag anchors match where the corner handles are actually drawn.
     */
    private static Rectangle2D effectiveBoundsForResize(IResizableNode node)
    {
        return node.getBounds();
    }

    /**
     * @return the single scale of a node that keeps its ratio: from a corner, the larger of both, so the dragged corner reaches
     *         the farthest mouse coordinate; from a side, the one across that side
     */
    private double getUniformScale(double scaleX, double scaleY)
    {
        switch (activeDirection)
        {
            case E: case W: return scaleX;
            case N: case S: return scaleY;
            default: return Math.max(scaleX, scaleY);
        }
    }

    private boolean isWestSide(ResizeDirection direction)
    {
        return direction == ResizeDirection.NW || direction == ResizeDirection.W || direction == ResizeDirection.SW;
    }

    private boolean isNorthSide(ResizeDirection direction)
    {
        return direction == ResizeDirection.NW || direction == ResizeDirection.N || direction == ResizeDirection.NE;
    }

    private Dimension snap(Dimension dimension)
    {
        double snappingWidth  = editorPart.getGrid().getSnappingWidth();
        double snappingHeight = editorPart.getGrid().getSnappingHeight();

        int width  = (int) (Math.floor(dimension.getWidth()  / snappingWidth)  * snappingWidth);
        int height = (int) (Math.floor(dimension.getHeight() / snappingHeight) * snappingHeight);

        width  = (int) Math.max(width,  MIN_SIZE);
        height = (int) Math.max(height, MIN_SIZE);

        return new Dimension(width, height);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final IEditorPartSelectionHandler selectionHandler;
    private final IEditorPart                 editorPart;

    private boolean         isReadyForResizing = false;
    private boolean         isResizing         = false;

    /** Which of the eight handles is currently being dragged. */
    private ResizeDirection activeDirection    = null;

    /** Node bounds captured at the start of the drag gesture. */
    private Rectangle2D     initialBounds      = null;

    /** Full (uncropped) node bounds captured at drag start, for ICroppableNode. */
    private Rectangle2D     initialFullBounds  = null;

    /** Crop insets captured at drag start, for ICroppableNode. */
    private CropInsets      initialCropInsets  = null;

    /** Node location captured at the start of the drag gesture. */
    private Point2D         initialLocation    = null;

    /** The handle under the mouse, drawn larger, or null */
    private ResizeDirection hoveredDirection   = null;

    /** Mouse location on the grid at the last resize of the gesture, so the next one waits for another grid point. */
    private Point2D         lastMouseOnGrid    = null;
}