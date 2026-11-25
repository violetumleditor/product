package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

import com.horstmann.violet.workspace.editorpart.EditorPart;

/**
 * Zooms with two fingers on a touch screen: the diagram follows both fingers, so it also moves with them. Java has no multi-touch
 * event: the page gives the position of the fingers (browser version).
 * <p>
 * The first finger may have pressed the mouse before the second one came. If it only holds the diagram to move it, the pinch
 * replaces this gesture, which is consumed until its release; if it does anything else (an element is dragged...), the pinch is
 * ignored. Comes first in the behaviors.
 */
public class ZoomByPinchBehavior extends AbstractEditorPartBehavior
{

    public ZoomByPinchBehavior(EditorPart editorPart)
    {
        this.editorPart = editorPart;
    }

    /**
     * Starts a pinch
     *
     * @param center the middle of both fingers, on the Swing component of the editor part
     * @param distance between both fingers
     */
    public void startPinch(Point2D center, double distance)
    {
        this.startPointOnGraph = null;
        List<DragGraphBehavior> dragBehaviors = this.editorPart.getBehaviorManager().getBehaviors(DragGraphBehavior.class);
        if (this.isMousePressed && !this.isGestureConsumed && dragBehaviors.stream().noneMatch(b -> b.isHoldingDiagram()))
        {
            return;
        }
        dragBehaviors.forEach(b -> b.cancel());
        this.isGestureConsumed = this.isMousePressed;
        this.startPointOnGraph = this.editorPart.toGraphPoint(center);
        this.startDistance = Math.max(MIN_DISTANCE, distance);
        this.startZoom = this.editorPart.getZoomFactor();
    }

    /**
     * Follows the fingers
     *
     * @param center the middle of both fingers, on the Swing component of the editor part
     * @param distance between both fingers
     */
    public void movePinch(Point2D center, double distance)
    {
        if (this.startPointOnGraph == null)
        {
            return;
        }
        double zoom = this.startZoom * Math.max(MIN_DISTANCE, distance) / this.startDistance;
        // A zoom already beyond the limits (buttons) doesn't jump
        zoom = Math.max(Math.min(MIN_ZOOM, this.startZoom), Math.min(Math.max(MAX_ZOOM, this.startZoom), zoom));
        if (Math.abs(zoom - 1) < REAL_SIZE_TOLERANCE)
        {
            zoom = 1;
        }
        this.editorPart.setZoomFactor(zoom, this.startPointOnGraph, center);
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        this.isMousePressed = true;
        this.isGestureConsumed = false;
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        consume(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
        this.isMousePressed = false;
        consume(event);
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        consume(event);
    }

    private void consume(MouseEvent event)
    {
        if (this.isGestureConsumed)
        {
            event.consume();
        }
    }

    private static final double MIN_ZOOM = 0.1;

    private static final double MAX_ZOOM = 8;

    /** So that the real size is easily found back */
    private static final double REAL_SIZE_TOLERANCE = 0.03;

    /** Between both fingers, in pixels */
    private static final double MIN_DISTANCE = 10;

    private final EditorPart editorPart;

    /** The point of the graph between both fingers, or null if there is no pinch or if it is ignored */
    private Point2D startPointOnGraph;

    private double startDistance;

    private double startZoom;

    private boolean isMousePressed;

    /** True if a pinch replaced the gesture started by the last press */
    private boolean isGestureConsumed;

}
