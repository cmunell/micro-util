package edu.cmu.ml.rtw.generic.data.feature.meta;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.meta.PredictionClassificationDatum;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;

public class FeatureMetaClassificationAttribute<L> extends Feature<PredictionClassificationDatum<L>, L> {
	public enum Attribute {
		LABEL,
		METHOD,
		SCORE,
		LOG_SCORE
	}
	
	private Attribute attribute;
	private String[] parameterNames = { "attribute" };
	
	private BidirectionalLookupTable<String, Integer> vocabulary;
	
	public FeatureMetaClassificationAttribute() {
		
	}
	
	public FeatureMetaClassificationAttribute(DatumContext<PredictionClassificationDatum<L>, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<PredictionClassificationDatum<L>, L> dataSet) {
		CounterTable<String> counter = new CounterTable<String>();
		
		if (this.attribute == Attribute.LABEL) {
			for (PredictionClassificationDatum<L> datum : dataSet) {
				counter.incrementCount(datum.getPrediction().getLabel().toString());
			}
		} else if (this.attribute == Attribute.METHOD) {
			for (PredictionClassificationDatum<L> datum : dataSet) {
				counter.incrementCount(datum.getPrediction().getMethod().getReferenceName());
			}
		} else if (this.attribute == Attribute.SCORE) {
			counter.incrementCount("value");
		} else if (this.attribute == Attribute.LOG_SCORE) {
			counter.incrementCount("value");
		}
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(PredictionClassificationDatum<L> datum, int offset, Map<Integer, Double> vector) {
		if (this.attribute == Attribute.LABEL) {
			if (!this.vocabulary.containsKey(datum.getPrediction().getLabel().toString()))
				return vector;
			vector.put(offset + this.vocabulary.get(datum.getPrediction().getLabel().toString()), 1.0);
		} else if (this.attribute == Attribute.METHOD) {
			if (!this.vocabulary.containsKey(datum.getPrediction().getMethod().getReferenceName()))
				return vector;
			vector.put(offset + this.vocabulary.get(datum.getPrediction().getMethod().getReferenceName()), 1.0);
		} else if (this.attribute == Attribute.SCORE) {
			vector.put(offset, datum.getPrediction().getScore());
		} else if (this.attribute == Attribute.LOG_SCORE) {
			vector.put(offset, datum.getPrediction().getScore() <= 0.0 ? 0.0 : Math.log(datum.getPrediction().getScore()));
		}
		
		return vector;
	}

	@Override
	public String getGenericName() {
		return "MetaClassificationAttribute";
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
		if (parameter.equals("attribute"))
			return (this.attribute == null) ? null : Obj.stringValue(this.attribute.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("attribute"))
			this.attribute = (parameterValue == null) ? null : Attribute.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Feature<PredictionClassificationDatum<L>, L> makeInstance(DatumContext<PredictionClassificationDatum<L>, L> context) {
		return new FeatureMetaClassificationAttribute<L>(context);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureMetaClassificationAttribute<Boolean> binaryFeatureEventAttribute = (FeatureMetaClassificationAttribute<Boolean>)binaryFeature;
		binaryFeatureEventAttribute.vocabulary = this.vocabulary;
		return (Feature<T, Boolean>)binaryFeatureEventAttribute;
	}

	@Override
	protected boolean cloneHelper(Feature<PredictionClassificationDatum<L>, L> clone) {
		FeatureMetaClassificationAttribute<L> featureClone = (FeatureMetaClassificationAttribute<L>)clone;
		featureClone.vocabulary = this.vocabulary;
		return true;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		return internalAssignments;
	}
}
