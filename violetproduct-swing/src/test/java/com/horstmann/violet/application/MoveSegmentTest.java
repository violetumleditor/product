package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.MovedSegments;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.AddTransitionPointBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.DragSelectedBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.EditButtonBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.MoveSegmentBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.MoveSelectedByKeyboardBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * The horizontal and vertical segments of an edge are dragged without changing its bent style; the path is undone in one step,
 * saved, and follows its nodes
 */
@ExtendWith(OnEventDispatchThread.class)
class MoveSegmentTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private MoveSelectedByKeyboardBehavior keyboard;

    private ClassNode start;

    private ClassNode end;

    private GenericEdge edge;

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
        GraphFile graphFile = new GraphFile(ClassDiagramGraph.class);
        this.graph = graphFile.getGraph();
        this.start = new ClassNode();
        assertTrue(this.graph.addNode(this.start, new Point2D.Double(100, 100)));
        this.end = new ClassNode();
        assertTrue(this.graph.addNode(this.end, new Point2D.Double(500, 300)));
        this.edge = new GenericEdge();
        this.edge.setBentStyle(BentStyle.HVH);
        assertTrue(this.graph.connect(this.edge, this.start, new Point2D.Double(), this.end, new Point2D.Double()));
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.editorPart.getSelectionHandler().setSelectedTool(GraphTool.SELECTION_TOOL);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        this.editorPart.getBehaviorManager().addBehavior(new EditButtonBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new MoveSegmentBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new SelectByClickBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new AddTransitionPointBehavior(this.editorPart, toolsBar));
        this.editorPart.getBehaviorManager().addBehavior(new DragSelectedBehavior(this.editorPart, toolsBar));
        this.keyboard = new MoveSelectedByKeyboardBehavior(this.editorPart, graphFile);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        paint();
    }

    @Test
    void shouldMoveTheMiddleSegmentOnly()
    {
        List<Point2D> before = this.edge.getPoints();
        assertEquals(4, before.size());

        dragSegment(1, 63, 40);

        List<Point2D> after = this.edge.getPoints();
        assertEquals(4, after.size());
        assertEquals(before.get(0), after.get(0));
        assertEquals(before.get(3), after.get(3));
        assertEquals(before.get(1).getX() + 60, after.get(1).getX(), 5);
        assertEquals(0, after.get(1).getX() % 10, 0.01, "on the grid");
        assertEquals(BentStyle.HVH, this.edge.getBentStyle());
        assertEquals(0, this.edge.getTransitionPoints().length);
    }

    @Test
    void shouldSlideTheEndAlongTheSide()
    {
        List<Point2D> before = this.edge.getPoints();

        dragSegment(2, 0, 10);

        List<Point2D> after = this.edge.getPoints();
        assertEquals(4, after.size());
        assertTrue(after.get(3).getY() > before.get(3).getY());
        assertEquals(before.get(3).getX(), after.get(3).getX(), 0.01, "same side");
    }

    @Test
    void shouldLeaveByTheNextSidePastTheCorner()
    {
        dragSegment(0, 0, 150);

        List<Point2D> after = this.edge.getPoints();
        Rectangle2D bounds = this.start.getBounds();
        assertEquals(bounds.getCenterX(), after.get(0).getX(), 0.01);
        assertEquals(bounds.getMaxY(), after.get(0).getY(), 1, "by the bottom side");
        assertStraightSegments(after);
    }

    @Test
    void shouldLeaveStraightFromTheNodeItIsBroughtUpon()
    {
        Point2D middle = getMiddle(1);

        dragSegment(1, (int) (this.start.getBounds().getCenterX() - middle.getX()), 0);

        List<Point2D> after = this.edge.getPoints();
        assertEquals(3, after.size(), "down then right");
        assertEquals(this.start.getBounds().getMaxY(), after.get(0).getY(), 1);
        assertStraightSegments(after);
    }

    @Test
    void shouldFoldASingleSegmentIntoAU()
    {
        this.end.translate(0, -200);
        paint();
        assertEquals(1, this.edge.getRoute().getSegmentCount());

        dragSegment(0, 0, -100);

        List<Point2D> after = this.edge.getPoints();
        assertEquals(4, after.size());
        assertEquals(this.start.getBounds().getMinY(), after.get(0).getY(), 1);
        assertEquals(this.end.getBounds().getMinY(), after.get(3).getY(), 1);

        dragSegment(1, 0, 100);
        assertFalse(this.edge.isPathAdapted(), "back to the automatic path");
    }

    @Test
    void shouldGoBackToTheAutomaticPath()
    {
        List<Point2D> before = this.edge.getPoints();
        dragSegment(1, 60, 0);
        assertTrue(this.edge.isPathAdapted());

        dragSegment(1, -57, 0);

        assertNull(this.edge.getMovedSegments());
        assertEquals(before, this.edge.getPoints());
    }

    @Test
    void shouldStayInPlaceWhenANodeMovesAndFollowWhenBothDo()
    {
        dragSegment(1, 60, 0);
        double x = this.edge.getPoints().get(1).getX();
        double y = this.edge.getPoints().get(0).getY();

        this.start.translate(-20, 10);
        paint();
        assertEquals(x, this.edge.getPoints().get(1).getX(), 0.01);
        assertEquals(y + 10, this.edge.getPoints().get(0).getY(), 0.01);

        this.editorPart.getSelectionHandler().clearSelection();
        this.editorPart.getSelectionHandler().addSelectedElement(this.start);
        this.editorPart.getSelectionHandler().addSelectedElement(this.end);
        this.keyboard.moveSelectedNodes(KeyEvent.VK_RIGHT, true);
        this.keyboard.commitMove();
        assertEquals(x + 50, this.edge.getPoints().get(1).getX(), 0.01);
        this.undoRedo.undo();
        assertEquals(x, this.edge.getPoints().get(1).getX(), 0.01);

        drag(new Point2D.Double(this.start.getBounds().getCenterX(), this.start.getBounds().getCenterY()), 40, 0);
        assertEquals(x + 40, this.edge.getPoints().get(1).getX(), 0.01);
        this.undoRedo.undo();
        assertEquals(x, this.edge.getPoints().get(1).getX(), 0.01);
    }

    @Test
    void shouldUndoTheMoveInOneStep()
    {
        List<Point2D> before = this.edge.getPoints();
        dragSegment(1, 60, 0);
        List<Point2D> after = this.edge.getPoints();

        this.undoRedo.undo();
        assertEquals(before, this.edge.getPoints());
        this.undoRedo.redo();
        assertEquals(after, this.edge.getPoints());
    }

    @Test
    void shouldKeepTheMoveWhenSavedAndReloaded() throws Exception
    {
        dragSegment(1, 60, 0);

        IGraph reloaded = reload();

        GenericEdge reloadedEdge = (GenericEdge) reloaded.getAllEdges().iterator().next();
        assertNotNull(reloadedEdge.getMovedSegments());
        assertEquals(this.edge.getMovedSegments(), reloadedEdge.getMovedSegments());
        assertEquals(this.edge.getPoints(), reloadedEdge.getPoints());
    }

    @Test
    void shouldReachTheShapeOfAnEllipse()
    {
        this.graph = new GraphFile(UseCaseDiagramGraph.class).getGraph();
        UseCaseNode startCase = new UseCaseNode();
        UseCaseNode endCase = new UseCaseNode();
        assertTrue(this.graph.addNode(startCase, new Point2D.Double(100, 100)));
        assertTrue(this.graph.addNode(endCase, new Point2D.Double(500, 300)));
        GenericEdge link = new GenericEdge();
        link.setBentStyle(BentStyle.HVH);
        assertTrue(this.graph.connect(link, startCase, new Point2D.Double(), endCase, new Point2D.Double()));

        link.setRoute(link.getRoute().move(0, startCase.getBounds().getMinY() + 10));

        Point2D first = link.getPoints().get(0);
        assertTrue(first.getX() < startCase.getBounds().getMaxX() - 2, "inside the bounds, on the ellipse");
        assertStraightSegments(link.getPoints());
    }

    @Test
    void shouldKeepAnAutomaticEndOffADraggedOne()
    {
        GenericEdge other = connectAgain();
        for (int dy = -20; dy <= 20; dy += 5)
        {
            this.edge.setMovedSegments(null);
            this.edge.setRoute(this.edge.getRoute().move(0, other.getPoints().get(0).getY() + dy));
            assertApart(this.edge, other);
        }
    }

    @Test
    void shouldKeepTwoDraggedEndsApart()
    {
        GenericEdge other = connectAgain();
        double startY = this.start.getBounds().getCenterY();
        double endY = this.end.getBounds().getCenterY();
        this.edge.setMovedSegments(new MovedSegments(true, List.of(startY - 100, 350.0, endY - 300)));
        other.setMovedSegments(this.edge.getMovedSegments());
        assertTrue(this.edge.isPathAdapted() && other.isPathAdapted());
        assertApart(this.edge, other);
        this.start.translate(0, 30);
        assertApart(this.edge, other);
    }

    @Test
    void shouldStoreWhereADraggedEndIsShown()
    {
        GenericEdge other = connectAgain();
        double startY = this.start.getBounds().getCenterY();
        this.edge.setMovedSegments(new MovedSegments(true, List.of(startY - 100, 350.0, 20.0)));
        other.setMovedSegments(new MovedSegments(true, List.of(startY - 80, 420.0, 40.0)));
        Point2D middle = new Point2D.Double(400, other.getPoints().get(0).getY());

        drag(middle, 0, -20);

        List<Point2D> shown = other.getPoints();
        assertApart(this.edge, other);
        this.edge.setMovedSegments(null);
        this.edge.setBentStyle(BentStyle.VH);
        assertEquals(shown.get(0), other.getPoints().get(0));
    }

    @Test
    void shouldKeepTwoEndsMovedPastTheCornerApart()
    {
        GenericEdge other = connectAgain();
        double y = this.start.getBounds().getMaxY();
        this.edge.setRoute(this.edge.getRoute().move(0, y + 40));
        other.setRoute(other.getRoute().move(0, y + 60));

        assertEquals(y, other.getPoints().get(0).getY(), 1, "by the bottom side too");
        assertApart(this.edge, other);
        assertStraightSegments(other.getPoints());
    }

    @Test
    void shouldKeepDraggedEndsApartOnAnEllipse()
    {
        this.graph = new GraphFile(UseCaseDiagramGraph.class).getGraph();
        UseCaseNode startCase = new UseCaseNode();
        UseCaseNode endCase = new UseCaseNode();
        assertTrue(this.graph.addNode(startCase, new Point2D.Double(100, 100)));
        assertTrue(this.graph.addNode(endCase, new Point2D.Double(500, 300)));
        GenericEdge[] links = { new GenericEdge(), new GenericEdge() };
        for (GenericEdge link : links)
        {
            link.setBentStyle(BentStyle.HVH);
            assertTrue(this.graph.connect(link, startCase, new Point2D.Double(), endCase, new Point2D.Double()));
        }
        links[1].setRoute(links[1].getRoute().move(1, 350));
        assertApart(links[0], links[1]);
        links[0].setRoute(links[0].getRoute().move(1, 400));
        assertApart(links[0], links[1]);
    }

    @Test
    void shouldKeepRoomForTheArrowHead()
    {
        this.edge.setEndArrowHead(ArrowHead.TRIANGLE);
        for (int width : new int[] { 1, 5, 10 })
        {
            this.edge.setBorderWidth(width);
            this.edge.setRoute(this.edge.getRoute().move(1, this.end.getBounds().getMinX() - 3));
            List<Point2D> points = this.edge.getPoints();
            assertEquals(4, points.size());
            assertTrue(points.get(2).distance(points.get(3)) > ArrowHead.TRIANGLE.getTotalLength(width));
        }
    }

    private GenericEdge connectAgain()
    {
        GenericEdge other = new GenericEdge();
        other.setBentStyle(BentStyle.HVH);
        assertTrue(this.graph.connect(other, this.start, new Point2D.Double(), this.end, new Point2D.Double()));
        paint();
        return other;
    }

    private void assertApart(GenericEdge edge1, GenericEdge edge2)
    {
        List<Point2D> points1 = edge1.getPoints();
        List<Point2D> points2 = edge2.getPoints();
        String message = points1 + " / " + points2;
        assertTrue(points1.get(0).distance(points2.get(0)) >= 9, "starts " + message);
        assertTrue(points1.get(points1.size() - 1).distance(points2.get(points2.size() - 1)) >= 9, "ends " + message);
    }

    @Test
    void shouldKeepTransitionPointsOfAFreeEdge()
    {
        this.edge.setBentStyle(BentStyle.FREE);
        assertFalse(this.edge.isSegmentsMovable());
        this.edge.setBentStyle(BentStyle.STRAIGHT);
        assertFalse(this.edge.isSegmentsMovable());
    }

    private IGraph reload() throws Exception
    {
        IFilePersistenceService persistenceService = new XHTMLPersistenceService(false);
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(this.graph, out);
        return persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
    }

    private void assertStraightSegments(List<Point2D> points)
    {
        for (int i = 1; i < points.size(); i++)
        {
            double dx = Math.abs(points.get(i).getX() - points.get(i - 1).getX());
            double dy = Math.abs(points.get(i).getY() - points.get(i - 1).getY());
            assertTrue(dx < 0.01 || dy < 0.01, "horizontal or vertical: " + points);
        }
    }

    private Point2D getMiddle(int segment)
    {
        List<Point2D> points = this.edge.getPoints();
        Point2D p = points.get(segment);
        Point2D q = points.get(segment + 1);
        return new Point2D.Double((p.getX() + q.getX()) / 2, (p.getY() + q.getY()) / 2);
    }

    private void dragSegment(int segment, int dx, int dy)
    {
        drag(getMiddle(segment), dx, dy);
    }

    private void drag(Point2D from, int dx, int dy)
    {
        Point2D to = new Point2D.Double(from.getX() + dx, from.getY() + dy);
        this.editorPart.getBehaviorManager().fireOnMousePressed(mouse(MouseEvent.MOUSE_PRESSED, from));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,
                new Point2D.Double((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2)));
        this.editorPart.getBehaviorManager().fireOnMouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, to));
        this.editorPart.getBehaviorManager().fireOnMouseReleased(mouse(MouseEvent.MOUSE_RELEASED, to));
        paint();
    }

    private MouseEvent mouse(int id, Point2D pointOnGraph)
    {
        Point2D p = this.editorPart.toComponentPoint(pointOnGraph);
        return new MouseEvent(this.editorPart, id, 0, MouseEvent.BUTTON1_DOWN_MASK, (int) Math.round(p.getX()), (int) Math.round(p.getY()),
                1, false, MouseEvent.BUTTON1);
    }

    private void paint()
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }
}
