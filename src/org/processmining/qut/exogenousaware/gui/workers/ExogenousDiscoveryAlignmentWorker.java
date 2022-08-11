package org.processmining.qut.exogenousaware.gui.workers;

import javax.swing.SwingWorker;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class ExogenousDiscoveryAlignmentWorker extends SwingWorker<PNRepResult, Integer> {

	@NonNull CostBasedCompleteParam parameters;
	@NonNull UIPluginContext context;
	@NonNull TransEvClassMapping mapping;
	@NonNull PetrinetGraph net;
	@NonNull XLog endogenousLog;
	
	protected PNRepResult doInBackground() throws Exception {
		// TODO Auto-generated method stub
		return new PNLogReplayer().replayLog(
				context,
				net,
				endogenousLog,
				mapping,
				new PetrinetReplayerWithILP(),
				parameters
				);
	}
	
}
