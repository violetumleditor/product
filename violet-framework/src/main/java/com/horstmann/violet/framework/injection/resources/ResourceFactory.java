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

package com.horstmann.violet.framework.injection.resources;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.framework.util.SmoothImageIcon;

public class ResourceFactory
{
    public ResourceFactory(ResourceBundle bundle)
    {
        this.bundle = bundle;
        this.referenceClass = Object.class;
    }

    public ResourceFactory(ResourceBundle bundle, Class<?> referenceClass)
    {
        this.bundle = bundle;
        this.referenceClass = referenceClass;
    }
    
    public ResourceBundle getResourceBundle() {
        return this.bundle;
    }

    public String createString(String key) {
        String text = bundle.getString(key);
        return text;
    }
    
    public JMenuItem createMenuItem(String prefix)
    {
        String text = bundle.getString(prefix + ".text");
        JMenuItem menuItem = new JMenuItem(text);
        updateMenuItem(menuItem, prefix);
        return menuItem;
    }

    public JMenuItem createCheckBoxMenuItem(String prefix)
    {
        String text = bundle.getString(prefix + ".text");
        JMenuItem menuItem = new JCheckBoxMenuItem(text);
        updateMenuItem(menuItem, prefix);
        return menuItem;
    }

    public JMenuItem createRadioButtonMenuItem(String prefix)
    {
        String text = bundle.getString(prefix + ".text");
        JMenuItem menuItem = new JRadioButtonMenuItem(text);
        updateMenuItem(menuItem, prefix);
        return menuItem;
    }

    private void updateMenuItem(JMenuItem menuItem, String prefix)
    {
        String text = null;
        String accelerator = null;

        menuItem.setName(prefix);
        try
        {
            text = bundle.getString(prefix + ".text");;
            menuItem.setText(text);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set mnemonic
        }
        try
        {
            String mnemonic = bundle.getString(prefix + ".mnemonic");
            menuItem.setMnemonic(mnemonic.charAt(0));
        }
        catch (MissingResourceException exception)
        {
            // ok not to set mnemonic
        }

        try
        {
            accelerator = bundle.getString(prefix + ".accelerator");
            menuItem.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        catch (MissingResourceException exception)
        {
            // ok not to set accelerator
        }

        try
        {
            String tooltip = bundle.getString(prefix + ".tooltip");
            menuItem.setToolTipText(tooltip);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set tooltip
        }

        try
        {
            String iconPath = bundle.getString(prefix + ".icon");
            if (iconPath != null)
            {
                menuItem.setIcon(createMenuIcon(iconPath));
            }
        }
        catch (MissingResourceException exception)
        {
            // ok not to set tooltip
        }

        if(text != null && accelerator != null) {
            ResourceShortcutProvider.getInstance().addShortcut(text, accelerator.replace(' ', '-').toUpperCase());
        }
    }

    /**
     * @param iconPath image path, or shape:name to get one of the {@link ToolShapes}, drawn in the color of the menu text
     * @return the menu icon
     */
    private Icon createMenuIcon(String iconPath)
    {
        if (iconPath.startsWith(SHAPE_PREFIX))
        {
            return new ShapeIcon(ToolShapes.get(iconPath.substring(SHAPE_PREFIX.length())), MENU_ICON_SIZE);
        }
        return new SmoothImageIcon(this.referenceClass.getResource(iconPath));
    }

    public JMenu createMenu(String prefix)
    {
        String text = bundle.getString(prefix + ".text");
        JMenu menu = new JMenu(text);
        configureMenu(menu, prefix);
        return menu;
    }

    /**
     * Updates menu configuration
     * 
     * @param menu
     * @param prefix (in properties file)
     */
    public void configureMenu(JMenu menu, String prefix)
    {
        menu.setName(prefix);
        String text = bundle.getString(prefix + ".text");
        menu.setText(text);
        try
        {
            String mnemonic = bundle.getString(prefix + ".mnemonic");
            menu.setMnemonic(mnemonic.charAt(0));
        }
        catch (MissingResourceException exception)
        {
            // ok not to set mnemonic
        }

        try
        {
            String tooltip = bundle.getString(prefix + ".tooltip");
            menu.setToolTipText(tooltip);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set tooltip
        }

        try
        {
            String iconPath = bundle.getString(prefix + ".icon");
            if (iconPath != null)
            {
                menu.setIcon(createMenuIcon(iconPath));
            }
        }
        catch (MissingResourceException exception)
        {
            // ok not to set tooltip
        }
    }

    public JButton createButton(String prefix)
    {
        JButton button = new JButton();
        try
        {
            String text = bundle.getString(prefix + ".text");
            button.setText(text);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set text
        }
        try
        {
            ImageIcon icon = new SmoothImageIcon(this.referenceClass.getResource(this.bundle.getString(prefix + ".icon")));
            button.setIcon(icon);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set text
        }
        try
        {
            String mnemonic = bundle.getString(prefix + ".mnemonic");
            button.setMnemonic(mnemonic.charAt(0));
        }
        catch (MissingResourceException exception)
        {
            // ok not to set mnemonic
        }

        try
        {
            String tooltip = bundle.getString(prefix + ".tooltip");
            button.setToolTipText(tooltip);
        }
        catch (MissingResourceException exception)
        {
            // ok not to set tooltip
        }
        return button;
    }


    /**
     * Creates a font from its correponding true type file
     * 
     * @param fontResource ttf file
     * @return new Font
     */
    public Font createFont(String fontResource)
    {
        try
        {
            InputStream is = this.referenceClass.getResourceAsStream(bundle.getString(fontResource));
            Font ttfBase;
            ttfBase = Font.createFont(Font.TRUETYPE_FONT, is);
            return ttfBase;
        }
        catch (FontFormatException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an image
     * 
     * @param resourceName image file path
     * @return image
     */
    public Image createImage(String resourceName)
    {
        ImageIcon icon = createIcon(resourceName);
        return icon.getImage();
    }

    /**
     * Creates an icon image
     * 
     * @param resourceName file path
     * @return icon
     */
    public ImageIcon createIcon(String resourceName)
    {
        return new SmoothImageIcon(this.referenceClass.getResource(bundle.getString(resourceName)));
    }

    private static final String SHAPE_PREFIX = "shape:";
    private static final int MENU_ICON_SIZE = 16;

    private ResourceBundle bundle;
    
    /**
     * Class used as reference to classpath ressources such as icons
     */
    private Class<?> referenceClass;
}
