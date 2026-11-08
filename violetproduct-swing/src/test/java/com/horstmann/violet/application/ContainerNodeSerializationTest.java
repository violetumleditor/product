package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.edges.DependencyEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.object.FieldNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;

class ContainerNodeSerializationTest
{

    @Test
    void shouldSaveAndReloadContainerProperties() throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        ContainerNode container = new ContainerNode();
        container.setPreferredSize(new Rectangle2D.Double(0, 0, 300, 200));
        container.setLineStyle(LineStyle.SOLID);
        container.setAngleStyle(AngleStyle.ROUNDED2);
        container.setBorderWidth(3);
        container.setBackgroundColor(new Color(240, 240, 240));
        graph.addNode(container, new Point2D.Double(20, 30));

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        ContainerNode reloadedContainer = (ContainerNode) reloaded.getAllNodes().iterator().next();
        assertEquals(new Rectangle2D.Double(20, 30, 300, 200), reloadedContainer.getBounds());
        assertSame(LineStyle.SOLID, reloadedContainer.getLineStyle());
        assertSame(AngleStyle.ROUNDED2, reloadedContainer.getAngleStyle());
        assertEquals(3, reloadedContainer.getBorderWidth());
        assertEquals(new Color(240, 240, 240), reloadedContainer.getBackgroundColor());
    }

    @Test
    void shouldStayInBackgroundAndNeverOwnNodesOrEdges()
    {
        IGraph graph = new ClassDiagramGraph();
        ContainerNode container = new ContainerNode();
        container.setPreferredSize(new Rectangle2D.Double(0, 0, 400, 300));
        graph.addNode(container, new Point2D.Double(0, 0));
        ContainerNode smallContainer = new ContainerNode();
        smallContainer.setPreferredSize(new Rectangle2D.Double(0, 0, 200, 150));
        graph.addNode(smallContainer, new Point2D.Double(10, 10));
        ClassNode classNode = new ClassNode();
        graph.addNode(classNode, new Point2D.Double(20, 20));

        // Added nodes stay attached to the graph
        assertTrue(classNode.getParent() == null);
        // Regular nodes are found first, then the smallest container
        assertSame(classNode, graph.findNode(new Point2D.Double(25, 25)));
        assertSame(smallContainer, graph.findNode(new Point2D.Double(190, 140)));
        assertSame(container, graph.findNode(new Point2D.Double(390, 290)));
        // A large container contains the small one and the class node
        List<INode> containedNodes = container.getContainedNodes();
        assertTrue(containedNodes.contains(smallContainer));
        assertTrue(containedNodes.contains(classNode));
        // Only generic edges can be connected to a container
        assertFalse(graph.connect(new DependencyEdge(), classNode, new Point2D.Double(), container, new Point2D.Double()));
        assertTrue(graph.connect(new GenericEdge(), classNode, new Point2D.Double(), container, new Point2D.Double()));
        assertTrue(graph.connect(new GenericEdge(), container, new Point2D.Double(), smallContainer, new Point2D.Double()));
        // Removing a container keeps its content
        graph.removeNode(container);
        assertTrue(graph.getAllNodes().contains(classNode));
        assertNotNull(graph.findNode(new Point2D.Double(25, 25)));
    }

    @Test
    void shouldBeAvailableInAllDiagrams()
    {
        IGraph[] graphs = {
                new ClassDiagramGraph(), new SequenceDiagramGraph(), new ActivityDiagramGraph(), new StateDiagramGraph(),
                new ObjectDiagramGraph(), new UseCaseDiagramGraph()
        };
        for (IGraph graph : graphs)
        {
            assertTrue(graph.getNodePrototypes().stream().anyMatch(ContainerNode.class::isInstance), graph.getClass().getSimpleName());
        }
    }

    @Test
    void shouldRefuseChildOnlyNodesDroppedOnContainer()
    {
        IGraph sequenceGraph = new SequenceDiagramGraph();
        ContainerNode container = new ContainerNode();
        sequenceGraph.addNode(container, new Point2D.Double(0, 0));
        assertFalse(sequenceGraph.addNode(new ActivationBarNode(), new Point2D.Double(50, 50)));

        IGraph objectGraph = new ObjectDiagramGraph();
        objectGraph.addNode(new ContainerNode(), new Point2D.Double(0, 0));
        assertFalse(objectGraph.addNode(new FieldNode(), new Point2D.Double(50, 50)));
    }
}
