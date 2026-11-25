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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.util.KeyModifierUtil;
import com.horstmann.violet.framework.theme.ThemeManager;
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
        Rectangle area = getContentArea();
        if (isVisiblePartFitted && !this.isMousePressed) // not during a drag: the diagram would move under the mouse
        {
            fitVisiblePart(visible, area);
        }
        if (!this.isDrawingAreaInitialized)
        {
            this.isDrawingAreaInitialized = true;
            initializeDrawingArea(area, visible.getSize());
            return;
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
        Point2D location = toComponentPoint(new Point2D.Double(boundsOnGraph.getX(), boundsOnGraph.getY()));
        Rectangle bounds = new Rectangle2D.Double(location.getX(), location.getY(), boundsOnGraph.getWidth() * this.zoom,
                boundsOnGraph.getHeight() * this.zoom).getBounds();
        bounds.grow(DRAWING_AREA_MARGIN, DRAWING_AREA_MARGIN);
        bounds = addNearBorders(bounds).intersection(new Rectangle(viewport.getViewSize()));
        // On an axis where it is larger than the view, the view doesn't move
        Rectangle visible = viewport.getViewRect();
        if (bounds.width > visible.width)
        {
            bounds.x = visible.x;
            bounds.width = visible.width;
        }
        if (bounds.height > visible.height)
        {
            bounds.y = visible.y;
            bounds.height = visible.height;
        }
        scrollRectToVisible(bounds);
        updateDrawingArea(); // the room left behind is no longer needed, nor are its scroll bars
    }

    /**
     * Scrolls up to the border of the drawing area rather than leaving a narrow strip of it hidden (what an edge or a text takes
     * around the element): the next scroll would move the whole diagram by these few pixels.
     *
     * @param bounds on this component
     */
    private Rectangle addNearBorders(Rectangle bounds)
    {
        Rectangle content = getContentArea();
        content.translate(-this.origin.x, -this.origin.y);
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
            g2.setColor(theme.getDiagramBackgroundColor());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.translate(-this.origin.x, -this.origin.y);
            g2.scale(zoom, zoom);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            grid.paint(g2);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Graphics2D diagramGraphics = theme.getDiagramGraphics(g2);
            diagramGraphics.setColor(Color.BLACK);
            graph.draw(diagramGraphics);
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

    private IGrid grid;

    private double zoom;

    private IEditorPartSelectionHandler selectionHandler = new EditorPartSelectionHandler();
    
    /** Where the top left corner of this component is on the diagram, in pixels (zoomed graph coordinates) */
    private Point origin = new Point();

    private boolean isDrawingAreaInitialized;

    private boolean isDrawingAreaFrozen;

    private boolean isMousePressed;

    private Timer drawingAreaTimer;

    /**
     * Room kept around the elements, in pixels, also around an element scrolled into view: the same, so that nothing of the drawing
     * area stays hidden beyond an element just dropped at its border
     */
    private static final int DRAWING_AREA_MARGIN = IGraph.DEFAULT_MARGIN;

    /** In ms */
    private static final int DRAWING_AREA_DELAY = 150;

    private IEditorPartBehaviorManager behaviorManager = new EditorPartBehaviorManager();
    


}