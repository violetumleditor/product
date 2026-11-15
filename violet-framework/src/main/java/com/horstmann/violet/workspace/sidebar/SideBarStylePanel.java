package com.horstmann.violet.workspace.sidebar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * Colors and line thickness, pinned below the tools of the open side bar so that they never scroll away: a section like the
 * others, open at first, which its header collapses to leave the room to the tools.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class SideBarStylePanel extends JPanel
{

    /**
     * @param choices the component holding the colors and the thicknesses to choose from, shared with the reduced side bar
     */
    public SideBarStylePanel(Component choices)
    {
        super(new BorderLayout());
        ResourceBundleInjector.getInjector().inject(this);
        this.choices = choices;
        setOpaque(false);
        setBorder(new EmptyBorder(SECTION_TOP_GAP, SECTION_GAP, SECTION_GAP, SECTION_GAP));
        createSection(true);
    }

    /**
     * The section takes its fonts and colors when it is built: built again with the new theme, as the other sections are
     */
    @Override
    public void updateUI()
    {
        super.updateUI();
        // Null while the constructor of JPanel runs
        if (this.section != null && this.choices.getParent() == this.section)
        {
            createSection(this.section.isExpanded());
        }
    }

    private void createSection(boolean isExpanded)
    {
        removeAll();
        this.choices.setVisible(isExpanded);
        this.section = new SideBarSection(this.styleTitle, colorsIcon(), this.choices);
        add(this.section, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Lends the choices to the reduced side bar, which shows them in a popup
     *
     * @return the component holding the colors and the thicknesses
     */
    public Component takeChoices()
    {
        if (this.choices.getParent() == this.section)
        {
            this.wasExpanded = this.section.isExpanded();
        }
        this.choices.setVisible(true);
        return this.choices;
    }

    /**
     * Takes the choices back, as they were before {@link #takeChoices()}
     */
    public void attachChoices()
    {
        createSection(this.wasExpanded);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        g.setColor(ThemeManager.getInstance().getTheme().getSidebarBackgroundStartColor());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * A palette
     */
    private Shape colorsIcon()
    {
        Path2D path = new Path2D.Double();
        path.append(new Ellipse2D.Double(3, 3, 18, 18), false);
        path.append(new Ellipse2D.Double(7, 9, 2, 2), false);
        path.append(new Ellipse2D.Double(11, 6.5, 2, 2), false);
        path.append(new Ellipse2D.Double(15, 9, 2, 2), false);
        path.append(new Ellipse2D.Double(10, 13, 5, 5), false);
        return path;
    }

    private static final int SECTION_GAP = 10;
    private static final int SECTION_TOP_GAP = 4;

    private final Component choices;
    private SideBarSection section;
    private boolean wasExpanded = true;

    @ResourceBundleBean(key = "title.style.text")
    private String styleTitle;

}
