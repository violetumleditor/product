package com.horstmann.violet.product.diagram.common;

import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * The bean info for the MarkerNode type. It has no property to edit: its color and its thickness come from the style tools.
 */
public class MarkerNodeBeanInfo extends SimpleBeanInfo
{
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        return new PropertyDescriptor[0];
    }
}
