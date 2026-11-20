package com.horstmann.violet.product.diagram.bpmn.type;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * What triggers a BPMN start event, shown by a marker inside its circle, drawn in outline as the event catches it. Saved by
 * name.
 */
public enum EventTriggerType
{
    NONE, MESSAGE, TIMER, SIGNAL, CONDITIONAL;

    /**
     * Draws the marker of this trigger.
     *
     * @param g2 the graphics, its color set to the color of the marker
     * @param area the square inside the event circle
     * @param background the color inside the event, filling the marker
     * @param lineWidth the width of the outline of the event
     */
    public void drawMarker(Graphics2D g2, Rectangle2D area, Color background, float lineWidth)
    {
        Color color = g2.getColor();
        Stroke oldStroke = MARKERS.setMarkerStroke(g2, lineWidth);
        double size = Math.min(area.getWidth(), area.getHeight());
        double cx = area.getCenterX();
        double cy = area.getCenterY();
        switch (this)
        {
            case MESSAGE:
                MARKERS.drawEnvelope(g2, cx, cy, size, false, color, background);
                break;
            case TIMER:
                MARKERS.drawClock(g2, cx, cy, size, color, background);
                break;
            case SIGNAL:
                MARKERS.drawTriangle(g2, cx, cy, size, false, color, background);
                break;
            case CONDITIONAL:
                MARKERS.drawConditionPage(g2, cx, cy, size, color, background);
                break;
            default:
                break;
        }
        g2.setColor(color);
        g2.setStroke(oldStroke);
    }

    private static final EventMarkers MARKERS = new EventMarkers();

}
