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

package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.sidebar.ISideBarElement;

/**
 * Side tool panel interface
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public interface IGraphToolsBar extends ISideBarElement
{

    /**
     * Registers a new node_old tool to this panel.<br>
     * <br>
     * Example :<br>
     * ImageIcon imageIcon = new ImageIcon(this.getClass().getResource("/icons/72x72/welcome_create.png"));<br>
     * final ImageNode imageNode = new ImageNode(imageIcon.getImage());<br>
     * addCustomTool(imageNode, "test");<br>
     * 
     * @param nodePrototype
     * @param title
     */
    public void addTool(INode nodePrototype, String title);

    /**
     * Resets tool selection by selecting the first of the list
     */
    public void reset();

    /**
     * Declares a new listener
     * 
     * @param listener
     */
    public void addListener(IGraphToolsBarListener listener);

    /**
     * Removes a declared listener
     * 
     * @param listener
     */
    public void removeListener(IGraphToolsBarListener listener);

    /**
     * Declares a new mouse listener
     * 
     * @param listener
     */
    public void addMouseListener(IGraphToolsBarMouseListener listener);

    /**
     * Removes a declared mouse listener
     * 
     * @param listener
     */
    public void removeMouseListener(IGraphToolsBarMouseListener listener);
    
    /**
     * @return currently selected tool
     */
    public GraphTool getSelectedTool();
    
    /**
     * Sets selected tool
     * 
     * @param tool
     */
    public void setSelectedTool(GraphTool t);

    /**
     * Notifies bar of tool click
     * 
     * @param tool
     */
    public void notifyMouseEvent(GraphTool t, MouseEvent event);
    
    /**
     * @return current graph node_old tools
     */
    public List<GraphTool> getNodeTools();
    
    /**
     * @return  current graph edge tools
     */
    public List<GraphTool> getEdgeTools();

    /**
     * @return the component holding the tools common to all diagrams (selection, notes, free text, images, grouping areas)
     */
    public Component getCommonToolsAWTComponent();

    /**
     * @return the name of the diagram type, or null if unknown
     */
    public String getDiagramName();

}