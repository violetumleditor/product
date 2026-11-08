package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.horstmann.violet.framework.dialog.DialogFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.propertyeditor.CustomPropertyEditor;
import com.horstmann.violet.framework.propertyeditor.ICustomPropertyEditor;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.DiagramLinkNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;

public class EditSelectedBehavior extends AbstractEditorPartBehavior
{

    public EditSelectedBehavior(IEditorPart editorPart)
    {
        BeanInjector.getInjector().inject(this);
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        boolean isButton1Clicked = (event.getButton() == MouseEvent.BUTTON1);
        if (event.getClickCount() > 1 && isButton1Clicked)
        {
            double zoom = editorPart.getZoomFactor();
            Point2D mouseLocation = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
            processSelection(mouseLocation);
            editSelected();
        }
    }
    
    /**
     * Selects double clicked element (inspired from SelectByClickBehavior class)
     * 
     * @param mouseLocation
     */
    private void processSelection(Point2D mouseLocation)
    {
    	this.selectionHandler.clearSelection();
    	INode node = this.graph.findNode(mouseLocation);
        IEdge edge = this.graph.findEdge(mouseLocation);
        if (edge != null)
        {
        	this.selectionHandler.addSelectedElement(edge);
        	if (this.selectionHandler.getSelectedElements().size() == 1) {
        		this.behaviorManager.fireOnEdgeSelected(edge);
        	}
        	return;
        }
        if (node != null)
        {
        	this.selectionHandler.addSelectedElement(node);
        	if (this.selectionHandler.getSelectedElements().size() == 1) {
        		this.behaviorManager.fireOnNodeSelected(node);
        	}
            return;
        }
    }
    

    public void editSelected()
    {
        final Object edited = selectionHandler.getLastSelectedElement();
        if (edited == null)
        {
            return;
        }
        final ICustomPropertyEditor sheet = new CustomPropertyEditor(edited);

        sheet.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(final PropertyChangeEvent event)
            {
                // TODO : fix open file event
                if (event.getSource() instanceof DiagramLinkNode)
                {
                    // DiagramLinkNode ln = (DiagramLinkNode) event.getSource();
                    // DiagramLink dl = ln.getDiagramLink();
                    // if (dl != null && dl.getOpenFlag().booleanValue())
                    // {
                    // diagramPanel.fireMustOpenFile(dl.getFile());
                    // dl.setOpenFlag(new Boolean(false));
                    // }
                }

                if (edited instanceof INode)
                {
                    behaviorManager.fireWhileEditingNode((INode) edited, event);
                }
                if (edited instanceof IEdge)
                {
                    behaviorManager.fireWhileEditingEdge((IEdge) edited, event);
                }
                editorPart.getSwingComponent().invalidate();
            }
        });

        JOptionPane optionPane = new JOptionPane();
        optionPane.setOpaque(true);
        optionPane.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent event)
            {
                if ((event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) && event.getNewValue() != null && event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE)
                {
                    if (sheet.isEditable())
                    {
                        // This manages optionPane submits through a property
                        // listener because, as dialog display could be
                        // delegated
                        // (to Eclipse for example), host system can work in
                        // other threads
                        if (edited instanceof INode)
                        {
                            behaviorManager.fireAfterEditingNode((INode) edited);
                        }
                        if (edited instanceof IEdge)
                        {
                            behaviorManager.fireAfterEditingEdge((IEdge) edited);
                        }
                        editorPart.getSwingComponent().invalidate();
                    }
                }
            }
        });

        String tooltip = "";
        if (sheet.isEditable())
        {
            if (edited instanceof INode)
            {
                tooltip = ((INode) edited).getToolTip();
                this.behaviorManager.fireBeforeEditingNode((INode) edited);
            }
            if (edited instanceof IEdge)
            {
                tooltip = ((IEdge) edited).getToolTip();
                this.behaviorManager.fireBeforeEditingEdge((IEdge) edited);
            }
            optionPane.setMessage(sheet.getAWTComponent());
        }
        if (!sheet.isEditable())
        {
            JLabel label = new JLabel(this.uneditableBeanMessage);
            label.setFont(label.getFont().deriveFont(Font.PLAIN));
            optionPane.setMessage(label);
        }
        String title = tooltip == null || tooltip.isEmpty() ? this.dialogTitle : tooltip + " – " + this.dialogTitle;
        this.dialogFactory.showDialog(optionPane, title, true);
    }
    
  

    private IEditorPartSelectionHandler selectionHandler;
    private IEditorPart editorPart;
    private IGraph graph;
    private IEditorPartBehaviorManager behaviorManager;

    @InjectedBean
    private DialogFactory dialogFactory;

    @ResourceBundleBean(key = "edit.properties.title")
    private String dialogTitle;

    @ResourceBundleBean(key = "edit.properties.empty_bean_message")
    private String uneditableBeanMessage;

}
