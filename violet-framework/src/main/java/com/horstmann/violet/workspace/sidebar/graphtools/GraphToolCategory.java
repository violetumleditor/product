package com.horstmann.violet.workspace.sidebar.graphtools;

import java.util.List;

/**
 * A named group of tools of the diagram, shown as a collapsible group in the side bar and as one button once it is reduced
 */
public class GraphToolCategory
{

    /**
     * @param name shown to the user
     * @param tools in display order
     */
    public GraphToolCategory(String name, List<GraphTool> tools)
    {
        this.name = name;
        this.tools = tools;
    }

    public String getName()
    {
        return this.name;
    }

    public List<GraphTool> getTools()
    {
        return this.tools;
    }

    private final String name;
    private final List<GraphTool> tools;

}
