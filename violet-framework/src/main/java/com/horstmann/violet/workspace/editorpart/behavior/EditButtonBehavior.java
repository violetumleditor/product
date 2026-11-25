package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ShapeEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.MarkerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Draws a round "pencil" button next to the selected element, which opens its properties dialog (as a double click does, which
 * is hard on a touch screen). Shown while the selection tool or a node tool is chosen and exactly one node or edge is selected: off the top
 * right corner of a node, at the middle of an edge. Its size on screen doesn't depend on the zoom. Hidden while something is
 * dragged.
 * <p>
 * It is drawn by the editor over the diagram, so it never appears in exports, prints nor files. It must be the first behavior of
 * the editor: it consumes the mouse events of a gesture started on it, and the behavior manager then stops dispatching them, so
 * that the press neither clears the selection, nor starts a lasso or a drag.
 */
public class EditButtonBehavior extends AbstractEditorPartBehavior
{

    public EditButtonBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.selectionHandler = editorPart.getSelectionHandler();
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isGestureOnButton = false;
        this.isDragging = false;
        if (event.getButton() == MouseEvent.BUTTON1 && isOnButton(event))
        {
            this.isGestureOnButton = true;
            event.consume();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            return;
        }
        if (!this.isDragging)
        {
            this.isDragging = true;
            repaint();
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            if (isOnButton(event))
            {
                // After the current mouse event, as the dialog is modal
                SwingUtilities.invokeLater(() -> editSelected());
            }
            return;
        }
        if (this.isDragging)
        {
            this.isDragging = false;
            repaint();
        }
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        String toolTip = isOnButton(event) ? this.toolTip : null;
        JComponent component = this.editorPart.getSwingComponent();
        if (toolTip == null ? component.getToolTipText() != null : !toolTip.equals(component.getToolTipText()))
        {
            component.setToolTipText(toolTip);
        }
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        Point2D center = getButtonCenter();
        if (center == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        double r = RADIUS / zoom;
        double x = center.getX();
        double y = center.getY();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(SHADOW_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r + 1 / zoom, 2 * r, 2 * r));
        g2.setColor(BUTTON_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        // Pencil, drawn on a 16 px grid centered on the button
        double u = 1 / zoom;
        Path2D pencil = new Path2D.Double();
        pencil.moveTo(x - 6 * u, y + 6 * u);
        pencil.lineTo(x - 4.8 * u, y + 1.8 * u);
        pencil.lineTo(x + 2.6 * u, y - 5.6 * u);
        pencil.lineTo(x + 5.6 * u, y - 2.6 * u);
        pencil.lineTo(x - 1.8 * u, y + 4.8 * u);
        pencil.closePath();
        pencil.moveTo(x + 0.6 * u, y - 3.6 * u);
        pencil.lineTo(x + 3.6 * u, y - 0.6 * u);
        g2.setColor(PENCIL_COLOR);
        g2.setStroke(new BasicStroke((float) (1.8 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(pencil);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * @return true while a text is edited in place: the buttons could lie on it
     */
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
     * Also places the link handle ({@link AddEdgeByDragBehavior}) and the "+" ({@link AddNodeByTapBehavior}), just below this
     * button: they are shown only with it.
     *
     * @return the center of the button on the graph (unzoomed coordinates), or {@code null} if no button is shown
     */
    Point2D getButtonCenter()
    {
        if (this.isDragging || isEditingText() || !isButtonTool(this.graphToolsBar.getSelectedTool()))
        {
            return null;
        }
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (selectedElements.size() != 1)
        {
            return null;
        }
        ISelectable element = selectedElements.get(0);
        double zoom = this.editorPart.getZoomFactor();
        double r = RADIUS / zoom;
        Rectangle2D area = this.editorPart.getDrawingArea();
        if (element instanceof MarkerNode)
        {
            return null; // nothing to edit, to link nor to add inside
        }
        if (element instanceof INode)
        {
            Rectangle2D bounds = getBoundsOnGraph((INode) element);
            if (bounds == null || bounds.getMaxX() < area.getMinX() || bounds.getMaxY() < area.getMinY())
            {
                return null; // node picked in the side bar, still off screen until it is dragged onto the diagram
            }
            double offset = (RADIUS + GAP) / zoom;
            double x = bounds.getMaxX() + offset;
            double y = bounds.getMinY() - offset;
            // Stays on the drawing area, even for a node at its top or right border
            double maxX = area.getMaxX() - r;
            x = Math.max(area.getMinX() + r, Math.min(x, maxX));
            y = Math.max(area.getMinY() + r, y);
            return new Point2D.Double(x, y);
        }
        if (element instanceof IEdge)
        {
            IEdge edge = (IEdge) element;
            Line2D segment = getMiddleSegment(edge);
            if (segment == null)
            {
                return null;
            }
            Rectangle2D label = edge.getMiddleLabelBounds();
            Point2D besideLine = getBesideMiddleSegment(segment, label, (RADIUS + 2 * GAP) / zoom);
            Point2D center = avoidLabel(besideLine, label, r + GAP / zoom);
            return new Point2D.Double(Math.max(area.getMinX() + r, center.getX()),
                    Math.max(area.getMinY() + r, center.getY()));
        }
        return null;
    }

    /**
     * The button lies beside the middle of the edge, off its line (it would hide the line, and a press there is for the edge), on
     * the side opposite to its text: the text typed straight on the selected edge (TypeTextOnSelectedNodeBehavior) appears next to
     * the middle of the edge, so the button must not lie there, nor jump at the first typed character. Without text, the side is
     * the one where SegmentedLineEdge draws it: above a horizontal segment (so the button is below it), else on the right of the
     * segment, upwards when it goes down to the right, downwards when it goes up.
     *
     * @param segment the middle segment of the edge
     * @param label the bounds of the text at the middle of the edge, or null
     * @param distance the distance from the line to the center of the button, on the graph
     * @return the center of the button
     */
    private Point2D getBesideMiddleSegment(Line2D segment, Rectangle2D label, double distance)
    {
        Point2D middle = new Point2D.Double((segment.getX1() + segment.getX2()) / 2, (segment.getY1() + segment.getY2()) / 2);
        double dx = segment.getX2() - segment.getX1();
        double dy = segment.getY2() - segment.getY1();
        double length = Math.hypot(dx, dy);
        if (length < 1e-6)
        {
            return new Point2D.Double(middle.getX(), middle.getY() + distance);
        }
        // Normal to the segment
        double nx = -dy / length;
        double ny = dx / length;
        // Direction of the text from the middle
        double textX;
        double textY;
        if (label != null && !label.isEmpty())
        {
            textX = label.getCenterX() - middle.getX();
            textY = label.getCenterY() - middle.getY();
        }
        else
        {
            // As drawn from its left end to its right end
            double leftToRightY = dx >= 0 ? dy : -dy;
            boolean isHorizontal = Math.abs(dy) < 1e-6;
            textX = isHorizontal ? 0 : 1;
            textY = isHorizontal ? -1 : Math.signum(-leftToRightY);
        }
        if (nx * textX + ny * textY > 0)
        {
            nx = -nx;
            ny = -ny;
        }
        return new Point2D.Double(middle.getX() + nx * distance, middle.getY() + ny * distance);
    }

    /**
     * The text at the middle of an edge is drawn next to its middle: there the button would hide it, and a tap on the text would
     * open the dialog. So the button moves away from the text, straight from its center, to the first place where it doesn't touch
     * it any more, which is the other side of the line for a straight edge.
     *
     * @param middle the middle of the edge
     * @param label the bounds of the text at the middle of the edge, or null
     * @param clearance the radius of the button plus the space to leave around it, on the graph
     * @return the center of the button
     */
    private Point2D avoidLabel(Point2D middle, Rectangle2D label, double clearance)
    {
        if (label == null || label.isEmpty())
        {
            return middle;
        }
        Rectangle2D forbidden = new Rectangle2D.Double(label.getX() - clearance, label.getY() - clearance,
                label.getWidth() + 2 * clearance, label.getHeight() + 2 * clearance);
        if (!forbidden.contains(middle))
        {
            return middle;
        }
        double dx = middle.getX() - label.getCenterX();
        double dy = middle.getY() - label.getCenterY();
        double length = Math.hypot(dx, dy);
        if (length < 1e-6)
        {
            dx = 0;
            dy = 1;
        }
        else
        {
            dx /= length;
            dy /= length;
        }
        double tx = dx > 0 ? (forbidden.getMaxX() - middle.getX()) / dx
                : dx < 0 ? (forbidden.getMinX() - middle.getX()) / dx : Double.POSITIVE_INFINITY;
        double ty = dy > 0 ? (forbidden.getMaxY() - middle.getY()) / dy
                : dy < 0 ? (forbidden.getMinY() - middle.getY()) / dy : Double.POSITIVE_INFINITY;
        double t = Math.min(tx, ty);
        return new Point2D.Double(middle.getX() + t * dx, middle.getY() + t * dy);
    }

    /**
     * A child node (activation bar in its lifeline, class in its package...) has its location and bounds relative to its parent:
     * the button is placed from its absolute location on the graph, as {@code AbstractGraph.findNode} does.
     *
     * @return the bounds of the node on the graph, or {@code null} if unknown
     */
    private Rectangle2D getBoundsOnGraph(INode node)
    {
        Rectangle2D bounds = node.getBounds();
        Point2D locationOnGraph = node.getLocationOnGraph();
        if (bounds == null || locationOnGraph == null)
        {
            return null;
        }
        return new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * The button is shown with the selection tool, and also with a node tool: a click on the diagram with a node tool creates and
     * selects a node but keeps the tool, and the new node must offer its button as one dropped from the side bar does. As the
     * button consumes its gesture, a click on it never creates another node.
     */
    private boolean isButtonTool(GraphTool tool)
    {
        return GraphTool.SELECTION_TOOL.equals(tool) || (tool != null && tool.getNodeOrEdge() instanceof INode);
    }

    /**
     * @return the segment along which the text of the middle of the edge is drawn: for a SegmentedLineEdge the one it uses (around
     *         its middle point), else the segment at half the length of the flattened path (the curve of a state transition, not
     *         the line between its ends)
     */
    private Line2D getMiddleSegment(IEdge edge)
    {
        Line2D connectionPoints = edge.getConnectionPoints();
        if (connectionPoints == null)
        {
            return null;
        }
        if (edge instanceof SegmentedLineEdge)
        {
            List<Point2D> points = ((SegmentedLineEdge) edge).getPoints();
            if (points.size() >= 2)
            {
                return new Line2D.Double(points.get(points.size() / 2 - 1), points.get(points.size() / 2));
            }
            return connectionPoints;
        }
        List<Point2D> points = getShapePoints(edge);
        if (points.size() < 2)
        {
            return connectionPoints;
        }
        double length = 0;
        for (int i = 1; i < points.size(); i++)
        {
            length += points.get(i - 1).distance(points.get(i));
        }
        double remaining = length / 2;
        for (int i = 1; i < points.size(); i++)
        {
            Point2D p = points.get(i - 1);
            Point2D q = points.get(i);
            double segment = p.distance(q);
            if (segment > 0 && remaining <= segment)
            {
                return new Line2D.Double(p, q);
            }
            remaining -= segment;
        }
        return new Line2D.Double(points.get(points.size() - 2), points.get(points.size() - 1));
    }

    /**
     * @return the points of the first subpath of the edge shape, curves flattened
     */
    private List<Point2D> getShapePoints(IEdge edge)
    {
        List<Point2D> points = new ArrayList<Point2D>();
        Shape shape = edge instanceof ShapeEdge ? ((ShapeEdge) edge).getShape() : null;
        if (shape == null)
        {
            return points;
        }
        double[] coords = new double[6];
        for (PathIterator iterator = shape.getPathIterator(null, 1); !iterator.isDone(); iterator.next())
        {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO && !points.isEmpty())
            {
                break; // an arrow head appended to the line
            }
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
            {
                points.add(new Point2D.Double(coords[0], coords[1]));
            }
        }
        return points;
    }

    private boolean isOnButton(MouseEvent event)
    {
        Point2D center = getButtonCenter();
        if (center == null)
        {
            return false;
        }
        return event.getPoint().distance(this.editorPart.toComponentPoint(center)) <= RADIUS + HIT_MARGIN;
    }

    private void editSelected()
    {
        List<EditSelectedBehavior> found = this.editorPart.getBehaviorManager().getBehaviors(EditSelectedBehavior.class);
        if (found.size() != 1)
        {
            return;
        }
        found.get(0).editSelected();
        repaint();
    }

    private void repaint()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    /** Radius of the button on screen, in pixels */
    private static final double RADIUS = 16;

    /** Space between a node and its button on screen, in pixels */
    private static final double GAP = 4;

    /** Extra margin around the button to catch a finger, in pixels */
    private static final double HIT_MARGIN = 4;

    private static final Color BUTTON_COLOR = new Color(123, 79, 214);

    private static final Color PENCIL_COLOR = Color.WHITE;

    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 70);

    private IEditorPart editorPart;

    private IEditorPartSelectionHandler selectionHandler;

    private IGraphToolsBar graphToolsBar;

    /** True while a gesture started on the button */
    private boolean isGestureOnButton = false;

    /** True while something else is dragged (a node, a lasso...) */
    private boolean isDragging = false;

    @ResourceBundleBean(key = "edit.button.tooltip")
    private String toolTip;

}
