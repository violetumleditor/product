package com.horstmann.violet.product.diagram.bpmn.type;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * What a BPMN end event produces when the process ends there (BPMN's "result": a message sent, a signal, an error, the end of
 * the whole process), shown by a marker inside its circle. Saved by name.
 */
public enum EventResultType
{
    NONE, MESSAGE, SIGNAL, ERROR, TERMINATE;

    /**
     * Draws the marker of this result.
     *
     * @param g2 the graphics, its color set to the color of the marker
     * @param area the square inside the event circle
     * @param filled true to fill the marker with its color, as the event throws it (on the diagram); false for its outline
     *            only (an icon on an unknown background)
     * @param background the color inside the event, used to draw the details of a filled marker
     * @param lineWidth the width of the outline of the event
     */
    public void drawMarker(Graphics2D g2, Rectangle2D area, boolean filled, Color background, float lineWidth)
    {
        Color color = g2.getColor();
        Stroke oldStroke = MARKERS.setMarkerStroke(g2, lineWidth);
        double size = Math.min(area.getWidth(), area.getHeight());
        double cx = area.getCenterX();
        double cy = area.getCenterY();
        switch (this)
        {
            case MESSAGE:
                MARKERS.drawEnvelope(g2, cx, cy, size, filled, color, background);
                break;
            case SIGNAL:
                MARKERS.drawTriangle(g2, cx, cy, size, filled, color, background);
                break;
            case ERROR:
                MARKERS.drawLightning(g2, cx, cy, size, filled, color, background);
                break;
            case TERMINATE:
                MARKERS.fillDisc(g2, cx, cy, size);
                break;
            default:
                break;
        }
        g2.setColor(color);
        g2.setStroke(oldStroke);
    }

    private static final EventMarkers MARKERS = new EventMarkers();

}
