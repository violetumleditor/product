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

package com.horstmann.violet.product.diagram.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.product.diagram.abstracts.node.IRotatableNode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A text that can be placed anywhere on a diagram. It has neither border nor background : only its text can be changed.
 * <p>
 * It is always attached to the graph, never accepts children nor edges other than generic ones (see {@link GenericEdge}), and is always drawn and found upon all other elements.
 * </p>
 * <p>
 * It can be turned around its center ({@link IRotatableNode}): its bounds are then those of the turned text, which lies centered in
 * them, and only the turned text catches the mouse.
 * </p>
 */
public class FreeTextNode extends RectangularNode implements IRotatableNode
{
    /**
     * Constructs a node with a default text, otherwise nothing would be visible once added to a diagram
     */
    public FreeTextNode()
    {
        ResourceBundle rs = ResourceBundle.getBundle(ResourceBundleConstant.NODE_AND_EDGE_STRINGS, Locale.getDefault());
        text = new MultiLineString();
        text.setJustification(MultiLineString.Justification.LEFT);
        text.setText(rs.getString("freetextnode.text"));
    }

    /**
     * Makes this node drawn as an icon instead of its text. Only intended for the prototype displayed in the tool bar : clones
     * and nodes loaded from files always draw their text.
     */
    public void enableToolIcon()
    {
        this.toolIcon = true;
    }

    @Override
    public int getZ()
    {
        return ABOVE_NOTES_Z_LEVEL;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D currentLocation = getLocation();
        double x = currentLocation.getX();
        double y = currentLocation.getY();
        if (this.toolIcon)
        {
            return new Rectangle2D.Double(x, y, TOOL_ICON_SIZE, TOOL_ICON_SIZE);
        }
        if (getRotationDegrees() == 0)
        {
            Rectangle2D b = text.getBounds();
            double w = Math.max(b.getWidth(), MINIMUM_WIDTH);
            double h = Math.max(b.getHeight(), MINIMUM_HEIGHT);
            return getGraph().getGridSticker().snap(new Rectangle2D.Double(x, y, w, h));
        }
        // Not on the grid: the turned text isn't, and its center must stay where it is while it turns
        Rectangle2D turned = getTurnedTextBox(getUnrotatedSize()).getBounds2D();
        return new Rectangle2D.Double(x, y, turned.getWidth(), turned.getHeight());
    }

    /**
     * @return the width of the text box, the one it has when not turned (on the grid)
     */
    @Override
    public double getUnrotatedWidth()
    {
        return getUnrotatedSize().getWidth();
    }

    /**
     * @return the height of the text box, the one it has when not turned (on the grid)
     */
    @Override
    public double getUnrotatedHeight()
    {
        return getUnrotatedSize().getHeight();
    }

    /**
     * @return the text box when not turned, at (0, 0), of the size {@link #getBounds()} gives it at 0 degree
     */
    private Rectangle2D getUnrotatedSize()
    {
        Rectangle2D b = text.getBounds();
        double w = Math.max(b.getWidth(), MINIMUM_WIDTH);
        double h = Math.max(b.getHeight(), MINIMUM_HEIGHT);
        return getGraph().getGridSticker().snap(new Rectangle2D.Double(0, 0, w, h));
    }

    /**
     * @return where the text is drawn before it is turned: centered in the bounds, in their coordinates
     */
    private Rectangle2D getTextBox()
    {
        Rectangle2D bounds = getBounds();
        if (this.toolIcon || getRotationDegrees() == 0)
        {
            return bounds;
        }
        Rectangle2D size = getUnrotatedSize();
        double w = size.getWidth();
        double h = size.getHeight();
        return new Rectangle2D.Double(bounds.getCenterX() - w / 2, bounds.getCenterY() - h / 2, w, h);
    }

    /**
     * @return the given box turned around its center by the angle of this node
     */
    private Shape getTurnedTextBox(Rectangle2D box)
    {
        return AffineTransform.getRotateInstance(Math.toRadians(getRotationDegrees()), box.getCenterX(), box.getCenterY())
                .createTransformedShape(box);
    }

    @Override
    public Shape getShape()
    {
        if (this.toolIcon || getRotationDegrees() == 0)
        {
            return super.getShape();
        }
        return getTurnedTextBox(getTextBox());
    }

    @Override
    public boolean contains(Point2D p)
    {
        return getShape().contains(p);
    }

