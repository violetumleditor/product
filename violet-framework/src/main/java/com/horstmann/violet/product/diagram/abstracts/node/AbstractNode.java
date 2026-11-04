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

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.Transient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A class that supplies convenience implementations for a number of methods in the Node interface
 * 
 * @author Cay Horstmann
 */
public abstract class AbstractNode implements INode, IColorable
{
    /**
     * Constructs a node with no parents or children at location (0, 0).
     */
    public AbstractNode()
    {
        // Nothing to do
    }

    /**
     * @return currently connected edges
     */
    protected List<IEdge> getConnectedEdges()
    {
        List<IEdge> connectedEdges = new ArrayList<IEdge>();
        IGraph currentGraph = getGraph();
        for (IEdge anEdge : currentGraph.getAllEdges())
        {
            INode start = anEdge.getStartNode();
            INode end = anEdge.getEndNode();
            if (this.equals(start) || this.equals(end))
            {
                connectedEdges.add(anEdge);
            }
        }
        return connectedEdges;
    }

    @Override
    public Point2D getLocation()
    {
        if (this.location == null) {
        	this.location = new Point2D.Double(0, 0);
        }
    	return this.location;
    }

    @Override
    public Point2D getLocationOnGraph()
    {
        INode parentNode = getParent();
        if (parentNode == null)
        {
            return getLocation();
        }
        Point2D parentLocationOnGraph = parentNode.getLocationOnGraph();
        Point2D relativeLocation = getLocation();
        Point2D result = new Point2D.Double(parentLocationOnGraph.getX() + relativeLocation.getX(), parentLocationOnGraph.getY()
                + relativeLocation.getY());
        return result;
    }

    @Override
    public void setLocation(Point2D aPoint)
    {
        this.location = aPoint;
    }

    @Override
    public Id getId()
    {
        if (this.id == null) {
        	this.id = new Id();
        }
    	return this.id;
    }

    @Override
    public void setId(Id id)
    {
        this.id = id;
    }

    @Override
    public Integer getRevision()
    {
        if (this.revision == null) {
        	this.revision = 0;
        }
    	return this.revision;
    }

    @Override
    public void setRevision(Integer newRevisionNumber)
    {
        this.revision = newRevisionNumber;
    }

    @Override
    public void incrementRevision()
    {
        int i = getRevision().intValue();
        i++;
        this.revision = i;
    }

    @Override
    public void translate(double dx, double dy)
    {
        Point2D newLocation = new Point2D.Double(getLocation().getX() + dx, getLocation().getY() + dy);
        setLocation(newLocation);
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        return e.getEndNode() != null;
    }

    @Override
    public void removeConnection(IEdge e)
    {
    }

    @Override
    public void removeChild(INode node)
    {
        if (node.getParent() != this) return;
        getChildren().remove(node);
    }

    @Override
    public boolean addChild(INode n, Point2D p)
    {
        return false;
    }

    @Override
    public INode getParent()
    {
        return parent;
    }

    @Override
    public void setParent(INode node)
    {
        parent = node;
    }

    @Override
    public List<INode> getChildren()
    {
        if (this.children == null) {
        	this.children = new ArrayList<INode>();
        }
    	return children;
    }

    @Override
    public boolean addChild(INode node, int index)
    {
        INode oldParent = node.getParent();
        if (oldParent != null) oldParent.removeChild(node);
        getChildren().add(index, node);
        node.setParent(this);
        node.setGraph(getGraph());
        return true;
    }

    /**
     * @return the shape to be used for computing the drop shadow
     */
    public Shape getShape()
    {
        return new Rectangle2D.Double(0, 0, 0, 0);
    }

    @Override
    public AbstractNode clone()
    {
        try
        {
            AbstractNode cloned = (AbstractNode) super.clone();
            cloned.id = getId().clone();
            cloned.children = new ArrayList<INode>();
            cloned.location = (Point2D.Double) getLocation().clone();

            for (INode child : getChildren())
            {
                INode clonedChild = child.clone();
                cloned.children.add(clonedChild);
                clonedChild.setParent(cloned);
            }
            return cloned;
        }
        catch (CloneNotSupportedException exception)
        {
            return null;
        }
    }

