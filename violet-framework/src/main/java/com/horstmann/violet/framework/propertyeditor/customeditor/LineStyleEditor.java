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

import javax.swing.Icon;

import com.horstmann.violet.framework.propertyeditor.CustomPropertyEditorSupport;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;

/**
 * A property editor for the LineStyle type.
 */
public class LineStyleEditor extends CustomPropertyEditorSupport
{
    /**
     * Default constructor
     */
    public LineStyleEditor()
    {
        super(NAMES, VALUES);
    }

    @Override
    public Icon getIcon(int index)
    {
        return ChoiceIcons.lineStyle((LineStyle) VALUES[index]);
    }

    @Override
    protected String getResourceKeyPrefix()
    {
        return "linestyle";
    }

    /**
     * Type names
     */
    public static final String[] NAMES =
    {
            "Solid",
            "Dotted"
    };

    /**
     * Corresponding objects
     */
    public static final Object[] VALUES =
    {
            LineStyle.SOLID,
            LineStyle.DOTTED
    };
}
