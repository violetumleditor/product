package com.horstmann.violet.workspace.sidebar;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.colortools.IColorChoiceBar;
import com.horstmann.violet.workspace.sidebar.thicknesstools.IThicknessChoiceBar;

/**
 * Shows the current color and line thickness (chosen last, or those of the selected element), in the reduced side bar where the choices themselves are hidden: a swatch
 * with the background color, its border in the border color and as thick as the lines.
 */
class StylePreview extends JComponent
{

    StylePreview(IColorChoiceBar colorChoiceBar, IThicknessChoiceBar thicknessChoiceBar)
    {
        setOpaque(false);
        this.colorChoiceBar = colorChoiceBar;
        this.thicknessChoiceBar = thicknessChoiceBar;
        colorChoiceBar.addShownColorChoiceListener(this::repaint);
        thicknessChoiceBar.addShownThicknessListener(this::repaint);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(SWATCH_SIZE, SWATCH_SIZE);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = (getWidth() - SWATCH_SIZE) / 2;
        int y = (getHeight() - SWATCH_SIZE) / 2;
        ColorChoice colorChoice = this.colorChoiceBar.getShownColorChoice();
        int borderWidth = Math.min(this.thicknessChoiceBar.getShownThickness(), SWATCH_MAX_BORDER);
        int inset = (borderWidth + 1) / 2;
        int swatchSize = SWATCH_SIZE - 2 * inset;
        g2.setColor(colorChoice.getBackgroundColor());
        g2.fillRoundRect(x + inset, y + inset, swatchSize, swatchSize, CORNER_ARC, CORNER_ARC);
        g2.setColor(colorChoice.getBorderColor());
        g2.setStroke(new BasicStroke(borderWidth));
        g2.drawRoundRect(x + inset, y + inset, swatchSize, swatchSize, CORNER_ARC, CORNER_ARC);
        g2.dispose();
    }

    private static final int SWATCH_SIZE = 22;
    private static final int SWATCH_MAX_BORDER = 5;
    private static final int CORNER_ARC = 6;

    private final IColorChoiceBar colorChoiceBar;
    private final IThicknessChoiceBar thicknessChoiceBar;

}
