package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Arrays;
import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnNGramDocument extends FnNGram {
	private boolean noSentence = false;
	
	public FnNGramDocument() {
		this(null);
	}
	
	public FnNGramDocument(Context<?, ?> context) {
		super(context);
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "noSentence";
	}
	
	@Override
	protected boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams) {
		DocumentNLP document = tokenSpan.getDocument();
		int sentenceCount = document.getSentenceCount();
		
		for (int i = 0; i < sentenceCount; i++) {
			if (!this.noSentence || i != tokenSpan.getSentenceIndex()) {
				int tokenCount = document.getSentenceTokenCount(i);
				for (int j = 0; j < tokenCount - this.n + 1; j++) {
					ngrams.add(new TokenSpan(document, i, j, j + this.n));
				}
			}
		}
		
		return true;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnNGramDocument(context);
	}
	
	@Override
	public String getGenericName() {
		return "NGramDocument";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("noSentence"))
			return Obj.stringValue(String.valueOf(this.noSentence));
		else 
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("noSentence"))
			this.noSentence = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
}
