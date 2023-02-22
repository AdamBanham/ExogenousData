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
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerInstanceMode;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask.MinerType;
import org.processmining.qut.exogenousaware.gui.styles.PanelStyler;

import lombok.Builder;

@Builder
public class ExogenousInvestigatorDecisionMinerSelector extends JPanel {

//	required parameters
	
//	gui elements
	private InstanceThresholdParameter instanceThreshold;
	private PruneParameter prune;
	private CrossValidateParameter crossValidate;
	private ConfidenceLevelParameter confidence;
	private ExperimentalFeaturesParameter experimental;
	private ProMScrollPane content;
	private JPanel contentView;
	
	
	public ExogenousInvestigatorDecisionMinerSelector setup() {
//		make parameters
		instanceThreshold = new InstanceThresholdParameter();
		prune = new PruneParameter();
		crossValidate = new CrossValidateParameter();
		confidence = new ConfidenceLevelParameter();
		experimental = new ExperimentalFeaturesParameter();
//		setup content
		PanelStyler.StylePanel(this, true, BoxLayout.Y_AXIS);
		contentView = new ProMScrollablePanel();
		PanelStyler.StylePanel(contentView, true, BoxLayout.Y_AXIS);
		content = new ProMScrollPane(contentView);
		PanelStyler.StylePanel(content, false);
		add(content);
//		add parameters
		contentView.add(experimental);
		contentView.add(instanceThreshold);
		contentView.add(prune);
		contentView.add(crossValidate);
		contentView.add(confidence);
		return this;
	}
	
	public MinerConfiguration makeConfig() {
		
		MinerConfigurationBuilder builder = MinerConfiguration.builder();
		instanceThreshold.addConfiguration(builder);
		prune.addConfiguration(builder);
		crossValidate.addConfiguration(builder);
		confidence.addConfiguration(builder);
		experimental.addConfiguration(builder);
		
		return builder.build();
	}
	
	public void updateParameters(MinerType miner) {
		
	}
	
	private interface DecisionMinerParameter {
		
		public void addConfiguration(MinerConfigurationBuilder builder);
		
	}
	
	private abstract class BaseParameter extends JPanel implements DecisionMinerParameter {
		
		protected Font titleFont = new Font("Serif", Font.BOLD, 18);
		protected int titleLeft = 10;
		protected Font plainFont = new Font("Serif", Font.PLAIN, 14);
		protected int contentLeft = 25;
		protected GridBagConstraints c = new GridBagConstraints();
		
		protected void setup() {
//			set layout
			PanelStyler.StylePanel(this, false);
			setLayout(new GridBagLayout());
		}
		
		protected void makeTitle(String title, String parameterInfo) {
//			add title
			JLabel text = new JLabel(title);
			text.setBackground(Color.DARK_GRAY);
			text.setFont(titleFont);
			c.weightx = 1.0;
			c.weighty = 0.05;
			c.insets = new Insets(20,titleLeft,5,0);
			c.gridx = 0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 10;
			c.anchor = GridBagConstraints.WEST;
			add(text, c);
//			add what the parameter does
			text = new JLabel(parameterInfo);
			text.setBackground(Color.DARK_GRAY);
			text.setFont(plainFont);
			c.gridy = 1;
			c.insets = new Insets(0,contentLeft,5,5);
			c.weightx = 1.0;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 10;
			add(text, c);
			c.fill = GridBagConstraints.NONE;
		}
	}
	
	public enum InstanceMode {
		REL,
		ABS,
		BOTH;
	}
	
	public enum PruneMode {
		PRUNE,
		UNPRUNE
	}
	
	public enum CrossValidateMode {
		ENABLE,
		DISBALE
	}
	
	public enum ExperimentalFeatureMode {
		ENABLE,
		DISABLE
	}
	
