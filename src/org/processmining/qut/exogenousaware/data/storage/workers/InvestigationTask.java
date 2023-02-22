package org.processmining.qut.exogenousaware.data.storage.workers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeBoolean;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeLiteral;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.processmining.datadiscovery.estimators.AbstractDecisionTreeFunctionEstimator;
import org.processmining.datadiscovery.estimators.DecisionTreeBasedFunctionEstimator;
import org.processmining.datadiscovery.estimators.FunctionEstimation;
import org.processmining.datadiscovery.estimators.FunctionEstimator;
import org.processmining.datadiscovery.estimators.Type;
import org.processmining.datadiscovery.estimators.impl.DecisionTreeFunctionEstimator;
import org.processmining.datadiscovery.estimators.impl.DiscriminatingFunctionEstimator;
import org.processmining.datadiscovery.estimators.impl.OverlappingEstimatorLocalDecisionTree;
import org.processmining.datadiscovery.estimators.weka.WekaUtil;
import org.processmining.datadiscovery.model.DecisionPointResult;
import org.processmining.datadiscovery.model.DiscoveredPetriNetWithData;
import org.processmining.datadiscovery.plugins.DecisionMining;
import org.processmining.datadiscovery.plugins.alignment.TraceProcessor;
import org.processmining.datapetrinets.expression.GuardExpression;
import org.processmining.framework.plugin.Progress;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PNWDTransition;
import org.processmining.models.graphbased.directed.petrinetwithdata.newImpl.PetriNetWithDataFactory;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.qut.exogenousaware.data.ExogenousDatasetType;
import org.processmining.qut.exogenousaware.data.graphs.followrelations.FollowGraph;
import org.processmining.qut.exogenousaware.data.storage.ExogenousDiscoveryInvestigation;
import org.processmining.qut.exogenousaware.ds.timeseries.approx.TimeSeriesSaxApproximator;
import org.processmining.qut.exogenousaware.ds.timeseries.data.DiscreteTimeSeries;
import org.processmining.qut.exogenousaware.ds.timeseries.data.RealTimeSeries;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressState;
import org.processmining.qut.exogenousaware.gui.panels.ExogenousDiscoveryProgresser.ProgressType;
import org.processmining.qut.exogenousaware.steps.determination.Determination;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries;
import org.processmining.qut.exogenousaware.steps.slicing.data.SubSeries.Scaling;
import org.processmining.qut.exogenousaware.steps.transform.data.TransformedAttribute;

import com.google.common.util.concurrent.AtomicLongMap;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;

@Builder
public class InvestigationTask extends SwingWorker<DiscoveredPetriNetWithData, Integer> {

//	Builder parameters
	@NonNull private ExogenousDiscoveryInvestigation source;
	@NonNull private ExogenousDiscoveryProgresser progresser;
	@NonNull private MinerType minerType;
	
//	optional parameters
	@Default private MinerConfiguration config = MinerConfiguration.builder().build();
	
//	Internal states
	@Default @Getter private int maxConcurrentThreads = Runtime.getRuntime().availableProcessors() > 3 
			? Runtime.getRuntime().availableProcessors() - 2 : 1;
	@Default @Getter private ThreadPoolExecutor pool = null;
	@Default private Map<Place, FunctionEstimator> estimators = new HashMap<>();
	@Default private AtomicLongMap<Transition> numberOfExecutions = AtomicLongMap.create();
	@Default private Map<Transition, AtomicLongMap<String>> numberOfWritesPerTransition = new HashMap<>();
	@Default @Getter private Map<String,String> converetedNames = new HashMap<String,String>();
	@Default @Getter private Map<Transition,Transition> transMap = null;
	
	
	public static enum MinerType {
		OVERLAPPING,
		DISCRIMINATING,
		DECISIONTREE;
	}
	
	public static enum MinerInstanceMode {
		REL,
		ABS,
		THRESHOLD;
		
		public boolean isREL() {
			return this == REL;
		}
		
		public boolean isABS() {
			return this == ABS;
		}
		
		public boolean isTHRESHOLD() {
			return this == THRESHOLD;
		}
	}
	
	@Builder
	public static class MinerConfiguration {
		@Default @Getter private double relativeInstanceLevel = 0.15;
		@Default @Getter private double absoluteInstanceLevel = 25;
		@Default @Getter private double instanceThreshold = 200;
		@Default @Getter private MinerInstanceMode instanceHandling = MinerInstanceMode.REL;
		@Default @Getter private double fitnessThreshold = 0.33;
		@Default @Getter private double mergeRatio = 0.15;
		@Default @Getter private boolean unpruned = false;
		@Default @Getter private boolean binarySplit = false;
		@Default @Getter private boolean crossValidate = true;
		@Default @Getter private int crossValidateFolds = 10;
		@Default @Getter private float confidenceLevel = 0.25f;
		@Default @Getter private boolean experimentalFeatures = false;
		@Default @Getter private boolean experimentalSAXFeatures = false;
		@Default @Getter private boolean experimentalDFTFeatures = false;
		@Default @Getter private boolean experimentalEDTTSFeatures = false;
	}
	
//	experimental targets
	private static List<String> SAX_FROM = new ArrayList() {{
		add("f");
		add("e");
		add("a");
		add("j");
	}};
	private static List<String> SAX_TO = new ArrayList() {{ 
		add("j");
		add("a");
		add("j");
		add("a");
	}};
	private static int K_TOP_DFT_COEFICIENTS = 3;
	
