package com.horstmann.violet.product.diagram.classes.nodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A package node in a UML diagram.
 */
public class PackageNode extends RectangularNode
{
    /**
     * Construct a package node with a default size
     */
    public PackageNode()
    {
        name = new MultiLineString();
        name.setSize(MultiLineString.FontSize.LARGE);
        content = new MultiLineString();
    }


    



    private Rectangle2D getTopRectangleBounds()
    {
        Rectangle2D globalBounds = new Rectangle2D.Double(0, 0, 0, 0);
        Rectangle2D nameBounds = name.getBounds();
        globalBounds.add(nameBounds);
        globalBounds.add(new Rectangle2D.Double(0, 0, DEFAULT_TOP_WIDTH, DEFAULT_TOP_HEIGHT));
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        double w = globalBounds.getWidth();
        double h = globalBounds.getHeight();
        globalBounds.setFrame(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(globalBounds);
        return snappedBounds;
    }

    private Rectangle2D getBottomRectangleBounds()
    {
        Rectangle2D globalBounds = new Rectangle2D.Double(0, 0, 0, 0);
        Rectangle2D contentsBounds = content.getBounds();
        globalBounds.add(contentsBounds);
        globalBounds.add(new Rectangle2D.Double(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        Rectangle2D childrenBounds = new Rectangle2D.Double(0, 0, 0, 0);
        for (INode child : getChildren())
        {
            Rectangle2D childBounds = child.getBounds();
            childrenBounds.add(childBounds);
        }
        childrenBounds.setFrame(childrenBounds.getX(), childrenBounds.getY(), childrenBounds.getWidth() + CHILD_GAP,
                childrenBounds.getHeight() + CHILD_GAP);
        globalBounds.add(childrenBounds);
        Rectangle2D topBounds = getTopRectangleBounds();
        double x = topBounds.getX();
        double y = topBounds.getMaxY();
        double w = Math.max(globalBounds.getWidth(), topBounds.getWidth() + 2 * NAME_GAP);
        double h = globalBounds.getHeight();
        globalBounds.setFrame(x, y, w, h);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(globalBounds);
        return snappedBounds;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D top = getTopRectangleBounds();
        Rectangle2D bot = getBottomRectangleBounds();
        top.add(bot);
        Rectangle2D snappedBounds = getGraph().getGridSticker().snap(top);
        return snappedBounds;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        // Backup current color;
        Color oldColor = g2.getColor();
        // Translate g2 if node has parent
        Point2D nodeLocationOnGraph = getLocationOnGraph();
        Point2D nodeLocation = getLocation();
        Point2D g2Location = new Point2D.Double(nodeLocationOnGraph.getX() - nodeLocation.getX(), nodeLocationOnGraph.getY()
                - nodeLocation.getY());
        g2.translate(g2Location.getX(), g2Location.getY());
        // Perform drawing
        super.draw(g2);
        Rectangle2D topBounds = getTopRectangleBounds();
        Rectangle2D bottomBounds = getBottomRectangleBounds();
        g2.setColor(getBackgroundColor());
        g2.fill(topBounds);
        g2.fill(bottomBounds);
        g2.setColor(getBorderColor());
        g2.draw(topBounds);
        g2.draw(bottomBounds);
        g2.setColor(getTextColor());
        name.draw(g2, topBounds);
        content.draw(g2, bottomBounds);
        // Restore g2 original location
        g2.translate(-g2Location.getX(), -g2Location.getY());
        // Restore first color
        g2.setColor(oldColor);
        // Draw its children
        for (INode node : getChildren())
        {
            fixChildLocation(topBounds, node);
            node.draw(g2);
        }
    }

    /**
     * Ensure that child node respects the minimum gap with package borders
     * 
     * @param topBounds
     * @param node
     */
    private void fixChildLocation(Rectangle2D topBounds, INode node)
    {
        Point2D childLocation = node.getLocation();
        if (childLocation.getY() <= topBounds.getHeight() + CHILD_GAP)
        {
            node.translate(0, topBounds.getHeight() + CHILD_GAP - childLocation.getY());
        }
        if (childLocation.getX() < CHILD_GAP)
        {
            node.translate(CHILD_GAP - childLocation.getX(), 0);
        }
    }

    @Override
    public Shape getShape()
    {
        GeneralPath path = new GeneralPath();
        path.append(getTopRectangleBounds(), false);
        path.append(getBottomRectangleBounds(), false);
        return path;
    }

    @Override
    public boolean addChild(INode n, Point2D p)
    {
        if (canAddChild(n))
        {
            n.setParent(this);
            n.setGraph(this.getGraph());
            n.setLocation(p);
            addChild(n, getChildren().size());
            return true;
        }
        return false;
    }

    /**
     * Classes, interfaces and packages
     */
    @Override
    public boolean canAddChild(INode n)
    {
        return n instanceof ClassNode || n instanceof InterfaceNode || n instanceof PackageNode;
    }

    public PackageNode clone()
    {
        PackageNode cloned = (PackageNode) super.clone();
        cloned.name = name.clone();
        cloned.content = content.clone();
        return cloned;
    }

    /**
     * Sets the name property value.
     * 
     * @param newValue the class name
     */
    public void setName(MultiLineString newValue)
    {
        name = newValue;
    }

    /**
     * Gets the name property value.
     * 
     * @return the class name
     */
    public MultiLineString getName()
    {
        return name;
    }

    /**
     * Sets the contents property value.
     * 
     * @param newValue the contents of this class
     */
    public void setContent(MultiLineString newValue)
    {
        content = newValue;
    }

    /**
     * Gets the contents property value.
     * 
     * @return the contents of this class
     */
    public MultiLineString getContent()
    {
        return content;
    }

    private MultiLineString name;
    private MultiLineString content;


    private static int DEFAULT_TOP_WIDTH = 60;
    private static int DEFAULT_TOP_HEIGHT = 20;
    private static int DEFAULT_WIDTH = 100;
    private static int DEFAULT_HEIGHT = 80;
    private static final int NAME_GAP = 3;
    private static final int CHILD_GAP = 20;

}
