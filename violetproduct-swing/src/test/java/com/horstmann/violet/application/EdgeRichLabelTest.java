package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTabbedPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.framework.propertyeditor.CustomPropertyEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.DiagramTextPane;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateNode;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;

/**
 * The texts of the edges are rich texts (MultiLineString), like those of the nodes: saved with their style, read from the plain
 * Strings of older files, edited in the properties dialog one at a time in tabs.
 */
@ExtendWith(OnEventDispatchThread.class)
class EdgeRichLabelTest
{
    @Test
    void shouldSaveAndReloadTheStyleOfTheTexts() throws Exception
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connect(graph);
        edge.getMiddleLabel().setText("<html><b>bold</b> <font color=\"#0000ff\">blue</font></html>");
        edge.getEndLabel().setText("0..*");
        edge.getEndLabel().setSize(16);
        edge.getEndLabel().setJustification(MultiLineString.Justification.RIGHT);

        GenericEdge reloaded = (GenericEdge) reload(write(graph)).getAllEdges().iterator().next();

        assertEquals("bold blue", reloaded.getMiddleLabel().getPlainText());
        assertTrue(reloaded.getMiddleLabel().getText().contains("<b>"));
        assertTrue(reloaded.getMiddleLabel().getText().toLowerCase().contains("#0000ff"));
        assertEquals("0..*", reloaded.getEndLabel().getPlainText());
        assertEquals(16, reloaded.getEndLabel().getSize());
        assertEquals(MultiLineString.Justification.RIGHT, reloaded.getEndLabel().getJustification());
    }

    @Test
    void shouldReadThePlainTextsOfOlderFiles() throws Exception
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = connect(graph);
        edge.getStartLabel().setText("1");
        edge.getMiddleLabel().setText("uses");
        String xml = write(graph);
        // As an older version wrote them: plain Strings, lines separated by a line break
        String olderXml = xml.replaceAll("(?s)<startLabel>.*?</startLabel>", "<startLabel>1</startLabel>")
                .replaceAll("(?s)<middleLabel>.*?</middleLabel>", "<middleLabel>List&lt;Order&gt;\nsometimes</middleLabel>")
                .replaceAll("(?s)<endLabel>.*?</endLabel>", "<endLabel></endLabel>");
        assertTrue(olderXml.contains("<middleLabel>List&lt;Order&gt;"));

        GenericEdge reloaded = (GenericEdge) reload(olderXml).getAllEdges().iterator().next();

        assertEquals("1", reloaded.getStartLabel().getPlainText());
        assertEquals("List<Order>\nsometimes", reloaded.getMiddleLabel().getPlainText());
        assertTrue(reloaded.getEndLabel().isBlank());
        assertNull(reloaded.getEndLabelBounds());
    }

    @Test
    void shouldReadTheLabelsOfTheStateTransitionsOfOlderFiles() throws Exception
    {
        IGraph graph = new StateDiagramGraph();
        StateNode start = new StateNode();
        StateNode end = new StateNode();
        graph.addNode(start, new Point2D.Double(20, 100));
        graph.addNode(end, new Point2D.Double(380, 100));
        StateTransitionEdge edge = new StateTransitionEdge();
        edge.getLabel().setText("new");
        assertTrue(graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
        String xml = write(graph);
        assertTrue(xml.contains("<label>"), xml);
        String olderXml = xml.replaceAll("(?s)<label>.*?</label>", "<labelText>&lt;b&gt;fire&lt;/b&gt; [x &amp;lt; 2]</labelText>");

        StateTransitionEdge reloaded = (StateTransitionEdge) reload(olderXml).getAllEdges().iterator().next();

        assertEquals("fire [x < 2]", reloaded.getLabel().getPlainText());
        assertTrue(reloaded.getLabel().getText().contains("<b>"));
        // Saved again as a rich text only, once read (the diagram is drawn before it can be changed and saved)
        IGraph shown = reload(olderXml);
        ((StateTransitionEdge) shown.getAllEdges().iterator().next()).getLabel();
        String savedAgain = write(shown);
        assertTrue(savedAgain.contains("<label>"));
        assertTrue(!savedAgain.contains("<labelText>"), savedAgain);
    }

    @Test
    void shouldGiveEachCopyItsOwnTexts()
    {
        GenericEdge edge = new GenericEdge();
        edge.getMiddleLabel().setText("original");

        GenericEdge copy = (GenericEdge) edge.clone();
        copy.getMiddleLabel().setText("changed");

        assertNotSame(edge.getMiddleLabel(), copy.getMiddleLabel());
        assertEquals("original", edge.getMiddleLabel().getPlainText());
        StateTransitionEdge transition = new StateTransitionEdge();
        assertNotSame(transition.getLabel(), ((StateTransitionEdge) transition.clone()).getLabel());
    }

    @Test
    void shouldShowTheThreeTextsOfAnEdgeInTabsOfTheDialog()
    {
        GenericEdge edge = new GenericEdge();
        edge.getStartLabel().setText("1");
        edge.setBorderColor(Color.BLUE);

        JTabbedPane tabs = findTabs(new CustomPropertyEditor(edge).getAWTComponent());

        assertNotNull(tabs);
        assertEquals(3, tabs.getTabCount());
        // The middle one is empty, the start one isn't: it is shown first
        assertEquals(0, tabs.getSelectedIndex());
        assertNotNull(tabs.getIconAt(0));
        assertNull(tabs.getIconAt(1));
        assertNull(tabs.getIconAt(2));
        // Typed in the middle tab: written into the edge, its tab marked as filled
        DiagramTextPane middle = findTextPanes(tabs.getComponentAt(1)).get(0);
        middle.setText("calls");
        assertEquals("calls", edge.getMiddleLabel().getPlainText());
        assertNotNull(tabs.getIconAt(1));
        // Shown in the color the diagram gives it
        assertEquals(Color.BLUE, edge.getTextColor());
    }

    @Test
    void shouldShowASingleTextWithoutTabs()
    {
        CallEdge edge = new CallEdge();

        Component sheet = new CustomPropertyEditor(edge).getAWTComponent();

        assertNull(findTabs(sheet));
        assertEquals(1, findTextPanes(sheet).size());
    }

    private GenericEdge connect(IGraph graph)
    {
        ClassNode start = new ClassNode();
        ClassNode end = new ClassNode();
        graph.addNode(start, new Point2D.Double(20, 100));
        graph.addNode(end, new Point2D.Double(400, 100));
        GenericEdge edge = new GenericEdge();
        assertTrue(graph.connect(edge, start, new Point2D.Double(), end, new Point2D.Double()));
        return edge;
    }

    private String write(IGraph graph)
    {
        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private IGraph reload(String xml) throws Exception
    {
        return new XMLPersistenceService().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private JTabbedPane findTabs(Component component)
    {
        if (component instanceof JTabbedPane)
        {
            return (JTabbedPane) component;
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                JTabbedPane found = findTabs(child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private List<DiagramTextPane> findTextPanes(Component component)
    {
        List<DiagramTextPane> found = new ArrayList<DiagramTextPane>();
        if (component instanceof DiagramTextPane)
        {
            found.add((DiagramTextPane) component);
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                found.addAll(findTextPanes(child));
            }
        }
        return found;
    }
}
