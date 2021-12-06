package org.processmining.qut.exogenousaware.exceptions;

public class LinkNotFoundException extends Exception {

	public LinkNotFoundException() {
		super("Link not found between trace and exogenous dataset.");
	}
}
