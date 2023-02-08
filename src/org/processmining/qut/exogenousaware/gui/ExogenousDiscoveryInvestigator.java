package org.processmining.qut.exogenousaware.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.datadiscovery.model.DiscoveredPetriNetWithData;
import org.processmining.datapetrinets.exception.NonExistingVariableException;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.ui.ConfigurationUIHelper;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.data.storage.ExogenousDiscoveryInvestigation;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementGraphInvestigator;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorDotPanel;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorDotPanel.DotOverlay;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorDotPanel.DotOverlayInformationDump;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorSelectionPanel;
import org.processmining.qut.exogenousaware.gui.workers.ExogenousDiscoveryAlignmentWorker;
import org.processmining.qut.exogenousaware.gui.workers.ExogenousDiscoveryMeasurementWorker;
import org.processmining.qut.exogenousaware.gui.workers.ExogenousDiscoveryStatisticWorker;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
public class ExogenousDiscoveryInvestigator extends JPanel{
	
	private static final long serialVersionUID = 7004747115076080275L;
//	builder parameters
	@NonNull private ExogenousAnnotatedLog source;
	@NonNull private PetriNetWithData controlflow;
	@NonNull private UIPluginContext context;
	
//	internal states
	@Default private List<String> endoVariables = null;
	@Default private List<String> exoVariables = null;
	@Default private PNRepResult alignment = null;
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default public String enhancementTracablityViewKey = "E-Trace";
	@Default public String enhancementSearchViewKey = "E-Search";
	@Default private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1;
	@Default @Getter private ProcessModelStatistics statistics = null;
	@Default @Getter private Map<String, GuardExpression> rules = new HashMap();
	
//  gui widgets
	@Default private JButton enhancementButton = new JButton("Open Enhancement");
	@Default private ExogenousInvestigatorDotPanel exoDotController = null;
	@Default private ExogenousInvestigatorSelectionPanel exoSelectionPanel = null;
	@Default private ExogenousEnhancementTracablity enhancementView = null;
	@Default private ExogenousEnhancementGraphInvestigator enhancementSearchView = null;
	@Default private ExogenousDiscoveryInvestigation result = null;
	
//	state + gui compontents
	@Default @Getter private ExogenousDiscoveryProgresser progresser = null;

	
//	workers
	private ExogenousDiscoveryStatisticWorker statWorker;
	private ExogenousDiscoveryMeasurementWorker measureWorker;
	
	public ExogenousDiscoveryInvestigator setup() {
//		precompute available attributes for decision mining
		this.getExogenousVariables();
		this.getEndogenousVariables();
//		add panels
		this.setLayout(new GridBagLayout());
		this.c.fill = GridBagConstraints.BOTH;
		this.c.gridheight = 1;
		this.c.gridwidth = 1;
		this.c.weightx = 1;
		this.c.weighty = 1;
		this.c.gridy= 0;
		this.c.insets = new Insets(10,0,0,0);
		this.createModelView();
		this.createProgresser();
		this.createSelectionPanel();
		this.setBackground(Color.DARK_GRAY);
		this.validate();
		this.progresser.validate();
		return this;
	}
	
	public ExogenousDiscoveryInvestigator precompute() {
		try {
			this.computeSimpleAlignment();
			
		} catch (Exception e) {
			System.out.println("[ExoDiscoveryInvestigator] Failed to precompute alignment :" + e.getCause());
		}
		return this;
	}
	
