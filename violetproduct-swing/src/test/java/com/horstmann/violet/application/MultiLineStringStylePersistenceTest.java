package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.object.ObjectNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;

/**
 * The alignment, the size and the underline of a text are not saved as such: they must come back from its HTML.
 */
class MultiLineStringStylePersistenceTest
{
    @Test
    void classTextsKeepTheirSizeAndAlignment() throws IOException
    {
        ClassNode node = new ClassNode();
        node.getName().setText("Customer");
        node.getAttributes().setText("name : String");
        node.getMethods().setText("pay()");
        node.getMethods().setJustification(MultiLineString.Justification.RIGHT);
        Rectangle2DCopy bounds = new Rectangle2DCopy(node);

        ClassNode reloaded = (ClassNode) reload(new ClassDiagramGraph(), node);

        assertEquals(MultiLineString.FontSize.LARGE, reloaded.getName().getSize());
        assertEquals(MultiLineString.Justification.CENTER, reloaded.getName().getJustification());
        assertEquals(MultiLineString.Justification.LEFT, reloaded.getAttributes().getJustification());
        assertEquals(MultiLineString.Justification.RIGHT, reloaded.getMethods().getJustification());
        assertEquals(MultiLineString.FontSize.NORMAL, reloaded.getAttributes().getSize());
        assertFalse(reloaded.getName().isUnderlined());
        assertEquals(bounds, new Rectangle2DCopy(reloaded));
    }

    @Test
    void objectNameStaysLargeAndUnderlined() throws IOException
    {
        ObjectNode node = new ObjectNode();
        node.getName().setText("customer : Customer");

        ObjectNode reloaded = (ObjectNode) reload(new ObjectDiagramGraph(), node);

        assertTrue(reloaded.getName().isUnderlined());
        assertEquals(MultiLineString.FontSize.LARGE, reloaded.getName().getSize());
    }

    @Test
    void lifelineNameStaysUnderlinedTwoReloadsLater() throws IOException
    {
        LifelineNode node = new LifelineNode();
        node.getName().setText("customer");

        LifelineNode reloaded = (LifelineNode) reload(new SequenceDiagramGraph(), node);
        reloaded = (LifelineNode) reload(new SequenceDiagramGraph(), reloaded);

        assertTrue(reloaded.getName().isUnderlined());
    }

    @Test
    void underlineRemovedFromANameIsNotBackAfterAReload() throws IOException
    {
        LifelineNode node = new LifelineNode();
        node.getName().setText("customer");
        node.getName().setUnderlined(false);

        LifelineNode reloaded = (LifelineNode) reload(new SequenceDiagramGraph(), node);

        assertFalse(reloaded.getName().isUnderlined());
    }

    private INode reload(IGraph graph, INode node) throws IOException
    {
        graph.addNode(node, new Point2D.Double(20, 30));
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        return reloaded.getAllNodes().iterator().next();
    }

    /**
     * The size of a node, to compare before and after a reload.
     */
    private record Rectangle2DCopy(double width, double height)
    {
        Rectangle2DCopy(INode node)
        {
            this(node.getBounds().getWidth(), node.getBounds().getHeight());
        }
    }
}
