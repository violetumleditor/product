package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JPanel;

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
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.object.ObjectDiagramGraph;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;
import com.horstmann.violet.workspace.editorpart.EditorPart;
import com.horstmann.violet.workspace.editorpart.behavior.CutCopyPasteBehavior;

class CutCopyPasteBehaviorTest
{

    private static final int OFFSET = 20;

    /** Lets the tests run anywhere, without touching the system clipboard */
    private static class TestedBehavior extends CutCopyPasteBehavior
    {
        private final Clipboard clipboard = new Clipboard("test");

        TestedBehavior(EditorPart editorPart)
        {
            super(editorPart);
        }

        @Override
        protected Clipboard getClipboard()
        {
            return this.clipboard;
        }

        void click(double x, double y)
        {
            onMousePressed(new MouseEvent(new JPanel(), MouseEvent.MOUSE_PRESSED, 0, 0, (int) x, (int) y, 1, false));
        }

        void putImage(Image image)
        {
            this.clipboard.setContents(new Transferable()
            {
                @Override
                public DataFlavor[] getTransferDataFlavors()
                {
                    return new DataFlavor[] { DataFlavor.imageFlavor };
                }

                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor)
                {
                    return DataFlavor.imageFlavor.equals(flavor);
                }

                @Override
                public Object getTransferData(DataFlavor flavor)
                {
                    return image;
                }
            }, null);
        }
    }

    @BeforeAll
    static void registerBeans()
    {
        BeanFactory.getFactory().register(IUserPreferencesDao.class, new DefaultUserPreferencesDao());
        BeanFactory.getFactory().register(IFileChooserService.class, new JFileChooserService());
        BeanFactory.getFactory().register(IFilePersistenceService.class, new XHTMLPersistenceService());
    }

    static Stream<IGraph> graphs()
    {
        return Stream.of(new ClassDiagramGraph(), new SequenceDiagramGraph(), new ActivityDiagramGraph(), new StateDiagramGraph(),
                new ObjectDiagramGraph(), new UseCaseDiagramGraph());
    }

    @ParameterizedTest
    @MethodSource("graphs")
    void shouldOfferImageToolInEveryDiagram(IGraph graph)
    {
        INode prototype = graph.getNodePrototypes().stream()
                .filter(ImageNode.class::isInstance)
                .findFirst()
                .orElseThrow();

        assertNotNull(prototype.getToolTip());
        assertFalse(prototype.getToolTip().isBlank());
    }

    @ParameterizedTest
    @MethodSource("graphs")
    void shouldCopyAndPasteImageInEveryDiagram(IGraph graph)
    {
        EditorPart editorPart = new EditorPart(graph);
        TestedBehavior behavior = new TestedBehavior(editorPart);
        ImageNode original = addImage(graph, 100, 100);

        // The mouse stays on the image, as when it has just been selected
        behavior.click(110, 110);
        select(editorPart, original);
        behavior.copy();
        behavior.paste();

        List<ImageNode> images = images(graph);
        assertEquals(2, images.size());
        ImageNode pasted = images.stream().filter(n -> n != original).findFirst().orElseThrow();
        assertNull(pasted.getParent());
        assertEquals(100 + OFFSET, pasted.getLocationOnGraph().getX());
        assertEquals(100 + OFFSET, pasted.getLocationOnGraph().getY());
        assertTrue(editorPart.getSelectionHandler().isElementAlreadySelected(pasted), "The new image should be selected");
    }

    @Test
    void shouldShiftEachNewPasteOfImage()
    {
        IGraph graph = new ClassDiagramGraph();
        EditorPart editorPart = new EditorPart(graph);
        TestedBehavior behavior = new TestedBehavior(editorPart);
        select(editorPart, addImage(graph, 100, 100));

        behavior.copy();
        behavior.paste();
        behavior.paste();
        behavior.paste();

        List<Double> locations = images(graph).stream().map(n -> n.getLocationOnGraph().getX()).sorted().collect(Collectors.toList());
        assertEquals(List.of(100d, 120d, 140d, 160d), locations);
    }

    @Test
    void shouldPasteAtMouseAfterAClick()
    {
        IGraph graph = new ClassDiagramGraph();
        EditorPart editorPart = new EditorPart(graph);
        TestedBehavior behavior = new TestedBehavior(editorPart);
        ImageNode original = addImage(graph, 100, 100);
        select(editorPart, original);

        behavior.copy();
        behavior.click(400, 300);
        behavior.paste();

        ImageNode pasted = images(graph).stream().filter(n -> n != original).findFirst().orElseThrow();
        assertEquals(400, pasted.getLocationOnGraph().getX());
        assertEquals(300, pasted.getLocationOnGraph().getY());
    }

    @Test
    void shouldShiftImagesPastedFromOutside()
    {
        IGraph graph = new SequenceDiagramGraph();
        TestedBehavior behavior = new TestedBehavior(new EditorPart(graph));
        behavior.putImage(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB));

        behavior.click(100, 100);
        behavior.paste();
        behavior.paste();

        List<Double> locations = images(graph).stream().map(n -> n.getLocationOnGraph().getX()).sorted().collect(Collectors.toList());
        assertEquals(List.of(100d, 120d), locations);
    }

    @Test
    void shouldPasteClassBesideTheCopiedOne()
    {
        IGraph graph = new ClassDiagramGraph();
        EditorPart editorPart = new EditorPart(graph);
        TestedBehavior behavior = new TestedBehavior(editorPart);
        ClassNode original = new ClassNode();
        graph.addNode(original, new Point2D.Double(100, 100));

        behavior.click(110, 110);
        select(editorPart, original);
        behavior.copy();
        behavior.paste();
        behavior.paste();

        List<INode> classes = graph.getAllNodes().stream().filter(ClassNode.class::isInstance).collect(Collectors.toList());
        assertEquals(3, classes.size());
        assertEquals(3, classes.stream().map(n -> n.getLocationOnGraph()).distinct().count());
        for (INode aClass : classes)
        {
            assertTrue(aClass == original || aClass.getLocationOnGraph().getX() > 100);
            assertTrue(aClass == original || aClass.getLocationOnGraph().getY() > 100);
        }
    }

    @Test
    void shouldStillPasteClassIntoClickedPackage()
    {
        IGraph graph = new ClassDiagramGraph();
        EditorPart editorPart = new EditorPart(graph);
        TestedBehavior behavior = new TestedBehavior(editorPart);
        ClassNode classNode = new ClassNode();
        PackageNode packageNode = new PackageNode();
        graph.addNode(classNode, new Point2D.Double(400, 400));
        graph.addNode(packageNode, new Point2D.Double(100, 100));
        double x = packageNode.getBounds().getCenterX();
        double y = packageNode.getBounds().getCenterY();

        select(editorPart, classNode);
        behavior.copy();
        behavior.click(x, y);
        behavior.paste();

        assertEquals(1, packageNode.getChildren().size());
    }

    @Test
    void shouldAttachImageToGraphWhenAddedOnAnotherNode()
    {
        IGraph graph = new ClassDiagramGraph();
        ClassNode classNode = new ClassNode();
        graph.addNode(classNode, new Point2D.Double(100, 100));

        ImageNode image = new ImageNode();
        assertTrue(graph.addNode(image, new Point2D.Double(110, 110)));

        assertNull(image.getParent());
        assertNotEquals(classNode, image.getParent());
        assertEquals(110, image.getLocationOnGraph().getX());
    }

    private static ImageNode addImage(IGraph graph, double x, double y)
    {
        ImageNode image = new ImageNode(new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB));
        graph.addNode(image, new Point2D.Double(x, y));
        return image;
    }

    private static void select(EditorPart editorPart, INode node)
    {
        editorPart.getSelectionHandler().clearSelection();
        editorPart.getSelectionHandler().addSelectedElement(node);
    }

    private static List<ImageNode> images(IGraph graph)
    {
        return graph.getAllNodes().stream()
                .filter(ImageNode.class::isInstance)
                .map(ImageNode.class::cast)
                .collect(Collectors.toList());
    }
}
