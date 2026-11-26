package com.horstmann.violet.application.cheerpj;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/**
 * 16x16 trash can drawn with the component foreground color, so it follows the theme and the selection
 */
class TrashIcon implements Icon {

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(c.getForeground());
		g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.translate(x, y);
		g2.drawLine(2, 4, 14, 4);                  // lid
		g2.drawRect(6, 2, 4, 2);                   // handle
		Path2D can = new Path2D.Float();
		can.moveTo(3.5f, 4.5f);
		can.lineTo(4.5f, 14.5f);
		can.lineTo(11.5f, 14.5f);
		can.lineTo(12.5f, 4.5f);
		g2.draw(can);
		g2.drawLine(6, 7, 6, 12);                  // ribs
		g2.drawLine(8, 7, 8, 12);
		g2.drawLine(10, 7, 10, 12);
		g2.dispose();
	}

	@Override
	public int getIconWidth() {
		return 16;
	}

	@Override
	public int getIconHeight() {
		return 16;
	}
}
