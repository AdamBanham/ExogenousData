package org.processmining.qut.exogenousaware.ab.jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.deckfour.xes.in.XesXmlGZIPParser;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datapetrinets.io.DataPetriNetImporter;
import org.processmining.datapetrinets.io.DataPetriNetImporter.DPNWithLayout;
import org.processmining.log.utils.XUtils;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.balancedconformance.DataConformanceJobber;
import org.processmining.plugins.balancedconformance.config.BalancedProcessorConfiguration;
import org.processmining.plugins.balancedconformance.export.ExtendLogWithAlignments;
import org.processmining.plugins.balancedconformance.result.AlignmentCollection;
import org.processmining.plugins.balancedconformance.result.BalancedReplayResult;
import org.processmining.qut.exogenousaware.ab.helpers.DataConformanceObserver;
import org.processmining.qut.exogenousaware.ab.helpers.MirrorConsoleStream;
import org.processmining.qut.exogenousaware.ab.helpers.progressListener;

public class ICPM2021 {
	
	private static int defaultMoveOnModelCost = 1;
	private static int defaultMoveOnLogCost = 1;
	private static int defaultMissingWriteOpCost = 2;
	private static int defaultIncorrectWriteOpCost = 2;
	private static long configMaxTraceCompute = 180; // in seconds, == 5 minutes max per trace.
	
