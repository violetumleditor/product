package com.horstmann.violet.product.diagram.bpmn;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.HtmlTextParser;
import com.horstmann.violet.product.diagram.abstracts.property.text.TextRun;
import com.horstmann.violet.product.diagram.common.ContainerNode;

/**
 * A BPMN pool: a participant of the process (a company, a department, a customer...), drawn as a large frame with its name in a
 * vertical band on the left. Each line of its "lanes" property adds a lane, a horizontal band of equal height with its own title
 * (a role, a person...).
 *
 * As a grouping area, it lies behind the other nodes, draws itself by dragging its frame and moves the nodes it holds. Like it,
 * only a generic edge links it: message flows go between the tasks and events of two pools.
 */
public class ProcessPoolNode extends ContainerNode
{

    public ProcessPoolNode()
    {
        super();
        setLineStyle(LineStyle.SOLID);
    }

    /**
     * Turns this node into a tool bar prototype: a smaller pool, whose band stays visible in the icon
     */
    @Override
    public void enableToolIcon()
    {
        this.isToolPrototype = true;
    }

    @Override
    public Rectangle2D getBounds()
    {
        Point2D location = getLocation();
        if (this.isToolPrototype)
        {
            return new Rectangle2D.Double(location.getX(), location.getY(), TOOL_ICON_WIDTH, TOOL_ICON_HEIGHT);
        }
        double width = DEFAULT_WIDTH;
        double height = DEFAULT_HEIGHT;
        Rectangle2D preferredSize = getPreferredSize();
        if (preferredSize != null && preferredSize.getWidth() > 0 && preferredSize.getHeight() > 0)
        {
            width = preferredSize.getWidth();
            height = preferredSize.getHeight();
        }
        return new Rectangle2D.Double(location.getX(), location.getY(), width, height);
    }

    @Override
    public void draw(Graphics2D g2)
    {
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        if (!this.isToolPrototype)
        {
            g2.setStroke(getLineStyle().getStroke(getBorderWidth()));
        }
        Rectangle2D b = getBounds();
        Shape shape = getShape();
        g2.setColor(getBackgroundColor());
        g2.fill(shape);
        g2.setColor(getBorderColor());
        g2.draw(shape);
        double bandWidth = Math.min(BAND_WIDTH, b.getWidth() / 4);
        g2.draw(new Line2D.Double(b.getX() + bandWidth, b.getY(), b.getX() + bandWidth, b.getMaxY()));
        if (this.isToolPrototype)
        {
            // Two lanes, so that the icon tells a pool from a grouping area
            g2.draw(new Line2D.Double(b.getX() + bandWidth, b.getCenterY(), b.getMaxX(), b.getCenterY()));
            g2.draw(new Line2D.Double(b.getX() + 2 * bandWidth, b.getY(), b.getX() + 2 * bandWidth, b.getMaxY()));
        }
        g2.setColor(getTextColor());
        DRAWING.drawVerticalText(g2, getName(), new Rectangle2D.Double(b.getX(), b.getY(), bandWidth, b.getHeight()));
        List<MultiLineString> laneTitles = getLaneTitles();
        double laneHeight = b.getHeight() / Math.max(1, laneTitles.size());
        for (int i = 0; i < laneTitles.size(); i++)
        {
            double laneY = b.getY() + i * laneHeight;
            g2.setColor(getBorderColor());
            if (i > 0)
            {
                g2.draw(new Line2D.Double(b.getX() + bandWidth, laneY, b.getMaxX(), laneY));
            }
            g2.draw(new Line2D.Double(b.getX() + 2 * bandWidth, laneY, b.getX() + 2 * bandWidth, laneY + laneHeight));
            g2.setColor(getTextColor());
            DRAWING.drawVerticalText(g2, laneTitles.get(i), new Rectangle2D.Double(b.getX() + bandWidth, laneY, bandWidth,
                    laneHeight));
        }
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * @return one title per non-empty line of the lanes property, rebuilt only when it changed
     */
    private List<MultiLineString> getLaneTitles()
    {
        String lanesText = getLanes().getText();
        if (this.laneTitles == null || !lanesText.equals(this.laneTitlesSource))
        {
            this.laneTitles = new ArrayList<MultiLineString>();
            this.laneTitlesSource = lanesText;
            for (List<TextRun> aLine : new HtmlTextParser().parse(lanesText))
            {
                StringBuilder lineText = new StringBuilder();
                for (TextRun aRun : aLine)
                {
                    lineText.append(aRun.text());
                }
                String title = lineText.toString().trim();
                if (!title.isEmpty())
                {
                    MultiLineString laneTitle = new MultiLineString();
                    laneTitle.setText(title);
                    this.laneTitles.add(laneTitle);
                }
            }
        }
        return this.laneTitles;
    }

    /**
     * Sets the name property value.
     *
     * @param newValue the name of the participant
     */
    public void setName(MultiLineString newValue)
    {
        this.name = newValue;
    }

    /**
     * Gets the name property value.
     *
     * @return the name of the participant, drawn vertically on the left
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
     * Sets the lanes property value.
     *
     * @param newValue the titles of the lanes, one per line
     */
    public void setLanes(MultiLineString newValue)
    {
        this.lanes = newValue;
    }

    /**
     * Gets the lanes property value.
     *
     * @return the titles of the lanes, one per line; none by default
     */
    public MultiLineString getLanes()
    {
        if (this.lanes == null)
        {
            this.lanes = new MultiLineString();
        }
        return this.lanes;
    }

    @Override
    public ProcessPoolNode clone()
    {
        ProcessPoolNode cloned = (ProcessPoolNode) super.clone();
        cloned.isToolPrototype = false;
        cloned.name = getName().clone();
        cloned.lanes = getLanes().clone();
        cloned.laneTitles = null;
        cloned.laneTitlesSource = null;
        return cloned;
    }

    private MultiLineString name;

    private MultiLineString lanes;

    private transient boolean isToolPrototype;

    private transient List<MultiLineString> laneTitles;

    private transient String laneTitlesSource;

    private static final ProcessDrawing DRAWING = new ProcessDrawing();

    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 250;
    private static final int BAND_WIDTH = 30;
    private static final int TOOL_ICON_WIDTH = 64;
    private static final int TOOL_ICON_HEIGHT = 44;

}
