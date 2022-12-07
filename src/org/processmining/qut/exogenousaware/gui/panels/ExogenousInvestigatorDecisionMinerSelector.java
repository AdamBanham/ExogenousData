package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.processmining.framework.util.ui.widgets.ProMScrollPane;
import org.processmining.framework.util.ui.widgets.ProMScrollablePanel;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerConfiguration;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerConfiguration.MinerConfigurationBuilder;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerType;
import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;

import lombok.Builder;

@Builder
public class ExogenousInvestigatorDecisionMinerSelector extends JPanel {

//	required parameters
	
//	gui elements
	private InstanceThresholdParameter instanceThreshold;
	private ProMScrollPane content;
	private JPanel contentView;
	
	
	public ExogenousInvestigatorDecisionMinerSelector setup() {
//		make parameters
		instanceThreshold = new InstanceThresholdParameter();
//		setup content
		PanelStyler.StylePanel(this, true, BoxLayout.Y_AXIS);
		contentView = new ProMScrollablePanel();
		PanelStyler.StylePanel(contentView, true, BoxLayout.Y_AXIS);
		content = new ProMScrollPane(contentView);
		PanelStyler.StylePanel(content, false);
		add(content);
//		add parameters
		contentView.add(instanceThreshold);
		return this;
	}
	
	public MinerConfiguration makeConfig() {
		
		MinerConfigurationBuilder builder = MinerConfiguration.builder();
		instanceThreshold.addConfiguration(builder);
		
		return builder.build();
	}
	
	public void updateParameters(MinerType miner) {
		
	}
	
	private interface DecisionMinerParameter {
		
		public void addConfiguration(MinerConfigurationBuilder builder);
		
	}
	
	private abstract class BaseParameter extends JPanel implements DecisionMinerParameter {
		
		protected Font titleFont = new Font("Serif", Font.BOLD, 18);
		protected Font plainFont = new Font("Serif", Font.PLAIN, 14);
	}
	
	private enum InstanceMode {
		REL,
		ABS,
		BOTH;
	}
	
	private class InstanceThresholdParameter extends BaseParameter {
		
//		gui elements
		private GridBagConstraints c = new GridBagConstraints();
		private JRadioButton relativeMode = new JRadioButton("Fractional");
		private JRadioButton absoluteMode = new JRadioButton("Absolute");
		private JRadioButton thresholdMode = new JRadioButton("Both");
		private ButtonGroup modeGroup = new ButtonGroup();
		private JTextPane fractionalTextInput = new JTextPane();
		private JTextPane absoluteTextInput = new JTextPane();
		private JTextPane thresholdTextInput = new JTextPane(); 
		
//		internal states
		private InstanceMode mode = InstanceMode.REL;
		private double instanceLevel = 0.15;
		private double relativeInstanceLevel = 0.15;
		private double absoluteInstanceLevel = 25;
		private double instanceLevelSplitLeft;
		private double instanceLevelSplitRight;
		private double instanceLevelSplitMid;
		
		public InstanceThresholdParameter() {
//			set layout
			PanelStyler.StylePanel(this, false);
			setLayout(new GridBagLayout());
//			add title
			JLabel text = new JLabel("Minimun Number of Instance per Leaf:");
			text.setBackground(Color.DARK_GRAY);
			text.setFont(titleFont);
			c.weightx = 1.0;
			c.weighty = 0.05;
			c.insets = new Insets(0,10,5,0);
			c.gridx = 0;
			c.gridwidth = 10;
			c.anchor = GridBagConstraints.WEST;
			add(text, c);
//			add what the parameter does
			text = new JLabel("<html><body><p>"
					+ "This parameter limits how leafs are constructed "
					+ "in the decision tree. Leafs will not be added unless they "
					+ "are supported at least the selected amount of observations."
					+ "</p><p>"
					+ "When set too low, overfitting will occur, however when set"
					+ " too high, a decision tree may not be constructed at all. "
					+ "Futhermore, depending on the (un)balance of observations across "
					+ "choices in the model, both underfitting and overfitting can occur."
					+ "</p><p>"
					+ "Three options are available, a user can select a fractional "
					+ "percentage of observations per tree, "
					+ "an absolute number, or both; where fractional is used above "
					+ "a selected minimum number of instances and the absolute value "
					+ "when under the minimum amount."
					+ "</p></body></html>");
			text.setBackground(Color.DARK_GRAY);
			text.setFont(plainFont);
			c.gridy = 1;
			c.insets = new Insets(0,25,5,5);
			c.weightx = 1.0;
			c.gridwidth = 10;
			add(text, c);
//			add radio button for selecting mode controls
			text = new JLabel("Instance Mode:");
			text.setFont(plainFont);
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			add(text, c);
			c.gridx += 1;
			c.insets = new Insets(0,5,5,5);
			relativeMode.setSelected(true);
			relativeMode.setBackground(Color.LIGHT_GRAY);
			relativeMode.setActionCommand(InstanceMode.REL.name());
			relativeMode.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand() == InstanceMode.REL.name()) {
					mode = InstanceMode.REL;
					instanceLevel = relativeInstanceLevel;
					System.out.println("set to REL");
				}
			}
			});
			modeGroup.add(relativeMode);
			add(relativeMode, c);
			c.gridx += 1;
			absoluteMode.setBackground(Color.LIGHT_GRAY);
			absoluteMode.setActionCommand(InstanceMode.ABS.name());
			absoluteMode.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == InstanceMode.ABS.name()) {
						mode = InstanceMode.ABS;
						instanceLevel = absoluteInstanceLevel;
						System.out.println("set to ABS");
					}
				}
			});
			modeGroup.add(absoluteMode);
			add(absoluteMode, c);
			c.gridx += 1;
			thresholdMode.setBackground(Color.LIGHT_GRAY);
