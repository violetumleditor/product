package com.horstmann.violet.workspace.editorpart;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.workspace.editorpart.behavior.IEditorPartBehavior;

public class EditorPartBehaviorManager implements IEditorPartBehaviorManager
{

    private List<IEditorPartBehavior> behaviors = new ArrayList<IEditorPartBehavior>();

    public void addBehavior(IEditorPartBehavior newBehavior)
    {
        this.behaviors.add(newBehavior);
    }

    public List<IEditorPartBehavior> getBehaviors()
    {
        return this.behaviors;
    }

    @Override
    public <T extends IEditorPartBehavior> List<T> getBehaviors(Class<T> type)
    {
        List<T> result = new ArrayList<T>();
        for (IEditorPartBehavior aBehavior : this.behaviors)
        {
            if (type.isInstance(aBehavior))
            {
                // Safe cast to T, type ensured by type.isInstance(aBehavior) check.
                @SuppressWarnings("unchecked")
                T castedBehavior = (T) aBehavior;
                result.add(castedBehavior);
            }
        }
        return result;
    }

    /*
     * A behavior consuming a mouse button event (see EditButtonBehavior) keeps it from the next ones. Only when it consumes it:
     * an event Swing already consumed (for instance the press closing a popup menu) still goes to all of them, as before.
     */

    @Override
    public void fireOnMousePressed(MouseEvent event)
    {
        boolean wasConsumed = event.isConsumed();
        for (IEditorPartBehavior aBehavior : this.behaviors)
        {
            aBehavior.onMousePressed(event);
            if (!wasConsumed && event.isConsumed()) break;
        }
    }

    @Override
    public void fireOnMouseDragged(MouseEvent event)
    {
        boolean wasConsumed = event.isConsumed();
        for (IEditorPartBehavior aBehavior : this.behaviors)
        {
            aBehavior.onMouseDragged(event);
            if (!wasConsumed && event.isConsumed()) break;
        }
    }

    @Override
    public void fireOnMouseReleased(MouseEvent event)
    {
        boolean wasConsumed = event.isConsumed();
        for (IEditorPartBehavior aBehavior : this.behaviors)
        {
            aBehavior.onMouseReleased(event);
            if (!wasConsumed && event.isConsumed()) break;
        }
    }

    @Override
    public void fireOnMouseClicked(MouseEvent event)
    {
        boolean wasConsumed = event.isConsumed();
        for (IEditorPartBehavior aBehavior : this.behaviors)
        {
            aBehavior.onMouseClicked(event);
            if (!wasConsumed && event.isConsumed()) break;
        }
    }

    @Override
    public void fireOnMouseMoved(MouseEvent event)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.onMouseMoved(event);
    }

    @Override
    public void fireOnMouseWheelMoved(MouseWheelEvent event)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.onMouseWheelMoved(event);
    }

    @Override
    public void fireBeforeEditingNode(INode node)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeEditingNode(node);
    }

    @Override
    public void fireWhileEditingNode(INode node, PropertyChangeEvent event)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.whileEditingNode(node, event);
    }

    @Override
    public void fireAfterEditingNode(INode node)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterEditingNode(node);
    }

    @Override
    public void fireBeforeEditingEdge(IEdge edge)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeEditingEdge(edge);
    }

    @Override
    public void fireWhileEditingEdge(IEdge edge, PropertyChangeEvent event)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.whileEditingEdge(edge, event);
    }

    @Override
    public void fireAfterEditingEdge(IEdge edge)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterEditingEdge(edge);
    }

    @Override
    public void fireBeforeRemovingSelectedElements()
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeRemovingSelectedElements();
    }

    @Override
    public void fireAfterRemovingSelectedElements()
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterRemovingSelectedElements();
    }

    @Override
    public void fireBeforeAddingNodeAtPoint(INode node, Point2D location)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeAddingNodeAtPoint(node, location);
    }

    @Override
    public void fireAfterAddingNodeAtPoint(INode node, Point2D location)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterAddingNodeAtPoint(node, location);
    }

    @Override
    public void fireBeforeAddingEdgeAtPoints(IEdge edge, Point2D startPoint, Point2D endPoint)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeAddingEdgeAtPoints(edge, startPoint, endPoint);
    }

    @Override
    public void fireAfterAddingEdgeAtPoints(IEdge edge, Point2D startPoint, Point2D endPoint)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterAddingEdgeAtPoints(edge, startPoint, endPoint);
    }

    @Override
    public void fireOnEdgeSelected(IEdge edge)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.onEdgeSelected(edge);
    }

    @Override
    public void fireOnNodeSelected(INode node)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.onNodeSelected(node);
    }
    
    @Override
    public void fireBeforeChangingTransitionPointsOnEdge(IEdge edge)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeChangingTransitionPointsOnEdge(edge);
    }
    
    @Override
    public void fireAfterChangingTransitionPointsOnEdge(IEdge edge)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterChangingTransitionPointsOnEdge(edge);
    }
    
    
    @Override
    public void fireBeforeChangingColorOnElement(IColorable element)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.beforeChangingColorOnElement(element);
    }
    
    @Override
    public void fireAfterChangingColorOnElement(IColorable element)
    {
        for (IEditorPartBehavior aBehavior : this.behaviors)
            aBehavior.afterChangingColorOnElement(element);
    }
    

}
