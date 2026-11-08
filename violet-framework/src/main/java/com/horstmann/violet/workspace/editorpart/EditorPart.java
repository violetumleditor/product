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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

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
                behaviorManager.fireOnMousePressed(event);
            }

            public void mouseReleased(MouseEvent event)
            {
                behaviorManager.fireOnMouseReleased(event);
                changePreferredSize();
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
                changePreferredSize();
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
        		initializeDrawingArea();
        		changePreferredSize();
        	}
		});
        setBounds(0, 0, 0, 0);
        setDoubleBuffered(false);
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


    private void changePreferredSize()
    {
        Container parent = getParent();
        if (parent == null) {
        	return;
        }
		Dimension parentSize = parent.getSize();
        Rectangle2D bounds = graph.getClipBounds();
        int width = (int) (Math.max((int) (bounds.getMaxX()), (int) parentSize.getWidth()) * this.getZoomFactor());
        int height = (int) (Math.max((int) (bounds.getMaxY()), (int) parentSize.getHeight()) * this.getZoomFactor());
        if (this.lastWidth != width || this.lastHeight != height) {
            this.lastWidth = width;
            this.lastHeight = height;
        }
        setPreferredSize(new Dimension(width, height));
    }


    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IEditorPart#changeZoom(int)
     */
    public void changeZoom(int steps)
    {
        final double FACTOR = Math.sqrt(Math.sqrt(2));
        double oldZoom = zoom;
        for (int i = 1; i <= steps; i++)
            zoom *= FACTOR;
        for (int i = 1; i <= -steps; i++)
            zoom /= FACTOR;
        changePreferredSize();
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

    /*
     * (non-Javadoc)
     * 
     * @seecom.horstmann.violet.framework.workspace.editorpart.IEditorPart# growDrawingArea()
     */
    public void growDrawingArea()
    {
        IGraph g = getGraph();
        Rectangle2D bounds = g.getClipBounds();
        bounds.add(getBounds());
        g.setBounds(new Double(0, 0, GROW_SCALE_FACTOR * bounds.getWidth(), GROW_SCALE_FACTOR * bounds.getHeight()));
        invalidate();
        repaint();
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.horstmann.violet.framework.workspace.editorpart.IEditorPart# clipDrawingArea()
     */
    public void clipDrawingArea()
    {
        IGraph g = getGraph();
        g.setBounds(null);
        invalidate();
        repaint();
    }

    private void initializeDrawingArea() {
    	Dimension size = this.getSize();
    	getGraph().setBounds(new Rectangle2D.Double(0, 0, 0, 0));
    	Rectangle2D bounds = getGraph().getClipBounds();
		getGraph().setBounds(new Double(0, 0, Math.max(size.getWidth(), bounds.getWidth()), Math.max(size.getHeight(), bounds.getHeight())));
		invalidate();
        repaint();
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
    protected void paintComponent(Graphics g)
    {
        boolean valid = getSwingComponent().isValid();
        if (valid)
        {
            return;
        }
        getSwingComponent().revalidate(); // to inform parent scrollpane container
        ITheme theme = ThemeManager.getInstance().getTheme();
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(theme.getDiagramBackgroundColor());
        g2.fillRect(0, 0, getWidth(), getHeight());
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
    
    private int lastWidth = 0;
    
    private int lastHeight = 0;

    /**
     * Scale factor used to grow drawing area
     */
    private static final double GROW_SCALE_FACTOR = Math.sqrt(2);

    private IEditorPartBehaviorManager behaviorManager = new EditorPartBehaviorManager();
    


}