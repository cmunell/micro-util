package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnNGram is an abstract representation of a function
 * that takes token spans and returns a collection of 
 * n-grams that are related to those token spans in 
 * some way. 
 * 
 * Parameters:
 *  n - the number of grams in the returned n-grams
 * 
 * @author Bill McDowell
 *
 */
public abstract class FnNGram extends Fn<TokenSpan, TokenSpan> {
	protected String[] parameterNames = { "n" };
	protected int n = 1;
	protected Context context;

	public FnNGram() {
		
	}
	
	public FnNGram(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("n"))
			return Obj.stringValue(String.valueOf(this.n));
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("n"))
			this.n = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input)
			getNGrams(tokenSpan, output);
		
		return output;
	}
	
	protected abstract boolean getNGrams(TokenSpan tokenSpan, Collection<TokenSpan> ngrams);
	

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
}
