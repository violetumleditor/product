package com.horstmann.violet.framework.file.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

class XMLPersistenceServiceTest {

    @Test
    void writeRejectsNullGraph() {
        XMLPersistenceService service = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThrows(IllegalArgumentException.class, () -> service.write(null, out));
    }

    @Test
    void readRejectsEmptyXmlContent() {
        XMLPersistenceService service = new XMLPersistenceService();
        ByteArrayInputStream in = new ByteArrayInputStream("   \n\t".getBytes());

        assertThrows(IOException.class, () -> service.read(in));
    }

    @Test
    void roundTripPreservesSimpleFields() throws Exception {
        XMLPersistenceService service = createServiceWithType(TestGraph.class);
        TestGraph graph = new TestGraph();
        graph.value = "Sample graph";
        graph.location = new Point2D.Double(12.5, 42.25);
        graph.preferredSize = new Rectangle2D.Double(0d, 0d, 320d, 180d);
        graph.backgroundColor = new Color(10, 20, 30, 40);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.write(graph, out);

        String xml = out.toString();
        assertTrue(xml.contains("<testGraph"));
        assertTrue(xml.contains("<backgroundColor red=\"10\""));

        IGraph reloaded = service.read(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(reloaded);
        assertTrue(reloaded instanceof TestGraph);

        TestGraph reloadedGraph = (TestGraph) reloaded;
        assertEquals(graph.value, reloadedGraph.value);
        assertEquals(graph.location.getX(), reloadedGraph.location.getX(), 0.0001d);
        assertEquals(graph.location.getY(), reloadedGraph.location.getY(), 0.0001d);
        assertEquals(graph.preferredSize.getWidth(), reloadedGraph.preferredSize.getWidth(), 0.0001d);
        assertEquals(graph.preferredSize.getHeight(), reloadedGraph.preferredSize.getHeight(), 0.0001d);
        assertEquals(graph.backgroundColor.getRed(), reloadedGraph.backgroundColor.getRed());
        assertEquals(graph.backgroundColor.getGreen(), reloadedGraph.backgroundColor.getGreen());
        assertEquals(graph.backgroundColor.getBlue(), reloadedGraph.backgroundColor.getBlue());
        assertEquals(graph.backgroundColor.getAlpha(), reloadedGraph.backgroundColor.getAlpha());
    }

    private static XMLPersistenceService createServiceWithType(Class<?> type) throws Exception {
        XMLPersistenceService service = new XMLPersistenceService();
        Field tagTypesField = XMLPersistenceService.class.getDeclaredField("tagTypes");
        tagTypesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> tagTypes = (Map<String, Class<?>>) tagTypesField.get(service);
        String tagName = Character.toLowerCase(type.getSimpleName().charAt(0)) + type.getSimpleName().substring(1);
        tagTypes.put(tagName, type);
        return service;
    }

    public static class TestGraph implements IGraph {

        public String value = "";
        public Point2D.Double location = new Point2D.Double();
        public Rectangle2D.Double preferredSize = new Rectangle2D.Double();
        public Color backgroundColor = new Color(0, 0, 0, 255);
        public Collection<INode> allNodes = new ArrayList<INode>();
        public Collection<IEdge> allEdges = new ArrayList<IEdge>();

        @Override
        public List<INode> getNodePrototypes() {
            return new ArrayList<INode>();
        }

        @Override
        public List<IEdge> getEdgePrototypes() {
            return new ArrayList<IEdge>();
        }

        @Override
        public List<IEdge> getEdgePrototypes(INode start, INode end) {
            return new ArrayList<IEdge>();
        }

        @Override
        public boolean canAddNode(INode n, Point2D p) {
            return true;
        }

        @Override
        public Collection<INode> getAllNodes() {
            return this.allNodes;
        }

        @Override
        public Collection<IEdge> getAllEdges() {
            return this.allEdges;
        }

        @Override
        public void removeEdge(IEdge... edgesToRemove) {
            // No-op for this persistence fixture.
        }

        @Override
        public void removeNode(INode... nodesToRemove) {
            // No-op for this persistence fixture.
        }

        @Override
        public boolean addNode(INode n, Point2D p) {
            return this.allNodes.add(n);
        }

        @Override
        public boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation) {
            return this.allEdges.add(e);
        }

        @Override
        public INode findNode(Id id) {
            return null;
        }

        @Override
        public INode findNode(Point2D p) {
            return null;
        }

        @Override
        public IEdge findEdge(Id id) {
            return null;
        }

        @Override
        public IEdge findEdge(Point2D p) {
            return null;
        }

        @Override
        public void draw(Graphics2D g2) {
            // No-op for this persistence fixture.
        }

        @Override
        public Rectangle2D getClipBounds() {
            return new Rectangle2D.Double();
        }

        @Override
        public void setBounds(Rectangle2D newValue) {
            // No-op for this persistence fixture.
        }

        @Override
        public IGridSticker getGridSticker() {
            return null;
        }

        @Override
        public void setGridSticker(IGridSticker newCorrector) {
            // No-op for this persistence fixture.
        }
    }
}
