package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Arrays;
import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnNGramContext extends FnNGram {
	public enum Type {
		BEFORE,
		AFTER
	}
	
	private Type type = Type.BEFORE;
	
	public FnNGramContext() {
		this(null);
	}
	
	public FnNGramContext(Context<?, ?> context) {
		super(context);
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "type";
	}
	
	@Override
	protected boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams) {
		if (this.type == Type.BEFORE && tokenSpan.getStartTokenIndex() - this.n >= 0) {
			ngrams.add(new TokenSpan(tokenSpan.getDocument(), 
									 tokenSpan.getSentenceIndex(), 
									 tokenSpan.getStartTokenIndex() - this.n, 
									 tokenSpan.getStartTokenIndex()));
		} else if (this.type == Type.AFTER && tokenSpan.getEndTokenIndex() + this.n <= tokenSpan.getDocument().getSentenceTokenCount(tokenSpan.getSentenceIndex())) {
			ngrams.add(new TokenSpan(tokenSpan.getDocument(), 
					 				 tokenSpan.getSentenceIndex(), 
					 				 tokenSpan.getEndTokenIndex(), 
					 				 tokenSpan.getEndTokenIndex() + this.n));
		}
		
		return true;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnNGramContext(context);
	}
	
	@Override
	public String getGenericName() {
		return "NGramContext";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("type"))
			return Obj.stringValue(String.valueOf(this.type));
		else 
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
}
