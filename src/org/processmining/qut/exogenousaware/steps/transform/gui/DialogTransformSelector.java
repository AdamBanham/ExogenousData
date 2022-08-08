package org.processmining.qut.exogenousaware.steps.transform.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.processmining.framework.util.ui.widgets.ProMComboBox;
import org.processmining.qut.exogenousaware.steps.determination.Determination;
import org.processmining.qut.exogenousaware.steps.transform.type.Transformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MaxTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MeanTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MedianTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.agg.MinTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.linear.SlopeTransformer;
import org.processmining.qut.exogenousaware.steps.transform.type.modeller.PolynomialCurveFitterModeller;
import org.processmining.qut.exogenousaware.steps.transform.type.velocity.VelocityTransformer;

import com.fluxicon.slickerbox.util.ColorUtils;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class DialogTransformSelector extends JPanel {
	
	@NonNull @Getter Determination partial;
	
	@Default List<TransformChoice> Transformers = new ArrayList() {{
		for (TransformChoice choice: TransformChoice.values()) {
			add(choice);
		}
	}};
	
	
	private ProMComboBox<TransformChoice> typeSelector;
	private ProMComboBox<TransformChoice> chainerChoices;
	private JLabel chainerLabel;
	private Color textColour;

	
	public DialogTransformSelector setup() {
		//	style panel
		if (this.partial instanceof DummyDetermination) {
			setBackground(Color.DARK_GRAY);
			this.textColour = Color.white;
		} else {
			setBackground(Color.GRAY);
			this.textColour = Color.black;
		}
		
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
		choice.setForeground(textColour);
		c.insets = new Insets(6, 5, 5, 5);
		add(choice, c);
		c.gridx++;
		typeSelector = new ProMComboBox<TransformChoice>( new DefaultComboBoxModel() );
		typeSelector.addAllItems(Transformers);
		typeSelector.setFont(new Font("Times New Roman", Font.BOLD, 9));
		typeSelector.addItemListener(new TransformChoiceListener(this));
		typeSelector.setPreferredSize(new Dimension(125,25));
		typeSelector.setMaximumSize(typeSelector.getPreferredSize());
		typeSelector.setMinimumSize(typeSelector.getPreferredSize());
		c.insets = new Insets(5,0, 0, 5);
		add(typeSelector, c);
		c.gridx++;
		// add a choice combo for chainers if its needed.
		chainerLabel = new JLabel("Next Transform :");
		chainerLabel.setForeground(textColour);
		c.insets = new Insets(6, 5, 5, 5);
		chainerLabel.setVisible(false);
		add(chainerLabel, c);
		c.gridx++;
		chainerChoices = new ProMComboBox<TransformChoice>( new DefaultComboBoxModel() );
		chainerChoices.addAllItems(Transformers.stream().filter(t -> !t.isRequiresChain()).collect(Collectors.toList()));
		chainerChoices.setFont(new Font("Times New Roman", Font.BOLD, 9));
		chainerChoices.setVisible(false);
		chainerChoices.setPreferredSize(new Dimension(125,25));
		chainerChoices.setMaximumSize(chainerChoices.getPreferredSize());
		chainerChoices.setMinimumSize(chainerChoices.getPreferredSize());
		c.insets = new Insets(5,0, 0, 5);
		add(chainerChoices, c);
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
		setMaximumSize(new Dimension(800,75));
		setMinimumSize(getMaximumSize());
		setPreferredSize(getMaximumSize());
//		this.validate();
		return this;
	}
	
	public void showChainer(boolean show) {
		this.chainerLabel.setVisible(show);
		this.chainerChoices.setVisible(show);
	}
	
	
	public Determination completeDetermination(Determination partial) {
		return Determination.builder()
			   .panel(partial.getPanel())
			   .linker(partial.getLinker())
			   .slicer(partial.getSlicer())
			   .transformer(createTransformer())
			   .build();
	}
	
	private Transformer getSimpleTransformer(Class<? extends Transformer> clazz) {
		try {
			return clazz.getConstructor().newInstance(new Object[0]);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private Transformer getChainerTransformer(Class<? extends Transformer> clazz, Object[] args) {
		try {
			return clazz.getConstructor(Transformer.class).newInstance(args);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	public Transformer createTransformer() {
		Transformer transform = null;
		TransformChoice choice = (TransformChoice) typeSelector.getSelectedItem();
		Class<? extends Transformer> transformChoice = choice.getClazz();
		
		if (choice.isRequiresChain()) {
			TransformChoice chainerChoice = (TransformChoice) chainerChoices.getSelectedItem();
			Transformer chainer = getSimpleTransformer(chainerChoice.getClazz());
			Object[] args = new Object[1];
			args[0] = chainer;
			transform = getChainerTransformer(transformChoice, args);
		} else {
			transform = getSimpleTransformer(transformChoice);
		}
		return transform;
	}


	static public enum TransformChoice {
		Slope("BestFit Slope", SlopeTransformer.class, false),
		AggMin("Minimum", MinTransformer.class, false),
		AggMax("Maximum", MaxTransformer.class, false),
		AggMean("Mean", MeanTransformer.class, false),
		AggMedian("Median", MedianTransformer.class, false),
		Velocity("Velocity", VelocityTransformer.class, true),
		PolyFitter("PolynomialFitter", PolynomialCurveFitterModeller.class, false)
		;
		
		@Getter private String name;
		@Getter private Class<? extends Transformer> clazz;
		@Getter private boolean requiresChain;
		
		private TransformChoice(String name, Class<? extends Transformer> clazz, boolean chain) {
			this.name = name;
			this.clazz = clazz;
			this.requiresChain = chain;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		
	}
	
	static private class TransformChoiceListener implements ItemListener{
		
		private DialogTransformSelector controller;
		
		public TransformChoiceListener(DialogTransformSelector controller) {
			this.controller = controller;
			
		}

		public void itemStateChanged(ItemEvent e) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				TransformChoice choice = (TransformChoice) e.getItem();
				boolean vis = choice.isRequiresChain();
				this.controller.showChainer(vis);
			}
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
