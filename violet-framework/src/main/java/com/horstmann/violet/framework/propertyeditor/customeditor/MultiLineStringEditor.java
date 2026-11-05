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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyEditorSupport;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.CSS;

import com.formdev.flatlaf.FlatLaf;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
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
        final JPanel panel = new JPanel(new BorderLayout());

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

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.boldBtn = new JToggleButton(adaptToTheme(boldIcon));
        boldBtn.setFocusable(false);
        boldBtn.setSelected(false);
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
        toolbar.add(boldBtn);

        this.italicBtn = new JToggleButton(adaptToTheme(italicIcon));
        italicBtn.setFocusable(false);
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
        toolbar.add(italicBtn);

        this.underlineBtn = new JToggleButton(adaptToTheme(underlineIcon));
        underlineBtn.setFocusable(false);
        underlineBtn.setSelected(source.isUnderlined());
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
        toolbar.add(underlineBtn);

        this.leftAlignBtn = new JToggleButton(adaptToTheme(alignLeftIcon));
        leftAlignBtn.setFocusable(false);
        leftAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.LEFT);
        leftAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_LEFT);
                textPane.requestFocusInWindow();
            }
        });
        toolbar.add(leftAlignBtn);

        this.centerAlignBtn = new JToggleButton(adaptToTheme(alignCenterIcon));
        centerAlignBtn.setFocusable(false);
        centerAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.CENTER);
        centerAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_CENTER);
                textPane.requestFocusInWindow();
            }
        });
        toolbar.add(centerAlignBtn);

        this.rightAlignBtn = new JToggleButton(adaptToTheme(alignRightIcon));
        rightAlignBtn.setFocusable(false);
        rightAlignBtn.setSelected(source.getJustification() == MultiLineString.Justification.RIGHT);
        rightAlignBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyAlignment(StyleConstants.ALIGN_RIGHT);
                textPane.requestFocusInWindow();
            }
        });
        toolbar.add(rightAlignBtn);

        String[] sizes = { "10", "12", "14", "16", "18", "20", "24", "32" };
        this.sizeBox = new JComboBox<String>(sizes);
        sizeBox.setFocusable(false);

        // Map MultiLineString size constants to pixel values
        int initialSize = MultiLineString.FontSize.NORMAL; // Default
        if (source.getSize() == MultiLineString.FontSize.LARGE)
            initialSize = MultiLineString.FontSize.LARGE;
        if (source.getSize() == MultiLineString.FontSize.SMALL)
            initialSize = MultiLineString.FontSize.SMALL;

        // Track the intended font size ourselves (HTML document attributes are
        // CSS-scaled)
        this.currentFontSize = initialSize;


        // Determine initial alignment from source
        int initialAlignment = StyleConstants.ALIGN_CENTER;
        if (source.getJustification() == MultiLineString.Justification.LEFT)
            initialAlignment = StyleConstants.ALIGN_LEFT;
        if (source.getJustification() == MultiLineString.Justification.RIGHT)
            initialAlignment = StyleConstants.ALIGN_RIGHT;
        this.currentAlignment = initialAlignment;

        // Ensure the JComboBox reflects the size
        sizeBox.setSelectedItem(String.valueOf(initialSize));
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

        sizeBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isUpdatingToolbar && sizeBox.getSelectedItem() != null) {
                    int size = Integer.parseInt((String) sizeBox.getSelectedItem());
                    currentFontSize = size;
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setFontSize(sas, size);
                    setCharacterAttributes(sas);
                    textPane.requestFocusInWindow();
                }
            }
        });
        toolbar.add(sizeBox);

        this.colorBox = new JComboBox<Color>(ColorChoice.TEXT_COLOR_LIST.toArray(new Color[0]));
        colorBox.setFocusable(false);
        colorBox.setMaximumRowCount(colorBox.getItemCount());
        colorBox.setRenderer(new ColorSwatchRenderer());
        colorBox.setSelectedItem(getPaletteColorAtCaret());
        colorBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isUpdatingToolbar && colorBox.getSelectedItem() != null) {
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    StyleConstants.setForeground(sas, (Color) colorBox.getSelectedItem());
                    // Input attributes copied from HTML text hold the previous color as CSS
                    textPane.getInputAttributes().removeAttribute(CSS.Attribute.COLOR);
                    setCharacterAttributes(sas);
                    textPane.requestFocusInWindow();
                }
            }
        });
        toolbar.add(colorBox);

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

        panel.add(toolbar, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(textPane);

        // Ensure the editor has a 5-line height based on the default font size (12pt)
        Font fontForHeight = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        int fontHeight = textPane.getFontMetrics(fontForHeight).getHeight();
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 5 * fontHeight + 30)); // +30 for breathing room

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
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

            // Use tracked alignment and font size instead of reading from HTML document
            // (HTML paragraph/character attributes may have defaults that differ from
            // source)
            leftAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_LEFT);
            centerAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_CENTER);
            rightAlignBtn.setSelected(currentAlignment == StyleConstants.ALIGN_RIGHT);

            sizeBox.setSelectedItem(String.valueOf(currentFontSize));
            colorBox.setSelectedItem(getPaletteColorAtCaret());
        } finally {
            isUpdatingToolbar = false;
        }
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
     * Renders the colors of the text color drop-down as swatches.
     */
    private static class ColorSwatchRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            super.getListCellRendererComponent(list, "", index, isSelected, cellHasFocus);
            setIcon(value == null ? null : new ColorSwatchIcon((Color) value));
            return this;
        }
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

    /**
     * @param icon black icon
     * @return an icon readable on the buttons of the current look and feel
     */
    private static Icon adaptToTheme(ImageIcon icon) {
        if (!FlatLaf.isLafDark()) {
            return icon;
        }
        RGBImageFilter inversion = new RGBImageFilter() {
            @Override
            public int filterRGB(int x, int y, int argb) {
                return (argb & 0xFF000000) | (~argb & 0x00FFFFFF);
            }
        };
        return new ImageIcon(Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(icon.getImage().getSource(), inversion)));
    }

    private static final Color DARK_THEME_SELECTION_COLOR = new Color(190, 170, 240);

    private static class ColorSwatchIcon implements Icon {
        ColorSwatchIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, SIZE, SIZE);
            g.setColor(Color.GRAY);
            g.drawRect(x, y, SIZE - 1, SIZE - 1);
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        private static final int SIZE = 16;
        private final Color color;
    }

    @ResourceBundleBean(key = "editor.bold.icon")
    private ImageIcon boldIcon;

    @ResourceBundleBean(key = "editor.italic.icon")
    private ImageIcon italicIcon;

    @ResourceBundleBean(key = "editor.underline.icon")
    private ImageIcon underlineIcon;

    @ResourceBundleBean(key = "editor.align_left.icon")
    private ImageIcon alignLeftIcon;

    @ResourceBundleBean(key = "editor.align_center.icon")
    private ImageIcon alignCenterIcon;

    @ResourceBundleBean(key = "editor.align_right.icon")
    private ImageIcon alignRightIcon;

    private MultiLineString source;
    private JTextPane textPane;
    private JToggleButton boldBtn;
    private JToggleButton italicBtn;
    private JToggleButton underlineBtn;
    private JToggleButton leftAlignBtn;
    private JToggleButton centerAlignBtn;
    private JToggleButton rightAlignBtn;
    private JComboBox<String> sizeBox;
    private JComboBox<Color> colorBox;
    private int currentFontSize = MultiLineString.FontSize.NORMAL;
    private int currentAlignment = StyleConstants.ALIGN_CENTER;
    private boolean isUpdating = false;
    private boolean isUpdatingToolbar = false;

    private static Set<KeyStroke> tab = new HashSet<KeyStroke>(1);
    private static Set<KeyStroke> shiftTab = new HashSet<KeyStroke>(1);
    static {
        tab.add(KeyStroke.getKeyStroke("TAB"));
        shiftTab.add(KeyStroke.getKeyStroke("shift TAB"));
    }
}
