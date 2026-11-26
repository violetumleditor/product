package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.GeometryCache;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.MarkerNode;

/**
 * Chooses how an automatic edge ({@link BentStyle#AUTO}) goes from its start node to its end node, preferring a single bend
 * (user's choice). A route is a side of the start node and a side of the end node, so one of the automatic paths: HV, VH, or
 * HVH / VHV (a straight line when the nodes are aligned). A route is feasible when it leaves its start node outward and enters
 * its end node inward without crossing them. Its cost: length, bends, a penalty for three segments (HVH / VHV only as a last
 * resort), other nodes crossed.
 * <p>
 * The branches of a node are routed together: the edges leaving a split (or reaching a merge) avoid the sides taken by the
 * other edges of the node, and each other, and cross each other as little as possible. A node with more incoming than outgoing
 * edges is a merge: its incoming edges are the branches; otherwise its outgoing edges are. Any node with two branches or more
 * is concerned (two parallel tasks reaching the same task: user's report, their edges ran along the same line to the same
 * side), a decision node ({@link #isDecision(INode)}) even with one. A decision never shares a vertex, and prefers the vertices
 * across the axis of its other edges: a split reached from above sends its branches from its left and right vertices, a split
 * reached from the left from its top and bottom ones. Elsewhere, ties go to HV.
 * <p>
 * The sides taken by the other edges of the node are those of their own best route alone, so that routing never calls itself
 * back.
 * <p>
 * Directions are as {@link IEdge#getDirection(INode)} returns them: from the other node toward this one, so SOUTH = the top
 * side, NORTH = the bottom side, EAST = the left side, WEST = the right side (the same vertices on a diamond). The nodes are
 * expected to connect an edge at the middle of the side it chooses, as rectangular, elliptical and diamond nodes do.
 * <p>
 * A diagram subclasses it to tell which edges are routed together and which nodes are decisions, and calls it from the
 * {@link IEdge#getDirection(INode)} of its edges.
 */
public abstract class AutoRouteChooser
{

    /**
     * @param edge the edge to route
     */
    protected AutoRouteChooser(SegmentedLineEdge edge)
    {
        this.edge = edge;
    }

    /**
     * @param anEdge an edge of the graph, with its two ends
     * @return true if this kind of edge is routed by this chooser (the edge being routed is one)
     */
    protected abstract boolean isRoutedKind(IEdge anEdge);

    /**
     * @param node a node of the graph
     * @return true for a decision (gateway): its branches never share a vertex
     */
    protected abstract boolean isDecision(INode node);

    /**
     * @param node the start or end node of the edge
     * @return the direction of the edge at this node, or null when the edge isn't routed automatically (no end node yet, a loop,
     *         a bent style chosen by the user)
     */
    public Direction getDirection(INode node)
    {
        if (!isRouted(this.edge))
        {
            return null;
        }
        // Asked for each end, by the edge and by the other edges of its nodes, many times per paint, while the choice looks at
        // every route of every branch: chosen once per pass over the graph (see GeometryCache)
        Object cachedRoute = GeometryCache.get(this.edge, ROUTE_CACHE_KEY);
        Route route;
        if (cachedRoute != null)
        {
            route = cachedRoute == NO_ROUTE ? null : (Route) cachedRoute;
        }
        else
        {
            route = chooseRoute();
            GeometryCache.put(this.edge, ROUTE_CACHE_KEY, route == null ? NO_ROUTE : route);
        }
        if (route == null)
        {
            return null;
        }
        return node.equals(this.edge.getStartNode()) ? route.startSide() : route.endSide();
    }

    /**
     * A way to go from the start node to the end node
     *
     * @param startSide the side of the start node it leaves
     * @param endSide the side of the end node it reaches
     * @param segments its segments
     * @param cost its length, bends and crossed nodes
     */
    private record Route(Direction startSide, Direction endSide, List<Line2D> segments, double cost)
    {
    }

