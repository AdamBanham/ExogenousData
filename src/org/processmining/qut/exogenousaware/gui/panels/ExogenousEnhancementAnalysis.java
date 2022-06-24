package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.workers.ExogenousObservedUniverse;
import org.processmining.qut.exogenousaware.gui.workers.helpers.ActivitySearchGrouper;
import org.processmining.qut.exogenousaware.gui.workers.helpers.ExogenousObserverGrouper;
import org.processmining.qut.exogenousaware.gui.workers.helpers.TransformerSearchGrouper;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class ExogenousEnhancementAnalysis {
	
	@NonNull private ExogenousEnhancementTracablity source;
	

	@Default private JPanel main = new JPanel();
	@Default private JScrollPane scroll = new JScrollPane();
	@Default private JLabel focusedTrans = new JLabel();
	@Default private JTextArea guard = new JTextArea();
	@Default private JProgressBar progress = new JProgressBar();
	@Default private JLabel progressLabel = new JLabel();
	@Default private Map<String,JPanel> exoCharts = new HashMap<String, JPanel>();
	@Default private ExoDotTransition focus = null;
	@Default private ExogenousObservedUniverse task = null;
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default private Map<ExoDotTransition, Map<String,XYSeriesCollection>> cacheUniverse = new HashMap<ExoDotTransition, Map<String,XYSeriesCollection>>();
	@Default private Map<ExoDotTransition, Map<String, List<Map<String,Object>>>> cacheStates = new HashMap<ExoDotTransition, Map<String, List<Map<String,Object>>>>();
	@Default private Map<ExoDotTransition, Map<String, List<Map<String,Object>>>> cacheSeriesStates = new HashMap<ExoDotTransition, Map<String, List<Map<String,Object>>>>();
	
	
	@Default private Color expressionFailedColor = new Color(128,0,0,25);
	@Default private Color expressionPassedColor = new Color(0,102,51,25);
	@Default private Color expressionNullColor = new Color(0,0,0,25);
	
	@Default private ExogenousObserverGrouper bloodCultureGrouper = ActivitySearchGrouper.builder()
			.activityName("blood cultured")
			.build();
	@Default private ExogenousObserverGrouper sepsisInfectionGrouper = TransformerSearchGrouper.builder()
			.transformedAttributeName("exogenous:blood_test:FOS:transform:POT")
			.value(1.0)
			.build();
	@Default private ExogenousObserverGrouper selectedGrouper = null;
	
	public ExogenousEnhancementAnalysis setup() {
		this.selectedGrouper = sepsisInfectionGrouper;
//		link main and scroll
		this.scroll.setViewportView(this.main);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
//		style main
		this.main.setLayout(new GridBagLayout());
		this.c.gridheight = 1;
		this.c.gridwidth = 2;
		this.c.weightx = 0;
		this.c.weighty = 0;
		this.c.gridx = 0;
		this.c.gridy = 1;
		this.c.anchor = GridBagConstraints.LINE_START;
		this.c.fill = GridBagConstraints.NONE;
		this.c.insets = new Insets(10,25,10,25);
		this.main.setBackground(Color.DARK_GRAY);
//		add label
		this.focusedTrans.setText("click a transition in the graph above");
		this.focusedTrans.setForeground(Color.WHITE);
		this.main.add(this.focusedTrans, c);
//		add label for guard
		this.c.gridy++;
		this.guard.setForeground(Color.WHITE);
		this.guard.setBorder(BorderFactory.createEmptyBorder());
		this.guard.setBackground(Color.DARK_GRAY);
		this.guard.setLineWrap(true);
		this.guard.setWrapStyleWord(true);
		this.guard.setFont(this.focusedTrans.getFont());
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.8;
		c.weighty = 0.5;
		this.main.add(guard, c);
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.weighty = 0.0;
//		add progress
		this.c.gridy++;
		this.c.gridwidth = 1;
		this.progressLabel.setForeground(Color.WHITE);
		this.c.anchor = GridBagConstraints.LINE_START;
		this.main.add(this.progressLabel, c);
		this.progressLabel.setVisible(false);
		this.c.gridx++;
		this.c.anchor = GridBagConstraints.LINE_START;
		this.main.add(this.progress, c);
		this.progress.setVisible(false);
//		setup constraints for any graphs to be added after
		this.c.gridwidth = 2;
		this.c.anchor = GridBagConstraints.CENTER;
		this.c.weightx = 1;
		this.c.weighty = 0.1;
		this.c.ipadx = 300;
		return this;
	}
	
	public void reset() {
//		clear cache
		this.cacheUniverse = new HashMap<ExoDotTransition, Map<String,XYSeriesCollection>>();
		this.cacheStates = new HashMap<ExoDotTransition, Map<String, List<Map<String,Object>>>>();
		this.updateAnalysis(null);
		this.task = null;
		this.hideCharts();		
	}
	
	public String formatTransLabel(String trans) {
		String formater = "Enhancing on \"%s\" :";
		return String.format(formater, trans);
	}
	
	public void updateAnalysis(ExoDotTransition node) {
		if(node != null) {
			this.focus = node;
			this.focusedTrans.setText(this.formatTransLabel(this.focus.getTransLabel()));
			if (node.getGuardExpression() != null) {
				this.guard.setVisible(true);
				this.guard.setText(node.getGuardExpression().getRepresentation());
				this.main.validate();
			}
			this.buildObservedUniverse();
			this.showProgress(true);
		} else {
			this.focus = null;
			this.hideCharts();
			this.focusedTrans.setText("click a transition in the graph above");
			this.guard.setVisible(false);
			this.showProgress(false);
		}
		
	}
	
	public void hideCharts() {
//		loop through previous chartDict and hide all chartDict
		for(Entry<String, JPanel> entry : this.exoCharts.entrySet()) {
			entry.getValue().setVisible(false);
		}
	}
	
	public void showProgress(Boolean vis) {
		this.progress.setVisible(vis);
		this.progressLabel.setVisible(vis);
	}
	
	
	public void buildObservedUniverse() {
		this.hideCharts();
//		check for cached results
		if(this.cacheUniverse.containsKey(this.focus)) {
			this.hideCharts();
			this.handleObservedUniverse(true);
		} else {
//		create new worker and build a universe
			ExogenousAnnotatedLog log = this.source.getSource().getSource();
			this.showProgress(true);
			this.progressLabel.setText("Building observed exogenous universe :");
			this.task = ExogenousObservedUniverse.builder()
				.log(log)
				.alignment(this.source.getAlignment())
				.focus(this.focus)
				.progress(this.progress)
				.label(this.progressLabel)
				.grouper(this.selectedGrouper)
				.build()
				.setup();
			this.task.addPropertyChangeListener(new ObservedListener(this));
			this.task.execute();
		}
	}
	
	public void handleObservedUniverse(Boolean cached) {
		Map<String, XYSeriesCollection> universe = null;
		Map<String, List<Map<String,Object>>> states = null;
		Map<String, List<Map<String,Object>>> seriesStates = null;
		this.progress.setVisible(false);
		try {
			universe = cached ? this.cacheUniverse.get(this.focus) : this.task.get();
			states = cached ? this.cacheStates.get(this.focus) : this.task.getDatasetStates();
			seriesStates = cached ? this.cacheSeriesStates.get(this.focus) : this.task.getSeriesStates();
			for(Entry<String,XYSeriesCollection> entry : universe.entrySet()) {
				if (cached) {
					showCachedGraph(this.focus.getControlFlowId()+ entry.getKey());
				} else {
//				build exogenous enhancment graphs
				EnhancementExogenousDatasetGraphController controller = EnhancementExogenousDatasetGraphController.builder()
						.datasetName(entry.getKey())
						.universe(entry.getValue())
						.states(states.get(entry.getKey()))
						.seriesStates(seriesStates.get(entry.getKey()))
						.hasExpression(this.focus.getGuardExpression().hasExpression())
						.guardExpression(this.focus.getGuardExpression())
						.transName(this.focus.getTransLabel())
						.useGroups(true)
						.grouper(this.selectedGrouper)
						.groups(this.task.getSeriesGroups().get(entry.getKey()))
						.build()
						.setup();
				cacheGraph(this.focus.getControlFlowId()+ entry.getKey(), controller);
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.main.validate();
		this.scroll.validate();
		this.progressLabel.setVisible(false);
		this.progress.setVisible(false);
		if (!cached && universe != null) {
			this.cacheUniverse.put(this.focus, universe);
			this.cacheStates.put(this.focus, states);
			this.cacheSeriesStates.put(this.focus, seriesStates);
		}
	}
	
	public void cacheGraph(String title,JPanel chart) {
		if (this.exoCharts.containsKey(title)) {
			this.exoCharts.get(title).setVisible(true);
		} else {
			this.exoCharts.put(title, chart);
			this.c.gridx = 0;
			int oldfill = this.c.fill;
			this.c.fill = GridBagConstraints.HORIZONTAL;
			double oldweightx = this.c.weightx;
			this.c.weightx = 1.0;
			this.c.gridy++;
			this.main.add(chart, c);
			this.c.fill = oldfill;
			this.c.weightx = oldweightx;
		}
		this.progress.setValue(this.progress.getValue()+1);
		this.main.validate();
	}
	
	public void showCachedGraph(String title) {
		if (this.exoCharts.containsKey(title)) {
			JPanel graph = this.exoCharts.get(title);
			graph.setVisible(true);
		}
	}
	
	
	public class ObservedListener implements PropertyChangeListener {
		
		ExogenousEnhancementAnalysis source;
		
		public ObservedListener(ExogenousEnhancementAnalysis source) {
			this.source = source;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			// TODO Auto-generated method stub
//			System.out.println("property trigger");
//			System.out.println(evt.getPropertyName());
//			System.out.println(evt.getNewValue());
			if (evt.getPropertyName().toString().equals("state")) {
//				System.out.println("state change");
				if(evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
//					System.out.println("work is done");
					this.source.handleObservedUniverse(false);
				}
			}
			
		}
		
	}


	
}
