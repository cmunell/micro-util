package edu.cmu.ml.rtw.generic.data.feature;

import java.util.*;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FeatureConstituencyParseRelation<D extends Datum<L>, L> extends Feature<D, L> {
	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected String[] parameterNames = { "sourceTokenExtractor", "targetTokenExtractor" };
	
	public FeatureConstituencyParseRelation() {
		
	}
	
	public FeatureConstituencyParseRelation(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {	
		return true;
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		TokenSpan[] sourceTokenSpans = this.sourceTokenExtractor.extract(datum);
		TokenSpan[] targetTokenSpans = this.targetTokenExtractor.extract(datum);
		
		ConstituencyParse.Relation relation = null;
		
		for (TokenSpan sourceSpan : sourceTokenSpans) {
			for (TokenSpan targetSpan : targetTokenSpans) {
				if (!sourceSpan.getDocument().getName().equals(targetSpan.getDocument().getName())
						|| sourceSpan.getSentenceIndex() != targetSpan.getSentenceIndex())
					return vector;
				DocumentNLP document = sourceSpan.getDocument();
				ConstituencyParse parse = document.getConstituencyParse(sourceSpan.getSentenceIndex());
				ConstituencyParse.Relation curRelation = parse.getRelation(sourceSpan, targetSpan);

				if (curRelation == ConstituencyParse.Relation.NONE
						|| (relation != null && relation != curRelation))
					return vector;
				relation = curRelation;
			}
		}
	
		if (relation == ConstituencyParse.Relation.DOMINATING)
			vector.put(offset, 1.0);
		else if (relation == ConstituencyParse.Relation.DOMINATED)
			vector.put(offset + 1, 1.0);
		
		return vector;
	}


	@Override
	public String getGenericName() {
		return "ConstituencyParseRelation";
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		if (index == 0) {
			return ConstituencyParse.Relation.DOMINATING.toString();
		} else if (index == 1) {
			return ConstituencyParse.Relation.DOMINATED.toString();
		} else {
			return null;
		}
	}

	@Override
	protected boolean setVocabularyTerm(int index, String term) {
		return true;
	}

	@Override
	public int getVocabularySize() {
		return 2;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("sourceTokenExtractor"))
			return Obj.stringValue((this.sourceTokenExtractor == null) ? "" : this.sourceTokenExtractor.toString());
		else if (parameter.equals("targetTokenExtractor"))
			return Obj.stringValue((this.targetTokenExtractor == null) ? "" : this.targetTokenExtractor.toString());
		return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("sourceTokenExtractor"))
			this.sourceTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("targetTokenExtractor"))
			this.targetTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureConstituencyParseRelation<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureConstituencyParseRelation<T, Boolean> binaryFeatureConst = (FeatureConstituencyParseRelation<T, Boolean>)binaryFeature;
		
		return binaryFeatureConst;
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
