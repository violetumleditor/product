package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

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
import com.horstmann.violet.framework.theme.FlatLightTheme;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;

/**
 * Ctrl + drag moves the view from an empty spot and from a grouping area (which is neither moved nor selected), and the open
 * hand shows as soon as Ctrl is down over such a spot, in a real workspace
 */
@ExtendWith(OnEventDispatchThread.class)
class CtrlDragGraphTest
{
    private Workspace workspace;
    private IEditorPart editorPart;
    private JComponent diagram;
    private JScrollPane scrollPane;
    private ClassNode node;
    private ClassNode otherNode;
    private ContainerNode area;

    @BeforeAll
    public static void registerBeans()
    {
        final ITheme theme = new FlatLightTheme();
        new ThemeManager()
        {
            @Override
            public ITheme getTheme()
            {
                return theme;
            }
        };
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
        this.workspace = new Workspace(new GraphFile(ClassDiagramGraph.class));
        this.editorPart = this.workspace.getEditorPart();
        this.diagram = this.editorPart.getSwingComponent();
        this.node = new ClassNode();
        this.otherNode = new ClassNode();
        this.area = new ContainerNode();
        assertTrue(this.editorPart.getGraph().addNode(this.node, new Point2D.Double(100, 100)));
        assertTrue(this.editorPart.getGraph().addNode(this.area, new Point2D.Double(400, 100)));
        assertTrue(this.editorPart.getGraph().addNode(this.otherNode, new Point2D.Double(3000, 2000)));
        this.scrollPane = this.workspace.getAWTComponent().getScrollableEditorPart();
        this.scrollPane.setSize(800, 600);
        this.scrollPane.doLayout();
        this.scrollPane.getViewport().doLayout();
        this.editorPart.ensureVisible(this.node.getBounds()); // first layout of the drawing area
        this.scrollPane.doLayout();
        this.scrollPane.getViewport().doLayout();
        this.editorPart.ensureVisible(this.node.getBounds());
        this.editorPart.getSelectionHandler().setSelectedElement(this.otherNode);
    }

    @Test
    void shouldMoveTheViewFromAGroupingArea()
    {
        Point2D before = this.editorPart.toGraphPoint(this.scrollPane.getViewport().getViewPosition());
        Point2D areaLocation = this.area.getLocation();
        Point2D press = getCenterOnComponent(this.area);

        fire(MouseEvent.MOUSE_PRESSED, press, 500, 400, true);
        fire(MouseEvent.MOUSE_DRAGGED, press, 440, 370, true);
        fire(MouseEvent.MOUSE_RELEASED, press, 440, 370, true);
        fire(MouseEvent.MOUSE_CLICKED, press, 440, 370, true);

        assertEquals(new Point2D.Double(before.getX() + 60, before.getY() + 30),
                this.editorPart.toGraphPoint(this.scrollPane.getViewport().getViewPosition()));
        assertEquals(areaLocation, this.area.getLocation());
        assertEquals(List.of(this.otherNode), this.editorPart.getSelectionHandler().getSelectedElements());
    }

    @Test
    void shouldShowTheOpenHandWithCtrlOverAnAreaOrAnEmptySpotOnly()
    {
        Point2D overArea = getCenterOnComponent(this.area);
        fire(MouseEvent.MOUSE_MOVED, overArea, 500, 400, false);
        assertEquals(Cursor.getDefaultCursor(), this.diagram.getCursor());

        fire(MouseEvent.MOUSE_MOVED, overArea, 500, 400, true);
        Cursor openHand = this.diagram.getCursor();
        assertNotEquals(Cursor.getDefaultCursor(), openHand);

        Point2D emptySpot = this.editorPart.toComponentPoint(new Point2D.Double(250, 500));
        fire(MouseEvent.MOUSE_MOVED, emptySpot, 500, 400, true);
        assertEquals(openHand, this.diagram.getCursor());

        // Over a class, Ctrl + click selects: no hand
        fire(MouseEvent.MOUSE_MOVED, getCenterOnComponent(this.node), 500, 400, true);
        assertEquals(Cursor.getDefaultCursor(), this.diagram.getCursor());

        // Held then released with Ctrl still down: the open hand again
        fire(MouseEvent.MOUSE_MOVED, overArea, 500, 400, true);
        fire(MouseEvent.MOUSE_PRESSED, overArea, 500, 400, true);
        fire(MouseEvent.MOUSE_RELEASED, overArea, 500, 400, true);
        assertEquals(openHand, this.diagram.getCursor());
        fire(MouseEvent.MOUSE_RELEASED, overArea, 500, 400, false);
        assertEquals(Cursor.getDefaultCursor(), this.diagram.getCursor());
    }

    private Point2D getCenterOnComponent(INode element)
    {
        Rectangle2D bounds = element.getBounds();
        return this.editorPart.toComponentPoint(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
    }

    private void fire(int id, Point2D point, int xOnScreen, int yOnScreen, boolean isCtrl)
    {
        boolean isMove = id == MouseEvent.MOUSE_MOVED;
        int modifiers = isCtrl ? InputEvent.CTRL_DOWN_MASK : 0;
        this.diagram.dispatchEvent(new MouseEvent(this.diagram, id, System.currentTimeMillis(), modifiers, (int) point.getX(),
                (int) point.getY(), xOnScreen, yOnScreen, isMove ? 0 : 1, false, isMove ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }
}
