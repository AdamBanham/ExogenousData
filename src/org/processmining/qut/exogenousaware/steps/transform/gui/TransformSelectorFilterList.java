package org.processmining.qut.exogenousaware.steps.transform.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;
import org.processmining.qut.exogenousaware.steps.determination.Determination;

import com.fluxicon.slickerbox.components.RoundedPanel;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class TransformSelectorFilterList {
	
	@NonNull @Getter TransformConfigurationDialog source;
	
	@Default List<DialogHolder> visibles = new ArrayList();
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
		DialogHolder holder = DialogHolder.builder()
				.dialog(dialog)
				.spacer(Box.createVerticalStrut(15))
				.build();
		visibles.add(holder);	
		c.gridy++;
		panel.add(holder.getSpacer());
		c.gridy++;
		dialog.addPropertyChangeListener("remove-parent", new RemovalListener(holder, this));
		panel.add(holder.getDialog());
		panel.validate();
	}
	
	public void filterToPartial(Determination partial) {
		for (DialogHolder visible: visibles) {
			visible.hide(visible.checkPartial(partial));
		}
	}
	
	@Builder
	@Data
	private static class DialogHolder {
		
		private DialogTransformSelector dialog;
		private Component spacer;
		
		public Boolean checkPartial(Determination partial) {
			if (this.dialog.getPartial() instanceof DummyDetermination) {
				return true;
			} else {
				return this.dialog.getPartial().equals(partial);
			}
			
		}
		
		public void hide(Boolean hide) {
			this.dialog.setVisible(hide);
			this.spacer.setVisible(hide);
		}
		
		public void remove(JPanel parent) {
			parent.remove(this.dialog);
			parent.remove(this.spacer);
		}
	}
	
	public static class RemovalListener implements PropertyChangeListener {
		
		private TransformSelectorFilterList controller;
		private DialogHolder holder;
		private JPanel panel;
		
		public RemovalListener(DialogHolder holder, TransformSelectorFilterList controller) {
			this.holder = holder;
			this.panel = controller.getPanel();
			this.controller = controller;
		}

		public void propertyChange(PropertyChangeEvent evt) {
			this.holder.remove(this.panel);
			this.controller.source.removeDialog(this.holder.getDialog().getPartial(), this.holder.getDialog());
			this.panel.validate();
			this.panel.repaint();
			this.controller.getSource().scroller.validate();
			this.controller.getSource().scroller.getViewport().validate();
		}
		
	}
}
