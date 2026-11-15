package com.horstmann.violet.workspace.sidebar.thicknesstools;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.PanelUI;

import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * The line thickness as a slider, under a title followed by the chosen value
 */
public class ThicknessToolsBarPanelUI extends PanelUI
{

    public ThicknessToolsBarPanelUI(ThicknessToolsBarPanel thicknessToolsPanel)
    {
        this.thicknessToolsPanel = thicknessToolsPanel;
    }

    @Override
    public void installUI(JComponent c)
    {
        // Built again with each theme: the slider and the labels take its look
        ITheme theme = ThemeManager.getInstance().getTheme();
        c.removeAll();
        c.setLayout(new BorderLayout(0, TITLE_GAP));
        c.setBorder(new EmptyBorder(TOP_GAP, SIDE_GAP, BOTTOM_GAP, SIDE_GAP));
        c.setBackground(theme.getSidebarElementBackgroundColor());
        Font font = theme.getToggleButtonFont();
        JLabel titleLabel = new JLabel(this.thicknessToolsPanel.getTitle());
        this.valueLabel = new JLabel();
        for (JLabel aLabel : new JLabel[] { titleLabel, this.valueLabel })
        {
            aLabel.setForeground(theme.getContrastColor(LABEL_ALPHA));
            if (font != null)
            {
                aLabel.setFont(font.deriveFont(Font.PLAIN, font.getSize2D() - 1f));
            }
        }
        final JSlider slider = new JSlider(ThicknessToolsBarPanel.MIN_THICKNESS, ThicknessToolsBarPanel.MAX_THICKNESS,
                this.thicknessToolsPanel.getShownThickness());
        this.slider = slider;
        slider.setOpaque(false);
        slider.setSnapToTicks(true);
        slider.setMinorTickSpacing(1);
        // Keeps the keyboard focus on the diagram
        slider.setFocusable(false);
        slider.setToolTipText(this.thicknessToolsPanel.getTitle());
        slider.addChangeListener(e -> {
            // Not when the slider is only moved to the thickness of the selected element
            if (slider.getValue() != this.thicknessToolsPanel.getShownThickness())
            {
                this.thicknessToolsPanel.fireThicknessChanged(slider.getValue());
            }
        });
        JComponent header = new JComponent()
        {
        };
        header.setLayout(new BorderLayout());
        header.add(titleLabel, BorderLayout.CENTER);
        header.add(this.valueLabel, BorderLayout.EAST);
        c.add(header, BorderLayout.NORTH);
        c.add(slider, BorderLayout.CENTER);
        refresh();
    }

    /**
     * Shows the current thickness of the panel
     */
    public void refresh()
    {
        if (this.slider == null)
        {
            return;
        }
        int thickness = this.thicknessToolsPanel.getShownThickness();
        this.valueLabel.setText(thickness + " px");
        this.slider.setValue(thickness);
    }

    private static final int TOP_GAP = 6;
    private static final int BOTTOM_GAP = 4;
    private static final int SIDE_GAP = 10;
    private static final int TITLE_GAP = 2;
    private static final int LABEL_ALPHA = 170;

    private ThicknessToolsBarPanel thicknessToolsPanel;
    private JSlider slider;
    private JLabel valueLabel;
}
