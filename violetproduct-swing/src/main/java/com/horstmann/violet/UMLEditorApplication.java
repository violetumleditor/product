/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2002 Cay S. Horstmann (http://horstmann.com)

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

package com.horstmann.violet;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;

import com.horstmann.violet.application.cheerpj.CheerpJFileChooserService;
import com.horstmann.violet.application.cheerpj.CheerpJInterfaceService;
import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.application.gui.SplashScreen;
import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.LocalFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.plugin.PluginLoader;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.framework.util.VersionChecker;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.Workspace;

/**
 * A program for editing UML diagrams.
 */
public class UMLEditorApplication
{


    /**
     * Standalone application entry point
     * 
     * @param args (could contains file to open)
     */
    public static void main(String[] args)
    {
        new UMLEditorApplication(args);
    }

    /**
     * Default constructor
     * 
        * @param args
     */
    private UMLEditorApplication(String[] args)
    {
        BeanFactory.getFactory().register(LaunchingPreferences.class, new LaunchingPreferences(args));
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());  
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, BeanFactory.getFactory().getBean(LaunchingPreferences.class).isCheerpjMode()
                ? new CheerpJFileChooserService()
                : new JFileChooserService());
        BeanFactory.getFactory().register(CheerpJInterfaceService.class, new CheerpJInterfaceService());
        BeanInjector.getInjector().inject(this);
        
        if (this.launchingPreferences.isResetUserPreferences())
        {
            this.userPreferencesService.reset();
            System.out.println("User preferences reset done.");
        }
        if (this.launchingPreferences.isEnglishLanguageForced())
        {
            Locale.setDefault(Locale.ENGLISH);
            System.out.println("Language forced to english.");
        }
        if (this.launchingPreferences.isHelpRequested())
        {
                System.out.println("Violet UML Editor command line help. Options are :");
                System.out.println("-reset to reset user preferences,");
                System.out.println("-english to force language to english.");
                System.out.println("-theme=light or -theme=dark to impose a theme for this launch,");
                System.out.println("-systemTheme=light or -systemTheme=dark to give the system theme when it can't be detected,");
                System.out.println("-kioskMode to start violet in kiosk mode (no menu, no toolbar, no status bar).");
                System.out.println("-autoSave to save modified diagrams after 10 seconds of inactivity.");
                System.out.println("-cheeprjMode to enable special configuration for CheerpJ when app is exposed in a web browser.");
                System.exit(0);
        }
        createDefaultWorkspace();
    }
    
    

    


    /**
     * Creates workspace when application works as a standalone one. It contains :<br>
     * + plugins loading + GUI theme management + a spash screen<br>
     * + jvm checking<br>
     * + command line args<br>
     * + last workspace restore<br>
     */
    private void createDefaultWorkspace()
    {
        installPlugins();
        this.versionChecker.checkJavaVersion();
        this.mainFrame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        this.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.mainFrame.setUndecorated(this.launchingPreferences.isKioskMode());
        SplashScreen.displayOverEditor(mainFrame, this.launchingPreferences.isKioskMode() ? 0 : 1000);
        List<IFile> fullList = new ArrayList<IFile>();
        List<IFile> lastSessionFiles = this.userPreferencesService.getOpenedFilesDuringLastSession();
        fullList.addAll(lastSessionFiles);
        String fileToOpen = this.launchingPreferences.getFileToOpen();
        if (fileToOpen != null && !fileToOpen.trim().isEmpty())
        {
            try
            {
                IFile requestedFile = new LocalFile(new File(fileToOpen));
                fullList.add(requestedFile);
                this.userPreferencesService.setActiveDiagramFile(requestedFile);
            }
            catch (IOException e)
            {
                // There's nothing to do. We're starting the program
                // Some logs should be nive
                e.printStackTrace();
            }
        }
        // Open files
        for (IFile aFile : fullList)
        {
            try
            {
                IGraphFile graphFile = new GraphFile(aFile);
                IWorkspace workspace = new Workspace(graphFile);
                mainFrame.addWorkspace(workspace);
            }
            catch (Exception e)
            {
                System.err.println("Unable to open file " + aFile.getFilename() + "from location " + aFile.getDirectory());
                userPreferencesService.removeOpenedFile(aFile);
                System.err.println("Removed from user preferences!");
            }
        }
        IFile activeFile = this.userPreferencesService.getActiveDiagramFile();
        this.mainFrame.setActiveWorkspace(activeFile);
        this.mainFrame.setVisible(true);
    }

    

    /**
     * Install plugins
     */
    private void installPlugins()
    {

        this.pluginLoader.installPlugins();
    }

    public void onApplicationExit()
    {
        System.out.println("Application exit requested");
    }


    @InjectedBean
    private VersionChecker versionChecker;


    @InjectedBean
    private PluginLoader pluginLoader;

    @InjectedBean
    private UserPreferencesService userPreferencesService;

    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    @InjectedBean
    private IUserPreferencesDao userPreferencesDao;

    @InjectedBean
    private MainFrame mainFrame;

    @InjectedBean
    private IFilePersistenceService filePersistenceService;



}