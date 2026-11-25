package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.ZoomByPinchBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.ZoomByWheelBehavior;

/**
 * The drawing area keeps a margin around the elements on all sides. Nothing moves on screen when it grows, and the view never stays
 * beyond this margin.
 */
class DrawingAreaTest
{
    @BeforeEach
    void setUp() throws Exception
    {
        this.graph = new ClassDiagramGraph();
        this.node = new ClassNode();
        this.graph.addNode(this.node, new Point2D.Double(20, 20));
        this.editorPart = new EditorPart(this.graph);
        this.scrollPane = new JScrollPane(this.editorPart);
        this.scrollPane.setSize(800, 600);
        layOut();
    }

    private void layOut() throws Exception
    {
        for (int i = 0; i < 3; i++)
        {
            SwingUtilities.invokeAndWait(() -> {
                this.scrollPane.doLayout();
                this.scrollPane.getViewport().doLayout();
            });
            SwingUtilities.invokeAndWait(() -> { }); // resize events
        }
    }

    @Test
    void shouldShowAMarginWhenOpened()
    {
        assertEquals(new Point(0, 0), getViewport().getViewPosition());
        assertEquals(new Point2D.Double(-40, -40), this.editorPart.toGraphPoint(new Point()));
        assertMarginOnAllSides();
    }

    @Test
    void shouldGrowOnAllSidesWithoutMovingOnScreen() throws Exception
    {
        this.graph.addNode(new ClassNode(), new Point2D.Double(1500, 1500));
        for (Point location : new Point[] { new Point(-500, -300), new Point(3000, 2000), new Point(-4000, 20) })
        {
            this.node.setLocation(location);
            Point2D before = getTopLeftOfView();
            drag();
            assertEquals(before, getTopLeftOfView());
            assertMarginOnAllSides();
            layOut();
            assertEquals(before, getTopLeftOfView());
        }
    }

    @Test
    void shouldKeepPointsWhenZoomed() throws Exception
    {
        addFarNodes();
        this.node.setLocation(new Point(-500, -300));
        drag();
        for (int steps : new int[] { 3, -6, 4 })
        {
            Point2D before = getCenterOfView();
            SwingUtilities.invokeAndWait(() -> this.editorPart.changeZoom(steps));
            assertTrue(before.distance(getCenterOfView()) < 2);
        }
        assertMarginOnAllSides();
        Point2D point = new Point2D.Double(-123, 45);
        assertTrue(point.distance(this.editorPart.toGraphPoint(this.editorPart.toComponentPoint(point))) < 1e-6);
    }

    @Test
    void shouldFollowBothFingersOfAPinch() throws Exception
    {
        addFarNodes();
        ZoomByPinchBehavior behavior = new ZoomByPinchBehavior(this.editorPart);
        Point start = getViewport().getViewPosition();
        start.translate(600, 120);
        Point2D pinched = this.editorPart.toGraphPoint(start);
        SwingUtilities.invokeAndWait(() -> behavior.startPinch(start, 100));
        double[][] moves = { { 500, 200, 150, 1.5 }, { 300, 300, 40, 0.4 }, { 320, 300, 101, 1 }, { 320, 300, 5000, 8 } };
        for (double[] move : moves)
        {
            Point target = getViewport().getViewPosition();
            target.translate((int) move[0], (int) move[1]);
            SwingUtilities.invokeAndWait(() -> behavior.movePinch(target, move[2]));
            layOut();
            assertEquals(move[3], this.editorPart.getZoomFactor(), 1e-9);
            Point after = getViewport().getViewPosition();
            after.translate((int) move[0], (int) move[1]);
            assertTrue(pinched.distance(this.editorPart.toGraphPoint(after)) < 2);
        }
    }

    @Test
    void shouldIgnoreAPinchWhileAnElementIsHeld() throws Exception
    {
        ZoomByPinchBehavior behavior = new ZoomByPinchBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(behavior);
        SwingUtilities.invokeAndWait(() -> {
            this.editorPart.dispatchEvent(new MouseEvent(this.editorPart, MouseEvent.MOUSE_PRESSED, 0,
                    InputEvent.BUTTON1_DOWN_MASK, 200, 200, 1, false, MouseEvent.BUTTON1));
            behavior.startPinch(new Point(200, 200), 100);
            behavior.movePinch(new Point(200, 200), 200);
        });
        assertEquals(1, this.editorPart.getZoomFactor());
    }

