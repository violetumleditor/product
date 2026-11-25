package com.horstmann.violet.product.diagram.abstracts.edge;

import java.util.ArrayList;
import java.util.List;

/**
 * The path of a segmented edge whose segments were dragged by the user, as it is kept and saved: horizontal and vertical segments
 * in turn, each known by its fixed coordinate (y of a horizontal segment, x of a vertical one). The first coordinate is relative
 * to the location of the start node, the last one to the location of the end node (the first one if there is only one), so that
 * the ends follow their nodes; the others are on the graph, so that the segments between stay in place.
 *
 * @param isFirstHorizontal true if the first segment is horizontal
 * @param coordinates one coordinate per segment
 */
public record MovedSegments(boolean isFirstHorizontal, List<Double> coordinates)
{

    /**
     * @param text as written by {@link #toText()}
     * @return the segments, or null if the text can't be read
     */
    public static MovedSegments fromText(String text)
    {
        if (text == null || text.length() < 3)
        {
            return null;
        }
        List<Double> coordinates = new ArrayList<Double>();
        try
        {
            for (String aCoordinate : text.substring(2).split(";"))
            {
                coordinates.add(Double.valueOf(aCoordinate));
            }
        }
        catch (NumberFormatException e)
        {
            return null;
        }
        return coordinates.isEmpty() ? null : new MovedSegments(text.startsWith(HORIZONTAL), coordinates);
    }

    /**
     * @return the text saved in the file: H or V (first segment), then the coordinates, such as "H:30.0;250.0;10.0"
     */
    public String toText()
    {
        StringBuilder text = new StringBuilder(this.isFirstHorizontal ? HORIZONTAL : VERTICAL).append(':');
        for (int i = 0; i < this.coordinates.size(); i++)
        {
            text.append(i > 0 ? ";" : "").append(this.coordinates.get(i));
        }
        return text.toString();
    }

    private static final String HORIZONTAL = "H";

    private static final String VERTICAL = "V";

}
