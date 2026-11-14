package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBarListener;

/**
 * "+" button to add a node without going to the tool bar: a tap on an empty spot of the diagram, with the selection tool, clears
 * the selection as before and also leaves a discreet "+" there. A tap on it shows the kinds of nodes the diagram accepts there,
 * with their names (the common elements too): the chosen one is created on that spot and selected, so that typing gives its name.
 * A double click on an empty spot therefore opens this choice straight away.
 * <p>
 * Same inside a node that takes children (lifeline, activation bar, package, object, field) or a grouping area: the tap selects it
 * as before and leaves the "+", which then offers the kinds of children this node takes (activation bar in a lifeline; class,
 * interface, package in a package...), created inside it. A double click there still opens the node's properties.
 * <p>
 * The "+" goes away after 5 seconds without a tap on it, on the next press anywhere else, a drag, Escape, another tool, or once
 * the selection changes. Drawn by the editor over the diagram: never in exports, prints nor files. Comes right after the link handle in the behaviors, as it consumes
 * the mouse events of a gesture started on the "+".
 */
public class AddNodeByTapBehavior extends AbstractEditorPartBehavior
{

    public AddNodeByTapBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
        this.graphToolsBar = graphToolsBar;
        graphToolsBar.addListener(new IGraphToolsBarListener()
        {
            @Override
            public void toolSelectionChanged(GraphTool selectedTool)
            {
                hideButton();
            }
        });
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isGestureOnButton = false;
        this.tapPoint = null;
        this.tapNode = null;
        if (event.getButton() == MouseEvent.BUTTON1 && isOnButton(event))
        {
            if (event.getClickCount() > 1 && this.buttonNode != null)
            {
                hideButton(); // a double click on a node still opens its properties
                return;
            }
            // Stays shown while it is pressed, then while the choice is open
            getHideTimer().stop();
            this.isGestureOnButton = true;
            event.consume();
            return;
        }
        hideButton();
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1 && isSelectionTool() && isOnTapSpot(event))
        {
            this.tapPoint = event.getPoint();
            this.tapNode = this.graph.findNode(getPointOnGraph(event.getPoint()));
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            return;
        }
        if (this.tapPoint != null && event.getPoint().distance(this.tapPoint) > TAP_THRESHOLD)
        {
            this.tapPoint = null; // a lasso, not a tap
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            if (isOnButton(event))
            {
                // After the current mouse event, as the choice is a popup
                SwingUtilities.invokeLater(() -> showChoice());
            }
            else if (getButtonCenter() != null)
            {
                getHideTimer().restart(); // released away from it: not a tap, the delay starts again
            }
            return;
        }
        Point point = this.tapPoint;
        INode node = this.tapNode;
        this.tapPoint = null;
        this.tapNode = null;
        // The tap has selected the node it was on, or cleared the selection on an empty spot
        if (point != null && isSelectionTool() && isSelectionOnly(node))
        {
            showButton(getPointOnGraph(point), node);
        }
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        // The buttons before this behavior have already removed their own tool tips if the mouse is not on them
        JComponent component = this.editorPart.getSwingComponent();
        if (isOnButton(event))
        {
            component.setToolTipText(this.toolTip);
        }
        else if (this.toolTip != null && this.toolTip.equals(component.getToolTipText()))
        {
            component.setToolTipText(null);
        }
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        Point2D center = getButtonCenter();
        if (center == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        double r = RADIUS / zoom;
        double u = 1 / zoom;
        double x = center.getX();
        double y = center.getY();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        Ellipse2D circle = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g2.setColor(FILL_COLOR);
        g2.fill(circle);
        g2.setColor(BUTTON_COLOR);
        g2.setStroke(new BasicStroke((float) (2 * u), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10,
                new float[] { (float) (4 * u), (float) (3 * u) }, 0));
        g2.draw(circle);
        g2.setStroke(new BasicStroke((float) (2.4 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(x - 6 * u, y, x + 6 * u, y));
        g2.draw(new Line2D.Double(x, y - 6 * u, x, y + 6 * u));
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Shows the kinds of nodes near the "+", which stays shown meanwhile
     */
    private void showChoice()
    {
        final Point2D point = getButtonCenter();
        List<GraphTool> nodeTools = point == null ? new ArrayList<GraphTool>() : getNodeTools(point);
        if (point == null || nodeTools.isEmpty())
        {
            hideButton();
            return;
        }
        getToolChoicePopup().showLabeledIcons(nodeTools, point, aTool -> {
            hideButton();
            addNode((INode) aTool.getNodeOrEdge(), point);
        }, () -> hideButton());
    }

    /**
     * @param point a spot of the diagram, on the graph
     * @return the node tools of the tool bar, in its order (the diagram's own, then the common ones), whose nodes the diagram
     *         accepts on that spot ({@link IGraph#canAddNode(INode, Point2D)}): on an empty spot or in a grouping area, all but
     *         those that only go into another node (activation bar, field); inside a node, only the kinds of children it takes
     *         (activation bar in a lifeline; class, interface, package in a package...), not the common elements
     */
    private List<GraphTool> getNodeTools(Point2D point)
    {
        INode nodeUnder = this.graph.findNode(point);
        boolean isInsideNode = nodeUnder != null && !(nodeUnder instanceof ContainerNode);
        List<GraphTool> nodeTools = new ArrayList<GraphTool>();
        for (GraphTool aTool : this.graphToolsBar.getNodeTools())
        {
            if (!(aTool.getNodeOrEdge() instanceof INode))
            {
                continue; // the selection tool
            }
            INode prototype = (INode) aTool.getNodeOrEdge();
            if (!this.graph.canAddNode(prototype, point) || (isInsideNode && !nodeUnder.canAddChild(prototype)))
            {
                continue;
            }
            nodeTools.add(aTool);
        }
        return nodeTools;
    }

    /**
     * Creates the node on the spot, as the node tools do, so that it can be undone, and selects it
     */
    private void addNode(INode prototype, Point2D point)
    {
        INode newNode = prototype.clone();
        newNode.setId(new Id());
        Point2D location = BehaviorUtils.getNewNodeLocation(this.graph, newNode, point);
        boolean isAdded = false;
        this.behaviorManager.fireBeforeAddingNodeAtPoint(newNode, location);
        try
        {
            isAdded = this.graph.addNode(newNode, location);
            if (isAdded)
            {
                newNode.incrementRevision();
            }
        }
        finally
        {
            this.behaviorManager.fireAfterAddingNodeAtPoint(newNode, location);
        }
        if (isAdded)
        {
            this.selectionHandler.setSelectedElement(newNode);
        }
        repaint();
    }

    private void showButton(Point2D center, INode node)
    {
        this.buttonCenter = center;
        this.buttonNode = node;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(getEscapeKeyDispatcher());
        getHideTimer().restart();
        repaint();
    }

    private void hideButton()
    {
        getHideTimer().stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        this.buttonNode = null;
        if (this.buttonCenter != null)
        {
            this.buttonCenter = null;
            repaint();
        }
    }

    /**
     * @return the center of the "+" on the graph (unzoomed coordinates), or {@code null} if it is not shown: never with another tool
     *         nor once the selection changes (Select all, Paste...): it must stay empty for a "+" on an empty spot, or hold only the
     *         node the "+" is in
     */
    private Point2D getButtonCenter()
    {
        if (this.buttonCenter == null || !isSelectionTool() || !isSelectionOnly(this.buttonNode))
        {
            return null;
        }
        return this.buttonCenter;
    }

    /**
     * @param node a node, or null for an empty spot
     * @return true if the selection is only this node, or empty for null
     */
    private boolean isSelectionOnly(INode node)
    {
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (node == null)
        {
            return selectedElements.isEmpty();
        }
        return selectedElements.size() == 1 && selectedElements.get(0) == node;
    }

    private boolean isOnButton(MouseEvent event)
    {
        Point2D center = getButtonCenter();
        if (center == null)
        {
            return false;
        }
        double zoom = this.editorPart.getZoomFactor();
        return event.getPoint().distance(center.getX() * zoom, center.getY() * zoom) <= RADIUS + HIT_MARGIN;
    }

    /**
     * @return true if a tap there can offer nodes: an empty spot, or a node that takes children (lifeline, package, object...) or a
     *         grouping area, but neither an edge nor a resize or crop handle
     */
    private boolean isOnTapSpot(MouseEvent event)
    {
        if (BehaviorUtils.isCursorOnResizePoint(this.editorPart, event) || BehaviorUtils.isCursorOnCropPoint(this.editorPart, event))
        {
            return false;
        }
        Point2D point = getPointOnGraph(event.getPoint());
        return this.graph.findEdge(point) == null && !getNodeTools(point).isEmpty();
    }

    private boolean isSelectionTool()
    {
        return GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool());
    }

    private Point2D getPointOnGraph(Point point)
    {
        double zoom = this.editorPart.getZoomFactor();
        return new Point2D.Double(point.getX() / zoom, point.getY() / zoom);
    }

    private ToolChoicePopup getToolChoicePopup()
    {
        if (this.toolChoicePopup == null)
        {
            this.toolChoicePopup = new ToolChoicePopup(this.editorPart);
        }
        return this.toolChoicePopup;
    }

    /**
     * Removes the "+" if it isn't tapped within {@link #HIDE_DELAY}. Not while it is pressed nor while its choice is open.
     */
    private Timer getHideTimer()
    {
        if (this.hideTimer == null)
        {
            this.hideTimer = new Timer(HIDE_DELAY, e -> hideButton());
            this.hideTimer.setRepeats(false);
        }
        return this.hideTimer;
    }

    /**
     * Escape removes the "+". Registered only while it is shown.
     */
    private KeyEventDispatcher getEscapeKeyDispatcher()
    {
        if (this.escapeKeyDispatcher == null)
        {
            this.escapeKeyDispatcher = new KeyEventDispatcher()
            {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event)
                {
                    if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_ESCAPE || getButtonCenter() == null)
                    {
                        return false;
                    }
                    hideButton();
                    return true;
                }
            };
        }
        return this.escapeKeyDispatcher;
    }

    private void repaint()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    /** Radius of the "+" on screen, in pixels: the same as the edit button */
    private static final double RADIUS = 16;

    /** Extra margin around the "+" to catch a finger, in pixels */
    private static final double HIT_MARGIN = 4;

    /** Distance on screen beyond which a press is a drag rather than a tap, in pixels: a finger always moves a little */
    private static final double TAP_THRESHOLD = 8;

    /** Time after which an untapped "+" goes away, in milliseconds */
    private static final int HIDE_DELAY = 5000;

    private static final Color BUTTON_COLOR = new Color(123, 79, 214);

    private static final Color FILL_COLOR = new Color(123, 79, 214, 40);

    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPartBehaviorManager behaviorManager;

    private IGraphToolsBar graphToolsBar;

    private ToolChoicePopup toolChoicePopup;

    private KeyEventDispatcher escapeKeyDispatcher;

    private Timer hideTimer;

    /** The center of the "+" on the graph, or null if it is not shown */
    private Point2D buttonCenter;

    /** The node the "+" is in, or null for an empty spot */
    private INode buttonNode;

    /** Where a press on a spot that can offer nodes happened, on screen, until it turns out to be a drag */
    private Point tapPoint;

    /** The node under that press, or null for an empty spot */
    private INode tapNode;

    /** True while the mouse events of the current gesture belong to this behavior */
    private boolean isGestureOnButton = false;

    @ResourceBundleBean(key = "add.button.tooltip")
    private String toolTip;

}
