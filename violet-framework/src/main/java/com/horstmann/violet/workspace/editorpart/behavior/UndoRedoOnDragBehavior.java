package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;

/**
 * Undo/Redo behavior triggered when node and edges are dragged
 * 
 * @author Alexandre de Pellegrin
 *
 */
public class UndoRedoOnDragBehavior extends AbstractEditorPartBehavior
{

    /**
     * The concerned workspace
     */
    private IEditorPart editorPart;

    /**
     * The global undo/redo behavior which contains all individual undo/redo behaviors
     */
    private UndoRedoCompoundBehavior compoundBehavior;

    /**
     * Keeps node_old locations before dragging event
     */
    private Map<INode, Point2D> nodesLocationsBeforeDrag = new HashMap<INode, Point2D>();

    /**
     * Keeps node parents (null: on the graph) before dragging event (a dragged node can change its parent, see DragSelectedBehavior)
     */
    private Map<INode, INode> nodesParentsBeforeDrag = new HashMap<INode, INode>();

    /**
     * Keeps the rank of each node among its siblings before dragging event
     */
    private Map<INode, Integer> nodesIndexesBeforeDrag = new HashMap<INode, Integer>();

    /**
     * Keeps (copies of) edges transition points before dragging event
     */
    private Map<IEdge, ITransitionPoint[]> transitionPointsBeforeDrag = new HashMap<IEdge, ITransitionPoint[]>();

    /**
     * Used on node_old's drag'n drop
     */
    private boolean isDragInProgress = false;

    /**
     * Default constructor
     * @param editorPart
     * @param compoundBehavior
     */
    public UndoRedoOnDragBehavior(IEditorPart editorPart, UndoRedoCompoundBehavior compoundBehavior)
    {
        this.editorPart = editorPart;
        this.compoundBehavior = compoundBehavior;
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        double zoom = editorPart.getZoomFactor();
        final Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
        this.isDragInProgress = false;
        if (isMouseOnNode(mousePoint))
        {
            saveNodesLocationsBeforeDrag();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        this.isDragInProgress = true;
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (!this.isDragInProgress)
        {
            return;
        }
        // All moved nodes are concerned, not only selected ones (e.g. nodes dragged with their container)
        List<UndoableEdit> editList = new ArrayList<UndoableEdit>();
        for (final INode aSelectedNode : this.nodesLocationsBeforeDrag.keySet())
        {
            Point2D lastNodeLocation = this.nodesLocationsBeforeDrag.get(aSelectedNode);
            Point2D currentNodeLocation = aSelectedNode.getLocation();
            INode lastParent = this.nodesParentsBeforeDrag.get(aSelectedNode);
            if (lastParent != aSelectedNode.getParent())
            {
                editList.add(createChangeParentEdit(aSelectedNode, lastParent, lastNodeLocation));
                continue;
            }
            if (currentNodeLocation.equals(lastNodeLocation))
            {
                continue;
            }
            final double dx = currentNodeLocation.getX() - lastNodeLocation.getX();
            final double dy = currentNodeLocation.getY() - lastNodeLocation.getY();
            UndoableEdit edit = new AbstractUndoableEdit()
            {
                @Override
                public void undo() throws CannotUndoException
                {
                    aSelectedNode.translate(-dx, -dy);
                    super.undo();
                }

                @Override
                public void redo() throws CannotRedoException
                {
                    super.redo();
                    aSelectedNode.translate(dx, dy);
                }
            };
            editList.add(edit);
        }
        // Transition points of edges between dragged nodes move with them
        IGraph graph = this.editorPart.getGraph();
        for (IEdge anEdge : this.transitionPointsBeforeDrag.keySet())
        {
            ITransitionPoint[] transitionPointsBeforeDrag = this.transitionPointsBeforeDrag.get(anEdge);
            ITransitionPoint[] transitionPointsAfterDrag = EdgeTransitionPoint.copyOf(anEdge.getTransitionPoints());
            if (UndoRedoOnTransitionPointChangeBehavior.isSameLocations(transitionPointsBeforeDrag, transitionPointsAfterDrag))
            {
                continue;
            }
            editList.add(UndoRedoOnTransitionPointChangeBehavior.createEdit(graph, this.editorPart.getSelectionHandler(), anEdge,
                    transitionPointsBeforeDrag, transitionPointsAfterDrag));
        }
        if (editList.size() > 0)
        {
            this.compoundBehavior.startHistoryCapture();
            CompoundEdit capturedEdit = this.compoundBehavior.getCurrentCapturedEdit();
            for (UndoableEdit edit : editList)
            {
                capturedEdit.addEdit(edit);
            }
            this.compoundBehavior.stopHistoryCapture();
        }
        this.nodesLocationsBeforeDrag.clear();
        this.nodesParentsBeforeDrag.clear();
        this.transitionPointsBeforeDrag.clear();
        this.isDragInProgress = false;
    }

    /**
     * A location is relative to the parent, so it is restored with the parent, at the same rank among its children
     */
    private UndoableEdit createChangeParentEdit(final INode node, final INode oldParent, final Point2D oldLocation)
    {
        final IGraph graph = this.editorPart.getGraph();
        final int oldIndex = this.nodesIndexesBeforeDrag.get(node);
        final INode newParent = node.getParent();
        final int newIndex = graph.getRank(node);
        final Point2D newLocation = node.getLocation();
        return new AbstractUndoableEdit()
        {
            @Override
            public void undo() throws CannotUndoException
            {
                graph.changeParent(node, oldParent, oldIndex);
                node.setLocation(oldLocation);
                super.undo();
            }

            @Override
            public void redo() throws CannotRedoException
            {
                super.redo();
                graph.changeParent(node, newParent, newIndex);
                node.setLocation(newLocation);
            }
        };
    }

    /**
     * Saves node locations
     */
    private void saveNodesLocationsBeforeDrag()
    {
        this.nodesLocationsBeforeDrag.clear();
        this.nodesParentsBeforeDrag.clear();
        this.nodesIndexesBeforeDrag.clear();
        Collection<INode> selectedNodes = this.editorPart.getGraph().getAllNodes();
        for (INode aSelectedNode : selectedNodes)
        {
            Point2D location = aSelectedNode.getLocation();
            this.nodesLocationsBeforeDrag.put(aSelectedNode, location);
            INode parent = aSelectedNode.getParent();
            this.nodesParentsBeforeDrag.put(aSelectedNode, parent);
            this.nodesIndexesBeforeDrag.put(aSelectedNode, this.editorPart.getGraph().getRank(aSelectedNode));
        }
        this.transitionPointsBeforeDrag.clear();
        for (IEdge anEdge : this.editorPart.getGraph().getAllEdges())
        {
            if (anEdge.isTransitionPointsSupported())
            {
                this.transitionPointsBeforeDrag.put(anEdge, EdgeTransitionPoint.copyOf(anEdge.getTransitionPoints()));
            }
        }
    }

    private boolean isMouseOnNode(Point2D mouseLocation)
    {
        IGraph graph = this.editorPart.getGraph();
        INode node = graph.findNode(mouseLocation);
        if (node == null)
        {
            return false;
        }
        return true;
    }

}
