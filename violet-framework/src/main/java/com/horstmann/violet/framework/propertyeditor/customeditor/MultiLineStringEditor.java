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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.UIScale;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.propertyeditor.ChoiceButtonGroup;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.DiagramFont;
import com.horstmann.violet.product.diagram.abstracts.property.text.DiagramTextFonts;
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
        this.textPane = new DiagramTextPane();

        // Shown as on the diagram: its font, its sizes (a point is a pixel), its line height
        textPane.setEditorKit(new DiagramTextEditorKit());

        // The default size of the string, in points: the size of the text that has none of its own, as on the diagram
        Color defaultColor = this.defaultTextColor;
        // No size after a Java deserialization, which skips the transient fields
        int defaultSize = source.getSize() > 0 ? source.getSize() : MultiLineString.FontSize.NORMAL;
        String bodyRule = "body { font-size: " + defaultSize + "pt; " +
                "color: " + String.format("#%06x", defaultColor.getRGB() & 0xFFFFFF) + "; }";
        javax.swing.text.html.StyleSheet styles = ((javax.swing.text.html.HTMLDocument) textPane.getDocument()).getStyleSheet();
        styles.addRule(bodyRule);
        // The diagram ignores the margins: a paragraph starts on the next line, like a line break
        styles.addRule("p { margin-top: 0; margin-bottom: 0; }");

        // Inside a div, Enter creates implied paragraphs which are written back without any tag: line breaks would be lost
        // Without the size and the underline it holds as a whole, set below from the source: the text pane writes back what it
        // has read on a paragraph, whatever its paragraphs are given later
        // Without the style sheet the text pane wrote into the text (an older one gave a body of 15 px): loaded after the rules
        // above, it would override them, while the diagram ignores it
        textPane.setText(source.getTextWithoutBlockStyle().replaceAll("(?i)<(/?)div\\b", "<$1p")
                .replaceAll("(?is)<style\\b.*?</style\\s*>", ""));

        textPane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, tab);
        textPane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, shiftTab);

        this.toolbar = new JPanel();
        this.toolbarLayout = new ToolbarWrapLayout();
        toolbar.setLayout(toolbarLayout);
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
                boolean isNowUnderline = !StyleConstants.isUnderline(attrs) && !source.isUnderlined();
                if (source.isUnderlined()) {
                    // A text underlined as a whole (object, lifeline): the underline goes into the text itself, so that
                    // the button changes the selection only
                    SimpleAttributeSet wholeText = new SimpleAttributeSet();
                    StyleConstants.setUnderline(wholeText, true);
                    StyledDocument doc = textPane.getStyledDocument();
                    doc.setCharacterAttributes(0, doc.getLength(), wholeText, false);
                    source.setUnderlined(false);
                }
                SimpleAttributeSet sas = new SimpleAttributeSet();
                StyleConstants.setUnderline(sas, isNowUnderline);
                setCharacterAttributes(sas);
                // Without selection the text doesn't change, but the string may have
                source.setText(getHtml());
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

        this.bulletBtn = new JToggleButton(TextFormatIcons.bullets());
        bulletBtn.setFocusable(false);
        bulletBtn.setToolTipText(bulletsTooltip);
        bulletBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleBullets();
                updateToolbarState();
                textPane.requestFocusInWindow();
            }
        });

        this.fontButton = new JButton(TextFormatIcons.chevron());
        fontButton.setFocusable(false);
        fontButton.setHorizontalTextPosition(SwingConstants.LEADING);
        fontButton.setIconTextGap(1);
        fontButton.setHorizontalAlignment(SwingConstants.LEADING);
        showFont(getFontAtCaret());
        fontButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFontMenu();
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

        sizeButton.setText(String.valueOf(initialSize));
        // No size given to the typed text: it takes the default size of the string, like the rest of it. Given, it would be
        // written as the nearest HTML size (14 for a 16 pt class name)
        javax.swing.text.MutableAttributeSet inputAttrs = textPane.getInputAttributes();
        // Seed underline state so newly typed text inherits the source default
        StyleConstants.setUnderline(inputAttrs, source.isUnderlined());

        initParagraphAlignments();

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

        continueBulletsOnEnter();

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
                    source.setText(getHtml());
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
        this.scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(null);

        // Ensure the editor has a 5-line height based on the default font size (12pt)
        Font fontForHeight = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        int fontHeight = textPane.getFontMetrics(fontForHeight).getHeight();
        scrollPane.setPreferredSize(new java.awt.Dimension(TEXT_WIDTH, 5 * fontHeight + 30)); // +30 for breathing room

        return new RichTextBox(toolbar, scrollPane, textPane);
    }

    /**
     * Lays out the toolbar: style, alignment, then font, size and color, separated by thin lines. All its controls are flat and take
     * the same height.
     */
    private void layoutToolbar() {
        toolbar.removeAll();
        AbstractButton[][] groups = {
                { boldBtn, italicBtn, underlineBtn },
                { leftAlignBtn, centerAlignBtn, rightAlignBtn, bulletBtn },
                { fontButton, sizeButton, colorButton } };
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
                if (button == fontButton) {
                    // Wide enough for every font
                    DiagramFont currentFont = this.shownFont;
                    for (DiagramFont font : DiagramFont.values()) {
                        showFont(font);
                        width = Math.max(width, fontButton.getPreferredSize().width);
                    }
                    showFont(currentFont);
                    width += UIScale.scale(2);
                }
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
     * Makes the editor fit in a narrow place (phone): its toolbar goes on several lines when needed, its text
     * area is lower, as its name takes a line above it.
     *
     * @param maxWidth of the editor and of its toolbar
     */
    public void setMaxWidth(int maxWidth) {
        toolbarLayout.setMaxWidth(maxWidth);
        toolbar.revalidate();
        Dimension size = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.min(TEXT_WIDTH, maxWidth), Math.min(size.height, NARROW_TEXT_HEIGHT)));
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
    public DiagramTextPane getTextComponent() {
        return textPane;
    }

    /**
     * @return the modifier of the menu shortcuts (Cmd on macOS, Ctrl elsewhere), Ctrl without a
     *         screen, where the toolkit throws a HeadlessException
     */
    private static int getMenuShortcutModifiers() {
        if (GraphicsEnvironment.isHeadless()) {
            return InputEvent.CTRL_DOWN_MASK;
        }
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    /**
     * @param tooltip of a button
     * @param key letter of its shortcut
     * @return the tooltip followed by the shortcut, as in the menus
     */
    private static String withShortcut(String tooltip, int key) {
        int modifiers = getMenuShortcutModifiers();
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
        int modifiers = getMenuShortcutModifiers();
        textPane.getInputMap().put(KeyStroke.getKeyStroke(key, modifiers), actionKey);
        textPane.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                button.doClick(0);
            }
        });
    }

    /**
     * Opens the list of fonts under the font button, each name in its font, the current one checked.
     */
    private void showFontMenu() {
        JPopupMenu menu = new JPopupMenu();
        ButtonGroup group = new ButtonGroup();
        for (final DiagramFont font : DiagramFont.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(getFontName(font), font == shownFont);
            item.setFont(getUIFont(font, item.getFont().getSize2D() + 2));
            item.setToolTipText(font.getFamily());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyFont(font);
                }
            });
            group.add(item);
            menu.add(item);
        }
        menu.show(fontButton, 0, fontButton.getHeight() + 2);
    }

    /**
     * Gives a font to the selection, and to the text typed next.
     */
    private void applyFont(DiagramFont font) {
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setFontFamily(sas, font.getFamily());
        // Input attributes copied from HTML text hold the previous font as CSS
        textPane.getInputAttributes().removeAttribute(CSS.Attribute.FONT_FAMILY);
        setCharacterAttributes(sas);
        showFont(font);
        textPane.requestFocusInWindow();
    }

    /**
     * Shows a font as the current one.
     */
    private void showFont(DiagramFont font) {
        this.shownFont = font;
        // A sample rather than its name, which would widen the toolbar
        fontButton.setText(FONT_SAMPLE);
        fontButton.setToolTipText(fontTooltip + " (" + getFontName(font) + ")");
        fontButton.setFont(getUIFont(font, sizeButton == null ? fontButton.getFont().getSize2D()
                : sizeButton.getFont().getSize2D()));
    }

    private Font getUIFont(DiagramFont font, float size) {
        return DiagramTextFonts.getInstance().getFont(font, false, false, size);
    }

    private String getFontName(DiagramFont font) {
        switch (font) {
        case CONDENSED:
            return condensedFontName;
        case BOOK:
            return bookFontName;
        case TYPEWRITER:
            return typewriterFontName;
        case ROUNDED:
            return roundedFontName;
        case HANDWRITTEN:
            return handwrittenFontName;
        default:
            return standardFontName;
        }
    }

    /**
     * @return the font of the text at the selection start (or before the caret)
     */
    private DiagramFont getFontAtCaret() {
        int start = textPane.getSelectionStart();
        int pos = start != textPane.getSelectionEnd() ? start : Math.max(0, start - 1);
        return DiagramTextEditorKit.readFont(textPane.getStyledDocument().getCharacterElement(pos).getAttributes());
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
                    applySize(size);
                }
            });
            group.add(item);
            menu.add(item);
        }
        menu.show(sizeButton, 0, sizeButton.getHeight() + 2);
    }

    /**
     * Gives a size to the selection, and to the text typed next.
     */
    private void applySize(int size) {
        currentFontSize = size;
        sizeButton.setText(String.valueOf(size));
        SimpleAttributeSet sas = new SimpleAttributeSet();
        if (size <= LARGEST_HTML_SIZE) {
            StyleConstants.setFontSize(sas, size);
        } else {
            // Given as the others, it would become the largest HTML size
            ((HTMLDocument) textPane.getDocument()).getStyleSheet().addCSSAttribute(sas, CSS.Attribute.FONT_SIZE, size + "pt");
        }
        // The previous size may be held either way
        textPane.getInputAttributes().removeAttribute(CSS.Attribute.FONT_SIZE);
        textPane.getInputAttributes().removeAttribute(StyleConstants.FontSize);
        setCharacterAttributes(sas);
        textPane.requestFocusInWindow();
    }

    /**
     * @return the text to save. The text pane writes a size in points as &lt;font size="48pt"&gt;, which it can't read back: it
     *         is written as a style instead
     */
    private String getHtml() {
        return textPane.getText().replaceAll("(<font [^>]*?)size=\"([0-9]+pt)\"", "$1style=\"font-size:$2\"");
    }

    /**
     * A thin vertical line between groups of buttons, in the border color of the theme.
     */
    private static class ToolbarSeparator extends ToolbarWrapLayout.Separator {
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

    /**
     * Gives each paragraph the alignment it was saved with, else the one of the string, as an attribute the buttons can change:
     * the text pane writes back the alignment it has read in a tag, whatever the paragraph is given later.
     */
    private void initParagraphAlignments() {
        int defaultAlignment = StyleConstants.ALIGN_CENTER;
        if (source.getJustification() == MultiLineString.Justification.LEFT)
            defaultAlignment = StyleConstants.ALIGN_LEFT;
        if (source.getJustification() == MultiLineString.Justification.RIGHT)
            defaultAlignment = StyleConstants.ALIGN_RIGHT;
        StyledDocument doc = textPane.getStyledDocument();
        int position = 0;
        while (position <= doc.getLength()) {
            Element paragraph = doc.getParagraphElement(position);
            SimpleAttributeSet attributes = new SimpleAttributeSet(paragraph.getAttributes());
            attributes.removeAttribute(HTML.Attribute.ALIGN);
            attributes.removeAttribute(CSS.Attribute.TEXT_ALIGN);
            StyleConstants.setAlignment(attributes, getAlignment(paragraph, defaultAlignment));
            doc.setParagraphAttributes(paragraph.getStartOffset(), 0, attributes, true);
            position = Math.max(position + 1, paragraph.getEndOffset());
        }
    }

    /**
     * @param paragraph of the text area
     * @param defaultAlignment of a paragraph that has none
     * @return its alignment, one of those of {@link StyleConstants}
     */
    private int getAlignment(Element paragraph, int defaultAlignment) {
        Object value = paragraph.getAttributes().getAttribute(CSS.Attribute.TEXT_ALIGN);
        if (value == null) {
            value = paragraph.getAttributes().getAttribute(HTML.Attribute.ALIGN);
        }
        String alignment = value == null ? "" : value.toString().trim().toLowerCase();
        switch (alignment) {
        case "left":
            return StyleConstants.ALIGN_LEFT;
        case "center":
            return StyleConstants.ALIGN_CENTER;
        case "right":
            return StyleConstants.ALIGN_RIGHT;
        default:
            return defaultAlignment;
        }
    }

    /**
     * Aligns the lines the selection touches, or the line of the caret when nothing is selected.
     */
    private void applyAlignment(int alignment) {
        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        int start = getTextPosition(selectionStart);
        // A selection that stops at the start of a line doesn't touch this line
        int last = Math.max(start, selectionEnd - 1);
        StyledDocument doc = textPane.getStyledDocument();
        // An alignment is the one of a paragraph: the selected lines go in paragraphs of their own
        int lineEnd = getLineEnd(last);
        if (isLineBreak(lineEnd)) {
            replaceLineBreakByParagraph(lineEnd);
        }
        int previousBreak = getLineStart(start) - 1;
        if (previousBreak >= 0 && isLineBreak(previousBreak)) {
            replaceLineBreakByParagraph(previousBreak);
        }
        SimpleAttributeSet sas = new SimpleAttributeSet();
        StyleConstants.setAlignment(sas, alignment);
        doc.setParagraphAttributes(start, last - start, sas, false);
        textPane.select(selectionStart, selectionEnd);
        String html = getHtml();
        source.setText(html);
        if (!Pattern.compile("(?i)<p\\b").matcher(html).find()) {
            // No paragraph written yet (empty text): the string keeps the alignment itself
            int[] justifications = { MultiLineString.Justification.LEFT, MultiLineString.Justification.CENTER,
                    MultiLineString.Justification.RIGHT };
            source.setJustification(justifications[alignment]);
        }
        firePropertyChange();
    }

    /**
     * @param position in the text area
     * @return the start of its line, whether lines are separated by a line break or by a paragraph
     */
    private int getLineStart(int position) {
        int paragraphStart = textPane.getStyledDocument().getParagraphElement(position).getStartOffset();
        int start = position;
        while (start > paragraphStart && !isLineBreak(start - 1)) {
            start--;
        }
        return start;
    }

    /**
     * @param position in the text area
     * @return the position of the character that ends its line: a line break, or the end of the paragraph
     */
    private int getLineEnd(int position) {
        int paragraphEnd = textPane.getStyledDocument().getParagraphElement(position).getEndOffset() - 1;
        int end = position;
        while (end < paragraphEnd && !isLineBreak(end)) {
            end++;
        }
        return end;
    }

    /**
     * @param lineStart start of a line
     * @return true if this line starts with a bullet
     */
    private boolean hasBullet(int lineStart) {
        if (getLineEnd(lineStart) - lineStart < BULLET.length()) {
            return false;
        }
        try {
            // The space may be a plain one, in a text pasted or read from elsewhere
            return textPane.getStyledDocument().getText(lineStart, BULLET.length()).replace(' ', NO_BREAK_SPACE).equals(BULLET);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Puts a bullet at the start of each line the selection touches (the line of the caret when nothing is selected), or removes
     * them when all these lines have one. A bullet is made of characters of the text, so it needs nothing from the file format
     * nor from the drawing. Empty lines among several are left as they are.
     */
    private void toggleBullets() {
        int start = getTextPosition(textPane.getSelectionStart());
        // A selection that stops at the start of a line doesn't touch this line
        int last = Math.max(start, textPane.getSelectionEnd() - 1);
        int firstLine = getLineStart(start);
        // From the last one, so that the changes don't move those to come
        List<Integer> lineStarts = new ArrayList<Integer>();
        for (int line = getLineStart(last); line >= firstLine; line = getLineStart(line - 1)) {
            if (getLineEnd(line) > line) {
                lineStarts.add(line);
            }
            if (line == 0) {
                break;
            }
        }
        if (lineStarts.isEmpty()) {
            lineStarts.add(firstLine);
        }
        boolean isRemoval = true;
        for (int line : lineStarts) {
            isRemoval = isRemoval && hasBullet(line);
        }
        for (int line : lineStarts) {
            if (isRemoval) {
                removeBullet(line);
            } else if (!hasBullet(line)) {
                addBullet(line);
            }
        }
    }

    private void addBullet(int lineStart) {
        StyledDocument doc = textPane.getStyledDocument();
        // In the color and the size of the text that follows
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        if (getLineEnd(lineStart) > lineStart) {
            attributes.addAttributes(doc.getCharacterElement(lineStart).getAttributes());
        }
        attributes.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        try {
            doc.insertString(lineStart, BULLET, attributes);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void removeBullet(int lineStart) {
        try {
            textPane.getStyledDocument().remove(lineStart, BULLET.length());
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Makes Enter start the next line of a list with a bullet, and end the list on a line that holds nothing but its bullet.
     */
    private void continueBulletsOnEnter() {
        final Action insertBreak = textPane.getActionMap().get(DefaultEditorKit.insertBreakAction);
        textPane.getActionMap().put(DefaultEditorKit.insertBreakAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int caret = textPane.getSelectionStart();
                int lineStart = getLineStart(caret);
                boolean isInListItem = hasBullet(lineStart) && caret >= lineStart + BULLET.length();
                boolean isEmptyItem = isInListItem && caret == textPane.getSelectionEnd()
                        && getLineEnd(lineStart) == lineStart + BULLET.length();
                if (isEmptyItem) {
                    removeBullet(lineStart);
                    return;
                }
                insertBreak.actionPerformed(e);
                if (isInListItem) {
                    textPane.replaceSelection(BULLET);
                }
            }
        });
    }

    private boolean isLineBreak(int position) {
        AttributeSet attributes = textPane.getStyledDocument().getCharacterElement(position).getAttributes();
        return attributes.getAttribute(StyleConstants.NameAttribute) == HTML.Tag.BR;
    }

    /**
     * Ends the paragraph at a line break, as well as at those just before it: a line break that ends a paragraph isn't an empty
     * line. The length of the text doesn't change.
     *
     * @param position of the line break
     */
    private void replaceLineBreakByParagraph(int position) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet content = new SimpleAttributeSet();
        content.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        try {
            doc.remove(position, 1);
            doc.insertString(position, "\n", content);
        } catch (BadLocationException e) {
            throw new IllegalStateException(e);
        }
        if (position > 0 && isLineBreak(position - 1)) {
            replaceLineBreakByParagraph(position - 1);
        }
    }

    /**
     * @param position in the text area
     * @return this position, or the start of the text when it is before: the head of the HTML counts as a line which isn't shown
     */
    private int getTextPosition(int position) {
        Element paragraph = textPane.getStyledDocument().getParagraphElement(position);
        Element parent = paragraph.getParentElement();
        return parent != null && HTML.Tag.HEAD.toString().equals(parent.getName()) ? paragraph.getEndOffset() : position;
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
            underlineBtn.setSelected(StyleConstants.isUnderline(attr) || source.isUnderlined());

            // Every paragraph was given an alignment, see initParagraphAlignments
            int alignment = getAlignment(doc.getParagraphElement(getTextPosition(textPane.getSelectionStart())),
                    StyleConstants.ALIGN_CENTER);
            leftAlignBtn.setSelected(alignment == StyleConstants.ALIGN_LEFT);
            centerAlignBtn.setSelected(alignment == StyleConstants.ALIGN_CENTER);
            rightAlignBtn.setSelected(alignment == StyleConstants.ALIGN_RIGHT);

            bulletBtn.setSelected(hasBullet(getLineStart(getTextPosition(textPane.getSelectionStart()))));

            showFont(getFontAtCaret());
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
                return FONT_SIZES[Math.max(1, Math.min(HTML_SIZE_COUNT, index)) - 1];
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
     * @param color of the text that has none of its own, as on the diagram; to set before {@link #getCustomEditor()}
     */
    public void setDefaultTextColor(Color color) {
        this.defaultTextColor = color;
    }

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

    @ResourceBundleBean(key = "editor.bullets.tooltip")
    private String bulletsTooltip;

    @ResourceBundleBean(key = "editor.font.tooltip")
    private String fontTooltip;

    @ResourceBundleBean(key = "editor.font.standard")
    private String standardFontName;

    @ResourceBundleBean(key = "editor.font.condensed")
    private String condensedFontName;

    @ResourceBundleBean(key = "editor.font.book")
    private String bookFontName;

    @ResourceBundleBean(key = "editor.font.typewriter")
    private String typewriterFontName;

    @ResourceBundleBean(key = "editor.font.rounded")
    private String roundedFontName;

    @ResourceBundleBean(key = "editor.font.handwritten")
    private String handwrittenFontName;

    @ResourceBundleBean(key = "editor.size.tooltip")
    private String sizeTooltip;

    @ResourceBundleBean(key = "editor.color.tooltip")
    private String colorTooltip;

    private MultiLineString source;
    private JPanel toolbar;
    private ToolbarWrapLayout toolbarLayout;
    private JScrollPane scrollPane;
    private Color defaultTextColor = ColorChoice.DEFAULT_TEXT_COLOR;
    private DiagramTextPane textPane;
    private JToggleButton boldBtn;
    private JToggleButton italicBtn;
    private JToggleButton underlineBtn;
    private JToggleButton leftAlignBtn;
    private JToggleButton centerAlignBtn;
    private JToggleButton rightAlignBtn;
    private JToggleButton bulletBtn;
    private JButton fontButton;
    private DiagramFont shownFont;
    private JButton sizeButton;
    private TextColorButton colorButton;
    private int currentFontSize = MultiLineString.FontSize.NORMAL;
    private int initialFontSize = MultiLineString.FontSize.NORMAL;
    private boolean isUpdating = false;
    private boolean isUpdatingToolbar = false;

    /** First the sizes HTML can write (font size 1 to 7), then larger ones, saved in points */
    private static final int[] FONT_SIZES = { 8, 10, 12, 14, 18, 24, 36, 48, 64, 72 };
    private static final int HTML_SIZE_COUNT = 7;
    private static final int LARGEST_HTML_SIZE = FONT_SIZES[HTML_SIZE_COUNT - 1];
    private static final char NO_BREAK_SPACE = '\u00A0';
    /** The start of a line of a list: the HTML would lose a plain space at the end of a line */
    private static final String BULLET = "\u2022" + NO_BREAK_SPACE;
    private static final int CONTROL_SIZE = 24;
    private static final int TEXT_WIDTH = 300;
    private static final int NARROW_TEXT_HEIGHT = 82;
    private static final String FONT_SAMPLE = "Aa";

    private static Set<KeyStroke> tab = new HashSet<KeyStroke>(1);
    private static Set<KeyStroke> shiftTab = new HashSet<KeyStroke>(1);
    static {
        tab.add(KeyStroke.getKeyStroke("TAB"));
        shiftTab.add(KeyStroke.getKeyStroke("shift TAB"));
    }
}
