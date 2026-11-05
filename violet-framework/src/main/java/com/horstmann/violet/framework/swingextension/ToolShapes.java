package com.horstmann.violet.framework.swingextension;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Outlines shared by the menus and the tool bar, on a 24x24 grid, to be displayed with a {@link ShapeIcon}.
 */
public final class ToolShapes
{

    public static final Shape NEW = newShape();
    public static final Shape OPEN = openShape();
    public static final Shape SAVE = saveShape();
    public static final Shape SAVE_AS = saveAsShape();
    public static final Shape EXPORT = exportShape();
    public static final Shape PRINT = printShape();
    public static final Shape UNDO = undoShape();
    public static final Shape REDO = new AffineTransform(-1, 0, 0, 1, 24, 0).createTransformedShape(UNDO);
    public static final Shape PROPERTIES = propertiesShape();
    public static final Shape CUT = cutShape();
    public static final Shape COPY = copyShape();
    public static final Shape PASTE = pasteShape();
    public static final Shape DELETE = deleteShape();
    public static final Shape ZOOM_OUT = zoomOutShape();
    public static final Shape ZOOM_IN = zoomInShape();
    public static final Shape DOT = new Ellipse2D.Double(8, 8, 8, 8);
    public static final Shape SUN = sunShape();
    public static final Shape MOON = moonShape();

    private static final Map<String, Shape> SHAPES_BY_NAME = new HashMap<String, Shape>();

    static
    {
        SHAPES_BY_NAME.put("new", NEW);
        SHAPES_BY_NAME.put("open", OPEN);
        SHAPES_BY_NAME.put("save", SAVE);
        SHAPES_BY_NAME.put("saveas", SAVE_AS);
        SHAPES_BY_NAME.put("export", EXPORT);
        SHAPES_BY_NAME.put("print", PRINT);
        SHAPES_BY_NAME.put("undo", UNDO);
        SHAPES_BY_NAME.put("redo", REDO);
        SHAPES_BY_NAME.put("properties", PROPERTIES);
        SHAPES_BY_NAME.put("cut", CUT);
        SHAPES_BY_NAME.put("copy", COPY);
        SHAPES_BY_NAME.put("paste", PASTE);
        SHAPES_BY_NAME.put("delete", DELETE);
        SHAPES_BY_NAME.put("zoomout", ZOOM_OUT);
        SHAPES_BY_NAME.put("zoomin", ZOOM_IN);
    }

    private ToolShapes()
    {
        // Constants only
    }

    /**
     * @param name as written in properties files
     * @return the matching shape
     */
    public static Shape get(String name)
    {
        Shape shape = SHAPES_BY_NAME.get(name);
        if (shape == null)
        {
            throw new IllegalArgumentException("Unknown tool shape: " + name);
        }
        return shape;
    }

