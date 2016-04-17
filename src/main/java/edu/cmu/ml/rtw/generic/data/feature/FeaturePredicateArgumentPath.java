package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Predicate;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

public class FeaturePredicateArgumentPath<D extends Datum<L>, L> extends Feature<D, L> {
	private static final TokenSpan.Relation[] SPAN_RELATIONS = new TokenSpan.Relation[] { TokenSpan.Relation.CONTAINED_BY, TokenSpan.Relation.CONTAINS };
	
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected int minFeatureOccurrence;
	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected String[] parameterNames = {"minFeatureOccurrence", "sourceTokenExtractor", "targetTokenExtractor" };
	
	public FeaturePredicateArgumentPath() {
		
	}
	
	public FeaturePredicateArgumentPath(DatumContext<D, L> context) {
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.context = context;
	}
	
	@Override
	public boolean init(DataSet<D, L> dataSet) {
		final CounterTable<String> counter = new CounterTable<String>();
		dataSet.map(new ThreadMapper.Fn<D, Boolean>() {
			@Override
			public Boolean apply(D datum) {
				Set<String> paths = getPathsForDatum(datum);
				for (String path : paths) {
					counter.incrementCount(path);
				}
				return true;
			}
		}, this.context.getMaxThreads());
		
		counter.removeCountsLessThan(this.minFeatureOccurrence);
		this.vocabulary = new BidirectionalLookupTable<String, Integer>(counter.buildIndex());
		return true;
	}
	
	private Set<String> getPathsForDatum(D datum){
		Set<String> paths = new HashSet<String>();
		
		TokenSpan[] sourceTokenSpans = this.sourceTokenExtractor.extract(datum);
		TokenSpan[] targetTokenSpans = this.targetTokenExtractor.extract(datum);
		
		for (TokenSpan sourceSpan : sourceTokenSpans) {
			for (TokenSpan targetSpan : targetTokenSpans){
				Set<String> spanPaths = getPaths(sourceSpan, targetSpan);
				if (spanPaths == null)
					continue;
				paths.addAll(spanPaths);
			}
		}
		return paths;
	}
	
	private Set<String> getPaths(TokenSpan sourceSpan, TokenSpan targetSpan){
		if (sourceSpan.getSentenceIndex() < 0 
				|| targetSpan.getSentenceIndex() < 0 
				|| sourceSpan.getSentenceIndex() != targetSpan.getSentenceIndex())
			return null;
		
		Set<String> paths = new HashSet<String>();
		List<Pair<TokenSpan, Predicate>> preds = sourceSpan.getDocument().getTokenSpanAnnotations(AnnotationTypeNLP.PREDICATE);
		List<TokenSpan> visited = new ArrayList<>();
		Stack<Pair<String, TokenSpan>> toVisit = new Stack<>();
		toVisit.push(new Pair<String, TokenSpan>("", sourceSpan));
		while (!toVisit.isEmpty()) {
			Pair<String, TokenSpan> cur = toVisit.pop();
			TokenSpan curSpan = cur.getSecond();
			visited.add(curSpan);
			
			List<Pair<TokenSpan, String>> neighbors = getRelations(curSpan, preds);
			for (Pair<TokenSpan, String> neighbor : neighbors) {
				TokenSpan neighborSpan = neighbor.getFirst();
				if (visitedHasSpan(neighborSpan, visited))
					continue;
				
				String path = cur.getFirst() + "_" + neighbor.getSecond();
				if (neighborSpan.hasRelationTo(targetSpan, SPAN_RELATIONS)) {
					paths.add(path);
				} else {
					toVisit.push(new Pair<String, TokenSpan>(path, neighborSpan));
				}
				
			}
		}
		
		return paths;
	}
	
	private boolean visitedHasSpan(TokenSpan span, List<TokenSpan> visited) {
		for (TokenSpan v : visited)
			if (span.hasRelationTo(v, SPAN_RELATIONS))
				return true;
		return false;
	}
	
	private List<Pair<TokenSpan, String>> getRelations(TokenSpan span, List<Pair<TokenSpan, Predicate>> preds) {
		List<Pair<TokenSpan, String>> relations = new ArrayList<>();
		
		for (Pair<TokenSpan, Predicate> predPair : preds) {
			TokenSpan predSpan = predPair.getFirst();
			Predicate pred = predPair.getSecond();
			Set<String> argTags = pred.getArgumentTags();
			if (span.hasRelationTo(predSpan, SPAN_RELATIONS)) {
				for (String argTag : argTags) {
					TokenSpan[] argSpans = pred.getArgument(argTag);
					for (TokenSpan argSpan : argSpans)
						relations.add(new Pair<TokenSpan, String>(argSpan, pred.getSense() + "_" + argTag));
				}
			}
			
			for (String argTag : argTags) {
				TokenSpan[] argSpans = pred.getArgument(argTag);
				for (TokenSpan argSpan : argSpans) {
					if (span.hasRelationTo(argSpan, SPAN_RELATIONS))
						relations.add(new Pair<TokenSpan, String>(predSpan, argTag + "_" + pred.getSense()));
				}
			}
		}
		
		return relations;
	}
	
	
	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		Set<String> pathsForDatum = getPathsForDatum(datum);
		
		for (String path : pathsForDatum) {
			if (this.vocabulary.containsKey(path))
				vector.put(this.vocabulary.get(path) + offset, 1.0);		
		}

		return vector;
	}


	@Override
	public String getGenericName() {
		return "PredicateArgumentPath";
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
		else if (parameter.equals("sourceTokenExtractor"))
			return Obj.stringValue((this.sourceTokenExtractor == null) ? "" : this.sourceTokenExtractor.toString());
		else if (parameter.equals("targetTokenExtractor"))
			return Obj.stringValue((this.targetTokenExtractor == null) ? "" : this.targetTokenExtractor.toString());
		return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("minFeatureOccurrence")) 
			this.minFeatureOccurrence = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("sourceTokenExtractor"))
			this.sourceTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("targetTokenExtractor"))
			this.targetTokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeaturePredicateArgumentPath<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeaturePredicateArgumentPath<T, Boolean> binaryFeaturePred = (FeaturePredicateArgumentPath<T, Boolean>)binaryFeature;
		
		binaryFeaturePred.vocabulary = this.vocabulary;
		
		return binaryFeaturePred;
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
		FeaturePredicateArgumentPath<D, L> cloneDep = (FeaturePredicateArgumentPath<D, L>)clone;
		cloneDep.vocabulary = this.vocabulary;
		return true;
	}

}