	public static class KMostElement {
		
		public boolean lessThan(KMostElement element) {
			return false;
		}
	}
	
	public static class DFTKMostElement extends KMostElement {
		
		private int frequency;
		private double power;
		
		public DFTKMostElement(int frequency, double power) {
			this.frequency = frequency;
			this.power = power;
		}
		
		public int getFrequency() {
			return this.frequency;
		}
		
		public double getPower() {
			return this.power;
		}
		
		@Override
		public boolean lessThan(KMostElement element) {
			if (element instanceof DFTKMostElement) {
				return this.power < ((DFTKMostElement) element).getPower();
			}
			return false;
		}
		
		@Override
		public String toString() {
			return String.format("(%.1f@%d)", this.power, this.frequency);
		}
	}
	
//	custom k-most container
	public static class KMostContainer<T extends KMostElement> {
		
		private int k;
		private List<T> sequence;
		
		public KMostContainer(int k) {
			this.k = k;
			this.sequence = new ArrayList(k);
		}
		
		public boolean add(T element) {
			boolean added = false;
			for(int i=0; i < Math.min(k, this.sequence.size()); i++) {
				T value = sequence.get(i);
				if (value.lessThan(element)) {
					sequence.remove(i);
					sequence.add(i, element);
					added = true;
					return added;
				}
			}
			if (this.sequence.size() < k) {
				sequence.add(element);
				added=true;
			}
			return added;
		}
		
		public List<T> getOrdering(){
			List<T> ret = new ArrayList(k);
			ret.addAll(this.sequence);
			return ret;
		}
		
		@Override
		public String toString() {
			return this.sequence.toString();
		}
		
	}
	
	
//	custom trace processor for classification examples
	public static class ExperimentalFeatureProcessor extends TraceProcessor {
		
		private boolean createDFT = false;
		private boolean createSAX = false;
		private boolean createEDTTS = false;
		
//		feature names 
		public static String TOP_K_DFT_FREQ_FEATURE_NAME = "%s_DFT_%d_FREQ";
		public static String TOP_K_DFT_POWER_FEATURE_NAME = "%s_DFT_%d_POWER";
		public static String SAX_MEAN_TO_OUTLIER_FEATURE_NAME = "%s_SAX_M_to_%s";
		public static String SAX_OUTLIER_TO_OUTLIER_FEATURE_NAME = "%s_SAX_%s_to_%s";
		
//		sax targets
		protected static String SAX_MEAN_POS = TimeSeriesSaxApproximator.SAX_LETTERS.get(5);
		protected static String SAX_MEAN_NEG = TimeSeriesSaxApproximator.SAX_LETTERS.get(4);
		protected static String SAX_OUTLIER_POS = TimeSeriesSaxApproximator.SAX_LETTERS.get(9);
		protected static String SAX_OUTLIER_NEG = TimeSeriesSaxApproximator.SAX_LETTERS.get(0);
		

		public ExperimentalFeatureProcessor(
				PetrinetGraph net,
				XTrace xTrace,
				Map<Place, FunctionEstimator> estimators,
				SyncReplayResult alignment,
				AtomicLongMap<Transition> numberOfExecutions,
				Map<Transition,
				AtomicLongMap<String>> numberOfWritesPerTransition,
				Progress progress) {
			super(net, xTrace, estimators, alignment, numberOfExecutions, numberOfWritesPerTransition, progress);
		}
		
		public ExperimentalFeatureProcessor(
				PetrinetGraph net,
				XTrace xTrace,
				Map<Place, FunctionEstimator> estimators,
				SyncReplayResult alignment,
				AtomicLongMap<Transition> numberOfExecutions,
				Map<Transition,
				AtomicLongMap<String>> numberOfWritesPerTransition,
				Progress progress,
				boolean dft,
				boolean sax,
				boolean edtts) {
			super(net, xTrace, estimators, alignment, numberOfExecutions, numberOfWritesPerTransition, progress);
			this.createDFT = dft;
			this.createSAX = sax;
			this.createEDTTS = edtts;
		}
		