    private static Shape newShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(14, 2);
        path.lineTo(6, 2);
        path.quadTo(4, 2, 4, 4);
        path.lineTo(4, 20);
        path.quadTo(4, 22, 6, 22);
        path.lineTo(18, 22);
        path.quadTo(20, 22, 20, 20);
        path.lineTo(20, 8);
        path.closePath();
        path.moveTo(14, 2);
        path.lineTo(14, 8);
        path.lineTo(20, 8);
        return path;
    }

    private static Shape openShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(5, 20);
        path.quadTo(3, 20, 3, 18);
        path.lineTo(3, 6);
        path.quadTo(3, 4, 5, 4);
        path.lineTo(9, 4);
        path.lineTo(12, 7);
        path.lineTo(19, 7);
        path.quadTo(21, 7, 21, 9);
        path.lineTo(21, 18);
        path.quadTo(21, 20, 19, 20);
        path.closePath();
        return path;
    }

    private static Shape saveShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(5, 3);
        path.lineTo(16, 3);
        path.lineTo(21, 8);
        path.lineTo(21, 19);
        path.quadTo(21, 21, 19, 21);
        path.lineTo(5, 21);
        path.quadTo(3, 21, 3, 19);
        path.lineTo(3, 5);
        path.quadTo(3, 3, 5, 3);
        path.closePath();
        path.moveTo(7, 21);
        path.lineTo(7, 13);
        path.lineTo(17, 13);
        path.lineTo(17, 21);
        path.moveTo(7, 3);
        path.lineTo(7, 8);
        path.lineTo(15, 8);
        return path;
    }

    private static Shape saveAsShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(11, 21);
        path.lineTo(5, 21);
        path.quadTo(3, 21, 3, 19);
        path.lineTo(3, 5);
        path.quadTo(3, 3, 5, 3);
        path.lineTo(16, 3);
        path.lineTo(21, 8);
        path.lineTo(21, 11);
        path.moveTo(7, 3);
        path.lineTo(7, 8);
        path.lineTo(15, 8);
        path.moveTo(7, 21);
        path.lineTo(7, 13);
        path.lineTo(13, 13);
        path.moveTo(14, 22);
        path.lineTo(14.5, 18.5);
        path.lineTo(20, 13);
        path.lineTo(23, 16);
        path.lineTo(17.5, 21.5);
        path.closePath();
        return path;
    }

    private static Shape exportShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(12, 15);
        path.lineTo(12, 3);
        path.moveTo(7, 8);
        path.lineTo(12, 3);
        path.lineTo(17, 8);
        path.moveTo(4, 15);
        path.lineTo(4, 19);
        path.quadTo(4, 21, 6, 21);
        path.lineTo(18, 21);
        path.quadTo(20, 21, 20, 19);
        path.lineTo(20, 15);
        return path;
    }

    private static Shape printShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(6, 9);
        path.lineTo(6, 2);
        path.lineTo(18, 2);
        path.lineTo(18, 9);
        path.moveTo(6, 18);
        path.lineTo(4, 18);
        path.quadTo(2, 18, 2, 16);
        path.lineTo(2, 11);
        path.quadTo(2, 9, 4, 9);
        path.lineTo(20, 9);
        path.quadTo(22, 9, 22, 11);
        path.lineTo(22, 16);
        path.quadTo(22, 18, 20, 18);
        path.lineTo(18, 18);
        path.append(new Rectangle2D.Double(6, 14, 12, 8), false);
        return path;
    }

    private static Shape undoShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(9, 14);
        path.lineTo(4, 9);
        path.lineTo(9, 4);
        path.moveTo(4, 9);
        path.lineTo(14.5, 9);
        path.append(new Arc2D.Double(9, 9, 11, 11, 90, -180, Arc2D.OPEN), true);
        path.lineTo(11, 20);
        return path;
    }

    private static Shape propertiesShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Line2D.Double(3, 6, 12, 6), false);
        path.append(new Ellipse2D.Double(12, 3.5, 5, 5), false);
        path.append(new Line2D.Double(17, 6, 21, 6), false);
        path.append(new Line2D.Double(3, 12, 6, 12), false);
        path.append(new Ellipse2D.Double(6, 9.5, 5, 5), false);
        path.append(new Line2D.Double(11, 12, 21, 12), false);
        path.append(new Line2D.Double(3, 18, 14, 18), false);
        path.append(new Ellipse2D.Double(14, 15.5, 5, 5), false);
        path.append(new Line2D.Double(19, 18, 21, 18), false);
        return path;
    }

    private static Shape cutShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 6, 6), false);
        path.append(new Ellipse2D.Double(3, 15, 6, 6), false);
        path.append(new Line2D.Double(20, 4, 8.12, 15.88), false);
        path.append(new Line2D.Double(14.47, 14.48, 20, 20), false);
        path.append(new Line2D.Double(8.12, 8.12, 12, 12), false);
        return path;
    }

    private static Shape copyShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new RoundRectangle2D.Double(8, 8, 14, 14, 4, 4), false);
        path.moveTo(4, 16);
        path.quadTo(2, 16, 2, 14);
        path.lineTo(2, 4);
        path.quadTo(2, 2, 4, 2);
        path.lineTo(14, 2);
        path.quadTo(16, 2, 16, 4);
        return path;
    }

    private static Shape pasteShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new RoundRectangle2D.Double(8, 2, 8, 4, 2, 2), false);
        path.moveTo(16, 4);
        path.lineTo(18, 4);
        path.quadTo(20, 4, 20, 6);
        path.lineTo(20, 20);
        path.quadTo(20, 22, 18, 22);
        path.lineTo(6, 22);
        path.quadTo(4, 22, 4, 20);
        path.lineTo(4, 6);
        path.quadTo(4, 4, 6, 4);
        path.lineTo(8, 4);
        return path;
    }

    private static Shape deleteShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Line2D.Double(3, 6, 21, 6), false);
        path.moveTo(19, 6);
        path.lineTo(19, 20);
        path.quadTo(19, 22, 17, 22);
        path.lineTo(7, 22);
        path.quadTo(5, 22, 5, 20);
        path.lineTo(5, 6);
        path.moveTo(8, 6);
        path.lineTo(8, 4);
        path.quadTo(8, 2, 10, 2);
        path.lineTo(14, 2);
        path.quadTo(16, 2, 16, 4);
        path.lineTo(16, 6);
        path.append(new Line2D.Double(10, 11, 10, 17), false);
        path.append(new Line2D.Double(14, 11, 14, 17), false);
        return path;
    }

    private static Shape zoomOutShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 16, 16), false);
        path.append(new Line2D.Double(21, 21, 16.65, 16.65), false);
        path.append(new Line2D.Double(8, 11, 14, 11), false);
        return path;
    }

    private static Shape zoomInShape()
    {
        Path2D path = new Path2D.Double(zoomOutShape());
        path.append(new Line2D.Double(11, 8, 11, 14), false);
        return path;
    }

    private static Shape sunShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(7.5, 7.5, 9, 9), false);
        for (int i = 0; i < 8; i++)
        {
            AffineTransform rotation = AffineTransform.getRotateInstance(Math.toRadians(45 * i), 12, 12);
            path.append(rotation.createTransformedShape(new Line2D.Double(12, 2, 12, 4.5)), false);
        }
        return path;
    }

    private static Shape moonShape()
    {
        Area crescent = new Area(new Ellipse2D.Double(3, 3, 18, 18));
        crescent.subtract(new Area(new Ellipse2D.Double(10, 0, 15, 15)));
        return crescent;
    }

}
