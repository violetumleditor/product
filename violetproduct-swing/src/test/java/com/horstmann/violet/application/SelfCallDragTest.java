package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * Two self calls on the same activation bar: the second callee, once dragged, still draws its loop next to it
 */
class SelfCallDragTest
{

    private SequenceDiagramGraph graph;

    private LifelineNode lifeline;

    private ActivationBarNode caller;

    private DragSelectedBehavior dragBehavior;

    @BeforeAll
    static void registerBeans()
    {
        if (BeanFactory.getFactory().contains(IFileChooserService.class))
        {
            return;
        }
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, new JFileChooserService());
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());
    }

    @BeforeEach
    void createDiagram()
    {
        this.graph = new SequenceDiagramGraph();
        this.lifeline = new LifelineNode();
        assertTrue(this.graph.addNode(this.lifeline, new Point2D.Double(100, 0)));
        this.caller = new ActivationBarNode();
        assertTrue(this.graph.addNode(this.caller, new Point2D.Double(130, 90)));
        assertSame(this.lifeline, this.caller.getParent());
        EditorPart editorPart = new EditorPart(this.graph);
        editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.dragBehavior = new DragSelectedBehavior(editorPart, null);
        paint();
    }

    @Test
    void shouldKeepTheLoopNextToTheSecondCalleeOnceDragged()
    {
        CallEdge firstCall = selfCall(0);
        CallEdge secondCall = selfCall(60);
        ActivationBarNode secondCallee = (ActivationBarNode) secondCall.getEndNode();
        assertSame(this.caller, secondCallee.getParent());
        assertLoopNextToCallee(firstCall);
        assertLoopNextToCallee(secondCall);

        // Grabbed on its right half (outside its caller), moved down along the lifeline
        Point2D calleeLocation = secondCallee.getLocationOnGraph();
        Point2D start = new Point2D.Double(calleeLocation.getX() + secondCallee.getBounds().getWidth() - 2, calleeLocation.getY() + 10);
        drag(start, new Point2D.Double(start.getX(), start.getY() + 40));

        assertSame(this.caller, secondCallee.getParent());
        assertLoopNextToCallee(firstCall);
        assertLoopNextToCallee(secondCall);
    }

    @Test
    void shouldKeepTheLoopNextToACalleeMovedIntoTheLifeline()
    {
        CallEdge call = selfCall(0);
        ActivationBarNode callee = (ActivationBarNode) call.getEndNode();
        Point2D locationOnGraph = callee.getLocationOnGraph();
        this.graph.changeParent(callee, this.lifeline, this.lifeline.getChildren().size());
        callee.setLocation(new Point2D.Double(locationOnGraph.getX() - this.lifeline.getLocationOnGraph().getX(),
                locationOnGraph.getY() - this.lifeline.getLocationOnGraph().getY()));
        paint();

        assertLoopNextToCallee(call);
    }

    @Test
    void shouldLeaveTheLoopBelowTheTopOfTheCaller()
    {
        // Started at the very top of the caller (relative location)
        Point2D startPoint = new Point2D.Double(8, 0);
        CallEdge edge = new CallEdge();
        assertTrue(this.graph.connect(edge, this.caller, startPoint, null, startPoint));
        paint();
        assertLoopNextToCallee(edge);
        double loopStartY = edge.getPoints().get(0).getY();
        assertEquals(this.caller.getLocationOnGraph().getY() + ActivationBarNode.CALL_YGAP / 2, loopStartY, 0.001);

        // Dragged up against its caller's top
        ActivationBarNode callee = (ActivationBarNode) edge.getEndNode();
        Point2D calleeLocation = callee.getLocationOnGraph();
        Point2D start = new Point2D.Double(calleeLocation.getX() + 4, calleeLocation.getY() + 10);
        drag(start, new Point2D.Double(start.getX(), start.getY() - 60));
        assertEquals(loopStartY, edge.getPoints().get(0).getY(), 0.001);
    }

    private CallEdge selfCall(double yFromTop)
    {
        Point2D location = this.caller.getLocationOnGraph();
        Point2D startPoint = new Point2D.Double(location.getX() + 8, location.getY() + 10 + yFromTop);
        CallEdge edge = new CallEdge();
        assertTrue(this.graph.connect(edge, this.caller, startPoint, null, startPoint));
        paint();
        return edge;
    }

    /** The loop leaves its caller just above the callee and comes back to the callee's top */
    private void assertLoopNextToCallee(CallEdge edge)
    {
        INode callee = edge.getEndNode();
        double calleeTop = callee.getLocationOnGraph().getY();
        List<Point2D> points = edge.getPoints();
        assertEquals(4, points.size());
        double startY = points.get(0).getY();
        double endY = points.get(3).getY();
        assertEquals(calleeTop, endY, 0.001);
        assertTrue(startY < endY && endY - startY <= ActivationBarNode.CALL_YGAP, "loop from " + startY + " to " + endY);
    }

    private void drag(Point2D start, Point2D target)
    {
        send(MouseEvent.MOUSE_PRESSED, start);
        Point2D middle = new Point2D.Double((start.getX() + target.getX()) / 2, (start.getY() + target.getY()) / 2);
        send(MouseEvent.MOUSE_DRAGGED, middle);
        paint();
        send(MouseEvent.MOUSE_DRAGGED, target);
        paint();
        send(MouseEvent.MOUSE_RELEASED, target);
        paint();
    }

    private void send(int id, Point2D p)
    {
        MouseEvent event = new MouseEvent(new JPanel(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) p.getX(), (int) p.getY(), 1, false,
                MouseEvent.BUTTON1);
        switch (id)
        {
        case MouseEvent.MOUSE_PRESSED:
            this.dragBehavior.onMousePressed(event);
            break;
        case MouseEvent.MOUSE_DRAGGED:
            this.dragBehavior.onMouseDragged(event);
            break;
        default:
            this.dragBehavior.onMouseReleased(event);
        }
    }

    /** As on screen: lifelines learn their common height, bars reset their location cache */
    private void paint()
    {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
