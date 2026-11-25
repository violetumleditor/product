package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.AddNodeByTapBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A tap on an empty spot while something is selected only clears the selection; the next one leaves the "+". A double click there
 * follows the same rule: it opens the choice of nodes only when nothing was selected.
 */
@ExtendWith(OnEventDispatchThread.class)
class AddNodeByTapSelectionTest
{

    private static final Point2D EMPTY_SPOT = new Point2D.Double(600, 400);

    private EditorPart editorPart;

    private AddNodeByTapBehavior tapBehavior;

    private ClassNode node;

    private Point2D mousePoint = EMPTY_SPOT;

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
        IGraph graph = new GraphFile(ClassDiagramGraph.class).getGraph();
        this.node = new ClassNode();
        assertTrue(graph.addNode(this.node, new Point2D.Double(100, 100)));
        this.editorPart = new EditorPart(graph);
        this.editorPart.setSize(900, 600);
        final List<GraphTool> nodeTools = List.of(new GraphTool(new ClassNode(), "Class"));
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class }, (proxy, method, args) -> {
                    if ("getSelectedTool".equals(method.getName())) return GraphTool.SELECTION_TOOL;
                    if ("getNodeTools".equals(method.getName())) return nodeTools;
                    return null;
                });
        // In the order of the workspace: the "+" before the selection, which clears it on the press
        this.tapBehavior = new AddNodeByTapBehavior(this.editorPart, toolsBar);
        this.editorPart.getBehaviorManager().addBehavior(this.tapBehavior);
        this.editorPart.getBehaviorManager().addBehavior(new SelectByClickBehavior(this.editorPart, toolsBar));
        graph.draw(new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    @Test
    void shouldOnlyDeselectOnTheFirstTapThenShowThePlusOnTheNext() throws Exception
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.node);

        click(1);

        assertTrue(this.editorPart.getSelectionHandler().getSelectedElements().isEmpty());
        assertNull(getSpotPoint());

        click(1);

        assertNotNull(getSpotPoint());
    }

    @Test
    void shouldOnlyDeselectOnADoubleClickWithASelection() throws Exception
    {
        this.editorPart.getSelectionHandler().setSelectedElement(this.node);

        click(1);
        MouseEvent secondPress = click(2);

        assertTrue(this.editorPart.getSelectionHandler().getSelectedElements().isEmpty());
        assertFalse(secondPress.isConsumed(), "the second press opened the choice of nodes");
        assertNull(getSpotPoint());
    }

    @Test
    void shouldOpenTheChoiceOnADoubleClickWithoutSelection() throws Exception
    {
        click(1);
        assertNotNull(getSpotPoint());

        MouseEvent secondPress = click(2);

        assertTrue(secondPress.isConsumed(), "the second press should be taken by the \"+\", which opens the choice");
    }

    @Test
    void shouldOnlyRemoveThePlusOnATapElsewhere() throws Exception
    {
        click(1);
        assertNotNull(getSpotPoint());

        this.mousePoint = new Point2D.Double(300, 500);
        click(1);
        assertNull(getSpotPoint(), "a tap elsewhere should not leave another \"+\"");

        click(1);
        assertNotNull(getSpotPoint());
    }

    /**
     * @return the press of this click
     */
    private MouseEvent click(int clickCount)
    {
        MouseEvent press = mouse(MouseEvent.MOUSE_PRESSED, clickCount);
        this.editorPart.getBehaviorManager().fireOnMousePressed(press);
        this.editorPart.getBehaviorManager().fireOnMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, clickCount));
        this.editorPart.getBehaviorManager().fireOnMouseClicked(mouse(MouseEvent.MOUSE_CLICKED, clickCount));
        return press;
    }

    private MouseEvent mouse(int id, int clickCount)
    {
        return new MouseEvent(this.editorPart, id, 0, 0, (int) this.mousePoint.getX(), (int) this.mousePoint.getY(), clickCount, false,
                MouseEvent.BUTTON1);
    }

    private Point2D getSpotPoint() throws Exception
    {
        Method method = AddNodeByTapBehavior.class.getDeclaredMethod("getSpotPoint");
        method.setAccessible(true);
        return (Point2D) method.invoke(this.tapBehavior);
    }
}
