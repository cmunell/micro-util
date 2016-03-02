package edu.cmu.ml.rtw.generic.data.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * For each datum d FeatureNGramContext computes a
 * vector:
 * 
 * <c(v_1\in C(T(d),k)), c(v_2 \in C(T(d),k)), ... , c(v_n \in C(T(d),k))>
 * 
 * Where T is a token span extractor, C(T(d), k) computes all n-grams in a 
 * context of window-size k surrounding the tokens given by T(d),
 * and c(v \in S) computes the number of occurrences of n-gram v in S.  The resulting
 * vector is given to methods in edu.cmu.ml.rtw.generic.data.feature.FeatureNGram to 
 * be normalized and scaled in some way.
 * 
 * Parameters:
 *  n - number of grams per n-gram 
 *
 * 	minFeatureOccurrence - minimum number of times an n-gram 
 *  must occur across the data set for it to have a component 
 *  in the computed vectors
 * 
 * 	cleanFn - string cleaning function that grams are passed through
 * 
 *  tokenExtractor - extractor for token spans from data
 *	 
 *  scale - scaling method for components of the vector
 * 
 *  clusterer - optional clusterer that maps grams to their clusters
 *  before combining them into n-(clustered)grams 
 * 
 *  maxGramDistance - (k) the number of tokens away from the token spans
 *  extracted by tokenExtractor from which to take n-grams
 *  
 *  mode - determined whether the context is extracted from before each
 *  token span, after each token span, or within each token span
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class FeatureNGramContext<D extends Datum<L>, L> extends FeatureNGram<D, L> {
	public enum Mode {
		BEFORE,
		AFTER,
		WITHIN
	}
	
	private int maxGramDistance;
	private Mode mode;
	
	public FeatureNGramContext() {
		
	}
	
	public FeatureNGramContext(DatumContext<D, L> context) {
		super(context);
		
		this.maxGramDistance = 0;
		this.mode = Mode.WITHIN;
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 2);
		this.parameterNames[this.parameterNames.length - 1] = "maxGramDistance";
		this.parameterNames[this.parameterNames.length - 2] = "mode";
	}
	
	@Override
	protected Map<String, Integer> getGramsForDatum(D datum) {
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		Map<String, Integer> retNgrams = new HashMap<String, Integer>();
		
		for (TokenSpan tokenSpan : tokenSpans) {
			if (tokenSpan.getSentenceIndex() < 0)
				continue;
			
			DocumentNLP document = tokenSpan.getDocument();
			int startIndex = 0, endIndex = 0;
			if (this.mode == Mode.BEFORE) {
				startIndex = Math.max(0, tokenSpan.getStartTokenIndex() - this.maxGramDistance);
				endIndex = tokenSpan.getStartTokenIndex();
			} else if (this.mode == Mode.AFTER) {
				startIndex = Math.min(document.getSentenceTokenCount(tokenSpan.getSentenceIndex()), tokenSpan.getEndTokenIndex());
				endIndex =  Math.min(document.getSentenceTokenCount(tokenSpan.getSentenceIndex()), tokenSpan.getEndTokenIndex() + this.maxGramDistance);
			} else { // WITHIN
				startIndex = tokenSpan.getStartTokenIndex();
				endIndex = tokenSpan.getEndTokenIndex();
			}
			
			List<String> ngrams = getNGramsInWindow(document, tokenSpan.getSentenceIndex(), startIndex, endIndex);
			for (String ngram : ngrams) {
				if (!retNgrams.containsKey(ngram))
					retNgrams.put(ngram, 1);
				else
					retNgrams.put(ngram, retNgrams.get(ngram) + 1);
			}
		}
			
		return retNgrams;
	}
	
	// All n-grams in window between startTokenIndex (inclusive) and endTokenIndex (exclusive) (whole n-gram must fit in window)
	private List<String> getNGramsInWindow(DocumentNLP document, int sentenceIndex, int startTokenIndex, int endTokenIndex) {
		List<String> ngrams = new ArrayList<String>();
		for (int i = startTokenIndex; i < endTokenIndex - this.n + 1; i++) {		
			List<String> ngramsAtPosition = getCleanNGramsAtPosition(document, sentenceIndex, i);
			if (ngrams != null) {
				ngrams.addAll(ngramsAtPosition);
			}
		}
		return ngrams;
	}

	@Override
	public String getGenericName() {
		return "NGramContext";
	}

	@Override
	public Feature<D, L> makeInstance(DatumContext<D, L> context) {
		return new FeatureNGramContext<D, L>(context);
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		Obj parameterValue = super.getParameterValue(parameter);
		if (parameterValue != null)
			return parameterValue;
		else if (parameter.equals("maxGramDistance"))
			return Obj.stringValue(String.valueOf(this.maxGramDistance));
		else if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (super.setParameterValue(parameter, parameterValue))
			return true;
		else if (parameter.equals("maxGramDistance"))
			this.maxGramDistance = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}
}
