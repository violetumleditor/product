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

import javax.swing.JTextArea;
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
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.AddNodeByTapBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.EditTextInPlaceBehavior;
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
        assertTrue(text instanceof JTextArea);
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

    private JTextComponent getTextComponent()
    {
        assertEquals(1, this.editorPart.getComponentCount());
        JTextComponent text = findTextComponent(this.editorPart.getComponent(0));
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
