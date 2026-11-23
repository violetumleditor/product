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
 * On an empty spot: a tap while something is selected only clears the selection; a tap while nothing is selected leaves a discreet
 * dashed "+" there. A tap on it shows the kinds of nodes the diagram accepts there, with their names (the common elements too): the
 * chosen one is created on that spot and selected, so that typing gives its name. A double click on an empty spot therefore opens
 * this choice straight away when nothing is selected, and only clears the selection otherwise.
 * <p>
 * In a node that takes children (lifeline, activation bar, package, object, field) or a grouping area: a "+" is the third button of
 * the column off the top right corner of the single selected node, below the edit button and the link handle, whatever the size or
 * the shape of the node. It offers the kinds of children this node takes (activation bar in a lifeline; class, interface, package in
 * a package...). The child goes to the first spot of the node not covered by one of its children, from its center: a tap inside the
 * node leaves nothing there (user's choice: the "+" is enough). A double click on the node still opens its properties, as nothing of
 * this lies under the finger.
 * <p>
 * The dashed "+" goes away after 5 seconds, on the next press anywhere else, a drag, Escape, another tool, or once the
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
        if (event.getButton() == MouseEvent.BUTTON1 && (isOnEmptySpotButton(event) || isOnNodeButton(event)))
        {
            // The spot stays shown while the "+" is pressed, then while the choice is open
            getHideTimer().stop();
            this.isGestureOnButton = true;
            this.isButtonGestureMoved = false;
            this.buttonPressScreenPoint = isOnEmptySpotButton(event) ? event.getLocationOnScreen() : null;
            event.consume();
            return;
        }
        hideSpot();
        // Read before the selection behaviors, which clear the selection on this press: a tap that deselects shows no "+", the
        // next one does (user's choice). So does a double click: with a selection, its first click only deselects, and its second
        // one finds no "+" to open
        boolean wasSelectionEmpty = isSelectionEmpty();
        if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 1 && isSelectionTool() && wasSelectionEmpty
                && isOnTapSpot(event))
        {
            this.tapPoint = event.getPoint();
            this.tapScreenPoint = event.getLocationOnScreen();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            // A double tap on an empty spot, then a drag, moves the view (DragGraphBehavior): on screen, as the view moves
            // under the finger, so the point stays on the "+" in the editor's coordinates
            if (this.buttonPressScreenPoint != null && event.getLocationOnScreen().distance(this.buttonPressScreenPoint) > TAP_THRESHOLD)
            {
                this.isButtonGestureMoved = true;
            }
            event.consume();
            return;
        }
        // On screen, as a drag of the view (DragGraphBehavior) keeps the finger still in the editor's coordinates
        if (this.tapPoint != null && event.getLocationOnScreen().distance(this.tapScreenPoint) > TAP_THRESHOLD)
        {
            this.tapPoint = null; // a lasso or a move of the view, not a tap
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.isGestureOnButton)
        {
            event.consume();
            if (this.isButtonGestureMoved)
            {
                hideSpot(); // the view moved: not a tap on the "+"
            }
            else if (isOnEmptySpotButton(event) || isOnNodeButton(event))
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
        this.tapPoint = null;
        // The tap has cleared the selection on the empty spot
        if (point != null && isSelectionTool() && isSelectionEmpty())
        {
            showSpot(getPointOnGraph(point));
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
        if (spot != null)
        {
            paintEmptySpotButton(g2, spot);
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
     * Shows the kinds of nodes near the tapped "+", which stays shown meanwhile
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
                moveBelowOverlappedSiblings(newNode);
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
     * A child goes where its parent puts it, which may differ from the free spot found for it: a lifeline centers its activation
     * bars on its line, so a bar added beside another one would cover it. Moves the new child below each sibling it overlaps,
     * until it overlaps none.
     *
     * @param newNode the node just added
     */
    private void moveBelowOverlappedSiblings(INode newNode)
    {
        INode parent = newNode.getParent();
        if (parent == null)
        {
            return;
        }
        // One move per sibling at most: each one leaves the new node below that sibling
        for (int i = 0; i < parent.getChildren().size(); i++)
        {
            INode overlappedSibling = findOverlappedSibling(newNode);
            if (overlappedSibling == null)
            {
                return;
            }
            Point2D location = newNode.getLocation();
            double y = overlappedSibling.getBounds().getMaxY() + SIBLING_GAP;
            newNode.setLocation(this.graph.getGridSticker().snap(new Point2D.Double(location.getX(), y)));
        }
    }

    /**
     * @param node a child node
     * @return one of its siblings whose bounds intersect its own, or null
     */
    private INode findOverlappedSibling(INode node)
    {
        Rectangle2D bounds = node.getBounds();
        for (INode aSibling : node.getParent().getChildren())
        {
            if (aSibling != node && aSibling.getBounds().intersects(bounds))
            {
                return aSibling;
            }
        }
        return null;
    }

    /**
     * @param point the tapped empty spot, on the graph
     */
    private void showSpot(Point2D point)
    {
        this.spotPoint = point;
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(getEscapeKeyDispatcher());
        getHideTimer().restart();
        repaint();
    }

    private void hideSpot()
    {
        getHideTimer().stop();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        if (this.spotPoint != null)
        {
            this.spotPoint = null;
            repaint();
        }
    }

    /**
     * @return the tapped empty spot on the graph (unzoomed coordinates), with the dashed "+", or {@code null} if it is not shown:
     *         never with another tool nor once the selection changes (Select all, Paste...): it must stay empty
     */
    private Point2D getSpotPoint()
    {
        if (this.spotPoint == null || !isSelectionTool() || !isSelectionEmpty())
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
     * @return where a child of this node goes: the first spot of the node, from its center, where the node itself is found (not one
     *         of its children), or null if there is none
     */
    private Point2D getInsertionPoint(INode node)
    {
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

    private boolean isSelectionEmpty()
    {
        return this.selectionHandler.getSelectedElements().isEmpty();
    }

    private boolean isOnEmptySpotButton(MouseEvent event)
    {
        return isOn(event, getSpotPoint());
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
     * @return true if a tap there can offer nodes: an empty spot that accepts some, neither a node (its children come from the
     *         "+" of the column), an edge nor a resize or crop handle
     */
    private boolean isOnTapSpot(MouseEvent event)
    {
        if (BehaviorUtils.isCursorOnResizePoint(this.editorPart, event) || BehaviorUtils.isCursorOnCropPoint(this.editorPart, event))
        {
            return false;
        }
        Point2D point = getPointOnGraph(event.getPoint());
        return this.graph.findNode(point) == null && this.graph.findEdge(point) == null && !getNodeTools(point).isEmpty();
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
     * Removes the dashed "+" if it isn't tapped within {@link #HIDE_DELAY}. Not while it is pressed nor while
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
     * Escape removes the dashed "+". Registered only while it is shown.
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

    /** Distance on screen between the centers of two buttons of the column, in pixels: the same as the link handle */
    private static final double COLUMN_STEP = 2 * RADIUS + 8;

    /** Extra margin around the "+" to catch a finger, in pixels */
    private static final double HIT_MARGIN = 4;

    /** Distance on screen beyond which a press is a drag rather than a tap, in pixels: a finger always moves a little */
    private static final double TAP_THRESHOLD = 8;

    /** Time after which an untapped dashed "+" goes away, in milliseconds */
    private static final int HIDE_DELAY = 5000;

    /** Spots tried on each side of a node's center, looking for one not covered by a child */
    private static final int SEARCH_STEPS = 4;

    /** Vertical space between a child moved off a sibling and that sibling, on the graph (the gap between two calls) */
    private static final int SIBLING_GAP = 20;

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

    /** The tapped empty spot on the graph, with the dashed "+", or null if none is shown */
    private Point2D spotPoint;

    /** Where a press on a spot that can offer nodes happened, in the editor, until it turns out to be a drag */
    private Point tapPoint;

    /** The same press, on screen, to tell a tap from a drag */
    private Point tapScreenPoint;

    /** Where the press on the dashed "+" of an empty spot happened, on screen, null for the "+" of a node */
    private Point buttonPressScreenPoint;

    /** True once the gesture started on a "+" moved: it moves the view, it doesn't offer nodes */
    private boolean isButtonGestureMoved = false;

    /** True while the mouse events of the current gesture belong to this behavior */
    private boolean isGestureOnButton = false;

    @ResourceBundleBean(key = "add.button.tooltip")
    private String toolTip;

    @ResourceBundleBean(key = "add.child.button.tooltip")
    private String childToolTip;

}
