package org.processmining.qut.exogenousaware.data;

import java.awt.Color;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.exceptions.CannotConvertException;
import org.processmining.qut.exogenousaware.exceptions.ExogenousAttributeNotFoundException;
import org.processmining.qut.exogenousaware.exceptions.LinkNotFoundException;
import org.processmining.qut.exogenousaware.gui.colours.ColourScheme;
import org.processmining.qut.exogenousaware.steps.linking.Linker;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Builder
public class ExogenousDataset {

	@NonNull @Getter XLog source;

	@Default @Getter ExogenousDatasetLinkType linkType = null;
	@Default @Getter ExogenousDatasetType dataType = null;
	@Default @Getter @Setter Color colourBase = ColourScheme.green;
	@Default @Getter Linker linker = null;
	@Default private Boolean setupCompleted = false;



	public ExogenousDataset setup() throws CannotConvertException {
		if (ExogenousUtils.isExogenousLog(source)) {
			// attempt to determine data type for measurements in log
			try {
				dataType = ExogenousUtils.findDataType(source);
				System.out.println("[ExogenousDataset] data type set as :: "+dataType.toString());
			} catch (ExogenousAttributeNotFoundException e) {
				System.out.println("[ExogenousDataset] Unable to determine datatype of log :: "+this.source.getAttributes().get("concept:name").toString());
				throw new CannotConvertException(source, this);
			}
			// attempt to determine link type for data set
			try {
				linkType = ExogenousUtils.findLinkType(source);
				System.out.println("[ExogenousDataset] link type set as :: "+linkType.toString());
			} catch (ExogenousAttributeNotFoundException e) {
				System.out.println("[ExogenousDataset] Unable to determine datatype of log :: "+this.source.getAttributes().get("concept:name").toString());
				throw new CannotConvertException(source, this);
			}		
			// attempt to construct a linker for data set
			linker = ExogenousUtils.constructLinker(source, this);
			System.out.println("[ExogenousDataset] linker created as :: "+linker.getClass().getSimpleName());

		} else {
			throw new CannotConvertException(source, this);
		}
		this.setupCompleted = true;
		return this;
	}



	/**
	 * Checks for a link between a trace and this exogenous dataset.
	 * @param trace to check for linkage
	 * @return whether linkage was found.
	 */
	public boolean checkLink(XTrace trace) {
		return linker.link(trace, source).size() > 0;
	}


	/**
	 * Checks for a link between a trace and returns the links found.
	 * @param trace to use as link source
	 * @return a collection of links
	 */
	public List<XTrace> findLinkage(XTrace trace) throws LinkNotFoundException {
		//		check for link
		//		if (!checkLink(trace)) {
		//			throw new LinkNotFoundException();
		//		}
		//		otherwise, find links
		return linker.link(trace, source);
	}

	public String getName() {
		String name = "Exogenous Data Set";
		boolean fallback = false;
		//		attempt to find exogenous:name on trace
		try {
			XTrace trace = this.source.get(0);
			name = trace.getAttributes().get("exogenous:name").toString();
		} catch (Exception e) {
			fallback = true;
		}
		if (fallback) {
			if (this.source.getAttributes().containsKey("concept:name")) {
				name = this.source.getAttributes().get("concept:name").toString();
			}
		}
		return name;
	}

	@Override
	public String toString() {
		return getName() + "(" + this.dataType.getLabel() + ")";
	}

}
