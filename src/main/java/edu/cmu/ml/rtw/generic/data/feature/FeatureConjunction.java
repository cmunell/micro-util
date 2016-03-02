package edu.cmu.ml.rtw.generic.data.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
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
 *  referencedFeatures - referenced features given by a '/' separated list of 
 *  feature 'referenceNames' used within the FeaturizedDataSet given to initialize
 *  FeatureConjunction.
 * 
 * FIXME Change the '/' separated feature list to be an edu.cmu.ml.rtw.generic.parse.Obj.Array
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class FeatureConjunction<D extends Datum<L>, L> extends Feature<D, L> {
	private BidirectionalLookupTable<String, Integer> vocabulary;
	private int minFeatureOccurrence;
	private String[] featureReferences;
	private String[] parameterNames = {"minFeatureOccurrence", "featureReferences"};
	
	public FeatureConjunction() {
		
	}
	
	public FeatureConjunction(DatumContext<D, L> context) {
		this.context = context;
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
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
		Map<String, Double> conjunction = new HashMap<String, Double>();
		conjunction.put("", 1.0);
		for (int i = 0; i < this.featureReferences.length; i++) {
			Feature<D, L> feature = this.context.getMatchFeature(Obj.curlyBracedValue(this.featureReferences[i])); // FIXME These should be stored as an Obj.Array
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
		else if (parameter.equals("featureReferences")) {
			if (this.featureReferences == null)
				return Obj.stringValue("");
			StringBuilder featureReferences = new StringBuilder();
			for (int i = 0; i < this.featureReferences.length; i++)
				featureReferences = featureReferences.append(this.featureReferences[i]).append("/");
			if (featureReferences.length() > 0)
				featureReferences = featureReferences.delete(featureReferences.length() - 1, featureReferences.length());
			return Obj.stringValue(featureReferences.toString());
		}

		return null;
	}

	@Override
	public boolean setParameterValue(String parameter,
			Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence")) {
		 	this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("featureReferences")) {
			this.featureReferences = this.context.getMatchValue(parameterValue).split("/");
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

		return binaryFeatureConj;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments;
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureConjunction<D, L> cloneConj = (FeatureConjunction<D, L>)clone;
		cloneConj.vocabulary = this.vocabulary;
		return true;
	}
}
