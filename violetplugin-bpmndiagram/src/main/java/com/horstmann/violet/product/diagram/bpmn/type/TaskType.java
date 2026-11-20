package com.horstmann.violet.product.diagram.bpmn.type;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The kind of a BPMN task, shown by a marker in its top left corner. Saved by name.
 */
public enum TaskType
{
    NONE, USER, MANUAL, SERVICE, SCRIPT, SEND, RECEIVE, BUSINESS_RULE;

    /**
     * Draws the marker of this kind of task.
     *
     * @param g2 the graphics, its color set to the color of the marker
     * @param area the square the marker fits in
     * @param background the color inside the task
     * @param lineWidth the width of the outline of the task
     */
    public void drawMarker(Graphics2D g2, Rectangle2D area, Color background, float lineWidth)
    {
        Color color = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double x = area.getX();
        double y = area.getY();
        double s = Math.min(area.getWidth(), area.getHeight());
        switch (this)
        {
            case USER:
                fillAndDraw(g2, new Arc2D.Double(x + s * 0.1, y + s * 0.56, s * 0.8, s * 0.8, 0, 180, Arc2D.CHORD), color,
                        background);
                fillAndDraw(g2, new Ellipse2D.Double(x + s * 0.3, y + s * 0.08, s * 0.4, s * 0.4), color, background);
                break;
            case MANUAL:
                drawHand(g2, x, y, s, color, background);
                break;
            case SERVICE:
                drawGear(g2, x + s / 2, y + s / 2, s, color, background);
                break;
            case SCRIPT:
                drawScript(g2, x, y, s, color, background);
                break;
            case SEND:
                MARKERS.drawEnvelope(g2, x + s / 2, y + s / 2, s, true, color, background);
                break;
            case RECEIVE:
                MARKERS.drawEnvelope(g2, x + s / 2, y + s / 2, s, false, color, background);
                break;
            case BUSINESS_RULE:
                drawTable(g2, x, y, s, color, background);
                break;
            default:
                break;
        }
        g2.setColor(color);
        g2.setStroke(oldStroke);
    }

    private void drawHand(Graphics2D g2, double x, double y, double s, Color color, Color background)
    {
        Area hand = new Area(new RoundRectangle2D.Double(x + s * 0.05, y + s * 0.42, s * 0.5, s * 0.48, s * 0.2, s * 0.2));
        for (int i = 0; i < 4; i++)
        {
            double fingerY = y + s * (0.18 + i * 0.18);
            double fingerLength = i == 0 ? s * 0.5 : s * 0.42;
            hand.add(new Area(new RoundRectangle2D.Double(x + s * 0.4, fingerY, fingerLength, s * 0.16, s * 0.16, s * 0.16)));
        }
        fillAndDraw(g2, hand, color, background);
    }

    private void drawGear(Graphics2D g2, double cx, double cy, double s, Color color, Color background)
    {
        double radius = s * 0.32;
        Area gear = new Area(new Ellipse2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius));
        Rectangle2D tooth = new Rectangle2D.Double(cx - s * 0.08, cy - s * 0.47, s * 0.16, s * 0.94);
        for (int i = 0; i < 4; i++)
        {
            gear.add(new Area(AffineTransform.getRotateInstance(Math.PI * i / 4, cx, cy).createTransformedShape(tooth)));
        }
        double holeRadius = s * 0.13;
        gear.subtract(new Area(new Ellipse2D.Double(cx - holeRadius, cy - holeRadius, 2 * holeRadius, 2 * holeRadius)));
        fillAndDraw(g2, gear, color, background);
    }

    private void drawScript(Graphics2D g2, double x, double y, double s, Color color, Color background)
    {
        Path2D.Double page = new Path2D.Double();
        page.moveTo(x + s * 0.3, y + s * 0.08);
        page.lineTo(x + s * 0.92, y + s * 0.08);
        page.quadTo(x + s * 0.68, y + s * 0.3, x + s * 0.8, y + s * 0.5);
        page.quadTo(x + s * 0.92, y + s * 0.72, x + s * 0.7, y + s * 0.92);
        page.lineTo(x + s * 0.08, y + s * 0.92);
        page.quadTo(x + s * 0.32, y + s * 0.72, x + s * 0.2, y + s * 0.5);
        page.quadTo(x + s * 0.08, y + s * 0.3, x + s * 0.3, y + s * 0.08);
        page.closePath();
        fillAndDraw(g2, page, color, background);
        for (int i = 1; i <= 3; i++)
        {
            double lineY = y + s * (0.08 + i * 0.21);
            g2.draw(new Line2D.Double(x + s * 0.34, lineY, x + s * 0.68, lineY));
        }
    }

    private void drawTable(Graphics2D g2, double x, double y, double s, Color color, Color background)
    {
        Rectangle2D table = new Rectangle2D.Double(x + s * 0.05, y + s * 0.18, s * 0.9, s * 0.66);
        fillAndDraw(g2, table, color, background);
        g2.fill(new Rectangle2D.Double(table.getX(), table.getY(), table.getWidth(), table.getHeight() * 0.28));
        double rowY = table.getY() + table.getHeight() * 0.64;
        g2.draw(new Line2D.Double(table.getX(), rowY, table.getMaxX(), rowY));
        double columnX = table.getX() + table.getWidth() * 0.3;
        g2.draw(new Line2D.Double(columnX, table.getY(), columnX, table.getMaxY()));
    }

    private void fillAndDraw(Graphics2D g2, Shape shape, Color color, Color background)
    {
        g2.setColor(background);
        g2.fill(shape);
        g2.setColor(color);
        g2.draw(shape);
    }

    private static final EventMarkers MARKERS = new EventMarkers();

}
