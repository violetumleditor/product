package com.horstmann.violet.workspace.editorpart.behavior;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * Undo/Redo behavior triggered when node and edges are colorized
 * 
 * @author Alexandre de Pellegrin
 * 
 */
public class UndoRedoOnColorizeBehavior extends AbstractEditorPartBehavior
{

    /**
     * The global undo/redo behavior which contains all individual undo/redo behaviors
     */
    private UndoRedoCompoundBehavior compoundBehavior;

    private ColorChoice oldColorChoice;
    private ColorChoice newColorChoice;

    /**
     * Default constructor
     * 
     * @param compoundBehavior
     */
    public UndoRedoOnColorizeBehavior(UndoRedoCompoundBehavior compoundBehavior)
    {
        this.compoundBehavior = compoundBehavior;
    }

    @Override
    public void beforeChangingColorOnElement(IColorable element)
    {
        reset();
        this.oldColorChoice = new ColorChoice(element.getBackgroundColor(), element.getBorderColor());
    }

    @Override
    public void afterChangingColorOnElement(final IColorable element)
    {
        this.newColorChoice = new ColorChoice(element.getBackgroundColor(), element.getBorderColor());
        this.compoundBehavior.startHistoryCapture();
        CompoundEdit currentCapturedEdit = this.compoundBehavior.getCurrentCapturedEdit();
        currentCapturedEdit.addEdit(new UndoableColorEdit(element, this.oldColorChoice, this.newColorChoice));
        this.compoundBehavior.stopHistoryCapture();
        reset();
    }

    private void reset()
    {
        this.oldColorChoice = null;
        this.newColorChoice = null;
    }

    private class UndoableColorEdit extends AbstractUndoableEdit
    {

        private IColorable element;
        private ColorChoice oldColorChoice;
        private ColorChoice newColorChoice;

        public UndoableColorEdit(IColorable element, ColorChoice oldColorChoice, ColorChoice newColorChoice)
        {
            this.element = element;
            this.oldColorChoice = oldColorChoice;
            this.newColorChoice = newColorChoice;
        }

        @Override
        public void undo() throws CannotUndoException
        {
            this.element.setBackgroundColor(this.oldColorChoice.getBackgroundColor());
            this.element.setBorderColor(this.oldColorChoice.getBorderColor());
        }

        @Override
        public void redo() throws CannotRedoException
        {
            this.element.setBackgroundColor(this.newColorChoice.getBackgroundColor());
            this.element.setBorderColor(this.newColorChoice.getBorderColor());
        }

    }

}
