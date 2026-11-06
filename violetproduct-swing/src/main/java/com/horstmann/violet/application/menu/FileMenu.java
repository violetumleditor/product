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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.horstmann.violet.application.ApplicationStopper;
import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.naming.ExtensionFilter;
import com.horstmann.violet.framework.file.naming.FileNamingService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.IFileReader;
import com.horstmann.violet.framework.file.persistence.IFileWriter;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.Workspace;

/**
 * Represents the file menu on the editor frame
 *
 * @author Alexandre de Pellegrin
 */
@ResourceBundleBean(resourceReference = MenuFactory.class)
public class FileMenu extends JMenu
{

    /**
     * Default constructor
     *
     * @param mainFrame
     */
    @ResourceBundleBean(key = "file")
    public FileMenu(MainFrame mainFrame)
    {
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        createMenu();
        addWindowsClosingListener();
    }


    public MainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * @return 'new file' menu
     */
    public JMenu getFileNewMenu()
    {
        return this.fileNewMenu;
    }

    /**
     * @return recently opened file menu
     */
    public JMenu getFileRecentMenu()
    {
        return this.fileRecentMenu;
    }

    public JMenuItem getFileSaveItem()
    {
        return this.fileSaveItem;
    }

    public JMenu getFileExportMenu()
    {
        return this.fileExportMenu;
    }

    public JMenuItem getFilePrintItem() {
        return this.filePrintItem;
    }

    public JMenuItem getFileOpenItem() {
        return this.fileOpenItem;
    }

    public JMenuItem getFileCloseItem() {
        return this.fileCloseItem;
    }

    public JMenuItem getFileExitItem() {
        return this.fileExitItem;
    }

    /**
     * Initialize the menu
     */
    private void createMenu()
    {
        initFileNewMenu();
        initFileOpenItem();
        initFileCloseItem();
        initFileRecentMenu();
        initFileSaveItem();
        initFileSaveAsItem();
        initFileExportMenu();
        initFilePrintItem();
        
        this.add(this.fileNewMenu);
        this.add(this.fileOpenItem);
        this.add(this.fileCloseItem);
        this.add(this.fileRecentMenu);
        this.add(this.fileSaveItem);
        this.add(this.fileSaveAsItem);
        this.add(this.fileExportMenu);
        this.add(this.filePrintItem);
        
        if (!this.launchingPreferences.isKioskMode()) {
            initFileExitItem();
            this.add(this.fileExitItem);
        }
    }

