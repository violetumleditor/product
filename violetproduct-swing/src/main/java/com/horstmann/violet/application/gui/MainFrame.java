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
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
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
import com.horstmann.violet.application.menu.ResponsiveMenuBar;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.IGraphFileListener;
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
        List<Image> icons = new ArrayList<Image>();
        for (int size : new int[] { 16, 20, 24, 32, 40, 48, 64, 128, 256 })
        {
            icons.add(createApplicationIcon(size));
        }
        setIconImages(icons);
    }

    /**
     * @param size in pixels
     * @return the icon of the web page (apple-touch-icon.png): the white flower on a violet rounded square
     */
    private Image createApplicationIcon(int size)
    {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double arc = size * 0.45;
        g2.setColor(APPLICATION_ICON_BACKGROUND);
        g2.fill(new RoundRectangle2D.Double(0, 0, size, size, arc, arc));
        double scale = size * 0.72 / 24;
        double offset = (size - 24 * scale) / 2;
        g2.translate(offset, offset);
        g2.scale(scale, scale);
        g2.setColor(Color.WHITE);
        g2.fill(createFlower());
        g2.dispose();
        return image;
    }

    /**
     * Creates menu bar
     */
    private void createMenuBar()
    {
        JLabel logo = Boolean.FALSE.equals(UIManager.get("TitlePane.showIcon")) ? createMenuBarLogo() : null;
        List<JMenu> menus = Arrays.asList(this.menuFactory.getFileMenu(this), this.menuFactory.getEditMenu(this),
                this.menuFactory.getViewMenu(this), this.menuFactory.getDocumentMenu(this), this.menuFactory.getHelpMenu(this));
        // This label replaces the title that FlatLaf draws in the window title bar, which can't be clicked
        getRootPane().putClientProperty(FlatClientProperties.TITLE_BAR_SHOW_TITLE, false);
        JMenuBar menuBar = new ResponsiveMenuBar(logo, menus, getFrameTitleInMenuBarLabel(), getMenuBarToolBar());
        menuBar.setFont(this.themeManager.getTheme().getMenubarFont());
        setJMenuBar(menuBar);
    }

    /**
     * @return a simplified violet flower in the menu bar foreground color, as the window icon stays colored in the task bar
     */
    private JLabel createMenuBarLogo()
    {
        Color foreground = this.themeManager.getTheme().getMenubarForegroundColor();
        JLabel logoLabel = new JLabel(new ShapeIcon(createFlower(), MENU_BAR_LOGO_SIZE, foreground, true));
        logoLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
        return logoLabel;
    }

    /**
     * @return the violet flower, on a 24x24 grid
     */
    private Shape createFlower()
    {
        Path2D flower = new Path2D.Double();
        flower.append(createFlowerPetal(-84, 11.4, 3.1), false);
        flower.append(createFlowerPetal(-8, 11.2, 2.8), false);
        flower.append(createFlowerPetal(58, 9.0, 2.7), false);
        flower.append(createFlowerPetal(124, 9.0, 2.7), false);
        flower.append(createFlowerPetal(188, 11.2, 2.8), false);
        return flower;
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
        updateActiveFilenameInBrowser();
    }

    /**
     * In a web browser, keeps the title of the page on the active diagram. Its name is not taken from the title of this frame, which
     * may be prefixed while unsaved.
     */
    private void updateActiveFilenameInBrowser()
    {
        if (!this.launchingPreferences.isCheerpjMode())
        {
            return;
        }
        IWorkspace activeWorkspace = getActiveWorkspace();
        String filename = activeWorkspace == null ? null : activeWorkspace.getGraphFile().getFilename();
        String toTell = filename == null ? "" : filename;
        if (toTell.equals(this.filenameToldToBrowser))
        {
            return;
        }
        this.filenameToldToBrowser = toTell;
        CheerpJInterfaceService.setActiveFilename(toTell);
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
            // Those never displayed have no component yet, built later in the new theme
            if (!aWorkspace.equals(activeWorkspace) && aWorkspace.isAWTComponentCreated())
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
     * Switches between full screen and windowed mode. In a web browser, the whole page goes full screen.
     */
    public void toggleFullScreen()
    {
        if (this.launchingPreferences.isCheerpjMode())
        {
            CheerpJInterfaceService.toggleFullScreen();
            return;
        }
        // FlatLaf hides its title bar in full screen, the menu bar stays
        GraphicsDevice device = getGraphicsConfiguration().getDevice();
        if (isFullScreen())
        {
            device.setFullScreenWindow(null);
            SwingUtilities.invokeLater(this::restoreWindowedBounds);
            return;
        }
        this.windowedBounds = getBounds();
        this.windowedExtendedState = getExtendedState();
        device.setFullScreenWindow(this);
    }

    /**
     * After full screen on a scaled Windows screen, the native window gets the restored bounds as physical pixels (smaller
     * than the frame believes, so the end of the menu bar is cut off), and the maximized state is lost. Setting the same
     * bounds again does nothing, so a different size forces the native window to follow. Later than leaving full screen:
     * in the same event, maximizing gives bounds in physical pixels too (wider than the screen).
     */
    private void restoreWindowedBounds()
    {
        Rectangle bounds = this.windowedBounds;
        setExtendedState(NORMAL);
        setBounds(bounds.x, bounds.y, bounds.width + 1, bounds.height);
        setBounds(bounds);
        setExtendedState(this.windowedExtendedState);
    }

    /**
     * @return true if this frame, or the web page holding it, is shown in full screen
     */
    public boolean isFullScreen()
    {
        if (this.launchingPreferences.isCheerpjMode())
        {
            return CheerpJInterfaceService.isFullScreen();
        }
        return getGraphicsConfiguration().getDevice().getFullScreenWindow() == this;
    }

    /**
     * @return false if this frame can't be shown in full screen (some mobile browsers)
     */
    public boolean isFullScreenSupported()
    {
        if (this.launchingPreferences.isCheerpjMode())
        {
            return CheerpJInterfaceService.isFullScreenSupported();
        }
        return getGraphicsConfiguration().getDevice().isFullScreenSupported();
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
            getWorkspaceList().remove(workspaceToRemove);
            updateUnsavedChangesInBrowser();
            menuFactory.getDocumentMenu(this).updateMenuItem();
            return;
        }
        getWorkspaceList().remove(workspaceToRemove);
        updateUnsavedChangesInBrowser();
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
        addWorkspaceWithoutDisplay(newWorkspace);
        setActiveWorkspace(newWorkspace);
    }

    /**
     * Adds a diagram to this frame without displaying it, nor building its component: it is only listed in the Document
     * menu until chosen. At startup, the diagrams of the former session other than the active one are added this way.
     *
     * @param newWorkspace
     */
    public void addWorkspaceWithoutDisplay(IWorkspace newWorkspace) {
        getWorkspaceList().add(newWorkspace);
        listenToUnsavedChanges(newWorkspace.getGraphFile());
    }

    /**
     * In a web browser, keeps the page told whether a diagram still waits for its auto-save
     */
    private void listenToUnsavedChanges(IGraphFile graphFile)
    {
        if (!this.launchingPreferences.isCheerpjMode())
        {
            return;
        }
        graphFile.addListener(new IGraphFileListener()
        {
            @Override
            public void onFileModified()
            {
                updateUnsavedChangesInBrowser();
            }

            @Override
            public void onFileSaved()
            {
                updateUnsavedChangesInBrowser();
            }
        });
        updateUnsavedChangesInBrowser();
    }

    /**
     * Lets the page ask before being left or reloaded, only while a diagram still waits for its save
     */
    private void updateUnsavedChangesInBrowser()
    {
        if (!this.launchingPreferences.isCheerpjMode())
        {
            return;
        }
        boolean isUnsaved = getWorkspaceList().stream().anyMatch(aWorkspace -> aWorkspace.getGraphFile().isSaveRequired());
        if (this.isUnsavedChangesToldToBrowser != null && this.isUnsavedChangesToldToBrowser == isUnsaved)
        {
            return;
        }
        this.isUnsavedChangesToldToBrowser = isUnsaved;
        CheerpJInterfaceService.setUnsavedChanges(isUnsaved);
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
        if (!readGraphFile(activeWorkspace)) {
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
     * Reads the file of a diagram opened at startup without being displayed (see {@link GraphFile#openLater}). A file that
     * can't be read any more is closed and forgotten, as it was when all the diagrams were read at startup.
     *
     * @param workspace about to be displayed
     * @return false if its file can't be read
     */
    private boolean readGraphFile(IWorkspace workspace) {
        IGraphFile graphFile = workspace.getGraphFile();
        if (!(graphFile instanceof GraphFile) || ((GraphFile) graphFile).isLoaded()) {
            return true;
        }
        try {
            ((GraphFile) graphFile).load();
            return true;
        } catch (Exception e) {
            System.err.println("Unable to open file " + graphFile.getFilename() + " from location " + graphFile.getDirectory());
            e.printStackTrace();
            this.userPreferencesService.removeOpenedFile(graphFile);
            getWorkspaceList().remove(workspace);
            updateUnsavedChangesInBrowser();
            menuFactory.getDocumentMenu(this).updateMenuItem();
            return false;
        }
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
            // Not built yet = never displayed: not the active one, and not to be built for that
            if (aWorkspace.isAWTComponentCreated() && activeWorkspace.equals(aWorkspace.getAWTComponent())) {
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

    /** Background of the window icon, the violet of the web page icons */
    private static final Color APPLICATION_ICON_BACKGROUND = new Color(0x6C4EBE);

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

    @ResourceBundleBean(resourceReference = MenuFactory.class, key = "dialog.rename.tooltip")
    private String renameTooltip;

    /**
     * All disgram workspaces
     */
    private List<IWorkspace> workspaceList;

    /**
     * Last state given to the web page, null before the first one
     */
    private Boolean isUnsavedChangesToldToBrowser;

    /**
     * Last name of the active diagram given to the web page, empty if there was none, null before the first one
     */
    private String filenameToldToBrowser;

    /**
     * Extended state (maximized or not) of this frame before going full screen on the desktop
     */
    private int windowedExtendedState = NORMAL;

    /**
     * Bounds of this frame before going full screen on the desktop
     */
    private Rectangle windowedBounds;

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
