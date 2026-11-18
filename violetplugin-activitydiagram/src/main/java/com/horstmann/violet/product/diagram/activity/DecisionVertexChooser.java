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

package com.horstmann.violet.product.diagram.activity;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.common.ContainerNode;

/**
 * Chooses the route of each automatic transition leaving a decision, for all of them at once, so that two branches don't share
 * a vertex when another one is free. A route is a vertex of the decision and a side of the end node, so one of the automatic
 * paths: HV from a side vertex to the top or bottom of the end node, VH from the top or bottom vertex to the left or right of
 * the end node (both preferred), a straight line, or else HVH / VHV. A route is feasible when it never goes back over the
 * diamond. Every combination of feasible routes is scored: length, bends, three segment paths, crossings between the branches,
 * other nodes crossed, and vertices shared with another edge (incoming transitions, or edges whose route is fixed).
 * <p>
 * Vertices are directions as {@link DecisionNode#getConnectionPoint(IEdge)} reads them: WEST = right vertex, EAST = left
 * vertex, NORTH = bottom vertex, SOUTH = top vertex. End sides are directions toward the end node, as
 * {@link com.horstmann.violet.product.diagram.abstracts.node.RectangularNode#getConnectionPoint(IEdge)} reads them: SOUTH =
 * its top side, NORTH = its bottom side, EAST = its left side, WEST = its right side.
 */
class DecisionVertexChooser
{

    /**
     * @param decision the decision the transitions leave
     */
    DecisionVertexChooser(DecisionNode decision)
    {
        this.decision = decision;
    }

    /**
     * @param edge an automatic transition leaving the decision
     * @param node its start or end node
     * @return as {@link ActivityTransitionEdge#getDirection(INode)}: the chosen vertex at the start, the chosen side of the end
     *         node at the end
     */
    Direction getDirection(ActivityTransitionEdge edge, INode node)
    {
        Route route = chooseRoute(edge);
        if (node.equals(this.decision))
        {
            return route.vertex();
        }
        return route.endSide();
    }

    /**
     * A way to go from the decision to the end node
     *
     * @param vertex the vertex of the decision it leaves from
     * @param endSide the side of the end node it reaches
     * @param path its segments, as BentStyle draws them
     * @param cost its length, bends and shape
     */
    private record Route(Direction vertex, Direction endSide, List<Line2D> path, double cost)
    {
    }

    private Route chooseRoute(ActivityTransitionEdge edge)
    {
        List<ActivityTransitionEdge> routedEdges = getRoutedEdges(edge);
        Map<Direction, Integer> occupiedVertices = getOccupiedVertices(routedEdges);
        List<List<Route>> candidates = new ArrayList<List<Route>>();
        long combinationCount = 1;
        for (ActivityTransitionEdge aRoutedEdge : routedEdges)
        {
            List<Route> routes = getRoutes(aRoutedEdge);
            candidates.add(routes);
            combinationCount *= routes.size();
        }
        int index = routedEdges.indexOf(edge);
        if (combinationCount > MAX_COMBINATIONS)
        {
            return getRouteOnItsOwn(candidates.get(index), occupiedVertices);
        }
        Route[] bestChoice = null;
        double bestCost = Double.MAX_VALUE;
        Route[] choice = new Route[routedEdges.size()];
        for (long k = 0; k < combinationCount; k++)
        {
            long rest = k;
            for (int i = 0; i < choice.length; i++)
            {
                List<Route> routes = candidates.get(i);
                choice[i] = routes.get((int) (rest % routes.size()));
                rest = rest / routes.size();
            }
            double cost = getCost(choice, occupiedVertices);
            if (cost < bestCost)
            {
                bestCost = cost;
                bestChoice = choice.clone();
            }
        }
        return bestChoice[index];
    }

    /**
     * @return the automatic transitions leaving the decision, in the graph's order, including the given one even if the graph
     *         doesn't list it yet
     */
    private List<ActivityTransitionEdge> getRoutedEdges(ActivityTransitionEdge edge)
    {
        List<ActivityTransitionEdge> routedEdges = new ArrayList<ActivityTransitionEdge>();
        IGraph graph = this.decision.getGraph();
        if (graph != null)
        {
            for (IEdge anEdge : graph.getAllEdges())
            {
                if (isRouted(anEdge))
                {
                    routedEdges.add((ActivityTransitionEdge) anEdge);
                }
            }
        }
        if (!routedEdges.contains(edge))
        {
            routedEdges.add(edge);
        }
        return routedEdges;
    }