//			thresholdMode.addChangeListener(new ChangeListener() {
//				
//				public void stateChanged(ChangeEvent e) {
//					if (thresholdMode.isSelected()) {
//						mode = InstanceMode.BOTH;
//					}
//				}
//			});
			modeGroup.add(thresholdMode);
			add(thresholdMode, c);
//			add slide for fraction 
			c.gridy = 3;
			c.gridx = 0;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.insets = new Insets(0,25,5,5);
			text = new JLabel("Fractional (%):");
			text.setFont(plainFont);
			add(text,c);
			c.gridx += 1;
			c.insets = new Insets(5,5,5,5);
			fractionalTextInput.setMaximumSize(new Dimension(100,25));
			fractionalTextInput.setText(Double.toString(instanceLevel));
			fractionalTextInput.setBackground(Color.DARK_GRAY);
			fractionalTextInput.setForeground(Color.WHITE);
			fractionalTextInput.setPreferredSize(fractionalTextInput.getMaximumSize());
			((AbstractDocument)fractionalTextInput.getStyledDocument())
				.setDocumentFilter(new FractionalFilter());
			((AbstractDocument)fractionalTextInput.getStyledDocument())
				.addDocumentListener(new DocumentListener() {
					
					public void removeUpdate(DocumentEvent e) {
						handleInstanceLevel();
					}
					
					public void insertUpdate(DocumentEvent e) {
						handleInstanceLevel();
					}
					
					public void changedUpdate(DocumentEvent e) {
						handleInstanceLevel();
					}
					
					private void handleInstanceLevel() {
						if (mode == InstanceMode.REL) {
							relativeInstanceLevel = Double.parseDouble(
									fractionalTextInput.getText()
							);
						}
					}
				});
			add(fractionalTextInput, c);
			
		}
		
		private class FractionalFilter extends DocumentFilter {
			
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				// TODO Auto-generated method stub
				super.remove(fb, offset, length);
			}

			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
					throws BadLocationException {
				// TODO Auto-generated method stub
				System.out.println("offset::"+ offset +" len ::"+ length +" text::"+ text);
				String left;
				String right = "";
				if (offset == 0) {
					left = "";
				} else {
					left = fb.getDocument().getText(0,offset);
				}
				if (length == 0) {
					if (text.equals("d")) {
						Toolkit.getDefaultToolkit().beep();
						return;
					}
					if (fb.getDocument().getLength() > offset) {
						right = fb.getDocument().getText(offset, fb.getDocument().getLength());
					}
				}
				String next = left + text + right;
				int total = next.length();
				System.out.println("(replace) length :: "+total);
				try {
					double num = Double.parseDouble(next);
					if (total <= 5 && num < 1.0) {
						super.replace(fb, offset,length, text, attrs);
					} else  {
						Toolkit.getDefaultToolkit().beep();
					}
				} catch (Exception e) {
					Toolkit.getDefaultToolkit().beep();
				}
				
			}

			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
					throws BadLocationException {
				String next = fb.getDocument().getText(0, offset) + string;
				int length = next.length();
				System.out.println("(insert) length :: "+length);
				try {
					double num = Double.parseDouble(next);
					if (length <= 5 && num < 1.0) {
						super.insertString(fb, offset, string, attr);
					} else  {
						Toolkit.getDefaultToolkit().beep();
					}
				} catch (Exception e) {
					Toolkit.getDefaultToolkit().beep();
				}
			}
			
		}

		public void addConfiguration(MinerConfigurationBuilder builder) {
//			handle mode
			if (mode == InstanceMode.REL) {
				builder.relativeInstanceThreshold(true);
				builder.instanceThreshold(relativeInstanceLevel);
			} else if (mode == InstanceMode.ABS) {
				builder.relativeInstanceThreshold(false);
				builder.instanceThreshold(absoluteInstanceLevel);
			} else if (mode == InstanceMode.BOTH) {
				
			}
		}
		
	}
	
}