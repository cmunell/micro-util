package edu.cmu.ml.rtw.generic.data.feature;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * For each datum d FeatureNGramSentence computes a
 * vector:
 * 
 * <c(v_1\in S(T(d))), c(v_2 \in S(T(d))), ... , c(v_n \in S(T(d)))>
 * 
 * Where T is a token extractor, S(T(d)) computes the n-grams 
 * in the sentence surrounding the tokens given by T(d) in a source text document,
 * and c(v \in S(T(d))) computes the number of occurrences of n-gram v in S.  
 * The resulting
 * vector is given to methods in edu.cmu.ml.rtw.generic.data.feature.FeatureNGram to be normalized
 * and scaled in some way.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 * 
 */
public class FeatureNGramSentence<D extends Datum<L>, L> extends FeatureNGram<D, L> {
	private boolean noTokenSpan;
	
	public FeatureNGramSentence() {
		
	}
	
	public FeatureNGramSentence(Context<D, L> context) {
		super(context);
		
		this.noTokenSpan = false;
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "noTokenSpan";
	}
	
	@Override
	protected Map<String, Integer> getGramsForDatum(D datum) {
		TokenSpan[] tokenSpans = this.tokenExtractor.extract(datum);
		Map<String, Integer> retNgrams = new HashMap<String, Integer>();
		
		for (TokenSpan tokenSpan : tokenSpans) {
			if (tokenSpan.getSentenceIndex() < 0)
				continue;
			
			DocumentNLP document = tokenSpan.getDocument();
			int sentenceIndex = tokenSpan.getSentenceIndex();
			for (int i = 0; i < document.getSentenceTokenCount(sentenceIndex)-this.n+1; i++) {
				if (this.noTokenSpan && (
						(tokenSpan.containsToken(sentenceIndex, i))
						|| (tokenSpan.containsToken(sentenceIndex, i + this.n - 1))
						|| (i < tokenSpan.getStartTokenIndex() && i + this.n - 1 >= tokenSpan.getEndTokenIndex())
						))
					continue;
				
				List<String> ngrams = getCleanNGramsAtPosition(document, sentenceIndex, i);
				if (ngrams != null) {
					for (String ngram : ngrams) {
						if (this.n == 1) {
							// FIXME: This further splits unigrams if 
							// the cleaning process inserts spaces into
							// them.  It's currently a hack for the
							// text classification project, but it should
							// be done differently. It shouldn't affect
							// anything if the clean function never inserts
							// spaces
							String[] ngramParts = ngram.split("\\s+");
							for (String ngramPart : ngramParts) {
								if (!retNgrams.containsKey(ngramPart))
									retNgrams.put(ngramPart, 1);
								else
									retNgrams.put(ngramPart, retNgrams.get(ngramPart) + 1);
							}
						} else {
							if (!retNgrams.containsKey(ngram))
								retNgrams.put(ngram, 1);
							else
								retNgrams.put(ngram, retNgrams.get(ngram) + 1);
						}
					}
				}
			}
		}
		return retNgrams;
	}

	@Override
	public String getGenericName() {
		return "NGramSentence";
	}

	@Override
	public Feature<D, L> makeInstance(Context<D, L> context) {
		return new FeatureNGramSentence<D, L>(context);
	} 
	
	@Override
	public Obj getParameterValue(String parameter) {
		Obj parameterValue = super.getParameterValue(parameter);
		if (parameterValue != null)
			return parameterValue;
		else if (parameter.equals("noTokenSpan"))
			return Obj.stringValue(String.valueOf(this.noTokenSpan));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (super.setParameterValue(parameter, parameterValue))
			return true;
		else if (parameter.equals("noTokenSpan"))
			this.noTokenSpan = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}

}
