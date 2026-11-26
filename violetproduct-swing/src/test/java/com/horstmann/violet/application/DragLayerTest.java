package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.product.diagram.abstracts.GeometryCache;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;

/**
 * While nodes are dragged in a class diagram, the editor draws what doesn't move once in an image, then only the moving
 * elements: each step must look exactly as if everything were drawn again, in a real workspace.
 */
@ExtendWith(OnEventDispatchThread.class)
class DragLayerTest
{
    private Workspace workspace;
    private IEditorPart editorPart;
    private JComponent diagram;
    private JScrollPane scrollPane;

    @BeforeAll
    static void registerBeans()
    {
        NavigationToolTest.registerBeans();
    }

    @BeforeEach
    void createDiagram()
    {
        createDiagram(6, 4);
    }

    private void createDiagram(int columns, int rows)
    {
        createWorkspace(ClassDiagramGraph.class);
        RenderingOptimizationTest.fillClassDiagram(this.editorPart.getGraph(), columns, rows, true);
        layOut();
    }

    private void createWorkspace(Class<? extends IGraph> graphClass)
    {
        this.workspace = new Workspace(new GraphFile(graphClass));
        this.editorPart = this.workspace.getEditorPart();
        this.diagram = this.editorPart.getSwingComponent();
    }

    private void layOut()
    {
        this.scrollPane = this.workspace.getAWTComponent().getScrollableEditorPart();
        this.scrollPane.setSize(1200, 800);
        for (int i = 0; i < 3; i++)
        {
            this.scrollPane.doLayout();
            this.scrollPane.getViewport().doLayout();
            this.editorPart.ensureVisible(new Rectangle2D.Double(0, 0, 10, 10));
        }
    }

    @AfterEach
    void enableAgain()
    {
        GeometryCache.setEnabled(true);
    }

    @Test
    void shouldDrawEachStepAsAFullPaint()
    {
        INode node = this.editorPart.getGraph().findNode(new Point2D.Double(300 + 20, 260 + 10));
        dragAndCompare(node, 30, 15, 6);
    }

    @Test
    void shouldDrawEachStepOfAnActivityDiagram()
    {
        createWorkspace(ActivityDiagramGraph.class);
        RenderingOptimizationTest.fillActivityDiagram(this.editorPart.getGraph(), 4);
        layOut();
        // A side activity, its transitions are routed again at each step
        INode node = this.editorPart.getGraph().findNode(new Point2D.Double(450 + 20, 160 + 10));
        dragAndCompare(node, 20, 12, 5);
    }

    @Test
    void shouldDrawEachStepOfALasso()
    {
        Point2D press = this.editorPart.toComponentPoint(new Point2D.Double(250, 200));
        fire(MouseEvent.MOUSE_PRESSED, press);
        paint();
        for (int i = 1; i <= 4; i++)
        {
            fire(MouseEvent.MOUSE_DRAGGED, new Point2D.Double(press.getX() + i * 60, press.getY() + i * 50));
            BufferedImage layered = paint();
            GeometryCache.setEnabled(false);
            BufferedImage full = paint();
            GeometryCache.setEnabled(true);
            assertSameImage("lasso step " + i, full, layered);
        }
        fire(MouseEvent.MOUSE_RELEASED, new Point2D.Double(press.getX() + 240, press.getY() + 200));
    }

    @Test
    void shouldPaintTheHoveredElementsUponTheImageOfTheDiagram()
    {
        paint();
        INode node = this.editorPart.getGraph().findNode(new Point2D.Double(300 + 20, 260 + 10));
        for (int i = 0; i < 12; i++)
        {
            // Over nodes, edges and the void: the halo moves, the graph doesn't change
            Point2D point = this.editorPart.toComponentPoint(new Point2D.Double(40 + i * 70, 60 + i * 45));
            this.diagram.dispatchEvent(new MouseEvent(this.diagram, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0,
                    (int) point.getX(), (int) point.getY(), 0, false));
            BufferedImage cached = paint();
            GeometryCache.setEnabled(false);
            BufferedImage full = paint();
            GeometryCache.setEnabled(true);
            assertSameImage("hover " + i, full, cached);
        }
        // A change of the graph followed by its repaint is drawn
        node.setLocation(new Point2D.Double(node.getLocation().getX() + 40, node.getLocation().getY()));
        this.diagram.repaint();
        BufferedImage cached = paint();
        GeometryCache.setEnabled(false);
        BufferedImage full = paint();
        GeometryCache.setEnabled(true);
        assertSameImage("after a change", full, cached);
    }

