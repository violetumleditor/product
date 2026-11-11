package com.horstmann.violet.framework.theme;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws a diagram for a dark screen by mirroring the lightness of its colors on the fly : white shapes turn dark, black lines turn
 * light, and tinted elements keep their hue. Diagrams, their files, exports and printouts keep their real colors, as only the
 * drawing on screen goes through this class. Images are left as they are.
 */
public class DarkDiagramGraphics2D extends Graphics2D
{

    /**
     * @param delegate where to draw
     */
    public DarkDiagramGraphics2D(Graphics2D delegate)
    {
        this.delegate = delegate;
        setColor(delegate.getColor());
        setBackground(delegate.getBackground());
    }

    /**
     * @param color from a diagram
     * @return its counterpart on a dark screen
     */
    public static Color toDarkColor(Color color)
    {
        return DARK_COLORS.computeIfAbsent(color.getRGB(), argb -> computeDarkColor(color));
    }

    private static Color computeDarkColor(Color color)
    {
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float lightness = (max + min) / 2;
        float chroma = max - min;
        float saturation = chroma == 0 ? 0 : chroma / (1 - Math.abs(2 * lightness - 1));
        float darkLightness = DARKEST_LIGHTNESS + (LIGHTEST_LIGHTNESS - DARKEST_LIGHTNESS) * mirrorLightness(lightness);
        if (saturation < NEUTRAL_SATURATION)
        {
            return fromHsl(NEUTRAL_HUE, NEUTRAL_TINT, darkLightness, color.getAlpha());
        }
        float hue;
        if (max == red)
        {
            hue = ((green - blue) / chroma + 6) % 6 / 6;
        }
        else if (max == green)
        {
            hue = ((blue - red) / chroma + 2) / 6;
        }
        else
        {
            hue = ((red - green) / chroma + 4) / 6;
        }
        return fromHsl(hue, saturation * SATURATION_FACTOR, darkLightness, color.getAlpha());
    }

    /**
     * @param lightness from 0 to 1
     * @return the lightness of the mirrored color, from 1 to 0 : fills stay dim, borders (as light as the default grey one or a
     *         pastel one) stand out from them, and anything as dark as a default text gets bright
     */
    private static float mirrorLightness(float lightness)
    {
        float darkness = 1 - lightness;
        if (darkness < FILL_DARKNESS_LIMIT)
        {
            return darkness * FILL_LIGHTNESS_LIMIT / FILL_DARKNESS_LIMIT;
        }
        if (darkness < BORDER_DARKNESS_LIMIT)
        {
            float ratio = (darkness - FILL_DARKNESS_LIMIT) / (BORDER_DARKNESS_LIMIT - FILL_DARKNESS_LIMIT);
            return FILL_LIGHTNESS_LIMIT + ratio * (BORDER_LIGHTNESS_LIMIT - FILL_LIGHTNESS_LIMIT);
        }
        float ratio = (darkness - BORDER_DARKNESS_LIMIT) / (TEXT_DARKNESS_LIMIT - BORDER_DARKNESS_LIMIT);
        return Math.min(1, BORDER_LIGHTNESS_LIMIT + ratio * (1 - BORDER_LIGHTNESS_LIMIT));
    }

    private static Color fromHsl(float hue, float saturation, float lightness, int alpha)
    {
        float chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        float min = lightness - chroma / 2;
        return new Color(toChannel(hue + 1 / 3f, chroma, min), toChannel(hue, chroma, min), toChannel(hue - 1 / 3f, chroma, min),
                alpha / 255f);
    }

    /**
     * @param hue of the color, shifted according to the channel
     * @param chroma
     * @param min
     * @return the value of a red, green or blue channel
     */
    private static float toChannel(float hue, float chroma, float min)
    {
        float position = ((hue % 1 + 1) % 1) * 6;
        float level = 0;
        if (position < 1)
        {
            level = position;
        }
        else if (position < 3)
        {
            level = 1;
        }
        else if (position < 4)
        {
            level = 4 - position;
        }
        return Math.max(0, Math.min(1, min + chroma * level));
    }

    /**
     * @return the color asked by the diagram, as diagram elements save and restore it
     */
    @Override
    public Color getColor()
    {
        return this.color;
    }

    @Override
    public void setColor(Color color)
    {
        this.color = color;
        this.paint = color;
        this.delegate.setColor(color == null ? null : toDarkColor(color));
    }

    @Override
    public Paint getPaint()
    {
        return this.paint;
    }

    @Override
    public void setPaint(Paint paint)
    {
        if (paint instanceof Color)
        {
            setColor((Color) paint);
            return;
        }
        this.paint = paint;
        this.delegate.setPaint(paint);
    }

    @Override
    public Color getBackground()
    {
        return this.background;
    }

    @Override
    public void setBackground(Color color)
    {
        this.background = color;
        this.delegate.setBackground(toDarkColor(color));
    }

    @Override
    public void setXORMode(Color color)
    {
        this.delegate.setXORMode(toDarkColor(color));
    }

