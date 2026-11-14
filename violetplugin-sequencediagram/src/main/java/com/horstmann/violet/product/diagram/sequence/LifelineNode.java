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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * An object node in a scenario diagram.
 */
public class LifelineNode extends RectangularNode
{
    /**
     * Construct an object node with a default size
     */
    public LifelineNode()
    {
        name = new MultiLineString();
        name.setUnderlined(true);
    }

    /**
     * Sets the name property value.
     * 
     * @param newValue the name of this object
     */
    public void setName(MultiLineString n)
    {
        name = n;
    }

    /**
     * Gets the name property value.
     * 
     * @return the name of this object
     */
    public MultiLineString getName()
    {
        return name;
    }

    public boolean addConnection(IEdge e)
    {
        return false;
    }

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
     * Only activation bars
     */
    @Override
    public boolean canAddChild(INode n)
    {
        return n.getClass().isAssignableFrom(ActivationBarNode.class);
    }



    @Override
    public Point2D getConnectionPoint(IEdge e)
    {
        Direction d = e.getDirection(this);
        Point2D locationOnGraph = getLocationOnGraph();
        Rectangle2D topRectBounds = getTopRectangle();
        if (d.getX() > 0)
        {
            return new Point2D.Double(locationOnGraph.getX(), locationOnGraph.getY() + topRectBounds.getHeight() / 2);
        }
        return new Point2D.Double(locationOnGraph.getX() + topRectBounds.getWidth(), locationOnGraph.getY()
                + topRectBounds.getHeight() / 2);
    }

    @Override
    public Point2D getLocation()
    {
        IGraph currentGraph = getGraph();
        if (currentGraph == null)
        {
            return new Point2D.Double(0, 0);
        }
        Collection<IEdge> edges = currentGraph.getAllEdges();
        for (IEdge edge : edges)
        {
            if (edge instanceof CallEdge)
            {
                INode endingNode = edge.getEndNode();
                if (endingNode == this)
                {
                    INode startingNode = edge.getStartNode();
                    Point2D locationOnGraph = startingNode.getLocationOnGraph();
                    Point2D realLocation = super.getLocation();
                    Point2D fixedLocation = new Point2D.Double(realLocation.getX(), locationOnGraph.getY()
                            - getTopRectangleHeight() / 2 + ActivationBarNode.CALL_YGAP / 2);
                    return fixedLocation;
                }
            }
        }
        Point2D realLocation = super.getLocation();
        Point2D fixedLocation = new Point2D.Double(realLocation.getX(), 0);
        return fixedLocation;
    }

    /**
     * Returns the rectangle at the top of the object node.
     * 
     * @return the top rectangle
     */
    public Rectangle2D getTopRectangle()
    {
        double topWidth = getTopRectangleWidth();
        double topHeight = getTopRectangleHeight();
        Rectangle2D topRectangle = new Rectangle2D.Double(0, 0, topWidth, topHeight);
        Rectangle2D snappedRectangle = getGraph().getGridSticker().snap(topRectangle);
        return snappedRectangle;
    }

    private double getTopRectangleHeight()
    {
        Rectangle2D bounds = name.getBounds();
        double topHeight = Math.max(bounds.getHeight(), DEFAULT_TOP_HEIGHT);
        return topHeight;
    }

    private double getTopRectangleWidth()
    {
        Rectangle2D bounds = name.getBounds();
        double topWidth = Math.max(bounds.getWidth(), DEFAULT_WIDTH);
        return topWidth;
    }

    @Override
    public Rectangle2D getBounds()
    {
        double topRectWidth = getTopRectangle().getWidth();
        double height = getLocalHeight();
        Point2D nodeLocation = getLocation();
        Rectangle2D bounds = new Rectangle2D.Double(nodeLocation.getX(), nodeLocation.getY(), topRectWidth, height);
        Rectangle2D scaledBounds = getScaledBounds(bounds);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(scaledBounds);
        return snappedBounds;
    }

    public double getLocalHeight()
    {
        double topRectHeight = getTopRectangle().getHeight();
        double height = topRectHeight; // default initial height
        List<INode> children = getChildren();
        for (INode n : children)
        {
            if (n.getClass().isAssignableFrom(ActivationBarNode.class))
            {
                // We are looking for the last activation bar node to get the total height needed
                height = Math.max(height, n.getBounds().getMaxY());
            }
        }
        height = height + ActivationBarNode.CALL_YGAP * 2;
        return height;
    }

    private Rectangle2D getScaledBounds(Rectangle2D bounds)
    {
        double x = bounds.getX();
        double y = bounds.getY();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double diffY = this.maxYOverAllLifeLineNodes - bounds.getMaxY();
        if (diffY > 0)
        {
            h = h + diffY;
        }
        return new Rectangle2D.Double(x, y, w, h);
    }

    @Override
    public Rectangle2D getShape()
    {
        Point2D currentLocation = getLocation();
        Rectangle2D topRectangle = getTopRectangle();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = topRectangle.getWidth();
        double h = topRectangle.getHeight();
        return new Rectangle2D.Double(x, y, w, h);
    }

    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Perform drawing
        Rectangle2D top = getShape();
        g2.setColor(getBackgroundColor());
        g2.fill(top);
        g2.setColor(getBorderColor());
        g2.draw(top);
        g2.setColor(getTextColor());
        name.draw(g2, top);
        double xmid = top.getCenterX();
        Line2D line = new Line2D.Double(xmid, top.getMaxY(), xmid, getMaxYOverAllLifeLineNodes());
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, new float[]
        {
                5.0f,
                5.0f
        }, 0.0f));
        g2.setColor(getBorderColor());
        g2.draw(line);
        g2.setStroke(oldStroke);

        // Restore first color
        g2.setColor(oldColor);
        // Draw its children
        for (INode node : getChildren())
        {
            node.draw(g2);
        }
    }

    private double getMaxYOverAllLifeLineNodes()
    {
        double maxY = this.getLocalHeight();
        IGraph graph = getGraph();
        if (graph == null)
        {
            return maxY;
        }
        Collection<INode> nodes = graph.getAllNodes();
        for (INode node : nodes)
        {
            if (!node.getClass().isAssignableFrom(LifelineNode.class))
            {
                continue;
            }
            LifelineNode aLifeLineNode = (LifelineNode) node;
            double localY = aLifeLineNode.getLocalHeight() + aLifeLineNode.getLocationOnGraph().getY();
            maxY = Math.max(maxY, localY);
        }
        this.maxYOverAllLifeLineNodes = maxY;
        return maxY;
    }

    public boolean contains(Point2D p)
    {
        Rectangle2D bounds = getBounds();
        return bounds.getX() <= p.getX() && p.getX() <= bounds.getX() + bounds.getWidth();
    }

    public LifelineNode clone()
    {
        LifelineNode cloned = (LifelineNode) super.clone();
        cloned.name = name.clone();
        return cloned;
    }

    private MultiLineString name;
    private transient double maxYOverAllLifeLineNodes = 0;
    private static int DEFAULT_TOP_HEIGHT = 60;
    private static int DEFAULT_WIDTH = 80;
}
