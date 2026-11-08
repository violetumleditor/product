/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditorSupport;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.CSS;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.UIScale;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.propertyeditor.ChoiceButtonGroup;
import com.horstmann.violet.framework.theme.DarkDiagramGraphics2D;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;

/**
 * A property editor for the MultiLineString type.
 */
@ResourceBundleBean
public class MultiLineStringEditor extends PropertyEditorSupport {
    public MultiLineStringEditor() {
        ResourceBundleInjector.getInjector().inject(this);
    }

    public boolean supportsCustomEditor() {
        return true;
    }

    public Component getCustomEditor() {
        this.source = (MultiLineString) getValue();
        this.textPane = createTextPane();

        // Map MultiLineString size constants to pixel values
        // Apply a 1.25x zoom factor in the editor so it matches the visual scale in the
        // diagram nodes
        double zoom = 1.25;
        int pixelSize = MultiLineString.FontSize.NORMAL; // Default
        if (source.getSize() == MultiLineString.FontSize.LARGE)
            pixelSize = MultiLineString.FontSize.LARGE;
        if (source.getSize() == MultiLineString.FontSize.SMALL)
            pixelSize = MultiLineString.FontSize.SMALL;
        int zoomedPixelSize = (int) (pixelSize * zoom);

        // Set the font before setting the content type to influence the default
        // attributes
        Font defaultFont = new Font(Font.SANS_SERIF, Font.PLAIN, zoomedPixelSize);
        textPane.setFont(defaultFont);
        textPane.setContentType("text/html");

        // Force a default font and size using px (not pt, which gets scaled by Swing's
        // HTML engine)
        Color defaultColor = ColorChoice.DEFAULT_TEXT_COLOR;
        String bodyRule = "body { font-family: " + defaultFont.getFamily() + "; " +
                "font-size: " + zoomedPixelSize + "px; " +
                "color: " + String.format("#%06x", defaultColor.getRGB() & 0xFFFFFF) + "; }";
        ((javax.swing.text.html.HTMLDocument) textPane.getDocument()).getStyleSheet().addRule(bodyRule);

        // Inside a div, Enter creates implied paragraphs which are written back without any tag: line breaks would be lost
        textPane.setText(source.getText().replaceAll("(?i)<(/?)div\\b", "<$1p"));

        textPane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, tab);
        textPane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, shiftTab);

        this.toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(3, 3, 4, 3));

        this.boldBtn = new JToggleButton(TextFormatIcons.bold());
        boldBtn.setFocusable(false);
        boldBtn.setSelected(false);
        boldBtn.setToolTipText(withShortcut(boldTooltip, KeyEvent.VK_B));
        boldBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javax.swing.text.MutableAttributeSet attrs = textPane.getInputAttributes();
                boolean isNowBold = !StyleConstants.isBold(attrs);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setBold(sas, isNowBold);
                setCharacterAttributes(sas);
                textPane.requestFocusInWindow();
            }
        });

        this.italicBtn = new JToggleButton(TextFormatIcons.italic());
        italicBtn.setFocusable(false);
        italicBtn.setToolTipText(withShortcut(italicTooltip, KeyEvent.VK_I));
        italicBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javax.swing.text.MutableAttributeSet attrs = textPane.getInputAttributes();
                boolean isNowItalic = !StyleConstants.isItalic(attrs);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setItalic(sas, isNowItalic);
                setCharacterAttributes(sas);
                textPane.requestFocusInWindow();
            }
        });

        this.underlineBtn = new JToggleButton(TextFormatIcons.underline());
        underlineBtn.setFocusable(false);
        underlineBtn.setSelected(source.isUnderlined());
        underlineBtn.setToolTipText(withShortcut(underlineTooltip, KeyEvent.VK_U));
        underlineBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                javax.swing.text.MutableAttributeSet attrs = textPane.getInputAttributes();
                boolean isNowUnderline = !StyleConstants.isUnderline(attrs);
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setUnderline(sas, isNowUnderline);
                setCharacterAttributes(sas);
                source.setUnderlined(isNowUnderline);
                firePropertyChange();
                textPane.requestFocusInWindow();
            }
        });

        this.leftAlignBtn = new JToggleButton(TextFormatIcons.alignment(StyleConstants.ALIGN_LEFT));
        leftAlignBtn.setFocusable(false);
        leftAlignBtn.setToolTipText(alignLeftTooltip);
        leftAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.LEFT);
        leftAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_LEFT);
                textPane.requestFocusInWindow();
            }
        });

        this.centerAlignBtn = new JToggleButton(TextFormatIcons.alignment(StyleConstants.ALIGN_CENTER));
        centerAlignBtn.setFocusable(false);
        centerAlignBtn.setToolTipText(alignCenterTooltip);
        centerAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.CENTER);
        centerAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_CENTER);
                textPane.requestFocusInWindow();
            }
        });

        this.rightAlignBtn = new JToggleButton(TextFormatIcons.alignment(StyleConstants.ALIGN_RIGHT));
        rightAlignBtn.setFocusable(false);
        rightAlignBtn.setToolTipText(alignRightTooltip);
        rightAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.RIGHT);
        rightAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_RIGHT);
                textPane.requestFocusInWindow();
            }
        });

        this.sizeButton = new JButton(TextFormatIcons.chevron());
        sizeButton.setFocusable(false);
        sizeButton.setHorizontalTextPosition(SwingConstants.LEADING);
        sizeButton.setIconTextGap(1);
        sizeButton.setToolTipText(sizeTooltip);

        // Map MultiLineString size constants to pixel values
        int initialSize = MultiLineString.FontSize.NORMAL; // Default
        if (source.getSize() == MultiLineString.FontSize.LARGE)
            initialSize = MultiLineString.FontSize.LARGE;
        if (source.getSize() == MultiLineString.FontSize.SMALL)
            initialSize = MultiLineString.FontSize.SMALL;

        // Track the intended font size ourselves (HTML document attributes are
        // CSS-scaled)
        this.currentFontSize = initialSize;
        this.initialFontSize = initialSize;


        // Determine initial alignment from source
        int initialAlignment = StyleConstants.ALIGN_CENTER;
        if (source.getJustification() == MultiLineString.Justification.LEFT)
            initialAlignment = StyleConstants.ALIGN_LEFT;
        if (source.getJustification() == MultiLineString.Justification.RIGHT)
            initialAlignment = StyleConstants.ALIGN_RIGHT;
        this.currentAlignment = initialAlignment;

        sizeButton.setText(String.valueOf(initialSize));
        // Set the input attributes so newly typed text uses the correct font size
        javax.swing.text.MutableAttributeSet inputAttrs = textPane.getInputAttributes();
        StyleConstants.setFontSize(inputAttrs, initialSize);
        // Seed underline state so newly typed text inherits the source default
        StyleConstants.setUnderline(inputAttrs, source.isUnderlined());

        // Apply initial alignment directly to the document's paragraph attributes
        SimpleAttributeSet alignAttr = new SimpleAttributeSet();
        StyleConstants.setAlignment(alignAttr, initialAlignment);
        StyledDocument styledDoc = textPane.getStyledDocument();
        styledDoc.setParagraphAttributes(0, styledDoc.getLength() + 1, alignAttr, false);

        sizeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSizeMenu();
            }
        });

        this.colorButton = new TextColorButton(ColorChoice.TEXT_COLOR_LIST);
        colorButton.setFocusable(false);
        colorButton.setToolTipText(colorTooltip);
        colorButton.setSelectedColor(getPaletteColorAtCaret());
        colorButton.addColorListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isUpdatingToolbar && colorButton.getSelectedColor() != null) {
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setForeground(sas, colorButton.getSelectedColor());
                    // Input attributes copied from HTML text hold the previous color as CSS
                    textPane.getInputAttributes().removeAttribute(CSS.Attribute.COLOR);
                    setCharacterAttributes(sas);
                    textPane.requestFocusInWindow();
                }
            }
        });
        bindShortcut(KeyEvent.VK_B, boldBtn);
        bindShortcut(KeyEvent.VK_I, italicBtn);
        bindShortcut(KeyEvent.VK_U, underlineBtn);

        textPane.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                if (!isUpdating) {
                    updateToolbarState();
                }
            }
        });

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                isUpdating = true;
                try {
                    source.setText(textPane.getText());
                    firePropertyChange();

                    // Run UI updates on the next event loop to avoid "Attempt to mutate in
                    // notification"
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateToolbarState();
                        }
                    });
                } finally {
                    isUpdating = false;
                }
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        layoutToolbar();
        textPane.setMargin(new Insets(3, 6, 3, 6));
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(null);

        // Ensure the editor has a 5-line height based on the default font size (12pt)
        Font fontForHeight = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        int fontHeight = textPane.getFontMetrics(fontForHeight).getHeight();
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 5 * fontHeight + 30)); // +30 for breathing room

        return new RichTextBox(toolbar, scrollPane, textPane);
    }

    /**
     * Lays out the toolbar: style, alignment, then size and color, separated by thin lines. All its controls are flat and take
     * the same height.
     */
    private void layoutToolbar() {
        toolbar.removeAll();
        AbstractButton[][] groups = {
                { boldBtn, italicBtn, underlineBtn },
                { leftAlignBtn, centerAlignBtn, rightAlignBtn },
                { sizeButton, colorButton } };
        int height = UIScale.scale(CONTROL_SIZE);
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) {
                toolbar.add(new ToolbarSeparator());
            }
            for (AbstractButton button : groups[i]) {
                ChoiceButtonGroup.styleAsToolButton(button);
                button.putClientProperty(FlatClientProperties.STYLE, button.getClientProperty(FlatClientProperties.STYLE)
                        + "; toolbar.margin: 0,4,0,4; minimumWidth: 0");
                int width = button.getPreferredSize().width;
                if (button == sizeButton) {
                    // Wide enough for every size, as the text follows the caret
                    String text = sizeButton.getText();
                    for (int fontSize : FONT_SIZES) {
                        sizeButton.setText(String.valueOf(fontSize));
                        width = Math.max(width, sizeButton.getPreferredSize().width);
                    }
                    sizeButton.setText(text);
                    // The text is measured in whole pixels but drawn with fractional widths on scaled screens: without some
                    // room, it would be cut to "..."
                    width += UIScale.scale(4);
                }
                button.setPreferredSize(new Dimension(Math.max(width, height), height));
                toolbar.add(button);
            }
        }
    }

    /**
     * @return the formatting toolbar, shown as the header of the text, created by {@link #getCustomEditor()}
     */
    public JComponent getToolbar() {
        return toolbar;
    }

    /**
     * @return the text area, created by {@link #getCustomEditor()}
     */
    public JComponent getTextComponent() {
        return textPane;
    }

    /**
     * @param tooltip of a button
     * @param key letter of its shortcut
     * @return the tooltip followed by the shortcut, as in the menus
     */
    private static String withShortcut(String tooltip, int key) {
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        return tooltip + " (" + KeyEvent.getModifiersExText(modifiers) + "+" + KeyEvent.getKeyText(key) + ")";
    }

    /**
     * Makes a menu shortcut of the text area click a button.
     *
     * @param key letter of the shortcut
     * @param button to click
     */
    private void bindShortcut(int key, final AbstractButton button) {
        String actionKey = "violet-format-" + key;
        int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        textPane.getInputMap().put(KeyStroke.getKeyStroke(key, modifiers), actionKey);
        textPane.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.doClick(0);
            }
        });
    }

    /**
     * Opens the list of text sizes under the size button, the current one checked.
     */
    private void showSizeMenu() {
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        for (final int size : FONT_SIZES) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(size), size == currentFontSize);
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentFontSize = size;
                    sizeButton.setText(String.valueOf(size));
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setFontSize(sas, size);
                    setCharacterAttributes(sas);
                    textPane.requestFocusInWindow();
                }
            });
            group.add(item);
            menu.add(item);
        }
        menu.show(sizeButton, 0, sizeButton.getHeight() + 2);
    }

    /**
     * A thin vertical line between groups of buttons, in the border color of the theme.
     */
    private static class ToolbarSeparator extends JComponent {
        ToolbarSeparator() {
            setPreferredSize(new Dimension(UIScale.scale(9), UIScale.scale(CONTROL_SIZE)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color color = UIManager.getColor("Component.borderColor");
            g.setColor(color != null ? color : Color.GRAY);
            int inset = getHeight() / 5;
            g.fillRect(getWidth() / 2, inset, 1, getHeight() - 2 * inset);
        }
    }

    private void setCharacterAttributes(AttributeSet attrs) {
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        if (start != end) {
            StyledDocument doc = textPane.getStyledDocument();
            doc.setCharacterAttributes(start, end - start, attrs, false);
        }
        // Always update input attributes for future typing
        javax.swing.text.MutableAttributeSet inputAttrs = textPane.getInputAttributes();
        inputAttrs.addAttributes(attrs);
    }

    private void applyAlignment(int alignment) {
        currentAlignment = alignment;
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setAlignment(sas, alignment);
        StyledDocument doc = textPane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength() + 1, sas, false);
        // Propagate alignment change back to source model
        if (alignment == StyleConstants.ALIGN_LEFT)
            source.setJustification(MultiLineString.Justification.LEFT);
        if (alignment == StyleConstants.ALIGN_CENTER)
            source.setJustification(MultiLineString.Justification.CENTER);
        if (alignment == StyleConstants.ALIGN_RIGHT)
            source.setJustification(MultiLineString.Justification.RIGHT);
        // Re-save HTML so it includes the updated alignment for next editor open
        source.setText(textPane.getText());
        firePropertyChange();
    }

    private void updateToolbarState() {
        isUpdatingToolbar = true;
        try {
            int pos = textPane.getCaretPosition();
            StyledDocument doc = textPane.getStyledDocument();

            // Use character attributes at caret position (or selection start)
            AttributeSet attr = textPane.getInputAttributes();
            if (pos > 0 && (textPane.getSelectedText() == null)) {
                attr = doc.getCharacterElement(pos - 1).getAttributes();
            }

            boldBtn.setSelected(StyleConstants.isBold(attr));
            italicBtn.setSelected(StyleConstants.isItalic(attr));
            underlineBtn.setSelected(StyleConstants.isUnderline(attr));

            // Use tracked alignment instead of reading from HTML document
            // (HTML paragraph attributes may have defaults that differ from source)
            leftAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_LEFT);
            centerAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_CENTER);
            rightAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_RIGHT);

            currentFontSize = getFontSizeAtCaret();
            sizeButton.setText(String.valueOf(currentFontSize));
            colorButton.setSelectedColor(getPaletteColorAtCaret());
        } finally {
            isUpdatingToolbar = false;
        }
    }

    /**
     * Reads the size as written in the text rather than as displayed: the HTML view scales the sizes with the font of the look
     * and feel.
     *
     * @return the size of the text at the selection start (or before the caret), in points
     */
    private int getFontSizeAtCaret() {
        int start = textPane.getSelectionStart();
        int pos = start != textPane.getSelectionEnd() ? start : Math.max(0, start - 1);
        AttributeSet attr = textPane.getStyledDocument().getCharacterElement(pos).getAttributes();
        Object size = attr.getAttribute(CSS.Attribute.FONT_SIZE);
        if (size == null) {
            return initialFontSize;
        }
        String value = size.toString().trim();
        try {
            if (value.matches("[+-]?\\d+")) {
                // <font size="n">: one of the 7 HTML sizes, relative to 3 when signed
                int index = value.startsWith("+") || value.startsWith("-") ? 3 + Integer.parseInt(value.replace("+", ""))
                        : Integer.parseInt(value);
                return FONT_SIZES[Math.max(1, Math.min(FONT_SIZES.length, index)) - 1];
            }
            if (value.endsWith("pt")) {
                return Math.round(Float.parseFloat(value.substring(0, value.length() - 2)));
            }
        } catch (NumberFormatException e) {
            // Unknown unit, fall back to the displayed size
        }
        return StyleConstants.getFontSize(attr);
    }

    /**
     * @return the color of the text at the selection start (or before the caret), or
     *         null if it is not part of the text color palette
     */
    private Color getPaletteColorAtCaret() {
        int start = textPane.getSelectionStart();
        int pos = start != textPane.getSelectionEnd() ? start : Math.max(0, start - 1);
        // Read from the document: input attributes are refreshed after caret listeners
        AttributeSet attr = textPane.getStyledDocument().getCharacterElement(pos).getAttributes();
        Color color = ColorChoice.DEFAULT_TEXT_COLOR;
        if (attr.isDefined(StyleConstants.Foreground)) {
            color = StyleConstants.getForeground(attr);
        }
        return ColorChoice.TEXT_COLOR_LIST.contains(color) ? color : null;
    }

    /**
     * The document holds the real colors of the diagram, as on a white page. A dark theme adapts them when painting, like on the
     * diagram canvas, so the colors of this pane are given as on this white page too.
     *
     * @return the text area
     */
    private static JTextPane createTextPane() {
        JTextPane pane = new JTextPane() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(FlatLaf.isLafDark() ? new DarkDiagramGraphics2D((Graphics2D) g) : g);
            }
        };
        pane.setBackground(Color.WHITE);
        pane.setCaretColor(Color.BLACK);
        if (FlatLaf.isLafDark()) {
            pane.setSelectionColor(DARK_THEME_SELECTION_COLOR);
            pane.setSelectedTextColor(Color.BLACK);
        }
        return pane;
    }

    private static final Color DARK_THEME_SELECTION_COLOR = new Color(190, 170, 240);

    @ResourceBundleBean(key = "editor.bold.tooltip")
    private String boldTooltip;

    @ResourceBundleBean(key = "editor.italic.tooltip")
    private String italicTooltip;

    @ResourceBundleBean(key = "editor.underline.tooltip")
    private String underlineTooltip;

    @ResourceBundleBean(key = "editor.align_left.tooltip")
    private String alignLeftTooltip;

    @ResourceBundleBean(key = "editor.align_center.tooltip")
    private String alignCenterTooltip;

    @ResourceBundleBean(key = "editor.align_right.tooltip")
    private String alignRightTooltip;

    @ResourceBundleBean(key = "editor.size.tooltip")
    private String sizeTooltip;

    @ResourceBundleBean(key = "editor.color.tooltip")
    private String colorTooltip;

    private MultiLineString source;
    private JPanel toolbar;
    private JTextPane textPane;
    private JToggleButton boldBtn;
    private JToggleButton italicBtn;
    private JToggleButton underlineBtn;
    private JToggleButton leftAlignBtn;
    private JToggleButton centerAlignBtn;
    private JToggleButton rightAlignBtn;
    private JButton sizeButton;
    private TextColorButton colorButton;
    private int currentFontSize = MultiLineString.FontSize.NORMAL;
    private int initialFontSize = MultiLineString.FontSize.NORMAL;
    private int currentAlignment = StyleConstants.ALIGN_CENTER;
    private boolean isUpdating = false;
    private boolean isUpdatingToolbar = false;

    /** The sizes HTML can write (font size 1 to 7): any other size picked would be saved as the nearest one */
    private static final int[] FONT_SIZES = { 8, 10, 12, 14, 18, 24, 36 };
    private static final int CONTROL_SIZE = 24;

    private static Set<KeyStroke> tab = new HashSet<KeyStroke>(1);
    private static Set<KeyStroke> shiftTab = new HashSet<KeyStroke>(1);
    static {
        tab.add(KeyStroke.getKeyStroke("TAB"));
        shiftTab.add(KeyStroke.getKeyStroke("shift TAB"));
    }
}