	public void computeSimpleAlignment() throws Exception {
		PetrinetGraph net = this.controlflow;
		TransEvClassMapping mapping = ConfigurationUIHelper.queryActivityEventClassMapping(context, net, this.source.getEndogenousLog());
		System.out.println("[ExoDiscoveryInvestigator] Net to Log Activity Map : " +mapping.toString());
		XLogInfo logInfo = XLogInfoFactory.createLogInfo((XLog) this.source.getEndogenousLog().clone(), mapping.getEventClassifier());
		CostBasedCompleteParam parameters = new CostBasedCompleteParam(
				logInfo.getEventClasses().getClasses(), mapping.getDummyEventClass(),  net.getTransitions(), 1, 1
		);
		parameters.setGUIMode(true);
		parameters.setUsePartialOrderedEvents(true);
		parameters.setCreateConn(false);
		parameters.setInitialMarking(this.controlflow.getInitialMarking());
		parameters.setFinalMarkings(this.controlflow.getFinalMarkings());
//		parameters.setNumThreads(this.maxConcurrentThreads); ILP problem with an array index when threaded : LPProblemProvider:24
		parameters.setMaxNumOfStates(Integer.MAX_VALUE);
		System.out.println("[ExoDiscoveryInvestigator] starting precompute of alignment...");
		
		
//		off load alignment work to worker
		ExogenousDiscoveryAlignmentWorker worker = ExogenousDiscoveryAlignmentWorker.builder()
			.context(this.context)
			.progress(progresser)
			.endogenousLog((XLog) this.source.getEndogenousLog().clone())
			.mapping(mapping)
			.net(net)
			.parameters(parameters)
			.build();
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub
				if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
					if (worker.isDone()) {
						try {
							setAlignment(worker.get());
						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});
		
		worker	
			.execute();
	}
	
