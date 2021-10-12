package org.processmining.qut.exogenousaware.steps.transform.data;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.impl.XAttributeImpl;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * 
 * A transformed attribute which is aware of its SubSeries for future references.<br>
 * <br>
 * This class uses the builder design pattern. Call TransformedAttribute.builder() to create new instances.
 * 
 * @author Adam Banham
 *
 */
@Builder
public class TransformedAttribute extends XAttributeImpl implements XAttributeContinuous {
	
	private static final long serialVersionUID = -2603180058844638217L;
	
	private String key;
	@Getter @Setter private double value;
	@Getter private XExtension extension;
	@Getter private String transform;
	@Getter @NonNull SubSeries source;
	
	public TransformedAttribute(String key, double value) {
		this(key,value,null,"unknown",null);
	}
	
	public TransformedAttribute(String key, double value, XExtension extension, String transform, SubSeries source) {
		super(String.format("exogenous:%s:transform:%s",key,transform),extension);
		this.key = key;
		this.value = value;
		this.extension = extension;
		this.transform = transform;
		this.source = source;
	}
	
	public String getKey() {
		return String.format("exogenous:%s:transform:%s", this.key,this.transform);
	}
	
	public String toString() {
		return Double.toString(this.value);
	}
	
	public double getRealValue() {
		return this.value;
	}
	
}
