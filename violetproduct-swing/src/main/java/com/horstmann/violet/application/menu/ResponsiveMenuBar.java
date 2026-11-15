package com.horstmann.violet.application.menu;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.workspace.sidebar.SideBar;

/**
 * Menu bar which follows the width of the window, the same way on a phone and on a desktop. When everything doesn't fit, all the
 * menus go into a single "hamburger" menu, which takes the place of the logo; then, if needed, the tool bar moves its least useful
 * buttons into its "more" menu. The diagram name shrinks, cut with an ellipsis, before anything else moves.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class ResponsiveMenuBar extends JMenuBar
{

    /**
     * @param logo shown at the left, or null
     * @param menus in display order
     * @param title diagram name, centered in the space between the menus and the tool bar
     * @param toolBar shown at the right
     */
    public ResponsiveMenuBar(JComponent logo, List<JMenu> menus, JComponent title, MenuBarToolBar toolBar)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.logo = logo;
        this.title = title;
        this.toolBar = toolBar;
        // Kept by the look and feel, as it isn't a UIResource
        setLayout(new MenuBarLayout());
        getMenus().addAll(menus);
        add(getMenuButton());
        getMenuButton().setVisible(false);
        if (logo != null)
        {
            add(logo);
        }
        for (JMenu aMenu : menus)
        {
            add(aMenu);
        }
        // Embedded in the window title bar, FlatLaf extends the menu bar up to the window buttons only if it holds a glue
        add(getGlue());
        add(title);
        add(toolBar);
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                fitWidth();
            }
        });
    }

    /**
     * Asks only for the width of its most compact state. Embedded in the window title bar, the menu bar gets this width, then is
     * extended up to the window buttons: a larger width would put its end under these buttons, and this bar would never know how
     * much space it really has. Its content is fitted to the width it gets.
     */
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(getCompactWidth(), super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(getCompactWidth(), super.getMinimumSize().height);
    }

    /**
     * @return the width of this bar once everything that can go into a menu went there, and the title is shrunk
     */
    private int getCompactWidth()
    {
        if (this.toolBar == null)
        {
            // Called by the constructor of JMenuBar, before this one
            return 0;
        }
        Insets insets = getInsets();
        return insets.left + insets.right + getMenuButton().getPreferredSize().width + TITLE_MIN_WIDTH
                + this.toolBar.getPreferredWidth(this.toolBar.getMaxCollapseLevel());
    }

    /**
     * Chooses what goes into the hamburger and "more" menus for the current width
     */
    private void fitWidth()
    {
        Insets insets = getInsets();
        int availableWidth = getWidth() - insets.left - insets.right;
        if (availableWidth <= 0)
        {
            return;
        }
        if (!this.isMenusCollapsed)
        {
            // Measured while the menus are in the bar: in the hamburger menu, they are laid out differently
            this.expandedMenusWidth = measureExpandedMenus();
        }
        boolean isMenusCollapsed = this.expandedMenusWidth + TITLE_MIN_WIDTH + this.toolBar.getPreferredWidth(0) > availableWidth;
        int menusWidth = isMenusCollapsed ? getMenuButton().getPreferredSize().width : this.expandedMenusWidth;
        int level = 0;
        int maxLevel = this.toolBar.getMaxCollapseLevel();
        while (level < maxLevel && menusWidth + TITLE_MIN_WIDTH + this.toolBar.getPreferredWidth(level) > availableWidth)
        {
            level++;
        }
        setMenusCollapsed(isMenusCollapsed);
        this.toolBar.setCollapseLevel(level);
        revalidate();
        repaint();
    }

    private int measureExpandedMenus()
    {
        int width = this.logo != null ? this.logo.getPreferredSize().width : 0;
        for (JMenu aMenu : getMenus())
        {
            width += aMenu.getPreferredSize().width;
        }
        return width;
    }

    /**
     * Moves all the menus into the hamburger menu, or back into the bar
     *
     * @param isCollapsed true to move them into the hamburger menu
     */
    private void setMenusCollapsed(boolean isCollapsed)
    {
        if (isCollapsed == this.isMenusCollapsed)
        {
            return;
        }
        this.isMenusCollapsed = isCollapsed;
        JMenu menuButton = getMenuButton();
        if (isCollapsed)
        {
            for (JMenu aMenu : getMenus())
            {
                remove(aMenu);
                menuButton.add(aMenu);
            }
        }
        else
        {
            menuButton.removeAll();
            int index = getComponentIndex(getGlue());
            for (JMenu aMenu : getMenus())
            {
                add(aMenu, index++);
            }
        }
        menuButton.setVisible(isCollapsed);
        if (this.logo != null)
        {
            this.logo.setVisible(!isCollapsed);
        }
    }

    /**
     * @return the hamburger menu, holding all the menus when the window is too narrow for them
     */
    private JMenu getMenuButton()
    {
        if (this.menuButton == null)
        {
            this.menuButton = new JMenu();
            this.menuButton.setIcon(new ShapeIcon(ToolShapes.MENU, MENU_ICON_SIZE));
            this.menuButton.setToolTipText(this.menuTooltip);
        }
        return this.menuButton;
    }

    private Component getGlue()
    {
        if (this.glue == null)
        {
            this.glue = Box.createHorizontalGlue();
        }
        return this.glue;
    }

    private List<JMenu> getMenus()
    {
        if (this.menus == null)
        {
            this.menus = new ArrayList<JMenu>();
        }
        return this.menus;
    }

    /**
     * Lays the components before the title out from the left, the tool bar at the right, each at its preferred width, and gives
     * the title the space left between them. Unlike the box layout of a menu bar, the tool bar is never squeezed.
     */
    private class MenuBarLayout implements LayoutManager
    {
        @Override
        public void layoutContainer(Container target)
        {
            Insets insets = target.getInsets();
            int height = target.getHeight() - insets.top - insets.bottom;
            int x = insets.left;
            for (Component aComponent : target.getComponents())
            {
                if (aComponent == ResponsiveMenuBar.this.title || aComponent == ResponsiveMenuBar.this.toolBar || aComponent == getGlue())
                {
                    continue;
                }
                if (!aComponent.isVisible())
                {
                    aComponent.setBounds(x, insets.top, 0, 0);
                    continue;
                }
                int width = aComponent.getPreferredSize().width;
                aComponent.setBounds(x, insets.top, width, height);
                x += width;
            }
            Dimension toolBarSize = ResponsiveMenuBar.this.toolBar.getPreferredSize();
            int toolBarX = Math.max(x, target.getWidth() - insets.right - toolBarSize.width);
            ResponsiveMenuBar.this.toolBar.setBounds(toolBarX, insets.top + (height - toolBarSize.height) / 2, toolBarSize.width,
                    toolBarSize.height);
            int titleHeight = ResponsiveMenuBar.this.title.getPreferredSize().height;
            ResponsiveMenuBar.this.title.setBounds(x, insets.top + (height - titleHeight) / 2, Math.max(0, toolBarX - x), titleHeight);
            getGlue().setBounds(x, insets.top, 0, height);
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            Insets insets = parent.getInsets();
            int width = 0;
            int height = 0;
            for (Component aComponent : parent.getComponents())
            {
                if (aComponent.isVisible())
                {
                    Dimension size = aComponent.getPreferredSize();
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            }
            return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return preferredLayoutSize(parent);
        }

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            // Nothing to remember
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // Nothing to forget
        }
    }

    private static final int TITLE_MIN_WIDTH = 90;
    private static final int MENU_ICON_SIZE = 20;

    private final JComponent logo;
    private final JComponent title;
    private final MenuBarToolBar toolBar;
    private JMenu menuButton;
    private Component glue;
    private List<JMenu> menus;
    /** True when the menus are in the hamburger menu */
    private boolean isMenusCollapsed = false;
    /** Width of the logo and the menus, as last measured in the bar */
    private int expandedMenusWidth;

    @ResourceBundleBean(key = "menubar.menu.tooltip")
    private String menuTooltip;

}
