package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;

/**
 * The labels of an edge may hold several lines, drawn one below the other, and never across the line of the edge, whatever its
 * direction
 */
class EdgeLabelMultiLineTest
{

    private static final String THREE_LINES = "first line\nsecond\nthe third line";

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

    /** Second node location, from the first one: the eight directions */
    static Stream<Point2D> directions()
    {
        return Stream.of(new Point2D.Double(400, 0), new Point2D.Double(-400, 0), new Point2D.Double(0, 300),
                new Point2D.Double(0, -300), new Point2D.Double(400, 300), new Point2D.Double(-400, -300),
                new Point2D.Double(400, -300), new Point2D.Double(-400, 300));
    }

    @Test
    void shouldDrawTheLinesOneBelowTheOther()
    {
        GenericEdge edge = connect(new ClassDiagramGraph(), new Point2D.Double(400, 0));
        edge.getMiddleLabel().setText("one line");
        Rectangle2D oneLine = edge.getMiddleLabelBounds();
        edge.getMiddleLabel().setText("one line\nanother line");
        Rectangle2D twoLines = edge.getMiddleLabelBounds();

        assertEquals(2 * oneLine.getHeight(), twoLines.getHeight(), 2);
    }

    @ParameterizedTest
    @MethodSource("directions")
    void shouldNotCrossTheEdge(Point2D direction)
    {
        GenericEdge edge = connect(new ClassDiagramGraph(), direction);
        edge.getStartLabel().setText(THREE_LINES);
        edge.getMiddleLabel().setText(THREE_LINES);
        edge.getEndLabel().setText(THREE_LINES);
        edge.setEndArrowHead(ArrowHead.V);
        List<Point2D> points = edge.getPoints();

        Rectangle2D start = edge.getStartLabelBounds();
        Rectangle2D middle = edge.getMiddleLabelBounds();
        Rectangle2D end = edge.getEndLabelBounds();

        for (Rectangle2D label : new Rectangle2D[] { start, middle, end })
        {
            assertNotNull(label);
            assertTrue(label.getHeight() > 30, "three lines expected: " + label);
            assertFalse(edge.getShape().intersects(label), "label " + label + " crosses the edge " + points);
        }
    }

    @Test
    void shouldKeepTheLinesWhenSavedAndReloaded() throws Exception
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connect(graph, new Point2D.Double(400, 0));
        edge.getMiddleLabel().setText(THREE_LINES);
        edge.getStartLabel().setText("a\nb");

        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        IEdge reloadedEdge = reloaded.getAllEdges().iterator().next();
        assertEquals(THREE_LINES, ((GenericEdge) reloadedEdge).getMiddleLabel().getPlainText());
        assertEquals("a\nb", ((GenericEdge) reloadedEdge).getStartLabel().getPlainText());
    }

    private GenericEdge connect(IGraph graph, Point2D direction)
    {
        ClassNode start = new ClassNode();
        ClassNode end = new ClassNode();
        graph.addNode(start, new Point2D.Double(500, 400));
        graph.addNode(end, new Point2D.Double(500 + direction.getX(), 400 + direction.getY()));
        GenericEdge edge = new GenericEdge();
        edge.setBentStyle(BentStyle.STRAIGHT);
        assertTrue(graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
        return edge;
    }
}
