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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
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

        // A menu item takes a single accelerator: the + and - of the numeric keypad are bound to the window
        bindZoomKey(KeyEvent.VK_SUBTRACT, "zoomOutFromKeypad", this::performZoomOut);
        bindZoomKey(KeyEvent.VK_ADD, "zoomInFromKeypad", this::performZoomIn);

        // Called right away: a browser accepts full screen only during a user gesture
        fullScreen.addActionListener(event -> mainFrame.toggleFullScreen());
        this.addSeparator();
        this.add(fullScreen);
        // The state can change without this menu (tool bar button, Escape key in a browser): read it when the menu opens
        this.addMenuListener(new MenuListener()
        {
            public void menuSelected(MenuEvent e)
            {
                refreshFullScreenItem();
            }

            public void menuDeselected(MenuEvent e)
            {
                // Nothing to do
            }

            public void menuCanceled(MenuEvent e)
            {
                // Nothing to do
            }
        });
        refreshFullScreenItem();
    }

    /**
     * Binds Ctrl + a key to a zoom action anywhere in the main window
     */
    private void bindZoomKey(int keyCode, String actionKey, Runnable zoom)
    {
        JRootPane rootPane = mainFrame.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK), actionKey);
        rootPane.getActionMap().put(actionKey, new AbstractAction()
        {
            public void actionPerformed(ActionEvent event)
            {
                zoom.run();
            }
        });
    }

    /**
     * Shows the full screen item only where full screen is possible (not on iPhone browsers), with the action it will do
     */
    private void refreshFullScreenItem()
    {
        fullScreen.setVisible(mainFrame.isFullScreenSupported());
        boolean isFullScreen = mainFrame.isFullScreen();
        fullScreen.setText(isFullScreen ? exitFullScreenText : enterFullScreenText);
        fullScreen.setIcon(new ShapeIcon(isFullScreen ? ToolShapes.EXIT_FULL_SCREEN : ToolShapes.FULL_SCREEN, fullScreen.getIcon().getIconWidth()));
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

    @ResourceBundleBean(key = "view.full_screen")
    private JMenuItem fullScreen;

    @ResourceBundleBean(key = "view.full_screen.text")
    private String enterFullScreenText;

    @ResourceBundleBean(key = "view.exit_full_screen.text")
    private String exitFullScreenText;

}
