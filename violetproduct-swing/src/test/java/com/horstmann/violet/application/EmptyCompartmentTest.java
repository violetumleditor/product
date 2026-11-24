package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.GraphFile;
import com.horstmann.violet.framework.file.chooser.IFileChooserService;
import com.horstmann.violet.framework.file.chooser.JFileChooserService;
import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanFactory;
import com.horstmann.violet.framework.propertyeditor.customeditor.MultiLineStringEditor;
import com.horstmann.violet.framework.userpreferences.DefaultUserPreferencesDao;
import com.horstmann.violet.framework.userpreferences.IUserPreferencesDao;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.InterfaceNode;

/**
 * The compartments of a class or an interface go away once their texts are emptied in the editor
 */
class EmptyCompartmentTest
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
    void classCompartmentsGoAwayOnceEmptied()
    {
        ClassNode node = new ClassNode();
        add(node);
        double height = getNameHeight(node, node.getName());
        type(node.getAttributes(), "name");
        type(node.getMethods(), "run()");
        double fullHeight = getNameHeight(node, node.getName());
        assertTrue(fullHeight < height);
        type(node.getAttributes(), "");
        assertEquals(fullHeight, getNameHeight(node, node.getName()), "the methods are still there");
        type(node.getMethods(), "");
        assertEquals(height, getNameHeight(node, node.getName()));
    }

    @Test
    void textsAreCenteredVerticallyAndOffTheSides()
    {
        for (int lineCount = 1; lineCount <= 8; lineCount++)
        {
            String text = "a rather long line of text to fill the width" + "\n.".repeat(lineCount - 1);
            ClassNode classNode = new ClassNode();
            add(classNode);
            classNode.getName().setText(text);
            checkTextsFill(classNode, classNode.getName());
            classNode.getAttributes().setText("name");
            classNode.getMethods().setText("run()\nstop()");
            checkTextsFill(classNode, classNode.getName(), classNode.getAttributes(), classNode.getMethods());

            InterfaceNode interfaceNode = new InterfaceNode();
            add(interfaceNode);
            interfaceNode.getName().setText(text);
            checkTextsFill(interfaceNode, interfaceNode.getName());
            interfaceNode.getMethods().setText("run()\nstop()");
            checkTextsFill(interfaceNode, interfaceNode.getName(), interfaceNode.getMethods());
        }
    }

    /**
     * The compartments, whose texts are centered in them, share the whole height of the node
     */
    private void checkTextsFill(INode node, MultiLineString... texts)
    {
        getNameHeight(node, texts[0]);
        double height = 0;
        for (MultiLineString text : texts)
        {
            height += text.getDrawnArea().getHeight();
            assertTrue(text.getDrawnTextArea().getX() >= node.getBounds().getX() + 6);
            assertTrue(text.getDrawnTextArea().getMaxX() <= node.getBounds().getMaxX() - 6);
            assertTrue(text.getBounds().getWidth() <= text.getDrawnTextArea().getWidth());
        }
        assertEquals(node.getBounds().getHeight(), height);
    }

    @Test
    void interfaceCompartmentGoesAwayOnceEmptied()
    {
        InterfaceNode node = new InterfaceNode();
        add(node);
        double height = getNameHeight(node, node.getName());
        type(node.getMethods(), "run()");
        assertTrue(getNameHeight(node, node.getName()) < height);
        type(node.getMethods(), "");
        assertEquals(height, getNameHeight(node, node.getName()));
    }

    @Test
    void compartmentStaysWhileEditedInPlace()
    {
        ClassNode node = new ClassNode();
        add(node);
        double height = getNameHeight(node, node.getName());
        type(node.getAttributes(), "name");
        double fullHeight = getNameHeight(node, node.getName());
        node.getAttributes().setHidden(true);
        type(node.getAttributes(), "");
        assertEquals(fullHeight, getNameHeight(node, node.getName()));
        node.getAttributes().setHidden(false);
        assertEquals(height, getNameHeight(node, node.getName()));
    }

    private void add(INode node)
    {
        IGraph graph = new GraphFile(ClassDiagramGraph.class).getGraph();
        assertTrue(graph.addNode(node, new Point2D.Double(100, 100)));
    }

    /**
     * The name fills the whole node when it has no compartment
     */
    private double getNameHeight(INode node, MultiLineString name)
    {
        Graphics2D g2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        node.draw(g2);
        g2.dispose();
        return name.getDrawnArea().getHeight();
    }

    private void type(MultiLineString string, String text)
    {
        MultiLineStringEditor editor = new MultiLineStringEditor();
        editor.setValue(string);
        editor.getCustomEditor();
        JTextComponent pane = editor.getTextComponent();
        pane.selectAll();
        pane.replaceSelection(text);
    }
}