	private void setAlignment(PNRepResult alignment) {
//		set alignment
		this.alignment = alignment;
		this.exoSelectionPanel.getEnhance().setEnabled(true);
		this.exoSelectionPanel.getInvestigate().setEnabled(true);
		System.out.println("[ExoDiscoveryInvestigator] completed precompute of alignment...");
		System.out.println("[ExoDiscoveryInvestigator] precomputed fitness : "+this.alignment.getInfo().get(PNRepResult.TRACEFITNESS) );
		
//		start statistic worker
		statWorker = ExogenousDiscoveryStatisticWorker.builder()
				.alignment(alignment)
				.progresser(progresser)
				.controlflow(controlflow)
				.log(source)
				.build()
				.setup();
		
		statWorker.addPropertyChangeListener(new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent evt) {
				// TODO Auto-generated method stub
				if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
					if (statWorker.isDone()) {
						try {
							setStatistics(statWorker.get());
						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		});
		
		statWorker.execute();
	}
	
	public void setStatistics(ProcessModelStatistics statistics) {
		this.statistics = statistics;
		this.exoDotController.setModelLogInfo(statistics);
		this.exoDotController.update();
		this.exoSelectionPanel.getMeasure().setEnabled(true);
		
		setupDotNodeListeners();

	}
	
	public void setupDotNodeListeners() {
//		set up listeners for clicks on dot overlay
		DotOverlay overlay = this.exoDotController.getController().getOverlay();
		Map<String, DotNode> nodes = this.exoDotController.getVisBuilder().getNodes();
		for (PetrinetNode node : this.controlflow.getNodes()) {
			if (node instanceof Transition) {
				DotNode dotNode = nodes.get(node.getId().toString());
				
				dotNode.addMouseListener(new MouseAdapter() {
					
					public void mouseClicked(MouseEvent e) {
						boolean highlighted = ((ExoDotTransition)dotNode).isHighlighted();
//						de-highlight other nodes
						for ( Entry<String, DotNode> other : nodes.entrySet()) {
							if (other.getValue() instanceof ExoDotTransition) {
								ExoDotTransition otherNode = (ExoDotTransition)other.getValue();
								otherNode.revertHighlight();
							}
						}
//						highlight this node
						if (dotNode instanceof ExoDotTransition && !highlighted) {
							((ExoDotTransition) dotNode).highlightNode();
							DotOverlayInformationDump.builder()
								.controlNode(node)
								.node(dotNode)
								.statistics(statistics)
								.overlay(overlay)
								.build()
								.setup();
						}
//						update dot vis
						exoDotController.getVis().changeDot(exoDotController.getVis().getDot(), false);
					}
				});
			}
		}
	}
	
	public List<String> getEndogenousVariables(){
		if (this.endoVariables == null) {
			Optional<Set<String>> temp = 
					this.source.getEndogenousLog()
					.stream()
					.map( trace -> {
							trace = (XTrace) trace.clone();
							Optional<Set<String>> evAttrs =
								trace.stream().map(ev ->  ev.getAttributes().keySet()).reduce( 
										(curr,next) -> {
											next.removeIf(s -> {return s.contains("exogenous");});
											return joinSets(curr,next);
										}
								);
							if (evAttrs.isPresent()) {
								return evAttrs.get();	
							} else {
								return new HashSet<String>();
							}
					})
					.reduce( (curr,next) -> {
						next.removeIf(s -> {return s.contains("exogenous");});
						curr.removeIf(s -> {return s.contains("exogenous");});
						next.removeIf(s -> {return s.contains("time:timestamp");});
						curr.removeIf(s -> {return s.contains("time:timestamp");});
						return joinSets(curr,next);
					});
			this.endoVariables = new ArrayList<String>();
			if (temp.isPresent()) {
				this.endoVariables.addAll(temp.get());
			}
			temp = 
					this.source.getEndogenousLog()
						.stream()
						.map(trace -> { trace = (XTrace) trace.clone(); return trace.getAttributes().keySet(); })
						.reduce( (curr,next) -> { 
							next.removeIf(s -> {return s.contains("exogenous");});
							next.removeIf(s -> {return s.contains("time:timestamp");});
							curr.removeIf(s -> {return s.contains("time:timestamp");});
							return joinSets(curr,next);
						});
			if (temp.isPresent()) {
				this.endoVariables.addAll(temp.get());
			}
			
		}
		System.out.println("[ExoDiscoveryInvestigator] endoVariables : "+this.endoVariables.toString());		
		return this.endoVariables;
	}
	
	public List<String> getExogenousVariables(){
		Optional<Set<String>> temp;
		if (this.exoVariables == null) {
			temp = 
					this.source.getEndogenousLog()
					.stream()
					.map( trace -> {
							trace = (XTrace)trace.clone();
							Optional<Set<String>> evAttrs =
								trace.stream().map(ev -> ev.getAttributes().keySet()).reduce( 
										(curr,next) -> {
											next.removeIf(s -> {return s.contains("exogenous") ? false : true;});
											return joinSets(curr,next);
										}
								);
							if (evAttrs.isPresent()) {
								return evAttrs.get();	
							} else {
								return new HashSet<String>();
							}
					})
					.reduce( (curr,next) -> {
							next.removeIf(s -> {return s.contains("exogenous") ? false : true;});
							next.removeIf(s -> {return s.contains("receiver") ? true : false;});
							curr.removeIf(s -> {return s.contains("exogenous") ? false : true;});
							curr.removeIf(s -> {return s.contains("receiver") ? true : false;});
							return joinSets(curr,next);
					});
			this.exoVariables = new ArrayList<String>();
			if (temp.isPresent()) {
				this.exoVariables.addAll(temp.get());
			}
			
		}
		System.out.println("[ExoDiscoveryInvestigator] exoVariables : "+this.exoVariables.toString());
		return this.exoVariables;
	}
	
	public void createModelView() {
//		setup layout manager
		this.c.gridx = 0;
		this.c.ipady = 300;
//		check for pre-existing rules
		Map<String, GuardExpression> rules= new HashMap();
		for( Transition T : this.controlflow.getTransitions()) {
			if (T instanceof PNWDTransition) {
				PNWDTransition t = (PNWDTransition)T;
				if (t.hasGuardExpression()) {
					rules.put(T.getId().toString(), t.getGuardExpression());
				}
			}
		}
		System.out.println("guards found in existing model :: "+ rules.toString());
		this.rules = rules;
//		create panel
		this.exoDotController = ExogenousInvestigatorDotPanel
				.builder()
				.graph(this.controlflow)
				.swapMap(new HashMap())
				.rules(this.rules)
				.build()
				.setup();
//		add panel
		this.add(exoDotController.getMain(), this.c);
	}
	
	public void createModelView(Map<String,String> swapMap, DiscoveredPetriNetWithData outcome, Map<Transition,Transition> transMapping) {
		
		
		this.statistics.clearMeasures();
		
		Map<String, GuardExpression> rules = new HashMap();
		
		this.controlflow.removeAllVariables();
		
		
		for (DataElement de :outcome.getVariables()) {
			this.controlflow.addVariable(de.getVarName(), de.getType(), de.getMinValue(), de.getMaxValue());
		}
		
		
		for(Entry<Transition, Transition> entry : transMapping.entrySet()) {
			
			Transition nt = entry.getValue();
			Transition ot = entry.getKey();
			
			PNWDTransition rnt = null;
			if (nt.getClass().equals(PNWDTransition.class)) {
				rnt = (PNWDTransition) nt;
			}
			
			if (rnt != null) {
			 if (rnt.hasGuardExpression()) {
				 try {
					this.controlflow.setGuard(ot, rnt.getGuardExpression());
					rules.put(ot.getId().toString(), rnt.getGuardExpression());
				} catch (NonExistingVariableException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 }
			}
		}
		
		this.rules = rules;
		
		this.exoDotController.setRules(rules);
		this.exoDotController.setSwapMap(swapMap);
		this.exoDotController.setUpdatedGraph(this.controlflow);
		this.exoDotController.update();
		getExoSelectionPanel().getInvestigate().setEnabled(true);
		getExoSelectionPanel().getMeasure().setEnabled(true);
		progresser.getState(ProgressType.Measurements).reset();
		
		setupDotNodeListeners();
	}
	
	public void createProgresser() {
//		setup layout 
		this.c.gridx = 0;
		this.c.gridy = 1;
		this.c.weighty = 0.0;
		this.c.fill = c.BOTH;
		this.c.ipady = 0;
		this.c.insets = new Insets(3,5,0,5);
//		create progresser
		this.progresser = ExogenousDiscoveryProgresser.builder()
				.build()
				.setup();
		add(progresser, this.c);
	}
	
	public void createSelectionPanel() {
//		setup layout manager
		this.c.gridx= 0;
		this.c.gridy= 2;
		this.c.ipady= 25;
		this.c.weighty = .5;
		this.c.insets = new Insets(30,15,5,15);
//		create panel
		this.exoSelectionPanel = ExogenousInvestigatorSelectionPanel.builder()
				.exoVariables(this.exoVariables)
				.endoVariables(this.endoVariables)
				.source(this)
				.build()
				.setup();
		this.add(this.exoSelectionPanel.getMain(), this.c);
		this.exoSelectionPanel.getEnhance().setEnabled(false);
		this.exoSelectionPanel.getInvestigate().setEnabled(false);
	}
	
	public JComponent getComponent() {
		return this;
	}
	
	public void runInvestigation() throws Exception {
		System.out.println("[Exogenous Investigator] Exo Variables Select : "+this.exoSelectionPanel.getSelectedExoVariables().getSelectedValuesList());
		System.out.println("[Exogenous Investigator] Endo Variables Select : "+this.exoSelectionPanel.getSelectedEndoVariables().getSelectedValuesList());
		this.result = ExogenousDiscoveryInvestigation.builder()
			.log(this.source.getEndogenousLog())
			.model(this.controlflow)
			.exogenousVariables(this.exoSelectionPanel.getSelectedExoVariables().getSelectedValuesList())
			.endogenousVariables(this.exoSelectionPanel.getSelectedEndoVariables().getSelectedValuesList())
			.miner(this.exoSelectionPanel.getSelectedMiner().getSelectedValuesList().get(0))
			.config(this.exoSelectionPanel.getDecisionMinerConfig().makeConfig())
			.alignment(this.alignment)
			.log(this.source.getEndogenousLog())
			.source(this)
			.build()
			.setup();
		System.out.println("[Exogenous Investigator] Made investigation...");
		this.validate();
		
		getExoSelectionPanel().getMeasure().setEnabled(false);
		getExoSelectionPanel().getInvestigate().setEnabled(false);
		
		result.run();
	}
	
	public void runMeasurements() {
		if (this.alignment != null & this.statistics != null) {
			this.exoSelectionPanel.getMeasure().setEnabled(false);
			this.exoSelectionPanel.getInvestigate().setEnabled(false);
			
			this.statistics.clearMeasures();
			
			this.measureWorker = ExogenousDiscoveryMeasurementWorker.builder()
					.endogenousLog(source.getEndogenousLog())
					.model(controlflow)
					.alignment(alignment)
					.variableMap(exoDotController.getSwapMap() != null ? exoDotController.getSwapMap() : new HashMap())
					.statistics(statistics)
					.progresser(progresser)
					.build();
			
			this.measureWorker.addPropertyChangeListener(new PropertyChangeListener() {
			
				
				public void propertyChange(PropertyChangeEvent evt) {
					// TODO Auto-generated method stub
					if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
						System.out.println("[MeasurementWorker] Done");
						try {
							setMeasurements(measureWorker.get());
						} catch (InterruptedException | ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						getExoSelectionPanel().getMeasure().setEnabled(true);
						getExoSelectionPanel().getInvestigate().setEnabled(true);
					}
				}
			});
			
			this.measureWorker.execute();
		}
	}
	
	public void setMeasurements(Map<String,Double> measures) {
		System.out.println("[ExogenousDiscoveryInvestigator] new measures :: "+ measures.toString());
		
		for ( Entry<String, Double> measure : measures.entrySet()) {
			this.statistics.addGraphMeasure(measure.getKey(), measure.getValue());
		}
		this.exoDotController.update();
		setupDotNodeListeners();
		this.repaint();
	}
	
	public void buildEnhancementView() {
		if (this.enhancementView == null) {
//			create panel
			this.enhancementView = ExogenousEnhancementTracablity.builder()
				.source(this)
				.focus(this.result)
				.controlflow(this.controlflow)
				.alignment(this.alignment)
				.build()
				.setup();
//			setup layout
			this.c.gridx = 0;
			this.c.gridy = 2;
			this.add(this.enhancementView.getMain(), this.c);
			this.exoSelectionPanel.getMain().setVisible(false);
			this.progresser.setVisible(false);
			this.exoDotController.getMain().setVisible(false);
			this.validate();
		} else {
//			if we have made the panel before, we need to update and rebuild
			this.enhancementView.update(this.result);
			this.switchView();
			
		}
	}
	
	public void switchView() {
//		check that we have something to switch to
		if (this.enhancementView != null) {
			if (this.enhancementView.getMain().isVisible()) {
				this.enhancementView.getMain().setVisible(false);
				this.exoSelectionPanel.getMain().setVisible(true);
				this.progresser.setVisible(true);
				this.exoDotController.getMain().setVisible(true);
			} else {
				this.enhancementView.getMain().setVisible(true);
				this.exoSelectionPanel.getMain().setVisible(false);
				this.progresser.setVisible(false);
				this.exoDotController.getMain().setVisible(false);
			}
		}
		this.validate();
	}
	
	public Set<String> joinSets(Set<String> curr, Set<String> next) {
		Set<String> temp = new HashSet<String>();
		temp.addAll(next);
		temp.addAll(curr);
		return temp;
	}
	
	public void makeEnhancementSearcher() {
		enhancementSearchView = ExogenousEnhancementGraphInvestigator.builder()
				.source(this.enhancementView.getAnalysis())
				.controller(this)
				.build()
				.setup();
	}
	
	public void switchView(String view) {
		if (view.equals(enhancementSearchViewKey)) {
			if (this.enhancementView != null) {
				this.enhancementView.getMain().setVisible(false);
			}
			this.exoSelectionPanel.getMain().setVisible(false);
			this.exoDotController.getMain().setVisible(false);
			makeEnhancementSearcher();
			enhancementSearchView.getMain().setVisible(true);
			// add panel to view
			this.c.gridy = 4;
			this.c.gridx = 0;
			this.add(enhancementSearchView.getMain(), this.c);
			this.validate();
		} else if (view.equals(enhancementTracablityViewKey)) {
			this.remove(enhancementSearchView.getMain());
			if (this.enhancementView != null) {
				this.enhancementView.getMain().setVisible(true);
			}
			this.enhancementSearchView.getMain().setVisible(false);
			this.exoSelectionPanel.getMain().setVisible(false);
			this.exoDotController.getMain().setVisible(false);
		}
	}

}
