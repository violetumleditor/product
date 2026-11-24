package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateNode;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.MoveLabelBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A text of an edge is dragged anywhere: its top left corner on the grid, it keeps its offset from its automatic place when the
 * nodes move, goes back there when released near it, and the drag is undone in one step and saved
 */
@ExtendWith(OnEventDispatchThread.class)
class MoveEdgeLabelTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private ClassNode end;

    private GenericEdge edge;

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
        this.graph = new GraphFile(ClassDiagramGraph.class).getGraph();
        ClassNode start = new ClassNode();
        assertTrue(this.graph.addNode(start, new Point2D.Double(100, 100)));
        this.end = new ClassNode();
        assertTrue(this.graph.addNode(this.end, new Point2D.Double(500, 100)));
        this.edge = new GenericEdge();
        this.edge.setBentStyle(BentStyle.STRAIGHT);
        assertTrue(this.graph.connect(this.edge, start, new Point2D.Double(), this.end, new Point2D.Double()));
        this.edge.setMiddleLabel("calls");
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        this.editorPart.getBehaviorManager().addBehavior(new MoveLabelBehavior(this.editorPart, toolsBar));
        paint();
    }

    @Test
    void shouldMoveTheTextWithItsCornerOnTheGrid()
    {
        Rectangle2D before = this.edge.getMiddleLabelBounds();

        drag(center(before), 37, 53);

        Rectangle2D after = this.edge.getMiddleLabelBounds();
        assertNotNull(this.edge.getMiddleLabelOffset());
        assertEquals(before.getX() + 37, after.getX(), 10);
        assertEquals(before.getY() + 53, after.getY(), 10);
        Point2D corner = new Point2D.Double(after.getX(), after.getY());
        assertEquals(corner, this.graph.getGridSticker().snap(corner));
    }

    @Test
    void shouldFollowWhenANodeMoves()
    {
        drag(center(this.edge.getMiddleLabelBounds()), 40, 60);
        Point2D.Double offset = this.edge.getMiddleLabelOffset();
        Rectangle2D defaultBefore = this.edge.getDefaultLabelBounds(SegmentedLineEdge.MIDDLE_LABEL);

        this.end.translate(100, 200);
        paint();

        Rectangle2D defaultAfter = this.edge.getDefaultLabelBounds(SegmentedLineEdge.MIDDLE_LABEL);
        Rectangle2D after = this.edge.getMiddleLabelBounds();
        assertTrue(defaultAfter.getX() != defaultBefore.getX() || defaultAfter.getY() != defaultBefore.getY());
        assertEquals(defaultAfter.getX() + offset.getX(), after.getX(), 0.01);
        assertEquals(defaultAfter.getY() + offset.getY(), after.getY(), 0.01);
    }

    @Test
    void shouldGoBackHomeWhenReleasedNearIt()
    {
        Rectangle2D before = this.edge.getMiddleLabelBounds();
        drag(center(before), 50, 50);
        Rectangle2D moved = this.edge.getMiddleLabelBounds();

        drag(center(moved), (int) (before.getX() - moved.getX()) + 4, (int) (before.getY() - moved.getY()) + 3);

        assertNull(this.edge.getMiddleLabelOffset());
        assertEquals(before, this.edge.getMiddleLabelBounds());
    }

    @Test
    void shouldUndoTheMoveInOneStep()
    {
        Rectangle2D before = this.edge.getMiddleLabelBounds();
        drag(center(before), 40, 60);

        this.undoRedo.undo();

        assertNull(this.edge.getMiddleLabelOffset());
        assertEquals(before, this.edge.getMiddleLabelBounds());
    }

    @Test
    void shouldKeepTheMoveWhenSavedAndReloaded() throws Exception
    {
        drag(center(this.edge.getMiddleLabelBounds()), 40, 60);
        Point2D.Double offset = this.edge.getMiddleLabelOffset();

        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        GenericEdge reloadedEdge = (GenericEdge) reloaded.getAllEdges().iterator().next();
        assertEquals(offset, reloadedEdge.getMiddleLabelOffset());
        assertNull(reloadedEdge.getStartLabelOffset());
    }

    @Test
    void shouldMoveTheTextOfAStateTransition() throws Exception
    {
        this.graph = new GraphFile(StateDiagramGraph.class).getGraph();
        StateNode startState = new StateNode();
        StateNode endState = new StateNode();
        assertTrue(this.graph.addNode(startState, new Point2D.Double(100, 100)));
        assertTrue(this.graph.addNode(endState, new Point2D.Double(500, 200)));
        StateTransitionEdge transition = new StateTransitionEdge();
        assertTrue(this.graph.connect(transition, startState, new Point2D.Double(), endState, new Point2D.Double()));
        transition.setLabel("fire");
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        this.editorPart.getBehaviorManager().addBehavior(new MoveLabelBehavior(this.editorPart, null));
        paint();
        Rectangle2D before = transition.getMiddleLabelBounds();

        drag(center(before), 40, 60);

        Rectangle2D after = transition.getMiddleLabelBounds();
        Point2D corner = new Point2D.Double(after.getX(), after.getY());
        assertEquals(before.getY() + 60, after.getY(), 10);
        assertEquals(corner, this.graph.getGridSticker().snap(corner));
        assertTrue(transition.contains(center(after)));
        assertTrue(transition.getBounds().contains(after));

        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(transition.getLabelOffset(), ((StateTransitionEdge) reloaded.getAllEdges().iterator().next()).getLabelOffset());

        this.undoRedo.undo();
        assertNull(transition.getLabelOffset());
        assertEquals(before, transition.getMiddleLabelBounds());
        this.undoRedo.redo();
        assertEquals(after, transition.getMiddleLabelBounds());
    }

    private void drag(Point2D from, int dx, int dy)
    {
        Point2D to = new Point2D.Double(from.getX() + dx, from.getY() + dy);
        this.editorPart.getBehaviorManager().fireOnMousePressed(mouse(MouseEvent.MOUSE_PRESSED, from));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,
                new Point2D.Double((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2)));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, to));
        this.editorPart.getBehaviorManager().fireOnMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, to));
        paint();
    }

    private MouseEvent mouse(int id, Point2D p)
    {
        return new MouseEvent(this.editorPart, id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) Math.round(p.getX()), (int) Math.round(p.getY()),
                1, false, MouseEvent.BUTTON1);
    }

    private Point2D center(Rectangle2D bounds)
    {
        assertNotNull(bounds);
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
