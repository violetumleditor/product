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
                PointNode p = new PointNode();
                p.translate(OFFSET, OFFSET);
                PointNode q = new PointNode();
                q.translate(ICON_SIZE - OFFSET + 7, ICON_SIZE - OFFSET + 3);
                e.setStartNode(p);
                e.setStartLocation(new Point2D.Double(0, 0));
                e.setEndNode(q);
                e.setEndLocation(new Point2D.Double(0, 0));
  
                Rectangle2D bounds = new Rectangle2D.Double();
                bounds.add(p.getBounds());
                bounds.add(q.getBounds());
                bounds.add(e.getBounds());

                double width = bounds.getWidth();
                double height = bounds.getHeight();
                double scaleX = (ICON_SIZE - OFFSET) / width;
                double scaleY = (ICON_SIZE - OFFSET) / height;
                double scale = Math.min(scaleX, scaleY);

                AffineTransform oldTransform = g2.getTransform();
                g2.translate(x, y);
                g2.scale(scale, scale);
                g2.translate(Math.max((height - width) / 2, 0), Math.max((width - height) / 2, 0));

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
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getInstance().getTheme().getToggleButtonSelectedBorderColor());
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10f, new float[] { 3f, 2.5f }, 0f));
                g2.drawRoundRect(x + 2, y + 3, ICON_SIZE - 5, ICON_SIZE - 6, 4, 4);
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
    public static final GraphTool SELECTION_TOOL = new GraphTool(); 
}
