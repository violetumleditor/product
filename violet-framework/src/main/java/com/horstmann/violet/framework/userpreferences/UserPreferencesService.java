package com.horstmann.violet.framework.userpreferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.framework.file.IFile;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;

/**
 * Manages all user preferences
 * 
 * @author alexandre de pellegrin
 *
 */
@ManagedBean
public class UserPreferencesService
{

    public void setPreferedLookAndFeel(String className)
    {
        IUserPreferencesDao pService = PreferencesServiceFactory.getInstance();
        pService.put(PreferencesConstant.LOOK_AND_FEEL, className);
    }

    /**
     * @return the theme class name forced by the user, or an empty string to follow the operating system
     */
    public String getPreferedLookAndFeel()
    {
        IUserPreferencesDao pService = PreferencesServiceFactory.getInstance();
        return pService.get(PreferencesConstant.LOOK_AND_FEEL, "");
    }

    /**
     * @return the list of lastest opened files (as path strings)
     */
    public List<IFile> getRecentFiles()
    {
        List<PreferredFile> recentFiles = new ArrayList<PreferredFile>();
        String recent = this.dao.get(PreferencesConstant.RECENT_FILES, "").trim();
        String[] strings = recent.split(PreferencesConstant.FILE_SEPARATOR.toString());
        for (String anEntry : strings)
        {
            try
            {
                PreferredFile aRecentFile = new PreferredFile(anEntry);
                if (!aRecentFile.getDirectory().equals("null") && !recentFiles.contains(aRecentFile))
                {
                    recentFiles.add(aRecentFile);
                }
            }
            catch (IOException e)
            {
                // Nothing to do, will be automatically deleted on dao.put() action
            }
        }
        updateRecentFileList(recentFiles);
        return new ArrayList<IFile>(recentFiles);
    }

    /**
     * add recently opened file into user preferences
     * 
     * @param aFile file
     */
    public void addRecentFile(IFile aFile)
    {
        PreferredFile newPreferredFile = new PreferredFile(aFile);
        List<PreferredFile> recentFileList = new ArrayList<PreferredFile>();
        recentFileList.add(newPreferredFile);
        for (IFile file : getRecentFiles())
        {
            PreferredFile aPreferredFile = new PreferredFile(file);
            if (aPreferredFile.equals(newPreferredFile))
            {
                continue;
            }
            recentFileList.add(aPreferredFile);
        }
        while (recentFileList.size() > DEFAULT_MAX_RECENT_FILES)
        {
            recentFileList.remove(recentFileList.get(recentFileList.size() - 1));
        }
        updateRecentFileList(recentFileList);
    }

    /**
     * Removes file from recently opened files list
     *
     * @param aFile file
     */
    public void removeRecentFile(IFile aFile)
    {
        PreferredFile newPreferredFile = new PreferredFile(aFile);
        List<PreferredFile> recentFileList = new ArrayList<PreferredFile>();
        for (IFile file : getRecentFiles())
        {
            recentFileList.add(new PreferredFile(file));
        }
        recentFileList.remove(newPreferredFile);
        updateRecentFileList(recentFileList);
    }

    /**
     * Update user preferences
     * 
     * @param recentFiles - set of recentFiles to be saved
     */
    private void updateRecentFileList(List<PreferredFile> recentFiles)
    {
        StringBuilder result = new StringBuilder("");
        for (IFile aFile : recentFiles)
        {
            PreferredFile aPreferredFile = new PreferredFile(aFile);
            result.append(aPreferredFile.toString()).append(PreferencesConstant.FILE_SEPARATOR.toString());
        }
        this.dao.put(PreferencesConstant.RECENT_FILES, result.toString());
    }

    /**
     * Gets opened files on last session. Used to restore workspace after restart
     * 
     * @return file list
     */
    public List<IFile> getOpenedFilesDuringLastSession()
    {
        String list = this.dao.get(PreferencesConstant.OPENED_FILES_ON_WORKSPACE, "");
        String[] strings = list.split(PreferencesConstant.FILE_SEPARATOR.toString());
        List<PreferredFile> result = new ArrayList<PreferredFile>();
        for (String anEntry : strings)
        {
            try
            {
                PreferredFile aFile = new PreferredFile(anEntry);
                if (!result.contains(aFile))
                {
                    result.add(aFile);
                }
            }
            catch (IOException a)
            {
                // We should purge list from unparsable entries
            }
        }
        updateOpenedFileList(result);
        return new ArrayList<IFile>(result);
    }

    /**
     * Saves newly opened file path into user preferences
     * 
     * @param aFile file path (should be relative or absolute)
     */
    public void addOpenedFile(IFile aFile)
    {
        PreferredFile newPreferredFile = new PreferredFile(aFile);
        List<PreferredFile> openedFileList = new ArrayList<PreferredFile>();
        openedFileList.add(newPreferredFile);
        for (IFile file : getOpenedFilesDuringLastSession())
        {
            PreferredFile aPreferredFile = new PreferredFile(file);
            if (aPreferredFile.equals(newPreferredFile))
            {
                continue;
            }
            openedFileList.add(aPreferredFile);
        }
        updateOpenedFileList(openedFileList);
    }

    /**
     * Removes newly closed file from user preferences
     * 
     * @param aFile file path (could be relative or absolute)
     */
    public void removeOpenedFile(IFile aFile)
    {
        PreferredFile newPreferredFile = new PreferredFile(aFile);
        List<PreferredFile> openedFileList = new ArrayList<PreferredFile>();
        for (IFile file : getOpenedFilesDuringLastSession())
        {
            openedFileList.add(new PreferredFile(file));
        }
        openedFileList.remove(newPreferredFile);
        updateOpenedFileList(openedFileList);
    }

    /**
     * Update user preferences
     * 
     * @param openedFiles set of Opened Files to be saved
     */
    private void updateOpenedFileList(List<PreferredFile> openedFiles)
    {
        StringBuilder result = new StringBuilder("");
        for (PreferredFile aPreferredFile : openedFiles)
        {
            result.append(aPreferredFile.toString()).append(PreferencesConstant.FILE_SEPARATOR.toString());
        }
        this.dao.put(PreferencesConstant.OPENED_FILES_ON_WORKSPACE, result.toString());
    }

    /**
     * Indicates which diagram is currently focused on workspace and saves it into user preferences
     * 
     * @param aFile file path (could be relative or absolute)
     */
    public void setActiveDiagramFile(IFile aFile)
    {
        if (aFile != null)
        {
            PreferredFile preferredFile = new PreferredFile(aFile);
            this.dao.put(PreferencesConstant.ACTIVE_FILE, preferredFile.toString());
        }
    }

    /**
     * Gets from user preferences which diagram was setted as focused
     * 
     * @return file path (could be relative or absolute). Returns null by default.
     */
    public IFile getActiveDiagramFile()
    {
        String entry = this.dao.get(PreferencesConstant.ACTIVE_FILE, "");
        IFile aFile = null;
        try
        {
            aFile = new PreferredFile(entry);
        }
        catch (IOException e)
        {
            // TODO : logger needed
        }
        return aFile;
    }

    /**
     * Clear user preferences
     */
    public void reset()
    {
        this.dao.reset();
    }

    /**
     * Allows to store and retrieve preferences
     */
    @InjectedBean
    private IUserPreferencesDao dao;

    /**
     * Recent opened files list capacity
     */
    private static final int DEFAULT_MAX_RECENT_FILES = 10;
}