    @Test
    void shouldZoomAroundTheMouseWithTheWheel() throws Exception
    {
        addFarNodes();
        this.editorPart.getBehaviorManager().addBehavior(new ZoomByWheelBehavior(this.editorPart));
        for (int rotation : new int[] { -1, -1, 1, 1, 1 })
        {
            Point mouse = getViewport().getViewPosition();
            mouse.translate(600, 120);
            Point2D before = this.editorPart.toGraphPoint(mouse);
            double zoom = this.editorPart.getZoomFactor();
            SwingUtilities.invokeAndWait(() -> this.editorPart.dispatchEvent(
                    new MouseWheelEvent(this.editorPart, MouseEvent.MOUSE_WHEEL, 0, InputEvent.CTRL_DOWN_MASK, mouse.x, mouse.y, 0,
                            false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation)));
            assertEquals(rotation < 0, this.editorPart.getZoomFactor() > zoom);
            layOut();
            Point after = getViewport().getViewPosition();
            after.translate(600, 120);
            assertTrue(before.distance(this.editorPart.toGraphPoint(after)) < 1);
        }
    }

    @Test
    void shouldScrollToShowWithRoomAround() throws Exception
    {
        this.node.setLocation(new Point(-500, -300));
        Rectangle2D bounds = this.node.getBounds();
        SwingUtilities.invokeAndWait(() -> this.editorPart.ensureVisible(bounds));
        Point2D topLeft = getTopLeftOfView();
        assertEquals(new Point2D.Double(bounds.getX() - 40, bounds.getY() - 40), topLeft);
    }

    @Test
    void shouldLeaveNothingHiddenBeyondAnElementDroppedAtTheBorder() throws Exception
    {
        for (Point location : new Point[] { new Point(-60, -50), new Point(-60, 650), new Point(850, -100) })
        {
            this.node.setLocation(location);
            drag();
            SwingUtilities.invokeAndWait(() -> this.editorPart.ensureVisible(this.node.getBounds()));
            layOut();
            assertEquals(new Point(0, 0), getViewport().getViewPosition());
            assertEquals(getViewport().getExtentSize(), this.editorPart.getSize());
            assertMarginOnAllSides();
        }
    }

    @Test
    void shouldScrollUpToTheBorderOfALargeDiagram() throws Exception
    {
        this.graph.addNode(new ClassNode(), new Point2D.Double(3000, 2000));
        layOut();
        this.node.setLocation(new Point(-100, -100));
        drag();
        SwingUtilities.invokeAndWait(() -> this.editorPart.ensureVisible(this.node.getBounds()));
        layOut();
        assertEquals(new Point(0, 0), getViewport().getViewPosition());
        assertEquals(new Point2D.Double(-140, -140), getTopLeftOfView());
    }

    @Test
    void shouldBringTheLifelinesBackToTheTopOnceNothingIsAbove() throws Exception
    {
        SequenceDiagramGraph sequence = new SequenceDiagramGraph();
        this.editorPart = new EditorPart(sequence);
        this.scrollPane.setViewportView(this.editorPart);
        layOut();
        Point2D top = new Point2D.Double(-40, -10);
        assertEquals(top, getTopOfArea());
        sequence.addNode(new LifelineNode(), new Point2D.Double(50, 0));
        NoteNode title = new NoteNode();
        sequence.addNode(title, new Point2D.Double(400, 100));
        drag();
        assertEquals(top, getTopOfArea());

        for (boolean isRemoved : new boolean[] { false, true })
        {
            title.setLocation(new Point2D.Double(400, -300));
            drag();
            SwingUtilities.invokeAndWait(() -> this.editorPart.ensureVisible(title.getBounds()));
            layOut();
            assertEquals(-310, getTopLeftOfView().getY());
            assertEquals(-310, getTopOfArea().getY());

            // Moved back down, the diagram doesn't move until the release
            title.setLocation(new Point2D.Double(400, 100));
            SwingUtilities.invokeAndWait(() -> dispatch(MouseEvent.MOUSE_PRESSED));
            SwingUtilities.invokeAndWait(() -> dispatch(MouseEvent.MOUSE_DRAGGED));
            assertEquals(-310, getTopLeftOfView().getY());
            if (isRemoved)
            {
                sequence.removeNode(title);
            }
            SwingUtilities.invokeAndWait(() -> dispatch(MouseEvent.MOUSE_RELEASED));
            layOut();
            assertEquals(top, getTopOfArea());
            assertEquals(0, getViewport().getViewPosition().y);
        }

        for (int steps : new int[] { -4, 4 })
        {
            SwingUtilities.invokeAndWait(() -> this.editorPart.changeZoom(steps));
            assertEquals(-10 / this.editorPart.getZoomFactor(), getTopOfArea().getY(), 1e-9);
        }
    }

