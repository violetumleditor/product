package com.horstmann.violet.product.diagram.bpmn.type;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Draws the markers shared by the triggers of start events, the results of end events and the send / receive tasks. Each
 * marker is centered on (cx, cy) and sized after size, the side of the square it fits in.
 */
class EventMarkers
{

    /**
     * Sets the round stroke of the markers
     *
     * @return the former stroke, to put back with {@link Graphics2D#setStroke(Stroke)}
     */
    Stroke setMarkerStroke(Graphics2D g2, float lineWidth)
    {
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        return oldStroke;
    }

    void drawEnvelope(Graphics2D g2, double cx, double cy, double size, boolean filled, Color color, Color background)
    {
        double width = size * 0.72;
        double height = size * 0.5;
        double x = cx - width / 2;
        double y = cy - height / 2;
        fillOrDraw(g2, new Rectangle2D.Double(x, y, width, height), filled, color, background);
        Path2D.Double flap = new Path2D.Double();
        flap.moveTo(x, y);
        flap.lineTo(cx, y + height * 0.55);
        flap.lineTo(x + width, y);
        g2.setColor(filled ? background : color);
        g2.draw(flap);
        g2.setColor(color);
    }

    void drawClock(Graphics2D g2, double cx, double cy, double size, Color color, Color background)
    {
        double radius = size * 0.42;
        fillOrDraw(g2, new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius), false, color, background);
        for (int i = 0; i < 12; i++)
        {
            double angle = Math.PI * 2 * i / 12;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            g2.draw(new Line2D.Double(cx + cos * radius * 0.75, cy + sin * radius * 0.75, cx + cos * radius * 0.9, cy + sin
                    * radius * 0.9));
        }
        g2.draw(new Line2D.Double(cx, cy, cx, cy - radius * 0.6));
        g2.draw(new Line2D.Double(cx, cy, cx + radius * 0.45, cy));
    }

    void drawConditionPage(Graphics2D g2, double cx, double cy, double size, Color color, Color background)
    {
        double width = size * 0.5;
        double height = size * 0.66;
        double x = cx - width / 2;
        double y = cy - height / 2;
        fillOrDraw(g2, new Rectangle2D.Double(x, y, width, height), false, color, background);
        for (int i = 1; i <= 4; i++)
        {
            double lineY = y + height * i / 5;
            g2.draw(new Line2D.Double(x + width * 0.2, lineY, x + width * 0.8, lineY));
        }
    }

    void drawTriangle(Graphics2D g2, double cx, double cy, double size, boolean filled, Color color, Color background)
    {
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(cx, cy - size * 0.38);
        triangle.lineTo(cx + size * 0.36, cy + size * 0.26);
        triangle.lineTo(cx - size * 0.36, cy + size * 0.26);
        triangle.closePath();
        fillOrDraw(g2, triangle, filled, color, background);
    }

    void drawLightning(Graphics2D g2, double cx, double cy, double size, boolean filled, Color color, Color background)
    {
        double[] points = { -0.30, 0.34, -0.12, -0.30, 0.08, 0.06, 0.30, -0.34, 0.12, 0.30, -0.08, -0.06 };
        Path2D.Double lightning = new Path2D.Double();
        lightning.moveTo(cx + points[0] * size, cy + points[1] * size);
        for (int i = 2; i < points.length; i += 2)
        {
            lightning.lineTo(cx + points[i] * size, cy + points[i + 1] * size);
        }
        lightning.closePath();
        fillOrDraw(g2, lightning, filled, color, background);
    }

    void fillDisc(Graphics2D g2, double cx, double cy, double size)
    {
        double radius = size * 0.36;
        g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius));
    }

    private void fillOrDraw(Graphics2D g2, Shape shape, boolean filled, Color color, Color background)
    {
        g2.setColor(filled ? color : background);
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

}
