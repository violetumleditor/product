package com.horstmann.violet.workspace.editorpart.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.workspace.editorpart.EditorPartSelectionHandler;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;

class UndoRedoSelectionTest
{

    private static final Point2D LOCATION = new Point2D.Double(10, 10);

    private AbstractGraph graph;
    private IEditorPartSelectionHandler selectionHandler;
    private UndoRedoCompoundBehavior behavior;

    @BeforeEach
    void setUp()
    {
        this.graph = new AbstractGraph()
        {
            @Override
            public List<INode> getNodePrototypes()
            {
                return Collections.emptyList();
            }

            @Override
            public List<IEdge> getEdgePrototypes()
            {
                return Collections.emptyList();
            }
        };
        this.selectionHandler = new EditorPartSelectionHandler();
        this.behavior = new UndoRedoCompoundBehavior(newEditorPart());
    }

    @Test
    void shouldUnselectNodeRemovedByUndo()
    {
        INode node = addNode();
        this.selectionHandler.setSelectedElement(node);

        this.behavior.undo();

        assertFalse(this.graph.getAllNodes().contains(node));
        assertTrue(this.selectionHandler.getSelectedElements().isEmpty());
    }

    @Test
    void shouldUnselectNodeRemovedByRedo()
    {
        INode node = new NoteNode();
        this.graph.addNode(node, LOCATION);
        this.selectionHandler.setSelectedElement(node);
        this.behavior.beforeRemovingSelectedElements();
        this.graph.removeNode(node);
        this.behavior.afterRemovingSelectedElements();
        this.behavior.undo();
        this.selectionHandler.setSelectedElement(node);

        this.behavior.redo();

        assertFalse(this.graph.getAllNodes().contains(node));
        assertTrue(this.selectionHandler.getSelectedElements().isEmpty());
    }

    @Test
    void shouldKeepElementsStillOnGraphSelected()
    {
        INode keptNode = new NoteNode();
        this.graph.addNode(keptNode, LOCATION);
        IEdge keptEdge = new SegmentedLineEdge() {};
        ITransitionPoint keptPoint = new EdgeTransitionPoint(20, 20);
        keptEdge.setTransitionPoints(new ITransitionPoint[] { keptPoint });
        INode otherNode = new NoteNode();
        this.graph.addNode(otherNode, LOCATION);
        assertTrue(this.graph.connect(keptEdge, keptNode, LOCATION, otherNode, LOCATION));
        INode removedNode = addNode();
        this.selectionHandler.addSelectedElement(keptNode);
        this.selectionHandler.addSelectedElement(keptEdge);
        this.selectionHandler.addSelectedElement(keptPoint);
        this.selectionHandler.addSelectedElement(removedNode);

        this.behavior.undo();

        assertEquals(List.of(keptNode, keptEdge, keptPoint), this.selectionHandler.getSelectedElements());
    }

    private INode addNode()
    {
        INode node = new NoteNode();
        this.behavior.beforeAddingNodeAtPoint(node, LOCATION);
        this.graph.addNode(node, LOCATION);
        this.behavior.afterAddingNodeAtPoint(node, LOCATION);
        return node;
    }

    private IEditorPart newEditorPart()
    {
        JPanel swingComponent = new JPanel();
        return (IEditorPart) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IEditorPart.class },
                (proxy, method, args) -> switch (method.getName())
                {
                    case "getGraph" -> this.graph;
                    case "getSelectionHandler" -> this.selectionHandler;
                    case "getSwingComponent" -> swingComponent;
                    default -> null;
                });
    }
}
