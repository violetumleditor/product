package com.horstmann.violet.workspace.sidebar;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * Tab hung on the right edge of the side bar, halfway down, with a chevron pointing where the side bar goes. It takes no room in
 * the side bar; it lies over the edge of the diagram. Its container places it and keeps it on top of the other components.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class SideBarHandle extends JComponent
{

    /**
     * @param onToggle run when the tab is clicked, to reduce or open the side bar
     */
    public SideBarHandle(Runnable onToggle)
    {
        ResourceBundleInjector.getInjector().inject(this);
        this.onToggle = onToggle;
        setOpaque(false);
        // Keeps the keyboard focus on the diagram
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e))
                {
                    SideBarHandle.this.onToggle.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                SideBarHandle.this.isRollover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                SideBarHandle.this.isRollover = false;
                repaint();
            }
        });
        setSideBarOpen(true);
    }

    /**
     * @param isOpen true if the side bar shows its tools with their names, so the tab reduces it; false if the tab opens it
     */
    public void setSideBarOpen(boolean isOpen)
    {
        this.isSideBarOpen = isOpen;
        setToolTipText(isOpen ? this.collapseTooltip : this.expandTooltip);
        repaint();
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(TAB_WIDTH, TAB_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ITheme theme = ThemeManager.getInstance().getTheme();
        Color accentColor = UIManager.getColor("Component.accentColor");
        if (accentColor == null)
        {
            accentColor = DEFAULT_ACCENT_COLOR;
        }
        // Square on the left, where it meets the side bar, rounded on the right
        Shape tab = new RoundRectangle2D.Double(-ARC, 0, getWidth() + ARC - 1, getHeight() - 1, 2 * ARC, 2 * ARC);
        g2.setColor(theme.getSidebarElementBackgroundColor());
        g2.fill(tab);
        if (this.isRollover)
        {
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), ROLLOVER_ALPHA));
            g2.fill(tab);
        }
        g2.setColor(theme.getContrastColor(BORDER_ALPHA));
        g2.draw(tab);
        Shape chevron = this.isSideBarOpen ? ToolShapes.CHEVRON_LEFT : ToolShapes.CHEVRON_RIGHT;
        new ShapeIcon(chevron, ICON_SIZE, accentColor).paintIcon(this, g2, (getWidth() - ICON_SIZE) / 2,
                (getHeight() - ICON_SIZE) / 2);
        g2.dispose();
    }

    private static final int TAB_WIDTH = 16;
    private static final int TAB_HEIGHT = 52;
    private static final int ARC = 8;
    private static final int ICON_SIZE = 14;
    private static final int ROLLOVER_ALPHA = 40;
    private static final int BORDER_ALPHA = 40;
    private static final Color DEFAULT_ACCENT_COLOR = new Color(123, 79, 214);

    private final Runnable onToggle;
    private boolean isSideBarOpen;
    private boolean isRollover = false;

    @ResourceBundleBean(key = "sidebar.collapse.tooltip")
    private String collapseTooltip;

    @ResourceBundleBean(key = "sidebar.expand.tooltip")
    private String expandTooltip;

}
