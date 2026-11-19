package com.horstmann.violet.product.diagram.bpmn;

/**
 * Edits the trigger of an end event: what it throws
 */
public class EndEventTriggerEditor extends EventTriggerEditor
{

    public EndEventTriggerEditor()
    {
        super(EventTrigger.NONE, EventTrigger.MESSAGE, EventTrigger.SIGNAL, EventTrigger.ERROR, EventTrigger.TERMINATE);
    }

}
