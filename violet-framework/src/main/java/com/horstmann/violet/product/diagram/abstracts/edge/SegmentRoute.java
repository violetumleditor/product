package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.Direction;

/**
 * A path of horizontal and vertical segments in turn, between the bounds of two nodes. A segment is known by its fixed coordinate
 * on the graph (y of a horizontal segment, x of a vertical one), so moving one only changes that coordinate and its neighbors
 * stretch. {@link #adjust()} then makes the path fit its nodes again: an end segment moved past the corner of its node makes the
 * edge leave by the neighboring side; a segment brought upon the node makes the edge leave straight from there; two segments
 * brought in line become a single one.
 */
public class SegmentRoute
{

    /**
     * @param startBounds the bounds of the start node on the graph
     * @param endBounds the bounds of the end node on the graph
     * @param isFirstHorizontal true if the first segment is horizontal
     * @param coordinates the coordinate of each segment, at least one
     * @param minStartLength the length the first segment needs for its arrow head
     * @param minEndLength the length the last segment needs for its arrow head
     */
    public SegmentRoute(Rectangle2D startBounds, Rectangle2D endBounds, boolean isFirstHorizontal, List<Double> coordinates,
            double minStartLength, double minEndLength)
    {
        this.startBounds = startBounds;
        this.endBounds = endBounds;
        this.isFirstHorizontal = isFirstHorizontal;
        this.coordinates = new ArrayList<Double>(coordinates);
        this.minStartLength = Math.max(MARGIN, minStartLength);
        this.minEndLength = Math.max(MARGIN, minEndLength);
    }

    public int getSegmentCount()
    {
        return this.coordinates.size();
    }

    public boolean isHorizontal(int index)
    {
        return (index % 2 == 0) == this.isFirstHorizontal;
    }

    public double getCoordinate(int index)
    {
        return this.coordinates.get(index);
    }

    public List<Double> getCoordinates()
    {
        return new ArrayList<Double>(this.coordinates);
    }

    public Rectangle2D getStartBounds()
    {
        return this.startBounds;
    }

    public Rectangle2D getEndBounds()
    {
        return this.endBounds;
    }

    /**
     * @return a copy whose given segment is moved, then adjusted to its nodes
     */
    public SegmentRoute move(int index, double coordinate)
    {
        SegmentRoute moved = withCoordinate(index, coordinate);
        moved.adjust();
        return moved;
    }

    /**
     * @return a copy whose given segment is moved, not adjusted to its nodes
     */
    public SegmentRoute withCoordinate(int index, double coordinate)
    {
        SegmentRoute moved = new SegmentRoute(this.startBounds, this.endBounds, this.isFirstHorizontal, this.coordinates,
                this.minStartLength, this.minEndLength);
        moved.coordinates.set(index, coordinate);
        return moved;
    }

    /**
     * Makes the path fit its nodes
     */
    public void adjust()
    {
        for (int i = 0; i < MAX_PASSES; i++)
        {
            adjustStart(this.startBounds, this.endBounds, this.minStartLength);
            reverse();
            adjustStart(this.endBounds, this.startBounds, this.minEndLength);
            reverse();
            mergeAlignedSegments();
        }
    }

    /**
     * Keeps a path of a single segment in front of both nodes
     *
     * @return false if they don't face each other any more
     */
    public boolean keepSingleSegmentInFrontOfNodes()
    {
        if (getSegmentCount() > 1)
        {
            return true;
        }
        boolean isHorizontal = isHorizontal(0);
        double min = Math.max(getMin(this.startBounds, isHorizontal) + getMargin(this.startBounds, isHorizontal),
                getMin(this.endBounds, isHorizontal) + getMargin(this.endBounds, isHorizontal));
        double max = Math.min(getMax(this.startBounds, isHorizontal) - getMargin(this.startBounds, isHorizontal),
                getMax(this.endBounds, isHorizontal) - getMargin(this.endBounds, isHorizontal));
        if (min > max)
        {
            return false;
        }
        this.coordinates.set(0, Math.max(min, Math.min(max, getCoordinate(0))));
        return true;
    }

    /**
     * @return the points of the path, from the bounds of the start node to the bounds of the end node
     */
    public List<Point2D> getPoints()
    {
        List<Point2D> points = new ArrayList<Point2D>();
        int last = getSegmentCount() - 1;
        for (int i = 0; i < last; i++)
        {
            points.add(isHorizontal(i) ? new Point2D.Double(getCoordinate(i + 1), getCoordinate(i))
                    : new Point2D.Double(getCoordinate(i), getCoordinate(i + 1)));
        }
        Point2D afterStart = last > 0 ? points.get(0) : getCenter(this.endBounds);
        Point2D beforeEnd = last > 0 ? points.get(last - 1) : getCenter(this.startBounds);
        points.add(0, getPointOnBounds(this.startBounds, 0, afterStart));
        points.add(getPointOnBounds(this.endBounds, last, beforeEnd));
        return points;
    }

    /**
     * @return the direction of the edge at its start node, as {@link IEdge#getDirection} does: from the other node toward this one
     */
    public Direction getStartDirection()
    {
        List<Point2D> points = getPoints();
        return getDirection(points.get(1), points.get(0));
    }

    /**
     * @return the direction of the edge at its end node
     */
    public Direction getEndDirection()
    {
        List<Point2D> points = getPoints();
        return getDirection(points.get(points.size() - 2), points.get(points.size() - 1));
    }

