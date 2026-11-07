package com.horstmann.violet.workspace.sidebar.graphtools;

import com.horstmann.violet.framework.swingextension.CustomToggleButton;
import com.horstmann.violet.framework.swingextension.CustomToggleButtonGraphToolUI;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * Button embedding a graph tool
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class GraphToolsBarButton extends CustomToggleButton
{

    /**
     * Default constructor
     * 
     * @param aTool to be displayed as a button
     */
    public GraphToolsBarButton(GraphTool aTool)
    {
        super(aTool.getLabel(), aTool.getIcon());
        installToolUI();
        this.tool = aTool;
    }

    private void installToolUI()
    {
        setUI(new CustomToggleButtonGraphToolUI(ThemeManager.getInstance().getTheme().getToggleButtonSelectedColor(), ThemeManager
                .getInstance().getTheme().getToggleButtonUnselectedColor()));
    }

    /**
     * Keeps the custom UI, and takes the colors of the new theme, when the look and feel changes
     */
    @Override
    public void updateUI()
    {
        if (getUI() instanceof CustomToggleButtonGraphToolUI)
        {
            installToolUI();
            return;
        }
        super.updateUI();
    }

    /**
     * @return embedded tool
     */
    public GraphTool getTool()
    {
        return tool;
    }

    /**
     * Embedded graph tool
     */
    private GraphTool tool;

}
