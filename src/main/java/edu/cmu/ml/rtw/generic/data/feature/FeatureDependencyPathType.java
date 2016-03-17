package edu.cmu.ml.rtw.generic.data.feature;

import java.util.*;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.DependencyPath;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FeatureDependencyPathType<D extends Datum<L>, L> extends Feature<D, L> {
	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected String[] parameterNames = { "sourceTokenExtractor", "targetTokenExtractor" };
	
	public FeatureDependencyPathType() {
		
	}
	
	public FeatureDependencyPathType(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {	
		return true;
	}
	
	private DependencyPath getShortestPath(TokenSpan sourceSpan, TokenSpan targetSpan){
		if (sourceSpan.getSentenceIndex() < 0 
				|| targetSpan.getSentenceIndex() < 0 
				|| sourceSpan.getSentenceIndex() != targetSpan.getSentenceIndex())
			return null;
		
		DependencyPath shortestPath = null;
		int sentenceIndex = sourceSpan.getSentenceIndex();
		DependencyParse parse = sourceSpan.getDocument().getDependencyParse(sentenceIndex);
		for (int i = sourceSpan.getStartTokenIndex(); i < sourceSpan.getEndTokenIndex(); i++){
			for (int j = targetSpan.getStartTokenIndex(); j < targetSpan.getEndTokenIndex(); j++){
				DependencyPath path = parse.getPath(i, j);
				if (shortestPath == null || (path != null && path.getTokenLength() < shortestPath.getTokenLength()))
					shortestPath = path;
			}
		}

		return shortestPath;
	}
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		TokenSpan[] sourceTokenSpans = this.sourceTokenExtractor.extract(datum);
		TokenSpan[] targetTokenSpans = this.targetTokenExtractor.extract(datum);
		
		DependencyParse.PathType pathType = null;
		
		for (TokenSpan sourceSpan : sourceTokenSpans) {
			for (TokenSpan targetSpan : targetTokenSpans){
				DependencyPath path = getShortestPath(sourceSpan, targetSpan);
				if (path == null)
					return vector;
				DependencyParse.PathType currentType = path.getType();
				if (currentType == DependencyParse.PathType.NONE
						|| (pathType != null && pathType != currentType))
					return vector;
				pathType = currentType;
			}
		}
	
		if (pathType == DependencyParse.PathType.GOVERNING)
			vector.put(offset, 1.0);
		else if (pathType == DependencyParse.PathType.GOVERNED_BY)
			vector.put(offset + 1, 1.0);
		
		return vector;
	}


	@Override
	public String getGenericName() {
		return "DependencyPathType";
	}
	
	@Override
	public String getVocabularyTerm(int index) {
		if (index == 0) {
			return DependencyParse.PathType.GOVERNING.toString();
		} else if (index == 1) {
			return DependencyParse.PathType.GOVERNED_BY.toString();
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
		return new FeatureDependencyPathType<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureDependencyPathType<T, Boolean> binaryFeatureDep = (FeatureDependencyPathType<T, Boolean>)binaryFeature;
		
		return binaryFeatureDep;
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