		@Override
		protected void updateAttributes(XAttributeMap xAttributeMap) {
			
			Set<SubSeries> slices = new HashSet<>();
			
			for (XAttribute xAttrib : xAttributeMap.values())
			{
				String attributeKey = xAttrib.getKey();
				String varName=WekaUtil.fixVarName(attributeKey);
//				collect slices from exogenous attributes
				if (xAttrib instanceof TransformedAttribute) {
					SubSeries slice = ((TransformedAttribute)xAttrib).getSource();
					if (slice.getDatatype() == ExogenousDatasetType.NUMERICAL) {
						slices.add( slice );
					}
				}
//				handle endogenous/exogenous attributes
				if (xAttrib instanceof XAttributeBoolean) {
					variableValues.put(varName, ((XAttributeBoolean)xAttrib).getValue());
				} else if (xAttrib instanceof XAttributeContinuous) {
					variableValues.put(varName, ((XAttributeContinuous)xAttrib).getValue());
				} else if (xAttrib instanceof XAttributeDiscrete) {
					variableValues.put(varName, ((XAttributeDiscrete)xAttrib).getValue());
				} else if (xAttrib instanceof XAttributeTimestamp) {
					variableValues.put(varName, ((XAttributeTimestamp)xAttrib).getValue());
				} else if (xAttrib instanceof XAttributeLiteral) {
					variableValues.put(varName,((XAttributeLiteral)xAttrib).getValue());
				}
			}
//			create experimental features on the fly 
//			System.out.println("[InvestigationTask] slices found :: "+slices.size());
			for(SubSeries slice : slices) {
//				generate DFT features for each slice
				if (createDFT) {
					generateDFTFeatures(slice, xAttributeMap);
				}
//				generate SAX features for each slice
				if (createSAX) {
					generateSAXFeatures(slice, xAttributeMap);
				}
//				generate EDTTS features
				if (createEDTTS) {
					generateEDTTSFeatures(slice, xAttributeMap);
				}
			}
//			System.out.println(variableValues.toString());
		}
		
		protected void addAttributes(XAttributeMap map, List<XAttribute> attrs) {
			for(XAttribute attr: attrs) {
				map.put(attr.getKey(), attr);
			}
		}

