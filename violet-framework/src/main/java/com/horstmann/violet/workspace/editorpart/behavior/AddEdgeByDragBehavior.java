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
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBarListener;

/**
 * Link handle: a round violet button just below the edit button of the single selected node, to link it to another node without
 * going to the tool bar. Drag it onto the other node, or tap it then tap the other node (easier on a touch screen). A right click
 * or Escape cancels.
 * <p>
 * Only the kinds of edges the diagram defines as possible between both nodes are offered
 * ({@link com.horstmann.violet.product.diagram.abstracts.IGraph#getEdgePrototypes(INode, INode)}): a single one is added
 * at once, several ones are shown in a row of icons near the release point, the same as in the tool bar, and nothing is added if
 * this choice is dismissed. The new edge is selected, so that typing fills its text; the selected tool doesn't change.
 * <p>
 * Released in the void (or a tap there after a tap on the handle), it offers the kinds of nodes that can be linked to the starting
 * node, with their names: the chosen one is created there, then linked as above (the kind of edge chosen if several fit), node
 * and edge undone in one step. The new node is selected, so that typing gives its name. Dismissing either choice creates nothing.
 * <p>
 * Shown when the edit button is ({@link EditButtonBehavior}), for a node only, and drawn by the editor over the diagram: never in
 * exports, prints nor files. Comes right after the edit button in the behaviors, as it consumes the mouse events of its gestures.
 */
public class AddEdgeByDragBehavior extends AbstractEditorPartBehavior
{

    public AddEdgeByDragBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
        this.graphToolsBar = graphToolsBar;
        // Another tool chosen while waiting for the tap on the other node: the link is given up
        graphToolsBar.addListener(new IGraphToolsBarListener()
        {
            @Override
            public void toolSelectionChanged(GraphTool selectedTool)
            {
                if (AddEdgeByDragBehavior.this.state == State.WAITING_FOR_TARGET)
                {
                    reset();
                }
            }
        });
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isGestureConsumed = false;
        if (this.state == State.WAITING_FOR_TARGET)
        {
            this.isGestureConsumed = true;
            event.consume();
            if (event.getButton() == MouseEvent.BUTTON1)
            {
                link(getPointOnGraph(event));
            }
            else
            {
                reset();
            }
            return;
        }
        if (this.state != State.IDLE || event.getButton() != MouseEvent.BUTTON1)
        {
            return;
        }
        Point2D center = getHandleCenter();
        if (center == null || !isOnHandle(event, center))
        {
            return;
        }
        this.isGestureConsumed = true;
        event.consume();
        this.state = State.PRESSED;
        this.startNode = (INode) this.selectionHandler.getSelectedElements().get(0);
        this.handleCenter = center;
        this.pressPoint = event.getPoint();
        this.currentPoint = center;
        this.targetNode = null;
        repaint();
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (!this.isGestureConsumed)
        {
            return;
        }
        event.consume();
        if (this.state == State.PRESSED && event.getPoint().distance(this.pressPoint) > DRAG_THRESHOLD)
        {
            this.state = State.DRAGGING;
        }
        if (this.state == State.DRAGGING)
        {
            follow(event);
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
        if (this.state == State.DRAGGING)
        {
            link(getPointOnGraph(event));
        }
        else if (this.state == State.PRESSED)
        {
            // A tap on the handle: the next tap gives the other node
            this.state = State.WAITING_FOR_TARGET;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(getEscapeKeyDispatcher());
            repaint();
        }
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (this.isGestureConsumed)
        {
            event.consume();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
        if (this.state == State.WAITING_FOR_TARGET)
        {
            follow(event);
        }
        // The edit button, before this behavior, has already removed its own tool tip if the mouse is not on it
        JComponent component = this.editorPart.getSwingComponent();
        Point2D center = this.state == State.IDLE ? getHandleCenter() : null;
        if (center != null && isOnHandle(event, center))
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
        if (this.state == State.IDLE)
        {
            Point2D center = getHandleCenter();
            if (center != null)
            {
                paintHandle(g2, center, BUTTON_COLOR);
            }
            return;
        }
        if (this.targetNode != null)
        {
            paintTarget(g2, this.targetNode);
        }
        paintLine(g2, this.handleCenter, this.currentPoint);
        paintHandle(g2, this.handleCenter, PRESSED_BUTTON_COLOR);
    }

    /**
     * Ends the gesture on the given point: links the starting node to the node found there, or gives up
     */
    private void link(Point2D point)
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        INode endNode = this.graph.findNode(point);
        if (this.startNode == null)
        {
            reset();
            return;
        }
        if (endNode == null)
        {
            offerNewNode(point);
            return;
        }
        INode start = this.startNode;
        Rectangle2D startBounds = start.getBounds();
        Point2D startLocation = new Point2D.Double(startBounds.getWidth() / 2, startBounds.getHeight() / 2);
        Point2D endNodeLocation = endNode.getLocationOnGraph();
        Point2D endLocation = new Point2D.Double(point.getX() - endNodeLocation.getX(), point.getY() - endNodeLocation.getY());
        List<GraphTool> tools = getPossibleTools(start, endNode);
        if (tools.size() == 1)
        {
            reset();
            addEdge((IEdge) tools.get(0).getNodeOrEdge(), start, startLocation, endNode, endLocation);
            return;
        }
        if (tools.isEmpty())
        {
            reset();
            return;
        }
        // The line stays drawn to the other node while the kind of edge is chosen
        this.state = State.CHOOSING;
        this.currentPoint = point;
        this.targetNode = endNode;
        repaint();
        showChoice(tools, point, start, startLocation, endNode, endLocation);
    }

