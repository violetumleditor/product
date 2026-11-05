package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;

public class ShowMenuOnRightClickBehavior extends AbstractEditorPartBehavior
{

    public ShowMenuOnRightClickBehavior(IEditorPart editorPart)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.editorPart = editorPart;
        this.graph = editorPart.getGraph();
        this.selectionHandler = editorPart.getSelectionHandler();
    }
    
    @Override
    public void onMouseClicked(MouseEvent event)
    {
        boolean isButton3Clicked = (event.getButton() == MouseEvent.BUTTON3);
        if (event.getClickCount() == 1 && isButton3Clicked)
        {
            double zoom = editorPart.getZoomFactor();
            final Point2D mousePoint = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
            changeSelectedElementIfNeeded(mousePoint);
            getPopupMenu().show(this.editorPart.getSwingComponent(), event.getX(), event.getY());
        }
    }
    
    private void changeSelectedElementIfNeeded(Point2D mouseLocation)
    {
        INode node = this.graph.findNode(mouseLocation);
        IEdge edge = this.graph.findEdge(mouseLocation);
        
        List<ISelectable> selectedElements = this.selectionHandler.getSelectedElements();
        if (node != null && !selectedElements.contains(node))
        {
            this.selectionHandler.clearSelection();
            this.selectionHandler.addSelectedElement(node);
        }
        if (edge != null && !selectedElements.contains(edge))
        {
            this.selectionHandler.clearSelection();
            this.selectionHandler.addSelectedElement(edge);
        }

    }
    
    private JPopupMenu getPopupMenu() {
        if (this.popupMenu == null) {
            this.popupMenu = new JPopupMenu();
            this.popupMenu = fillMenu(this.popupMenu);
            this.popupMenuTheme = ThemeManager.getInstance().getTheme();
        }
        // This menu stays out of the windows updated when the theme changes
        ITheme currentTheme = ThemeManager.getInstance().getTheme();
        if (currentTheme != this.popupMenuTheme)
        {
            SwingUtilities.updateComponentTreeUI(this.popupMenu);
            this.popupMenuTheme = currentTheme;
        }
        return this.popupMenu;
    }
    
    /**
     * Initializes menu
     */
    private JPopupMenu fillMenu(JPopupMenu aPopupMenu)
    {
        undo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<UndoRedoCompoundBehavior> found = behaviorManager.getBehaviors(UndoRedoCompoundBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).undo();
            }
        });
        aPopupMenu.add(undo);

        redo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<UndoRedoCompoundBehavior> found = behaviorManager.getBehaviors(UndoRedoCompoundBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).redo();
            }
        });
        aPopupMenu.add(redo);

        properties.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<EditSelectedBehavior> found = behaviorManager.getBehaviors(EditSelectedBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).editSelected();
            }
        });
        aPopupMenu.add(properties);

        cut.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<CutCopyPasteBehavior> found = behaviorManager.getBehaviors(CutCopyPasteBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).cut();
            }
        });
        aPopupMenu.add(cut);

        copy.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<CutCopyPasteBehavior> found = behaviorManager.getBehaviors(CutCopyPasteBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).copy();
            }
        });
        aPopupMenu.add(copy);

        paste.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<CutCopyPasteBehavior> found = behaviorManager.getBehaviors(CutCopyPasteBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).paste();
            }
        });
        aPopupMenu.add(paste);

        delete.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                ShowMenuOnRightClickBehavior.this.editorPart.removeSelected();
            }
        });
        aPopupMenu.add(delete);
        
        selectAll.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                IEditorPartBehaviorManager behaviorManager = ShowMenuOnRightClickBehavior.this.editorPart.getBehaviorManager();
                List<SelectAllBehavior> found = behaviorManager.getBehaviors(SelectAllBehavior.class);
                if (found.size() != 1) {
                    return;
                }
                found.get(0).selectAllGraphElements();
            }
        });
        aPopupMenu.add(selectAll);
        
        return aPopupMenu;
    }
    
    private JPopupMenu popupMenu; 

    private ITheme popupMenuTheme;
    
    private IGraph graph;

    private IEditorPartSelectionHandler selectionHandler;
    
    @ResourceBundleBean(key = "edit.undo")
    private JMenuItem undo;

    @ResourceBundleBean(key = "edit.redo")
    private JMenuItem redo;

    @ResourceBundleBean(key = "edit.properties")
    private JMenuItem properties;

    @ResourceBundleBean(key = "edit.cut")
    private JMenuItem cut;

    @ResourceBundleBean(key = "edit.copy")
    private JMenuItem copy;

    @ResourceBundleBean(key = "edit.paste")
    private JMenuItem paste;

    @ResourceBundleBean(key = "edit.delete")
    private JMenuItem delete;

    @ResourceBundleBean(key = "edit.select_all")
    private JMenuItem selectAll;
    
    private IEditorPart editorPart;

  
}
