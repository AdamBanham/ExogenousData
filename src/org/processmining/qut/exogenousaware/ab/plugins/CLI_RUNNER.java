package org.processmining.qut.exogenousaware.ab.plugins;

import org.processmining.framework.plugin.annotations.Bootable;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.boot.Boot;
import org.processmining.framework.boot.Boot.Level;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.CommandLineArgumentList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashMap;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;

public class CLI_RUNNER {
	/*
	 * 
	 * Boilerplate code to force a cli run of PRoM for some job script.
	 * CLI RUNNER
	 * 
	 * 
	 * 
	 */
	
	
	
	@Plugin(name = "CLI Parser", parameterLabels = {"Event Log"}, returnLabels = {}, returnTypes = {}, userAccessible = true)
	@UITopiaVariant(affiliation = "QUT", author = "A. Banham", email = "")
	public void main(UIPluginContext context, XLog log) {
		
		
	}
	
	@Plugin(name = "CLI Parser", parameterLabels = {}, returnLabels = {}, returnTypes = {}, userAccessible = false)
	@Bootable
	public Object main(CommandLineArgumentList commandlineArguments) throws Throwable {

		if (Boot.VERBOSE != Level.NONE) {
			System.out.println("Starting CLI execution engine...");
			System.out.println("Parameters: "+commandlineArguments);
		}

		if (commandlineArguments.size() < 2) {
			System.out.println("You must specify at least the main class as argument");
		}
		
		// handling args and making map to handle inputs		
		HashMap<String,String> parsedArgs = parse(commandlineArguments);
		if (!parsedArgs.containsKey("job")) {
			System.out.println("You must specify at --job [class] to run a bulk job");
			System.out.println("Given Parameters: "+parsedArgs.toString());
			return null;
		}

		// load in class and pass along args to begin running experiments		
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class<?> cliClass = sysloader.loadClass("org.processmining.qut.exogenousaware.ab.jobs."+parsedArgs.get("job"));
		Method mainMethod = cliClass.getMethod("main", parsedArgs.getClass() );
		mainMethod.invoke(null, new Object[] { parsedArgs });

		return null; // not needed but otherwise reflection does not work
	}
	
	private HashMap<String,String> parse(CommandLineArgumentList argList){
		HashMap<String,String> args = new HashMap<String,String>();
		// works through commands and creates pairs for --[X] and value
		// expected output [--job, "some"] to { "job" : "some" }
		int idx = 0;
		for(String input: argList) {
			if ( input.startsWith("--") ) {
				args.put(input.substring(2), argList.get(idx+1));
			}
			idx++;
		}
		return args;
	}
	
	public static void main(String[] args) throws Throwable {
		try {
			Boot.boot(CLI_RUNNER.class, CLIPluginContext.class, args);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
