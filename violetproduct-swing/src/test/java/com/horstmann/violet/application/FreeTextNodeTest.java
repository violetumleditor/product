package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.edges.DependencyEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.NoteEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.object.FieldNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;

class FreeTextNodeTest
{
    private static final Color TEXT_COLOR = new Color(205, 70, 29);

    @Test
    void shouldBeAvailableInAllDiagrams()
    {
        IGraph[] graphs = { new ActivityDiagramGraph(), new ClassDiagramGraph(), new ObjectDiagramGraph(),
                new SequenceDiagramGraph(), new StateDiagramGraph(), new UseCaseDiagramGraph() };
        for (IGraph aGraph : graphs)
        {
            FreeTextNode prototype = findPrototype(aGraph);
            assertNotNull(prototype, aGraph.getClass().getSimpleName());
            assertFalse(prototype.getToolTip().isEmpty());
        }
    }

    @Test
    void shouldDrawAnIconInTheToolBarButItsTextOnceAdded()
    {
        FreeTextNode prototype = findPrototype(new ClassDiagramGraph());
        Rectangle2D iconBounds = prototype.getBounds();
        assertEquals(iconBounds.getWidth(), iconBounds.getHeight());
        BufferedImage icon = whiteImage();
        Graphics2D g2 = icon.createGraphics();
        prototype.draw(g2);
        g2.dispose();
        assertTrue(hasPixelOtherThanWhite(icon));

        FreeTextNode added = (FreeTextNode) prototype.clone();
        new ClassDiagramGraph().addNode(added, new Point2D.Double(10, 10));

        assertTrue(added.getBounds().getWidth() > added.getBounds().getHeight());
    }

    @Test
    void shouldHaveAVisibleDefaultText()
    {
        FreeTextNode node = new FreeTextNode();
        new ClassDiagramGraph().addNode(node, new Point2D.Double(10, 10));

        assertFalse(node.getText().getText().isEmpty());
        assertTrue(hasPixelOtherThanWhite(draw(node.getGraph())));
    }

    @Test
    void shouldDrawNeitherBorderNorBackground()
    {
        FreeTextNode node = new FreeTextNode();
        node.setBackgroundColor(Color.RED);
        node.setBorderColor(Color.GREEN);
        node.getText().setText("<html><font color=\"#cd461d\">HHHHHHHH</font></html>");
        new ClassDiagramGraph().addNode(node, new Point2D.Double(10, 10));

        BufferedImage image = draw(node.getGraph());

        assertFalse(containsPixel(image, Color.RED));
        assertFalse(containsPixel(image, Color.GREEN));
        assertTrue(containsPixel(image, TEXT_COLOR));
    }

    @Test
    void shouldOnlyExposeItsTextInThePropertySheet() throws IntrospectionException
    {
        List<String> names = new ArrayList<String>();
        for (PropertyDescriptor aDescriptor : Introspector.getBeanInfo(FreeTextNode.class).getPropertyDescriptors())
        {
            names.add(aDescriptor.getName());
        }
        assertEquals(List.of("text"), names);
    }

    @Test
    void shouldBeDrawnUponOtherNodes()
    {
        IGraph graph = new ClassDiagramGraph();
        FreeTextNode text = new FreeTextNode();
        text.getText().setText("<html><font color=\"#cd461d\">HHHHHHHH</font></html>");
        // Added first so that the drawing order can not come from the insertion order
        graph.addNode(text, new Point2D.Double(25, 25));
        ClassNode classNode = new ClassNode();
        classNode.setBackgroundColor(Color.BLUE);
        classNode.setBorderColor(Color.BLUE);
        classNode.getName().setText("<html><br><br><br><br><br><br>HHHHHHHHHHHHHHHHHHHHHHHHHHHHH</html>");
        graph.addNode(classNode, new Point2D.Double(20, 20));

        assertTrue(containsPixel(draw(graph), TEXT_COLOR));
    }