    /**
     * Add frame listener to detect closing request
     */
    private void addWindowsClosingListener()
    {
        this.mainFrame.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent event)
            {
                stopper.exitProgram(mainFrame);
            }
        });
    }

    /**
     * Init exit menu entry
     */
    private void initFileExitItem()
    {
        this.fileExitItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                stopper.exitProgram(mainFrame);
            }
        });
        if (this.fileChooserService == null) this.fileExitItem.setEnabled(false);
    }

    /**
     * Init export submenu
     */
    private void initFileExportMenu()
    {
        initFileExportToImageItem();
        initFileExportToClipboardItem();
        initFileExportToPdfItem();
        initFileExportToJavaItem();
        initFileExportToPythonItem();

        this.fileExportMenu.add(this.fileExportToImageItem);
        this.fileExportMenu.add(this.fileExportToClipBoardItem);
        this.fileExportMenu.add(this.fileExportToPdfItem);
        // this.fileExportMenu.add(this.fileExportToJavaItem);
        // this.fileExportMenu.add(this.fileExportToPythonItem);

        if (this.fileChooserService == null) this.fileExportMenu.setEnabled(false);
    }

    /**
     * Init export to python menu entry
     */
    private void initFileExportToPythonItem()
    {
        this.fileExportToPythonItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                }
            }
        });
    }

    /**
     * Init export to java menu entry
     */
    private void initFileExportToJavaItem()
    {
        this.fileExportToJavaItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                }
            }
        });
    }

    /**
     * Init export to clipboard menu entry
     */
    private void initFileExportToClipboardItem()
    {
        this.fileExportToClipBoardItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    workspace.getGraphFile().exportToClipboard();
                }
            }
        });
    }

    /**
     * Init export to image menu entry
     */
    private void initFileExportToImageItem()
    {
        this.fileExportToImageItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    try
                    {
                        ExtensionFilter exportFilter = fileNamingService.getImageExportExtensionFilter();
                        String filename = fileNamingService.getExportFilename(workspace.getGraphFile().getFilename(), exportFilter);
                        IFileWriter fileSaver = fileChooserService.chooseAndGetFileWriter(filename, exportFilter);
                        if (fileSaver == null)
                        {
                            // Action cancelled by user
                            return;
                        }
                        OutputStream out = fileSaver.getOutputStream();
                        if (out != null)
                        {
                            String format = exportFilter.getExtension().replace(".", "");
                            workspace.getGraphFile().exportImage(out, format);
                        }
                    }
                    catch (Exception e1)
                    {
                        throw new RuntimeException(e1);
                    }
                }
            }
        });
    }

    private void initFileExportToPdfItem()
    {
        this.fileExportToPdfItem.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    try {
                        ExtensionFilter extensionFilter = fileNamingService.getPdfExtensionFilter();
                        String filename = fileNamingService.getExportFilename(workspace.getGraphFile().getFilename(), extensionFilter);
                        IFileWriter fileSaver = fileChooserService.chooseAndGetFileWriter(filename, extensionFilter);
                        if (fileSaver == null)
                        {
                            // Action cancelled by user
                            return;
                        }
                        OutputStream out = fileSaver.getOutputStream();
                        if(null == out)
                        {
                            throw new IOException("Unable to get output stream for extension "
                                                    + extensionFilter.getExtension());
                        }
                        workspace.getGraphFile().exportToPdf(out);
                    }
                    catch (IOException e1)
                    {
                        String message = MessageFormat.format(fileExportErrorMessage, e1.getMessage());
                        JOptionPane.showMessageDialog(null, message, fileExportError, JOptionPane.ERROR_MESSAGE);
                    }

                }
            }
        });
    }

    /**
     * Init 'save as' menu entry
     */
    private void initFileSaveAsItem()
    {
        this.fileSaveAsItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    IGraphFile graphFile = workspace.getGraphFile();
                    graphFile.saveToNewLocation();
                    userPreferencesService.addRecentFile(graphFile);
                }
            }
        });
        if (this.fileChooserService == null) this.fileSaveAsItem.setEnabled(false);
    }

    /**
     * Init save menu entry
     */
    private void initFileSaveItem()
    {
        this.fileSaveItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    IGraphFile graphFile = workspace.getGraphFile();
                    graphFile.save(true);
                    userPreferencesService.addRecentFile(graphFile);
                }
            }
        });
        if (this.fileChooserService == null || (this.fileChooserService != null && this.fileChooserService.isWebStart()))
        {
            this.fileSaveItem.setEnabled(false);
        }
    }

    /**
     * Init print menu entry
     */
    private void initFilePrintItem()
    {
        this.filePrintItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                IWorkspace workspace = (Workspace) mainFrame.getActiveWorkspace();
                if (workspace != null)
                {
                    workspace.getGraphFile().exportToPrinter();
                }
            }
        });
        if (this.fileChooserService == null) this.filePrintItem.setEnabled(false);
    }

    /**
     * Init close menu entry
     */
    private void initFileCloseItem()
    {
        this.fileCloseItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IWorkspace workspace = null;
                try
                {
                    workspace = (Workspace) mainFrame.getActiveWorkspace();
                }
                catch (RuntimeException e)
                {
                    // If no diagram is opened, close app
                    stopper.exitProgram(mainFrame);
                }
                if (workspace != null)
                {
                    IGraphFile graphFile = workspace.getGraphFile();
                    if (graphFile.isSaveRequired())
                    {
                        JOptionPane optionPane = new JOptionPane();
                        optionPane.setMessage(dialogCloseMessage);
                        optionPane.setOptionType(JOptionPane.YES_NO_CANCEL_OPTION);
                        optionPane.setIcon(dialogCloseIcon);
                        dialogFactory.showDialog(optionPane, dialogCloseTitle, true);

                        int result = JOptionPane.CANCEL_OPTION;
                        if (!JOptionPane.UNINITIALIZED_VALUE.equals(optionPane.getValue()))
                        {
                            result = ((Integer) optionPane.getValue()).intValue();
                        }

                        if (result == JOptionPane.YES_OPTION)
                        {
                            String filename = graphFile.getFilename();
                            if (filename == null)
                            {
                                graphFile.saveToNewLocation();
                                userPreferencesService.addRecentFile(graphFile);
                            }
                            if (filename != null)
                            {
                                graphFile.save(true);
                            }
                            if (!graphFile.isSaveRequired())
                            {
                                mainFrame.removeWorkspace(workspace);
                                userPreferencesService.removeOpenedFile(graphFile);
                            }
                        }
                        if (result == JOptionPane.NO_OPTION)
                        {
                            mainFrame.removeWorkspace(workspace);
                            userPreferencesService.removeOpenedFile(graphFile);
                        }
                    }
                    if (!graphFile.isSaveRequired())
                    {
                        mainFrame.removeWorkspace(workspace);
                        userPreferencesService.removeOpenedFile(graphFile);
                    }
                    List<IWorkspace> workspaceList = mainFrame.getWorkspaceList();
                    if (workspaceList.size() == 0)
                    {
                        mainFrame.requestFocus();
                    }
                }
            }
        });
    }

    /**
     * Init open menu entry
     */
    private void initFileOpenItem()
    {
        this.fileOpenItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IFile selectedFile = null;
                try
                {
                    ExtensionFilter[] filters = fileNamingService.getFileFilters();
                    IFileReader fileOpener = fileChooserService.chooseAndGetFileReader(filters);
                    if (fileOpener == null)
                    {
                        // Action cancelled by user
                        return;
                    }
                    selectedFile = fileOpener.getFileDefinition();
                    IGraphFile graphFile = new GraphFile(selectedFile);
                    IWorkspace workspace = new Workspace(graphFile);
                    mainFrame.addWorkspace(workspace);
                    userPreferencesService.addOpenedFile(graphFile);
                    userPreferencesService.addRecentFile(graphFile);
                }
                catch (Exception e)
                {
                    dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
                }
            }
        });
        if (this.fileChooserService == null) this.fileOpenItem.setEnabled(false);
    }

    /**
     * Init new menu entry
     */
    public void initFileNewMenu()
    {
        List<IDiagramPlugin> diagramPlugins = this.pluginRegistry.getDiagramPlugins();

        // Step 1 : sort diagram plugins by categories and names
        SortedMap<String, SortedSet<IDiagramPlugin>> diagramPluginsSortedByCategory = new TreeMap<String, SortedSet<IDiagramPlugin>>();
        for (final IDiagramPlugin aDiagramPlugin : diagramPlugins)
        {
            String category = aDiagramPlugin.getCategory();
            if (!diagramPluginsSortedByCategory.containsKey(category))
            {
                SortedSet<IDiagramPlugin> newSortedSet = new TreeSet<IDiagramPlugin>(new Comparator<IDiagramPlugin>()
                {
                    @Override
                    public int compare(IDiagramPlugin o1, IDiagramPlugin o2)
                    {
                        String n1 = o1.getName();
                        String n2 = o2.getName();
                        return n1.compareTo(n2);
                    }
                });
                diagramPluginsSortedByCategory.put(category, newSortedSet);
            }
            SortedSet<IDiagramPlugin> aSortedSet = diagramPluginsSortedByCategory.get(category);
            aSortedSet.add(aDiagramPlugin);
        }
        // Step 2 : populate menu entry
		
        for (String aCategory : diagramPluginsSortedByCategory.keySet())
        {
            String categoryName = aCategory.replaceFirst("[0-9]*\\.", "");
            JMenu categorySubMenu = new JMenu(categoryName);
            Dimension preferredSize = categorySubMenu.getPreferredSize();
            preferredSize = new Dimension((int) preferredSize.getWidth() + 30, (int) preferredSize.getHeight());
            categorySubMenu.setPreferredSize(preferredSize);
            fileNewMenu.add(categorySubMenu);
            SortedSet<IDiagramPlugin> diagramPluginsByCategory = diagramPluginsSortedByCategory.get(aCategory);
            for (final IDiagramPlugin aDiagramPlugin : diagramPluginsByCategory)
            {
                String name = aDiagramPlugin.getName();
                name = name.replaceFirst("[0-9]*\\.", "");
                JMenuItem item = new JMenuItem(name);
                item.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        Class<? extends IGraph> graphClass = aDiagramPlugin.getGraphClass();
                        IGraphFile graphFile = new GraphFile(graphClass);
                        IWorkspace diagramPanel = new Workspace(graphFile);
                        String name = aDiagramPlugin.getName();
                        name = name.replaceFirst("[0-9]*\\.", "");
                        name = unsavedPrefix + " " + name.toLowerCase();
                        diagramPanel.setTitle(name);
                        mainFrame.addWorkspace(diagramPanel);
					}
				});
				categorySubMenu.add( item );
			}
		}
	}

   
    /**
     * Init recent menu entry
     */
    public void initFileRecentMenu()
    {
        // Set entries on startup
        refreshFileRecentMenu();
        // Refresh recent files list each time the file menu is opened (a JMenu never gets the
        // keyboard focus, so a FocusListener would never fire). The list changes behind the
        // menu's back, e.g. when auto-save first writes a new diagram under its temp name.
        this.addMenuListener(new MenuListener()
        {

            public void menuSelected(MenuEvent e)
            {
                refreshFileRecentMenu();
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
        if (this.fileChooserService == null || (this.fileChooserService != null && this.fileChooserService.isWebStart()))
        {
            this.fileRecentMenu.setEnabled(false);
        }
    }

    /**
     * Updates file recent menu
     */
    private void refreshFileRecentMenu()
    {
        fileRecentMenu.removeAll();
        for (final IFile aFile : userPreferencesService.getRecentFiles())
        {
            String name = aFile.getFilename();
            JMenuItem item = new JMenuItem(name);
            fileRecentMenu.add(item);
            item.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    try
                    {
                        IGraphFile graphFile = new GraphFile(aFile);
                        IWorkspace workspace = new Workspace(graphFile);
                        mainFrame.addWorkspace(workspace);
                        userPreferencesService.addOpenedFile(aFile);
                        userPreferencesService.addRecentFile(aFile);
                    }
                    catch (Exception e)
                    {
                        dialogFactory.showErrorDialog(dialogOpenFileErrorMessage + " : " + e.getMessage());
                        userPreferencesService.removeOpenedFile(aFile);
                        userPreferencesService.removeRecentFile(aFile);
                    }
                }
            });
        }
    }

    /**
     * The file chooser to use with with menu
     */
    @InjectedBean
    private IFileChooserService fileChooserService;

    /**
     * Application stopper
     */
    private ApplicationStopper stopper = new ApplicationStopper();

    /**
     * Plugin registry
     */
    @InjectedBean
    private PluginRegistry pluginRegistry;

    /**
     * DialogBox handler
     */
    @InjectedBean
    private DialogFactory dialogFactory;

    /**
     * Access to user preferences
     */
    @InjectedBean
    private UserPreferencesService userPreferencesService;

    /**
     * File services
     */
    @InjectedBean
    private FileNamingService fileNamingService;

    /**
     * Service to convert IGraph to XML content (and XML to IGraph of course)
     */
    @InjectedBean
    private IFilePersistenceService filePersistenceService;


    /**
     * Launching preferences
     */
    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    /**
     * Application main frame
     */
    private MainFrame mainFrame;

    @ResourceBundleBean(key = "file.new")
    private JMenu fileNewMenu;

    @ResourceBundleBean(key = "file.open")
    private JMenuItem fileOpenItem;

    @ResourceBundleBean(key = "file.recent")
    private JMenu fileRecentMenu;

    @ResourceBundleBean(key = "file.close")
    private JMenuItem fileCloseItem;

    @ResourceBundleBean(key = "file.save")
    private JMenuItem fileSaveItem;

    @ResourceBundleBean(key = "file.save_as")
    private JMenuItem fileSaveAsItem;

    @ResourceBundleBean(key = "file.export_to_pdf")
    private JMenuItem fileExportToPdfItem;

    @ResourceBundleBean(key = "file.export_to_image")
    private JMenuItem fileExportToImageItem;

    @ResourceBundleBean(key = "file.export_to_clipboard")
    private JMenuItem fileExportToClipBoardItem;

    @ResourceBundleBean(key = "file.export_to_java")
    private JMenuItem fileExportToJavaItem;

    @ResourceBundleBean(key = "file.export_to_python")
    private JMenuItem fileExportToPythonItem;

    @ResourceBundleBean(key = "file.export")
    private JMenu fileExportMenu;

    @ResourceBundleBean(key = "file.print")
    private JMenuItem filePrintItem;

    @ResourceBundleBean(key = "file.exit")
    private JMenuItem fileExitItem;

    @ResourceBundleBean(key = "dialog.close.title")
    private String dialogCloseTitle;

    @ResourceBundleBean(key = "dialog.close.ok")
    private String dialogCloseMessage;

    @ResourceBundleBean(key = "dialog.close.icon")
    private ImageIcon dialogCloseIcon;

    @ResourceBundleBean(key = "dialog.open_file_failed.text")
    private String dialogOpenFileErrorMessage;

    @ResourceBundleBean(key = "dialog.open_file_content_incompatibility.text")
    private String dialogOpenFileIncompatibilityMessage;

    @ResourceBundleBean(key = "workspace.unsaved_prefix")
    private String unsavedPrefix;

    @ResourceBundleBean(key = "file.export.error")
    private String fileExportError;

    @ResourceBundleBean(key = "file.export.error.message")
    private String fileExportErrorMessage;

}
