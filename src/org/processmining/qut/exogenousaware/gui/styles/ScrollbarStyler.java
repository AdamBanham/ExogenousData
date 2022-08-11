package org.processmining.qut.exogenousaware.gui.styles;

import java.awt.Color;

import javax.swing.JScrollBar;

public class ScrollbarStyler {

	private ScrollbarStyler() {};
	
	public static void styleScrollBar(JScrollBar bar) {
		bar.setBackground(Color.LIGHT_GRAY);
		bar.setForeground(Color.gray);
	}
}
