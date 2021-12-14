package org.processmining.qut.exogenousaware.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.datadiscovery.model.DiscoveredPetriNetWithData;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.datapetrinets.ui.ConfigurationUIHelper;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.data.storage.ExogenousDiscoveryInvestigation;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementGraphInvestigator;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorDotPanel;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousInvestigatorSelectionPanel;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
public class ExogenousDiscoveryInvestigator extends JPanel{
	
	/**
	 * 
	 */
	
	
	
	
	private static final long serialVersionUID = 7004747115076080275L;
	@NonNull private ExogenousAnnotatedLog source;
	@NonNull private PetriNetWithData controlflow;
	@NonNull private UIPluginContext context;
	
	@Default private List<String> endoVariables = null;
	@Default private List<String> exoVariables = null;
	@Default private PNRepResult alignment = null;
	@Default private JButton enhancementButton = new JButton("Open Enhancement");
	@Default private GridBagConstraints c = new GridBagConstraints();
	@Default private ExogenousInvestigatorDotPanel exoDotController = null;
	@Default private ExogenousInvestigatorSelectionPanel exoSelectionPanel = null;
	@Default public String enhancementTracablityViewKey = "E-Trace";
	@Default private ExogenousEnhancementTracablity enhancementView = null;
	@Default public String enhancementSearchViewKey = "E-Search";
	@Default private ExogenousEnhancementGraphInvestigator enhancementSearchView = null;
	@Default private ExogenousDiscoveryInvestigation result = null;
	@Default private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors() > 3 ? Runtime.getRuntime().availableProcessors() - 2 : 1;
	
	
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
		this.c.insets = new Insets(10,25,10,25);
		this.createModelView();
		this.createSelectionPanel();
		this.setBackground(Color.DARK_GRAY);
		this.validate();
		return this;
	}
	
	public ExogenousDiscoveryInvestigator precompute() {
		try {
			this.alignment = this.computeSimpleAlignment();
			System.out.println("[ExoDiscoveryInvestigator] completed precompute of alignment...");
			System.out.println("[ExoDiscoveryInvestigator] precomputed fitness : "+this.alignment.getInfo().get(PNRepResult.TRACEFITNESS) );
		} catch (Exception e) {
			System.out.println("[ExoDiscoveryInvestigator] Failed to precompute alignment :" + e.getCause());
		}
		return this;
	}
	
	public PNRepResult computeSimpleAlignment() throws Exception {
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
		return new PNLogReplayer().replayLog(
				this.context,
				net,
				(XLog) this.source.getEndogenousLog().clone(),
				mapping,
				new PetrinetReplayerWithILP() ,
				parameters
				);
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
//		create panel
		this.exoDotController = ExogenousInvestigatorDotPanel
				.builder()
				.graph(this.controlflow)
				.build()
				.setup();
//		JPanel modelView = new JPanel();
//		modelView.setLayout(new GridLayout());
//		ProMJGraphPanel model = ProMJGraphVisualizer.instance().visualizeGraphWithoutRememberingLayout(this.controlflow.getGraph());
////		model.setPreferredSize(new Dimension(3000,600));
//		modelView.add(model);
//		modelView.setBackground(Color.DARK_GRAY);
//		add panel
		this.add(exoDotController.getMain(), this.c);
	}
	
	public void createModelView(Map<String, GuardExpression> newRules, Map<String,String> swapMap, DiscoveredPetriNetWithData outcome) {
		this.exoDotController.setRules(newRules);
		this.exoDotController.setSwapMap(swapMap);
		this.exoDotController.setUpdatedGraph(outcome);
		this.exoDotController.update();
		this.exoSelectionPanel.getEnhance().setEnabled(true);
	}
	
	public void createSelectionPanel() {
//		setup layout manager
		this.c.gridx= 0;
		this.c.gridy= 1;
		this.c.ipady=0;
//		create panel
		this.exoSelectionPanel = ExogenousInvestigatorSelectionPanel.builder()
				.exoVariables(this.exoVariables)
				.endoVariables(this.endoVariables)
				.source(this)
				.build()
				.setup();
		this.add(this.exoSelectionPanel.getMain(), this.c);
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
			.alignment(this.alignment)
			.log(this.source.getEndogenousLog())
			.source(this)
			.build()
			.setup();
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 3;
		this.add(this.result.getMain(), c);
		System.out.println("[Exogenous Investigator] Made investigation...");
		this.validate();
		result.run();
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
				this.exoDotController.getMain().setVisible(true);
			} else {
				this.enhancementView.getMain().setVisible(true);
				this.exoSelectionPanel.getMain().setVisible(false);
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
