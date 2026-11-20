package com.horstmann.violet.product.diagram.bpmn;

import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.product.diagram.abstracts.IGraph;

/**
 * Describes the BPMN (Business Process Model and Notation) diagram type
 */
public class ProcessDiagramPlugin implements IDiagramPlugin
{

    @Override
    public String getDescription()
    {
        return "BPMN business process diagram";
    }

    @Override
    public String getProvider()
    {
        return "Alexandre de Pellegrin";
    }

    @Override
    public String getVersion()
    {
        return "1.0.0";
    }

    @Override
    public String getName()
    {
        return getResourceBundle().getString("menu.bpmn_diagram.name");
    }

    /**
     * File &gt; New &gt; BPMN &gt; Process diagram (user's choice), beside the UML menu
     */
    @Override
    public String getNotation()
    {
        return "2.BPMN";
    }

    /**
     * None: the process diagram is listed right in the BPMN menu
     */
    @Override
    public String getCategory()
    {
        return "";
    }

    @Override
    public String getFileExtension()
    {
        return getResourceBundle().getString("files.bpmn.extension");
    }

    @Override
    public String getFileExtensionName()
    {
        return getResourceBundle().getString("files.bpmn.name");
    }

    @Override
    public Class<? extends IGraph> getGraphClass()
    {
        return ProcessDiagramGraph.class;
    }

    private ResourceBundle getResourceBundle()
    {
        if (this.resourceBundle == null)
        {
            this.resourceBundle = ResourceBundle.getBundle(ProcessDiagramConstant.PROCESS_DIAGRAM_STRINGS, Locale.getDefault());
        }
        return this.resourceBundle;
    }

    private ResourceBundle resourceBundle;

}
