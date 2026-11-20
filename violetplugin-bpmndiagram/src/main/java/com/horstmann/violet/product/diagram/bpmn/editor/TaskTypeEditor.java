package com.horstmann.violet.product.diagram.bpmn.editor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import com.horstmann.violet.product.diagram.bpmn.type.TaskType;

/**
 * Edits the kind of a task
 */
public class TaskTypeEditor extends ProcessChoiceEditor
{

    public TaskTypeEditor()
    {
        super(TaskType.values(), "tasktype");
    }

    @Override
    protected void paintChoice(Graphics2D g2, Enum<?> choice)
    {
        if (TaskType.NONE.equals(choice))
        {
            g2.draw(new RoundRectangle2D.Double(4, 1, 16, 12, 5, 5));
            return;
        }
        ((TaskType) choice).drawMarker(g2, new Rectangle2D.Double(5, 0, 14, 14), TRANSPARENT, 1);
    }

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

}
