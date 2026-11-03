package com.horstmann.violet.workspace.editorpart.behavior;

import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceChangeListener;

public class ColorizeBehavior extends AbstractEditorPartBehavior
{

    public ColorizeBehavior(final IWorkspace workspace, final IColorChoiceBar colorChoiceBar)
    {
        this.behaviorManager = workspace.getEditorPart().getBehaviorManager();
        colorChoiceBar.addColorChoiceChangeListener(new IColorChoiceChangeListener()
        {

            @Override
            public void onColorChoiceChange(ColorChoice newColorChoice)
            {
                List<ISelectable> selectedElements = workspace.getEditorPart().getSelectionHandler().getSelectedElements();
            	for (ISelectable element : selectedElements) {
            		if (element != null && IColorable.class.isInstance(element)) {
                    	IColorable colorableElement = (IColorable) element;
                    	updateElementColor(colorableElement, newColorChoice);
            		}
            	}
            	workspace.getEditorPart().getSwingComponent().repaint();
            }
        });
    }
    
    
    private void updateElementColor(IColorable colorableElement, ColorChoice currentColorChoice) {
    	this.behaviorManager.fireBeforeChangingColorOnElement(colorableElement);
        colorableElement.setBackgroundColor(currentColorChoice.getBackgroundColor());
        colorableElement.setBorderColor(currentColorChoice.getBorderColor());
        this.behaviorManager.fireAfterChangingColorOnElement(colorableElement);
    }



    private IEditorPartBehaviorManager behaviorManager;

}
