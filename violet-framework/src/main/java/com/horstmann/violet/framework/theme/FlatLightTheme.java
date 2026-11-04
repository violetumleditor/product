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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

/**
 * Currently supported JVM theme. This themes fakes swing composants to extract current look and feel theme colors
 * 
 * @author ALexandre de Pellegrin
 */
public class FlatLightTheme extends AbstractTheme
{

    private static final Color SIDEBAR_BACKGROUND_COLOR = new Color(243, 241, 248);
    private static final Color SELECTED_TOOL_COLOR = new Color(226, 219, 247);
    private static final Color ACCENT_COLOR = new Color(108, 78, 190);


    /**
     * Colors the menu bar and the window title bar, which are merged by FlatLaf. Only FlatLaf reads these defaults, so the other
     * themes are not affected.
     */
    private void configureMenuBarColors()
    {
        Map<String, String> defaults = new HashMap<String, String>();
        defaults.put("TitlePane.unifiedBackground", "false");
        defaults.put("TitlePane.showIcon", "false");
        defaults.put("MenuBar.background", "#6C4EBE");
        defaults.put("MenuBar.foreground", "#FFFFFF");
        defaults.put("MenuBar.borderColor", "#6C4EBE");
        defaults.put("MenuBar.hoverBackground", "#7E62CC");
        defaults.put("MenuBar.selectionBackground", "#5A3FA8");
        defaults.put("MenuBar.selectionForeground", "#FFFFFF");
        defaults.put("TitlePane.background", "#6C4EBE");
        defaults.put("TitlePane.inactiveBackground", "#6C4EBE");
        defaults.put("TitlePane.foreground", "#FFFFFF");
        defaults.put("TitlePane.embeddedForeground", "#FFFFFF");
        defaults.put("TitlePane.inactiveForeground", "#DDD6F3");
        defaults.put("TitlePane.buttonHoverBackground", "#7E62CC");
        defaults.put("TitlePane.buttonPressedBackground", "#5A3FA8");
        FlatLaf.setGlobalExtraDefaults(defaults);
    }

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
        this.defaultGridColor = new Color(250, 250, 250);
        configureMenuBarColors();

        JMenuBar menuBar = new JMenuBar();
        this.basicMenubarBackground = menuBar.getBackground();
        this.basicMenubarForeground = menuBar.getForeground();
        this.basicMenubarFont = menuBar.getFont();

        JMenu menu = new JMenu();
        this.basicMenuBackground = menu.getBackground();
        this.basicMenuForeground = menu.getForeground();
        this.basicMenuFont = menu.getFont();

        JTextField textArea = new JTextField();
        this.basicWhite = textArea.getBackground();
        this.basicBlack = textArea.getForeground();

        JFrame frame = new JFrame();
        this.basicFrameBackground = frame.getBackground();
        frame.dispose();

        JProgressBar progressBar = new JProgressBar();
        this.basicProgressbarForeground = progressBar.getForeground();

        JTabbedPane pane = new JTabbedPane();
        this.basicTabbedPaneBackground = pane.getBackground();
    }

    public Color getBlackColor()
    {
        return this.basicBlack;
    }

    public Color getWhiteColor()
    {
        return this.basicWhite;
    }

    public Color getGridColor()
    {
        return this.defaultGridColor;
    }

    public Color getBackgroundColor()
    {
        return this.basicMenuBackground;
    }

    public Font getMenubarFont()
    {
        return this.basicMenubarFont;
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
        return this.basicMenuBackground;
    }

    public Color getRolloverButtonRolloverBorderColor()
    {
        return this.basicMenuForeground;
    }

    public Color getRolloverButtonRolloverColor()
    {
        return this.basicMenuBackground;
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
        return this.basicMenuBackground;
    }

    public Color getSidebarElementBackgroundColor()
    {
        return this.basicWhite;
    }

    public Color getSidebarElementTitleBackgroundEndColor()
    {
        return this.basicMenuBackground.darker().darker().darker();
    }

    public Color getSidebarElementTitleBackgroundStartColor()
    {
        return this.basicMenuBackground.darker().darker().darker();
    }

    public Color getSidebarElementForegroundColor()
    {
        return this.basicFrameBackground;
    }

    public Color getSidebarElementTitleOverColor()
    {
        return this.basicFrameBackground.brighter();
    }

    public Color getStatusbarBackgroundColor()
    {
        return this.basicMenuBackground;
    }

    public Color getStatusbarBorderColor()
    {
        return this.basicMenuBackground;
    }

    public Font getToggleButtonFont()
    {
        return this.basicMenuFont.deriveFont(Font.PLAIN);
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
        return this.basicWhite;
    }

    public Font getWelcomeSmallFont()
    {
        return this.basicMenuFont.deriveFont((float) 12.0).deriveFont(Font.PLAIN);
    }

    public Font getWelcomeBigFont()
    {
        return this.basicMenuFont.deriveFont((float) 28.0);
    }

    public Color getWelcomeBackgroundEndColor()
    {
        return this.basicMenuBackground;
    }

    public Color getWelcomeBackgroundStartColor()
    {
        return this.basicMenuBackground.brighter();
    }

    public Color getWelcomeBigForegroundColor()
    {
        return this.basicProgressbarForeground;
    }

    public Color getWelcomeBigRolloverForegroundColor()
    {
        return getWelcomeBigForegroundColor().darker();
    }

    /** Default grid color */
    private Color defaultGridColor;

    /** Menubar bg color */
    private Color basicMenubarBackground;

    /** Menubar fg color */
    private Color basicMenubarForeground;

    /** Menubar font */
    private Font basicMenubarFont;

    /** Menu bg color */
    private Color basicMenuBackground;

    /** Menu fg color */
    private Color basicMenuForeground;

    /** Menu font */
    private Font basicMenuFont;

    /** Default white */
    private Color basicWhite;

    /** Default black */
    private Color basicBlack;

    /** Frame bg color */
    private Color basicFrameBackground;

    /** Progress bar color */
    private Color basicProgressbarForeground;

    /** Tabbed pane background color */
    private Color basicTabbedPaneBackground;



}
