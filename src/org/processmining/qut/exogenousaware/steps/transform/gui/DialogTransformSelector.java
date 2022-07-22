package org.processmining.qut.exogenousaware.steps.transform.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MaxTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MeanTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MedianTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MinTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.linear.SlopeTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.velocity.VelocityTransformer;

import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;

@Builder
public class DialogTransformSelector extends JPanel {
	
	@Default List<TransformChoice> Transformers = new ArrayList() {{
		for (TransformChoice choice: TransformChoice.values()) {
			add(choice);
		}
	}};

	
	public DialogTransformSelector setup() {
		//	style panel
		setBackground(Color.GRAY);
		//	setup layout and manager
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = c.FIRST_LINE_START;
		c.fill = c.NONE;
		c.weightx = 0.0;
		c.weighty = 0.0;
		c.insets = new Insets(5, 5, 5, 5);
		// add components
		// add transform choice
		JLabel choice = new JLabel("Transformer :");
		c.insets = new Insets(6, 5, 5, 5);
		add(choice, c);
		c.gridx++;
		ProMComboBox<TransformChoice> typeSelector = new ProMComboBox<TransformChoice>( new DefaultComboBoxModel() );
		typeSelector.addAllItems(Transformers);
		typeSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
//		typeSelector.addItemListener(new SlicerTypeListener(this));
		c.insets = new Insets(0,0, 0, 5);
		add(typeSelector, c);
		c.gridx++;
		
		
		// remove button
		JButton remove = new JButton("X");
		remove.setForeground(Color.white);
		remove.setBackground(ColorUtils.darken(Color.red, 150));
		remove.addMouseListener(new RemoveListener(this));
		c.insets = new Insets(2, 5, 2, 5);
		add(remove, c);
		c.gridx++;
		
		
//		 add filler 
		c.fill = c.HORIZONTAL;
		c.weightx = 1.0;
		add(Box.createHorizontalGlue(), c);
		// set preferred size
		setMaximumSize(new Dimension(600,75));
		setMinimumSize(getMaximumSize());
		setPreferredSize(getMaximumSize());
//		this.validate();
		return this;
	}
	
	
	static public enum TransformChoice {
		Slope("BestFit Slope", SlopeTransformer.class),
		AggMin("Minimum", MinTransformer.class),
		AggMax("Maximum", MaxTransformer.class),
		AggMean("Mean", MeanTransformer.class),
		AggMedian("Median", MedianTransformer.class),
		Velocity("Velocity", VelocityTransformer.class)
		;
		
		private String name;
		@Getter private Class<? extends Transformer> clazz;
		
		private TransformChoice(String name, Class<? extends Transformer> clazz) {
			this.name = name;
			this.clazz = clazz;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		
	}

	static private class RemoveListener implements MouseListener{
		
		private DialogTransformSelector source;
		
		public RemoveListener(DialogTransformSelector source) {
			this.source = source;
		}

		public void mouseClicked(MouseEvent e) {
			this.source.firePropertyChange("remove-parent", false, true);
		}

		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		
	}
}
