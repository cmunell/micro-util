package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

/**
 * For a datum d, FeatureConjunction computes a vector whose elements are given by
 * a flattening of the tensor product of vectors computed for d by referenced
 * features.  
 * 
 * Parameters:
 *  minFeatureOccurrence - the minimum number of times a conjunction must occur to 
 *  be included in the vector
 *
 *  features - referenced features used to initialize the feature conjunction 
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class FeatureConjunction<D extends Datum<L>, L> extends Feature<D, L> {
	public enum Mode {
		EQUALITY,
		PRODUCT
	}
	
	private BidirectionalLookupTable<String, Integer> vocabulary;
	private List<Feature<D, L>> internalFeatures;
	
	private Mode mode = Mode.PRODUCT;
	private int minFeatureOccurrence;
	private Obj.Array features;
	private String[] parameterNames = {"minFeatureOccurrence", "features", "mode"};
	
	public FeatureConjunction() {
		
	}
	
	public FeatureConjunction(DatumContext<D, L> context) {
		this.context = context;
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.internalFeatures = new ArrayList<>();
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		this.internalFeatures = new ArrayList<>();
		
		for (int i = 0; i < this.features.size(); i++) {
			Feature<D, L> feature = this.context.getMatchFeature(this.features.get(i)).clone(false);
			if (feature.getVocabularySize() == 0)
				if (!feature.init(dataSet))
					return false;
			this.internalFeatures.add(feature);
		}
		
		final CounterTable<String> counter = new CounterTable<String>();
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Map<String, Double> conjunction = conjunctionForDatum(datum);
				for (String key : conjunction.keySet())
					counter.incrementCount(key);
				return true;
			}
		}, this.context.getMaxThreads());
		
		counter.removeCountsLessThan(this.minFeatureOccurrence);
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}

	private Feature<D, L> getFeature(int index) {
		if (this.internalFeatures != null && this.internalFeatures.size() > index)
			return this.internalFeatures.get(index);
		else
			return this.context.getMatchFeature(this.features.get(index));
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Map<String, Double> unfilteredConjunction = conjunctionForDatum(datum);
		for (Entry<String, Double> entry : unfilteredConjunction.entrySet()) {
			if (this.vocabulary.containsKey(entry.getKey()))
				vector.put(this.vocabulary.get(entry.getKey()) + offset, entry.getValue());
		}
		
		return vector;
	}
	
	private Map<String, Double> conjunctionForDatum(D datum) {
		if (this.mode == Mode.PRODUCT)
			return productForDatum(datum);
		else
			return equalityForDatum(datum);
	}
	
	private Map<String, Double> equalityForDatum(D datum) {
		Map<String, Double> conjunction = new HashMap<String, Double>();
		boolean equal = true;
		Map<Integer, Double> prevValues = null;
		Map<Integer, String> prevVocab = null;
		for (int i = 0; i < this.features.size(); i++) {
			Feature<D, L> feature = getFeature(i);
			Map<Integer, Double> values = feature.computeVector(datum, 0, new HashMap<Integer, Double>());
			Map<Integer, String> vocab = feature.getVocabularyForIndices(values.keySet());
		
			if (prevValues != null) {
				if (values.size() != prevValues.size()) {
					equal = false;
					break;
				}
				
				for (Entry<Integer, Double> entry : values.entrySet()) {
					if (!prevValues.containsKey(entry.getKey())) {
						equal = false;
						break;
					}
					
					if (!prevValues.get(entry.getKey()).equals(entry.getValue())) {
						equal = false;
						break;
					}
					
					if (!prevVocab.get(entry.getKey()).equals(vocab.get(entry.getKey()))) {
						equal = false;
						break;
					}
				}
				
				if (!equal)
					break;
			} else {
				prevValues = values;
				prevVocab = vocab;
			}
		}
		
		conjunction.put("eq", (equal) ? 1.0 : 0.0);
		
		return conjunction;
	}
	
	private Map<String, Double> productForDatum(D datum) {
		Map<String, Double> conjunction = new HashMap<String, Double>();
		conjunction.put("", 1.0);
		for (int i = 0; i < this.features.size(); i++) {
			Feature<D, L> feature = getFeature(i); 
			Map<Integer, Double> values = feature.computeVector(datum, 0, new HashMap<Integer, Double>());
			Map<Integer, String> vocab = feature.getVocabularyForIndices(values.keySet());
			Map<String, Double> nextConjunction = new HashMap<String, Double>();
			
			for (Entry<String, Double> conjunctionEntry : conjunction.entrySet()) {
				for (Entry<Integer, String> vocabEntry : vocab.entrySet()) {
					nextConjunction.put(conjunctionEntry.getKey() + "//" + vocabEntry.getValue(), conjunctionEntry.getValue()*values.get(vocabEntry.getKey()));
				}
			}
			
			conjunction = nextConjunction;
		}
		
		return conjunction;
	}

	@Override
	public String getGenericName() {
		return "Conjunction";
	}

	@Override
	public int getVocabularySize() {
		return this.vocabulary.size();
	}

	@Override
	public String getVocabularyTerm(int index) {
		return this.vocabulary.reverseGet(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		this.vocabulary.put(term, index);
		return true;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("minFeatureOccurrence"))
			return Obj.stringValue(String.valueOf(this.minFeatureOccurrence));
		else if (parameter.equals("features")) {
			return this.features;		
		} else if (parameter.equals("mode")) {
			return Obj.stringValue(this.mode.toString());
		}

		return null;
	}

	@Override
	public boolean setParameterValue(String parameter,
			Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence")) {
		 	this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("features")) {
			this.features = (Obj.Array)parameterValue;
		} else if (parameter.equals("mode")) {
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		} else {
			return false;
		}
		return true;
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureConjunction<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureConjunction<T, Boolean> binaryFeatureConj = (FeatureConjunction<T, Boolean>)binaryFeature;
		binaryFeatureConj.vocabulary = this.vocabulary;

		if (this.internalFeatures != null) {
			for (Feature<D, L> feature : this.internalFeatures)
				binaryFeatureConj.internalFeatures.add(feature.makeBinary(context, labelIndicator));
		}
		
		return binaryFeatureConj;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		this.internalFeatures = new ArrayList<>();
		
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (!assignment.getType().equals(DatumContext.ObjectType.FEATURE.name()))
				continue;
			
			Obj.Function featureObj = (Obj.Function)assignment.getValue();
			Feature<D, L> feature = this.context.getDatumTools().makeFeatureInstance(featureObj.getName(), this.context);
			if (!feature.fromParse(assignment.getModifiers(), this.referenceName, featureObj))
				return false;
			
			this.internalFeatures.add(feature);
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		if (this.internalFeatures == null)
			return internalAssignments;
		
		for (Feature<D, L> feature : this.internalFeatures) {
			internalAssignments.add(
				AssignmentTyped.assignmentTyped(null, DatumContext.ObjectType.FEATURE.name(), feature.getReferenceName(), feature.toParse(true))
			);
		}
		
		return internalAssignments;
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureConjunction<D, L> cloneConj = (FeatureConjunction<D, L>)clone;
		cloneConj.vocabulary = this.vocabulary;
		
		for (Feature<D, L> feature : this.internalFeatures) {
			cloneConj.internalFeatures.add(feature.clone(true));
		}
		
		return true;
	}
}
