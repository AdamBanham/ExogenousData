package org.processmining.qut.exogenousaware.gui.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartUtils;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementGraphInvestigator;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementGraphInvestigator.RankedListItem;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;

@Builder
public class EnhancementRankExporter extends SwingWorker<Boolean, Integer> {

	@NonNull ExogenousEnhancementGraphInvestigator gui;
	@NonNull List<RankedListItem> ranks;
	@NonNull JProgressBar progress;
	
	@Default private String dumpLocation = "C:\\Users\\n7176546\\OneDrive - Queensland University of Technology\\Desktop\\narratives\\dump_temp\\ranks\\"; 
	
	
	protected Boolean doInBackground() throws Exception {
		// export ranks in rank order
		BufferedWriter writer = null;
		progress.setValue(0);
		progress.setMaximum(ranks.size());
		try {
//			create write for csv
			File f = new File(dumpLocation);
			f.mkdirs();
			f = new File(dumpLocation+"rank_information.csv");
			writer = new BufferedWriter(new FileWriter(f));
			writer.write("rank,distance,wilcoxon,common,transition,exogenous,group_1,group_2,group_1_name,group_2_name,path");
			writer.newLine();
			for(RankedListItem item: ranks) {
				String path = saveMedianChart(item,dumpLocation);
				extractRankInformation(item,writer,path);
				
				progress.setValue(progress.getValue()+1);
			}
			writer.close();
			System.out.println("[EnhancementRankExporter] export finished check :: " + dumpLocation);
			return true;
		} catch (Exception e) {
			writer.close();
			System.out.println("[EnhancementRankExporter] Failed to finish extraction.");
			e.printStackTrace();
			return false;
		}
	}
	
	private String saveMedianChart(RankedListItem item, String dumpFolder) throws InterruptedException, IOException {
		EnhancementMedianGraph grapher = (EnhancementMedianGraph) item.getController().getCachedGraphs().get("Median-S");
		try {
		while (!grapher.isDone()) {
			grapher.execute();
			Thread.sleep(1500);
		}
		} catch (Exception e) {
			System.out.println("[EnhancementRankExporter] ERROR :: unable to make graph.");
			return "failed";
		}
		String chartFilenameFormat = "r%04d_d%.6f_p%.6f_%s.png";
		int rank = item.getRank();
		double distance = item.rankable() ? item.getRankDistance() : -1.0;
		double wilcoxon = item.rankable() ? item.getWilcoxonP() : -1.0;
		String name = item.getController().getTransName()+ "_" + item.getController().getDatasetName();
		name = name.replace(":", "_").replace("/", "_");
		File f = new File(
				dumpFolder +
				String.format(chartFilenameFormat, rank, distance, wilcoxon, name)
		);
		f.createNewFile();
		ChartUtils.saveChartAsPNG(f, grapher.getGraph().getChart(), 1800, 600);
		return f.getName();
	}
	
	private void extractRankInformation(RankedListItem item, BufferedWriter writer, String path) throws IOException {
		String rowFormat = "%04d,%.6f,%.6f,%d,%s,%s,%d,%d,%s,%s,%s";
//		get row data
		int rank = item.getRank();
		double distance = item.rankable() ? item.getRankDistance() : -1.0;
		double wilcoxon = item.rankable() ? item.getWilcoxonP() : -1.0;
		int common = item.getCommonLength();
		String transition = item.getController().getTransName();
		String exogenous = item.getController().getDatasetName();
		int group_1 = 0;
		int group_2 = 0;
		String group_1_name = "";
		String group_2_name = "";
//		check if graph has groups
		if (item.getController().isUseGroups()) {
			group_1 = item.getController().getGroups().stream()
					.filter(g -> g == 1)
					.reduce(0, (c,n) -> c+1);
			group_2 = item.getController().getGroups().stream()
					.filter(g -> g == 0)
					.reduce(0, (c,n) -> c+1);
			group_1_name = item.getController().getGrouper().getGroupName(1);
			group_2_name = item.getController().getGrouper().getGroupName(0);
		}
//		write row data
		writer.write(
				String.format(rowFormat,
						rank, 
						distance,
						wilcoxon,
						common,
						transition,
						exogenous,
						group_1,
						group_2,
						group_1_name,
						group_2_name,
						path
						)
		);
		writer.newLine();
	}
	
	@Override
	protected void done() {
		this.gui.changeToRankedList();
		this.gui.getBack().setEnabled(true);
	}
	

}
