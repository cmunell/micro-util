package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;

/**
 * 
 * 
 * @author Bill McDowell
 * 
 * @param <D> datum type
 * @param <L> datum label type
 *
 */
public abstract class FeatureGram<D extends Datum<L>, L> extends Feature<D, L> {
	/**
	 * 
	 * Scale gives possible functions by which to scale the computed
	 * feature vectors.  The INDICATOR function just returns 1 if an n-gram is 
	 * in F(T(d)) for datum d (F and T defined in documenation above).  The
	 * NORMALIZED_LOG function applies log(1+tf(F(T(d)), v) to n-gram v, where
	 * tf(x,v) computes the frequency of v in x.   Similarly, NORMALIZED_TFIDF
	 * applies tfidf for each n-gram.  Both NORMALIZED_LOG and NORMALIZED_TFIDF
	 * are normalized in the sense that the feature vector for n-gram v is 
	 * scaled to length 1.
	 * 
	 */
	public enum Scale {
		INDICATOR,
		NORMALIZED_LOG,
		NORMALIZED_TFIDF
	}
	
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	protected Map<Integer, Double> idfs; // maps vocabulary term indices to idf values to use in tfidf scale function
	
	protected int minFeatureOccurrence;
	protected DataTools.StringTransform cleanFn;
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected Scale scale;
	protected String[] parameterNames = {"minFeatureOccurrence", "cleanFn", "tokenExtractor", "scale"};
	
	/**
	 * @param datum
	 * @return grams associated with the datum in a certain way that
	 * depends on the the particular Gram feature that is being computed.  See
	 * edu.cmu.ml.rtw.generic.data.feature.FeatureNGram for example.
	 */
	protected abstract Map<String, Integer> getGramsForDatum(D datum);
	
	public FeatureGram() {
		
	}
	
	public FeatureGram(Context<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.idfs = new HashMap<Integer, Double>();
		this.scale = Scale.INDICATOR;
		this.context = context;
	}
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		final CounterTable<String> counter = new CounterTable<String>();
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Map<String, Integer> gramsForDatum = getGramsForDatum(datum);
				for (String gram : gramsForDatum.keySet()) {
					counter.incrementCount(gram);
				}
				return true;
			}
		});
		
		counter.removeCountsLessThan(this.minFeatureOccurrence);
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		Map<String, Integer> counts = counter.getCounts();
		double N = dataSet.size();
		for (Entry<String, Integer> entry : counts.entrySet()) {
			this.idfs.put(this.vocabulary.get(entry.getKey()), Math.log(N/(1.0 + entry.getValue())));
		}
		
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Map<String, Integer> gramsForDatum = getGramsForDatum(datum);
		
		if (this.scale == Scale.INDICATOR) {
			for (String gram : gramsForDatum.keySet()) {
				if (this.vocabulary.containsKey(gram))
					vector.put(this.vocabulary.get(gram) + offset, 1.0);		
			}
		} else if (this.scale == Scale.NORMALIZED_LOG) {
			double norm = 0.0;
			Map<Integer, Double> tempVector = new HashMap<Integer, Double>();
			for (Entry<String, Integer> entry : gramsForDatum.entrySet()) {
				if (!this.vocabulary.containsKey(entry.getKey()))
					continue;
				int index = this.vocabulary.get(entry.getKey());
				double value = Math.log(entry.getValue() + 1.0);
				norm += value*value;
				tempVector.put(index, value);
			}
			
			norm = Math.sqrt(norm);
			
			for (Entry<Integer, Double> entry : tempVector.entrySet()) {
				vector.put(entry.getKey() + offset, entry.getValue()/norm);
			}
		} else if (this.scale == Scale.NORMALIZED_TFIDF) {
			double norm = 0.0;
			Map<Integer, Double> tempVector = new HashMap<Integer, Double>();
			for (Entry<String, Integer> entry : gramsForDatum.entrySet()) {
				if (!this.vocabulary.containsKey(entry.getKey()))
					continue;
				int index = this.vocabulary.get(entry.getKey());
				double value = entry.getValue()*this.idfs.get(index);//Math.log(entry.getValue() + 1.0);
				norm += value*value;
				tempVector.put(index, value);
			}
			
			norm = Math.sqrt(norm);
			
			for (Entry<Integer, Double> entry : tempVector.entrySet()) {
				vector.put(entry.getKey() + offset, entry.getValue()/norm);
			}
		}

		return vector;
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
	public int getVocabularySize() {
		return this.vocabulary.size();
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("minFeatureOccurrence")) 
			return Obj.stringValue(String.valueOf(this.minFeatureOccurrence));
		else if (parameter.equals("cleanFn"))
			return Obj.stringValue((this.cleanFn == null) ? "" : this.cleanFn.toString());
		else if (parameter.equals("tokenExtractor"))
			return Obj.stringValue((this.tokenExtractor == null) ? "" : this.tokenExtractor.toString());
		else if (parameter.equals("scale"))
			return Obj.stringValue(this.scale.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence")) 
			this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("cleanFn"))
			this.cleanFn = this.context.getDatumTools().getDataTools().getCleanFn(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("scale"))
			this.scale = Scale.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureGram<T, Boolean> binaryFeatureGram = (FeatureGram<T, Boolean>)binaryFeature;
		
		binaryFeatureGram.vocabulary = this.vocabulary;
		binaryFeatureGram.idfs = this.idfs;
		
		return binaryFeatureGram;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;
		if (!internalAssignments.contains("idfs"))
			return false;
		
		this.idfs = new HashMap<Integer, Double>();
		
		Obj.Array idfs = (Obj.Array)internalAssignments.get("idfs").getValue();
		for (int i = 0; i < idfs.size(); i++)
			this.idfs.put(i, Double.valueOf(idfs.get(i).getStr()));
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (this.vocabulary.size() == 0)
			return internalAssignments;
		
		Obj.Array idfs = Obj.array();
		for (int i = 0; i < this.vocabulary.size(); i++) {
			if (this.idfs.containsKey(i))
				idfs.add(Obj.stringValue(String.valueOf(this.idfs.get(i))));
			else
				idfs.add(Obj.stringValue("0.0"));
		}
		
		internalAssignments.add(Assignment.assignmentTyped(new ArrayList<String>(), Context.ARRAY_STR, "idfs", idfs));
		
		return internalAssignments;
	}
	
	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureGram<D, L> cloneGram = (FeatureGram<D, L>)clone;
		cloneGram.vocabulary = this.vocabulary;
		cloneGram.idfs = this.idfs;
		return true;
	}
}
