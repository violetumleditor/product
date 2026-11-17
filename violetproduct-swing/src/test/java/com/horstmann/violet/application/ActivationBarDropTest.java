package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * An activation bar dragged and dropped on another lifeline or activation bar moves into it, undoably
 */
class ActivationBarDropTest
{

    private SequenceDiagramGraph graph;

    private LifelineNode firstLifeline;

    private LifelineNode secondLifeline;

    private ActivationBarNode bar;

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
        this.graph = new SequenceDiagramGraph();
        this.firstLifeline = new LifelineNode();
        this.secondLifeline = new LifelineNode();
        assertTrue(this.graph.addNode(this.firstLifeline, new Point2D.Double(100, 0)));
        assertTrue(this.graph.addNode(this.secondLifeline, new Point2D.Double(300, 0)));
        this.bar = new ActivationBarNode();
        assertTrue(this.graph.addNode(this.bar, new Point2D.Double(130, 90)));
        assertSame(this.firstLifeline, this.bar.getParent());
        EditorPart editorPart = new EditorPart(this.graph);
        editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.dragBehavior = new DragSelectedBehavior(editorPart, null);
        this.undoRedoBehavior = new UndoRedoCompoundBehavior(editorPart);
        paint();
    }

    @Test
    void shouldMoveIntoTheLifelineItIsDroppedOn()
    {
        // Grabbed 10 px below its top
        drag(this.bar, new Point2D.Double(340, 160));

        assertSame(this.secondLifeline, this.bar.getParent());
        assertTrue(this.secondLifeline.getChildren().contains(this.bar));
        assertTrue(!this.firstLifeline.getChildren().contains(this.bar));
        assertEquals(150, this.bar.getLocationOnGraph().getY(), 0.001);
        assertSame(this.secondLifeline, this.bar.getImplicitParameter());
    }

    @Test
    void shouldMoveIntoTheActivationBarItIsDroppedOn()
    {
        ActivationBarNode otherBar = new ActivationBarNode();
        assertTrue(this.graph.addNode(otherBar, new Point2D.Double(330, 100)));
        assertSame(this.secondLifeline, otherBar.getParent());
        paint();

        Point2D otherBarCenter = getCenter(otherBar);
        drag(this.bar, otherBarCenter);

        assertSame(otherBar, this.bar.getParent());
        assertSame(this.secondLifeline, this.bar.getImplicitParameter());
    }

    @Test
    void shouldGoBackToItsFormerParentOnUndo()
    {
        Point2D locationBefore = this.bar.getLocation();
        drag(this.bar, new Point2D.Double(340, 160));
        Point2D locationAfter = this.bar.getLocation();

        this.undoRedoBehavior.undo();
        assertSame(this.firstLifeline, this.bar.getParent());
        assertTrue(!this.secondLifeline.getChildren().contains(this.bar));
        assertEquals(locationBefore, this.bar.getLocation());

        this.undoRedoBehavior.redo();
        assertSame(this.secondLifeline, this.bar.getParent());
        assertTrue(!this.firstLifeline.getChildren().contains(this.bar));
        assertEquals(locationAfter, this.bar.getLocation());
    }

    @Test
    void shouldNotMoveIntoItsOwnNestedBar()
    {
        ActivationBarNode nestedBar = new ActivationBarNode();
        Point2D barLocation = this.bar.getLocationOnGraph();
        assertTrue(this.bar.addChild(nestedBar, new Point2D.Double(0, 20)));
        paint();
        assertTrue(barLocation.getY() < nestedBar.getLocationOnGraph().getY());

        drag(this.bar, getCenter(nestedBar));

        assertSame(this.firstLifeline, this.bar.getParent());
        assertSame(this.bar, nestedBar.getParent());
    }

    @Test
    void shouldStayInItsLifelineWhenMovedAlongIt()
    {
        drag(this.bar, new Point2D.Double(140, 200));

        assertSame(this.firstLifeline, this.bar.getParent());
        assertEquals(1, this.firstLifeline.getChildren().size());
    }

    private void drag(INode node, Point2D target)
    {
        Point2D start = getCenter(node);
        send(MouseEvent.MOUSE_PRESSED, start);
        Point2D middle = new Point2D.Double((start.getX() + target.getX()) / 2, (start.getY() + target.getY()) / 2);
        send(MouseEvent.MOUSE_DRAGGED, middle);
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

    private Point2D getCenter(INode node)
    {
        Point2D location = node.getLocationOnGraph();
        return new Point2D.Double(location.getX() + node.getBounds().getWidth() / 2, location.getY() + 10);
    }

    /** As on screen: lifelines learn their common height, bars reset their location cache */
    private void paint()
    {
        BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
