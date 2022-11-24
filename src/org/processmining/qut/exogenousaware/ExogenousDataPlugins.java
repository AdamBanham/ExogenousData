package org.processmining.qut.exogenousaware;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.apache.commons.lang.NotImplementedException;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithDataFactory;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.exceptions.CannotConvertException;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceExplorer;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.steps.determination.configs.AIIM2022;



/**
 * Plugins for process mining with exogenous data as presented in:<br>
 * <b>xPM: Process Mining with Exogenous Data [1]</b><br>
 * <br>
 * Current plugins are:<br>
 * <ul>
 * <li>Exogenous Aware Log Preperation [1,2]</li>
 * <li>Exogenous Trace Visualisation (Visualiser) [1,2]</li>
 * <li>Exogenous Aware Discovery [1,2]</li>
 * <li>Exogenous Discovery Investigator (Visualiser) [1,2]</li>
 * <li>Exogenous Aware Enhancement (EESA Visualisations and Ranking) [2]</li>
 *</ul>
 *<br>
 *[1] 	A. Banham, S. J. J. Leemans, M. T. Wynn, R. Andrews, xPM: A framework 
 *       for process mining with exogenous data, in: Process Mining Workshops - 
 *       ICPM 2021 International Workshops, volume 433 of Lecture Notes in 
 *       Business Information Processing, Springer, 2021, pp. 85–97.
 *[2]	xPM: Enhancing Exogenous Data Visibility. Adam Banham, Sander J.J. 
 *		 Leemans, Moe T. Wynn, Robert Andrews, Kevin B. Laupland, Lucy Shinners.
 *		 Artificial Intelligence in Medicine 2022 (Accepted as-of 24/09/2022).
*/
public class ExogenousDataPlugins {
	
	public static final String version = "<br> Package Version: 0.0.5.beta";
	
	
	@Plugin(
			name = "Exogenous Annotated Log Preparation",
			parameterLabels = {"Event Log", "Exo-Panels"},
			returnLabels = {"Exogenous Annotated Log"},
			returnTypes = {ExogenousAnnotatedLog.class},
			help="Given an event log and several exo-panels, this plugin allows"
					+ " users to create determinations as identified by xPM [1]."
					+ " After building determinations, each one will be applied"
					+ " to all traces seen in the event log. Note that an xlog "
					+ "will be made but all changes will be done in place. This"
					+ " process is not memory efficient and may require systems "
					+ "to have more than 12 GB of heap available depending on the"
					+ " size of the exo-panels and event log. [1] xPM: Enhancing"
					+ " Exogenous Data Visibility. Adam Banham et. al. Artificial"
					+ " Intelligence in Medicine 2022 <br> See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			categories={PluginCategory.Analytics, PluginCategory.Enhancement},
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au",
			pack = "ExogenousData"
	)
	public ExogenousAnnotatedLog preperation(UIPluginContext context, 
			XLog endogenous, XLog[] exogenous) throws Throwable{
		List<ExogenousDataset> exoLogs = new ArrayList<ExogenousDataset>();
		for(XLog elog: exogenous) {
			ExogenousDataset temp;
			try {
				temp = ExogenousDataset.builder()
						.source(elog)
						.build()
						.setup();
			} catch (CannotConvertException e) {
				// if log cannot naively be convert to dataset then move on
				System.out.println(
					"[ExogenousAnnotatedLog] Cannot convert log='"
					+ elog.getAttributes().get("concept:name").toString()+"' "
					+ "to an exogenous dataset.");
				continue;
			}
			exoLogs.add(temp);
		}
		ExogenousAnnotatedLog annotated = ExogenousAnnotatedLog
				.builder()
				.endogenousLog(endogenous)
				.exogenousDatasets(exoLogs)
				.classifiers(endogenous.getClassifiers())
				.extensions(endogenous.getExtensions())
				.useDefaultConfiguration(false)
				.globalEventAttributes(endogenous.getGlobalEventAttributes())
				.globalTraceAttributes(endogenous.getGlobalTraceAttributes())
				.attributes(endogenous.getAttributes())
				.parsed(false)
				.build()
				.setup(context);
		return annotated;
		
	}
	
