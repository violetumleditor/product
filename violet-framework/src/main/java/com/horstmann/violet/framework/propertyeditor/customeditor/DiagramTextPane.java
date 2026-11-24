package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JTextPane;

import com.formdev.flatlaf.FlatLaf;
import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;

/**
 * A text pane showing a text of a diagram. Its document holds the real colors of the diagram, as on a white page. A dark theme
 * adapts them when painting, like on the diagram canvas, so the colors of this pane are given as on this white page too.
 * <p>
 * Inside a {@link ZoomedTextBox}, which paints it at the zoom of the diagram, it lets this box handle its mouse events and
 * repaint it.
 */
public class DiagramTextPane extends JTextPane {

    public DiagramTextPane() {
        setBackground(Color.WHITE);
        setCaretColor(Color.BLACK);
        if (FlatLaf.isLafDark()) {
            setSelectionColor(DARK_THEME_SELECTION_COLOR);
            setSelectedTextColor(Color.BLACK);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(FlatLaf.isLafDark() ? new DarkDiagramGraphics2D((Graphics2D) g) : g);
    }

    @Override
    public void repaint(long delay, int x, int y, int width, int height) {
        ZoomedTextBox box = getZoomedBox();
        if (box != null) {
            box.repaint();
            return;
        }
        super.repaint(delay, x, y, width, height);
    }

    @Override
    public boolean contains(int x, int y) {
        // Its bounds are not the ones it is painted in
        return getZoomedBox() == null && super.contains(x, y);
    }

    @Override
    public void scrollRectToVisible(Rectangle rectangle) {
        ZoomedTextBox box = getZoomedBox();
        if (box != null) {
            box.scrollRectToVisible(box.toBox(rectangle));
            return;
        }
        super.scrollRectToVisible(rectangle);
    }

    private ZoomedTextBox getZoomedBox() {
        return getParent() instanceof ZoomedTextBox ? (ZoomedTextBox) getParent() : null;
    }

    private static final Color DARK_THEME_SELECTION_COLOR = new Color(190, 170, 240);
}
