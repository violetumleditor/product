package com.horstmann.violet.preferences;

import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;

/**
 * Preferences lost when the tests end
 */
class InMemoryPreferences extends AbstractPreferences
{
    InMemoryPreferences(AbstractPreferences parent, String name)
    {
        super(parent, name);
    }

    @Override
    protected void putSpi(String key, String value)
    {
        this.values.put(key, value);
    }

    @Override
    protected String getSpi(String key)
    {
        return this.values.get(key);
    }

    @Override
    protected void removeSpi(String key)
    {
        this.values.remove(key);
    }

    @Override
    protected void removeNodeSpi()
    {
        this.values.clear();
    }

    @Override
    protected String[] keysSpi()
    {
        return this.values.keySet().toArray(new String[0]);
    }

    @Override
    protected String[] childrenNamesSpi()
    {
        return new String[0]; // the children are all in the cache of AbstractPreferences
    }

    @Override
    protected AbstractPreferences childSpi(String name)
    {
        return new InMemoryPreferences(this, name);
    }

    @Override
    protected void syncSpi()
    {
    }

    @Override
    protected void flushSpi()
    {
    }

    private final Map<String, String> values = new TreeMap<>();
}
