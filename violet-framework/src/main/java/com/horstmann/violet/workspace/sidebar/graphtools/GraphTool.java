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

package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javax.swing.Icon;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.edge.AbstractEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.PointNode;

public class GraphTool
{

    /**
     * Constructs an edge type tool
     * 
     * @param e
     * @param label
     */
    public GraphTool(final IEdge e, String label)
    {
        this.nodeOrEdge = e;
        this.label = label;
        this.icon = new CachedIcon(new Icon()
        {
            public int getIconHeight()
            {
                return ICON_SIZE;
            }

            public int getIconWidth()
            {
                return ICON_SIZE;
            }

            public void paintIcon(Component c, Graphics g, int x, int y)
            {
                Graphics2D g2 = ThemeManager.getInstance().getTheme().getDiagramGraphics((Graphics2D) g);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Horizontal edge, from left to right (or right to left if the edge asks for it)
                PointNode p = new PointNode();
                PointNode q = new PointNode();
                double drop = EDGE_ICON_LENGTH * e.getToolIconSlope();
                if (e.isToolIconReversed())
                {
                    p.translate(EDGE_ICON_LENGTH, 0);
                    q.translate(0, drop);
                }
                else
                {
                    q.translate(EDGE_ICON_LENGTH, drop);
                }
                e.setStartNode(p);
                e.setStartLocation(new Point2D.Double(0, 0));
                e.setEndNode(q);
                e.setEndLocation(new Point2D.Double(0, 0));

                Rectangle2D bounds = e.getBounds().getBounds2D();
                bounds.add(p.getBounds());
                bounds.add(q.getBounds());

                double width = bounds.getWidth();
                double height = bounds.getHeight();
                double scaleX = ICON_SIZE / width;
                double scaleY = (ICON_SIZE - OFFSET) / height;
                double scale = Math.min(scaleX, scaleY);

                // Centered in the icon
                AffineTransform oldTransform = g2.getTransform();
                g2.translate(x + (ICON_SIZE - width * scale) / 2, y + (ICON_SIZE - height * scale) / 2);
                g2.scale(scale, scale);
                g2.translate(-bounds.getX(), -bounds.getY());

                g2.setColor(Color.black);
                e.draw(g2);
                g2.setTransform(oldTransform);
            }
        }, () -> getState(e));

    }

    /**
     * Constructs a node_old type tool
     * 
     * @param n
     * @param label
     */
    public GraphTool(final INode n, String label)
    {
        this.nodeOrEdge = n;
        this.label = label;
        this.icon = new CachedIcon(new Icon()
        {
            public int getIconHeight()
            {
                return ICON_SIZE;
            }

            public int getIconWidth()
            {
                return ICON_SIZE;
            }

            public void paintIcon(Component c, Graphics g, int x, int y)
            {
                // Use a buffer image to be more precise in rendering, especially for join/fork node_old
                double width = n.getBounds().getWidth();
                double height = n.getBounds().getHeight();
                // BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = ThemeManager.getInstance().getTheme().getDiagramGraphics((Graphics2D) g);
                // Graphics2D g2 = (Graphics2D) image.getGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                double scaleX = (ICON_SIZE - OFFSET) / width;
                double scaleY = (ICON_SIZE - OFFSET) / height;
                double scale = Math.min(scaleX, scaleY);

                AffineTransform oldTransform = g2.getTransform();
                g2.translate(x + OFFSET / 2, y + OFFSET / 2);
                g2.scale(scale, scale);
                g2.translate(Math.max((height - width) / 2, 0), Math.max((width - height) / 2, 0));
                g2.setColor(Color.black);
                // Compensates the scale, or the outlines vanish on a white side bar
                Stroke oldStroke = g2.getStroke();
                g2.setStroke(new BasicStroke((float) (1 / scale)));
                n.draw(g2);
                g2.setStroke(oldStroke);
                g2.setTransform(oldTransform);

                // g2b.drawImage(image, x, y, null);
            }
        }, () -> getState(n));
    }

