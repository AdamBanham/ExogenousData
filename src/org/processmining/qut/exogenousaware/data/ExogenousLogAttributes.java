package org.processmining.qut.exogenousaware.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;

/**
 * A Static class which describes the attributes needed for a event to be converted to a
 * an exogenous dataset (wrapper around an event log for xPM).
 * <br>
 * <br>
 * Exogenous Attribute Structure:<br>
 * <ul>
 * 	<li> exogenous:dataset [TRUE/FALSE]
 *  <ul>
 *  	<li> exogenous:type [discrete/numerical]
 *  	<li> exogenous:link:method [match/ematch]
 *  	<li> exogenous:link:matching [NULL]
 *  	<ul>
 *  		<li> attribute_x [some named attribute to find i.e. 'concept:name']
 *  	</ul>
 *  </ul>
 * </ul>
 * <br>
 * @author Adam P Banham
 *
 */
public class ExogenousLogAttributes {

	private ExogenousLogAttributes() {};
	
	public static ExogenousLogAttribute EXOGENOUS_DATASET = ExogenousLogAttribute.EXOGENOUS_DATASET;
	public static ExogenousLogAttribute EXOGENOUS_DATA_TYPE_DISCRETE = ExogenousLogAttribute.EXOGENOUS_DATA_TYPE_DISCRETE;
	public static ExogenousLogAttribute EXOGENOUS_DATA_TYPE_NUMERICAL = ExogenousLogAttribute.EXOGENOUS_DATA_TYPE_NUMERICAL;
	public static ExogenousLogAttribute EXOGENOUS_LINK_TYPE_TRACEATTRS = ExogenousLogAttribute.EXOGENOUS_LINK_TYPE_TRACEATTRS;
	public static ExogenousLogAttribute EXOGENOUS_LINK_TYPE_EVENTATTRS = ExogenousLogAttribute.EXOGENOUS_LINK_TYPE_EVENTATTRS;
	
	
	public static List<String> extractLinkAttributes(XLog elog){
		List<String> attributes = new ArrayList<String>();
//		check that matching exists 
		if (ExogenousLogAttribute.EXOGENOUS_LINK_ATTRIBUTES.check(elog)) {
			// extract values
			XAttribute attr = elog.getAttributes().get("exogenous:dataset").getAttributes().get("exogenous:link:matching");
			for(Entry<String, XAttribute> entry: attr.getAttributes().entrySet()) {
				attributes.add(entry.getValue().toString());
			}
		}
		return attributes;
	}
	
	public static enum ExogenousLogAttribute{
		EXOGENOUS_DATASET("exogenous:dataset","TRUE"),
		EXOGENOUS_DATA_TYPE_DISCRETE(
				new ArrayList<String>(){{add("exogenous:dataset");add("exogenous:type");}},
				new ArrayList<String>(){{add("TRUE");add("discrete");}}
		),
		EXOGENOUS_DATA_TYPE_NUMERICAL(				
				new ArrayList<String>(){{add("exogenous:dataset");add("exogenous:type");}},
				new ArrayList<String>(){{add("TRUE");add("numerical");}}
		),
		EXOGENOUS_LINK_TYPE_TRACEATTRS(
				new ArrayList<String>(){{add("exogenous:dataset");add("exogenous:link:method");}},
				new ArrayList<String>(){{add("TRUE");add("match");}}
		),
		EXOGENOUS_LINK_TYPE_EVENTATTRS(
				new ArrayList<String>(){{add("exogenous:dataset");add("exogenous:link:method");}},
				new ArrayList<String>(){{add("TRUE");add("ematch");}}
		),
		EXOGENOUS_LINK_ATTRIBUTES(
				new ArrayList<String>(){{add("exogenous:dataset");add("exogenous:link:matching");}},
				new ArrayList<String>(){{add("TRUE");add("");}}
		);
		
		private Boolean chain;
		private String key;
		private String value;
		private List<String> keys;
		private List<String> values;
		
		private ExogenousLogAttribute(String key, String value) {
			this.chain = false;
			this.key = key;
			this.value = value;
		}
		
		private ExogenousLogAttribute(List<String> keys, List<String> values) {
			this.chain = true;
			this.keys = keys;
			this.values = values;
		}
		
		/**
		 * Checks if a log has this log attribute and value.
		 * @param log to be checked
		 * @return if the log has attribute available
		 */
		public Boolean check(XLog log) {
			if (!this.chain) {
				if (log.getAttributes().containsKey(this.key)) {
					XAttribute attr = log.getAttributes().get(this.key);
					return attr.toString().equals(this.value);
				}
			} else {
				XAttribute attr = null;
				Boolean check = true;
				// check for top level to begin chain
				if (log.getAttributes().containsKey(this.keys.get(0))) {
					attr = log.getAttributes().get(this.keys.get(0));
					check = check & attr.toString().equals(this.values.get(0));
				} else {
					check = false;
				}
				// check sub levels
				if (check) {
					for(int i=1;i < this.values.size();i++) {
						String key = this.keys.get(i);
						String value = this.values.get(i);
						if (attr.getAttributes().containsKey(key)) {
							attr = attr.getAttributes().get(key);
							check = check & attr.toString().equals(value);
						} else {
							check = false;
							break;
						}
						
					}
				}
				return check;
			}
			return false;
		}
	}
}