    /**
     * @return the edge tools of the tool bar whose edges the diagram defines as possible between both nodes, in the order of the
     *         tool bar
     */
    private List<GraphTool> getPossibleTools(INode start, INode end)
    {
        List<IEdge> possiblePrototypes = this.graph.getEdgePrototypes(start, end);
        List<GraphTool> possibleTools = new ArrayList<GraphTool>();
        for (GraphTool aTool : this.graphToolsBar.getEdgeTools())
        {
            if (possiblePrototypes.contains(aTool.getNodeOrEdge()))
            {
                possibleTools.add(aTool);
            }
        }
        return possibleTools;
    }

    /**
     * Shows the accepted kinds of edges in a row of icons near the release point
     */
    private void showChoice(List<GraphTool> tools, Point2D point, final INode start, final Point2D startLocation, final INode end,
            final Point2D endLocation)
    {
        getToolChoicePopup().showIcons(tools, point, aTool -> {
            reset();
            addEdge((IEdge) aTool.getNodeOrEdge(), start, startLocation, end, endLocation);
        }, () -> reset());
    }

    /**
     * Released in the void (or tapped there after a tap on the handle): offers the kinds of nodes that can be linked to the
     * starting node, to create one there, already linked
     */
    private void offerNewNode(final Point2D point)
    {
        final INode start = this.startNode;
        List<GraphTool> nodeTools = getLinkableNodeTools(start, point);
        if (nodeTools.isEmpty())
        {
            reset();
            return;
        }
        // The line stays drawn to the release point while the kind of node, then of edge, is chosen
        this.state = State.CHOOSING;
        this.currentPoint = point;
        this.targetNode = null;
        repaint();
        getToolChoicePopup().showLabeledIcons(nodeTools, point, aTool -> chooseEdgeToNewNode((INode) aTool.getNodeOrEdge(), start, point),
                () -> reset());
    }