	@Plugin(
			name = "Exogenous Annotated Log Preparation (AIIM 2022)",
			parameterLabels = {"Event Log", "Exo-Panels"},
			returnLabels = {"Exogenous Annotated Log"},
			returnTypes = {ExogenousAnnotatedLog.class},
			help="Given an event log and several exo-panels, this plugin allows"
				 + " users to reproduce the xPM instantition used in :"
				 + " xPM: Enhancing Exogenous Data Visibility. Adam "
				 + "Banham et. al. Artificial Intelligence in Medicine 2022"
				 + version,
			categories={PluginCategory.Analytics, PluginCategory.Enhancement},
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au",
			pack = "ExogenousData"
	)
	public ExogenousAnnotatedLog AIIM2022preperation(UIPluginContext context, 
			XLog endogenous, XLog[] exogenous) throws Throwable {
		List<ExogenousDataset> exoLogs = new ArrayList<ExogenousDataset>();
		for(XLog elog: exogenous) {
			ExogenousDataset temp;
			try {
				temp = ExogenousDataset.builder()
						.source(elog)
						.build()
						.setup();
			} catch (CannotConvertException e) {
				// if log cannot naively be convert to dataset then move on
				System.out.println("[ExogenousAnnotatedLog] Cannot convert log="
						+ "'"
						+ elog.getAttributes().get("concept:name").toString()
						+"' to an exogenous dataset.");
				continue;
			}
			exoLogs.add(temp);
		}
		ExogenousAnnotatedLog annotated = ExogenousAnnotatedLog
				.builder()
				.endogenousLog(endogenous)
				.exogenousDatasets(exoLogs)
				.classifiers(endogenous.getClassifiers())
				.extensions(endogenous.getExtensions())
				.useDefaultConfiguration(true)
				.determinations(AIIM2022.getConfiguration(exoLogs))
				.globalEventAttributes(endogenous.getGlobalEventAttributes())
				.globalTraceAttributes(endogenous.getGlobalTraceAttributes())
				.attributes(endogenous.getAttributes())
				.parsed(false)
				.build()
				.setup(context);
		return annotated;
		
	}
	
	
	
	@Plugin(
			name = "Exogenous Annotated Log Explorer",
			parameterLabels = {"Exogenous Annotated Log"},
			returnLabels = {"ExogenousTraceExplorer"}, 
			returnTypes = {ExogenousTraceView.class},
			help="This plugin allows users to explore an xlog through a GUI. "
					+ "Users can see slices that were related to events and "
					+ "see how the original exo-series evolved in comparision to"
					+ " the execution of the trace. See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			categories={PluginCategory.Analytics, PluginCategory.Enhancement},
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au",
			pack = "ExogenousData"
	)
	@Visualizer
	public JComponent exogenousAnnotationViewing(UIPluginContext context, 
			ExogenousAnnotatedLog xlog) throws Throwable {
		return new ExogenousTraceExplorer().visualise(
				context
				,
				ExogenousTraceView.builder()
				   .source(xlog)
				   .context(context)
				   .build()
				   .setup()
		);
	}
	
	@Plugin(
			name = "(Non) Exogenous Aware Discovery (DPN)",
			parameterLabels = {"Event log","Control Flow (DPN)"},
			returnLabels = {"Exogenous Discovery Investigator"},
			returnTypes = {ExogenousDiscoveryInvestigator.class},
			categories={PluginCategory.Analytics, PluginCategory.Enhancement,
						PluginCategory.Discovery
			},
			help="This plugin allows users to perform various process discovery "
					+ "methods using an event log (converted without exogenous data)"
					+ "and a control flow description. "
					+ " Such as performing decision mining and then exploring "
					+ "annotated transition guards using a visual format."
					+ "<br> See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			userAccessible = true
	)
	@UITopiaVariant(affiliation = "QUT",
		author = "A. Banham", 
		email = "adam.banham@hdr.qut.edu.au",
		pack = "ExogenousData"
	)
	public ExogenousDiscoveryInvestigator NonExogenousDiscovery_DPN(
			UIPluginContext context, XLog eventlog,
			PetriNetWithData dpn) throws Throwable {
		
		ExogenousAnnotatedLog xlog = ExogenousAnnotatedLog.builder()
				.endogenousLog(eventlog)
				.parsed(false)
				.showConfiguration(false)
				.build()
				.setup(context);
		
		ExogenousDiscoveryInvestigator edi = ExogenousDiscoveryInvestigator.builder()
				.source(xlog)
				.controlflow(dpn)
				.context(context)
				.build()
				.setup()
				.precompute();
		
		return edi;
	}
	
	@Plugin(
			name = "Exogenous Aware Discovery (DPN)",
			parameterLabels = {"Exogenous Annotated Log (xlog)","Control Flow (DPN)"},
			returnLabels = {"Exogenous Discovery Investigator"},
			returnTypes = {ExogenousDiscoveryInvestigator.class},
			categories={PluginCategory.Analytics, PluginCategory.Enhancement,
						PluginCategory.Discovery
			},
			help="This plugin allows users to perform various process discovery "
					+ "methods using an xlog and a control flow description. "
					+ " Such as performing decision mining and then exploring "
					+ "annotated transition guards using a visual format."
					+ "<br> See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			userAccessible = true
	)
	@UITopiaVariant(affiliation = "QUT",
		author = "A. Banham", 
		email = "adam.banham@hdr.qut.edu.au",
		pack = "ExogenousData"
	)
	public ExogenousDiscoveryInvestigator ExogenousDiscovery_DPN(
			UIPluginContext context, ExogenousAnnotatedLog exogenous,
			PetriNetWithData dpn) throws Throwable {
		
		ExogenousDiscoveryInvestigator edi = ExogenousDiscoveryInvestigator.builder()
				.source(exogenous)
				.controlflow(dpn)
				.context(context)
				.build()
				.setup()
				.precompute();
		
		return edi;
	}
	
