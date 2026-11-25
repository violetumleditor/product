package com.horstmann.violet.preferences;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Keeps the tests away from the real preferences of the user. Found by java.util.prefs through META-INF/services, so by any test,
 * run by Maven or by an IDE.
 */
public class InMemoryPreferencesFactory implements PreferencesFactory
{
    @Override
    public Preferences systemRoot()
    {
        return this.systemRoot;
    }

    @Override
    public Preferences userRoot()
    {
        return this.userRoot;
    }

    private final Preferences systemRoot = new InMemoryPreferences(null, "");

    private final Preferences userRoot = new InMemoryPreferences(null, "");
}
