package org.processmining.tests.qut.exogenousaware.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.junit.Test;
import org.processmining.qut.exogenousaware.steps.linking.AttributeLinker;

public class AttributeLinkerTest {

	XTrace endogenousTrace = new XTraceImpl(
			new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
				put("foo:bar", new XAttributeLiteralImpl("foo:bar", "some"));
			}})
	);
	
	List<XTrace> noValueMatch = new ArrayList<XTrace>() {{
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "nope"));
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "naa"));
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "noooooo"));
					}})
			)
		);
	}};
	
	List<XTrace> allMatch = new ArrayList<XTrace>() {{
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "some"));
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "some"));
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:bar", new XAttributeLiteralImpl("foo:bar", "some"));
					}})
			)
		);
	}};
	
	List<XTrace> noMatch = new ArrayList<XTrace>() {{
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						
					}})
			)
		);
		add(
			new XTraceImpl(
					new XAttributeMapImpl(new HashMap<String, XAttribute>(){{
						put("foo:buzz", new XAttributeLiteralImpl("foo:buzz", "some"));
					}})
			)
		);
	}};
	
	
	@Test
	public void emptyTest() {
		
		AttributeLinker linking = AttributeLinker.builder()
				.attributeName("foo:bar")
				.build();
		
		List<XTrace> exoSubset = linking.link(this.endogenousTrace, noMatch);
		
		assertEquals(exoSubset.size(), 0);
	}
	
	@Test
	public void matchTest() {
		
		AttributeLinker linking = AttributeLinker.builder()
				.attributeName("foo:bar")
				.build();
		
		List<XTrace> exoSubset = linking.link(this.endogenousTrace, allMatch);
		
		assertEquals(exoSubset.size(), 3);
	}
	
	@Test
	public void attributeMatchTest() {
		
		AttributeLinker linking = AttributeLinker.builder()
				.attributeName("foo:bar")
				.build();
		
		List<XTrace> exoSubset = linking.link(this.endogenousTrace, noValueMatch);
		
		assertEquals(exoSubset.size(), 0);
	}

}
