package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpanCluster;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * FnCoref takes a collection of token spans
 * and returns token spans that are coreferent
 * with them.
 * 
 * @author Bill McDowell
 * 
 */
public class FnCoref extends Fn<TokenSpan, TokenSpan> {
	private String[] parameterNames = {};
	
	public FnCoref() {
		
	}
	
	public FnCoref(Context<?, ?> context) {
		
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return false;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			List<Pair<TokenSpan, TokenSpanCluster>> clusters = span.getDocument().getCoref(span);
			for (Pair<TokenSpan, TokenSpanCluster> cluster : clusters) {
				List<TokenSpan> corefSpans = cluster.getSecond().getTokenSpans();
				for (TokenSpan corefSpan : corefSpans)
					if (!corefSpan.equals(span))
						output.add(corefSpan);
			}
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context<?, ?> context) {
		return new FnCoref(context);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		return null;
	}
 
	@Override
	public String getGenericName() {
		return "Coref";
	}
}
