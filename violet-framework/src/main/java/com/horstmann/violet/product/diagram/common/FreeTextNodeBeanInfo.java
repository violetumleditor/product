package com.horstmann.violet.product.diagram.common;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import com.horstmann.violet.framework.propertyeditor.customeditor.RotationEditor;

/**
 * The bean info for the FreeTextNode type. Only its text and its angle can be edited.
 */
public class FreeTextNodeBeanInfo extends SimpleBeanInfo
{
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            return new PropertyDescriptor[]
            {
                    getTextDescriptor(),
                    getRotationDescriptor()
            };
        }
        catch (IntrospectionException exception)
        {
            return null;
        }
    }

    /**
     * The text first in the dialog, the angle below it (else sorted by name)
     */
    private PropertyDescriptor getTextDescriptor() throws IntrospectionException
    {
        PropertyDescriptor text = new PropertyDescriptor("text", FreeTextNode.class);
        text.setValue("priority", 0);
        return text;
    }

    private PropertyDescriptor getRotationDescriptor() throws IntrospectionException
    {
        PropertyDescriptor rotation = new PropertyDescriptor("rotation", FreeTextNode.class);
        rotation.setPropertyEditorClass(RotationEditor.class);
        rotation.setValue("priority", 1);
        return rotation;
    }
}
