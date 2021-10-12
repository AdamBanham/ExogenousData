package org.processmining.qut.exogenousaware.ab.jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.XLog;
import org.processmining.dataawarereplayer.precision.DataAwarePrecisionPlugin;
import org.processmining.dataawarereplayer.precision.PrecisionConfig;
import org.processmining.dataawarereplayer.precision.PrecisionResult;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.datapetrinets.io.DataPetriNetImporter.DPNWithLayout;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.DataElement;
import org.processmining.xesalignmentextension.XAlignmentExtension;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignedLog;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignment;
import org.processmining.xesalignmentextension.XAlignmentExtension.XAlignmentMove;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ICPM2021A {

	public static void main(HashMap<String,String> args) throws Throwable {
		FileWriter writer = null;
		try {
		// check for all required cmd args 
		if (requires(args) == false) {
			System.out.println("Cannot run job, requires the following inputs:");
			System.out.println("--alog : path to algined log folder");
			System.out.println("--log : path to original log");
			System.out.println("--model : path to dpn folder");
			System.out.println("--output: path to outpu folder");
			System.out.println("cmd args passed were: "+args.keySet());
			return;
		}
		// load in the .xes.gz log 
		XLog logs = null;
		writer = new FileWriter(new File(args.get("output")+"\\precision_scores.csv"));
		writer.write("log,type,param,score\n");
		try {
			System.out.println("[Job] parsing log...");
			logs = new XesXmlGZIPParser().parse(new File(args.get("log"))).get(0);
		} catch (Exception ie) {
			System.out.println("[Job] Unable to parse log: "+args.get("log"));
			throw ie.getCause();
		}
		// load in the aligned .xes.gz log 
		for(Path logPath: Files.walk(Paths.get(args.get("alog"))).filter(file -> !Files.isDirectory(file)).collect(Collectors.toSet())) {
			XLog alignedLogs = null;
			String job = logPath.getFileName().toString().replace(".xes.gz", "");
			System.out.println("[Job] starting "+job+"...");
			try {
				System.out.println("[Job] parsing aligned log...");
				alignedLogs = new XesXmlGZIPParser().parse(logPath.toFile()).get(0);
			} catch (Exception ie) {
				System.out.println("[Job] Unable to parse log: "+args.get("alog"));
				throw ie.getCause();
			}
			String dpnPath = args.get("model")+"\\"+logPath.getFileName().toString().replace(".xes.gz", ".pnml");
			// load in DPN
			DPNWithLayout dpn = null;
			System.out.println("[Job] loading dpn...");
			try {
				System.out.println("[Job] dpn size: "+new File(dpnPath).length()/1000 +"Kb");
				dpn = new DataPetriNetImporter().importFromStream(new FileInputStream(dpnPath));
			} catch (Exception ie) {
				System.out.println("[Job] error: unable to parse pnml into petrinet and on to dpn");
				throw ie;
			}
			// run precision test
			PrecisionResult result = run(logs,alignedLogs,dpn);
			writer.write(job.replace("_", ",")+","+result.getPrecision()+"\n");
			HandlePlaceDump(result,args,job,dpn);
		}
		writer.close();
		} catch (Exception e) {
			System.out.println("[Job] Error occured: "+e.getMessage());
			writer.close();
			throw e.getCause();
		}
	} 
	
	public static boolean requires(HashMap<String,String> args) {
		if (args.containsKey("model")) {
			if (args.containsKey("log")) {
				if (args.containsKey("alog")) {
					if (args.containsKey("output")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void HandlePlaceDump(PrecisionResult results, HashMap<String,String> args, String job, DPNWithLayout dpn) throws Throwable {
		FileWriter dumper = new FileWriter(new File(args.get("output")+"\\places\\"+job+"_place_results.csv"));
		dumper.write("place_name,precision,occurences\n");
		//	for each place in the dpn record the place precision and the occurrences of that place in the log
		for(Place place: dpn.getDPN().getPlaces()) {
			if (place.getGraph().getOutEdges(place).size() > 1) {
				String line = "";
				line += place.getLabel()+",";
				line += results.getPrecision(place)+",";
				long frequencySum = results.getPossibleStateTransitions(place).keySet().stream().map(state -> results.getFrequency(state)).reduce((long)0, (a,b) -> a+b);
				line += frequencySum+"\n";
				dumper.write(line);
			}
		}
		dumper.close();
	}
	
	public static PrecisionResult run(XLog log, XLog alignedLog, DPNWithLayout dpn) throws Throwable {
		//	create new plugin	
		DataAwarePrecisionPlugin precisionRunner = new DataAwarePrecisionPlugin();
//		setup config
//		create an activity map that uses the exact mapping
		SetMultimap<String, Transition> activityMapping = HashMultimap.create();
		for(Transition trans: dpn.getDPN().getTransitions()) {
			activityMapping.put(trans.getLabel(), trans);
		}
//		create a varaible mapping that uses the exact mapping
		Map<String,String> variableMapping = new HashMap<String,String>();
		for(DataElement el : dpn.getDPN().getVariables()) {
			variableMapping.put(el.getVarName(), el.getVarName());
		}	
//		cast alignedLog to XAlignment
		XAlignedLog newAlignedLog = XAlignmentExtension.instance().extendLog(alignedLog);
		
		PrecisionConfig config = new PrecisionConfig(dpn.getDPN().getInitialMarking(), activityMapping,
				log.getClassifiers().get(0), variableMapping);
//		run measure
		PrecisionResult result = precisionRunner.doMeasurePrecisionWithAlignment(dpn.getDPN(), log, newAlignedLog, config);
		System.out.println(
				"[Job] Multi-perspective precision calulated: " +
				result.getPrecision()
		);
		return result;
		
	
	}
}
