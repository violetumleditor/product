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

package com.horstmann.violet.product.diagram.sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.GeometryCache;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;

/**
 * An activation bar in a sequence diagram. This activation bar is hang on a lifeline (implicit parameter)
 */
public class ActivationBarNode extends RectangularNode
{

    @Override
    public boolean addChild(INode n, Point2D p)
    {
        if (!canAddChild(n))
        {
            return false;
        }
        n.setParent(this);
        n.setGraph(getGraph());
        n.setLocation(p);
        addChild(n, getChildren().size());
        return true;
    }

    /**
     * Only activation bars (nested executions)
     */
    @Override
    public boolean canAddChild(INode n)
    {
        return n.getClass().isAssignableFrom(ActivationBarNode.class);
    }


    @Override
    public boolean addConnection(IEdge edge)
    {
        if (edge.getStartNode() == edge.getEndNode() || !(edge.getStartNode() instanceof ActivationBarNode))
        {
            return false;
        }
        if (edge instanceof MessageEdge && ((MessageEdge) edge).isPutBack())
        {
            return true;
        }
        if (edge instanceof CallEdge)
        {
            return addCall((CallEdge) edge, (ActivationBarNode) edge.getStartNode());
        }
        return edge instanceof ReturnEdge && isReturnAcceptable((ReturnEdge) edge);
    }

