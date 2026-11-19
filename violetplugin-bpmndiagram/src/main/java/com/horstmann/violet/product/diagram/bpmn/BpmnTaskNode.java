package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 * A BPMN task: a unit of work, whose kind (user, service, manual...) is shown in its top left corner.
 */
public class BpmnTaskNode extends BpmnActivityNode
{

    @Override
    protected void drawMarkers(Graphics2D g2, Rectangle2D bounds, float lineWidth)
    {
        Rectangle2D markerArea = new Rectangle2D.Double(bounds.getX() + MARKER_MARGIN, bounds.getY() + MARKER_MARGIN, MARKER_SIZE,
                MARKER_SIZE);
        getTaskType().drawMarker(g2, markerArea, getBackgroundColor(), lineWidth);
    }

    /**
     * Sets the taskType property value.
     *
     * @param newValue the kind of task
     */
    public void setTaskType(TaskType newValue)
    {
        this.taskType = newValue;
    }

    /**
     * Gets the taskType property value.
     *
     * @return the kind of task, standard by default
     */
    public TaskType getTaskType()
    {
        if (this.taskType == null)
        {
            this.taskType = TaskType.NONE;
        }
        return this.taskType;
    }

    @Override
    public BpmnTaskNode clone()
    {
        return (BpmnTaskNode) super.clone();
    }

    private TaskType taskType;

    private static final int MARKER_MARGIN = 6;
    private static final int MARKER_SIZE = 16;

}