    @Test
    void shouldDrawEachStepWithAPackage()
    {
        PackageNode packageNode = new PackageNode();
        assertTrue(this.editorPart.getGraph().addNode(packageNode, new Point2D.Double(40, 900)));
        ClassNode child = new ClassNode();
        assertTrue(packageNode.addChild(child, new Point2D.Double(10, 40)));
        dragAndCompare(child, 20, 10, 5);
    }

    /**
     * Not a check: prints how long a step of a drag takes (mouse event, then paint) on a large class diagram, without and with
     * the optimizations
     */
    @Test
    void printDragTimes()
    {
        createDiagram(15, 10);
        double[] times = new double[2];
        INode node = this.editorPart.getGraph().findNode(new Point2D.Double(300 + 20, 260 + 10));
        for (int round = 0; round < 6; round++)
        {
            boolean isOptimized = round % 2 == 1;
            GeometryCache.setEnabled(isOptimized);
            Point2D press = this.editorPart.toComponentPoint(new Point2D.Double(node.getLocationOnGraph().getX() + 15,
                    node.getLocationOnGraph().getY() + 8));
            fire(MouseEvent.MOUSE_PRESSED, press);
            paint();
            long start = System.nanoTime();
            int sign = round % 4 < 2 ? 1 : -1;
            for (int i = 1; i <= 30; i++)
            {
                fire(MouseEvent.MOUSE_DRAGGED, new Point2D.Double(press.getX() + sign * i * 4, press.getY() + sign * i * 2));
                paint();
            }
            if (round >= 2)
            {
                times[isOptimized ? 1 : 0] += (System.nanoTime() - start) / 1e6 / 30 / 2;
            }
            fire(MouseEvent.MOUSE_RELEASED, new Point2D.Double(press.getX() + sign * 120, press.getY() + sign * 60));
        }
        System.out.println(String.format(java.util.Locale.ROOT,
                "[perf] drag in a class diagram of 150 classes: %.1f ms -> %.1f ms per step (x%.1f)", times[0], times[1],
                times[0] / times[1]));
    }

    private void dragAndCompare(INode node, int dx, int dy, int steps)
    {
        Point2D press = this.editorPart.toComponentPoint(new Point2D.Double(node.getLocationOnGraph().getX() + 15,
                node.getLocationOnGraph().getY() + 8));
        fire(MouseEvent.MOUSE_PRESSED, press);
        paint();
        for (int i = 1; i <= steps; i++)
        {
            Point2D point = new Point2D.Double(press.getX() + i * dx, press.getY() + i * dy);
            fire(MouseEvent.MOUSE_DRAGGED, point);
            BufferedImage layered = paint();
            GeometryCache.setEnabled(false);
            BufferedImage full = paint();
            GeometryCache.setEnabled(true);
            assertSameImage("step " + i, full, layered);
        }
        Point2D release = new Point2D.Double(press.getX() + steps * dx, press.getY() + steps * dy);
        fire(MouseEvent.MOUSE_RELEASED, release);
        assertTrue(node.getLocationOnGraph().getX() > 0);
    }

    /**
     * @return the visible part of the diagram, painted as Swing does it
     */
    private BufferedImage paint()
    {
        Rectangle visible = this.scrollPane.getViewport().getViewRect();
        BufferedImage image = new BufferedImage(visible.width, visible.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.MAGENTA);
        g2.fillRect(0, 0, visible.width, visible.height);
        g2.translate(-visible.x, -visible.y);
        g2.setClip(visible);
        this.diagram.invalidate();
        this.diagram.paint(g2);
        g2.dispose();
        return image;
    }

    private static void assertSameImage(String name, BufferedImage expected, BufferedImage actual)
    {
        int differences = 0;
        String first = null;
        for (int x = 0; x < expected.getWidth(); x++)
        {
            for (int y = 0; y < expected.getHeight(); y++)
            {
                if (expected.getRGB(x, y) != actual.getRGB(x, y))
                {
                    differences++;
                    first = first == null ? x + "," + y : first;
                }
            }
        }
        assertEquals(0, differences, name + ": pixels differing, first at " + first);
    }

    private void fire(int id, Point2D point)
    {
        this.diagram.dispatchEvent(new MouseEvent(this.diagram, id, System.currentTimeMillis(), 0, (int) point.getX(),
                (int) point.getY(), 500, 400, 1, false, MouseEvent.BUTTON1));
    }
}
