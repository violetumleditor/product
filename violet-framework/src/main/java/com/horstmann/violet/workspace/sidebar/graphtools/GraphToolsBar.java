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

package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.SideBar;

/**
 * Node and edge prototypes of a graph, shown in two parts: the tools of the diagram type, and the tools common to all diagrams
 * (selection, notes, free text, images, grouping areas). Exactly one tool is selected at any time.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class GraphToolsBar implements IGraphToolsBar
{

    /**
     * Common tools, in display order
     */
    private static final List<Class<? extends INode>> COMMON_NODE_CLASSES = AbstractGraph.COMMON_NODE_CLASSES;

    public void install(IWorkspace workspace)
    {
        BeanInjector.getInjector().inject(this);
        ResourceBundleInjector.getInjector().inject(this);
        IEditorPart editorPart = workspace.getEditorPart();
        IGraph graph = editorPart.getGraph();
        this.diagramName = findDiagramName(graph);
        dispatchTools(graph);
        this.diagramPanel = new GraphToolsBarPanel(this);
        this.diagramPanel.addHeader(this.elementsTitle, this.diagramNodeTools);
        this.diagramPanel.addTools(this.diagramNodeTools);
        this.diagramPanel.addHeader(this.linksTitle, this.diagramEdgeTools);
        this.diagramPanel.addTools(this.diagramEdgeTools);
        this.commonPanel = new GraphToolsBarPanel(this);
        List<GraphTool> gridTools = new ArrayList<GraphTool>();
        gridTools.add(GraphTool.SELECTION_TOOL);
        gridTools.addAll(this.commonNodeTools);
        this.commonPanel.addToolGrid(gridTools, getShortLabels(gridTools));
        this.commonPanel.addTools(this.commonEdgeTools);
        reset();
        editorPart.getSelectionHandler().setSelectedTool(getSelectedTool());
    }

    /**
     * Splits the prototypes of the graph between its own tools and the common ones
     */
    private void dispatchTools(IGraph graph)
    {
        GraphTool[] commonNodes = new GraphTool[COMMON_NODE_CLASSES.size()];
        for (INode aNode : graph.getNodePrototypes())
        {
            GraphTool aTool = new GraphTool(aNode, aNode.getToolTip());
            int commonIndex = COMMON_NODE_CLASSES.indexOf(aNode.getClass());
            if (commonIndex >= 0 && commonNodes[commonIndex] == null)
            {
                commonNodes[commonIndex] = aTool;
            }
            else
            {
                this.diagramNodeTools.add(aTool);
            }
        }
        for (GraphTool aTool : commonNodes)
        {
            if (aTool != null)
            {
                this.commonNodeTools.add(aTool);
            }
        }
        for (IEdge anEdge : graph.getEdgePrototypes())
        {
            GraphTool aTool = new GraphTool(anEdge, anEdge.getToolTip());
            if (anEdge instanceof GenericEdge)
            {
                this.commonEdgeTools.add(aTool);
            }
            else
            {
                this.diagramEdgeTools.add(aTool);
            }
        }
    }

    private List<String> getShortLabels(List<GraphTool> tools)
    {
        List<String> labels = new ArrayList<String>();
        for (GraphTool aTool : tools)
        {
            Object prototype = aTool.getNodeOrEdge();
            if (GraphTool.SELECTION_TOOL.equals(aTool))
            {
                labels.add(this.selectShortLabel);
            }
            else if (prototype instanceof NoteNode)
            {
                labels.add(this.noteShortLabel);
            }
            else if (prototype instanceof FreeTextNode)
            {
                labels.add(this.freeTextShortLabel);
            }
            else if (prototype instanceof ImageNode)
            {
                labels.add(this.imageShortLabel);
            }
            else if (prototype instanceof ContainerNode)
            {
                labels.add(this.containerShortLabel);
            }
            else
            {
                labels.add(aTool.getLabel());
            }
        }
        return labels;
    }

    /**
     * @return the name of the diagram type, without the menu order prefix ("1.Class diagram"), or null if unknown
     */
    private String findDiagramName(IGraph graph)
    {
        if (this.pluginRegistry == null)
        {
            return null;
        }
        for (IDiagramPlugin aPlugin : this.pluginRegistry.getDiagramPlugins())
        {
            if (aPlugin.getGraphClass().equals(graph.getClass()))
            {
                return aPlugin.getName().replaceFirst("^\\d+\\.", "").trim();
            }
        }
        return null;
    }

    /**
     * @return the tools of the diagram type
     */
    public Component getAWTComponent()
    {
        return this.diagramPanel;
    }

    @Override
    public Component getCommonToolsAWTComponent()
    {
        return this.commonPanel;
    }

    @Override
    public String getDiagramName()
    {
        return this.diagramName;
    }

    @Override
    public List<GraphTool> getNodeTools()
    {
        List<GraphTool> tools = new ArrayList<GraphTool>();
        tools.add(GraphTool.SELECTION_TOOL);
        tools.addAll(this.diagramNodeTools);
        tools.addAll(this.commonNodeTools);
        return tools;
    }

    @Override
    public List<GraphTool> getEdgeTools()
    {
        List<GraphTool> tools = new ArrayList<GraphTool>();
        tools.addAll(this.diagramEdgeTools);
        tools.addAll(this.commonEdgeTools);
        return tools;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.horstmann.violet.framework.display.clipboard.sidebar.ISideToolPanel#addCustomTool(com.horstmann.violet.product.diagram.abstracts.Node,
     *      java.lang.String)
     */
    public void addTool(INode nodePrototype, String title)
    {
        GraphTool newTool = new GraphTool(nodePrototype, title);
        this.diagramNodeTools.add(newTool);
        this.diagramPanel.addTools(Arrays.asList(newTool));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.horstmann.violet.framework.display.clipboard.sidebar.ISideToolPanel#getSelectedTool()
     */
    public GraphTool getSelectedTool()
    {
        return this.selectedTool;
    }

    @Override
    public void setSelectedTool(GraphTool t)
    {
        boolean isNewSelection = !t.equals(this.selectedTool);
        this.selectedTool = t;
        if (isNewSelection)
        {
            fireToolChangeEvent(t);
        }
    }

    @Override
    public void notifyMouseEvent(GraphTool t, MouseEvent event)
    {
        fireToolMouseEvent(t, event);
    }

    private void fireToolMouseEvent(GraphTool tool, MouseEvent event)
    {
        Iterator<IGraphToolsBarMouseListener> it = this.clickListeners.iterator();
        while (it.hasNext())
        {
            IGraphToolsBarMouseListener listener = it.next();
            if (event.getID() == MouseEvent.MOUSE_PRESSED)
            {
                listener.onMouseToolClicked(tool);
            }
            else if (event.getID() == MouseEvent.MOUSE_DRAGGED)
            {
                listener.onMouseToolDragged(event);
            }
            else if (event.getID() == MouseEvent.MOUSE_RELEASED)
            {
                listener.onMouseToolReleased(event);
            }
        }
    }

    /**
     * Informs all listeners about a tool change
     *
     * @param tool
     */
    private void fireToolChangeEvent(GraphTool tool)
    {
        for (IGraphToolsBarListener listener : new ArrayList<IGraphToolsBarListener>(this.listeners))
        {
            listener.toolSelectionChanged(tool);
        }
    }

    /**
     * Selects the selection tool
     */
    public void reset()
    {
        setSelectedTool(GraphTool.SELECTION_TOOL);
    }

    public void addListener(IGraphToolsBarListener listener)
    {
        this.listeners.add(listener);
    }

    public void removeListener(IGraphToolsBarListener listener)
    {
        this.listeners.remove(listener);
    }

    public void addMouseListener(IGraphToolsBarMouseListener listener)
    {
        this.clickListeners.add(listener);
    }

    public void removeMouseListener(IGraphToolsBarMouseListener listener)
    {
        this.clickListeners.remove(listener);
    }

    private List<IGraphToolsBarListener> listeners = new ArrayList<IGraphToolsBarListener>();
    private List<IGraphToolsBarMouseListener> clickListeners = new ArrayList<IGraphToolsBarMouseListener>();
    private List<GraphTool> diagramNodeTools = new ArrayList<GraphTool>();
    private List<GraphTool> diagramEdgeTools = new ArrayList<GraphTool>();
    private List<GraphTool> commonNodeTools = new ArrayList<GraphTool>();
    private List<GraphTool> commonEdgeTools = new ArrayList<GraphTool>();
    private GraphTool selectedTool;
    private String diagramName;
    private GraphToolsBarPanel diagramPanel;
    private GraphToolsBarPanel commonPanel;

    @InjectedBean
    private PluginRegistry pluginRegistry;

    @ResourceBundleBean(key = "graphtools.elements.text")
    private String elementsTitle;

    @ResourceBundleBean(key = "graphtools.links.text")
    private String linksTitle;

    @ResourceBundleBean(key = "graphtools.note.short")
    private String noteShortLabel;

    @ResourceBundleBean(key = "graphtools.freetext.short")
    private String freeTextShortLabel;

    @ResourceBundleBean(key = "graphtools.image.short")
    private String imageShortLabel;

    @ResourceBundleBean(key = "graphtools.container.short")
    private String containerShortLabel;

    @ResourceBundleBean(key = "graphtools.select.short")
    private String selectShortLabel;

}
