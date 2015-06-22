package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FeatureTokenCount computes an indicator of whether token
 * spans extracted from a datum have greater length (in number
 * of tokens) than a given count.
 * 
 * Parameters:
 *  tokenExtractor - extractor for taking token spans from a datum
 *  
 *  maxCount - number of tokens in a token span above which the computed
 *  indicator is is 1
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class FeatureTokenCount<D extends Datum<L>, L> extends Feature<D, L> {
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected int maxCount;
	protected String[] parameterNames = {"tokenExtractor", "maxCount"};
	
	public FeatureTokenCount() {
		
	}
	
	public FeatureTokenCount(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		
		for (TokenSpan tokenSpan : tokenSpans) {
			int tokenCount = tokenSpan.getLength();
			if (tokenCount > this.maxCount)
				tokenCount = this.maxCount;
			
			vector.put(tokenCount + offset, 1.0);
		}

		return vector;
	}

	@Override
	public String getVocabularyTerm(int index) {
		if (index > this.maxCount)
			return String.valueOf(this.maxCount);
		else
			return String.valueOf(index);
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	@Override
	public int getVocabularySize() {
		return this.maxCount + 1;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("maxCount")) 
			return Obj.stringValue(String.valueOf(this.maxCount));
		else if (parameter.equals("tokenExtractor"))
			return (this.tokenExtractor == null) ? null : Obj.stringValue(this.tokenExtractor.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("maxCount")) 
			this.maxCount = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public String getGenericName() {
		return "TokenCount";
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureTokenCount<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		return binaryFeature;
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
		return true;
	}
}
