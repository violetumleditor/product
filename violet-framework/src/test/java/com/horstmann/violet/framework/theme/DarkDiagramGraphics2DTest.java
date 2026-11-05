package com.horstmann.violet.framework.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class DarkDiagramGraphics2DTest
{

    private static float lightness(Color color)
    {
        return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2];
    }

    @Test
    void whiteTurnsDarkAndBlackTurnsLight()
    {
        assertTrue(lightness(DarkDiagramGraphics2D.toDarkColor(Color.WHITE)) < 0.25f);
        assertTrue(lightness(DarkDiagramGraphics2D.toDarkColor(Color.BLACK)) > 0.85f);
    }

    @Test
    void defaultTextStaysReadableOnATintedFill()
    {
        Color text = DarkDiagramGraphics2D.toDarkColor(new Color(91, 91, 91));
        Color fill = DarkDiagramGraphics2D.toDarkColor(new Color(193, 235, 251));
        assertTrue(lightness(text) - lightness(fill) > 0.4f);
    }

    @Test
    void tintKeepsItsHueAndAlphaIsKept()
    {
        Color dark = DarkDiagramGraphics2D.toDarkColor(new Color(251, 199, 183, 100));
        assertTrue(dark.getRed() > dark.getGreen() && dark.getRed() > dark.getBlue());
        assertEquals(100, dark.getAlpha());
    }

    @Test
    void diagramElementsReadBackTheirOwnColor()
    {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D delegate = image.createGraphics();
        Graphics2D g2 = new DarkDiagramGraphics2D(delegate);
        g2.setColor(Color.WHITE);
        assertEquals(Color.WHITE, g2.getColor());
        assertEquals(DarkDiagramGraphics2D.toDarkColor(Color.WHITE), delegate.getColor());
        g2.fillRect(0, 0, 1, 1);
        assertEquals(DarkDiagramGraphics2D.toDarkColor(Color.WHITE).getRGB(), image.getRGB(0, 0));
    }

    @Test
    void copiesKeepTheDiagramColor()
    {
        Graphics2D g2 = new DarkDiagramGraphics2D(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics());
        g2.setColor(Color.RED);
        Graphics2D copy = (Graphics2D) g2.create();
        assertTrue(copy instanceof DarkDiagramGraphics2D);
        assertEquals(Color.RED, copy.getColor());
    }

}