    /**
     * Once the kind of node is chosen: the kind of edge, at once if a single one fits, else in the same row of icons as for an
     * existing node. Nothing is created if this second choice is dismissed.
     */
    private void chooseEdgeToNewNode(final INode nodePrototype, final INode start, final Point2D point)
    {
        List<GraphTool> edgeTools = getPossibleTools(start, nodePrototype);
        if (edgeTools.isEmpty())
        {
            reset();
            return;
        }
        if (edgeTools.size() == 1)
        {
            reset();
            addNodeAndEdge(nodePrototype, (IEdge) edgeTools.get(0).getNodeOrEdge(), start, point);
            return;
        }
        getToolChoicePopup().showIcons(edgeTools, point, aTool -> {
            reset();
            addNodeAndEdge(nodePrototype, (IEdge) aTool.getNodeOrEdge(), start, point);
        }, () -> reset());
    }

    /**
     * @return the node tools of the tool bar, in its order, whose nodes the diagram lets link to the starting node with one of its
     *         own edges; the elements common to all diagrams always, linked by the generic edge, but the grouping area, which is
     *         not something to link to. If the starting node is a common element, every kind of node. Only those the diagram
     *         accepts on the release point ({@link IGraph#canAddNode(INode, Point2D)}): never an activation bar nor a field there.
     */
    private List<GraphTool> getLinkableNodeTools(INode start, Point2D point)
    {
        List<GraphTool> linkableTools = new ArrayList<GraphTool>();
        boolean isCommonStart = isCommonNode(start);
        for (GraphTool aTool : this.graphToolsBar.getNodeTools())
        {
            Object prototype = aTool.getNodeOrEdge();
            if (!(prototype instanceof INode) || prototype instanceof ContainerNode)
            {
                continue; // the selection tool
            }
            INode nodePrototype = (INode) prototype;
            if (!this.graph.canAddNode(nodePrototype, point))
            {
                continue;
            }
            if (isCommonStart || isCommonNode(nodePrototype) || hasDiagramEdge(start, nodePrototype))
            {
                linkableTools.add(aTool);
            }
        }
        return linkableTools;
    }

    private boolean hasDiagramEdge(INode start, INode end)
    {
        for (IEdge aPrototype : this.graph.getEdgePrototypes(start, end))
        {
            if (!(aPrototype instanceof GenericEdge))
            {
                return true;
            }
        }
        return false;
    }