    /**
     * @return the route of the edge, chosen with the other branches of its start or end node when it is one of them
     */
    private Route chooseRoute()
    {
        INode start = this.edge.getStartNode();
        INode end = this.edge.getEndNode();
        if (isRoutingBranches(start, true))
        {
            return chooseRouteAmongBranches(start, true);
        }
        if (isRoutingBranches(end, false))
        {
            return chooseRouteAmongBranches(end, false);
        }
        return getBestRoute(getRoutes(this.edge, null, null));
    }

    /**
     * @param node the start or end node of the edge
     * @param isLeaving true if the edge leaves the node
     * @return true if the edge is one of the branches of the node, routed together
     */
    private boolean isRoutingBranches(INode node, boolean isLeaving)
    {
        if (isLeaving == isMerge(node))
        {
            return false;
        }
        return isDecision(node) || getBranches(node, isLeaving).size() >= 2;
    }

    /**
     * @param node the node whose branches are routed together
     * @param isLeaving true for the edges leaving a split, false for the edges reaching a merge
     */
    private Route chooseRouteAmongBranches(INode node, boolean isLeaving)
    {
        boolean isDecision = isDecision(node);
        double sharedCost = isDecision ? SHARED_VERTEX_COST : SHARED_SIDE_COST;
        List<SegmentedLineEdge> branches = getBranches(node, isLeaving);
        Set<Direction> takenVertices = getTakenVertices(node, branches);
        Boolean isVerticalPreferred = isDecision ? isVerticalPreferred(takenVertices) : null;
        List<List<Route>> candidates = new ArrayList<List<Route>>();
        long combinationCount = 1;
        for (SegmentedLineEdge aBranch : branches)
        {
            List<Route> routes = getRoutes(aBranch, isLeaving ? node : null, isLeaving ? null : node);
            if (routes.isEmpty())
            {
                routes = getRoutes(aBranch, null, null);
            }
            candidates.add(routes);
            combinationCount *= Math.max(1, routes.size());
        }
        int index = branches.indexOf(this.edge);
        if (candidates.get(index).isEmpty())
        {
            return null;
        }
        if (combinationCount > MAX_COMBINATIONS)
        {
            return getBestBranchRoute(candidates.get(index), isLeaving, takenVertices, isVerticalPreferred, sharedCost);
        }
        Route[] bestChoice = null;
        double bestCost = Double.MAX_VALUE;
        Route[] choice = new Route[branches.size()];
        for (long k = 0; k < combinationCount; k++)
        {
            long rest = k;
            for (int i = 0; i < choice.length; i++)
            {
                List<Route> routes = candidates.get(i);
                choice[i] = routes.isEmpty() ? null : routes.get((int) (rest % routes.size()));
                rest = rest / Math.max(1, routes.size());
            }
            double cost = getCombinationCost(choice, isLeaving, takenVertices, isVerticalPreferred, sharedCost);
            if (cost < bestCost)
            {
                bestCost = cost;
                bestChoice = choice.clone();
            }
        }
        return bestChoice[index];
    }

    /**
     * @return true if the node has more incoming than outgoing edges
     */
    private boolean isMerge(INode node)
    {
        int incoming = 0;
        int outgoing = 0;
        IGraph graph = node.getGraph();
        if (graph == null)
        {
            return false;
        }
        for (IEdge anEdge : graph.getAllEdges())
        {
            if (anEdge.getStartNode() == null || anEdge.getEndNode() == null || anEdge.getStartNode().equals(anEdge.getEndNode()))
            {
                continue;
            }
            if (node.equals(anEdge.getStartNode()))
            {
                outgoing++;
            }
            if (node.equals(anEdge.getEndNode()))
            {
                incoming++;
            }
        }
        if (!isListed(graph, this.edge))
        {
            // An edge being created
            if (node.equals(this.edge.getStartNode()))
            {
                outgoing++;
            }
            else
            {
                incoming++;
            }
        }
        return incoming > outgoing;
    }

    private boolean isListed(IGraph graph, IEdge anEdge)
    {
        return graph.getAllEdges().contains(anEdge);
    }

