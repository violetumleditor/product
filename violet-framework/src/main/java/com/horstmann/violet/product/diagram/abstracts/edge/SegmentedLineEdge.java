/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.IMovableLabelElement;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.node.RectangularNode;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * An edge that is composed of multiple line segments
 */
public abstract class SegmentedLineEdge extends ShapeEdge implements IColorable, IMovableLabelElement
{
    /**
     * Constructs an edge with no adornments.
     */
    public SegmentedLineEdge()
    {
        startLabel = "";
        middleLabel = "";
        endLabel = "";
    }
    
    @Override
    public boolean isTransitionPointsSupported()
    {
        return true;
    }
    
    @Override
    public void setTransitionPoints(ITransitionPoint[] transitionPoints) {
    	super.setTransitionPoints(transitionPoints);
    	if (transitionPoints.length > 0) {
    		setBentStyle(BentStyle.FREE);
    	}
    }
    
    
    @Override
    public ITransitionPoint[] getTransitionPoints() {
    	if (!BentStyle.FREE.equals(getBentStyle())) {
    		return new ITransitionPoint[] {};
    	}
    	return super.getTransitionPoints();
    }

    @Override
    public boolean contains(Point2D aPoint)
    {
        // When angle style is rounded, the Bézier path skirts around corner
        // points so the default shape-based hit test misses transition points.
        if (super.contains(aPoint)) return true;
        if (findTransitionPoint(aPoint) != null) return true;
        return isOnLabel(aPoint);
    }

    @Override
    public void setHiddenText(String textPropertyName)
    {
        this.hiddenText = textPropertyName;
    }

    /**
     * A text of the edge belongs to it: a click on it selects the edge, a double click edits it in place
     * (EditTextInPlaceBehavior), and it is never an empty spot of the diagram (where a tap leaves a "+" to add a node). Not inside
     * the nodes the edge links, which keep their clicks.
     *
     * @param aPoint a point on the graph
     * @return true if the point is on the start, middle or end text of this edge
     */
    public boolean isOnLabel(Point2D aPoint)
    {
        return findLabel(aPoint) != null;
    }

    /**
     * Sets the bentStyle property
     * 
     * @param newValue the bent style
     */
    public void setBentStyle(BentStyle newValue)
    {
        this.bentStyle = newValue;
    }

    /**
     * Gets the bentStyle property
     * 
     * @return the bent style
     */
    public BentStyle getBentStyle()
    {
        if (this.bentStyle == null)
        {
            this.bentStyle = BentStyle.AUTO;
        }
        return bentStyle;
    }

    /**
     * Sets the angle style property.
     * 
     * @param newValue the new value
     */
    public void setAngleStyle(AngleStyle newValue)
    {
        this.angleStyle = newValue;
    }

    /**
     * Gets the angle style property.
     * 
     * @return the angle style
     */
    public AngleStyle getAngleStyle()
    {
        if (this.angleStyle == null)
        {
            this.angleStyle = AngleStyle.RAW;
        }
        return this.angleStyle;
    }

    /**
     * Sets the line style property.
     * 
     * @param newValue the new value
     */
    public void setLineStyle(LineStyle newValue)
    {
        this.lineStyle = newValue;
    }

    /**
     * Gets the line style property.
     * 
     * @return the line style
     */
    public LineStyle getLineStyle()
    {
        if (this.lineStyle == null)
        {
            this.lineStyle = LineStyle.SOLID;
        }
        return this.lineStyle;
    }

    /**
     * Sets the start arrow head property
     * 
     * @param newValue the new value
     */
    public void setStartArrowHead(ArrowHead newValue)
    {
        this.startArrowHead = newValue;
    }

    /**
     * Gets the start arrow head property
     * 
     * @return the start arrow head style
     */
    public ArrowHead getStartArrowHead()
    {
        if (this.startArrowHead == null)
        {
            this.startArrowHead = ArrowHead.NONE;
        }
        return this.startArrowHead;
    }

    /**
     * Sets the end arrow head property
     * 
     * @param newValue the new value
     */
    public void setEndArrowHead(ArrowHead newValue)
    {
        this.endArrowHead = newValue;
    }

    /**
     * Gets the end arrow head property
     * 
     * @return the end arrow head style
     */
    public ArrowHead getEndArrowHead()
    {
        if (this.endArrowHead == null)
        {
            this.endArrowHead = ArrowHead.NONE;
        }
        return this.endArrowHead;
    }

    /**
     * Sets the start label property
     * 
     * @param newValue the new value
     */
    public void setStartLabel(String newValue)
    {
        startLabel = newValue;
    }

    /**
     * Gets the start label property
     * 
     * @return the label at the start of the edge
     */
    public String getStartLabel()
    {
        return startLabel;
    }

    /**
     * Sets the middle label property
     * 
     * @param newValue the new value
     */
    public void setMiddleLabel(String newValue)
    {
        middleLabel = newValue;
    }

    /**
     * Gets the middle label property
     * 
     * @return the label at the middle of the edge
     */
    public String getMiddleLabel()
    {
        return middleLabel;
    }

    /**
     * Sets the end label property
     * 
     * @param newValue the new value
     */
    public void setEndLabel(String newValue)
    {
        endLabel = newValue;
    }

    /**
     * Gets the end label property
     * 
     * @return the label at the end of the edge
     */
    public String getEndLabel()
    {
        return endLabel;
    }

