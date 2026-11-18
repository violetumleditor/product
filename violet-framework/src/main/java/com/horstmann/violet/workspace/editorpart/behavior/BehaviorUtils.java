package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.ICroppableNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.IResizableNode;
import com.horstmann.violet.product.diagram.abstracts.node.ResizeDirection;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;

public class BehaviorUtils
{
    /**
     * Returns the {@link ResizeDirection} of the resize handle the mouse is
     * currently hovering over, or {@code null} if not over any resize handle.
     */
    public static ResizeDirection findResizeDirection(IEditorPart editorPart, MouseEvent event)
    {
        INode node = findScaledNode(editorPart, event);
        if (node == null || !(node instanceof IResizableNode)) return null;
        IResizableNode resizableNode = (IResizableNode) node;
        Point scaledLocation = scaledPoint(editorPart, event);
        for (Map.Entry<ResizeDirection, Rectangle2D> entry : resizableNode.getResizableDragPoints().entrySet())
        {
            if (entry.getValue().contains(scaledLocation)) return entry.getKey();
        }
        return null;
    }

    /**
     * Returns the {@link ResizeDirection} of the crop handle the mouse is
     * currently hovering over, or {@code null} if not over any crop handle.
     * Never returns a corner direction (NW/NE/SW/SE); only N/S/W/E.
     */
    public static ResizeDirection findCropDirection(IEditorPart editorPart, MouseEvent event)
    {
        INode node = findScaledNode(editorPart, event);
        if (node == null || !(node instanceof ICroppableNode)) return null;
        ICroppableNode croppableNode = (ICroppableNode) node;
        Point scaledLocation = scaledPoint(editorPart, event);
        for (Map.Entry<ResizeDirection, Rectangle2D> entry : croppableNode.getCropDragPoints().entrySet())
        {
            if (entry.getValue().contains(scaledLocation)) return entry.getKey();
        }
        return null;
    }

    /** Returns {@code true} if the mouse is over any resize handle. */
    public static boolean isCursorOnResizePoint(IEditorPart editorPart, MouseEvent event)
    {
        return findResizeDirection(editorPart, event) != null;
    }

    /** Returns {@code true} if the mouse is over any crop handle. */
    public static boolean isCursorOnCropPoint(IEditorPart editorPart, MouseEvent event)
    {
        return findCropDirection(editorPart, event) != null;
    }

    /**
     * Returns {@code true} if the node under the mouse (as found by the graph) is selected, or if the mouse is inside a selected
     * container, even upon a node it contains. With the container tool, such a press drags the selection instead of drawing a
     * new frame.
     */
    public static boolean isCursorOnSelectedNode(IEditorPart editorPart, MouseEvent event)
    {
        double zoom = editorPart.getZoomFactor();
        Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        IEditorPartSelectionHandler selectionHandler = editorPart.getSelectionHandler();
        INode node = editorPart.getGraph().findNode(mousePoint);
        if (node != null && selectionHandler.isElementAlreadySelected(node))
        {
            return true;
        }
        for (ISelectable aSelectedElement : selectionHandler.getSelectedElements())
        {
            if (aSelectedElement instanceof ContainerNode && ((ContainerNode) aSelectedElement).getBounds().contains(mousePoint))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Where to put a node created on an empty spot of the diagram without the tool bar (link handle released in the void, "+"
     * button): centered on that spot, unless its top left corner would then fall on another node, which would become its parent or
     * refuse it ({@code AbstractGraph.addNode}); then its top left corner goes on the spot, as with a node tool.
     *
     * @param graph the diagram
     * @param newNode the node to add, not on the diagram yet
     * @param point the spot, on the graph (unzoomed coordinates)
     * @return the location to give to {@code IGraph.addNode}
     */
    public static Point2D getNewNodeLocation(IGraph graph, INode newNode, Point2D point)
    {
        IGridSticker gridSticker = graph.getGridSticker();
        Rectangle2D bounds = newNode.getBounds();
        Point2D centered = gridSticker.snap(new Point2D.Double(Math.max(0, point.getX() - bounds.getWidth() / 2),
                Math.max(0, point.getY() - bounds.getHeight() / 2)));
        if (graph.findNode(centered) == graph.findNode(point))
        {
            return centered;
        }
        return point;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Point scaledPoint(IEditorPart editorPart, MouseEvent event)
    {
        double zoom = editorPart.getZoomFactor();
        Point p = new Point();
        p.setLocation(event.getX() / zoom, event.getY() / zoom);
        return p;
    }

    private static INode findScaledNode(IEditorPart editorPart, MouseEvent event)
    {
        return editorPart.getGraph().findNode(scaledPoint(editorPart, event));
    }

    /**
     * Moves a node into the node it is dropped on, last among its new siblings, keeping its location on the graph (as far as its
     * new parent allows it)
     *
     * @param graph the graph of the node
     * @param node the dropped node
     * @param newParent the node that takes it, see {@link IGraph#findNewParentOnDrop}
     */
    public static void moveIntoNewParent(IGraph graph, INode node, INode newParent)
    {
        Point2D locationOnGraph = node.getLocationOnGraph();
        graph.changeParent(node, newParent, -1);
        Point2D newParentLocationOnGraph = newParent.getLocationOnGraph();
        node.setLocation(new Point2D.Double(locationOnGraph.getX() - newParentLocationOnGraph.getX(),
                locationOnGraph.getY() - newParentLocationOnGraph.getY()));
    }

    /**
     * The transition points moved with the given nodes: those of the edges between two moved nodes, plus the selected ones
     * (e.g. caught by the lasso), each one once
     *
     * @param graph the graph of the nodes
     * @param movedNodes the nodes being moved
     * @param selection the selected elements
     * @return the points to move, in place
     */
    public static List<ITransitionPoint> getTransitionPointsToMove(IGraph graph, Collection<INode> movedNodes,
            List<ISelectable> selection)
    {
        Set<ITransitionPoint> points = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IEdge edge : graph.getAllEdges())
        {
            if (movedNodes.contains(edge.getStartNode()) && movedNodes.contains(edge.getEndNode()))
            {
                points.addAll(Arrays.asList(edge.getTransitionPoints()));
            }
        }
        for (ISelectable element : selection)
        {
            if (element instanceof ITransitionPoint)
            {
                points.add((ITransitionPoint) element);
            }
        }
        return new ArrayList<>(points);
    }

    /**
     * Outlines, in dashed violet, the node a dragged node is about to go into
     *
     * @param g2 the diagram graphics (graph coordinates)
     * @param node the target node
     */
    public static void drawDropTarget(Graphics2D g2, INode node)
    {
        Point2D locationOnGraph = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        drawDropOutline(g2, new Rectangle2D.Double(locationOnGraph.getX() - DROP_OUTLINE_GAP, locationOnGraph.getY() - DROP_OUTLINE_GAP,
                bounds.getWidth() + 2 * DROP_OUTLINE_GAP, bounds.getHeight() + 2 * DROP_OUTLINE_GAP));
    }

    /**
     * Draws a dashed violet rectangle, as the drop target
     */
    public static void drawDropOutline(Graphics2D g2, Rectangle2D outline)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(DROP_OUTLINE_COLOR);
        g2.setStroke(DROP_OUTLINE_STROKE);
        g2.draw(outline);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private static final double DROP_OUTLINE_GAP = 3;

    private static final Color DROP_OUTLINE_COLOR = new Color(0.7f, 0.4f, 0.7f);

    private static final Stroke DROP_OUTLINE_STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
            new float[] { 4, 3 }, 0);
}
