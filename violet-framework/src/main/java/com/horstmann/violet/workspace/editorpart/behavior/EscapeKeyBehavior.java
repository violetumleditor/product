package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Escape goes back to the selection tool, as a right click does, for instance to leave a locked link tool. Only while another
 * tool is selected, so that this key still closes menus and dialogs otherwise. Bound to the window, as the diagram never takes
 * the keyboard focus; Swing ignores the bindings of diagrams hidden in other tabs.
 */
public class EscapeKeyBehavior extends AbstractEditorPartBehavior
{

    public EscapeKeyBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.graphToolsBar = graphToolsBar;
        JComponent component = editorPart.getSwingComponent();
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), ACTION_KEY);
        component.getActionMap().put(ACTION_KEY, new AbstractAction()
        {
            @Override
            public boolean isEnabled()
            {
                return !GraphTool.SELECTION_TOOL.equals(EscapeKeyBehavior.this.graphToolsBar.getSelectedTool());
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                EscapeKeyBehavior.this.graphToolsBar.reset();
            }
        });
    }

    private static final String ACTION_KEY = "violet.selectionTool";

    private IGraphToolsBar graphToolsBar;

}