    /**
     * Draws the edge.
     * 
     * @param g2 the graphics context
     */
    public void draw(Graphics2D g2)
    {
    	Color oldColor = g2.getColor();
    	g2.setColor(getBorderColor());
    	ArrayList<Point2D> points = getPoints();
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(getLineStyle().getStroke(getBorderWidth()));
        g2.draw(getSegmentPath());
        g2.setStroke(oldStroke);
        getStartArrowHead().draw(g2, (Point2D) points.get(1), (Point2D) points.get(0), getBorderWidth());
        getEndArrowHead().draw(g2, (Point2D) points.get(points.size() - 2), (Point2D) points.get(points.size() - 1),
                getBorderWidth());

        for (String labelName : LABEL_NAMES)
        {
            if (!labelName.equals(this.hiddenText))
            {
                drawLabel(g2, labelName);
            }
        }
        g2.setColor(oldColor);
    }

    /**
     * Draws one of the texts of the edge, where it is (see {@link #getLabelBounds(String)})
     *
     * @param g2 the graphics context
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     */
    private void drawLabel(Graphics2D g2, String labelName)
    {
        Rectangle2D b = getLabelBounds(labelName);
        if (b == null) return;
        Color oldColor = g2.getColor();
        g2.setColor(getTextColor());
        getLabelText(getLabel(labelName), getLabelJustification(labelName)).draw(g2, b);
        g2.setColor(oldColor);
    }

    /**
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return how the lines of that text are aligned: centered at the middle, on the side of the line beside an end
     */
    public int getLabelJustification(String labelName)
    {
        Rectangle2D b = getLabelBounds(labelName);
        if (b == null || MIDDLE_LABEL.equals(labelName))
        {
            return MultiLineString.Justification.CENTER;
        }
        return b.getCenterX() < getLabelAnchor(labelName).getX() ? MultiLineString.Justification.RIGHT
                : MultiLineString.Justification.LEFT;
    }

    /**
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return the text of that label
     */
    private String getLabel(String labelName)
    {
        if (START_LABEL.equals(labelName)) return startLabel;
        if (END_LABEL.equals(labelName)) return endLabel;
        return middleLabel;
    }

    /**
     * Where a text of the edge is placed automatically, beside its line: at the start, at the middle (of the middle segment), at
     * the end. It follows the edge, so a moved text (see {@link #getLabelOffset(String)}) does too.
     *
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return its bounds on the graph, or null if this text is empty
     */
    @Override
    public Rectangle2D getDefaultLabelBounds(String labelName)
    {
        String label = getLabel(labelName);
        if (label == null || label.isEmpty())
        {
            return null;
        }
        ArrayList<Point2D> points = getPoints();
        if (START_LABEL.equals(labelName))
        {
            return getStringBounds(points.get(1), points.get(0), getStartArrowHead(), label, false);
        }
        if (END_LABEL.equals(labelName))
        {
            return getStringBounds(points.get(points.size() - 2), points.get(points.size() - 1), getEndArrowHead(), label, false);
        }
        return getStringBounds(points.get(points.size() / 2 - 1), points.get(points.size() / 2), null, label, true);
    }

    /**
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return the bounds of that text on the graph, moved by the user if it was, or null if it is empty
     */
    @Override
    public Rectangle2D getLabelBounds(String labelName)
    {
        Rectangle2D bounds = getDefaultLabelBounds(labelName);
        Point2D offset = getLabelOffset(labelName);
        if (bounds == null || offset == null)
        {
            return bounds;
        }
        return new Rectangle2D.Double(bounds.getX() + offset.getX(), bounds.getY() + offset.getY(), bounds.getWidth(),
                bounds.getHeight());
    }

    /**
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return the point of the edge this text belongs to: its start, the middle of its middle segment, its end
     */
    @Override
    public Point2D getLabelAnchor(String labelName)
    {
        ArrayList<Point2D> points = getPoints();
        if (START_LABEL.equals(labelName))
        {
            return points.get(0);
        }
        if (END_LABEL.equals(labelName))
        {
            return points.get(points.size() - 1);
        }
        Point2D p = points.get(points.size() / 2 - 1);
        Point2D q = points.get(points.size() / 2);
        return new Point2D.Double((p.getX() + q.getX()) / 2, (p.getY() + q.getY()) / 2);
    }

    /**
     * A text dragged by the user keeps the same offset from its automatic place, which follows the edge when its nodes move
     *
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @return the offset, or null if this text is at its automatic place
     */
    @Override
    public Point2D.Double getLabelOffset(String labelName)
    {
        if (START_LABEL.equals(labelName)) return startLabelOffset;
        if (END_LABEL.equals(labelName)) return endLabelOffset;
        return middleLabelOffset;
    }

    /**
     * @param labelName START_LABEL, MIDDLE_LABEL or END_LABEL
     * @param offset the offset from its automatic place, or null to put it back there
     */
    @Override
    public void setLabelOffset(String labelName, Point2D.Double offset)
    {
        if (START_LABEL.equals(labelName)) startLabelOffset = offset;
        else if (END_LABEL.equals(labelName)) endLabelOffset = offset;
        else middleLabelOffset = offset;
    }

    /**
     * @param aPoint a point on the graph
     * @return the name of the text of this edge under the point (START_LABEL, MIDDLE_LABEL or END_LABEL), or null; never inside
     *         the nodes the edge links, which keep their clicks
     */
    @Override
    public String findLabel(Point2D aPoint)
    {
        for (String labelName : LABEL_NAMES)
        {
            Rectangle2D bounds = getLabelBounds(labelName);
            if (bounds != null && bounds.contains(aPoint) && !isInsideLinkedNode(aPoint))
            {
                return labelName;
            }
        }
        return null;
    }

