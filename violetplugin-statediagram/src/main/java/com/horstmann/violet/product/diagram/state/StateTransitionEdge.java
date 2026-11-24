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

package com.horstmann.violet.product.diagram.state;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.regex.Pattern;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.IMovableLabelElement;
import com.horstmann.violet.product.diagram.abstracts.edge.ShapeEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A curved edge for a state transition in a state diagram.
 */
public class StateTransitionEdge extends ShapeEdge implements IColorable, IMovableLabelElement
{
    /**
     * Sets the label property value.
     * 
     * @param newValue the new value
     */
    public void setLabel(String newValue)
    {
        labelText = newValue;
    }

    /**
     * Gets the label property value.
     * 
     * @return the current value
     */
    public String getLabel()
    {
        return labelText;
    }

    public void draw(Graphics2D g2)
    {
        Color oldColor = g2.getColor();
        g2.setColor(getBorderColor());
        g2.draw(getShape());
        if (!LABEL.equals(this.hiddenText))
        {
            drawLabel(g2);
        }
        ArrowHead.V.draw(g2, getControlPoint(), getConnectionPoints().getP2());
        g2.setColor(oldColor);
    }

    @Override
    public void setHiddenText(String textPropertyName)
    {
        this.hiddenText = textPropertyName;
    }

    /** The label is not drawn while it is edited in place ("label"), else null; never saved */
    private transient String hiddenText;

    /**
     * Draws the label.
     * 
     * @param g2 the graphics context
     */
    private void drawLabel(Graphics2D g2)
    {
        Color oldColor = g2.getColor();
        g2.setColor(getTextColor());
        getLabelContent().draw(g2, getTextBounds());
        g2.setColor(oldColor);
    }

    /**
     * The label is a plain String, saved as it is (file format unchanged), drawn as a MultiLineString like the other texts of the
     * diagrams: same font and sizes on the desktop and in the browser, nothing taken from the look and feel. It was always read as
     * HTML, so the one of an older file which holds known tags or entities still is; any other one is shown as typed.
     * 
     * @return the drawing of the label, kept while the label doesn't change
     */
    private MultiLineString getLabelContent()
    {
        String text = labelText == null ? "" : labelText;
        if (labelContent == null || !text.equals(textOfLabelContent))
        {
            labelContent = new MultiLineString();
            labelContent.setJustification(MultiLineString.Justification.LEFT);
            boolean isHtml = HTML.matcher(text).find();
            labelContent.setText(isHtml ? "<html>" + text.replaceAll("\\R", "<br>") + "</html>" : text);
            textOfLabelContent = text;
        }
        return labelContent;
    }

    /**
     * @return the bounds of the label text, moved by the user if it was
     */
    private Rectangle2D getTextBounds()
    {
        Rectangle2D bounds = getDefaultTextBounds();
        if (labelOffset == null)
        {
            return bounds;
        }
        return new Rectangle2D.Double(bounds.getX() + labelOffset.getX(), bounds.getY() + labelOffset.getY(), bounds.getWidth(),
                bounds.getHeight());
    }

    /**
     * @return the bounds of the label text at its automatic place, beside the middle of the curve
     */
    private Rectangle2D getDefaultTextBounds()
    {
        Rectangle2D d = getLabelContent().getBounds();

        Line2D line = getConnectionPoints();
        Point2D middle = getLabelAnchor(LABEL);
        double x = middle.getX();
        double y = middle.getY();

        final int GAP = 3;
        if (line.getY1() == line.getY2()) x -= d.getWidth() / 2;
        else if (line.getY1() <= line.getY2()) x += GAP;
        else x -= d.getWidth() + GAP;

        if (line.getX1() == line.getX2()) y += d.getHeight() / 2;
        else if (line.getX1() <= line.getX2()) y -= d.getHeight() + GAP;
        else y += GAP;
        if (Math.abs(line.getX1() - line.getX2()) >= Math.abs(line.getY1() - line.getY2()))
        {
            x = x - d.getWidth() / 2;
        }
        if (Math.abs(line.getX1() - line.getX2()) <= Math.abs(line.getY1() - line.getY2()))
        {
            y = y - d.getHeight() / 2;
        }
        return new Rectangle2D.Double(x, y, d.getWidth(), d.getHeight());
    }

