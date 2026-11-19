package com.horstmann.violet.product.diagram.bpmn;

/**
 * Edits the trigger of an intermediate event: what the process waits for
 */
public class IntermediateEventTriggerEditor extends EventTriggerEditor
{

    public IntermediateEventTriggerEditor()
    {
        super(EventTrigger.NONE, EventTrigger.MESSAGE, EventTrigger.TIMER, EventTrigger.SIGNAL, EventTrigger.CONDITIONAL);
    }

}
