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

/**
 * Preferences constants. (Pattern to avoid use of string to store preferences)
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class PreferencesConstant
{

    /**
     * Default constructor
     * 
     * @param key
     */
    private PreferencesConstant(String key)
    {
        this.key = key;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return this.key;
    }

    /**
     * Constant key
     */
    private String key;

    /**
     * Key to store prefered look and feel
     */
    public static final PreferencesConstant LOOK_AND_FEEL = new PreferencesConstant("look_and_feel");

    /**
     * Key to store recently opened files
     */
    public static final PreferencesConstant RECENT_FILES = new PreferencesConstant("recent");

    /**
     * Key to store files that are currently opened (usefull to restore workspace state on next session
     */
    public static final PreferencesConstant OPENED_FILES_ON_WORKSPACE = new PreferencesConstant("opened");

    /**
     * Constant just used as file separator for files
     */
    public static final PreferencesConstant FILE_SEPARATOR = new PreferencesConstant("\n");
    
    /**
     * Constant to separate directory and filename
     */
    public static final PreferencesConstant PATH_SEPARATOR = new PreferencesConstant(" -> ");

    /**
     * Key to store selected file on workspace
     */
    public static final PreferencesConstant ACTIVE_FILE = new PreferencesConstant("active");

    /**
     * Key to store user id for peer-to-peer mode. Only for for host config (in other words, program instance hosting shared
     * documents)
     */
    public static final PreferencesConstant NETWORK_HOSTCONFIG_USERID = new PreferencesConstant("network.hostconfig.userid");

    /**
     * Key to store user id for peer-to-peer mode. Only for for guest config (in other words, to access to remote documents)
     */
    public static final PreferencesConstant NETWORK_GUESTCONFIG_USERID = new PreferencesConstant("network.guestconfig.userid");

    /**
     * Key to store SERVER http url for peer-to-peer mode. Only for for guest config (in other words, to access to remote documents)
     */
    public static final PreferencesConstant NETWORK_GUESTCONFIG_HTTP_SERVERURL = new PreferencesConstant(
            "network.guestconfig.http.serverurl");

    
    /**
     * Preference constants list
     */
    public static final PreferencesConstant[] LIST;

    static
    {
        LIST = new PreferencesConstant[7];
        LIST[0] = LOOK_AND_FEEL;
        LIST[1] = RECENT_FILES;
        LIST[2] = OPENED_FILES_ON_WORKSPACE;
        LIST[3] = ACTIVE_FILE;
        LIST[4] = NETWORK_HOSTCONFIG_USERID;
        LIST[5] = NETWORK_GUESTCONFIG_USERID;
        LIST[6] = NETWORK_GUESTCONFIG_HTTP_SERVERURL;
    }

}
