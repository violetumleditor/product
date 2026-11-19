package com.horstmann.violet.product.diagram.bpmn;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * The properties of a sequence flow shown in the properties dialog, in this order
 */
public class BpmnSequenceFlowEdgeBeanInfo extends SimpleBeanInfo
{

    @Override
    public PropertyDescriptor[] getPropertyDescriptors()
    {
        try
        {
            List<PropertyDescriptor> descriptors = new ArrayList<PropertyDescriptor>();
            descriptors.add(createDescriptor("middleLabel", null));
            descriptors.add(createDescriptor("flowType", FlowTypeEditor.class));
            descriptors.add(createDescriptor("bentStyle", null));
            descriptors.add(createDescriptor("angleStyle", null));
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
        PropertyDescriptor descriptor = new PropertyDescriptor(name, BpmnSequenceFlowEdge.class);
        if (editorClass != null)
        {
            descriptor.setPropertyEditorClass(editorClass);
        }
        descriptor.setValue("priority", this.priority++);
        return descriptor;
    }

    private int priority;

}
