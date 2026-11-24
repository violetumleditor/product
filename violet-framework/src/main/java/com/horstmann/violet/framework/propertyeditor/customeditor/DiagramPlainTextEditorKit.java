package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.DiagramTextFonts;

/**
 * The editor kit of a plain text of a diagram (the label of an edge), shown as the diagram draws it, like
 * {@link DiagramTextEditorKit} does for a formatted one.
 */
public class DiagramPlainTextEditorKit extends StyledEditorKit {

    /**
     * @param textColor the color of the whole text
     */
    public DiagramPlainTextEditorKit(Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public Document createDefaultDocument() {
        final Color color = this.textColor;
        return new DefaultStyledDocument() {
            @Override
            public Font getFont(AttributeSet attributes) {
                return DiagramTextFonts.getInstance().getFont(false, false, MultiLineString.FontSize.NORMAL);
            }

            @Override
            public Color getForeground(AttributeSet attributes) {
                return color;
            }
        };
    }

    @Override
    public ViewFactory getViewFactory() {
        final ViewFactory standardFactory = super.getViewFactory();
        return new ViewFactory() {
            @Override
            public View create(Element element) {
                View view = standardFactory.create(element);
                if (view instanceof GlyphView) {
                    ((GlyphView) view).setGlyphPainter(new DiagramTextEditorKit.DiagramGlyphPainter());
                }
                return view;
            }
        };
    }

    private final Color textColor;
}
