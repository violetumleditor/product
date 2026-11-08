package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Draws a round "pencil" button next to the selected element, which opens its properties dialog (as a double click does, which
 * is hard on a touch screen). Shown while the selection tool is chosen and exactly one node or edge is selected: off the top
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
     * @return the center of the button on the graph (unzoomed coordinates), or {@code null} if no button is shown
     */
    private Point2D getButtonCenter()
    {
        if (this.isDragging || !GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool()))
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
        if (element instanceof INode)
        {
            Rectangle2D bounds = ((INode) element).getBounds();
            double offset = (RADIUS + GAP) / zoom;
            double x = bounds.getMaxX() + offset;
            double y = bounds.getMinY() - offset;
            // Stays on the drawing area, even for a node at its top or right border
            double maxX = this.editorPart.getSwingComponent().getWidth() / zoom - r;
            x = Math.max(r, Math.min(x, maxX));
            y = Math.max(r, y);
            return new Point2D.Double(x, y);
        }
        if (element instanceof IEdge)
        {
            return getEdgeMiddle((IEdge) element);
        }
        return null;
    }

    /**
     * @return the point at half the length of the edge path
     */
    private Point2D getEdgeMiddle(IEdge edge)
    {
        Line2D connectionPoints = edge.getConnectionPoints();
        if (connectionPoints == null)
        {
            return null;
        }
        if (!(edge instanceof SegmentedLineEdge))
        {
            return new Point2D.Double((connectionPoints.getX1() + connectionPoints.getX2()) / 2,
                    (connectionPoints.getY1() + connectionPoints.getY2()) / 2);
        }
        List<Point2D> points = ((SegmentedLineEdge) edge).getPoints();
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
                double t = remaining / segment;
                return new Point2D.Double(p.getX() + t * (q.getX() - p.getX()), p.getY() + t * (q.getY() - p.getY()));
            }
            remaining -= segment;
        }
        return points.get(points.size() - 1);
    }

    private boolean isOnButton(MouseEvent event)
    {
        Point2D center = getButtonCenter();
        if (center == null)
        {
            return false;
        }
        double zoom = this.editorPart.getZoomFactor();
        return event.getPoint().distance(center.getX() * zoom, center.getY() * zoom) <= RADIUS + HIT_MARGIN;
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
