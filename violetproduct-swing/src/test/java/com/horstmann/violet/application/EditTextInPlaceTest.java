package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.bpmn.ProcessEndEventNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessGatewayNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessPoolNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessStartEventNode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.AddNodeByTapBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.EditButtonBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.EditTextInPlaceBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.RemoveEmptyFreeTextBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.SelectByClickBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A double click on a text edits it in place: an edge label in a text area, the text of a node (the one under the mouse) in the
 * rich text box; the diagram follows the typing, validating is one undoable edit, Escape restores the former text
 */
class EditTextInPlaceTest
{

    private IGraph graph;

    private EditorPart editorPart;

    private UndoRedoCompoundBehavior undoRedo;

    private EditTextInPlaceBehavior behavior;

    private ClassNode start;

    private GenericEdge edge;

    @BeforeAll
    static void registerBeans()
    {
        // The editor paints with the colors of the theme
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
        this.graph = new GraphFile(ClassDiagramGraph.class).getGraph();
        this.start = new ClassNode();
        assertTrue(this.graph.addNode(this.start, new Point2D.Double(100, 100)));
        ClassNode end = new ClassNode();
        assertTrue(this.graph.addNode(end, new Point2D.Double(500, 100)));
        this.edge = new GenericEdge();
        this.edge.setBentStyle(BentStyle.STRAIGHT);
        assertTrue(this.graph.connect(this.edge, this.start, new Point2D.Double(), end, new Point2D.Double()));
        this.edge.setMiddleLabel("calls");
        this.edge.setStartLabel("1");
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        this.behavior = new EditTextInPlaceBehavior(this.editorPart, toolsBar);
        this.editorPart.getBehaviorManager().addBehavior(this.behavior);
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
    }

    @Test
    void shouldEditTheMiddleTextOfAnEdgeAndUndoItInOneStep() throws Exception
    {
        Rectangle2D label = this.edge.getMiddleLabelBounds();

        assertTrue(this.behavior.startEditing(center(label)));
        JTextComponent text = getTextComponent();
        assertEquals("calls", text.getText());
        type(text, "\nasync");
        assertEquals("calls\nasync", this.edge.getMiddleLabel()); // shown as typed
        this.behavior.commit();

        assertFalse(this.behavior.isEditing());
        assertEquals(0, this.editorPart.getComponentCount());
        assertEquals("calls\nasync", this.edge.getMiddleLabel());
        this.undoRedo.undo();
        assertEquals("calls", this.edge.getMiddleLabel());
    }

    @Test
    void shouldEditTheTextUnderTheMouseAtTheStartOfAnEdge() throws Exception
    {
        assertTrue(this.behavior.startEditing(center(this.edge.getStartLabelBounds())));

        assertEquals("1", getTextComponent().getText());
    }

    @Test
    void shouldRestoreTheFormerTextOnEscape() throws Exception
    {
        assertTrue(this.behavior.startEditing(center(this.edge.getMiddleLabelBounds())));
        type(getTextComponent(), " more");

        this.behavior.cancel();

        assertEquals("calls", this.edge.getMiddleLabel());
        assertEquals(0, this.editorPart.getComponentCount());
    }

    @Test
    void shouldEditTheCompartmentOfAClassUnderTheMouse() throws Exception
    {
        this.start.getName().setText("Order");
        this.start.getAttributes().setText("id");
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
        Rectangle2D attributesArea = this.start.getAttributes().getDrawnArea();
        assertNotNull(attributesArea);
        String formerName = this.start.getName().getText();

        assertTrue(this.behavior.startEditing(center(attributesArea)));
        JTextComponent text = getTextComponent();
        type(text, "total");
        this.behavior.commit();

        assertTrue(plainText(this.start.getAttributes()).contains("idtotal") || plainText(this.start.getAttributes()).contains("totalid"),
                this.start.getAttributes().getText());
        assertEquals(formerName, this.start.getName().getText());
        this.undoRedo.undo();
        assertEquals("id", plainText(this.start.getAttributes()));
    }

    @Test
    void shouldHideTheEdgeTextWhileItIsEdited()
    {
        Rectangle2D label = this.edge.getMiddleLabelBounds();
        assertTrue(countInkPixels(label) > 0);

        assertTrue(this.behavior.startEditing(center(label)));
        assertEquals(0, countInkPixels(label));
        this.behavior.cancel();

        assertTrue(countInkPixels(label) > 0);
    }

