package com.horstmann.violet.framework.swingextension;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Open hand, shown over what can be held, and closed hand, shown while it is held: no standard cursor of Java is either. Drawn at
 * the size of the system's cursors, like them (white, thin black outline), so that the mouse doesn't seem to change. The same on
 * the desktop and in the browser (CheerpJ supports custom cursors).
 */
public class HandCursors
{

    /**
     * @return the cursor, or the standard move cursor where custom cursors are not supported
     */
    public Cursor getClosedHand()
    {
        if (this.closedHand == null)
        {
            this.closedHand = createCursor(createClosedHandShape(), CLOSED_FINGERS_BOTTOM, "closedHand");
        }
        return this.closedHand;
    }

    /**
     * @return the cursor, or the standard move cursor where custom cursors are not supported
     */
    public Cursor getOpenHand()
    {
        if (this.openHand == null)
        {
            this.openHand = createCursor(createOpenHandShape(), OPEN_FINGERS_BOTTOM, "openHand");
        }
        return this.openHand;
    }

    private Cursor createCursor(Path2D hand, double fingersBottom, String name)
    {
        Cursor defaultCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        if (GraphicsEnvironment.isHeadless())
        {
            return defaultCursor;
        }
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension size = toolkit.getBestCursorSize(BASE_SIZE, BASE_SIZE);
        if (size.width <= 0 || size.height <= 0)
        {
            return defaultCursor;
        }
        Point hotSpot = new Point(size.width / 2, size.height / 2);
        return toolkit.createCustomCursor(createImage(hand, fingersBottom, size), hotSpot, name);
    }

    private BufferedImage createImage(Path2D hand, double fingersBottom, Dimension size)
    {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.scale(size.width / (double) BASE_SIZE, size.height / (double) BASE_SIZE);
        g2.setColor(Color.WHITE);
        g2.fill(hand);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(hand);
        // Between the fingers, down from the outline
        g2.clip(hand);
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        for (double x = 12.5; x < 22; x += 4)
        {
            g2.draw(new Line2D.Double(x, 0, x, fingersBottom));
        }
        g2.dispose();
        return image;
    }

    /**
     * @return the outline of a fist: four folded fingers on top, the thumb on the left
     */
    private Path2D createClosedHandShape()
    {
        Path2D hand = new Path2D.Double();
        hand.moveTo(8.5, 15);
        hand.lineTo(8.5, 11);
        hand.curveTo(8.5, 8.4, 12.5, 8.4, 12.5, 10);
        hand.curveTo(12.5, 7.4, 16.5, 7.4, 16.5, 10);
        hand.curveTo(16.5, 7.4, 20.5, 7.4, 20.5, 10);
        hand.curveTo(20.5, 8.4, 24.5, 8.4, 24.5, 11.5);
        hand.lineTo(24.5, 18);
        hand.curveTo(24.5, 22.5, 21.5, 24.5, 17.5, 24.5);
        hand.curveTo(13, 24.5, 11, 23, 9, 20);
        hand.lineTo(7, 17);
        hand.curveTo(5.8, 15, 6.8, 14, 8.5, 14.2);
        hand.closePath();
        return hand;
    }

    /**
     * @return the same hand, its fingers straight
     */
    private Path2D createOpenHandShape()
    {
        Path2D hand = new Path2D.Double();
        hand.moveTo(8.5, 16);
        hand.lineTo(8.5, 9);
        hand.curveTo(8.5, 6.4, 12.5, 6.4, 12.5, 9);
        hand.lineTo(12.5, 7);
        hand.curveTo(12.5, 4.4, 16.5, 4.4, 16.5, 7);
        hand.lineTo(16.5, 7.5);
        hand.curveTo(16.5, 4.9, 20.5, 4.9, 20.5, 7.5);
        hand.lineTo(20.5, 9.5);
        hand.curveTo(20.5, 6.9, 24.5, 6.9, 24.5, 9.5);
        hand.lineTo(24.5, 18);
        hand.curveTo(24.5, 23, 21.5, 25.5, 17.5, 25.5);
        hand.curveTo(13, 25.5, 11, 24, 9, 21);
        hand.lineTo(5.5, 16.5);
        hand.curveTo(4.5, 14.5, 6.5, 13.5, 8.5, 16);
        hand.closePath();
        return hand;
    }

    /** Side of the square the hands are drawn in, before they are scaled to the size of the system's cursors */
    private static final int BASE_SIZE = 32;

    private static final double CLOSED_FINGERS_BOTTOM = 14;

    private static final double OPEN_FINGERS_BOTTOM = 15;

    private Cursor closedHand;

    private Cursor openHand;

}
