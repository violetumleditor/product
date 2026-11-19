package com.horstmann.violet.product.diagram.bpmn;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The kind of a BPMN gateway, shown by a marker inside its diamond. Saved by name.
 */
public enum GatewayType
{
    EXCLUSIVE, PARALLEL, INCLUSIVE, EVENT_BASED;

    /**
     * Draws the marker of this kind of gateway.
     *
     * @param g2 the graphics, its color set to the color of the marker
     * @param area the bounds of the diamond
     * @param lineWidth the width of the outline of the gateway
     */
    public void drawMarker(Graphics2D g2, Rectangle2D area, float lineWidth)
    {
        Stroke oldStroke = g2.getStroke();
        double s = Math.min(area.getWidth(), area.getHeight());
        double cx = area.getCenterX();
        double cy = area.getCenterY();
        switch (this)
        {
            case EXCLUSIVE:
                g2.setStroke(new BasicStroke(lineWidth * 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                double d = s * 0.15;
                g2.draw(new Line2D.Double(cx - d, cy - d, cx + d, cy + d));
                g2.draw(new Line2D.Double(cx - d, cy + d, cx + d, cy - d));
                break;
            case PARALLEL:
                g2.setStroke(new BasicStroke(lineWidth * 3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                double l = s * 0.22;
                g2.draw(new Line2D.Double(cx - l, cy, cx + l, cy));
                g2.draw(new Line2D.Double(cx, cy - l, cx, cy + l));
                break;
            case INCLUSIVE:
                g2.setStroke(new BasicStroke(lineWidth * 2.5f));
                drawCircle(g2, cx, cy, s * 0.21);
                break;
            case EVENT_BASED:
                g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawCircle(g2, cx, cy, s * 0.25);
                drawCircle(g2, cx, cy, s * 0.21);
                drawPentagon(g2, cx, cy, s * 0.13);
                break;
            default:
                break;
        }
        g2.setStroke(oldStroke);
    }

    private void drawCircle(Graphics2D g2, double cx, double cy, double radius)
    {
        g2.draw(new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius));
    }

    private void drawPentagon(Graphics2D g2, double cx, double cy, double radius)
    {
        Path2D.Double pentagon = new Path2D.Double();
        for (int i = 0; i < 5; i++)
        {
            double angle = -Math.PI / 2 + Math.PI * 2 * i / 5;
            double x = cx + Math.cos(angle) * radius;
            double y = cy + Math.sin(angle) * radius;
            if (i == 0)
            {
                pentagon.moveTo(x, y);
            }
            else
            {
                pentagon.lineTo(x, y);
            }
        }
        pentagon.closePath();
        g2.draw(pentagon);
    }

}
