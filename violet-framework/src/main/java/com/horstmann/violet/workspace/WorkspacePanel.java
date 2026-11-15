package com.horstmann.violet.workspace;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.horstmann.violet.framework.swingextension.DragScrollSupport;
import com.horstmann.violet.framework.swingextension.ScrollFadeLayerUI;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.sidebar.ISideBar;
import com.horstmann.violet.workspace.sidebar.SideBarHandle;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Diagram editor with its side bar. The side bar follows the width of this panel, the same way on a phone and on a desktop:
 * open when wide enough, else reduced to a column of icons. Opened from that column, it shows over the diagram and closes as soon
 * as a tool is chosen or the diagram is clicked.
 */
public class WorkspacePanel extends JPanel
{

    public WorkspacePanel(IWorkspace workspace)
    {
        this.workspace = workspace;
    }

    /**
     * Applies the new theme to the components which do not take their color from the look and feel, and to the side bar parts
     * which are not shown right now
     */
    @Override
    public void updateUI()
    {
        super.updateUI();
        // Fields read directly: this method is first called by the constructor of JPanel, before any field exists
        ITheme theme = ThemeManager.getInstance().getTheme();
        if (this.scrollableEditorPart != null)
        {
            this.scrollableEditorPart.setBackground(theme.getDiagramBackgroundColor());
        }
        updateDetachedComponentUI(this.sideBarPanel);
        updateDetachedComponentUI(this.compactSideBarPanel);
        if (this.sideBarPanel != null)
        {
            applyThemeColors();
        }
    }

    /**
     * @param component a side bar part, which may currently be out of this panel
     */
    private void updateDetachedComponentUI(JComponent component)
    {
        if (component != null && component.getParent() == null)
        {
            SwingUtilities.updateComponentTreeUI(component);
        }
    }

    private void applyThemeColors()
    {
        ITheme theme = ThemeManager.getInstance().getTheme();
        getCompactSideBarPanel().setBackground(theme.getSidebarElementBackgroundColor());
        getCompactSideBarPanel().setBorder(new MatteBorder(0, 0, 0, 1, theme.getContrastColor(SEPARATOR_ALPHA)));
        getScrollableCompactSideBar().getViewport().setBackground(theme.getSidebarElementBackgroundColor());
        getSideBarPanel().setBorder(isOverlayShown() ? new MatteBorder(0, 0, 0, 1, theme.getContrastColor(SEPARATOR_ALPHA)) : null);
    }

