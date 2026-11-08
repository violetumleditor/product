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
import java.awt.FlowLayout;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * A row of small toggle buttons showing each value of a choice as an icon, so that the current value and the other ones are seen
 * without opening a list. It takes the height of a text field.
 */
public class ChoiceButtonGroup extends RoundedFieldPanel
{
    /**
     * @param editor the editor of the property, which gives the values, their icons and their names
     */
    public ChoiceButtonGroup(final CustomPropertyEditorSupport editor)
    {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0), 1);
        ButtonGroup group = new ButtonGroup();
        int selectedIndex = editor.getSelectedIndex();
        for (int i = 0; i < editor.getChoiceCount(); i++)
        {
            final int index = i;
            JToggleButton button = new JToggleButton(editor.getIcon(i));
            styleAsToolButton(button);
            button.setToolTipText(editor.getDisplayName(i));
            button.getAccessibleContext().setAccessibleName(editor.getDisplayName(i));
            button.setSelected(i == selectedIndex);
            button.addActionListener(e -> editor.select(index));
            group.add(button);
            add(button);
        }
    }

    /**
     * Makes a button small and flat, selected with a light tint of the accent color, as in the rows of choices of the property
     * editor.
     *
     * @param button to style
     */
    public static void styleAsToolButton(AbstractButton button)
    {
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 6; toolbar.margin: 2,3,2,3; toolbar.spacingInsets: 0,0,0,0; toolbar.selectedBackground: " + toHex(getSelectedBackground()));
    }

    /**
     * @return a light tint of the accent color over the field background, read from the current theme
     */
    private static Color getSelectedBackground()
    {
        Color accent = UIManager.getColor("Component.focusColor");
        Color background = UIManager.getColor("TextField.background");
        if (accent == null || background == null)
        {
            Color selected = UIManager.getColor("Button.toolbar.selectedBackground");
            return selected != null ? selected : Color.LIGHT_GRAY;
        }
        float ratio = 0.22f;
        return new Color(mix(accent.getRed(), background.getRed(), ratio), mix(accent.getGreen(), background.getGreen(), ratio),
                mix(accent.getBlue(), background.getBlue(), ratio));
    }

    private static int mix(int a, int b, float ratio)
    {
        return Math.round(a * ratio + b * (1 - ratio));
    }

    private static String toHex(Color color)
    {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }
}
