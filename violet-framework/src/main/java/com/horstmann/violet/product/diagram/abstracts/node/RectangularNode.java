/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.product.diagram.abstracts.node;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.AbstractEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.EndSeparator;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentRoute;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;

/**
 * A node that has a rectangular shape.
 */
public abstract class RectangularNode extends AbstractNode
{

    public RectangularNode()
    {
        super();
    }

    public boolean contains(Point2D p)
    {
        return getBounds().contains(p);
    }
    
    @Override
    public boolean addConnection(IEdge e) {
    	// Self call (loop)
    	INode endingNode = e.getEndNode();
    	if (endingNode == null) {
    		e.setEndNode(e.getStartNode());
    		e.setEndLocation(e.getStartLocation());
    	}
    	// Back to default behavior
    	return super.addConnection(e);
    }

    
    /**
     * List edges connected to the same side
     * 
     * @param edge
     * @return ordered list of edges 
     */
    private List<IEdge> getEdgesOnSameSide(IEdge edge) {
        // Step 1 : look for edges
        List<IEdge> result = new ArrayList<IEdge>();
        Direction d = edge.getDirection(this);

        if (d == null) return result;
        Direction cardinalDirectionToSearch = d.getNearestCardinalDirection();
        for (IEdge anEdge : getConnectedEdges()) {
            Direction edgeDirection = anEdge.getDirection(this);
            Direction nearestCardinalDirection = edgeDirection.getNearestCardinalDirection();
            if (cardinalDirectionToSearch.equals(nearestCardinalDirection)) {
                result.add(anEdge);
            }
            if (anEdge.getStartNode().equals(anEdge.getEndNode()) && anEdge.getStartNode().equals(this)) {
            	// self loop
            	result.add(anEdge);
            }
        }
        // Step 2: sort them
        if (Direction.NORTH.equals(cardinalDirectionToSearch) || Direction.SOUTH.equals(cardinalDirectionToSearch)) {
            Collections.sort(result, new Comparator<IEdge>() {
                @Override
                public int compare(IEdge e1, IEdge e2) {
                    Direction d1 = e1.getDirection(RectangularNode.this);
                    Direction d2 = e2.getDirection(RectangularNode.this);
                    double x1 = d1.getX();
                    double x2 = d2.getX();
                    return Double.compare(x1, x2);
                }
            });
        }
        if (Direction.EAST.equals(cardinalDirectionToSearch) || Direction.WEST.equals(cardinalDirectionToSearch)) {
            Collections.sort(result, new Comparator<IEdge>() {
                @Override
                public int compare(IEdge e1, IEdge e2) {
                    Direction d1 = e1.getDirection(RectangularNode.this);
                    Direction d2 = e2.getDirection(RectangularNode.this);
                    double y1 = d1.getY();
                    double y2 = d2.getY();
                    return Double.compare(y1, y2);
                }
            });
        }
        return result;
    }
    
    
    
    public Point2D getConnectionPoint(IEdge e)
    {
        Point2D point = getConnectionPointOnSide(e);
        if (!hasAdaptedEdges())
        {
            return point;
        }
        // The automatic ends are placed one after the other, off the adapted ones and each other
        List<IEdge> automaticEdges = new ArrayList<IEdge>();
        List<Point2D> points = new ArrayList<Point2D>();
        for (IEdge anEdge : getEdgesOnSameSide(e))
        {
            if (!isPathAdapted(anEdge) && !automaticEdges.contains(anEdge))
            {
                automaticEdges.add(anEdge);
                points.add(getConnectionPointOnSide(anEdge));
            }
        }
        for (int i = 0; i < automaticEdges.size(); i++)
        {
            List<Point2D> otherPoints = new ArrayList<Point2D>(points);
            otherPoints.remove(i);
            points.set(i, keepOffAdaptedEdges(automaticEdges.get(i), points.get(i), otherPoints));
            if (automaticEdges.get(i).equals(e))
            {
                return points.get(i);
            }
        }
        return point;
    }

