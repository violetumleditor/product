package com.horstmann.violet.product.diagram.bpmn;

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
 * What triggers a BPMN event (or what an end event throws), shown by a marker inside its circle. Saved by name.
 */
public enum EventTrigger
{
    NONE, MESSAGE, TIMER, SIGNAL, CONDITIONAL, ERROR, TERMINATE;

    /**
     * Draws the marker of this trigger.
     *
     * @param g2 the graphics, its color set to the color of the marker
     * @param area the square inside the event circle
     * @param filled true for a throwing event (an end event): the marker is filled with its color
     * @param background the color inside the event, used to draw the details of a filled marker
     * @param lineWidth the width of the outline of the event
     */
    public void drawMarker(Graphics2D g2, Rectangle2D area, boolean filled, Color background, float lineWidth)
    {
        Color color = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double size = Math.min(area.getWidth(), area.getHeight());
        double cx = area.getCenterX();
        double cy = area.getCenterY();
        switch (this)
        {
            case MESSAGE:
                drawEnvelope(g2, cx, cy, size, filled, color, background);
                break;
            case TIMER:
                drawClock(g2, cx, cy, size, color, background);
                break;
            case SIGNAL:
                fillOrDraw(g2, getTriangle(cx, cy, size), filled, color, background);
                break;
            case CONDITIONAL:
                drawConditionPage(g2, cx, cy, size, color, background);
                break;
            case ERROR:
                fillOrDraw(g2, getLightning(cx, cy, size), filled, color, background);
                break;
            case TERMINATE:
                double radius = size * 0.36;
                g2.fill(new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius));
                break;
            default:
                break;
        }
        g2.setColor(color);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws an envelope, also used by the send and receive tasks
     */
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

    private void drawClock(Graphics2D g2, double cx, double cy, double size, Color color, Color background)
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

    private void drawConditionPage(Graphics2D g2, double cx, double cy, double size, Color color, Color background)
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

    private Shape getTriangle(double cx, double cy, double size)
    {
        Path2D.Double triangle = new Path2D.Double();
        triangle.moveTo(cx, cy - size * 0.38);
        triangle.lineTo(cx + size * 0.36, cy + size * 0.26);
        triangle.lineTo(cx - size * 0.36, cy + size * 0.26);
        triangle.closePath();
        return triangle;
    }

    private Shape getLightning(double cx, double cy, double size)
    {
        double[] points = { -0.30, 0.34, -0.12, -0.30, 0.08, 0.06, 0.30, -0.34, 0.12, 0.30, -0.08, -0.06 };
        Path2D.Double lightning = new Path2D.Double();
        lightning.moveTo(cx + points[0] * size, cy + points[1] * size);
        for (int i = 2; i < points.length; i += 2)
        {
            lightning.lineTo(cx + points[i] * size, cy + points[i + 1] * size);
        }
        lightning.closePath();
        return lightning;
    }

    private void fillOrDraw(Graphics2D g2, Shape shape, boolean filled, Color color, Color background)
    {
        g2.setColor(filled ? color : background);
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

}
