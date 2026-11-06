package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.JSONPersistenceService;
import com.horstmann.violet.framework.file.persistence.XHTMLPersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.CropInsets;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
import com.horstmann.violet.product.diagram.common.ImageNode;

class ImageNodePersistenceTest
{

    private static final int WIDTH = 40;
    private static final int HEIGHT = 20;
    private static final int PIXEL = 0xFF3366CC;

    static Stream<IFilePersistenceService> persistenceServices()
    {
        XHTMLPersistenceService xhtmlPersistenceService = new XHTMLPersistenceService();
        BeanInjector.getInjector().inject(xhtmlPersistenceService);
        return Stream.of(xhtmlPersistenceService, new XMLPersistenceService(), new JSONPersistenceService());
    }

    @Test
    void shouldOnlyExposeImageToPropertyEditor() throws IntrospectionException
    {
        List<String> editableProperties = Arrays.stream(Introspector.getBeanInfo(ImageNode.class).getPropertyDescriptors())
                .filter(descriptor -> descriptor.getReadMethod() != null && descriptor.getWriteMethod() != null)
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toList());

        assertTrue(editableProperties.contains("image"), "Image should stay editable");
        assertFalse(editableProperties.contains("imageBase64"), "Image content should not be editable as text");
    }

    @ParameterizedTest
    @MethodSource("persistenceServices")
    void shouldKeepImageSizeAndCrop(IFilePersistenceService persistenceService) throws IOException
    {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(3, 4, PIXEL);
        ImageNode imageNode = new ImageNode(image);
        imageNode.setPreferredSize(new Rectangle2D.Double(0, 0, 2 * WIDTH, 2 * HEIGHT));
        imageNode.setCropInsets(new CropInsets(1, 2, 3, 4));

        ImageNode restored = reload(persistenceService, imageNode);

        assertEquals(WIDTH, restored.getImage().getWidth(null));
        assertEquals(HEIGHT, restored.getImage().getHeight(null));
        assertEquals(PIXEL, toBufferedImage(restored.getImage()).getRGB(3, 4));
        assertEquals(2 * WIDTH, restored.getPreferredSize().getWidth());
        assertEquals(2 * HEIGHT, restored.getPreferredSize().getHeight());
        assertEquals(1, restored.getCropInsets().getTop());
        assertEquals(2, restored.getCropInsets().getLeft());
        assertEquals(3, restored.getCropInsets().getBottom());
        assertEquals(4, restored.getCropInsets().getRight());
    }

    @ParameterizedTest
    @MethodSource("persistenceServices")
    void shouldKeepPlaceholder(IFilePersistenceService persistenceService) throws IOException
    {
        assertNull(reload(persistenceService, new ImageNode()).getImage());
    }

    private static ImageNode reload(IFilePersistenceService persistenceService, ImageNode imageNode) throws IOException
    {
        IGraph graph = new ClassDiagramGraph();
        graph.addNode(imageNode, new Point2D.Double(40, 40));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);

        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));
        return (ImageNode) reloaded.getAllNodes().stream()
                .filter(ImageNode.class::isInstance)
                .findFirst()
                .orElseThrow();
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
