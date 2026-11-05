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

package com.horstmann.violet.framework.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Dark theme of the application : neutral surfaces with a slight violet tint, and the violet of the light theme as accent color.
 *
 * @author Alexandre de Pellegrin
 */
public class DarkBlueTheme extends AbstractTheme
{

    private static final Color CANVAS_COLOR = new Color(0x18, 0x16, 0x1F);
    private static final Color GRID_COLOR = new Color(0x22, 0x20, 0x2B);
    private static final Color BACKGROUND_COLOR = new Color(0x1F, 0x1D, 0x27);
    private static final Color CARD_COLOR = new Color(0x2A, 0x27, 0x35);
    private static final Color MENU_BAR_COLOR = new Color(0x4B, 0x36, 0x99);
    private static final Color SELECTED_TOOL_COLOR = new Color(0x48, 0x3B, 0x78);
    private static final Color ACCENT_COLOR = new Color(0xB3, 0x9D, 0xF5);
    private static final Color TEXT_COLOR = new Color(0xE6, 0xE4, 0xEE);
    private static final Font MENU_FONT = new Font(Font.DIALOG, Font.BOLD, 12);

    @Override
    public ThemeInfo getThemeInfo()
    {
        return new ThemeInfo("Flat Dark", DarkBlueTheme.class, FlatDarkLaf.class);
    }

    @Override
    protected void configure()
    {
        Map<String, String> defaults = new HashMap<String, String>();
        defaults.put("@background", "#1F1D27");
        defaults.put("@foreground", "#E6E4EE");
        defaults.put("@accentColor", "#8E72E0");
        defaults.put("@menuBackground", "#2A2735");
        defaults.put("Component.borderColor", "#45414F");
        defaults.put("Separator.foreground", "#45414F");
        defaults.put("MenuItem.selectionForeground", "#FFFFFF");
        defaults.put("Menu.selectionForeground", "#FFFFFF");
        defaults.put("CheckBoxMenuItem.selectionForeground", "#FFFFFF");
        defaults.put("RadioButtonMenuItem.selectionForeground", "#FFFFFF");
        defaults.put("CheckBoxMenuItem.icon.checkmarkColor", "#B39DF5");
        configureFlatLaf("#4B3699", "#5C46AD", "#3C2B7D", "#CFC6EE", "#6C4EBE", defaults);
    }

    @Override
    public boolean isDark()
    {
        return true;
    }

    @Override
    public Color getDiagramBackgroundColor()
    {
        return CANVAS_COLOR;
    }

    @Override
    public Graphics2D getDiagramGraphics(Graphics2D g2)
    {
        return new DarkDiagramGraphics2D(g2);
    }

    public Color getWhiteColor()
    {
        return CARD_COLOR;
    }

    public Color getBlackColor()
    {
        return TEXT_COLOR;
    }

    public Color getGridColor()
    {
        return GRID_COLOR;
    }

    public Color getBackgroundColor()
    {
        return BACKGROUND_COLOR;
    }

    public Font getMenubarFont()
    {
        return MENU_FONT;
    }

    public Color getMenubarBackgroundColor()
    {
        return MENU_BAR_COLOR;
    }

    public Color getMenubarForegroundColor()
    {
        return Color.WHITE;
    }

    public Color getRolloverButtonDefaultColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getRolloverButtonRolloverBorderColor()
    {
        return TEXT_COLOR;
    }

    public Color getRolloverButtonRolloverColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getSidebarBackgroundEndColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getSidebarBackgroundStartColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getSidebarBorderColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getSidebarElementBackgroundColor()
    {
        return CARD_COLOR;
    }

    public Color getSidebarElementTitleBackgroundEndColor()
    {
        return CARD_COLOR;
    }

    public Color getSidebarElementTitleBackgroundStartColor()
    {
        return CARD_COLOR;
    }

    public Color getSidebarElementForegroundColor()
    {
        return TEXT_COLOR;
    }

    public Color getSidebarElementTitleOverColor()
    {
        return Color.WHITE;
    }

    public Color getStatusbarBackgroundColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getStatusbarBorderColor()
    {
        return BACKGROUND_COLOR;
    }

    public Font getToggleButtonFont()
    {
        return MENU_FONT.deriveFont(Font.PLAIN);
    }

    public Color getToggleButtonSelectedBorderColor()
    {
        return ACCENT_COLOR;
    }

    public Color getToggleButtonSelectedColor()
    {
        return SELECTED_TOOL_COLOR;
    }

    public Color getToggleButtonUnselectedColor()
    {
        return CARD_COLOR;
    }

    public Font getWelcomeBigFont()
    {
        return MENU_FONT.deriveFont((float) 28.0);
    }

    public Font getWelcomeSmallFont()
    {
        return MENU_FONT.deriveFont((float) 12.0).deriveFont(Font.PLAIN);
    }

    public Color getWelcomeBackgroundEndColor()
    {
        return BACKGROUND_COLOR;
    }

    public Color getWelcomeBackgroundStartColor()
    {
        return CARD_COLOR;
    }

    public Color getWelcomeBigForegroundColor()
    {
        return ACCENT_COLOR;
    }

    public Color getWelcomeBigRolloverForegroundColor()
    {
        return Color.WHITE;
    }

}
