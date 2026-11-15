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

package com.horstmann.violet.workspace.sidebar;

import java.awt.Component;

import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;

/**
 * Side bar interface definition
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public interface ISideBar
{

    /**
     * Registers a new swing element to the side bar. This element will be automatically embedded in a collapsible panel which has a
     * title
     * 
     * @param element new element to be added
     * @param title title of the collapsible panel embedding the component
     */
    public void addElement(ISideBarElement element, String title);
    
    
    /**
     * @return the graph tools bar
     */
    public IGraphToolsBar getGraphToolsBar();
    
    
    /**
     * @return the color choice bar
     */
    public IColorChoiceBar getColorChoiceBar();

    /**
     * @return the thickness choice bar
     */
    public IThicknessChoiceBar getThicknessChoiceBar();

    
    /**
     * @return the AWT component representing this side bar
     */
    public Component getAWTComponent();

    /**
     * @return the component pinned above the tools of the open side bar, which doesn't scroll with them: the tool search
     */
    public Component getHeaderAWTComponent();

    /**
     * @return the component pinned below the tools of the open side bar, which doesn't scroll with them: colors and thickness
     */
    public Component getFooterAWTComponent();

    /**
     * @return the component pinned below the tool icons of the reduced side bar: the button opening colors and thickness
     */
    public Component getCompactFooterAWTComponent();

}
