package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.product.diagram.common.ImageNode;

class GraphDrawingOrderTest
{

    private static final Color OLD_COLOR = Color.RED;
    private static final Color NEW_COLOR = Color.BLUE;

    /** A point located in both images */
    private static final Point2D OVERLAP = new Point2D.Double(70, 70);

    @Test
    void shouldDrawMostRecentImageUponOldestOne()
    {
        IGraph graph = newGraphWithTwoImages();

        assertEquals(NEW_COLOR.getRGB(), drawAndGetOverlapPixel(graph));
    }

    @Test
    void shouldKeepOrderOnceReloaded() throws IOException
    {
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(newGraphWithTwoImages(), out);

        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(NEW_COLOR.getRGB(), drawAndGetOverlapPixel(reloaded));
    }

    @Test
    void shouldFindNodeDrawnOnTop()
    {
        IGraph graph = new ClassDiagramGraph();
        addImage(graph, OLD_COLOR, 50);
        ImageNode mostRecent = addImage(graph, NEW_COLOR, 60);

        assertSame(mostRecent, graph.findNode(OVERLAP));
    }

    @Test
    void shouldDrawMostRecentImageUponOldestClass()
    {
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(new ClassNode(), new Point2D.Double(50, 50));
        addImage(graph, NEW_COLOR, 60);

        assertEquals(NEW_COLOR.getRGB(), drawAndGetOverlapPixel(graph));
    }

    @Test
    void shouldDrawChildrenOfMostRecentNodeUponOldestImage()
    {
        IGraph graph = new ClassDiagramGraph();
        addImage(graph, OLD_COLOR, 50);
        PackageNode packageNode = new PackageNode();
        graph.addNode(packageNode, new Point2D.Double(300, 300));
        ClassNode child = new ClassNode();
        child.setBackgroundColor(NEW_COLOR);
        packageNode.addChild(child, new Point2D.Double(10, 30));
        Point2D childCenter = new Point2D.Double(child.getLocationOnGraph().getX() + 20, child.getLocationOnGraph().getY() + 20);
        double dx = OVERLAP.getX() - childCenter.getX();
        double dy = OVERLAP.getY() - childCenter.getY();
        packageNode.translate(dx, dy);

        assertEquals(NEW_COLOR.getRGB(), drawAndGetOverlapPixel(graph));
    }

    private static IGraph newGraphWithTwoImages()
    {
        IGraph graph = new ClassDiagramGraph();
        addImage(graph, OLD_COLOR, 50);
        addImage(graph, NEW_COLOR, 60);
        return graph;
    }

    private static ImageNode addImage(IGraph graph, Color color, double location)
    {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, 40, 40);
        g2.dispose();
        ImageNode node = new ImageNode(image);
        graph.addNode(node, new Point2D.Double(location, location));
        return node;
    }

    private static int drawAndGetOverlapPixel(IGraph graph)
    {
        BufferedImage canvas = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        graph.draw(g2);
        g2.dispose();
        return canvas.getRGB((int) OVERLAP.getX(), (int) OVERLAP.getY());
    }
}
