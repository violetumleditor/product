package com.horstmann.violet.framework.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatClientProperties;
import com.horstmann.violet.framework.propertyeditor.customeditor.ChoiceIcons;

/**
 * Rounded "chips" showing each value of a choice with its icon and its name, wrapped on as many lines as needed, so that every
 * value and the current one are understood at a glance, without opening a list nor hovering an icon. The current value is
 * tinted with the accent color and circled with it.
 */
public class ChoiceChipGroup extends JPanel
{
    /**
     * @param editor the editor of the property, which gives the values, their icons and their names
     */
    public ChoiceChipGroup(final CustomPropertyEditorSupport editor)
    {
        setLayout(new WrapLayout());
        setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        int selectedIndex = editor.getSelectedIndex();
        for (int i = 0; i < editor.getChoiceCount(); i++)
        {
            final int index = i;
            JToggleButton chip = new JToggleButton(editor.getDisplayName(i), editor.getIcon(i));
            chip.setIconTextGap(4);
            chip.putClientProperty(ChoiceIcons.TEXT_COLOR_WHEN_SELECTED, Boolean.TRUE);
            chip.setSelected(i == selectedIndex);
            chip.addActionListener(e -> editor.select(index));
            chip.addItemListener(e -> styleChip(chip));
            styleChip(chip);
            group.add(chip);
            add(chip);
            this.chips.add(chip);
        }
    }

    /**
     * Gives a chip its pill shape, with the accent color when selected. Colors are read from the current theme.
     *
     * @param chip to style
     */
    private void styleChip(JToggleButton chip)
    {
        Color border = chip.isSelected() ? UIManager.getColor("Component.focusColor") : UIManager.getColor("Component.borderColor");
        StringBuilder style = new StringBuilder("arc: 999; margin: 2,6,2,10; focusWidth: 0");
        Color background = UIManager.getColor("TextField.background");
        if (background != null)
        {
            style.append("; background: ").append(toHex(background));
        }
        if (border != null)
        {
            style.append("; borderColor: ").append(toHex(border)).append("; hoverBorderColor: ").append(toHex(border));
        }
        Color selectedBackground = getSelectedBackground();
        if (selectedBackground != null)
        {
            style.append("; selectedBackground: ").append(toHex(selectedBackground));
        }
        chip.putClientProperty(FlatClientProperties.STYLE, style.toString());
    }

    /**
     * @return a light tint of the accent color over the field background, or null if the theme has none
     */
    private Color getSelectedBackground()
    {
        Color accent = UIManager.getColor("Component.focusColor");
        Color background = UIManager.getColor("TextField.background");
        if (accent == null || background == null)
        {
            return UIManager.getColor("ToggleButton.selectedBackground");
        }
        float ratio = 0.22f;
        return new Color(mix(accent.getRed(), background.getRed(), ratio), mix(accent.getGreen(), background.getGreen(), ratio),
                mix(accent.getBlue(), background.getBlue(), ratio));
    }

    private int mix(int a, int b, float ratio)
    {
        return Math.round(a * ratio + b * (1 - ratio));
    }

    private String toHex(Color color)
    {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    /**
     * Restyles the chips with the colors of the new theme
     */
    @Override
    public void updateUI()
    {
        super.updateUI();
        if (this.chips != null)
        {
            for (JToggleButton aChip : this.chips)
            {
                styleChip(aChip);
            }
        }
    }

    private final List<JToggleButton> chips = new ArrayList<JToggleButton>();

    /**
     * Lays the chips out from left to right, starting a new line when the next one doesn't fit in the width. Before the panel
     * has a width (first layout of the dialog), lines are cut at {@link #DEFAULT_WIDTH}, so the chips don't widen the dialog.
     */
    private class WrapLayout implements LayoutManager
    {

        @Override
        public void addLayoutComponent(String name, Component comp)
        {
            // Nothing to remember
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
            // Nothing to forget
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            int width = parent.getWidth() > 0 ? parent.getWidth() : DEFAULT_WIDTH;
            return layout(parent, width, false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            int widest = 0;
            for (Component aComponent : parent.getComponents())
            {
                widest = Math.max(widest, aComponent.getPreferredSize().width);
            }
            Insets insets = parent.getInsets();
            return layout(parent, widest + insets.left + insets.right, false);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            layout(parent, parent.getWidth(), true);
        }

        /**
         * @param parent the panel
         * @param width available, insets included
         * @param isApplied true to move the components, false to compute the size only
         * @return the size taken by the components cut in lines of this width, insets included
         */
        private Dimension layout(Container parent, int width, boolean isApplied)
        {
            Insets insets = parent.getInsets();
            int maxX = width - insets.right;
            int x = insets.left;
            int y = insets.top;
            int lineHeight = 0;
            int usedWidth = 0;
            for (Component aComponent : parent.getComponents())
            {
                if (!aComponent.isVisible())
                {
                    continue;
                }
                Dimension size = aComponent.getPreferredSize();
                if (x > insets.left && x + size.width > maxX)
                {
                    x = insets.left;
                    y += lineHeight + GAP;
                    lineHeight = 0;
                }
                if (isApplied)
                {
                    aComponent.setBounds(x, y, size.width, size.height);
                }
                x += size.width;
                usedWidth = Math.max(usedWidth, x);
                x += GAP;
                lineHeight = Math.max(lineHeight, size.height);
            }
            return new Dimension(usedWidth + insets.right, y + lineHeight + insets.bottom);
        }

    }

    /**
     * Space between two chips, horizontally and vertically
     */
    private static final int GAP = 4;

    /**
     * Width at which lines are cut while the panel has no width yet
     */
    private static final int DEFAULT_WIDTH = 300;
}
