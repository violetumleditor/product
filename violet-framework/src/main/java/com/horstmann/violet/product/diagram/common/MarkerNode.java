package com.horstmann.violet.product.diagram.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A stroke drawn freehand upon the other elements with a translucent highlighter.
 * <p>
 * It is always attached to the graph, drawn upon all other elements, found only along its stroke, never a parent and never linked.
 * Its color is the border color, made vivid and translucent, its width follows the thickness.
 * </p>
 */
public class MarkerNode extends RectangularNode
{
    public MarkerNode()
    {
        setBackgroundColor(DEFAULT_COLOR_CHOICE.getBackgroundColor());
        setBorderColor(DEFAULT_COLOR_CHOICE.getBorderColor());
        setBorderWidth(DEFAULT_THICKNESS);
    }

    /**
     * @return the prototype of the tool bar, drawn as an icon: clones and nodes loaded from files draw their stroke
     */
    public static MarkerNode createToolPrototype()
    {
        ResourceBundle rs = ResourceBundle.getBundle(ResourceBundleConstant.NODE_AND_EDGE_STRINGS, Locale.getDefault());
        MarkerNode prototype = new MarkerNode();
        prototype.setToolTip(rs.getString("markernode.tooltip"));
        prototype.toolIcon = true;
        return prototype;
    }

