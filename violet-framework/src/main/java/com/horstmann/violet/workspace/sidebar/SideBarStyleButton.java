package com.horstmann.violet.workspace.sidebar;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;

/**
 * Colors and line thickness in the reduced side bar: a swatch pinned below the tool icons, showing the color and the thickness
 * chosen last, which opens the choices beside it.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class SideBarStyleButton extends JPanel
{

    /**
     * @param colorChoiceBar
     * @param thicknessChoiceBar
     * @param stylePanel the style section of the open side bar, which lends its colors and thicknesses to choose from
     */
    public SideBarStyleButton(IColorChoiceBar colorChoiceBar, IThicknessChoiceBar thicknessChoiceBar, SideBarStylePanel stylePanel)
    {
        super(new BorderLayout());
        ResourceBundleInjector.getInjector().inject(this);
        this.stylePanel = stylePanel;
        setOpaque(false);
        // Keeps the keyboard focus on the diagram
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(this.styleTooltip);
        add(new StylePreview(colorChoiceBar, thicknessChoiceBar), BorderLayout.CENTER);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // The popup closes on this press before the button gets it: the click that follows must not open it again
                SideBarStyleButton.this.isClickClosingChoices = e.getWhen() == SideBarStyleButton.this.choicesClosingPressTime;
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e) && !SideBarStyleButton.this.isClickClosingChoices)
                {
                    showChoices();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                SideBarStyleButton.this.isRollover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                SideBarStyleButton.this.isRollover = false;
                repaint();
            }
        });
    }

    private void showChoices()
    {
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(this.stylePanel.takeChoices(), BorderLayout.CENTER);
        popup.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                rememberPressOnButton();
                // Later: the popup still owns the choices while it closes
                SwingUtilities.invokeLater(SideBarStyleButton.this.stylePanel::attachChoices);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
        // Beside the button, its bottom aligned with the bottom of the side bar
        Dimension size = popup.getPreferredSize();
        popup.show(this, getWidth() + POPUP_GAP, getHeight() - size.height);
    }

    /**
     * Remembers the time of the press that closes the choices, if it is made on this button.
     */
    private void rememberPressOnButton()
    {
        AWTEvent event = EventQueue.getCurrentEvent();
        if (event instanceof MouseEvent && event.getID() == MouseEvent.MOUSE_PRESSED)
        {
            MouseEvent mouseEvent = (MouseEvent) event;
            if (SwingUtilities.isDescendingFrom(mouseEvent.getComponent(), this))
            {
                this.choicesClosingPressTime = mouseEvent.getWhen();
            }
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        ITheme theme = ThemeManager.getInstance().getTheme();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(theme.getSidebarElementBackgroundColor());
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(theme.getContrastColor(SEPARATOR_ALPHA));
        g2.drawLine(0, 0, getWidth(), 0);
        if (this.isRollover)
        {
            Color accentColor = theme.getToggleButtonSelectedBorderColor();
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), ROLLOVER_ALPHA));
            g2.fillRoundRect(ROLLOVER_INSET, ROLLOVER_INSET, getWidth() - 2 * ROLLOVER_INSET, getHeight() - 2 * ROLLOVER_INSET,
                    ROLLOVER_ARC, ROLLOVER_ARC);
        }
        g2.dispose();
    }

    private static final int BUTTON_WIDTH = 40;
    private static final int BUTTON_HEIGHT = 44;
    private static final int POPUP_GAP = 6;
    private static final int SEPARATOR_ALPHA = 40;
    private static final int ROLLOVER_ALPHA = 40;
    private static final int ROLLOVER_INSET = 4;
    private static final int ROLLOVER_ARC = 8;

    private final SideBarStylePanel stylePanel;
    private boolean isRollover = false;
    /** Time of the last press on this button which closed the choices */
    private long choicesClosingPressTime = -1;
    /** Whether the current click closed the choices, so must not open them again */
    private boolean isClickClosingChoices = false;

    @ResourceBundleBean(key = "style.tooltip")
    private String styleTooltip;

}
