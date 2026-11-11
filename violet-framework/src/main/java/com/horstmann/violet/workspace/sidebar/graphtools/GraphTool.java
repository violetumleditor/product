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
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.Icon;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
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
                Graphics2D g2 = ThemeManager.getInstance().getTheme().getDiagramGraphics((Graphics2D) g);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Horizontal edge, from left to right (or right to left if the edge asks for it)
                PointNode p = new PointNode();
                PointNode q = new PointNode();
                if (e.isToolIconReversed())
                {
                    p.translate(EDGE_ICON_LENGTH, 0);
                }
                else
                {
                    q.translate(EDGE_ICON_LENGTH, 0);
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
        };

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
        };
    }

    /**
     * Special constructor for selection tool
     */
    public GraphTool()
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
                // Mouse pointer, in the accent color of the theme
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(5, 2);
                arrow.lineTo(5, 17);
                arrow.lineTo(8.6, 13.6);
                arrow.lineTo(11.4, 19.4);
                arrow.lineTo(13.8, 18.3);
                arrow.lineTo(11.1, 12.6);
                arrow.lineTo(16, 12.6);
                arrow.closePath();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Centered, a little smaller than the other icons
                Rectangle2D arrowBounds = arrow.getBounds2D();
                g2.translate(x + ICON_SIZE / 2.0, y + ICON_SIZE / 2.0);
                g2.scale(SELECTION_ICON_SCALE, SELECTION_ICON_SCALE);
                g2.translate(-arrowBounds.getCenterX(), -arrowBounds.getCenterY());
                g2.setColor(ThemeManager.getInstance().getTheme().getToggleButtonSelectedBorderColor());
                g2.fill(arrow);
                g2.dispose();
            }
        };
        ResourceBundle rs = ResourceBundle.getBundle(ResourceBundleConstant.OTHER_STRINGS, Locale.getDefault());
        this.label = rs.getString("grabber.tooltip");
        this.nodeOrEdge = null;
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
    public static final GraphTool SELECTION_TOOL = new GraphTool(); 
}
