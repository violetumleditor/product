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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTextPane;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;

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

        assertTrue(text.getText().contains("<font color=\"#8493ca\">X</font>"), text.getText());
    }

    @Test
    void coloredTextIsDrawnInTheDiagram()
    {
        MultiLineString text = plainText("HHHHHH");
        EditorFixture editor = new EditorFixture(text);
        editor.textPane.select(1, 7);
        editor.colorBox.setSelectedItem(RED);

        Rectangle2D bounds = text.getBounds();
        BufferedImage image = new BufferedImage((int) bounds.getWidth(), (int) bounds.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        text.draw(g2, bounds);
        g2.dispose();

        assertTrue(containsPixel(image, RED));
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
