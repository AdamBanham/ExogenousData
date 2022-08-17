package org.processmining.qut.exogenousaware.stats.models;

import java.util.List;

public interface ModelStatistics<P,T,S> {
	
	/**
	 * Gets the number of times this action was definitely seen in the right place and time.
	 * @param action
	 * @return
	 */
	public Integer getObservations(T action);

	/**
	 * Gets the list of decision moments in a process model
	 * @return
	 */
	public List<P> getDecisionMoments();
	
	/**
	 * Gets all outcomes for a single decision moment
	 * @param moment
	 * @return
	 */
	public List<T> getDecisionOutcomes(P moment);
	
	
	/**
	 * Checks if this moment is indeed a decision moment
	 * @param moment
	 * @return
	 */
	public Boolean isDecisionMoment(P moment);
	
	/**
	 * Checks the moment is a decision moment, and if so is this outcome part of that decision moment.
	 * @param outcome
	 * @param moment
	 * @return
	 */
	public Boolean isOutcome(T outcome, P moment);
	
	/**
	 * Gets the statistic information about this moment.
	 * @param moment
	 * @return
	 */
	public S getInformation(P moment);
}
