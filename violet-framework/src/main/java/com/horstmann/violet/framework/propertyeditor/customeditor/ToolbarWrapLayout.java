package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * Lays a toolbar out from left to right. Its groups of buttons, between two separators, stay whole: one that doesn't fit in the
 * width starts a new line, without its separator. A group wider than the whole toolbar is cut between two buttons.
 */
class ToolbarWrapLayout implements LayoutManager {

    /**
     * A line between two groups
     */
    abstract static class Separator extends JComponent {
    }

    /**
     * @param newValue the preferred width of the toolbar doesn't go past it, insets included
     */
    void setMaxWidth(int newValue) {
        this.maxWidth = newValue;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
        // Nothing to remember
    }

    @Override
    public void removeLayoutComponent(Component component) {
        // Nothing to forget
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return layout(parent, this.maxWidth, false);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        layout(parent, parent.getWidth(), true);
    }

    /**
     * @param parent the toolbar
     * @param width available, insets included
     * @param isApplied true to move the components, false to compute the size only
     * @return the size taken, insets included
     */
    private Dimension layout(Container parent, int width, boolean isApplied) {
        Insets insets = parent.getInsets();
        int maxX = width - insets.right;
        int x = insets.left;
        int y = insets.top;
        int lineHeight = 0;
        int usedWidth = 0;
        for (List<Component> group : getGroups(parent)) {
            boolean isNewLine = x > insets.left && x + getWidth(group) > maxX;
            if (isNewLine) {
                x = insets.left;
                y += lineHeight;
                lineHeight = 0;
            }
            for (Component component : group) {
                boolean isSkipped = isNewLine && component instanceof Separator;
                Dimension size = isSkipped ? new Dimension() : component.getPreferredSize();
                if (x > insets.left && x + size.width > maxX) {
                    x = insets.left;
                    y += lineHeight;
                    lineHeight = 0;
                }
                if (isApplied) {
                    component.setBounds(x, y, size.width, size.height);
                }
                x += size.width;
                usedWidth = Math.max(usedWidth, x);
                lineHeight = Math.max(lineHeight, size.height);
            }
        }
        return new Dimension(usedWidth + insets.right, y + lineHeight + insets.bottom);
    }

    /**
     * @param parent the toolbar
     * @return its groups, each one starting with its separator, except the first one
     */
    private List<List<Component>> getGroups(Container parent) {
        List<List<Component>> groups = new ArrayList<List<Component>>();
        for (Component component : parent.getComponents()) {
            if (groups.isEmpty() || component instanceof Separator) {
                groups.add(new ArrayList<Component>());
            }
            groups.get(groups.size() - 1).add(component);
        }
        return groups;
    }

    private int getWidth(List<Component> group) {
        int width = 0;
        for (Component component : group) {
            width += component.getPreferredSize().width;
        }
        return width;
    }

    private int maxWidth = Integer.MAX_VALUE;
}
