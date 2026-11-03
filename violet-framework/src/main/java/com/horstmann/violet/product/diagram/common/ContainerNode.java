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
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.IResizableNode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;

/**
 * A rectangular area used to delimit a part of a diagram.
 * <p>
 * A container is always attached to the graph (never to another node), is always drawn in background (the smallest containers
 * upon the largest ones), never accepts edges nor children and, when dragged, also drags every top-level node located inside its
 * bounds (see {@link #getContainedNodes()}). Removing it never removes the nodes it delimits.
 * </p>
 * Its background is only painted when a color other than the default one has been chosen, and is painted lighter than this
 * color so that the container and the nodes it delimits can share the same color.
 *
 * @author Alexandre de Pellegrin
 */
public class ContainerNode extends RectangularNode implements IResizableNode
{

    /**
     * Default constructor
     */
    public ContainerNode()
    {
        super();
    }

    @Override
    public boolean addConnection(IEdge e)
    {
        return false;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D currentLocation = getLocation();
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        Rectangle2D preferredSize = getPreferredSize();
        if (preferredSize != null && preferredSize.getWidth() > 0 && preferredSize.getHeight() > 0)
        {
            width = preferredSize.getWidth();
            height = preferredSize.getHeight();
        }
        return new Rectangle2D.Double(currentLocation.getX(), currentLocation.getY(), width, height);
    }

    /**
     * @return the surface of this container, used to draw the smallest containers upon the largest ones
     */
    public double getArea()
    {
        Rectangle2D bounds = getBounds();
        return bounds.getWidth() * bounds.getHeight();
    }

    /**
     * Lists the top-level nodes (i.e. nodes directly attached to the graph, including other containers) which are entirely
     * located inside this container. Children nodes are not listed as they follow their parents.
     *
     * @return contained nodes
     */
    public List<INode> getContainedNodes()
    {
        List<INode> containedNodes = new ArrayList<INode>();
        Rectangle2D bounds = getBounds();
        for (INode aNode : getGraph().getAllNodes())
        {
            if (aNode == this || aNode.getParent() != null)
            {
                continue;
            }
            if (bounds.contains(aNode.getBounds()))
            {
                containedNodes.add(aNode);
            }
        }
        return containedNodes;
    }

    /**
     * Makes this node drawn as an explicit icon instead of a plain frame. Only intended for the prototype displayed in the
     * tool bar : clones and nodes loaded from files are always drawn as frames.
     */
    public void enableToolIcon()
    {
        ResourceBundleInjector.getInjector().inject(this);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        if (this.toolIcon != null)
        {
            drawToolIcon(g2);
            return;
        }

        // Backup current state
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();

        // Perform drawing
        Shape shape = getShape();
        if (isFilled())
        {
            g2.setColor(getLighterColor(getBackgroundColor()));
            g2.fill(shape);
        }
        g2.setColor(getBorderColor());
        g2.setStroke(getLineStyle().getStroke(getBorderWidth()));
        g2.draw(shape);

        // Restore first state
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Draws the tool icon as a square centered on the node bounds. The square is as large as the longest side so that the
     * icon fills the tool bar cell like other icons.
     */
    private void drawToolIcon(Graphics2D g2)
    {
        Object oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        Rectangle2D bounds = getBounds();
        double side = Math.max(bounds.getWidth(), bounds.getHeight());
        int x = (int) (bounds.getCenterX() - side / 2);
        int y = (int) (bounds.getCenterY() - side / 2);
        g2.drawImage(this.toolIcon, x, y, (int) side, (int) side, null);
        if (oldInterpolation != null)
        {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
    }

    /**
     * @return true if the user has chosen another color than the default one
     */
    private boolean isFilled()
    {
        return !ColorToolsBarPanel.DEFAULT_COLOR.getBackgroundColor().equals(getBackgroundColor());
    }

    /**
     * Blends a color with white so that a container painted with the same color as the nodes it delimits stays lighter than
     * them. The persisted color is left unchanged.
     *
     * @param color the chosen background color
     * @return a lighter (pastel) version of this color
     */
    private static Color getLighterColor(Color color)
    {
        int red = (int) Math.round(color.getRed() + (255 - color.getRed()) * WHITE_RATIO);
        int green = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * WHITE_RATIO);
        int blue = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * WHITE_RATIO);
        return new Color(red, green, blue, color.getAlpha());
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        int radius = getAngleStyle().getRadius();
        if (radius <= 0)
        {
            return bounds;
        }
        double arc = Math.min(2 * radius, Math.min(bounds.getWidth(), bounds.getHeight()));
        return new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), arc, arc);
    }

    /**
     * Sets the line style property.
     *
     * @param newValue the new value
     */
    public void setLineStyle(LineStyle newValue)
    {
        this.lineStyle = newValue;
    }

    /**
     * Gets the line style property.
     *
     * @return the line style (dotted by default)
     */
    public LineStyle getLineStyle()
    {
        if (this.lineStyle == null)
        {
            this.lineStyle = LineStyle.DOTTED;
        }
        return this.lineStyle;
    }

    /**
     * Sets the angle style property.
     *
     * @param newValue the new value
     */
    public void setAngleStyle(AngleStyle newValue)
    {
        this.angleStyle = newValue;
    }

    /**
     * Gets the angle style property.
     *
     * @return the angle style (sharp corners by default)
     */
    public AngleStyle getAngleStyle()
    {
        if (this.angleStyle == null)
        {
            this.angleStyle = AngleStyle.RAW;
        }
        return this.angleStyle;
    }

    @Override
    public ContainerNode clone()
    {
        ContainerNode cloned = (ContainerNode) super.clone();
        cloned.toolIcon = null;
        return cloned;
    }

    private LineStyle lineStyle;
    private AngleStyle angleStyle;

    /** Only set on the tool bar prototype (never persisted) */
    @ResourceBundleBean(key = "containernode.icon")
    private transient Image toolIcon;

    private static final int DEFAULT_WIDTH = 160;
    private static final int DEFAULT_HEIGHT = 100;

    /** Part of white blended into the background color (0 = chosen color, 1 = white) */
    private static final double WHITE_RATIO = 0.6;
}
