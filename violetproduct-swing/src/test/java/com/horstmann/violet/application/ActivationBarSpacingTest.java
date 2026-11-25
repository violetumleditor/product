package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

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
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * Activation bars of a same lifeline, or of a same activation bar, always keep a space between them
 */
class ActivationBarSpacingTest
{
    private SequenceDiagramGraph graph;

    private LifelineNode lifeline;

    private LifelineNode otherLifeline;

    private DragSelectedBehavior dragBehavior;

    private UndoRedoCompoundBehavior undoRedoBehavior;

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
        this.otherLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(this.lifeline, new Point2D.Double(100, 0)));
        assertTrue(this.graph.addNode(this.otherLifeline, new Point2D.Double(300, 0)));
        EditorPart editorPart = new EditorPart(this.graph);
        editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.dragBehavior = new DragSelectedBehavior(editorPart, null);
        this.undoRedoBehavior = new UndoRedoCompoundBehavior(editorPart);
    }

    @Test
    void shouldSeparateBarsOfALifeline()
    {
        ActivationBarNode first = addBar(this.lifeline, 100);
        ActivationBarNode second = addBar(this.lifeline, 110);
        ActivationBarNode third = addBar(this.lifeline, 140);
        paint();
        assertSpace(first, second);
        assertSpace(second, third);
        assertEquals(100, first.getLocation().getY(), 0.001);
    }

    @Test
    void shouldSeparateBarsOfABar()
    {
        ActivationBarNode parent = addBar(this.lifeline, 100);
        ActivationBarNode below = addBar(this.lifeline, 180);
        ActivationBarNode first = addBar(parent, 20);
        ActivationBarNode second = addBar(parent, 30);
        paint();
        assertSpace(first, second);
        assertTrue(parent.getBounds().getHeight() >= second.getBounds().getMaxY());
        assertSpace(parent, below);
    }

    @Test
    void shouldSeparateBarsLengthenedByACall()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode below = addBar(this.lifeline, 160);
        ActivationBarNode callee = addBar(this.otherLifeline, 130);
        ActivationBarNode calleeOfBelow = addBar(this.otherLifeline, 120);
        call(below, calleeOfBelow);
        call(caller, callee);
        paint();
        assertSpace(caller, below);
        assertSpace(callee, calleeOfBelow);
        assertTrue(calleeOfBelow.getLocationOnGraph().getY() > below.getLocationOnGraph().getY());
    }

    @Test
    void shouldLeaveABarBesideTheOneCallingIt()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode callee = addBar(this.lifeline, 120);
        call(caller, callee);
        paint();
        assertEquals(120, callee.getLocation().getY(), 0.001);
    }

    @Test
    void shouldCrossAnotherBarInBothDirections()
    {
        ActivationBarNode first = addBar(this.lifeline, 100);
        ActivationBarNode second = addBar(this.lifeline, 160);
        paint();
        drag(first, 90);
        assertSpace(second, first);
        assertEquals(160, second.getLocation().getY(), 0.001);

        drag(first, -120);
        assertSpace(first, second);
        assertEquals(160, second.getLocation().getY(), 0.001);
    }

    @Test
    void shouldPutAPushedBarBackOnUndo()
    {
        ActivationBarNode first = addBar(this.lifeline, 100);
        ActivationBarNode second = addBar(this.lifeline, 160);
        ActivationBarNode third = addBar(this.lifeline, 220);
        paint();
        drag(first, 30);
        assertSpace(first, second);
        assertSpace(second, third);
        double pushedY = third.getLocation().getY();
        assertTrue(pushedY > 220);

        this.undoRedoBehavior.undo();
        paint();
        assertEquals(100, first.getLocation().getY(), 0.001);
        assertEquals(160, second.getLocation().getY(), 0.001);
        assertEquals(220, third.getLocation().getY(), 0.001);

        this.undoRedoBehavior.redo();
        paint();
        assertEquals(130, first.getLocation().getY(), 0.001);
        assertEquals(pushedY, third.getLocation().getY(), 0.001);
    }

    @Test
    void shouldMoveTheCallersWithTheirCalleeInsteadOfResizingThem()
    {
        ActivationBarNode first = addBar(this.lifeline, 150);
        ActivationBarNode second = addBar(this.otherLifeline, 170);
        LifelineNode thirdLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(thirdLifeline, new Point2D.Double(500, 0)));
        ActivationBarNode third = addBar(thirdLifeline, 190);
        call(first, second);
        call(second, third);
        paint();
        double height = first.getBounds().getHeight();

        drag(third, 100);
        assertBar(first, 250, height);
        assertEquals(270, second.getLocation().getY(), 0.001);
        assertEquals(290, third.getLocation().getY(), 0.001);

        drag(third, -50);
        assertBar(first, 200, height);
        assertEquals(240, third.getLocation().getY(), 0.001);

        this.undoRedoBehavior.undo();
        this.undoRedoBehavior.undo();
        paint();
        assertBar(first, 150, height);
        assertEquals(190, third.getLocation().getY(), 0.001);
        this.undoRedoBehavior.redo();
        paint();
        assertBar(first, 250, height);
    }

    @Test
    void shouldResizeACallerKeptAboveItsOtherCallee()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode upper = addBar(this.otherLifeline, 130);
        ActivationBarNode lower = addBar(this.otherLifeline, 200);
        call(caller, upper);
        call(caller, lower);
        paint();

        drag(lower, 60);
        assertEquals(130, upper.getLocation().getY(), 0.001);
        assertEquals(260, lower.getLocation().getY(), 0.001);
        assertEquals(120, caller.getLocation().getY(), 0.001);
        assertTrue(caller.getBounds().getMaxY() >= lower.getBounds().getMaxY());
    }

    private void assertBar(INode bar, double y, double height)
    {
        assertEquals(y, bar.getLocation().getY(), 0.001);
        assertEquals(height, bar.getBounds().getHeight(), 0.001);
    }

    private ActivationBarNode addBar(INode parent, double y)
    {
        ActivationBarNode bar = new ActivationBarNode();
        assertTrue(parent.addChild(bar, new Point2D.Double(0, y)));
        return bar;
    }

    private void call(INode start, INode end)
    {
        assertTrue(this.graph.connect(new CallEdge(), start, start.getLocationOnGraph(), end, end.getLocationOnGraph()));
    }

    private void assertSpace(INode above, INode below)
    {
        double gap = below.getBounds().getY() - above.getBounds().getMaxY();
        assertTrue(gap >= ActivationBarNode.CALL_YGAP, "gap: " + gap);
    }

    private void drag(INode node, int dy)
    {
        Point2D location = node.getLocationOnGraph();
        double x = location.getX() + 8;
        double y = location.getY() + 10;
        send(MouseEvent.MOUSE_PRESSED, x, y);
        for (int i = 5; i <= Math.abs(dy); i += 5)
        {
            send(MouseEvent.MOUSE_DRAGGED, x, y + Math.signum(dy) * i);
            paint();
        }
        send(MouseEvent.MOUSE_RELEASED, x, y + dy);
        paint();
    }

    private void send(int id, double x, double y)
    {
        MouseEvent event = new MouseEvent(new JPanel(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) x, (int) y, 1, false,
                MouseEvent.BUTTON1);
        switch (id)
        {
        case MouseEvent.MOUSE_PRESSED:
            this.dragBehavior.onMousePressed(event);
            this.undoRedoBehavior.onMousePressed(event);
            break;
        case MouseEvent.MOUSE_DRAGGED:
            this.dragBehavior.onMouseDragged(event);
            this.undoRedoBehavior.onMouseDragged(event);
            break;
        default:
            this.dragBehavior.onMouseReleased(event);
            this.undoRedoBehavior.onMouseReleased(event);
        }
    }

    private void paint()
    {
        this.graph.draw(new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }
}
