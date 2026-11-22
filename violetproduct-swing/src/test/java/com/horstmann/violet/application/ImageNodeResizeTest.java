package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.CropInsets;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.ResizeNodeBehavior;

/**
 * Dragging a corner of an image resizes it from the opposite corner, which doesn't move at all, and the image keeps its ratio
 */
class ImageNodeResizeTest
{

    private static final double GRID = 10;

    private EditorPart editorPart;

    private ResizeNodeBehavior behavior;

    private ImageNode image;

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
        this.image = new ImageNode(new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB));
        assertTrue(graph.addNode(this.image, new Point2D.Double(200, 200)));
        this.image.setCropInsets(new CropInsets(6, 12, 4, 8));
        this.editorPart = new EditorPart(graph);
        this.behavior = new ResizeNodeBehavior(this.editorPart, null);
        this.editorPart.getSelectionHandler().addSelectedElement(this.image);
    }

    @ParameterizedTest
    @ValueSource(strings = { "NW", "NE", "SW", "SE" })
    void oppositeCornerStaysWhereItIs(String corner)
    {
        Rectangle2D before = this.image.getBounds();
        double ratio = before.getHeight() / before.getWidth();
        boolean west = corner.endsWith("W");
        boolean north = corner.startsWith("N");
        // One pixel inside the corner, so the press is on the node
        double startX = west ? before.getMinX() + 1 : before.getMaxX() - 1;
        double startY = north ? before.getMinY() + 1 : before.getMaxY() - 1;

        for (double delta : new double[] { 36, 81, -24 })
        {
            double outward = delta;
            double endX = startX + (west ? -outward : outward);
            double endY = startY + (north ? -outward / 3 : outward / 3);
            double fixedXBefore = west ? before.getMaxX() : before.getMinX();
            double fixedYBefore = north ? before.getMaxY() : before.getMinY();
            this.behavior.onMousePressed(mouseEvent(MouseEvent.MOUSE_PRESSED, startX, startY));
            assertEquals(before, this.image.getBounds(), corner + " " + delta + ": the press doesn't move the image");
            // Every step of the drag keeps the opposite corner exactly where it is
            for (double step = 0.25; step <= 1; step += 0.25)
            {
                this.behavior.onMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, startX + (endX - startX) * step,
                        startY + (endY - startY) * step));
                Rectangle2D during = this.image.getBounds();
                assertEquals(fixedXBefore, west ? during.getMaxX() : during.getMinX(), 0.001, corner + " " + delta + ": opposite corner, x");
                assertEquals(fixedYBefore, north ? during.getMaxY() : during.getMinY(), 0.001, corner + " " + delta + ": opposite corner, y");
            }
            Rectangle2D during = this.image.getBounds();
            this.behavior.onMouseReleased(mouseEvent(MouseEvent.MOUSE_RELEASED, endX, endY));

            Rectangle2D after = this.image.getBounds();
            assertEquals(during, after, corner + " " + delta + ": the release doesn't move the image");
            // One scale for both axes: the one that reaches the farthest mouse coordinate
            double mouseWidth = Math.abs(onGrid(Math.round(endX)) - fixedXBefore);
            double mouseHeight = Math.abs(onGrid(Math.round(endY)) - fixedYBefore);
            double expectedWidth = Math.max(mouseWidth, mouseHeight / ratio);
            assertEquals(expectedWidth, after.getWidth(), 1.5, corner + " " + delta + ": follows the mouse");
            assertEquals(ratio, after.getHeight() / after.getWidth(), 0.02, corner + " " + delta + ": ratio kept");
            before = after;
            startX = west ? after.getMinX() + 1 : after.getMaxX() - 1;
            startY = north ? after.getMinY() + 1 : after.getMaxY() - 1;
        }
    }

    /** The image is resized grid step by grid step: a move that stays near the same grid point changes nothing */
    @Test
    void resizesOnlyFromOneGridPointToAnother()
    {
        Rectangle2D before = this.image.getBounds();
        double startX = before.getMaxX() - 1;
        double startY = before.getMaxY() - 1;
        double gridX = onGrid(startX) + 3 * GRID;
        double gridY = onGrid(startY) + GRID;
        this.behavior.onMousePressed(mouseEvent(MouseEvent.MOUSE_PRESSED, startX, startY));
        this.behavior.onMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, gridX, gridY));
        Rectangle2D onGridPoint = this.image.getBounds();
        assertNotEquals(before, onGridPoint, "resized at a new grid point");

        this.behavior.onMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, gridX + 3, gridY - 2));
        assertEquals(onGridPoint, this.image.getBounds(), "nothing changes until the next grid point");
        this.behavior.onMouseDragged(mouseEvent(MouseEvent.MOUSE_DRAGGED, gridX + GRID, gridY));
        assertNotEquals(onGridPoint, this.image.getBounds(), "resized again at the next grid point");
        this.behavior.onMouseReleased(mouseEvent(MouseEvent.MOUSE_RELEASED, gridX + GRID, gridY));
    }

    private double onGrid(double coordinate)
    {
        return Math.round(coordinate / GRID) * GRID;
    }

    private MouseEvent mouseEvent(int id, double x, double y)
    {
        return new MouseEvent(this.editorPart.getSwingComponent(), id, 0, InputEvent.BUTTON1_DOWN_MASK, (int) Math.round(x),
                (int) Math.round(y), 1, false, MouseEvent.BUTTON1);
    }
}
