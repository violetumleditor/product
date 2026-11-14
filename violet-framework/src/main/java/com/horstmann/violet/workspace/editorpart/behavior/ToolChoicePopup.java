package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;

/**
 * A choice among tools of the tool bar, shown on the diagram near a point: large buttons for a finger, with the tool bar icons.
 * Used to choose the kind of edge to add ({@link AddEdgeByDragBehavior}) and the kind of node to create
 * ({@link AddEdgeByDragBehavior}, {@link AddNodeByTapBehavior}).
 * <p>
 * Shown above the point, so that the finger doesn't hide it, or below it near the top of the diagram.
 */
class ToolChoicePopup
{

    ToolChoicePopup(IEditorPart editorPart)
    {
        this.editorPart = editorPart;
    }

    /**
     * Kinds of edges: a single row of icons, the tool name as tool tip
     *
     * @param tools the tools to offer, in this order
     * @param point where to show the choice, on the graph (unzoomed coordinates)
     * @param onChoice called with the chosen tool
     * @param onCancel called if the choice is dismissed
     */
    void showIcons(List<GraphTool> tools, Point2D point, Consumer<GraphTool> onChoice, Runnable onCancel)
    {
        show(tools, point, false, onChoice, onCancel);
    }

    /**
     * Kinds of nodes: icons with their name below, as several node icons look alike and a touch screen shows no tool tip
     *
     * @param tools the tools to offer, in this order
     * @param point where to show the choice, on the graph (unzoomed coordinates)
     * @param onChoice called with the chosen tool
     * @param onCancel called if the choice is dismissed
     */
    void showLabeledIcons(List<GraphTool> tools, Point2D point, Consumer<GraphTool> onChoice, Runnable onCancel)
    {
        show(tools, point, true, onChoice, onCancel);
    }

    private void show(List<GraphTool> tools, Point2D point, boolean isLabeled, final Consumer<GraphTool> onChoice,
            final Runnable onCancel)
    {
        final JPopupMenu popup = new JPopupMenu();
        int columns = isLabeled ? Math.min(tools.size(), LABELED_COLUMNS) : tools.size();
        popup.setLayout(new GridLayout(0, Math.max(columns, 1), CHOICE_GAP, CHOICE_GAP));
        // Hiding the popup after a choice also tells its listener, which must not take it for a dismissal
        final boolean[] isChosen = { false };
        for (final GraphTool aTool : tools)
        {
            JButton button = new JButton(getChoiceIcon(aTool.getIcon()));
            button.setToolTipText(aTool.getLabel());
            button.getAccessibleContext().setAccessibleName(aTool.getLabel());
            button.putClientProperty("JButton.buttonType", "toolBarButton");
            button.setFocusable(false);
            if (isLabeled)
            {
                button.setText(getWrappedLabel(aTool.getLabel()));
                button.setVerticalTextPosition(SwingConstants.BOTTOM);
                button.setHorizontalTextPosition(SwingConstants.CENTER);
                button.setFont(button.getFont().deriveFont(button.getFont().getSize2D() - 1));
                button.setMargin(new Insets(4, 2, 4, 2));
            }
            else
            {
                button.setPreferredSize(new Dimension(CHOICE_SIZE, CHOICE_SIZE));
            }
            button.addActionListener(e -> {
                isChosen[0] = true;
                popup.setVisible(false);
                onChoice.accept(aTool);
            });
            popup.add(button);
        }
        popup.addPopupMenuListener(new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e)
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
            {
                if (!isChosen[0])
                {
                    isChosen[0] = true; // told once only
                    onCancel.run();
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e)
            {
            }
        });
        JComponent component = this.editorPart.getSwingComponent();
        double zoom = this.editorPart.getZoomFactor();
        Dimension size = popup.getPreferredSize();
        int x = (int) Math.round(point.getX() * zoom) - size.width / 2;
        int y = (int) Math.round(point.getY() * zoom) - size.height - CHOICE_OFFSET;
        Rectangle visible = component.getVisibleRect();
        if (y < visible.y)
        {
            y = (int) Math.round(point.getY() * zoom) + CHOICE_OFFSET;
        }
        x = Math.max(visible.x, Math.min(x, visible.x + visible.width - size.width));
        popup.show(component, x, y);
    }

    /**
     * @return the label on two lines at most if it is long, centered below the icon
     */
    private String getWrappedLabel(String label)
    {
        String text = label == null ? "" : label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><div style='text-align:center;width:" + LABEL_WIDTH + "px'>" + text + "</div></html>";
    }

    /**
     * @return the tool bar icon, larger for a finger
     */
    private Icon getChoiceIcon(final Icon icon)
    {
        return new Icon()
        {
            @Override
            public int getIconWidth()
            {
                return (int) Math.round(icon.getIconWidth() * CHOICE_ICON_SCALE);
            }

            @Override
            public int getIconHeight()
            {
                return (int) Math.round(icon.getIconHeight() * CHOICE_ICON_SCALE);
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.translate(x, y);
                g2.scale(CHOICE_ICON_SCALE, CHOICE_ICON_SCALE);
                icon.paintIcon(c, g2, 0, 0);
                g2.dispose();
            }
        };
    }

    /** Size of each icon only button, in pixels: large enough for a finger */
    private static final int CHOICE_SIZE = 44;

    private static final int CHOICE_GAP = 2;

    private static final double CHOICE_ICON_SCALE = 1.5;

    /** Columns of the labeled choice: it stays narrower than a phone screen */
    private static final int LABELED_COLUMNS = 5;

    /** Width of a label, in pixels: a longer one goes on two lines */
    private static final int LABEL_WIDTH = 62;

    /** Space between the point and the choice, in pixels */
    private static final int CHOICE_OFFSET = 16;

    private IEditorPart editorPart;

}
