package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.abstracts.node.CropInsets;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.ImageNode;

/**
 * The image keeps its ratio, so its size can't follow the grid: the center of its visible part is put on the grid instead, so
 * that links to it can be straight.
 */
class ImageNodeGridTest
{

    private static final double GRID = 10;

    @Test
    void centerOfResizedImageIsOnTheGrid()
    {
        ImageNode node = addImage(new Point2D.Double(20, 30));
        node.setPreferredSize(new Rectangle2D.Double(0, 0, 73, 73));

        assertCenterOnGrid(node.getBounds());
        assertEquals(73, node.getBounds().getWidth(), 0.001, "the size of the image stays as resized");
    }

    @Test
    void centerOfCroppedImageIsOnTheGrid()
    {
        ImageNode node = addImage(new Point2D.Double(3, 7));
        node.setCropInsets(new CropInsets(4, 9, 1, 2));

        assertCenterOnGrid(node.getBounds());
    }

    @Test
    void locationOnGraphIsTheVisibleTopLeftCorner()
    {
        ImageNode node = addImage(new Point2D.Double(3, 7));
        node.setPreferredSize(new Rectangle2D.Double(0, 0, 73, 73));
        node.setCropInsets(new CropInsets(4, 9, 1, 2));

        Rectangle2D bounds = node.getBounds();
        Point2D locationOnGraph = node.getLocationOnGraph();
        assertEquals(bounds.getX(), locationOnGraph.getX(), 0.001);
        assertEquals(bounds.getY(), locationOnGraph.getY(), 0.001);
    }

    /** Two links on a side of 100 px used to end at 33.3 and 66.7 px: off the grid, so never straight */
    @Test
    void linksSharingASideEndOnTheGrid()
    {
        IGraph graph = newGraph();
        ImageNode target = addImage(graph, new Point2D.Double(300, 100));
        target.setPreferredSize(new Rectangle2D.Double(0, 0, 100, 100));
        ImageNode upper = addImage(graph, new Point2D.Double(0, 98));
        ImageNode lower = addImage(graph, new Point2D.Double(0, 138));
        IEdge upperLink = new GenericEdge();
        IEdge lowerLink = new GenericEdge();
        assertTrue(graph.connect(upperLink, upper, new Point2D.Double(), target, new Point2D.Double()));
        assertTrue(graph.connect(lowerLink, lower, new Point2D.Double(), target, new Point2D.Double()));

        Line2D upperPoints = upperLink.getConnectionPoints();
        Line2D lowerPoints = lowerLink.getConnectionPoints();
        assertEquals(130, upperPoints.getY2(), 0.001, "upper link, 20 px above the middle of the side");
        assertEquals(170, lowerPoints.getY2(), 0.001, "lower link, 20 px below the middle of the side");
        assertEquals(upperPoints.getY1(), upperPoints.getY2(), 0.001, "an image centered at the same height gets a straight link");
    }

    private ImageNode addImage(Point2D location)
    {
        return addImage(newGraph(), location);
    }

    private IGraph newGraph()
    {
        IGraph graph = new ClassDiagramGraph();
        graph.setGridSticker(new RoundingGridSticker());
        return graph;
    }

    private ImageNode addImage(IGraph graph, Point2D location)
    {
        ImageNode node = new ImageNode(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB));
        graph.addNode(node, new Point2D.Double(0, 0));
        node.setLocation(location);
        return node;
    }

    private void assertCenterOnGrid(Rectangle2D bounds)
    {
        assertEquals(0, Math.IEEEremainder(bounds.getCenterX(), GRID), 0.001, "center x on the grid: " + bounds);
        assertEquals(0, Math.IEEEremainder(bounds.getCenterY(), GRID), 0.001, "center y on the grid: " + bounds);
    }

    private static final class RoundingGridSticker implements IGridSticker
    {
        @Override
        public Rectangle2D snap(Rectangle2D r)
        {
            return r;
        }

        @Override
        public Point2D snap(Point2D p)
        {
            return new Point2D.Double(Math.round(p.getX() / GRID) * GRID, Math.round(p.getY() / GRID) * GRID);
        }
    }
}
