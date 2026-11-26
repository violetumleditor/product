package com.horstmann.violet.workspace.editorpart.behavior;

import java.util.Collections;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JComponent;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.util.GrabberUtils;
import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.MovedSegments;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentRoute;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Drags the segments of an automatic or bent edge ({@link SegmentedLineEdge#isSegmentsMovable()}) with the selection tool: a
 * horizontal segment up or down, a vertical one left or right. The bent style doesn't change; see {@link SegmentRoute} for what
 * happens near the nodes. A dragged segment goes on the grid, or on what attracts it: its automatic place, the next parallel
 * segment, the middle of the side of the node it is attached to (dashed line while dragging).
 * <p>
 * The selected edge shows a handle on each segment, and once adapted a button, beside the pencil, that gives its automatic path
 * back. One drag (or one tap on that button) is one undoable property edit; Escape gives up the drag.
 * <p>
 * Comes before the behaviors that move the view, select or drag, which don't get the drag of a segment. The press is left to the
 * selection, unless it is on a handle: a handle is larger than the line.
 */
public class MoveSegmentBehavior extends AbstractEditorPartBehavior
{

    public MoveSegmentBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        reset();
        this.isPressConsumed = false;
        this.hoveredIndex = -1;
        if (event.getButton() != MouseEvent.BUTTON1 || event.getClickCount() != 1 || KeyModifierUtil.isCtrl(event)
                || !isSelectionTool())
        {
            return;
        }
        if (isOnResetButton(event))
        {
            this.isPressConsumed = true;
            event.consume();
            SegmentedLineEdge selectedEdge = getSelectedEdge();
            MovedSegments before = selectedEdge.getMovedSegments();
            selectedEdge.setMovedSegments(null);
            fireChange(selectedEdge, before);
            refresh();
            return;
        }
        Point2D point = getPointOnGraph(event);
        SegmentedLineEdge foundEdge = findEdge(event);
        if (foundEdge == null)
        {
            return;
        }
        this.edge = foundEdge;
        this.route = foundEdge.getRoute();
        this.segmentIndex = findSegmentIndex(this.route, point);
        this.pressPoint = point;
        this.pressScreenPoint = event.getPoint();
        this.segmentsBefore = foundEdge.getMovedSegments();
        if (getGraph().findEdge(point) != foundEdge)
        {
            // On a handle, beside the line: would clear the selection
            this.isPressConsumed = true;
            event.consume();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.edge == null)
        {
            if (this.isPressConsumed)
            {
                event.consume();
            }
            return;
        }
        event.consume();
        if (!this.isDragging)
        {
            if (event.getPoint().distance(this.pressScreenPoint) < DRAG_THRESHOLD || getSelectedEdge() != this.edge)
            {
                return;
            }
            this.isDragging = true;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(getEscapeKeyDispatcher());
        }
        Point2D point = getPointOnGraph(event);
        boolean isHorizontal = this.route.isHorizontal(this.segmentIndex);
        double distance = isHorizontal ? point.getY() - this.pressPoint.getY() : point.getX() - this.pressPoint.getX();
        double coordinate = this.route.getCoordinate(this.segmentIndex) + distance;
        this.attraction = findAttraction(coordinate);
        this.draggedCoordinate = this.attraction != null ? this.attraction : snap(coordinate, isHorizontal);
        // The editor draws the rest of the diagram once, not at each step
        this.editorPart.startMoving(Collections.<INode> emptyList(), Collections.singletonList(this.edge));
        this.edge.setRoute(this.route.move(this.segmentIndex, this.draggedCoordinate));
        refresh();
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.edge != null && this.isDragging)
        {
            // Where it is shown, its ends maybe kept off those of other edges
            this.edge.setRoute(this.edge.getRoute());
            fireChange(this.edge, this.segmentsBefore);
        }
        if (this.isPressConsumed)
        {
            event.consume();
        }
        boolean wasDragging = this.isDragging;
        reset();
        if (wasDragging)
        {
            refresh();
        }
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.isPressConsumed && event.getClickCount() == 1)
        {
            event.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        JComponent component = this.editorPart.getSwingComponent();
        // The edit button, before this behavior, has already removed its own tool tip if the mouse is not on it
        if (isSelectionTool() && isOnResetButton(event))
        {
            component.setToolTipText(this.toolTip);
        }
        else if (this.toolTip != null && this.toolTip.equals(component.getToolTipText()))
        {
            component.setToolTipText(null);
        }
        SegmentedLineEdge foundEdge = isSelectionTool() ? findEdge(event) : null;
        int index = -1;
        if (foundEdge != null)
        {
            SegmentRoute foundRoute = foundEdge.getRoute();
            index = findSegmentIndex(foundRoute, getPointOnGraph(event));
            component.setCursor(Cursor
                    .getPredefinedCursor(foundRoute.isHorizontal(index) ? Cursor.N_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR));
            this.isCursorShown = true;
        }
        else if (this.isCursorShown)
        {
            component.setCursor(Cursor.getDefaultCursor());
            this.isCursorShown = false;
        }
        int newHoveredIndex = foundEdge != null && foundEdge == getSelectedEdge() ? index : -1;
        if (newHoveredIndex != this.hoveredIndex)
        {
            this.hoveredIndex = newHoveredIndex;
            refresh();
        }
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        SegmentedLineEdge selectedEdge = isSelectionTool() ? getSelectedEdge() : null;
        SegmentRoute selectedRoute = selectedEdge != null ? selectedEdge.getRoute() : null;
        if (selectedRoute == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        int activeIndex = this.isDragging ? findDraggedIndex(selectedRoute) : this.hoveredIndex;
        if (this.isDragging && this.attraction != null && activeIndex >= 0)
        {
            drawAttraction(g2);
        }
        List<Point2D> points = selectedRoute.getPoints();
        for (int i = 0; i + 1 < points.size(); i++)
        {
            if (hasHandle(points, i))
            {
                GrabberUtils.drawSegmentHandle(g2, getMiddle(points, i), zoom, selectedRoute.isHorizontal(i), i == activeIndex);
            }
        }
        Point2D center = getResetButtonCenter();
        if (center != null)
        {
            drawResetButton(g2, center, zoom);
        }
    }

    /**
     * @return the edge whose segment a drag would move: the selected one if the mouse is on one of its handles, else the one a
     *         click would select, its texts left out
     */
    private SegmentedLineEdge findEdge(MouseEvent event)
    {
        if (BehaviorUtils.isCursorOnResizePoint(this.editorPart, event) || BehaviorUtils.isCursorOnCropPoint(this.editorPart, event))
        {
            return null;
        }
        Point2D point = getPointOnGraph(event);
        IEdge found = getGraph().findEdge(point);
        if (found == null && getGraph().findNode(point) == null && isOnHandle(event))
        {
            return getSelectedEdge();
        }
        if (found instanceof SegmentedLineEdge)
        {
            SegmentedLineEdge foundEdge = (SegmentedLineEdge) found;
            if (foundEdge.isSegmentsMovable() && !foundEdge.isOnLabel(point))
            {
                return foundEdge;
            }
        }
        return null;
    }

    private boolean isOnHandle(MouseEvent event)
    {
        SegmentedLineEdge selectedEdge = getSelectedEdge();
        if (selectedEdge == null)
        {
            return false;
        }
        List<Point2D> points = selectedEdge.getRoute().getPoints();
        for (int i = 0; i + 1 < points.size(); i++)
        {
            if (hasHandle(points, i) && event.getPoint()
                    .distance(this.editorPart.toComponentPoint(getMiddle(points, i))) <= HANDLE_HIT_DISTANCE)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the segment is long enough for a handle (it can be dragged anyway)
     */
    private boolean hasHandle(List<Point2D> points, int index)
    {
        return points.get(index).distance(points.get(index + 1)) * this.editorPart.getZoomFactor() >= MIN_LENGTH_FOR_HANDLE;
    }

    private Point2D getMiddle(List<Point2D> points, int index)
    {
        Point2D p = points.get(index);
        Point2D q = points.get(index + 1);
        return new Point2D.Double((p.getX() + q.getX()) / 2, (p.getY() + q.getY()) / 2);
    }

    private int findSegmentIndex(SegmentRoute aRoute, Point2D point)
    {
        List<Point2D> points = aRoute.getPoints();
        int closest = 0;
        double closestDistance = Double.MAX_VALUE;
        for (int i = 0; i + 1 < points.size(); i++)
        {
            double distance = new Line2D.Double(points.get(i), points.get(i + 1)).ptSegDist(point);
            if (distance < closestDistance)
            {
                closest = i;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * @return the dragged segment in the current path, which may have more or fewer segments than when the drag started, or -1
     */
    private int findDraggedIndex(SegmentRoute currentRoute)
    {
        for (int i = 0; i < currentRoute.getSegmentCount(); i++)
        {
            if (currentRoute.isHorizontal(i) == this.route.isHorizontal(this.segmentIndex)
                    && Math.abs(currentRoute.getCoordinate(i) - this.draggedCoordinate) < 1)
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the coordinate that attracts the dragged segment, or null
     */
    private Double findAttraction(double coordinate)
    {
        List<Double> candidates = new ArrayList<Double>();
        boolean isHorizontal = this.route.isHorizontal(this.segmentIndex);
        int last = this.route.getSegmentCount() - 1;
        SegmentRoute automaticRoute = this.edge.getAutomaticRoute();
        if (automaticRoute != null && automaticRoute.getSegmentCount() == last + 1
                && automaticRoute.isHorizontal(this.segmentIndex) == isHorizontal)
        {
            candidates.add(automaticRoute.getCoordinate(this.segmentIndex));
        }
        if (this.segmentIndex >= 2)
        {
            candidates.add(this.route.getCoordinate(this.segmentIndex - 2));
        }
        if (this.segmentIndex + 2 <= last)
        {
            candidates.add(this.route.getCoordinate(this.segmentIndex + 2));
        }
        if (this.segmentIndex == 0)
        {
            candidates.add(getCenter(this.route.getStartBounds(), isHorizontal));
        }
        if (this.segmentIndex == last)
        {
            candidates.add(getCenter(this.route.getEndBounds(), isHorizontal));
        }
        for (Double aCandidate : candidates)
        {
            if (Math.abs(aCandidate - coordinate) <= ATTRACTION_PIXELS / this.editorPart.getZoomFactor())
            {
                return aCandidate;
            }
        }
        return null;
    }

    private double getCenter(Rectangle2D bounds, boolean isHorizontal)
    {
        return isHorizontal ? bounds.getCenterY() : bounds.getCenterX();
    }

    private double snap(double coordinate, boolean isHorizontal)
    {
        Point2D snapped = getGraph().getGridSticker().snap(new Point2D.Double(coordinate, coordinate));
        return isHorizontal ? snapped.getY() : snapped.getX();
    }

    /**
     * Tells undo/redo, auto-save and repainting that the path changed
     */
    private void fireChange(SegmentedLineEdge changedEdge, MovedSegments before)
    {
        MovedSegments after = changedEdge.getMovedSegments();
        if (Objects.equals(before, after))
        {
            return;
        }
        IEditorPartBehaviorManager behaviorManager = this.editorPart.getBehaviorManager();
        behaviorManager.fireBeforeEditingEdge(changedEdge);
        behaviorManager.fireWhileEditingEdge(changedEdge,
                new PropertyChangeEvent(changedEdge, SegmentedLineEdge.MOVED_SEGMENTS, before, after));
        behaviorManager.fireAfterEditingEdge(changedEdge);
    }

    /**
     * @return the edge selected alone if its segments can be dragged, else null
     */
    private SegmentedLineEdge getSelectedEdge()
    {
        List<ISelectable> selectedElements = this.editorPart.getSelectionHandler().getSelectedElements();
        if (selectedElements.size() != 1 || !(selectedElements.get(0) instanceof SegmentedLineEdge))
        {
            return null;
        }
        SegmentedLineEdge selectedEdge = (SegmentedLineEdge) selectedElements.get(0);
        return selectedEdge.isSegmentsMovable() ? selectedEdge : null;
    }

    /**
     * @return the center of the button giving the automatic path back, beside the pencil, along the segment, or null
     */
    private Point2D getResetButtonCenter()
    {
        SegmentedLineEdge selectedEdge = getSelectedEdge();
        if (selectedEdge == null || !selectedEdge.isPathAdapted())
        {
            return null;
        }
        List<EditButtonBehavior> editButtons = this.editorPart.getBehaviorManager().getBehaviors(EditButtonBehavior.class);
        Point2D pencil = editButtons.size() == 1 ? editButtons.get(0).getButtonCenter() : null;
        if (pencil == null)
        {
            return null;
        }
        List<Point2D> points = selectedEdge.getPoints();
        int middle = points.size() / 2;
        boolean isHorizontal = Math.abs(points.get(middle).getY() - points.get(middle - 1).getY()) < 0.5;
        double distance = BUTTON_DISTANCE / this.editorPart.getZoomFactor();
        return new Point2D.Double(pencil.getX() + (isHorizontal ? distance : 0), pencil.getY() + (isHorizontal ? 0 : distance));
    }

    private boolean isOnResetButton(MouseEvent event)
    {
        Point2D center = getResetButtonCenter();
        return center != null && event.getPoint().distance(this.editorPart.toComponentPoint(center)) <= RADIUS + HIT_MARGIN;
    }

    private void drawResetButton(Graphics2D g2, Point2D center, double zoom)
    {
        double u = 1 / zoom;
        double r = RADIUS * u;
        double x = center.getX();
        double y = center.getY();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(SHADOW_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r + u, 2 * r, 2 * r));
        g2.setColor(BUTTON_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        // Arrow turning back
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke((float) (1.8 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(x - 6 * u, y - 6 * u, 12 * u, 12 * u, 150, -290, Arc2D.OPEN));
        Path2D head = new Path2D.Double();
        head.moveTo(x - 6.5 * u, y - 7.5 * u);
        head.lineTo(x - 5.5 * u, y - 2.5 * u);
        head.lineTo(x - 0.8 * u, y - 3.5 * u);
        g2.draw(head);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private void drawAttraction(Graphics2D g2)
    {
        Rectangle2D area = this.editorPart.getDrawingArea();
        double zoom = this.editorPart.getZoomFactor();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(ATTRACTION_COLOR);
        g2.setStroke(new BasicStroke((float) (1 / zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                new float[] { (float) (3 / zoom), (float) (4 / zoom) }, 0));
        g2.draw(this.route.isHorizontal(this.segmentIndex)
                ? new Line2D.Double(area.getMinX(), this.attraction, area.getMaxX(), this.attraction)
                : new Line2D.Double(this.attraction, area.getMinY(), this.attraction, area.getMaxY()));
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private KeyEventDispatcher getEscapeKeyDispatcher()
    {
        if (this.escapeKeyDispatcher == null)
        {
            this.escapeKeyDispatcher = event -> {
                if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_ESCAPE || !this.isDragging)
                {
                    return false;
                }
                this.edge.setMovedSegments(this.segmentsBefore);
                reset();
                this.isPressConsumed = true; // the rest of the gesture does nothing
                refresh();
                return true;
            };
        }
        return this.escapeKeyDispatcher;
    }

    private void reset()
    {
        if (this.isDragging)
        {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        }
        this.edge = null;
        this.route = null;
        this.attraction = null;
        this.segmentsBefore = null;
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
        return this.editorPart.toGraphPoint(event.getPoint());
    }

    private boolean isSelectionTool()
    {
        return GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool());
    }

    private IGraph getGraph()
    {
        return this.editorPart.getGraph();
    }

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    /** The edge whose segment is pressed, or null */
    private SegmentedLineEdge edge;

    /** Its path when pressed */
    private SegmentRoute route;

    private int segmentIndex;

    private Point2D pressPoint;

    private Point pressScreenPoint;

    private MovedSegments segmentsBefore;

    private double draggedCoordinate;

    /** What attracts the dragged segment now, or null */
    private Double attraction;

    private boolean isDragging;

    private boolean isPressConsumed;

    /** The segment of the selected edge under the mouse, or -1 */
    private int hoveredIndex = -1;

    /** True if this behavior set the cursor */
    private boolean isCursorShown;

    private KeyEventDispatcher escapeKeyDispatcher;

    @ResourceBundleBean(key = "segment.reset.tooltip")
    private String toolTip;

    /** Distance on screen before a press on a segment becomes a drag (a click selects, a double click edits) */
    private static final double DRAG_THRESHOLD = 4;

    private static final double ATTRACTION_PIXELS = 8;

    private static final double HANDLE_HIT_DISTANCE = 12;

    private static final double MIN_LENGTH_FOR_HANDLE = 22;

    private static final double RADIUS = 16;

    private static final double HIT_MARGIN = 2;

    /** Distance on screen from the center of the pencil */
    private static final double BUTTON_DISTANCE = 38;

    private static final Color BUTTON_COLOR = new Color(123, 79, 214);

    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 70);

    private static final Color ATTRACTION_COLOR = new Color(194, 65, 124);

}
