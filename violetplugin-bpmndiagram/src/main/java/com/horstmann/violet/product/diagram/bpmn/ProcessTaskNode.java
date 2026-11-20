package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.bpmn.type.TaskType;

/**
 * A BPMN task: a unit of work, a rounded rectangle with its name inside, which grows with the name. Its kind (user, service,
 * manual...) is shown in its top left corner.
 */
public class ProcessTaskNode extends RectangularNode
{

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D nameBounds = getName().getBounds();
        Point2D location = getLocation();
        double width = Math.max(nameBounds.getWidth() + 2 * HORIZONTAL_PADDING, DEFAULT_WIDTH);
        double height = Math.max(nameBounds.getHeight() + 2 * VERTICAL_PADDING, DEFAULT_HEIGHT);
        Rectangle2D bounds = new Rectangle2D.Double(location.getX(), location.getY(), width, height);
        return getGraph().getGridSticker().snap(bounds);
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D bounds = getBounds();
        return new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ARC_SIZE, ARC_SIZE);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);
        Color oldColor = g2.getColor();
        Shape shape = getShape();
        g2.setColor(getBackgroundColor());
        g2.fill(shape);
        g2.setColor(getBorderColor());
        g2.draw(shape);
        Rectangle2D bounds = getBounds();
        g2.setColor(getTextColor());
        getName().draw(g2, bounds);
        float lineWidth = DRAWING.getLineWidth(g2);
        if (this.isToolPrototype)
        {
            // Marker enlarged around the corner of the node, else it vanishes in the small tool icon
            AffineTransform oldTransform = g2.getTransform();
            g2.translate(bounds.getX(), bounds.getY());
            g2.scale(TOOL_ICON_MARKER_ZOOM, TOOL_ICON_MARKER_ZOOM);
            drawMarker(g2, new Rectangle2D.Double(0, 0, bounds.getWidth() / TOOL_ICON_MARKER_ZOOM, bounds.getHeight()
                    / TOOL_ICON_MARKER_ZOOM), (float) (lineWidth / TOOL_ICON_MARKER_ZOOM));
            g2.setTransform(oldTransform);
        }
        else
        {
            drawMarker(g2, bounds, lineWidth);
        }
        g2.setColor(oldColor);
    }

    /**
     * Draws the marker of the kind of task in the top left corner
     *
     * @param g2 graphics, its color set to the text color
     * @param bounds the bounds of the task
     * @param lineWidth the width of the outline
     */
    private void drawMarker(Graphics2D g2, Rectangle2D bounds, float lineWidth)
    {
        Rectangle2D markerArea = new Rectangle2D.Double(bounds.getX() + MARKER_MARGIN, bounds.getY() + MARKER_MARGIN, MARKER_SIZE,
                MARKER_SIZE);
        getTaskType().drawMarker(g2, markerArea, getBackgroundColor(), lineWidth);
    }

    /**
     * Turns this node into a tool bar prototype, whose marker is drawn larger
     */
    public void enableToolIcon()
    {
        this.isToolPrototype = true;
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the task
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the task
     */
    public MultiLineString getName()
    {
        if (this.name == null)
        {
            this.name = new MultiLineString();
        }
        return this.name;
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
    public ProcessTaskNode clone()
    {
        ProcessTaskNode cloned = (ProcessTaskNode) super.clone();
        cloned.name = getName().clone();
        cloned.isToolPrototype = false;
        return cloned;
    }

    private MultiLineString name;

    private TaskType taskType;

    private transient boolean isToolPrototype;

    private static final double TOOL_ICON_MARKER_ZOOM = 2.2;

    private static final ProcessDrawing DRAWING = new ProcessDrawing();

    private static final int ARC_SIZE = 20;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 80;
    private static final int HORIZONTAL_PADDING = 22;
    private static final int VERTICAL_PADDING = 10;
    private static final int MARKER_MARGIN = 6;
    private static final int MARKER_SIZE = 16;

}
