package com.horstmann.violet.product.diagram.abstracts.property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.FlatLightLaf;
import com.horstmann.violet.product.diagram.abstracts.property.text.HtmlTextParser;
import com.horstmann.violet.product.diagram.abstracts.property.text.TextRun;
import com.horstmann.violet.product.diagram.abstracts.property.text.TextStyle;

class MultiLineStringRenderingTest
{
    /** "Hello world" as the text pane of the editor writes it: indented, one paragraph per line */
    private static final String EDITOR_HTML = "<html>\n  <head>\n\n  </head>\n  <body>\n"
            + "    <p style=\"margin-top: 0\" align=\"center\">\n      Hello world\n    </p>\n  </body>\n</html>\n";

    @Test
    void textTakesTheSameRoomOnEveryComputer()
    {
        // The font is bundled and the lines are laid out by Violet: these numbers don't depend on the system
        assertEquals(new Rectangle2D.Double(0, 0, 61, 16), text("Hello world").getBounds());
        assertEquals(new Rectangle2D.Double(0, 0, 81, 22), text("Hello world", MultiLineString.FontSize.LARGE).getBounds());
    }

    @Test
    void lookAndFeelDoesNotChangeTheRoomTaken() throws Exception
    {
        Rectangle2D bounds = text("Hello world").getBounds();
        LookAndFeel previousLookAndFeel = UIManager.getLookAndFeel();
        UIManager.setLookAndFeel(new FlatLightLaf());
        try
        {
            assertEquals(bounds, text("Hello world").getBounds());
        }
        finally
        {
            UIManager.setLookAndFeel(previousLookAndFeel);
        }
    }

    @Test
    void textFromTheEditorTakesTheSameRoomAsTypedText()
    {
        assertEquals(text("Hello world").getBounds(), text(EDITOR_HTML).getBounds());
    }

    @Test
    void alignmentComesBackWithTheSavedText()
    {
        MultiLineString typed = new MultiLineString();
        typed.setJustification(MultiLineString.Justification.RIGHT);
        typed.setText("Hello world");

        // A new string, given the saved text
        assertEquals(MultiLineString.Justification.RIGHT, text(typed.getText()).getJustification());
        assertEquals(MultiLineString.Justification.LEFT, text(EDITOR_HTML.replace("center", "left")).getJustification());
        assertEquals(MultiLineString.Justification.CENTER, text("<html>Hello</html>").getJustification());

        // Changed once the text is there
        typed.setJustification(MultiLineString.Justification.LEFT);
        assertEquals(MultiLineString.Justification.LEFT, text(typed.getText()).getJustification());
        MultiLineString edited = text(EDITOR_HTML);
        edited.setJustification(MultiLineString.Justification.RIGHT);
        assertEquals(MultiLineString.Justification.RIGHT, text(edited.getText()).getJustification());
    }

    @Test
    void sizeAndUnderlineComeBackWithTheSavedText()
    {
        for (String value : new String[] { "Hello world", EDITOR_HTML })
        {
            MultiLineString saved = new MultiLineString();
            saved.setSize(MultiLineString.FontSize.LARGE);
            saved.setUnderlined(true);
            saved.setText(value);

            // A new string, given the saved text
            MultiLineString reloaded = text(saved.getText());
            assertEquals(MultiLineString.FontSize.LARGE, reloaded.getSize(), saved.getText());
            assertTrue(reloaded.isUnderlined(), saved.getText());
            assertEquals(saved.getBounds(), reloaded.getBounds());
            assertTrue(sameImage(draw(saved), draw(reloaded)));
            assertEquals(saved.getText(), reloaded.getText());

            // Changed once the text is there
            saved.setUnderlined(false);
            saved.setSize(MultiLineString.FontSize.SMALL);
            reloaded = text(saved.getText());
            assertEquals(MultiLineString.FontSize.SMALL, reloaded.getSize(), saved.getText());
            assertFalse(reloaded.isUnderlined(), saved.getText());
        }
    }

    @Test
    void styleComesBackWhenThePersistenceSetsTheTextField() throws Exception
    {
        MultiLineString saved = new MultiLineString();
        saved.setSize(MultiLineString.FontSize.LARGE);
        saved.setUnderlined(true);
        saved.setJustification(MultiLineString.Justification.RIGHT);
        saved.setText("Hello\nworld");

        // The persistence creates a new string and sets its field, without any setter
        MultiLineString reloaded = new MultiLineString();
        java.lang.reflect.Field field = MultiLineString.class.getDeclaredField("text");
        field.setAccessible(true);
        field.set(reloaded, saved.getText());

        assertEquals(saved.getBounds(), reloaded.getBounds());
        assertTrue(sameImage(draw(saved), draw(reloaded)));
        assertEquals(MultiLineString.FontSize.LARGE, reloaded.getSize());
        assertEquals(MultiLineString.Justification.RIGHT, reloaded.getJustification());
        assertTrue(reloaded.isUnderlined());

        // A text of an old file is not HTML
        MultiLineString old = new MultiLineString();
        field.set(old, "Hello\nworld");
        assertEquals(text("Hello\nworld").getBounds(), old.getBounds());
    }

