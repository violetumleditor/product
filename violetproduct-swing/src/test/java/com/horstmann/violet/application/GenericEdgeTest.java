package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.object.ObjectNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;

class GenericEdgeTest
{
    private static final Point2D ANY_POINT = new Point2D.Double();

    @Test
    void shouldBeAvailableInAllDiagrams()
    {
        IGraph[] graphs = { new ActivityDiagramGraph(), new ClassDiagramGraph(), new ObjectDiagramGraph(),
                new SequenceDiagramGraph(), new StateDiagramGraph(), new UseCaseDiagramGraph() };
        for (IGraph aGraph : graphs)
        {
            GenericEdge prototype = findPrototype(aGraph);
            assertNotNull(prototype, aGraph.getClass().getSimpleName());
            assertFalse(prototype.getToolTip().isEmpty());
        }
    }

    @Test
    void shouldDrawLikeTheFormerNoteEdgeByDefault()
    {
        GenericEdge edge = new GenericEdge();
        assertEquals(LineStyle.DOTTED, edge.getLineStyle());
        assertEquals(BentStyle.STRAIGHT, edge.getBentStyle());
        assertEquals(ArrowHead.NONE, edge.getStartArrowHead());
        assertEquals(ArrowHead.NONE, edge.getEndArrowHead());
    }

    @Test
    void shouldLinkAGenericNodeToAnyOtherNode()
    {
        IGraph sequence = new SequenceDiagramGraph();
        LifelineNode lifeline = new LifelineNode();
        NoteNode sequenceNote = new NoteNode();
        sequence.addNode(lifeline, new Point2D.Double(20, 20));
        sequence.addNode(sequenceNote, new Point2D.Double(300, 20));
        // A lifeline refuses the other edges, even as a starting node
        assertTrue(sequence.connect(new GenericEdge(), lifeline, ANY_POINT, sequenceNote, ANY_POINT));

        IGraph object = new ObjectDiagramGraph();
        ObjectNode objectNode = new ObjectNode();
        NoteNode objectNote = new NoteNode();
        object.addNode(objectNode, new Point2D.Double(20, 20));
        object.addNode(objectNote, new Point2D.Double(300, 20));
        assertTrue(object.connect(new GenericEdge(), objectNode, ANY_POINT, objectNote, ANY_POINT));

        IGraph graph = new ClassDiagramGraph();
        NoteNode note1 = new NoteNode();
        NoteNode note2 = new NoteNode();
        ImageNode image = new ImageNode();
        ContainerNode container = new ContainerNode();
        graph.addNode(note1, new Point2D.Double(20, 20));
        graph.addNode(note2, new Point2D.Double(300, 20));
        graph.addNode(image, new Point2D.Double(20, 300));
        graph.addNode(container, new Point2D.Double(600, 600));
        assertTrue(graph.connect(new GenericEdge(), note1, ANY_POINT, note2, ANY_POINT));
        assertTrue(graph.connect(new GenericEdge(), note1, ANY_POINT, image, ANY_POINT));
        assertTrue(graph.connect(new GenericEdge(), image, ANY_POINT, container, ANY_POINT));
    }

    @Test
    void shouldAlsoLinkTwoNodesOfTheDiagram()
    {
        IGraph classes = new ClassDiagramGraph();
        ClassNode class1 = new ClassNode();
        ClassNode class2 = new ClassNode();
        classes.addNode(class1, new Point2D.Double(20, 20));
        classes.addNode(class2, new Point2D.Double(300, 20));
        assertTrue(classes.connect(new GenericEdge(), class1, ANY_POINT, class2, ANY_POINT));
        assertTrue(classes.connect(new GenericEdge(), class1, ANY_POINT, class1, ANY_POINT));

        IGraph sequence = new SequenceDiagramGraph();
        LifelineNode lifeline1 = new LifelineNode();
        LifelineNode lifeline2 = new LifelineNode();
        sequence.addNode(lifeline1, new Point2D.Double(20, 20));
        sequence.addNode(lifeline2, new Point2D.Double(300, 20));
        assertTrue(sequence.connect(new GenericEdge(), lifeline1, ANY_POINT, lifeline2, ANY_POINT));
    }

