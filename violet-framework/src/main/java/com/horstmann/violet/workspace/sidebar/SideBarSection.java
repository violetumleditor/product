package com.horstmann.violet.workspace.sidebar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * Rounded card of the side bar, with a header which collapses its content.
 */
public class SideBarSection extends JPanel
{

    /**
     * @param title
     * @param icon shape on a 24x24 grid, or null
     * @param content
     */
    public SideBarSection(String title, Shape icon, Component content)
    {
        super(new BorderLayout());
        ITheme theme = ThemeManager.getInstance().getTheme();
        this.content = content;
        setOpaque(false);
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(theme.getToggleButtonFont().deriveFont(Font.BOLD));
        titleLabel.setIconTextGap(8);
        if (icon != null)
        {
            titleLabel.setIcon(new ShapeIcon(icon, HEADER_ICON_SIZE, theme.getToggleButtonSelectedBorderColor()));
        }
        this.chevronLabel.setForeground(titleLabel.getForeground());

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(2, HEADER_SIDE_GAP, 4, HEADER_SIDE_GAP));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(this.chevronLabel, BorderLayout.EAST);
        header.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                setExpanded(!isExpanded());
            }
        });

        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        // Sections are rebuilt when the theme changes
        setExpanded(content.isVisible());
    }

    /**
     * @return true if the content is visible
     */
    public boolean isExpanded()
    {
        return this.content.isVisible();
    }

    /**
     * Shows or hides the content
     *
     * @param isExpanded
     */
    public void setExpanded(boolean isExpanded)
    {
        this.content.setVisible(isExpanded);
        this.chevronLabel.setIcon(isExpanded ? CHEVRON_DOWN : CHEVRON_RIGHT);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ITheme theme = ThemeManager.getInstance().getTheme();
        g2.setColor(theme.getSidebarElementBackgroundColor());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_ARC, CORNER_ARC);
        g2.setColor(theme.getContrastColor(OUTLINE_ALPHA));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_ARC, CORNER_ARC);
        g2.dispose();
    }

    private static Icon createChevron(double x1, double y1, double x2, double y2, double x3, double y3)
    {
        Path2D path = new Path2D.Double();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        return new ShapeIcon(path, CHEVRON_SIZE);
    }

    private static final int PADDING = 8;
    private static final int HEADER_SIDE_GAP = 6;
    private static final int HEADER_ICON_SIZE = 18;
    private static final int CHEVRON_SIZE = 16;
    private static final int CORNER_ARC = 14;
    private static final int OUTLINE_ALPHA = 22;
    private static final Icon CHEVRON_DOWN = createChevron(6, 9, 12, 15, 18, 9);
    private static final Icon CHEVRON_RIGHT = createChevron(9, 6, 15, 12, 9, 18);

    private final Component content;
    private final JLabel chevronLabel = new JLabel();

}
