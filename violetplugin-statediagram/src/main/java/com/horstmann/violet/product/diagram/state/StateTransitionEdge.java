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
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.edge.ShapeEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A curved edge for a state transition in a state diagram.
 */
public class StateTransitionEdge extends ShapeEdge implements IColorable
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
        drawLabel(g2);
        ArrowHead.V.draw(g2, getControlPoint(), getConnectionPoints().getP2());
        g2.setColor(oldColor);
    }

    /**
     * Draws the label.
     * 
     * @param g2 the graphics context
     */
    private void drawLabel(Graphics2D g2)
    {
        label.setForeground(getTextColor());
        Rectangle2D labelBounds = getLabelBounds();
        double x = labelBounds.getX();
        double y = labelBounds.getY();

        g2.translate(x, y);
        label.paint(g2);
        g2.translate(-x, -y);
    }

    /**
     * Gets the bounds of the label text
     * 
     * @param g2 the graphics context
     * @return the bounds of the label text
     */
    private Rectangle2D getLabelBounds()
    {
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        // need a dummy image to get a Graphics to
        // measure the size
        Graphics2D g2 = (Graphics2D) dummy.getGraphics();
        
        label.setText("<html>" + labelText + "</html>");
        label.setFont(g2.getFont());
        Dimension d = label.getPreferredSize();
        label.setBounds(0, 0, d.width, d.height);

        Line2D line = getConnectionPoints();
        Point2D control = getControlPoint();
        double x = control.getX() / 2 + line.getX1() / 4 + line.getX2() / 4;
        double y = control.getY() / 2 + line.getY1() / 4 + line.getY2() / 4;

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
        return new Rectangle2D.Double(x, y, d.width, d.height);
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
        r.add(getLabelBounds());
        return r;
    }

    @Override
    public Rectangle2D getMiddleLabelBounds()
    {
        if (labelText == null || labelText.isEmpty())
        {
            return null;
        }
        return getLabelBounds();
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
    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;

    private static JLabel label = new JLabel();
}
