package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.GeometryCache;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.ActivityNode;
import com.horstmann.violet.product.diagram.activity.ActivityTransitionEdge;
import com.horstmann.violet.product.diagram.activity.DecisionNode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.edges.AggregationEdge;
import com.horstmann.violet.product.diagram.classes.edges.AssociationEdge;
import com.horstmann.violet.product.diagram.classes.edges.InheritanceEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.ReturnEdge;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;

/**
 * The optimizations of the drawing (geometry computed once per pass, elements out of the painted part skipped) must change
 * nothing on screen nor for the mouse: every diagram is drawn and searched with and without them, pixel by pixel. Also prints
 * the time saved on large diagrams.
 */
class RenderingOptimizationTest
{
    private static final String[] SAMPLES = { "sample.class.violet.html", "sample.activity.violet.html", "sample.seq.violet.html",
            "sample.state.violet.html", "sample.object.violet.html", "sample.ucase.violet.html" };

    @AfterEach
    void enableAgain()
    {
        GeometryCache.setEnabled(true);
    }

    static Stream<Arguments> diagrams() throws IOException
    {
        List<Arguments> diagrams = new ArrayList<Arguments>();
        for (String aSample : SAMPLES)
        {
            diagrams.add(Arguments.of(aSample, readSample(aSample)));
        }
        diagrams.add(Arguments.of("large class diagram", createClassDiagram(6, 4, true)));
        diagrams.add(Arguments.of("large sequence diagram", createSequenceDiagram(4, 3)));
        diagrams.add(Arguments.of("large activity diagram", createActivityDiagram(8)));
        return diagrams.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldDrawTheSameImage(String name, IGraph graph)
    {
        BufferedImage expected = render(graph, false, null);
        BufferedImage actual = render(graph, true, null);
        assertSameImage(name, expected, actual, null);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldDrawTheSamePaintedPart(String name, IGraph graph)
    {
        BufferedImage expected = render(graph, false, null);
        Rectangle paintedPart = new Rectangle(expected.getWidth() / 4, expected.getHeight() / 4, expected.getWidth() / 3,
                expected.getHeight() / 3);
        BufferedImage actual = render(graph, true, paintedPart);
        assertSameImage(name, expected, actual, paintedPart);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldFindTheSameElements(String name, IGraph graph)
    {
        render(graph, false, null);
        Rectangle2D bounds = graph.getClipBounds();
        for (double x = bounds.getMinX() - 10; x <= bounds.getMaxX() + 10; x += 13)
        {
            for (double y = bounds.getMinY() - 10; y <= bounds.getMaxY() + 10; y += 13)
            {
                Point2D point = new Point2D.Double(x, y);
                GeometryCache.setEnabled(false);
                IEdge expectedEdge = graph.findEdge(point);
                INode expectedNode = graph.findNode(point);
                GeometryCache.setEnabled(true);
                assertSame(expectedEdge, graph.findEdge(point), name + ": edge at " + point);
                assertSame(expectedNode, graph.findNode(point), name + ": node at " + point);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("diagrams")
    void shouldMeasureTheSameBounds(String name, IGraph graph)
    {
        render(graph, false, null);
        GeometryCache.setEnabled(false);
        Rectangle2D expected = graph.getClipBounds();
        GeometryCache.setEnabled(true);
        assertEquals(expected, graph.getClipBounds(), name);
    }

    /**
     * Not a check: prints how long a paint of the visible part and a mouse move take on large diagrams, without and with the
     * optimizations
     */
    @Test
    void printTimes()
    {
        printTimes("class diagram, 150 classes, ~300 links", createClassDiagram(15, 10, true));
        printTimes("sequence diagram, 10 x 6 lifelines", createSequenceDiagram(6, 10));
        printTimes("activity diagram, 40 steps", createActivityDiagram(40));
    }

    private static void printTimes(String name, IGraph graph)
    {
        Rectangle visiblePart = new Rectangle(0, 0, 1400, 900);
        double[] paint = new double[2];
        double[] mouse = new double[2];
        for (int i = 0; i < 2; i++)
        {
            boolean isOptimized = i == 1;
            render(graph, isOptimized, visiblePart); // warm up
            paint[i] = time(() -> render(graph, isOptimized, visiblePart), 5);
            GeometryCache.setEnabled(isOptimized);
            Rectangle2D bounds = graph.getClipBounds();
            mouse[i] = time(() -> {
                Point2D point = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
                graph.findEdge(point);
                graph.findNode(point);
            }, 5);
        }
        GeometryCache.setEnabled(true);
        System.out.println(String.format(Locale.ROOT,
                "[perf] %s: paint %.1f ms -> %.1f ms (x%.0f), mouse move %.1f ms -> %.1f ms (x%.0f)", name, paint[0],
                paint[1], paint[0] / paint[1], mouse[0], mouse[1], mouse[0] / mouse[1]));
    }

    private static double time(Runnable action, int count)
    {
        long start = System.nanoTime();
        for (int i = 0; i < count; i++)
        {
            action.run();
        }
        return (System.nanoTime() - start) / 1e6 / count;
    }

    /**
     * Draws the graph twice (the first paint settles what some nodes remember while drawn, as on screen), the second one in
     * the image, its elements translated into it
     */
    private static BufferedImage render(IGraph graph, boolean isOptimized, Rectangle paintedPart)
    {
        GeometryCache.setEnabled(isOptimized);
        try
        {
            Rectangle2D bounds = graph.getClipBounds();
            int width = (int) Math.min(5000, Math.ceil(bounds.getWidth()) + 40);
            int height = (int) Math.min(5000, Math.ceil(bounds.getHeight()) + 40);
            BufferedImage image = null;
            for (int i = 0; i < 2; i++)
            {
                image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, width, height);
                if (paintedPart != null)
                {
                    g2.setClip(paintedPart);
                }
                g2.translate(20 - bounds.getX(), 20 - bounds.getY());
                g2.setColor(Color.BLACK);
                graph.draw(g2);
                g2.dispose();
            }
            return image;
        }
        finally
        {
            GeometryCache.setEnabled(true);
        }
    }

    private static void assertSameImage(String name, BufferedImage expected, BufferedImage actual, Rectangle area)
    {
        assertEquals(expected.getWidth(), actual.getWidth(), name);
        assertEquals(expected.getHeight(), actual.getHeight(), name);
        Rectangle compared = area != null ? area : new Rectangle(0, 0, expected.getWidth(), expected.getHeight());
        int differences = 0;
        String first = null;
        for (int x = compared.x; x < compared.x + compared.width; x++)
        {
            for (int y = compared.y; y < compared.y + compared.height; y++)
            {
                if (expected.getRGB(x, y) != actual.getRGB(x, y))
                {
                    differences++;
                    if (first == null)
                    {
                        first = x + "," + y;
                    }
                }
            }
        }
        assertEquals(0, differences, name + ": pixels differing, first at " + first);
    }

    private static IGraph readSample(String resourceName) throws IOException
    {
        try (InputStream inputStream = RenderingOptimizationTest.class.getClassLoader().getResourceAsStream(resourceName))
        {
            assertNotNull(inputStream, resourceName);
            return new XHTMLPersistenceService().read(inputStream);
        }
    }

    private static MultiLineString text(String value)
    {
        MultiLineString text = new MultiLineString();
        text.setText(value);
        return text;
    }

    /**
     * A grid of classes, each linked to its right and lower neighbors, and to one farther away; some generic links with texts,
     * a few of them with segments moved by the user
     */
    static IGraph createClassDiagram(int columns, int rows, boolean withAdaptedLinks)
    {
        ClassDiagramGraph graph = new ClassDiagramGraph();
        ClassNode[][] classes = new ClassNode[columns][rows];
        for (int i = 0; i < columns; i++)
        {
            for (int j = 0; j < rows; j++)
            {
                ClassNode aClass = new ClassNode();
                aClass.setName(text("Class" + i + "_" + j));
                aClass.setAttributes(text("id : int\nname : String"));
                aClass.setMethods(text("getName() : String"));
                assertTrue(graph.addNode(aClass, new Point2D.Double(40 + i * 260, 40 + j * 220)));
                classes[i][j] = aClass;
            }
        }
        int count = 0;
        for (int i = 0; i < columns; i++)
        {
            for (int j = 0; j < rows; j++)
            {
                if (i + 1 < columns)
                {
                    IEdge edge = count % 3 == 0 ? new AssociationEdge() : count % 3 == 1 ? new AggregationEdge() : new InheritanceEdge();
                    connect(graph, edge, classes[i][j], classes[i + 1][j]);
                }
                if (j + 1 < rows)
                {
                    GenericEdge edge = new GenericEdge();
                    edge.setMiddleLabel("link " + count);
                    connect(graph, edge, classes[i][j], classes[i][j + 1]);
                    if (withAdaptedLinks && count % 7 == 0 && edge.getRoute() != null)
                    {
                        edge.setRoute(edge.getRoute().move(0, edge.getPoints().get(0).getX() + 30));
                    }
                }
                if (i + 2 < columns && j + 1 < rows && count % 4 == 0)
                {
                    connect(graph, new AssociationEdge(), classes[i][j], classes[i + 2][j + 1]);
                }
                count++;
            }
        }
        return graph;
    }

    /**
     * Blocks of calls going from the first lifeline to the last one and back
     */
    static IGraph createSequenceDiagram(int lifelineCount, int blockCount)
    {
        SequenceDiagramGraph graph = new SequenceDiagramGraph();
        LifelineNode[] lifelines = new LifelineNode[lifelineCount];
        for (int i = 0; i < lifelineCount; i++)
        {
            lifelines[i] = new LifelineNode();
            assertTrue(graph.addNode(lifelines[i], new Point2D.Double(40 + i * 180, 0)));
        }
        for (int k = 0; k < blockCount; k++)
        {
            double y = 100 + k * 60 * lifelineCount;
            ActivationBarNode caller = new ActivationBarNode();
            assertTrue(lifelines[0].addChild(caller, new Point2D.Double(0, y)));
            for (int i = 1; i < lifelineCount; i++)
            {
                ActivationBarNode callee = new ActivationBarNode();
                assertTrue(lifelines[i].addChild(callee, new Point2D.Double(0, y + i * 20)));
                connect(graph, new CallEdge(), caller, callee);
                connect(graph, new ReturnEdge(), callee, caller);
                caller = callee;
            }
        }
        return graph;
    }

    /**
     * A column of activities, each followed by a decision whose branches join the next activity
     */
    static IGraph createActivityDiagram(int stepCount)
    {
        ActivityDiagramGraph graph = new ActivityDiagramGraph();
        ActivityNode previous = null;
        for (int k = 0; k < stepCount; k++)
        {
            ActivityNode activity = new ActivityNode();
            activity.setName(text("Step " + k));
            assertTrue(graph.addNode(activity, new Point2D.Double(200, 40 + k * 260)));
            if (previous != null)
            {
                DecisionNode decision = new DecisionNode();
                assertTrue(graph.addNode(decision, new Point2D.Double(220, k * 260 - 90)));
                connect(graph, new ActivityTransitionEdge(), previous, decision);
                ActivityNode side = new ActivityNode();
                side.setName(text("Side " + k));
                assertTrue(graph.addNode(side, new Point2D.Double(450, k * 260 - 100)));
                connect(graph, new ActivityTransitionEdge(), decision, side);
                connect(graph, new ActivityTransitionEdge(), decision, activity);
                connect(graph, new ActivityTransitionEdge(), side, activity);
            }
            previous = activity;
        }
        return graph;
    }

    private static void connect(IGraph graph, IEdge edge, INode start, INode end)
    {
        assertTrue(graph.connect(edge, start, start.getLocationOnGraph(), end, end.getLocationOnGraph()),
                edge.getClass().getSimpleName() + " " + start + " -> " + end);
    }
}
