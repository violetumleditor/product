package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;

import javax.swing.JPanel;

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
import com.horstmann.violet.product.diagram.abstracts.edge.EdgeTransitionPoint;
import com.horstmann.violet.product.diagram.abstracts.edge.ITransitionPoint;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.MoveSelectedByKeyboardBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByLassoBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * The lasso selects the transition points inside it, and the selected transition points move with the selection, by mouse or
 * keyboard, undoably
 */
class TransitionPointMoveTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private IEditorPartSelectionHandler selection;

    private UndoRedoCompoundBehavior undoRedo;

    private SelectByLassoBehavior lassoBehavior;

    private DragSelectedBehavior dragBehavior;

    private MoveSelectedByKeyboardBehavior keyboardBehavior;

    private ClassNode start;

    private ClassNode end;

    private GenericEdge edge;

    private ITransitionPoint point;

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
        IGraphFile graphFile = new GraphFile(ClassDiagramGraph.class);
        this.graph = graphFile.getGraph();
        this.start = new ClassNode();
        assertTrue(this.graph.addNode(this.start, new Point2D.Double(100, 100)));
        this.end = new ClassNode();
        assertTrue(this.graph.addNode(this.end, new Point2D.Double(500, 100)));
        this.edge = new GenericEdge();
        assertTrue(this.graph.connect(this.edge, this.start, new Point2D.Double(), this.end, new Point2D.Double()));
        this.edge.setTransitionPoints(new ITransitionPoint[] { new EdgeTransitionPoint(250, 300) });
        this.point = this.edge.getTransitionPoints()[0];
        this.editorPart = new EditorPart(this.graph);
        this.selection = this.editorPart.getSelectionHandler();
        this.selection.setSelectedTool(GraphTool.SELECTION_TOOL);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        this.lassoBehavior = new SelectByLassoBehavior(this.editorPart, toolsBar);
        this.dragBehavior = new DragSelectedBehavior(this.editorPart, toolsBar);
        this.keyboardBehavior = new MoveSelectedByKeyboardBehavior(this.editorPart, graphFile);
        this.editorPart.getBehaviorManager().addBehavior(this.keyboardBehavior);
        paint();
    }

    @Test
    void shouldSelectTheTransitionPointsInsideTheLasso()
    {
        lasso(new Point2D.Double(50, 50), new Point2D.Double(300, 320));

        assertTrue(this.selection.isElementAlreadySelected(this.start));
        assertTrue(this.selection.isElementAlreadySelected(this.point));
        assertFalse(this.selection.isElementAlreadySelected(this.end));
        assertFalse(this.selection.isElementAlreadySelected(this.edge)); // only partly inside
    }

    @Test
    void shouldNotSelectTheTransitionPointsOutsideTheLasso()
    {
        lasso(new Point2D.Double(50, 50), new Point2D.Double(300, 250));

        assertTrue(this.selection.isElementAlreadySelected(this.start));
        assertFalse(this.selection.isElementAlreadySelected(this.point));
    }

    @Test
    void shouldDragTheSelectedTransitionPointsWithTheNodes()
    {
        lasso(new Point2D.Double(50, 50), new Point2D.Double(300, 320));

        drag(new Point2D.Double(120, 120), new Point2D.Double(170, 140));

        assertEquals(new Point2D.Double(150, 120), this.start.getLocation());
        assertEquals(new Point2D.Double(300, 320), this.point.toPoint2D());
        assertEquals(new Point2D.Double(500, 100), this.end.getLocation());

        this.undoRedo.undo();
        assertEquals(new Point2D.Double(100, 100), this.start.getLocation());
        assertEquals(new Point2D.Double(250, 300), this.edge.getTransitionPoints()[0].toPoint2D());
    }

    @Test
    void shouldMoveTheSelectedTransitionPointsWithTheKeyboard()
    {
        lasso(new Point2D.Double(50, 50), new Point2D.Double(300, 320));

        this.keyboardBehavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);
        this.keyboardBehavior.commitMove();

        assertEquals(new Point2D.Double(110, 100), this.start.getLocation());
        assertEquals(new Point2D.Double(260, 300), this.point.toPoint2D());

        this.undoRedo.undo();
        assertEquals(new Point2D.Double(100, 100), this.start.getLocation());
        assertEquals(new Point2D.Double(250, 300), this.edge.getTransitionPoints()[0].toPoint2D());
    }

    @Test
    void shouldMoveASelectedTransitionPointAloneWithTheKeyboard()
    {
        lasso(new Point2D.Double(200, 250), new Point2D.Double(300, 350));
        assertEquals(1, this.selection.getSelectedElements().size());

        assertTrue(this.keyboardBehavior.moveSelectedNodes(KeyEvent.VK_DOWN, false));

        assertEquals(new Point2D.Double(250, 310), this.point.toPoint2D());
        assertEquals(new Point2D.Double(100, 100), this.start.getLocation());
    }

    @Test
    void shouldMoveAPointOnceWhenItsEdgeIsBetweenMovedNodes()
    {
        lasso(new Point2D.Double(50, 50), new Point2D.Double(800, 400));
        assertTrue(this.selection.isElementAlreadySelected(this.point));

        this.keyboardBehavior.moveSelectedNodes(KeyEvent.VK_RIGHT, false);

        assertEquals(new Point2D.Double(260, 300), this.point.toPoint2D());
    }

    private void lasso(Point2D from, Point2D to)
    {
        this.lassoBehavior.onMousePressed(mouse(MouseEvent.MOUSE_PRESSED, from));
        this.lassoBehavior.onMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, to));
        this.lassoBehavior.onMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, to));
    }

    private void drag(Point2D from, Point2D to)
    {
        MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, from);
        this.dragBehavior.onMousePressed(press);
        this.undoRedo.onMousePressed(press);
        MouseEvent drag = mouse(MouseEvent.MOUSE_DRAGGED, to);
        this.dragBehavior.onMouseDragged(drag);
        this.undoRedo.onMouseDragged(drag);
        MouseEvent release = mouse(MouseEvent.MOUSE_RELEASED, to);
        this.dragBehavior.onMouseReleased(release);
        this.undoRedo.onMouseReleased(release);
    }

    private MouseEvent mouse(int id, Point2D p)
    {
        return new MouseEvent(new JPanel(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) p.getX(), (int) p.getY(), 1, false,
                MouseEvent.BUTTON1);
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