    @Test
    void shouldHideTheNodeTextWhileItIsEdited()
    {
        this.start.getAttributes().setText("identifier");
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        this.graph.draw(image.createGraphics());
        Rectangle2D area = this.start.getAttributes().getDrawnArea();
        // Inside the compartment, off its border
        Rectangle2D inside = new Rectangle2D.Double(area.getX() + 3, area.getY() + 3, area.getWidth() - 6, area.getHeight() - 6);
        assertTrue(countInkPixels(inside) > 0);

        assertTrue(this.behavior.startEditing(center(area)));
        assertEquals(0, countInkPixels(inside));
        this.behavior.commit();

        assertTrue(countInkPixels(inside) > 0);
    }

    /**
     * @return the number of dark pixels the graph paints in the given area
     */
    private int countInkPixels(Rectangle2D area)
    {
        BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setColor(java.awt.Color.BLACK);
        this.graph.draw(g2);
        g2.dispose();
        int count = 0;
        for (int x = (int) Math.ceil(area.getMinX()); x < (int) area.getMaxX(); x++)
        {
            for (int y = (int) Math.ceil(area.getMinY()); y < (int) area.getMaxY(); y++)
            {
                // Dark pixels: the text, not the background of a node
                int rgb = image.getRGB(x, y);
                int brightness = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
                if ((rgb >>> 24) > 128 && brightness < 3 * 128)
                {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void shouldFindTheEdgeOnItsText()
    {
        assertSame(this.edge, this.graph.findEdge(center(this.edge.getMiddleLabelBounds())));
        assertSame(this.edge, this.graph.findEdge(center(this.edge.getStartLabelBounds())));
    }

    /**
     * Through the behaviors placed before it in the workspace: the first click of the double click must not be taken for a tap on
     * an empty spot, whose "+" would catch the second one
     */
    @Test
    void shouldEditTheTextOfAnEdgeOnARealDoubleClick()
    {
        EditorPart part = new EditorPart(this.graph);
        part.setSize(900, 600);
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        part.getBehaviorManager().addBehavior(new AddNodeByTapBehavior(part, toolsBar));
        part.getBehaviorManager().addBehavior(new SelectByClickBehavior(part, toolsBar));
        EditTextInPlaceBehavior inPlace = new EditTextInPlaceBehavior(part, toolsBar);
        part.getBehaviorManager().addBehavior(inPlace);
        Point2D point = center(this.edge.getMiddleLabelBounds());

        for (int clickCount = 1; clickCount <= 2; clickCount++)
        {
            part.getBehaviorManager().fireOnMousePressed(mouse(part, MouseEvent.MOUSE_PRESSED, point, clickCount));
            part.getBehaviorManager().fireOnMouseReleased(mouse(part, MouseEvent.MOUSE_RELEASED, point, clickCount));
            part.getBehaviorManager().fireOnMouseClicked(mouse(part, MouseEvent.MOUSE_CLICKED, point, clickCount));
        }

        assertTrue(part.getSelectionHandler().isElementAlreadySelected(this.edge));
        assertTrue(inPlace.isEditing());
    }

    private MouseEvent mouse(Component source, int id, Point2D point, int clickCount)
    {
        return new MouseEvent(source, id, 0, 0, (int) point.getX(), (int) point.getY(), clickCount, false, MouseEvent.BUTTON1);
    }

    @Test
    void shouldNotEditWhereThereIsNoText()
    {
        assertFalse(this.behavior.startEditing(new Point2D.Double(800, 500)));
        assertFalse(this.behavior.isEditing());
    }

    /**
     * The edited text must be painted where the diagram draws it, whatever the zoom
     */
    @Test
    void shouldShowTheEditedTextWhereTheDiagramDrawsItAtAnyZoom() throws Exception
    {
        this.start.getName().setText("Customer order");
        this.start.getAttributes().setText("identifier\ntotal");
        this.edge.setMiddleLabel("calls\nsometimes");
        for (int zoomSteps : new int[] { -3, 0, 1, 5 })
        {
            SwingUtilities.invokeAndWait(() -> {
                this.editorPart.changeZoom(zoomSteps);
                for (Point2D point : new Point2D[] { center(this.edge.getMiddleLabelBounds()), center(this.edge.getStartLabelBounds()),
                        center(drawnArea(this.start.getName())), center(drawnArea(this.start.getAttributes())) })
                {
                    BufferedImage expected = paint();
                    assertTrue(this.behavior.startEditing(point));
                    JTextComponent text = getTextComponent();
                    text.getCaret().setVisible(false);
                    BufferedImage edited = paint();
                    Rectangle2D box = text.getParent().getBounds();
                    int[] expectedInk = inkBounds(expected, box);
                    int[] editedInk = inkBounds(edited, box);
                    for (int i = 0; i < 4; i++)
                    {
                        assertEquals(expectedInk[i], editedInk[i], 2, "zoom " + zoomSteps + ", " + text.getText());
                    }
                    this.behavior.cancel();
                }
                this.editorPart.changeZoom(-zoomSteps);
            });
        }
    }

    /**
     * The name of a participant is drawn bottom to top
     */
    @Test
    void shouldEditAVerticalTextVertically() throws Exception
    {
        this.graph = new GraphFile(ProcessDiagramGraph.class).getGraph();
        ProcessPoolNode pool = new ProcessPoolNode();
        assertTrue(this.graph.addNode(pool, new Point2D.Double(100, 100)));
        pool.getName().setText("Customer");
        this.editorPart = new EditorPart(this.graph);
        this.behavior = new EditTextInPlaceBehavior(this.editorPart, null);
        SwingUtilities.invokeAndWait(() -> {
            for (int zoomSteps : new int[] { -2, 0, 3 })
            {
                this.editorPart.changeZoom(zoomSteps);
                Rectangle2D drawn = drawnArea(pool.getName());
                BufferedImage expected = paint();
                assertTrue(this.behavior.startEditing(center(drawn)));
                JTextComponent text = getTextComponent();
                text.getCaret().setVisible(false);
                BufferedImage edited = paint();
                Rectangle2D box = text.getParent().getBounds();
                assertTrue(box.getHeight() > box.getWidth());
                // Around the text, off the border of the pool
                double zoom = this.editorPart.getZoomFactor();
                Rectangle2D inside = new Rectangle2D.Double((drawn.getX() + 5) * zoom, (drawn.getY() + 8) * zoom,
                        (drawn.getWidth() - 10) * zoom, (drawn.getHeight() - 16) * zoom);
                int[] expectedInk = inkBounds(expected, inside);
                int[] editedInk = inkBounds(edited, inside);
                assertTrue(expectedInk[2] > 0);
                for (int i = 0; i < 4; i++)
                {
                    assertEquals(expectedInk[i], editedInk[i], 2, "zoom " + zoomSteps);
                }
                // A press at the bottom of the box is at the start of the text, at the top at its end
                int x = (int) box.getWidth() / 2;
                pressBox(text, x, (int) box.getHeight() - 2);
                int start = text.getCaretPosition();
                pressBox(text, x, 2);
                assertEquals("Customer".length(), text.getCaretPosition() - start);
                this.behavior.cancel();
                this.editorPart.changeZoom(-zoomSteps);
            }
        });
    }

    /**
     * The name band of a participant, then the participant itself, grow with its name
     */
    @Test
    void shouldGrowAParticipantWithItsName()
    {
        ProcessPoolNode pool = new ProcessPoolNode();
        assertTrue(new GraphFile(ProcessDiagramGraph.class).getGraph().addNode(pool, new Point2D.Double(100, 100)));
        pool.getLanes().setText("A\nB");
        double formerLaneX = pool.getSeparators().get(0).getX1();
        pool.getName().setText("one");
        assertEquals(formerLaneX, pool.getSeparators().get(0).getX1());

        pool.getName().setText("one\ntwo\nthree");
        assertEquals(formerLaneX + 2 * 16, pool.getSeparators().get(0).getX1());

        pool.getName().setText("a very long name ".repeat(10));
        assertTrue(pool.getBounds().getHeight() > pool.getName().getBounds().getWidth());
    }

    /**
     * Events and gateways draw their name outside their bounds, on a pool or not: it belongs to them
     */
    @Test
    void shouldEditTheNameDrawnOutsideANodeOnARealDoubleClick()
    {
        for (boolean isInPool : new boolean[] { false, true })
        {
            ProcessStartEventNode startEvent = new ProcessStartEventNode();
            ProcessEndEventNode endEvent = new ProcessEndEventNode();
            ProcessGatewayNode gateway = new ProcessGatewayNode();
            startEvent.getName().setText("Order received");
            endEvent.getName().setText("Order delivered");
            gateway.getName().setText("In stock?");
            for (INode node : new INode[] { startEvent, endEvent, gateway })
            {
                this.graph = new GraphFile(ProcessDiagramGraph.class).getGraph();
                if (isInPool)
                {
                    assertTrue(this.graph.addNode(new ProcessPoolNode(), new Point2D.Double(100, 100)));
                }
                assertTrue(this.graph.addNode(node, new Point2D.Double(300, 200)));
                EditorPart part = new EditorPart(this.graph);
                part.setSize(900, 600);
                IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                        new Class<?>[] { IGraphToolsBar.class },
                        (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL
                                : "getNodeTools".equals(method.getName()) ? java.util.List.of() : null);
                part.getBehaviorManager().addBehavior(new AddNodeByTapBehavior(part, toolsBar));
                part.getBehaviorManager().addBehavior(new SelectByClickBehavior(part, toolsBar));
                EditTextInPlaceBehavior inPlace = new EditTextInPlaceBehavior(part, toolsBar);
                part.getBehaviorManager().addBehavior(inPlace);
                Rectangle2D label = node.getOutsideLabelBounds();
                // At the end of the text, out of the shape
                Point2D point = new Point2D.Double(label.getMaxX() - 3, label.getCenterY());

                for (int clickCount = 1; clickCount <= 2; clickCount++)
                {
                    part.getBehaviorManager().fireOnMousePressed(mouse(part, MouseEvent.MOUSE_PRESSED, point, clickCount));
                    part.getBehaviorManager().fireOnMouseReleased(mouse(part, MouseEvent.MOUSE_RELEASED, point, clickCount));
                    part.getBehaviorManager().fireOnMouseClicked(mouse(part, MouseEvent.MOUSE_CLICKED, point, clickCount));
                }

                assertTrue(part.getSelectionHandler().isElementAlreadySelected(node));
                assertTrue(inPlace.isEditing(), node.getClass().getName());
                inPlace.cancel();
            }
        }
    }

    /**
     * The pencil, and the buttons placed below it, could lie on the edited text
     */
    @Test
    void shouldHideTheButtonsWhileEditing() throws Exception
    {
        IGraphToolsBar toolsBar = (IGraphToolsBar) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { IGraphToolsBar.class },
                (proxy, method, args) -> "getSelectedTool".equals(method.getName()) ? GraphTool.SELECTION_TOOL : null);
        EditButtonBehavior editButton = new EditButtonBehavior(this.editorPart, toolsBar);
        java.lang.reflect.Method getButtonCenter = EditButtonBehavior.class.getDeclaredMethod("getButtonCenter");
        getButtonCenter.setAccessible(true);
        this.editorPart.getSelectionHandler().setSelectedElement(this.edge);
        assertNotNull(getButtonCenter.invoke(editButton));

        assertTrue(this.behavior.startEditing(center(this.edge.getMiddleLabelBounds())));
        assertEquals(null, getButtonCenter.invoke(editButton));

        this.behavior.commit();
        assertNotNull(getButtonCenter.invoke(editButton));
    }

    /**
     * Without any text, a free text couldn't be seen any more: it is removed, with its links, in the same undoable step
     */
    @Test
    void shouldRemoveAFreeTextLeftEmpty() throws Exception
    {
        this.editorPart = new EditorPart(this.graph);
        this.editorPart.setSize(900, 600);
        this.behavior = new EditTextInPlaceBehavior(this.editorPart, null);
        this.editorPart.getBehaviorManager().addBehavior(this.behavior);
        this.editorPart.getBehaviorManager().addBehavior(new RemoveEmptyFreeTextBehavior(this.editorPart));
        this.undoRedo = new UndoRedoCompoundBehavior(this.editorPart);
        this.editorPart.getBehaviorManager().addBehavior(this.undoRedo);
        FreeTextNode freeText = new FreeTextNode();
        assertTrue(this.graph.addNode(freeText, new Point2D.Double(300, 400)));
        GenericEdge link = new GenericEdge();
        assertTrue(this.graph.connect(link, freeText, new Point2D.Double(), this.start, new Point2D.Double()));
        String formerText = plainText(freeText.getText());
        SwingUtilities.invokeAndWait(() -> {
            this.editorPart.getSelectionHandler().setSelectedElement(freeText);
            assertTrue(this.behavior.startEditing(center(drawnArea(freeText.getText()))));
            JTextComponent text = getTextComponent();
            text.selectAll();
            text.replaceSelection("  ");
            assertTrue(this.graph.getAllNodes().contains(freeText)); // still being typed
            this.behavior.commit();

            assertFalse(this.graph.getAllNodes().contains(freeText));
            assertFalse(this.graph.getAllEdges().contains(link));
            assertTrue(this.editorPart.getSelectionHandler().getSelectedElements().isEmpty());

            this.undoRedo.undo();
            assertTrue(this.graph.getAllNodes().contains(freeText));
            assertTrue(this.graph.getAllEdges().contains(link));
            assertEquals(formerText, plainText(freeText.getText()));
            this.undoRedo.redo();
            assertFalse(this.graph.getAllNodes().contains(freeText));
        });
    }

    private void pressBox(JTextComponent text, int x, int y)
    {
        Component box = text.getParent();
        box.dispatchEvent(new MouseEvent(box, MouseEvent.MOUSE_PRESSED, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false,
                MouseEvent.BUTTON1));
    }

    @Test
    void shouldFollowTheZoomWhileEditing() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Rectangle2D label = this.edge.getMiddleLabelBounds();
            assertTrue(this.behavior.startEditing(center(label)));
            this.editorPart.changeZoom(4);

            assertTrue(this.behavior.isEditing());
            Rectangle2D box = getTextComponent().getParent().getBounds();
            assertEquals(label.getCenterX() * 2, box.getCenterX(), 3);
            assertTrue(box.contains(label.getX() * 2, label.getY() * 2, label.getWidth() * 2, label.getHeight() * 2));
        });
    }