    @Override
    public Graphics create()
    {
        Graphics2D delegateCopy = (Graphics2D) this.delegate.create();
        delegateCopy.setColor(this.color);
        delegateCopy.setBackground(this.background);
        DarkDiagramGraphics2D copy = new DarkDiagramGraphics2D(delegateCopy);
        copy.setPaint(this.paint);
        return copy;
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer)
    {
        return this.delegate.drawImage(img, x, y, toDarkColor(bgcolor), observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer)
    {
        return this.delegate.drawImage(img, x, y, width, height, toDarkColor(bgcolor), observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor,
            ImageObserver observer)
    {
        return this.delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, toDarkColor(bgcolor), observer);
    }

    @Override
    public void draw(Shape s)
    {
        this.delegate.draw(s);
    }

    @Override
    public void fill(Shape s)
    {
        this.delegate.fill(s);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs)
    {
        return this.delegate.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y)
    {
        this.delegate.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform)
    {
        this.delegate.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform)
    {
        this.delegate.drawRenderableImage(img, xform);
    }

    @Override
    public void drawString(String str, int x, int y)
    {
        this.delegate.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y)
    {
        this.delegate.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        this.delegate.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y)
    {
        this.delegate.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y)
    {
        this.delegate.drawGlyphVector(g, x, y);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke)
    {
        return this.delegate.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration()
    {
        return this.delegate.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp)
    {
        this.delegate.setComposite(comp);
    }

    @Override
    public Composite getComposite()
    {
        return this.delegate.getComposite();
    }

    @Override
    public void setStroke(Stroke s)
    {
        this.delegate.setStroke(s);
    }

    @Override
    public Stroke getStroke()
    {
        return this.delegate.getStroke();
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue)
    {
        this.delegate.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey)
    {
        return this.delegate.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints)
    {
        this.delegate.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints)
    {
        this.delegate.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints()
    {
        return this.delegate.getRenderingHints();
    }

    @Override
    public void translate(int x, int y)
    {
        this.delegate.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty)
    {
        this.delegate.translate(tx, ty);
    }

    @Override
    public void rotate(double theta)
    {
        this.delegate.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y)
    {
        this.delegate.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy)
    {
        this.delegate.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy)
    {
        this.delegate.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx)
    {
        this.delegate.transform(tx);
    }

    @Override
    public void setTransform(AffineTransform tx)
    {
        this.delegate.setTransform(tx);
    }

    @Override
    public AffineTransform getTransform()
    {
        return this.delegate.getTransform();
    }

    @Override
    public void clip(Shape s)
    {
        this.delegate.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext()
    {
        return this.delegate.getFontRenderContext();
    }

    @Override
    public void setPaintMode()
    {
        this.delegate.setPaintMode();
    }

    @Override
    public Font getFont()
    {
        return this.delegate.getFont();
    }

    @Override
    public void setFont(Font font)
    {
        this.delegate.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f)
    {
        return this.delegate.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds()
    {
        return this.delegate.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height)
    {
        this.delegate.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height)
    {
        this.delegate.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip()
    {
        return this.delegate.getClip();
    }

    @Override
    public void setClip(Shape clip)
    {
        this.delegate.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {
        this.delegate.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2)
    {
        this.delegate.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height)
    {
        this.delegate.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height)
    {
        this.delegate.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        this.delegate.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        this.delegate.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height)
    {
        this.delegate.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height)
    {
        this.delegate.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        this.delegate.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        this.delegate.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
    {
        this.delegate.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        this.delegate.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p)
    {
        this.delegate.drawPolygon(p);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
    {
        this.delegate.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Polygon p)
    {
        this.delegate.fillPolygon(p);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer)
    {
        return this.delegate.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer)
    {
        return this.delegate.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer)
    {
        return this.delegate.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public void dispose()
    {
        this.delegate.dispose();
    }

    private static final float DARKEST_LIGHTNESS = 0.16f;
    private static final float LIGHTEST_LIGHTNESS = 0.93f;
    /** The lightest backgrounds of the palette (white, pastel ones) are below, its borders (the default grey one) above */
    private static final float FILL_DARKNESS_LIMIT = 0.22f;
    private static final float FILL_LIGHTNESS_LIMIT = 0.26f;
    /** The default border grey (191, 191, 191) */
    private static final float BORDER_DARKNESS_LIMIT = 0.25f;
    private static final float BORDER_LIGHTNESS_LIMIT = 0.6f;
    private static final float TEXT_DARKNESS_LIMIT = 0.65f;
    private static final float NEUTRAL_SATURATION = 0.08f;
    private static final float NEUTRAL_HUE = 0.71f;
    private static final float NEUTRAL_TINT = 0.14f;
    private static final float SATURATION_FACTOR = 0.5f;
    private static final Map<Integer, Color> DARK_COLORS = new ConcurrentHashMap<Integer, Color>();

    private final Graphics2D delegate;
    private Color color;
    private Paint paint;
    private Color background;

}
