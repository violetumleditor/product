package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * The saved HTML page: a viewer with a light and a dark image of the diagram, the version that saved it, and the XML
 * that Violet reads back.
 */
class XHTMLFileTemplateTest
{

    private static final String NOTE_TEXT = "Order ${image} ${content}";

    private String html;

    @BeforeEach
    void saveDiagram() throws IOException
    {
        XHTMLPersistenceService persistenceService = new XHTMLPersistenceService();
        BeanInjector.getInjector().inject(persistenceService);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(createGraph(), out);
        this.html = out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldFillEveryTemplateKey()
    {
        assertFalse(this.html.contains("${version}"), "Version key left in the page");
        assertFalse(this.html.contains("${imageDark}"), "Dark image key left in the page");
        assertTrue(this.html.contains("<meta name=\"generator\" content=\"Violet UML Editor "), "Version of Violet missing");
    }

    @Test
    void shouldEmbedLightAndDarkImages() throws IOException
    {
        BufferedImage light = readImage("diagram-light");
        BufferedImage dark = readImage("diagram-dark");

        assertEquals(light.getWidth(), dark.getWidth());
        assertEquals(light.getHeight(), dark.getHeight());
        // Top right corner: no node there, the note being on the left and the class lower
        int x = light.getWidth() - 1;
        assertEquals(Color.WHITE.getRGB(), light.getRGB(x, 0), "The light image keeps a white background");
        assertNotEquals(light.getRGB(x, 0), dark.getRGB(x, 0));
        assertTrue(getLightness(dark.getRGB(x, 0)) < 0.25, "The dark image has a dark background");
    }

    @Test
    void shouldReadBackTheDiagramWithItsTextAsTyped() throws IOException
    {
        IGraph graph = new XHTMLPersistenceService().read(new ByteArrayInputStream(this.html.getBytes(StandardCharsets.UTF_8)));

        NoteNode note = (NoteNode) graph.getAllNodes().stream().filter(NoteNode.class::isInstance).findFirst().orElseThrow();
        assertTrue(note.getText().getText().contains(NOTE_TEXT), "A text looking like a template key must not be replaced");
    }

    private static IGraph createGraph()
    {
        IGraph graph = new ClassDiagramGraph();
        NoteNode note = new NoteNode();
        MultiLineString text = new MultiLineString();
        text.setText(NOTE_TEXT);
        note.setText(text);
        graph.addNode(note, new Point2D.Double(40, 40));
        graph.addNode(new ClassNode(), new Point2D.Double(400, 300));
        return graph;
    }

    private BufferedImage readImage(String cssClass) throws IOException
    {
        Matcher matcher = Pattern.compile("<img[^>]*class=\"" + cssClass + "\"[^>]*src=\"data:image/png;base64,([^\"]+)\"")
                .matcher(this.html);
        assertTrue(matcher.find(), "No image of class " + cssClass);
        return ImageIO.read(new ByteArrayInputStream(Base64.getMimeDecoder().decode(matcher.group(1))));
    }

    private static float getLightness(int rgb)
    {
        Color color = new Color(rgb);
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2];
    }
}
