package edu.cmu.ml.rtw.generic.data.feature;

import java.util.*;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.Dependency;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse.DependencyPath;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.CounterTable;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;

/**
 * FeatureDependencyPath computes paths in dependency parse trees
 * between token spans
 * associated with a datum. For a datum d with source token-span extractor S,
 * and target token span extractor T, the feature computes vector:
 * 
 * <1(p_1 \in P(S(d),T(d)), 1(p_2 \in P(S(d),T(d))), ... , 1(p_n \in P(S(d),T(d)))>
 * 
 * Where P(S(d),T(d)) gives the set of shortest dependency paths between token spans
 * in S(d) and token spans in T(d), and p_i is a dependency path in the vocabulary
 * of possible paths from the full data set containing d.
 *  
 * Parameters:
 *  minFeatureOccurrence - determines the minimum number of times a
 *  path p_i must appear in the full data set for it to have a component in the 
 *  returned vectors.
 * 
 *  useRelationTypes - determines whether the dependency paths corresponding
 *  to components in the returned vector should be typed.
 * 
 *  sourceTokenExtractor - token span extractor used to extract the source token spans
 * 
 *  targetTokenExtractor - token span extractor used to extract the target token spans
 * 
 * @author Jesse Dodge, Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 * 
 */
public class FeatureDependencyPath<D extends Datum<L>, L> extends Feature<D, L> {
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	
	protected int minFeatureOccurrence;
	protected Datum.Tools.TokenSpanExtractor<D, L> sourceTokenExtractor;
	protected Datum.Tools.TokenSpanExtractor<D, L> targetTokenExtractor;
	protected boolean useRelationTypes = true;
	protected boolean assumeTree = false;
	protected String[] parameterNames = {"minFeatureOccurrence", "sourceTokenExtractor", "targetTokenExtractor", "useRelationTypes", "assumeTree"};
	
	public FeatureDependencyPath() {
		
	}
	
	public FeatureDependencyPath(DatumContext<D, L> context) {
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
				if (this.assumeTree) {
					String pathStr = getShortestPathStringAssumeTree(sourceSpan, targetSpan);
					if (pathStr != null)
						paths.add(pathStr);
				} else {
					DependencyPath path = getShortestPath(sourceSpan, targetSpan);
					if (path == null)
						continue;
					paths.add(path.toString(this.useRelationTypes));
				}
			}
		}
		return paths;
	}
	
	private DependencyPath getShortestPath(TokenSpan sourceSpan, TokenSpan targetSpan) {
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
	
	// Note: This is only used if "assumeTree" is true.  There is generally no reason to use this
	// in place of the other shortest path method above.  It was just necessary when replicating
	// the results of the CAEVO temporal ordering system.
	private String getShortestPathStringAssumeTree(TokenSpan sourceSpan, TokenSpan targetSpan) {
		if (sourceSpan.getSentenceIndex() < 0 
				|| targetSpan.getSentenceIndex() < 0 
				|| sourceSpan.getSentenceIndex() != targetSpan.getSentenceIndex())
			return null;
		
		int sentenceIndex = sourceSpan.getSentenceIndex();
		List<Dependency> deps = sourceSpan.getDocument().getDependencyParse(sentenceIndex).toList();
		String shortestPath = null;
		int minDist = Integer.MAX_VALUE;
		for (int i = sourceSpan.getStartTokenIndex(); i < sourceSpan.getEndTokenIndex(); i++){
			for (int j = targetSpan.getStartTokenIndex(); j < targetSpan.getEndTokenIndex(); j++){
				List<String> pathStrs = getPaths(deps, i, j, null);
				for (String pathStr : pathStrs) {
					int count = pathStr.split("->").length + pathStr.split("<-").length;
					if (count < minDist) {
						minDist = count;
						shortestPath = pathStr;
					}
				}
			}
		}

		return shortestPath;
	}
	
	private List<String> getPaths(List<Dependency> deps, int start, int end, Set<Integer> visited) {
		List<String> paths = new ArrayList<String>();
		
		if(start == end) {
			paths.add("");
			return paths;
		}

		if(visited == null) 
			visited = new HashSet<Integer>();
		
		visited.add(start);
	    
		for(Dependency dep : deps) {
			if(dep != null) {
				String type = (this.useRelationTypes) ? dep.getType() : "_";
				if (dep.getGoverningTokenIndex() == start && !visited.contains(dep.getDependentTokenIndex())) {
					List<String> newpaths = getPaths(deps, dep.getDependentTokenIndex(), end, visited);
					for(String newpath : newpaths) {
						paths.add(type + "->" + newpath);
					}
				}
		
				if (dep.getDependentTokenIndex() == start && !visited.contains(dep.getGoverningTokenIndex())) {
					List<String> newpaths = getPaths(deps, dep.getGoverningTokenIndex(), end, visited);
					for(String newpath : newpaths)
						paths.add(type + "<-" + newpath);
				}
			}
		}

	    return paths;
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
		return "DependencyPath";
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
		else if (parameter.equals("useRelationTypes"))
			return Obj.stringValue(String.valueOf(this.useRelationTypes));
		else if (parameter.equals("assumeTree"))
			return Obj.stringValue(String.valueOf(this.assumeTree));
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
		else if (parameter.equals("useRelationTypes"))
			this.useRelationTypes = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("assumeTree"))
			this.assumeTree = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureDependencyPath<D, L>(context);
	}

	@Override
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		FeatureDependencyPath<T, Boolean> binaryFeatureDep = (FeatureDependencyPath<T, Boolean>)binaryFeature;
		
		binaryFeatureDep.vocabulary = this.vocabulary;
		
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
		FeatureDependencyPath<D, L> cloneDep = (FeatureDependencyPath<D, L>)clone;
		cloneDep.vocabulary = this.vocabulary;
		return true;
	}
}
