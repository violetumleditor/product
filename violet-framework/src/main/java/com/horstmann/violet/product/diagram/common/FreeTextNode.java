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
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * A text that can be placed anywhere on a diagram. It has neither border nor background : only its text can be changed.
 * <p>
 * It is always attached to the graph, never accepts children nor edges other than generic ones (see {@link GenericEdge}), and is always drawn and found upon all other elements.
 * </p>
 */
public class FreeTextNode extends RectangularNode
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
        Rectangle2D b = text.getBounds();
        double w = Math.max(b.getWidth(), MINIMUM_WIDTH);
        double h = Math.max(b.getHeight(), MINIMUM_HEIGHT);
        return getGraph().getGridSticker().snap(new Rectangle2D.Double(x, y, w, h));
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
        else
        {
            text.draw(g2, getBounds());
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

    /** Only set on the tool bar prototype (never persisted) */
    private transient boolean toolIcon;

    private static final int MINIMUM_WIDTH = 20;
    private static final int MINIMUM_HEIGHT = 16;
    private static final int TOOL_ICON_SIZE = 40;

    /** Just above notes, which are drawn at level 10000 */
    private static final int ABOVE_NOTES_Z_LEVEL = 10001;
}
