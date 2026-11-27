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

package com.horstmann.violet.product.diagram.abstracts.property;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.horstmann.violet.product.diagram.abstracts.property.text.HtmlTextParser;
import com.horstmann.violet.product.diagram.abstracts.property.text.StyledTextLayout;
import com.horstmann.violet.product.diagram.abstracts.property.text.TextRun;


/**
 * A string that can extend over multiple lines.
 */
public class MultiLineString implements Serializable, Cloneable {
	/**
	 * Constructs an empty, centered, normal size multiline string that is not
	 * underlined.
	 */
	public MultiLineString() {
		text = "";
		defaultJustification = Justification.CENTER;
		defaultSize = FontSize.NORMAL;
		defaultUnderlined = false;
		defaultBold = false;
	}

	/**
	 * Sets the value of the text property.
	 * 
	 * @param newValue
	 *            the text of the multiline string
	 */
	public void setText(String newValue) {
		String value = newValue == null ? "" : newValue;
		if (startsWithHtml(value)) {
			text = value;
			readHtmlStyle();
			writeHtmlStyle();
		} else {
			readHtmlStyle();
			text = buildHtmlFromRawText(value);
		}
		this.textOfStyle = text;
		invalidateLayout();
	}

	/**
	 * Takes the style of the text as a whole (alignment, default size,
	 * underline) from its HTML, if the text is not the one this style was
	 * read from or written to. The HTML is all that gets saved, and a file is
	 * read into a new string whose text field is set directly: without this,
	 * a reloaded text would be centered, of normal size and not underlined,
	 * whatever it was. An HTML that doesn't tell a style leaves it as it is.
	 */
	private void readHtmlStyle() {
		if (text == this.textOfStyle) {
			return;
		}
		this.textOfStyle = text;
		invalidateLayout();
		if (!startsWithHtml(text)) {
			return;
		}
		HtmlTextParser html = parseHtml();
		if (html.getJustification() != null) {
			defaultJustification = html.getJustification();
		}
		if (html.getDefaultSize() != null) {
			defaultSize = Math.round(html.getDefaultSize());
		}
		if (html.isUnderlined()) {
			defaultUnderlined = true;
		}
		if (html.isBold()) {
			defaultBold = true;
		}
	}

	/**
	 * Gets the value of the text property.
	 * 
	 * @return the text of the multiline string
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the value of the justification property: the alignment of every
	 * line, even one that had its own.
	 * 
	 * @param newValue
	 *            the justification, one of LEFT, CENTER, RIGHT
	 */
	public void setJustification(int newValue) {
		readHtmlStyle();
		defaultJustification = newValue;
		if (startsWithHtml(text)) {
			text = replaceBlockAttributes((name, attributes) -> removeAlignment(attributes));
		}
		writeHtmlStyle();
		invalidateLayout();
	}

	private HtmlTextParser parseHtml() {
		HtmlTextParser parser = new HtmlTextParser();
		parser.parse(text);
		return parser;
	}

	/**
	 * @return the style of the text as a whole, as CSS declarations
	 */
	private String getBlockStyle() {
		String alignment = "center";
		if (defaultJustification == Justification.LEFT) alignment = "left";
		else if (defaultJustification == Justification.RIGHT) alignment = "right";
		return "text-align:" + alignment + ";font-size:" + defaultSize + "pt;"
				+ (defaultUnderlined ? "text-decoration:underline;" : "")
				+ (defaultBold ? "font-weight:bold;" : "");
	}

	/**
	 * Writes the style of the text as a whole (alignment, default size,
	 * underline) into its HTML, which is all that gets saved: on its first
	 * block, or on its body when the text pane of the editor wrote no paragraph
	 * yet. The other blocks keep their alignment. An HTML that already tells this style is left as it is.
	 */
	private void writeHtmlStyle() {
		if (!startsWithHtml(text)) {
			return;
		}
		HtmlTextParser html = parseHtml();
		boolean isWritten = html.getJustification() != null && html.getJustification() == defaultJustification
				&& html.getDefaultSize() != null && html.getDefaultSize() == defaultSize
				&& html.isUnderlined() == defaultUnderlined && html.isBold() == defaultBold;
		if (isWritten) {
			return;
		}
		boolean hasBlock = Pattern.compile("(?i)<(p|div)\\b").matcher(text).find();
		Matcher tag = BLOCK_TAG.matcher(text);
		StringBuilder result = new StringBuilder();
		boolean isDone = false;
		while (tag.find()) {
			boolean isBody = "body".equalsIgnoreCase(tag.group(1));
			String attributes = removeSizeAndUnderline(tag.group(2));
			if (isBody || !isDone) {
				attributes = removeAlignment(attributes);
			}
			if (!isDone && (!isBody || !hasBlock)) {
				Matcher style = Pattern.compile("(?i)style\\s*=\\s*[\"']").matcher(attributes);
				if (style.find()) {
					attributes = attributes.substring(0, style.end()) + getBlockStyle() + attributes.substring(style.end());
				} else {
					attributes += " style=\"" + getBlockStyle() + "\"";
				}
				isDone = true;
			}
			tag.appendReplacement(result, Matcher.quoteReplacement("<" + tag.group(1) + attributes + ">"));
		}
		tag.appendTail(result);
		text = result.toString();
		this.textOfStyle = text;
	}