    @Test
    void textSavedWithoutStyleKeepsTheDefaultOne()
    {
        MultiLineString reloaded = text("<html><body><p>Hello</p></body></html>");

        assertEquals(MultiLineString.FontSize.NORMAL, reloaded.getSize());
        assertEquals(MultiLineString.Justification.CENTER, reloaded.getJustification());
        assertFalse(reloaded.isUnderlined());
    }

    @Test
    void emptyTextTakesNoRoom()
    {
        assertTrue(new MultiLineString().getBounds().isEmpty());
        assertTrue(text("").getBounds().isEmpty());
    }

    @Test
    void eachLineBreakAddsALine()
    {
        double lineHeight = text("a").getBounds().getHeight();

        assertEquals(2 * lineHeight, text("a\nb").getBounds().getHeight());
        assertEquals(3 * lineHeight, text("a\n\nb").getBounds().getHeight());
        assertEquals(3 * lineHeight, text("<html><body><p>a</p><p>\n  </p><p>b</p></body></html>").getBounds().getHeight());
    }

    @Test
    void typedSpacesAreKept()
    {
        assertTrue(text("a   b").getBounds().getWidth() > text("a b").getBounds().getWidth());
        assertTrue(text("  a").getBounds().getWidth() > text("a").getBounds().getWidth());
    }

    @Test
    void indentationAfterATagDoesNotWidenTheLine()
    {
        // As the text pane writes a line ended by Enter in a text whose typed characters have a size
        String html = "<html>\n  <body>\n    <p style=\"margin-top: 0\">\n      Hello<br>world<font size=\"3\">\n</font>    </p>\n"
                + "    <p style=\"margin-top: 0\">\n      <font size=\"3\">Hello</font>\n    </p>\n  </body>\n</html>\n";

        assertEquals(text("Hello\nworld\nHello").getBounds(), text(html).getBounds());
    }

    @Test
    void readsTheStylesTheEditorWrites()
    {
        List<List<TextRun>> lines = new HtmlTextParser().parse("<html><body><p>a <b>b</b><i>c</i><u>d</u>"
                + "<font color=\"#ff0000\" size=\"6\">e</font>&lt;&#233;&amp;&nbsp;</p></body></html>");

        List<TextRun> runs = lines.get(0);
        assertEquals(1, lines.size());
        assertEquals(new TextRun("a ", TextStyle.PLAIN), runs.get(0));
        assertEquals(new TextRun("b", TextStyle.PLAIN.withBold(true)), runs.get(1));
        assertEquals(new TextRun("c", TextStyle.PLAIN.withItalic(true)), runs.get(2));
        assertEquals(new TextRun("d", TextStyle.PLAIN.withUnderlined(true)), runs.get(3));
        assertEquals(new TextRun("e", TextStyle.PLAIN.withColor(Color.RED).withSize(24)), runs.get(4));
        assertEquals(new TextRun("<é& ", TextStyle.PLAIN), runs.get(5));
    }

    @Test
    void stylesChangeTheDrawing()
    {
        assertTrue(text("<html><b>Hello world</b></html>").getBounds().getWidth() > text("Hello world").getBounds().getWidth());
        assertTrue(text("<html><font size=\"6\">Hello</font></html>").getBounds().getHeight() > text("Hello").getBounds()
                .getHeight());

        MultiLineString underlined = text("Hello");
        underlined.setUnderlined(true);
        assertFalse(sameImage(draw(text("Hello")), draw(underlined)));
        assertTrue(sameImage(draw(underlined), draw(text("<html><u>Hello</u></html>"))));
    }

    @Test
    void drawingLeavesTheGraphicsAsItWas()
    {
        BufferedImage image = new BufferedImage(100, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.BLUE);
        Object antialiasing = g2.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING);

        text("<html><font color=\"#ff0000\">Hello</font></html>").draw(g2, new Rectangle2D.Double(10.5, 3.25, 80, 20));

        assertEquals(Color.BLUE, g2.getColor());
        assertEquals(antialiasing, g2.getRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING));
        assertTrue(g2.getTransform().isIdentity());
        g2.dispose();
    }

    private MultiLineString text(String value)
    {
        return text(value, MultiLineString.FontSize.NORMAL);
    }

    private MultiLineString text(String value, int size)
    {
        MultiLineString text = new MultiLineString();
        text.setSize(size);
        text.setText(value);
        return text;
    }

    private BufferedImage draw(MultiLineString text)
    {
        BufferedImage image = new BufferedImage(100, 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        text.draw(g2, new Rectangle2D.Double(0, 0, 100, 30));
        g2.dispose();
        return image;
    }

    private boolean sameImage(BufferedImage first, BufferedImage second)
    {
        for (int x = 0; x < first.getWidth(); x++)
        {
            for (int y = 0; y < first.getHeight(); y++)
            {
                if (first.getRGB(x, y) != second.getRGB(x, y))
                {
                    return false;
                }
            }
        }
        return true;
    }
}
