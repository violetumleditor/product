package com.horstmann.violet.product.diagram.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;

class ImageNodePlaceholderTest
{

    private static final int SIZE = 64;

    private static final int BACKGROUND_X = 12;
    private static final int BACKGROUND_Y = 12;
    private static final int SUN_X = 44;
    private static final int SUN_Y = 19;

    private static final Color BACKGROUND_COLOR = new Color(0xE8DEF5);

    @Test
    void shouldHaveNoImageAndKeepDefaultSize()
    {
        ImageNode node = new ImageNode();

        assertNull(node.getImage());
        assertEquals(new Rectangle2D.Double(0, 0, SIZE, SIZE), node.getUncroppedBounds());
    }

    @Test
    void shouldDrawLightPlaceholderOnLightTheme()
    {
        BufferedImage image = newImage();
        new ImageNode().draw(newGraphics(image));

        assertEquals(BACKGROUND_COLOR.getRGB(), image.getRGB(BACKGROUND_X, BACKGROUND_Y));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(SUN_X, SUN_Y));
    }

    @Test
    void shouldFollowDarkTheme()
    {
        BufferedImage image = newImage();
        new ImageNode().draw(new DarkDiagramGraphics2D(newGraphics(image)));

        assertEquals(DarkDiagramGraphics2D.toDarkColor(BACKGROUND_COLOR).getRGB(), image.getRGB(BACKGROUND_X, BACKGROUND_Y));
        assertEquals(DarkDiagramGraphics2D.toDarkColor(Color.WHITE).getRGB(), image.getRGB(SUN_X, SUN_Y));
    }

    @Test
    void shouldRestoreGraphicsState()
    {
        Graphics2D g2 = newGraphics(newImage());
        g2.setColor(Color.RED);
        Stroke stroke = g2.getStroke();
        AffineTransform transform = g2.getTransform();
        new ImageNode().draw(g2);

        assertEquals(Color.RED, g2.getColor());
        assertEquals(stroke, g2.getStroke());
        assertEquals(transform, g2.getTransform());
    }

    @Test
    void shouldKeepSquareRatioWhenResized()
    {
        ImageNode node = new ImageNode();
        node.setPreferredSize(new Rectangle2D.Double(0, 0, 128, 300));

        assertEquals(128, node.getUncroppedBounds().getWidth());
        assertEquals(128, node.getUncroppedBounds().getHeight());
    }

    @Test
    void shouldDrawImageInsteadOfPlaceholderOnceSet()
    {
        ImageNode node = new ImageNode();
        node.setImage(new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB));

        BufferedImage image = newImage();
        node.draw(newGraphics(image));

        assertNotEquals(BACKGROUND_COLOR.getRGB(), image.getRGB(BACKGROUND_X, BACKGROUND_Y));
    }

    private static BufferedImage newImage()
    {
        return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D newGraphics(BufferedImage image)
    {
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }
}
