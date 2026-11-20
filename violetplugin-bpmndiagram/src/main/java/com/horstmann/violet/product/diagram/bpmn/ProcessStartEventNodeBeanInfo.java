package com.horstmann.violet.product.diagram.bpmn;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.product.diagram.bpmn.editor.StartEventTriggerEditor;

/**
 * The properties of a start event shown in the properties dialog, in this order
 */
public class ProcessStartEventNodeBeanInfo extends SimpleBeanInfo
{

    @Override
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
            descriptors.add(createDescriptor("name", null));
            descriptors.add(createDescriptor("trigger", StartEventTriggerEditor.class));
            return descriptors.toArray(new PropertyDescriptor[descriptors.size()]);
        }
        catch (IntrospectionException exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    private PropertyDescriptor createDescriptor(String name, Class<?> editorClass) throws IntrospectionException
    {
        PropertyDescriptor descriptor = new PropertyDescriptor(name, ProcessStartEventNode.class);
        if (editorClass != null)
        {
            descriptor.setPropertyEditorClass(editorClass);
        }
        descriptor.setValue("priority", this.priority++);
        return descriptor;
    }

    private int priority;

}
