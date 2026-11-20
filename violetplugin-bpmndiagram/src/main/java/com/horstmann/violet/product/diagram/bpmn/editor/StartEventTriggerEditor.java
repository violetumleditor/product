package com.horstmann.violet.product.diagram.bpmn.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.bpmn.type.EventTriggerType;

/**
 * Edits the trigger of a start event: what starts the process
 */
public class StartEventTriggerEditor extends ProcessChoiceEditor
{

    public StartEventTriggerEditor()
    {
        super(EventTriggerType.values(), "trigger");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        g2.draw(new Ellipse2D.Double(5, 0, 14, 14));
        ((EventTriggerType) choice).drawMarker(g2, new Rectangle2D.Double(8, 3, 8, 8), TRANSPARENT, 1);
    }

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

}
