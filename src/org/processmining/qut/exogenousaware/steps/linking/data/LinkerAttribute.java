package org.processmining.qut.exogenousaware.steps.linking.data;

import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * 
 * An attribute to describe that this trace has a linked exogenous trace in a dataset.<br>
 * <br>
 * This class uses the builder design pattern. Call LinkerAttribute.builder() to create new instances.
 * 
 * @author Adam Banham
 *
 */


@Builder
public class LinkerAttribute extends XAttributeImpl implements XAttributeLiteral {

	@NonNull @Getter XTrace endogenousTrace;
	@NonNull @Getter XTrace exogenousTrace;
	@NonNull @Getter String dataset;
	
	
	
	protected LinkerAttribute(String key) {
		super(key);
	}
	
	public LinkerAttribute(XTrace endo, XTrace exo, String dataset) {
		super(String.format("exogenous:%s:link", dataset));
		this.endogenousTrace = endo;
		this.exogenousTrace = exo;
		this.dataset = dataset;
//		add identifiers to each trace, if needed
		if (!this.endogenousTrace.getAttributes().containsKey("exogenous:id")) {
			this.endogenousTrace.getAttributes().put("exogenous:id", new XAttributeLiteralImpl("exogenous:id", this.endogenousTrace.toString()));
		}
		if (!this.exogenousTrace.getAttributes().containsKey("exogenous:id")) {
			this.exogenousTrace.getAttributes().put("exogenous:id", new XAttributeLiteralImpl("exogenous:id", this.exogenousTrace.toString()));
		}	
	}

	public String getValue() {
		return this.exogenousTrace.getAttributes().get("exongeous:id").toString();
	}

	public void setValue(String arg0) {
	}

}