    private Point2D getTopOfArea()
    {
        Rectangle2D area = this.editorPart.getDrawingArea();
        return new Point2D.Double(area.getX(), area.getY());
    }

    @Test
    void shouldComeBackToTheElementsLeft() throws Exception
    {
        for (Point location : new Point[] { new Point(-900, -700), new Point(3000, 2000) })
        {
            ClassNode removed = new ClassNode();
            this.graph.addNode(removed, new Point2D.Double(location.x, location.y));
            drag();
            SwingUtilities.invokeAndWait(() -> this.editorPart.ensureVisible(removed.getBounds()));
            layOut();
            assertEquals(location.x < 0 ? location.x - 40 : -20, getTopOfArea().getX());
            this.graph.removeNode(removed);
            drag();
            layOut();
            drag(); // scroll bars gone
            assertEquals(new Point2D.Double(-40, -40), getTopOfArea());
            assertEquals(new Point2D.Double(-40, -40), getTopLeftOfView());
            assertEquals(getViewport().getExtentSize(), this.editorPart.getSize());
        }
    }

    @Test
    void shouldNotMoveADiagramInThePositiveCoordinates() throws Exception
    {
        for (Point location : new Point[] { new Point(300, 200), new Point(0, 0), new Point(400, 10) })
        {
            this.node.setLocation(location);
            drag();
            layOut();
            assertEquals(new Point2D.Double(-40, -40), getTopLeftOfView());
        }
    }

    /**
     * So that the borders of the drawing area are far from the view
     */
    private void addFarNodes() throws Exception
    {
        this.graph.addNode(new ClassNode(), new Point2D.Double(-9000, -9000));
        this.graph.addNode(new ClassNode(), new Point2D.Double(9000, 9000));
        drag();
        layOut();
    }

    private void drag() throws Exception
    {
        SwingUtilities.invokeAndWait(this::dispatchDrag);
    }

    private void dispatchDrag()
    {
        for (int id : new int[] { MouseEvent.MOUSE_DRAGGED, MouseEvent.MOUSE_RELEASED })
        {
            dispatch(id);
        }
    }

    private void dispatch(int id)
    {
        this.editorPart.dispatchEvent(new MouseEvent(this.editorPart, id, 0, 0, 5, 5, 1, false, MouseEvent.BUTTON1));
    }

    private void assertMarginOnAllSides()
    {
        Rectangle2D area = this.editorPart.getDrawingArea();
        Rectangle2D bounds = this.node.getBounds();
        double margin = 40 / this.editorPart.getZoomFactor() - 1;
        Rectangle2D expected = new Rectangle2D.Double(bounds.getX() - margin, bounds.getY() - margin,
                bounds.getWidth() + 2 * margin, bounds.getHeight() + 2 * margin);
        assertTrue(area.contains(expected), area + " / " + expected);
    }

    private Point2D getTopLeftOfView()
    {
        return this.editorPart.toGraphPoint(getViewport().getViewPosition());
    }

    private Point2D getCenterOfView()
    {
        Rectangle view = getViewport().getViewRect();
        return this.editorPart.toGraphPoint(new Point2D.Double(view.getCenterX(), view.getCenterY()));
    }

    private JViewport getViewport()
    {
        return this.scrollPane.getViewport();
    }

    private ClassDiagramGraph graph;
    private ClassNode node;
    private EditorPart editorPart;
    private JScrollPane scrollPane;
}