    private boolean isRouted(IEdge anEdge)
    {
        if (!ActivityTransitionEdge.class.isInstance(anEdge))
        {
            return false;
        }
        ActivityTransitionEdge transition = (ActivityTransitionEdge) anEdge;
        INode end = transition.getEndNode();
        return this.decision.equals(transition.getStartNode()) && end != null && !this.decision.equals(end)
                && !SynchronizationBarNode.class.isInstance(end) && BentStyle.AUTO.equals(transition.getBentStyle());
    }

    /**
     * @return how many other edges end or start at each vertex of the decision
     */
    private Map<Direction, Integer> getOccupiedVertices(List<ActivityTransitionEdge> routedEdges)
    {
        Map<Direction, Integer> occupiedVertices = new HashMap<Direction, Integer>();
        IGraph graph = this.decision.getGraph();
        // An incoming transition may itself leave a decision this one leads to: don't ask it again while it asks this one
        if (graph == null || this.decision.isChoosingVertices())
        {
            return occupiedVertices;
        }
        this.decision.setChoosingVertices(true);
        try
        {
            for (IEdge anEdge : graph.getAllEdges())
            {
                boolean isTouching = this.decision.equals(anEdge.getStartNode()) || this.decision.equals(anEdge.getEndNode());
                boolean isLoop = anEdge.getStartNode() != null && anEdge.getStartNode().equals(anEdge.getEndNode());
                if (isTouching && !isLoop && !routedEdges.contains(anEdge))
                {
                    Direction vertex = anEdge.getDirection(this.decision).getNearestCardinalDirection();
                    occupiedVertices.merge(vertex, 1, Integer::sum);
                }
            }
        }
        finally
        {
            this.decision.setChoosingVertices(false);
        }
        return occupiedVertices;
    }

    /**
     * @return the feasible routes to the end node of an edge, cheapest first (other nodes crossed included); without any (nodes
     *         overlapping), the one facing the end node
     */
    private List<Route> getRoutes(ActivityTransitionEdge edge)
    {
        INode end = edge.getEndNode();
        Rectangle2D endBounds = getBoundsOnGraph(end);
        List<Route> routes = new ArrayList<Route>();
        addSideVertexRoute(routes, Direction.WEST, endBounds);
        addSideVertexRoute(routes, Direction.EAST, endBounds);
        addVerticalVertexRoutes(routes, Direction.NORTH, endBounds);
        addVerticalVertexRoutes(routes, Direction.SOUTH, endBounds);
        List<Route> sortedRoutes = new ArrayList<Route>();
        for (Route aRoute : routes)
        {
            Route routeWithObstacles = new Route(aRoute.vertex(), aRoute.endSide(), aRoute.path(),
                    aRoute.cost() + NODE_CROSSING_COST * getCrossedNodeCount(aRoute.path(), end));
            int position = 0;
            while (position < sortedRoutes.size() && sortedRoutes.get(position).cost() <= routeWithObstacles.cost())
            {
                position++;
            }
            sortedRoutes.add(position, routeWithObstacles);
        }
        if (sortedRoutes.isEmpty())
        {
            sortedRoutes.add(getFacingRoute(endBounds));
        }
        return sortedRoutes;
    }

    /**
     * From the right (WEST) or left (EAST) vertex: HV to the top or bottom of an end node above or below, straight or HVH to an
     * end node on the same row. Only toward an end node whose center lies clearly beyond the vertex.
     */
    private void addSideVertexRoute(List<Route> routes, Direction vertex, Rectangle2D endBounds)
    {
        Rectangle2D d = getDecisionBounds();
        boolean isRightVertex = Direction.WEST.equals(vertex);
        double sign = isRightVertex ? 1 : -1;
        double vertexX = isRightVertex ? d.getMaxX() : d.getMinX();
        double vertexY = d.getCenterY();
        double endX = endBounds.getCenterX();
        double endY = endBounds.getCenterY();
        if (sign * (endX - vertexX) < MIN_HORIZONTAL_GAP)
        {
            return;
        }
        List<Point2D> points = new ArrayList<Point2D>();
        if (isOnSameRow(endBounds))
        {
            double sideX = isRightVertex ? endBounds.getMinX() : endBounds.getMaxX();
            if (sign * (sideX - vertexX) < 2 * MIN_SEGMENT)
            {
                return;
            }
            Direction endSide = isRightVertex ? Direction.EAST : Direction.WEST;
            if (Math.abs(endY - vertexY) <= MIN_SEGMENT)
            {
                points.add(new Point2D.Double(vertexX, endY));
                points.add(new Point2D.Double(sideX, endY));
                routes.add(createRoute(vertex, endSide, points));
                return;
            }
            double middleX = (vertexX + sideX) / 2;
            points.add(new Point2D.Double(vertexX, vertexY));
            points.add(new Point2D.Double(middleX, vertexY));
            points.add(new Point2D.Double(middleX, endY));
            points.add(new Point2D.Double(sideX, endY));
            routes.add(createRoute(vertex, endSide, points));
            return;
        }
        boolean isEndBelow = endY >= vertexY;
        double sideY = isEndBelow ? endBounds.getMinY() : endBounds.getMaxY();
        points.add(new Point2D.Double(vertexX, vertexY));
        points.add(new Point2D.Double(endX, vertexY));
        points.add(new Point2D.Double(endX, sideY));
        routes.add(createRoute(vertex, isEndBelow ? Direction.SOUTH : Direction.NORTH, points));
    }

