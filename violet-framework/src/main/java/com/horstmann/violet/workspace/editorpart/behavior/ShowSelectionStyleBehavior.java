package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.AbstractEdge;
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.ThicknessToolsBarPanel;

/**
 * Makes the style tools of the side bar show the colors and the line thickness of the selected element. With several elements
 * selected, they show what these elements share, and the default style for what differs; so do they with nothing selected.
 * <p>
 * Nothing tells when the selection changes (many behaviors change it), but the diagram is always painted then: the style of the
 * selection is read each time the diagram is painted, and given to the tools only when the selection or its style changed. This
 * also follows an undo, or a style changed from the properties dialog.
 */
public class ShowSelectionStyleBehavior extends AbstractEditorPartBehavior
{

    public ShowSelectionStyleBehavior(IEditorPart editorPart, IColorChoiceBar colorChoiceBar, IThicknessChoiceBar thicknessChoiceBar)
    {
        this.editorPart = editorPart;
        this.colorChoiceBar = colorChoiceBar;
        this.thicknessChoiceBar = thicknessChoiceBar;
    }

    @Override
    public void onPaint(Graphics2D g2)
    {
        List<ISelectable> selection = getStyledElements();
        ColorChoice colorChoice = getColorChoice(selection);
        int thickness = getThickness(selection);
        // A choice made in the tools with nothing to apply it to stays shown until the selection changes
        boolean isSameSelection = selection.equals(this.lastSelection);
        if (isSameSelection && colorChoice.equals(this.lastColorChoice) && thickness == this.lastThickness)
        {
            return;
        }
        this.lastSelection = new ArrayList<ISelectable>(selection);
        this.lastColorChoice = colorChoice;
        this.lastThickness = thickness;
        this.colorChoiceBar.setShownColorChoice(colorChoice);
        this.thicknessChoiceBar.setShownThickness(thickness);
    }

    /**
     * @return the selected elements or, with the marker tool, the marker itself
     */
    private List<ISelectable> getStyledElements()
    {
        for (DrawMarkerBehavior aBehavior : this.editorPart.getBehaviorManager().getBehaviors(DrawMarkerBehavior.class))
        {
            if (aBehavior.isMarkerToolSelected())
            {
                return Collections.<ISelectable> singletonList(aBehavior.getPen());
            }
        }
        return this.editorPart.getSelectionHandler().getSelectedElements();
    }

    /**
     * @return the colors shared by the selected elements, the default ones if they differ or if nothing with colors is selected
     */
    private ColorChoice getColorChoice(List<ISelectable> selection)
    {
        ColorChoice result = null;
        for (ISelectable anElement : selection)
        {
            if (!(anElement instanceof IColorable))
            {
                continue;
            }
            IColorable colorable = (IColorable) anElement;
            ColorChoice colorChoice = new ColorChoice(colorable.getBackgroundColor(), colorable.getBorderColor());
            if (result != null && !result.equals(colorChoice))
            {
                return ColorChoice.DEFAULT;
            }
            result = colorChoice;
        }
        return result != null ? result : ColorChoice.DEFAULT;
    }

    /**
     * @return the line thickness shared by the selected elements, the default one if they differ or if nothing is selected
     */
    private int getThickness(List<ISelectable> selection)
    {
        int result = NO_THICKNESS;
        for (ISelectable anElement : selection)
        {
            int thickness = NO_THICKNESS;
            if (anElement instanceof AbstractNode)
            {
                thickness = ((AbstractNode) anElement).getBorderWidth();
            }
            if (anElement instanceof AbstractEdge)
            {
                thickness = ((AbstractEdge) anElement).getBorderWidth();
            }
            if (thickness == NO_THICKNESS)
            {
                continue;
            }
            if (result != NO_THICKNESS && result != thickness)
            {
                return ThicknessToolsBarPanel.DEFAULT_THICKNESS;
            }
            result = thickness;
        }
        return result != NO_THICKNESS ? result : ThicknessToolsBarPanel.DEFAULT_THICKNESS;
    }

    private static final int NO_THICKNESS = -1;

    private final IEditorPart editorPart;
    private final IColorChoiceBar colorChoiceBar;
    private final IThicknessChoiceBar thicknessChoiceBar;
    /** Selection and its style given to the tools last: they are told again only when one of them changes */
    private List<ISelectable> lastSelection;
    private ColorChoice lastColorChoice;
    private int lastThickness = NO_THICKNESS;

}
