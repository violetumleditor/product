package com.horstmann.violet.framework.plugin;

import com.horstmann.violet.product.diagram.abstracts.IGraph;

/**
 * Describes a Violet's plugin embedding a new kind of diagram.
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public interface IDiagramPlugin extends AbstractPlugin
{

    /**
     * @return graph type name (ex : Class Diagram)
     */
    public abstract String getName();
    
    /**
     * @return the notation of this diagram, the first level of File &gt; New, after an optional order prefix ("1.UML",
     *         "2.BPMN"); UML by default
     */
    public default String getNotation()
    {
        return UML_NOTATION;
    }

    /**
     * @return the category of this diagram inside its notation (ex : Static diagram), the second level of File &gt; New, or an
     *         empty string to list the diagram right in its notation's menu
     */
    public abstract String getCategory();

    /**
     * The notation of the UML diagrams
     */
    public static final String UML_NOTATION = "1.UML";
    
    /**
     * @return file extension associated to this graph (ex : .class.violet)
     */
    public abstract String getFileExtension();

    /**
     * @return file extension textual name (ex : Class Diagram Files)
     */
    public abstract String getFileExtensionName();
    
    /**
     * @return corresponding graph class
     */
    public abstract Class<? extends IGraph> getGraphClass();

}
