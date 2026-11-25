package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.MarkerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Scrolls the diagram while something is dragged near a border of the view, or beyond it: as the drawing area grows with its
 * elements, they can be dragged as far as wanted. A drag which starts near a border scrolls only once it gets closer to it. Once
 * dropped or added, nodes are shown with some room around.
 * <p>
 * Neither for the drag of the view itself (DragGraphBehavior), nor for the marker, as the diagram must not move under its strokes.
 */
public class AutoScrollOnDragBehavior extends AbstractEditorPartBehavior
{

    public AutoScrollOnDragBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        stop();
        if (getViewport() == null || event.getButton() != MouseEvent.BUTTON1
                || DrawMarkerBehavior.isMarkerTool(this.graphToolsBar.getSelectedTool()))
        {
            return;
        }
        this.pressPoint = getPointOnViewport(event);
        this.isPressOnNode = this.editorPart.getGraph().findNode(this.editorPart.toGraphPoint(event.getPoint())) != null;
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.pressPoint == null || (event.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == 0)
        {
            return;
        }
        this.lastEvent = event;
        this.dragPoint = getPointOnViewport(event);
        this.isDragging = this.isDragging || this.dragPoint.distance(this.pressPoint) > DRAG_THRESHOLD;
        if (this.isDragging && !isHoldingDiagram() && !getStep().equals(new Point()))
        {
            getTimer().start();
        }
        else
        {
            getTimer().stop();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        boolean isNodeDragged = this.isDragging && this.isPressOnNode && !isHoldingDiagram();
        stop();
        if (isNodeDragged)
        {
            SwingUtilities.invokeLater(this::showSelectedNodes);
        }
    }

    @Override
    public void afterAddingNodeAtPoint(INode node, Point2D location)
    {
        if (!(node instanceof MarkerNode))
        {
            // Later: the node may still be moved by what adds it
            SwingUtilities.invokeLater(() -> show(node));
        }
    }

    private void showSelectedNodes()
    {
        Rectangle2D bounds = null;
        for (ISelectable element : this.editorPart.getSelectionHandler().getSelectedElements())
        {
            if (element instanceof INode && !(element instanceof MarkerNode) && isOnDiagram((INode) element))
            {
                Rectangle2D nodeBounds = BehaviorUtils.getBoundsOnGraph((INode) element);
                bounds = bounds == null ? nodeBounds : bounds.createUnion(nodeBounds);
            }
        }
        if (bounds != null)
        {
            this.editorPart.ensureVisible(bounds);
        }
    }

    private void show(INode node)
    {
        if (isOnDiagram(node))
        {
            this.editorPart.ensureVisible(BehaviorUtils.getBoundsOnGraph(node));
        }
    }

    /**
     * While the drawing area is frozen, a node dragged from the side bar waits off screen
     */
    private boolean isOnDiagram(INode node)
    {
        return !this.editorPart.isDrawingAreaFrozen() && this.editorPart.getGraph().getAllNodes().contains(node);
    }

    private void onTick()
    {
        JViewport viewport = getViewport();
        if (viewport == null || this.lastEvent == null)
        {
            stop();
            return;
        }
        Point step = getStep();
        JComponent diagram = this.editorPart.getSwingComponent();
        Point position = viewport.getViewPosition();
        int x = Math.max(0, Math.min(position.x + step.x, diagram.getWidth() - viewport.getWidth()));
        int y = Math.max(0, Math.min(position.y + step.y, diagram.getHeight() - viewport.getHeight()));
        if (x == position.x && y == position.y)
        {
            return;
        }
        viewport.setViewPosition(new Point(x, y));
        // The mouse didn't move on screen, but it did on the diagram
        MouseEvent last = this.lastEvent;
        Point point = SwingUtilities.convertPoint(viewport, this.dragPoint, diagram);
        diagram.dispatchEvent(new MouseEvent(diagram, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), last.getModifiersEx(),
                point.x, point.y, last.getXOnScreen(), last.getYOnScreen(), last.getClickCount(), false, last.getButton()));
    }

    /**
     * @return how far to scroll at each tick: the further in the zone or beyond the border, the faster
     */
    private Point getStep()
    {
        Dimension size = getViewport().getSize();
        return new Point(getStep(this.dragPoint.x, this.pressPoint.x, size.width),
                getStep(this.dragPoint.y, this.pressPoint.y, size.height));
    }

    private int getStep(int position, int pressPosition, int size)
    {
        int zone = Math.min(ZONE, size / 4);
        int depthBefore = Math.min(zone, pressPosition - DRAG_THRESHOLD) - position;
        int depthAfter = position - Math.max(size - zone, pressPosition + DRAG_THRESHOLD);
        int depth = Math.max(depthBefore, depthAfter);
        if (depth <= 0)
        {
            return 0;
        }
        int step = Math.min(MAX_STEP, 1 + depth * MAX_STEP / (2 * ZONE));
        return depthBefore > 0 ? -step : step;
    }

    private Point getPointOnViewport(MouseEvent event)
    {
        return SwingUtilities.convertPoint(this.editorPart.getSwingComponent(), event.getPoint(), getViewport());
    }

    private JViewport getViewport()
    {
        return (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this.editorPart.getSwingComponent());
    }

    private boolean isHoldingDiagram()
    {
        return this.editorPart.getBehaviorManager().getBehaviors(DragGraphBehavior.class).stream()
                .anyMatch(DragGraphBehavior::isHoldingDiagram);
    }

    private void stop()
    {
        getTimer().stop();
        this.pressPoint = null;
        this.dragPoint = null;
        this.lastEvent = null;
        this.isDragging = false;
        this.isPressOnNode = false;
    }

    private Timer getTimer()
    {
        if (this.timer == null)
        {
            this.timer = new Timer(TICK, e -> onTick());
            this.timer.setInitialDelay(TICK);
        }
        return this.timer;
    }

    /** Width of the zone along the borders of the view where a drag scrolls, in pixels */
    private static final int ZONE = 40;

    private static final int DRAG_THRESHOLD = 8;

    /** Pixels scrolled at each tick, at most */
    private static final int MAX_STEP = 16;

    /** In ms */
    private static final int TICK = 20;

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    private Timer timer;

    /** On the viewport, as the diagram moves under it; null when there is nothing to follow */
    private Point pressPoint;

    private Point dragPoint;

    private MouseEvent lastEvent;

    private boolean isDragging;

    private boolean isPressOnNode;

}
