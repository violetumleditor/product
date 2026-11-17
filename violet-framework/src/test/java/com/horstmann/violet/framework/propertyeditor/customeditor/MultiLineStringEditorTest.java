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

    @Test
    void underlinesTheSelectionOnly()
    {
        MultiLineString text = plainText("hello world");
        EditorFixture editor = new EditorFixture(text);

        editor.textPane.select(1, 6);
        editor.underlineButton.doClick(0);

        assertFalse(text.isUnderlined());
        assertTrue(text.getText().contains("<u>hello</u>"), text.getText());
    }

    @Test
    void removesTheUnderlineOfTheSelectionOnlyInATextUnderlinedAsAWhole()
    {
        MultiLineString text = new MultiLineString();
        text.setUnderlined(true);
        text.setText("hello world");
        EditorFixture editor = new EditorFixture(text);

        editor.textPane.select(1, 6);
        editor.underlineButton.doClick(0);

        assertFalse(text.isUnderlined());
        assertFalse(text.getText().contains("<u>hello"), text.getText());
        assertTrue(text.getText().replaceAll("\s+", " ").contains("<u> world</u>"), text.getText());
    }

    @Test
    void textEditedInTheEditorKeepsItsSizeAndUnderlineAfterAReload()
    {
        MultiLineString text = new MultiLineString();
        text.setSize(MultiLineString.FontSize.LARGE);
        text.setUnderlined(true);
        text.setText("hello world");
        EditorFixture editor = new EditorFixture(text);

        editor.textPane.setCaretPosition(3);
        editor.textPane.replaceSelection("X");
        editor.pressEnterAt(5);

        MultiLineString reloaded = plainText(text.getText());
        assertEquals(MultiLineString.FontSize.LARGE, reloaded.getSize(), text.getText());
        assertTrue(reloaded.isUnderlined(), text.getText());
        assertEquals(text.getBounds(), reloaded.getBounds());

        // And still after another edition of the reloaded text
        new EditorFixture(reloaded).textPane.replaceSelection("Y");
        MultiLineString reloadedAgain = plainText(reloaded.getText());
        assertEquals(MultiLineString.FontSize.LARGE, reloadedAgain.getSize(), reloaded.getText());
        assertTrue(reloadedAgain.isUnderlined(), reloaded.getText());
    }

    @Test
    void underlineRemovedFromASelectionStaysRemovedAfterAReload()
    {
        MultiLineString text = new MultiLineString();
        text.setUnderlined(true);
        text.setText("hello world");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 6);
        editor.underlineButton.doClick(0);

        MultiLineString reloaded = plainText(text.getText());

        assertFalse(reloaded.isUnderlined(), text.getText());
        assertFalse(reloaded.getText().contains("<u>hello"), reloaded.getText());
        assertTrue(reloaded.getText().contains("<u>"), reloaded.getText());

        // The editor of the reloaded text doesn't bring the underline back
        new EditorFixture(reloaded).textPane.replaceSelection("Y");
        assertFalse(plainText(reloaded.getText()).isUnderlined(), reloaded.getText());
    }

    @Test
    void alignmentButtonsAlignTheTextAndItsSavedCopy()
    {
        for (String value : new String[] { "hello", "hello\nworld", "" })
        {
            MultiLineString text = new MultiLineString();
            text.setJustification(MultiLineString.Justification.LEFT);
            text.setText(value);
            EditorFixture editor = new EditorFixture(text);
            assertEquals(MultiLineString.Justification.LEFT, text.getJustification(), text.getText());

            // Left, center, right: the toggle buttons after bold, italic and underline
            for (int justification : new int[] { MultiLineString.Justification.RIGHT, MultiLineString.Justification.CENTER,
                    MultiLineString.Justification.LEFT, MultiLineString.Justification.RIGHT })
            {
                editor.toggles.get(3 + justification).doClick(0);
                assertEquals(justification, text.getJustification(), text.getText());
                assertEquals(justification, plainText(text.getText()).getJustification(), text.getText());

                editor.textPane.setCaretPosition(editor.textPane.getDocument().getLength());
                editor.textPane.replaceSelection("x");
                assertEquals(justification, text.getJustification(), text.getText());
                editor.pressEnterAt(1);
                assertEquals(justification, text.getJustification(), text.getText());
                assertEquals(justification, plainText(text.getText()).getJustification(), text.getText());
            }

            // As after a reload: a new string, given the saved text
            MultiLineString reloaded = plainText(text.getText());
            EditorFixture reopened = new EditorFixture(reloaded);
            assertTrue(reopened.toggles.get(5).isSelected());
            reopened.textPane.replaceSelection("y");
            assertEquals(MultiLineString.Justification.RIGHT, reloaded.getJustification(), reloaded.getText());
            reopened.toggles.get(3).doClick(0);
            assertEquals(MultiLineString.Justification.LEFT, reloaded.getJustification(), reloaded.getText());
            reopened.textPane.replaceSelection("z");
            assertEquals(MultiLineString.Justification.LEFT, reloaded.getJustification(), reloaded.getText());
            assertEquals(MultiLineString.Justification.LEFT, plainText(reloaded.getText()).getJustification(),
                    reloaded.getText());
        }
    }

    @Test
    void textAreaShowsTheTextAsTheDiagramDrawsIt() throws Exception
    {
        for (int size : new int[] { MultiLineString.FontSize.NORMAL, MultiLineString.FontSize.LARGE })
        {
            MultiLineString text = new MultiLineString();
            text.setSize(size);
            text.setText("Hello\nworld");
            EditorFixture editor = new EditorFixture(text);
            // A line break, then a paragraph
            editor.textPane.setCaretPosition(editor.textPane.getDocument().getLength());
            editor.pressEnterAt(editor.textPane.getDocument().getLength());
            editor.textPane.replaceSelection("Hello");
            editor.textPane.setSize(300, 200);
            editor.textPane.doLayout();

            String shown = editor.textPane.getDocument().getText(0, editor.textPane.getDocument().getLength());
            int firstLine = shown.indexOf("Hello");
            int secondLine = shown.indexOf("world");
            int thirdLine = shown.lastIndexOf("Hello");
            double lineHeight = plainText(size, "a").getBounds().getHeight();
            Rectangle2D first = editor.textPane.modelToView2D(firstLine);
            assertEquals(lineHeight, editor.textPane.modelToView2D(secondLine).getY() - first.getY(), text.getText());
            assertEquals(2 * lineHeight, editor.textPane.modelToView2D(thirdLine).getY() - first.getY(), text.getText());

            // Same font and size: the same width, give or take the rounding of each character
            double shownWidth = editor.textPane.modelToView2D(firstLine + 5).getX() - first.getX();
            assertEquals(plainText(size, "Hello").getBounds().getWidth(), shownWidth, 2, text.getText());

            // The typed text takes the default size of the string, the diagram draws all its lines alike
            assertFalse(text.getText().contains("<font size"), text.getText());
            assertEquals(plainText(size, "Hello\nworld\nHello").getBounds(), text.getBounds(), text.getText());
        }
    }

    @Test
    void charactersAreDrawnAsOnTheDiagramOnAScaledScreen() throws Exception
    {
        MultiLineString text = new MultiLineString();
        text.setJustification(MultiLineString.Justification.LEFT);
        text.setText("fghfhgfghfgh");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.setCaretColor(Color.WHITE);
        editor.textPane.setMargin(new java.awt.Insets(0, 0, 0, 0));
        editor.textPane.setSize(200, 40);
        editor.textPane.doLayout();

        // As on a screen scaled to 125 %, where Swing would draw the characters hinted, a few pixels narrower
        double scale = 1.25;
        BufferedImage shown = new BufferedImage(250, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = shown.createGraphics();
        g2.scale(scale, scale);
        editor.textPane.paint(g2);
        g2.dispose();
        Rectangle2D bounds = text.getBounds();
        BufferedImage drawn = new BufferedImage(250, 50, BufferedImage.TYPE_INT_RGB);
        g2 = drawn.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 250, 50);
        g2.scale(scale, scale);
        g2.setColor(ColorChoice.DEFAULT_TEXT_COLOR);
        text.draw(g2, bounds);
        g2.dispose();

        java.awt.Rectangle shownInk = getInk(shown);
        java.awt.Rectangle drawnInk = getInk(drawn);
        assertEquals(drawnInk.width, shownInk.width, 1);
        assertEquals(drawnInk.height, shownInk.height, 1);
    }

    /**
     * @return the bounds of the dark pixels of an image
     */
    private static java.awt.Rectangle getInk(BufferedImage image)
    {
        java.awt.Rectangle ink = null;
        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                Color color = new Color(image.getRGB(x, y));
                if (color.getRed() + color.getGreen() + color.getBlue() < 3 * 160)
                {
                    if (ink == null)
                    {
                        ink = new java.awt.Rectangle(x, y, 1, 1);
                    }
                    else
                    {
                        ink.add(new java.awt.Rectangle(x, y, 1, 1));
                    }
                }
            }
        }
        return ink;
    }

    @Test
    void styleSheetSavedByAnOlderEditorDoesNotChangeTheShownSize() throws Exception
    {
        String saved = "<html>\n  <head>\n    <style type=\"text/css\">\n      <!--\n"
                + "        body { font-family: SansSerif; font-size: 15px; color: #5b5b5b }\n      -->\n    </style>\n  </head>\n"
                + "  <body>\n    <p align=\"left\" style=\"margin-top: 0\">\n      Hello<br>world\n    </p>\n  </body>\n</html>\n";
        EditorFixture editor = new EditorFixture(plainText(saved));
        editor.textPane.setSize(300, 200);
        editor.textPane.doLayout();

        String shown = editor.textPane.getDocument().getText(0, editor.textPane.getDocument().getLength());
        double lineHeight = plainText("a").getBounds().getHeight();
        assertEquals(lineHeight, editor.textPane.modelToView2D(shown.indexOf("world")).getY()
                - editor.textPane.modelToView2D(shown.indexOf("Hello")).getY());
        double shownWidth = editor.textPane.modelToView2D(shown.indexOf("Hello") + 5).getX()
                - editor.textPane.modelToView2D(shown.indexOf("Hello")).getX();
        assertEquals(plainText("Hello").getBounds().getWidth(), shownWidth, 2);
    }

    private static MultiLineString plainText(int size, String value)
    {
        MultiLineString text = new MultiLineString();
        text.setSize(size);
        text.setText(value);
        return text;
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
        final javax.swing.JToggleButton underlineButton;
        final List<javax.swing.JToggleButton> toggles;

        EditorFixture(MultiLineString value)
        {
            MultiLineStringEditor editor = new MultiLineStringEditor();
            editor.setValue(value);
            List<Component> components = new ArrayList<Component>();
            collect((Container) editor.getCustomEditor(), components);
            JTextPane pane = null;
            TextColorButton button = null;
            javax.swing.JButton size = null;
            List<javax.swing.JToggleButton> toggles = new ArrayList<javax.swing.JToggleButton>();
            for (Component aComponent : components)
            {
                if (aComponent instanceof JTextPane)
                {
                    pane = (JTextPane) aComponent;
                }
                if (aComponent instanceof javax.swing.JToggleButton)
                {
                    toggles.add((javax.swing.JToggleButton) aComponent);
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
            // Bold, italic, underline, then the alignments
            this.underlineButton = toggles.get(2);
            this.toggles = toggles;
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