    /**
     * @return the routed edges leaving (or reaching) the node, in the graph's order, this edge included
     */
    private List<SegmentedLineEdge> getBranches(INode node, boolean isLeaving)
    {
        List<SegmentedLineEdge> branches = new ArrayList<SegmentedLineEdge>();
        IGraph graph = node.getGraph();
        if (graph != null)
        {
            for (IEdge anEdge : graph.getAllEdges())
            {
                INode end = isLeaving ? anEdge.getStartNode() : anEdge.getEndNode();
                if (node.equals(end) && isRouted(anEdge))
                {
                    branches.add((SegmentedLineEdge) anEdge);
                }
            }
        }
        if (!branches.contains(this.edge))
        {
            branches.add(this.edge);
        }
        return branches;
    }

    /**
     * @return true for an automatic edge of a routed kind, between two distinct nodes, whose segments weren't dragged by the user
     */
    private boolean isRouted(IEdge anEdge)
    {
        if (!(anEdge instanceof SegmentedLineEdge))
        {
            return false;
        }
        INode start = anEdge.getStartNode();
        INode end = anEdge.getEndNode();
        return start != null && end != null && !start.equals(end)
                && BentStyle.AUTO.equals(((SegmentedLineEdge) anEdge).getBentStyle()) && isRoutedKind(anEdge)
                && !((SegmentedLineEdge) anEdge).isPathAdapted();
    }

    /**
     * @return the sides of the node used by its other edges: for an automatic edge, those of its best route alone
     */
    private Set<Direction> getTakenVertices(INode node, List<SegmentedLineEdge> branches)
    {
        Set<Direction> takenVertices = new HashSet<Direction>();
        IGraph graph = node.getGraph();
        if (graph == null)
        {
            return takenVertices;
        }
        for (IEdge anEdge : graph.getAllEdges())
        {
            boolean isTouching = node.equals(anEdge.getStartNode()) || node.equals(anEdge.getEndNode());
            boolean isLoop = anEdge.getStartNode() != null && anEdge.getStartNode().equals(anEdge.getEndNode());
            if (isTouching && !isLoop && anEdge.getEndNode() != null && !branches.contains(anEdge))
            {
                takenVertices.add(getSide(anEdge, node));
            }
        }
        return takenVertices;
    }

    /**
     * @return the side of the node the edge uses, without routing it with branches (no call back to the routing of the node)
     */
    private Direction getSide(IEdge anEdge, INode node)
    {
        if (isRouted(anEdge))
        {
            Route route = getBestRoute(getRoutes((SegmentedLineEdge) anEdge, null, null));
            if (route != null)
            {
                return node.equals(anEdge.getStartNode()) ? route.startSide() : route.endSide();
            }
        }
        return anEdge.getDirection(node).getNearestCardinalDirection();
    }

    /**
     * @return true to prefer the top and bottom vertices for the branches, false the left and right ones, null for no
     *         preference: across the axis of the vertices already taken, the top and bottom ones when none is
     */
    private Boolean isVerticalPreferred(Set<Direction> takenVertices)
    {
        boolean isHorizontalTaken = takenVertices.contains(Direction.EAST) || takenVertices.contains(Direction.WEST);
        boolean isVerticalTaken = takenVertices.contains(Direction.NORTH) || takenVertices.contains(Direction.SOUTH);
        if (isHorizontalTaken == isVerticalTaken)
        {
            return takenVertices.isEmpty() ? Boolean.TRUE : null;
        }
        return isHorizontalTaken;
    }

