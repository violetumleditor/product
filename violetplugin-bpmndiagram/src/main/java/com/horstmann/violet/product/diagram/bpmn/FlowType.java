package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * The kind of a BPMN sequence flow: normal, conditional (a small diamond where it starts, taken when its condition holds) or
 * default (a slash where it starts, taken when no other flow of the gateway or activity is). Saved by name.
 */
public enum FlowType
{
    NORMAL, CONDITIONAL, DEFAULT;

    /**
     * Draws the slash of a default flow, across its first segment
     *
     * @param g2 the graphics, with the color and the stroke of the flow
     * @param start where the flow starts
     * @param next the end of its first segment
     * @param scale the width of the flow
     */
    void drawDefaultMarker(Graphics2D g2, Point2D start, Point2D next, double scale)
    {
        double dx = next.getX() - start.getX();
        double dy = next.getY() - start.getY();
        double length = Math.hypot(dx, dy);
        if (length < 1)
        {
            return;
        }
        double ux = dx / length;
        double uy = dy / length;
        double distance = Math.min(SLASH_DISTANCE * scale, length / 2);
        double cx = start.getX() + ux * distance;
        double cy = start.getY() + uy * distance;
        // Across the line, leaning toward the end of the flow
        double half = SLASH_HALF_LENGTH * scale;
        double ax = (-uy + ux * 0.6) * half;
        double ay = (ux + uy * 0.6) * half;
        g2.draw(new Line2D.Double(cx - ax, cy - ay, cx + ax, cy + ay));
    }

    private static final double SLASH_DISTANCE = 12;
    private static final double SLASH_HALF_LENGTH = 6;

}