    /**
     * From the bottom (NORTH) or top (SOUTH) vertex: VH to the left or right of an end node lying aside, straight or VHV to the
     * top or bottom of an end node beyond the diamond.
     */
    private void addVerticalVertexRoutes(List<Route> routes, Direction vertex, Rectangle2D endBounds)
    {
        Rectangle2D d = getDecisionBounds();
        boolean isBottomVertex = Direction.NORTH.equals(vertex);
        double sign = isBottomVertex ? 1 : -1;
        double vertexX = d.getCenterX();
        double vertexY = isBottomVertex ? d.getMaxY() : d.getMinY();
        double endX = endBounds.getCenterX();
        double endY = endBounds.getCenterY();
        if (sign * (endY - vertexY) >= MIN_SEGMENT)
        {
            boolean isEndOnTheRight = endBounds.getMinX() >= vertexX + MIN_SEGMENT;
            boolean isEndOnTheLeft = endBounds.getMaxX() <= vertexX - MIN_SEGMENT;
            if (isEndOnTheRight || isEndOnTheLeft)
            {
                double sideX = isEndOnTheRight ? endBounds.getMinX() : endBounds.getMaxX();
                List<Point2D> points = new ArrayList<Point2D>();
                points.add(new Point2D.Double(vertexX, vertexY));
                points.add(new Point2D.Double(vertexX, endY));
                points.add(new Point2D.Double(sideX, endY));
                routes.add(createRoute(vertex, isEndOnTheRight ? Direction.EAST : Direction.WEST, points));
            }
        }
        double sideY = isBottomVertex ? endBounds.getMinY() : endBounds.getMaxY();
        if (sign * (sideY - vertexY) < 2 * MIN_SEGMENT)
        {
            return;
        }
        Direction endSide = isBottomVertex ? Direction.SOUTH : Direction.NORTH;
        List<Point2D> points = new ArrayList<Point2D>();
        if (Math.abs(endX - vertexX) <= MIN_SEGMENT)
        {
            points.add(new Point2D.Double(endX, vertexY));
            points.add(new Point2D.Double(endX, sideY));
            routes.add(createRoute(vertex, endSide, points));
            return;
        }
        double middleY = (vertexY + sideY) / 2;
        points.add(new Point2D.Double(vertexX, vertexY));
        points.add(new Point2D.Double(vertexX, middleY));
        points.add(new Point2D.Double(endX, middleY));
        points.add(new Point2D.Double(endX, sideY));
        routes.add(createRoute(vertex, endSide, points));
    }

    /**
     * @param points the corners of the path, from the vertex to the end node
     */
    private Route createRoute(Direction vertex, Direction endSide, List<Point2D> points)
    {
        List<Line2D> path = new ArrayList<Line2D>();
        double length = 0;
        for (int i = 1; i < points.size(); i++)
        {
            Line2D segment = new Line2D.Double(points.get(i - 1), points.get(i));
            path.add(segment);
            length += segment.getP1().distance(segment.getP2());
        }
        double cost = length + BEND_COST * (path.size() - 1);
        if (path.size() > 2)
        {
            cost += THREE_SEGMENT_COST;
        }
        return new Route(vertex, endSide, path, cost);
    }

