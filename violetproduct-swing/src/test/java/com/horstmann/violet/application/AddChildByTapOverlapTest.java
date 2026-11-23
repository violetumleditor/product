package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

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
import com.horstmann.violet.workspace.editorpart.behavior.AddNodeByTapBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * An activation bar added to a lifeline by its "+" goes below the bars it would cover, never on them: the free spot found beside
 * a bar is not where the lifeline puts it, centered on its line
 */
class AddChildByTapOverlapTest
{

    private SequenceDiagramGraph graph;

    private LifelineNode lifeline;

    private AddNodeByTapBehavior tapBehavior;

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
        this.lifeline = new LifelineNode();
        assertTrue(this.graph.addNode(this.lifeline, new Point2D.Double(100, 0)));
        assertTrue(this.graph.addNode(new ActivationBarNode(), new Point2D.Double(130, 90)));
        EditorPart editorPart = new EditorPart(this.graph);
        editorPart.setSize(900, 600);
        final List<GraphTool> nodeTools = List.of(new GraphTool(new ActivationBarNode(), "Activation bar"));
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class }, (proxy, method, args) -> {
                    if ("getSelectedTool".equals(method.getName())) return GraphTool.SELECTION_TOOL;
                    if ("getNodeTools".equals(method.getName())) return nodeTools;
                    return null;
                });
        this.tapBehavior = new AddNodeByTapBehavior(editorPart, toolsBar);
        editorPart.getBehaviorManager().addBehavior(this.tapBehavior);
        paint();
    }

    @Test
    void shouldPutEachNewBarBelowTheOthers() throws Exception
    {
        for (int i = 0; i < 3; i++)
        {
            Point2D insertionPoint = getInsertionPoint(this.lifeline);
            assertNotNull(insertionPoint);
            addNode(new ActivationBarNode(), insertionPoint);
            paint();
        }

        List<INode> bars = this.lifeline.getChildren();
        assertEquals(4, bars.size());
        for (INode aBar : bars)
        {
            assertSame(this.lifeline, aBar.getParent());
            for (INode anotherBar : bars)
            {
                assertFalse(aBar != anotherBar && aBar.getBounds().intersects(anotherBar.getBounds()),
                        "two activation bars overlap: " + aBar.getBounds() + " and " + anotherBar.getBounds());
            }
        }
    }

    private void paint()
    {
        this.graph.draw(new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    private Point2D getInsertionPoint(INode node) throws Exception
    {
        Method method = AddNodeByTapBehavior.class.getDeclaredMethod("getInsertionPoint", INode.class);
        method.setAccessible(true);
        return (Point2D) method.invoke(this.tapBehavior, node);
    }

    private void addNode(INode prototype, Point2D point) throws Exception
    {
        Method method = AddNodeByTapBehavior.class.getDeclaredMethod("addNode", INode.class, Point2D.class);
        method.setAccessible(true);
        method.invoke(this.tapBehavior, prototype, point);
    }
}
