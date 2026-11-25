package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.userpreferences.LaunchingPreferences;
import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * Moves the view of the diagram by holding it on an empty spot: double tap (or double click) and drag without lifting the second
 * tap, with the selection tool, so that a plain drag still draws the lasso. Ctrl + drag on an empty spot does it at once, whatever
 * the tool, as before. The same with the finger (CheerpJ gives it to Java as a mouse) and the mouse.
 * <p>
 * On a touch screen (web page on a tablet or a phone, launch argument <code>-touchScreen</code>), there is no lasso: a plain drag
 * on an empty spot with the selection tool moves the view, as the finger does in any other application.
 * <p>
 * While the diagram is held, the mouse cursor is the standard move cursor (a touch screen shows none). Once released, the view
 * keeps going for a while (inertia). A double tap without moving still shows the node choice of AddNodeByTapBehavior. Comes
 * before AddNodeByTapBehavior in the behaviors, as that one consumes the second press, made on its "+".
 */
public class DragGraphBehavior extends AbstractEditorPartBehavior
{

    public DragGraphBehavior(Workspace workspace)
    {
        BeanInjector.getInjector().inject(this);
        this.workspace = workspace;
        this.editorPart = workspace.getEditorPart();
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        getInertiaTimer().stop();
        release();
        if (event.getButton() != MouseEvent.BUTTON1)
        {
            return;
        }
        boolean isCtrl = KeyModifierUtil.isCtrl(event);
        boolean isDoubleTap = event.getClickCount() == 2 && isSelectionTool();
        boolean isFingerDrag = this.launchingPreferences.isTouchScreen() && event.getClickCount() == 1 && isSelectionTool();
        if (!isCtrl && !isDoubleTap && !isFingerDrag)
        {
            return;
        }
        if (BehaviorUtils.isCursorOnResizePoint(this.editorPart, event))
        {
            return;
        }
        Point2D mousePointOnGraph = this.editorPart.toGraphPoint(event.getPoint());
        IGraph graph = this.editorPart.getGraph();
        if (graph.findNode(mousePointOnGraph) != null || graph.findEdge(mousePointOnGraph) != null)
        {
            return;
        }
        JScrollPane scrollPane = getScrollPane();
        this.pressPoint = event.getLocationOnScreen();
        this.pressScrollPosition = new Point(scrollPane.getHorizontalScrollBar().getValue(), scrollPane.getVerticalScrollBar().getValue());
        // On the diagram itself: its own cursor (set by the resize behaviors...) hides the one of the scroll pane
        Component diagram = this.editorPart.getSwingComponent();
        this.initialCursor = diagram.getCursor();
        diagram.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        getSamples().clear();
        addSample(event);
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        if (this.pressPoint == null)
        {
            return;
        }
        // On screen: the view moves under the finger, so the point stays still in the editor's coordinates
        Point point = event.getLocationOnScreen();
        JScrollPane scrollPane = getScrollPane();
        scrollPane.getHorizontalScrollBar().setValue(this.pressScrollPosition.x - (point.x - this.pressPoint.x));
        scrollPane.getVerticalScrollBar().setValue(this.pressScrollPosition.y - (point.y - this.pressPoint.y));
        addSample(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        if (this.pressPoint == null)
        {
            return;
        }
        startInertia(event);
        release();
    }

    /**
     * Lets the diagram go: former cursor back
     */
    private void release()
    {
        if (this.pressPoint == null)
        {
            return;
        }
        this.editorPart.getSwingComponent().setCursor(this.initialCursor);
        this.pressPoint = null;
        this.initialCursor = null;
    }

    /**
     * @return true while the diagram is held
     */
    public boolean isHoldingDiagram()
    {
        return this.pressPoint != null;
    }

    private boolean isSelectionTool()
    {
        return GraphTool.SELECTION_TOOL.equals(this.workspace.getSideBar().getGraphToolsBar().getSelectedTool());
    }

    private JScrollPane getScrollPane()
    {
        return this.workspace.getAWTComponent().getScrollableEditorPart();
    }

    /**
     * Keeps the positions of the last 100 ms, to know the speed of the finger when it leaves the screen
     */
    private void addSample(MouseEvent event)
    {
        Deque<long[]> samples = getSamples();
        Point point = event.getLocationOnScreen();
        samples.addLast(new long[] { event.getWhen(), point.x, point.y });
        while (samples.size() > 2 && event.getWhen() - samples.getFirst()[0] > SAMPLE_DURATION)
        {
            samples.removeFirst();
        }
    }

    private void startInertia(MouseEvent event)
    {
        Deque<long[]> samples = getSamples();
        if (samples.size() < 2 || event.getWhen() - samples.getLast()[0] > INERTIA_MAX_PAUSE)
        {
            return; // the finger stopped before leaving the screen
        }
        long[] first = samples.getFirst();
        long[] last = samples.getLast();
        long duration = last[0] - first[0];
        if (duration <= 0)
        {
            return;
        }
        double velocityX = -(double) (last[1] - first[1]) / duration;
        double velocityY = -(double) (last[2] - first[2]) / duration;
        double speed = Math.hypot(velocityX, velocityY);
        if (speed < MIN_VELOCITY)
        {
            return;
        }
        if (speed > MAX_VELOCITY)
        {
            velocityX = velocityX * MAX_VELOCITY / speed;
            velocityY = velocityY * MAX_VELOCITY / speed;
        }
        JScrollPane scrollPane = getScrollPane();
        this.inertiaVelocityX = velocityX;
        this.inertiaVelocityY = velocityY;
        this.inertiaX = scrollPane.getHorizontalScrollBar().getValue();
        this.inertiaY = scrollPane.getVerticalScrollBar().getValue();
        this.inertiaLastTime = System.nanoTime();
        getInertiaTimer().start();
    }

    private void onInertiaTick()
    {
        long now = System.nanoTime();
        double elapsed = Math.min(MAX_TICK, (now - this.inertiaLastTime) / 1e6);
        this.inertiaLastTime = now;
        double slowDown = Math.exp(-elapsed / INERTIA_TIME_CONSTANT);
        this.inertiaVelocityX *= slowDown;
        this.inertiaVelocityY *= slowDown;
        this.inertiaX += this.inertiaVelocityX * elapsed;
        this.inertiaY += this.inertiaVelocityY * elapsed;
        JScrollPane scrollPane = getScrollPane();
        boolean isMoving = moveScrollBar(scrollPane.getHorizontalScrollBar(), this.inertiaX);
        isMoving |= moveScrollBar(scrollPane.getVerticalScrollBar(), this.inertiaY);
        if (!isMoving || Math.hypot(this.inertiaVelocityX, this.inertiaVelocityY) < MIN_VELOCITY)
        {
            getInertiaTimer().stop();
        }
    }

    /**
     * @return false once the scroll bar is at an end it can't go beyond
     */
    private boolean moveScrollBar(JScrollBar scrollBar, double value)
    {
        int newValue = (int) Math.round(value);
        int max = scrollBar.getMaximum() - scrollBar.getVisibleAmount();
        scrollBar.setValue(newValue);
        return newValue > scrollBar.getMinimum() && newValue < max;
    }

    private Timer getInertiaTimer()
    {
        if (this.inertiaTimer == null)
        {
            this.inertiaTimer = new Timer(INERTIA_TICK, e -> onInertiaTick());
        }
        return this.inertiaTimer;
    }

    private Deque<long[]> getSamples()
    {
        if (this.samples == null)
        {
            this.samples = new ArrayDeque<long[]>();
        }
        return this.samples;
    }

    /** Positions kept to measure the speed of the finger, in ms */
    private static final long SAMPLE_DURATION = 100;

    /** Pause before release, in ms, after which the view doesn't keep going */
    private static final long INERTIA_MAX_PAUSE = 80;

    /** Speeds in pixels per ms */
    private static final double MIN_VELOCITY = 0.05;
    private static final double MAX_VELOCITY = 5;

    /** Slowing down of the inertia, in ms (the speed is divided by e every 325 ms, as on phones) */
    private static final double INERTIA_TIME_CONSTANT = 325;

    private static final int INERTIA_TICK = 15;
    private static final double MAX_TICK = 50;

    private final Workspace workspace;

    private final IEditorPart editorPart;

    @InjectedBean
    private LaunchingPreferences launchingPreferences;

    private Timer inertiaTimer;

    private Deque<long[]> samples;

    /** Where the press holding the diagram was, on screen, or null if the diagram isn't held */
    private Point pressPoint;

    private Point pressScrollPosition;

    private Cursor initialCursor;

    private double inertiaVelocityX;

    private double inertiaVelocityY;

    private double inertiaX;

    private double inertiaY;

    private long inertiaLastTime;

}
