package org.processmining.qut.exogenousaware;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.exceptions.CannotConvertException;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceExplorer;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;
import org.processmining.qut.exogenousaware.steps.determination.configs.AIME2022;



/**
 * Plugins to perform Exogenous-Aware Discovery using the framework presented in:<br>
 * <b>xPM: Process Mining with Exogenous Data [1]</b><br>
 * <br>
 * Current plugins are:<br>
 * <ul>
 * <li>Exogenous Aware Log Preperation [1]</li>
 * <li>Exogenous Trace Visualisation (Visualiser) [1]</li>
 * <li>Exogenous Aware Discovery [1]</li>
 * <li>Exogenous Discovery Investigator (Visualiser)</li>
 * <li>Exogenous Aware Enhancement (EESA Visualisations and Ranking) [x]</li>
 *</ul>
 *<br>
 *[1] 	A. Banham, S. J. J. Leemans, M. T. Wynn, R. Andrews, xPM: A framework for process mining
		with exogenous data, in: Process Mining Workshops - ICPM 2021 International Workshops, volume 
		433 of Lecture Notes in Business Information Processing,
		Springer, 2021, pp. 85–97.
 *[x]	A journal article in the coming future (under review as-of 12/07/2022).
*/
public class ExogenousAwareDiscoveryPlugin {
	
	@Plugin(
			name = "Exogenous Annotated Log Preparation",
			parameterLabels = {"Event Log", "Exo-Panels"},
			returnLabels = {"Exogenous Annotated Log"},
			returnTypes = {ExogenousAnnotatedLog.class},
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au"
	)
	public ExogenousAnnotatedLog preperation(UIPluginContext context, XLog endogenous, XLog[] exogenous) throws Throwable{
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
				System.out.println("[ExogenousAnnotatedLog] Cannot convert log='"+ elog.getAttributes().get("concept:name").toString()+"' to an exogenous dataset.");
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
			name = "Exogenous Annotated Log Preparation (AIME 2022)",
			parameterLabels = {"Event Log", "Exo-Panels"},
			returnLabels = {"Exogenous Annotated Log"},
			returnTypes = {ExogenousAnnotatedLog.class},
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au"
	)
	public ExogenousAnnotatedLog AIME2022preperation(UIPluginContext context, XLog endogenous, XLog[] exogenous) throws Throwable{
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
				System.out.println("[ExogenousAnnotatedLog] Cannot convert log='"+ elog.getAttributes().get("concept:name").toString()+"' to an exogenous dataset.");
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
				.determinations(AIME2022.getConfiguration(exoLogs))
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
			userAccessible = true
	)
	@UITopiaVariant(
			affiliation = "QUT",
			author = "A. Banham",
			email = "adam.banham@hdr.qut.edu.au"
	)
	@Visualizer
	public JComponent exogenousAnnotationViewing(UIPluginContext context, ExogenousAnnotatedLog xlog) throws Throwable{
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
	
	@Plugin(name = "Exogenous Aware Discovery", parameterLabels = {"Exogenous Annotated Log(xlog)","Control Flow DPN"}, returnLabels = {"Exogenous Discovery Investigator"}, returnTypes = {ExogenousDiscoveryInvestigator.class}, userAccessible = true)
	@UITopiaVariant(affiliation = "QUT", author = "A. Banham", email = "adam.banham@hdr.qut.edu.au")
	public ExogenousDiscoveryInvestigator exogenousDiscovery(UIPluginContext context, ExogenousAnnotatedLog exogenous, PetriNetWithData dpn) throws Throwable{
		
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
	public JComponent exogenousDiscoveryViewing(UIPluginContext context, ExogenousDiscoveryInvestigator edi) {
		return edi;
	}
	
	@Plugin(name = "Exogenous Aware Enhancement", parameterLabels = {"Exogenous Annotated Log", "xDPN",}, returnLabels = {}, returnTypes = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "QUT", author = "A. Banham", email = "adam.banham@hdr.qut.edu.au")
	public void exogenousEnhancement(UIPluginContext context, XLog exogenous, PetriNetWithData model) throws Throwable{
		System.out.println("hello world again again again!");
	}
}
