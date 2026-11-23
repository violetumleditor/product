package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;

/**
 * A thin edge is found a few pixels away from its line, but never inside the nodes it links, which keep their clicks
 */
class EdgeHitTest
{

    private IGraph graph;

    private ClassNode start;

    private GenericEdge edge;

    @BeforeAll
    static void registerBeans()
    {
        if (BeanFactory.getFactory().contains(IFileChooserService.class))
        {
            return;
        }
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, new JFileChooserService());
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());
    }

    @BeforeEach
    void createDiagram()
    {
        this.graph = new GraphFile(ClassDiagramGraph.class).getGraph();
        this.start = new ClassNode();
        assertTrue(this.graph.addNode(this.start, new Point2D.Double(100, 100)));
        ClassNode end = new ClassNode();
        assertTrue(this.graph.addNode(end, new Point2D.Double(500, 100)));
        this.edge = new GenericEdge();
        this.edge.setBentStyle(BentStyle.STRAIGHT);
        assertTrue(this.graph.connect(this.edge, this.start, new Point2D.Double(), end, new Point2D.Double()));
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }

    @Test
    void shouldFindTheEdgeAFewPixelsAwayFromItsLine()
    {
        Point2D middle = getMiddle();

        assertSame(this.edge, this.graph.findEdge(new Point2D.Double(middle.getX(), middle.getY() + 5)));
        assertSame(this.edge, this.graph.findEdge(new Point2D.Double(middle.getX(), middle.getY() - 5)));
    }

    @Test
    void shouldNotFindTheEdgeFarFromItsLine()
    {
        Point2D middle = getMiddle();

        assertNull(this.graph.findEdge(new Point2D.Double(middle.getX(), middle.getY() + 9)));
    }

    @Test
    void shouldLeaveTheClicksInsideALinkedNodeToTheNode()
    {
        Line2D connectionPoints = this.edge.getConnectionPoints();
        // Straight horizontal line: its start point is on the right side of the start node
        assertEquals(connectionPoints.getY1(), connectionPoints.getY2(), 0.5);
        Point2D insideStartNode = new Point2D.Double(connectionPoints.getX1() - 5, connectionPoints.getY1());

        assertFalse(this.edge.contains(insideStartNode));
        assertSame(this.start, this.graph.findNode(insideStartNode));
    }

    private Point2D getMiddle()
    {
        Line2D connectionPoints = this.edge.getConnectionPoints();
        return new Point2D.Double((connectionPoints.getX1() + connectionPoints.getX2()) / 2,
                (connectionPoints.getY1() + connectionPoints.getY2()) / 2);
    }
}
