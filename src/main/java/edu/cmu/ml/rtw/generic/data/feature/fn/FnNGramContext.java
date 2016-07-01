package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Arrays;
import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnNGramContext takes a collection of token spans
 * and computes the n-gram spans that occur either immediately
 * before or immediately after them.
 * 
 * Parameters:
 *  n - the number of grams in the returned n-grams
 *  
 *  type - determines whether the returned n-grams come 
 *  from immediately before or immediately after each 
 *  token span
 * 
 * @author Bill McDowell
 *
 */
public class FnNGramContext extends FnNGram {
	public enum Type {
		BEFORE,
		AFTER,
		BEFORE_INCLUDING,
		AFTER_INCLUDING,
		BEFORE_AND_AFTER,
		BEFORE_AND_AFTER_INCLUDING
	}
	
	public enum SentenceBoundaryMode {
		SQUEEZE,
		STRICT,
		NONE
	}
	
	private SentenceBoundaryMode sentenceBoundaryMode = SentenceBoundaryMode.STRICT;
	private Type type = Type.BEFORE;
	
	public FnNGramContext() {
		this(null);
	}
	
	public FnNGramContext(Context context) {
		super(context);
		
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 2);
		this.parameterNames[this.parameterNames.length - 1] = "type";
		this.parameterNames[this.parameterNames.length - 2] = "sentenceBoundaryMode";
	}
	
	@Override
	protected boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams) {
		if ((this.type != Type.AFTER && this.type != Type.AFTER_INCLUDING) && (this.sentenceBoundaryMode != SentenceBoundaryMode.STRICT || tokenSpan.getStartTokenIndex() - this.n >= 0)) {
			ngrams.add(new TokenSpan(tokenSpan.getDocument(), 
									 tokenSpan.getSentenceIndex(), 
									 (this.sentenceBoundaryMode == SentenceBoundaryMode.SQUEEZE) ? Math.max(0, tokenSpan.getStartTokenIndex() - this.n) : tokenSpan.getStartTokenIndex() - this.n, 
									 tokenSpan.getStartTokenIndex() + ((this.type == Type.BEFORE || this.type == Type.BEFORE_AND_AFTER) ? 0 : tokenSpan.getLength())));
		} 
		
		if ((this.type != Type.BEFORE && this.type != Type.BEFORE_INCLUDING) && (this.sentenceBoundaryMode != SentenceBoundaryMode.STRICT || tokenSpan.getEndTokenIndex() + this.n <= tokenSpan.getDocument().getSentenceTokenCount(tokenSpan.getSentenceIndex()))) {
			ngrams.add(new TokenSpan(tokenSpan.getDocument(), 
					 				 tokenSpan.getSentenceIndex(), 
					 				 tokenSpan.getEndTokenIndex() - ((this.type == Type.AFTER || this.type == Type.BEFORE_AND_AFTER) ? 0 : tokenSpan.getLength()), 
					 				 (this.sentenceBoundaryMode == SentenceBoundaryMode.SQUEEZE) ? Math.min(tokenSpan.getEndTokenIndex() + this.n, tokenSpan.getDocument().getSentenceTokenCount(tokenSpan.getSentenceIndex())) : tokenSpan.getEndTokenIndex() + this.n));
		}
		
		return true;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context context) {
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
		else if (parameter.equals("sentenceBoundaryMode"))
			return Obj.stringValue(this.sentenceBoundaryMode.toString());
		else 
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("sentenceBoundaryMode"))
			this.sentenceBoundaryMode = SentenceBoundaryMode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
}
