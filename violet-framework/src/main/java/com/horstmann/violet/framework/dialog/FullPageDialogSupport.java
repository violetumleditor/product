package com.horstmann.violet.framework.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.RootPaneContainer;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

/**
 * Places a dialog over its owner window, and places it again each time the window changes size or moves while the dialog is
 * shown (page resized, screen rotated, keyboard opened when the page lets it shrink the display): everything is responsive.
 * <p>
 * On a large window where it fits, the dialog floats at its own size, centered. On a small window (a phone, upright or lying, a
 * narrow desktop window) or when the dialog doesn't fit, it becomes a page, as a floating dialog would hide its buttons off screen
 * or under the keyboard: a tall dialog takes the whole window; a short one (a message, a question) takes the whole width but
 * keeps its own height, centered, as a whole page for two lines is disturbing.
 * <p>
 * In a {@link JOptionPane}, the buttons stay at the bottom and the message fills the rest, scrolling when it is too tall. Other
 * dialogs only get their bounds: their own layout decides.
 * <p>
 * Only the size of the owner decides, never the kind of device, like the rest of the responsive layout.
 */
public class FullPageDialogSupport
{

    /**
     * Client property of the dialog's root pane, holding this object
     */
    public static final String CLIENT_PROPERTY = FullPageDialogSupport.class.getName();

    /**
     * @param dialog a packed dialog, not shown yet
     * @param owner the window the dialog belongs to, may be null
     */
    public FullPageDialogSupport(JDialog dialog, Window owner)
    {
        this.dialog = dialog;
        this.owner = owner;
    }

    /**
     * @param window a window
     * @return the object placing this window if it is a dialog placed by it, else null
     */
    public static FullPageDialogSupport get(Window window)
    {
        if (window instanceof RootPaneContainer)
        {
            Object support = ((RootPaneContainer) window).getRootPane().getClientProperty(CLIENT_PROPERTY);
            if (support instanceof FullPageDialogSupport)
            {
                return (FullPageDialogSupport) support;
            }
        }
        return null;
    }

