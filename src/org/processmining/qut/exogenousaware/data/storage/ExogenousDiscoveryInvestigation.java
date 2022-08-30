package org.processmining.qut.exogenousaware.data.storage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.datadiscovery.estimators.Type;
import org.processmining.datadiscovery.model.DiscoveredPetriNetWithData;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithData;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.qut.exogenousaware.data.storage.workers.InvestigationTask;
import org.processmining.qut.exogenousaware.gui.ExogenousDiscoveryInvestigator;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

@Builder
@Data
public class ExogenousDiscoveryInvestigation {

	@NonNull @Singular private List<String> endogenousVariables;
	@NonNull @Singular private List<String> exogenousVariables;
	@NonNull private PetriNetWithData model;
	@NonNull private XLog log;
	@NonNull private PNRepResult alignment;
	@NonNull private ExogenousDiscoveryInvestigator source;
	
	
	@Getter @Default private DiscoveredPetriNetWithData outcome = null;
	@Getter @Default private Map<String, GuardExpression> foundExpressions = new HashMap<String, GuardExpression>();
	@Getter @Default private InvestigationTask task = null;
	@Default @Getter private Marking[] markings = new Marking[2];
	@Default @Getter private Boolean success = false;
	@Getter @Default private JPanel main = new JPanel();
	@Getter @Default private JProgressBar progress = new JProgressBar();
	@Getter @Default private Map<String, Type> classTypes = new HashMap<String, Type>();
	@Getter @Default private Map<String, Set<String>> literalValues = new HashMap<String, Set<String>>();
	@Getter @Default private Map<Transition,Transition> transMap = null;
	
	public ExogenousDiscoveryInvestigation setup( ) {
		this.progress.setMaximum(100);
		this.progress.setValue(0);
		this.main.add(progress);
		this.task = InvestigationTask.builder()
				.source(this)
				.progresser(this.source.getProgresser())
				.build()
				.setup();
		return this;
	}
	
	
	public void run() {
		this.task.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())) {
					progress.setValue((Integer)evt.getNewValue());
					progress.validate();
	            }
				else if ("state".equals(evt.getPropertyName())) {
					if (evt.getNewValue().equals(SwingWorker.StateValue.DONE)) {
						main.setVisible(false);
						handleTask();
					}
				} else {
					System.out.println("[ExogenousDiscoveryInvestigation] unsed propertychange event : " + evt.getPropertyName());
				}
			}
	     });
		System.out.println("[Exogenous Investigation Task] Starting Computation");
		task.execute();
	}
	
	public void handleTask() {
		try {
			this.outcome = this.task.get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (Transition transition : this.outcome.getTransitions()) {
			PNWDTransition transitionInPNWithData = (PNWDTransition) transition;
			if (transitionInPNWithData.hasGuardExpression()) {
				String guardExpression = transitionInPNWithData.getGuardAsString();
				for(Entry<String,String> val : this.task.getConveretedNames().entrySet() ){
					guardExpression = guardExpression.replace(val.getValue(), val.getKey());
				}
				this.foundExpressions.put(transition.getId().toString(), transitionInPNWithData.getGuardExpression());
				System.out.println("[ExogenousDiscoveryInvestigation] Found guard for "+ transitionInPNWithData.getLabel() + " : "+ guardExpression);
			}
		}
		this.transMap = this.task.getTransMap();
		
		this.source.createModelView(this.task.getConveretedNames(), this.outcome, this.transMap);
	
	}
	
	public Map<String, Type> makeClassTypes() {
//		#TODO exogenous variables are all considered continous at this stage
		for(String exo: this.exogenousVariables) {
			this.classTypes.put(exo, Type.CONTINUOS);
		}
//		for each endo variable find a suitable attribute type
		for(String endo: this.endogenousVariables) {
			Boolean found = false;
			for(XTrace trace: this.log) {
				XAttributeMap attrs = trace.getAttributes();
				if (attrs.containsKey(endo)) {
					found = true;
					this.classTypes.put(endo,this.findAttributeType(attrs.get(endo)));
					break;
				}
				for(XEvent event: trace) {
					attrs = event.getAttributes();
					if (attrs.containsKey(endo)) {
						found = true;
						this.classTypes.put(endo,this.findAttributeType(attrs.get(endo)));
						break;
					}
				}
				if (found) {
					break;
				}
			}
		}
		System.out.println("[ExoDiscoveryInvestigation] Found class types : " + this.classTypes.toString());
		return this.classTypes;
	}
	
	public Type findAttributeType(XAttribute xAttrib) {
		if (xAttrib instanceof XAttributeBoolean) {
			return Type.BOOLEAN;
		} else if (xAttrib instanceof XAttributeContinuous) {
			return Type.CONTINUOS;
		} else if (xAttrib instanceof XAttributeDiscrete) {
			return Type.DISCRETE;
		} else if (xAttrib instanceof XAttributeTimestamp) {
			return Type.TIMESTAMP;
		} else {
			return Type.LITERAL;
		}
	}
	
	public Map<String, Set<String>> makeLiteralValues () {
//		find all literal attributes
		Set<String> literalAttrs = new HashSet<String>();
		for(String key: this.classTypes.keySet()) {
			if (this.classTypes.get(key).equals(Type.LITERAL)) {
				literalAttrs.add(key);
				this.literalValues.put(key, new HashSet<String>());
			}
		}
//		go through log and find all values
		for(XTrace trace: this.log) {
			XAttributeMap attrs = trace.getAttributes();
			for (String key: literalAttrs) {
				if (attrs.containsKey(key)) {
					this.literalValues.get(key).add(attrs.get(key).toString());
				}
			}
			for (XEvent event: trace) {
				attrs = event.getAttributes();
				for (String key: literalAttrs) {
					if (attrs.containsKey(key)) {
						this.literalValues.get(key).add(attrs.get(key).toString());
					}
				}
			}
		}
		System.out.println("[ExoDiscoveryInvestigation] Found literal value sets : " + this.literalValues.toString());
		return this.literalValues;
	}
		
}