    /**
     * @return the corners of the turned text, on the graph
     */
    @Override
    public List<Point2D> getSelectionPoints()
    {
        if (this.toolIcon || getRotationDegrees() == 0)
        {
            return super.getSelectionPoints();
        }
        Point2D location = getLocation();
        Point2D locationOnGraph = getLocationOnGraph();
        Rectangle2D box = getTextBox();
        double[] corners = { box.getMinX(), box.getMinY(), box.getMaxX(), box.getMinY(), box.getMinX(), box.getMaxY(),
                box.getMaxX(), box.getMaxY() };
        AffineTransform toGraph = AffineTransform.getTranslateInstance(locationOnGraph.getX() - location.getX(),
                locationOnGraph.getY() - location.getY());
        toGraph.rotate(Math.toRadians(getRotationDegrees()), box.getCenterX(), box.getCenterY());
        toGraph.transform(corners, 0, corners, 0, 4);
        List<Point2D> points = new ArrayList<Point2D>();
        for (int i = 0; i < corners.length; i += 2)
        {
            points.add(new Point2D.Double(corners[i], corners[i + 1]));
        }
        return points;
    }

    @Override
    public Double getRotation()
    {
        return this.rotation;
    }

    /**
     * Turns the text around its center, which stays where it is (its location, the top left corner of its bounds, moves)
     *
     * @param degrees the angle, clockwise on screen; brought back from 0 to 360, to the hundredth; null or 0 for none
     */
    @Override
    public void setRotation(Double degrees)
    {
        Double newValue = normalizeRotation(degrees);
        if (Objects.equals(newValue, this.rotation))
        {
            return;
        }
        Rectangle2D before = getBounds();
        this.rotation = newValue;
        Rectangle2D after = getBounds();
        setLocation(new Point2D.Double(before.getCenterX() - after.getWidth() / 2, before.getCenterY() - after.getHeight() / 2));
    }

    /**
     * @return the angle from 0 included to 360 excluded, rounded to the hundredth; null for 0 (not written in files)
     */
    private Double normalizeRotation(Double degrees)
    {
        if (degrees == null || degrees.isNaN() || degrees.isInfinite())
        {
            return null;
        }
        double normalized = Math.round((((degrees % 360) + 360) % 360) * 100) / 100.0;
        if (normalized == 0 || normalized == 360)
        {
            return null;
        }
        return normalized;
    }

    /**
     * Gets the value of the text property.
     *
     * @return the displayed text
     */
    public MultiLineString getText()
    {
        return text;
    }

    /**
     * Sets the value of the text property.
     *
     * @param newValue the displayed text
     */
    public void setText(MultiLineString newValue)
    {
        text = newValue;
    }

    @Override
    public void draw(Graphics2D g2)
    {
        Color oldColor = g2.getColor();
        g2.setColor(getTextColor());
        if (this.toolIcon)
        {
            drawToolIcon(g2);
        }
        else if (getRotationDegrees() == 0)
        {
            text.draw(g2, getBounds());
        }
        else
        {
            // Drawn turned: the text remembers this transform, so it is edited in place where it is drawn
            Rectangle2D box = getTextBox();
            AffineTransform oldTransform = g2.getTransform();
            g2.rotate(Math.toRadians(getRotationDegrees()), box.getCenterX(), box.getCenterY());
            text.draw(g2, box);
            g2.setTransform(oldTransform);
        }
        g2.setColor(oldColor);
    }

    /**
     * Draws a "T" centered on the node bounds
     */
    private void drawToolIcon(Graphics2D g2)
    {
        Font font = new Font(Font.SERIF, Font.BOLD, TOOL_ICON_SIZE);
        Shape glyph = font.createGlyphVector(g2.getFontRenderContext(), "T").getOutline();
        Rectangle2D glyphBounds = glyph.getBounds2D();
        Rectangle2D bounds = getBounds();
        g2.fill(AffineTransform.getTranslateInstance(bounds.getCenterX() - glyphBounds.getCenterX(),
                bounds.getCenterY() - glyphBounds.getCenterY()).createTransformedShape(glyph));
    }

    @Override
    public FreeTextNode clone()
    {
        FreeTextNode cloned = (FreeTextNode) super.clone();
        cloned.text = text.clone();
        cloned.toolIcon = false;
        return cloned;
    }

    private MultiLineString text;

    /** Angle in degrees, clockwise on screen, from 0 excluded to 360 excluded; null for none (not written in files) */
    private Double rotation;

    /** Only set on the tool bar prototype (never persisted) */
    private transient boolean toolIcon;

    private static final int MINIMUM_WIDTH = 20;
    private static final int MINIMUM_HEIGHT = 16;
    private static final int TOOL_ICON_SIZE = 40;

    /** Just above notes, which are drawn at level 10000 */
    private static final int ABOVE_NOTES_Z_LEVEL = 10001;
}
