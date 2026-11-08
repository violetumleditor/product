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

package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLaf;
import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;

/**
 * A toolbar button showing a letter A underlined with the current text color, which opens a grid of the colors of a palette.
 */
class TextColorButton extends JButton
{
    /**
     * @param colors offered, in display order
     */
    TextColorButton(List<Color> colors)
    {
        this.colors = colors;
        setIcon(TextFormatIcons.withChevron(TextFormatIcons.textColor(() -> selectedColor)));
        addActionListener(e -> showPalette());
    }

    /**
     * @return the colors offered
     */
    List<Color> getColors()
    {
        return colors;
    }

    /**
     * @return the current color, or null if it is none of the palette
     */
    Color getSelectedColor()
    {
        return selectedColor;
    }

    /**
     * Shows a color as the current one and notifies the color listeners, as when the user picks it
     *
     * @param color one of the palette, or null
     */
    void setSelectedColor(Color color)
    {
        this.selectedColor = color;
        repaint();
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "color");
        for (ActionListener listener : new ArrayList<ActionListener>(colorListeners))
        {
            listener.actionPerformed(event);
        }
    }

    /**
     * @param listener notified when a color is picked
     */
    void addColorListener(ActionListener listener)
    {
        colorListeners.add(listener);
    }

    /**
     * The text holds the real colors of the diagram; the dark theme shows them as on the diagram canvas.
     *
     * @param color real color
     * @return the color as seen on screen
     */
    static Color toDisplayedColor(Color color)
    {
        return FlatLaf.isLafDark() ? DarkDiagramGraphics2D.toDarkColor(color) : color;
    }

    private void showPalette()
    {
        final JPopupMenu popup = new JPopupMenu();
        JPanel grid = new JPanel(new GridLayout(0, COLUMNS, GAP, GAP));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        for (final Color color : colors)
        {
            final Swatch swatch = new Swatch(color);
            swatch.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased(MouseEvent e)
                {
                    popup.setVisible(false);
                    setSelectedColor(color);
                }
            });
            grid.add(swatch);
        }
        popup.add(grid);
        popup.show(this, 0, getHeight() + 2);
    }

    /**
     * A color of the palette, ringed with the accent when current or under the mouse
     */
    private class Swatch extends JComponent
    {
        Swatch(Color color)
        {
            this.color = color;
            setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double w = getWidth(), h = getHeight();
                g2.setColor(toDisplayedColor(color));
                g2.fill(new RoundRectangle2D.Double(3, 3, w - 6, h - 6, 4, 4));
                if (color.equals(selectedColor) || isHovered)
                {
                    Color accent = UIManager.getColor("Component.focusColor");
                    g2.setColor(accent != null ? accent : Color.GRAY);
                    g2.setStroke(new BasicStroke(color.equals(selectedColor) ? 2f : 1f));
                    g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 7, 7));
                }
            }
            finally
            {
                g2.dispose();
            }
        }

        private final Color color;
        private boolean isHovered;
    }

    private final List<Color> colors;
    private final List<ActionListener> colorListeners = new ArrayList<ActionListener>();
    private Color selectedColor;

    private static final int COLUMNS = 7;
    private static final int GAP = 2;
    private static final int SWATCH_SIZE = 22;
}
