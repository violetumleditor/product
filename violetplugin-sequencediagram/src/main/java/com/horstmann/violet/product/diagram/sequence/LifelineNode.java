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

import com.horstmann.violet.product.diagram.abstracts.Direction;
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
    public boolean isConnectionPointFixed()
    {
        return true;
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

    /**
     * At the top of the diagram, or beside the call that creates it
     */
    @Override
    public Point2D getLocation()
    {
        double x = super.getLocation().getX();
        for (IEdge edge : getGraph().getAllEdges())
        {
            if (edge instanceof CallEdge && edge.getEndNode() == this)
            {
                double creatorY = edge.getStartNode().getLocationOnGraph().getY();
                return new Point2D.Double(x, creatorY - getTopRectangleHeight() / 2 + ActivationBarNode.CALL_YGAP / 2);
            }
        }
        return new Point2D.Double(x, 0);
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

    protected double getTopRectangleHeight()
    {
        return Math.max(name.getBounds().getHeight(), DEFAULT_TOP_HEIGHT);
    }

    protected double getTopRectangleWidth()
    {
        return Math.max(name.getBoundsWithMargins().getWidth(), DEFAULT_WIDTH);
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
        double height = getTopRectangle().getHeight();
        for (INode child : getChildren())
        {
            if (child instanceof ActivationBarNode)
            {
                height = Math.max(height, child.getBounds().getMaxY());
            }
        }
        return height + ActivationBarNode.CALL_YGAP * 2;
    }

    /**
     * All lifelines go down to the same place
     */
    private Rectangle2D getScaledBounds(Rectangle2D bounds)
    {
        double height = Math.max(bounds.getHeight(), this.maxYOverAllLifeLineNodes - bounds.getY());
        return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), height);
    }

    @Override
    public Rectangle2D getShape()
    {
        Point2D location = getLocation();
        Rectangle2D top = getTopRectangle();
        return new Rectangle2D.Double(location.getX(), location.getY(), top.getWidth(), top.getHeight());
    }

    public void draw(Graphics2D g2)
    {
        super.draw(g2);

        // Backup current color;
        Color oldColor = g2.getColor();

        // Perform drawing
        Rectangle2D top = getShape();
        drawTop(g2, top);
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

    /**
     * Draws the head of the lifeline, above its dashed line
     * 
     * @param g2 the graphics
     * @param top the head bounds, on the graph
     */
    protected void drawTop(Graphics2D g2, Rectangle2D top)
    {
        g2.setColor(getBackgroundColor());
        g2.fill(top);
        g2.setColor(getBorderColor());
        g2.draw(top);
        g2.setColor(getTextColor());
        name.draw(g2, top);
    }

    /**
     * Computes again where all lifelines end, as drawing it does: called before the graph is drawn, as a lifeline out of the
     * painted part isn't drawn, and its bounds must still follow the others
     */
    void updateMaxYOverAllLifeLineNodes()
    {
        getMaxYOverAllLifeLineNodes();
    }

    private double getMaxYOverAllLifeLineNodes()
    {
        double maxY = getLocalHeight();
        for (INode node : getGraph().getAllNodes())
        {
            if (node instanceof LifelineNode)
            {
                maxY = Math.max(maxY, ((LifelineNode) node).getLocalHeight() + node.getLocationOnGraph().getY());
            }
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