	/**
	 * @param attributes
	 *            of a block tag
	 * @return these attributes without alignment
	 */
	private String removeAlignment(String attributes) {
		return attributes.replaceAll("(?i)\\s+align\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "")
				.replaceAll("(?i)text-align\\s*:[^;\"']*;?\\s*", "");
	}

	/**
	 * @param attributes
	 *            of a block tag
	 * @return these attributes without font size nor underline
	 */
	private String removeSizeAndUnderline(String attributes) {
		return attributes.replaceAll("(?i)(font-size|text-decoration|font-weight)\\s*:[^;\"']*;?\\s*", "");
	}

	/**
	 * @param change
	 *            gives the new attributes of a block tag, from its name and its
	 *            attributes
	 * @return the HTML of the text with these attributes
	 */
	private String replaceBlockAttributes(BinaryOperator<String> change) {
		return BLOCK_TAG.matcher(text).replaceAll(tag -> Matcher
				.quoteReplacement("<" + tag.group(1) + change.apply(tag.group(1), tag.group(2)) + ">"));
	}

	/**
	 * Gets the text for an editor that sets the size and the underline itself:
	 * the text pane writes back what it has read on a paragraph, whatever is
	 * changed later. The paragraphs keep their alignment, as each may have its
	 * own.
	 * 
	 * @return the HTML of the text without its size and underline as a whole
	 */
	public String getTextWithoutBlockStyle() {
		return replaceBlockAttributes((name, attributes) -> {
			String result = removeSizeAndUnderline(attributes);
			return "body".equalsIgnoreCase(name) ? removeAlignment(result) : result;
		});
	}

	/**
	 * Gets the value of the justification property.
	 * 
	 * @return the justification, one of LEFT, CENTER, RIGHT
	 */
	public int getJustification() {
		readHtmlStyle();
		return defaultJustification;
	}

	/**
	 * Gets the value of the underlined property.
	 * 
	 * @return true if the text is underlined
	 */
	public boolean isUnderlined() {
		readHtmlStyle();
		return defaultUnderlined;
	}

	/**
	 * Sets the value of the underlined property.
	 * 
	 * @param newValue
	 *            true to underline the text
	 */
	public void setUnderlined(boolean newValue) {
		readHtmlStyle();
		defaultUnderlined = newValue;
		writeHtmlStyle();
		invalidateLayout();
	}

	/**
	 * Gets the value of the bold property: the whole text is bold, whatever
	 * the style of its characters.
	 * 
	 * @return true if the text is bold as a whole
	 */
	public boolean isBold() {
		readHtmlStyle();
		return defaultBold;
	}

	/**
	 * Sets the value of the bold property, saved in the HTML like the size
	 * and the underline.
	 * 
	 * @param newValue
	 *            true to make the whole text bold
	 */
	public void setBold(boolean newValue) {
		readHtmlStyle();
		defaultBold = newValue;
		writeHtmlStyle();
		invalidateLayout();
	}

	/**
	 * Sets the value of the size property.
	 * 
	 * @param newValue
	 *            the size, one of SMALL, NORMAL, LARGE
	 */
	public void setSize(int newValue) {
		readHtmlStyle();
		defaultSize = newValue;
		writeHtmlStyle();
		invalidateLayout();
	}

	/**
	 * Gets the value of the size property.
	 * 
	 * @return the size, one of SMALL, NORMAL, LARGE
	 */
	public int getSize() {
		readHtmlStyle();
		return defaultSize;
	}

	/**
	 * @return true if nothing of this string can be seen: no character, or spaces only
	 */
	public boolean isBlank() {
		readHtmlStyle();
		if (this.blank == null) {
			this.blank = computeBlank();
		}
		return this.blank;
	}

	private boolean computeBlank() {
		String html = startsWithHtml(text) ? text : buildHtmlFromRawText(text);
		for (List<TextRun> line : new HtmlTextParser().parse(html)) {
			for (TextRun run : line) {
				if (!run.text().replace((char) 0xA0, ' ').isBlank()) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @return the characters of this string without their style, its lines
	 *         separated by '\n'
	 */
	public String getPlainText() {
		String html = startsWithHtml(text) ? text : buildHtmlFromRawText(text);
		List<String> lines = new ArrayList<String>();
		for (List<TextRun> line : new HtmlTextParser().parse(html)) {
			StringBuilder characters = new StringBuilder();
			for (TextRun run : line) {
				characters.append(run.text());
			}
			lines.add(characters.toString());
		}
		return String.join("\n", lines);
	}

	public String toString() {
		return text;
	}

	private boolean startsWithHtml(String value) {
		return value != null && value.toLowerCase().startsWith("<html>");
	}

	private String buildHtmlFromRawText(String raw) {
		String blockStyle = "margin:0;padding:0;" + getBlockStyle();
		String escaped = raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
		return "<html><div style=\"" + blockStyle + "\">" + escaped + "</div></html>";
	}

	/**
	 * Gets the bounding rectangle for this multiline string.
	 * 
	 * @return the bounding rectangle (with top left corner (0,0))
	 */
	public Rectangle2D getBounds() {
		readHtmlStyle();
		if (this.bounds == null) {
			this.bounds = getLayout().getBounds();
		}
		return this.bounds;
	}

	/**
	 * Gets the room an element whose border surrounds this text must give it:
	 * every such element sizes itself with this rectangle, so that no text
	 * touches the border on its left and right sides.
	 * 
	 * @return the bounding rectangle widened by a margin on each side (with top
	 *         left corner (0,0)), empty for an empty text
	 */
	public Rectangle2D getBoundsWithMargins() {
		Rectangle2D textBounds = getBounds();
		if (textBounds.isEmpty()) {
			return textBounds;
		}
		return new Rectangle2D.Double(0, 0, textBounds.getWidth() + 2 * HORIZONTAL_MARGIN, textBounds.getHeight());
	}

	/**
	 * Draws this multiline string inside a given rectangle
	 * 
	 * @param g2
	 *            the graphics context
	 * @param r
	 *            the rectangle into which to place this multiline string
	 */
	public void draw(Graphics2D g2, Rectangle2D r) {
		readHtmlStyle();
		this.drawnArea = g2.getTransform().createTransformedShape(r).getBounds2D();
		this.drawnTextArea = computeDrawnTextArea(r);
		this.drawnTransform = g2.getTransform();
		if (this.hidden) {
			return;
		}
		getLayout().draw(g2, getTextArea(r), defaultJustification);
	}

	/**
	 * Hides this string while it is edited in place (EditTextInPlaceBehavior): the text box shows it, and both would mix. Its
	 * size still counts. Never saved nor cloned.
	 *
	 * @param isHidden true to draw nothing
	 */
	public void setHidden(boolean isHidden) {
		this.hidden = isHidden;
	}

	/**
	 * @return true while this string is edited in place, see {@link #setHidden(boolean)}
	 */
	public boolean isHidden() {
		return this.hidden;
	}

	/**
	 * Tells where this string was last drawn: the rectangle its node gave it (a whole compartment, even when the text is
	 * empty), in the device space of the graphics it was drawn on. Drawn with the graph on a graphics without transform, this
	 * is its place on the graph, whatever the translations of the parent nodes: the in-place text editor finds the text under
	 * the mouse this way (EditTextInPlaceBehavior).
	 *
	 * @return the area, or null if it was not drawn since {@link #forgetDrawnArea()}
	 */
	public Rectangle2D getDrawnArea() {
		return this.drawnArea;
	}

	/**
	 * Tells where the lines of this string were last drawn, inside its {@link #getDrawnArea()}: they are aligned in this width
	 * and take this height (the one of a line when the text is empty).
	 *
	 * @return the area, before the {@link #getDrawnTransform()}, or null if it was not drawn
	 */
	public Rectangle2D getDrawnTextArea() {
		return this.drawnTextArea;
	}

	/**
	 * @return how the place of {@link #getDrawnTextArea()} goes to the device space of the graphics it was drawn on: a
	 *         translation, or a rotation too for a vertical text; null if it was not drawn
	 */
	public AffineTransform getDrawnTransform() {
		return this.drawnTransform;
	}

	private Rectangle2D computeDrawnTextArea(Rectangle2D r) {
		double height = getBounds().getHeight();
		if (height == 0) {
			height = Math.ceil((defaultSize > 0 ? defaultSize : FontSize.NORMAL) * StyledTextLayout.LINE_HEIGHT);
		}
		// Placed as StyledTextLayout does
		Rectangle2D lines = getTextArea(r);
		double y = r.getY() + Math.round((r.getHeight() - height) / 2);
		return new Rectangle2D.Double(lines.getX(), y, lines.getWidth(), height);
	}

	/**
	 * Forgets where this string was drawn, before drawing again to find out where it is now
	 */
	public void forgetDrawnArea() {
		this.drawnArea = null;
		this.drawnTextArea = null;
		this.drawnTransform = null;
	}

	/**
	 * Keeps a left or right aligned text off the sides of the rectangle, which
	 * are often the border of a node. The margin only takes the room left by the
	 * text: the elements give it with {@link #getBoundsWithMargins()}, a text as
	 * wide as its rectangle has none.
	 *
	 * @param r
	 *            the rectangle given to draw the text
	 * @return the rectangle the lines are aligned in
	 */
	private Rectangle2D getTextArea(Rectangle2D r) {
		double room = r.getWidth() - getBounds().getWidth();
		double margin = Math.min(HORIZONTAL_MARGIN, Math.max(0, room / 2));
		return new Rectangle2D.Double(r.getX() + margin, r.getY(), r.getWidth() - 2 * margin, r.getHeight());
	}

	public MultiLineString clone() {
		readHtmlStyle();
		MultiLineString cloned = new MultiLineString();
		cloned.text = text;
		cloned.textOfStyle = text;
		cloned.defaultJustification = defaultJustification;
		cloned.defaultSize = defaultSize;
		cloned.defaultUnderlined = defaultUnderlined;
		cloned.defaultBold = defaultBold;
		return cloned;
	}

	/**
	 * The text is kept as HTML but drawn without any HTML component: the HTML is
	 * read as lines of styled text, laid out and turned into shapes once, until
	 * the text or its default style changes.
	 * 
	 * @return the drawing of the current text
	 */
	private StyledTextLayout getLayout() {
		if (this.layout == null) {
			// A text of an old file is not HTML, and was put into the field as it is
			String html = startsWithHtml(text) ? text : buildHtmlFromRawText(text);
			HtmlTextParser parser = new HtmlTextParser();
			List<List<TextRun>> lines = parser.parse(html);
			// No size after a Java deserialization, which skips the transient fields
			int size = defaultSize > 0 ? defaultSize : FontSize.NORMAL;
			this.layout = new StyledTextLayout(lines, parser.getLineJustifications(), size, defaultUnderlined, defaultBold);
		}
		return this.layout;
	}

	/**
	 * Forgets the drawing of the text, to compute when needed again.
	 */
	private void invalidateLayout() {
		this.layout = null;
		this.bounds = null;
		this.blank = null;
	}

	public interface Justification {
		public static final int LEFT = 0;
		public static final int CENTER = 1;
		public static final int RIGHT = 2;
	}

	public interface FontSize {

		public static final int SMALL = 10;
		public static final int NORMAL = 12;
		public static final int LARGE = 16;
	}

	/** A tag that may hold a style of the text as a whole: name, attributes */
	private static final Pattern BLOCK_TAG = Pattern.compile("(?i)<(p|div|body)\\b([^>]*)>");
	/** Room kept between a left or right aligned text and its rectangle's side */
	private static final double HORIZONTAL_MARGIN = 6;

	private String text;
	private transient int defaultJustification;
	private transient int defaultSize;
	private transient boolean defaultUnderlined;
	private transient boolean defaultBold;
	private transient StyledTextLayout layout;
	/** The text whose HTML tells the style held by the fields above */
	private transient String textOfStyle;
	private transient Rectangle2D bounds;
	/** Whether nothing can be seen, null until computed */
	private transient Boolean blank;
	/** Where this string was last drawn, see {@link #getDrawnArea()} */
	private transient Rectangle2D drawnArea;
	/** Where its lines were last drawn, see {@link #getDrawnTextArea()} */
	private transient Rectangle2D drawnTextArea;
	private transient AffineTransform drawnTransform;
	/** True while edited in place, see {@link #setHidden(boolean)} */
	private transient boolean hidden;
}
