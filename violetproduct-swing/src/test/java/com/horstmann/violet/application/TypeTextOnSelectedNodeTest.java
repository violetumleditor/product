package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.object.ObjectReferenceEdge;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.object.ObjectNode;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.TypeTextOnSelectedNodeBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;

class TypeTextOnSelectedNodeTest
{

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

    static Stream<IGraph> graphs()
    {
        return Stream.of(new ClassDiagramGraph(), new SequenceDiagramGraph(), new ActivityDiagramGraph(), new StateDiagramGraph(),
                new ObjectDiagramGraph(), new UseCaseDiagramGraph());
    }

    /** The text typed on each node of the side bar, by node class: the first MultiLineString field */
    private static final Map<String, String> TYPED_PROPERTIES = Map.ofEntries(Map.entry("NoteNode", "text"),
            Map.entry("FreeTextNode", "text"), Map.entry("ImageNode", "text"), Map.entry("ActivityNode", "name"),
            Map.entry("DecisionNode", "condition"), Map.entry("SignalReceiptNode", "signal"),
            Map.entry("SignalSendingNode", "signal"), Map.entry("ClassNode", "name"), Map.entry("InterfaceNode", "name"),
            Map.entry("PackageNode", "name"), Map.entry("ObjectNode", "name"), Map.entry("FieldNode", "name"),
            Map.entry("LifelineNode", "name"), Map.entry("StateNode", "name"), Map.entry("ActorNode", "name"),
            Map.entry("UseCaseNode", "name"));