    /**
     * @return the cost of routing the branches so: their own costs, plus vertices shared between them or with the other edges,
     *         crossings between them, and the vertices against the preferred axis, less a bonus when they all have the same
     *         shape (all HV or all VH...) on distinct sides, which looks symmetric
     */
    private double getCombinationCost(Route[] choice, boolean isLeaving, Set<Direction> takenVertices, Boolean isVerticalPreferred,
            double sharedCost)
    {
        double cost = 0;
        boolean isAnySideShared = false;
        for (int i = 0; i < choice.length; i++)
        {
            if (choice[i] == null)
            {
                continue;
            }
            cost += getBranchCost(choice[i], isLeaving, takenVertices, isVerticalPreferred, sharedCost);
            Direction vertex = getVertex(choice[i], isLeaving);
            for (int j = i + 1; j < choice.length; j++)
            {
                if (choice[j] == null)
                {
                    continue;
                }
                if (vertex.equals(getVertex(choice[j], isLeaving)))
                {
                    cost += sharedCost;
                    isAnySideShared = true;
                }
                if (isCrossing(choice[i], choice[j]))
                {
                    cost += CROSSING_COST;
                }
            }
        }
        // Only on distinct sides: two branches of the same shape on one side run along the same line
        if (isSameShape(choice) && !isAnySideShared)
        {
            cost -= SAME_SHAPE_BONUS;
        }
        return cost;
    }

    /**
     * @return true if there are several routes, all leaving and reaching their nodes along the same axes
     */
    private boolean isSameShape(Route[] choice)
    {
        Route first = null;
        int count = 0;
        for (Route aRoute : choice)
        {
            if (aRoute == null)
            {
                continue;
            }
            count++;
            if (first == null)
            {
                first = aRoute;
            }
            else if (isVertical(aRoute.startSide()) != isVertical(first.startSide())
                    || isVertical(aRoute.endSide()) != isVertical(first.endSide()))
            {
                return false;
            }
        }
        return count > 1;
    }

    private double getBranchCost(Route route, boolean isLeaving, Set<Direction> takenVertices, Boolean isVerticalPreferred,
            double sharedCost)
    {
        double cost = route.cost();
        Direction vertex = getVertex(route, isLeaving);
        if (takenVertices.contains(vertex))
        {
            cost += sharedCost;
        }
        if (isVerticalPreferred != null && isVerticalPreferred.booleanValue() != isVertical(vertex))
        {
            cost += PREFERENCE_COST;
        }
        return cost;
    }

    private Route getBestBranchRoute(List<Route> routes, boolean isLeaving, Set<Direction> takenVertices,
            Boolean isVerticalPreferred, double sharedCost)
    {
        Route best = null;
        double bestCost = Double.MAX_VALUE;
        for (Route aRoute : routes)
        {
            double cost = getBranchCost(aRoute, isLeaving, takenVertices, isVerticalPreferred, sharedCost);
            if (cost < bestCost)
            {
                bestCost = cost;
                best = aRoute;
            }
        }
        return best;
    }

    private Direction getVertex(Route route, boolean isLeaving)
    {
        return isLeaving ? route.startSide() : route.endSide();
    }

    private boolean isVertical(Direction side)
    {
        return Direction.NORTH.equals(side) || Direction.SOUTH.equals(side);
    }

