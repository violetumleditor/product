package com.horstmann.violet.product.diagram.common;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * The bean info for the FreeTextNode type. Only its text can be edited.
 */
public class FreeTextNodeBeanInfo extends SimpleBeanInfo
{
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            return new PropertyDescriptor[]
            {
                    new PropertyDescriptor("text", FreeTextNode.class)
            };
        }
        catch (IntrospectionException exception)
        {
            return null;
        }
    }
}
