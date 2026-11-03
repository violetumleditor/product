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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;


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
		} else {
			text = buildHtmlFromRawText(value);
		}
		setLabelText();
		isBoundsDirty = true;
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
		defaultJustification = newValue;
		setLabelText();
		isBoundsDirty = true;
	}

	/**
	 * Gets the value of the justification property.
	 * 
	 * @return the justification, one of LEFT, CENTER, RIGHT
	 */
	public int getJustification() {
		return defaultJustification;
	}

	/**
	 * Gets the value of the underlined property.
	 * 
	 * @return true if the text is underlined
	 */
	public boolean isUnderlined() {
		return defaultUnderlined;
	}

	/**
	 * Sets the value of the underlined property.
	 * 
	 * @param newValue
	 *            true to underline the text
	 */
	public void setUnderlined(boolean newValue) {
		defaultUnderlined = newValue;
		setLabelText();
		isBoundsDirty = true;
	}

	/**
	 * Sets the value of the size property.
	 * 
	 * @param newValue
	 *            the size, one of SMALL, NORMAL, LARGE
	 */
	public void setSize(int newValue) {
		defaultSize = newValue;
		setLabelText();
		isBoundsDirty = true;
	}

	/**
	 * Gets the value of the size property.
	 * 
	 * @return the size, one of SMALL, NORMAL, LARGE
	 */
	public int getSize() {
		return defaultSize;
	}

	public String toString() {
		return text;
	}

	private void setLabelText() {
		String alignStr = "center";
		if (defaultJustification == Justification.LEFT) alignStr = "left";
		else if (defaultJustification == Justification.RIGHT) alignStr = "right";
		String blockStyle = "margin:0;padding:0;text-align:" + alignStr + ";font-size:" + defaultSize + "pt;";

		String html = text;
		if (!startsWithHtml(html)) {
			html = buildHtmlFromRawText(html);
		} else {
			// Normalize editor HTML to one zero-margin block to avoid default body/p spacing.
			String content = html;
			content = content.replaceAll("(?is)<head>.*?</head>", "");
			content = content.replaceAll("(?i)^\\s*<html>\\s*", "");
			content = content.replaceAll("(?i)\\s*</html>\\s*$", "");
			content = content.replaceAll("(?i)^\\s*<body[^>]*>\\s*", "");
			content = content.replaceAll("(?i)\\s*</body>\\s*$", "");
			content = content.replaceAll("(?i)</p>\\s*<p[^>]*>", "<br>");
			content = content.replaceAll("(?i)</div>\\s*<div[^>]*>", "<br>");
			content = content.replaceAll("(?i)</?p(\\s[^>]*)?>", "");
			content = content.replaceAll("(?i)</?div(\\s[^>]*)?>", "");
			// Remove any persisted underline style from editor HTML; model flag controls underline.
			content = content.replaceAll("(?i)text-decoration\\s*:\\s*[^;\"']+;?", "");
			html = "<html><div style=\"" + blockStyle + "\">" + content + "</div></html>";
		}
		// Preserve leading/trailing spaces (HTML collapses whitespace at text-node boundaries)
		html = java.util.regex.Pattern.compile(">([ ]+)")
				.matcher(html)
				.replaceAll(m -> ">" + m.group(1).replace(" ", "&nbsp;"));
		html = java.util.regex.Pattern.compile("([ ]+)<")
				.matcher(html)
				.replaceAll(m -> m.group(1).replace(" ", "&nbsp;") + "<");
		// Preserve multiple consecutive spaces in the middle of text content:
		// keep the first space as a normal space (so line-wrap still works) and
		// replace every additional space in the run with &nbsp;
		html = java.util.regex.Pattern.compile(" {2,}")
				.matcher(html)
				.replaceAll(m -> {
					int n = m.group().length();
					StringBuilder sb = new StringBuilder(" ");
					for (int i = 1; i < n; i++) sb.append("&nbsp;");
					return sb.toString();
				});
		JLabel currentLabel = getLabel();
		Font currentFont = currentLabel.getFont().deriveFont((float) defaultSize);
		if (defaultUnderlined) {
			Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>(currentFont.getAttributes());
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			currentFont = currentFont.deriveFont(attributes);
		}
		currentLabel.setFont(currentFont);
		currentLabel.setText(html);
	}

	private boolean startsWithHtml(String value) {
		return value != null && value.toLowerCase().startsWith("<html>");
	}

	private String buildHtmlFromRawText(String raw) {
		String alignStr = "center";
		if (defaultJustification == Justification.LEFT) alignStr = "left";
		else if (defaultJustification == Justification.RIGHT) alignStr = "right";
		String blockStyle = "margin:0;padding:0;text-align:" + alignStr + ";font-size:" + defaultSize + "pt;";
		String escaped = raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\r\n", "<br>").replace("\n", "<br>").replace("\r", "<br>");
		return "<html><div style=\"" + blockStyle + "\">" + escaped + "</div></html>";
	}

	/**
	 * Gets the bounding rectangle for this multiline string.
	 * 
	 * @param g2
	 *            the graphics context
	 * @return the bounding rectangle (with top left corner (0,0))
	 */
	private Rectangle2D getBounds(Graphics2D g2) {
		setLabelText();
		if (text.length() == 0)
			return new Rectangle2D.Double(0, 0, 0, 0);
		Dimension dim = getLabel().getPreferredSize();
		return new Rectangle2D.Double(0, 0, dim.getWidth(), dim.getHeight());
	}

	/**
	 * Gets the bounding rectangle for this multiline string.
	 * 
	 * @return the bounding rectangle (with top left corner (0,0))
	 */
	public Rectangle2D getBounds() {
		if (this.isBoundsDirty || this.bounds == null) {
			BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D) image.getGraphics();
			this.bounds = getBounds(g2);
			this.isBoundsDirty = false;
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
		// Before sizing the view: a foreground change makes the label rebuild its HTML view
		getLabel().setForeground(g2.getColor());
		getLabel().setBounds(0, 0, (int) r.getWidth(), (int) r.getHeight());
		Object viewObj = getLabel().getClientProperty(BasicHTML.propertyKey);
		if (viewObj instanceof View) {
			((View) viewObj).setSize((int) r.getWidth(), (int) r.getHeight());
		}
		g2.translate(r.getX(), r.getY());
		getLabel().paint(g2);
		g2.translate(-r.getX(), -r.getY());
	}

	public MultiLineString clone() {
		MultiLineString cloned = new MultiLineString();
		cloned.text = text;
		cloned.defaultJustification = defaultJustification;
		cloned.defaultSize = defaultSize;
		cloned.defaultUnderlined = defaultUnderlined;
		cloned.setLabelText();
		return cloned;
	}

	private JLabel getLabel() {
		if (this.label == null) {
			this.label = new JLabel();
		}
		return this.label;
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

	private String text;
	private transient int defaultJustification;
	private transient int defaultSize;
	private transient boolean defaultUnderlined;
	private transient JLabel label;
	private transient boolean isBoundsDirty = true;
	private transient Rectangle2D bounds;
}
