package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Edits the trigger of an event: each kind of event offers the triggers that make sense for it.
 */
abstract class EventTriggerEditor extends BpmnChoiceEditor
{

    EventTriggerEditor(EventTrigger... choices)
    {
        super(choices, "trigger");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        g2.draw(new Ellipse2D.Double(5, 0, 14, 14));
        ((EventTrigger) choice).drawMarker(g2, new Rectangle2D.Double(8, 3, 8, 8), false, TRANSPARENT, 1);
    }

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

}