    /**
     * @return true if both paths have the same segments, at the same places
     */
    public boolean isSamePath(SegmentRoute other)
    {
        if (other == null || other.isFirstHorizontal != this.isFirstHorizontal || other.getSegmentCount() != getSegmentCount())
        {
            return false;
        }
        for (int i = 0; i < getSegmentCount(); i++)
        {
            if (Math.abs(getCoordinate(i) - other.getCoordinate(i)) >= SAME_COORDINATE)
            {
                return false;
            }
        }
        return true;
    }

    private Direction getDirection(Point2D from, Point2D to)
    {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        if (Math.abs(dx) > Math.abs(dy))
        {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dy > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private void adjustStart(Rectangle2D bounds, Rectangle2D otherBounds, double minLength)
    {
        for (int i = 0; i < MAX_PASSES; i++)
        {
            boolean isHorizontal = this.isFirstHorizontal;
            double min = getMin(bounds, isHorizontal);
            double max = getMax(bounds, isHorizontal);
            double margin = getMargin(bounds, isHorizontal);
            double first = getCoordinate(0);
            if (first < min)
            {
                first = Math.min(first, min - MARGIN);
            }
            else if (first > max)
            {
                first = Math.max(first, max + MARGIN);
            }
            else
            {
                first = Math.max(min + margin, Math.min(max - margin, first));
            }
            this.coordinates.set(0, first);
            if (first < min || first > max)
            {
                // Past the corner: leaves by the neighboring side
                this.coordinates.add(0, isHorizontal ? bounds.getCenterX() : bounds.getCenterY());
                this.isFirstHorizontal = !isHorizontal;
                continue;
            }
            if (getSegmentCount() > 1 && !keepSecondSegmentOff(bounds, otherBounds, minLength))
            {
                // The second segment is upon the node: leaves straight by it
                this.coordinates.remove(0);
                this.isFirstHorizontal = !isHorizontal;
                continue;
            }
            return;
        }
    }

    /**
     * @return false if the second segment lies upon the node
     */
    private boolean keepSecondSegmentOff(Rectangle2D bounds, Rectangle2D otherBounds, double minLength)
    {
        double min = getMin(bounds, !this.isFirstHorizontal);
        double max = getMax(bounds, !this.isFirstHorizontal);
        double second = getCoordinate(1);
        if (second < min)
        {
            double gap = min - getMax(otherBounds, !this.isFirstHorizontal);
            this.coordinates.set(1, Math.min(second, min - getLengthInGap(minLength, gap)));
            return true;
        }
        if (second > max)
        {
            double gap = getMin(otherBounds, !this.isFirstHorizontal) - max;
            this.coordinates.set(1, Math.max(second, max + getLengthInGap(minLength, gap)));
            return true;
        }
        return false;
    }

    /**
     * @param gap the distance to the other node, when it is on this side
     * @return the length to keep, at most half of the gap
     */
    private double getLengthInGap(double minLength, double gap)
    {
        return gap > 0 ? Math.max(MARGIN, Math.min(minLength, gap / 2)) : minLength;
    }

    private void mergeAlignedSegments()
    {
        int i = 0;
        while (i + 2 < getSegmentCount())
        {
            if (Math.abs(getCoordinate(i) - getCoordinate(i + 2)) < SAME_COORDINATE)
            {
                this.coordinates.remove(i + 2);
                this.coordinates.remove(i + 1);
                i = Math.max(0, i - 2);
            }
            else
            {
                i++;
            }
        }
    }

    private void reverse()
    {
        this.isFirstHorizontal = isHorizontal(getSegmentCount() - 1);
        Collections.reverse(this.coordinates);
    }

    private Point2D getPointOnBounds(Rectangle2D bounds, int index, Point2D toward)
    {
        if (isHorizontal(index))
        {
            return new Point2D.Double(toward.getX() > bounds.getCenterX() ? bounds.getMaxX() : bounds.getMinX(),
                    getCoordinate(index));
        }
        return new Point2D.Double(getCoordinate(index),
                toward.getY() > bounds.getCenterY() ? bounds.getMaxY() : bounds.getMinY());
    }

    private Point2D getCenter(Rectangle2D bounds)
    {
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    /** Extent of the bounds along the coordinate of a horizontal (y) or vertical (x) segment */
    private double getMin(Rectangle2D bounds, boolean isHorizontal)
    {
        return isHorizontal ? bounds.getMinY() : bounds.getMinX();
    }

    private double getMax(Rectangle2D bounds, boolean isHorizontal)
    {
        return isHorizontal ? bounds.getMaxY() : bounds.getMaxX();
    }

    private double getMargin(Rectangle2D bounds, boolean isHorizontal)
    {
        return Math.min(MARGIN, (getMax(bounds, isHorizontal) - getMin(bounds, isHorizontal)) / 2);
    }

    private final Rectangle2D startBounds;

    private final Rectangle2D endBounds;

    private final double minStartLength;

    private final double minEndLength;

    private boolean isFirstHorizontal;

    private final List<Double> coordinates;

    /** Distance kept to the corners of a node by an end segment, and to its sides by the next one */
    private static final double MARGIN = 10;

    private static final double SAME_COORDINATE = 0.5;

    private static final int MAX_PASSES = 4;

}
