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

package com.horstmann.violet.framework.swingextension;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * This class as the same behavoiuyr of an toggle button but its state (selected or not) directly acts on it graphical rendering.
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class CustomToggleButton extends JPanel
{

   
    
    /**
     * Constructor empty button
     */
    public CustomToggleButton()
    {
        addStateMouseListener();
        setDefaultLayout();
    }
    
    
    /**
     * Constructor with text and icon 
     * 
     * @param text label text
     * @param icon associated icon
     * @param selectedColor
     * @param selectedBorderColor
     * @param unselectedColor
     * @param unselectedBorderColor
     */
    public CustomToggleButton(String text, Icon icon)
    {
        this.iconLabel.setIcon(icon);
        this.textLabel.setText(text);
        this.textLabel.setFont(ThemeManager.getInstance().getTheme().getToggleButtonFont());
        this.iconLabel.setBorder(new EmptyBorder(VGAP, ICON_LEFT_GAP, VGAP, HGAP));
        this.textLabel.setBorder(new EmptyBorder(VGAP, 0, VGAP, HGAP));
        setToolTipText(text);

        addStateMouseListener();

        setDefaultLayout();
        this.add(this.iconLabel);
        this.add(this.textLabel);
        this.textLabel.setPreferredSize(new Dimension(MAX_TEXT_WIDTH, (int) getBounds().getHeight()));
        setDefaultPreferredSize();
    }

    /**
     * Keeps selection and rollover states up to date
     */
    private void addStateMouseListener()
    {
        addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent arg0)
            {
                isSelected = true;
                repaint();
            }

            public void mouseEntered(MouseEvent arg0)
            {
                isRollover = true;
                repaint();
            }

            public void mouseExited(MouseEvent arg0)
            {
                isRollover = false;
                repaint();
            }

        });
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#removeNotify()
     */
    @Override
    public void removeNotify()
    {
        // No exit event is received when the tool list scrolls this button away
        this.isRollover = false;
        super.removeNotify();
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#setPreferredSize(java.awt.Dimension)
     */
    public void setPreferredSize(Dimension p)
    {
        super.setPreferredSize(p);
        this.textLabel.setPreferredSize(new Dimension((int) p.getWidth(), (int) getBounds().getHeight()));
    }

    /**
     * Sets default preferred size to ensure a constant width for all buttons
     */
    private void setDefaultPreferredSize() {
        setPreferredSize(new Dimension(MAX_TEXT_WIDTH, (int) getPreferredSize().getHeight()));
    }
    
    /**
     * Sets default layout (used when both icon and text are visible)
     */
    private void setDefaultLayout() {
        this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    }
    
    /**
     * Setter for text
     * 
     * @param text
     */
    public void setText(String text)
    {
        this.textLabel.setText(text);
    }

    /**
     * Setter for icon
     * 
     * @param icon
     */
    public void setIcon(Icon icon)
    {
        this.iconLabel.setIcon(icon);
    }

    /**
     * @return true if state is 'selected', false if not
     */
    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * @return true if the mouse is over this button
     */
    public boolean isRollover()
    {
        return isRollover;
    }

    /**
     * @return true if a separator line is shown above this button
     */
    public boolean isSeparatorAbove()
    {
        return isSeparatorAbove;
    }

    /**
     * Shows/Hides a separator line above this button
     *
     * @param isSeparatorAbove
     */
    public void setSeparatorAbove(boolean isSeparatorAbove)
    {
        this.isSeparatorAbove = isSeparatorAbove;
    }

    /**
     * Setter for state
     *
     * @param isSelected
     */
    public void setSelected(boolean isSelected)
    {
        this.isSelected = isSelected;
        repaint();
    }
    
    /**
     * Shows/Hides button text
     * 
     * @param isVisible
     */
    public void setTextVisible(boolean isVisible) {
        this.textLabel.setVisible(isVisible);
        if (isVisible) {
            setDefaultPreferredSize();
            setDefaultLayout();
        }
        if (!isVisible) {
            super.setPreferredSize(null);
            FlowLayout layout = new FlowLayout(FlowLayout.CENTER, 0, 0);
            setLayout(layout);
        }
    }
    
    /**
     * Shows the text centered below the icon, in a smaller font, for buttons laid out in a grid
     */
    public void setTextBelowIcon()
    {
        removeAll();
        setLayout(new BorderLayout());
        this.iconLabel.setHorizontalAlignment(JLabel.CENTER);
        this.iconLabel.setBorder(new EmptyBorder(VGAP + 1, 0, 0, 0));
        this.textLabel.setHorizontalAlignment(JLabel.CENTER);
        this.textLabel.setBorder(new EmptyBorder(1, 1, VGAP + 1, 1));
        this.textLabel.setPreferredSize(null);
        this.textLabel.setVisible(true);
        Font font = this.textLabel.getFont();
        if (font != null)
        {
            this.textLabel.setFont(font.deriveFont(font.getSize2D() - 1f));
        }
        add(this.iconLabel, BorderLayout.CENTER);
        add(this.textLabel, BorderLayout.SOUTH);
        super.setPreferredSize(null);
        revalidate();
    }

    /**
     * @return true is button's text is visible.
     */
    public boolean isTextVisible() {
        return this.textLabel.isVisible();
    }

    private boolean isSelected = false;
    private boolean isRollover = false;
    private boolean isSeparatorAbove = false;
    private JLabel iconLabel = new JLabel();;
    private JLabel textLabel = new JLabel();

    private static final int ICON_LEFT_GAP = 6;
    private static final int HGAP = 5;
    private static final int VGAP = 3;
    private static final int MAX_TEXT_WIDTH = 150;

}
