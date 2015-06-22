package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Arrays;
import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnNGramSentence computes all n-gram spans occurring
 * in the same sentences as a given collection
 * of token spans.
 *
 * Parameters:
 *  n - the number of grams in each returned n-gram
 *  
 *  noSpan - indicates whether n-grams that overlap
 *  with the input token spans should be excluded 
 *  from the output.
 * 
 * @author Bill McDowell
 *
 */
public class FnNGramSentence extends FnNGram {
	private boolean noSpan = false;
	
	public FnNGramSentence() {
		this(null);
	}
	
	public FnNGramSentence(Context<?, ?> context) {
		super(context);
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "noSpan";
	}
	
	@Override
	protected boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams) {
		int s = tokenSpan.getSentenceIndex();
		DocumentNLP document = tokenSpan.getDocument();
		int tokenCount = document.getSentenceTokenCount(s);
		
		for (int i = 0; i < tokenCount - this.n + 1; i++) {
			TokenSpan ngram = new TokenSpan(document, s, i, i + this.n);
			
			if (!this.noSpan 
					|| 
						(!tokenSpan.containsToken(s, i) 
						&& !tokenSpan.containsToken(s, i + this.n - 1)
						&& !ngram.containsToken(s, tokenSpan.getStartTokenIndex())
						&& !ngram.containsToken(s, tokenSpan.getEndTokenIndex() - 1)
							))
				ngrams.add(ngram);
			
		}
		
		return true;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnNGramSentence(context);
	}
	
	@Override
	public String getGenericName() {
		return "NGramSentence";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("noSpan"))
			return Obj.stringValue(String.valueOf(this.noSpan));
		else 
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("noSpan"))
			this.noSpan = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
}
