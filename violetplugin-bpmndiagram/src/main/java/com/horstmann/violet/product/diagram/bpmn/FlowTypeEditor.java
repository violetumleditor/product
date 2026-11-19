package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * Edits the kind of a sequence flow
 */
public class FlowTypeEditor extends BpmnChoiceEditor
{

    public FlowTypeEditor()
    {
        super(FlowType.values(), "flowtype");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        double y = 7;
        double lineStart = FlowType.CONDITIONAL.equals(choice) ? 9 : 2;
        g2.draw(new Line2D.Double(lineStart, y, 17, y));
        Path2D.Double arrow = new Path2D.Double();
        arrow.moveTo(22, y);
        arrow.lineTo(16, y - 3);
        arrow.lineTo(16, y + 3);
        arrow.closePath();
        g2.fill(arrow);
        g2.draw(arrow);
        if (FlowType.CONDITIONAL.equals(choice))
        {
            Path2D.Double diamond = new Path2D.Double();
            diamond.moveTo(1, y);
            diamond.lineTo(5, y - 3);
            diamond.lineTo(9, y);
            diamond.lineTo(5, y + 3);
            diamond.closePath();
            g2.draw(diamond);
        }
        else if (FlowType.DEFAULT.equals(choice))
        {
            ((FlowType) choice).drawDefaultMarker(g2, new Point2D.Double(0, y), new Point2D.Double(17, y), 0.5);
        }
    }

}
