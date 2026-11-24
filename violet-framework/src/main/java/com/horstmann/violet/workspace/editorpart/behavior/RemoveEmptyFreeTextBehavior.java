package com.horstmann.violet.workspace.editorpart.behavior;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;

/**
 * A free text left without any text once edited (in place, by typing on it or in its dialog) is removed from the diagram: having
 * neither border nor background, it couldn't be seen any more.
 * <p>
 * Comes before UndoRedoCompoundBehavior, whose capture of the edit is still open then: the edit and the removal are undone in one
 * step, which gives the node and its text back.
 */
public class RemoveEmptyFreeTextBehavior extends AbstractEditorPartBehavior
{

    public RemoveEmptyFreeTextBehavior(IEditorPart editorPart)
    {
        this.editorPart = editorPart;
    }

    @Override
    public void afterEditingNode(INode node)
    {
        if (!(node instanceof FreeTextNode) || !((FreeTextNode) node).getText().isBlank())
        {
            return;
        }
        IEditorPartBehaviorManager behaviorManager = this.editorPart.getBehaviorManager();
        // Undo/redo, auto-save and repainting listen to these
        behaviorManager.fireBeforeRemovingSelectedElements();
        this.editorPart.getGraph().removeNode(node);
        this.editorPart.getSelectionHandler().removeElementFromSelection(node);
        behaviorManager.fireAfterRemovingSelectedElements();
    }

    private final IEditorPart editorPart;
}
