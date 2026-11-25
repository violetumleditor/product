package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoublePredicate;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;

/**
 * Keeps apart the ends of the edges that share a side of a node, so that two edges are never mixed up. The paths adapted by the
 * user are placed first, the oldest edge first, each end sliding along its side to the closest free place; the automatic ends then
 * keep off them (see RectangularNode), except on the nodes where automatic ends have a fixed place (diamond, ellipse...), that the
 * adapted paths keep off. The places are computed each time from the paths as the user left them, which don't depend on other edges, so
 * nothing calls itself back.
 */
public class EndSeparator
{

    /**
     * @param edge an edge whose path is adapted by the user
     * @return its path, its ends kept off those of the other edges
     */
    public SegmentRoute getSeparatedRoute(SegmentedLineEdge edge)
    {
        SegmentRoute ownRoute = edge.getAdaptedRoute();
        IGraph graph = edge.getStartNode().getGraph();
        if (ownRoute == null || graph == null)
        {
            return ownRoute;
        }
        List<SegmentedLineEdge> neighbors = getNeighbors(edge, graph);
        List<End> occupied = getFixedEnds(neighbors, graph);
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (!neighbors.contains(anEdge))
            {
                continue;
            }
            SegmentedLineEdge neighbor = (SegmentedLineEdge) anEdge;
            SegmentRoute route = separate(neighbor, neighbor.getAdaptedRoute(), occupied);
            if (neighbor == edge)
            {
                return route;
            }
            occupied.addAll(getEnds(neighbor, route));
        }
        return ownRoute;
    }

    /**
     * @return true if the place is far enough from the taken ones
     */
    public boolean isFree(double place, List<Double> occupied)
    {
        return isFree(place, occupied, GAP);
    }

    /**
     * @param desired a place on a side
     * @param min the first possible place
     * @param max the last possible place
     * @param occupied the places already taken
     * @param isPossible other condition on a place
     * @return the place closest to the desired one that is far enough from the taken ones, the desired one if there is none
     */
    public double findFreePlace(double desired, double min, double max, List<Double> occupied, DoublePredicate isPossible)
    {
        for (double gap = GAP; gap >= SMALLEST_GAP; gap /= 2)
        {
            if (isFree(desired, occupied, gap))
            {
                return desired;
            }
            for (double distance = gap; distance <= max - min + gap; distance += gap)
            {
                for (double place : new double[] { desired + distance, desired - distance })
                {
                    if (place >= min && place <= max && isFree(place, occupied, gap) && isPossible.test(place))
                    {
                        return place;
                    }
                }
            }
        }
        return desired;
    }

    private boolean isFree(double place, List<Double> occupied, double gap)
    {
        for (double aPlace : occupied)
        {
            if (Math.abs(aPlace - place) < gap - TOLERANCE)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the edges adapted by the user that share a side with the given one, or with one of these, and so on; itself included
     */
    private List<SegmentedLineEdge> getNeighbors(SegmentedLineEdge edge, IGraph graph)
    {
        List<SegmentedLineEdge> others = new ArrayList<SegmentedLineEdge>();
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (anEdge != edge && anEdge instanceof SegmentedLineEdge && ((SegmentedLineEdge) anEdge).isPathAdapted())
            {
                others.add((SegmentedLineEdge) anEdge);
            }
        }
        List<SegmentedLineEdge> neighbors = new ArrayList<SegmentedLineEdge>();
        neighbors.add(edge);
        List<End> ends = getEnds(edge, edge.getAdaptedRoute());
        boolean isGrowing = true;
        while (isGrowing)
        {
            isGrowing = false;
            for (SegmentedLineEdge other : new ArrayList<SegmentedLineEdge>(others))
            {
                List<End> otherEnds = getEnds(other, other.getAdaptedRoute());
                if (isSideShared(ends, otherEnds))
                {
                    others.remove(other);
                    neighbors.add(other);
                    ends.addAll(otherEnds);
                    isGrowing = true;
                }
            }
        }
        return neighbors;
    }

    private boolean isSideShared(List<End> ends, List<End> otherEnds)
    {
        for (End anEnd : ends)
        {
            for (End otherEnd : otherEnds)
            {
                if (anEnd.isOnSameSide(otherEnd))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the ends of the automatic edges on the nodes giving fixed connection points these edges are connected to
     */
    private List<End> getFixedEnds(List<SegmentedLineEdge> neighbors, IGraph graph)
    {
        List<INode> nodes = new ArrayList<INode>();
        for (SegmentedLineEdge neighbor : neighbors)
        {
            for (INode node : new INode[] { neighbor.getStartNode(), neighbor.getEndNode() })
            {
                if (node instanceof RectangularNode && ((RectangularNode) node).isConnectionPointFixed() && !nodes.contains(node))
                {
                    nodes.add(node);
                }
            }
        }
        List<End> ends = new ArrayList<End>();
        if (nodes.isEmpty())
        {
            return ends;
        }
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (anEdge instanceof SegmentedLineEdge && ((SegmentedLineEdge) anEdge).isPathAdapted())
            {
                continue;
            }
            for (INode node : nodes)
            {
                if (!node.equals(anEdge.getStartNode()) && !node.equals(anEdge.getEndNode()))
                {
                    continue;
                }
                Direction direction = anEdge.getDirection(node);
                if (direction != null)
                {
                    Direction side = direction.getNearestCardinalDirection();
                    Point2D point = node.getConnectionPoint(anEdge);
                    Point2D location = node.getLocation();
                    Point2D locationOnGraph = node.getLocationOnGraph();
                    boolean isVerticalSide = Direction.EAST.equals(side) || Direction.WEST.equals(side);
                    ends.add(new End(node, side, isVerticalSide ? point.getY() + locationOnGraph.getY() - location.getY()
                            : point.getX() + locationOnGraph.getX() - location.getX()));
                }
            }
        }
        return ends;
    }

    private List<End> getEnds(SegmentedLineEdge edge, SegmentRoute route)
    {
        List<End> ends = new ArrayList<End>();
        ends.add(new End(edge.getStartNode(), route.getStartDirection(), route.getCoordinate(0)));
        ends.add(new End(edge.getEndNode(), route.getEndDirection(), route.getCoordinate(route.getSegmentCount() - 1)));
        return ends;
    }

    private SegmentRoute separate(SegmentedLineEdge edge, SegmentRoute route, List<End> occupied)
    {
        List<End> ends = getEnds(edge, route);
        int last = route.getSegmentCount() - 1;
        if (last == 0)
        {
            // Both ends slide together
            List<Double> places = getPlaces(ends.get(0), occupied);
            places.addAll(getPlaces(ends.get(1), occupied));
            return slide(route, 0, places, Math.max(getMin(route, 0, true), getMin(route, 0, false)),
                    Math.min(getMax(route, 0, true), getMax(route, 0, false)));
        }
        SegmentRoute separated = slide(route, 0, getPlaces(ends.get(0), occupied), getMin(route, 0, true), getMax(route, 0, true));
        return slide(separated, last, getPlaces(ends.get(1), occupied), getMin(route, last, false), getMax(route, last, false));
    }

    private SegmentRoute slide(SegmentRoute route, int index, List<Double> places, double min, double max)
    {
        double desired = route.getCoordinate(index);
        double place = findFreePlace(desired, min + CORNER, max - CORNER, places, candidate -> isKept(route, index, candidate));
        return place == desired ? route : route.withCoordinate(index, place);
    }

    /**
     * @return true if the path keeps its shape and its sides with this coordinate
     */
    private boolean isKept(SegmentRoute route, int index, double coordinate)
    {
        SegmentRoute candidate = route.withCoordinate(index, coordinate);
        int last = route.getSegmentCount() - 1;
        if (last == 1)
        {
            // The other node must stay off the segment that slides
            boolean isOtherStart = index == last;
            if (coordinate > getMin(route, index, isOtherStart) - CORNER && coordinate < getMax(route, index, isOtherStart) + CORNER)
            {
                return false;
            }
        }
        if (last >= 2 && Math.abs(coordinate - route.getCoordinate(index == 0 ? 2 : last - 2)) < MIN_JOG)
        {
            return false;
        }
        return candidate.getStartDirection().equals(route.getStartDirection())
                && candidate.getEndDirection().equals(route.getEndDirection());
    }

    private List<Double> getPlaces(End end, List<End> occupied)
    {
        List<Double> places = new ArrayList<Double>();
        for (End anEnd : occupied)
        {
            if (anEnd.isOnSameSide(end))
            {
                places.add(anEnd.place());
            }
        }
        return places;
    }

    private double getMin(SegmentRoute route, int index, boolean isStart)
    {
        Rectangle2D bounds = isStart ? route.getStartBounds() : route.getEndBounds();
        return route.isHorizontal(index) ? bounds.getMinY() : bounds.getMinX();
    }

    private double getMax(SegmentRoute route, int index, boolean isStart)
    {
        Rectangle2D bounds = isStart ? route.getStartBounds() : route.getEndBounds();
        return route.isHorizontal(index) ? bounds.getMaxY() : bounds.getMaxX();
    }

    /**
     * An end of an edge
     *
     * @param node the node it is connected to
     * @param side the direction of the edge at this node
     * @param place its coordinate along this side, on the graph
     */
    private record End(INode node, Direction side, double place)
    {
        boolean isOnSameSide(End other)
        {
            return this.node.equals(other.node) && this.side.equals(other.side);
        }
    }

    /** Distance kept between two ends; halved while the side is too short */
    private static final double GAP = 10;

    private static final double SMALLEST_GAP = 1.25;

    /** Distance between two parallel segments under which they would look like a single one */
    private static final double MIN_JOG = 2.5;

    /** Distance kept to the corners */
    private static final double CORNER = 2;

    private static final double TOLERANCE = 0.01;

}