	private class ExperimentalFeaturesParameter extends BaseParameter {
		
//		states
		private boolean enabled = false;
		private boolean dftFeatures = true;
		private boolean saxFeatures = true;
		private boolean edttsFeatures = false;
		
//		gui elements
		private JRadioButton enabledMode = new JRadioButton("Enable");
		private JRadioButton disableMode = new JRadioButton("Disable");
		private ButtonGroup modeGroup = new ButtonGroup();
		private JRadioButton addSAXFeatures = new JRadioButton("SAX Features");
		private JRadioButton addDFTFeatures = new JRadioButton("DFT Features");
		private JRadioButton addEDTTSFeatures = new JRadioButton("EDT-TS Features");
		
//		defaults/labels
		String title = "Experimental Time Series Features";
		String info = "<html><body><p>"
				+ "Enabling this mode will create experimental time series features "
				+ "within each observation for classification problems. This assumes "
				+ "that each observation has some slices attached to the associated "
				+ "event, and these have a numerical time series representation. </p> "
				+ "<p> Introduced features are the following:"
				+ "<ul> "
				+ "<li> SAX features (Boolean): </li>"
				+ "<ul>"
				+ "<li> A Rise or eventually follows from f to j </li>"
				+ "<li> A Trough or eventually follows from e to a </li>"
				+ "<li> A Jump or eventually follows from a to j </li>"
				+ "<li> A Drop or eventually follows from j to a </li>"
				+ "</ul>"
				+ "<li> DFT features for the top k-coefficients: </li>"
				+ "<ul>"
				+ "<li> (Discrete) The frequency of the kth coefficient </li>"
				+ "<li> (Continous) The power of the kth coefficient </li>"
				+ "</ul>"
				+ "<li> EDT-TS proposed in 'Decision Mining with Time Series Data "
				+ "Based on Automatic Feature Generation'. Scheibel, B. and Rinderele-Ma,"
				+ " S. CAiSE 2022. However, only interval-based "
				+ "features are considered. Moreover, global features can be "
				+ "introduced via "
				+ "slicing configuration using xPM setup.</li>"
				+ "<ul>"
				+ "<li> Aggregation functions used: </li>"
				+ "<ul>"
				+ "<li> (Continous) Mean </li>"
				+ "<li> (Continous) Standard Deviation </li>"
				+ "<li> (Continous) Percentage Change </li>"
				+ "<li> (Continous) Slope </li>"
				+ "</ul>"
				+ "<li> Interval-based features </li>"
				+ "<ul>"
				+ "<li> Equally split into 2 groups then use agg functions on groups</li>"
				+ "<li> Equally split into 4 groups then use agg functions on groups</li>"
				+ "<li> Equally split into 10 groups then use agg functions on groups</li>"
				+ "</ul>"
//				+ "<li> and so called ''Pattern-based features'' </li>"
				+ "</ul>"
				+ "</ul>"
				+ "</p></body></html>";
		
		public ExperimentalFeaturesParameter() {
//			setup parameter layout
			setup();
//			add title and parameter info
			makeTitle(title, info);
//			add label for mode selection
			JLabel text = new JLabel("Mode:");
			text.setFont(plainFont);
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,this.contentLeft,5,5);
			add(text, c);
//			add enable mode
			c.gridx += 1;
			c.insets = new Insets(0,5,5,5);
			c.weightx = 0;
			c.gridwidth = 2;
			enabledMode.setSelected(false);
			enabledMode.setBackground(Color.LIGHT_GRAY);
			enabledMode.setActionCommand(ExperimentalFeatureMode.ENABLE.name());
			enabledMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == ExperimentalFeatureMode.ENABLE.name()) {
						moveMode(ExperimentalFeatureMode.ENABLE);
					}
			}
			});
			modeGroup.add(enabledMode);
			add(enabledMode, c);
//			add disable mode
			c.gridx += 2;
			disableMode.setBackground(Color.LIGHT_GRAY);
			disableMode.setSelected(true);
			disableMode.setActionCommand(ExperimentalFeatureMode.DISABLE.name());
			disableMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == ExperimentalFeatureMode.DISABLE.name()) {
						moveMode(ExperimentalFeatureMode.DISABLE);
					}
				}
			});
			modeGroup.add(disableMode);
			add(disableMode, c);
//			add label for feature selection
			text = new JLabel("Features to Introduce:");
			text.setFont(plainFont);
			c.gridy = 3;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(0,this.contentLeft,5,5);
			add(text, c);
//			add sax features
			c.gridx += 1;
			c.insets = new Insets(0,5,5,5);
			c.weightx = 0;
			c.gridwidth = 1;
			addSAXFeatures.setSelected(true);
			addSAXFeatures.setBackground(Color.LIGHT_GRAY);
			addSAXFeatures.setActionCommand("SWITCH");
			addSAXFeatures.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("SWITCH")) {
						saxFeatures = addSAXFeatures.isSelected();
					}
			}
			});
			add(addSAXFeatures, c);
