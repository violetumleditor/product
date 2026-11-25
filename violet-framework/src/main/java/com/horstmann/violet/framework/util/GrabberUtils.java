package com.horstmann.violet.framework.util;

import java.awt.geom.RoundRectangle2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.ResizeDirection;

/**
 * Utility class to draw "grabber", filled squares
 * 
 * @author HP-4720s
 *
 */
public class GrabberUtils
{

    /**
     * Draws a single "grabber", colored in purple
     * 
     * @param g2 the graphics context
     * @param x the x coordinate of the center of the grabber
     * @param y the y coordinate of the center of the grabber
     */
    public static void drawPurpleGrabber(Graphics2D g2, double x, double y)
    {
        Color oldColor = g2.getColor();
        g2.setColor(GrabberUtils.PURPLE);
        g2.fill(new Rectangle2D.Double(x - GrabberUtils.GRABBER_WIDTH / 2, y - GrabberUtils.GRABBER_WIDTH / 2, GrabberUtils.GRABBER_WIDTH, GrabberUtils.GRABBER_WIDTH));
        g2.setColor(oldColor);
    }

    /**
     * Draws a single "grabber", colored in purple
     * 
     * @param g2 the graphics context
     * @param p the grabber point location
     */
    public static void drawPurpleGrabber(Graphics2D g2, Point2D p) {
    	drawPurpleGrabber(g2, p.getX(), p.getY());
    }
    
    
    /**
     * Draws a single "grabber", colored in gray
     * 
     * @param g2 the graphics context
     * @param x the x coordinate of the center of the grabber
     * @param y the y coordinate of the center of the grabber
     */
    public static void drawGrayGrabber(Graphics2D g2, double x, double y)
    {
        Color oldColor = g2.getColor();
        g2.setColor(GrabberUtils.GRAY);
        g2.fill(new Rectangle2D.Double(x - GrabberUtils.GRABBER_WIDTH / 2, y - GrabberUtils.GRABBER_WIDTH / 2, GrabberUtils.GRABBER_WIDTH, GrabberUtils.GRABBER_WIDTH));
        g2.setColor(oldColor);
    }
    
    /**
     * Draws a single "grabber", colored in gray
     * 
     * @param g2 the graphics context
     * @param p the grabber point location
     */
    public static void drawGrayGrabber(Graphics2D g2, Point2D p) {
    	drawGrayGrabber(g2, p.getX(), p.getY());
    }
    
    
    
