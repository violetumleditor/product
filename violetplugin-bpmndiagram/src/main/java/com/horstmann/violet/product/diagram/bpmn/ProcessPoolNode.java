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

import com.horstmann.violet.product.diagram.abstracts.node.ISplittableNode;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.HtmlTextParser;
import com.horstmann.violet.product.diagram.abstracts.property.text.TextRun;
import com.horstmann.violet.product.diagram.common.ContainerNode;

/**
 * A BPMN pool: a participant of the process (a company, a department, a customer...), drawn as a large frame with its name in a
 * vertical band on the left. Each line of its "lanes" property adds a lane, a horizontal band with its own title
 * (a role, a person...). The lanes share the height of the pool equally until the user drags the line between two of them (see
 * {@link ISplittableNode}); their heights are kept as proportions, so resizing the pool keeps them.
 *
 * As a grouping area, it lies behind the other nodes, draws itself by dragging its frame and moves the nodes it holds. Like it,
 * only a generic edge links it: message flows go between the tasks and events of two pools.
 */
public class ProcessPoolNode extends ContainerNode implements ISplittableNode
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
        double[] laneHeights = getLaneHeightsOnDiagram(laneTitles.size(), b.getHeight());
        double laneY = b.getY();
        for (int i = 0; i < laneTitles.size(); i++)
        {
            double laneHeight = laneHeights[i];
            g2.setColor(getBorderColor());
            if (i > 0)
            {
                g2.draw(new Line2D.Double(b.getX() + bandWidth, laneY, b.getMaxX(), laneY));
            }
            g2.draw(new Line2D.Double(b.getX() + 2 * bandWidth, laneY, b.getX() + 2 * bandWidth, laneY + laneHeight));
            g2.setColor(getTextColor());
            DRAWING.drawVerticalText(g2, laneTitles.get(i), new Rectangle2D.Double(b.getX() + bandWidth, laneY, bandWidth,
                    laneHeight));
            laneY += laneHeight;
        }
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    @Override
    public List<Line2D> getSeparators()
    {
        List<Line2D> separators = new ArrayList<Line2D>();
        if (this.isToolPrototype)
        {
            return separators;
        }
        Rectangle2D b = getBounds();
        double bandWidth = Math.min(BAND_WIDTH, b.getWidth() / 4);
        double[] laneHeights = getLaneHeightsOnDiagram(getLaneTitles().size(), b.getHeight());
        double y = b.getY();
        for (int i = 0; i < laneHeights.length - 1; i++)
        {
            y += laneHeights[i];
            separators.add(new Line2D.Double(b.getX() + bandWidth, y, b.getMaxX(), y));
        }
        return separators;
    }

    @Override
    public void moveSeparator(int index, Point2D location)
    {
        double y = location.getY();
        Rectangle2D b = getBounds();
        double[] laneHeights = getLaneHeightsOnDiagram(getLaneTitles().size(), b.getHeight());
        if (index < 0 || index >= laneHeights.length - 1)
        {
            return;
        }
        double laneTop = b.getY();
        for (int i = 0; i < index; i++)
        {
            laneTop += laneHeights[i];
        }
        double bothHeights = laneHeights[index] + laneHeights[index + 1];
        double minHeight = Math.min(MIN_LANE_HEIGHT, bothHeights / 2);
        double upperHeight = Math.max(minHeight, Math.min(bothHeights - minHeight, y - laneTop));
        laneHeights[index] = upperHeight;
        laneHeights[index + 1] = bothHeights - upperHeight;
        StringBuilder text = new StringBuilder();
        for (double aHeight : laneHeights)
        {
            if (text.length() > 0)
            {
                text.append(LANE_HEIGHTS_SEPARATOR);
            }
            long rounded = Math.round(aHeight);
            text.append(Math.abs(aHeight - rounded) < 0.001 ? Long.toString(rounded) : Double.toString(aHeight));
        }
        this.laneHeights = text.toString();
    }

    @Override
    public String getSeparatorPositions()
    {
        return this.laneHeights;
    }

    @Override
    public void setSeparatorPositions(String separatorPositions)
    {
        this.laneHeights = separatorPositions;
    }

    /**
     * Shares the height of the pool between its lanes, in proportion to the saved heights. A lane without a saved height (added
     * since) gets the mean of the others, all lanes the same height when none was saved.
     *
     * @param laneCount the number of lanes
     * @param poolHeight the height of the pool
     * @return the height of each lane on the diagram, top to bottom
     */
    private double[] getLaneHeightsOnDiagram(int laneCount, double poolHeight)
    {
        double[] heights = new double[laneCount];
        String[] savedHeights = this.laneHeights == null ? new String[0] : this.laneHeights.split(LANE_HEIGHTS_SEPARATOR);
        double savedSum = 0;
        int savedCount = 0;
        for (int i = 0; i < laneCount; i++)
        {
            heights[i] = i < savedHeights.length ? parseHeight(savedHeights[i]) : 0;
            if (heights[i] > 0)
            {
                savedSum += heights[i];
                savedCount++;
            }
        }
        double defaultHeight = savedCount > 0 ? savedSum / savedCount : 1;
        double sum = 0;
        for (int i = 0; i < laneCount; i++)
        {
            if (heights[i] <= 0)
            {
                heights[i] = defaultHeight;
            }
            sum += heights[i];
        }
        for (int i = 0; i < laneCount; i++)
        {
            heights[i] = heights[i] * poolHeight / sum;
        }
        return heights;
    }

    /**
     * @return the height written in the file, 0 if it isn't a positive number
     */
    private double parseHeight(String text)
    {
        try
        {
            double height = Double.parseDouble(text.trim());
            return height > 0 && !Double.isInfinite(height) ? height : 0;
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
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

    /** The heights of the lanes, top to bottom, as proportions separated by ";"; null while all lanes have the same height */
    private String laneHeights;

    private transient boolean isToolPrototype;

    private transient List<MultiLineString> laneTitles;

    private transient String laneTitlesSource;

    private static final ProcessDrawing DRAWING = new ProcessDrawing();

    private static final int DEFAULT_WIDTH = 600;
    private static final int DEFAULT_HEIGHT = 250;
    private static final int BAND_WIDTH = 30;
    private static final int MIN_LANE_HEIGHT = 30;
    private static final String LANE_HEIGHTS_SEPARATOR = ";";
    private static final int TOOL_ICON_WIDTH = 64;
    private static final int TOOL_ICON_HEIGHT = 44;

}