//			add dft features
			c.gridx += 1;
			addDFTFeatures.setSelected(true);
			addDFTFeatures.setBackground(Color.LIGHT_GRAY);
			addDFTFeatures.setActionCommand("SWITCH");
			addDFTFeatures.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("SWITCH")) {
						dftFeatures = addDFTFeatures.isSelected();
					}
			}
			});
			add(addDFTFeatures, c);
//			add edtts features
			c.gridx += 1;
			c.insets = new Insets(0,5,5,5);
			c.weightx = 0;
			c.gridwidth = 2;
			addEDTTSFeatures.setSelected(false);
			addEDTTSFeatures.setEnabled(false);
			addEDTTSFeatures.setBackground(Color.LIGHT_GRAY);
			addEDTTSFeatures.setActionCommand("SWITCH");
			addEDTTSFeatures.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand().equals("SWITCH")) {
						edttsFeatures = addEDTTSFeatures.isSelected();
					}
			}
			});
			add(addEDTTSFeatures, c);
			
		}

		public void addConfiguration(MinerConfigurationBuilder builder) {
			builder.experimentalFeatures(enabled);
			builder.experimentalDFTFeatures(dftFeatures);
			builder.experimentalSAXFeatures(saxFeatures);
			builder.experimentalEDTTSFeatures(edttsFeatures);
		}
		
		public void moveMode(ExperimentalFeatureMode mode) {
			if (mode == ExperimentalFeatureMode.ENABLE) {
				enabled = true;
			} else {
				enabled = false;
			}
		}
		
	}
	
	private class InstanceThresholdParameter extends BaseParameter {
		
//		gui elements
		private JRadioButton relativeMode = new JRadioButton("Fractional");
		private JRadioButton absoluteMode = new JRadioButton("Absolute");
		private JRadioButton thresholdMode = new JRadioButton("Both");
		private ButtonGroup modeGroup = new ButtonGroup();
		private JTextPane fractionalTextInput = new JTextPane();
		private JTextPane absoluteTextInput = new JTextPane();
		private JTextPane thresholdTextInput = new JTextPane(); 
		
//		internal states
		private InstanceMode mode = InstanceMode.REL;
		private double relativeInstanceLevel = 0.15;
		private double absoluteInstanceLevel = 25;
		private double thresholdInstanceLevel = 200;
		
//		labels
		String title = "Minimun Number of Instances per Leaf:";
		String info = "<html><body><p>"
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
				+ "percentage of observations per leaf, "
				+ "an absolute number, or both; where fractional is used above "
				+ "a threshold of instances and the absolute value is used "
				+ "when a choice has less observations than the thershold."
				+ "</p></body></html>";
		
		public InstanceThresholdParameter() {
//			setup parameter layout
			setup();
//			add title and parameter info
			makeTitle(title, info);
//			add radio button for selecting mode controls
			JLabel text = new JLabel("Instance Mode:");
			text.setFont(plainFont);
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			add(text, c);
			c.gridx += 1;
			c.insets = new Insets(0,5,5,5);
			c.weightx = 1.0;
			c.gridwidth = 2;
			relativeMode.setSelected(true);
			relativeMode.setBackground(Color.LIGHT_GRAY);
			relativeMode.setActionCommand(InstanceMode.REL.name());
			relativeMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == InstanceMode.REL.name()) {
						moveMode(InstanceMode.REL);
					}
			}
			});
			modeGroup.add(relativeMode);
			add(relativeMode, c);
			c.gridx += 2;
			absoluteMode.setBackground(Color.LIGHT_GRAY);
			absoluteMode.setActionCommand(InstanceMode.ABS.name());
			absoluteMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == InstanceMode.ABS.name()) {
						moveMode(InstanceMode.ABS);
					}
				}
			});
			modeGroup.add(absoluteMode);
			add(absoluteMode, c);
			c.gridx += 2;
			thresholdMode.setBackground(Color.LIGHT_GRAY);
			thresholdMode.setActionCommand(InstanceMode.BOTH.name());
			thresholdMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == InstanceMode.BOTH.name()) {
						moveMode(InstanceMode.BOTH);
					}
				}
			});
			modeGroup.add(thresholdMode);
			add(thresholdMode, c);