    /**
     * Sets the stroke and places this node on it. The location is put on the grid, the stroke itself isn't: moved later with other
     * nodes, which follow the grid, it stays at the same place upon them.
     *
     * @param pointsOnGraph the points the marker went through, at least one
     */
    public void setPointsOnGraph(List<Point2D> pointsOnGraph)
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (Point2D aPoint : pointsOnGraph)
        {
            minX = Math.min(minX, aPoint.getX());
            minY = Math.min(minY, aPoint.getY());
        }
        double margin = getStrokeWidth() / 2;
        Point2D newLocation = getGridPointBefore(new Point2D.Double(minX - margin, minY - margin));
        StringBuilder text = new StringBuilder();
        for (Point2D aPoint : pointsOnGraph)
        {
            text.append(text.length() == 0 ? "" : " ");
            text.append(round(aPoint.getX() - newLocation.getX())).append(',').append(round(aPoint.getY() - newLocation.getY()));
        }
        this.points = text.toString();
        this.parsedPoints = null;
        this.strokeShape = null;
        setLocation(newLocation);
    }

    /**
     * @return the points the marker went through
     */
    public List<Point2D> getPointsOnGraph()
    {
        Point2D location = getLocation();
        List<Point2D> pointsOnGraph = new ArrayList<Point2D>();
        for (Point2D aPoint : getParsedPoints())
        {
            pointsOnGraph.add(new Point2D.Double(location.getX() + aPoint.getX(), location.getY() + aPoint.getY()));
        }
        return pointsOnGraph;
    }

    /**
     * @return the closest point of the grid at the left and above the given one
     */
    private Point2D getGridPointBefore(Point2D p)
    {
        IGridSticker gridSticker = getGraph().getGridSticker();
        double x = p.getX();
        double y = p.getY();
        Point2D snapped = gridSticker.snap(new Point2D.Double(x, y));
        while (snapped.getX() > p.getX())
        {
            x--;
            snapped = gridSticker.snap(new Point2D.Double(x, y));
        }
        while (snapped.getY() > p.getY())
        {
            y--;
            snapped = gridSticker.snap(new Point2D.Double(x, y));
        }
        return new Point2D.Double(snapped.getX(), snapped.getY());
    }

    private double round(double value)
    {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * @return the points of the stroke, from the location of this node
     */
    private List<Point2D> getParsedPoints()
    {
        if (this.parsedPoints == null)
        {
            this.parsedPoints = new ArrayList<Point2D>();
            String text = this.points == null ? "" : this.points.trim();
            if (!text.isEmpty())
            {
                for (String aPoint : text.split("\\s+"))
                {
                    String[] coordinates = aPoint.split(",");
                    this.parsedPoints.add(new Point2D.Double(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1])));
                }
            }
        }
        return this.parsedPoints;
    }

    /**
     * The stroke as a single shape, so that its translucent color is the same where it crosses itself
     */
    private Shape getStrokeShape()
    {
        if (this.strokeShape == null)
        {
            Stroke stroke = new BasicStroke((float) getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            this.strokeShape = stroke.createStrokedShape(createPath());
        }
        return this.strokeShape;
    }

    /**
     * @return a smooth line along the points: curves between the middles of the segments
     */
    private Path2D createPath()
    {
        Path2D path = new Path2D.Double();
        List<Point2D> all = getParsedPoints();
        if (all.isEmpty())
        {
            return path;
        }
        path.moveTo(all.get(0).getX(), all.get(0).getY());
        for (int i = 1; i < all.size() - 1; i++)
        {
            Point2D p = all.get(i);
            Point2D next = all.get(i + 1);
            path.quadTo(p.getX(), p.getY(), (p.getX() + next.getX()) / 2, (p.getY() + next.getY()) / 2);
        }
        Point2D last = all.get(all.size() - 1);
        path.lineTo(last.getX(), last.getY());
        return path;
    }

    /**
     * @return the width of the marker on the graph
     */
    public double getStrokeWidth()
    {
        return getBorderWidth() * WIDTH_PER_THICKNESS;
    }

    /**
     * Keeps the stroke where it is, whatever its width
     */
    @Override
    public void setBorderWidth(int borderWidth)
    {
        if (this.points == null)
        {
            super.setBorderWidth(borderWidth);
            return; // also while this node is built, before a file gives its points
        }
        List<Point2D> pointsOnGraph = getPointsOnGraph();
        super.setBorderWidth(borderWidth);
        setPointsOnGraph(pointsOnGraph);
    }

    /**
     * @return the chosen color as a highlighter shows it: the colors of the palette are too pale once translucent
     */
    public Color getMarkerColor()
    {
        Color color = getBorderColor();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        if (hsb[1] >= GREY_SATURATION)
        {
            color = Color.getHSBColor(hsb[0], Math.max(hsb[1], MIN_SATURATION), 1);
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), ALPHA);
    }

    /**
     * @param p a point on the graph
     * @return true if the point lies on the stroke itself
     */
    public boolean isOnStroke(Point2D p)
    {
        double distance = Math.max(getStrokeWidth() / 2, MIN_HIT_DISTANCE);
        Rectangle2D bounds = getBounds();
        Rectangle2D largeBounds = new Rectangle2D.Double(bounds.getX() - distance, bounds.getY() - distance,
                bounds.getWidth() + 2 * distance, bounds.getHeight() + 2 * distance);
        if (!largeBounds.contains(p))
        {
            return false;
        }
        double x = p.getX() - getLocation().getX();
        double y = p.getY() - getLocation().getY();
        Point2D previous = null;
        for (Point2D aPoint : getParsedPoints())
        {
            Point2D start = previous == null ? aPoint : previous;
            if (Line2D.ptSegDist(start.getX(), start.getY(), aPoint.getX(), aPoint.getY(), x, y) <= distance)
            {
                return true;
            }
            previous = aPoint;
        }
        return false;
    }

    @Override
    public boolean contains(Point2D p)
    {
        return isOnStroke(p);
    }

    /**
     * Never linked to anything
     */
    @Override
    public boolean addConnection(IEdge e)
    {
        return false;
    }

    @Override
    public int getZ()
    {
        return ABOVE_FREE_TEXTS_Z_LEVEL;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        if (this.toolIcon)
        {
            return new Rectangle2D.Double(location.getX(), location.getY(), TOOL_ICON_SIZE, TOOL_ICON_SIZE);
        }
        double margin = getStrokeWidth() / 2;
        double width = margin;
        double height = margin;
        for (Point2D aPoint : getParsedPoints())
        {
            width = Math.max(width, aPoint.getX() + margin);
            height = Math.max(height, aPoint.getY() + margin);
        }
        return new Rectangle2D.Double(location.getX(), location.getY(), width, height);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        if (this.toolIcon)
        {
            drawToolIcon(g2);
            return;
        }
        Color oldColor = g2.getColor();
        g2.setColor(getMarkerColor());
        Point2D location = getLocation();
        g2.fill(AffineTransform.getTranslateInstance(location.getX(), location.getY()).createTransformedShape(getStrokeShape()));
        g2.setColor(oldColor);
    }

    /**
     * Draws a slanting highlighter above its stroke, in the color chosen in the palette
     */
    private void drawToolIcon(Graphics2D g2)
    {
        Color oldColor = g2.getColor();
        Point2D location = getLocation();
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(location.getX(), location.getY());
        g2.setColor(getBorderColor());
        g2.fill(new RoundRectangle2D.Double(0, 32, TOOL_ICON_SIZE, 8, 8, 8));
        g2.translate(14, 27);
        g2.rotate(Math.toRadians(40));
        g2.setColor(TOOL_ICON_BODY_COLOR);
        g2.fill(new RoundRectangle2D.Double(-6, -33, 12, 22, 5, 5));
        Path2D tip = new Path2D.Double();
        tip.moveTo(-6, -9);
        tip.lineTo(6, -9);
        tip.lineTo(3, -3);
        tip.lineTo(3, 2);
        tip.lineTo(-3, -1);
        tip.lineTo(-3, -3);
        tip.closePath();
        g2.fill(tip);
        g2.setTransform(oldTransform);
        g2.setColor(oldColor);
    }

    @Override
    public MarkerNode clone()
    {
        MarkerNode cloned = (MarkerNode) super.clone();
        cloned.toolIcon = false;
        cloned.parsedPoints = null;
        cloned.strokeShape = null;
        return cloned;
    }

    /** Points of the stroke from the location of this node: "x,y x,y..." */
    private String points;

    /** The points above, once read */
    private transient List<Point2D> parsedPoints;

    private transient Shape strokeShape;

    /** Only set on the tool bar prototype (never persisted) */
    private transient boolean toolIcon;

    private static final ColorChoice DEFAULT_COLOR_CHOICE = ColorChoice.PASTEL_YELLOW;
    private static final int DEFAULT_THICKNESS = 4;
    private static final double WIDTH_PER_THICKNESS = 4;
    private static final int ALPHA = 110;
    private static final float MIN_SATURATION = 0.9f;
    /** Under it, a color is a grey: it has no hue to make vivid */
    private static final float GREY_SATURATION = 0.05f;
    /** A thin stroke is found up to this distance */
    private static final double MIN_HIT_DISTANCE = 6;
    private static final int TOOL_ICON_SIZE = 40;
    private static final Color TOOL_ICON_BODY_COLOR = new Color(0x3C3C3C);

    /** Just above free texts, which are drawn at level 10001 */
    private static final int ABOVE_FREE_TEXTS_Z_LEVEL = 10002;
}
