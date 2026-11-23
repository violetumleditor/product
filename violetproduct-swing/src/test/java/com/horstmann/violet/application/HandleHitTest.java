package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.GraphFile;
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
import com.horstmann.violet.product.diagram.abstracts.node.ResizeDirection;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.BehaviorUtils;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * The resize handles of the selected node are grabbed all around its corners, outside the node too or under another node, and
 * the transition points a few pixels away, the closest first
 */
class HandleHitTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private ContainerNode container;

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
        this.container = new ContainerNode();
        assertTrue(this.graph.addNode(this.container, new Point2D.Double(100, 100)));
        this.editorPart = new EditorPart(this.graph);
        paint();
    }

    @Test
    void shouldGrabACornerFromOutsideTheNode()
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.container);
        Point2D corner = getBottomRightCorner();

        assertEquals(ResizeDirection.SE, BehaviorUtils.findResizeDirection(this.editorPart,
                mouse(MouseEvent.MOUSE_MOVED, corner.getX() + 7, corner.getY() + 7)));
    }

    @Test
    void shouldGrabACornerUnderAnotherNode()
    {
        Point2D corner = getBottomRightCorner();
        ClassNode aClass = new ClassNode();
        assertTrue(this.graph.addNode(aClass, new Point2D.Double(corner.getX() - 30, corner.getY() - 30)));
        paint();
        this.editorPart.getSelectionHandler().setSelectedElement(this.container);

        assertEquals(ResizeDirection.SE, BehaviorUtils.findResizeDirection(this.editorPart,
                mouse(MouseEvent.MOUSE_MOVED, corner.getX() - 2, corner.getY() - 2)));
    }

    @Test
    void shouldHaveNoHandleWhenTheNodeIsNotSelected()
    {
        Point2D corner = getBottomRightCorner();

        assertNull(BehaviorUtils.findResizeDirection(this.editorPart, mouse(MouseEvent.MOUSE_MOVED, corner.getX(), corner.getY())));
    }

    @Test
    void shouldKeepTheSelectionOnAPressOnTheOuterPartOfAHandle()
    {
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        SelectByClickBehavior selectBehavior = new SelectByClickBehavior(this.editorPart, toolsBar);
        this.editorPart.getSelectionHandler().setSelectedElement(this.container);
        Point2D corner = getBottomRightCorner();

        selectBehavior.onMousePressed(mouse(MouseEvent.MOUSE_PRESSED, corner.getX() + 7, corner.getY() + 7));

        assertTrue(this.editorPart.getSelectionHandler().isElementAlreadySelected(this.container));
    }

    @Test
    void shouldFindTheClosestTransitionPointAFewPixelsAway()
    {
        ClassNode start = new ClassNode();
        assertTrue(this.graph.addNode(start, new Point2D.Double(400, 100)));
        ClassNode end = new ClassNode();
        assertTrue(this.graph.addNode(end, new Point2D.Double(700, 100)));
        GenericEdge edge = new GenericEdge();
        edge.setBentStyle(BentStyle.FREE);
        assertTrue(this.graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
        edge.setTransitionPoints(new ITransitionPoint[] { new EdgeTransitionPoint(500, 300), new EdgeTransitionPoint(506, 300) });
        ITransitionPoint first = edge.getTransitionPoints()[0];
        ITransitionPoint second = edge.getTransitionPoints()[1];

        assertSame(second, edge.findTransitionPoint(new Point2D.Double(505, 300)));
        assertSame(first, edge.findTransitionPoint(new Point2D.Double(500, 308)));
        assertNull(edge.findTransitionPoint(new Point2D.Double(500, 312)));
    }

    private Point2D getBottomRightCorner()
    {
        return new Point2D.Double(this.container.getBounds().getMaxX(), this.container.getBounds().getMaxY());
    }

    private MouseEvent mouse(int id, double x, double y)
    {
        return new MouseEvent(new JPanel(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) x, (int) y, 1, false, MouseEvent.BUTTON1);
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