//			add slide for fraction 
			c.gridy = 3;
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.insets = new Insets(0,25,5,5);
			text = new JLabel("Fractional (%):");
			text.setFont(plainFont);
			add(text,c);
			c.gridx += 1;
			c.insets = new Insets(5,5,5,5);
			fractionalTextInput.setMaximumSize(new Dimension(100,25));
			fractionalTextInput.setText(Double.toString(relativeInstanceLevel));
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
							relativeInstanceLevel = Double.parseDouble(
									fractionalTextInput.getText()
							);
					}
				});
			add(fractionalTextInput, c);
//			add absolute input
			text = new JLabel("Absolute (#):");
			text.setFont(plainFont);
			c.gridx += 1;
			c.insets = new Insets(5,20,5,5);
			add(text, c);
			c.gridx += 1;
			c.insets = new Insets(5,5,5,5);
			absoluteTextInput.setMaximumSize(new Dimension(100,25));
			absoluteTextInput.setText(Integer.toString((int)absoluteInstanceLevel));
			absoluteTextInput.setBackground(Color.DARK_GRAY);
			absoluteTextInput.setForeground(Color.WHITE);
			absoluteTextInput.setPreferredSize(fractionalTextInput.getMaximumSize());
			((AbstractDocument)absoluteTextInput.getStyledDocument())
				.setDocumentFilter(new IntegerFilter());
			((AbstractDocument)absoluteTextInput.getStyledDocument())
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
						if (absoluteTextInput.getText().length() > 0) {
							absoluteInstanceLevel = Integer.parseInt(
									absoluteTextInput.getText()
							);
						}
				}
			});
			add(absoluteTextInput, c);