    @Test
    void shouldNeedAnEndingNode()
    {
        IGraph graph = new ClassDiagramGraph();
        NoteNode note = new NoteNode();
        graph.addNode(note, new Point2D.Double(20, 20));
        assertFalse(graph.connect(new GenericEdge(), note, ANY_POINT, null, ANY_POINT));
        assertTrue(graph.connect(new GenericEdge(), note, ANY_POINT, note, ANY_POINT));
    }

    @Test
    void shouldSaveAndReloadAllItsProperties() throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connectAClassToANote(graph);
        edge.setStartLabel("start");
        edge.setMiddleLabel("middle");
        edge.setEndLabel("end");
        edge.setLineStyle(LineStyle.SOLID);
        edge.setStartArrowHead(ArrowHead.DIAMOND);
        edge.setEndArrowHead(ArrowHead.TRIANGLE);
        edge.setBentStyle(BentStyle.HVH);
        edge.setAngleStyle(AngleStyle.ROUNDED2);

        GenericEdge reloaded = (GenericEdge) reload(write(graph)).getAllEdges().iterator().next();

        assertEquals("start", reloaded.getStartLabel());
        assertEquals("middle", reloaded.getMiddleLabel());
        assertEquals("end", reloaded.getEndLabel());
        assertEquals(LineStyle.SOLID, reloaded.getLineStyle());
        assertEquals(ArrowHead.DIAMOND, reloaded.getStartArrowHead());
        assertEquals(ArrowHead.TRIANGLE, reloaded.getEndArrowHead());
        assertEquals(BentStyle.HVH, reloaded.getBentStyle());
        assertEquals(AngleStyle.ROUNDED2, reloaded.getAngleStyle());
    }

    @Test
    void shouldReadTheFormerNoteEdgesAsGenericEdges() throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        connectAClassToANote(graph);
        // A former note edge had none of the segmented line properties
        String legacyXml = write(graph)
                .replaceAll("(?s)<(lineStyle|bentStyle|angleStyle|startArrowHead|endArrowHead|startLabel|middleLabel|endLabel)\\b.*?(/>|</\\1>)", "")
                .replace("<genericEdge", "<" + GenericEdge.LEGACY_NOTE_EDGE_TAG)
                .replace("</genericEdge>", "</" + GenericEdge.LEGACY_NOTE_EDGE_TAG + ">");
        assertFalse(legacyXml.contains("genericEdge"));
        assertFalse(legacyXml.contains("lineStyle"));

        IGraph reloaded = reload(legacyXml);

        assertEquals(1, reloaded.getAllEdges().size());
        IEdge edge = reloaded.getAllEdges().iterator().next();
        assertTrue(edge instanceof GenericEdge);
        assertEquals(LineStyle.DOTTED, ((GenericEdge) edge).getLineStyle());
        assertEquals(BentStyle.STRAIGHT, ((GenericEdge) edge).getBentStyle());
        assertNotNull(edge.getStartNode());
        assertNotNull(edge.getEndNode());
        assertTrue(write(reloaded).contains("<genericEdge"));
    }

    private static GenericEdge connectAClassToANote(IGraph graph)
    {
        ClassNode classNode = new ClassNode();
        NoteNode note = new NoteNode();
        graph.addNode(classNode, new Point2D.Double(20, 20));
        graph.addNode(note, new Point2D.Double(300, 20));
        GenericEdge edge = new GenericEdge();
        assertTrue(graph.connect(edge, classNode, ANY_POINT, note, ANY_POINT));
        return edge;
    }

    private static String write(IGraph graph)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XMLPersistenceService().write(graph, out);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static IGraph reload(String xml) throws IOException
    {
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        return persistenceService.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static GenericEdge findPrototype(IGraph graph)
    {
        for (IEdge aPrototype : graph.getEdgePrototypes())
        {
            if (aPrototype instanceof GenericEdge)
            {
                return (GenericEdge) aPrototype;
            }
        }
        return null;
    }
}
