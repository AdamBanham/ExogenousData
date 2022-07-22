package org.processmining.qut.exogenousaware.steps.transform.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;

import com.fluxicon.slickerbox.components.RoundedPanel;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class TransformSelectorFilterList {
	
	@NonNull @Getter TransformConfigurationDialog source;
	
	@Default List<DialogTransformSelector> visibles = new ArrayList();
	@Default GridBagConstraints c = new GridBagConstraints();
	
	@Getter private RoundedPanel panel;

	
	public TransformSelectorFilterList setup() {
		// create panel
		panel = new RoundedPanel(8,0,0);
		// style panel
		PanelStyler.StylePanel(panel, false);
		panel.setBackground(Color.LIGHT_GRAY);
		// setup layout of panel
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return this;
	}


	public void addDialog(DialogTransformSelector dialog) {
		visibles.add(dialog);	
		c.gridy++;
		panel.add(Box.createVerticalStrut(15));
		c.gridy++;
		dialog.addPropertyChangeListener("remove-parent", new RemovalListener(dialog, this));
		panel.add(dialog);
		panel.validate();
	}
	
	public static class RemovalListener implements PropertyChangeListener {
		
		private TransformSelectorFilterList controller;
		private DialogTransformSelector dialog;
		private JPanel panel;
		
		public RemovalListener(DialogTransformSelector dialog, TransformSelectorFilterList controller) {
			this.dialog = dialog;
			this.panel = controller.getPanel();
			this.controller = controller;
		}

		public void propertyChange(PropertyChangeEvent evt) {
			for(int i=0; i<this.panel.getComponentCount();i++) {
				if (this.panel.getComponent(i).equals(dialog)) {
					this.panel.remove(i-1);
				}
			}
			this.panel.remove(dialog);
			this.panel.validate();
			this.panel.repaint();
			this.controller.getSource().scroller.validate();
			this.controller.getSource().scroller.getViewport().validate();
		}
		
	}
}
