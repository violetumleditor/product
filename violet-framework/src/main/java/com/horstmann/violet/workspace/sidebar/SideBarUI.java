package com.horstmann.violet.workspace.sidebar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.PanelUI;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.theme.ThemeManager;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

@ResourceBundleBean(resourceReference=SideBar.class)
public class SideBarUI extends PanelUI
{

    public SideBarUI(SideBar sideBar)
    {
        this.sideBar = sideBar;
        ResourceBundleInjector.getInjector().inject(this);
    }

    @Override
    public void installUI(JComponent c)
    {
        c.removeAll();
        c.setLayout(new BorderLayout());
        this.sectionsPanel = new JPanel(new GridBagLayout());
        this.sectionsPanel.setBackground(ThemeManager.getInstance().getTheme().getSidebarBackgroundStartColor());
        this.sectionsPanel.setBorder(new EmptyBorder(0, 0, SECTION_GAP, 0));
        IGraphToolsBar graphToolsBar = this.sideBar.getGraphToolsBar();
        String diagramName = graphToolsBar.getDiagramName();
        this.toolSections.clear();
        this.toolSections.add(addSection(graphToolsBar.getCommonToolsAWTComponent(), commonToolsTitle, commonToolsIcon(), 0));
        this.toolSections.add(addSection(graphToolsBar.getAWTComponent(), diagramName != null ? diagramName : diagramToolsTitle,
                toolsIcon(), 1));
        this.searchSection = addSection(graphToolsBar.getSearchResultsAWTComponent(), "", searchIcon(), 2);
        int i = 3;
        for (ISideBarElement anExternalElement : this.sideBar.getExternalContributionElements().keySet()) {
            String externalElementTitle = this.sideBar.getExternalContributionElements().get(anExternalElement);
            this.toolSections.add(addSection(anExternalElement.getAWTComponent(), externalElementTitle, null, i++));
        }
        refreshSearch();
        c.add(this.sectionsPanel, BorderLayout.NORTH);
        c.setBorder(new MatteBorder(0, 0, 0, 0, ThemeManager.getInstance().getTheme().getSidebarBorderColor()));
        c.setBackground(ThemeManager.getInstance().getTheme().getSidebarBackgroundStartColor());
        this.sideBar.doLayout();
        JRootPane rootPane = SwingUtilities.getRootPane(this.sideBar);
        if (rootPane != null) {
            rootPane.repaint();
        }
    }

    /**
     * Shows the tools found by the search of the side bar instead of the tool sections, or these sections if nothing is searched
     */
    public void refreshSearch()
    {
        if (this.searchSection == null)
        {
            return;
        }
        String searchText = this.sideBar.getSearchText();
        boolean isSearching = !searchText.isEmpty();
        int toolCount = this.sideBar.getGraphToolsBar().setSearchText(searchText);
        this.searchSection.setTitle(toolCount > 0 ? MessageFormat.format(this.searchResultsTitle, toolCount) : this.searchNoResultTitle);
        this.searchSection.setVisible(isSearching);
        for (SideBarSection aSection : this.toolSections)
        {
            aSection.setVisible(!isSearching);
        }
        this.sectionsPanel.revalidate();
        this.sectionsPanel.repaint();
    }

    private SideBarSection addSection(final Component c, String title, Shape icon, int order)
    {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = order;
        gbc.weightx = 1;
        gbc.insets = new Insets(SECTION_GAP, SECTION_GAP, 0, SECTION_GAP);
        SideBarSection section = new SideBarSection(title, icon, c);
        this.sectionsPanel.add(section, gbc);
        return section;
    }

    /**
     * A magnifying glass
     */
    private static Shape searchIcon()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(4, 4, 12, 12), false);
        path.moveTo(14.5, 14.5);
        path.lineTo(20, 20);
        return path;
    }

    private static Shape toolsIcon()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(12, 3);
        path.lineTo(21, 8);
        path.lineTo(12, 13);
        path.lineTo(3, 8);
        path.closePath();
        path.moveTo(3, 12.5);
        path.lineTo(12, 17.5);
        path.lineTo(21, 12.5);
        path.moveTo(3, 17);
        path.lineTo(12, 22);
        path.lineTo(21, 17);
        return path;
    }

    /**
     * A note with a folded corner, the most used common tool
     */
    private static Shape commonToolsIcon()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(5, 3);
        path.lineTo(15, 3);
        path.lineTo(20, 8);
        path.lineTo(20, 21);
        path.lineTo(5, 21);
        path.closePath();
        path.moveTo(15, 3);
        path.lineTo(15, 8);
        path.lineTo(20, 8);
        path.moveTo(8.5, 12);
        path.lineTo(16.5, 12);
        path.moveTo(8.5, 16);
        path.lineTo(14, 16);
        return path;
    }

    private static final int SECTION_GAP = 10;

    private SideBar sideBar;
    private JPanel sectionsPanel;
    /** Sections hidden while tools are searched */
    private List<SideBarSection> toolSections = new ArrayList<SideBarSection>();
    /** Section of the tools found by the search, shown only while tools are searched */
    private SideBarSection searchSection;

    @ResourceBundleBean(key = "search.results.text")
    private String searchResultsTitle;

    @ResourceBundleBean(key = "search.noresult.text")
    private String searchNoResultTitle;

    @ResourceBundleBean(key = "title.diagramtools.text")
    private String diagramToolsTitle;

    @ResourceBundleBean(key = "title.commontools.text")
    private String commonToolsTitle;

    @ResourceBundleBean(key = "title.extendedfunctions.text")
    private String extendedFunctionsTitle;

}
