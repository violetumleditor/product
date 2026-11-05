package com.horstmann.violet.framework.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;

class SystemThemeDetectorTest
{

    @Test
    void readsTheWindowsRegistryOutput()
    {
        assertTrue(SystemThemeDetector.isWindowsOutputDark("    AppsUseLightTheme    REG_DWORD    0x0\r\n"));
        assertFalse(SystemThemeDetector.isWindowsOutputDark("    AppsUseLightTheme    REG_DWORD    0x1\r\n"));
        assertFalse(SystemThemeDetector.isWindowsOutputDark(""));
    }

    @Test
    void readsTheMacOutput()
    {
        assertTrue(SystemThemeDetector.isMacOutputDark("Dark\n"));
        assertFalse(SystemThemeDetector.isMacOutputDark(""));
    }

    @Test
    void readsTheLinuxOutput()
    {
        assertTrue(SystemThemeDetector.isLinuxOutputDark("'prefer-dark'\n"));
        assertTrue(SystemThemeDetector.isLinuxOutputDark("'Adwaita-dark'\n"));
        assertFalse(SystemThemeDetector.isLinuxOutputDark("'default'\n"));
    }

    @Test
    void launchArgumentsGiveTheThemes()
    {
        LaunchingPreferences preferences = new LaunchingPreferences(new String[] { "-theme=dark", "-systemTheme=light" });
        assertEquals(LaunchingPreferences.DARK_THEME, preferences.getForcedTheme());
        assertEquals(LaunchingPreferences.LIGHT_THEME, preferences.getSystemTheme());
    }

    @Test
    void unknownThemesAreIgnored()
    {
        LaunchingPreferences preferences = new LaunchingPreferences(new String[] { "-theme=blue" });
        assertNull(preferences.getForcedTheme());
        assertNull(preferences.getSystemTheme());
    }

}
