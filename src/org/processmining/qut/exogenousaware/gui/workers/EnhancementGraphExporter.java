package org.processmining.qut.exogenousaware.gui.workers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import org.jfree.chart.ChartUtils;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.qut.exogenousaware.gui.ExogenousEnhancementTracablity;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotEdge;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotPlace;
import org.processmining.qut.exogenousaware.gui.dot.ExoDotTransition;
import org.processmining.qut.exogenousaware.gui.panels.EnhancementExogenousDatasetGraphController;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementAnalysis;

import lombok.Builder;
import lombok.NonNull;


@Builder
public class EnhancementGraphExporter extends SwingWorker<Boolean, Integer> {

	@NonNull private ExogenousEnhancementTracablity gui;
	@NonNull private ExogenousEnhancementAnalysis datacontroller;
	@NonNull private Path dirPath;
	
	private boolean work() {
//		loop through all transitions on controlflow model
		List<ExoDotTransition> trans = walkGraph();
		int transCounter = 1;
		for(ExoDotTransition tran: trans) {
			if (tran.getTransLabel().contains("tau")) {
				continue;
			}
			System.out.println("[EnhancementGraphExporter]-"+transCounter+" Starting on trans="+tran.getTransLabel());
//			call build graphs for transition
			tran.setTransLabel(String.format("%03d - %s", transCounter, tran.getTransLabel()));
			gui.getVis().updateSelectedNode(tran);
			gui.getVis().getVis().changeDot(gui.getVis().getVis().getDot(), false);
			datacontroller.updateAnalysis(tran);
//			wait for graphs to be ready
			Boolean waiting = true;
			while (waiting) {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (datacontroller.getCacheUniverse().containsKey(tran)) {
					waiting = false;
				}
			}
			System.out.println("[EnhancementGraphExporter]-"+transCounter+" graphs built");
//			export all (median) graphs to given path 
			List<String> graphkeys = this.datacontroller.getExoCharts().keySet()
					.stream().filter(k -> k.contains(tran.getControlFlowId())).collect(Collectors.toList());
			
			try {
				for(String graphkey: graphkeys) {
					if (!this.datacontroller.getExoCharts().containsKey(graphkey)) {
						System.out.println("[EnhancementGraphExporter] cannot find graphkey="+graphkey+ " in controller, moving on...");
						continue;
					}
					EnhancementExogenousDatasetGraphController graphcontroller = (EnhancementExogenousDatasetGraphController) this.datacontroller.getExoCharts().get(graphkey);
					EnhancementMedianGraph grapher = (EnhancementMedianGraph) graphcontroller.getCachedGraphs().get("Median-S");
					
					if (!grapher.isDone()) {
						grapher.execute();
					}
					while (!grapher.isDone()) {
							Thread.sleep(1500);
					}					
					
					String filename = tran.getTransLabel()+"__"+ grapher.getTitle();
					filename = filename.replace("-", "_").replace(":", "_").replace("/", "").replace(" ", "_");
					filename = this.dirPath.toAbsolutePath() + File.separator + filename +".png";
					System.out.println("[EnhancementGraphExporter] creating graph="+filename);
					File f = new File(filename);
					f.createNewFile();
					ChartUtils.saveChartAsPNG(f, grapher.getGraph().getChart(), 1800, 600);
				}
			} catch (IOException e) {
				System.out.println("[EnhancementGraphExporter] Graph Exporter ran into IO exception from writing chart");
				e.getCause().printStackTrace();
			} catch (Exception e) {
				System.out.println("[EnhancementGraphExporter] Graph Exporter had unexpected error");
				e.getCause().printStackTrace();
			}
//			number exports sequentially 
			transCounter++;
		}
//		if all exports work return true, else return false
		System.out.println("[EnhancementGraphExporter] finished exporting to "+this.dirPath.toString());
		return true;
	}
	
	protected Boolean doInBackground() throws Exception {
		boolean result = false;
		try {
			result = work();
		} catch (Exception e) {
			System.out.println("[EnhancementGraphExporter] ERROR :: failed to complete work.");
			e.getCause().printStackTrace();
			e.printStackTrace();
		}
		return result;
	}
	
	private List<ExoDotTransition> walkGraph(){
		List<ExoDotTransition> orderedTrans = new ArrayList<ExoDotTransition>();
//		walk graph along edges, add trans as we seen them 
		Dot graph = gui.getVis().getVis().getDot();
//		get all places and edges
		List<ExoDotPlace> places = graph.getNodes().stream()
				.filter(n -> n.getClass().equals(ExoDotPlace.class))
				.map(n -> (ExoDotPlace)n)
				.collect(Collectors.toList());
		List<ExoDotEdge> edges = graph.getEdges().stream()
				.filter(e -> e.getClass().equals(ExoDotEdge.class))
				.map(e -> (ExoDotEdge)e)
				.collect(Collectors.toList());
//		find the place with no incoming edges
		ExoDotPlace start = null;
		for(ExoDotPlace place: places) {
			if (place.getOption("xlabel").equals("START")) {
				start = place;
				break;
			}
		}
		if (start == null) {
			System.out.println("[EnhancementGraphExporter] Unable to find place with no incoming edges");
			return orderedTrans;
		} 
//		walk from start to end
		List<ExoDotPlace> seen = new ArrayList<ExoDotPlace>();
		seen.add(start);
		List<ExoDotPlace> queue = new ArrayList<ExoDotPlace>();
		queue.add(start);
		while(queue.size() > 0) {
			ExoDotPlace place = queue.get(0);
			queue.remove(0);
//			find all edges from this place, then collect transitions
			List<ExoDotTransition> pedges = edges.stream()
					.filter(e -> e.getSource().equals(place))
					.map(e -> e.getTarget())
					.filter(t -> t.getClass().equals(ExoDotTransition.class))
					.map(t -> (ExoDotTransition)t) 
					.collect(Collectors.toList());
//			add all transitions, collect set of places from transition
			Set<ExoDotPlace> newPlaces = new HashSet<ExoDotPlace>();
			for(ExoDotTransition pedge: pedges) {
				orderedTrans.add(pedge);
				if (pedge.getTransLabel().contains("No Action")) {
					continue;
				}
				List<ExoDotPlace> tplaces = edges.stream()
						.filter(e -> e.getSource().equals(pedge))
						.map(e -> e.getTarget())
						.filter(t -> t.getClass().equals(ExoDotPlace.class))
						.map(t -> (ExoDotPlace)t)
						.filter(p -> !seen.contains(p))
						.collect(Collectors.toList());
				newPlaces.addAll(tplaces);
			}
			seen.addAll(newPlaces);
			queue.addAll(newPlaces);
		}
//		return order transition list
		return orderedTrans;
	}
	
	@Override
	protected void done() {
//		release all resources
		gui.getExport().setEnabled(true);
		gui.getExport().setText("Export");
		gui.getBack().setEnabled(true);
		gui.getRight().setEnabled(true);
	}
	
	@Override
	protected void process(List<Integer> chunks) {
//		update progress bar from this function
	}

}
