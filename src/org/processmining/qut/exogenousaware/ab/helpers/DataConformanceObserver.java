package org.processmining.qut.exogenousaware.ab.helpers;

import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.logging.Level;

import org.apache.commons.math3.stat.StatUtils;
import org.processmining.plugins.balancedconformance.observer.DataConformancePlusObserverImpl;

public class DataConformanceObserver extends DataConformancePlusObserverImpl {

	public DataConformanceObserver() {
		super(null);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void log(Level level, String message) {
		if (level.equals(Level.SEVERE)) {
			
		} else if (level.equals(Level.WARNING)) {
			
		} else {
			
		}
		System.out.println("[Job] " +message);
	}
	
	@Override
	public void finishedAlignment() {
		log("****************************");
		log("Overall statistics:");
		double mean = StatUtils.mean(getFitnessArray());
		log(MessageFormat.format("Max Queue: {0,number,#}", StatUtils.max(getQueuedStatesArray())));
		log(MessageFormat.format("Avg Queue: {0,number,#.###}", mean));	
		log(MessageFormat.format("Min Fitness: {0,number,#.###}", StatUtils.min(getFitnessArray())));
		log(MessageFormat.format("Avg Fitness: {0,number,#.###}", StatUtils.mean(getFitnessArray())));
		log(MessageFormat.format("Max Fitness: {0,number,#.###}", StatUtils.max(getFitnessArray())));
		// work out standard deviation		
		double std = 0;
		for(double fit: getFitnessArray()) {
			std += Math.pow(fit - mean,2);
		}
		std = std / (double)getFitnessArray().length;
		std = Math.sqrt(std);
		log(MessageFormat.format("Std Fitness: {0,number,#.###}", std));
		log("****************************");
	}
	
	public void writeResults(FileWriter outputcsv,String log, String model, String type,
			String param, long time) throws Throwable {
		double mean = StatUtils.mean(getFitnessArray());
		double std= 0;
		for(double fit: getFitnessArray()) {
			std += Math.pow(fit - mean,2);
		}
		std = std / (double)getFitnessArray().length;
		std = Math.sqrt(std);
		int uncomputed = 0;
		for (Boolean notComputed: this.getUnreliableDetected()) {
			uncomputed += notComputed ? 1 : 0;
		}
		outputcsv.write(
				String.format(
						"%1$s,%2$s,%3$s,%4$s,%5$.3f,%6$.3f,%7$.3f,%8$.3f,%9$.3f,%10$d\n",
						log,
						model,
						type,
						param,
						mean,
						StatUtils.min(getFitnessArray()),
						StatUtils.max(getFitnessArray()),
						std,
						(float)time,
						uncomputed
				).toString()
		);
	}

}
