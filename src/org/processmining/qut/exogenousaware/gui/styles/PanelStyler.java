package org.processmining.qut.exogenousaware.gui.styles;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class PanelStyler {

	public static void StylePanel(JComponent comp) {
		StylePanel(comp, true);
	}
	
	public static void StylePanel(JComponent comp, Boolean addLayout) {
		comp.setBackground(Color.LIGHT_GRAY);
		comp.setBorder(BorderFactory.createEmptyBorder());
		if (addLayout) {
			comp.setLayout(new BoxLayout(comp, BoxLayout.Y_AXIS));
		}
		comp.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		comp.setAlignmentY(JPanel.CENTER_ALIGNMENT);
	}
	
	private PanelStyler() {};
}
