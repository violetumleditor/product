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
import java.util.HashMap;
import java.util.Map;

import javax.swing.LookAndFeel;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Light theme of the application. Its colors are constants, as the application can switch to it from any other theme.
 * 
 * @author ALexandre de Pellegrin
 */
public class FlatLightTheme extends AbstractTheme
{

    private static final Color SIDEBAR_BACKGROUND_COLOR = new Color(243, 241, 248);
    private static final Color SELECTED_TOOL_COLOR = new Color(226, 219, 247);
    private static final Color ACCENT_COLOR = new Color(108, 78, 190);
    private static final Color PANEL_COLOR = new Color(238, 238, 238);
    private static final Color TEXT_COLOR = new Color(51, 51, 51);
    private static final Color WELCOME_TEXT_COLOR = new Color(163, 184, 204);
    private static final Color GRID_COLOR = new Color(250, 250, 250);
    private static final Font MENU_FONT = new Font(Font.DIALOG, Font.BOLD, 12);

	@Override
	public ThemeInfo getThemeInfo() {
		try {
			// This cast is necessary as Class.forName returns Class<?>.
			// FlatLightLaf.class.getName() ensures it's a LookAndFeel subclass.
			@SuppressWarnings("unchecked")
			Class<? extends LookAndFeel> lnfClass = (Class<? extends LookAndFeel>) Class.forName(FlatLightLaf.class.getName());
			return new ThemeInfo("Flat Light", FlatLightTheme.class, lnfClass);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
    

    @Override
    protected void configure()
    {
        Map<String, String> defaults = new HashMap<String, String>();
        // Text selections, focus borders and default buttons derive from it
        defaults.put("@accentColor", "#6C4EBE");
        defaults.put("CheckBoxMenuItem.icon.checkmarkColor", "#6C4EBE");
        configureFlatLaf("#6C4EBE", "#7E62CC", "#5A3FA8", "#DDD6F3", "#6C4EBE", defaults);
    }

    public Color getBlackColor()
    {
        return TEXT_COLOR;
    }

    public Color getWhiteColor()
    {
        return Color.WHITE;
    }

    public Color getGridColor()
    {
        return GRID_COLOR;
    }

    public Color getBackgroundColor()
    {
        return PANEL_COLOR;
    }

    public Font getMenubarFont()
    {
        return MENU_FONT;
    }

    public Color getMenubarBackgroundColor()
    {
        return ACCENT_COLOR;
    }

    public Color getMenubarForegroundColor()
    {
        return Color.WHITE;
    }

    public Color getRolloverButtonDefaultColor()
    {
        return PANEL_COLOR;
    }

    public Color getRolloverButtonRolloverBorderColor()
    {
        return TEXT_COLOR;
    }

    public Color getRolloverButtonRolloverColor()
    {
        return PANEL_COLOR;
    }

    public Color getSidebarBackgroundEndColor()
    {
        return SIDEBAR_BACKGROUND_COLOR;
    }

    public Color getSidebarBackgroundStartColor()
    {
        return SIDEBAR_BACKGROUND_COLOR;
    }

    public Color getSidebarBorderColor()
    {
        return PANEL_COLOR;
    }

    public Color getSidebarElementBackgroundColor()
    {
        return Color.WHITE;
    }

    public Color getSidebarElementTitleBackgroundEndColor()
    {
        return PANEL_COLOR.darker().darker().darker();
    }

    public Color getSidebarElementTitleBackgroundStartColor()
    {
        return PANEL_COLOR.darker().darker().darker();
    }

    public Color getSidebarElementForegroundColor()
    {
        return PANEL_COLOR;
    }

    public Color getSidebarElementTitleOverColor()
    {
        return PANEL_COLOR.brighter();
    }

    public Color getStatusbarBackgroundColor()
    {
        return PANEL_COLOR;
    }

    public Color getStatusbarBorderColor()
    {
        return PANEL_COLOR;
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
        return Color.WHITE;
    }

    public Font getWelcomeSmallFont()
    {
        return MENU_FONT.deriveFont((float) 12.0).deriveFont(Font.PLAIN);
    }

    public Font getWelcomeBigFont()
    {
        return MENU_FONT.deriveFont((float) 28.0);
    }

    public Color getWelcomeBackgroundEndColor()
    {
        return PANEL_COLOR;
    }

    public Color getWelcomeBackgroundStartColor()
    {
        return PANEL_COLOR.brighter();
    }

    public Color getWelcomeBigForegroundColor()
    {
        return WELCOME_TEXT_COLOR;
    }

    public Color getWelcomeBigRolloverForegroundColor()
    {
        return getWelcomeBigForegroundColor().darker();
    }

}
