package com.horstmann.violet.product.diagram.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;

class ContainerNodeToolIconTest
{

    private static final int ICON_SIZE = 160;

    /** The node is 100 high and its icon is a 160 square centered on it */
    private static final int ICON_TOP = 30;

    private static final int BACKGROUND_X = 110;
    private static final int BACKGROUND_Y = 50;
    private static final int NODE_X = 55;
    private static final int NODE_Y = 52;

    private static final Color BACKGROUND_COLOR = new Color(0xE8DEF5);

    @Test
    void shouldDrawLightIconOnLightTheme()
    {
        BufferedImage image = newImage();
        Graphics2D g2 = newGraphics(image);
        newToolIcon().draw(g2);

        assertEquals(BACKGROUND_COLOR.getRGB(), image.getRGB(BACKGROUND_X, BACKGROUND_Y));
        assertEquals(Color.WHITE.getRGB(), image.getRGB(NODE_X, NODE_Y));
    }

    @Test
    void shouldFollowDarkTheme()
    {
        BufferedImage image = newImage();
        Graphics2D g2 = new DarkDiagramGraphics2D(newGraphics(image));
        newToolIcon().draw(g2);

        assertEquals(DarkDiagramGraphics2D.toDarkColor(BACKGROUND_COLOR).getRGB(), image.getRGB(BACKGROUND_X, BACKGROUND_Y));
        assertEquals(DarkDiagramGraphics2D.toDarkColor(Color.WHITE).getRGB(), image.getRGB(NODE_X, NODE_Y));
    }

    @Test
    void shouldRestoreGraphicsState()
    {
        Graphics2D g2 = newGraphics(newImage());
        g2.setColor(Color.RED);
        Stroke stroke = g2.getStroke();
        AffineTransform transform = g2.getTransform();
        newToolIcon().draw(g2);

        assertEquals(Color.RED, g2.getColor());
        assertEquals(stroke, g2.getStroke());
        assertEquals(transform, g2.getTransform());
    }

    @Test
    void shouldDrawClonesAsPlainFrames()
    {
        BufferedImage image = newImage();
        newToolIcon().clone().draw(newGraphics(image));

        assertEquals(0, image.getRGB(BACKGROUND_X, BACKGROUND_Y));
    }

    private static ContainerNode newToolIcon()
    {
        ContainerNode node = new ContainerNode();
        node.enableToolIcon();
        return node;
    }

    private static BufferedImage newImage()
    {
        return new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D newGraphics(BufferedImage image)
    {
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(0, ICON_TOP);
        return g2;
    }
}
