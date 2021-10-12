package org.processmining.qut.exogenousaware.data.storage;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.impl.XAttributeImpl;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Builder
@Data
@EqualsAndHashCode(callSuper=false)
public class ExogenousAttribute extends XAttributeImpl implements XAttributeContinuous {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7581819879857126131L;

	@Setter private String key;
	@Getter @Setter private double value;
	@Getter @Setter private XExtension extension;
	@Getter private String transform;
	
	public ExogenousAttribute(String key, double value) {
		this(key,value,null,"unknown");
	}
	
	public ExogenousAttribute(String key, double value, XExtension extension, String transform) {
		super(String.format("exogenous:%s:transform:%s", key,transform), extension);
		this.value = value;
		this.key = key;
		this.transform = transform;
	}
	
	public String getKey() {
		return String.format("exogenous:%s:transform:%s", this.key,this.transform);
	}
	
	public String toString() {
		return Double.toString(this.value);
	}

}
