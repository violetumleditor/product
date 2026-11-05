package com.horstmann.violet.framework.theme;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Finds out if the operating system uses a dark theme. Any failure means a light one, so an unknown desktop keeps the default look.
 */
public final class SystemThemeDetector
{

    private SystemThemeDetector()
    {
        // Utility class
    }

    /**
     * @return true if the operating system is known to use a dark theme
     */
    public static boolean isSystemThemeDark()
    {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win"))
        {
            return isWindowsOutputDark(run("reg", "query", WINDOWS_THEME_KEY, "/v", "AppsUseLightTheme"));
        }
        if (osName.contains("mac"))
        {
            return isMacOutputDark(run("defaults", "read", "-g", "AppleInterfaceStyle"));
        }
        return isLinuxOutputDark(run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"))
                || isLinuxOutputDark(run("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"));
    }

    static boolean isWindowsOutputDark(String output)
    {
        return WINDOWS_DARK_OUTPUT.matcher(output).find();
    }

    static boolean isMacOutputDark(String output)
    {
        return "dark".equalsIgnoreCase(output.trim());
    }

    static boolean isLinuxOutputDark(String output)
    {
        return output.toLowerCase(Locale.ROOT).contains("dark");
    }

    /**
     * @param command
     * @return the output of a successful command, or an empty string
     */
    private static String run(String... command)
    {
        try
        {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS))
            {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0)
            {
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), Charset.defaultCharset());
        }
        catch (IOException e)
        {
            return "";
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    private static final String WINDOWS_THEME_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize";
    private static final Pattern WINDOWS_DARK_OUTPUT = Pattern.compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x0\\b");
    private static final int TIMEOUT_IN_SECONDS = 2;

}
