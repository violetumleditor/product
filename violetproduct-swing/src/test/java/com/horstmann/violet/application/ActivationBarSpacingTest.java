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
import com.horstmann.violet.product.diagram.sequence.ReturnEdge;
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

    @Test
    void shouldBlockACallerWithItsCallee()
    {
        ActivationBarNode caller = addBar(this.lifeline, 200);
        ActivationBarNode above = addBar(this.otherLifeline, 120);
        ActivationBarNode callee = addBar(this.otherLifeline, 220);
        call(caller, callee);
        paint();
        double height = caller.getBounds().getHeight();

        // Room for 40: the callee follows, then both are blocked
        drag(caller, -90);
        assertBar(caller, 160, height);
        assertEquals(180, callee.getLocation().getY(), 0.001);
        assertEquals(120, above.getLocation().getY(), 0.001);

        this.undoRedoBehavior.undo();
        paint();
        assertBar(caller, 200, height);
        assertEquals(220, callee.getLocation().getY(), 0.001);
    }

    @Test
    void shouldShortenACallerBlockedAboveItsCallee()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode callee = addBar(this.otherLifeline, 160);
        call(caller, callee);
        paint();
        double calleeHeight = callee.getBounds().getHeight();

        // The caller is blocked by the head of its lifeline, then its callee by its top
        drag(callee, -60);
        assertEquals(80, caller.getLocation().getY(), 0.001);
        assertBar(callee, 100, calleeHeight);
        drag(callee, -60);
        assertEquals(80, caller.getLocation().getY(), 0.001);
        assertBar(callee, 90, calleeHeight);
    }

    /** The user's diagram */
    @Test
    void shouldMoveUpTheCalleesOfABlockedCallerWithNestedBars()
    {
        LifelineNode rightLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(rightLifeline, new Point2D.Double(500, 0)));
        ActivationBarNode above = addBar(this.lifeline, 110);
        ActivationBarNode root = addBar(this.lifeline, 170);
        addBar(root, 40);
        ActivationBarNode nested = addBar(root, 100);
        ActivationBarNode upper = addBar(this.otherLifeline, 280);
        ActivationBarNode middle = addBar(this.otherLifeline, 470);
        ActivationBarNode right = addBar(rightLifeline, 480);
        call(nested, upper);
        call(root, middle);
        call(middle, right);
        paint();
        assertSpace(above, root);
        double nestedHeight = nested.getBounds().getHeight();
        double rootHeight = root.getBounds().getHeight();
        double middleHeight = middle.getBounds().getHeight();

        drag(middle, -60);
        assertBar(middle, 410, middleHeight);
        assertEquals(420, right.getLocation().getY(), 0.001);
        assertBar(root, 170, rootHeight - 60);
        assertBar(nested, 100, nestedHeight);
        assertEquals(280, upper.getLocation().getY(), 0.001);

        drag(right, -40);
        assertEquals(380, right.getLocation().getY(), 0.001);
        assertBar(middle, 370, middleHeight);
        assertBar(root, 170, rootHeight - 100);

        // Both are blocked under the bar above the middle one (280 + 40 + 20)
        drag(right, -100);
        assertTrue(middle.getLocation().getY() >= nested.getLocationOnGraph().getY() + nested.getBounds().getHeight() + 10);
        assertBar(middle, 340, middleHeight);
        assertEquals(350, right.getLocation().getY(), 0.001);
        assertBar(nested, 100, nestedHeight);
        assertEquals(280, upper.getLocation().getY(), 0.001);
    }

    @Test
    void shouldKeepTheLengthOfABarBlockedBetweenItsCallerAndItsCallee()
    {
        LifelineNode thirdLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(thirdLifeline, new Point2D.Double(500, 0)));
        ActivationBarNode first = addBar(this.lifeline, 180);
        ActivationBarNode second = addBar(this.otherLifeline, 200);
        addBar(thirdLifeline, 120);
        ActivationBarNode third = addBar(thirdLifeline, 220);
        call(first, second);
        call(second, third);
        paint();
        double firstHeight = first.getBounds().getHeight();
        double secondHeight = second.getBounds().getHeight();

        drag(second, -100);
        assertBar(first, 140, firstHeight);
        assertBar(second, 160, secondHeight);
        assertEquals(180, third.getLocation().getY(), 0.001);
    }

    /** The user's diagram: the dragged bar calls the one that creates a lifeline used by the bar below */
    @Test
    void shouldNotGrowEndlesslyWhenTheCreationOfALifelineIsMovedDown()
    {
        LifelineNode created = new LifelineNode();
        assertTrue(this.graph.addNode(created, new Point2D.Double(500, 0)));
        ActivationBarNode first = addBar(this.lifeline, 110);
        ActivationBarNode creator = addBar(this.otherLifeline, 130);
        ActivationBarNode root = addBar(this.lifeline, 210);
        ActivationBarNode nested = addBar(root, 40);
        ActivationBarNode upper = addBar(this.otherLifeline, 260);
        ActivationBarNode middle = addBar(this.otherLifeline, 510);
        ActivationBarNode right = addBar(created, 400);
        call(first, creator);
        assertTrue(this.graph.connect(new CallEdge(), creator, new Point2D.Double(), created, new Point2D.Double(10, 10)));
        call(nested, upper);
        call(root, middle);
        call(middle, right);
        paint();
        double rootHeight = root.getBounds().getHeight();
        double middleHeight = middle.getBounds().getHeight();

        for (int dy : new int[] { 60, 300, 500 })
        {
            drag(first, dy);
            assertSpace(first, root);
            assertSpace(creator, middle);
            assertTrue(root.getBounds().getHeight() <= rootHeight + dy, "root: " + root.getBounds());
            assertTrue(middle.getBounds().getHeight() <= middleHeight + dy, "middle: " + middle.getBounds());
            this.undoRedoBehavior.undo();
            paint();
            assertBar(root, 210, rootHeight);
        }
    }

    /** Bars of two lifelines calling each other the opposite way round */
    @Test
    void shouldStayBoundedWhenTheOrdersOfTheLifelinesContradictEachOther()
    {
        ActivationBarNode first = addBar(this.lifeline, 100);
        ActivationBarNode second = addBar(this.lifeline, 200);
        ActivationBarNode firstCallee = addBar(this.otherLifeline, 400);
        ActivationBarNode secondCallee = addBar(this.otherLifeline, 300);
        call(first, firstCallee);
        call(second, secondCallee);
        paint();
        assertSpace(first, second);
        assertSpace(firstCallee, secondCallee);
        assertTrue(second.getBounds().getMaxY() < 1000, "second: " + second.getBounds());
    }

    @Test
    void shouldMoveANestedBarBelowTheReturnToItsParent()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode nested = addBar(caller, 40);
        ActivationBarNode callee = addBar(this.otherLifeline, 110);
        call(caller, callee);
        paint();
        assertEquals(40, nested.getLocation().getY(), 0.001);

        ReturnEdge reply = new ReturnEdge();
        assertTrue(this.graph.connect(reply, callee, new Point2D.Double(), caller, new Point2D.Double()));
        paint();
        // The return is at 150 on the graph, so 50 in the caller
        assertEquals(60, nested.getLocation().getY(), 0.001);
        assertTrue(caller.getBounds().getHeight() > nested.getBounds().getMaxY());
        // A bar after it keeps off it
        ActivationBarNode last = addBar(caller, 70);
        paint();
        assertSpace(nested, last);

        this.graph.removeEdge(reply);
        paint();
        assertEquals(40, nested.getLocation().getY(), 0.001);
    }

    @Test
    void shouldMoveANestedBarBelowACallOfItsParent()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode nested = addBar(caller, 40);
        ActivationBarNode callee = addBar(this.otherLifeline, 160);
        paint();
        CallEdge edge = new CallEdge();
        assertTrue(this.graph.connect(edge, caller, new Point2D.Double(), callee, new Point2D.Double()));
        paint();
        // The call is at 60 in the caller
        assertEquals(160, callee.getLocation().getY(), 0.001);
        assertEquals(70, nested.getLocation().getY(), 0.001);

        this.graph.removeEdge(edge);
        paint();
        assertEquals(40, nested.getLocation().getY(), 0.001);
    }

    @Test
    void shouldNotMoveASelfCalledBar()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode nested = addBar(caller, 30);
        call(caller, nested);
        paint();
        assertEquals(30, nested.getLocation().getY(), 0.001);
    }

    @Test
    void shouldMoveACallBelowANestedBarCallingAboveIt()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode nested = addBar(caller, 20);
        ActivationBarNode upper = addBar(this.otherLifeline, 130);
        ActivationBarNode lower = addBar(this.otherLifeline, 160);
        call(nested, upper);
        call(caller, lower);
        paint();
        assertEquals(20, nested.getLocation().getY(), 0.001);
        assertEquals(130, upper.getLocation().getY(), 0.001);
        double nestedBottom = nested.getLocationOnGraph().getY() + nested.getBounds().getHeight();
        assertEquals(nestedBottom + 10, lower.getLocation().getY(), 0.001);
    }

    @Test
    void shouldLeaveANestedBarOffTheReturn()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode callee = addBar(this.otherLifeline, 200);
        ActivationBarNode nested = addBar(caller, 20);
        ActivationBarNode callBack = addBar(caller, 110);
        call(caller, callee);
        call(callee, callBack);
        assertTrue(this.graph.connect(new ReturnEdge(), callee, new Point2D.Double(), caller, new Point2D.Double()));
        paint();
        assertEquals(20, nested.getLocation().getY(), 0.001);
        assertEquals(110, callBack.getLocation().getY(), 0.001);
    }

    @Test
    void shouldLeaveTheSameSpaceBelowTheReturnAsAboveTheCall()
    {
        ActivationBarNode caller = addBar(this.lifeline, 100);
        ActivationBarNode callee = addBar(this.otherLifeline, 110);
        call(caller, callee);
        assertTrue(this.graph.connect(new ReturnEdge(), callee, new Point2D.Double(), caller, new Point2D.Double()));
        paint();
        double topGap = callee.getLocationOnGraph().getY() - caller.getLocationOnGraph().getY();
        double bottomGap = caller.getBounds().getMaxY() - (callee.getLocationOnGraph().getY() + callee.getBounds().getHeight());
        assertEquals(10, topGap, 0.001);
        assertEquals(topGap, bottomGap, 0.001);

        // Another callee, lower: the space stays below the last one
        LifelineNode thirdLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(thirdLifeline, new Point2D.Double(500, 0)));
        ActivationBarNode lower = addBar(thirdLifeline, 190);
        call(caller, lower);
        paint();
        bottomGap = caller.getBounds().getMaxY() - (lower.getLocationOnGraph().getY() + lower.getBounds().getHeight());
        assertEquals(topGap, bottomGap, 0.001);
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