    public Point2D.Double getStartLabelOffset()
    {
        return startLabelOffset;
    }

    public void setStartLabelOffset(Point2D.Double offset)
    {
        startLabelOffset = offset;
    }

    public Point2D.Double getMiddleLabelOffset()
    {
        return middleLabelOffset;
    }

    public void setMiddleLabelOffset(Point2D.Double offset)
    {
        middleLabelOffset = offset;
    }

    public Point2D.Double getEndLabelOffset()
    {
        return endLabelOffset;
    }

    public void setEndLabelOffset(Point2D.Double offset)
    {
        endLabelOffset = offset;
    }

    /**
     * A label is a plain String, saved as it is (file format unchanged), whose lines are separated by '\n'. It is drawn as a
     * MultiLineString, like the texts of the nodes: same font and sizes on the desktop and in the browser, nothing taken from the
     * look and feel.
     *
     * @param label the text of the label
     * @param justification how its lines are aligned
     * @return the MultiLineString showing it, kept while the label doesn't change
     */
    private MultiLineString getLabelText(String label, int justification)
    {
        String key = justification + "\u0000" + label;
        MultiLineString text = getLabelTexts().get(key);
        if (text == null)
        {
            text = new MultiLineString();
            text.setJustification(justification);
            text.setText(label);
            getLabelTexts().put(key, text);
        }
        return text;
    }

