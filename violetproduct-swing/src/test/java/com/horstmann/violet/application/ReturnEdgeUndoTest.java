package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.ReturnEdge;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;

/**
 * Undoing the removal of a call, of its return or of their bars puts everything back
 */
class ReturnEdgeUndoTest
{
    private SequenceDiagramGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedoBehavior;

    private LifelineNode calleeLifeline;

    private ActivationBarNode caller;

    private ActivationBarNode callee;

    private CallEdge call;

    private ReturnEdge reply;

    @BeforeAll
    static void registerBeans()
    {
        if (BeanFactory.getFactory().contains(IFileChooserService.class))
        {
            return;
        }
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, new JFileChooserService());
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());
    }

    @BeforeEach
    void createDiagram()
    {
        this.graph = new SequenceDiagramGraph();
        LifelineNode callerLifeline = new LifelineNode();
        this.calleeLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(callerLifeline, new Point2D.Double(100, 0)));
        assertTrue(this.graph.addNode(this.calleeLifeline, new Point2D.Double(300, 0)));
        this.caller = new ActivationBarNode();
        this.callee = new ActivationBarNode();
        assertTrue(callerLifeline.addChild(this.caller, new Point2D.Double(0, 100)));
        assertTrue(this.calleeLifeline.addChild(this.callee, new Point2D.Double(0, 120)));
        this.call = new CallEdge();
        this.reply = new ReturnEdge();
        assertTrue(this.graph.connect(this.call, this.caller, new Point2D.Double(), this.callee, new Point2D.Double()));
        assertTrue(this.graph.connect(this.reply, this.callee, new Point2D.Double(), this.caller, new Point2D.Double()));
        this.editorPart = new EditorPart(this.graph);
        this.undoRedoBehavior = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedoBehavior);
        paint();
    }

    @Test
    void shouldPutBackACallAndItsReturn()
    {
        this.editorPart.getSelectionHandler().addSelectedElement(this.call);
        this.editorPart.getSelectionHandler().addSelectedElement(this.reply);
        checkRemovalAndUndo();
    }

    @Test
    void shouldPutBackTheCalledBarAndItsLinks()
    {
        this.editorPart.getSelectionHandler().addSelectedElement(this.callee);
        checkRemovalAndUndo();
    }

    @Test
    void shouldPutBackANestedBarRemovedWithItsParent()
    {
        ActivationBarNode nested = new ActivationBarNode();
        assertTrue(this.callee.addChild(nested, new Point2D.Double(0, 20)));
        paint();
        this.editorPart.getSelectionHandler().addSelectedElement(this.callee);
        this.editorPart.getSelectionHandler().addSelectedElement(nested);
        checkRemovalAndUndo();
        assertTrue(this.callee.getChildren().contains(nested));
    }

    @Test
    void shouldPutBackALifelineAndTheLinksOfItsBars()
    {
        this.editorPart.getSelectionHandler().addSelectedElement(this.calleeLifeline);
        checkRemovalAndUndo();
    }

    private void checkRemovalAndUndo()
    {
        Point2D location = (Point2D) this.callee.getLocationOnGraph().clone();
        this.editorPart.removeSelected();
        assertTrue(this.graph.getAllEdges().isEmpty());
        this.undoRedoBehavior.undo();
        checkDiagram(location);
        this.undoRedoBehavior.redo();
        assertTrue(this.graph.getAllEdges().isEmpty());
        this.undoRedoBehavior.undo();
        checkDiagram(location);
    }

    private void checkDiagram(Point2D location)
    {
        paint();
        assertEquals(2, this.graph.getAllEdges().size());
        assertTrue(this.graph.getAllEdges().contains(this.call));
        assertTrue(this.graph.getAllEdges().contains(this.reply));
        assertSame(this.calleeLifeline, this.callee.getParent());
        assertTrue(this.calleeLifeline.getChildren().contains(this.callee));
        assertTrue(this.graph.getAllNodes().contains(this.callee));
        assertEquals(location, this.callee.getLocationOnGraph());
    }

    private void paint()
    {
        this.graph.draw(new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }
}
