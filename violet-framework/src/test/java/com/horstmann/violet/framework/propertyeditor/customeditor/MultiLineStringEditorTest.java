package com.horstmann.violet.framework.propertyeditor.customeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JTextPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import com.formdev.flatlaf.FlatDarkLaf;
import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

@ExtendWith(MultiLineStringEditorTest.OnEventDispatchThread.class)
class MultiLineStringEditorTest
{
    private static final Color RED = ColorChoice.TEXT_COLOR_LIST.get(2);
    private static final Color BLUE = ColorChoice.TEXT_COLOR_LIST.get(12);

    @Test
    void offersTheTextColorPalette()
    {
        TextColorButton colorButton = new EditorFixture(new MultiLineString()).colorButton;

        assertEquals(ColorChoice.TEXT_COLOR_LIST, colorButton.getColors());
    }

    @Test
    void showsTheSizeOfTheTextAtTheCaret()
    {
        MultiLineString text = plainText("aaaa bbbb");
        EditorFixture editor = new EditorFixture(text);
        javax.swing.text.SimpleAttributeSet size = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setFontSize(size, 24);
        editor.textPane.getStyledDocument().setCharacterAttributes(6, 4, size, false);

        EditorFixture reopened = new EditorFixture(text);
        reopened.textPane.setCaretPosition(8);
        assertEquals("24", reopened.sizeButton.getText());
        reopened.textPane.setCaretPosition(3);
        assertEquals("12", reopened.sizeButton.getText());
    }

    @Test
    void colorsTheSelectionOnly()
    {
        MultiLineString text = plainText("hello world");
        EditorFixture editor = new EditorFixture(text);

        editor.textPane.select(1, 6);
        editor.colorButton.setSelectedColor(RED);

        assertTrue(text.getText().contains("<font color=\"" + hex(RED) + "\">hello</font>"), text.getText());
        assertTrue(text.getText().contains("world"), text.getText());
    }

    @Test
    void reopenedTextShowsItsColorAndCanBeRecolored()
    {
        MultiLineString text = plainText("hello");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 6);
        editor.colorButton.setSelectedColor(RED);

        EditorFixture reopened = new EditorFixture(text);
        reopened.textPane.select(1, 6);
        assertEquals(RED, reopened.colorButton.getSelectedColor());

        reopened.colorButton.setSelectedColor(BLUE);

        assertTrue(text.getText().contains(hex(BLUE)), text.getText());
        assertFalse(text.getText().contains(hex(RED)), text.getText());
    }

    @Test
    void typedTextTakesThePickedColorOverThePreviousOne()
    {
        MultiLineString text = plainText("hello");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 6);
        editor.colorButton.setSelectedColor(RED);

        EditorFixture reopened = new EditorFixture(text);
        reopened.textPane.setCaretPosition(3);
        reopened.colorButton.setSelectedColor(BLUE);
        reopened.textPane.replaceSelection("X");

        assertTrue(text.getText().contains("color=\"" + hex(BLUE) + "\">X</font>"), text.getText());
    }

    @Test
    void coloredTextIsDrawnInTheDiagram()
    {
        MultiLineString text = plainText("HHHHHH");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 7);
        editor.colorButton.setSelectedColor(RED);

        assertTrue(containsPixel(draw(text, BLUE), RED));
    }

    @Test
    void uncoloredTextShowsTheDefaultColorWithoutStoringIt()
    {
        Color defaultColor = ColorChoice.DEFAULT_TEXT_COLOR;
        MultiLineString text = plainText("HHHHHH");
        EditorFixture editor = new EditorFixture(text);

        assertEquals(defaultColor, editor.colorButton.getSelectedColor());

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
    void textAreaFollowsTheDarkThemeWithoutChangingTheText() throws Exception
    {
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new FlatDarkLaf());
        try
        {
            MultiLineString text = plainText("HHHHHH");
            String storedText = text.getText();
            EditorFixture editor = new EditorFixture(text);

            editor.textPane.setSize(300, 100);
            BufferedImage image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();
            editor.textPane.paint(g2);
            g2.dispose();

            assertEquals(DarkDiagramGraphics2D.toDarkColor(Color.WHITE).getRGB(), image.getRGB(299, 99));
            assertTrue(containsPixel(image, DarkDiagramGraphics2D.toDarkColor(ColorChoice.DEFAULT_TEXT_COLOR)));
            assertEquals(storedText, text.getText());
        }
        finally
        {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    void uncoloredTextIsDrawnWithTheGraphicsColor()
    {
        Color textColor = ColorChoice.DEFAULT_TEXT_COLOR;

        assertTrue(containsPixel(draw(plainText("HHHHHH"), textColor), textColor));
    }

    @Test
    void enterStartsANewLineInTextNotTypedInTheEditor()
    {
        MultiLineString text = plainText("hello");
        double singleLineHeight = text.getBounds().getHeight();
        EditorFixture editor = new EditorFixture(text);

        editor.pressEnterAt(3);

        assertTrue(text.getBounds().getHeight() > 1.5 * singleLineHeight, text.getText());
    }

    @Test
    void enterStartsANewLineInTextSavedInsideADiv()
    {
        MultiLineString text = plainText("<html><head></head><body><div align=\"left\">hello</div></body></html>");
        double singleLineHeight = text.getBounds().getHeight();
        EditorFixture editor = new EditorFixture(text);

        editor.pressEnterAt(3);

        assertTrue(text.getBounds().getHeight() > 1.5 * singleLineHeight, text.getText());
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
        final TextColorButton colorButton;
        final javax.swing.JButton sizeButton;

        EditorFixture(MultiLineString value)
        {
            MultiLineStringEditor editor = new MultiLineStringEditor();
            editor.setValue(value);
            List<Component> components = new ArrayList<Component>();
            collect((Container) editor.getCustomEditor(), components);
            JTextPane pane = null;
            TextColorButton button = null;
            javax.swing.JButton size = null;
            for (Component aComponent : components)
            {
                if (aComponent instanceof JTextPane)
                {
                    pane = (JTextPane) aComponent;
                }
                if (aComponent instanceof TextColorButton)
                {
                    button = (TextColorButton) aComponent;
                }
                else if (aComponent instanceof javax.swing.JButton && ((javax.swing.JButton) aComponent).getText().matches("\\d+"))
                {
                    size = (javax.swing.JButton) aComponent;
                }
            }
            this.textPane = pane;
            this.colorButton = button;
            this.sizeButton = size;
        }

        void pressEnterAt(int position)
        {
            textPane.setCaretPosition(position);
            textPane.getActionMap().get(DefaultEditorKit.insertBreakAction).actionPerformed(new ActionEvent(textPane, 0, ""));
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

    private static String hex(Color color)
    {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
