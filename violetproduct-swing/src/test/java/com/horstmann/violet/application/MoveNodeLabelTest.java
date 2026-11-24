package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import com.horstmann.violet.product.diagram.abstracts.node.IMovableLabelNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.ProcessGatewayNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessStartEventNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessTaskNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.MoveLabelBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * The text a node draws outside (BPMN gateway, event) is dragged anywhere, like the texts of the edges: the node itself stays
 */
class MoveNodeLabelTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private ProcessGatewayNode gateway;

    private ProcessStartEventNode event;

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
        this.graph = new GraphFile(ProcessDiagramGraph.class).getGraph();
        this.gateway = new ProcessGatewayNode();
        assertTrue(this.graph.addNode(this.gateway, new Point2D.Double(200, 200)));
        this.gateway.getName().setText("In stock?");
        this.event = new ProcessStartEventNode();
        assertTrue(this.graph.addNode(this.event, new Point2D.Double(500, 100)));
        this.event.getName().setText("Order received");
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        // Same order as in the workspace
        this.editorPart.getBehaviorManager().addBehavior(new SelectByClickBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new MoveLabelBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new DragSelectedBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        paint();
    }

    @Test
    void shouldMoveTheTextAndNotTheNode()
    {
        for (IMovableLabelNode node : new IMovableLabelNode[] { this.gateway, this.event })
        {
            Rectangle2D before = node.getOutsideLabelBounds();
            Point2D location = node.getLocation();

            drag(center(before), 37, 83);

            Rectangle2D after = node.getOutsideLabelBounds();
            assertEquals(location, node.getLocation());
            assertEquals(before.getX() + 37, after.getX(), 10);
            assertEquals(before.getY() + 83, after.getY(), 10);
            Point2D corner = new Point2D.Double(after.getX(), after.getY());
            assertEquals(corner, this.graph.getGridSticker().snap(corner));
            assertSame(node, this.graph.findNode(center(after)));
        }
    }

    @Test
    void shouldFollowTheNodeDraggedByItsShape()
    {
        drag(center(this.gateway.getOutsideLabelBounds()), 40, 100);
        Rectangle2D before = this.gateway.getOutsideLabelBounds();
        Point2D location = this.gateway.getLocation();

        drag(center(this.gateway.getBounds()), 100, 50);

        Rectangle2D after = this.gateway.getOutsideLabelBounds();
        double dx = this.gateway.getLocation().getX() - location.getX();
        assertTrue(dx > 0);
        assertEquals(before.getX() + dx, after.getX(), 0.01);
    }

    @Test
    void shouldGoBackHomeWhenReleasedNearIt()
    {
        Rectangle2D before = this.gateway.getOutsideLabelBounds();
        drag(center(before), 50, 70);
        Rectangle2D moved = this.gateway.getOutsideLabelBounds();

        drag(center(moved), (int) (before.getX() - moved.getX()) + 4, (int) (before.getY() - moved.getY()) + 3);

        assertNull(this.gateway.getNameOffset());
    }

    @Test
    void shouldUndoAndRedoTheMoveInOneStep()
    {
        Rectangle2D before = this.gateway.getOutsideLabelBounds();
        drag(center(before), 40, 60);
        Rectangle2D after = this.gateway.getOutsideLabelBounds();

        this.undoRedo.undo();
        assertEquals(before, this.gateway.getOutsideLabelBounds());

        this.undoRedo.redo();
        assertEquals(after, this.gateway.getOutsideLabelBounds());
    }

    @Test
    void shouldKeepTheMoveWhenSavedAndReloadedAndCopied() throws Exception
    {
        drag(center(this.gateway.getOutsideLabelBounds()), 40, 60);
        Point2D.Double offset = this.gateway.getNameOffset();
        assertNotNull(offset);
        assertEquals(offset, this.gateway.clone().getNameOffset());

        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        assertEquals(2, reloaded.getAllNodes().size());
        for (INode node : reloaded.getAllNodes())
        {
            assertEquals(node instanceof ProcessGatewayNode ? offset : null, ((IMovableLabelNode) node).getNameOffset());
        }
    }

    @Test
    void shouldTakeTheTextRatherThanTheLineCrossingIt()
    {
        ProcessTaskNode task = new ProcessTaskNode();
        assertTrue(this.graph.addNode(task, new Point2D.Double(470, 300)));
        GenericEdge edge = new GenericEdge();
        edge.setBentStyle(BentStyle.STRAIGHT);
        assertTrue(this.graph.connect(edge, this.event, new Point2D.Double(), task, new Point2D.Double()));
        paint();
        Rectangle2D before = this.event.getOutsideLabelBounds();
        assertTrue(edge.contains(center(before)));

        drag(center(before), 100, 0);

        assertNotNull(this.event.getNameOffset());
        assertSame(edge, this.graph.findEdge(center(before)));
    }

    @Test
    void shouldHoldTheMovedTextInTheBoundsOfTheGraph()
    {
        drag(center(this.event.getOutsideLabelBounds()), 200, 300);

        assertTrue(this.graph.getClipBounds().contains(this.event.getOutsideLabelBounds()));
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