    public void prepareLayout()
    {
        setLayout(new SideBarLayout());
        add(getScrollableEditorPart(), BorderLayout.CENTER);
        add(getSideBarHandle(), SideBarLayout.HANDLE, 0);
        getGraphToolsBar().addListener(this::onToolSelectionChanged);
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                followWidth();
            }
        });
        refreshSideBar();
        refreshDisplay();
    }

    /**
     * Opens the side bar when this panel becomes wide enough, reduces it when it becomes too narrow. In between, the side bar
     * stays as the user left it.
     */
    private void followWidth()
    {
        boolean isNarrow = getWidth() < NARROW_WIDTH;
        if (this.isNarrow != null && this.isNarrow == isNarrow)
        {
            return;
        }
        this.isNarrow = isNarrow;
        this.isSideBarOpen = !isNarrow;
        refreshSideBar();
    }

    /**
     * Shows the side bar, reduced or open, beside or over the diagram, as its state requires
     */
    private void refreshSideBar()
    {
        boolean isNarrow = this.isNarrow != null && this.isNarrow;
        boolean isDocked = this.isSideBarOpen && !isNarrow;
        boolean isOverlay = this.isSideBarOpen && isNarrow;
        remove(getSideBarPanel());
        remove(getCompactSideBarPanel());
        add(isDocked ? getSideBarPanel() : getCompactSideBarPanel(), BorderLayout.WEST);
        if (isOverlay)
        {
            // On top of everything else
            add(getSideBarPanel(), SideBarLayout.OVERLAY, 0);
            // The handle stays above the side bar it is attached to
            setComponentZOrder(getSideBarHandle(), 0);
            this.toolWhenOverlayOpened = getGraphToolsBar().getSelectedTool();
            Toolkit.getDefaultToolkit().addAWTEventListener(getOverlayCloser(), AWTEvent.MOUSE_EVENT_MASK);
        }
        else
        {
            Toolkit.getDefaultToolkit().removeAWTEventListener(getOverlayCloser());
        }
        getSideBarHandle().setSideBarOpen(this.isSideBarOpen);
        applyThemeColors();
        revalidate();
        repaint();
    }

    private void toggleSideBar()
    {
        this.isSideBarOpen = !this.isSideBarOpen;
        refreshSideBar();
    }

    private void closeOverlay()
    {
        if (isOverlayShown())
        {
            this.isSideBarOpen = false;
            refreshSideBar();
        }
    }

    /**
     * @return true if the side bar is shown over the diagram
     */
    private boolean isOverlayShown()
    {
        return getLayout() instanceof SideBarLayout && ((SideBarLayout) getLayout()).getOverlay() != null;
    }

    /**
     * The side bar shown over the diagram closes once a tool is chosen. During a press, it waits for the release: the press may
     * start a drag of the tool onto the diagram, or a scroll of the side bar.
     */
    private void onToolSelectionChanged(GraphTool selectedTool)
    {
        if (isOverlayShown() && !this.isMousePressed)
        {
            closeOverlay();
        }
    }

    /**
     * @return what closes the side bar shown over the diagram: a press elsewhere, or the release which ends the choice of a tool
     */
    private AWTEventListener getOverlayCloser()
    {
        if (this.overlayCloser == null)
        {
            this.overlayCloser = event -> {
                if (!(event instanceof MouseEvent) || !isOverlayShown())
                {
                    return;
                }
                MouseEvent mouseEvent = (MouseEvent) event;
                Component source = mouseEvent.getComponent();
                if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED)
                {
                    this.isMousePressed = true;
                    if (source != null && !SwingUtilities.isDescendingFrom(source, getSideBarPanel()) && source != getSideBarHandle())
                    {
                        // After this event, which still reaches its target
                        SwingUtilities.invokeLater(this::closeOverlay);
                    }
                }
                else if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED)
                {
                    this.isMousePressed = false;
                    GraphTool selectedTool = getGraphToolsBar().getSelectedTool();
                    if (selectedTool != null && !selectedTool.equals(this.toolWhenOverlayOpened))
                    {
                        SwingUtilities.invokeLater(this::closeOverlay);
                    }
                }
            };
        }
        return this.overlayCloser;
    }

    /**
     * Stops listening to the whole application once this diagram is no longer shown
     */
    @Override
    public void removeNotify()
    {
        super.removeNotify();
        if (this.overlayCloser != null)
        {
            Toolkit.getDefaultToolkit().removeAWTEventListener(this.overlayCloser);
        }
        // Later: the components of this panel can't change while it is being removed
        SwingUtilities.invokeLater(this::closeOverlay);
    }

    /**
     * The handle of the side bar, and the side bar shown over the diagram, must be painted after it
     */
    @Override
    public boolean isOptimizedDrawingEnabled()
    {
        return false;
    }

    private IGraphToolsBar getGraphToolsBar()
    {
        return this.workspace.getSideBar().getGraphToolsBar();
    }

    /**
     * @return the scrollable panel containing the editor
     */
    public JScrollPane getScrollableEditorPart()
    {
        if (this.scrollableEditorPart == null)
        {
            final IEditorPart editorPart = this.workspace.getEditorPart();
            final Component panel = editorPart.getSwingComponent();

            this.scrollableEditorPart = new JScrollPane() {
                @Override
                public void paint(Graphics g)
                {
                	editorPart.getSwingComponent().invalidate();
                    super.paint(g);
                }

            };
            this.scrollableEditorPart.getViewport().setView(panel);
            this.scrollableEditorPart.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
            {

                @Override
                public void adjustmentValueChanged(AdjustmentEvent e)
                {
                	editorPart.getSwingComponent().invalidate();
                    editorPart.getSwingComponent().repaint();
                }
            });
            this.scrollableEditorPart.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener()
            {

                @Override
                public void adjustmentValueChanged(AdjustmentEvent e)
                {
                    editorPart.getSwingComponent().invalidate();
                    editorPart.getSwingComponent().repaint();
                }
            });
            this.scrollableEditorPart.setBackground(ThemeManager.getInstance().getTheme().getDiagramBackgroundColor());
            this.scrollableEditorPart.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.scrollableEditorPart.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            // The wheel scrolls the diagram: one pixel per unit would be far too slow
            this.scrollableEditorPart.getVerticalScrollBar().setUnitIncrement(EDITOR_SCROLL_UNIT);
            this.scrollableEditorPart.getHorizontalScrollBar().setUnitIncrement(EDITOR_SCROLL_UNIT);
        }
        return this.scrollableEditorPart;
    }

    /**
     * @return the open side bar, with the tools and their names
     */
    private JPanel getSideBarPanel()
    {
        if (this.sideBarPanel == null)
        {
            this.sideBarPanel = new JPanel(new BorderLayout())
            {
                /**
                 * Never narrower than it has been: a collapsed section no longer counts in the width, which would shrink the
                 * side bar and the sections still open
                 */
                @Override
                public Dimension getPreferredSize()
                {
                    Dimension size = super.getPreferredSize();
                    WorkspacePanel.this.sideBarWidth = Math.max(WorkspacePanel.this.sideBarWidth, size.width);
                    size.width = WorkspacePanel.this.sideBarWidth;
                    return size;
                }
            };
            // Above and below the tools, out of their scroll pane: always at hand
            this.sideBarPanel.add(this.workspace.getSideBar().getHeaderAWTComponent(), BorderLayout.NORTH);
            this.sideBarPanel.add(getScrollableSideBar(), BorderLayout.CENTER);
            this.sideBarPanel.add(this.workspace.getSideBar().getFooterAWTComponent(), BorderLayout.SOUTH);
        }
        return this.sideBarPanel;
    }

    /**
     * @return the reduced side bar, with the tools as icons
     */
    private JPanel getCompactSideBarPanel()
    {
        if (this.compactSideBarPanel == null)
        {
            this.compactSideBarPanel = new JPanel(new BorderLayout());
            this.compactSideBarPanel.add(getScrollableCompactSideBar(), BorderLayout.CENTER);
            this.compactSideBarPanel.add(this.workspace.getSideBar().getCompactFooterAWTComponent(), BorderLayout.SOUTH);
        }
        return this.compactSideBarPanel;
    }

    /**
     * @return the tab on the right edge of the side bar, which reduces or opens it
     */
    private SideBarHandle getSideBarHandle()
    {
        if (this.sideBarHandle == null)
        {
            this.sideBarHandle = new SideBarHandle(this::toggleSideBar);
        }
        return this.sideBarHandle;
    }

    /**
     * @return scrollpane containing sidebar
     */
    public JScrollPane getScrollableSideBar()
    {
        if (this.scrollableSideBar == null)
        {
            ISideBar sideBar = this.workspace.getSideBar();
            this.scrollableSideBar = createToolsScrollPane(sideBar.getAWTComponent());
            //this.scrollableSideBar.getHorizontalScrollBar().setUI(new TinyScrollBarUI());
            //this.scrollableSideBar.getVerticalScrollBar().setUI(new TinyScrollBarUI());
            //this.scrollableSideBar.setBorder(new MatteBorder(0, 1, 0, 0, ThemeManager.getInstance().getTheme().getSidebarBorderColor()));
        }
        return this.scrollableSideBar;
    }

    /**
     * @return the scroll pane of the tools shown as icons when the side bar is reduced
     */
    private JScrollPane getScrollableCompactSideBar()
    {
        if (this.scrollableCompactSideBar == null)
        {
            this.scrollableCompactSideBar = createToolsScrollPane(getGraphToolsBar().getCompactAWTComponent());
        }
        return this.scrollableCompactSideBar;
    }

    private JScrollPane createToolsScrollPane(Component tools)
    {
        // Fades at the top and the bottom while tools are hidden there
        JScrollPane scrollPane = new JScrollPane(new JLayer<Component>(tools, new ScrollFadeLayerUI()));
        // The fades follow the visible part: the viewport must repaint it all, not copy what it already shows
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        // The finger scrolls the tools by dragging them: the scroll bar is too thin to be caught
        new DragScrollSupport(scrollPane);
        scrollPane.setAlignmentY(Component.TOP_ALIGNMENT);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        // The tools are not Scrollable: without this, the wheel moves them by one pixel per notch
        scrollPane.getVerticalScrollBar().setUnitIncrement(SIDEBAR_SCROLL_UNIT);
        return scrollPane;
    }

    public void refreshDisplay()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                WorkspacePanel.this.revalidate();
                WorkspacePanel.this.repaint();
            }
        });
    }

    /**
     * Border layout which also places one component over the others, along the left edge and as high as the container: the side
     * bar opened over the diagram
     */
    private static class SideBarLayout extends BorderLayout
    {
        static final String OVERLAY = "Overlay";
        static final String HANDLE = "Handle";

        @Override
        public void addLayoutComponent(Component comp, Object constraints)
        {
            if (OVERLAY.equals(constraints))
            {
                this.overlay = comp;
                return;
            }
            if (HANDLE.equals(constraints))
            {
                this.handle = comp;
                return;
            }
            super.addLayoutComponent(comp, constraints);
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            if (comp == this.overlay)
            {
                this.overlay = null;
                return;
            }
            if (comp == this.handle)
            {
                this.handle = null;
                return;
            }
            super.removeLayoutComponent(comp);
        }

        @Override
        public void layoutContainer(Container target)
        {
            super.layoutContainer(target);
            Insets insets = target.getInsets();
            if (this.overlay != null)
            {
                int availableWidth = target.getWidth() - insets.left - insets.right;
                Dimension preferredSize = this.overlay.getPreferredSize();
                int width = Math.min(preferredSize.width, availableWidth);
                this.overlay.setBounds(insets.left, insets.top, width, target.getHeight() - insets.top - insets.bottom);
            }
            if (this.handle != null)
            {
                Component sideBar = this.overlay != null ? this.overlay : getLayoutComponent(WEST);
                int x = sideBar != null ? sideBar.getX() + sideBar.getWidth() : insets.left;
                Dimension size = this.handle.getPreferredSize();
                int height = target.getHeight() - insets.top - insets.bottom;
                this.handle.setBounds(x, insets.top + (height - size.height) / 2, size.width, size.height);
            }
        }

        Component getOverlay()
        {
            return this.overlay;
        }

        private Component overlay;
        private Component handle;
    }

    /** Below this width, the side bar is reduced to icons, and shown over the diagram when opened */
    private static final int NARROW_WIDTH = 1000;
    private static final int EDITOR_SCROLL_UNIT = 16;
    private static final int SIDEBAR_SCROLL_UNIT = 16;
    private static final int SEPARATOR_ALPHA = 40;

    private IWorkspace workspace;
    private JScrollPane scrollableSideBar;
    private JScrollPane scrollableEditorPart;
    private JScrollPane scrollableCompactSideBar;
    private JPanel sideBarPanel;
    private JPanel compactSideBarPanel;
    private SideBarHandle sideBarHandle;
    private AWTEventListener overlayCloser;

    /** Widest preferred width the open side bar ever had, which it keeps */
    private int sideBarWidth = 0;
    /** Null until this panel gets its first width */
    private Boolean isNarrow;
    /** True if the side bar shows its tools with their names, beside or over the diagram */
    private boolean isSideBarOpen = true;
    /** True between a press and its release, anywhere in the application, while the side bar is shown over the diagram */
    private boolean isMousePressed = false;
    /** Tool selected when the side bar was opened over the diagram: a different one closes it */
    private GraphTool toolWhenOverlayOpened;

}