	public static void main(HashMap<String,String> args) throws Throwable {
		// keep reference of consolestream
		File outputcsv= null;
		FileWriter outputcsvWriter = null;
		MirrorConsoleStream mirrorStream = new MirrorConsoleStream(); 
		try {
			// redirect system out to file
			if (args.containsKey("output")) {
				System.out.println("[Job] Redirecting console to output folder in file: "+args.get("output")+"\\debug.stdout");
				mirrorStream.create(args.get("output")+"\\debug.stdout");
				// create an output csv for results
				outputcsv = new File(args.get("output")+"\\scores.csv");
				outputcsvWriter = new FileWriter(outputcsv);
				outputcsvWriter.write("log,model,type,param,mean,min,max,std,time(seconds),uncomputed_traces\n");
			}
			System.out.println("[Job] starting job :: ICPM2021");
			System.out.println("[Job] inputs: "+args.toString());
			if (args.containsKey("model")) {
				// find all pnml files in input directory and run balanced conformances
				System.out.println("[Job] Running single batch on --model");
				runBalancedConformance(args, args.get("model"), null,null);
			} else if (args.containsKey("input")) {
				//get all the pnml models in input dir and run conformance checking using log on all models
				System.out.println("[Job] Running batch conformance checking using --input dir");
				Set<String> pnmlModels = Files.walk(Paths.get(args.get("input")),2)
						.filter(file -> !Files.isDirectory(file))
						.map(file -> file.toAbsolutePath())
						.map(file -> file.toString())
						.collect(Collectors.toSet()); 
				for(String file: pnmlModels) {
					System.out.println("[Job] Starting work on pnml model: "+ file);
					String name = new File(file).getName().replace(".pnml", "");
					String folder = name.split("_")[0];
					// ensure folder exists
					new File(args.get("output")+"\\"+ folder+"\\").mkdirs();
					// do the actual work for this job
					runBalancedConformance(args, file, outputcsvWriter, new File (args.get("output")+"\\"+ folder +"\\"+ name +".xes.gz") );
					System.out.println("[Job] Completed work on pnml model: "+file);
					// flush out streams after job completed to ensure no work is lost
					if (mirrorStream != null) {
						mirrorStream.flush();
					}
					if (outputcsvWriter != null) {
						outputcsvWriter.flush();
					}
				}
			} else {
				throw new Exception("requires targeted models via either --model (single) --input (bulk)");
			}
			//revert system out
			if (args.containsKey("output")) {
				mirrorStream.close();
				outputcsvWriter.close();
			}
			
		} catch (Exception e) {
			//make sure not to lose any work
			if (args.containsKey("output")) {
				mirrorStream.close();
				outputcsvWriter.close();
			}
			System.out.println(e.getCause());
			throw e;
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void runBalancedConformance(HashMap<String,String> args, String inputModel, FileWriter outputcsv, File outputlog) throws Throwable {
		try {
			System.out.println("[Job] starting job :: ICPM2021 :: "+args.get("model"));
			// load in the .xes.gz log 
			Collection<XLog> logs = new ArrayList<XLog>();
			try {
				System.out.println("[Job] parsing log...");
				logs = new XesXmlGZIPParser().parse(new File(args.get("log")));
			} catch (Exception ie) {
				System.out.println("[Job] Unable to parse log: "+args.get("log"));
				throw ie.getCause();
			}
			System.out.println("[Job] parsed log...");
			System.out.println("[Job] Number of traces: "+ logs.iterator().next().size());
			XTrace example = logs.iterator().next().stream().findAny().get();
			System.out.println("[Job] Example trace: "+example.stream()
				.map(x -> x.getAttributes().get("concept:name").toString())
				.reduce(
						"",
						(a,b) -> a + "|" + b
				)
			);
			// release logs			
			//logs = null;
			// load in DPN
			DPNWithLayout dpn = null;
			System.out.println("[Job] loading dpn...");
			try {
				if (inputModel == null) {
					throw new Exception("[Job] error: missing --model [file] argument or inputModel parameter");
				}
				System.out.println("[Job] dpn size: "+new File(inputModel).length()/1000 +"Kb");
				dpn = new DataPetriNetImporter().importFromStream(new FileInputStream(inputModel));
				
			} catch (Exception ie) {
				System.out.println("[Job] error: unable to parse pnml into petrinet and on to dpn");
				throw ie;
			}
			System.out.println("[Job] loaded dpn...");
			System.out.println("[Job] " +dpn.getDPN().toString());
			System.out.println(
					"[Job] Inital Markings: " +
					dpn.getDPN().getInitialMarking().toString()
			);
			for (Marking mark: dpn.getDPN().getFinalMarkings()) {
				System.out.println(
						"[Job] Final marking: " +
						mark.toString()
				);
			}
			// finally able to get to the balanced conformance analysis
			System.out.println("[Job] starting conformance checking...");
			System.out.println("[Job] log classifiers, using first, "+logs.stream().map(x -> x.getClassifiers().toString()).reduce("", (a,b) -> a+b));
			// create carries for results
			ArrayList<Float> fitness = new ArrayList<Float>();
			int run = 1;
			long start = new Date().getTime();
			while (run < 2) {
					
				try {
					for(XLog log: logs) {
						DataConformanceObserver obs = new DataConformanceObserver();
						BalancedProcessorConfiguration config = BalancedProcessorConfiguration.newDefaultInstance(
								dpn.getDPN(),
								dpn.getDPN().getInitialMarking(),
								dpn.getDPN().getFinalMarkings(),
								log,
								log.getClassifiers().get(0),
								defaultMoveOnModelCost,
								defaultMoveOnLogCost,
								defaultMissingWriteOpCost,
								defaultIncorrectWriteOpCost
						);		
						config.setObserver(obs);
						// default settings update for computation limits						
						config.setTimeLimitPerTrace(configMaxTraceCompute);
						//config.setMaxCostFactor(0.25);
						//config.setMaxQueuedStates(Integer.MAX_VALUE/8);
						// set default limits for variables
						BalancedProcessorConfiguration.autoGuessBounds(config, dpn.getDPN(), log);
						// set amount of processors to be one less 
						config.setConcurrentThreads(config.getConcurrentThreads()-1);
						System.out.println("[Job] lower: " + config.getLowerBounds().toString());
						System.out.println("[Job] upper: " + config.getUpperBounds().toString());
						// compute conformance checking						
						BalancedReplayResult result = new DataConformanceJobber().doBalancedAlignmentDataConformanceChecking(
								dpn.getDPN(),
								log,
								new progressListener(),
								config
						);
						System.out.println("[Progress][Run "+run+"] Finished conformance checking, took "+result.getCalcTime()+" seconds..");
						System.out.println( 
								"[Progress][Run "+run+"] Mean fitness: " + result.meanFitness
						);
						// run complete so keep score
						fitness.add(result.meanFitness);
						if (outputcsv != null) {
							String[] temp = args.get("log").split("\\\\");
							String logname = temp[temp.length-1];
							temp = inputModel.split("\\\\");
							String model = temp[temp.length-1];
							obs.writeResults(
									outputcsv,
									logname,
									model,
									model.split("_")[1],
									model.split("_")[2].replace(".pnml", ""),
									result.getCalcTime()
							);
						}
						if (outputlog != null) {
							progressListener plug = new progressListener();
							XUtils.saveLogGzip(ExtendLogWithAlignments.doExtendLogWithAlignments(plug, (AlignmentCollection) result), outputlog);
						}
					}
				} catch (Exception ie) {
					System.out.println("[Progress][Run "+run+"] System run fail: "+ie.getMessage());
					throw ie;
				}
				run++;
			};
			// how did we go ?
			float avgFitness = fitness.stream().reduce((float)0.0, (a,b) -> a+b) / fitness.size();
			System.out.println("[Job] Completed runs in "+(new Date().getTime() - start)/1000+" seconds...");
			System.out.println("[Job] Over runs the min fitness was :: "+ fitness.stream().reduce(null, (a,b) -> {return a == null ? b : (a < b ? a : b); }));
			System.out.println("[Job] Over runs the mean fitness was :: "+ avgFitness);
			System.out.println("[Job] Over runs the max fitness was :: "+ fitness.stream().reduce(null, (a,b) -> {return a == null ? b : (a > b ? a : b); }));
		} catch (Exception e) {
			throw e;
		}
	}
}
