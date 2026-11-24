package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.MarkerNode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.workspace.Workspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Highlighter strokes drawn freehand upon the other elements, in a real workspace
 */
@ExtendWith(OnEventDispatchThread.class)
class MarkerNodeTest
{
    private Workspace workspace;
    private IEditorPart editorPart;
    private IGraph graph;
    private ClassNode classNode;

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
        this.workspace = new Workspace(new GraphFile(ClassDiagramGraph.class));
        this.editorPart = this.workspace.getEditorPart();
        this.editorPart.getSwingComponent().setSize(900, 600);
        this.graph = this.editorPart.getGraph();
        this.classNode = new ClassNode();
        assertTrue(this.graph.addNode(this.classNode, new Point2D.Double(100, 100)));
    }

    @Test
    void shouldDrawStrokesUponTheOtherElementsAndKeepTheTool()
    {
        selectMarkerTool();
        stroke(90, 115, 220, 121);
        stroke(300, 300, 303, 340);

        assertEquals(2, getMarkers().size());
        assertTrue(isMarkerToolSelected());
        assertTrue(this.editorPart.getSelectionHandler().getSelectedElements().isEmpty());
        assertEquals(new Point2D.Double(100, 100), this.classNode.getLocation());
        assertSame(getMarkers().get(0), this.graph.findNode(new Point2D.Double(150, 118)));
        assertSame(this.classNode, this.graph.findNodeUnderMarkers(new Point2D.Double(150, 118)));
        assertEquals(0, getMarkers().get(0).getPointsOnGraph().get(0).distance(90, 115), 0.2);
    }

    @Test
    void shouldLeaveADotOnAClickAndNothingWhenTheToolIsChosen()
    {
        selectMarkerTool();
        assertTrue(getMarkers().isEmpty());
        stroke(300, 300, 300, 300);

        assertEquals(1, getMarkers().size());
        BufferedImage image = draw();
        assertNotEquals(Color.WHITE.getRGB(), image.getRGB(300, 300));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(320, 300));
    }

    @Test
    void shouldUndoAndRedoOneStrokeAtATime()
    {
        selectMarkerTool();
        stroke(300, 300, 400, 300);
        stroke(300, 400, 400, 400);
        List<Point2D> points = getMarkers().get(1).getPointsOnGraph();
        UndoRedoCompoundBehavior undoRedo = this.editorPart.getBehaviorManager().getBehaviors(UndoRedoCompoundBehavior.class).get(0);

        undoRedo.undo();
        assertEquals(1, getMarkers().size());
        undoRedo.redo();
        assertEquals(2, getMarkers().size());
        assertEquals(points, getMarkers().get(1).getPointsOnGraph());
    }

    @Test
    void shouldTakeItsStyleFromTheStyleTools()
    {
        Workspace otherWorkspace = new Workspace(new GraphFile(ClassDiagramGraph.class));
        selectMarkerTool();
        paint();
        assertEquals(ColorChoice.PASTEL_YELLOW, this.workspace.getSideBar().getColorChoiceBar().getShownColorChoice());
        ((ColorToolsBarPanel) this.workspace.getSideBar().getColorChoiceBar()).fireColorChoiceChanged(ColorChoice.PASTEL_GREEN);
        stroke(300, 300, 400, 300);

        assertEquals(ColorChoice.PASTEL_GREEN.getBorderColor(), getMarkers().get(0).getBorderColor());
        paint();
        assertEquals(ColorChoice.PASTEL_GREEN, this.workspace.getSideBar().getColorChoiceBar().getShownColorChoice());
        // The tool icon of this diagram only
        assertEquals(ColorChoice.PASTEL_GREEN.getBorderColor().getRGB(), drawToolIcon(this.workspace).getRGB(20, 36));
        assertEquals(ColorChoice.PASTEL_YELLOW.getBorderColor().getRGB(), drawToolIcon(otherWorkspace).getRGB(20, 36));
        assertEquals(4, this.workspace.getSideBar().getThicknessChoiceBar().getShownThickness());
    }

    @Test
    void shouldBeSelectedAndMovedWithTheSelectionTool()
    {
        selectMarkerTool();
        stroke(303, 307, 404, 409);
        getTools().reset();
        MarkerNode marker = getMarkers().get(0);
        Point2D before = marker.getLocation();
        stroke(350, 354, 450, 354);

        assertEquals(1, getMarkers().size());
        assertEquals(List.of(marker), this.editorPart.getSelectionHandler().getSelectedElements());
        assertEquals(before.getX() + 100, marker.getLocation().getX(), 0.01);
        assertEquals(before.getY(), marker.getLocation().getY(), 0.01);
        this.editorPart.removeSelected();
        assertTrue(getMarkers().isEmpty());
    }

    @Test
    void shouldOnlyBeFoundAlongItsStroke()
    {
        selectMarkerTool();
        stroke(300, 300, 400, 400);

        assertNotNull(this.graph.findNode(new Point2D.Double(350, 350)));
        assertNull(this.graph.findNode(new Point2D.Double(390, 310)));
    }

    @Test
    void shouldStayInPlaceWhenItsThicknessChanges()
    {
        selectMarkerTool();
        stroke(303, 307, 404, 409);
        MarkerNode marker = getMarkers().get(0);
        List<Point2D> before = marker.getPointsOnGraph();

        marker.setBorderWidth(10);

        for (int i = 0; i < before.size(); i++)
        {
            assertEquals(0, before.get(i).distance(marker.getPointsOnGraph().get(i)), 0.2);
        }
        assertTrue(marker.getBounds().contains(303 - 20, 307 - 20, 101, 102));
        assertEquals(marker.getLocation(), this.graph.getGridSticker().snap(marker.getLocation()));
    }

    @Test
    void shouldLeaveTheElementsUnderItUsable()
    {
        IGraph sequence = new SequenceDiagramGraph();
        LifelineNode lifeline = new LifelineNode();
        sequence.addNode(lifeline, new Point2D.Double(100, 100));
        double x = lifeline.getBounds().getCenterX();
        double y = lifeline.getBounds().getMaxY() - 20;
        MarkerNode marker = new MarkerNode();
        marker.setPointsOnGraph(List.of(new Point2D.Double(x - 50, y), new Point2D.Double(x + 50, y)));
        sequence.addNode(marker, marker.getLocation());
        INode bar = new ActivationBarNode();

        assertSame(marker, sequence.findNode(new Point2D.Double(x, y)));
        assertTrue(sequence.addNode(bar, new Point2D.Double(x, y)));
        assertSame(lifeline, bar.getParent());
    }

    @Test
    void shouldNeverBeLinked()
    {
        MarkerNode marker = new MarkerNode();
        marker.setPointsOnGraph(List.of(new Point2D.Double(300, 300)));
        this.graph.addNode(marker, marker.getLocation());
        Point2D anyPoint = new Point2D.Double();

        assertFalse(this.graph.connect(new GenericEdge(), this.classNode, anyPoint, marker, anyPoint));
    }

    @Test
    void shouldBeSavedAndReloaded() throws Exception
    {
        selectMarkerTool();
        stroke(303, 307, 404, 409);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XMLPersistenceService().write(this.graph, out);
        IGraph reloaded = new XMLPersistenceService().read(new ByteArrayInputStream(out.toByteArray()));

        MarkerNode before = getMarkers().get(0);
        MarkerNode after = (MarkerNode) reloaded.getAllNodes().stream().filter(MarkerNode.class::isInstance).findFirst().get();
        assertEquals(before.getPointsOnGraph(), after.getPointsOnGraph());
        assertEquals(before.getBounds(), after.getBounds());
        assertEquals(before.getMarkerColor(), after.getMarkerColor());
    }

    private BufferedImage drawToolIcon(Workspace aWorkspace)
    {
        BufferedImage image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        for (GraphTool aTool : aWorkspace.getSideBar().getGraphToolsBar().getNodeTools())
        {
            if (aTool.getNodeOrEdge() instanceof MarkerNode)
            {
                ((MarkerNode) aTool.getNodeOrEdge()).draw(image.createGraphics());
            }
        }
        return image;
    }

    private IGraphToolsBar getTools()
    {
        return this.workspace.getSideBar().getGraphToolsBar();
    }

    private boolean isMarkerToolSelected()
    {
        return getTools().getSelectedTool().getNodeOrEdge() instanceof MarkerNode;
    }

    private void selectMarkerTool()
    {
        for (GraphTool aTool : getTools().getNodeTools())
        {
            if (aTool.getNodeOrEdge() instanceof MarkerNode)
            {
                // As the press then the release of its button
                getTools().setSelectedTool(aTool);
                getTools().notifyMouseEvent(aTool, mouse(MouseEvent.MOUSE_PRESSED, 1, 1));
                getTools().notifyMouseEvent(aTool, mouse(MouseEvent.MOUSE_RELEASED, 1, 1));
            }
        }
        assertTrue(isMarkerToolSelected());
    }

    private void stroke(int x1, int y1, int x2, int y2)
    {
        send(MouseEvent.MOUSE_PRESSED, x1, y1);
        for (int i = 1; i <= 10 && (x1 != x2 || y1 != y2); i++)
        {
            send(MouseEvent.MOUSE_DRAGGED, x1 + (x2 - x1) * i / 10, y1 + (y2 - y1) * i / 10);
        }
        send(MouseEvent.MOUSE_RELEASED, x2, y2);
    }

    private void send(int id, int x, int y)
    {
        this.editorPart.getSwingComponent().dispatchEvent(mouse(id, x, y));
    }

    private MouseEvent mouse(int id, int x, int y)
    {
        return new MouseEvent(this.editorPart.getSwingComponent(), id, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                MouseEvent.BUTTON1);
    }

    /** Oldest first */
    private List<MarkerNode> getMarkers()
    {
        List<MarkerNode> markers = new ArrayList<MarkerNode>();
        for (INode aNode : this.graph.getAllNodes())
        {
            if (aNode instanceof MarkerNode)
            {
                markers.add(0, (MarkerNode) aNode);
            }
        }
        return markers;
    }

    private void paint()
    {
        this.editorPart.getSwingComponent().invalidate();
        Graphics2D g2 = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB).createGraphics();
        this.editorPart.getSwingComponent().paint(g2);
        g2.dispose();
    }

    private BufferedImage draw()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 900, 600);
        this.graph.draw(g2);
        g2.dispose();
        return image;
    }
}
