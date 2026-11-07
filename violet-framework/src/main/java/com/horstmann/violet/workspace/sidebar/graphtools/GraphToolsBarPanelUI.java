package com.horstmann.violet.workspace.sidebar.graphtools;

import javax.swing.JComponent;
import javax.swing.plaf.PanelUI;

import com.horstmann.violet.framework.theme.ThemeManager;

public class GraphToolsBarPanelUI extends PanelUI
{

    @Override
    public void installUI(JComponent c)
    {
        c.setOpaque(true);
        c.setBackground(ThemeManager.getInstance().getTheme().getSidebarElementBackgroundColor());
    }

}