    /**
     * Special constructor for the tools without element (selection, navigation)
     *
     * @param iconShape filled in the accent color of the theme, centered, a little smaller than the other icons
     * @param labelKey
     */
    private GraphTool(final Shape iconShape, String labelKey)
    {
        this.icon = new Icon()
        {
            public int getIconHeight()
            {
                return ICON_SIZE;
            }

            public int getIconWidth()
            {
                return ICON_SIZE;
            }

            public void paintIcon(Component c, Graphics g, int x, int y)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle2D shapeBounds = iconShape.getBounds2D();
                g2.translate(x + ICON_SIZE / 2.0, y + ICON_SIZE / 2.0);
                g2.scale(SELECTION_ICON_SCALE, SELECTION_ICON_SCALE);
                g2.translate(-shapeBounds.getCenterX(), -shapeBounds.getCenterY());
                g2.setColor(ThemeManager.getInstance().getTheme().getToggleButtonSelectedBorderColor());
                g2.fill(iconShape);
                g2.dispose();
            }
        };
        ResourceBundle rs = ResourceBundle.getBundle(ResourceBundleConstant.OTHER_STRINGS, Locale.getDefault());
        this.label = rs.getString(labelKey);
        this.nodeOrEdge = null;
    }

    /**
     * @return mouse pointer
     */
    private static Shape createArrow()
    {
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(5, 2);
        arrow.lineTo(5, 17);
        arrow.lineTo(8.6, 13.6);
        arrow.lineTo(11.4, 19.4);
        arrow.lineTo(13.8, 18.3);
        arrow.lineTo(11.1, 12.6);
        arrow.lineTo(16, 12.6);
        arrow.closePath();
        return arrow;
    }

    /**
     * @return open hand
     */
    private static Shape createHand()
    {
        Area hand = new Area(new RoundRectangle2D.Double(5, 9.5, 14.2, 11, 8, 8));
        hand.add(new Area(new Rectangle2D.Double(5, 10, 14.2, 4)));
        double[] fingerTops = { 3.5, 1, 1.5, 4 };
        for (int i = 0; i < fingerTops.length; i++)
        {
            hand.add(new Area(new RoundRectangle2D.Double(5 + i * 3.8, fingerTops[i], 2.8, 14 - fingerTops[i], 2.8, 2.8)));
        }
        Shape thumb = new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                .createStrokedShape(new Line2D.Double(2, 12.5, 7.5, 17.8));
        hand.add(new Area(thumb));
        return hand;
    }

    /**
     * @return what the icon of a prototype depends on beside its class: its colors and thickness (the marker's tool shows the
     *         pen chosen)
     */
    private static List<Object> getState(Object element)
    {
        List<Object> state = new ArrayList<Object>();
        if (element instanceof IColorable)
        {
            IColorable colorable = (IColorable) element;
            state.add(colorable.getBorderColor());
            state.add(colorable.getBackgroundColor());
            state.add(colorable.getTextColor());
        }
        if (element instanceof AbstractNode)
        {
            state.add(((AbstractNode) element).getBorderWidth());
        }
        if (element instanceof AbstractEdge)
        {
            state.add(((AbstractEdge) element).getBorderWidth());
        }
        return state;
    }

    /**
     * Keeps the drawing of a prototype in an image, at the scale of the screen, drawn again only when the theme, the scale or
     * the prototype's style change: a side bar redraws its icons at each hover of a button, and drawing a prototype (its texts,
     * an edge's path) is slow in the browser
     */
    private static final class CachedIcon implements Icon
    {
        private final Icon drawnIcon;
        private final Supplier<List<Object>> state;
        private BufferedImage image;
        private List<Object> imageKey;

        CachedIcon(Icon drawnIcon, Supplier<List<Object>> state)
        {
            this.drawnIcon = drawnIcon;
            this.state = state;
        }

        @Override
        public int getIconWidth()
        {
            return this.drawnIcon.getIconWidth();
        }

        @Override
        public int getIconHeight()
        {
            return this.drawnIcon.getIconHeight();
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                AffineTransform transform = g2.getTransform();
                if (transform.getShearX() != 0 || transform.getShearY() != 0 || transform.getScaleX() <= 0
                        || transform.getScaleY() <= 0)
                {
                    this.drawnIcon.paintIcon(c, g2, x, y);
                    return;
                }
                double scaleX = transform.getScaleX();
                double scaleY = transform.getScaleY();
                List<Object> key = Arrays.asList(ThemeManager.getInstance().getTheme(), scaleX, scaleY, this.state.get());
                if (this.image == null || !key.equals(this.imageKey))
                {
                    int width = (int) Math.ceil(getIconWidth() * scaleX);
                    int height = (int) Math.ceil(getIconHeight() * scaleY);
                    BufferedImage newImage = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D imageGraphics = newImage.createGraphics();
                    try
                    {
                        imageGraphics.scale(scaleX, scaleY);
                        this.drawnIcon.paintIcon(c, imageGraphics, 0, 0);
                    }
                    finally
                    {
                        imageGraphics.dispose();
                    }
                    this.image = newImage;
                    this.imageKey = key;
                }
                Point2D origin = transform.transform(new Point2D.Double(x, y), null);
                g2.setTransform(new AffineTransform());
                g2.drawImage(this.image, (int) Math.round(origin.getX()), (int) Math.round(origin.getY()), null);
            }
            finally
            {
                g2.dispose();
            }
        }
    }

    /**
     * @return tool's icon
     */
    public Icon getIcon()
    {
        return icon;
    }

    /**
     * @return tool's label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @return node_old or edge associated with this tool
     */
    public Object getNodeOrEdge()
    {
        return nodeOrEdge;
    }

    private Object nodeOrEdge;
    private Icon icon;
    private String label;
    private static final int ICON_SIZE = 20;
    private static final int OFFSET = 4;
    private static final double SELECTION_ICON_SCALE = 0.75;
    /** Length of the edge drawn in a tool icon, before scaling: arrow heads stay readable */
    private static final int EDGE_ICON_LENGTH = 34;
    public static final GraphTool SELECTION_TOOL = new GraphTool(createArrow(), "grabber.tooltip");
    /** Moves the view (see DragGraphBehavior) */
    public static final GraphTool NAVIGATION_TOOL = new GraphTool(createHand(), "navigation.tooltip");
}