    /**
     * Gets the control point for the quadratic spline.
     * 
     * @return the control point
     */
    private Point2D getControlPoint()
    {
        Line2D line = getConnectionPoints();
        double t = Math.tan(Math.toRadians(angle));
        double dx = (line.getX2() - line.getX1()) / 2;
        double dy = (line.getY2() - line.getY1()) / 2;
        return new Point2D.Double((line.getX1() + line.getX2()) / 2 + t * dy, (line.getY1() + line.getY2()) / 2 - t * dx);
    }

    public Shape getShape()
    {
        Line2D line = getConnectionPoints();
        Point2D control = getControlPoint();
        GeneralPath p = new GeneralPath();
        p.moveTo((float) line.getX1(), (float) line.getY1());
        p.quadTo((float) control.getX(), (float) control.getY(), (float) line.getX2(), (float) line.getY2());
        return p;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D r = super.getBounds();
        r.add(getTextBounds());
        return r;
    }

    @Override
    public Rectangle2D getMiddleLabelBounds()
    {
        return getLabelBounds(LABEL);
    }

    @Override
    public Rectangle2D getLabelBounds(String labelName)
    {
        return hasText() ? getTextBounds() : null;
    }

    @Override
    public Rectangle2D getDefaultLabelBounds(String labelName)
    {
        return hasText() ? getDefaultTextBounds() : null;
    }

    private boolean hasText()
    {
        return labelText != null && !labelText.isEmpty();
    }

    /**
     * @return the middle of the curve
     */
    @Override
    public Point2D getLabelAnchor(String labelName)
    {
        Line2D line = getConnectionPoints();
        Point2D control = getControlPoint();
        return new Point2D.Double(control.getX() / 2 + line.getX1() / 4 + line.getX2() / 4,
                control.getY() / 2 + line.getY1() / 4 + line.getY2() / 4);
    }

    @Override
    public String findLabel(Point2D aPoint)
    {
        Rectangle2D bounds = getLabelBounds(LABEL);
        return bounds != null && bounds.contains(aPoint) && !isInsideLinkedNode(aPoint) ? LABEL : null;
    }

    @Override
    public Point2D.Double getLabelOffset(String labelName)
    {
        return getLabelOffset();
    }

    @Override
    public void setLabelOffset(String labelName, Point2D.Double offset)
    {
        setLabelOffset(offset);
    }

    public Point2D.Double getLabelOffset()
    {
        return labelOffset;
    }

    public void setLabelOffset(Point2D.Double offset)
    {
        this.labelOffset = offset;
    }

    /**
     * Its text belongs to it: a click on it selects the transition, a double click edits it in place, and it is never an empty
     * spot of the diagram. Not inside the states it links.
     */
    @Override
    public boolean contains(Point2D aPoint)
    {
        return super.contains(aPoint) || findLabel(aPoint) != null;
    }

    @Override
    public Direction getDirection(INode node)
    {
        if (getStartNode() == getEndNode())
        {
            angle = 60;
            if (node.equals(getStartNode())) return Direction.EAST.turn(-30);
            if (node.equals(getEndNode())) return Direction.EAST.turn(30);
        }
        angle = 10;
        return super.getDirection(node);
    }

    @Override
    public void setBackgroundColor(Color bgColor)
    {
        this.backgroundColor = bgColor;
    }

    @Override
    public Color getBackgroundColor()
    {
        if (backgroundColor == null)
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
    public Color getBorderColor()
    {
        if (borderColor == null)
        {
            return Color.BLACK;
        }
        return borderColor;
    }

    @Override
    public void setTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    @Override
    public Color getTextColor()
    {
        if (textColor == null)
        {
            return ColorChoice.DEFAULT_TEXT_COLOR;
        }
        return textColor;
    }

    private double angle;
    private String labelText = "";
    /** Offset of the label dragged by the user from its automatic place; null (not saved) when left there */
    private Point2D.Double labelOffset;
    /** Name of the only text of this edge, as its String property */
    private static final String LABEL = "label";
    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;

    /** The drawing of the label, see {@link #getLabelContent()}; never saved */
    private transient MultiLineString labelContent;
    /** The label it was made from */
    private transient String textOfLabelContent;

    /** A tag or an entity that the label of an older file may hold */
    private static final Pattern HTML = Pattern
            .compile("(?i)</?(html|body|b|i|u|strong|em|font|span|p|div|br)(\\s[^<>]*)?/?>|&(#\\d+|#x[0-9a-f]+|lt|gt|amp|quot|nbsp);");
}
