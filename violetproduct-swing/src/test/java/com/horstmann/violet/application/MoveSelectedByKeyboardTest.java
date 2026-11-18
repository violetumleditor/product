package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.IGraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.MoveSelectedByKeyboardBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;

/**
 * The arrow keys move the selected nodes by a grid step (5 with Shift), as a drag does, and a key press until its release is one
 * undoable move
 */
class MoveSelectedByKeyboardTest
{

    private IGraphFile graphFile;

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private MoveSelectedByKeyboardBehavior behavior;

    private ClassNode cls;

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
        this.graphFile = new GraphFile(ClassDiagramGraph.class);
        this.graph = this.graphFile.getGraph();
        this.cls = new ClassNode();
        assertTrue(this.graph.addNode(this.cls, new Point2D.Double(100, 100)));
        this.editorPart = new EditorPart(this.graph);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        this.behavior = new MoveSelectedByKeyboardBehavior(this.editorPart, this.graphFile);
        this.editorPart.getBehaviorManager().addBehavior(this.behavior);
    }

    @Test
    void shouldMoveTheSelectedNodeByOneGridStep()
    {
        select(this.cls);

        assertTrue(this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false));
        assertEquals(new Point2D.Double(110, 100), this.cls.getLocation());
        this.behavior.moveSelectedNodes(KeyEvent.VK_UP, false);
        assertEquals(new Point2D.Double(110, 90), this.cls.getLocation());
        this.behavior.moveSelectedNodes(KeyEvent.VK_KP_LEFT, false);
        assertEquals(new Point2D.Double(100, 90), this.cls.getLocation());
        this.behavior.commitMove();
        assertTrue(this.graphFile.isSaveRequired());
    }

    @Test
    void shouldMoveByFiveStepsWithShift()
    {
        select(this.cls);

        this.behavior.moveSelectedNodes(KeyEvent.VK_DOWN, true);

        assertEquals(new Point2D.Double(100, 150), this.cls.getLocation());
    }

    @Test
    void shouldDoNothingWithoutSelection()
    {
        assertFalse(this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false));
        assertEquals(new Point2D.Double(100, 100), this.cls.getLocation());
        assertFalse(this.graphFile.isSaveRequired());
    }

    @Test
    void shouldNotGoIntoNegativeCoordinates()
    {
        select(this.cls);

        this.behavior.moveSelectedNodes(KeyEvent.VK_LEFT, true);
        this.behavior.moveSelectedNodes(KeyEvent.VK_LEFT, true);
        this.behavior.moveSelectedNodes(KeyEvent.VK_LEFT, true);

        assertEquals(new Point2D.Double(0, 100), this.cls.getLocation());
    }

    @Test
    void shouldUndoAndRedoTheMovesOfOneKeyPressAsOneEdit()
    {
        select(this.cls);
        this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);
        this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);
        this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);
        this.behavior.commitMove();
        this.behavior.moveSelectedNodes(KeyEvent.VK_DOWN, false);
        this.behavior.commitMove();

        this.undoRedo.undo();
        assertEquals(new Point2D.Double(130, 100), this.cls.getLocation());
        this.undoRedo.undo();
        assertEquals(new Point2D.Double(100, 100), this.cls.getLocation());
        this.undoRedo.redo();
        assertEquals(new Point2D.Double(130, 100), this.cls.getLocation());
    }

    @Test
    void shouldMoveTheNodesLyingInASelectedGroupingArea()
    {
        ContainerNode container = new ContainerNode();
        assertTrue(this.graph.addNode(container, new Point2D.Double(50, 50)));
        container.setPreferredSize(new Rectangle2D.Double(0, 0, 400, 300));
        select(container);

        this.behavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);

        assertEquals(new Point2D.Double(60, 50), container.getLocation());
        assertEquals(new Point2D.Double(110, 100), this.cls.getLocation());
    }

    @Test
    void shouldMoveAChildOnlyWithItsParent()
    {
        PackageNode pkg = new PackageNode();
        assertTrue(this.graph.addNode(pkg, new Point2D.Double(300, 300)));
        ClassNode child = new ClassNode();
        assertTrue(pkg.addChild(child, new Point2D.Double(20, 40)));
        Point2D childLocation = child.getLocation();
        select(pkg);
        this.editorPart.getSelectionHandler().addSelectedElement(child);

        this.behavior.moveSelectedNodes(KeyEvent.VK_DOWN, false);

        assertEquals(new Point2D.Double(300, 310), pkg.getLocation());
        assertEquals(childLocation, child.getLocation());
    }

    private void select(com.horstmann.violet.product.diagram.abstracts.node.INode node)
    {
        this.editorPart.getSelectionHandler().clearSelection();
        this.editorPart.getSelectionHandler().addSelectedElement(node);
    }
}
