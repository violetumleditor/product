package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.MovedSegments;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentRoute;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.ActivityNode;
import com.horstmann.violet.product.diagram.activity.ActivityTransitionEdge;
import com.horstmann.violet.product.diagram.activity.SynchronizationBarNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.ProcessGatewayNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessSequenceFlowEdge;
import com.horstmann.violet.product.diagram.bpmn.ProcessTaskNode;

/**
 * While something is dragged, the editor draws once the elements that {@link IGraph#getChangingElements} leaves out, and keeps
 * that image until the release: those elements must stay exactly the same whatever the user does to the dragged ones. Each
 * node of each diagram is moved, each edge gets a moved segment and a moved text, and the elements left out are drawn before
 * and after, pixel for pixel.
 */
class LayeredDrawingTest
{
    static Stream<Arguments> diagrams() throws IOException
    {
        List<Arguments> diagrams = new ArrayList<Arguments>();
        for (String aSample : new String[] { "sample.class.violet.html", "sample.activity.violet.html", "sample.state.violet.html",
                "sample.object.violet.html", "sample.ucase.violet.html" })
        {
            diagrams.add(Arguments.of(aSample, RenderingOptimizationTest.readSampleGraph(aSample)));
        }
        diagrams.add(Arguments.of("generated class diagram", RenderingOptimizationTest.createClassDiagram(4, 3, true)));
        diagrams.add(Arguments.of("generated activity diagram", RenderingOptimizationTest.createActivityDiagram(4)));
        diagrams.add(Arguments.of("activity diagram with synchronization bars", createActivityDiagramWithBars()));
        diagrams.add(Arguments.of("BPMN diagram", createProcessDiagram()));
        return diagrams.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldLeaveTheOtherNodesUnchangedWhenANodeMoves(String name, IGraph graph)
    {
        List<INode> nodes = new ArrayList<INode>(graph.getAllNodes());
        for (INode aNode : nodes.subList(0, Math.min(nodes.size(), 30)))
        {
            Predicate<Object> changing = graph.getChangingElements(Collections.singletonList(aNode), Collections.<IEdge> emptyList());
            assertNotNull(changing, name);
            Point2D location = aNode.getLocation();
            assertUnchangedWhile(name + ", " + aNode.getClass().getSimpleName() + " moved", graph, changing,
                    () -> aNode.setLocation(new Point2D.Double(location.getX() + 37, location.getY() + 23)),
                    () -> aNode.setLocation(location));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldLeaveTheOtherElementsUnchangedWhenAnEdgeChanges(String name, IGraph graph)
    {
        for (IEdge anEdge : new ArrayList<IEdge>(graph.getAllEdges()))
        {
            if (!(anEdge instanceof SegmentedLineEdge))
            {
                continue;
            }
            SegmentedLineEdge edge = (SegmentedLineEdge) anEdge;
            Predicate<Object> changing = graph.getChangingElements(Collections.<INode> emptyList(), Collections.singletonList(anEdge));
            assertNotNull(changing, name);
            Point2D.Double offset = edge.getLabelOffset(SegmentedLineEdge.MIDDLE_LABEL);
            assertUnchangedWhile(name + ", text of " + edge.getClass().getSimpleName() + " moved", graph, changing,
                    () -> edge.setLabelOffset(SegmentedLineEdge.MIDDLE_LABEL, new Point2D.Double(15, 25)),
                    () -> edge.setLabelOffset(SegmentedLineEdge.MIDDLE_LABEL, offset));
            SegmentRoute route = edge.isSegmentsMovable() ? edge.getRoute() : null;
            if (route != null && route.getCoordinates().size() > 0)
            {
                MovedSegments movedSegments = edge.getMovedSegments();
                assertUnchangedWhile(name + ", segment of " + edge.getClass().getSimpleName() + " moved", graph, changing,
                        () -> edge.setRoute(route.move(0, route.getCoordinate(0) + 20)),
                        () -> edge.setMovedSegments(movedSegments));
            }
        }
    }

    private static void assertUnchangedWhile(String name, IGraph graph, Predicate<Object> changing, Runnable change, Runnable undo)
    {
        Rectangle2D bounds = graph.getClipBounds();
        BufferedImage before = drawUnchanged(graph, changing, bounds);
        change.run();
        try
        {
            BufferedImage after = drawUnchanged(graph, changing, bounds);
            int differences = 0;
            for (int x = 0; x < before.getWidth(); x++)
            {
                for (int y = 0; y < before.getHeight(); y++)
                {
                    if (before.getRGB(x, y) != after.getRGB(x, y))
                    {
                        differences++;
                    }
                }
            }
            assertEquals(0, differences, name + ": pixels differing among the elements drawn once");
        }
        finally
        {
            undo.run();
        }
    }

    /**
     * Draws the whole graph once (as the screen does before), then only the elements that don't change
     */
    private static BufferedImage drawUnchanged(IGraph graph, Predicate<Object> changing, Rectangle2D bounds)
    {
        int width = (int) Math.ceil(bounds.getWidth()) + 200;
        int height = (int) Math.ceil(bounds.getHeight()) + 200;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.translate(100 - bounds.getX(), 100 - bounds.getY());
        graph.draw(g2);
        g2.dispose();
        g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.translate(100 - bounds.getX(), 100 - bounds.getY());
        g2.setColor(Color.BLACK);
        graph.draw(g2, changing.negate());
        g2.dispose();
        return image;
    }

    /**
     * A fork bar after a first activity, three branches, a join bar linked to the fork bar too, then a last activity
     */
    static IGraph createActivityDiagramWithBars()
    {
        ActivityDiagramGraph graph = new ActivityDiagramGraph();
        ActivityNode first = addActivity(graph, "First", 240, 20);
        SynchronizationBarNode fork = new SynchronizationBarNode();
        assertTrue(graph.addNode(fork, new Point2D.Double(200, 120)));
        SynchronizationBarNode join = new SynchronizationBarNode();
        assertTrue(graph.addNode(join, new Point2D.Double(200, 360)));
        connect(graph, first, fork);
        for (int i = 0; i < 3; i++)
        {
            ActivityNode branch = addActivity(graph, "Branch " + i, 60 + i * 200, 220);
            connect(graph, fork, branch);
            connect(graph, branch, join);
        }
        connect(graph, fork, join);
        connect(graph, join, addActivity(graph, "Last", 240, 460));
        return graph;
    }

    private static ActivityNode addActivity(IGraph graph, String name, double x, double y)
    {
        ActivityNode activity = new ActivityNode();
        activity.getName().setText(name);
        assertTrue(graph.addNode(activity, new Point2D.Double(x, y)));
        return activity;
    }

    private static void connect(IGraph graph, INode start, INode end)
    {
        graph.connect(new ActivityTransitionEdge(), start, start.getLocationOnGraph(), end, end.getLocationOnGraph());
    }

    /**
     * Tasks, a gateway with two branches joining a last task
     */
    static IGraph createProcessDiagram()
    {
        ProcessDiagramGraph graph = new ProcessDiagramGraph();
        ProcessTaskNode first = addTask(graph, "Receive", 40, 100);
        ProcessGatewayNode gateway = new ProcessGatewayNode();
        assertTrue(graph.addNode(gateway, new Point2D.Double(240, 110)));
        ProcessTaskNode yes = addTask(graph, "Accept", 380, 20);
        ProcessTaskNode no = addTask(graph, "Refuse", 380, 200);
        ProcessTaskNode last = addTask(graph, "Notify", 600, 100);
        for (INode[] aLink : new INode[][] { { first, gateway }, { gateway, yes }, { gateway, no }, { yes, last }, { no, last } })
        {
            assertTrue(graph.connect(new ProcessSequenceFlowEdge(), aLink[0], aLink[0].getLocationOnGraph(), aLink[1],
                    aLink[1].getLocationOnGraph()));
        }
        return graph;
    }

    private static ProcessTaskNode addTask(IGraph graph, String name, double x, double y)
    {
        ProcessTaskNode task = new ProcessTaskNode();
        task.getName().setText(name);
        assertTrue(graph.addNode(task, new Point2D.Double(x, y)));
        return task;
    }
}
