package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A BPMN text annotation: a comment, opened by a square bracket on its left, linked to what it explains by an association.
 */
public class BpmnTextAnnotationNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        if (this.isToolPrototype)
        {
            return new Rectangle2D.Double(location.getX(), location.getY(), TOOL_ICON_SIZE, TOOL_ICON_SIZE);
        }
        Rectangle2D textBounds = getText().getBounds();
        double width = Math.max(textBounds.getWidth() + 2 * PADDING, DEFAULT_WIDTH);
        double height = Math.max(textBounds.getHeight() + 2 * PADDING, DEFAULT_HEIGHT);
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), width, height);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        Color oldColor = g2.getColor();
        Rectangle2D b = getBounds();
        // Transparent unless colored, as in most BPMN tools
        if (!ColorChoice.DEFAULT.getBackgroundColor().equals(getBackgroundColor()))
        {
            g2.setColor(getBackgroundColor());
            g2.fill(b);
        }
        Path2D.Double bracket = new Path2D.Double();
        bracket.moveTo(b.getX() + BRACKET_WIDTH, b.getY());
        bracket.lineTo(b.getX(), b.getY());
        bracket.lineTo(b.getX(), b.getMaxY());
        bracket.lineTo(b.getX() + BRACKET_WIDTH, b.getMaxY());
        g2.setColor(getBorderColor());
        g2.draw(bracket);
        g2.setColor(getTextColor());
        if (this.isToolPrototype)
        {
            // Lines standing for the text
            for (int i = 1; i <= 3; i++)
            {
                double y = b.getY() + b.getHeight() * i / 4;
                double end = b.getMaxX() - (i == 3 ? b.getWidth() * 0.3 : b.getWidth() * 0.1);
                g2.draw(new Line2D.Double(b.getX() + BRACKET_WIDTH, y, end, y));
            }
            g2.setColor(oldColor);
            return;
        }
        getText().draw(g2, new Rectangle2D.Double(b.getX() + PADDING, b.getY(), b.getWidth() - 2 * PADDING, b.getHeight()));
        g2.setColor(oldColor);
    }

    /**
     * Sets the text property value.
     *
     * @param newValue the text of the annotation
     */
    public void setText(MultiLineString newValue)
    {
        this.text = newValue;
    }

    /**
     * Gets the text property value.
     *
     * @return the text of the annotation, aligned to the left
     */
    public MultiLineString getText()
    {
        if (this.text == null)
        {
            this.text = new MultiLineString();
            this.text.setJustification(MultiLineString.Justification.LEFT);
        }
        return this.text;
    }

    @Override
    public BpmnTextAnnotationNode clone()
    {
        BpmnTextAnnotationNode cloned = (BpmnTextAnnotationNode) super.clone();
        cloned.text = getText().clone();
        cloned.isToolPrototype = false;
        return cloned;
    }

    /**
     * Turns this node into a tool bar prototype: a square bracket with lines standing for the text
     */
    public void enableToolIcon()
    {
        this.isToolPrototype = true;
    }

    private MultiLineString text;

    private transient boolean isToolPrototype;

    private static final int TOOL_ICON_SIZE = 40;

    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 40;
    private static final int PADDING = 8;
    private static final int BRACKET_WIDTH = 12;

}
