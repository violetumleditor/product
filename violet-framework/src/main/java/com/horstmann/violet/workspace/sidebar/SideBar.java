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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.optionaltools.OptionalToolsPanel;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.ThicknessToolsBarPanel;

public class SideBar extends JPanel implements ISideBar, Scrollable
{

    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return SCROLL_UNIT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return visibleRect.height;
    }

    /**
     * As wide as the scroll pane showing it, whatever its sections need: a collapsed section no longer counts in the preferred
     * width, and the sections still open would get narrower than the side bar
     */
    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }

    public SideBar(IWorkspace diagramPanel)
    {
        this.diagramPanel = diagramPanel;
        setupUI();
    }

    private void setupUI()
    {
        setUI(new SideBarUI(this));
    }

    /**
     * Keeps the custom UI, and takes the colors of the new theme, when the look and feel changes
     */
    @Override
    public void updateUI()
    {
        if (getUI() instanceof SideBarUI)
        {
            setupUI();
            return;
        }
        super.updateUI();
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

    @Override
    public Component getHeaderAWTComponent()
    {
        if (this.header == null)
        {
            this.header = new JPanel(new BorderLayout())
            {
                @Override
                protected void paintComponent(Graphics g)
                {
                    g.setColor(ThemeManager.getInstance().getTheme().getSidebarBackgroundStartColor());
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            this.header.setOpaque(false);
            this.header.setBorder(new EmptyBorder(HEADER_GAP, HEADER_GAP, 0, HEADER_GAP));
            this.header.add(new SideBarSearchField(this::search), BorderLayout.CENTER);
        }
        return this.header;
    }

    @Override
    public Component getFooterAWTComponent()
    {
        return getStylePanel();
    }

    @Override
    public Component getCompactFooterAWTComponent()
    {
        if (this.styleButton == null)
        {
            this.styleButton = new SideBarStyleButton(getColorChoiceBar(), getThicknessChoiceBar(), getStylePanel());
        }
        return this.styleButton;
    }

    private SideBarStylePanel getStylePanel()
    {
        if (this.stylePanel == null)
        {
            this.stylePanel = new SideBarStylePanel(getStyleChoices());
        }
        return this.stylePanel;
    }

    /**
     * @return the colors above the thicknesses, shown by the open side bar or by the popup of the reduced one
     */
    private JPanel getStyleChoices()
    {
        if (this.styleChoices == null)
        {
            this.styleChoices = new JPanel();
            this.styleChoices.setLayout(new BoxLayout(this.styleChoices, BoxLayout.PAGE_AXIS));
            this.styleChoices.setOpaque(false);
            this.styleChoices.add(getColorChoiceBar().getAWTComponent());
            this.styleChoices.add(getThicknessChoiceBar().getAWTComponent());
        }
        return this.styleChoices;
    }

    /**
     * Shows the tools whose name contains a text instead of the tool sections
     *
     * @param text typed in the search field
     */
    private void search(String text)
    {
        this.searchText = text == null ? "" : text.trim();
        if (getUI() instanceof SideBarUI)
        {
            ((SideBarUI) getUI()).refreshSearch();
        }
    }

    /**
     * @return the text searched among the tools, empty if none
     */
    protected String getSearchText()
    {
        return this.searchText;
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
    private JPanel header;
    private SideBarStylePanel stylePanel;
    private SideBarStyleButton styleButton;
    private JPanel styleChoices;
    private String searchText = "";

    private static final int HEADER_GAP = 10;
    private static final int SCROLL_UNIT = 16;
    private Map<ISideBarElement, String> externalContributionElements = new HashMap<ISideBarElement, String>();

}
