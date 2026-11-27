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

package com.horstmann.violet.framework.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.horstmann.violet.framework.propertyeditor.customeditor.MultiLineStringEditor;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;

/**
 * The texts of an edge (start, middle, end) in the properties dialog: one rich text editor at a time, chosen by a tab, so the dialog
 * keeps the size of a single text however many the edge has. A dot marks the tabs whose text is not empty, as it can't be seen
 * from another tab.
 */
class LabelTabs extends JTabbedPane
{
    /**
     * Adds a tab
     *
     * @param title short name of the text (Start, Middle, End)
     * @param toolTip its full name
     * @param editor its editor, whose custom editor is already made
     * @param editorComponent the custom editor
     */
    void addLabel(String title, String toolTip, final MultiLineStringEditor editor, Component editorComponent)
    {
        final int index = getTabCount();
        addTab(title, null, editorComponent, toolTip);
        getEditors().add(editor);
        showWhetherFilled(index);
        editor.addPropertyChangeListener(e -> showWhetherFilled(index));
    }

    /**
     * Opens the middle text, the one most edges show, unless it is empty while another one isn't
     *
     * @param middleIndex index of the tab of the middle text, or -1 if there is none
     */
    void selectFirstShown(int middleIndex)
    {
        if (middleIndex >= 0 && !isBlank(middleIndex))
        {
            setSelectedIndex(middleIndex);
            return;
        }
        for (int i = 0; i < getTabCount(); i++)
        {
            if (!isBlank(i))
            {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(Math.max(0, middleIndex));
    }

    /**
     * @return the height of the row of tabs, on screen
     */
    int getTabRowHeight()
    {
        return UIScale.scale(TAB_HEIGHT);
    }

    @Override
    public void updateUI()
    {
        super.updateUI();
        putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_CONTENT_SEPARATOR, false);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_HEIGHT, TAB_HEIGHT);
        putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ICON_PLACEMENT, TRAILING);
        setOpaque(false);
    }

    private void showWhetherFilled(int index)
    {
        setIconAt(index, isBlank(index) ? null : getFilledIcon());
    }

    private boolean isBlank(int index)
    {
        Object value = getEditors().get(index).getValue();
        return !(value instanceof MultiLineString) || ((MultiLineString) value).isBlank();
    }

    private List<MultiLineStringEditor> getEditors()
    {
        if (this.editors == null)
        {
            this.editors = new ArrayList<MultiLineStringEditor>();
        }
        return this.editors;
    }

    private Icon getFilledIcon()
    {
        if (this.filledIcon == null)
        {
            this.filledIcon = new FilledIcon();
        }
        return this.filledIcon;
    }

    /**
     * A small dot in the accent color, read when painting so that it follows the theme
     */
    private class FilledIcon implements Icon
    {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color accent = UIManager.getColor("Component.focusColor");
                g2.setColor(accent != null ? accent : c.getForeground());
                g2.fillOval(x, y, DOT, DOT);
            }
            finally
            {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth()
        {
            return DOT;
        }

        @Override
        public int getIconHeight()
        {
            return DOT;
        }
    }

    private List<MultiLineStringEditor> editors;

    private Icon filledIcon;

    private static final int TAB_HEIGHT = 28;

    private static final int DOT = 6;
}