    @Override
    public boolean isConnectionPointFixed()
    {
        return true;
    }

    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        Point2D p = null;
        if (e instanceof CallEdge)
        {
            p = getCallPoint((CallEdge) e);
        }
        else if (e instanceof ReturnEdge)
        {
            p = getReturnPoint((ReturnEdge) e);
        }
        if (p == null)
        {
            // Other kinds of edges
            Rectangle2D bounds = getBounds();
            p = new Point2D.Double(e.getDirection(this).getX() > 0 ? bounds.getMaxX() : bounds.getX(), bounds.getMinY());
        }
        return getGraph().getGridSticker().snap(p);
    }

    /**
     * A call is drawn at the top of the bar it reaches; the one to this lifeline (self call) leaves and comes back on the right
     */
    private Point2D getCallPoint(CallEdge call)
    {
        Point2D location = getLocationOnGraph();
        INode endNode = call.getEndNode();
        if (endNode instanceof LifelineNode)
        {
            return new Point2D.Double(getXOfSide(call), location.getY() + CALL_YGAP / 2);
        }
        if (!isBetweenBars(call))
        {
            return null;
        }
        double y = endNode.getLocationOnGraph().getY();
        if (!isBetweenLifelines(call))
        {
            return new Point2D.Double(location.getX() + DEFAULT_WIDTH, this == endNode ? y : y - CALL_YGAP / 2);
        }
        return new Point2D.Double(getXOfSide(call), y);
    }

    /**
     * A return is drawn at the bottom of the bar it leaves
     */
    private Point2D getReturnPoint(ReturnEdge edge)
    {
        if (!isBetweenBars(edge) || !isBetweenLifelines(edge))
        {
            return null;
        }
        INode startNode = edge.getStartNode();
        return new Point2D.Double(getXOfSide(edge), startNode.getLocationOnGraph().getY() + startNode.getBounds().getHeight());
    }

    /**
     * @return true if the edge links two bars that are on lifelines
     */
    private boolean isBetweenBars(IEdge edge)
    {
        return edge.getStartNode() instanceof ActivationBarNode && edge.getEndNode() instanceof ActivationBarNode
                && ((ActivationBarNode) edge.getStartNode()).getImplicitParameter() != null
                && ((ActivationBarNode) edge.getEndNode()).getImplicitParameter() != null;
    }

    private boolean isBetweenLifelines(IEdge edge)
    {
        return ((ActivationBarNode) edge.getStartNode()).getImplicitParameter() != ((ActivationBarNode) edge.getEndNode())
                .getImplicitParameter();
    }

    /**
     * @return x, on the graph, of the side the edge is on
     */
    private double getXOfSide(IEdge edge)
    {
        double x = getLocationOnGraph().getX();
        return edge.getDirection(this).getX() > 0 ? x : x + DEFAULT_WIDTH;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        // TODO : manage openbottom
        double ownHeight = Math.max(DEFAULT_HEIGHT, getHeightWhenHasChildren());
        Rectangle2D bounds = getGraph().getGridSticker().snap(
                new Rectangle2D.Double(location.getX(), location.getY(), DEFAULT_WIDTH, ownHeight));
        // Not rounded like the rest: the space below the last return must be the one above the first call
        bounds.setFrame(bounds.getX(), bounds.getY(), bounds.getWidth(), Math.max(bounds.getHeight(), getHeightWhenLinked()));
        return bounds;
    }

    public double getHeight()
    {
        return Math.max(DEFAULT_HEIGHT, Math.max(getHeightWhenLinked(), getHeightWhenHasChildren()));
    }

    /**
     * @return the height needed to go below the bars called by this one, 0 if none. Below those of other lifelines (where the
     *         returns are), the same space as the one kept above a call (see getGapBelowCaller).
     */
    private double getHeightWhenLinked()
    {
        // Each bar asks the bars it calls, which ask theirs, for each edge end and each bound of a paint: computed once per
        // pass over the graph (see GeometryCache)
        Double cachedHeight = (Double) GeometryCache.get(this, HEIGHT_WHEN_LINKED_CACHE_KEY);
        if (cachedHeight != null)
        {
            return cachedHeight;
        }
        double height = computeHeightWhenLinked();
        GeometryCache.put(this, HEIGHT_WHEN_LINKED_CACHE_KEY, height);
        return height;
    }

    private double computeHeightWhenLinked()
    {
        double height = 0;
        for (IEdge edge : getGraph().getAllEdges())
        {
            if (edge instanceof CallEdge && edge.getStartNode() == this && edge.getEndNode() instanceof ActivationBarNode)
            {
                ActivationBarNode callee = (ActivationBarNode) edge.getEndNode();
                double calleeY = callee.getLocationOnGraph().getY() - getLocationOnGraph().getY();
                double calleeBottom = calleeY + callee.getBounds().getHeight();
                if (callee.getImplicitParameter() == getImplicitParameter())
                {
                    height = Math.max(height, calleeBottom + CALL_YGAP); // self call, nested in this bar
                }
                else
                {
                    height = Math.max(height, calleeBottom + callee.getGapBelowCaller(this));
                }
            }
        }
        return height;
    }

    /**
     * @return the height needed to go below the bars nested in this one, 0 if none
     */
    private double getHeightWhenHasChildren()
    {
        Double cachedHeight = (Double) GeometryCache.get(this, HEIGHT_WHEN_HAS_CHILDREN_CACHE_KEY);
        if (cachedHeight != null)
        {
            return cachedHeight;
        }
        double height = computeHeightWhenHasChildren();
        GeometryCache.put(this, HEIGHT_WHEN_HAS_CHILDREN_CACHE_KEY, height);
        return height;
    }

    private double computeHeightWhenHasChildren()
    {
        double height = 0;
        for (INode child : getChildren())
        {
            if (child instanceof ActivationBarNode)
            {
                height = Math.max(height, child.getLocation().getY() + ((ActivationBarNode) child).getHeight() + CALL_YGAP);
            }
        }
        return height;
    }

    /**
     * A bar stays below the head of its lifeline
     */
    @Override
    public void setLocation(Point2D aPoint)
    {
        setSiblingShift(0);
        if (getParent() instanceof LifelineNode)
        {
            double minY = ((LifelineNode) getParent()).getTopRectangle().getHeight() + CALL_YGAP;
            super.setLocation(new Point2D.Double(aPoint.getX(), Math.max(minY, aPoint.getY())));
            return;
        }
        super.setLocation(aPoint);
    }

    @Override
    public Point2D getLocation()
    {
        if (this.locationCache == null) {
            Point2D ownLocation = getOwnLocation();
            this.locationCache = new Point2D.Double(ownLocation.getX(), ownLocation.getY() + this.siblingShift);
        }
        return this.locationCache;
    }

    /**
     * @return the location of this bar before it is moved down to keep off its siblings: below its caller, if any
     */
    Point2D getOwnLocation()
    {
        if (getParent() == null)
        {
            return super.getLocation();
        }
        double y = super.getLocation().getY();
        Double minY = getMinYBelowCaller();
        if (minY != null && y < minY)
        {
            // Not saved: the caller may only be there for a while, pushed by a bar being dragged
            return getGraph().getGridSticker().snap(new Point2D.Double(getHorizontalLocation(), minY));
        }
        if (getParent() instanceof ActivationBarNode && getConnectedEdges().isEmpty())
        {
            y = Math.max(y, CALL_YGAP);
        }
        Point2D location = getGraph().getGridSticker().snap(new Point2D.Double(getHorizontalLocation(), y));
        super.setLocation(location);
        return location;
    }

    /**
     * @return the highest place in its parent below the top of its caller, null if this bar isn't called
     */
    private Double getMinYBelowCaller()
    {
        for (IEdge edge : getGraph().getAllEdges())
        {
            if (edge instanceof CallEdge && edge.getEndNode() == this)
            {
                INode caller = edge.getStartNode();
                return caller.getLocationOnGraph().getY() - getParent().getLocationOnGraph().getY() + getGapBelowCaller(caller);
            }
        }
        return null;
    }

    /**
     * @return the space between the top of the caller and the top of this bar: the loop of a self call leaves its caller below its
     *         top, as far from it as from this bar
     */
    double getGapBelowCaller(INode caller)
    {
        boolean isSelfCall = caller instanceof ActivationBarNode
                && ((ActivationBarNode) caller).getImplicitParameter() == getImplicitParameter();
        return isSelfCall ? CALL_YGAP : CALL_YGAP / 2;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        resetLocationCache();
        // Backup current color;
        Color oldColor = g2.getColor();
        // Translate g2 if node has parent
        Point2D nodeLocationOnGraph = getLocationOnGraph();
        Point2D nodeLocation = getLocation();
        Rectangle2D b = getBounds();
        Point2D g2Location = new Point2D.Double(nodeLocationOnGraph.getX() - nodeLocation.getX(), nodeLocationOnGraph.getY()
                - nodeLocation.getY());
        g2.translate(g2Location.getX(), g2Location.getY());
        // Perform painting
        super.draw(g2);
        g2.setColor(getBackgroundColor());
        g2.fill(b);
        g2.setColor(getBorderColor());
        g2.draw(b);
        // Restore g2 original location
        g2.translate(-g2Location.getX(), -g2Location.getY());
        // Restore first color
        g2.setColor(oldColor);
        // Draw its children
        for (INode node : getChildren())
        {
            node.draw(g2);
        }
    }

    /**
     * Gets the participant's life line of this call. Note : method's name is ot set to getLifeLine to keep compatibility with older
     * versions
     * 
     * @return the participant's life line
     */
    public LifelineNode getImplicitParameter()
    {
        // Not cached: the parent is only known once the bar is added
        INode aNode = getParent();
        while (aNode instanceof ActivationBarNode)
        {
            aNode = aNode.getParent();
        }
        return aNode instanceof LifelineNode ? (LifelineNode) aNode : null;
    }

    @Override
    public void setParent(INode node)
    {
        resetLocationCache();
        super.setParent(node);
    }

    /**
     * Sets the participant's life line of this call. Note : method's name is ot set to setLifeLine to keep compatibility with older
     * versions
     * 
     * @param newValue the participant's lifeline
     */
    public void setImplicitParameter(LifelineNode newValue)
    {
        // Nothing to do. Just here to keep compatibility.
    }

    private boolean isReturnAcceptable(ReturnEdge edge)
    {
        return edge.getEndNode() instanceof ActivationBarNode && isBetweenLifelines(edge)
                && new CallRules(getGraph()).isReturnAcceptable(edge.getStartNode(), edge.getEndNode());
    }

    /**
     * @return true if the call is accepted; the bar it reaches is created if needed
     */
    private boolean addCall(CallEdge call, ActivationBarNode caller)
    {
        CallRules callRules = new CallRules(getGraph());
        INode end = call.getEndNode();
        if (end == null)
        {
            // Self call
            return addCalledBar(call, caller, call.getStartLocation());
        }
        if (end instanceof ActivationBarNode)
        {
            return callRules.isCallAcceptable(caller, (ActivationBarNode) end);
        }
        if (!(end instanceof LifelineNode))
        {
            return false;
        }
        LifelineNode lifeline = (LifelineNode) end;
        Point2D endLocation = call.getEndLocation();
        boolean isSelfCall = caller.getImplicitParameter() == lifeline;
        if (lifeline.getTopRectangle().contains(endLocation))
        {
            // Creation of the lifeline
            return !isSelfCall && callRules.isCreationAcceptable(caller, lifeline);
        }
        if (isSelfCall)
        {
            Point2D startLocation = call.getStartLocation();
            return addCalledBar(call, caller, new Point2D.Double(startLocation.getX(), startLocation.getY() + CALL_YGAP / 2));
        }
        ActivationBarNode runningBar = callRules.getRunningBar(caller, lifeline);
        if (runningBar == null)
        {
            return addCalledBar(call, lifeline, endLocation);
        }
        // Call back: nested in the bar that is still running
        double yOnGraph = lifeline.getLocationOnGraph().getY() + endLocation.getY();
        return addCalledBar(call, runningBar,
                new Point2D.Double(endLocation.getX(), yOnGraph - runningBar.getLocationOnGraph().getY()));
    }

    private boolean addCalledBar(CallEdge call, INode parent, Point2D location)
    {
        ActivationBarNode bar = new ActivationBarNode();
        parent.addChild(bar, new Point2D.Double(location.getX(), location.getY()));
        call.setEndNode(bar);
        return true;
    }

    /**
     * @return x location relative to the parent
     */
    private double getHorizontalLocation()
    {
        if (getParent() instanceof LifelineNode)
        {
            return ((LifelineNode) getParent()).getTopRectangle().getWidth() / 2 - DEFAULT_WIDTH / 2;
        }
        return DEFAULT_WIDTH / 2;
    }

    /**
     * @param shift how much this bar is moved down to keep off its siblings (see ActivationBarSpacing), never saved
     */
    void setSiblingShift(double shift)
    {
        this.siblingShift = shift;
        resetLocationCache();
    }

    /**
     * @return the location asked by the user, before any adjustment
     */
    Point2D getAskedLocation()
    {
        return super.getLocation();
    }

    double getSiblingShift()
    {
        return this.siblingShift;
    }

    void resetLocationCache()
    {
        this.locationCache = null;
    }

    /**
     * @param rank rank among the children of its parent when removed from the graph, null once put back
     */
    void setRemovedRank(Integer rank)
    {
        this.removedRank = rank;
    }

    Integer getRemovedRank()
    {
        return this.removedRank;
    }

    @Override
    public ActivationBarNode clone()
    {
        ActivationBarNode cloned = (ActivationBarNode) super.clone();
        cloned.removedRank = null;
        return cloned;
    }

    /** Rank among the children of its parent when removed from the graph, null otherwise */
    private transient Integer removedRank;

    /** See ActivationBarSpacing */
    private transient double siblingShift;

    private transient Point2D locationCache;
    
    /** Default width */
    private static final int DEFAULT_WIDTH = 16;

    /** Default height */
    private static final int DEFAULT_HEIGHT = 30;

    /** Default vertical gap between two call nodes and a call node and an implicit node */
    public static final int CALL_YGAP = 20;

    /** What {@link #getHeightWhenLinked()} remembers in a {@link GeometryCache} pass */
    private static final Object HEIGHT_WHEN_LINKED_CACHE_KEY = new Object();

    /** What {@link #getHeightWhenHasChildren()} remembers in a {@link GeometryCache} pass */
    private static final Object HEIGHT_WHEN_HAS_CHILDREN_CACHE_KEY = new Object();
}
