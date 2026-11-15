package com.horstmann.violet.framework.swingextension;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Scrolls a scroll pane vertically by dragging its content with the finger (or the mouse), wherever the press starts, a button
 * included, as on a phone: the scroll bar is too thin to be caught with a finger.
 * <p>
 * A short tap still acts on what lies under it. Past 8 px of mostly vertical movement, the gesture becomes a scroll: the pressed
 * button is disarmed and the content follows the finger, then keeps going for a while once released (inertia). A mostly horizontal
 * movement is left to the components, e.g. a tool dragged from the side bar onto the diagram. Components that react on press
 * cancel what they started when {@link #isScrollGesture()} becomes true; they find this object as the client property
 * {@link #CLIENT_PROPERTY} of the scroll pane.
 * <p>
 * CheerpJ gives the touch screen to Java as a mouse, so the same code serves the browser and the desktop. The mouse events are
 * watched through the toolkit, before the components get them, only while the scroll pane is displayable.
 */
public class DragScrollSupport
{

    /**
     * Client property of the scroll pane holding this object
     */
    public static final String CLIENT_PROPERTY = DragScrollSupport.class.getName();

    public DragScrollSupport(JScrollPane scrollPane)
    {
        this.scrollPane = scrollPane;
        scrollPane.putClientProperty(CLIENT_PROPERTY, this);
        scrollPane.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0)
            {
                updateRegistration();
            }
        });
        updateRegistration();
    }

    /**
     * @return true if the current gesture (or the last one, once released, until the next press) scrolls the content
     */
    public boolean isScrollGesture()
    {
        return this.state == State.SCROLLING;
    }

    /**
     * @return true if the current gesture moved far enough, mostly horizontally (or the content can't scroll), to be handled by the
     *         components; false while it may still be a tap or a scroll
     */
    public boolean isDragLeftToComponents()
    {
        return this.state == State.LEFT_TO_COMPONENTS;
    }

    private void updateRegistration()
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.removeAWTEventListener(getEventListener());
        if (this.scrollPane.isDisplayable())
        {
            toolkit.addAWTEventListener(getEventListener(), AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        }
        else
        {
            getInertiaTimer().stop();
        }
    }

    private AWTEventListener getEventListener()
    {
        if (this.eventListener == null)
        {
            this.eventListener = event -> {
                if (event instanceof MouseEvent)
                {
                    onMouseEvent((MouseEvent) event);
                }
            };
        }
        return this.eventListener;
    }

    private void onMouseEvent(MouseEvent event)
    {
        Component view = this.scrollPane.getViewport().getView();
        Component source = event.getComponent();
        if (view == null || source == null || !SwingUtilities.isDescendingFrom(source, view))
        {
            return;
        }
        switch (event.getID())
        {
        case MouseEvent.MOUSE_PRESSED:
            onPressed(event);
            break;
        case MouseEvent.MOUSE_DRAGGED:
            onDragged(event);
            break;
        case MouseEvent.MOUSE_RELEASED:
            onReleased(event);
            break;
        default:
            break;
        }
    }

    private void onPressed(MouseEvent event)
    {
        getInertiaTimer().stop();
        getSamples().clear();
        if (!SwingUtilities.isLeftMouseButton(event))
        {
            this.state = State.IDLE;
            return;
        }
        this.state = State.UNDECIDED;
        this.pressPoint = event.getLocationOnScreen();
        this.pressViewY = this.scrollPane.getViewport().getViewPosition().y;
        this.pressedComponent = event.getComponent();
        addSample(event);
    }

    private void onDragged(MouseEvent event)
    {
        if (this.state != State.UNDECIDED && this.state != State.SCROLLING)
        {
            return;
        }
        Point point = event.getLocationOnScreen();
        int dx = point.x - this.pressPoint.x;
        int dy = point.y - this.pressPoint.y;
        if (this.state == State.UNDECIDED)
        {
            if (Math.max(Math.abs(dx), Math.abs(dy)) <= DRAG_THRESHOLD)
            {
                return;
            }
            if (Math.abs(dy) <= Math.abs(dx) || getMaxViewY() == 0)
            {
                this.state = State.LEFT_TO_COMPONENTS;
                return;
            }
            this.state = State.SCROLLING;
            disarm(this.pressedComponent);
        }
        setViewY(this.pressViewY - dy);
        addSample(event);
    }

    private void onReleased(MouseEvent event)
    {
        this.pressedComponent = null;
        if (this.state != State.SCROLLING)
        {
            this.state = State.IDLE;
            return;
        }
        // The state stays SCROLLING until the next press, as the components get this release after this listener
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
        double velocity = -(double) (last[1] - first[1]) / duration;
        velocity = Math.max(-MAX_VELOCITY, Math.min(MAX_VELOCITY, velocity));
        if (Math.abs(velocity) > MIN_VELOCITY)
        {
            this.inertiaVelocity = velocity;
            this.inertiaY = this.scrollPane.getViewport().getViewPosition().y;
            this.inertiaLastTime = System.nanoTime();
            getInertiaTimer().start();
        }
    }

    /**
     * Keeps the positions of the last 100 ms, to know the speed of the finger when it leaves the screen
     */
    private void addSample(MouseEvent event)
    {
        Deque<long[]> samples = getSamples();
        samples.addLast(new long[] { event.getWhen(), event.getLocationOnScreen().y });
        while (samples.size() > 2 && event.getWhen() - samples.getFirst()[0] > SAMPLE_DURATION)
        {
            samples.removeFirst();
        }
    }

    private void onInertiaTick()
    {
        long now = System.nanoTime();
        double elapsed = Math.min(MAX_TICK, (now - this.inertiaLastTime) / 1e6);
        this.inertiaLastTime = now;
        this.inertiaVelocity *= Math.exp(-elapsed / INERTIA_TIME_CONSTANT);
        this.inertiaY += this.inertiaVelocity * elapsed;
        int maxViewY = getMaxViewY();
        if (this.inertiaY <= 0 || this.inertiaY >= maxViewY || Math.abs(this.inertiaVelocity) < MIN_VELOCITY)
        {
            getInertiaTimer().stop();
        }
        setViewY((int) Math.round(this.inertiaY));
    }

    /**
     * The pressed button must not act on release, even if the finger is still on it: the content moved with the finger
     */
    private void disarm(Component component)
    {
        if (component instanceof AbstractButton)
        {
            ButtonModel model = ((AbstractButton) component).getModel();
            model.setArmed(false);
            model.setPressed(false);
            model.setRollover(false);
        }
    }

    private int getMaxViewY()
    {
        JViewport viewport = this.scrollPane.getViewport();
        Component view = viewport.getView();
        if (view == null)
        {
            return 0;
        }
        Dimension extent = viewport.getExtentSize();
        return Math.max(0, view.getHeight() - extent.height);
    }

    private void setViewY(int y)
    {
        JViewport viewport = this.scrollPane.getViewport();
        Point position = viewport.getViewPosition();
        int newY = Math.max(0, Math.min(getMaxViewY(), y));
        if (newY != position.y)
        {
            viewport.setViewPosition(new Point(position.x, newY));
        }
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

    private enum State
    {
        /** No gesture, or one this object ignores (other button) */
        IDLE,
        /** Pressed, not moved far enough yet: still a tap */
        UNDECIDED,
        /** Mostly vertical movement: the content follows the finger */
        SCROLLING,
        /** Mostly horizontal movement: the components handle it */
        LEFT_TO_COMPONENTS
    }

    /** Movement, in pixels, beyond which a press is no longer a tap */
    private static final int DRAG_THRESHOLD = 8;

    /** Positions kept to measure the speed of the finger, in ms */
    private static final long SAMPLE_DURATION = 100;

    /** Pause before release, in ms, after which the content doesn't keep going */
    private static final long INERTIA_MAX_PAUSE = 80;

    /** Speeds in pixels per ms */
    private static final double MIN_VELOCITY = 0.05;
    private static final double MAX_VELOCITY = 5;

    /** Slowing down of the inertia, in ms (the speed is divided by e every 325 ms, as on phones) */
    private static final double INERTIA_TIME_CONSTANT = 325;

    private static final int INERTIA_TICK = 15;
    private static final double MAX_TICK = 50;

    private final JScrollPane scrollPane;
    private AWTEventListener eventListener;
    private Timer inertiaTimer;
    private Deque<long[]> samples;
    private State state = State.IDLE;
    private Point pressPoint;
    private int pressViewY;
    private Component pressedComponent;
    private double inertiaVelocity;
    private double inertiaY;
    private long inertiaLastTime;

}
