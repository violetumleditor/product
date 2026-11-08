/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2008 Cay S. Horstmann (http://horstmann.com)
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

package com.horstmann.violet.application.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.beans.BeanInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import com.horstmann.violet.application.cheerpj.CheerpJInterfaceService;
import com.horstmann.violet.application.help.AboutDialog;
import com.horstmann.violet.application.menu.MenuBarToolBar;
import com.horstmann.violet.application.menu.MenuFactory;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.PostConstruct;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.IWorkspaceListener;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.WorkspacePanel;

/**
 * This desktop frame contains panes that show graphs.
 * 
 * @author Alexandre de Pellegrin
 */
@ResourceBundleBean(resourceReference = AboutDialog.class)
@ManagedBean
public class MainFrame extends JFrame
{
    
    /**
     * Constructs a blank frame with a desktop pane but no graph windows.
     * 
     */
    @PostConstruct
    public void initialize()
    {
        ResourceBundleInjector.getInjector().inject(this);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.dialogFactory.setDialogOwner(this);
        decorateFrame();
        setInitialSize();
        createMenuBar();
        getContentPane().add(this.getMainPanel());
    }


    /**
     * Sets initial size on startup
     */
    private void setInitialSize()
    {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        setBounds(screenWidth / 16, screenHeight / 16, screenWidth * 7 / 8, screenHeight * 7 / 8);
        setLocation(0, 0);
        // For screenshots only -> setBounds(50, 50, 850, 650);
    }

    /**
     * Decorates the frame (title, icon...)
     */
    private void decorateFrame()
    {
        setTitle(this.applicationName);
        setIconImage(this.applicationIcon);
    }

    /**
     * Creates menu bar
     */
    private void createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setFont(this.themeManager.getTheme().getMenubarFont());
        if (Boolean.FALSE.equals(UIManager.get("TitlePane.showIcon")))
        {
            menuBar.add(createMenuBarLogo());
        }
        menuBar.add(this.menuFactory.getFileMenu(this));
        menuBar.add(this.menuFactory.getEditMenu(this));
        menuBar.add(this.menuFactory.getViewMenu(this));
        menuBar.add(this.menuFactory.getDocumentMenu(this));
        menuBar.add(this.menuFactory.getHelpMenu(this));
        // This label replaces the title that FlatLaf draws in the window title bar, which can't be clicked
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(getFrameTitleInMenuBarLabel());
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(getMenuBarToolBar());
        setJMenuBar(menuBar);
    }

    /**
     * @return a simplified violet flower in the menu bar foreground color, as the window icon stays colored in the task bar
     */
    private JLabel createMenuBarLogo()
    {
        Path2D flower = new Path2D.Double();
        flower.append(createFlowerPetal(-84, 11.4, 3.1), false);
        flower.append(createFlowerPetal(-8, 11.2, 2.8), false);
        flower.append(createFlowerPetal(58, 9.0, 2.7), false);
        flower.append(createFlowerPetal(124, 9.0, 2.7), false);
        flower.append(createFlowerPetal(188, 11.2, 2.8), false);
        Color foreground = this.themeManager.getTheme().getMenubarForegroundColor();
        JLabel logoLabel = new JLabel(new ShapeIcon(flower, MENU_BAR_LOGO_SIZE, foreground, true));
        logoLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
        return logoLabel;
    }

    /**
     * @param angle in degrees, around the center of a 24x24 grid
     * @param length
     * @param width
     * @return a petal, separated from the center of the flower
     */
    private Shape createFlowerPetal(double angle, double length, double width)
    {
        double start = 2.2;
        Path2D petal = new Path2D.Double();
        petal.moveTo(start, 0);
        petal.curveTo(start + length * 0.25, -width * 1.1, length * 0.85, -width * 1.35, length, 0);
        petal.curveTo(length * 0.85, width * 1.35, start + length * 0.25, width * 1.1, start, 0);
        petal.closePath();
        AffineTransform transform = AffineTransform.getTranslateInstance(12, 12.5);
        transform.rotate(Math.toRadians(angle));
        return transform.createTransformedShape(petal);
    }

    @Override
    public void setTitle(String title)
    {
        super.setTitle(title);
        getFrameTitleInMenuBarLabel().setText(title);
        boolean isRenamable = getActiveWorkspace() != null;
        getFrameTitleInMenuBarLabel().setCursor(Cursor.getPredefinedCursor(isRenamable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        getFrameTitleInMenuBarLabel().setToolTipText(isRenamable ? this.renameTooltip : null);
    }

    /**
     * Highlights the title in red if the current diagram contains unsaved changes, or in black if not.
     * @param isUnsaved
     */
    public void changeTitleColor(boolean isUnsaved) {
        getFrameTitleInMenuBarLabel().setForeground(isUnsaved ? UNSAVED_TITLE_COLOR : this.themeManager.getTheme().getMenubarForegroundColor());
    }

    /**
     * @return the frame title displayed in the menu bar, as native decoration is hidden. A click on it renames the active diagram.
     */
    private JLabel getFrameTitleInMenuBarLabel()
    {
        if (this.frameTitleInMenuBarLabel == null)
        {
            this.frameTitleInMenuBarLabel = new JLabel(getTitle());
            this.frameTitleInMenuBarLabel.setForeground(this.themeManager.getTheme().getMenubarForegroundColor());
            this.frameTitleInMenuBarLabel.setHorizontalAlignment(JLabel.CENTER);
            this.frameTitleInMenuBarLabel.setFont(this.themeManager.getTheme().getMenubarFont());
            this.frameTitleInMenuBarLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
            this.frameTitleInMenuBarLabel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent event)
                {
                    if (SwingUtilities.isLeftMouseButton(event))
                    {
                        MainFrame.this.menuFactory.getFileMenu(MainFrame.this).renameActiveFile();
                        MainFrame.this.menuFactory.getDocumentMenu(MainFrame.this).updateMenuItem();
                    }
                }
            });
        }
        return this.frameTitleInMenuBarLabel;
    }


    /**
     * Switches between the light and the dark theme, and remembers this choice for the next launches
     */
    public void toggleTheme()
    {
        IWorkspace activeWorkspace = getActiveWorkspace();
        this.themeManager.toggleDarkTheme();
        // Only the active workspace belongs to this frame
        for (IWorkspace aWorkspace : getWorkspaceList())
        {
            if (!aWorkspace.equals(activeWorkspace))
            {
                SwingUtilities.updateComponentTreeUI(aWorkspace.getAWTComponent());
            }
        }
        if (this.launchingPreferences.isCheerpjMode())
        {
            boolean isDark = this.themeManager.getTheme().isDark();
            CheerpJInterfaceService.storeTheme(isDark ? LaunchingPreferences.DARK_THEME : LaunchingPreferences.LIGHT_THEME);
        }
    }

    /**
     * Add a listener to perform action when something happens on this diagram
     * 
     * @param workspace
     */
    private void listenToWorkspaceEvents(final IWorkspace workspace)
    {
        workspace.addListener(new IWorkspaceListener()
        {
            public void titleChanged(String newTitle)
            {
                setTitle(newTitle);
            }

            public void graphCouldBeSaved()
            {
                changeTitleColor(true);
            }

            @Override
            public void graphSaved() {
                changeTitleColor(false);
                // Its name may have changed. An auto-save can also finish after switching to another diagram
                if (workspace.equals(getActiveWorkspace())) {
                    userPreferencesService.setActiveDiagramFile(workspace.getGraphFile());
                }
            }

            public void mustOpenfile(IFile file)
            {
                try
                {
                    IGraphFile graphFile = new GraphFile(file);
                    IWorkspace newWorkspace = new Workspace(graphFile);
                    addWorkspace(newWorkspace);
                }
                catch (IOException e)
                {
                    DialogFactory.getInstance().showErrorDialog(e.getMessage());
                }
            }
        });
    }

    /**
     * Removes a diagram panel from this editor frame
     * 
     * @param diagramPanel
     */
    public void removeWorkspace(IWorkspace workspaceToRemove)
    {
        if (!getWorkspaceList().contains(workspaceToRemove))
        {
            return;
        }
        int pos = getWorkspaceList().indexOf(workspaceToRemove);
        if (pos < 0) {
            return;
        }
        boolean isWorkspaceDisplayed = workspaceToRemove.equals(getActiveWorkspace());
        if (!isWorkspaceDisplayed) {
            // TODO : update window menu here
            return;
        }
        getWorkspaceList().remove(workspaceToRemove);
        if (pos >= getWorkspaceList().size()) {
            pos = getWorkspaceList().size() - 1;
        }
        if (pos < 0) {
            Component currentWorkspaceComponent = ((BorderLayout) getMainPanel().getLayout()).getLayoutComponent(BorderLayout.CENTER);
            getMainPanel().remove(currentWorkspaceComponent);
            getMainPanel().add(new JPanel(), BorderLayout.CENTER);
            setTitle(this.applicationName);
            getMenuBarToolBar().setActiveWorkspace(null);
            menuFactory.getDocumentMenu(this).updateMenuItem();
            getMainPanel().revalidate();
            getMainPanel().repaint();
            return;
        }
        IWorkspace workspaceToDisplay = getWorkspaceList().get(pos);
        setActiveWorkspace(workspaceToDisplay);
    }
    
    public void addWorkspace(IWorkspace newWorkspace) {
        getWorkspaceList().add(newWorkspace);
        setActiveWorkspace(newWorkspace);
    }
    
    
    /**
     * Looks for an opened diagram from its file path and focus it
     * 
     * @param diagramFilePath diagram file path
     */
    public void setActiveWorkspace(IFile aGraphFile)
    {
        if (aGraphFile == null) return;
        for (IWorkspace aWorkspace : getWorkspaceList())
        {
            IFile toCompare = aWorkspace.getGraphFile();
            boolean isSameFilename = aGraphFile.getFilename().equals(toCompare.getFilename());
            if (isSameFilename)
            {
                setActiveWorkspace(aWorkspace);
                return;
            }
        }
    }
    
    public void setActiveWorkspace(IWorkspace activeWorkspace) {
        if (!getWorkspaceList().contains(activeWorkspace)) {
            return;
        }
        WorkspacePanel activeWorkspaceComponent = activeWorkspace.getAWTComponent();
        Component currentWorkspaceComponent = ((BorderLayout) getMainPanel().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        getMainPanel().remove(currentWorkspaceComponent);
        getMainPanel().add(activeWorkspaceComponent, BorderLayout.CENTER);
        listenToWorkspaceEvents(activeWorkspace);
        getMenuBarToolBar().setActiveWorkspace(activeWorkspace);
        menuFactory.getDocumentMenu(this).updateMenuItem();
        setTitle(activeWorkspace.getTitle());
        // Remembered right now: in a web browser, the application is never told that the page is reloaded or closed
        this.userPreferencesService.setActiveDiagramFile(activeWorkspace.getGraphFile());
        getMainPanel().revalidate();
        getMainPanel().repaint();
    }
    

    /**
     * @return true if at least a diagram is displayed
     */
    public boolean isThereAnyDiagramDisplayed()
    {
        return !getWorkspaceList().isEmpty();
    }

    public List<IWorkspace> getWorkspaceList()
    {
        if (this.workspaceList == null)
        {
            this.workspaceList = new ArrayList<IWorkspace>();
        }
        return this.workspaceList;
    }

    /**
     * @return selected diagram file path (or null if not one is selected; that should never happen)
     */
    public IWorkspace getActiveWorkspace()
    {
        Component activeWorkspace = ((BorderLayout) getMainPanel().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (activeWorkspace == null) {
            return null;
        }
        for (IWorkspace aWorkspace : getWorkspaceList()) {
            if (activeWorkspace.equals(aWorkspace.getAWTComponent())) {
                return aWorkspace;
            }
        }
        return null;
    }


    private JPanel getMainPanel() {
        if (this.mainPanel == null) {
            this.mainPanel = new JPanel(new BorderLayout());
            this.mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.mainPanel.add(new JPanel(), BorderLayout.CENTER);
        }
        return this.mainPanel;
    }

    private MenuBarToolBar getMenuBarToolBar() {
        if (this.menuBarToolBar == null) {
            this.menuBarToolBar = new MenuBarToolBar(this);
        }
        return this.menuBarToolBar;
    }
    
    
    
    /**
     * Readable on a colored menu bar
     */
    private static final Color UNSAVED_TITLE_COLOR = new Color(255, 190, 190);

    private static final int MENU_BAR_LOGO_SIZE = 20;

    /**
     * Main panel
     */
    private JPanel mainPanel;

    /**
     * Menu factory instance
     */
    @InjectedBean
    private MenuFactory menuFactory;

    /**
     * GUI Theme manager
     */
    @InjectedBean
    private ThemeManager themeManager;

    /**
     * Needed to display dialog boxes
     */
    @InjectedBean
    private DialogFactory dialogFactory;


    /**
     * Launching preferences
     */
    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    /**
     * Remembers the active diagram for the next launch
     */
    @InjectedBean
    private UserPreferencesService userPreferencesService;




    /**
     * Needed to open files
     */
    @InjectedBean
    private IFileChooserService fileChooserService;
    
    @ResourceBundleBean(key="app.name")
    private String applicationName;
    
    @ResourceBundleBean(key="app.icon")
    private Image applicationIcon;

    @ResourceBundleBean(resourceReference = MenuFactory.class, key = "dialog.rename.tooltip")
    private String renameTooltip;

    /**
     * All disgram workspaces
     */
    private List<IWorkspace> workspaceList;

    /**
     * Displays frame title when native decoration is hidden.
     */
    private JLabel frameTitleInMenuBarLabel;

    /** Tool bar at the right of the menu bar, which shows the zoom of the active diagram */
    private MenuBarToolBar menuBarToolBar;

    // workaround for bug #4646747 in J2SE SDK 1.4.0
    private static java.util.HashMap<Class<?>, BeanInfo> beanInfos;
    static
    {
        beanInfos = new java.util.HashMap<Class<?>, BeanInfo>();
        Class<?>[] cls = new Class<?>[]
        {
                Point2D.Double.class,
                BentStyle.class,
                ArrowHead.class,
                LineStyle.class,
                IGraph.class,
                AbstractNode.class,
        };
        for (int i = 0; i < cls.length; i++)
        {
            try
            {
                beanInfos.put(cls[i], java.beans.Introspector.getBeanInfo(cls[i]));
            }
            catch (java.beans.IntrospectionException ex)
            {
            }
        }
    }
}
