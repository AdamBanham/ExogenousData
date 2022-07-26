package org.processmining.qut.exogenousaware.steps.transform.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.processmining.framework.util.ui.widgets.ProMList;
import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.qut.exogenousaware.steps.determination.Determination;

import com.fluxicon.slickerbox.components.RoundedPanel;
import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import prefuse.data.query.ListModel;

@Builder
public class TransformConfigurationDialog extends JPanel {

	@NonNull List<Determination> partialDeterminations;
	
	@Default private Determination allDeter = new DummyDetermination(null, null, null, null);
	@Default private Map<Determination, List<DialogTransformSelector>> transformers = new HashMap();
	
	private ProMList<Determination> partials;
	public ProMScrollPane scroller;
	private TransformSelectorFilterList viewer;
	
	
	public TransformConfigurationDialog setup() {
		setBackground(Color.DARK_GRAY);
//		setup layout
		GridBagConstraints c = new GridBagConstraints();
		setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(15,0,0,0);
		c.anchor = c.LINE_START;
//		add a selectable list of determinations
		JLabel title = new JLabel("Partial Determinations:");
		add(title, c);
		c.gridy++;
		DefaultListModel<Determination> model = new DefaultListModel<Determination>();
		model.addElement(allDeter);
		transformers.put(allDeter, new ArrayList<DialogTransformSelector>());
		for(Determination deter: this.partialDeterminations) {
			model.addElement(deter);
			transformers.put(deter, new ArrayList<DialogTransformSelector>());
		}
		partials = new ProMList<Determination>("", model );
		partials.setSelectionMode(ListModel.SINGLE_SELECTION);
		partials.setSelectedIndex(0);
		partials.setPreferredSize(new Dimension(350,500));
		partials.addListSelectionListener(new FilterToPartialListener(this));
		c.insets = new Insets(15,0,15,0);
		add(partials, c);
		c.gridy= 0;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridx++;
//		add filterable transform point
//		add controls and label first
		c.weightx = 1.0;
		title = new JLabel("Transformers:");
		c.insets = new Insets(15,0,0,0);
		c.weighty = 0.0;
		add(title, c);
		c.gridx++;
		c.anchor = c.LINE_END;
		JButton adder = new JButton("Add Transformer");
		adder.setBackground(ColorUtils.darken(Color.GREEN, 100));
		adder.setForeground(Color.white);
		adder.addMouseListener(new CreateTransformListener(this));
		add(adder, c);
		c.anchor = c.LINE_START;
//		add filter panel
		c.gridy++;
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = c.BOTH;
		JPanel transformers = new RoundedPanel(20,10,0);
		transformers.setLayout(new BoxLayout(transformers, BoxLayout.Y_AXIS));
		transformers.setBackground(Color.DARK_GRAY);
		viewer = TransformSelectorFilterList.builder().source(this).build().setup();
		scroller = new ProMScrollPane(viewer.getPanel());
		scroller.getViewport().setBackground(Color.DARK_GRAY);
		transformers.add(scroller);
		add(transformers, c);
		return this;
	}
	
	public Determination getSelectedPartial() {
		if (this.partials.getSelectedValuesList().size() > 0) {
			return this.partials.getSelectedValuesList().get(0);
		}
		return null;
	}
	
	public void addDialog(Determination partial, DialogTransformSelector dialog) {
		this.transformers.get(partial).add(dialog);
	}
	
	public void removeDialog(Determination partial, DialogTransformSelector dialog) {
		this.transformers.get(partial).remove(dialog);
	}
	
	public void filterToPartial(Determination partial) {
		this.viewer.filterToPartial(partial);
	}
	
	public List<Determination> generateDeterminations(){
		List<Determination> deters = new ArrayList<>();
		
		// build all generic determinations		
		for(DialogTransformSelector selector: this.transformers.get(this.allDeter)) {
			for(Determination partial: this.partialDeterminations) {
				Determination complete = Determination.builder()
						.panel(partial.getPanel())
						.linker(partial.getLinker())
						.slicer(partial.getSlicer())
						.transformer(selector.createTransformer())
						.build();
				deters.add(complete);
			}
		}
		
		// build all targeted determinations		
		for (Determination partial: this.partialDeterminations) {
			for (DialogTransformSelector selector: this.transformers.get(partial)) {
				Determination complete = Determination.builder() 
						.panel(partial.getPanel())
						.linker(partial.getLinker())
						.slicer(partial.getSlicer())
						.transformer(selector.createTransformer())
						.build();
				deters.add(complete);
			}
		}
		
		return deters;
	}
	
	private static class FilterToPartialListener implements ListSelectionListener {
		
		private TransformConfigurationDialog controller;
		
		public FilterToPartialListener(TransformConfigurationDialog controller) {
			this.controller = controller;
		}

		public void valueChanged(ListSelectionEvent e) {
			Determination partial = this.controller.getSelectedPartial();
			this.controller.filterToPartial(partial);
		}
		
	}
	
	private static class CreateTransformListener implements MouseListener {
		
		private TransformConfigurationDialog controller;
		
		public CreateTransformListener(TransformConfigurationDialog controller) {
			this.controller = controller;
		}

		public void mouseClicked(MouseEvent e) {
			Determination partial = this.controller.getSelectedPartial();
			
			if (partial != null) {
				DialogTransformSelector dialog = DialogTransformSelector.builder()
						.partial(partial)
						.build()
						.setup();
				
				this.controller.addDialog(partial, dialog);
				
				//	add transform dialog
				this.controller.viewer.addDialog(
					dialog
				);
			}
			this.controller.scroller.validate();
		}

		public void mousePressed(MouseEvent e) {
		}

		public void mouseReleased(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}
}
