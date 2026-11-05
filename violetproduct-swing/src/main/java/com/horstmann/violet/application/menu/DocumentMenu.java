package com.horstmann.violet.application.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.workspace.IWorkspace;

@ResourceBundleBean(resourceReference = MenuFactory.class)
public class DocumentMenu extends JMenu
{

    /**
     * Default constructor
     * 
     * @param mainFrame where is attached this menu
     * @param factory for accessing to external resources
     */
    @ResourceBundleBean(key = "document")
    public DocumentMenu(final MainFrame mainFrame)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        List<IWorkspace> workspaceList = this.mainFrame.getWorkspaceList();
        setEnabled(!workspaceList.isEmpty());
    }
    
    public void updateMenuItem() {
        List<IWorkspace> workspaceList = this.mainFrame.getWorkspaceList();
        setEnabled(!workspaceList.isEmpty());
        IWorkspace activeWorkspace = this.mainFrame.getActiveWorkspace();
        removeAll();
        for (final IWorkspace aWorkspace : workspaceList) {
            String title = aWorkspace.getTitle();
            final JMenuItem menuItem = new JMenuItem(title);
            add(menuItem);
            menuItem.addActionListener(new ActionListener()
            {
                
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    mainFrame.setActiveWorkspace(aWorkspace);
                    menuItem.setIcon(activeWorkspaceIcon);
                    
                }
            });
            if (aWorkspace.equals(activeWorkspace)) {
                menuItem.setIcon(activeWorkspaceIcon);
            }
        }
    }
    
    
    /**
     * Current editor frame
     */
    private MainFrame mainFrame;
    
    private static final int ACTIVE_ICON_SIZE = 16;

    private final Icon activeWorkspaceIcon = new ShapeIcon(ToolShapes.DOT, ACTIVE_ICON_SIZE, null, true);
}
