package com.horstmann.violet.application.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;

/**
 * Entry of the document menu: a click on its text shows the diagram, a click on the cross at its right end closes it
 */
public class DocumentMenuItem extends JMenuItem
{

    /**
     * @param title of the diagram
     * @param closeAction run when the cross is clicked, the menu still open
     * @param closeToolTip shown over the cross only
     */
    public DocumentMenuItem(String title, Runnable closeAction, String closeToolTip)
    {
        super(title);
        this.closeAction = closeAction;
        this.closeToolTip = closeToolTip;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent e)
    {
        if (getCloseButtonBounds().contains(e.getPoint()))
        {
            return this.closeToolTip;
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width + CLOSE_AREA_WIDTH, size.height);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Rectangle closeBounds = getCloseButtonBounds();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (this.isMouseOverCloseButton)
        {
            Color foreground = isArmed() ? UIManager.getColor("MenuItem.selectionForeground") : getForeground();
            g2.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), HOVER_ALPHA));
            g2.fill(new RoundRectangle2D.Double(closeBounds.x, closeBounds.y, closeBounds.width, closeBounds.height, 6, 6));
        }
        int iconX = closeBounds.x + (closeBounds.width - CLOSE_ICON_SIZE) / 2;
        int iconY = closeBounds.y + (closeBounds.height - CLOSE_ICON_SIZE) / 2;
        getCloseIcon().paintIcon(this, g2, iconX, iconY);
        g2.dispose();
    }

    @Override
    protected void processMouseEvent(MouseEvent e)
    {
        boolean isOnCloseButton = getCloseButtonBounds().contains(e.getPoint());
        if (e.getID() == MouseEvent.MOUSE_EXITED)
        {
            setMouseOverCloseButton(false);
        }
        if (isOnCloseButton && (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_CLICKED))
        {
            // Kept from the menu item, which would show the diagram
            e.consume();
            return;
        }
        if (isOnCloseButton && e.getID() == MouseEvent.MOUSE_RELEASED)
        {
            e.consume();
            this.closeAction.run();
            return;
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e)
    {
        setMouseOverCloseButton(getCloseButtonBounds().contains(e.getPoint()));
        super.processMouseMotionEvent(e);
    }

    private void setMouseOverCloseButton(boolean isMouseOverCloseButton)
    {
        if (this.isMouseOverCloseButton == isMouseOverCloseButton)
        {
            return;
        }
        this.isMouseOverCloseButton = isMouseOverCloseButton;
        repaint();
    }

    /**
     * @return the area of the cross, at the right end of the item
     */
    private Rectangle getCloseButtonBounds()
    {
        int x = getWidth() - CLOSE_BUTTON_RIGHT_MARGIN - CLOSE_BUTTON_SIZE;
        int y = (getHeight() - CLOSE_BUTTON_SIZE) / 2;
        return new Rectangle(x, y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
    }

    private Icon getCloseIcon()
    {
        if (this.closeIcon == null)
        {
            this.closeIcon = new ShapeIcon(ToolShapes.CLOSE, CLOSE_ICON_SIZE);
        }
        return this.closeIcon;
    }

    private static final int CLOSE_ICON_SIZE = 10;
    private static final int CLOSE_BUTTON_SIZE = 16;
    private static final int CLOSE_BUTTON_RIGHT_MARGIN = 6;
    private static final int CLOSE_AREA_WIDTH = CLOSE_BUTTON_SIZE + CLOSE_BUTTON_RIGHT_MARGIN + 12;
    private static final int HOVER_ALPHA = 50;

    private final Runnable closeAction;
    private final String closeToolTip;
    private Icon closeIcon;
    private boolean isMouseOverCloseButton;

}
