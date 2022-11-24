package org.processmining.qut.exogenousaware.gui.dot;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.VariableAccess;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.dot.Dot.GraphDirection;
import org.processmining.plugins.graphviz.dot.DotEdge;
import org.processmining.plugins.graphviz.dot.DotNode;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This class builds a dot graph in a stable way. Ordering of places, transitions and arcs is import to ensure that
 * the visualisation is stable.
 * <br><br>
 * 
 * This class uses the builder design pattern, to create new instances call DotGraphVisualisation.builder().build().
 * <br><br>
 * 
 * Note that this class is no intended to be resuable after calling make(), instead no reference should be kept.
 * <br><br>
 * 
 * Ideally usage should be a call like the following:<br>
 * <i>DotGraphVisualisation.builder().graph(graph).build().make().getVisualisation();</i>
 * <br>
 * This call returns the stable Dot class to be used in a dot panel.
 *  
 * @author Adam Banham
 *
 */
@Builder
public class DotGraphVisualisation {
	
//	builder parameters
	@NonNull private PetriNetWithData graph;
	@NonNull private Map<String,String> swapMap;
	
//	optional parameters
	@Default private Map<Transition,Transition> transMapping = null;
	@Default @Setter private Map<String, GuardExpression> rules = null;
	@Default @Setter private PetriNetWithData updatedGraph = null;
	@Default private DotNode selectedNode = null;
	private ProcessModelStatistics modelLogInfo;
	
//	internal states
	@Default @Getter private Dot visualisation = new Dot();
	@Default private List<Place> initial = new ArrayList<>();
	@Default private List<Place> end = new ArrayList<>();
	@Default private List<Object> curr = new ArrayList<>();
	@Default private List<PetrinetNode> next = new ArrayList<PetrinetNode>();
	@Default private List<Object> nextEdges = new ArrayList<>();
	@Default private List<String> seen = new ArrayList<>();
	@Default private int group =2;
	@Default @Getter private Map<String, DotNode> nodes = new HashMap<String, DotNode>();
	@Default @Getter private List<ExoDotTransition> transitions = new ArrayList<ExoDotTransition>();
	@Default private List<Place> decisionPlaces = new ArrayList();
	@Default private DotNode leftAnchor = new DotAnchor("L");
	@Default private DotNode rightAnchor = new DotAnchor("R");
	
	
	public DotGraphVisualisation make() {
		styleDot();
		findStartsAndEnds();
		walkGraph();
		if (modelLogInfo != null) {
			makeDecisionClusters();
		}
		return this;
	}
	