    /**
     * Places the dialog before it is shown, then each time its owner changes size or moves
     *
     * @return true if the dialog is placed; false without a showing owner: the caller places it
     */
    public boolean install()
    {
        if (this.owner == null || !this.owner.isShowing())
        {
            return false;
        }
        this.dialog.getRootPane().putClientProperty(CLIENT_PROPERTY, this);
        JOptionPane optionPane = findOptionPane();
        if (optionPane != null)
        {
            layOutAsPage(optionPane);
        }
        fit();
        this.owner.addComponentListener(getOwnerListener());
        this.dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent e)
            {
                owner.removeComponentListener(getOwnerListener());
            }
        });
        return true;
    }

    /**
     * @return true if the dialog is shown as a page: it must not change its own size, but call {@link #fit()} when its content
     *         grows
     */
    public boolean isPage()
    {
        return this.isPage;
    }

    /**
     * Places the dialog again according to the size of its owner and its own preferred size
     */
    public void fit()
    {
        Dimension preferredSize = this.dialog.getPreferredSize();
        Rectangle ownerBounds = this.owner.getBounds();
        boolean wasPage = this.isPage;
        this.isPage = isPage(ownerBounds, preferredSize);
        // Back from a page, the dialog takes its own size again
        Dimension size = wasPage ? preferredSize : this.dialog.getSize();
        Rectangle bounds = computeBounds(ownerBounds, preferredSize, size, this.isPage);
        if (!bounds.equals(this.dialog.getBounds()))
        {
            this.dialog.setBounds(bounds);
            this.dialog.validate();
        }
    }

    /**
     * @return true if a dialog of this preferred size must be a page over this owner
     */
    boolean isPage(Rectangle ownerBounds, Dimension preferredSize)
    {
        return ownerBounds.width < MAX_PAGE_WIDTH || ownerBounds.height < MAX_PAGE_HEIGHT
                || preferredSize.width > ownerBounds.width || preferredSize.height > ownerBounds.height;
    }

    /**
     * Package visible for tests
     *
     * @param ownerBounds bounds of the owner window
     * @param preferredSize preferred size of the dialog
     * @param size current size of the dialog, kept when it floats
     * @param isPage true if the dialog is a page, see {@link #isPage(Rectangle, Dimension)}
     * @return the bounds of the dialog
     */
    Rectangle computeBounds(Rectangle ownerBounds, Dimension preferredSize, Dimension size, boolean isPage)
    {
        if (!isPage)
        {
            return centerIn(ownerBounds, Math.min(size.width, ownerBounds.width), Math.min(size.height, ownerBounds.height));
        }
        if (preferredSize.height > ownerBounds.height * TALL_DIALOG_RATIO)
        {
            return new Rectangle(ownerBounds);
        }
        return centerIn(ownerBounds, ownerBounds.width, preferredSize.height);
    }

    private Rectangle centerIn(Rectangle ownerBounds, int width, int height)
    {
        int x = ownerBounds.x + (ownerBounds.width - width) / 2;
        int y = ownerBounds.y + (ownerBounds.height - height) / 2;
        return new Rectangle(x, y, width, height);
    }

    private ComponentListener getOwnerListener()
    {
        if (this.ownerListener == null)
        {
            this.ownerListener = new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    fit();
                }

                @Override
                public void componentMoved(ComponentEvent e)
                {
                    fit();
                }
            };
        }
        return this.ownerListener;
    }

    /**
     * @return the option pane that makes the whole content of the dialog, as built by {@link DialogFactory}, or null
     */
    private JOptionPane findOptionPane()
    {
        Container contentPane = this.dialog.getContentPane();
        if (contentPane.getComponentCount() == 1 && contentPane.getComponent(0) instanceof JOptionPane)
        {
            return (JOptionPane) contentPane.getComponent(0);
        }
        return null;
    }

    /**
     * Puts the buttons of the option pane at the bottom and its message in a scroll pane filling the rest. The button area is
     * found by the name the Swing option pane UIs give it, the message area is the other child; with another UI, the option pane
     * keeps its layout. Package visible for tests.
     */
    void layOutAsPage(JOptionPane optionPane)
    {
        Component[] children = optionPane.getComponents();
        if (children.length != 2 || !BUTTON_AREA_NAME.equals(children[1].getName()))
        {
            return;
        }
        Component messageArea = children[0];
        optionPane.remove(messageArea);
        optionPane.setLayout(new PageLayout());
        optionPane.add(createScrollPane(messageArea), 0);
    }

    private JScrollPane createScrollPane(Component content)
    {
        JScrollPane scrollPane = new JScrollPane(new PageContent(content));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(SCROLL_UNIT);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(SCROLL_UNIT);
        return scrollPane;
    }

    /**
     * Content of the page's scroll pane: as wide as the page, and as tall as the page while it fits in it (so a table in the
     * message grows with the page), else scrolled
     */
    private static class PageContent extends JPanel implements Scrollable
    {

        PageContent(Component content)
        {
            super(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return SCROLL_UNIT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            int visible = orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
            return Math.max(SCROLL_UNIT, visible - SCROLL_UNIT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return !(getParent() instanceof JViewport) || getParent().getWidth() >= getMinimumSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return !(getParent() instanceof JViewport) || getParent().getHeight() >= getPreferredSize().height;
        }

    }

    /**
     * Layout of the option pane as a page: the button area at the bottom at its preferred height, the rest above it. Works by the
     * names of the areas, so it stays right if the option pane rebuilds them.
     */
    private static class PageLayout implements LayoutManager
    {

        @Override
        public void addLayoutComponent(String name, Component component)
        {
            // Placed by name
        }

        @Override
        public void removeLayoutComponent(Component component)
        {
            // Placed by name
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            return getSize(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return getSize(parent, false);
        }

        private Dimension getSize(Container parent, boolean isPreferred)
        {
            int width = 0;
            int height = 0;
            for (Component child : parent.getComponents())
            {
                Dimension size = isPreferred ? child.getPreferredSize() : child.getMinimumSize();
                width = Math.max(width, size.width);
                height += size.height;
            }
            Insets insets = parent.getInsets();
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            Insets insets = parent.getInsets();
            int x = insets.left;
            int top = insets.top;
            int width = Math.max(0, parent.getWidth() - insets.left - insets.right);
            int bottom = parent.getHeight() - insets.bottom;
            for (Component child : parent.getComponents())
            {
                if (BUTTON_AREA_NAME.equals(child.getName()))
                {
                    int height = Math.min(child.getPreferredSize().height, Math.max(0, bottom - top));
                    bottom -= height;
                    child.setBounds(x, bottom, width, height);
                }
            }
            for (Component child : parent.getComponents())
            {
                if (!BUTTON_AREA_NAME.equals(child.getName()))
                {
                    child.setBounds(x, top, width, Math.max(0, bottom - top));
                }
            }
        }

    }

    /** Below this owner width, dialogs are pages: a phone held upright, a narrow window */
    private static final int MAX_PAGE_WIDTH = 600;

    /** Below this owner height, dialogs are pages: a phone lying on its side, or with its keyboard open */
    private static final int MAX_PAGE_HEIGHT = 500;

    /** A page taller than this part of the window takes the whole window (a list, a form); shorter, it keeps its height */
    private static final double TALL_DIALOG_RATIO = 0.5;

    /** Name given by BasicOptionPaneUI (and the look and feels extending it, FlatLaf included) to the area of the buttons */
    private static final String BUTTON_AREA_NAME = "OptionPane.buttonArea";

    private static final int SCROLL_UNIT = 16;

    private final JDialog dialog;
    private final Window owner;
    private ComponentListener ownerListener;
    private boolean isPage;

}
