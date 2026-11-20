package com.horstmann.violet.product.diagram.bpmn.editor;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import com.horstmann.violet.product.diagram.bpmn.type.FlowType;

/**
 * Edits the kind of a sequence flow: normal or timer
 */
public class FlowTypeEditor extends ProcessChoiceEditor
{

    public FlowTypeEditor()
    {
        super(FlowType.values(), "flowtype");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        double y = 7;
        if (FlowType.TIMER.equals(choice))
        {
            // Line broken by a small clock
            g2.draw(new Line2D.Double(0, y, 4, y));
            g2.draw(new Line2D.Double(14, y, 17, y));
            g2.draw(new Ellipse2D.Double(4, 2, 10, 10));
            g2.draw(new Line2D.Double(9, 7, 9, 4));
            g2.draw(new Line2D.Double(9, 7, 11, 8));
        }
        else
        {
            g2.draw(new Line2D.Double(2, y, 17, y));
        }
        Path2D.Double arrow = new Path2D.Double();
        arrow.moveTo(22, y);
        arrow.lineTo(16, y - 3);
        arrow.lineTo(16, y + 3);
        arrow.closePath();
        g2.fill(arrow);
        g2.draw(arrow);
    }

}
