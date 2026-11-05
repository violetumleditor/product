package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.horstmann.violet.framework.swingextension.ToolShapes;

class MenuIconsTest
{

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de" })
    void everyMenuBarShapeExists(String language)
    {
        assertShapesExist("com.horstmann.violet.application.menu.MenuFactory", language);
    }

    @ParameterizedTest
    @ValueSource(strings = { "en", "fr", "de", "pl" })
    void everyContextMenuShapeExists(String language)
    {
        assertShapesExist("com.horstmann.violet.workspace.editorpart.behavior.ShowMenuOnRightClickBehavior", language);
    }

    private void assertShapesExist(String bundleName, String language)
    {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
        int shapeCount = 0;
        for (String key : bundle.keySet())
        {
            String value = bundle.getString(key);
            if (value.startsWith("shape:"))
            {
                assertNotNull(ToolShapes.get(value.substring("shape:".length())));
                shapeCount++;
            }
        }
        assertTrue(shapeCount > 5);
    }

}