    /**
     * @return true if two routes cross, their shared ends apart
     */
    private boolean isCrossing(Route first, Route second)
    {
        for (Line2D aSegment : first.segments())
        {
            for (Line2D anotherSegment : second.segments())
            {
                if (aSegment.intersectsLine(anotherSegment) && !isTouchingAtEnds(aSegment, anotherSegment))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTouchingAtEnds(Line2D aSegment, Line2D anotherSegment)
    {
        Point2D[] ends = { aSegment.getP1(), aSegment.getP2() };
        Point2D[] otherEnds = { anotherSegment.getP1(), anotherSegment.getP2() };
        for (Point2D anEnd : ends)
        {
            for (Point2D anotherEnd : otherEnds)
            {
                if (anEnd.distance(anotherEnd) < 1)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private Route getBestRoute(List<Route> routes)
    {
        Route best = null;
        for (Route aRoute : routes)
        {
            if (best == null || aRoute.cost() < best.cost())
            {
                best = aRoute;
            }
        }
        return best;
    }

    /**
     * @param anEdge an edge
     * @param splitNode its start node when it is a routed branch leaving it, else null
     * @param mergeNode its end node when it is a routed branch reaching it, else null
     * @return its feasible routes, ties going to HV out of anything but a branch
     */
    private List<Route> getRoutes(SegmentedLineEdge anEdge, INode splitNode, INode mergeNode)
    {
        INode start = anEdge.getStartNode();
        INode end = anEdge.getEndNode();
        Rectangle2D startBounds = getBoundsOnGraph(start);
        Rectangle2D endBounds = getBoundsOnGraph(end);
        List<Route> routes = new ArrayList<Route>();
        for (Direction aStartSide : SIDES)
        {
            for (Direction anEndSide : SIDES)
            {
                List<Point2D> points = getPoints(startBounds, aStartSide, endBounds, anEndSide);
                if (!isFeasible(points, startBounds, aStartSide, endBounds, anEndSide))
                {
                    continue;
                }
                List<Line2D> segments = new ArrayList<Line2D>();
                double length = 0;
                for (int i = 1; i < points.size(); i++)
                {
                    Line2D segment = new Line2D.Double(points.get(i - 1), points.get(i));
                    if (points.get(i - 1).distance(points.get(i)) > 0)
                    {
                        segments.add(segment);
                        length += points.get(i - 1).distance(points.get(i));
                    }
                }
                double cost = length + BEND_COST * (segments.size() - 1)
                        + CROSSED_NODE_COST * countCrossedNodes(start, end, segments);
                if (segments.size() > 2)
                {
                    cost += THREE_SEGMENT_COST;
                }
                boolean isHv = !isVertical(aStartSide) && isVertical(anEndSide);
                if (isHv && splitNode == null && mergeNode == null)
                {
                    cost -= TIE_BREAK;
                }
                routes.add(new Route(aStartSide, anEndSide, segments, cost));
            }
        }
        return routes;
    }

    /**
     * @return the points of the path the automatic bent style draws between these sides, as BentStyle does
     */
    private List<Point2D> getPoints(Rectangle2D startBounds, Direction startSide, Rectangle2D endBounds, Direction endSide)
    {
        Point2D p0 = getSideCenter(startBounds, startSide);
        Point2D p3 = getSideCenter(endBounds, endSide);
        List<Point2D> points = new ArrayList<Point2D>();
        points.add(p0);
        boolean isLeavingVertically = isVertical(startSide);
        boolean isArrivingVertically = isVertical(endSide);
        if (!isLeavingVertically && isArrivingVertically)
        {
            points.add(new Point2D.Double(p3.getX(), p0.getY()));
        }
        else if (isLeavingVertically && !isArrivingVertically)
        {
            points.add(new Point2D.Double(p0.getX(), p3.getY()));
        }
        else if (!isLeavingVertically)
        {
            double middleX = (p0.getX() + p3.getX()) / 2;
            if (Math.abs(p0.getY() - p3.getY()) > MIN_SEGMENT)
            {
                points.add(new Point2D.Double(middleX, p0.getY()));
                points.add(new Point2D.Double(middleX, p3.getY()));
            }
        }
        else
        {
            double middleY = (p0.getY() + p3.getY()) / 2;
            if (Math.abs(p0.getX() - p3.getX()) > MIN_SEGMENT)
            {
                points.add(new Point2D.Double(p0.getX(), middleY));
                points.add(new Point2D.Double(p3.getX(), middleY));
            }
        }
        points.add(p3);
        return points;
    }

    /**
     * @return true if the path leaves its start node outward and enters its end node inward, by at least MIN_SEGMENT, without
     *         crossing them
     */
    private boolean isFeasible(List<Point2D> points, Rectangle2D startBounds, Direction startSide, Rectangle2D endBounds,
            Direction endSide)
    {
        Point2D first = points.get(0);
        Point2D second = points.get(1);
        // Leaving: opposite to the direction toward the start node
        double out = (second.getX() - first.getX()) * -startSide.getX() + (second.getY() - first.getY()) * -startSide.getY();
        Point2D beforeLast = points.get(points.size() - 2);
        Point2D last = points.get(points.size() - 1);
        double in = (last.getX() - beforeLast.getX()) * endSide.getX() + (last.getY() - beforeLast.getY()) * endSide.getY();
        if (out < MIN_SEGMENT || in < MIN_SEGMENT)
        {
            return false;
        }
        Rectangle2D startInside = shrink(startBounds);
        Rectangle2D endInside = shrink(endBounds);
        for (int i = 1; i < points.size(); i++)
        {
            Line2D segment = new Line2D.Double(points.get(i - 1), points.get(i));
            if (segment.intersects(startInside) || segment.intersects(endInside))
            {
                return false;
            }
        }
        return true;
    }

    private Rectangle2D shrink(Rectangle2D bounds)
    {
        return new Rectangle2D.Double(bounds.getX() + 2, bounds.getY() + 2, Math.max(0, bounds.getWidth() - 4),
                Math.max(0, bounds.getHeight() - 4));
    }

    private Point2D getSideCenter(Rectangle2D bounds, Direction side)
    {
        if (Direction.NORTH.equals(side))
        {
            return new Point2D.Double(bounds.getCenterX(), bounds.getMaxY());
        }
        if (Direction.SOUTH.equals(side))
        {
            return new Point2D.Double(bounds.getCenterX(), bounds.getMinY());
        }
        if (Direction.EAST.equals(side))
        {
            return new Point2D.Double(bounds.getMinX(), bounds.getCenterY());
        }
        return new Point2D.Double(bounds.getMaxX(), bounds.getCenterY());
    }

    /**
     * @return how many nodes, other than the two ends, their parents and the background ones (pools, grouping areas), the
     *         segments cross
     */
    private int countCrossedNodes(INode start, INode end, List<Line2D> segments)
    {
        IGraph graph = start.getGraph();
        if (graph == null)
        {
            return 0;
        }
        int count = 0;
        for (INode aNode : graph.getAllNodes())
        {
            if (aNode.equals(start) || aNode.equals(end) || aNode instanceof ContainerNode
                    || aNode instanceof MarkerNode || isAncestor(aNode, start) || isAncestor(aNode, end))
            {
                continue;
            }
            Rectangle2D bounds = getBoundsOnGraph(aNode);
            for (Line2D aSegment : segments)
            {
                if (bounds.intersectsLine(aSegment))
                {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private boolean isAncestor(INode candidate, INode node)
    {
        for (INode aParent = node.getParent(); aParent != null; aParent = aParent.getParent())
        {
            if (aParent.equals(candidate))
            {
                return true;
            }
        }
        return false;
    }

    private Rectangle2D getBoundsOnGraph(INode node)
    {
        Point2D location = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        return new Rectangle2D.Double(location.getX(), location.getY(), bounds.getWidth(), bounds.getHeight());
    }

    private final SegmentedLineEdge edge;

    private static final Direction[] SIDES = { Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH };

    /** BentStyle's minimum segment */
    private static final double MIN_SEGMENT = 10;

    private static final double BEND_COST = 20;

    /**
     * HVH and VHV paths, taken only when no HV, VH or straight path fits (user's choice): else a node widened by its text makes
     * the branches reaching it switch from HV to HVH, shorter
     */
    private static final double THREE_SEGMENT_COST = 300;

    private static final double CROSSED_NODE_COST = 1000;

    /** A decision's vertex used by two edges */
    private static final double SHARED_VERTEX_COST = 2000;

    /** Another node's side used by two branches: allowed, but their lines then run together */
    private static final double SHARED_SIDE_COST = 150;

    private static final double CROSSING_COST = 300;

    private static final double PREFERENCE_COST = 100;

    /**
     * Branches all of the same shape, on distinct sides: strong enough to keep the symmetry when a node grows with its text
     * (user's report: a merging task widened by its name made one of its two branches switch from its top to its left side)
     */
    private static final double SAME_SHAPE_BONUS = 200;

    private static final double TIE_BREAK = 0.5;

    private static final long MAX_COMBINATIONS = 4096;


    /** What {@link #getDirection(INode)} remembers in a {@link GeometryCache} pass */
    private static final Object ROUTE_CACHE_KEY = new Object();

    /** Remembered when no route was found */
    private static final Object NO_ROUTE = new Object();
}
