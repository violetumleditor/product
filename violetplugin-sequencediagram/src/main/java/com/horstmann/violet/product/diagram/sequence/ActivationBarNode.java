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
import java.util.ArrayList;
import java.util.List;

import javax.lang.model.type.NullType;

import com.horstmann.violet.product.diagram.abstracts.Direction;
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
    public void removeChild(INode node)
    {
        super.removeChild(node);
    }

    @Override
    public boolean addConnection(IEdge edge)
    {
        INode endingNode = edge.getEndNode();
        INode startingNode = edge.getStartNode();
        if (startingNode == endingNode)
        {
            return false;
        }
        if (edge instanceof CallEdge)
        {
            return isCallEdgeAcceptable((CallEdge) edge);

        }
        else if (edge instanceof ReturnEdge)
        {
            return isReturnEdgeAcceptable((ReturnEdge) edge);
        }
        return false;
    }

    @Override
    public void removeConnection(IEdge e)
    {
        super.removeConnection(e);
    }

    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        boolean isCallEdge = e.getClass().isAssignableFrom(CallEdge.class);
        boolean isReturnEdge = e.getClass().isAssignableFrom(ReturnEdge.class);
        boolean isActivationBarNodeOnStart = e.getStartNode() != null
                && e.getStartNode().getClass().isAssignableFrom(ActivationBarNode.class);
        boolean isActivationBarNodeOnEnd = e.getEndNode() != null && e.getEndNode().getClass().isAssignableFrom(ActivationBarNode.class);
        boolean isLifelineNodeOnEnd = e.getEndNode() != null && e.getEndNode().getClass().isAssignableFrom(LifelineNode.class);
        if (isCallEdge)
        {
            if (isActivationBarNodeOnStart && isActivationBarNodeOnEnd)
            {
                ActivationBarNode startingNode = (ActivationBarNode) e.getStartNode();
                ActivationBarNode endingNode = (ActivationBarNode) e.getEndNode();
                LifelineNode startingLifelineNode = startingNode.getImplicitParameter();
                LifelineNode endingLifelineNode = endingNode.getImplicitParameter();
                boolean isSameLifelineNode = startingLifelineNode != null && endingLifelineNode != null
                        && startingLifelineNode.equals(endingLifelineNode);
                boolean isDifferentLifelineNodes = startingLifelineNode != null && endingLifelineNode != null
                        && !startingLifelineNode.equals(endingLifelineNode);
                // Case 1 : two activation bars connected on differents
                // LifeLines
                if (isDifferentLifelineNodes && isActivationBarNodeOnStart && isActivationBarNodeOnEnd)
                {
                    boolean isStartingNode = this.equals(e.getStartNode());
                    boolean isEndingNode = this.equals(e.getEndNode());
                    if (isStartingNode)
                    {
                        Point2D startingNodeLocation = getLocationOnGraph();
                        Point2D endingNodeLocation = e.getEndNode().getLocationOnGraph();
                        Direction d = e.getDirection(this);
                        if (d.getX() > 0)
                        {
                            double x = startingNodeLocation.getX();
                            double y = endingNodeLocation.getY();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                        else
                        {
                            double x = startingNodeLocation.getX() + DEFAULT_WIDTH;
                            double y = endingNodeLocation.getY();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                    }
                    if (isEndingNode)
                    {
                        Point2D endingNodeLocation = getLocationOnGraph();
                        Direction d = e.getDirection(this);
                        if (d.getX() > 0)
                        {
                            double x = endingNodeLocation.getX();
                            double y = endingNodeLocation.getY();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                        else
                        {
                            double x = endingNodeLocation.getX() + DEFAULT_WIDTH;
                            double y = endingNodeLocation.getY();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                    }
                }
                // Case 2 : two activation bars connected on same lifeline (self
                // call)
                if (isSameLifelineNode && isActivationBarNodeOnStart && isActivationBarNodeOnEnd)
                {
                	boolean isStartingNode = this.equals(e.getStartNode());
                    boolean isEndingNode = this.equals(e.getEndNode());
                    if (isStartingNode)
                    {
                        Point2D startingNodeLocation = getLocationOnGraph();
                        Point2D endingNodeLocation = e.getEndNode().getLocation();
                        double x = startingNodeLocation.getX() + DEFAULT_WIDTH;
                        double y = startingNodeLocation.getY() + endingNodeLocation.getY() - CALL_YGAP / 2;
                        Point2D p = new Point2D.Double(x, y);
                        p = getGraph().getGridSticker().snap(p);
                        return p;
                    }
                    if (isEndingNode)
                    {
                        Point2D endingNodeLocation = getLocationOnGraph();
                        double x = endingNodeLocation.getX() + DEFAULT_WIDTH;
                        double y = endingNodeLocation.getY();
                        Point2D p = new Point2D.Double(x, y);
                        p = getGraph().getGridSticker().snap(p);
                        return p;
                    }
                }
            }
            if (isActivationBarNodeOnStart && isLifelineNodeOnEnd)
            {
                Direction d = e.getDirection(this);
                Point2D startingNodeLocation = getLocationOnGraph();
                if (d.getX() > 0)
                {
                    double x = startingNodeLocation.getX();
                    double y = startingNodeLocation.getY() + CALL_YGAP / 2;
                    Point2D p = new Point2D.Double(x, y);
                    p = getGraph().getGridSticker().snap(p);
                    return p;
                }
                else
                {
                    double x = startingNodeLocation.getX() + DEFAULT_WIDTH;
                    double y = startingNodeLocation.getY() + CALL_YGAP / 2;
                    Point2D p = new Point2D.Double(x, y);
                    p = getGraph().getGridSticker().snap(p);
                    return p;
                }
            }
        }
        if (isReturnEdge)
        {
            if (isActivationBarNodeOnStart && isActivationBarNodeOnEnd)
            {
                ActivationBarNode startingNode = (ActivationBarNode) e.getStartNode();
                ActivationBarNode endingNode = (ActivationBarNode) e.getEndNode();
                LifelineNode startingLifelineNode = startingNode.getImplicitParameter();
                LifelineNode endingLifelineNode = endingNode.getImplicitParameter();
                boolean isDifferentLifelineNodes = startingLifelineNode != null && endingLifelineNode != null
                        && !startingLifelineNode.equals(endingLifelineNode);
                // Case 1 : two activation bars connected on differents
                // LifeLines
                if (isDifferentLifelineNodes && isActivationBarNodeOnStart && isActivationBarNodeOnEnd)
                {
                    boolean isStartingNode = this.equals(e.getStartNode());
                    boolean isEndingNode = this.equals(e.getEndNode());
                    if (isStartingNode)
                    {
                        Point2D startingNodeLocation = getLocationOnGraph();
                        Rectangle2D startingNodeBounds = getBounds();
                        Direction d = e.getDirection(this);
                        if (d.getX() > 0)
                        {
                            double x = startingNodeLocation.getX();
                            double y = startingNodeLocation.getY() + startingNodeBounds.getHeight();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                        else
                        {
                            double x = startingNodeLocation.getX() + DEFAULT_WIDTH;
                            double y = startingNodeLocation.getY() + startingNodeBounds.getHeight();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                    }
                    if (isEndingNode)
                    {
                        Point2D startingNodeLocation = e.getStartNode().getLocationOnGraph();
                        Rectangle2D startingNodeBounds = e.getStartNode().getBounds();
                        Point2D endingNodeLocation = getLocationOnGraph();
                        Direction d = e.getDirection(this);
                        if (d.getX() > 0)
                        {
                            double x = endingNodeLocation.getX();
                            double y = startingNodeLocation.getY() + startingNodeBounds.getHeight();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                        else
                        {
                            double x = endingNodeLocation.getX() + DEFAULT_WIDTH;
                            double y = startingNodeLocation.getY() + startingNodeBounds.getHeight();
                            Point2D p = new Point2D.Double(x, y);
                            p = getGraph().getGridSticker().snap(p);
                            return p;
                        }
                    }
                }
            }
        }
        // Default case
        Direction d = e.getDirection(this);
        if (d.getX() > 0)
        {
            double y = getBounds().getMinY();
            double x = getBounds().getMaxX();
            Point2D p = new Point2D.Double(x, y);
            p = getGraph().getGridSticker().snap(p);
            return p;
        }
        else
        {
            double y = getBounds().getMinY();
            double x = getBounds().getX();
            Point2D p = new Point2D.Double(x, y);
            p = getGraph().getGridSticker().snap(p);
            return p;
        }

    }





    /**
     * 
     * @return true if this activation bar has been called by another activation bar
     */
    private boolean isCalledNode()
    {
        LifelineNode currentLifelineNode = getImplicitParameter();
        for (IEdge edge : getGraph().getAllEdges())
        {
            if (edge.getEndNode() != this)
            {
                continue;
            }
            if (!edge.getClass().isAssignableFrom(CallEdge.class))
            {
                continue;
            }
            INode startingNode = edge.getStartNode();
            if (!startingNode.getClass().isAssignableFrom(ActivationBarNode.class))
            {
                continue;
            }
            if (((ActivationBarNode) startingNode).getImplicitParameter() == currentLifelineNode)
            {
                continue;
            }
            return true;
        }
        return false;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D nodeLocation = getLocation();
        // Height
        double height = getHeight();
        // TODO : manage openbottom
        Rectangle2D currentBounds = new Rectangle2D.Double(nodeLocation.getX(), nodeLocation.getY(), DEFAULT_WIDTH, height);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(currentBounds);
        return snappedBounds;
    }

    public double getHeight()
    {
        double height = DEFAULT_HEIGHT;
        height = Math.max(height, getHeightWhenLinked());
        height = Math.max(height, getHeightWhenHasChildren());
        return height;
    }

    /**
     * If this activation bar calls another activation bar on another life line, its height must be greater than the activation bar
     * which is called
     * 
     * @return h
     */
    private double getHeightWhenLinked()
    {
    	double height = 0;
    	for (IEdge edge : getGraph().getAllEdges())
        {
            if (!edge.getClass().isAssignableFrom(CallEdge.class))
            {
                continue;
            }
            if (edge.getStartNode() == this)
            {
                INode endingNode = edge.getEndNode();
                boolean isActivationBarNode = endingNode instanceof ActivationBarNode;
				if (isActivationBarNode)
                {
					Rectangle2D endingNodeBounds = endingNode.getBounds();
					double newHeight = CALL_YGAP / 2 + endingNodeBounds.getHeight() + (endingNode.getLocationOnGraph().getY() - this.getLocationOnGraph().getY()) + CALL_YGAP / 2;
					height = Math.max(height,  newHeight);
                }
            }
        }
        return Math.max(DEFAULT_HEIGHT, height);
    }

    /**
     * 
     * @return
     */
    private double getHeightWhenHasChildren()
    {
        double height = DEFAULT_HEIGHT;
        int childVisibleNodesCounter = 0;
        for (INode aNode : getChildren())
        {
            if (aNode instanceof ActivationBarNode)
            {
                childVisibleNodesCounter++;
            }
        }
        if (childVisibleNodesCounter > 0)
        {
            for (INode aNode : getChildren())
            {
                if (aNode instanceof ActivationBarNode)
                {
                	ActivationBarNode anActivationBarNode = (ActivationBarNode) aNode;
                	double h = anActivationBarNode.getHeight();
                	double v = anActivationBarNode.getLocation().getY();
                	double maxY = v + h;
                    height = Math.max(height, maxY);
                }
            }
            height = height + CALL_YGAP;
        }
        return height;
    }
    
    @Override
    public void setLocation(Point2D aPoint)
    {
        INode parentNode = getParent();
        // Special use case : when this node is connected to another activation bar on another life line,
        // we adjust its location to keep it relative to the node it is connected to
        if (parentNode != null && parentNode.getClass().isAssignableFrom(LifelineNode.class)) {
            LifelineNode lifelineNode = getImplicitParameter();
            Rectangle2D topRectangle = lifelineNode.getTopRectangle();
            if (aPoint.getY() <= topRectangle.getHeight() + CALL_YGAP) {
                aPoint = new Point2D.Double(aPoint.getX(), topRectangle.getHeight() + CALL_YGAP);
            }
        }
        super.setLocation(aPoint);
    }
    
    @Override
    public Point2D getLocation()
    {
        if (this.locationCache != null) {
        	return this.locationCache;
        }
    	INode parentNode = getParent();
        if (parentNode == null) {
        	this.locationCache = super.getLocation();
            return this.locationCache;
        }
        List<IEdge> connectedEdges = getConnectedEdges();
        boolean isChildOfActivationBarNode = (parentNode.getClass().isAssignableFrom(ActivationBarNode.class));
        boolean isChildOfLifelineNode = (parentNode.getClass().isAssignableFrom(LifelineNode.class));
        // Case 1 : just attached to a lifeline
        if (isChildOfLifelineNode && connectedEdges.isEmpty()) {
            Point2D rawLocation = super.getLocation();
            double horizontalLocation = getHorizontalLocation();
            double verticalLocation = rawLocation.getY();
            Point2D adjustedLocation = new Point2D.Double(horizontalLocation, verticalLocation);
            adjustedLocation = getGraph().getGridSticker().snap(adjustedLocation);
            super.setLocation(adjustedLocation);
            this.locationCache = adjustedLocation;
            return adjustedLocation;
        }
        // Case 2 : is child of another activation bar
        if (isChildOfActivationBarNode && connectedEdges.isEmpty()) {
            Point2D rawLocation = super.getLocation();
            double horizontalLocation = getHorizontalLocation();
            double verticalLocation = rawLocation.getY();
            verticalLocation = Math.max(verticalLocation, CALL_YGAP);
            Point2D adjustedLocation = new Point2D.Double(horizontalLocation, verticalLocation);
            adjustedLocation = getGraph().getGridSticker().snap(adjustedLocation);
            super.setLocation(adjustedLocation);
            this.locationCache = adjustedLocation;
            return adjustedLocation;
        }
        // Case 3 : is connected
        if (!connectedEdges.isEmpty()) {
            Point2D rawLocation = super.getLocation();
            for (IEdge edge : getConnectedEdges()) {
                if (!edge.getClass().isAssignableFrom(CallEdge.class))
                {
                    continue;
                }
                if (edge.getEndNode() == this)
                {
                    INode startingNode = edge.getStartNode();
                    Point2D startingNodeLocationOnGraph = startingNode.getLocationOnGraph();
                    Point2D endingNodeParentLocationOnGraph = getParent().getLocationOnGraph();
                    double yGap = rawLocation.getY() - startingNodeLocationOnGraph.getY() + endingNodeParentLocationOnGraph.getY();
                    if (yGap < CALL_YGAP / 2) {
                        double horizontalLocation = getHorizontalLocation();
                        double minY = startingNodeLocationOnGraph.getY() - endingNodeParentLocationOnGraph.getY() + CALL_YGAP / 2;
                        Point2D adjustedLocation = new Point2D.Double(horizontalLocation, minY);
                        adjustedLocation = getGraph().getGridSticker().snap(adjustedLocation);
                        super.setLocation(adjustedLocation);
                        this.locationCache = adjustedLocation;
                        return adjustedLocation;
                    }
                    break;
                }
            }
        }
        // Case 4 : default case
        Point2D rawLocation = super.getLocation();
        double horizontalLocation = getHorizontalLocation();
        double verticalLocation = rawLocation.getY();
        Point2D adjustedLocation = new Point2D.Double(horizontalLocation, verticalLocation);
        adjustedLocation = getGraph().getGridSticker().snap(adjustedLocation);
        super.setLocation(adjustedLocation);
        this.locationCache = adjustedLocation;
        return adjustedLocation;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        // Reset location cache;
    	this.locationCache = null;
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
        // Reset location for next draw
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
        if (this.lifeline == null) {
        	INode aParent = this.getParent();
        	List<INode> nodeStack = new ArrayList<INode>();
        	nodeStack.add(aParent);
        	while (!nodeStack.isEmpty()) {
        		INode aNode = nodeStack.get(0);
        		if (LifelineNode.class.isInstance(aNode)) {
        			this.lifeline = (LifelineNode) aNode;
        		}
        		if (ActivationBarNode.class.isInstance(aNode)) {
        			INode aNodeParent = aNode.getParent();
        			nodeStack.add(aNodeParent);
        		}
        		nodeStack.remove(0);
        	}
        }
    	return lifeline;
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

    private boolean isReturnEdgeAcceptable(ReturnEdge edge)
    {
        INode endingNode = edge.getEndNode();
        INode startingNode = edge.getStartNode();
        if (startingNode == null || endingNode == null) {
        	return false;
        }
        Class<? extends INode> startingNodeClass = startingNode.getClass();
        Class<? extends INode> endingNodeClass = endingNode.getClass();
        if (!startingNodeClass.isAssignableFrom(ActivationBarNode.class)
                || !endingNodeClass.isAssignableFrom(ActivationBarNode.class))
        {
            return false;
        }
        ActivationBarNode startingActivationBarNode = (ActivationBarNode) startingNode;
        ActivationBarNode endingActivationBarNode = (ActivationBarNode) endingNode;
        if (startingActivationBarNode.getImplicitParameter() == endingActivationBarNode.getImplicitParameter())
        {
            return false;
        }
        if (!isCalledNode())
        {
            return false;
        }
        return true;
    }

    private boolean isCallEdgeAcceptable(CallEdge edge)
    {
        INode endingNode = edge.getEndNode();
        INode startingNode = edge.getStartNode();
        Point2D endingNodePoint = edge.getEndLocation();
        Class<?> startingNodeClass = (startingNode != null ? startingNode.getClass() : NullType.class);
        Class<?> endingNodeClass = (endingNode != null ? endingNode.getClass() : NullType.class);
        // Case 1 : check is call edge doesn't connect already connected nodes
        if (startingNode != null && endingNode != null) {
        	for (IEdge anEdge : getGraph().getAllEdges()) {
        		if (!anEdge.getClass().isAssignableFrom(CallEdge.class)) {
        			continue;
        		}
        		INode anEdgeStartingNode = anEdge.getStartNode();
        		INode anEdgeEndingNode = anEdge.getEndNode();
        		if (startingNode.equals(anEdgeStartingNode) && endingNode.equals(anEdgeEndingNode)) {
        			return false;
        		}
        	}
        }
        // Case 2 : classic connection between activation bars
        if (startingNodeClass.isAssignableFrom(ActivationBarNode.class)
                && endingNodeClass.isAssignableFrom(ActivationBarNode.class))
        {
            return true;
        }
        // Case 3 : an activation bar creates a new class instance
        if (startingNodeClass.isAssignableFrom(ActivationBarNode.class) && endingNodeClass.isAssignableFrom(LifelineNode.class))
        {
            ActivationBarNode startingActivationBarNode = (ActivationBarNode) startingNode;
            LifelineNode startingLifeLineNode = startingActivationBarNode.getImplicitParameter();
            LifelineNode endingLifeLineNode = (LifelineNode) endingNode;
            Rectangle2D topRectangle = endingLifeLineNode.getTopRectangle();
            if (startingLifeLineNode != endingLifeLineNode && topRectangle.contains(endingNodePoint))
            {
                return true;
            }
        }
        // Case 4 : classic connection between activation bars but the ending
        // bar doesn't exist and need to be automatically created
        if (startingNodeClass.isAssignableFrom(ActivationBarNode.class) && endingNodeClass.isAssignableFrom(LifelineNode.class))
        {
            ActivationBarNode startingActivationBarNode = (ActivationBarNode) startingNode;
            LifelineNode startingLifeLineNode = startingActivationBarNode.getImplicitParameter();
            LifelineNode endingLifeLineNode = (LifelineNode) endingNode;
            Rectangle2D topRectangle = endingLifeLineNode.getTopRectangle();
            if (startingLifeLineNode != endingLifeLineNode && !topRectangle.contains(endingNodePoint))
            {
                ActivationBarNode newActivationBar = new ActivationBarNode();
                Point2D edgeStartLocation = edge.getEndLocation();
                double x = edgeStartLocation.getX();
                double y = edgeStartLocation.getY();
                Point2D newActivationBarLocation = new Point2D.Double(x, y);
                endingNode.addChild(newActivationBar, newActivationBarLocation);
                edge.setEndNode(newActivationBar);
                return true;
            }
        }
        // Case 5 : self call on an activation bar
        if (startingNodeClass.isAssignableFrom(ActivationBarNode.class) && endingNodeClass.isAssignableFrom(LifelineNode.class))
        {
            ActivationBarNode startingActivationBarNode = (ActivationBarNode) startingNode;
            LifelineNode startingLifeLineNode = startingActivationBarNode.getImplicitParameter();
            LifelineNode endingLifeLineNode = (LifelineNode) endingNode;
            Rectangle2D topRectangle = endingLifeLineNode.getTopRectangle();
            if (startingLifeLineNode == endingLifeLineNode && !topRectangle.contains(endingNodePoint))
            {
                ActivationBarNode newActivationBar = new ActivationBarNode();
                Point2D edgeStartLocation = edge.getStartLocation();
                double x = edgeStartLocation.getX();
                double y = edgeStartLocation.getY() + CALL_YGAP / 2;
                Point2D newActivationBarLocation = new Point2D.Double(x, y);
                startingNode.addChild(newActivationBar, newActivationBarLocation);
                edge.setEndNode(newActivationBar);
                return true;
            }
        }
        // Case 6 : self call on an activation bar but its child which is called
        // doesn"t exist and need to be created automatically
        if (startingNodeClass.isAssignableFrom(ActivationBarNode.class) && endingNodeClass.isAssignableFrom(NullType.class))
        {
            ActivationBarNode newActivationBar = new ActivationBarNode();
            edge.getStartLocation();
            startingNode.addChild(newActivationBar, edge.getStartLocation());
            edge.setEndNode(newActivationBar);
            return true;
        }
        return false;
    }



    /**
     * @return x location relative to the parent
     */
    private double getHorizontalLocation()
    {
        INode parentNode = getParent();
        if (parentNode != null && parentNode.getClass().isAssignableFrom(ActivationBarNode.class))
        {
            return DEFAULT_WIDTH / 2;
        }
        if (parentNode != null && parentNode.getClass().isAssignableFrom(LifelineNode.class))
        {
            LifelineNode lifeLineNode = (LifelineNode) parentNode;
            Rectangle2D lifeLineTopRectangle = lifeLineNode.getTopRectangle();
            return lifeLineTopRectangle.getWidth() / 2 - DEFAULT_WIDTH / 2;
        }
        return 0;
    }

 
 



    /** The lifeline that embeds this activation bar in the sequence diagram */
    private transient LifelineNode lifeline;
    
    private transient Point2D locationCache;
    
    /** Default with */
    private static int DEFAULT_WIDTH = 16;

    /** Default height */
    private static int DEFAULT_HEIGHT = 30;

    /** Default vertical gap between two call nodes and a call node and an implicit node */
    public static int CALL_YGAP = 20;
}
