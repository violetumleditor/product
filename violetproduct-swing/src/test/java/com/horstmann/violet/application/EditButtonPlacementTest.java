package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateNode;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.EditButtonBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * The edit button of a selected edge must not cover the text at its middle
 */
class EditButtonPlacementTest
{

    /** Radius of the button on screen, at zoom 1 */
    private static final double RADIUS = 16;

    /** Second node location: on the right, below, diagonals both ways */
    static Stream<Point2D> secondNodeLocations()
    {
        return Stream.of(new Point2D.Double(400, 50), new Point2D.Double(50, 400), new Point2D.Double(400, 400),
                new Point2D.Double(400, -300), new Point2D.Double(-300, 50));
    }

    @ParameterizedTest
    @MethodSource("secondNodeLocations")
    void shouldStayOnTheMiddleOfAnEdgeWithoutText(Point2D location) throws Exception
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connect(graph, new ClassNode(), new ClassNode(), new GenericEdge(), location);

        Point2D center = getButtonCenter(graph, edge);

        assertNotNull(center);
        assertTrue(edge.contains(center) || edge.getShape().intersects(center.getX() - 2, center.getY() - 2, 4, 4));
    }

    @ParameterizedTest
    @MethodSource("secondNodeLocations")
    void shouldNotCoverTheMiddleLabelOfASegmentedEdge(Point2D location) throws Exception
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connect(graph, new ClassNode(), new ClassNode(), new GenericEdge(), location);
        edge.setMiddleLabel("a rather long label");

        assertButtonAvoidsLabel(graph, edge);
    }

    @ParameterizedTest
    @MethodSource("secondNodeLocations")
    void shouldNotCoverTheLabelOfAStateTransition(Point2D location) throws Exception
    {
        IGraph graph = new StateDiagramGraph();
        StateTransitionEdge edge = connect(graph, new StateNode(), new StateNode(), new StateTransitionEdge(), location);
        edge.setLabel("event [guard] / action");

        assertButtonAvoidsLabel(graph, edge);
    }

    private void assertButtonAvoidsLabel(IGraph graph, IEdge edge) throws Exception
    {
        Rectangle2D label = edge.getMiddleLabelBounds();
        assertNotNull(label);
        assertFalse(label.isEmpty());
        Point2D center = getButtonCenter(graph, edge);
        assertNotNull(center);
        Ellipse2D button = new Ellipse2D.Double(center.getX() - RADIUS, center.getY() - RADIUS, 2 * RADIUS, 2 * RADIUS);
        assertFalse(button.intersects(label), "button " + center + " covers label " + label);
        // Still next to the edge, not far away
        double distance = edge instanceof SegmentedLineEdge ? distanceToPath((SegmentedLineEdge) edge, center) : 0;
        assertTrue(distance < 4 * RADIUS + label.getHeight(), "button too far from the edge: " + distance);
    }

    private double distanceToPath(SegmentedLineEdge edge, Point2D p)
    {
        double min = Double.MAX_VALUE;
        List<Point2D> points = edge.getPoints();
        for (int i = 1; i < points.size(); i++)
        {
            min = Math.min(min, new Line2D.Double(points.get(i - 1), points.get(i)).ptSegDist(p));
        }
        return min;
    }

    private <E extends IEdge> E connect(IGraph graph, INode start,
            INode end, E edge, Point2D endLocation)
    {
        graph.addNode(start, new Point2D.Double(400, 400));
        graph.addNode(end, new Point2D.Double(400 + endLocation.getX(), 400 + endLocation.getY()));
        assertTrue(graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
        return edge;
    }

    private Point2D getButtonCenter(IGraph graph, IEdge edge) throws Exception
    {
        EditorPart editorPart = new EditorPart(graph);
        editorPart.getSelectionHandler().setSelectedElement(edge);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        EditButtonBehavior behavior = new EditButtonBehavior(editorPart, toolsBar);
        Method method = EditButtonBehavior.class.getDeclaredMethod("getButtonCenter");
        method.setAccessible(true);
        assertEquals(1.0, editorPart.getZoomFactor());
        return (Point2D) method.invoke(behavior);
    }
}