    /**
     * @return true if the node gives its automatic edges fixed connection points, that the edges adapted by the user keep off. Such
     *         a point must not depend on the other edges.
     */
    public boolean isConnectionPointFixed()
    {
        return false;
    }

    private boolean isPathAdapted(IEdge e)
    {
        return e instanceof SegmentedLineEdge && ((SegmentedLineEdge) e).isPathAdapted();
    }

    private boolean hasAdaptedEdges()
    {
        for (IEdge anEdge : getConnectedEdges())
        {
            if (isPathAdapted(anEdge))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Slides the end of an automatic edge along its side when it would meet the end of an edge whose path was adapted by the user
     *
     * @param e the automatic edge
     * @param point where it would be connected
     * @param otherPoints where the other automatic edges of this side are connected
     * @return the point where it is connected
     */
    public Point2D keepOffAdaptedEdges(IEdge e, Point2D point, List<Point2D> otherPoints)
    {
        Direction side = e.getDirection(this).getNearestCardinalDirection();
        boolean isVertical = Direction.EAST.equals(side) || Direction.WEST.equals(side);
        Point2D location = getLocation();
        Point2D locationOnGraph = getLocationOnGraph();
        double shift = isVertical ? locationOnGraph.getY() - location.getY() : locationOnGraph.getX() - location.getX();
        List<Double> places = new ArrayList<Double>();
        for (IEdge anEdge : getConnectedEdges())
        {
            if (anEdge != e && isPathAdapted(anEdge) && side.equals(anEdge.getDirection(this)))
            {
                SegmentRoute route = ((SegmentedLineEdge) anEdge).getRoute();
                int index = this.equals(anEdge.getStartNode()) ? 0 : route.getSegmentCount() - 1;
                places.add(route.getCoordinate(index) - shift);
            }
        }
        double desired = isVertical ? point.getY() : point.getX();
        EndSeparator separator = new EndSeparator();
        if (separator.isFree(desired, places))
        {
            return point;
        }
        for (Point2D otherPoint : otherPoints)
        {
            places.add(isVertical ? otherPoint.getY() : otherPoint.getX());
        }
        Rectangle2D b = getBounds();
        double min = isVertical ? location.getY() : location.getX();
        double length = isVertical ? b.getHeight() : b.getWidth();
        double margin = Math.min(SIDE_MARGIN, length / 2);
        double place = separator.findFreePlace(desired, min + margin, min + length - margin, places, candidate -> true);
        return isVertical ? new Point2D.Double(point.getX(), place) : new Point2D.Double(place, point.getY());
    }

    /**
     * @return the point where an edge is connected, the edges of a side being spread evenly along it
     */
    protected Point2D getConnectionPointOnSide(IEdge e)
    {
        List<IEdge> edgesOnSameSide = getEdgesOnSameSide(e);
        int position = edgesOnSameSide.indexOf(e);
        int size = edgesOnSameSide.size();
        Rectangle2D b = getBoundsForConnectionPoint();
        
        double x = b.getCenterX();
        double y = b.getCenterY();

        Direction d = e.getDirection(this);
        Direction nearestCardinalDirection = d.getNearestCardinalDirection();
        if (Direction.NORTH.equals(nearestCardinalDirection)) {
            x = b.getCenterX() + getOffsetOnSide(b.getWidth(), position, size);
            y = b.getMaxY();
        }
        if (Direction.SOUTH.equals(nearestCardinalDirection)) {
            x = b.getCenterX() + getOffsetOnSide(b.getWidth(), position, size);
            y = b.getMinY();
        }
        if (Direction.EAST.equals(nearestCardinalDirection)) {
            x = b.getMinX();
            y = b.getCenterY() + getOffsetOnSide(b.getHeight(), position, size);
        }
        if (Direction.WEST.equals(nearestCardinalDirection)) {
            x = b.getMaxX();
            y = b.getCenterY() + getOffsetOnSide(b.getHeight(), position, size);
        }
        Point2D rawPoint = new Point2D.Double(x, y);

        // Offset outward so thick strokes don't overlap the node.
        // The visible tip of an arrow head is the end point itself,
        // while a plain line cap (CAP_SQUARE) extends edgeBW/2.
        int nodeBorderWidth = getBorderWidth();
        int edgeBorderWidth = (e instanceof AbstractEdge) ? ((AbstractEdge) e).getBorderWidth() : 1;
        boolean hasArrowAtThisEnd = false;
        if (e instanceof SegmentedLineEdge)
        {
            SegmentedLineEdge sle = (SegmentedLineEdge) e;
            ArrowHead ah = this.equals(e.getStartNode()) ? sle.getStartArrowHead() : sle.getEndArrowHead();
            hasArrowAtThisEnd = (ah != null && ah != ArrowHead.NONE);
        }
        double edgeOffset = hasArrowAtThisEnd ? 0 : edgeBorderWidth / 2.0;
        double offset = nodeBorderWidth / 2.0 + edgeOffset;
        if (offset > 0.5)
        {
            if (Direction.NORTH.equals(nearestCardinalDirection))
            {
                rawPoint = new Point2D.Double(rawPoint.getX(), rawPoint.getY() + offset);
            }
            else if (Direction.SOUTH.equals(nearestCardinalDirection))
            {
                rawPoint = new Point2D.Double(rawPoint.getX(), rawPoint.getY() - offset);
            }
            else if (Direction.EAST.equals(nearestCardinalDirection))
            {
                rawPoint = new Point2D.Double(rawPoint.getX() - offset, rawPoint.getY());
            }
            else if (Direction.WEST.equals(nearestCardinalDirection))
            {
                rawPoint = new Point2D.Double(rawPoint.getX() + offset, rawPoint.getY());
            }
        }

        return rawPoint;
    }
    
    
    
    /**
     * Edges sharing a side are spread evenly along it. Their distance to the middle of the side, which is on the grid, is put on
     * the grid too, so that a link to a node aligned on the grid can be straight. The even spread is kept when the side is too
     * short for that, i.e. when two points would meet or one would leave the side.
     *
     * @param sideLength the width or height of the side
     * @param position the rank of the edge on the side
     * @param size the number of edges on the side
     * @return the distance from the middle of the side to the connection point (positive towards the side's maximum)
     */
    private double getOffsetOnSide(double sideLength, int position, int size)
    {
        double snapped = getSnappedOffsetOnSide(sideLength, position, size);
        boolean meetsPrevious = position > 0 && snapped == getSnappedOffsetOnSide(sideLength, position - 1, size);
        boolean meetsNext = position < size - 1 && snapped == getSnappedOffsetOnSide(sideLength, position + 1, size);
        if (meetsPrevious || meetsNext || Math.abs(snapped) >= sideLength / 2)
        {
            return getEvenOffsetOnSide(sideLength, position, size);
        }
        return snapped;
    }

    private double getSnappedOffsetOnSide(double sideLength, int position, int size)
    {
        double offset = getEvenOffsetOnSide(sideLength, position, size);
        IGraph graph = getGraph();
        if (graph == null)
        {
            return offset;
        }
        return graph.getGridSticker().snap(new Point2D.Double(offset, 0)).getX();
    }

    private double getEvenOffsetOnSide(double sideLength, int position, int size)
    {
        return sideLength / 2 - (sideLength / (size + 1)) * (position + 1);
    }

    @Override
    public void draw(Graphics2D g2)
    {

    }
    
    /**
     * Returns the bounds used for connection-point calculation.
     * Subclasses that support cropping should override this to return
     * the visible (cropped) bounds so that edges attach to the visible edge.
     */
    protected Rectangle2D getBoundsForConnectionPoint()
    {
        return getBounds();
    }
    
    


    /** Distance kept to the corners by an end that slides along its side */
    private static final double SIDE_MARGIN = 5;

    public Shape getShape()
    {
        return getBounds();
    }

   
    

    
}
