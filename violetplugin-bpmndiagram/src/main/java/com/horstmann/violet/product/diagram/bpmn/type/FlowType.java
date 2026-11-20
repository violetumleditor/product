package com.horstmann.violet.product.diagram.bpmn.type;

/**
 * The kind of a BPMN sequence flow: normal, or timer (a clock where it starts: the next step waits for a date or a delay, as a
 * scheduled task). Saved by name.
 */
public enum FlowType
{
    NORMAL, TIMER;
}