    /**
     * @return the MultiLineStrings of the last labels drawn (start, middle, end, and their latest values while typing)
     */
    private Map<String, MultiLineString> getLabelTexts()
    {
        if (this.labelTexts == null)
        {
            this.labelTexts = new LinkedHashMap<String, MultiLineString>(16, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, MultiLineString> eldest)
                {
                    return size() > MAX_LABEL_TEXTS;
                }
            };
        }
        return this.labelTexts;
    }

    /**
     * A label is placed beside its line, on the side its attachment point chooses. A label of several lines grows away from the
     * line, but on a slanting segment its corner may still reach the line: then it is moved straight away from the line, on its own
     * side, until it no longer touches it.
     *
     * @param bounds the bounds of the label
     * @param p an end of the segment along which the label is drawn
     * @param q the other end
     * @return the bounds, moved if needed
     */
    private Rectangle2D moveOffSegment(Rectangle2D bounds, Point2D p, Point2D q)
    {
        final double GAP = 3;
        Rectangle2D touchingZone = new Rectangle2D.Double(bounds.getX() - GAP + 1, bounds.getY() - GAP + 1,
                bounds.getWidth() + 2 * (GAP - 1), bounds.getHeight() + 2 * (GAP - 1));
        double length = p.distance(q);
        if (length < 1e-6 || !touchingZone.intersectsLine(p.getX(), p.getY(), q.getX(), q.getY()))
        {
            return bounds;
        }
        double nx = -(q.getY() - p.getY()) / length;
        double ny = (q.getX() - p.getX()) / length;
        double side = (bounds.getCenterX() - p.getX()) * nx + (bounds.getCenterY() - p.getY()) * ny;
        if (side < 0 || (side == 0 && ny > 0))
        {
            nx = -nx;
            ny = -ny;
        }
        // Distance to add so that the corner closest to the line is GAP away from it
        double cornerX = nx > 0 ? bounds.getMinX() : bounds.getMaxX();
        double cornerY = ny > 0 ? bounds.getMinY() : bounds.getMaxY();
        double shift = GAP - ((cornerX - p.getX()) * nx + (cornerY - p.getY()) * ny);
        if (shift <= 0)
        {
            return bounds;
        }
        return new Rectangle2D.Double(bounds.getX() + nx * shift, bounds.getY() + ny * shift, bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Computes the attachment point for drawing a string.
     * 
     * @param p an endpoint of the segment along which to draw the string
     * @param q the other endpoint of the segment along which to draw the string
     * @param b the bounds of the string to draw
     * @param center true if the string should be centered along the segment
     * @return the point at which to draw the string
     */
    private Point2D getAttachmentPoint(Point2D p, Point2D q, ArrowHead arrow, Dimension d, boolean center)
    {
        final int GAP = 3;
        double xoff = GAP;
        double yoff = -GAP - d.getHeight();
        Point2D attach = q;
        if (center)
        {
            if (p.getX() > q.getX())
            {
                return getAttachmentPoint(q, p, arrow, d, center);
            }
            attach = new Point2D.Double((p.getX() + q.getX()) / 2, (p.getY() + q.getY()) / 2);
            if (p.getY() < q.getY()) yoff = -GAP - d.getHeight();
            else if (p.getY() == q.getY()) xoff = -d.getWidth() / 2;
            else yoff = GAP;
        }
        else
        {
            if (p.getX() < q.getX())
            {
                xoff = -GAP - d.getWidth();
            }
            if (p.getY() > q.getY())
            {
                yoff = GAP;
            }
            if (arrow != null)
            {
                Rectangle2D arrowBounds = arrow.getPath(p, q, getBorderWidth()).getBounds2D();
                if (p.getX() < q.getX())
                {
                    xoff -= arrowBounds.getWidth();
                }
                else
                {
                    xoff += arrowBounds.getWidth();
                }
            }
        }
        return new Point2D.Double(attach.getX() + xoff, attach.getY() + yoff);
    }

    /**
     * Computes the extent of a string that is drawn along a line segment.
     * 
     * @param g2 the graphics context
     * @param p an endpoint of the segment along which to draw the string
     * @param q the other endpoint of the segment along which to draw the string
     * @param s the string to draw
     * @param center true if the string should be centered along the segment
     * @return the rectangle enclosing the string
     */
    private Rectangle2D getStringBounds(Point2D p, Point2D q, ArrowHead arrow, String s, boolean center)
    {
        if (s == null || s.equals("")) return new Rectangle2D.Double(q.getX(), q.getY(), 0, 0);
        // The size doesn't depend on the alignment of the lines
        Rectangle2D textBounds = getLabelText(s, MultiLineString.Justification.CENTER).getBounds();
        Dimension d = new Dimension((int) Math.ceil(textBounds.getWidth()), (int) Math.ceil(textBounds.getHeight()));
        Point2D a = getAttachmentPoint(p, q, arrow, d, center);
        return moveOffSegment(new Rectangle2D.Double(a.getX(), a.getY(), d.getWidth(), d.getHeight()), p, q);
    }

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D r = super.getBounds();
        for (String labelName : LABEL_NAMES)
        {
            Rectangle2D labelBounds = getLabelBounds(labelName);
            if (labelBounds != null)
            {
                r.add(labelBounds);
            }
        }
        return r;
    }

    /**
     * @return the bounds of the text at the start of the edge, or null if it has none
     */
    public Rectangle2D getStartLabelBounds()
    {
        return getLabelBounds(START_LABEL);
    }

    /**
     * @return the bounds of the text at the end of the edge, or null if it has none
     */
    public Rectangle2D getEndLabelBounds()
    {
        return getLabelBounds(END_LABEL);
    }

    @Override
    public Rectangle2D getMiddleLabelBounds()
    {
        return getLabelBounds(MIDDLE_LABEL);
    }

    @Override
    public Shape getShape()
    {
        GeneralPath path = getSegmentPath();
        ArrayList<Point2D> points = getPoints();
        path.append(getStartArrowHead().getPath((Point2D) points.get(1), (Point2D) points.get(0), getBorderWidth()), false);
        path.append(getEndArrowHead().getPath((Point2D) points.get(points.size() - 2), (Point2D) points.get(points.size() - 1),
                getBorderWidth()), false);
        return path;
    }

    private GeneralPath getSegmentPath()
    {
        ArrayList<Point2D> points = getPoints();
        int radius = getAngleStyle().getRadius();
        float bw = getBorderWidth();

        // Shorten start and end to avoid the thick line showing behind arrows
        double startCut = getStartArrowHead().getLineCut(points.get(1), points.get(0), bw);
        double endCut = getEndArrowHead().getLineCut(points.get(points.size() - 2), points.get(points.size() - 1), bw);

        // Clone points so we can modify endpoints without affecting the originals
        ArrayList<Point2D> pts = new ArrayList<>(points);
        if (startCut > 0 && pts.size() >= 2)
        {
            Point2D p0 = pts.get(0);
            Point2D p1 = pts.get(1);
            double len = p0.distance(p1);
            if (len > 0)
            {
                double ratio = startCut / len;
                pts.set(0, new Point2D.Double(
                    p0.getX() + (p1.getX() - p0.getX()) * ratio,
                    p0.getY() + (p1.getY() - p0.getY()) * ratio));
            }
        }
        if (endCut > 0 && pts.size() >= 2)
        {
            int last = pts.size() - 1;
            Point2D pLast = pts.get(last);
            Point2D pPrev = pts.get(last - 1);
            double len = pLast.distance(pPrev);
            if (len > 0)
            {
                double ratio = endCut / len;
                pts.set(last, new Point2D.Double(
                    pLast.getX() + (pPrev.getX() - pLast.getX()) * ratio,
                    pLast.getY() + (pPrev.getY() - pLast.getY()) * ratio));
            }
        }

        GeneralPath path = new GeneralPath();

        if (radius <= 0 || pts.size() < 3)
        {
            // RAW mode or simple line: straight segments only
            Point2D p = pts.get(pts.size() - 1);
            path.moveTo((float) p.getX(), (float) p.getY());
            for (int i = pts.size() - 2; i >= 0; i--)
            {
                p = pts.get(i);
                path.lineTo((float) p.getX(), (float) p.getY());
            }
            return path;
        }

        // Rounded mode: draw arcs at each interior point
        // We iterate forward (0 → last) so the path direction matches the logical edge direction.
        Point2D first = pts.get(0);
        path.moveTo((float) first.getX(), (float) first.getY());

        for (int i = 1; i < pts.size() - 1; i++)
        {
            Point2D prev = pts.get(i - 1);
            Point2D curr = pts.get(i);
            Point2D next = pts.get(i + 1);

            // Compute the maximum radius we can use so we don't overshoot
            // either of the two segments adjacent to the corner.
            double segLenBefore = prev.distance(curr);
            double segLenAfter  = curr.distance(next);
            double r = Math.min(radius, Math.min(segLenBefore / 2.0, segLenAfter / 2.0));
            if (r < 1)
            {
                // Segments too short – fall back to sharp corner
                path.lineTo((float) curr.getX(), (float) curr.getY());
                continue;
            }

            // Points where the arc begins and ends
            double dxIn  = curr.getX() - prev.getX();
            double dyIn  = curr.getY() - prev.getY();
            double lenIn = segLenBefore;
            double arcStartX = curr.getX() - r * dxIn / lenIn;
            double arcStartY = curr.getY() - r * dyIn / lenIn;

            double dxOut  = next.getX() - curr.getX();
            double dyOut  = next.getY() - curr.getY();
            double lenOut = segLenAfter;
            double arcEndX = curr.getX() + r * dxOut / lenOut;
            double arcEndY = curr.getY() + r * dyOut / lenOut;

            // Line to the arc start, then a quadratic curve using
            // the corner point as the control point.
            path.lineTo((float) arcStartX, (float) arcStartY);
            path.quadTo((float) curr.getX(), (float) curr.getY(),
                        (float) arcEndX,     (float) arcEndY);
        }

        // Final segment to last point
        Point2D last = pts.get(pts.size() - 1);
        path.lineTo((float) last.getX(), (float) last.getY());
        return path;
    }

    /**
     * Gets the corner points of this segmented line edge
     * 
     * @return an array list of Point2D objects, containing the corner points
     */
    public ArrayList<Point2D> getPoints()
    {
        SegmentRoute adaptedRoute = isPathAdapted() ? new EndSeparator().getSeparatedRoute(this) : null;
        if (adaptedRoute != null)
        {
            ArrayList<Point2D> points = new ArrayList<Point2D>(adaptedRoute.getPoints());
            int last = points.size() - 1;
            Point2D start = getPointOnShape(getStartNode(), points.get(0), points.get(1), getStartArrowHead());
            Point2D end = getPointOnShape(getEndNode(), points.get(last), points.get(last - 1), getEndArrowHead());
            points.set(0, start);
            points.set(last, end);
            return points;
        }
        Line2D connectionPoints = getConnectionPoints();
        Point2D startingPoint = connectionPoints.getP1();
        Point2D endingPoint = connectionPoints.getP2();
        
        // Path for self loop
        if (getStartNode().equals(getEndNode())) {
        	int gapX = 20;
        	int gapY = 20;
        	Point2D p1 = new Point2D.Double(getStartNode().getBounds().getMaxX(), startingPoint.getY());
        	Point2D p5 = new Point2D.Double(endingPoint.getX(), getEndNode().getBounds().getMaxY());
        	Point2D p2 = new Point2D.Double(p1.getX() + gapX, p1.getY());
        	Point2D p4 = new Point2D.Double(p5.getX(), p5.getY() + gapY);
        	Point2D p3 = new Point2D.Double(p2.getX(), p4.getY());
        	ArrayList<Point2D> path = new ArrayList<Point2D>();
        	path.add(p1);
        	path.add(p2);
        	path.add(p3);
        	path.add(p4);
        	path.add(p5);
        	return path;
        }
        

        // Automatic based path
        if (!BentStyle.AUTO.equals(getBentStyle()))
        {
            List<Point2D> bentStylePoints = new ArrayList<Point2D>();
            bentStylePoints.add(startingPoint);
            for (ITransitionPoint aTransitionPoint : getTransitionPoints()) {
                bentStylePoints.add(aTransitionPoint.toPoint2D());
            }
            bentStylePoints.add(endingPoint);

            Point2D[] bentStylePointsAsArray = bentStylePoints.toArray(new Point2D[bentStylePoints.size()]);
            return keepOffAdaptedEdges(getBentStyle().getPath(bentStylePointsAsArray), startingPoint);
        }

        // User choice based path
        Direction startingCardinalDirection = getDirection(getStartNode()).getNearestCardinalDirection();
        Direction endingCardinalDirection = getDirection(getEndNode()).getNearestCardinalDirection();
        if ((Direction.NORTH.equals(startingCardinalDirection) || Direction.SOUTH.equals(startingCardinalDirection))
                && (Direction.NORTH.equals(endingCardinalDirection) || Direction.SOUTH.equals(endingCardinalDirection)))
        {
            return keepOffAdaptedEdges(BentStyle.VHV.getPath(startingPoint, endingPoint), startingPoint);
        }
        if ((Direction.NORTH.equals(startingCardinalDirection) || Direction.SOUTH.equals(startingCardinalDirection))
                && (Direction.EAST.equals(endingCardinalDirection) || Direction.WEST.equals(endingCardinalDirection)))
        {
            return BentStyle.VH.getPath(startingPoint, endingPoint);
        }
        if ((Direction.EAST.equals(startingCardinalDirection) || Direction.WEST.equals(startingCardinalDirection))
                && (Direction.NORTH.equals(endingCardinalDirection) || Direction.SOUTH.equals(endingCardinalDirection)))
        {
            return BentStyle.HV.getPath(startingPoint, endingPoint);
        }
        if ((Direction.EAST.equals(startingCardinalDirection) || Direction.WEST.equals(startingCardinalDirection))
                && (Direction.EAST.equals(endingCardinalDirection) || Direction.WEST.equals(endingCardinalDirection)))
        {
            return keepOffAdaptedEdges(BentStyle.HVH.getPath(startingPoint, endingPoint), startingPoint);
        }
        return BentStyle.STRAIGHT.getPath(startingPoint, endingPoint);
    }

    /**
     * The bent styles draw a single segment when both ends are nearly aligned, its start moved in front of its end. Keeps the bend
     * if the start would meet there the end of an edge adapted by the user.
     *
     * @param path the path given by the bent style
     * @param startingPoint where this edge is connected to its start node
     * @return the path to draw
     */
    private ArrayList<Point2D> keepOffAdaptedEdges(ArrayList<Point2D> path, Point2D startingPoint)
    {
        boolean isFree = BentStyle.FREE.equals(getBentStyle()) || BentStyle.STRAIGHT.equals(getBentStyle());
        if (isFree || path.size() != 2 || path.get(0).distance(startingPoint) < STRAIGHTNESS
                || !(getStartNode() instanceof RectangularNode))
        {
            return path;
        }
        Point2D start = path.get(0);
        Point2D end = path.get(1);
        Point2D location = getStartNode().getLocation();
        Point2D locationOnGraph = getStartNode().getLocationOnGraph();
        Point2D startOnNode = new Point2D.Double(start.getX() - locationOnGraph.getX() + location.getX(),
                start.getY() - locationOnGraph.getY() + location.getY());
        if (((RectangularNode) getStartNode()).keepOffAdaptedEdges(this, startOnNode, new ArrayList<Point2D>()).equals(startOnNode))
        {
            return path;
        }
        ArrayList<Point2D> bentPath = new ArrayList<Point2D>();
        bentPath.add(startingPoint);
        if (Math.abs(start.getY() - end.getY()) < STRAIGHTNESS)
        {
            double x = (startingPoint.getX() + end.getX()) / 2;
            bentPath.add(new Point2D.Double(x, startingPoint.getY()));
            bentPath.add(new Point2D.Double(x, end.getY()));
        }
        else
        {
            double y = (startingPoint.getY() + end.getY()) / 2;
            bentPath.add(new Point2D.Double(startingPoint.getX(), y));
            bentPath.add(new Point2D.Double(end.getX(), y));
        }
        bentPath.add(end);
        return bentPath;
    }

    /**
     * @return true if the segments of this edge can be dragged: an automatic or bent path, made of horizontal and vertical segments,
     *         between two nodes apart from each other
     */
    public boolean isSegmentsMovable()
    {
        return getRoute() != null;
    }

    /**
     * @return true if the path drawn is the one the user adapted by dragging segments
     */
    public boolean isPathAdapted()
    {
        return getAdaptedRoute() != null;
    }

    /**
     * @return the path, adapted by the user or automatic, or null if its segments can't be dragged
     */
    public SegmentRoute getRoute()
    {
        SegmentRoute adaptedRoute = isPathAdapted() ? new EndSeparator().getSeparatedRoute(this) : null;
        return adaptedRoute != null ? adaptedRoute : getAutomaticRoute();
    }

    /**
     * @return the path without the user's adaptation, or null if its segments can't be dragged
     */
    public SegmentRoute getAutomaticRoute()
    {
        if (!isAdaptationSupported())
        {
            return null;
        }
        String formerMovedSegments = this.movedSegments;
        List<Point2D> points;
        try
        {
            this.movedSegments = null;
            points = getPoints();
        }
        finally
        {
            this.movedSegments = formerMovedSegments;
        }
        List<Double> coordinates = new ArrayList<Double>();
        boolean isFirstHorizontal = false;
        boolean isPreviousHorizontal = false;
        for (int i = 1; i < points.size(); i++)
        {
            double dx = Math.abs(points.get(i).getX() - points.get(i - 1).getX());
            double dy = Math.abs(points.get(i).getY() - points.get(i - 1).getY());
            if (dx < STRAIGHTNESS && dy < STRAIGHTNESS)
            {
                continue;
            }
            if (dx >= STRAIGHTNESS && dy >= STRAIGHTNESS)
            {
                return null;
            }
            boolean isHorizontal = dy < STRAIGHTNESS;
            if (!coordinates.isEmpty() && isHorizontal == isPreviousHorizontal)
            {
                continue;
            }
            if (coordinates.isEmpty())
            {
                isFirstHorizontal = isHorizontal;
            }
            coordinates.add(isHorizontal ? points.get(i).getY() : points.get(i).getX());
            isPreviousHorizontal = isHorizontal;
        }
        if (coordinates.isEmpty())
        {
            return null;
        }
        return new SegmentRoute(getBoundsOnGraph(getStartNode()), getBoundsOnGraph(getEndNode()), isFirstHorizontal, coordinates,
                getMinEndLength(getStartNode(), getStartArrowHead()), getMinEndLength(getEndNode(), getEndArrowHead()));
    }

    /**
     * Adapts the path; back to the automatic one if it is the same
     *
     * @param route the new path
     */
    public void setRoute(SegmentRoute route)
    {
        if (route.isSamePath(getAutomaticRoute()))
        {
            setMovedSegments(null);
            return;
        }
        List<Double> coordinates = route.getCoordinates();
        int last = coordinates.size() - 1;
        if (last > 0)
        {
            coordinates.set(last, coordinates.get(last) - getOrigin(route.getEndBounds(), route.isHorizontal(last)));
        }
        coordinates.set(0, coordinates.get(0) - getOrigin(route.getStartBounds(), route.isHorizontal(0)));
        setMovedSegments(new MovedSegments(route.isHorizontal(0), coordinates));
    }

    /**
     * @return what the user adapted, or null for an automatic path
     */
    public MovedSegments getMovedSegments()
    {
        return MovedSegments.fromText(this.movedSegments);
    }

    public void setMovedSegments(MovedSegments newValue)
    {
        this.movedSegments = newValue == null ? null : newValue.toText();
    }

    /**
     * The segments between the ends stay in place when a node moves, but follow when both nodes move together
     */
    public void translateMovedSegments(double dx, double dy)
    {
        MovedSegments segments = getMovedSegments();
        if (segments == null)
        {
            return;
        }
        List<Double> coordinates = new ArrayList<Double>(segments.coordinates());
        for (int i = 1; i < coordinates.size() - 1; i++)
        {
            boolean isHorizontal = (i % 2 == 0) == segments.isFirstHorizontal();
            coordinates.set(i, coordinates.get(i) + (isHorizontal ? dy : dx));
        }
        setMovedSegments(new MovedSegments(segments.isFirstHorizontal(), coordinates));
    }

    /**
     * Never depends on other edges, so that the routing of an edge can ask it of another one
     *
     * @return the path as the user left it (EndSeparator then keeps its ends off those of other edges), or null if there is none or if it can't be drawn where the nodes are now
     */
    SegmentRoute getAdaptedRoute()
    {
        if (this.movedSegments == null || !isAdaptationSupported())
        {
            return null;
        }
        MovedSegments segments = getMovedSegments();
        if (segments == null)
        {
            return null;
        }
        Rectangle2D startBounds = getBoundsOnGraph(getStartNode());
        Rectangle2D endBounds = getBoundsOnGraph(getEndNode());
        List<Double> coordinates = new ArrayList<Double>(segments.coordinates());
        int last = coordinates.size() - 1;
        if (last > 0)
        {
            boolean isLastHorizontal = (last % 2 == 0) == segments.isFirstHorizontal();
            coordinates.set(last, coordinates.get(last) + getOrigin(endBounds, isLastHorizontal));
        }
        coordinates.set(0, coordinates.get(0) + getOrigin(startBounds, segments.isFirstHorizontal()));
        SegmentRoute route = new SegmentRoute(startBounds, endBounds, segments.isFirstHorizontal(), coordinates,
                getMinEndLength(getStartNode(), getStartArrowHead()), getMinEndLength(getEndNode(), getEndArrowHead()));
        if (!route.keepSingleSegmentInFrontOfNodes())
        {
            return null;
        }
        route.adjust();
        return route;
    }

    private boolean isAdaptationSupported()
    {
        INode start = getStartNode();
        INode end = getEndNode();
        if (!isTransitionPointsSupported() || start == null || end == null || start.equals(end))
        {
            return false;
        }
        if (BentStyle.FREE.equals(getBentStyle()) || BentStyle.STRAIGHT.equals(getBentStyle()))
        {
            return false;
        }
        return !getBoundsOnGraph(start).intersects(getBoundsOnGraph(end));
    }

    /**
     * @return the length an end segment needs for its arrow head
     */
    private double getMinEndLength(INode node, ArrowHead arrowHead)
    {
        double nodeBorder = node instanceof AbstractNode ? ((AbstractNode) node).getBorderWidth() / 2.0 : 0;
        double headLength = arrowHead == null ? 0 : arrowHead.getTotalLength(getBorderWidth());
        return headLength == 0 ? 0 : headLength + nodeBorder + LINE_BEFORE_HEAD;
    }

    private double getOrigin(Rectangle2D bounds, boolean isHorizontal)
    {
        return isHorizontal ? bounds.getY() : bounds.getX();
    }

    private Rectangle2D getBoundsOnGraph(INode node)
    {
        Point2D location = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        return new Rectangle2D.Double(location.getX(), location.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * An end of an adapted path lies on the bounds of its node: goes on to the shape of the node (ellipse, diamond...), and stops
     * before its border when it is thick, as RectangularNode.getConnectionPoint does
     *
     * @param node the node at this end
     * @param end the end of the path, on the bounds of the node
     * @param next the next point of the path
     * @param arrowHead the arrow head at this end
     * @return the end to draw
     */
    private Point2D getPointOnShape(INode node, Point2D end, Point2D next, ArrowHead arrowHead)
    {
        if (!(node instanceof AbstractNode))
        {
            return end;
        }
        AbstractNode abstractNode = (AbstractNode) node;
        Rectangle2D boundsOnGraph = getBoundsOnGraph(node);
        Rectangle2D bounds = node.getBounds();
        double outX = Math.abs(next.getX() - end.getX()) < STRAIGHTNESS ? 0 : Math.signum(next.getX() - end.getX());
        double outY = Math.abs(next.getY() - end.getY()) < STRAIGHTNESS ? 0 : Math.signum(next.getY() - end.getY());
        double depth = Math.abs(outX) * boundsOnGraph.getWidth() + Math.abs(outY) * boundsOnGraph.getHeight();
        AffineTransform toGraph = AffineTransform.getTranslateInstance(boundsOnGraph.getX() - bounds.getX(),
                boundsOnGraph.getY() - bounds.getY());
        // Starts just outside, not to miss a line on the bounds themselves
        Line2D ray = new Line2D.Double(end.getX() + outX, end.getY() + outY, end.getX() - outX * depth, end.getY() - outY * depth);
        double distance = getDistanceToShape(abstractNode.getShape().getPathIterator(toGraph, 0.5), ray) - 1;
        double edgeOffset = arrowHead != null && arrowHead != ArrowHead.NONE ? 0 : getBorderWidth() / 2.0;
        double offset = abstractNode.getBorderWidth() / 2.0 + edgeOffset;
        if (offset > 0.5)
        {
            distance -= offset;
        }
        return new Point2D.Double(end.getX() - outX * distance, end.getY() - outY * distance);
    }

    /**
     * @return the distance from the start of the ray to the first line of the flattened shape it meets, 1 (the bounds) if it meets
     *         none
     */
    private double getDistanceToShape(PathIterator iterator, Line2D ray)
    {
        double closest = Double.MAX_VALUE;
        double[] coords = new double[6];
        Point2D first = null;
        Point2D previous = null;
        for (; !iterator.isDone(); iterator.next())
        {
            int type = iterator.currentSegment(coords);
            Point2D point = type == PathIterator.SEG_CLOSE ? first : new Point2D.Double(coords[0], coords[1]);
            if (type == PathIterator.SEG_MOVETO)
            {
                first = point;
            }
            else if (previous != null && point != null)
            {
                closest = Math.min(closest, getDistanceToLine(previous, point, ray));
            }
            previous = point;
        }
        return closest == Double.MAX_VALUE ? 1 : closest;
    }

    private double getDistanceToLine(Point2D p, Point2D q, Line2D ray)
    {
        double rx = ray.getX2() - ray.getX1();
        double ry = ray.getY2() - ray.getY1();
        double sx = q.getX() - p.getX();
        double sy = q.getY() - p.getY();
        double cross = rx * sy - ry * sx;
        if (Math.abs(cross) < 1e-9)
        {
            return Double.MAX_VALUE;
        }
        double px = p.getX() - ray.getX1();
        double py = p.getY() - ray.getY1();
        double t = (px * sy - py * sx) / cross;
        double u = (px * ry - py * rx) / cross;
        if (t < 0 || t > 1 || u < 0 || u > 1)
        {
            return Double.MAX_VALUE;
        }
        return t * Math.hypot(rx, ry);
    }

    @Override
    public Line2D getConnectionPoints()
    {
        if (isPathAdapted())
        {
            List<Point2D> points = getPoints();
            return new Line2D.Double(points.get(0), points.get(points.size() - 1));
        }
        return super.getConnectionPoints();
    }

    @Override
    public Direction getDirection(INode node)
    {
        SegmentRoute adaptedRoute = getAdaptedRoute();
        if (adaptedRoute != null && node.equals(getStartNode()))
        {
            return adaptedRoute.getStartDirection();
        }
        if (adaptedRoute != null && node.equals(getEndNode()))
        {
            return adaptedRoute.getEndDirection();
        }
        Direction straightDirection = super.getDirection(node);
        double x = straightDirection.getX();
        double y = straightDirection.getY();
        if (!getStartNode().equals(getEndNode()) && node.equals(getStartNode()))
        {
            if (BentStyle.HV.equals(getBentStyle()) || BentStyle.HVH.equals(getBentStyle()))
            {
                return (x >= 0) ? Direction.EAST : Direction.WEST;
            }
            if (BentStyle.VH.equals(getBentStyle()) || BentStyle.VHV.equals(getBentStyle()))
            {
                return (y >= 0) ? Direction.SOUTH : Direction.NORTH;
            }
        }
        if (!getStartNode().equals(getEndNode()) && node.equals(getEndNode()))
        {
            if (BentStyle.HV.equals(getBentStyle()) || BentStyle.VHV.equals(getBentStyle()))
            {
                return (y >= 0) ? Direction.SOUTH : Direction.NORTH;
            }
            if (BentStyle.VH.equals(getBentStyle()) || BentStyle.HVH.equals(getBentStyle()))
            {
                return (x >= 0) ? Direction.EAST : Direction.WEST;
            }
        }
        return straightDirection;
    }

    private LineStyle lineStyle;
    private AngleStyle angleStyle;
    private ArrowHead startArrowHead;
    private ArrowHead endArrowHead;
    private BentStyle bentStyle;
    private String startLabel;
    private String middleLabel;
    private String endLabel;

    /** Offsets of the texts dragged by the user from their automatic place; null (not saved) for a text left there */
    private Point2D.Double startLabelOffset;
    private Point2D.Double middleLabelOffset;
    private Point2D.Double endLabelOffset;

    /** Names of the texts of the edge, as their String properties */
    public static final String START_LABEL = "startLabel";
    public static final String MIDDLE_LABEL = "middleLabel";
    public static final String END_LABEL = "endLabel";
    private static final String[] LABEL_NAMES = { START_LABEL, MIDDLE_LABEL, END_LABEL };

    /** The path adapted by the user, as MovedSegments writes it, or null (not saved then) */
    private String movedSegments;

    /** Name of the property of the path adapted by the user */
    public static final String MOVED_SEGMENTS = "movedSegments";

    /** Distance under which two points are on the same horizontal or vertical line */
    private static final double STRAIGHTNESS = 0.5;

    /** Length of the line shown between its last corner and its arrow head */
    private static final double LINE_BEFORE_HEAD = 4;

    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;

    @Override
    public void setBackgroundColor(Color bgColor)
    {
        this.backgroundColor = bgColor;
    }

    @Override
    public Color getBackgroundColor()
    {
        if (backgroundColor == null)
        {
            return ColorChoice.DEFAULT.getBackgroundColor();
        }
        return backgroundColor;
    }

    @Override
    public void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
    }

    @Override
    public Color getBorderColor()
    {
        if (borderColor == null)
        {
            return Color.BLACK;
        }
        return borderColor;
    }

    @Override
    public void setTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    @Override
    public Color getTextColor()
    {
        if (textColor == null)
        {
            return ColorChoice.DEFAULT_TEXT_COLOR;
        }
        return textColor;
    }

    /** The MultiLineStrings drawing the labels, see {@link #getLabelText(String, int)}; never saved */
    private transient Map<String, MultiLineString> labelTexts;

    /** The property name of the label not drawn while it is edited in place, or null; never saved */
    private transient String hiddenText;

    /** Number of label texts kept: three labels, plus the former values of one being typed */
    private static final int MAX_LABEL_TEXTS = 6;
}
