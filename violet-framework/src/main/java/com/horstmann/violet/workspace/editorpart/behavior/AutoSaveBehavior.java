package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;

import javax.swing.Timer;

import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.IGraphFileListener;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.userpreferences.UserPreferencesService;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

public class AutoSaveBehavior extends AbstractEditorPartBehavior
{

    private static final int AUTO_SAVE_IDLE_MILLIS = 2_000;

    public AutoSaveBehavior(IGraphFile graphFile)
    {
        BeanInjector.getInjector().inject(this);
        this.graphFile = graphFile;
        this.idleTimer = new Timer(AUTO_SAVE_IDLE_MILLIS, event -> saveIfNeeded());
        this.idleTimer.setRepeats(false);
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveIfNeeded));
        this.graphFile.addListener(new IGraphFileListener()
        {
            @Override
            public void onFileModified()
            {
                // Also for the changes that reach no behavior event (a behavior consuming its mouse events, such as
                // MoveSeparatorBehavior, only marks the file as modified)
                touch();
            }

            @Override
            public void onFileSaved()
            {
                // Keep the opened-files and recent-files preferences in sync after every
                // save (auto-save to temp location or explicit save to a real location).
                // This ensures the app can restore the correct file list on restart.
                userPreferencesService.addOpenedFile(graphFile);
                userPreferencesService.addRecentFile(graphFile);
            }
        });
    }

    private void touch()
    {
        // Only keep the idle timer alive while the diagram is dirty.
        // Non-modifying interactions (mouse moves, etc.) will still postpone
        // the save, but only when there is actually something to save.
        if (!this.graphFile.isSaveRequired())
        {
            return;
        }
        this.idleTimer.restart();
    }

    private void saveIfNeeded()
    {
        this.graphFile.save(false);
    }

    @Override
    public void afterAddingEdgeAtPoints(IEdge edge, Point2D startPoint, Point2D endPoint)
    {
        touch();
    }

    @Override
    public void afterAddingNodeAtPoint(INode node, Point2D location)
    {
        touch();
    }

    @Override
    public void afterEditingEdge(IEdge edge)
    {
        touch();
    }

    @Override
    public void afterEditingNode(INode node)
    {
        touch();
    }

    @Override
    public void afterRemovingSelectedElements()
    {
        touch();
    }

    @Override
    public void whileEditingEdge(IEdge edge, PropertyChangeEvent event)
    {
        touch();
    }

    @Override
    public void whileEditingNode(INode node, PropertyChangeEvent event)
    {
        touch();
    }

    @Override
    public void beforeChangingTransitionPointsOnEdge(IEdge edge)
    {
        touch();
    }

    @Override
    public void afterChangingTransitionPointsOnEdge(IEdge edge)
    {
        touch();
    }

    @Override
    public void beforeChangingColorOnElement(IColorable element)
    {
        touch();
    }

    @Override
    public void afterChangingColorOnElement(IColorable element)
    {
        touch();
    }

    @Override
    public void onMouseDragged(MouseEvent event)
    {
        touch();
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
    }

    @Override
    public void onMouseReleased(MouseEvent event)
    {
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
    }

    @Override
    public void onMouseMoved(MouseEvent event)
    {
    }

    @Override
    public void onMouseWheelMoved(MouseWheelEvent event)
    {
    }

    @Override
    public void onToolSelected(GraphTool selectedTool)
    {
    }

    @Override
    public void onNodeSelected(INode node)
    {
    }

    @Override
    public void onEdgeSelected(IEdge edge)
    {
    }

    private final IGraphFile graphFile;

    private final Timer idleTimer;

    @InjectedBean
    private UserPreferencesService userPreferencesService;

}
