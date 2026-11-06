package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.ImageNode;

class ImageNodeClipboardSerializationTest
{

    @Test
    void shouldSerializeAndDeserializeImageNodeWithoutBufferedImageCtorErrors() throws IOException
    {
        IGraph graph = new ClassDiagramGraph();

        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 4, 0xFF00AA11);

        ImageNode imageNode = new ImageNode(image);
        graph.addNode(imageNode, imageNode.getLocationOnGraph());

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);

        String serialized = out.toString(StandardCharsets.UTF_8);
        assertTrue(serialized.contains("<ClassDiagramGraph") || serialized.contains("<classDiagramGraph"),
            "Compatible persistence should write graph XML");
        assertTrue(!serialized.startsWith("<java"),
            "Compatible persistence should not default to Java XMLEncoder XML");

        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        assertNotNull(reloaded, "Deserialized graph should not be null");
        assertTrue(reloaded.getAllNodes().stream().anyMatch(ImageNode.class::isInstance),
                "Deserialized graph should contain an ImageNode");

        ImageNode restoredImageNode = (ImageNode) reloaded.getAllNodes().stream()
                .filter(ImageNode.class::isInstance)
                .findFirst()
                .orElseThrow();
        assertNotNull(restoredImageNode.getImage(), "Restored ImageNode should contain an image");

        BufferedImage restoredImage = toBufferedImage(restoredImageNode.getImage());
        assertEquals(0xFF00AA11, restoredImage.getRGB(3, 4),
                "Restored image should keep original pixel content");
    }

    @Test
    void shouldKeepPlaceholderOfImageNodeWithoutImage() throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        ImageNode imageNode = new ImageNode();
        graph.addNode(imageNode, imageNode.getLocationOnGraph());

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);

        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        ImageNode restoredImageNode = (ImageNode) reloaded.getAllNodes().stream()
                .filter(ImageNode.class::isInstance)
                .findFirst()
                .orElseThrow();
        assertNull(restoredImageNode.getImage(), "No image should be stored for the placeholder");
    }

    private static BufferedImage toBufferedImage(Image image)
    {
        if (image instanceof BufferedImage)
        {
            return (BufferedImage) image;
        }
        BufferedImage converted = new BufferedImage(image.getWidth(null), image.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return converted;
    }
}
