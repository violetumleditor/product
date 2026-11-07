package com.horstmann.violet.workspace.editorpart.behavior;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.Id;
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;

/**
 * Undo/Redo behavior triggered when transition points change on free path edge
 *
 * @author Alexandre de Pellegrin
 *
 */
public class UndoRedoOnTransitionPointChangeBehavior extends AbstractEditorPartBehavior {

	/**
	 * The global undo/redo behavior which contains all individual undo/redo
	 * behaviors
	 */
	private UndoRedoCompoundBehavior compoundBehavior;

    private IEditorPartSelectionHandler selectionHandler;

	private IEdge selectedEdge;

	private IGraph graph;

	private ITransitionPoint[] transitionPointsBeforeChanges;

	/**
	 * Default constructor
	 *
	 * @param editorPart
	 */
	public UndoRedoOnTransitionPointChangeBehavior(IEditorPart editorPart, UndoRedoCompoundBehavior compoundBehavior) {
		this.compoundBehavior = compoundBehavior;
		this.selectionHandler = editorPart.getSelectionHandler();
		this.graph = editorPart.getGraph();
	}

	@Override
	public void beforeChangingTransitionPointsOnEdge(IEdge edge) {
		this.selectedEdge = edge;
		this.transitionPointsBeforeChanges = EdgeTransitionPoint.copyOf(edge.getTransitionPoints());
	}

	@Override
	public void afterChangingTransitionPointsOnEdge(final IEdge edge) {
		if (!edge.equals(this.selectedEdge)) {
			return;
		}
		ITransitionPoint[] transitionPointsAfterChanges = EdgeTransitionPoint.copyOf(edge.getTransitionPoints());
		this.selectedEdge = null;
		if (isSameLocations(this.transitionPointsBeforeChanges, transitionPointsAfterChanges)) {
			return;
		}
		this.compoundBehavior.startHistoryCapture();
		CompoundEdit capturedEdit = this.compoundBehavior.getCurrentCapturedEdit();
		capturedEdit.addEdit(createEdit(this.graph, this.selectionHandler, edge, this.transitionPointsBeforeChanges, transitionPointsAfterChanges));
		this.compoundBehavior.stopHistoryCapture();
	}

	/**
	 * @return true if both arrays hold points at the same locations, in the same order
	 */
	static boolean isSameLocations(ITransitionPoint[] points1, ITransitionPoint[] points2) {
		if (points1.length != points2.length) {
			return false;
		}
		for (int i = 0; i < points1.length; i++) {
			if (!points1[i].toPoint2D().equals(points2[i].toPoint2D())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Builds an edit which restores the given transition points on an edge. The
	 * points are copied each time they are applied: the edge then moves its own
	 * points (drags change them in place), never the ones kept in the history.
	 */
	static UndoableEdit createEdit(final IGraph graph, final IEditorPartSelectionHandler selectionHandler, final IEdge edge, final ITransitionPoint[] transitionPointsBeforeChanges, final ITransitionPoint[] transitionPointsAfterChanges) {
		final Id edgeId = edge.getId();
		final ITransitionPoint[] before = EdgeTransitionPoint.copyOf(transitionPointsBeforeChanges);
		final ITransitionPoint[] after = EdgeTransitionPoint.copyOf(transitionPointsAfterChanges);
		return new AbstractUndoableEdit() {
			@Override
			public void undo() throws CannotUndoException {
				super.undo();
				apply(before);
			}

			@Override
			public void redo() throws CannotRedoException {
				super.redo();
				apply(after);
			}

			private void apply(ITransitionPoint[] transitionPoints) {
				IEdge edgeOnGraph = graph.findEdge(edgeId);
				if (edgeOnGraph == null) {
					return;
				}
				edgeOnGraph.setTransitionPoints(EdgeTransitionPoint.copyOf(transitionPoints));
				removeAllTransitionPointsFromSelection(selectionHandler);
			}
		};
	}

	private static void removeAllTransitionPointsFromSelection(IEditorPartSelectionHandler selectionHandler) {
		if (selectionHandler == null) {
			return;
		}
		List<ISelectable> selectableListToRemove = new ArrayList<>();
		for (ISelectable aSelectable : selectionHandler.getSelectedElements()) {
			if (ITransitionPoint.class.isInstance(aSelectable)) {
				selectableListToRemove.add(aSelectable);
			}
		}
		for (ISelectable aSelectableToRemove : selectableListToRemove) {
			selectionHandler.removeElementFromSelection(aSelectableToRemove);
		}
	}

}
