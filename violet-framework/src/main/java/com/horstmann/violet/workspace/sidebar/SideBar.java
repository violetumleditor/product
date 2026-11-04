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
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;

import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.optionaltools.OptionalToolsPanel;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.ThicknessToolsBarPanel;

public class SideBar extends JPanel implements ISideBar
{

    public SideBar(IWorkspace diagramPanel)
    {
        this.diagramPanel = diagramPanel;
        setupUI();
    }

    private void setupUI()
    {
        setUI(new SideBarUI(this));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.horstmann.violet.framework.display.clipboard.sidebar.ISideBar#addElement(com.horstmann.violet.framework.display.clipboard
     * .sidebar.ISideBarElement, java.lang.String)
     */
    public void addElement(ISideBarElement element, String title)
    {
        element.install(this.diagramPanel);
        this.externalContributionElements.put(element, title);
    }

    public IGraphToolsBar getGraphToolsBar()
    {
        if (this.graphToolsBar == null)
        {
            this.graphToolsBar = new GraphToolsBar();
            this.graphToolsBar.install(this.diagramPanel);
        }
        return this.graphToolsBar;
    }

    protected ISideBarElement getOptionalToolsBar()
    {
        if (this.optionalToolsBar == null)
        {
            this.optionalToolsBar = new OptionalToolsPanel();
            this.optionalToolsBar.install(this.diagramPanel);
        }
        return this.optionalToolsBar;
    }

    public IColorChoiceBar getColorChoiceBar()
    {
        if (this.colorChoiceBar == null)
        {
            this.colorChoiceBar = new ColorToolsBarPanel();
            this.colorChoiceBar.install(this.diagramPanel);
        }
        return this.colorChoiceBar;

    }

    public IThicknessChoiceBar getThicknessChoiceBar()
    {
        if (this.thicknessChoiceBar == null)
        {
            this.thicknessChoiceBar = new ThicknessToolsBarPanel();
            this.thicknessChoiceBar.install(this.diagramPanel);
        }
        return this.thicknessChoiceBar;
    }

    protected Map<ISideBarElement, String> getExternalContributionElements()
    {
        return this.externalContributionElements;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.sidebar.ISideBar#getAWTComponent()
     */
    public Component getAWTComponent()
    {
        return this;
    }

    private IWorkspace diagramPanel;
    private IGraphToolsBar graphToolsBar;
    private ISideBarElement optionalToolsBar;
    private IColorChoiceBar colorChoiceBar;
    private IThicknessChoiceBar thicknessChoiceBar;
    private Map<ISideBarElement, String> externalContributionElements = new HashMap<ISideBarElement, String>();

}
