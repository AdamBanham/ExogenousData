package org.processmining.qut.exogenousaware.exceptions;

public class ExogenousAttributeNotFoundException extends Exception {

	public ExogenousAttributeNotFoundException(Object looking) {
		super("Unable to find a matching exogenous attribute on log for :: "+looking.getClass().getName());
	}
	
}
