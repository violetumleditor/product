package com.horstmann.violet.product.diagram.state;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * The bean info for the StateTransitionEdge type. Colors are set from the color palette, not from the property sheet.
 */
public class StateTransitionEdgeBeanInfo extends SimpleBeanInfo
{
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            return new PropertyDescriptor[]
            {
                    new PropertyDescriptor("label", StateTransitionEdge.class)
            };
        }
        catch (IntrospectionException exception)
        {
            return null;
        }
    }
}