	private void makeDecisionClusters() {
		int dp = 1;
		Dot clusterDot = new Dot();		
		clusterDot.setDirection(GraphDirection.leftRight);
		
		Place last = null;
		
		
		
		
		for(Place dplace: this.decisionPlaces) {
			
			List<DotEdge> seenEdges = new ArrayList();
			List<DotEdge> seenTranEdges = new ArrayList();
						
			List<DotNode> members = new ArrayList();
			
			String label = String.format("Decision Point #%d (%d / %.1f%%)",
					dp,
					this.modelLogInfo.getInformation(dplace).getTotalInstances(),
					this.modelLogInfo.getInformation(dplace).getRelativeFrequency()*100
			);
			
			DotNode decisionNode = nodes.get(dplace.getId().toString());
			members.add(decisionNode);
			
			for (DotEdge edge : this.visualisation.getEdges()) {
				if (edge.getSource().equals(decisionNode)) {
					seenEdges.add(edge);
				}
			}
			
			for(Transition outcome: modelLogInfo.getDecisionOutcomes(dplace)) {
					DotNode tran = nodes.get(outcome.getId().toString());
					members.add(tran);	
					for (DotEdge edge : this.visualisation.getEdges()) {
						if (edge.getSource().equals(tran)) {
							seenTranEdges.add(edge);
						}
					}
//					this.visualisation.removeNode(tran);
			}
			
			this.group++;
			DotNode dc = DotNodeStyles.buildDecisionCluster(label, members, group);
			
			for (DotEdge edge : seenEdges) {
//				((DecisionCluster)dc).addEdge(edge);
//				this.visualisation.removeEdge(edge);
			}
			
			clusterDot.addNode(dc);	
			
			for (DotEdge edge : seenTranEdges) {
				clusterDot.addEdge(edge);
			}
			
			if (last == null) {
				last = dplace;
			} else {
				DotEdge invEdge = new DotAnchorEdge(nodes.get(last.getId().toString()), nodes.get(dplace.getId().toString()));
//				((DecisionCluster)dc).addEdge(invEdge);
//				this.visualisation.addEdge(invEdge);
			}
			
			this.visualisation.addNode(dc);	
			
			dp++;
		}
		clusterDot.addNode(nodes.get(end.get(0).getId().toString()));
		File f = new File("C:\\\\Users\\\\n7176546\\\\OneDrive - Queensland University of Technology\\\\Desktop\\\\test\\\\dummy.dot");
		try {
			clusterDot.exportToFile(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	
	}
	
	private void styleDot() {
		this.visualisation.setOption("bgcolor", "none");
		this.visualisation.setOption("rank", "max");
		this.visualisation.setOption("compound", "true");
		this.visualisation.setOption("splines", "true");
		this.visualisation.setOption("searchsize", "1000");
//		this.visualisation.setOption("layers", "dp:net:top");
		this.visualisation.setDirection(GraphDirection.leftRight);
	}
	
	/**
	 * Finds the starts and ending places, stores them for later so that these places uses a special style
	 */
	private void findStartsAndEnds() {
		decisionPlaces.clear();
//		add initial places first
		leftAnchor.setOption("group", "1");
		this.visualisation.addNode(leftAnchor);
		for (Place place : graph.getInitialMarking().stream().collect(Collectors.toList())) {
				initial.add(place);
				curr.add(place);
		}
		for (Place place : graph.getFinalMarkings()[0].stream().collect(Collectors.toList())) {
				end.add(place);
			
		}
	}
	
	/**
	 * Walks through the graph expanding upon each place as they are first seen.
	 * <br>
	 * This ensures that the dot graph produced will be stable and will likely be more 
	 * readable.
	 */
	private void walkGraph() {
//		walk through petri net and find backwards edges
		while (curr.size() > 0) {
			next = new ArrayList<PetrinetNode>();
			nextEdges = new ArrayList<>();
			for(Object node : curr) {
//				ignore data elements
				if (node instanceof VariableAccess || node instanceof DataElement) {
					continue;
				}			
//				check that we have not seen this node before
				String id = node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString();
//				System.out.println("Comparing '"+id+"'");
				if (seen.contains(id)) {
//					System.out.println("Found Match!");
					continue;
				}
				Place p = null;
				Transition t = null;
				PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> arc = null;
//				handle adding element
				if (node.getClass().equals(Place.class)) {
					p = (Place) node;
					DotNode pp = handlePlace(p);
//					pp.setOption("row", Integer.toString(group));
					
//					check for decision place
					if(graph.getOutEdges(p).size() > 1) {
						decisionPlaces.add(p);
					}
//					check if place in initial
					if (initial.contains(p)) {
						DotEdge anchorEdge = new DotAnchorEdge(leftAnchor, pp);
						visualisation.addEdge(anchorEdge);
					}
					
					visualisation.addNode(pp);
					
//					check if place is final marking
					if (end.contains(p)) {
						DotEdge anchorEdge = new DotAnchorEdge(pp, rightAnchor);
						visualisation.addEdge(anchorEdge);
					}
					
					
				} else if (node.getClass().equals(PNWDTransition.class)) {
					t = (Transition) node;
					DotNode newNode = handleTransition(t);
//					newNode.setOption("row", Integer.toString(group));
					nodes.put(t.getId().toString(), newNode);
//					newNode.addMouseListener(new EnhancementListener(this, this.vis, newNode));
					visualisation.addNode( newNode );
				} else if (node.getClass().equals(Arc.class)) {
					arc = (Arc) node;
					DotEdge arcDot;
					if (seen.contains(arc.getTarget().getId().toString())) {
						arcDot = DotNodeStyles.buildBackwardsEdge(
								nodes.get(arc.getSource().getId().toString()),
								nodes.get(arc.getTarget().getId().toString())
						);
					} else {
						arcDot = DotNodeStyles.buildEdge(
								nodes.get(arc.getSource().getId().toString()),
								nodes.get(arc.getTarget().getId().toString())
						);
					}
					
					visualisation.addEdge(
							arcDot
					);
				} else {
					throw new IllegalStateException("Unable to find a class type for a petrinet node:: "+ node.getClass());
				}
//				collect the nodes for the next iteration
				getNextNodes(p, t);
			}
			group++;
//			System.out.println("Number of new elements:: "+next.size());
//			add elements to seen
			seen.addAll(curr.stream()
					.filter(node -> !(node instanceof VariableAccess))
					.map(node -> node.getClass().equals(Arc.class) ? ((Arc)node).getLocalID().toString() : ((PetrinetNode)node).getId().toString())
					.collect(Collectors.toList()));
//			System.out.println("Number of seen elements:: "+seen.size());
			curr = new ArrayList<Object>();
			curr.addAll(next.stream().sorted(Comparator.comparing(PetrinetNode::getLabel)).collect(Collectors.toList()));
			curr.addAll(nextEdges);
		}
//		all end place holder
		group++;
		rightAnchor.setOption("group", Integer.toString(group));
		this.visualisation.addNode(rightAnchor);
	}
	
	/**
	 * Finds the output nodes for the given p or t, which ever is not null.
	 * @param p A possible current place
	 * @param t A possible current transition
	 */
	private void getNextNodes(Place p, Transition t) {
//		get edges with this element as source
		List<PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode>> edges;
		PetriNetWithData graph = this.graph;
		if (p != null) {
			final Place pp = p;
			edges = graph.getEdges().stream()
				.filter(ed -> {return ed.getSource().getId().toString().equals(pp.getId().toString());})
				.collect(Collectors.toList());
//			System.out.println("Place="+pp.getLabel()+" has "+edges.size()+" arcs.");
		} else if (t != null) {
			final Transition tt = t;
			edges = graph.getEdges().stream()
				.filter(ed -> {return ed.getSource().getId().toString().equals(tt.getId().toString());})
				.collect(Collectors.toList());
//			System.out.println("trans="+tt.getLabel()+" has "+edges.size()+" arcs.");
		} else {
			edges = null;
		}
//		get targets for edges
		if (edges != null) {
			for(PetrinetEdge< ? extends PetrinetNode, ? extends PetrinetNode> edge: edges) {
				if (!seen.contains(edge.getTarget().getId().toString()) & !next.contains(edge.getTarget())) {
					next.add(edge.getTarget());
				}
				nextEdges.add(edge);
			}
		}
	}
	
	
	/**
	 * Builds a styled DotNode instances depending on if the place is part of the initial, final marking.<br>
	 * Otherwise returns the default styling. 
	 * @param p Place to be reconsidered as an ExoDotPlace
	 * @return a styled dot node
	 */
	private DotNode handlePlace(Place p) {
		DotNode pp = null;
		if (initial.contains(p)) {
			pp = DotNodeStyles.buildStartingPlaceNode(p.getId().toString());
		}
		else if (end.contains(p)) {
			pp = DotNodeStyles.buildEndingPlaceNode(p.getId().toString());
		} else {
			pp = DotNodeStyles.buildPlaceNode(p.getLabel());
		}
		nodes.put(p.getId().toString(), pp);
		return pp;
	}
	
	private DotNode handleTransition(Transition t) {
		Transition oldTrans = t;
		DotNode newNode;
		if (this.rules != null) {
			if (this.rules.containsKey(t.getId().toString())) {
				if (this.modelLogInfo != null) {
					newNode = DotNodeStyles.buildRuleTransition(oldTrans, this.modelLogInfo, this.rules.get(t.getId().toString()), swapMap);
				} else {
					newNode = DotNodeStyles.buildRuleTransition(oldTrans, this.rules.get(t.getId().toString()), swapMap);
				}
				
			} else {
				if (this.modelLogInfo != null) {
					newNode = DotNodeStyles.buildNoRuleTransition(oldTrans, this.modelLogInfo, swapMap);
				} else {
					newNode = DotNodeStyles.buildNoRuleTransition(oldTrans, swapMap);
				}
			}
		} else {
			if (this.modelLogInfo != null) {
				newNode = DotNodeStyles.buildNoRuleTransition(oldTrans, this.modelLogInfo, swapMap);
			} else {
				newNode = DotNodeStyles.buildNoRuleTransition(oldTrans, swapMap);
			}
		}
		transitions.add((ExoDotTransition)newNode);
		return newNode;
	}
	
	/**
	 * Finds the original transition by nodeID using some mapping.
	 * @param t new transition
	 * @param mapper the mapping between old and new transitions
	 * @return the old tranistion
	 */
	private Transition findOldTrans(Transition t, Map<Transition,Transition> mapper) {
		Transition map = null;
		for (Entry<Transition, Transition> entry : mapper.entrySet()) {
			if (entry.getValue().equals(t)) {
				map = entry.getKey();
				break;
			}
		}
		return map;
	}
	
	public static class ShouterListener extends MouseAdapter {
		
		private DotNode source;
		
		public ShouterListener(DotNode source) {
			this.source = source;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			System.out.println(source.getLabel()+ " Clicked!");
		}
	}
}
