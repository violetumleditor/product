package com.horstmann.violet.application.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.behavior.CutCopyPasteBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.IEditorPartBehavior;
import com.horstmann.violet.workspace.editorpart.behavior.UndoRedoCompoundBehavior;
import com.horstmann.violet.workspace.sidebar.SideBar;

/**
 * Compact tool bar shown at the right of the menu bar. As the menu bar is shared by all the diagrams, each button acts on the
 * active workspace.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class MenuBarToolBar extends JPanel
{

    public MenuBarToolBar(MainFrame mainFrame)
    {
        super(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        ResourceBundleInjector.getInjector().inject(this);
        this.mainFrame = mainFrame;
        setOpaque(false);
        addButton(this.undoTooltip, undoShape(), workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::undo));
        addButton(this.redoTooltip, mirror(undoShape()), workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::redo));
        addSeparator();
        addButton(this.cutTooltip, cutShape(), workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::cut));
        addButton(this.copyTooltip, copyShape(), workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::copy));
        addButton(this.pasteTooltip, pasteShape(), workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::paste));
        addButton(this.deleteTooltip, deleteShape(), workspace -> workspace.getEditorPart().removeSelected());
        addSeparator();
        addButton(this.zoomOutTooltip, zoomOutShape(), workspace -> workspace.getEditorPart().changeZoom(-1));
        addButton(this.zoomInTooltip, zoomInShape(), workspace -> workspace.getEditorPart().changeZoom(1));
    }

    /**
     * Prevents the menu bar from stretching this panel, so that the free space goes to the horizontal glue before it.
     */
    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    private void addButton(String tooltip, Shape shape, Consumer<IWorkspace> action)
    {
        JButton button = new ToolButton(new ShapeIcon(shape, ICON_SIZE));
        button.setToolTipText(tooltip);
        button.addActionListener(event -> {
            IWorkspace workspace = this.mainFrame.getActiveWorkspace();
            if (workspace != null)
            {
                action.accept(workspace);
            }
        });
        add(button);
    }

    private void addSeparator()
    {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        Color foreground = ThemeManager.getInstance().getTheme().getMenubarForegroundColor();
        separator.setForeground(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 90));
        separator.setPreferredSize(new Dimension(9, BUTTON_SIZE.height));
        add(separator);
    }

    private static <T extends IEditorPartBehavior> void withBehavior(IWorkspace workspace, Class<T> type, Consumer<T> action)
    {
        IEditorPartBehaviorManager behaviorManager = workspace.getEditorPart().getBehaviorManager();
        List<T> found = behaviorManager.getBehaviors(type);
        if (found.size() == 1)
        {
            action.accept(found.get(0));
        }
    }

    private static Shape undoShape()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(9, 14);
        path.lineTo(4, 9);
        path.lineTo(9, 4);
        path.moveTo(4, 9);
        path.lineTo(14.5, 9);
        path.append(new Arc2D.Double(9, 9, 11, 11, 90, -180, Arc2D.OPEN), true);
        path.lineTo(11, 20);
        return path;
    }

    private static Shape mirror(Shape shape)
    {
        return new AffineTransform(-1, 0, 0, 1, 24, 0).createTransformedShape(shape);
    }

    private static Shape cutShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 6, 6), false);
        path.append(new Ellipse2D.Double(3, 15, 6, 6), false);
        path.append(new Line2D.Double(20, 4, 8.12, 15.88), false);
        path.append(new Line2D.Double(14.47, 14.48, 20, 20), false);
        path.append(new Line2D.Double(8.12, 8.12, 12, 12), false);
        return path;
    }

    private static Shape copyShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new RoundRectangle2D.Double(8, 8, 14, 14, 4, 4), false);
        path.moveTo(4, 16);
        path.quadTo(2, 16, 2, 14);
        path.lineTo(2, 4);
        path.quadTo(2, 2, 4, 2);
        path.lineTo(14, 2);
        path.quadTo(16, 2, 16, 4);
        return path;
    }

    private static Shape pasteShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new RoundRectangle2D.Double(8, 2, 8, 4, 2, 2), false);
        path.moveTo(16, 4);
        path.lineTo(18, 4);
        path.quadTo(20, 4, 20, 6);
        path.lineTo(20, 20);
        path.quadTo(20, 22, 18, 22);
        path.lineTo(6, 22);
        path.quadTo(4, 22, 4, 20);
        path.lineTo(4, 6);
        path.quadTo(4, 4, 6, 4);
        path.lineTo(8, 4);
        return path;
    }

    private static Shape deleteShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Line2D.Double(3, 6, 21, 6), false);
        path.moveTo(19, 6);
        path.lineTo(19, 20);
        path.quadTo(19, 22, 17, 22);
        path.lineTo(7, 22);
        path.quadTo(5, 22, 5, 20);
        path.lineTo(5, 6);
        path.moveTo(8, 6);
        path.lineTo(8, 4);
        path.quadTo(8, 2, 10, 2);
        path.lineTo(14, 2);
        path.quadTo(16, 2, 16, 4);
        path.lineTo(16, 6);
        path.append(new Line2D.Double(10, 11, 10, 17), false);
        path.append(new Line2D.Double(14, 11, 14, 17), false);
        return path;
    }

    private static Shape zoomOutShape()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 16, 16), false);
        path.append(new Line2D.Double(21, 21, 16.65, 16.65), false);
        path.append(new Line2D.Double(8, 11, 14, 11), false);
        return path;
    }

    private static Shape zoomInShape()
    {
        Path2D path = new Path2D.Double(zoomOutShape());
        path.append(new Line2D.Double(11, 8, 11, 14), false);
        return path;
    }

    /**
     * Flat button showing a rounded background on hover, whatever the current theme is.
     */
    private static class ToolButton extends JButton
    {
        ToolButton(Icon icon)
        {
            super(icon);
            setPreferredSize(BUTTON_SIZE);
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            // Keeps the keyboard focus on the diagram
            setFocusable(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            if (getModel().isRollover())
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color foreground = getForeground();
                int alpha = getModel().isPressed() ? 60 : 30;
                g2.setColor(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), alpha));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    private static final int ICON_SIZE = 18;
    private static final Dimension BUTTON_SIZE = new Dimension(28, 26);

    private final MainFrame mainFrame;

    @ResourceBundleBean(key = "undo.tooltip")
    private String undoTooltip;
    @ResourceBundleBean(key = "redo.tooltip")
    private String redoTooltip;
    @ResourceBundleBean(key = "cut.tooltip")
    private String cutTooltip;
    @ResourceBundleBean(key = "copy.tooltip")
    private String copyTooltip;
    @ResourceBundleBean(key = "paste.tooltip")
    private String pasteTooltip;
    @ResourceBundleBean(key = "delete.tooltip")
    private String deleteTooltip;
    @ResourceBundleBean(key = "zoomout.tooltip")
    private String zoomOutTooltip;
    @ResourceBundleBean(key = "zoomin.tooltip")
    private String zoomInTooltip;

}
