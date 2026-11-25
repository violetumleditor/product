package com.horstmann.violet.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.prefs.Preferences;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.PreferencesConstant;

/**
 * The tests never touch the real preferences of the user
 */
class PreferencesIsolationTest
{
    @Test
    void shouldKeepPreferencesInMemory()
    {
        assertInstanceOf(InMemoryPreferences.class, Preferences.userNodeForPackage(DefaultUserPreferencesDao.class));
        DefaultUserPreferencesDao dao = new DefaultUserPreferencesDao();
        String former = dao.get(PreferencesConstant.LOOK_AND_FEEL, "");
        dao.put(PreferencesConstant.LOOK_AND_FEEL, "test");
        assertEquals("test", new DefaultUserPreferencesDao().get(PreferencesConstant.LOOK_AND_FEEL, ""));
        dao.put(PreferencesConstant.LOOK_AND_FEEL, former);
    }
}