    /** Every node offered in the side bar with a text property can be written into, in the right one */
    @ParameterizedTest
    @MethodSource("graphs")
    void shouldWriteIntoEveryNodeHavingAText(IGraph graph) throws Exception
    {
        for (INode prototype : graph.getNodePrototypes())
        {
            String nodeName = prototype.getClass().getSimpleName();
            String expectedProperty = TYPED_PROPERTIES.get(nodeName);
            INode node = prototype.clone();
            graph.addNode(node, new Point2D.Double(0, 0));
            TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(graph), null);
            assertEquals(expectedProperty != null, behavior.startTyping(node, "Abc"), nodeName);
            behavior.commitTyping();
            for (PropertyDescriptor property : Introspector.getBeanInfo(node.getClass()).getPropertyDescriptors())
            {
                if (MultiLineString.class.equals(property.getPropertyType()) && property.getReadMethod() != null)
                {
                    MultiLineString text = (MultiLineString) property.getReadMethod().invoke(node);
                    boolean isTyped = text != null && text.getText().contains(">Abc<");
                    assertEquals(property.getName().equals(expectedProperty), isTyped, nodeName + "." + property.getName());
                }
            }
        }
    }

    @Test
    void shouldGoToTheClassNameWithItsDefaultsAndNoCompartment()
    {
        IGraph graph = new ClassDiagramGraph();
        ClassNode node = new ClassNode();
        graph.addNode(node, new Point2D.Double(0, 0));
        double emptyHeight = node.getBounds().getHeight();
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(graph), null);

        behavior.startTyping(node, "Fo");
        behavior.typeText("ox");
        behavior.deleteLastCharacter();
        behavior.commitTyping();

        assertTrue(node.getName().getText().contains(">Foo<"));
        assertEquals(MultiLineString.FontSize.LARGE, node.getName().getSize());
        assertEquals("", node.getAttributes().getText());
        assertEquals("", node.getMethods().getText());
        assertEquals(emptyHeight, node.getBounds().getHeight());
    }

    @Test
    void shouldKeepDefaultsEvenWhenTheTextWasChanged()
    {
        IGraph graph = new SequenceDiagramGraph();
        LifelineNode node = new LifelineNode();
        node.getName().setUnderlined(false);
        node.getName().setJustification(MultiLineString.Justification.RIGHT);
        graph.addNode(node, new Point2D.Double(0, 0));
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(graph), null);

        behavior.startTyping(node, "a");
        behavior.typeText("\nb");
        behavior.commitTyping();

        assertTrue(node.getName().isUnderlined());
        assertEquals(MultiLineString.Justification.CENTER, node.getName().getJustification());
        assertTrue(node.getName().getText().contains(">a<br>b<"));
    }

    @Test
    void shouldRestoreTheFormerTextOnCancel()
    {
        IGraph graph = new ClassDiagramGraph();
        NoteNode node = new NoteNode();
        node.getText().setText("former");
        MultiLineString former = node.getText();
        graph.addNode(node, new Point2D.Double(0, 0));
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(graph), null);

        behavior.startTyping(node, "new");
        assertTrue(node.getText().getText().contains(">new<"));
        behavior.cancelTyping();

        assertFalse(behavior.isTyping());
        assertSame(former, node.getText());
        assertTrue(former.getText().contains(">former<"));
    }

    @Test
    void shouldUndoAndRedoTheTypingAsOneEdit()
    {
        IGraph graph = new ObjectDiagramGraph();
        ObjectNode node = new ObjectNode();
        graph.addNode(node, new Point2D.Double(0, 0));
        MultiLineString former = node.getName();
        EditorPart editorPart = new EditorPart(graph);
        UndoRedoCompoundBehavior undoRedo = new UndoRedoCompoundBehavior(editorPart);
        editorPart.getBehaviorManager().addBehavior(undoRedo);
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(editorPart, null);
        editorPart.getBehaviorManager().addBehavior(behavior);

        behavior.startTyping(node, "o");
        behavior.typeText("bj");
        behavior.commitTyping();
        MultiLineString typed = node.getName();
        assertTrue(typed.getText().contains(">obj<"));
        assertTrue(typed.isUnderlined());

        undoRedo.undo();
        assertSame(former, node.getName());
        undoRedo.redo();
        assertSame(typed, node.getName());
    }

    /** Every edge of the side bar showing a text at its middle can be written into: middle label, or label of a transition */
    @ParameterizedTest
    @MethodSource("graphs")
    void shouldWriteIntoTheMiddleOfEveryEdgeHavingOne(IGraph graph) throws Exception
    {
        for (IEdge prototype : graph.getEdgePrototypes())
        {
            String edgeName = prototype.getClass().getSimpleName();
            IEdge edge = (IEdge) prototype.clone();
            TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(graph), null);
            boolean isTyping = behavior.startTyping(edge, "Ab");
            behavior.typeText("c");
            behavior.commitTyping();
            if (edge instanceof StateTransitionEdge)
            {
                assertTrue(isTyping, edgeName);
                assertEquals("Abc", ((StateTransitionEdge) edge).getLabel());
            }
            else if (edge instanceof SegmentedLineEdge && !(edge instanceof ObjectReferenceEdge))
            {
                assertTrue(isTyping, edgeName);
                assertEquals("Abc", ((SegmentedLineEdge) edge).getMiddleLabel(), edgeName);
                assertFalse("Abc".equals(((SegmentedLineEdge) edge).getStartLabel()), edgeName);
                assertFalse("Abc".equals(((SegmentedLineEdge) edge).getEndLabel()), edgeName);
            }
        }
    }

    @Test
    void shouldReplaceTheMiddleLabelOfAnEdgeAndRestoreItOnCancel()
    {
        GenericEdge edge = new GenericEdge();
        edge.setMiddleLabel("former");
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(new EditorPart(new ClassDiagramGraph()), null);

        behavior.startTyping(edge, "n");
        behavior.typeText("ew!");
        behavior.deleteLastCharacter();
        assertEquals("new", edge.getMiddleLabel());
        behavior.cancelTyping();

        assertFalse(behavior.isTyping());
        assertEquals("former", edge.getMiddleLabel());
    }

    @Test
    void shouldUndoAndRedoTheTypingOnAnEdgeAsOneEdit()
    {
        IGraph graph = new ClassDiagramGraph();
        GenericEdge edge = new GenericEdge();
        edge.setMiddleLabel("former");
        EditorPart editorPart = new EditorPart(graph);
        UndoRedoCompoundBehavior undoRedo = new UndoRedoCompoundBehavior(editorPart);
        editorPart.getBehaviorManager().addBehavior(undoRedo);
        TypeTextOnSelectedNodeBehavior behavior = new TypeTextOnSelectedNodeBehavior(editorPart, null);
        editorPart.getBehaviorManager().addBehavior(behavior);

        behavior.startTyping(edge, "us");
        behavior.typeText("es");
        behavior.commitTyping();
        assertEquals("uses", edge.getMiddleLabel());

        undoRedo.undo();
        assertEquals("former", edge.getMiddleLabel());
        undoRedo.redo();
        assertEquals("uses", edge.getMiddleLabel());
    }
}
