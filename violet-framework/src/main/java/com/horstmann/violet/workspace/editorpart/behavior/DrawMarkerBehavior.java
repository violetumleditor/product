package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.common.MarkerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceChangeListener;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBarListener;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChangeListener;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;

/**
 * With the marker tool, a drag anywhere, even upon the other elements, draws a highlighter stroke freehand; a click leaves a dot.
 * The tool stays selected, to draw several strokes, and the style tools give the color and the thickness of the next ones.
 * <p>
 * Comes first, as it consumes its gestures. Ctrl + drag still moves the view (see DragGraphBehavior).
 * </p>
 */
public class DrawMarkerBehavior extends AbstractEditorPartBehavior
{
    public DrawMarkerBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar, IColorChoiceBar colorChoiceBar,
            IThicknessChoiceBar thicknessChoiceBar)
    {
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
        graphToolsBar.addListener(new IGraphToolsBarListener()
        {
            @Override
            public void toolSelectionChanged(GraphTool selectedTool)
            {
                onToolChanged(selectedTool);
            }
        });
        colorChoiceBar.addColorChoiceChangeListener(new IColorChoiceChangeListener()
        {
            @Override
            public void onColorChoiceChange(ColorChoice newColorChoice)
            {
                if (isMarkerToolSelected())
                {
                    getPen().setBackgroundColor(newColorChoice.getBackgroundColor());
                    getPen().setBorderColor(newColorChoice.getBorderColor());
                    repaintToolIcon();
                }
            }
        });
        thicknessChoiceBar.addThicknessChangeListener(new IThicknessChangeListener()
        {
            @Override
            public void onThicknessChange(int newThickness)
            {
                if (isMarkerToolSelected())
                {
                    getPen().setBorderWidth(newThickness);
                }
            }
        });
        // No mouse exit event for the behaviors: listen to the component itself
        editorPart.getSwingComponent().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseExited(MouseEvent event)
            {
                setTipPoint(null);
            }
        });
    }

    /**
     * @return true if the tool draws marker strokes
     */
    public static boolean isMarkerTool(GraphTool tool)
    {
        return tool != null && tool.getNodeOrEdge() instanceof MarkerNode;
    }

    public boolean isMarkerToolSelected()
    {
        return isMarkerTool(this.graphToolsBar.getSelectedTool());
    }

    /**
     * @return the marker whose style the next strokes take, never on the diagram itself: the one of the tool bar, so that its icon
     *         has the color of the next strokes
     */
    public MarkerNode getPen()
    {
        if (this.pen == null)
        {
            for (GraphTool aTool : this.graphToolsBar.getNodeTools())
            {
                if (isMarkerTool(aTool))
                {
                    this.pen = (MarkerNode) aTool.getNodeOrEdge();
                }
            }
        }
        return this.pen;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isGestureConsumed = false;
        if (event.getButton() != MouseEvent.BUTTON1 || KeyModifierUtil.isCtrl(event) || !isMarkerToolSelected())
        {
            cancelStroke();
            return;
        }
        this.isGestureConsumed = true;
        event.consume();
        setTipPoint(null);
        getStrokePoints().clear();
        addPoint(event);
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (!this.isGestureConsumed)
        {
            return;
        }
        event.consume();
        if (!getStrokePoints().isEmpty())
        {
            addPoint(event);
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (!this.isGestureConsumed)
        {
            return;
        }
        event.consume();
        if (event.getButton() != MouseEvent.BUTTON1 || getStrokePoints().isEmpty())
        {
            return;
        }
        addPoint(event);
        MarkerNode newNode = createStroke();
        getStrokePoints().clear();
        addToGraph(newNode);
        repaint();
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.isGestureConsumed)
        {
            event.consume(); // a second dot close to the first one isn't a double click
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        setTipPoint(isMarkerToolSelected() ? getPointOnGraph(event) : null);
    }

    /**
     * Paints the stroke being drawn, or the tip of the marker under the mouse
     */
    @Override
    public void onPaint(Graphics2D g2)
    {
        if (!getStrokePoints().isEmpty())
        {
            createStroke().draw(g2);
            return;
        }
        if (this.tipPoint == null || !isMarkerToolSelected())
        {
            return;
        }
        double width = getPen().getStrokeWidth();
        Color oldColor = g2.getColor();
        g2.setColor(getPen().getMarkerColor());
        g2.fill(new Ellipse2D.Double(this.tipPoint.getX() - width / 2, this.tipPoint.getY() - width / 2, width, width));
        g2.setColor(oldColor);
    }

    private void onToolChanged(GraphTool selectedTool)
    {
        cancelStroke();
        this.tipPoint = null;
        if (isMarkerTool(selectedTool))
        {
            // The style tools are then for the marker
            this.editorPart.getSelectionHandler().clearSelection();
        }
        repaint();
    }

    private void cancelStroke()
    {
        if (!getStrokePoints().isEmpty())
        {
            getStrokePoints().clear();
            repaint();
        }
    }

    private void addPoint(MouseEvent event)
    {
        Point2D point = getPointOnGraph(event);
        List<Point2D> points = getStrokePoints();
        double minDistance = MIN_POINT_DISTANCE / this.editorPart.getZoomFactor();
        if (points.isEmpty() || points.get(points.size() - 1).distance(point) >= minDistance)
        {
            points.add(point);
            repaint();
        }
    }

    private MarkerNode createStroke()
    {
        MarkerNode newNode = getPen().clone();
        newNode.setId(new Id());
        newNode.setGraph(getGraph());
        newNode.setPointsOnGraph(getStrokePoints());
        return newNode;
    }

    /**
     * Adds the stroke as the node tools do, so that it can be undone
     */
    private void addToGraph(MarkerNode newNode)
    {
        IEditorPartBehaviorManager behaviorManager = this.editorPart.getBehaviorManager();
        Point2D location = newNode.getLocation();
        behaviorManager.fireBeforeAddingNodeAtPoint(newNode, location);
        try
        {
            if (getGraph().addNode(newNode, location))
            {
                newNode.incrementRevision();
            }
        }
        finally
        {
            behaviorManager.fireAfterAddingNodeAtPoint(newNode, location);
        }
    }

    private void setTipPoint(Point2D newTipPoint)
    {
        if (newTipPoint == null && this.tipPoint == null)
        {
            return;
        }
        this.tipPoint = newTipPoint;
        repaint();
    }

    private Point2D getPointOnGraph(MouseEvent event)
    {
        double zoom = this.editorPart.getZoomFactor();
        return new Point2D.Double(Math.max(0, event.getX() / zoom), Math.max(0, event.getY() / zoom));
    }

    private IGraph getGraph()
    {
        return this.editorPart.getGraph();
    }

    private List<Point2D> getStrokePoints()
    {
        if (this.strokePoints == null)
        {
            this.strokePoints = new ArrayList<Point2D>();
        }
        return this.strokePoints;
    }

    private void repaintToolIcon()
    {
        this.graphToolsBar.getCommonToolsAWTComponent().repaint();
        this.graphToolsBar.getCompactAWTComponent().repaint();
        this.graphToolsBar.getSearchResultsAWTComponent().repaint();
    }

    private void repaint()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    private MarkerNode pen;

    /** Points of the stroke being drawn (graph coordinates), empty if none */
    private List<Point2D> strokePoints;

    /** Where the tip of the marker is shown, null if it isn't */
    private Point2D tipPoint;

    private boolean isGestureConsumed;

    /** Distance on screen between two points of a stroke */
    private static final double MIN_POINT_DISTANCE = 2;
}
