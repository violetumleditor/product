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
import java.awt.geom.Rectangle2D;
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
 * "+" buttons to add a node without going to the tool bar, with the selection tool.
 * <p>
 * On an empty spot: a tap clears the selection as before and also leaves a discreet dashed "+" there. A tap on it shows the kinds
 * of nodes the diagram accepts there, with their names (the common elements too): the chosen one is created on that spot and
 * selected, so that typing gives its name. A double click on an empty spot therefore opens this choice straight away.
 * <p>
 * In a node that takes children (lifeline, activation bar, package, object, field) or a grouping area: a "+" is the third button of
 * the column off the top right corner of the single selected node, below the edit button and the link handle, whatever the size or
 * the shape of the node. It offers the kinds of children this node takes (activation bar in a lifeline; class, interface, package in
 * a package...). A tap inside the node leaves a small target where the child will be created; without one, the child goes to the
 * first spot of the node not covered by one of its children, from its center. A double click on the node still opens its
 * properties, as nothing of this lies under the finger.
 * <p>
 * The dashed "+" and the target go away after 5 seconds, on the next press anywhere else, a drag, Escape, another tool, or once the
 * selection changes; the "+" of the column stays as long as its node is selected. Drawn by the editor over the diagram: never in
 * exports, prints nor files. Comes right after the link handle in the behaviors, as it consumes the mouse events of a gesture started
 * on a "+".
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
                hideSpot();
            }
        });
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isGestureOnButton = false;
        this.tapPoint = null;
        this.tapNode = null;
        if (event.getButton() == MouseEvent.BUTTON1 && (isOnEmptySpotButton(event) || isOnNodeButton(event)))
        {
            // The spot stays shown while the "+" is pressed, then while the choice is open
            getHideTimer().stop();
            this.isGestureOnButton = true;
            event.consume();
            return;
        }
        hideSpot();
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
            this.tapPoint = null; // a lasso or a move, not a tap
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            if (isOnEmptySpotButton(event) || isOnNodeButton(event))
            {
                // After the current mouse event, as the choice is a popup
                final INode node = getNodeButtonNode();
                SwingUtilities.invokeLater(() -> showChoice(node));
            }
            else if (getSpotPoint() != null)
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
            showSpot(getPointOnGraph(point), node);
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
        String text = isOnEmptySpotButton(event) ? this.toolTip : isOnNodeButton(event) ? this.childToolTip : null;
        if (text != null)
        {
            component.setToolTipText(text);
        }
        else if (isOwnToolTip(component.getToolTipText()))
        {
            component.setToolTipText(null);
        }
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        Point2D spot = getSpotPoint();
        Point2D nodeButton = getNodeButtonCenter();
        if (spot == null && nodeButton == null)
        {
            return;
        }
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        if (spot != null && this.spotNode == null)
        {
            paintEmptySpotButton(g2, spot);
        }
        if (spot != null && this.spotNode != null)
        {
            paintTarget(g2, spot);
        }
        if (nodeButton != null)
        {
            paintNodeButton(g2, nodeButton);
        }
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Dashed "+" on an empty spot
     */
    private void paintEmptySpotButton(Graphics2D g2, Point2D center)
    {
        double u = 1 / this.editorPart.getZoomFactor();
        double r = RADIUS * u;
        double x = center.getX();
        double y = center.getY();
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
    }

    /**
     * Plain "+" of the column, like the edit button and the link handle above it
     */
    private void paintNodeButton(Graphics2D g2, Point2D center)
    {
        double u = 1 / this.editorPart.getZoomFactor();
        double r = RADIUS * u;
        double x = center.getX();
        double y = center.getY();
        g2.setColor(BUTTON_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke((float) (2.4 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Line2D.Double(x - 7 * u, y, x + 7 * u, y));
        g2.draw(new Line2D.Double(x, y - 7 * u, x, y + 7 * u));
    }

    /**
     * Small target where the child will be created, small enough for an activation bar
     */
    private void paintTarget(Graphics2D g2, Point2D center)
    {
        double u = 1 / this.editorPart.getZoomFactor();
        double r = TARGET_RADIUS * u;
        double dot = 2 * u;
        double x = center.getX();
        double y = center.getY();
        Ellipse2D ring = new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r);
        g2.setColor(FILL_COLOR);
        g2.fill(ring);
        g2.setColor(BUTTON_COLOR);
        g2.setStroke(new BasicStroke((float) (2 * u)));
        g2.draw(ring);
        g2.fill(new Ellipse2D.Double(x - dot, y - dot, 2 * dot, 2 * dot));
    }

    /**
     * Shows the kinds of nodes near the tapped "+", which stays shown meanwhile, with the target where the node will go
     *
     * @param node the node of the tapped "+" of the column, or null for the "+" on an empty spot
     */
    private void showChoice(INode node)
    {
        Point2D point = node == null ? getSpotPoint() : getInsertionPoint(node);
        Point2D popupPoint = node == null ? point : getNodeButtonCenter();
        List<GraphTool> nodeTools = point == null ? new ArrayList<GraphTool>() : getNodeTools(point);
        if (point == null || popupPoint == null || nodeTools.isEmpty())
        {
            hideSpot();
            return;
        }
        if (node != null)
        {
            // Shows the target even when it has gone away, or wasn't there: the choice tells where the node will go
            this.spotPoint = point;
            this.spotNode = node;
            repaint();
        }
        final Point2D location = point;
        getToolChoicePopup().showLabeledIcons(nodeTools, popupPoint, aTool -> {
            hideSpot();
            addNode((INode) aTool.getNodeOrEdge(), location);
        }, () -> hideSpot());
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

    /**
     * @param point the tapped spot, on the graph
     * @param node the node tapped, or null for an empty spot
     */
    private void showSpot(Point2D point, INode node)
    {
        this.spotPoint = point;
        this.spotNode = node;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(getEscapeKeyDispatcher());
        getHideTimer().restart();
        repaint();
    }

    private void hideSpot()
    {
        getHideTimer().stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        this.spotNode = null;
        if (this.spotPoint != null)
        {
            this.spotPoint = null;
            repaint();
        }
    }

    /**
     * @return the tapped spot on the graph (unzoomed coordinates), with the dashed "+" on an empty spot or the target in a node, or
     *         {@code null} if it is not shown: never with another tool nor once the selection changes (Select all, Paste...): it
     *         must stay empty for an empty spot, or hold only the tapped node
     */
    private Point2D getSpotPoint()
    {
        if (this.spotPoint == null || !isSelectionTool() || !isSelectionOnly(this.spotNode))
        {
            return null;
        }
        return this.spotPoint;
    }

    /**
     * @return the single selected node if the selection tool is chosen and this node takes at least one kind of the tool bar's
     *         nodes as a child, else null
     */
    private INode getNodeButtonNode()
    {
        if (!isSelectionTool())
        {
            return null;
        }
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (selectedElements.size() != 1 || !(selectedElements.get(0) instanceof INode))
        {
            return null;
        }
        INode node = (INode) selectedElements.get(0);
        Point2D point = getInsertionPoint(node);
        if (point == null || getNodeTools(point).isEmpty())
        {
            return null;
        }
        return node;
    }

    /**
     * @return the center of the "+" of the column on the graph (unzoomed coordinates), below the link handle, itself below the edit
     *         button, or {@code null} if it is not shown
     */
    private Point2D getNodeButtonCenter()
    {
        if (getNodeButtonNode() == null)
        {
            return null;
        }
        EditButtonBehavior editButton = getEditButtonBehavior();
        Point2D editButtonCenter = editButton == null ? null : editButton.getButtonCenter();
        if (editButtonCenter == null)
        {
            return null;
        }
        double zoom = this.editorPart.getZoomFactor();
        return new Point2D.Double(editButtonCenter.getX(), editButtonCenter.getY() + 2 * COLUMN_STEP / zoom);
    }

    /**
     * @param node a node
     * @return where a child of this node goes: the tapped spot if it was in this node and is still shown, else the first spot of
     *         the node, from its center, where the node itself is found (not one of its children), or null if there is none
     */
    private Point2D getInsertionPoint(INode node)
    {
        Point2D spot = getSpotPoint();
        if (spot != null && this.spotNode == node)
        {
            return spot;
        }
        Rectangle2D bounds = node.getBounds();
        Point2D location = node.getLocationOnGraph();
        if (bounds == null || location == null)
        {
            return null;
        }
        double centerX = location.getX() + bounds.getWidth() / 2;
        double centerY = location.getY() + bounds.getHeight() / 2;
        // Center first, then rings of spots farther and farther from it
        for (int ring = 0; ring <= SEARCH_STEPS; ring++)
        {
            for (int i = -ring; i <= ring; i++)
            {
                for (int j = -ring; j <= ring; j++)
                {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring)
                    {
                        continue;
                    }
                    Point2D candidate = new Point2D.Double(centerX + i * bounds.getWidth() / (2 * SEARCH_STEPS + 2),
                            centerY + j * bounds.getHeight() / (2 * SEARCH_STEPS + 2));
                    if (this.graph.findNode(candidate) == node)
                    {
                        return candidate;
                    }
                }
            }
        }
        return null;
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

    private boolean isOnEmptySpotButton(MouseEvent event)
    {
        return this.spotNode == null && isOn(event, getSpotPoint());
    }

    private boolean isOnNodeButton(MouseEvent event)
    {
        return isOn(event, getNodeButtonCenter());
    }

    private boolean isOn(MouseEvent event, Point2D center)
    {
        if (center == null)
        {
            return false;
        }
        double zoom = this.editorPart.getZoomFactor();
        return event.getPoint().distance(center.getX() * zoom, center.getY() * zoom) <= RADIUS + HIT_MARGIN;
    }

    private boolean isOwnToolTip(String text)
    {
        return text != null && (text.equals(this.toolTip) || text.equals(this.childToolTip));
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

    private EditButtonBehavior getEditButtonBehavior()
    {
        if (this.editButtonBehavior == null)
        {
            List<EditButtonBehavior> found = this.behaviorManager.getBehaviors(EditButtonBehavior.class);
            this.editButtonBehavior = found.isEmpty() ? null : found.get(0);
        }
        return this.editButtonBehavior;
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
     * Removes the dashed "+" or the target if the "+" isn't tapped within {@link #HIDE_DELAY}. Not while it is pressed nor while
     * its choice is open.
     */
    private Timer getHideTimer()
    {
        if (this.hideTimer == null)
        {
            this.hideTimer = new Timer(HIDE_DELAY, e -> hideSpot());
            this.hideTimer.setRepeats(false);
        }
        return this.hideTimer;
    }

    /**
     * Escape removes the dashed "+" or the target. Registered only while one is shown.
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
                    if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_ESCAPE || getSpotPoint() == null)
                    {
                        return false;
                    }
                    hideSpot();
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

    /** Radius of the target on screen, in pixels: it fits in an activation bar */
    private static final double TARGET_RADIUS = 5;

    /** Distance on screen between the centers of two buttons of the column, in pixels: the same as the link handle */
    private static final double COLUMN_STEP = 2 * RADIUS + 8;

    /** Extra margin around the "+" to catch a finger, in pixels */
    private static final double HIT_MARGIN = 4;

    /** Distance on screen beyond which a press is a drag rather than a tap, in pixels: a finger always moves a little */
    private static final double TAP_THRESHOLD = 8;

    /** Time after which an untapped dashed "+" or a target goes away, in milliseconds */
    private static final int HIDE_DELAY = 5000;

    /** Spots tried on each side of a node's center, looking for one not covered by a child */
    private static final int SEARCH_STEPS = 4;

    private static final Color BUTTON_COLOR = new Color(123, 79, 214);

    private static final Color FILL_COLOR = new Color(123, 79, 214, 40);

    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPartBehaviorManager behaviorManager;

    private IGraphToolsBar graphToolsBar;

    private EditButtonBehavior editButtonBehavior;

    private ToolChoicePopup toolChoicePopup;

    private KeyEventDispatcher escapeKeyDispatcher;

    private Timer hideTimer;

    /** The tapped spot on the graph, with the dashed "+" or the target, or null if none is shown */
    private Point2D spotPoint;

    /** The node the target is in, or null for the dashed "+" on an empty spot */
    private INode spotNode;

    /** Where a press on a spot that can offer nodes happened, on screen, until it turns out to be a drag */
    private Point tapPoint;

    /** The node under that press, or null for an empty spot */
    private INode tapNode;

    /** True while the mouse events of the current gesture belong to this behavior */
    private boolean isGestureOnButton = false;

    @ResourceBundleBean(key = "add.button.tooltip")
    private String toolTip;

    @ResourceBundleBean(key = "add.child.button.tooltip")
    private String childToolTip;

}
