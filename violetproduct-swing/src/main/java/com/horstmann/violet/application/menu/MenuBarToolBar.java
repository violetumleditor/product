package com.horstmann.violet.application.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.horstmann.violet.application.gui.MainFrame;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.swingextension.ShapeIcon;
import com.horstmann.violet.framework.swingextension.ToolShapes;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.IWorkspace;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
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
        addButton(this.undoTooltip, ToolShapes.UNDO, workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::undo));
        addButton(this.redoTooltip, ToolShapes.REDO, workspace -> withBehavior(workspace, UndoRedoCompoundBehavior.class, UndoRedoCompoundBehavior::redo));
        addSeparator();
        addButton(this.cutTooltip, ToolShapes.CUT, workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::cut));
        addButton(this.copyTooltip, ToolShapes.COPY, workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::copy));
        addButton(this.pasteTooltip, ToolShapes.PASTE, workspace -> withBehavior(workspace, CutCopyPasteBehavior.class, CutCopyPasteBehavior::paste));
        addButton(this.deleteTooltip, ToolShapes.DELETE, workspace -> workspace.getEditorPart().removeSelected());
        addSeparator();
        addButton(this.zoomOutTooltip, ToolShapes.ZOOM_OUT, workspace -> workspace.getEditorPart().changeZoom(-1));
        add(getZoomLabel());
        addButton(this.zoomInTooltip, ToolShapes.ZOOM_IN, workspace -> workspace.getEditorPart().changeZoom(1));
        addSeparator();
        add(new ThemeSwitch());
    }

    /**
     * Prevents the menu bar from stretching this panel, so that the free space goes to the horizontal glue before it.
     */
    @Override
    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }

    /**
     * Shows the zoom of this diagram, and follows its changes
     *
     * @param workspace the active diagram, or null if none is opened
     */
    public void setActiveWorkspace(IWorkspace workspace)
    {
        JComponent editorComponent = workspace != null ? workspace.getEditorPart().getSwingComponent() : null;
        if (editorComponent == this.zoomSource)
        {
            return;
        }
        if (this.zoomSource != null)
        {
            this.zoomSource.removePropertyChangeListener(IEditorPart.ZOOM_PROPERTY, getZoomListener());
        }
        this.zoomSource = editorComponent;
        if (editorComponent != null)
        {
            editorComponent.addPropertyChangeListener(IEditorPart.ZOOM_PROPERTY, getZoomListener());
            getZoomLabel().showZoom(workspace.getEditorPart().getZoomFactor());
        }
        else
        {
            getZoomLabel().setText("");
        }
    }

    private ZoomLabel getZoomLabel()
    {
        if (this.zoomLabel == null)
        {
            this.zoomLabel = new ZoomLabel();
        }
        return this.zoomLabel;
    }

    /**
     * @return the listener which shows the zoom changes of the active diagram. Always the same instance, so that it can be removed.
     */
    private PropertyChangeListener getZoomListener()
    {
        if (this.zoomListener == null)
        {
            this.zoomListener = event -> getZoomLabel().showZoom((Double) event.getNewValue());
        }
        return this.zoomListener;
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

    /**
     * Zoom of the active diagram as a percentage, with a fixed width so that the buttons around it don't move.
     */
    private static class ZoomLabel extends JLabel
    {
        ZoomLabel()
        {
            super("", SwingConstants.CENTER);
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
        }

        void showZoom(double zoomFactor)
        {
            setText(Math.round(zoomFactor * 100) + "%");
        }

        @Override
        public Dimension getPreferredSize()
        {
            // Only grows beyond 999%, which is rare
            FontMetrics metrics = getFontMetrics(getFont());
            int width = Math.max(metrics.stringWidth(USUAL_WIDEST_TEXT), metrics.stringWidth(getText()));
            return new Dimension(width, BUTTON_SIZE.height);
        }

        private static final String USUAL_WIDEST_TEXT = "000%";
    }

    /**
     * Switch between a sun and a moon, which toggles the light and dark themes.
     */
    private class ThemeSwitch extends JToggleButton
    {
        ThemeSwitch()
        {
            setPreferredSize(new Dimension(2 * (SWITCH_ICON_SIZE + SWITCH_GAP) + TRACK_WIDTH, BUTTON_SIZE.height));
            setForeground(ThemeManager.getInstance().getTheme().getMenubarForegroundColor());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setFocusable(false);
            refreshState();
            // Later, as the look and feel can't change safely during the events of this button
            addActionListener(event -> SwingUtilities.invokeLater(() -> {
                MenuBarToolBar.this.mainFrame.toggleTheme();
                refreshState();
            }));
        }

        private void refreshState()
        {
            boolean isDark = ThemeManager.getInstance().getTheme().isDark();
            setSelected(isDark);
            setToolTipText(isDark ? MenuBarToolBar.this.lightThemeTooltip : MenuBarToolBar.this.darkThemeTooltip);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int iconY = (getHeight() - SWITCH_ICON_SIZE) / 2;
            int trackX = SWITCH_ICON_SIZE + SWITCH_GAP;
            int trackY = (getHeight() - TRACK_HEIGHT) / 2;
            boolean isDark = isSelected();
            new ShapeIcon(ToolShapes.SUN, SWITCH_ICON_SIZE, getForegroundColor(isDark ? INACTIVE_ALPHA : 255)).paintIcon(this, g2, 0, iconY);
            new ShapeIcon(ToolShapes.MOON, SWITCH_ICON_SIZE, getForegroundColor(isDark ? 255 : INACTIVE_ALPHA)).paintIcon(this, g2,
                    trackX + TRACK_WIDTH + SWITCH_GAP, iconY);
            g2.setColor(getForegroundColor(getModel().isRollover() ? TRACK_ROLLOVER_ALPHA : TRACK_ALPHA));
            g2.fillRoundRect(trackX, trackY, TRACK_WIDTH, TRACK_HEIGHT, TRACK_HEIGHT, TRACK_HEIGHT);
            int knobSize = TRACK_HEIGHT - 2 * KNOB_GAP;
            int knobX = isDark ? trackX + TRACK_WIDTH - KNOB_GAP - knobSize : trackX + KNOB_GAP;
            g2.setColor(getForeground());
            g2.fillOval(knobX, trackY + KNOB_GAP, knobSize, knobSize);
            g2.dispose();
        }

        private Color getForegroundColor(int alpha)
        {
            Color foreground = getForeground();
            return new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), alpha);
        }
    }

    private static final int SWITCH_ICON_SIZE = 16;
    private static final int SWITCH_GAP = 5;
    private static final int TRACK_WIDTH = 30;
    private static final int TRACK_HEIGHT = 16;
    private static final int KNOB_GAP = 3;
    private static final int TRACK_ALPHA = 70;
    private static final int TRACK_ROLLOVER_ALPHA = 110;
    private static final int INACTIVE_ALPHA = 130;
    private static final int ICON_SIZE = 18;
    private static final Dimension BUTTON_SIZE = new Dimension(28, 26);

    private final MainFrame mainFrame;
    private ZoomLabel zoomLabel;
    private PropertyChangeListener zoomListener;
    /** Editor of the active diagram, whose zoom is shown */
    private JComponent zoomSource;

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
    @ResourceBundleBean(key = "theme.dark.tooltip")
    private String darkThemeTooltip;
    @ResourceBundleBean(key = "theme.light.tooltip")
    private String lightThemeTooltip;

}
