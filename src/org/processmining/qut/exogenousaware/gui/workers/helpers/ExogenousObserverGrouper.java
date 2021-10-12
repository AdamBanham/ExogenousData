package org.processmining.qut.exogenousaware.gui.workers.helpers;

import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

/**
 * This is a interface for a grouper class, which defines clear groups of subseries based on some interesting aspect of the related trace 
 * or sliced subseries.
 * 
 * @author Adam Banham
 *
 */
public interface ExogenousObserverGrouper {

	/**
	 * This function finds a group for a given trace and subseries to be used later in enhancement graphs.
	 * @param trace an endogenous trace
	 * @param sliced a linked subseries
	 * @return a group identifier
	 */
	public int findGroup(XTrace trace, SubSeries sliced);
	
	/**
	 * This function finds a group for a given trace and subseries, but only considers up to the n-th event in a trace.
	 * @param trace a endogenous trace
	 * @param sliced a linked subseries
	 * @param n the number of events to consider from subseries linked endogenous event
	 * @return a group identifier
	 */
	public int findGroup(XTrace trace, SubSeries sliced,int n);
	
	/**
	 * Finds a human readable string representation for a group identifier.
	 * @param group number identifier
	 * @return group a string of group identifier 
	 */
	public String getGroupName(int group);
	
}
