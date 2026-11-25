package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
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
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * With the navigation tool, a drag moves the view, even from an element, and changes nothing else, in a real workspace
 */
@ExtendWith(OnEventDispatchThread.class)
class NavigationToolTest
{
    private Workspace workspace;
    private IEditorPart editorPart;
    private JComponent diagram;
    private JScrollPane scrollPane;
    private ClassNode node;
    private ClassNode otherNode;

    @BeforeAll
    static void registerBeans()
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
        assertTrue(this.editorPart.getGraph().addNode(this.node, new Point2D.Double(100, 100)));
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
    void shouldFollowTheSelectionTool()
    {
        List<GraphTool> tools = getToolsBar().getNodeTools();
        assertSame(GraphTool.SELECTION_TOOL, tools.get(0));
        assertSame(GraphTool.NAVIGATION_TOOL, tools.get(1));
    }

    @Test
    void shouldMoveOnlyTheViewEvenFromAnElement()
    {
        getToolsBar().setSelectedTool(GraphTool.NAVIGATION_TOOL);
        Point2D before = this.editorPart.toGraphPoint(this.scrollPane.getViewport().getViewPosition());
        Point2D center = new Point2D.Double(this.node.getBounds().getCenterX(), this.node.getBounds().getCenterY());
        Point2D press = this.editorPart.toComponentPoint(center);

        fire(MouseEvent.MOUSE_PRESSED, press, 500, 400);
        fire(MouseEvent.MOUSE_DRAGGED, press, 440, 370);
        fire(MouseEvent.MOUSE_RELEASED, press, 440, 370);
        fire(MouseEvent.MOUSE_CLICKED, press, 440, 370);

        // On the graph: the drawing area may have lost the empty room left behind
        assertEquals(new Point2D.Double(before.getX() + 60, before.getY() + 30),
                this.editorPart.toGraphPoint(this.scrollPane.getViewport().getViewPosition()));
        assertEquals(new Point2D.Double(100, 100), this.node.getLocation());
        assertEquals(List.of(this.otherNode), this.editorPart.getSelectionHandler().getSelectedElements());
        assertSame(GraphTool.NAVIGATION_TOOL, getToolsBar().getSelectedTool());
    }

    @Test
    void shouldShowTheHandOnlyWithThisTool()
    {
        getToolsBar().setSelectedTool(GraphTool.NAVIGATION_TOOL);
        Cursor openHand = this.diagram.getCursor();
        assertNotEquals(Cursor.getDefaultCursor(), openHand);

        // Over a corner of the selected node: no resize cursor
        Point2D corner = new Point2D.Double(this.otherNode.getBounds().getMaxX(), this.otherNode.getBounds().getMaxY());
        Point2D point = this.editorPart.toComponentPoint(corner);
        fire(MouseEvent.MOUSE_MOVED, point, 500, 400);
        assertSame(openHand, this.diagram.getCursor());

        // Tool left while the diagram is held
        fire(MouseEvent.MOUSE_PRESSED, point, 500, 400);
        getToolsBar().reset();
        fire(MouseEvent.MOUSE_RELEASED, point, 500, 400);
        assertEquals(Cursor.getDefaultCursor(), this.diagram.getCursor());
    }

    private IGraphToolsBar getToolsBar()
    {
        return this.workspace.getSideBar().getGraphToolsBar();
    }

    private void fire(int id, Point2D point, int xOnScreen, int yOnScreen)
    {
        boolean isMove = id == MouseEvent.MOUSE_MOVED;
        this.diagram.dispatchEvent(new MouseEvent(this.diagram, id, System.currentTimeMillis(), 0, (int) point.getX(),
                (int) point.getY(), xOnScreen, yOnScreen, isMove ? 0 : 1, false, isMove ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
    }
}
