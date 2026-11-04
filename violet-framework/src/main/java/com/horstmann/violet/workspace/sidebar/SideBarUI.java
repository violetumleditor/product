package com.horstmann.violet.workspace.sidebar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

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
        addSection(this.sideBar.getGraphToolsBar().getAWTComponent(), diagramToolsTitle, toolsIcon(), 0);
        addSection(this.sideBar.getColorChoiceBar().getAWTComponent(), colorToolsTitle, colorsIcon(), 1);
        addSection(this.sideBar.getThicknessChoiceBar().getAWTComponent(), thicknessToolsTitle, thicknessIcon(), 2);
        int i = 3;
        for (ISideBarElement anExternalElement : this.sideBar.getExternalContributionElements().keySet()) {
            String externalElementTitle = this.sideBar.getExternalContributionElements().get(anExternalElement);
            addSection(anExternalElement.getAWTComponent(), externalElementTitle, null, i++);
        }
        c.add(this.sectionsPanel, BorderLayout.NORTH);
        c.setBorder(new MatteBorder(0, 0, 0, 0, ThemeManager.getInstance().getTheme().getSidebarBorderColor()));
        c.setBackground(ThemeManager.getInstance().getTheme().getSidebarBackgroundStartColor());
        this.sideBar.doLayout();
        JRootPane rootPane = SwingUtilities.getRootPane(this.sideBar);
        if (rootPane != null) {
            rootPane.repaint();
        }
    }

    private void addSection(final Component c, String title, Shape icon, int order)
    {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = order;
        gbc.weightx = 1;
        gbc.insets = new Insets(SECTION_GAP, SECTION_GAP, 0, SECTION_GAP);
        this.sectionsPanel.add(new SideBarSection(title, icon, c), gbc);
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

    private static Shape colorsIcon()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 18, 18), false);
        path.append(new Ellipse2D.Double(7, 9, 2, 2), false);
        path.append(new Ellipse2D.Double(11, 6.5, 2, 2), false);
        path.append(new Ellipse2D.Double(15, 9, 2, 2), false);
        path.append(new Ellipse2D.Double(10, 13, 5, 5), false);
        return path;
    }

    private static Shape thicknessIcon()
    {
        Path2D path = new Path2D.Double();
        path.moveTo(3, 14);
        path.quadTo(7.5, 5, 12, 13);
        path.quadTo(16.5, 21, 21, 11);
        return path;
    }

    private static final int SECTION_GAP = 10;

    private SideBar sideBar;
    private JPanel sectionsPanel;

    @ResourceBundleBean(key = "title.diagramtools.text")
    private String diagramToolsTitle;

    @ResourceBundleBean(key = "title.extendedfunctions.text")
    private String extendedFunctionsTitle;
    
    @ResourceBundleBean(key = "title.colortools.text")
    private String colorToolsTitle;

    @ResourceBundleBean(key = "title.thicknesstools.text")
    private String thicknessToolsTitle;

}
