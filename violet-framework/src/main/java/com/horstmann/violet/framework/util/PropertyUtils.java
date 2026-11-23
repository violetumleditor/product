package com.horstmann.violet.framework.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PropertyUtils
{

    public static void copyProperties(Object fromBean, Object toBean) {
        try
        {
            BeanInfo info = Introspector.getBeanInfo(fromBean.getClass());
            PropertyDescriptor[] descriptors = (PropertyDescriptor[]) info.getPropertyDescriptors().clone();
            for (int i = 0; i < descriptors.length; i++) {
                PropertyDescriptor prop = descriptors[i];
                Method getter = prop.getReadMethod();
                Method setter = prop.getWriteMethod();
                if (getter != null && setter != null) {
                    Object value = getter.invoke(fromBean, new Object[] {});
                    setter.invoke(toBean, new Object[] {value});
                }
            }
        }
        catch (IntrospectionException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * @return the public method set + PropertyName taking one parameter that accepts the value (any object type for null), or null
     */
    private static Method findSetter(Class<?> beanClass, String propertyName, Object propertyValue)
    {
        if (propertyName == null || propertyName.isEmpty())
        {
            return null;
        }
        String name = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        for (Method method : beanClass.getMethods())
        {
            if (!method.getName().equals(name) || method.getParameterCount() != 1)
            {
                continue;
            }
            Class<?> type = method.getParameterTypes()[0];
            boolean isAccepted = propertyValue == null ? !type.isPrimitive() : type.isInstance(propertyValue);
            if (isAccepted)
            {
                return method;
            }
        }
        return null;
    }

    public static void setProperty(Object bean, String propertyName, Object propertyValue)
    {
        try
        {
            BeanInfo info = Introspector.getBeanInfo(bean.getClass());
            for (PropertyDescriptor prop : info.getPropertyDescriptors()) 
            {
                if (prop.getName().equals(propertyName))
                {
                    Method setter = prop.getWriteMethod();
                    if (setter != null)
                        setter.invoke(bean, propertyValue);
                    return;
                }
            }
            // A BeanInfo lists the properties shown in the dialog only (GenericEdgeBeanInfo): a hidden one, like the offset of a
            // text dragged on an edge, is still undoable through its public setter
            Method setter = findSetter(bean.getClass(), propertyName, propertyValue);
            if (setter != null)
            {
                setter.invoke(bean, propertyValue);
            }
        }
        catch (IntrospectionException e)
        {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
    }    
}