//			add threshold limit
			c.gridx += 1;
			c.insets = new Insets(5,20,5,5);
			text = new JLabel("Threshold (#):");
			text.setFont(plainFont);
			add(text, c);
			c.gridx += 1;
			c.insets = new Insets(5,5,5,5);
			thresholdTextInput.setMaximumSize(new Dimension(100,25));
			thresholdTextInput.setText(Integer.toString((int)absoluteInstanceLevel));
			thresholdTextInput.setBackground(Color.DARK_GRAY);
			thresholdTextInput.setForeground(Color.WHITE);
			thresholdTextInput.setPreferredSize(thresholdTextInput.getMaximumSize());
			((AbstractDocument)thresholdTextInput.getStyledDocument())
				.setDocumentFilter(new IntegerFilter());
			((AbstractDocument)thresholdTextInput.getStyledDocument())
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
						if (absoluteTextInput.getText().length() > 0) {
							thresholdInstanceLevel = Integer.parseInt(
									thresholdTextInput.getText()
							);
						}
				}
			});
			add(thresholdTextInput, c);
			
		}
		
		private void moveMode(InstanceMode mode) {
			this.mode = mode;
		}
		
		private class IntegerFilter extends DocumentFilter {
			
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				// TODO Auto-generated method stub
				super.remove(fb, offset, length);
			}

			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
					throws BadLocationException {
				// TODO Auto-generated method stub
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
				if (next.equals("")) {
					super.insertString(fb, 0, "2", attrs);
				}
				try {
					double num = Integer.parseInt(next);
					if (total <= 10 && num >= 2) {
						super.replace(fb, offset,length, text, attrs);
					} else if (length <= 10 && num < 2) {
						super.insertString(fb, 0, "2", attrs);
					} else  {
						Toolkit.getDefaultToolkit().beep();
					}
				} catch (Exception e) {
					Toolkit.getDefaultToolkit().beep();
					super.insertString(fb, 0, "2", attrs);
				}
				
			}

			public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
					throws BadLocationException {
				String next = fb.getDocument().getText(0, offset) + string;
				int length = next.length();
				if (next.equals("")) {
					super.insertString(fb, offset, "2", attr);
				}
				try {
					double num = Integer.parseInt(next);
					if (length <= 10 && num >= 2) {
						super.insertString(fb, offset, string, attr);
					} else if (length <= 10 && num < 2) {
						super.insertString(fb, offset, "2", attr);
					} else  {
						Toolkit.getDefaultToolkit().beep();
					}
				} catch (Exception e) {
					Toolkit.getDefaultToolkit().beep();
					super.insertString(fb, 0, "2", attr);
				}
			}
		}
		
		private class FractionalFilter extends DocumentFilter {
			
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				// TODO Auto-generated method stub
				super.remove(fb, offset, length);
			}

			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
					throws BadLocationException {
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
				builder.relativeInstanceLevel(relativeInstanceLevel);
				builder.instanceHandling(MinerInstanceMode.REL);
			} else if (mode == InstanceMode.ABS) {
				builder.absoluteInstanceLevel(absoluteInstanceLevel);
				builder.instanceHandling(MinerInstanceMode.ABS);
			} else if (mode == InstanceMode.BOTH) {
				builder.relativeInstanceLevel(relativeInstanceLevel);
				builder.absoluteInstanceLevel(absoluteInstanceLevel);
				builder.instanceThreshold(thresholdInstanceLevel);
				builder.instanceHandling(MinerInstanceMode.THRESHOLD);
			}
		}
		
	}

	private class PruneParameter extends BaseParameter {

//		gui elements
		private JRadioButton unprunedRadio = new JRadioButton("Unpruned");
		private JRadioButton prunedRadio = new JRadioButton("Pruned");
		private ButtonGroup radios = new ButtonGroup();
		
//		internal states
		boolean unpruned = false;
		
//		labels
		String title = "Pruning:";
		String info = "<html><body><p>"
				+ "The result of decision tree construction process can be a "
				+ "decision tree which is often very complex and overfits the data "
				+ "by inferring more structure than is justified by the observations."
				+ "Moreover, this complexity in the structure of the decision tree "
				+ "can have a higher error than a simpler tree. Thus, pruning occurs "
				+ "on the decision tree before the algorithm completes, which is a "
				+ "process which removes sub-trees of the structure with leafs without "
				+ "affecting the classification accuracy on unseen cases."
				+ "</p>"
				+ "<p>"
				+ "While a simplied tree can be more understandable, the pruning process "
				+ "will return a tree which covers less training observations. Thus, "
				+ "turning off this process can be useful for some classification problems, "
				+ "where unbalanced outcomes occur as pruning will be favour of the majority class."
				+ "</p>"
				+ "</body></html>";
		
		public PruneParameter() {
//			setup parameter
			setup();
//			add title and info
			makeTitle(title, info);
//			add radio buttons
			JLabel text = new JLabel("Pruning Mode:");
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(10,contentLeft,5,5);
			add(text, c);
//			add radio buttons
			c.gridx += 1;
			c.insets = new Insets(10,5,5,5);
			prunedRadio.setSelected(true);
			prunedRadio.setBackground(Color.LIGHT_GRAY);
			prunedRadio.setActionCommand(PruneMode.PRUNE.name());
			prunedRadio.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == PruneMode.PRUNE.name()) {
						moveMode(PruneMode.PRUNE);
					}
			}
			});
			add(prunedRadio, c);
			radios.add(prunedRadio);
			c.gridx += 1;
			unprunedRadio.setSelected(false);
			unprunedRadio.setBackground(Color.LIGHT_GRAY);
			unprunedRadio.setActionCommand(PruneMode.UNPRUNE.name());
			unprunedRadio.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == PruneMode.UNPRUNE.name()) {
						moveMode(PruneMode.UNPRUNE);
					}
			}
			});
			add(unprunedRadio, c);
			radios.add(unprunedRadio);
		}
		
		public void moveMode(PruneMode mode) {
			if (mode == PruneMode.PRUNE) {
				unpruned = false;
			} else {
				unpruned = true;
			}
		}

		public void addConfiguration(MinerConfigurationBuilder builder) {
			// TODO Auto-generated method stub
			builder.unpruned(unpruned);
		}
		
	}
	
	private class CrossValidateParameter extends BaseParameter {
		
			// gui elements
			JRadioButton enableButton = new JRadioButton("Enable");
			JRadioButton disableButton = new JRadioButton("Disable");
			ButtonGroup radios = new ButtonGroup();
			
			// states
			boolean enabled = false;
			int folds = 5;
		
			// labels
			String title = "Cross Validation:";
			String info = "<html><body><p>"
					+ "To ensure that the decision tree is robustly constructed "
					+ "and possibly not locked to a local mimimun, cross validation "
					+ "can be used. Where X random subsets of the training observations "
					+ "are used to construct X decision trees, returning the best decision "
					+ "tree found, where one decision tree is constructed from "
					+ "X - 1 subsets and validating on the remanining subset. Note that "
					+ "enabling cross validation will require X-1 times more computation "
					+ "as X decision trees are construction instead of 1."
					+ "</p></body></html>";
			
		public CrossValidateParameter() {
//			setup parameter
			setup();
//			make title and info
			makeTitle(title, info);
//			add in mode
			JLabel text = new JLabel("Enable Cross Validation:");
			text.setFont(plainFont);
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(10,contentLeft,5,5);
			add(text, c);
//			add radio buttons
			c.gridx += 1;
			c.insets = new Insets(10,5,5,5);
			enableButton.setSelected(enabled);
			enableButton.setBackground(Color.LIGHT_GRAY);
			enableButton.setActionCommand(CrossValidateMode.ENABLE.name());
			enableButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == CrossValidateMode.ENABLE.name()) {
						moveMode(CrossValidateMode.ENABLE);
					}
			}
			});
			radios.add(enableButton);
			add(enableButton, c);
			c.gridx += 1;
			disableButton.setSelected(!enabled);
			disableButton.setBackground(Color.LIGHT_GRAY);
			disableButton.setActionCommand(CrossValidateMode.DISBALE.name());
			disableButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (e.getActionCommand() == CrossValidateMode.DISBALE.name()) {
						moveMode(CrossValidateMode.DISBALE);
					}
			}
			});
			radios.add(disableButton);
			add(disableButton, c);
			c.gridx += 1;