    /**
     * A blink of the caret repaints the box alone: the diagram under this transparent box must be painted too
     */
    @Test
    void shouldPaintTheDiagramAgainUnderTheBox() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(this.behavior.startEditing(center(this.edge.getMiddleLabelBounds())));
            JTextComponent text = getTextComponent();
            text.getCaret().setVisible(false);
            this.editorPart.addNotify();
            this.editorPart.validate();
            assertTrue(this.editorPart.isValid());
            BufferedImage image = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
            this.editorPart.paint(image.createGraphics());

            assertTrue((image.getRGB(890, 590) & 0xFFFFFF) != 0);
        });
    }

    private Rectangle2D drawnArea(MultiLineString text)
    {
        this.graph.draw(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());
        return text.getDrawnArea();
    }

    private BufferedImage paint()
    {
        BufferedImage image = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
        this.editorPart.setSize(1600, 900);
        this.editorPart.invalidate();
        this.editorPart.paint(image.createGraphics());
        return image;
    }

    /**
     * @return left, top, right, bottom of the dark pixels in the area, off its sides
     */
    private int[] inkBounds(BufferedImage image, Rectangle2D area)
    {
        int[] result = { Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1 };
        for (int x = (int) area.getMinX() + 2; x < (int) area.getMaxX() - 2; x++)
        {
            for (int y = (int) area.getMinY() + 2; y < (int) area.getMaxY() - 2; y++)
            {
                int rgb = image.getRGB(x, y);
                if (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF) < 560)
                {
                    result[0] = Math.min(result[0], x);
                    result[1] = Math.min(result[1], y);
                    result[2] = Math.max(result[2], x);
                    result[3] = Math.max(result[3], y);
                }
            }
        }
        return result;
    }

    private JTextComponent getTextComponent()
    {
        JTextComponent text = findTextComponent(this.editorPart);
        assertNotNull(text);
        return text;
    }

    private JTextComponent findTextComponent(Component component)
    {
        if (component instanceof JTextComponent)
        {
            return (JTextComponent) component;
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                JTextComponent found = findTextComponent(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private void type(JTextComponent text, String typed) throws BadLocationException
    {
        text.getDocument().insertString(text.getCaretPosition(), typed, null);
    }

    private String plainText(MultiLineString text)
    {
        return text.getText().replaceAll("<[^>]*>", "").replaceAll("\\s+", "");
    }

    private Point2D center(Rectangle2D bounds)
    {
        assertNotNull(bounds);
        return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
    }
}
