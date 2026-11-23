package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;
import com.horstmann.violet.framework.util.GrabberUtils;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.ShapeEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * With the selection tool, highlights the element a click would select while the mouse moves over it: a violet halo along an
 * edge, a violet frame around a node. Same search as SelectByClickBehavior: the edge first, else the node. Nothing on a touch
 * screen (no mouse moves), nor on an element already selected (its handles show it), nor while a button is pressed.
 */
public class HighlightHoveredElementBehavior extends AbstractEditorPartBehavior
{

    public HighlightHoveredElementBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        BeanInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
        // No mouse exit event for the behaviors: listen to the component itself
        editorPart.getSwingComponent().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent event)
            {
                setHoveredElement(null);
            }
        });
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        setHoveredElement(findHoveredElement(event));
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        setHoveredElement(null);
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        setHoveredElement(null);
    }

    @Override
    public void onToolSelected(GraphTool selectedTool)
    {
        setHoveredElement(null);
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        if (this.hoveredElement == null || getSelectionHandler().isElementAlreadySelected(this.hoveredElement))
        {
            return;
        }
        // The element may have been removed since the last mouse move
        if (this.hoveredElement instanceof IEdge && getGraph().getAllEdges().contains(this.hoveredElement))
        {
            drawEdgeHighlight(g2, (IEdge) this.hoveredElement);
        }
        if (this.hoveredElement instanceof INode && getGraph().getAllNodes().contains(this.hoveredElement))
        {
            drawNodeHighlight(g2, (INode) this.hoveredElement);
        }
    }

    /**
     * @return the edge or the node a click at the mouse location would select, or null
     */
    private ISelectable findHoveredElement(MouseEvent event)
    {
        if (this.launchingPreferences.isTouchScreen() || !GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool()))
        {
            return null;
        }
        if (BehaviorUtils.isCursorOnResizePoint(this.editorPart, event) || BehaviorUtils.isCursorOnCropPoint(this.editorPart, event))
        {
            return null; // the handle is highlighted by ResizeNodeBehavior / CropNodeBehavior
        }
        double zoom = this.editorPart.getZoomFactor();
        Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        IEdge edge = getGraph().findEdge(mousePoint);
        if (edge != null)
        {
            return edge;
        }
        return getGraph().findNode(mousePoint);
    }

    private void setHoveredElement(ISelectable element)
    {
        if (element == this.hoveredElement)
        {
            return;
        }
        this.hoveredElement = element;
        Component component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    /**
     * Draws a wide translucent stroke along the edge (arrow heads included), and its transition points, which can be grabbed
     * right away (the one under the mouse is drawn larger by SelectByClickBehavior)
     */
    private void drawEdgeHighlight(Graphics2D g2, IEdge edge)
    {
        if (!(edge instanceof ShapeEdge))
        {
            drawHalo(g2, edge.getBounds());
            return;
        }
        Shape shape = ((ShapeEdge) edge).getShape();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(HALO_COLOR);
        g2.setStroke(EDGE_HALO_STROKE);
        g2.draw(shape);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
        double zoom = this.editorPart.getZoomFactor();
        for (ITransitionPoint aTransitionPoint : edge.getTransitionPoints())
        {
            GrabberUtils.drawTransitionPointHandle(g2, aTransitionPoint.toPoint2D(), zoom, false, false);
        }
    }

    /**
     * Draws a rounded violet frame with a soft halo around the node
     */
    private void drawNodeHighlight(Graphics2D g2, INode node)
    {
        Point2D locationOnGraph = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        drawHalo(g2, new Rectangle2D.Double(locationOnGraph.getX(), locationOnGraph.getY(), bounds.getWidth(), bounds.getHeight()));
    }

    private void drawHalo(Graphics2D g2, Rectangle2D bounds)
    {
        RoundRectangle2D frame = new RoundRectangle2D.Double(bounds.getX() - NODE_GAP, bounds.getY() - NODE_GAP,
                bounds.getWidth() + 2 * NODE_GAP, bounds.getHeight() + 2 * NODE_GAP, NODE_ARC, NODE_ARC);
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(HALO_COLOR);
        g2.setStroke(NODE_HALO_STROKE);
        g2.draw(frame);
        g2.setColor(FRAME_COLOR);
        g2.setStroke(NODE_FRAME_STROKE);
        g2.draw(frame);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private IGraph getGraph()
    {
        return this.editorPart.getGraph();
    }

    private IEditorPartSelectionHandler getSelectionHandler()
    {
        return this.editorPart.getSelectionHandler();
    }

    private IEditorPart editorPart;

    private IGraphToolsBar graphToolsBar;

    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    /** The element under the mouse, highlighted, or null */
    private ISelectable hoveredElement;

    /** Gap between a node and its frame */
    private static final double NODE_GAP = 3;

    private static final double NODE_ARC = 10;

    /** Violet of the selection handles (GrabberUtils), translucent */
    private static final Color HALO_COLOR = new Color(179, 102, 179, 70);

    private static final Color FRAME_COLOR = new Color(179, 102, 179, 200);

    private static final Stroke EDGE_HALO_STROKE = new BasicStroke(9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final Stroke NODE_HALO_STROKE = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final Stroke NODE_FRAME_STROKE = new BasicStroke(1.5f);
}
