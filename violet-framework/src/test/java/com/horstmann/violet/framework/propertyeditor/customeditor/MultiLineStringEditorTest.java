package com.horstmann.violet.framework.propertyeditor.customeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComboBox;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;

@ExtendWith(MultiLineStringEditorTest.OnEventDispatchThread.class)
class MultiLineStringEditorTest
{
    private static final Color RED = new Color(246, 132, 98);
    private static final Color BLUE = new Color(132, 147, 202);

    @Test
    void offersThePaletteBorderColors()
    {
        JComboBox<?> colorBox = new EditorFixture(new MultiLineString()).colorBox;

        List<Object> items = new ArrayList<Object>();
        for (int i = 0; i < colorBox.getItemCount(); i++)
        {
            items.add(colorBox.getItemAt(i));
        }
        assertEquals(ColorToolsBarPanel.getBorderColors(), items);
    }

    @Test
    void colorsTheSelectionOnly()
    {
        MultiLineString text = plainText("hello world");
        EditorFixture editor = new EditorFixture(text);

        editor.textPane.select(1, 6);
        editor.colorBox.setSelectedItem(RED);

        assertTrue(text.getText().contains("<font color=\"#f68462\">hello</font>"), text.getText());
        assertTrue(text.getText().contains("world"), text.getText());
    }

    @Test
    void reopenedTextShowsItsColorAndCanBeRecolored()
    {
        MultiLineString text = plainText("hello");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 6);
        editor.colorBox.setSelectedItem(RED);

        EditorFixture reopened = new EditorFixture(text);
        reopened.textPane.select(1, 6);
        assertEquals(RED, reopened.colorBox.getSelectedItem());

        reopened.colorBox.setSelectedItem(BLUE);

        assertTrue(text.getText().contains("#8493ca"), text.getText());
        assertFalse(text.getText().contains("#f68462"), text.getText());
    }

    @Test
    void typedTextTakesThePickedColorOverThePreviousOne()
    {
        MultiLineString text = plainText("hello");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 6);
        editor.colorBox.setSelectedItem(RED);

        EditorFixture reopened = new EditorFixture(text);
        reopened.textPane.setCaretPosition(3);
        reopened.colorBox.setSelectedItem(BLUE);
        reopened.textPane.replaceSelection("X");

        assertTrue(text.getText().contains("color=\"#8493ca\">X</font>"), text.getText());
    }

    @Test
    void coloredTextIsDrawnInTheDiagram()
    {
        MultiLineString text = plainText("HHHHHH");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 7);
        editor.colorBox.setSelectedItem(RED);

        assertTrue(containsPixel(draw(text, BLUE), RED));
    }

    @Test
    void uncoloredTextShowsTheDefaultColorWithoutStoringIt()
    {
        Color defaultColor = ColorToolsBarPanel.DEFAULT_COLOR.getTextColor();
        MultiLineString text = plainText("HHHHHH");
        EditorFixture editor = new EditorFixture(text);

        assertEquals(defaultColor, editor.colorBox.getSelectedItem());

        editor.textPane.setSize(300, 100);
        BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        editor.textPane.paint(g2);
        g2.dispose();
        assertTrue(containsPixel(image, defaultColor));

        editor.textPane.select(1, 3);
        editor.textPane.replaceSelection("HH");
        assertFalse(text.getText().contains("<font color"), text.getText());
    }

    @Test
    void uncoloredTextIsDrawnWithTheGraphicsColor()
    {
        Color textColor = ColorToolsBarPanel.DEFAULT_COLOR.getTextColor();

        assertTrue(containsPixel(draw(plainText("HHHHHH"), textColor), textColor));
    }

    private static BufferedImage draw(MultiLineString text, Color graphicsColor)
    {
        Rectangle2D bounds = text.getBounds();
        BufferedImage image = new BufferedImage((int) bounds.getWidth(), (int) bounds.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setColor(graphicsColor);
        text.draw(g2, bounds);
        g2.dispose();
        return image;
    }

    private static boolean containsPixel(BufferedImage image, Color color)
    {
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                if (image.getRGB(x, y) == color.getRGB())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static MultiLineString plainText(String value)
    {
        MultiLineString text = new MultiLineString();
        text.setText(value);
        return text;
    }

    /**
     * The editor posts toolbar updates to the event dispatch thread, so tests must drive it from there too.
     */
    static class OnEventDispatchThread implements InvocationInterceptor
    {
        @Override
        public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
                ExtensionContext extensionContext) throws Throwable
        {
            AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
            SwingUtilities.invokeAndWait(() -> {
                try
                {
                    invocation.proceed();
                }
                catch (Throwable throwable)
                {
                    failure.set(throwable);
                }
            });
            if (failure.get() != null)
            {
                throw failure.get();
            }
        }
    }

    private static class EditorFixture
    {
        final JTextPane textPane;
        final JComboBox<?> colorBox;

        EditorFixture(MultiLineString value)
        {
            MultiLineStringEditor editor = new MultiLineStringEditor();
            editor.setValue(value);
            List<Component> components = new ArrayList<Component>();
            collect((Container) editor.getCustomEditor(), components);
            JTextPane pane = null;
            JComboBox<?> box = null;
            for (Component aComponent : components)
            {
                if (aComponent instanceof JTextPane)
                {
                    pane = (JTextPane) aComponent;
                }
                if (aComponent instanceof JComboBox && ((JComboBox<?>) aComponent).getItemAt(0) instanceof Color)
                {
                    box = (JComboBox<?>) aComponent;
                }
            }
            this.textPane = pane;
            this.colorBox = box;
        }

        private static void collect(Container parent, List<Component> components)
        {
            for (Component aComponent : parent.getComponents())
            {
                components.add(aComponent);
                if (aComponent instanceof Container)
                {
                    collect((Container) aComponent, components);
                }
            }
        }
    }
}
