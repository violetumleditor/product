package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.DecisionNode;

/**
 * A decision is a diamond of standard size, its text outside, also when read from an older file where the text was inside
 */
class DecisionNodeTest
{

    private static final String LONG_TEXT = "Is there any product left in the warehouse?";

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
    void shouldKeepItsSizeWhateverItsText()
    {
        DecisionNode decision = new DecisionNode();
        assertTrue(new ActivityDiagramGraph().addNode(decision, new Point2D.Double(100, 100)));
        Rectangle2D before = decision.getBounds();
        decision.getName().setText(LONG_TEXT);
        assertEquals(before, decision.getBounds());
        assertTrue(decision.getOutsideLabelBounds().getWidth() > before.getWidth());
    }

    @Test
    void shouldStayCenteredWhenReadFromAnOlderFile() throws Exception
    {
        // The former diamond of an empty text was 64.6 x 37.3, the one of a long text far wider
        Rectangle2D small = read(writeFormerFile("")).getBounds();
        assertEquals(432, small.getCenterX(), 10);
        assertEquals(319, small.getCenterY(), 10);
        DecisionNode large = read(writeFormerFile(LONG_TEXT));
        assertEquals(small.getWidth(), large.getBounds().getWidth());
        double formerCenter = 400 + large.getName().getBoundsWithMargins().getWidth() / 2
                + Math.sqrt(3) * 10;
        assertEquals(formerCenter, large.getBounds().getCenterX(), 12);
        assertTrue(large.getName().getText().contains(LONG_TEXT), large.getName().getText());
    }

    @Test
    void shouldNotMoveAgainOnceSavedAndReloaded() throws Exception
    {
        DecisionNode first = read(writeFormerFile(LONG_TEXT));
        String xml = write(first.getGraph());
        assertFalse(xml.contains("<condition"), xml);
        DecisionNode second = read(xml);
        assertEquals(first.getBounds(), second.getBounds());
        assertEquals(first.getName().getText(), second.getName().getText());
    }

    /**
     * @return a diagram as older versions wrote it: the text was the condition
     */
    private String writeFormerFile(String text)
    {
        IGraph graph = new GraphFile(ActivityDiagramGraph.class).getGraph();
        DecisionNode decision = new DecisionNode();
        decision.getName().setText(text);
        assertTrue(graph.addNode(decision, new Point2D.Double(400, 300)));
        String xml = write(graph);
        assertTrue(xml.contains("<name>"), xml);
        return xml.replace("<name>", "<condition>").replace("</name>", "</condition>");
    }

    private String write(IGraph graph)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new XMLPersistenceService().write(graph, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private DecisionNode read(String xml) throws Exception
    {
        IGraph graph = new XMLPersistenceService().read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        return (DecisionNode) graph.getAllNodes().iterator().next();
    }
}
