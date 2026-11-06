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

package com.horstmann.violet.framework.userpreferences;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;

/**
 * The default preferences service that uses the java.util.prefs API.
 */
@ManagedBean
public class DefaultUserPreferencesDao implements IUserPreferencesDao
{

    /**
     * Gets an instance of the service.
     * 
     * @return an instance of the service
     */
    public DefaultUserPreferencesDao()
    {
        prefs = Preferences.userNodeForPackage(DefaultUserPreferencesDao.class);
    }

    public String get(PreferencesConstant key, String defval)
    {
        return prefs.get(key.toString(), defval);
    }

    public void put(PreferencesConstant key, String defval)
    {
        if (defval.equals(prefs.get(key.toString(), null)))
        {
            return;
        }
        prefs.put(key.toString(), defval);
        try
        {
            // Otherwise java.util.prefs writes about every 30 seconds, and a reloaded web page loses what is more recent
            prefs.flush();
        }
        catch (BackingStoreException e)
        {
            // Nothing to do: java.util.prefs tries again by itself
        }
    }

    public void reset()
    {
        for (int i = 0; i < PreferencesConstant.LIST.length; i++)
        {
            prefs.remove(PreferencesConstant.LIST[i].toString());
        }
    }

    private Preferences prefs;

}
