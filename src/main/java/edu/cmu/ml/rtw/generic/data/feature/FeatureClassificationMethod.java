package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public class FeatureClassificationMethod<D extends Datum<L>, L> extends Feature<D, L> {	
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected Obj methodObj;
	protected MethodClassification<D, L> method;
	protected String[] parameterNames = {"method"};
	
	public FeatureClassificationMethod() {
		
	}
	
	public FeatureClassificationMethod(DatumContext<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		final CounterTable<String> counter = new CounterTable<String>();		
		
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				L label = method.classify(datum);
				if (label != null)
					counter.incrementCount(label.toString());
				return true;
			}
		}, this.context.getMaxThreads());
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Pair<L, Double> label = this.method.classifyWithScore(datum);
		if (label == null)
			return vector;
		
		String labelStr = label.toString();
		if (!this.vocabulary.containsKey(labelStr))
			return vector;
		
		double score = label.getSecond() == null ? 1.0 : label.getSecond();
		
		vector.put(this.vocabulary.get(labelStr) + offset, score);
		
		return vector;
	}

	public Integer getVocabularyIndex(String term) {
		return this.vocabulary.get(term);
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
		if (parameter.equals("method")) {
			return this.methodObj;
		}
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("method")) {
			this.methodObj = parameterValue;
			this.method = (parameterValue == null) ? null : this.context.getMatchClassifyMethod(parameterValue);
		} else
			return false;
		return true;
	}
	
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureClassificationMethod<T, Boolean> binaryFeatureCM = (FeatureClassificationMethod<T, Boolean>)binaryFeature;
		
		binaryFeatureCM.vocabulary = this.vocabulary;
		
		return binaryFeatureCM;
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
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureClassificationMethod<D, L>(context);	
	}

	@Override
	public String getGenericName() {
		return "MethodClassification";
	}

	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureClassificationMethod<D, L> cloneData = (FeatureClassificationMethod<D, L>)clone;
		cloneData.vocabulary = this.vocabulary;
		return true;
	}
}
