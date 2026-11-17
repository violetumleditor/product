package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

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
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.InterfaceNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * A class or an interface dragged and dropped on a package moves into it, undoably; it never goes out of it. A package never goes into another one
 */
class PackageDropTest
{

    private ClassDiagramGraph graph;

    private PackageNode pkg;

    private ClassNode cls;

    private DragSelectedBehavior dragBehavior;

    private UndoRedoCompoundBehavior undoRedoBehavior;

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
        this.graph = new ClassDiagramGraph();
        this.pkg = new PackageNode();
        assertTrue(this.graph.addNode(this.pkg, new Point2D.Double(100, 50)));
        this.cls = new ClassNode();
        assertTrue(this.graph.addNode(this.cls, new Point2D.Double(400, 60)));
        assertNull(this.cls.getParent());
        EditorPart editorPart = new EditorPart(this.graph);
        editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.dragBehavior = new DragSelectedBehavior(editorPart, null);
        this.undoRedoBehavior = new UndoRedoCompoundBehavior(editorPart);
        paint();
    }

    @Test
    void shouldMoveIntoThePackageItIsDroppedOn()
    {
        drag(this.cls, new Point2D.Double(140, 110));

        assertSame(this.pkg, this.cls.getParent());
        assertTrue(this.pkg.getChildren().contains(this.cls));
        assertTrue(!this.graph.getAllNodes().stream().filter(n -> n.getParent() == null).toList().contains(this.cls));
        assertEquals(2, this.graph.getAllNodes().size());
    }

    @Test
    void shouldMoveAnInterfaceIntoAPackage()
    {
        InterfaceNode anInterface = new InterfaceNode();
        assertTrue(this.graph.addNode(anInterface, new Point2D.Double(400, 300)));
        paint();

        drag(anInterface, new Point2D.Double(140, 110));

        assertSame(this.pkg, anInterface.getParent());
    }

    @Test
    void shouldStayInItsPackageWhenMovedInsideIt()
    {
        drag(this.cls, new Point2D.Double(140, 110));
        assertSame(this.pkg, this.cls.getParent());

        // Down, then right: in the part of the package that only exists because of the class
        Point2D location = this.cls.getLocationOnGraph();
        drag(this.cls, new Point2D.Double(location.getX() + 10, location.getY() + 70));
        assertSame(this.pkg, this.cls.getParent());
        location = this.cls.getLocationOnGraph();
        drag(this.cls, new Point2D.Double(location.getX() + 60, location.getY() + 20));

        assertSame(this.pkg, this.cls.getParent());
        assertTrue(this.pkg.getChildren().contains(this.cls));
        assertEquals(2, this.graph.getAllNodes().size());
    }

    @Test
    void shouldStayInItsPackageWhenDroppedOnTheEmptyDiagram()
    {
        drag(this.cls, new Point2D.Double(140, 110));

        drag(this.cls, new Point2D.Double(600, 400));

        assertSame(this.pkg, this.cls.getParent());
        assertEquals(2, this.graph.getAllNodes().size());
    }

    @Test
    void shouldBeUndoneAndRedone()
    {
        Point2D locationBefore = this.cls.getLocation();
        drag(this.cls, new Point2D.Double(140, 110));
        Point2D locationInPackage = this.cls.getLocation();

        this.undoRedoBehavior.undo();
        assertNull(this.cls.getParent());
        assertTrue(!this.pkg.getChildren().contains(this.cls));
        assertEquals(locationBefore, this.cls.getLocation());
        assertEquals(2, this.graph.getAllNodes().size());

        this.undoRedoBehavior.redo();
        assertSame(this.pkg, this.cls.getParent());
        assertEquals(locationInPackage, this.cls.getLocation());
        assertEquals(2, this.graph.getAllNodes().size());
    }

    @Test
    void shouldNotMoveAPackageIntoAnotherOne()
    {
        PackageNode otherPackage = new PackageNode();
        assertTrue(this.graph.addNode(otherPackage, new Point2D.Double(400, 300)));
        paint();

        drag(otherPackage, new Point2D.Double(140, 110));

        assertNull(otherPackage.getParent());
        assertTrue(this.pkg.getChildren().isEmpty());
        // Neither by the tool: nothing is added on a package
        assertTrue(!this.graph.canAddNode(new PackageNode(), new Point2D.Double(140, 110)));
    }

    private void drag(INode node, Point2D target)
    {
        Point2D location = node.getLocationOnGraph();
        Point2D start = new Point2D.Double(location.getX() + 10, location.getY() + 10);
        send(MouseEvent.MOUSE_PRESSED, start);
        send(MouseEvent.MOUSE_DRAGGED, new Point2D.Double((start.getX() + target.getX()) / 2, (start.getY() + target.getY()) / 2));
        paint();
        send(MouseEvent.MOUSE_DRAGGED, target);
        paint();
        send(MouseEvent.MOUSE_RELEASED, target);
        paint();
    }

    private void send(int id, Point2D p)
    {
        MouseEvent event = new MouseEvent(new JPanel(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) p.getX(), (int) p.getY(), 1, false,
                MouseEvent.BUTTON1);
        switch (id)
        {
        case MouseEvent.MOUSE_PRESSED:
            this.dragBehavior.onMousePressed(event);
            this.undoRedoBehavior.onMousePressed(event);
            break;
        case MouseEvent.MOUSE_DRAGGED:
            this.dragBehavior.onMouseDragged(event);
            this.undoRedoBehavior.onMouseDragged(event);
            break;
        default:
            this.dragBehavior.onMouseReleased(event);
            this.undoRedoBehavior.onMouseReleased(event);
        }
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
