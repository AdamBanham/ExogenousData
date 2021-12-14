package org.processmining.qut.exogenousaware.gui.workers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.processmining.qut.exogenousaware.gui.panels.EnhancementExogenousDatasetGraphController;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousEnhancementGraphInvestigator.RankedListItem;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class EnhancementGraphSearchRankedList extends SwingWorker<List<RankedListItem>, String>{
	
	@NonNull private Map<String, JPanel> data;
	@NonNull private JProgressBar progress;
	@Default @Getter private List<RankedListItem> outcome = new ArrayList<RankedListItem>();

	@Default String agreementStatement = "[ExogenousDiscoveryInvestigator] Overall agreement between rank vs wrank :: %.2f%%";
	
	protected List<RankedListItem> doInBackground() throws Exception {
		outcome = data.entrySet().stream()
				  .map((entry) -> this.processItem(entry))
				  .collect(Collectors.toList());
		outcome.sort(new wilcoxonSorter());
		IntStream.range(0, outcome.size())
			.forEach(rank -> outcome.get(rank).setWrank(rank+1));
		outcome.sort(new distanceSorter());
		IntStream.range(0, outcome.size())
			.forEach(rank -> outcome.get(rank).setRank(rank+1));
		int agreement = outcome.stream()
			.filter(o -> o.rankable())
			.map(o -> o.getRank() == o.getWrank())
			.mapToInt(o -> o ? 1 : 0)
			.reduce(0, Integer::sum);
		
		System.out.println(String.format(agreementStatement,(agreement*1.0)/outcome.size()*100));
		return outcome;
	}
	
	private RankedListItem processItem(Entry<String, JPanel> entry) {
		String key = entry.getKey();
		JPanel value = entry.getValue();
		RankedListItem out = RankedListItem.builder()
								.controller((EnhancementExogenousDatasetGraphController)value)
								.id(key)
								.build()
								.rank();
		this.progress.setValue(progress.getValue()+1);
		return out;
	}
	
	private class wilcoxonSorter implements Comparator<RankedListItem>{

		public int compare(RankedListItem o1, RankedListItem o2) {
			if (!o1.rankable() && !o2.rankable()) {
				return 0;
			} else if (!o1.rankable()) {
				return 1;
			} else if (!o2.rankable()) {
				return -1;
			} else {
				return Double.compare(o1.getWilcoxonP(), o2.getWilcoxonP());
			}
		}
	
	}
	
	
	private class distanceSorter implements Comparator<RankedListItem>{

		public int compare(RankedListItem o1, RankedListItem o2) {
			if (!o1.rankable() && !o2.rankable()) {
				return 0;
			} else if (!o1.rankable()) {
				return 1;
			} else if (!o2.rankable()) {
				return -1;
			} else {
				return Double.compare(o1.getRankDistance(), o2.getRankDistance()) * -1;
			}
		}
		
	}

}
