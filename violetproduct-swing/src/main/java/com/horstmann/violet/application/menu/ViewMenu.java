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

package com.horstmann.violet.application.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.workspace.IWorkspace;

/**
 * View menu
 * 
 * @author Alexandre de Pellegrin
 * 
 */
@ResourceBundleBean(resourceReference = MenuFactory.class)
public class ViewMenu extends JMenu
{

    /**
     * Default constructor
     * 
     * @param mainFrame where this menu is attached
     */
    @ResourceBundleBean(key = "view")
    public ViewMenu(MainFrame mainFrame)
    {
        BeanInjector.getInjector().inject(this);
        ResourceBundleInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        this.createMenu();
    }

    /**
     * Initializes menu
     */
    private void createMenu()
    {

        zoomOut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                performZoomOut();
            }
        });
        this.add(zoomOut);

        zoomIn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                performZoomIn();
            }
        });
        this.add(zoomIn);
    }

    /**
     * Performs zoom out action
     */
    private void performZoomOut()
    {
        if (mainFrame.getWorkspaceList().size() == 0) return;
        IWorkspace workspace = mainFrame.getActiveWorkspace();
        workspace.getEditorPart().changeZoom(-1);
    }

    /**
     * Performs zoom in action
     */
    private void performZoomIn()
    {
        if (mainFrame.getWorkspaceList().size() == 0) return;
        IWorkspace workspace = mainFrame.getActiveWorkspace();
        workspace.getEditorPart().changeZoom(1);
    }

    /**
     * Current editor frame
     */
    private MainFrame mainFrame;

    @ResourceBundleBean(key = "view.zoom_out")
    private JMenuItem zoomOut;

    @ResourceBundleBean(key = "view.zoom_in")
    private JMenuItem zoomIn;

}
