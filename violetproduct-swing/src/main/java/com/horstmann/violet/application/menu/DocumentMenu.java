package com.horstmann.violet.application.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

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
     * @param fileMenu which knows how to close a diagram
     */
    @ResourceBundleBean(key = "document")
    public DocumentMenu(final MainFrame mainFrame, FileMenu fileMenu)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        this.fileMenu = fileMenu;
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
            final JMenuItem menuItem = new DocumentMenuItem(title, () -> closeFromMenu(aWorkspace), this.closeToolTip);
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
        if (!workspaceList.isEmpty()) {
            addSeparator();
            add(getCloseAllItem());
        }
        if (getPopupMenu().isVisible()) {
            refreshOpenPopup();
        }
    }

    /**
     * Closes a diagram from the cross of its entry, keeping the menu open while other diagrams remain
     *
     * @param workspace of the diagram to close
     */
    private void closeFromMenu(IWorkspace workspace)
    {
        MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
        if (workspace.getGraphFile().isSaveRequired())
        {
            // The question about unsaved changes needs the menu closed first
            menuSelectionManager.clearSelectedPath();
            SwingUtilities.invokeLater(() -> this.fileMenu.closeWorkspace(workspace));
            return;
        }
        this.fileMenu.closeWorkspace(workspace);
        if (this.mainFrame.getWorkspaceList().isEmpty())
        {
            menuSelectionManager.clearSelectedPath();
        }
    }

    /**
     * Fits the open popup to its new entries, and forgets the removed entry which was selected
     */
    private void refreshOpenPopup()
    {
        MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
        MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
        for (int i = 0; i < selectedPath.length; i++)
        {
            if (selectedPath[i] == getPopupMenu())
            {
                menuSelectionManager.setSelectedPath(Arrays.copyOf(selectedPath, i + 1));
                break;
            }
        }
        getPopupMenu().pack();
        getPopupMenu().repaint();
    }

    /**
     * @return the entry which closes every diagram
     */
    private JMenuItem getCloseAllItem()
    {
        if (this.closeAllItem == null)
        {
            this.closeAllItem = new JMenuItem(this.closeAllText, new ShapeIcon(ToolShapes.CLOSE, ACTIVE_ICON_SIZE));
            this.closeAllItem.addActionListener(e -> this.fileMenu.closeAllWorkspaces());
        }
        return this.closeAllItem;
    }


    /**
     * Current editor frame
     */
    private MainFrame mainFrame;

    private FileMenu fileMenu;

    private JMenuItem closeAllItem;

    @ResourceBundleBean(key = "document.close_all.text")
    private String closeAllText;

    @ResourceBundleBean(key = "document.close.tooltip")
    private String closeToolTip;

    private static final int ACTIVE_ICON_SIZE = 16;

    private final Icon activeWorkspaceIcon = new ShapeIcon(ToolShapes.DOT, ACTIVE_ICON_SIZE, null, true);
}
