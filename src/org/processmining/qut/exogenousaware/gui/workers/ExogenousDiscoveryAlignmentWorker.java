package org.processmining.qut.exogenousaware.gui.workers;

import javax.swing.SwingWorker;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerWithoutILP;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.replayer.PNLogReplayer;
import org.processmining.plugins.petrinet.replayer.algorithms.costbasedcomplete.CostBasedCompleteParam;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;

import lombok.Builder;
import lombok.NonNull;

@Builder
public class ExogenousDiscoveryAlignmentWorker extends SwingWorker<PNRepResult, Integer> {
	
	@NonNull ExogenousDiscoveryProgresser progress;

	@NonNull CostBasedCompleteParam parameters;
	@NonNull UIPluginContext context;
	@NonNull TransEvClassMapping mapping;
	@NonNull PetrinetGraph net;
	@NonNull XLog endogenousLog;
	
	protected PNRepResult doInBackground() throws Exception {
		
		DummyContext dummy = new DummyContext(context, "tester");
		dummy.setProgresser(progress);
		// TODO Auto-generated method stub
		return new PNLogReplayer().replayLog(
				dummy,
				net,
				endogenousLog,
				mapping,
				new PetrinetReplayerWithoutILP(),
				parameters
				);
	}
	
	private class DummyContext extends UIPluginContext {

		private DummyProgress progresser;
		
		protected DummyContext(UIPluginContext context, String label) {
			super(context, label);
		}
		
		public void setProgresser(ExogenousDiscoveryProgresser progresser) {
			this.progresser = new DummyProgress(progresser);
		}
		
		@Override
		public Progress getProgress() {
			// TODO Auto-generated method stub
			return this.progresser;
		}
		
	}
	
	private class DummyProgress implements Progress {
		
		private ExogenousDiscoveryProgresser progresser;
		
		public DummyProgress(ExogenousDiscoveryProgresser progresser) {
			this.progresser = progresser;
		}

		public void setMinimum(int value) {
			// TODO Auto-generated method stub
			
		}

		public void setMaximum(int value) {
//			System.out.println("max set:"+value);
			progresser.getState(ProgressType.Alignment).setTotal(value);
		}

		public void setValue(int value) {
//			System.out.println("value set:"+value);
			progresser.getState(ProgressType.Alignment).setCurrent(value);
			progresser.getState(ProgressType.Alignment).setProgress(value);
		}

		public void setCaption(String message) {
			// TODO Auto-generated method stub
			progresser.getState(ProgressType.Alignment).setCaption(message);
			System.out.println("[ExogenousDiscoveryAlignment] "+message);
		}

		public String getCaption() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getValue() {
			int progress = progresser.getState(ProgressType.Alignment).getProgress();
//			System.out.println("got progress:"+progress);
			return progress;
		}

		public void inc() {
//			System.out.println("progress incremented");
			progresser.getState(ProgressType.Alignment).increment();
			
			
		}

		public void setIndeterminate(boolean makeIndeterminate) {
			// TODO Auto-generated method stub
			
		}

		public boolean isIndeterminate() {
			// TODO Auto-generated method stub
			return false;
		}

		public int getMinimum() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getMaximum() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		public void cancel() {
			// TODO Auto-generated method stub
			progresser.getState(ProgressType.Alignment).setCaption("Alignment cancelled");
		}
		
	}
	
}
