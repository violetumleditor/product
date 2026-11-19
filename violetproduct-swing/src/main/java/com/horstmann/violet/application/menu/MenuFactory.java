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

import com.horstmann.violet.application.cheerpj.CheerpJFileMenuCustomizerCommand;
import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;

/**
 * Menu factory
 * 
 * Be careful, it is not a singleton
 * 
 * @author Alexandre de Pellegrin
 * 
 */
@ManagedBean
public class MenuFactory
{

    /**
     * @param mainFrame
     * @return edit menu
     */
    public EditMenu getEditMenu(MainFrame mainFrame)
    {
        if (this.editMenu == null)
        {
            this.editMenu = new EditMenu(mainFrame);
        }
        return this.editMenu;
    }

    /**
     * @param editorFrame
     * @return file menu
     */
    public FileMenu getFileMenu(MainFrame editorFrame)
    {
        if (this.fileMenu == null)
        {
            boolean isCheerpJMode = this.launchingPreferences.isCheerpjMode();
            this.fileMenu = new FileMenu(editorFrame);
            if (isCheerpJMode)  {
                CheerpJFileMenuCustomizerCommand command = new CheerpJFileMenuCustomizerCommand(this.fileMenu);
                this.fileMenu = command.execute();
            }
        }
        return this.fileMenu;
    }

    /**
     * @param editorFrame
     * @return help menu
     */
    public HelpMenu getHelpMenu(MainFrame editorFrame)
    {
        if (this.helpMenu == null)
        {
            this.helpMenu = new HelpMenu(editorFrame);
        }
        return this.helpMenu;
    }

    /**
     * @param editorFrame
     * @return view menu
     */
    public ViewMenu getViewMenu(MainFrame editorFrame)
    {
        if (this.viewMenu == null)
        {
            this.viewMenu = new ViewMenu(editorFrame);
        }
        return this.viewMenu;
    }

    /**
     * @param editorFrame
     * @return window menu
     */    
    public DocumentMenu getDocumentMenu(MainFrame editorFrame)
    {
        if (this.documentMenu == null)
        {
            this.documentMenu = new DocumentMenu(editorFrame, getFileMenu(editorFrame));
        }
        return this.documentMenu;
    }


    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    private EditMenu editMenu;
    private FileMenu fileMenu;
    private HelpMenu helpMenu;
    private ViewMenu viewMenu;
    private DocumentMenu documentMenu;

}
