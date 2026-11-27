package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.RotateNodeBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A free text turns around its center, by the handle off its top or by its angle in the dialog
 */
@ExtendWith(OnEventDispatchThread.class)
class FreeTextRotationTest
{

    private GraphFile graphFile;

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private FreeTextNode text;

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
        this.text = new FreeTextNode();
        this.text.getText().setText("A rather long free text");
        assertTrue(this.graph.addNode(this.text, new Point2D.Double(200, 200)));
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        // Same order as in the workspace
        this.editorPart.getBehaviorManager().addBehavior(new RotateNodeBehavior(this.editorPart, toolsBar, this.graphFile));
        this.editorPart.getBehaviorManager().addBehavior(new SelectByClickBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new DragSelectedBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        this.editorPart.getSelectionHandler().setSelectedElement(this.text);
        paint();
    }

    @Test
    void shouldTurnAroundItsCenter()
    {
        Rectangle2D before = this.text.getBounds();
        double width = this.text.getUnrotatedWidth();
        double height = this.text.getUnrotatedHeight();
        assertEquals(before.getWidth(), width, 0.001);
        assertEquals(before.getHeight(), height, 0.001);

        this.text.setRotation(90.0);

        Rectangle2D after = this.text.getBounds();
        assertEquals(before.getCenterX(), after.getCenterX(), 0.001);
        assertEquals(before.getCenterY(), after.getCenterY(), 0.001);
        assertEquals(height, after.getWidth(), 0.001);
        assertEquals(width, after.getHeight(), 0.001);
        assertEquals(after.getX(), this.text.getLocation().getX(), 0.001); // the bounds still start at its location

        this.text.setRotation(null);
        assertEquals(before, this.text.getBounds());
    }

    @Test
    void shouldBringTheAngleBackFrom0To360()
    {
        this.text.setRotation(-90.0);
        assertEquals(270.0, this.text.getRotation());
        this.text.setRotation(405.0);
        assertEquals(45.0, this.text.getRotation());
        this.text.setRotation(360.0);
        assertNull(this.text.getRotation());
        this.text.setRotation(0.0);
        assertNull(this.text.getRotation());
    }

    @Test
    void shouldOnlyCatchTheMouseOnTheTurnedText()
    {
        this.text.setRotation(45.0);
        Rectangle2D bounds = this.text.getBounds();
        Point2D corner = new Point2D.Double(bounds.getX() + 3, bounds.getY() + 3);
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());

        assertFalse(this.text.contains(corner));
        assertNull(this.graph.findNode(corner));
        assertSame(this.text, this.graph.findNode(center));
        List<Point2D> corners = this.text.getSelectionPoints();
        assertEquals(4, corners.size());
        for (Point2D aCorner : corners)
        {
            assertEquals(Math.hypot(this.text.getUnrotatedWidth(), this.text.getUnrotatedHeight()) / 2, aCorner.distance(center), 0.001);
        }
    }

    @Test
    void shouldDrawItsTextTurnedSoThatItIsEditedInPlaceTurned()
    {
        this.text.setRotation(90.0);
        paint();

        AffineTransform transform = this.text.getText().getDrawnTransform();
        assertEquals(0, transform.getScaleX(), 0.001);
        assertEquals(1, transform.getShearY(), 0.001);
        Rectangle2D drawnArea = this.text.getText().getDrawnArea();
        assertTrue(drawnArea.getHeight() > drawnArea.getWidth(), drawnArea.toString());
    }

    @Test
    void shouldTurnByStepsWithTheHandleAndUndoInOneStep()
    {
        Point2D location = this.text.getLocation();
        Rectangle2D bounds = this.text.getBounds();
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        Point2D handle = new Point2D.Double(center.getX(), bounds.getMinY() - 26);

        // A quarter turn clockwise, a bit off: by steps of 15 degrees
        drag(handle, new Point2D.Double(center.getX() + 80, center.getY() + 4), 0);

        assertEquals(90.0, this.text.getRotation());
        Rectangle2D turned = this.text.getBounds();
        assertEquals(center.getX(), turned.getCenterX(), 0.001);
        assertEquals(center.getY(), turned.getCenterY(), 0.001);
        assertTrue(this.graphFile.isSaveRequired());

        this.undoRedo.undo();
        assertNull(this.text.getRotation());
        assertEquals(location, this.text.getLocation());

        this.undoRedo.redo();
        assertEquals(90.0, this.text.getRotation());
    }

    @Test
    void shouldTurnByAnyAngleWithShift()
    {
        Rectangle2D bounds = this.text.getBounds();
        Point2D center = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
        Point2D handle = new Point2D.Double(center.getX(), bounds.getMinY() - 26);

        drag(handle, new Point2D.Double(center.getX() + 50, center.getY() - 30), InputEvent.SHIFT_DOWN_MASK);

        double degrees = this.text.getRotation();
        assertNotEquals(0, degrees % 15, 0.5);
        assertEquals(Math.toDegrees(Math.atan2(-30, 50)) + 90, degrees, 2);
    }

    @Test
    void shouldKeepItsAngleWhenSavedAndReloaded() throws Exception
    {
        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        assertFalse(write(persistenceService).contains("rotation"), "nothing written while not turned");

        this.text.setRotation(30.0);
        Point2D location = this.text.getLocation();
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(write(persistenceService).getBytes(StandardCharsets.UTF_8)));

        INode node = reloaded.getAllNodes().iterator().next();
        assertEquals(30.0, ((FreeTextNode) node).getRotation());
        assertEquals(location, node.getLocation());
        assertEquals(30.0, this.text.clone().getRotation());
    }

    private String write(IFilePersistenceService persistenceService) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private void drag(Point2D from, Point2D to, int modifiers)
    {
        Point2D fromOnScreen = this.editorPart.toComponentPoint(from);
        Point2D toOnScreen = this.editorPart.toComponentPoint(to);
        this.editorPart.getBehaviorManager().fireOnMousePressed(mouse(MouseEvent.MOUSE_PRESSED, fromOnScreen, modifiers));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,
                new Point2D.Double((fromOnScreen.getX() + toOnScreen.getX()) / 2, (fromOnScreen.getY() + toOnScreen.getY()) / 2),
                modifiers));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, toOnScreen, modifiers));
        this.editorPart.getBehaviorManager().fireOnMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, toOnScreen, modifiers));
        paint();
    }

    private MouseEvent mouse(int id, Point2D p, int modifiers)
    {
        return new MouseEvent(this.editorPart, id, 0, MouseEvent.BUTTON1_DOWN_MASK | modifiers, (int) Math.round(p.getX()),
                (int) Math.round(p.getY()), 1, false, MouseEvent.BUTTON1);
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
