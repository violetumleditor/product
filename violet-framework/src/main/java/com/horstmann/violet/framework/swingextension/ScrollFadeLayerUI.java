package com.horstmann.violet.framework.swingextension;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.plaf.LayerUI;

/**
 * Fades the top and the bottom of the visible part of a scrolled component into its background, while some content is hidden on
 * that side: shows that it can be scrolled without looking at the scroll bar. The layer goes between the component and the
 * viewport, which must repaint the whole view when it scrolls (JViewport.SIMPLE_SCROLL_MODE), else the blitting copies the fades.
 */
public class ScrollFadeLayerUI extends LayerUI<Component>
{

    @Override
    public void paint(Graphics g, JComponent c)
    {
        super.paint(g, c);
        Component view = ((JLayer<?>) c).getView();
        if (view == null)
        {
            return;
        }
        Rectangle visible = c.getVisibleRect();
        if (visible.isEmpty())
        {
            return;
        }
        Color opaque = view.getBackground();
        Color transparent = new Color(opaque.getRed(), opaque.getGreen(), opaque.getBlue(), 0);
        int height = Math.min(FADE_HEIGHT, visible.height / 3);
        Graphics2D g2 = (Graphics2D) g.create();
        if (visible.y > 0)
        {
            g2.setPaint(new GradientPaint(0, visible.y, opaque, 0, visible.y + height, transparent));
            g2.fillRect(visible.x, visible.y, visible.width, height);
        }
        int bottom = visible.y + visible.height;
        if (bottom < c.getHeight())
        {
            g2.setPaint(new GradientPaint(0, bottom - height, transparent, 0, bottom, opaque));
            g2.fillRect(visible.x, bottom - height, visible.width, height);
        }
        g2.dispose();
    }

    private static final int FADE_HEIGHT = 28;

}
