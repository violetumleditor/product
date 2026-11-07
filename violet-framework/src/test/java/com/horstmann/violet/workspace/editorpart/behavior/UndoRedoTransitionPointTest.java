package com.horstmann.violet.workspace.editorpart.behavior;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
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

class UndoRedoTransitionPointTest
{

    private AbstractGraph graph;
    private IEditorPartSelectionHandler selectionHandler;
    private UndoRedoCompoundBehavior behavior;
    private JPanel swingComponent = new JPanel();
    private INode startNode;
    private IEdge edge;

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
        this.startNode = new NoteNode();
        this.graph.addNode(this.startNode, new Point2D.Double(10, 10));
        INode endNode = new NoteNode();
        this.graph.addNode(endNode, new Point2D.Double(300, 10));
        this.edge = new SegmentedLineEdge()
        {
        };
        this.edge.setTransitionPoints(new ITransitionPoint[] { new EdgeTransitionPoint(150, 200) });
        assertTrue(this.graph.connect(this.edge, this.startNode, new Point2D.Double(15, 15), endNode, new Point2D.Double(305, 15)));
    }

    @Test
    void shouldRestoreTransitionPointsMovedWithDraggedNodes()
    {
        // What DragSelectedBehavior does when both ends of the edge are dragged
        this.behavior.onMousePressed(mouseEvent(MouseEvent.MOUSE_PRESSED, 15, 15));
        this.behavior.onMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, 65, 35));
        this.startNode.translate(50, 20);
        ITransitionPoint point = this.edge.getTransitionPoints()[0];
        point.setX(200);
        point.setY(220);
        this.behavior.onMouseReleased(mouseEvent(MouseEvent.MOUSE_RELEASED, 65, 35));

        this.behavior.undo();
        assertEquals(new Point2D.Double(150, 200), this.edge.getTransitionPoints()[0].toPoint2D());
        assertEquals(new Point2D.Double(10, 10), this.startNode.getLocation());

        this.behavior.redo();
        assertEquals(new Point2D.Double(200, 220), this.edge.getTransitionPoints()[0].toPoint2D());
    }

    @Test
    void shouldKeepHistoryWhenRestoredPointIsDraggedAgain()
    {
        moveTransitionPoint(new Point2D.Double(160, 210));
        this.behavior.undo();
        // A new drag of the restored point mustn't change the history
        moveTransitionPoint(new Point2D.Double(170, 230));

        this.behavior.undo();
        assertEquals(new Point2D.Double(150, 200), this.edge.getTransitionPoints()[0].toPoint2D());
        this.behavior.redo();
        assertEquals(new Point2D.Double(170, 230), this.edge.getTransitionPoints()[0].toPoint2D());
    }

    private void moveTransitionPoint(Point2D location)
    {
        this.behavior.beforeChangingTransitionPointsOnEdge(this.edge);
        ITransitionPoint point = this.edge.getTransitionPoints()[0];
        point.setX(location.getX());
        point.setY(location.getY());
        this.behavior.afterChangingTransitionPointsOnEdge(this.edge);
    }

    private MouseEvent mouseEvent(int id, int x, int y)
    {
        return new MouseEvent(this.swingComponent, id, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
    }

    private IEditorPart newEditorPart()
    {
        return (IEditorPart) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IEditorPart.class },
                (proxy, method, args) -> switch (method.getName())
                {
                    case "getGraph" -> this.graph;
                    case "getSelectionHandler" -> this.selectionHandler;
                    case "getSwingComponent" -> this.swingComponent;
                    case "getZoomFactor" -> 1.0;
                    default -> null;
                });
    }
}