//			add counter for folds
			text = new JLabel("# of Subsets:");
			text.setFont(plainFont);
			c.anchor = GridBagConstraints.EAST;
			add(text, c);
			c.anchor = GridBagConstraints.WEST;
			c.gridx += 1;
			JSpinner spinner = new JSpinner(new SpinnerNumberModel(5, 2, 25, 1));
			spinner.addChangeListener(new ChangeListener() {
				
				public void stateChanged(ChangeEvent e) {
					int change = (int) spinner.getValue();
					System.out.println("Fold changed to :: "+change);
					folds = change;
					
				}
			});
			add(spinner, c);
		}
		
		private void moveMode(CrossValidateMode mode) {
			if (mode == CrossValidateMode.ENABLE) {
				enabled = true;
			} else {
				enabled = false;
			}
		}

		public void addConfiguration(MinerConfigurationBuilder builder) {
			// TODO Auto-generated method stub
			builder.crossValidate(enabled);
			builder.crossValidateFolds(folds);
		}
		
	}
	
	private class ConfidenceLevelParameter extends BaseParameter {
		// gui elements
		private JSpinner confidenceFactor;
		
		// states
		float factor = 0.25f;
		float min = 0.01f;
		float max = 0.99f;
					
		// labels
		String title = "Confidence Factor";
		String info = "<html><body><p>"
				+ "The confidence factor for C4.5 constructed decision trees affects "
				+ "how the initial decision tree is pruned to the resulting tree. "
				+ "Smaller values cause more pruning to occur than larger values, "
				+ "where this affect is more pronounced on smaller datasets. If the "
				+ "returned decision tree has a high error rate on unseen cases, "
				+ "increasing this factor can improve the tree (indicative of "
				+ "underprunning)."
				+ "</p></body></html>";
		
		public ConfidenceLevelParameter() {
			//	setup panel
			setup();
			// add title and info
			makeTitle(title, info);
			// add spinner
			JLabel text = new JLabel("Factor (0.01-0.99):");
			text.setFont(plainFont);
			c.insets = new Insets(5, contentLeft, 5, 5);
			c.gridy = 2;
			c.gridx = 0;
			c.weightx = 0;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			add(text, c);
			c.gridx += 1;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(5,5,5,5);
			confidenceFactor = new JSpinner(new SpinnerNumberModel(
					factor, min, max, 0.01)
			);
			confidenceFactor.addChangeListener(new ChangeListener() {
				
				public void stateChanged(ChangeEvent e) {
					double val = (double) confidenceFactor.getValue();
					factor = (float) val;
				}
			});
			add(confidenceFactor, c);
		}


		public void addConfiguration(MinerConfigurationBuilder builder) {
			
			builder.confidenceLevel(factor);
		}
		
	}
}
