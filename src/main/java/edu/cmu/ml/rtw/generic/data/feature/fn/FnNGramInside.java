package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Arrays;
import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnNGramInside extends FnNGram {
	private boolean noHead = false;
	
	public FnNGramInside() {
		this(null);
	}
	
	public FnNGramInside(Context<?, ?> context) {
		super(context);
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "noHead";
	}
	
	@Override
	protected boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams) {
		int iteratorBound = (this.noHead) ? tokenSpan.getLength() - this.n : tokenSpan.getLength() - this.n + 1; 
		
		for (int i = 0; i < iteratorBound; i++) {
			ngrams.add(tokenSpan.getSubspan(i, i + this.n));
		}
		
		return true;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnNGramInside(context);
	}
	
	@Override
	public String getGenericName() {
		return "NGramInside";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("noHead"))
			return Obj.stringValue(String.valueOf(this.noHead));
		else 
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("noHead"))
			this.noHead = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
}
