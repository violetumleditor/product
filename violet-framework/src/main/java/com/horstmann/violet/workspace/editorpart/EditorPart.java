/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.workspace.editorpart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.product.diagram.abstracts.GeometryCache;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.behavior.IEditorPartBehavior;

/**
 * Graph editor
 */
public class EditorPart extends JPanel implements IEditorPart
{

    /**
     * Default constructor
     * 
     * @param aGraph graph which will be drawn in this editor part
     */
    public EditorPart(IGraph aGraph)
    {
        this.graph = aGraph;
        this.zoom = 1;
        this.grid = new PlainGrid(this);
        this.graph.setGridSticker(grid.getGridSticker());
        addMouseListener(new MouseAdapter()
        {

            public void mousePressed(MouseEvent event)
            {
                isMousePressed = true;
                getViewAnimation().stop();
                behaviorManager.fireOnMousePressed(event);
            }

            public void mouseReleased(MouseEvent event)
            {
                behaviorManager.fireOnMouseReleased(event);
                isMousePressed = false;
                updateDrawingArea(true);
            }

            public void mouseClicked(MouseEvent event)
            {
                behaviorManager.fireOnMouseClicked(event);
            }
        });

        addMouseWheelListener(new MouseWheelListener()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                behaviorManager.fireOnMouseWheelMoved(e);
                // Ctrl + wheel zooms; otherwise the wheel scrolls the diagram, which a listener here would prevent
                if (!KeyModifierUtil.isCtrl(e))
                {
                    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, EditorPart.this);
                    if (scrollPane != null)
                    {
                        scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(EditorPart.this, e, scrollPane));
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter()
        {
            public void mouseDragged(MouseEvent event)
            {
                behaviorManager.fireOnMouseDragged(event);
                updateDrawingArea();
            }

            @Override
            public void mouseMoved(MouseEvent event)
            {
                behaviorManager.fireOnMouseMoved(event);
            }
        });
        addComponentListener(new ComponentAdapter() {
        	@Override
        	public void componentResized(ComponentEvent e) {
        		updateDrawingArea();
        	}
		});
        setBounds(0, 0, 0, 0);
        setDoubleBuffered(false);
        // Its only children, those of the in-place text editor (EditTextInPlaceBehavior), are placed on the text it edits; the
        // preferred size of the diagram is set by setDrawingArea, never computed by a layout
        setLayout(null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IEditorPart#getGraph()
     */
    public IGraph getGraph()
    {
        return this.graph;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IEditorPart#removeSelected()
     */
    public void removeSelected()
    {
        this.behaviorManager.fireBeforeRemovingSelectedElements();
        try
        {
            List<ISelectable> selectedElements = selectionHandler.getSelectedElements();
            List<IEdge> edges = new ArrayList<IEdge>(selectedElements.stream().filter(e -> IEdge.class.isInstance(e)).map(e -> (IEdge) e).toList());
            List<INode> nodes = selectedElements.stream().filter(e -> INode.class.isInstance(e)).map(e -> (INode) e).toList();
            List<ITransitionPoint> transitionPoints = selectedElements.stream().filter(e -> ITransitionPoint.class.isInstance(e)).map(e -> (ITransitionPoint) e).toList();
            
            // Remove only transition points and bypass concerned edges
            for (ITransitionPoint aTransitionPoint : transitionPoints) {
            	List<IEdge> edgesWithTransitionPointToRemove = new ArrayList<IEdge>(edges.stream().filter(e -> Arrays.asList(e.getTransitionPoints()).contains(aTransitionPoint)).toList());
            	for (IEdge edgeContainingTransitionPointToRemove : edgesWithTransitionPointToRemove) {
            		this.behaviorManager.fireBeforeChangingTransitionPointsOnEdge(edgeContainingTransitionPointToRemove);
            		edgeContainingTransitionPointToRemove.removeTransitionPoint(aTransitionPoint);
            		this.behaviorManager.fireAfterChangingTransitionPointsOnEdge(edgeContainingTransitionPointToRemove);
            		edges.remove(edgeContainingTransitionPointToRemove);
            	}
            }
            
            // Remove other nodes and edges
            graph.removeNode(nodes.stream().toArray(INode[]::new));
            graph.removeEdge(edges.stream().toArray(IEdge[]::new));
        }
        finally
        {
            this.selectionHandler.clearSelection();
            this.behaviorManager.fireAfterRemovingSelectedElements();
        }
    }


    /**
     * Fits the drawing area to the elements, with a margin around them on all sides, and to what is visible, so that nothing moves
     * on screen. While the mouse is pressed, it only grows.
     */
    private void updateDrawingArea()
    {
        updateDrawingArea(false);
    }

    /**
     * @param isVisiblePartFitted true to also move the visible part if it shows more than the elements and the margin around them
     *        ({@link #fitVisiblePart}), unless the mouse is pressed
     */
    private void updateDrawingArea(boolean isVisiblePartFitted)
    {
        JViewport viewport = getViewport();
        if (viewport == null || this.isDrawingAreaFrozen)
        {
            return;
        }
        Rectangle visible = viewport.getViewRect();
        if (visible.isEmpty())
        {
            return; // not laid out yet
        }
        visible.translate(this.origin.x, this.origin.y);
        Rectangle area = this.isMousePressed && this.isDrawingAreaInitialized ? getContentAreaWhileDragging() : getContentArea();
        if (!this.isDrawingAreaInitialized)
        {
            this.isDrawingAreaInitialized = true;
            initializeDrawingArea(area, visible.getSize());
            return;
        }
        if (isVisiblePartFitted && !this.isMousePressed && !getViewAnimation().isRunning())
        {
            // Not during a drag: the diagram would move under the mouse
            Rectangle fitted = new Rectangle(visible);
            fitVisiblePart(fitted, area);
            if (!fitted.equals(visible))
            {
                moveView(fitted.getLocation());
                return;
            }
        }
        area = addVisiblePart(area, visible);
        if (this.isMousePressed)
        {
            area.add(new Rectangle(this.origin, getSize()));
        }
        setDrawingArea(area, visible.getLocation());
    }

    /**
     * A diagram just opened is shown from its top left corner
     */
    private void initializeDrawingArea(Rectangle contentArea, Dimension visibleSize)
    {
        Rectangle area = addTopLeft(contentArea);
        area.add(new Rectangle(area.getLocation(), visibleSize));
        setDrawingArea(area, area.getLocation());
    }

    /**
     * Moves the visible part so that it shows no more room around the elements than the margin, as far as they fill it: the place of
     * a removed element, or what a zoom out shows beyond the borders. On the left and at the top, it may show up to the coordinate 0:
     * an element moved in the positive coordinates of a diagram shown from there moves nothing else.
     *
     * @param visible the visible part, moved by this method
     */
    private void fitVisiblePart(Rectangle visible, Rectangle contentArea)
    {
        Rectangle area = addTopLeft(contentArea);
        visible.x = Math.max(area.x, Math.min(visible.x, area.x + area.width - visible.width));
        visible.y = Math.max(area.y, Math.min(visible.y, area.y + area.height - visible.height));
    }

    /**
     * @return this area, extended to the coordinate 0 with its margin
     */
    private Rectangle addTopLeft(Rectangle contentArea)
    {
        Rectangle area = new Rectangle(contentArea);
        area.add(new Point(-DRAWING_AREA_MARGIN, -this.graph.getTopMargin()));
        return area;
    }

    /**
     * @return the elements with the margin around them, in pixels (zoomed graph coordinates)
     */
    private Rectangle getContentArea()
    {
        Rectangle area = getElementsArea();
        int left = area.x - DRAWING_AREA_MARGIN;
        int top = area.y - this.graph.getTopMargin();
        int right = area.x + area.width + DRAWING_AREA_MARGIN;
        int bottom = area.y + area.height + DRAWING_AREA_MARGIN;
        return new Rectangle(left, top, right - left, bottom - top);
    }

    /**
     * While the mouse is pressed the drawing area only grows (it already holds what was there at the press): when nodes are
     * dragged in a diagram drawn in layers, only the moving elements may make it grow, and they alone are measured at each step
     * instead of the whole diagram
     *
     * @return the moving elements with the margin around them, or all of them as {@link #getContentArea()}, in pixels (zoomed
     *         graph coordinates)
     */
    private Rectangle getContentAreaWhileDragging()
    {
        Predicate<Object> isMoving = getMovingElementsFilter();
        if (isMoving == null)
        {
            return getContentArea();
        }
        Rectangle2D bounds = null;
        GeometryCache.begin();
        try
        {
            for (INode aNode : this.graph.getAllNodes())
            {
                if (aNode.getParent() == null && isMoving.test(aNode))
                {
                    bounds = add(bounds, aNode.getBounds());
                    bounds = add(bounds, aNode.getOutsideLabelBounds());
                }
            }
            for (IEdge anEdge : this.graph.getAllEdges())
            {
                if (isMoving.test(anEdge))
                {
                    bounds = add(bounds, anEdge.getBounds());
                }
            }
        }
        finally
        {
            GeometryCache.end();
        }
        if (bounds == null)
        {
            return getContentArea();
        }
        Rectangle area = new Rectangle2D.Double(bounds.getX() * this.zoom, bounds.getY() * this.zoom, bounds.getWidth() * this.zoom,
                bounds.getHeight() * this.zoom).getBounds();
        int left = area.x - DRAWING_AREA_MARGIN;
        int top = area.y - this.graph.getTopMargin();
        int right = area.x + area.width + DRAWING_AREA_MARGIN;
        int bottom = area.y + area.height + DRAWING_AREA_MARGIN;
        return new Rectangle(left, top, right - left, bottom - top);
    }

    private static Rectangle2D add(Rectangle2D bounds, Rectangle2D other)
    {
        if (other == null)
        {
            return bounds;
        }
        if (bounds == null)
        {
            return new Rectangle2D.Double(other.getX(), other.getY(), other.getWidth(), other.getHeight());
        }
        bounds.add(other);
        return bounds;
    }

    /**
     * @return the elements, in pixels (zoomed graph coordinates)
     */
    private Rectangle getElementsArea()
    {
        if (this.graph.getAllNodes().isEmpty())
        {
            return new Rectangle();
        }
        Rectangle2D bounds = this.graph.getClipBounds();
        return new Rectangle2D.Double(bounds.getX() * this.zoom, bounds.getY() * this.zoom, bounds.getWidth() * this.zoom,
                bounds.getHeight() * this.zoom).getBounds();
    }

    /**
     * @param area in pixels (zoomed graph coordinates)
     * @param viewLocation the top left corner of the visible part, in the same coordinates
     */
    private void setDrawingArea(Rectangle area, Point viewLocation)
    {
        JViewport viewport = getViewport();
        Point viewPosition = new Point(viewLocation.x - area.x, viewLocation.y - area.y);
        boolean isOriginChanged = !area.getLocation().equals(this.origin);
        if (!isOriginChanged && area.getSize().equals(getSize()) && area.getSize().equals(getPreferredSize())
                && viewPosition.equals(viewport.getViewPosition()))
        {
            return;
        }
        // All at once, so that a point of the screen stays on the same point of the graph for the next mouse event
        this.origin = area.getLocation();
        setPreferredSize(area.getSize());
        viewport.setViewSize(area.getSize());
        viewport.setViewPosition(viewPosition);
        invalidate();
        repaint();
        if (isOriginChanged)
        {
            firePropertyChange(DRAWING_AREA_PROPERTY, null, getDrawingArea());
        }
    }

    /**
     * @return both areas, with the room of each scroll bar no longer needed: the diagram would move on screen when the bar
     *         disappears, if nothing lay under it
     */
    private Rectangle addVisiblePart(Rectangle contentArea, Rectangle visible)
    {
        Rectangle area = contentArea.union(visible);
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        if (scrollPane == null)
        {
            return area;
        }
        Insets insets = scrollPane.getInsets();
        Rectangle visibleWithoutBars = new Rectangle(visible);
        if (area.height <= visible.height)
        {
            visibleWithoutBars.width = scrollPane.getWidth() - insets.left - insets.right;
        }
        if (area.width <= visible.width)
        {
            visibleWithoutBars.height = scrollPane.getHeight() - insets.top - insets.bottom;
        }
        return area.union(visibleWithoutBars);
    }

    private JViewport getViewport()
    {
        return getParent() instanceof JViewport ? (JViewport) getParent() : null;
    }

    /**
     * @return the timer which fits the drawing area once the diagram stays still: whatever changes an element (undo, paste,
     *         keyboard...) paints the diagram, and so does a scroll
     */
    private Timer getDrawingAreaTimer()
    {
        if (this.drawingAreaTimer == null)
        {
            this.drawingAreaTimer = new Timer(DRAWING_AREA_DELAY, e -> {
                if (isScrollBarHeld())
                {
                    getDrawingAreaTimer().restart(); // its thumb would jump
                    return;
                }
                updateDrawingArea(true);
            });
            this.drawingAreaTimer.setRepeats(false);
        }
        return this.drawingAreaTimer;
    }

    private boolean isScrollBarHeld()
    {
        JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
        return scrollPane != null && (scrollPane.getHorizontalScrollBar().getValueIsAdjusting()
                || scrollPane.getVerticalScrollBar().getValueIsAdjusting());
    }

    @Override
    public Point2D toGraphPoint(Point2D point)
    {
        return new Point2D.Double((point.getX() + this.origin.x) / this.zoom, (point.getY() + this.origin.y) / this.zoom);
    }

    @Override
    public Point2D toComponentPoint(Point2D point)
    {
        return new Point2D.Double(point.getX() * this.zoom - this.origin.x, point.getY() * this.zoom - this.origin.y);
    }

    @Override
    public Rectangle2D getDrawingArea()
    {
        return new Rectangle2D.Double(this.origin.x / this.zoom, this.origin.y / this.zoom, getWidth() / this.zoom,
                getHeight() / this.zoom);
    }

    @Override
    public void setDrawingAreaFrozen(boolean isFrozen)
    {
        this.isDrawingAreaFrozen = isFrozen;
        getViewAnimation().stop();
        updateDrawingArea();
    }

    @Override
    public boolean isDrawingAreaFrozen()
    {
        return this.isDrawingAreaFrozen;
    }

    @Override
    public void ensureVisible(Rectangle2D boundsOnGraph)
    {
        JViewport viewport = getViewport();
        if (viewport == null)
        {
            return;
        }
        updateDrawingArea();
        Rectangle visible = viewport.getViewRect();
        visible.translate(this.origin.x, this.origin.y);
        if (getViewAnimation().isRunning())
        {
            visible.setLocation(this.animationEnd);
        }
        Rectangle bounds = new Rectangle2D.Double(boundsOnGraph.getX() * this.zoom, boundsOnGraph.getY() * this.zoom,
                boundsOnGraph.getWidth() * this.zoom, boundsOnGraph.getHeight() * this.zoom).getBounds();
        bounds.grow(DRAWING_AREA_MARGIN, DRAWING_AREA_MARGIN);
        bounds = addNearBorders(bounds).intersection(getContentArea().union(visible));
        moveView(new Point(getScrolledPosition(visible.x, visible.width, bounds.x, bounds.width),
                getScrolledPosition(visible.y, visible.height, bounds.y, bounds.height)));
    }

    /**
     * @return the position of the visible part, on one axis, moved just enough to show something; not moved if this is larger
     */
    private int getScrolledPosition(int visiblePosition, int visibleSize, int position, int size)
    {
        if (size > visibleSize)
        {
            return visiblePosition;
        }
        return Math.max(position + size - visibleSize, Math.min(visiblePosition, position));
    }

    /**
     * Moves the visible part, with a short animation so that the eye follows the elements. Whatever else moves it meanwhile, or a
     * press, stops the animation.
     *
     * @param location its new top left corner, in pixels (zoomed graph coordinates)
     */
    private void moveView(Point location)
    {
        getViewAnimation().stop();
        Point current = getViewLocation();
        if (!isShowing() || location.equals(current))
        {
            showViewAt(location);
            return;
        }
        this.animationStart = current;
        this.animationEnd = location;
        this.animationLocation = current;
        this.animationStartTime = System.nanoTime();
        getViewAnimation().start();
    }

    private void onViewAnimationTick()
    {
        if (getViewport() == null || !getViewLocation().equals(this.animationLocation))
        {
            getViewAnimation().stop();
            return;
        }
        double progress = Math.min(1, (System.nanoTime() - this.animationStartTime) / (ANIMATION_DURATION * 1e6));
        double eased = 1 - Math.pow(1 - progress, 3);
        if (progress == 1)
        {
            getViewAnimation().stop();
        }
        showViewAt(new Point((int) Math.round(this.animationStart.x + (this.animationEnd.x - this.animationStart.x) * eased),
                (int) Math.round(this.animationStart.y + (this.animationEnd.y - this.animationStart.y) * eased)));
        this.animationLocation = getViewLocation();
    }

    private void showViewAt(Point location)
    {
        Rectangle visible = new Rectangle(location, getViewport().getExtentSize());
        setDrawingArea(addVisiblePart(getContentArea(), visible), location);
    }

    /**
     * @return the top left corner of the visible part, in pixels (zoomed graph coordinates)
     */
    private Point getViewLocation()
    {
        Point location = getViewport().getViewPosition();
        location.translate(this.origin.x, this.origin.y);
        return location;
    }

    private Timer getViewAnimation()
    {
        if (this.viewAnimation == null)
        {
            this.viewAnimation = new Timer(ANIMATION_TICK, e -> onViewAnimationTick());
            this.viewAnimation.setInitialDelay(0);
        }
        return this.viewAnimation;
    }

    /**
     * Scrolls up to the border of the drawing area rather than leaving a narrow strip of it hidden (what an edge or a text takes
     * around the element): the next scroll would move the whole diagram by these few pixels.
     *
     * @param bounds in pixels (zoomed graph coordinates)
     */
    private Rectangle addNearBorders(Rectangle bounds)
    {
        Rectangle content = getContentArea();
        int left = getNearBorder(bounds.x, content.x, -1);
        int top = getNearBorder(bounds.y, content.y, -1);
        int right = getNearBorder(bounds.x + bounds.width, content.x + content.width, 1);
        int bottom = getNearBorder(bounds.y + bounds.height, content.y + content.height, 1);
        return new Rectangle(left, top, right - left, bottom - top);
    }

    private int getNearBorder(int position, int border, int direction)
    {
        int distance = (border - position) * direction;
        return distance > 0 && distance <= DRAWING_AREA_MARGIN ? border : position;
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IEditorPart#changeZoom(int)
     */
    public void changeZoom(int steps)
    {
        Rectangle visible = getViewport() == null ? new Rectangle() : getViewport().getViewRect();
        changeZoom(steps, new Point2D.Double(visible.getCenterX(), visible.getCenterY()));
    }

    @Override
    public void changeZoom(int steps, Point2D center)
    {
        final double FACTOR = Math.sqrt(Math.sqrt(2));
        double newZoom = zoom;
        for (int i = 1; i <= steps; i++)
            newZoom *= FACTOR;
        for (int i = 1; i <= -steps; i++)
            newZoom /= FACTOR;
        // The center of the zoom stays at the same place in the view
        setZoomFactor(newZoom, toGraphPoint(center), center);
    }

    /**
     * Sets the zoom and moves the view, so that a given point of the graph is shown at a given place. The Swing component then
     * fires a {@link #ZOOM_PROPERTY} property change.
     *
     * @param newZoom the new zoom factor
     * @param pointOnGraph the point of the graph to show at this place
     * @param target this place, on the Swing component as it is before the change
     */
    public void setZoomFactor(double newZoom, Point2D pointOnGraph, Point2D target)
    {
        double oldZoom = zoom;
        JViewport viewport = getViewport();
        getViewAnimation().stop();
        zoom = newZoom;
        if (viewport != null)
        {
            Point viewPosition = viewport.getViewPosition();
            double xInView = target.getX() - viewPosition.x;
            double yInView = target.getY() - viewPosition.y;
            Point newViewLocation = new Point((int) Math.round(pointOnGraph.getX() * zoom - xInView),
                    (int) Math.round(pointOnGraph.getY() * zoom - yInView));
            Rectangle area = new Rectangle(newViewLocation, viewport.getExtentSize());
            Rectangle contentArea = getContentArea();
            fitVisiblePart(area, contentArea);
            setDrawingArea(addVisiblePart(contentArea, area), area.getLocation());
        }
        invalidate();
        repaint();
        firePropertyChange(ZOOM_PROPERTY, oldZoom, zoom);
    }

    @Override
    public double getZoomFactor()
    {
        return this.zoom;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.product.workspace.editorpart.IEditorPart#getGrid()
     */
    public IGrid getGrid()
    {
        return this.grid;
    }

    public JComponent getSwingComponent()
    {
        return this;
    }
    
    
    @Override
    public void paintImmediately(int x, int y, int w, int h)
    {
        getSwingComponent().invalidate();
        super.paintImmediately(x, y, w, h);
    }
    
    
   
    @Override
    public boolean isOptimizedDrawingEnabled()
    {
        return getComponentCount() < 2; // the toolbar of the in-place text editor may lie on its text
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        boolean valid = getSwingComponent().isValid();
        // With the in-place text editor on it, a repaint may come from this transparent child (each blink of its caret): the
        // diagram under it must then be painted again, else the former image stays and both mix
        if (valid && getComponentCount() == 0)
        {
            return;
        }
        getSwingComponent().revalidate(); // to inform parent scrollpane container
        ITheme theme = ThemeManager.getInstance().getTheme();
        // A copy: the children are painted with the same graphics, the zoom must not be left on it
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            Predicate<Object> isMoving = getMovingElementsFilter();
            Graphics2D diagramGraphics;
            if (isMoving != null)
            {
                // During a drag: what doesn't move comes from an image drawn once, only the moving elements are drawn again
                paintStaticLayer(g2, theme, isMoving);
                diagramGraphics = getDiagramGraphics(g2, theme, false);
                this.graph.draw(diagramGraphics, isMoving);
            }
            else
            {
                this.staticLayer = null;
                diagramGraphics = getDiagramGraphics(g2, theme, true);
                this.graph.draw(diagramGraphics);
            }
            for (IEditorPartBehavior behavior : this.behaviorManager.getBehaviors())
            {
                behavior.onPaint(diagramGraphics);
            }
        }
        finally
        {
            g2.dispose();
        }
        getDrawingAreaTimer().restart();
    }

    /**
     * Prepares the graphics to draw the graph: background and grid (if asked), origin of the drawing area, zoom, theme
     *
     * @param g2 the graphics of this component, changed by this method
     * @return the graphics to draw the graph with
     */
    private Graphics2D getDiagramGraphics(Graphics2D g2, ITheme theme, boolean isBackgroundPainted)
    {
        if (isBackgroundPainted)
        {
            g2.setColor(theme.getDiagramBackgroundColor());
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.translate(-this.origin.x, -this.origin.y);
        g2.scale(this.zoom, this.zoom);
        if (isBackgroundPainted)
        {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            this.grid.paint(g2);
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Graphics2D diagramGraphics = theme.getDiagramGraphics(g2);
        diagramGraphics.setColor(Color.BLACK);
        return diagramGraphics;
    }

    @Override
    public void setMovingNodes(Collection<INode> nodes)
    {
        this.movingNodes = nodes == null || nodes.isEmpty() ? Collections.<INode> emptyList() : new ArrayList<INode>(nodes);
        this.staticLayer = null;
        this.staticLayerKey = null;
    }

    /**
     * @return what is drawn again at each step of a drag: the moving nodes, their ancestors (which may grow) and descendants, the
     *         edges linked to them and the selected ones (their transition points move), and the edges sharing a node with these
     *         ones (their ends are spread along the same sides); null when everything is drawn as usual
     */
    private Predicate<Object> getMovingElementsFilter()
    {
        if (this.movingNodes.isEmpty() || !this.graph.isDrawnInLayers() || !GeometryCache.isEnabled() || getComponentCount() > 0)
        {
            return null;
        }
        Set<Object> moving = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (INode aNode : this.movingNodes)
        {
            addWithDescendants(aNode, moving);
            for (INode aParent = aNode.getParent(); aParent != null; aParent = aParent.getParent())
            {
                moving.add(aParent);
            }
        }
        Set<INode> neighbors = Collections.newSetFromMap(new IdentityHashMap<INode, Boolean>());
        List<IEdge> movingEdges = new ArrayList<IEdge>();
        List<ISelectable> selection = this.selectionHandler.getSelectedElements();
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (moving.contains(anEdge.getStartNode()) || moving.contains(anEdge.getEndNode()) || selection.contains(anEdge))
            {
                movingEdges.add(anEdge);
                neighbors.add(anEdge.getStartNode());
                neighbors.add(anEdge.getEndNode());
            }
        }
        moving.addAll(movingEdges);
        for (IEdge anEdge : this.graph.getAllEdges())
        {
            if (neighbors.contains(anEdge.getStartNode()) || neighbors.contains(anEdge.getEndNode()))
            {
                moving.add(anEdge);
            }
        }
        return moving::contains;
    }

    private static void addWithDescendants(INode node, Set<Object> nodes)
    {
        nodes.add(node);
        for (INode aChild : node.getChildren())
        {
            addWithDescendants(aChild, nodes);
        }
    }

    /**
     * Paints the background, the grid and the elements that don't move from an image of the visible part, drawn again only when
     * the view, the zoom, the theme or the moving nodes change
     *
     * @param g2 the graphics of this component, left unchanged
     */
    private void paintStaticLayer(Graphics2D g2, ITheme theme, Predicate<Object> isMoving)
    {
        // The whole view, whatever the part repainted now (the clip), so that the image serves the next steps too
        JViewport viewport = getViewport();
        Rectangle visible = viewport != null ? viewport.getViewRect() : g2.getClipBounds();
        if (visible == null)
        {
            visible = new Rectangle(0, 0, getWidth(), getHeight());
        }
        AffineTransform baseTransform = g2.getTransform();
        // In device pixels, so that the image is copied pixel for pixel, whatever the scale of the screen
        Rectangle deviceArea = baseTransform.createTransformedShape(visible).getBounds();
        if (deviceArea.isEmpty())
        {
            return;
        }
        List<Object> key = Arrays.asList(new Point(this.origin), this.zoom, visible, baseTransform, theme, this.graph);
        if (this.staticLayer == null || !key.equals(this.staticLayerKey))
        {
            // Opaque: the background covers it whole, and an opaque image is copied faster
            BufferedImage image = new BufferedImage(deviceArea.width, deviceArea.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D imageGraphics = image.createGraphics();
            try
            {
                imageGraphics.translate(-deviceArea.x, -deviceArea.y);
                imageGraphics.transform(baseTransform);
                imageGraphics.setClip(visible);
                Graphics2D diagramGraphics = getDiagramGraphics(imageGraphics, theme, true);
                this.graph.draw(diagramGraphics, isMoving.negate());
            }
            finally
            {
                imageGraphics.dispose();
            }
            this.staticLayer = image;
            this.staticLayerKey = key;
        }
        Graphics2D copyGraphics = (Graphics2D) g2.create();
        try
        {
            copyGraphics.setTransform(new AffineTransform());
            copyGraphics.drawImage(this.staticLayer, deviceArea.x, deviceArea.y, null);
        }
        finally
        {
            copyGraphics.dispose();
        }
    }

    @Override
    public IEditorPartSelectionHandler getSelectionHandler()
    {
        return this.selectionHandler;
    }

    @Override
    public IEditorPartBehaviorManager getBehaviorManager()
    {
        return this.behaviorManager;
    }

    
    

    
    private IGraph graph;

    /** The nodes being dragged, see {@link #setMovingNodes(Collection)} */
    private List<INode> movingNodes = Collections.emptyList();

    /** The elements that don't move during the current drag, drawn once on the visible part, or null */
    private BufferedImage staticLayer;

    /** What the static layer was drawn for: view, zoom, screen transform, theme, graph */
    private List<Object> staticLayerKey;

    private IGrid grid;

    private double zoom;

    private IEditorPartSelectionHandler selectionHandler = new EditorPartSelectionHandler();
    
    /** Where the top left corner of this component is on the diagram, in pixels (zoomed graph coordinates) */
    private Point origin = new Point();

    private boolean isDrawingAreaInitialized;

    private boolean isDrawingAreaFrozen;

    private boolean isMousePressed;

    private Timer drawingAreaTimer;

    /** Runs while the visible part moves, see {@link #moveView(Point)} */
    private Timer viewAnimation;

    private Point animationStart;

    private Point animationEnd;

    /** Where the last tick left the visible part */
    private Point animationLocation;

    private long animationStartTime;

    /**
     * Room kept around the elements, in pixels, also around an element scrolled into view: the same, so that nothing of the drawing
     * area stays hidden beyond an element just dropped at its border
     */
    private static final int DRAWING_AREA_MARGIN = IGraph.DEFAULT_MARGIN;

    /** In ms */
    private static final int DRAWING_AREA_DELAY = 150;

    /** In ms */
    private static final int ANIMATION_DURATION = 220;

    /** In ms */
    private static final int ANIMATION_TICK = 15;

    private IEditorPartBehaviorManager behaviorManager = new EditorPartBehaviorManager();
    


}