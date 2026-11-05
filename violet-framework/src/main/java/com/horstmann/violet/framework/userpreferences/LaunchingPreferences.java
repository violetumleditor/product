package com.horstmann.violet.framework.userpreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;

/**
 * Bean containing preferences parsed from command line arguments.
 */
@ManagedBean
public class LaunchingPreferences
{

    public static final String DARK_THEME = "dark";
    public static final String LIGHT_THEME = "light";

    public LaunchingPreferences()
    {
        this(new String[0]);
    }

    public LaunchingPreferences(String[] args)
    {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        List<String> optionArgList = new ArrayList<String>();
        for (String arg : argsList)
        {
            if ("-reset".equals(arg))
            {
                this.isResetUserPreferences = true;
                optionArgList.add(arg);
            }
            if ("-english".equals(arg))
            {
                this.isEnglishLanguageForced = true;
                optionArgList.add(arg);
            }
            if ("-help".equals(arg) || "-?".equals(arg))
            {
                this.isHelpRequested = true;
                optionArgList.add(arg);
            }
            if ("-kioskMode".equals(arg))
            {
                this.isKioskMode = true;
                optionArgList.add(arg);
            }
            if (arg.startsWith("-adminPassword="))
            {
                this.adminPassword = arg.substring("-adminPassword=".length());
                optionArgList.add(arg);
            }
            if ("-cheerpjMode".equals(arg))
            {
                this.isCheerpjMode = true;
                optionArgList.add(arg);
            }
            if (arg.startsWith("-theme="))
            {
                this.forcedTheme = parseTheme(arg.substring("-theme=".length()));
                optionArgList.add(arg);
            }
            if (arg.startsWith("-systemTheme="))
            {
                this.systemTheme = parseTheme(arg.substring("-systemTheme=".length()));
                optionArgList.add(arg);
            }
            if (arg.startsWith("-file="))
            {
                this.fileToOpen = arg.substring("-file=".length());
                optionArgList.add(arg);
            }
        }
        
    }

    private static String parseTheme(String theme)
    {
        return DARK_THEME.equals(theme) || LIGHT_THEME.equals(theme) ? theme : null;
    }

    public boolean isResetUserPreferences()
    {
        return isResetUserPreferences;
    }

    public boolean isKioskMode()
    {
        return isKioskMode;
    }

    public boolean isEnglishLanguageForced()
    {
        return isEnglishLanguageForced;
    }

    public boolean isHelpRequested()
    {
        return isHelpRequested;
    }

    public String getFileToOpen()
    {
        return fileToOpen;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public boolean isCheerpjMode()
    {
        return isCheerpjMode;
    }

    /**
     * @return {@link #DARK_THEME} or {@link #LIGHT_THEME} if a theme is imposed for this launch, or null
     */
    public String getForcedTheme()
    {
        return forcedTheme;
    }

    /**
     * @return {@link #DARK_THEME} or {@link #LIGHT_THEME} if the launcher knows the theme of the system (as a web page does), or null
     */
    public String getSystemTheme()
    {
        return systemTheme;
    }

    private boolean isResetUserPreferences;
    private boolean isKioskMode;
    private String adminPassword = "admin";
    private boolean isEnglishLanguageForced;
    private boolean isHelpRequested;
    private boolean isCheerpjMode;
    private String fileToOpen;
    private String forcedTheme;
    private String systemTheme;

}
