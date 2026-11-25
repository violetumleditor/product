package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;

/**
 * The arrow heads keep their proportions whatever the thickness: exact tip, length, never more than half of the segment
 */
class ArrowHeadTest
{

    private final Point2D start = new Point2D.Double(0, 0);

    private final Point2D end = new Point2D.Double(200, 0);

    @Test
    void shouldEndAtTheTipWithTheExpectedLength()
    {
        for (int width = 1; width <= 10; width++)
        {
            checkLength(ArrowHead.BLACK_TRIANGLE, width, 7 + 2 * width, Math.min(width, 1.5f));
            checkLength(ArrowHead.TRIANGLE, width, 6.3 + 2.7 * width, width);
            for (ArrowHead head : new ArrowHead[] { ArrowHead.DIAMOND, ArrowHead.BLACK_DIAMOND })
            {
                checkLength(head, width, 7 + 2.7 * width, width);
            }
        }
    }

    @Test
    void shouldGiveTheSameShapeToOpenWhiteAndBlackArrows()
    {
        Rectangle2D black = drawnBounds(ArrowHead.BLACK_TRIANGLE, 1, 1);
        assertEquals(black, drawnBounds(ArrowHead.TRIANGLE, 1, 1));
        for (int width = 1; width <= 10; width++)
        {
            black = drawnBounds(ArrowHead.BLACK_TRIANGLE, width, Math.min(width, 1.5f));
            Rectangle2D open = drawnBounds(ArrowHead.V, width, width);
            assertEquals(black.getHeight(), open.getHeight(), 0.1);
            assertEquals(black.getWidth(), open.getWidth(), 0.1);
        }
    }

    @Test
    void shouldStayHollow()
    {
        for (int width = 1; width <= 10; width++)
        {
            for (ArrowHead head : new ArrowHead[] { ArrowHead.TRIANGLE, ArrowHead.DIAMOND })
            {
                Rectangle2D b = head.getPath(this.start, this.end, width).getBounds2D();
                double x = head == ArrowHead.DIAMOND ? b.getCenterX() : b.getX() + b.getWidth() / 3;
                assertFalse(stroke(width).createStrokedShape(head.getPath(this.start, this.end, width)).contains(x, 0));
            }
        }
    }

    @Test
    void shouldDrawTheLineUpToTheTipOfAnOpenHead()
    {
        assertEquals(1, ArrowHead.V.getLineCut(this.start, this.end, 1));
        assertEquals(10, ArrowHead.V.getLineCut(this.start, this.end, 10));
    }

    @Test
    void shouldKeepTheSameSizeOnAShortSegment()
    {
        Point2D near = new Point2D.Double(12, 0);
        for (ArrowHead head : new ArrowHead[] { ArrowHead.V, ArrowHead.TRIANGLE, ArrowHead.BLACK_TRIANGLE, ArrowHead.DIAMOND,
                ArrowHead.BLACK_DIAMOND })
        {
            for (int width = 1; width <= 10; width++)
            {
                Rectangle2D expected = head.getPath(this.start, this.end, width).getBounds2D();
                Rectangle2D actual = head.getPath(this.start, near, width).getBounds2D();
                assertEquals(expected.getWidth(), actual.getWidth(), 0.01);
                assertEquals(expected.getHeight(), actual.getHeight(), 0.01);
                assertTrue(head.getLineCut(this.start, near, width) <= 12);
            }
        }
    }

    private void checkLength(ArrowHead head, int width, double expectedLength, float outline)
    {
        Rectangle2D drawn = drawnBounds(head, width, outline);
        assertEquals(200, drawn.getMaxX(), 0.1);
        assertEquals(expectedLength, drawn.getWidth(), 0.1);
    }

    private Rectangle2D drawnBounds(ArrowHead head, int width, float outline)
    {
        return stroke(outline).createStrokedShape(head.getPath(this.start, this.end, width)).getBounds2D();
    }

    private BasicStroke stroke(float width)
    {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }
}
