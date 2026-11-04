package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.edges.DependencyEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateNode;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

class EdgeLabelColorTest
{
    private static final Color PALETTE_RED = new Color(246, 132, 98);
    private static final Color DEFAULT = ColorChoice.DEFAULT_TEXT_COLOR;

    @Test
    void shouldDrawSegmentedEdgeLabelsWithTheEdgeTextColor()
    {
        DependencyEdge edge = new DependencyEdge();
        edge.setMiddleLabel("HHHHHHHH");
        edge.setTextColor(PALETTE_RED);
        connect(new ClassDiagramGraph(), edge, new ClassNode(), new ClassNode());

        assertTrue(containsPixel(draw(edge), PALETTE_RED));
    }

    @Test
    void shouldDrawStateTransitionLabelsWithTheDefaultTextColor()
    {
        StateTransitionEdge edge = new StateTransitionEdge();
        edge.setLabel("HHHHHHHH");
        connect(new StateDiagramGraph(), edge, new StateNode(), new StateNode());

        assertTrue(containsPixel(draw(edge), DEFAULT));
    }

    @Test
    void shouldColorizeStateTransitions()
    {
        StateTransitionEdge edge = new StateTransitionEdge();
        edge.setLabel("HHHHHHHH");
        edge.setTextColor(PALETTE_RED);
        edge.setBorderColor(Color.BLUE);
        connect(new StateDiagramGraph(), edge, new StateNode(), new StateNode());

        BufferedImage image = draw(edge);

        assertTrue(containsPixel(image, PALETTE_RED));
        assertTrue(containsPixel(image, Color.BLUE));
    }

    @Test
    void shouldDrawNodeTextWithTheNodeTextColor()
    {
        ClassNode node = new ClassNode();
        node.getName().setText("HHHHHHHH");
        node.setTextColor(PALETTE_RED);
        new ClassDiagramGraph().addNode(node, new Point2D.Double(20, 30));

        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        node.draw(g2);
        g2.dispose();

        assertTrue(containsPixel(image, PALETTE_RED));
    }

    @Test
    void shouldKeepStateTransitionColorsOutOfThePropertySheet() throws IntrospectionException
    {
        PropertyDescriptor[] descriptors = Introspector.getBeanInfo(StateTransitionEdge.class).getPropertyDescriptors();

        assertEquals(1, descriptors.length);
        assertEquals("label", descriptors[0].getName());
    }

    @Test
    void shouldSaveAndReloadStateTransitionColors() throws IOException
    {
        StateTransitionEdge edge = new StateTransitionEdge();
        edge.setTextColor(PALETTE_RED);
        edge.setBorderColor(Color.BLUE);
        IGraph graph = new StateDiagramGraph();
        connect(graph, edge, new StateNode(), new StateNode());

        StateTransitionEdge reloaded = (StateTransitionEdge) saveAndReload(graph).getAllEdges().iterator().next();

        assertEquals(PALETTE_RED, reloaded.getTextColor());
        assertEquals(Color.BLUE, reloaded.getBorderColor());
    }

    private static IGraph saveAndReload(IGraph graph) throws IOException
    {
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        return persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
    }

    private static void connect(IGraph graph, IEdge edge, INode start, INode end)
    {
        Point2D startLocation = new Point2D.Double(20, 100);
        Point2D endLocation = new Point2D.Double(380, 100);
        graph.addNode(start, startLocation);
        graph.addNode(end, endLocation);
        assertTrue(graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
    }

    private static BufferedImage draw(IEdge edge)
    {
        BufferedImage image = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setColor(Color.BLACK);
        edge.draw(g2);
        g2.dispose();
        return image;
    }

    private static boolean containsPixel(BufferedImage image, Color color)
    {
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                if (image.getRGB(x, y) == color.getRGB())
                {
                    return true;
                }
            }
        }
        return false;
    }
}