		protected void generateSAXFeatures(SubSeries slice, XAttributeMap map) {
			// TODO Auto-generated method stub
			try {
				DiscreteTimeSeries saxSeries = slice
						.getTimeSeries(Scaling.hour)
						.createSAXRepresentation();
				FollowGraph saxGraph = new FollowGraph(saxSeries);
				addAttributes(map, 
						createSAXFeature(SAX_MEAN_POS, SAX_OUTLIER_POS, saxGraph, slice)
				);
				addAttributes(map, 
						createSAXFeature(SAX_MEAN_NEG, SAX_OUTLIER_NEG, saxGraph, slice)
				);
				addAttributes(map, 
						createSAXFeature(SAX_OUTLIER_NEG, SAX_OUTLIER_POS, saxGraph, slice)
				);
				addAttributes(map, 
						createSAXFeature(SAX_OUTLIER_POS, SAX_OUTLIER_NEG, saxGraph, slice)
				);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private List<XAttribute> createSAXFeature(String startNode, String endNode, 
				FollowGraph saxGraph, SubSeries slice) {
			List<XAttribute> attrs = new ArrayList();
			boolean found = saxGraph.checkForEventualFollowsBetween(startNode, endNode);
//			System.out.println("saxFeature ::"+ saxGraph +" :: "
//					+ startNode + " -> " + endNode + " :: " + found);
			String featureName = String.format(SAX_OUTLIER_TO_OUTLIER_FEATURE_NAME,
					slice.getComesFrom().getName()+"_"+slice.getAbvSlicingName(),
					startNode,
					endNode
			);	
			featureName = featureName.replace(":", "_");
			XAttribute attr = new XAttributeBooleanImpl(featureName, found);
			featureName = WekaUtil.fixVarName(featureName);
			variableValues.put(featureName, found);
			attrs.add(attr);
			return attrs;
		}

		protected void generateDFTFeatures(SubSeries slice, XAttributeMap map) {
			// TODO Auto-generated method stub
			try {
				RealTimeSeries sample = slice.getTimeSeries(Scaling.hour)
						.resampleWithEvenSpacing(64);
				if (sample.getSize() < 64) {
					return;
				}
	//			perform FFT on series
//				System.out.println("{InvestigationTask} resampled.");
				FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
				double[] transformArray = new double[64];
				List<Double> transformValues = sample.getValues();
	//			fill transform array with values
				for(int i=0; i< 64;i++) {
					if (i < transformValues.size()) {
						transformArray[i] = transformValues.get(i);
					} else {
						transformArray[i] = 0;
					}
				}
//				System.out.println("{InvestigationTask} prepped DFT.");
				Complex[] coeficcients = transformer.transform(transformArray, TransformType.FORWARD);
				String coefString = "Sample :: "+sample.toString()+" || DFT coeficcients :: ";
				boolean nanFound = false;
				KMostContainer<DFTKMostElement> kcoefs = new KMostContainer(K_TOP_DFT_COEFICIENTS);
				int freq = 1;
				for(int i=1;i < 64; i+=2) {
//					TODO double check that the power of the frequency is |x[k]|^2
					double cpow = (2.0/64) * Math.pow(coeficcients[i].abs(), 2);
					nanFound = nanFound || Double.isNaN(cpow);
					coefString = coefString+"C"+i+"="+cpow+" | ";
					if (!Double.isNaN(cpow)) {
						DFTKMostElement el = new DFTKMostElement(freq, cpow);
						kcoefs.add(el);
					}
					freq += 1;
				}
				if (nanFound) {
					System.out.println("{InvestigationTask} completed DFT but "
							+ "coeficcients are NaN :: "+coefString);
				} else {
//					System.out.println("K-most features :: "+ kcoefs.toString());
				}
//			create features for top-k powers and top-k frequencies
			List<DFTKMostElement> ordering = kcoefs.getOrdering();
			for(int i=0; i < ordering.size(); i++) {
				addAttributes(map, 
						createDFTFeature(ordering.get(i), i+1, slice)
				);
			}
			
			} catch (Exception e) {
				System.out.println("DFT features failed :: "+ e.getMessage());
			}
			
		}
		
		private List<XAttribute> createDFTFeature(DFTKMostElement element, int k, SubSeries slice) {
			List<XAttribute> attrs = new ArrayList();
//			create feature to k-freq
			String featureName = String.format(TOP_K_DFT_FREQ_FEATURE_NAME,
					slice.getComesFrom().getName()+":"+slice.getAbvSlicingName(),
					k
			);
			featureName = featureName.replace(":", "_");
			variableValues.put(WekaUtil.fixVarName(featureName), element.getFrequency());
			attrs.add( new XAttributeDiscreteImpl(featureName, element.getFrequency()));
//			create feature for k-power
			featureName = String.format(TOP_K_DFT_POWER_FEATURE_NAME,
					slice.getComesFrom().getName()+":"+slice.getAbvSlicingName(),
					k
			);
			featureName = featureName.replace(":", "_");
			variableValues.put(WekaUtil.fixVarName(featureName), element.getPower());
			attrs.add( new XAttributeContinuousImpl(featureName, element.getPower()));
			return attrs;
		}
		
		private void generateEDTTSFeatures(SubSeries slice, XAttributeMap map) {
			// TODO Auto-generated method stub
			
		}
	}
	
	
	public InvestigationTask setup() {
//		setup pool for later use
		this.pool = new ThreadPoolExecutor(maxConcurrentThreads, maxConcurrentThreads, 60,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		pool.allowCoreThreadTimeOut(false);

		for (Transition trans : this.source.getModel().getTransitions()) {
			/*
			 * For each transition, set the numberOfExecutions to 0 and
			 * initialize the HashMap<String, Integer>
			 */
			this.numberOfExecutions.put(trans, 0);
			this.numberOfWritesPerTransition.put(trans, AtomicLongMap.<String>create());
		}
//		Need to create class types and literalvalues for selected variables
		Map<String, Type> classTypes = new HashMap<String,Type>();
		Map<String, Set<String>> literalValues = new HashMap<String,Set<String>>();
//		handle experimental features inclusion for J48
		if (config.isExperimentalFeatures()) {
			for(  Determination deter : this.source.getSource().getSource().getDeterminations()) {
				String featureName = deter.getPanel().getName()
						+"_"+deter.getSlicer().getShortenName();
				featureName = featureName.replace(":", "_");
//				add sax feature names
				if (config.isExperimentalSAXFeatures()) {
					for(int i=0;i < this.SAX_FROM.size(); i++) {
						String expAttr = String.format(ExperimentalFeatureProcessor.SAX_OUTLIER_TO_OUTLIER_FEATURE_NAME,
								featureName,
								this.SAX_FROM.get(i),
								this.SAX_TO.get(i)
						);
						String likelyHandledname = GuardExpression.Factory.transformToVariableIdentifier(
								DecisionMining.wekaUnescape(
										WekaUtil.fixVarName(expAttr)));
						this.converetedNames.put(expAttr, likelyHandledname);
						classTypes.put(DecisionMining.fixVarName(expAttr), Type.BOOLEAN);
					}
				}
//				add dft feature names
				if (config.isExperimentalDFTFeatures()) {
					for(int k=0; k < K_TOP_DFT_COEFICIENTS; k++) {
	//					add discrete feature for k-freq
						String expAttr = String.format(ExperimentalFeatureProcessor.TOP_K_DFT_FREQ_FEATURE_NAME,
								featureName,
								k+1
						);
						String likelyHandledname = GuardExpression.Factory.transformToVariableIdentifier(
								DecisionMining.wekaUnescape(
										WekaUtil.fixVarName(expAttr)));
						this.converetedNames.put(expAttr, likelyHandledname);
						classTypes.put(DecisionMining.fixVarName(expAttr), Type.DISCRETE);
	//					add continuous feature for k-power
						expAttr = String.format(ExperimentalFeatureProcessor.TOP_K_DFT_POWER_FEATURE_NAME,
								featureName,
								k+1
						);
						likelyHandledname = GuardExpression.Factory.transformToVariableIdentifier(
								DecisionMining.wekaUnescape(
										WekaUtil.fixVarName(expAttr)));
						this.converetedNames.put(expAttr, likelyHandledname);
						classTypes.put(DecisionMining.fixVarName(expAttr), Type.CONTINUOS);
					}
				}
			}
		}
//		handle class types for attributes
		for (Entry<String, Type> val : this.source.makeClassTypes().entrySet()) {
			classTypes.put(DecisionMining.fixVarName(val.getKey()), val.getValue());
			this.converetedNames.put(val.getKey(), DecisionMining.fixVarName(val.getKey()));
		}
//		handle literal values for discrete attributes		
		for( Entry<String, Set<String>> val : this.source.makeLiteralValues().entrySet() ) {
			literalValues.put(DecisionMining.fixVarName(val.getKey()), val.getValue());
		}
		System.out.println("[InvestigationTask] Using the following attributes "
				+ "for the classification problem :: " + converetedNames.keySet().toString());
		System.out.println("[InvestigationTask] Using the following mappings "
				+ "between attribute and types :: "+ classTypes.toString());
		
//		### Pirate Code from DecisionMining:393 ###
		/*
		 * For each place with at least 2 outgoing edges in the net ..
		 */
		PetrinetGraph net = this.source.getModel();
		for (Place place : net.getPlaces()) {
			// If 'place' does not represent an OR-split (outgoing edges < 2), goto next iteration of for-loop (skip the place, not interesting)
			if (net.getOutEdges(place).size() < 2) {
				continue;
			}
			/*
			 * 'place' is part of an OR-split decision point with >= 2 outgoing
			 * edges. Prepare an array outputValues[] to collect the target
			 * Transitions of the outgoing edges of 'place'
			 */
			Transition outputValues[] = new Transition[net.getOutEdges(place).size()];
			int index = 0;
			/*
			 * For each outgoing edge 'arc' of Place 'place', store that edge's
			 * target Transition in the array.
			 */
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> arc : net.getOutEdges(place)) {
				outputValues[index++] = (Transition) arc.getTarget();
			}
			/*
			 * Prepare variable f for a functionEstimator, and create one with
			 * the attribute types, values and resulting Transitions for Place
			 * place
			 */
			FunctionEstimator f;
			
			if (this.minerType == MinerType.OVERLAPPING) {
				f = new OverlappingEstimatorLocalDecisionTree(classTypes, 
						literalValues, outputValues, this.source.getLog().size(),
						place.getLabel()
				);
			} else if (this.minerType == MinerType.DISCRIMINATING) {
				f = new DiscriminatingFunctionEstimator(classTypes, literalValues,
						outputValues, this.source.getLog().size(), place.getLabel()
				);
			} else if (this.minerType == MinerType.DECISIONTREE) {
				f = new DecisionTreeFunctionEstimator(classTypes, literalValues,
						outputValues, place.getLabel(), this.source.getLog().size()
					);
			} else {
				throw new IllegalArgumentException("[InvestigationTask] Decision"
						+ " Miner unknown :: " + this.minerType);
			}

			

			/*
			 * Associate the created FunctionEstimator 'f' with Place 'place' by
			 * putting them in a Mapping from <Place> to <FunctionEstimator>
			 */
			this.estimators.put(place, f);
		}
		
		return this;
	}
	
	@Override
	protected DiscoveredPetriNetWithData doInBackground() throws Exception {
		try {
		dummyProgress progress = new dummyProgress(this);
//		create some futures
		List<Future<Integer>> traceFutures = new ArrayList<>();
		// Prepare trace counters
		int traceSkipped = 0;
		int totalNumTraces = 0;
		// For each alignment in the PNRepResult 'input'
		for (SyncReplayResult alignment : this.source.getAlignment()) {
			// Count the number of traces encountered in total
			totalNumTraces += alignment.getTraceIndex().size();
			// If the alignment's fitness complies with the specified minimum 
//			   fitness in fitnessThresholdSlider
			if (alignment.getInfo().get(PNRepResult.TRACEFITNESS).floatValue() 
					>= this.config.getFitnessThreshold()
				) {
				// Then for each trace in the alignment
				for (Integer index : alignment.getTraceIndex()) {
					/*
					 * Have a thread in the pool process the trace in the
					 * alignment, adding instances to the estimator and keeping
					 * track of attribute values. For each alignment, For each
					 * step in the alignment, For each in-edge of the
					 * transition, If the source-Place of that in-edge has an
					 * estimator, Add an instance corresponding to the variable
					 * values in that place, before executing the transition
					 * corresponding to the step. An instance is a set of
					 * attribute values' pre-values and a transition to be
					 * executed
					 */
					if (config.isExperimentalFeatures()) {
						traceFutures.add(this.pool.submit(
								new ExperimentalFeatureProcessor(
									this.source.getModel(), 
									this.source.getLog().get(index),
									this.estimators,
									alignment,
									this.numberOfExecutions,
									this.numberOfWritesPerTransition,
									progress,
									config.isExperimentalDFTFeatures(),
									config.isExperimentalSAXFeatures(),
									config.isExperimentalEDTTSFeatures()
									)
								, index)
							);
					} else {
						traceFutures.add(this.pool.submit(
							new TraceProcessor(
								this.source.getModel(), 
								this.source.getLog().get(index),
								this.estimators,
								alignment,
								this.numberOfExecutions,
								this.numberOfWritesPerTransition,
								progress
								)
							, index)
						);
					}
				}
			} else {
				// The alignment's fitness is lower than the fitness threshold; skip the trace and count the skipped traces
				traceSkipped += alignment.getTraceIndex().size();
			}
		}
		
		progress.setMaximum(traceFutures.size()+ estimators.entrySet().size());
		
		for (Future<Integer> traceFuture : traceFutures) {
			// This blocks until the trace processor is done
			traceFuture.get();
		}
		System.out.println("[ExogenousInvestigationTask] Completed trace futures...");
		System.out.println("[ExogenousInvestigationTask] skipped traces : " + traceSkipped);
		
		
		DiscoveredPetriNetWithData discoveredDPN = null;
		Map<Place, Future<DecisionPointResult>> results = new HashMap<>();

		// For each place
		for (Place place : this.source.getModel().getPlaces()) {
			// Configure the corresponding function estimator's decision tree parameters
			FunctionEstimator f = estimators.get(place);
			if (f != null) {
				if (f instanceof DecisionTreeBasedFunctionEstimator) {
					DecisionTreeBasedFunctionEstimator ff = (DecisionTreeBasedFunctionEstimator) f;
					int numInstances = ff.getNumInstances();
					// Set the minimal number of instances that each leaf should support
					if (this.config.getInstanceHandling().isREL()) {
						ff.setMinNumObj( 
							(int) (numInstances * this.config.getRelativeInstanceLevel())	
						);
						System.out.println("[ExogenousInvestigationTask] Setting estimator ("
								+place.getLabel()+") : "+ ff.getNumInstances() 
								+" :min: " 
								+(int) (numInstances * this.config.getRelativeInstanceLevel())
						);
					} else if (this.config.getInstanceHandling().isABS()){
						ff.setMinNumObj( 
								(int) ( this.config.getAbsoluteInstanceLevel())	
							);
						System.out.println("[ExogenousInvestigationTask] Setting estimator ("
								+place.getLabel()+") : "+ ff.getNumInstances() 
								+" :min: " 
								+(int) (this.config.getAbsoluteInstanceLevel())
						);
					} else if (this.config.getInstanceHandling().isTHRESHOLD()) {
						int min = 2;
						if (numInstances > this.config.getInstanceThreshold()) {
							min = (int) (this.config.getRelativeInstanceLevel() 
									* numInstances); 
						} else {
							min = (int)this.config.getAbsoluteInstanceLevel();
						}
						ff.setMinNumObj(min);
						System.out.println("[ExogenousInvestigationTask] Setting estimator ("
								+place.getLabel()+") : "+ ff.getNumInstances() 
								+" :min: " 
								+ min
						);
					} else {
//						default to relative
						ff.setMinNumObj( 
								(int) (numInstances * this.config.getRelativeInstanceLevel())	
						);
					}
//					other parameters of note
					ff.setUnpruned(this.config.isUnpruned());
					ff.setBinarySplit(this.config.isBinarySplit());
					ff.setCrossValidate(this.config.isCrossValidate());
					if (ff instanceof AbstractDecisionTreeFunctionEstimator) {
						((AbstractDecisionTreeFunctionEstimator) ff).setNumFoldCrossValidation(
								this.config.getCrossValidateFolds()
						);
					}
					ff.setConfidenceFactor(this.config.getConfidenceLevel());
				}
			}
		}
		System.out.println("[ExogenousInvestigationTask] Configured estimators...");
		/*
		 * Collect result for each decision point
		 */
		for (Entry<Place, FunctionEstimator> estimatorPlacePair : estimators.entrySet()) {

			final Place place = estimatorPlacePair.getKey();
			final FunctionEstimator f = estimatorPlacePair.getValue();

			Future<DecisionPointResult> result = pool.submit(new Callable<DecisionPointResult>() {

				public DecisionPointResult call() throws Exception {

//					   Calculate the conditions with likelihoods for each target
//					   transition of place entry2.getKey()
					final Map<Object, FunctionEstimation> estimationTransitionExpression = f
							.getFunctionEstimation(null);
					
					double sumFMeasures = 0;
					int numRules = 0;
					for (FunctionEstimation val : estimationTransitionExpression.values()) {
						sumFMeasures += val.getQualityMeasure();
						numRules++;
					}

					final double singleFScore = numRules > 0 ? sumFMeasures / numRules : 0;
					final String decisionPointClassifier = f.toString();

					DecisionPointResult result = new DecisionPointResult() {

						public String toString() {
							return decisionPointClassifier;
						}

						public double getQualityMeasure() {
							return singleFScore;
						}

						public Map<Object, FunctionEstimation> getEstimatedGuards() {

							return estimationTransitionExpression;
						}

					};

					System.out.println(String.format("[ExogenousInvestigationTask]"
							+ " Generated the conditions for decision point %s "
							+ "with f-score %s",
							place.getLabel(), result.getQualityMeasure())
					);
					progress.inc();
					return result;
				}

			});
			results.put(place, result);
		}
		System.out.println("[ExogenousInvestigationTask] Completed training estimators...");
//		Prepare the mining algorithm's resulting PetriNetWithData.
		String dpnName = String.format("%s (%s, min instances per leaf: %.3f, "
				+ "pruning: %s, binary: %s)", this.source.getModel().getLabel(),
				"Overlapping Decision Tree", this.config.getInstanceThreshold(),
				false, false);
		final PetriNetWithDataFactory factory = new PetriNetWithDataFactory(this.source.getModel(),
				new DiscoveredPetriNetWithData(dpnName), false);
		discoveredDPN = (DiscoveredPetriNetWithData) factory.getRetValue(); // cast if safe
		
		/*
		 * For each entry in classTypes, <String, Type> representing (Attribute
		 * name, attribute type), depending on the type add a new Variable to
		 * the new PetriNetWithData without min or max values
		 */

		for (Entry<String, Type> entry : this.source.makeClassTypes().entrySet()) {
			Class<?> classType = null;
			switch (entry.getValue()) {
				case BOOLEAN :
					classType = Boolean.class;
					break;
				case CONTINUOS :
					classType = Double.class;
					break;
				case DISCRETE :
					classType = Long.class;
					break;
				case LITERAL :
					classType = String.class;
					break;
				case TIMESTAMP :
					classType = Date.class;
					break;
				default :
					break;

			}
			String wekaUnescaped = DecisionMining.wekaUnescape(entry.getKey());
			String saneVariableName = GuardExpression.Factory.transformToVariableIdentifier(wekaUnescaped);
			discoveredDPN.addVariable(saneVariableName, classType, null, null);
		}
		if (config.isExperimentalFeatures()) {
			for(  Determination deter : this.source.getSource().getSource().getDeterminations()) {
				String featureName = deter.getPanel().getName()
						+"_"+deter.getSlicer().getShortenName();
				featureName = featureName.replace(":", "_");
//				add sax feature names
				for(int i=0;i < this.SAX_FROM.size(); i++) {
					String expAttr = String.format(ExperimentalFeatureProcessor.SAX_OUTLIER_TO_OUTLIER_FEATURE_NAME,
							featureName,
							this.SAX_FROM.get(i),
							this.SAX_TO.get(i)
					);
					expAttr = WekaUtil.fixVarName(expAttr);
					String wekaUnescaped = DecisionMining.wekaUnescape(expAttr);
					String saneVariableName = GuardExpression.Factory.transformToVariableIdentifier(wekaUnescaped);
//					System.out.println("Adding var to DPN :: " + saneVariableName);
					discoveredDPN.addVariable(saneVariableName, Boolean.class, null, null);
				}
//				add dft feature names
				for(int k=0; k < K_TOP_DFT_COEFICIENTS; k++) {
//					add discrete feature for k-freq
					String expAttr = String.format(ExperimentalFeatureProcessor.TOP_K_DFT_FREQ_FEATURE_NAME,
							featureName,
							k+1
					);
					expAttr = WekaUtil.fixVarName(expAttr);
					String wekaUnescaped = DecisionMining.wekaUnescape(expAttr);
					String saneVariableName = GuardExpression.Factory.transformToVariableIdentifier(wekaUnescaped);
					discoveredDPN.addVariable(saneVariableName, long.class, null, null);
//					add continuous feature for k-power
					expAttr = String.format(ExperimentalFeatureProcessor.TOP_K_DFT_POWER_FEATURE_NAME,
							featureName,
							k+1
					);
					expAttr = WekaUtil.fixVarName(expAttr);
					wekaUnescaped = DecisionMining.wekaUnescape(expAttr);
					saneVariableName = GuardExpression.Factory.transformToVariableIdentifier(wekaUnescaped);
					discoveredDPN.addVariable(saneVariableName, Double.class, null, null);
				}
			}
		}
		
		//TODO FM, we should not change the default Locale!! What about concurrent operations that rely on the correct Locale? Why is it done anyway?
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.US);

		double sumFScores = 0;
		int numEstimators = 0;

		Map<Place, Place> placeMapping = factory.getPlaceMapping();
		this.transMap = factory.getTransMapping();

		for (Entry<Place, Future<DecisionPointResult>> futureEntry : results.entrySet()) {
			try {
				DecisionPointResult result = futureEntry.getValue().get();
				discoveredDPN.storeDecisionPointResult(placeMapping.get(futureEntry.getKey()), result);

				// If, for any Transition at this decision point, an expression is found..
				if (!result.getEstimatedGuards().isEmpty()) {
					// Then, for each such Transition, set the guard in the PetriNetWithData
					for (Entry<Object, FunctionEstimation> transitionEntry : result.getEstimatedGuards().entrySet()) {
						Transition transitionInPNWithoutData = (Transition) transitionEntry.getKey();
						PNWDTransition transitionInPNWithData = (PNWDTransition) factory.getTransMapping()
								.get(transitionInPNWithoutData);
						FunctionEstimation value = transitionEntry.getValue();
						if (transitionInPNWithData.getGuardExpression() != null) {
							//TODO find correct method to update f-score / what to do in the general case for more than two incoming arcs
							Double combinedFScore = (value.getQualityMeasure() + transitionInPNWithData.getQuality())
									/ 2;
							GuardExpression existingGuard = transitionInPNWithData.getGuardExpression();
							GuardExpression additionalGuard = value.getExpression();
							GuardExpression combinedGuard = GuardExpression.Operation.and(existingGuard,
									additionalGuard);
//							System.out.println(
//									String.format("[ExogenousInvestigationTask] Combining two guards for non-free choice construct: %s (%s) %s (%s)",
//											existingGuard, value.getQualityMeasure(), additionalGuard,
//											transitionInPNWithData.getQuality()));
							discoveredDPN.setGuard(transitionInPNWithData, combinedGuard, combinedFScore);
						} else {
//							System.out.println("[ExogenousInvestigationTask] found guard expression : "+ value.getExpression());
							
							discoveredDPN.setGuard(transitionInPNWithData, value.getExpression(),
									value.getQualityMeasure());
						}
					}

					sumFScores += result.getQualityMeasure();
					numEstimators++;
				} else {
//					System.out.println("[ExogenousInvestigationTask] hmmm :" +result.toString());
				}
			} catch (ExecutionException e) {
				System.out.println("[ExogenousInvestigationTask] error occured :" +e);
			}
		}

		//TODO see above!
		Locale.setDefault(defaultLocale);
		
		if (sumFScores > 0) {
			System.out.println("[ExogenousInvestigationTask] Average F Score: " + sumFScores / numEstimators);
		}
		discoveredDPN.removeAllVariablesNotInGuard();
		pool.shutdown();
		return discoveredDPN;
		
		
		} catch (Exception e) {
			System.out.println("[ExogenousInvestigationTask] Error occured in work : "+ e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public class dummyProgress implements Progress {
		
		private InvestigationTask source;
		private ExogenousDiscoveryProgresser progresser;
		private ProgressState state;
		private double value;
		private int max;
		
		public dummyProgress(InvestigationTask source) {
			this.source = source;
			this.progresser = source.progresser;
			this.state = this.progresser.getState(ProgressType.Investigation);
			this.state.setCaption("Starting...");
			this.state.setProgress(0);
			this.state.setTotal(0);
			this.state.update();
		}

		@Override
		public void setMinimum(int value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMaximum(int value) {
			// TODO Auto-generated method stub
			state.setTotal(value);
		}

		@Override
		public void setValue(int value) {
			// TODO Auto-generated method stub
			state.setProgress(value);
		}

		@Override
		public void setCaption(String message) {
			// TODO Auto-generated method stub
			state.setCaption(message);
		}

		@Override
		public String getCaption() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getValue() {
			// TODO Auto-generated method stub
			return state.getProgress();
		}

		@Override
		public void inc() {
			// TODO Auto-generated method stub
			state.increment();
		}

		@Override
		public void setIndeterminate(boolean makeIndeterminate) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isIndeterminate() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getMinimum() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getMaximum() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isCancelled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void cancel() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
}