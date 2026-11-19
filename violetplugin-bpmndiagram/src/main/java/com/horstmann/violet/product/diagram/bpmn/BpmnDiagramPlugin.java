package com.horstmann.violet.product.diagram.bpmn;

import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.product.diagram.abstracts.IGraph;

/**
 * Describes the BPMN (Business Process Model and Notation) diagram type
 */
public class BpmnDiagramPlugin implements IDiagramPlugin
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

    @Override
    public String getCategory()
    {
        return getResourceBundle().getString("menu.bpmn_diagram.category");
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
        return BpmnDiagramGraph.class;
    }

    private ResourceBundle getResourceBundle()
    {
        if (this.resourceBundle == null)
        {
            this.resourceBundle = ResourceBundle.getBundle(BpmnDiagramConstant.BPMN_DIAGRAM_STRINGS, Locale.getDefault());
        }
        return this.resourceBundle;
    }

    private ResourceBundle resourceBundle;

}
