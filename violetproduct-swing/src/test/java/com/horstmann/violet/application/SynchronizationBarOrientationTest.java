package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.ActivityNode;
import com.horstmann.violet.product.diagram.activity.ActivityTransitionEdge;
import com.horstmann.violet.product.diagram.activity.SynchronizationBarNode;

/**
 * A synchronization bar is horizontal or vertical, each with its own tool: a vertical bar spans its connected nodes from top
 * to bottom and its transitions reach its left or right side, horizontally.
 */
class SynchronizationBarOrientationTest
{

    private ActivityDiagramGraph graph;

    @BeforeEach
    void createDiagram()
    {
        this.graph = new ActivityDiagramGraph();
    }

    @Test
    void shouldOfferAHorizontalAndAVerticalTool()
    {
        List<SynchronizationBarNode> bars = this.graph.getNodePrototypes().stream()
                .filter(SynchronizationBarNode.class::isInstance).map(SynchronizationBarNode.class::cast).toList();
        assertEquals(2, bars.size());
        assertFalse(bars.get(0).isVertical());
        assertTrue(bars.get(1).isVertical());
        assertTrue(bars.get(1).clone().isVertical());
    }

    @Test
    void shouldSpanConnectedNodesVerticallyAndBeReachedBySides()
    {
        SynchronizationBarNode bar = new SynchronizationBarNode(true);
        assertTrue(this.graph.addNode(bar, new Point2D.Double(300, 0)));
        ActivityNode left = addActivity(50, 40);
        ActivityNode rightTop = addActivity(450, 20);
        ActivityNode rightBottom = addActivity(450, 160);
        ActivityTransitionEdge in = connect(left, bar);
        ActivityTransitionEdge out1 = connect(bar, rightTop);
        ActivityTransitionEdge out2 = connect(bar, rightBottom);

        Rectangle2D b = bar.getBounds();
        assertTrue(b.getHeight() > b.getWidth(), "vertical: " + b);
        assertEquals(300, b.getX(), 0.5);
        assertTrue(b.getMinY() < 20 && b.getMaxY() > rightBottom.getBounds().getMaxY(), "spans the nodes: " + b);

        // Incoming from the left: right side of the activity to the left side of the bar, horizontally
        List<Point2D> points = in.getPoints();
        Point2D first = points.get(0);
        Point2D last = points.get(points.size() - 1);
        assertEquals(left.getBounds().getMaxX(), first.getX(), 2);
        assertEquals(b.getMinX(), last.getX(), 2);
        assertEquals(first.getY(), last.getY(), 0.5);
        assertTrue(b.getMinY() <= last.getY() && last.getY() <= b.getMaxY());

        // Outgoing to the right: right side of the bar to the left side of each activity, horizontally
        for (ActivityTransitionEdge out : List.of(out1, out2))
        {
            points = out.getPoints();
            first = points.get(0);
            last = points.get(points.size() - 1);
            assertEquals(b.getMaxX(), first.getX(), 2);
            assertEquals(out.getEndNode().getBounds().getMinX(), last.getX(), 2);
            assertEquals(first.getY(), last.getY(), 0.5);
        }
    }

    @Test
    void shouldKeepHorizontalBarsReachedByTopAndBottom()
    {
        SynchronizationBarNode bar = new SynchronizationBarNode();
        assertTrue(this.graph.addNode(bar, new Point2D.Double(0, 200)));
        ActivityNode above = addActivity(100, 50);
        ActivityTransitionEdge in = connect(above, bar);
        Rectangle2D b = bar.getBounds();
        assertTrue(b.getWidth() > b.getHeight(), "horizontal: " + b);
        List<Point2D> points = in.getPoints();
        Point2D first = points.get(0);
        Point2D last = points.get(points.size() - 1);
        assertEquals(above.getBounds().getMaxY(), first.getY(), 2);
        assertEquals(b.getMinY(), last.getY(), 2);
        assertEquals(first.getX(), last.getX(), 0.5);
    }

    @Test
    void shouldConnectTwoBarsWithoutEndlessRecursion()
    {
        SynchronizationBarNode fork = new SynchronizationBarNode();
        assertTrue(this.graph.addNode(fork, new Point2D.Double(0, 100)));
        SynchronizationBarNode join = new SynchronizationBarNode();
        assertTrue(this.graph.addNode(join, new Point2D.Double(0, 300)));
        addActivity(50, 0);
        connect(this.graph.getAllNodes().stream().filter(ActivityNode.class::isInstance).findFirst().get(), fork);
        ActivityTransitionEdge between = connect(fork, join);
        List<Point2D> points = between.getPoints();
        assertEquals(points.get(0).getX(), points.get(points.size() - 1).getX(), 0.5);
        assertTrue(join.getBounds().getWidth() > 0);
    }

    @Test
    void shouldSaveTheOrientationOfVerticalBarsOnly() throws IOException
    {
        assertTrue(this.graph.addNode(new SynchronizationBarNode(), new Point2D.Double(10, 10)));
        assertTrue(this.graph.addNode(new SynchronizationBarNode(true), new Point2D.Double(200, 10)));
        XMLPersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        String xml = out.toString(StandardCharsets.UTF_8);
        assertEquals(1, xml.split("<vertical>", -1).length - 1, xml);

        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        long verticalCount = reloaded.getAllNodes().stream().filter(SynchronizationBarNode.class::isInstance)
                .map(SynchronizationBarNode.class::cast).filter(SynchronizationBarNode::isVertical).count();
        long barCount = reloaded.getAllNodes().stream().filter(SynchronizationBarNode.class::isInstance).count();
        assertEquals(2, barCount);
        assertEquals(1, verticalCount);
    }

    private ActivityNode addActivity(double x, double y)
    {
        ActivityNode activity = new ActivityNode();
        assertTrue(this.graph.addNode(activity, new Point2D.Double(x, y)));
        return activity;
    }

    private ActivityTransitionEdge connect(INode start, INode end)
    {
        ActivityTransitionEdge edge = new ActivityTransitionEdge();
        Rectangle2D s = start.getBounds();
        Rectangle2D e = end.getBounds();
        assertTrue(this.graph.connect(edge, start, new Point2D.Double(s.getCenterX(), s.getCenterY()), end,
                new Point2D.Double(e.getCenterX(), e.getCenterY())));
        return edge;
    }
}
