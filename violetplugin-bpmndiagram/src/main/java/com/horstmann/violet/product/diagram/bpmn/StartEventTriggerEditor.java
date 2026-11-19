package com.horstmann.violet.product.diagram.bpmn;

/**
 * Edits the trigger of a start event: what starts the process
 */
public class StartEventTriggerEditor extends EventTriggerEditor
{

    public StartEventTriggerEditor()
    {
        super(EventTrigger.NONE, EventTrigger.MESSAGE, EventTrigger.TIMER, EventTrigger.SIGNAL, EventTrigger.CONDITIONAL);
    }

}