    @Test
    void shouldBeDrawnUponNotes()
    {
        assertTrue(new FreeTextNode().getZ() > new NoteNode().getZ());
    }

    @Test
    void shouldBeFoundBeforeTheNodesBelow()
    {
        IGraph graph = new ClassDiagramGraph();
        ClassNode classNode = new ClassNode();
        graph.addNode(classNode, new Point2D.Double(20, 20));
        FreeTextNode text = new FreeTextNode();
        graph.addNode(text, new Point2D.Double(25, 25));

        assertSame(text, graph.findNode(new Point2D.Double(30, 30)));
    }

    @Test
    void shouldStayAttachedToTheGraphWhenAddedOnANode()
    {
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(new ClassNode(), new Point2D.Double(20, 20));
        FreeTextNode text = new FreeTextNode();

        assertTrue(graph.addNode(text, new Point2D.Double(25, 25)));

        assertNull(text.getParent());
    }

    @Test
    void shouldNeverBeAParent()
    {
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(new FreeTextNode(), new Point2D.Double(20, 20));
        ClassNode classNode = new ClassNode();

        assertTrue(graph.addNode(classNode, new Point2D.Double(25, 25)));

        assertNull(classNode.getParent());
    }

    @Test
    void shouldNotAcceptNodesRequiringAParent()
    {
        IGraph sequenceGraph = new SequenceDiagramGraph();
        sequenceGraph.addNode(new FreeTextNode(), new Point2D.Double(20, 20));
        assertFalse(sequenceGraph.addNode(new ActivationBarNode(), new Point2D.Double(25, 25)));

        IGraph objectGraph = new ObjectDiagramGraph();
        objectGraph.addNode(new FreeTextNode(), new Point2D.Double(20, 20));
        assertFalse(objectGraph.addNode(new FieldNode(), new Point2D.Double(25, 25)));
    }

    @Test
    void shouldNeverBeConnected()
    {
        IGraph graph = new ClassDiagramGraph();
        FreeTextNode text = new FreeTextNode();
        ClassNode classNode = new ClassNode();
        NoteNode note = new NoteNode();
        graph.addNode(text, new Point2D.Double(20, 20));
        graph.addNode(classNode, new Point2D.Double(200, 20));
        graph.addNode(note, new Point2D.Double(400, 20));
        Point2D anyPoint = new Point2D.Double();

        assertFalse(graph.connect(new DependencyEdge(), text, anyPoint, classNode, anyPoint));
        assertFalse(graph.connect(new DependencyEdge(), classNode, anyPoint, text, anyPoint));
        assertFalse(graph.connect(new NoteEdge(), note, anyPoint, text, anyPoint));
    }

    @Test
    void shouldSaveAndReloadItsText() throws IOException
    {
        FreeTextNode text = new FreeTextNode();
        text.getText().setText("<html><font color=\"#cd461d\">hello</font></html>");
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(text, new Point2D.Double(20, 30));

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        FreeTextNode reloadedText = (FreeTextNode) reloaded.getAllNodes().iterator().next();
        assertEquals(text.getText().getText(), reloadedText.getText().getText());
        assertEquals(new Point2D.Double(20, 30), reloadedText.getLocation());
    }

    private static FreeTextNode findPrototype(IGraph graph)
    {
        for (INode aPrototype : graph.getNodePrototypes())
        {
            if (aPrototype instanceof FreeTextNode)
            {
                return (FreeTextNode) aPrototype;
            }
        }
        return null;
    }

    private static BufferedImage draw(IGraph graph)
    {
        BufferedImage image = whiteImage();
        Graphics2D g2 = image.createGraphics();
        graph.draw(g2);
        g2.dispose();
        return image;
    }

    private static BufferedImage whiteImage()
    {
        BufferedImage image = new BufferedImage(500, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.dispose();
        return image;
    }

    private static boolean hasPixelOtherThanWhite(BufferedImage image)
    {
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                if (image.getRGB(x, y) != Color.WHITE.getRGB())
                {
                    return true;
                }
            }
        }
        return false;
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