    @Override
    public void setGraph(IGraph g)
    {
        this.graph = g;
        for (INode aChild : getChildren()) {
        	aChild.setGraph(g);
        }
    }

    @Override
    @Transient
    public IGraph getGraph()
    {
        if (this.graph == null) {
        	this.graph = new AbstractGraph()
            {
                @Override
                public List<INode> getNodePrototypes()
                {
                    return new ArrayList<INode>();
                }

                @Override
                public List<IEdge> getEdgePrototypes()
                {
                    return new ArrayList<IEdge>();
                }
            };
        }
    	return this.graph;
    }

    @Override
    public int getZ()
    {
        return z;
    }

    @Override
    public void setZ(int z)
    {
        this.z = z;
    }

    /**
     * Sets node tool tip
     * 
     * @param label
     */
    public void setToolTip(String s)
    {
        this.toolTip = s;
    }

    @Override
    public String getToolTip()
    {
        if (this.toolTip == null) {
        	this.toolTip = "";
        }
    	return this.toolTip;
    }
    

    @Override
    public void setBackgroundColor(Color bgColor)
    {
        backgroundColor = bgColor;
    }

    @Override
    public final Color getBackgroundColor()
    {
        if(null == backgroundColor)
        {
            return ColorChoice.DEFAULT.getBackgroundColor();
        }
        return backgroundColor;
    }

    @Override
    public void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
    }

    @Override
    public final Color getBorderColor()
    {
        if(null == borderColor)
        {
            return ColorChoice.DEFAULT.getBorderColor();
        }
        return borderColor;
    }

    @Override
    public void setTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    @Override
    public final Color getTextColor()
    {
        if(null == textColor)
        {
            return ColorChoice.DEFAULT_TEXT_COLOR;
        }
        return textColor;
    }
    
    @Override
    public List<Point2D> getSelectionPoints() {
    	Rectangle2D bounds = getBounds();
    	if (bounds == null) {
    		return new ArrayList<>();
    	}
    	Point2D locationOnGraph = getLocationOnGraph();
    	double locationOnGraphX = 0;
    	double locationOnGraphY = 0;
    	if (locationOnGraph != null) {
    		locationOnGraphX = locationOnGraph.getX();
    		locationOnGraphY = locationOnGraph.getY();
    	}
    	Point2D p1 = new Point2D.Double(locationOnGraphX, locationOnGraphY);
    	Point2D p2 = new Point2D.Double(locationOnGraphX + bounds.getWidth(), locationOnGraphY);
    	Point2D p3 = new Point2D.Double(locationOnGraphX, locationOnGraphY + bounds.getHeight());
    	Point2D p4 = new Point2D.Double(locationOnGraphX + bounds.getWidth(), locationOnGraphY + bounds.getHeight());
    	return Arrays.asList(p1, p2, p3, p4);
    }
    
    
    @Override
    public ISelectable getSelectableParent() {
    	return getParent();
    }
    
    @Override
    public List<ISelectable> getSelectableChildren() {
    	List<ISelectable> children = new ArrayList<>();
    	children.addAll(getChildren());
    	return children;
    }
    
    public void setPreferredSize(Rectangle2D size) {
    	this.preferredSize = size;
    }
    
    public Rectangle2D getPreferredSize() {
    	return this.preferredSize;
    }

    private ArrayList<INode> children;
    private INode parent;
    private transient IGraph graph;
    private Point2D location;
    private transient String toolTip;
    private transient int z;

    /** Node's current id (unique in all the graph) */
    private Id id;

    /** Node's current revision */
    private Integer revision;
    

    private Color backgroundColor = ColorChoice.DEFAULT.getBackgroundColor();
    private Color borderColor =  ColorChoice.DEFAULT.getBorderColor();
    private Color textColor =  ColorChoice.DEFAULT_TEXT_COLOR;
    
    private Rectangle2D preferredSize = new Rectangle2D.Double();

    private int borderWidth = 1;

    public void setBorderWidth(int borderWidth)
    {
        this.borderWidth = Math.max(1, borderWidth);
    }

    public int getBorderWidth()
    {
        return this.borderWidth;
    }
}
