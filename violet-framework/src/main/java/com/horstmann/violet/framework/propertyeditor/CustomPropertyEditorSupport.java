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

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Icon;

import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;

/**
 * A helper class for showing names of objects in a property sheet that allows the user to pick one of a finite set of named values.
 */
public class CustomPropertyEditorSupport extends PropertyEditorSupport
{
    /**
     * Constructs a selector that correlates names and objects.
     * 
     * @param n the strings to display in a combo box
     * @param v the corresponding object values
     */
    public CustomPropertyEditorSupport(String[] n, Object[] v)
    {
        names = n;
        values = v;
    }

    public String[] getTags()
    {
        return names;
    }

    public String getAsText()
    {
        for (int i = 0; i < values.length; i++)
            if (getValue().equals(values[i])) return names[i];
        return null;
    }

    public void setAsText(String s)
    {
        for (int i = 0; i < names.length; i++)
            if (s.equals(names[i])) setValue(values[i]);
    }

    /**
     * @return the number of values to choose from
     */
    public int getChoiceCount()
    {
        return values.length;
    }

    /**
     * @return the index of the current value, or -1 if it is not one of the values
     */
    public int getSelectedIndex()
    {
        for (int i = 0; i < values.length; i++)
            if (values[i].equals(getValue())) return i;
        return -1;
    }

    /**
     * Sets the value at the given index
     * 
     * @param index of the value
     */
    public void select(int index)
    {
        setValue(values[index]);
    }

    /**
     * @param index of a value
     * @return an icon showing the value, or null to choose it in a combo box
     */
    public Icon getIcon(int index)
    {
        return null;
    }

    /**
     * @param index of a value
     * @return the name of the value in the user's language (key {@code <prefix>.<name>} of the node and edge strings), else its
     *         technical name
     */
    public String getDisplayName(int index)
    {
        String prefix = getResourceKeyPrefix();
        if (prefix != null)
        {
            String key = prefix + "." + names[index].toLowerCase(Locale.ROOT).replace(' ', '_');
            try
            {
                return ResourceBundle.getBundle(ResourceBundleConstant.NODE_AND_EDGE_STRINGS, Locale.getDefault()).getString(key);
            }
            catch (MissingResourceException e)
            {
                // Falls back to the technical name
            }
        }
        return names[index];
    }

    /**
     * @return the prefix of the keys holding the translated names of the values, or null if there is none
     */
    protected String getResourceKeyPrefix()
    {
        return null;
    }

    /**
     * @return the name of the edited property, or null if unknown
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * @param propertyName the name of the edited property, for editors whose presentation depends on it
     */
    public void setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
    }

    private String[] names;
    private Object[] values;
    private String propertyName;
}