	@Plugin(
			name = "Exogenous Aware Discovery (PN)",
			parameterLabels = {"Exogenous Annotated Log (xlog)","Control Flow (PN)"},
			returnLabels = {"Exogenous Discovery Investigator"},
			returnTypes = {ExogenousDiscoveryInvestigator.class},
			categories={PluginCategory.Analytics, PluginCategory.Enhancement,
						PluginCategory.Discovery
			},
			help="This plugin allows users to perform various process discovery "
					+ "methods using an xlog and a control flow description. "
					+ " Such as performing decision mining and then exploring "
					+ "annotated transition guards using a visual format."
					+ "<br> See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			userAccessible = true
	)
	@UITopiaVariant(affiliation = "QUT",
		author = "A. Banham", 
		email = "adam.banham@hdr.qut.edu.au",
		pack = "ExogenousData"
	)
	public ExogenousDiscoveryInvestigator ExogenousDiscovery_PN(
			UIPluginContext context, ExogenousAnnotatedLog exogenous,
			Petrinet pn) throws Throwable {
		
		PetriNetWithDataFactory factory = new PetriNetWithDataFactory(pn, pn.getLabel());
		factory.cloneInitialAndFinalConnection(context);
		PetriNetWithData dpn = factory.getRetValue();
		
		ExogenousDiscoveryInvestigator edi = ExogenousDiscoveryInvestigator.builder()
				.source(exogenous)
				.controlflow(dpn)
				.context(context)
				.build()
				.setup()
				.precompute();
		
		return edi;
	}
	
	@Plugin(
			name = "Exogenous Aware Discovery (PT)",
			parameterLabels = {"Exogenous Annotated Log (xlog)","Control Flow (PT)"},
			returnLabels = {"Exogenous Discovery Investigator"},
			returnTypes = {ExogenousDiscoveryInvestigator.class},
			categories={PluginCategory.Analytics, PluginCategory.Enhancement,
						PluginCategory.Discovery
			},
			help="This plugin allows users to perform various process discovery "
					+ "methods using an xlog and a control flow description. "
					+ " Such as performing decision mining and then exploring "
					+ "annotated transition guards using a visual format."
					+ "<br> See "
					+ " <a href=\"https://youtu.be/iSklEeNUJSc\" target=\"_blank\">"
					+ "https://youtu.be/iSklEeNUJSc</a> for a walkthough of tooling."
					+ version,
			userAccessible = true
	)
	@UITopiaVariant(affiliation = "QUT",
		author = "A. Banham", 
		email = "adam.banham@hdr.qut.edu.au",
		pack = "ExogenousData"
	)
	public ExogenousDiscoveryInvestigator ExogenousDiscovery_PT(
			UIPluginContext context, ExogenousAnnotatedLog exogenous,
			ProcessTree pt) throws Throwable {
		
		PetrinetWithMarkings result = ProcessTree2Petrinet.convert(pt, true);
		
		Petrinet pn = result.petrinet;
		
		context.addConnection(new InitialMarkingConnection(pn, result.initialMarking));
		context.addConnection(new FinalMarkingConnection(pn, result.finalMarking));
		
		PetriNetWithDataFactory factory = new PetriNetWithDataFactory(pn, pn.getLabel());
		factory.cloneInitialAndFinalConnection(context);
		PetriNetWithData dpn = factory.getRetValue();
		
		ExogenousDiscoveryInvestigator edi = ExogenousDiscoveryInvestigator.builder()
				.source(exogenous)
				.controlflow(dpn)
				.context(context)
				.build()
				.setup()
				.precompute();
		
		return edi;
	}
	
	@Plugin(
			name="Exogenous Discovery Investigator",
			level=PluginLevel.NightlyBuild,
			returnLabels= {"Exogenous Annotated Explorer UI"},
			returnTypes = { JComponent.class },
			userAccessible = true, 
			parameterLabels = { "" }
	)
	@Visualizer
	public JComponent exogenousDiscoveryViewing(UIPluginContext context, 
			ExogenousDiscoveryInvestigator edi) {
		return edi;
	}
	
	@Plugin(
			name = "Exogenous Aware Enhancement",
			parameterLabels = {"Exogenous Annotated Log", "xDPN",},
			categories={PluginCategory.Analytics, PluginCategory.Enhancement},
			help="This plugin allows users to build and extract EESA visualisations."
					+ " This plugin is currently under construction, and as such"
					+ " is not fully implemented for use via a GUI."
					+ version,
			returnLabels = {}, returnTypes = {}, userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au",
			pack = "ExogenousData"
	)
	public void exogenousEnhancement(UIPluginContext context, 
			XLog exogenous, PetriNetWithData model) throws Throwable {
		throw new NotImplementedException("Still under construction...");
	}
}
