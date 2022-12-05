package org.processmining.qut.exogenousaware.gui.workers;

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingWorker;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressState;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;
import org.processmining.qut.exogenousaware.measures.datapetrinets.DecisionPrecision;
import org.processmining.qut.exogenousaware.measures.datapetrinets.DecisionRecall;
import org.processmining.qut.exogenousaware.stats.models.ModelStatistics;
import org.processmining.qut.exogenousaware.stats.models.ProcessModelStatistics;

import lombok.Builder;
import lombok.NonNull;


@Builder
public class ExogenousDiscoveryMeasurementWorker extends SwingWorker<Map<String, Double>, Integer> {

	@NonNull XLog endogenousLog;
	@NonNull Object model;
	@NonNull ModelStatistics statistics;
	@NonNull PNRepResult alignment;
	@NonNull Map<String,String> variableMap;
	@NonNull ExogenousDiscoveryProgresser progresser;
	
	
	protected Map<String, Double> doInBackground() throws Exception {
		Map<String,Double> measures = new HashMap();
		
		
		if (statistics instanceof ProcessModelStatistics) {
			((ProcessModelStatistics) statistics ).clearMeasures();
		}
		
//		setup measure state
		int total = 0;
		for(Object moment : statistics.getDecisionMoments()) {
			total += statistics.getDecisionOutcomes(moment).size();
		}
		ProgressState state = progresser.getState(ProgressType.Measurements);
		state.setTotal(total * 2);
		state.setProgress(0);
//		measure decision recall
		double recall = DecisionRecall.builder()
				.progressInc(1)
				.progresser(progresser)
				.variableMapping(variableMap)
				.build()
				.measure(endogenousLog, model, statistics, alignment);
		measures.put(DecisionRecall.NAME, recall);
		
		double precision = DecisionPrecision.builder()
				.progressInc(1)
				.progresser(progresser)
				.variableMapping(variableMap)
				.build()
				.measure(endogenousLog, model, statistics, alignment);
		measures.put(DecisionPrecision.NAME, precision);
		
		return measures;
	}

}