    /**
     * @return the route facing the end node, when no route is feasible
     */
    private Route getFacingRoute(Rectangle2D endBounds)
    {
        Rectangle2D d = getDecisionBounds();
        boolean isEndOnTheRight = endBounds.getCenterX() >= d.getCenterX();
        boolean isEndBelow = endBounds.getCenterY() >= d.getCenterY();
        if (isOnSameRow(endBounds))
        {
            return new Route(isEndOnTheRight ? Direction.WEST : Direction.EAST, isEndOnTheRight ? Direction.EAST : Direction.WEST,
                    new ArrayList<Line2D>(), 0);
        }
        return new Route(isEndBelow ? Direction.NORTH : Direction.SOUTH, isEndBelow ? Direction.SOUTH : Direction.NORTH,
                new ArrayList<Line2D>(), 0);
    }

    /**
     * @return the number of nodes, other than the decision, the end node and grouping areas, that the path goes through
     */
    private int getCrossedNodeCount(List<Line2D> path, INode end)
    {
        IGraph graph = this.decision.getGraph();
        if (graph == null)
        {
            return 0;
        }
        int count = 0;
        for (INode aNode : graph.getAllNodes())
        {
            if (aNode.equals(this.decision) || aNode.equals(end) || ContainerNode.class.isInstance(aNode))
            {
                continue;
            }
            Rectangle2D bounds = getBoundsOnGraph(aNode);
            for (Line2D aSegment : path)
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

    private double getCost(Route[] choice, Map<Direction, Integer> occupiedVertices)
    {
        double cost = 0;
        Map<Direction, Integer> edgesPerVertex = new HashMap<Direction, Integer>(occupiedVertices);
        for (Route aRoute : choice)
        {
            cost += aRoute.cost();
            edgesPerVertex.merge(aRoute.vertex(), 1, Integer::sum);
        }
        for (int count : edgesPerVertex.values())
        {
            cost += SHARING_COST * Math.max(0, count - 1);
        }
        for (int i = 0; i < choice.length; i++)
        {
            for (int j = i + 1; j < choice.length; j++)
            {
                if (!choice[i].vertex().equals(choice[j].vertex()))
                {
                    cost += CROSSING_COST * getCrossingCount(choice[i].path(), choice[j].path());
                }
            }
        }
        return cost;
    }

    private int getCrossingCount(List<Line2D> path, List<Line2D> otherPath)
    {
        int count = 0;
        for (Line2D aSegment : path)
        {
            for (Line2D anotherSegment : otherPath)
            {
                if (aSegment.intersectsLine(anotherSegment))
                {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * @return the cheapest route of an edge from a vertex left free by the other edges, when there are too many branches to try
     *         every combination
     */
    private Route getRouteOnItsOwn(List<Route> routes, Map<Direction, Integer> occupiedVertices)
    {
        Route bestRoute = routes.get(0);
        double bestCost = Double.MAX_VALUE;
        for (Route aRoute : routes)
        {
            double cost = aRoute.cost() + SHARING_COST * occupiedVertices.getOrDefault(aRoute.vertex(), 0);
            if (cost < bestCost)
            {
                bestCost = cost;
                bestRoute = aRoute;
            }
        }
        return bestRoute;
    }

    /**
     * @return true when the node spans the height of the decision's side vertices, so that a horizontal line can join them
     */
    private boolean isOnSameRow(Rectangle2D endBounds)
    {
        double vertexY = getDecisionBounds().getCenterY();
        return endBounds.getMinY() - MIN_SEGMENT <= vertexY && vertexY <= endBounds.getMaxY() + MIN_SEGMENT;
    }

    private Rectangle2D getDecisionBounds()
    {
        return getBoundsOnGraph(this.decision);
    }

    private Rectangle2D getBoundsOnGraph(INode node)
    {
        Rectangle2D bounds = node.getBounds();
        Point2D location = node.getLocationOnGraph();
        return new Rectangle2D.Double(location.getX(), location.getY(), bounds.getWidth(), bounds.getHeight());
    }

    private final DecisionNode decision;

    /** BentStyle's minimum segment */
    private static final double MIN_SEGMENT = 10;

    /** Below this gap between the end node's center and a side vertex, a horizontal segment would go back over the diamond */
    private static final double MIN_HORIZONTAL_GAP = 20;

    private static final double BEND_COST = 20;

    /** HVH and VHV paths, taken only when no HV, VH or straight path fits */
    private static final double THREE_SEGMENT_COST = 300;

    private static final double NODE_CROSSING_COST = 500;

    private static final double CROSSING_COST = 500;

    private static final double SHARING_COST = 1000;

    /** Beyond this number of combinations, each transition takes its cheapest free vertex on its own */
    private static final long MAX_COMBINATIONS = 1024;

}