    private boolean isCommonNode(INode node)
    {
        for (Class<? extends INode> aClass : AbstractGraph.COMMON_NODE_CLASSES)
        {
            if (aClass.isInstance(node))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the node on the release point and links the starting node to it, as one step to undo; the new node is selected, so
     * that typing gives its name
     */
    private void addNodeAndEdge(INode nodePrototype, IEdge edgePrototype, INode start, Point2D point)
    {
        INode newNode = nodePrototype.clone();
        newNode.setId(new Id());
        Point2D location = BehaviorUtils.getNewNodeLocation(this.graph, newNode, point);
        IEdge newEdge = edgePrototype.clone();
        newEdge.setId(new Id());
        Rectangle2D startBounds = start.getBounds();
        Point2D startLocation = new Point2D.Double(startBounds.getWidth() / 2, startBounds.getHeight() / 2);
        Rectangle2D newNodeBounds = newNode.getBounds();
        Point2D endLocation = new Point2D.Double(newNodeBounds.getWidth() / 2, newNodeBounds.getHeight() / 2);
        Point2D startPoint = getPointOnGraph(start, startLocation);
        Point2D endPoint = new Point2D.Double(location.getX() + endLocation.getX(), location.getY() + endLocation.getY());
        boolean isNodeAdded = false;
        // Both inside the events of the edge: the undo history captures the node with it, as one step
        this.behaviorManager.fireBeforeAddingEdgeAtPoints(newEdge, startPoint, endPoint);
        try
        {
            isNodeAdded = this.graph.addNode(newNode, location);
            if (isNodeAdded)
            {
                newNode.incrementRevision();
                if (this.graph.connect(newEdge, start, startLocation, newNode, endLocation))
                {
                    newEdge.incrementRevision();
                }
            }
        }
        finally
        {
            this.behaviorManager.fireAfterAddingEdgeAtPoints(newEdge, startPoint, endPoint);
        }
        if (isNodeAdded)
        {
            this.selectionHandler.setSelectedElement(newNode);
        }
        repaint();
    }

    /**
     * Adds the edge as the link tool does, so that it can be undone
     */
    private void addEdge(IEdge prototype, INode start, Point2D startLocation, INode end, Point2D endLocation)
    {
        IEdge newEdge = prototype.clone();
        newEdge.setId(new Id());
        Point2D startPoint = getPointOnGraph(start, startLocation);
        Point2D endPoint = getPointOnGraph(end, endLocation);
        boolean isAdded = false;
        this.behaviorManager.fireBeforeAddingEdgeAtPoints(newEdge, startPoint, endPoint);
        try
        {
            isAdded = this.graph.connect(newEdge, start, startLocation, end, endLocation);
            if (isAdded)
            {
                newEdge.incrementRevision();
            }
        }
        finally
        {
            this.behaviorManager.fireAfterAddingEdgeAtPoints(newEdge, startPoint, endPoint);
        }
        if (isAdded)
        {
            this.selectionHandler.setSelectedElement(newEdge);
        }
        repaint();
    }

    /**
     * Back to the handle of the selected node
     */
    private void reset()
    {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(getEscapeKeyDispatcher());
        this.state = State.IDLE;
        this.startNode = null;
        this.targetNode = null;
        this.handleCenter = null;
        this.currentPoint = null;
        repaint();
    }

    private void follow(MouseEvent event)
    {
        this.currentPoint = getPointOnGraph(event);
        this.targetNode = this.graph.findNode(this.currentPoint);
        repaint();
    }

    /**
     * @return the center of the handle on the graph (unzoomed coordinates), just below the edit button, or {@code null} if no
     *         handle is shown
     */
    private Point2D getHandleCenter()
    {
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (selectedElements.size() != 1 || !(selectedElements.get(0) instanceof INode))
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
        return new Point2D.Double(editButtonCenter.getX(), editButtonCenter.getY() + (2 * RADIUS + GAP) / zoom);
    }

    private boolean isOnHandle(MouseEvent event, Point2D center)
    {
        double zoom = this.editorPart.getZoomFactor();
        return event.getPoint().distance(center.getX() * zoom, center.getY() * zoom) <= RADIUS + HIT_MARGIN;
    }

    private Point2D getPointOnGraph(MouseEvent event)
    {
        double zoom = this.editorPart.getZoomFactor();
        return new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
    }

    private Point2D getPointOnGraph(INode node, Point2D relativeLocation)
    {
        Point2D nodeLocation = node.getLocationOnGraph();
        return new Point2D.Double(nodeLocation.getX() + relativeLocation.getX(), nodeLocation.getY() + relativeLocation.getY());
    }

    /**
     * Same size and colors as the edit button, with an arrow leaving towards the top right
     */
    private void paintHandle(Graphics2D g2, Point2D center, Color color)
    {
        double zoom = this.editorPart.getZoomFactor();
        double r = RADIUS / zoom;
        double u = 1 / zoom;
        double x = center.getX();
        double y = center.getY();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(SHADOW_COLOR);
        g2.fill(new Ellipse2D.Double(x - r, y - r + u, 2 * r, 2 * r));
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(x - r, y - r, 2 * r, 2 * r));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(x - 5 * u, y + 5 * u);
        arrow.lineTo(x + 5 * u, y - 5 * u);
        arrow.moveTo(x - 1 * u, y - 5 * u);
        arrow.lineTo(x + 5 * u, y - 5 * u);
        arrow.lineTo(x + 5 * u, y + 1 * u);
        g2.setColor(ARROW_COLOR);
        g2.setStroke(new BasicStroke((float) (1.8 * u), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(arrow);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private void paintLine(Graphics2D g2, Point2D from, Point2D to)
    {
        if (from == null || to == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(BUTTON_COLOR);
        g2.setStroke(new BasicStroke((float) (2 / zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10,
                new float[] { (float) (6 / zoom), (float) (5 / zoom) }, 0));
        g2.draw(new Line2D.Double(from, to));
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    /**
     * Outlines the node under the pointer, the one the edge would end on
     */
    private void paintTarget(Graphics2D g2, INode node)
    {
        Point2D location = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        if (location == null || bounds == null)
        {
            return;
        }
        double zoom = this.editorPart.getZoomFactor();
        double margin = TARGET_MARGIN / zoom;
        double arc = 2 * TARGET_MARGIN / zoom;
        RoundRectangle2D outline = new RoundRectangle2D.Double(location.getX() - margin, location.getY() - margin,
                bounds.getWidth() + 2 * margin, bounds.getHeight() + 2 * margin, arc, arc);
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(TARGET_FILL_COLOR);
        g2.fill(outline);
        g2.setColor(BUTTON_COLOR);
        g2.setStroke(new BasicStroke((float) (2 / zoom)));
        g2.draw(outline);
        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private ToolChoicePopup getToolChoicePopup()
    {
        if (this.toolChoicePopup == null)
        {
            this.toolChoicePopup = new ToolChoicePopup(this.editorPart);
        }
        return this.toolChoicePopup;
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

    /**
     * Escape gives up the link while waiting for the tap on the other node. Registered only meanwhile.
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
                    if (event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_ESCAPE
                            || AddEdgeByDragBehavior.this.state != State.WAITING_FOR_TARGET)
                    {
                        return false;
                    }
                    reset();
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

    private enum State
    {
        /** The handle of the selected node is shown, if any */
        IDLE,
        /** Pressed on the handle, not moved yet */
        PRESSED,
        /** The handle is dragged towards the other node */
        DRAGGING,
        /** The handle was tapped: the next tap gives the other node */
        WAITING_FOR_TARGET,
        /** The kind of edge is being chosen */
        CHOOSING
    }

    /** Radius of the handle on screen, in pixels: the same as the edit button */
    private static final double RADIUS = 16;

    /** Space between the edit button and the handle on screen, in pixels: their areas caught for a finger don't overlap */
    private static final double GAP = 8;

    /** Extra margin around the handle to catch a finger, in pixels */
    private static final double HIT_MARGIN = 4;

    /** Distance on screen beyond which a press on the handle becomes a drag, in pixels: a finger always moves a little */
    private static final double DRAG_THRESHOLD = 8;

    /** Space around the outline of the node under the pointer, on screen, in pixels */
    private static final double TARGET_MARGIN = 4;

    private static final Color BUTTON_COLOR = new Color(123, 79, 214);

    private static final Color PRESSED_BUTTON_COLOR = new Color(91, 59, 176);

    private static final Color ARROW_COLOR = Color.WHITE;

    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 70);

    private static final Color TARGET_FILL_COLOR = new Color(123, 79, 214, 40);

    private IEditorPart editorPart;

    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;

    private IEditorPartBehaviorManager behaviorManager;

    private IGraphToolsBar graphToolsBar;

    private EditButtonBehavior editButtonBehavior;

    private ToolChoicePopup toolChoicePopup;

    private KeyEventDispatcher escapeKeyDispatcher;

    private State state = State.IDLE;

    /** True while the mouse events of the current gesture belong to this behavior */
    private boolean isGestureConsumed = false;

    /** The node the edge starts from, while linking */
    private INode startNode;

    /** The node under the pointer, while linking */
    private INode targetNode;

    /** The center of the handle on the graph when it was pressed */
    private Point2D handleCenter;

    /** Where the handle was pressed, on screen */
    private Point pressPoint;

    /** The end of the line drawn from the handle, on the graph */
    private Point2D currentPoint;

    @ResourceBundleBean(key = "link.handle.tooltip")
    private String toolTip;

}
