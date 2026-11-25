package com.horstmann.violet.workspace.editorpart.behavior;

import com.horstmann.violet.product.diagram.abstracts.edge.MovedSegments;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.IGridSticker;
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.editorpart.PlainGrid;

/**
 * The arrow keys move the selected nodes by one grid step, Shift + an arrow by {@value #LARGE_STEP_COUNT} steps. They move as
 * a drag does: with the nodes lying in a selected grouping area, the transition points of the edges between moved nodes and the
 * selected transition points (caught by the lasso; they also move alone), and a node
 * never goes out of the top left of its parent. Ctrl + an arrow is left to the Edit menu (select next / previous).
 * <p>
 * Pressing an arrow key, held or repeated, until its release is one undoable move.
 * <p>
 * The diagram never takes the keyboard focus, so keys are caught by a dispatcher of the window, registered only while this
 * diagram is showing (Swing hides the diagrams of the other tabs). It comes after {@link TypeTextOnSelectedNodeBehavior}, which
 * keeps every key while a text is typed. Keys of a text field, a menu or a dialog are left alone.
 */
public class MoveSelectedByKeyboardBehavior extends AbstractEditorPartBehavior implements KeyEventDispatcher
{

    public MoveSelectedByKeyboardBehavior(IEditorPart editorPart, IGraphFile graphFile)
    {
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.graphFile = graphFile;
        JComponent component = editorPart.getSwingComponent();
        component.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
            {
                updateKeyListening();
            }
        });
        updateKeyListening();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (!isArrowKey(event.getKeyCode()))
        {
            return false;
        }
        if (event.getID() == KeyEvent.KEY_RELEASED)
        {
            boolean wasMoving = isMoving();
            commitMove();
            return wasMoving;
        }
        if (event.getID() != KeyEvent.KEY_PRESSED || !isForDiagram(event))
        {
            return false;
        }
        return moveSelectedNodes(event.getKeyCode(), event.isShiftDown());
    }

    /**
     * Moves the selected nodes as an arrow key does; {@link #commitMove()} makes the moves done since the first one undoable.
     *
     * @param keyCode an arrow key code (VK_LEFT, VK_KP_LEFT...)
     * @param isLargeStep true for {@value #LARGE_STEP_COUNT} grid steps (Shift), false for one
     * @return false when there is nothing to move
     */
    public boolean moveSelectedNodes(int keyCode, boolean isLargeStep)
    {
        List<INode> nodesToMove = getNodesToMove();
        List<ITransitionPoint> transitionPointsToMove = BehaviorUtils.getTransitionPointsToMove(this.graph, nodesToMove,
                this.selectionHandler.getSelectedElements());
        if ((nodesToMove.isEmpty() && transitionPointsToMove.isEmpty()) || !isArrowKey(keyCode))
        {
            return false;
        }
        if (!isMoving())
        {
            saveLocationsBeforeMove();
        }
        int stepCount = isLargeStep ? LARGE_STEP_COUNT : 1;
        move(nodesToMove, transitionPointsToMove, getStepX(keyCode) * stepCount, getStepY(keyCode) * stepCount);
        return true;
    }

    /**
     * Arrows without Ctrl, Alt nor Meta, pressed in this diagram's window, outside of text fields and open menus
     */
    private boolean isForDiagram(KeyEvent event)
    {
        if (event.isControlDown() || event.isAltDown() || event.isMetaDown() || event.isAltGraphDown())
        {
            return false;
        }
        Component component = event.getComponent();
        if (component == null || component instanceof JTextComponent)
        {
            return false;
        }
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0)
        {
            return false;
        }
        Window window = component instanceof Window ? (Window) component : SwingUtilities.getWindowAncestor(component);
        return window != null && window == SwingUtilities.getWindowAncestor(this.editorPart.getSwingComponent());
    }

    private boolean isArrowKey(int keyCode)
    {
        return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN
                || keyCode == KeyEvent.VK_KP_LEFT || keyCode == KeyEvent.VK_KP_RIGHT || keyCode == KeyEvent.VK_KP_UP
                || keyCode == KeyEvent.VK_KP_DOWN;
    }

    private double getStepX(int keyCode)
    {
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_KP_LEFT)
        {
            return -getGridStepX();
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_KP_RIGHT)
        {
            return getGridStepX();
        }
        return 0;
    }

    private double getStepY(int keyCode)
    {
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_KP_UP)
        {
            return -getGridStepY();
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_KP_DOWN)
        {
            return getGridStepY();
        }
        return 0;
    }

    private double getGridStepX()
    {
        double step = this.editorPart.getGrid().getSnappingWidth();
        return step > 0 ? step : PlainGrid.DEFAULT_GRID_SIZE;
    }

    private double getGridStepY()
    {
        double step = this.editorPart.getGrid().getSnappingHeight();
        return step > 0 ? step : PlainGrid.DEFAULT_GRID_SIZE;
    }

    /**
     * Selected nodes plus the nodes lying in selected grouping areas, as a drag moves them
     */
    private List<INode> getNodesToMove()
    {
        Set<INode> nodesToMove = new LinkedHashSet<>();
        for (Object element : this.selectionHandler.getSelectedElements())
        {
            if (element instanceof ContainerNode)
            {
                nodesToMove.addAll(((ContainerNode) element).getContainedNodes());
            }
            if (element instanceof INode)
            {
                nodesToMove.add((INode) element);
            }
        }
        return new ArrayList<>(nodesToMove);
    }

    private void move(List<INode> nodesToMove, List<ITransitionPoint> transitionPointsToMove, double dx, double dy)
    {
        // Nodes stay in the positive coordinates of their parent; the diagram itself extends in all directions
        dx = Math.max(dx, BehaviorUtils.getMinMoveX(nodesToMove));
        dy = Math.max(dy, BehaviorUtils.getMinMoveY(nodesToMove));
        if (dx == 0 && dy == 0)
        {
            return;
        }
        Map<SegmentedLineEdge, Point2D> adaptedEdges = BehaviorUtils.getAdaptedEdgesToMove(this.graph, nodesToMove);
        IGridSticker gridSticker = this.graph.getGridSticker();
        for (INode node : nodesToMove)
        {
            if (nodesToMove.contains(node.getParent()))
            {
                continue; // moved with its parent
            }
            Point2D location = node.getLocation();
            node.setLocation(gridSticker.snap(new Point2D.Double(location.getX() + dx, location.getY() + dy)));
        }
        BehaviorUtils.followNodes(adaptedEdges);
        for (ITransitionPoint transitionPoint : transitionPointsToMove)
        {
            Point2D p = gridSticker.snap(new Point2D.Double(transitionPoint.getX() + dx, transitionPoint.getY() + dy));
            transitionPoint.setX(p.getX());
            transitionPoint.setY(p.getY());
        }
        this.graphFile.setSaveRequired();
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
        Rectangle2D movedBounds = null;
        for (INode node : nodesToMove)
        {
            Rectangle2D bounds = BehaviorUtils.getBoundsOnGraph(node);
            movedBounds = movedBounds == null ? bounds : movedBounds.createUnion(bounds);
        }
        if (movedBounds != null)
        {
            this.editorPart.ensureVisible(movedBounds);
        }
    }

    private boolean isMoving()
    {
        return !getLocationsBeforeMove().isEmpty();
    }

    private void saveLocationsBeforeMove()
    {
        getMovedSegmentsBeforeMove().putAll(BehaviorUtils.getMovedSegments(this.graph));
        for (INode node : this.graph.getAllNodes())
        {
            getLocationsBeforeMove().put(node, node.getLocation());
        }
        for (IEdge edge : this.graph.getAllEdges())
        {
            if (edge.isTransitionPointsSupported())
            {
                getTransitionPointsBeforeMove().put(edge, EdgeTransitionPoint.copyOf(edge.getTransitionPoints()));
            }
        }
    }

    /**
     * Records the move done since the arrow key was pressed as one undoable edit
     */
    public void commitMove()
    {
        if (!isMoving())
        {
            return;
        }
        List<UndoableEdit> edits = new ArrayList<>();
        for (Map.Entry<INode, Point2D> entry : getLocationsBeforeMove().entrySet())
        {
            final INode node = entry.getKey();
            Point2D formerLocation = entry.getValue();
            Point2D location = node.getLocation();
            if (location.equals(formerLocation))
            {
                continue;
            }
            final double dx = location.getX() - formerLocation.getX();
            final double dy = location.getY() - formerLocation.getY();
            edits.add(new AbstractUndoableEdit()
            {
                @Override
                public void undo() throws CannotUndoException
                {
                    node.translate(-dx, -dy);
                    super.undo();
                }

                @Override
                public void redo() throws CannotRedoException
                {
                    super.redo();
                    node.translate(dx, dy);
                }
            });
        }
        for (Map.Entry<IEdge, ITransitionPoint[]> entry : getTransitionPointsBeforeMove().entrySet())
        {
            IEdge edge = entry.getKey();
            ITransitionPoint[] after = EdgeTransitionPoint.copyOf(edge.getTransitionPoints());
            if (!UndoRedoOnTransitionPointChangeBehavior.isSameLocations(entry.getValue(), after))
            {
                edits.add(UndoRedoOnTransitionPointChangeBehavior.createEdit(this.graph, this.selectionHandler, edge, entry.getValue(),
                        after));
            }
        }
        edits.addAll(BehaviorUtils.createMovedSegmentsEdits(getMovedSegmentsBeforeMove()));
        getMovedSegmentsBeforeMove().clear();
        getLocationsBeforeMove().clear();
        getTransitionPointsBeforeMove().clear();
        List<UndoRedoCompoundBehavior> undoRedoBehaviors = this.editorPart.getBehaviorManager()
                .getBehaviors(UndoRedoCompoundBehavior.class);
        if (edits.isEmpty() || undoRedoBehaviors.size() != 1)
        {
            return;
        }
        UndoRedoCompoundBehavior undoRedoBehavior = undoRedoBehaviors.get(0);
        undoRedoBehavior.startHistoryCapture();
        CompoundEdit capturedEdit = undoRedoBehavior.getCurrentCapturedEdit();
        for (UndoableEdit edit : edits)
        {
            capturedEdit.addEdit(edit);
        }
        undoRedoBehavior.stopHistoryCapture();
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        commitMove(); // an arrow key still held: the move ends here
    }

    private void updateKeyListening()
    {
        boolean isShowing = this.editorPart.getSwingComponent().isShowing();
        if (isShowing == this.isListeningToKeys)
        {
            return;
        }
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (isShowing)
        {
            focusManager.addKeyEventDispatcher(this);
        }
        else
        {
            focusManager.removeKeyEventDispatcher(this);
            commitMove();
        }
        this.isListeningToKeys = isShowing;
    }

    private Map<INode, Point2D> getLocationsBeforeMove()
    {
        if (this.locationsBeforeMove == null)
        {
            this.locationsBeforeMove = new LinkedHashMap<>();
        }
        return this.locationsBeforeMove;
    }

    private Map<SegmentedLineEdge, MovedSegments> getMovedSegmentsBeforeMove()
    {
        if (this.movedSegmentsBeforeMove == null)
        {
            this.movedSegmentsBeforeMove = new LinkedHashMap<>();
        }
        return this.movedSegmentsBeforeMove;
    }

    private Map<IEdge, ITransitionPoint[]> getTransitionPointsBeforeMove()
    {
        if (this.transitionPointsBeforeMove == null)
        {
            this.transitionPointsBeforeMove = new LinkedHashMap<>();
        }
        return this.transitionPointsBeforeMove;
    }

    /** Grid steps of a move with Shift */
    private static final int LARGE_STEP_COUNT = 5;

    private IEditorPart editorPart;
    private IGraph graph;
    private IEditorPartSelectionHandler selectionHandler;
    private IGraphFile graphFile;
    private boolean isListeningToKeys;

    /** Locations of all nodes when the arrow key was pressed, empty while no move is going on */
    private Map<INode, Point2D> locationsBeforeMove;
    /** Copies of the transition points of the edges when the arrow key was pressed */
    private Map<IEdge, ITransitionPoint[]> transitionPointsBeforeMove;
    /** The segments dragged by the user on edges when the arrow key was pressed */
    private Map<SegmentedLineEdge, MovedSegments> movedSegmentsBeforeMove;

}
