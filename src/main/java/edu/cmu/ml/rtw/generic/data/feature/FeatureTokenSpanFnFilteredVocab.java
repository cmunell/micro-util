package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;

/**
 * FeatureTokenSpanFnFilteredVocab computes a vector
 * of indicators of whether elements of a vocabulary of
 * strings are associated with a datum.  The vocabulary of
 * strings is computed as a transformed subset of the vocabulary
 * of a separate FeatureTokenSpanFnDataVocabTrie feature.
 * 
 * This feature is only currently used by 
 * edu.cmu.ml.rtw.generic.model.SupervisedModelLogistmarGramression
 * to construct new features from its current feature set.  New 
 * features are constructed as transformations on precomputed 
 * FeatureTokenSpanFnDataVocabTrie vocabularies to save time.  This
 * way of doing things makes it possible to quickly construct thousands
 * of new features with small vocabularies from large precomputed
 * vocabularies.
 * 
 * Parameters:
 *  vocabFeature - the FeatureTokenSpanFnDataVocabTrie from which
 *  to compute the vocabulary
 *
 *  vocabFilterInit - determines whether the full source vocabulary
 *  will be given to vocabFilterFn, or only subsets of the source
 *  vocabulary that are prefixed/suffixed by vocabFilterInitArg 
 *  (this is done separately outside of vocabFilterFn to cleanly use
 *  the tries in FeatureTokenSpanFnDataVocabTrie for the sake
 *  of efficiency)
 *  
 *  vocabFilterInitArg - a string argument to use with vocabFilterInit
 *  
 *  vocabFilterFn - a function that transforms the strings from the
 *  source vocabulary into strings of the target vocabulary 
 *  
 *  tokenExtractor - extracts token spans from input datums for 
 *  fn to operate on
 *  
 *  fn - transforms token spans extracted by tokenExtractor into strings.
 *  The output vector indicates which elements of the vocabulary are in the
 *  collection of strings output by fn.
 * 
 * FIXME: This class will break featurized data set 
 * if it has a non-contiguous vocabulary
 * but that's okay for now since it's not ever used with a 
 * featurized data set (non-contiguous vocabularies are
 * constructed when several
 * FeatureTokenSpanFnFilteredVocabs are merged.  The merging
 * is done in order to improve efficiency when many (thousands)
 * of new features with single element vocabularies are 
 * constructed (without the merging, many separate hash set
 * containment operations have to be performed for each individual
 * feature vocabulary).
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class FeatureTokenSpanFnFilteredVocab<D extends Datum<L>, L> extends Feature<D, L> {
	protected BidirectionalLookupTable<String, Integer> vocabulary;
	private boolean merged = false;
	private TreeSet<Integer> indexRangeStarts; // Range inclusive
	private TreeSet<Integer> indexRangeEnds; // Range exclusive
	
	private Fn.CacheMode fnCacheMode = Fn.CacheMode.OFF;
	
	public enum VocabFilterInit {
		SUFFIX,
		PREFIX,
		SUFFIX_OR_PREFIX,
		FULL
	}
	
	protected FeatureTokenSpanFnDataVocabTrie<D, L> vocabFeature;
	protected Fn<String, String> vocabFilterFn;
	protected VocabFilterInit vocabFilterInit;
	protected String vocabFilterInitArg;
	protected Fn<TokenSpan, String> fn;
	protected Datum.Tools.TokenSpanExtractor<D, L> tokenExtractor;
	protected String[] parameterNames = {"vocabFeature", "vocabFilterFn", "vocabFilterInit", "vocabFilterInitArg", "fn", "tokenExtractor"};
	
	public FeatureTokenSpanFnFilteredVocab() {
		
	}
	
	public FeatureTokenSpanFnFilteredVocab(FeatureTokenSpanFnFilteredVocab<D, L> feature, Set<Integer> exceptIndices) {
		if (feature.indexRangeStarts.size() > 1 || !feature.indexRangeStarts.contains(0))
			throw new IllegalArgumentException();
		
		this.vocabFeature = feature.vocabFeature;
		this.vocabFilterFn = feature.vocabFilterFn;
		this.vocabFilterInit = feature.vocabFilterInit;
		this.vocabFilterInitArg = feature.vocabFilterInitArg;
		this.fn = feature.fn;
		this.tokenExtractor = feature.tokenExtractor;
		this.modifiers = feature.modifiers;
		this.referenceName = feature.referenceName;
		this.context = feature.context;
		this.fnCacheMode = feature.fnCacheMode;
		
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.indexRangeStarts = new TreeSet<Integer>();
		this.indexRangeEnds = new TreeSet<Integer>();
	
		for (Integer rangeStart : feature.indexRangeStarts)
			this.indexRangeStarts.add(rangeStart);
		for (Integer rangeEnd : feature.indexRangeEnds)
			this.indexRangeEnds.add(rangeEnd);
			
		int newIndex = 0;
		for (int i = 0; i < feature.getVocabularySize(); i++) {
			if (!exceptIndices.contains(i)) {
				this.vocabulary.put(feature.getVocabularyTerm(i), newIndex);
				newIndex++;
			}
		}
		
	}
	
	public FeatureTokenSpanFnFilteredVocab(Context<D, L> context) {
		this.context = context;
		this.vocabulary = new BidirectionalLookupTable<String, Integer>();
		this.indexRangeStarts = new TreeSet<Integer>();
		this.indexRangeEnds = new TreeSet<Integer>();
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("vocabFeature"))
			return Obj.curlyBracedValue(this.vocabFeature.getReferenceName());
		else if (parameter.equals("vocabFilterFn"))
			return this.vocabFilterFn.toParse();
		else if (parameter.equals("vocabFilterInit"))
			return Obj.stringValue(this.vocabFilterInit.toString());
		else if (parameter.equals("vocabFilterInitArg"))
			return (this.vocabFilterInitArg == null) ? Obj.stringValue("") : Obj.stringValue(this.vocabFilterInitArg);
		else if (parameter.equals("fn"))
			return this.fn.toParse();
		else if (parameter.equals("tokenExtractor"))
			return Obj.stringValue(this.tokenExtractor.toString());
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("vocabFeature"))
			this.vocabFeature = (FeatureTokenSpanFnDataVocabTrie<D, L>)this.context.getMatchFeature(parameterValue);
		else if (parameter.equals("vocabFilterFn"))
			this.vocabFilterFn = this.context.getMatchOrConstructStrFn(parameterValue);
		else if (parameter.equals("vocabFilterInit"))
			this.vocabFilterInit = VocabFilterInit.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("vocabFilterInitArg"))
			this.vocabFilterInitArg = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("fn"))
			this.fn = this.context.getMatchOrConstructTokenSpanStrFn(parameterValue);
		else if (parameter.equals("tokenExtractor"))
			this.tokenExtractor = this.context.getDatumTools().getTokenSpanExtractor(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public boolean init(FeaturizedDataSet<D, L> dataSet) {
		Set<String> initVocab = null;
		if (this.vocabFilterInit == VocabFilterInit.SUFFIX) {
			initVocab = this.vocabFeature.getVocabularyTermsSuffixedBy(this.vocabFilterInitArg);
		} else if (this.vocabFilterInit == VocabFilterInit.PREFIX) {
			initVocab = this.vocabFeature.getVocabularyTermsPrefixedBy(this.vocabFilterInitArg);
		} else if (this.vocabFilterInit == VocabFilterInit.SUFFIX_OR_PREFIX) {
			initVocab = this.vocabFeature.getVocabularyTermsSuffixedBy(this.vocabFilterInitArg);
			initVocab.addAll(this.vocabFeature.getVocabularyTermsPrefixedBy(this.vocabFilterInitArg));
		} else { // FULL
			initVocab = this.vocabFeature.vocabulary.keySet();
		}
		
		Set<String> vocab = this.vocabFilterFn.setCompute(initVocab);
		
		int i = 0;
		for (String term : vocab) {
			this.vocabulary.put(term, i);
			i++;
		}
		
		this.indexRangeStarts.add(0);
		this.indexRangeEnds.add(this.vocabulary.size());
		
		return true;
	}

	@Override
	public Map<Integer, Double> computeVector(D datum, int offset, Map<Integer, Double> vector) {
		return computeVector(datum, offset, this.indexRangeStarts.first(), this.indexRangeEnds.last(), vector);
	}
	
	public Map<Integer, Double> computeVector(D datum, int offset, int minIndex, int maxIndex, Map<Integer, Double> vector) {
		// This ends the range immediately before the target (minIndex, maxIndex) range
		Integer endPointBeforeMin = this.indexRangeEnds.floor(minIndex); 
		if (endPointBeforeMin == null)
			endPointBeforeMin = 0;
		
		// This is the start point of the first range that can overlap with the target range.
		// If it does not overlap, then neither does any other range, and so no computation 
		// needs to be done
		Integer startPointAfterEndPointBeforeMin = this.indexRangeStarts.ceiling(endPointBeforeMin);
		if (startPointAfterEndPointBeforeMin == null || startPointAfterEndPointBeforeMin >= maxIndex)
			return vector;
		
		List<TokenSpan> spans = Arrays.asList(this.tokenExtractor.extract(datum)); 
		
		Set<String> strs = this.fn.setCompute(spans, 
											(this.fnCacheMode == Fn.CacheMode.ON) ? this.tokenExtractor.toString() + datum.getId() : "0", 
											this.fnCacheMode);
		
		if (strs.size() < this.vocabulary.size()) {
			for (String str : strs) {
				if (this.vocabulary.containsKey(str)) {
					int index = this.vocabulary.get(str);
					if (index >= minIndex && index < maxIndex)
						vector.put(index + offset, 1.0);
				}
			}
		} else {
			for (String str : this.vocabulary.keySet()) {
				if (strs.contains(str)) {
					int index = this.vocabulary.get(str);
					if (index >= minIndex && index < maxIndex)
						vector.put(index + offset, 1.0);
				}
			}
		}
		
		return vector;
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
	protected <T extends Datum<Boolean>> Feature<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			Feature<T, Boolean> binaryFeature) {
		
		FeatureTokenSpanFnFilteredVocab<T, Boolean> binaryFeatureTokenSpanFnFilteredVocab = (FeatureTokenSpanFnFilteredVocab<T, Boolean>)binaryFeature;
		
		binaryFeatureTokenSpanFnFilteredVocab.vocabulary = this.vocabulary;
		binaryFeatureTokenSpanFnFilteredVocab.indexRangeEnds = this.indexRangeEnds;
		binaryFeatureTokenSpanFnFilteredVocab.indexRangeStarts = this.indexRangeStarts;
		binaryFeatureTokenSpanFnFilteredVocab.merged = this.merged;
		
		return binaryFeatureTokenSpanFnFilteredVocab;
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;
		
		if (internalAssignments.contains("vocabulary")) {
			if (!internalAssignments.contains("indices"))
				return false;
			
			Obj.Array vocabulary = (Obj.Array)internalAssignments.get("vocabulary").getValue();
			Obj.Array indices = (Obj.Array)internalAssignments.get("indices").getValue();
			
			for (int i = 0; i < vocabulary.size(); i++) {
				if (!setVocabularyTerm(Integer.valueOf(indices.get(i).getStr()), vocabulary.getStr(i)))
					return false;
			}
		}
		
		return fromParseInternalHelper(internalAssignments);
	}
	
	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList internalAssignments = new AssignmentList();
		
		Obj.Array vocabulary = new Obj.Array();
		Obj.Array indices = new Obj.Array();
		Set<Integer> indexSet = this.vocabulary.reverseKeySet();
		
		for (Integer index : indexSet) {
			vocabulary.add(Obj.stringValue(this.vocabulary.reverseGet(index)));
			indices.add(Obj.stringValue(index.toString()));
		}
		
		if (vocabulary.size() > 0) {
			internalAssignments.add(
					Assignment.assignmentTyped(new ArrayList<String>(), Context.ARRAY_STR, "vocabulary", vocabulary)
			);
			
			internalAssignments.add(
					Assignment.assignmentTyped(new ArrayList<String>(), Context.ARRAY_STR, "indices", indices)
			);
		}
		
		internalAssignments = toParseInternalHelper(internalAssignments);
		
		return (internalAssignments.size() == 0) ? null : internalAssignments;
	}
	
	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;
		if (internalAssignments.contains("merged"))
			this.merged = Boolean.valueOf(((Obj.Value)internalAssignments.get("merged").getValue()).getStr());
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return null;
		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "merged", Obj.stringValue(String.valueOf(this.merged))));
		
		return internalAssignments;
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureTokenSpanFnFilteredVocab<D, L>(context);
	}

	@Override
	public String getGenericName() {
		return "TokenSpanFnFilteredVocab";
	}
	
	@Override
	protected boolean cloneHelper(Feature<D, L> clone) {
		FeatureTokenSpanFnFilteredVocab<D, L> cloneFilt = (FeatureTokenSpanFnFilteredVocab<D, L>)clone;
		cloneFilt.vocabulary = this.vocabulary;
		cloneFilt.indexRangeEnds = this.indexRangeEnds;
		cloneFilt.indexRangeStarts = this.indexRangeStarts;
		cloneFilt.merged = this.merged;
		
		return true;
	}

	public void clearFnCaches() {
		this.fn.clearCaches();
	}
	
	public void setFnCacheMode(Fn.CacheMode fnCacheMode) {
		this.fnCacheMode = fnCacheMode;
	}
	
	public Fn<TokenSpan, String> getFn() {
		return this.fn;
	}
	
	public boolean merge(FeatureTokenSpanFnFilteredVocab<D, L> feature, int offset) {
		if (offset < this.indexRangeEnds.last())
			throw new IllegalArgumentException();
		
		if (!this.getReferenceName().equals(feature.getReferenceName())
				|| !this.fn.equals(feature.fn))
			return false;

		int i = 0;
		Set<String> featureVocabulary = feature.vocabulary.keySet();
		for (String term : featureVocabulary) {
			if (this.vocabulary.containsKey(term))
				throw new IllegalArgumentException();
			
			this.vocabulary.put(term, offset + i);
			
			i++;
		}
		
		if (this.indexRangeEnds.last() != offset) {
			this.indexRangeStarts.add(offset);
			this.indexRangeEnds.add(offset + i);
		} else {
			this.indexRangeEnds.remove(offset);
			this.indexRangeEnds.add(offset + i);
		}
		
		this.merged = true;
		return true;
	}
	
	public Map<Integer, String> getSpecificShortNamesForIndices(Iterable<Integer> indices, int offset, Map<Integer, String> specificShortNames) {
		String prefix = getReferenceName() + "_";
		
		for (Integer index : indices) {
			String vocabularyTerm = getVocabularyTerm(index);
			if (vocabularyTerm != null)
				specificShortNames.put(index + offset, prefix + getVocabularyTerm(index));
			else 
				specificShortNames.put(index + offset, null);
		}
		
		return specificShortNames;
	}
	
	public Map<Integer, String> getVocabularyForIndices(Iterable<Integer> indices) {
		Map<Integer, String> vocabulary = new HashMap<Integer, String>();
		for (Integer index : indices) {
			vocabulary.put(index, getVocabularyTerm(index));
		}
		
		return vocabulary;
	}
	
	public List<String> getSpecificShortNames(List<String> specificShortNames) {
		if (this.merged)
			throw new UnsupportedOperationException();
	
		return super.getSpecificShortNames(specificShortNames);
	}
	
	public boolean vocabularyContainsIndex(int index) {
		Integer indexRangeStart = this.indexRangeStarts.floor(index);
		if (indexRangeStart == null)
			return false;
		
		Integer indexRangeEnd = this.indexRangeEnds.ceiling(indexRangeStart + 1);
		if (indexRangeEnd == null || indexRangeEnd <= index)
			return false;
		
		return true;
	}
}
