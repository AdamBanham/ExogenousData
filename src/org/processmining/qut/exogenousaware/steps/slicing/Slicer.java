package org.processmining.qut.exogenousaware.steps.slicing;

import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.crypto.KeySelectorException;

import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.data.ExogenousDataset;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;


/**
 * 
 * A common set of functions that all slicing functions must implement.
 * 
 * @author Adam Banham
 *
 */
public interface Slicer {
	
	/**
	 * For a event, finds the event time in milliseconds.
	 * 
	 * @param ev a event in XES formatted list
	 * 
	 * @throws KeySelectorException when time:timestamp attribute cannot be found
	 * 
	 * @return long the event's timestamp in milliseconds.
	 */
	public default long getEventTimeMillis(XEvent ev) throws NoSuchElementException {
		if (ev.getAttributes().containsKey("time:timestamp")) {
			return ( (XAttributeTimestamp) ev.getAttributes().get("time:timestamp")).getValueMillis();
		} else {
			throw new NoSuchElementException("Unable to find suitable attribute for event to describe time.");
		}
	};
	
	/**
	 * Compares between two events and returns the event that occurred last.
	 * @param curr a event
	 * @param next a event
	 * @return XEvent
	 */
	public default XEvent getLastEvent(XEvent curr, XEvent next) {
//		handle events carefully
		double currTime = -1; 
		try {
		currTime = getEventTimeMillis(curr);
		} catch (NoSuchElementException e) {
			return next;
		}
		double nextTime = -1;
		try {
			nextTime = getEventTimeMillis(next);
		} catch (NoSuchElementException e) {
			return curr;
		}
//		compare between curr and next
		if (nextTime == -1 && currTime != -1) {
			return curr;
		}
		else if (currTime == -1 && nextTime != -1) {
			return next;
		}
		else if (currTime != -1 && nextTime != -1) {
			if ( currTime > nextTime) {
				 return curr;
			 }
			 return next;
		}
		else {
			return curr;
		}
	}
	
	
	
	/**
	 * Compares between two events and returns the event that occurred first.
	 * @param curr a event
	 * @param next a event
	 * @return XEvent
	 */
	public default XEvent getFirstEvent(XEvent curr, XEvent next) {
//		handle events carefully
		double currTime = -1; 
		try {
		currTime = getEventTimeMillis(curr);
		} catch (NoSuchElementException e) {
			return next;
		}
		double nextTime = -1;
		try {
			nextTime = getEventTimeMillis(next);
		} catch (NoSuchElementException e) {
			return curr;
		}
//		compare between curr and next
		if (nextTime == -1 && currTime != -1) {
			return curr;
		}
		else if (currTime == -1 && nextTime != -1) {
			return next;
		}
		else if (currTime != -1 && nextTime != -1) {
			if ( currTime > nextTime) {
				 return next;
			 }
			 return curr;
		}
		else {
			return curr;
		}
	}
	
	/**
	 * Performs the slicing between an endogenous trace and an exogenous timeseries.
	 *  
	 * @param endogenous an endogenous trace, with control flow events
	 * @param exogenous an exogenous time series, with measurement events
	 * @return a mapping of endogenous events to a sub-timeseries from the exogenous time series
	 */
	public Map<XEvent,SubSeries> slice (XTrace endogenous, XTrace exogenous, ExogenousDataset dataset);
	
	/**
	 * Creates a gui widget to allow for users to specify any needed parameters
	 */
	public void createUserChoices ();
	
	/**
	 * Gets the name of this slicing function, used in annotation.
	 * @return the name of the slicer
	 */
	public String getName();
	
	/**
	 * Gets the abbreviated of this slicing function, used in annotation.
	 * @return the abbreviation of the slicer
	 */
	public String getShortenName();
}
