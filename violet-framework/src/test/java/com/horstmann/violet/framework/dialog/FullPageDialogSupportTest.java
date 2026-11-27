package com.horstmann.violet.framework.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

/**
 * The option pane of a dialog shown as a page: buttons at the bottom of the page, message above them, scrolled when too tall
 */
class FullPageDialogSupportTest
{

    private static final int PAGE_WIDTH = 360;
    private static final int PAGE_HEIGHT = 400;

    @Test
    void shouldKeepTheButtonsAtTheBottomOfThePage()
    {
        JOptionPane optionPane = createOptionPane(new Dimension(200, 50));

        Component buttonArea = findByName(optionPane, "OptionPane.buttonArea");
        JScrollPane scrollPane = findScrollPane(optionPane);
        assertNotNull(buttonArea);
        assertNotNull(scrollPane);
        int bottom = PAGE_HEIGHT - optionPane.getInsets().bottom;
        assertEquals(bottom, buttonArea.getY() + buttonArea.getHeight(), "buttons at the bottom of the page");
        assertTrue(scrollPane.getY() + scrollPane.getHeight() <= buttonArea.getY(), "message above the buttons");
    }

    @Test
    void shouldStretchAShortMessageOverThePage()
    {
        JOptionPane optionPane = createOptionPane(new Dimension(200, 50));

        JViewport viewport = findScrollPane(optionPane).getViewport();
        assertEquals(viewport.getExtentSize(), viewport.getView().getSize(), "the message fills the page, nothing to scroll");
    }

    @Test
    void shouldScrollATallMessage()
    {
        JOptionPane optionPane = createOptionPane(new Dimension(200, 2000));

        JViewport viewport = findScrollPane(optionPane).getViewport();
        assertTrue(viewport.getView().getHeight() > viewport.getExtentSize().height, "the message scrolls");
        assertEquals(viewport.getExtentSize().width, viewport.getView().getWidth(), "never wider than the page");
        Component buttonArea = findByName(optionPane, "OptionPane.buttonArea");
        Rectangle page = new Rectangle(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        assertTrue(page.contains(buttonArea.getBounds()), "buttons still on the page");
    }

    @Test
    void shouldFloatCenteredOnALargeWindow()
    {
        FullPageDialogSupport support = new FullPageDialogSupport(null, null);
        Rectangle owner = new Rectangle(0, 0, 1200, 800);
        Dimension size = new Dimension(400, 200);

        assertFalse(support.isPage(owner, size));
        assertEquals(new Rectangle(400, 300, 400, 200), support.computeBounds(owner, size, size, false));
    }

    @Test
    void shouldKeepTheHeightOfAShortDialogOnAPhone()
    {
        FullPageDialogSupport support = new FullPageDialogSupport(null, null);
        Rectangle phone = new Rectangle(0, 0, 390, 800);
        Dimension question = new Dimension(300, 160);

        assertTrue(support.isPage(phone, question));
        assertEquals(new Rectangle(0, 320, 390, 160), support.computeBounds(phone, question, question, true),
                "whole width, own height, centered");
    }

    @Test
    void shouldTakeTheWholeWindowForATallDialog()
    {
        FullPageDialogSupport support = new FullPageDialogSupport(null, null);
        Rectangle phone = new Rectangle(0, 0, 390, 800);
        Dimension list = new Dimension(360, 450);
        Rectangle lying = new Rectangle(0, 0, 800, 390);

        assertEquals(phone, support.computeBounds(phone, list, list, true));
        assertTrue(support.isPage(lying, list), "a phone lying on its side");
        assertEquals(lying, support.computeBounds(lying, list, list, true));
    }

    @Test
    void shouldBecomeAPageWhenTheWindowGetsSmaller()
    {
        FullPageDialogSupport support = new FullPageDialogSupport(null, null);
        Dimension size = new Dimension(700, 500);

        assertFalse(support.isPage(new Rectangle(0, 0, 1200, 800), size));
        assertTrue(support.isPage(new Rectangle(0, 0, 650, 800), size), "the dialog no longer fits");
    }

    private JOptionPane createOptionPane(Dimension messageSize)
    {
        JPanel message = new JPanel();
        message.setPreferredSize(messageSize);
        message.setMinimumSize(new Dimension(100, 20));
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        new FullPageDialogSupport(null, null).layOutAsPage(optionPane);
        optionPane.setSize(PAGE_WIDTH, PAGE_HEIGHT);
        layOut(optionPane);
        return optionPane;
    }

    private void layOut(Container container)
    {
        container.doLayout();
        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                layOut((Container) child);
            }
        }
    }

    private Component findByName(Container container, String name)
    {
        for (Component child : container.getComponents())
        {
            if (name.equals(child.getName()))
            {
                return child;
            }
        }
        return null;
    }

    private JScrollPane findScrollPane(JOptionPane optionPane)
    {
        for (Component child : optionPane.getComponents())
        {
            if (child instanceof JScrollPane)
            {
                return (JScrollPane) child;
            }
        }
        return (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, optionPane);
    }

}
