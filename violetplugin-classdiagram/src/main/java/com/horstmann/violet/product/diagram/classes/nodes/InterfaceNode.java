package com.horstmann.violet.product.diagram.classes.nodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.common.PointNode;

/**
 * An interface node in a class diagram.
 */
public class InterfaceNode extends RectangularNode
{
    /**
     * Construct an interface node with a default size and the text <<interface>>.
     */
    public InterfaceNode()
    {
        name = new MultiLineString();
        name.setSize(MultiLineString.FontSize.LARGE);
        name.setText("«interface»");
        methods = new MultiLineString();
        methods.setJustification(MultiLineString.Justification.LEFT);
    }

    @Override
    public Rectangle2D getBounds()
    {
        boolean hasMethods = methods.getText().length() > 0;
        double maxW = Math.max(DEFAULT_WIDTH,
                     Math.max(name.getBounds().getWidth(), methods.getBounds().getWidth()));
        double nameH = Math.max(name.getBounds().getHeight(),
                       hasMethods ? DEFAULT_COMPARTMENT_HEIGHT : DEFAULT_HEIGHT);
        double methH = hasMethods ? Math.max(methods.getBounds().getHeight(), DEFAULT_COMPARTMENT_HEIGHT) : 0;
        Point2D loc = getLocation();
        Rectangle2D result = new Rectangle2D.Double(loc.getX(), loc.getY(), maxW, nameH + methH);
        return getGraph().getGridSticker().snap(result);
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
        boolean hasMethods = methods.getText().length() > 0;
        Rectangle2D currentBounds = getBounds();
        double x = currentBounds.getX();
        double y = currentBounds.getY();
        double w = currentBounds.getWidth();
        double nameH = Math.max(name.getBounds().getHeight(),
                       hasMethods ? DEFAULT_COMPARTMENT_HEIGHT : DEFAULT_HEIGHT);
        double methH = hasMethods ? Math.max(methods.getBounds().getHeight(), DEFAULT_COMPARTMENT_HEIGHT) : 0;
        Rectangle2D topBounds    = new Rectangle2D.Double(x, y,         w, nameH);
        Rectangle2D bottomBounds = new Rectangle2D.Double(x, y + nameH, w, methH);
        g2.setColor(getBackgroundColor());
        g2.fill(currentBounds);
        g2.setColor(getBorderColor());
        g2.draw(currentBounds);
        if (hasMethods)
        {
            g2.drawLine((int) x, (int) topBounds.getMaxY(), (int) currentBounds.getMaxX(), (int) topBounds.getMaxY());
        }
        g2.setColor(getTextColor());
        name.draw(g2, topBounds);
        methods.draw(g2, bottomBounds);
        // Restore g2 original location
        g2.translate(-g2Location.getX(), -g2Location.getY());
        // Restore first color
        g2.setColor(oldColor);
    }

    @Override
    public boolean addChild(INode n, Point2D p)
    {
        return canAddChild(n);
    }

    @Override
    public boolean canAddChild(INode n)
    {
        return n instanceof PointNode;
    }

    /**
     * Sets the name property value.
     * 
     * @param newValue the interface name
     */
    public void setName(MultiLineString newValue)
    {
        name = newValue;
    }

    /**
     * Gets the name property value.
     * 
     * @return the interface name
     */
    public MultiLineString getName()
    {
        return name;
    }

    /**
     * Sets the methods property value.
     * 
     * @param newValue the methods of this interface
     */
    public void setMethods(MultiLineString newValue)
    {
        methods = newValue;
    }

    /**
     * Gets the methods property value.
     * 
     * @return the methods of this interface
     */
    public MultiLineString getMethods()
    {
        return methods;
    }

    @Override
    public InterfaceNode clone()
    {
        InterfaceNode cloned = (InterfaceNode) super.clone();
        cloned.name = name.clone();
        cloned.methods = methods.clone();
        return cloned;
    }

    // private transient double midHeight;
    // private transient double botHeight;
    private MultiLineString name;
    private MultiLineString methods;

    private static int DEFAULT_COMPARTMENT_HEIGHT = 20;
    private static int DEFAULT_WIDTH = 100;
    private static int DEFAULT_HEIGHT = 60;
}
