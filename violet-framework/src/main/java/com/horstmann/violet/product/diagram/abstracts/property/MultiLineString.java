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
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.List;
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
	 * Sets the value of the justification property.
	 * 
	 * @param newValue
	 *            the justification, one of LEFT, CENTER, RIGHT
	 */
	public void setJustification(int newValue) {
		readHtmlStyle();
		defaultJustification = newValue;
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
				+ (defaultUnderlined ? "text-decoration:underline;" : "");
	}

	/**
	 * Writes the style of the text as a whole (alignment, default size,
	 * underline) into its HTML, which is all that gets saved: on its first
	 * block, or on its body when the text pane of the editor wrote no paragraph
	 * yet. An HTML that already tells this style is left as it is.
	 */
	private void writeHtmlStyle() {
		if (!startsWithHtml(text)) {
			return;
		}
		HtmlTextParser html = parseHtml();
		boolean isWritten = html.getJustification() != null && html.getJustification() == defaultJustification
				&& html.getDefaultSize() != null && html.getDefaultSize() == defaultSize
				&& html.isUnderlined() == defaultUnderlined;
		if (isWritten) {
			return;
		}
		boolean hasBlock = Pattern.compile("(?i)<(p|div)\\b").matcher(text).find();
		Matcher tag = Pattern.compile("(?i)<(p|div|body)\\b([^>]*)>").matcher(text);
		StringBuilder result = new StringBuilder();
		boolean isDone = false;
		while (tag.find()) {
			String attributes = removeBlockStyle(tag.group(2));
			boolean isBody = "body".equalsIgnoreCase(tag.group(1));
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
	 * @return these attributes without alignment, font size nor underline
	 */
	private String removeBlockStyle(String attributes) {
		return attributes.replaceAll("(?i)\\s+align\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "")
				.replaceAll("(?i)(text-align|font-size|text-decoration)\\s*:[^;\"']*;?\\s*", "");
	}

	/**
	 * Gets the text for an editor that sets the alignment, the size and the
	 * underline itself: the text pane writes back what it has read on a
	 * paragraph, whatever is changed later.
	 * 
	 * @return the HTML of the text without its style as a whole
	 */
	public String getTextWithoutBlockStyle() {
		Matcher tag = Pattern.compile("(?i)<(p|div|body)\\b([^>]*)>").matcher(text);
		StringBuilder result = new StringBuilder();
		while (tag.find()) {
			tag.appendReplacement(result, Matcher.quoteReplacement("<" + tag.group(1) + removeBlockStyle(tag.group(2)) + ">"));
		}
		tag.appendTail(result);
		return result.toString();
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
	 * Draws this multiline string inside a given rectangle
	 * 
	 * @param g2
	 *            the graphics context
	 * @param r
	 *            the rectangle into which to place this multiline string
	 */
	public void draw(Graphics2D g2, Rectangle2D r) {
		readHtmlStyle();
		getLayout().draw(g2, getTextArea(r), defaultJustification);
	}

	/**
	 * Keeps a left or right aligned text off the sides of the rectangle, which
	 * are often the border of a node. The margin only takes the room left by the
	 * text, so the bounds and the sizes of the nodes don't change: a text as wide
	 * as its rectangle stays as it was.
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
			List<List<TextRun>> lines = new HtmlTextParser().parse(html);
			// No size after a Java deserialization, which skips the transient fields
			int size = defaultSize > 0 ? defaultSize : FontSize.NORMAL;
			this.layout = new StyledTextLayout(lines, size, defaultUnderlined);
		}
		return this.layout;
	}

	/**
	 * Forgets the drawing of the text, to compute when needed again.
	 */
	private void invalidateLayout() {
		this.layout = null;
		this.bounds = null;
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

	/** Room kept between a left or right aligned text and its rectangle's side */
	private static final double HORIZONTAL_MARGIN = 6;

	private String text;
	private transient int defaultJustification;
	private transient int defaultSize;
	private transient boolean defaultUnderlined;
	private transient StyledTextLayout layout;
	/** The text whose HTML tells the style held by the fields above */
	private transient String textOfStyle;
	private transient Rectangle2D bounds;
}
