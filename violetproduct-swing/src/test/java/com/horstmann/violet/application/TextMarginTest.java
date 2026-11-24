package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
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
import com.horstmann.violet.product.diagram.abstracts.node.AbstractNode;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.object.FieldNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.object.ObjectNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;

/**
 * The general rule of the texts surrounded by the border of their element: off its left and right sides, centered vertically
 */
class TextMarginTest
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

    @Test
    void textsAreOffTheBordersAndCenteredInEveryDiagram() throws Exception
    {
        List<String> checked = new ArrayList<String>();
        for (Class<? extends IGraph> graphClass : List.of(ClassDiagramGraph.class, SequenceDiagramGraph.class,
                ActivityDiagramGraph.class, StateDiagramGraph.class, ObjectDiagramGraph.class, UseCaseDiagramGraph.class,
                ProcessDiagramGraph.class))
        {
            for (INode prototype : new GraphFile(graphClass).getGraph().getNodePrototypes())
            {
                String name = prototype.getClass().getSimpleName();
                if (WITHOUT_BORDER_AROUND_TEXT.contains(name) || getTexts(prototype).isEmpty())
                {
                    continue;
                }
                for (int length = 1; length <= 60; length++)
                {
                    for (int lineCount = 1; lineCount <= 5; lineCount += 2)
                    {
                        INode node = prototype.clone();
                        for (MultiLineString text : getTexts(node))
                        {
                            text.setText("m".repeat(length) + "\nm".repeat(lineCount - 1));
                        }
                        assertTrue(new GraphFile(graphClass).getGraph().addNode(node, new Point2D.Double(100, 100)), name);
                        check(node, name + " " + length + " " + lineCount);
                    }
                }
                checked.add(name);
            }
        }
        assertTrue(checked.containsAll(List.of("ClassNode", "InterfaceNode", "PackageNode", "LifelineNode", "ActivityNode",
                "DecisionNode", "SignalSendingNode", "SignalReceiptNode", "StateNode", "ObjectNode", "UseCaseNode", "NoteNode",
                "ProcessTaskNode")), checked.toString());
    }

    @Test
    void fieldsAreOffTheSidesOfTheirObject()
    {
        for (int length = 1; length <= 60; length++)
        {
            IGraph graph = new GraphFile(ObjectDiagramGraph.class).getGraph();
            ObjectNode object = new ObjectNode();
            assertTrue(graph.addNode(object, new Point2D.Double(100, 100)));
            FieldNode field = new FieldNode();
            assertTrue(object.addChild(field, 0));
            field.getName().setText("m".repeat(length));
            field.getValue().setText("m".repeat(length));
            draw(object);
            Rectangle2D name = field.getName().getDrawnTextArea();
            Rectangle2D nameArea = field.getName().getDrawnArea();
            assertEquals(MARGIN, nameArea.getMaxX() - toGraph(field.getName(), name).getMaxX(), 0.01);
            assertTrue(toGraph(field.getName(), name).getX() >= object.getBounds().getX() + MARGIN);
            assertTrue(field.getName().getBounds().getWidth() <= name.getWidth());
            assertTrue(toGraph(field.getValue(), field.getValue().getDrawnTextArea()).getMaxX() <= object.getBounds().getMaxX()
                    - MARGIN);
        }
    }

    private void check(INode node, String message) throws Exception
    {
        draw(node);
        Shape shape = ((AbstractNode) node).getShape();
        for (MultiLineString text : getTexts(node))
        {
            Rectangle2D lines = toGraph(text, text.getDrawnTextArea());
            double width = text.getBounds().getWidth();
            assertTrue(width <= lines.getWidth() + 0.01, message);
            if (!CURVED.contains(node.getClass().getSimpleName()))
            {
                // Whatever its alignment
                assertTrue(lines.getX() >= node.getBounds().getX() + MARGIN, message);
                assertTrue(lines.getMaxX() <= node.getBounds().getMaxX() - MARGIN, message);
            }
            // Within the shape, whatever this shape: at the middle of the height, as a round corner takes a part of the others
            assertTrue(shape.contains(lines.getCenterX() - width / 2 - MARGIN + 1, lines.getCenterY()), message);
            assertTrue(shape.contains(lines.getCenterX() + width / 2 + MARGIN - 1, lines.getCenterY()), message);
        }
        List<MultiLineString> texts = getTexts(node);
        if (texts.size() == 1 && !"ObjectNode".equals(node.getClass().getSimpleName()))
        {
            Rectangle2D lines = toGraph(texts.get(0), texts.get(0).getDrawnTextArea());
            assertEquals(shape.getBounds2D().getCenterY(), lines.getCenterY(), 1, message);
        }
    }

    private Rectangle2D toGraph(MultiLineString text, Rectangle2D area)
    {
        return text.getDrawnTransform().createTransformedShape(area).getBounds2D();
    }

    private void draw(INode node)
    {
        Graphics2D g2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        node.draw(g2);
        g2.dispose();
    }

    private List<MultiLineString> getTexts(INode node) throws Exception
    {
        List<MultiLineString> texts = new ArrayList<MultiLineString>();
        for (Method method : node.getClass().getMethods())
        {
            if (method.getReturnType() == MultiLineString.class && method.getName().startsWith("get")
                    && method.getParameterCount() == 0)
            {
                MultiLineString text = (MultiLineString) method.invoke(node);
                if (text != null)
                {
                    texts.add(text);
                }
            }
        }
        return texts;
    }

    private static final double MARGIN = 6;
    /** Their text is outside their shape, below a drawing, vertical, or has no border; a field is drawn in its object */
    private static final Set<String> WITHOUT_BORDER_AROUND_TEXT = Set.of("FreeTextNode", "ImageNode", "ActorNode",
            "UserLifelineNode", "ProcessStartEventNode", "ProcessEndEventNode", "ProcessGatewayNode", "ProcessPoolNode",
            "FieldNode");
    private static final Set<String> CURVED = Set.of("DecisionNode", "UseCaseNode");
}
