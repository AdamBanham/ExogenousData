package org.processmining.qut.exogenousaware.exceptions;

public class CannotConvertException extends Exception {

	public CannotConvertException(Object from, Object to) {
		super("Cannot natively convert between :: "+ from.getClass().getName()+","+to.getClass().getName());
	}
}