    /**
     * Draws a directional crop handle at the given point.
     * Uses a teal (cyan-green) colour to distinguish crop handles from resize
     * handles.  Edge directions (N/S = horizontal bar, W/E = vertical bar)
     * are rendered as a short, thick dash with arrow-like notches pointing
     * inward to indicate the crop boundary.
     *
     * @param g2        the graphics context
     * @param p         the centre of the handle
     * @param direction the crop edge this handle represents (N, S, W, or E)
     */
    public static void drawTealCropGrabber(Graphics2D g2, Point2D p, ResizeDirection direction)
    {
        Color oldColor  = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(TEAL);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        int arm = GRABBER_WIDTH * 2;
        int cx  = (int) p.getX();
        int cy  = (int) p.getY();
        switch (direction)
        {
            case N: // horizontal bar + inward tick pointing down
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy, cx, cy + arm / 2);
                break;
            case S: // horizontal bar + inward tick pointing up
                g2.drawLine(cx - arm, cy, cx + arm, cy);
                g2.drawLine(cx, cy - arm / 2, cx, cy);
                break;
            case W: // vertical bar + inward tick pointing right
                g2.drawLine(cx, cy - arm, cx, cy + arm);
                g2.drawLine(cx, cy, cx + arm / 2, cy);
                break;
            case E: // vertical bar + inward tick pointing left
                g2.drawLine(cx, cy - arm, cx, cy + arm);
                g2.drawLine(cx - arm / 2, cy, cx, cy);
                break;
            default: break;
        }
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    /**
     * Draws a resize handle: a white square bordered in violet, as in most drawing tools; under the mouse (or dragged), a larger
     * violet square in a soft halo. Its size is the same on screen whatever the zoom.
     *
     * @param g2 the diagram graphics (graph coordinates, scaled by the zoom)
     * @param p the center of the handle (the corner of the node)
     * @param zoom the zoom factor of the editor
     * @param hovered true if the mouse is on it, or if it is dragged
     */
    public static void drawResizeHandle(Graphics2D g2, Point2D p, double zoom, boolean hovered)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        if (hovered)
        {
            drawHalo(g2, p, HANDLE_HALO_DIAMETER / zoom, PURPLE_HALO);
        }
        double size = (hovered ? HOVERED_HANDLE_SIZE : HANDLE_SIZE) / zoom;
        Rectangle2D square = new Rectangle2D.Double(p.getX() - size / 2, p.getY() - size / 2, size, size);
        g2.setColor(hovered ? PURPLE : Color.WHITE);
        g2.fill(square);
        g2.setColor(hovered ? Color.WHITE : PURPLE);
        g2.setStroke(new BasicStroke((float) (HANDLE_BORDER / zoom)));
        g2.draw(square);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Draws the soft halo of a crop handle under the mouse (or dragged), around the handle drawn by
     * {@link #drawTealCropGrabber(Graphics2D, Point2D, ResizeDirection)}
     *
     * @param g2 the diagram graphics (graph coordinates, scaled by the zoom)
     * @param p the center of the handle
     * @param zoom the zoom factor of the editor
     */
    public static void drawHoveredCropHandle(Graphics2D g2, Point2D p, double zoom)
    {
        Color oldColor = g2.getColor();
        drawHalo(g2, p, HANDLE_HALO_DIAMETER / zoom, TEAL_HALO);
        g2.setColor(oldColor);
    }

    /**
     * Draws a transition point of an edge as a round handle: white bordered in violet, filled in violet when it is selected, larger
     * in a soft halo under the mouse. Its size is the same on screen whatever the zoom.
     *
     * @param g2 the diagram graphics (graph coordinates, scaled by the zoom)
     * @param p the transition point
     * @param zoom the zoom factor of the editor
     * @param selected true if this point is selected
     * @param hovered true if the mouse is on it
     */
    public static void drawTransitionPointHandle(Graphics2D g2, Point2D p, double zoom, boolean selected, boolean hovered)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        if (hovered)
        {
            drawHalo(g2, p, HANDLE_HALO_DIAMETER / zoom, PURPLE_HALO);
        }
        double diameter = (hovered ? HOVERED_HANDLE_SIZE : HANDLE_SIZE) / zoom;
        Ellipse2D circle = new Ellipse2D.Double(p.getX() - diameter / 2, p.getY() - diameter / 2, diameter, diameter);
        boolean isFilled = selected || hovered;
        g2.setColor(isFilled ? PURPLE : Color.WHITE);
        g2.fill(circle);
        g2.setColor(isFilled ? Color.WHITE : PURPLE);
        g2.setStroke(new BasicStroke((float) (HANDLE_BORDER / zoom)));
        g2.draw(circle);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Draws the handle of a draggable segment of an edge: a bar along the segment, white bordered in violet; filled in violet and
     * larger under the mouse (or dragged). Its size is the same on screen whatever the zoom.
     *
     * @param g2 the diagram graphics (graph coordinates, scaled by the zoom)
     * @param p the middle of the segment
     * @param zoom the zoom factor of the editor
     * @param isHorizontal true for a horizontal segment
     * @param hovered true if the mouse is on the segment, or if it is dragged
     */
    public static void drawSegmentHandle(Graphics2D g2, Point2D p, double zoom, boolean isHorizontal, boolean hovered)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        if (hovered)
        {
            drawHalo(g2, p, HANDLE_HALO_DIAMETER / zoom, PURPLE_HALO);
        }
        double length = (hovered ? HOVERED_SEGMENT_HANDLE_LENGTH : SEGMENT_HANDLE_LENGTH) / zoom;
        double thickness = (hovered ? HANDLE_SIZE : SEGMENT_HANDLE_THICKNESS) / zoom;
        double width = isHorizontal ? length : thickness;
        double height = isHorizontal ? thickness : length;
        RoundRectangle2D bar = new RoundRectangle2D.Double(p.getX() - width / 2, p.getY() - height / 2, width, height, thickness,
                thickness);
        g2.setColor(hovered ? PURPLE : Color.WHITE);
        g2.fill(bar);
        g2.setColor(hovered ? Color.WHITE : PURPLE);
        g2.setStroke(new BasicStroke((float) (HANDLE_BORDER / zoom)));
        g2.draw(bar);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private static void drawHalo(Graphics2D g2, Point2D p, double diameter, Color color)
    {
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(p.getX() - diameter / 2, p.getY() - diameter / 2, diameter, diameter));
    }

    /** Size on screen of a resize or transition point handle */
    private static final double HANDLE_SIZE = 8;

    /** Size on screen of a handle under the mouse */
    private static final double HOVERED_HANDLE_SIZE = 11;

    /** Diameter on screen of the halo of a handle under the mouse */
    private static final double HANDLE_HALO_DIAMETER = 26;

    private static final double HANDLE_BORDER = 1.5;

    private static final double SEGMENT_HANDLE_LENGTH = 16;

    private static final double HOVERED_SEGMENT_HANDLE_LENGTH = 22;

    private static final double SEGMENT_HANDLE_THICKNESS = 6;

    private static final Color PURPLE_HALO = new Color(179, 102, 179, 70);

    private static final Color TEAL_HALO = new Color(0, 153, 128, 70);

    private static final Color TEAL   = new Color(0.0f, 0.6f, 0.5f);
    private static final Color PURPLE = new Color(0.7f, 0.4f, 0.7f);
    private static final Color GRAY = Color.GRAY.brighter();
    public static final int GRABBER_WIDTH = 5;

}
