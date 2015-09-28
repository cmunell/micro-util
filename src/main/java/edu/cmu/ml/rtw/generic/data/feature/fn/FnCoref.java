package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpanCluster;
import edu.cmu.ml.rtw.generic.data.feature.fn.FnFilter.Type;
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
	private Context<?, ?> context;
	
	private String[] parameterNames = { "spanMinLength", "spanMaxLength" };
	private int spanMinLength = -1;
	private int spanMaxLength = -1;
	
	public FnCoref() {
		
	}
	
	public FnCoref(Context<?, ?> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("spanMinLength"))
			return Obj.stringValue(String.valueOf(this.spanMinLength));
		else if (parameter.equals("spanMaxLength"))
			return Obj.stringValue(String.valueOf(this.spanMaxLength));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("spanMinLength"))
			this.spanMinLength = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("spanMaxLength"))
			this.spanMaxLength = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			List<Pair<TokenSpan, TokenSpanCluster>> clusters = span.getDocument().getCoref(span);
			for (Pair<TokenSpan, TokenSpanCluster> cluster : clusters) {
				List<TokenSpan> corefSpans = cluster.getSecond().getTokenSpans();
				for (TokenSpan corefSpan : corefSpans) {
					if (!corefSpan.equals(span)
							&& (this.spanMaxLength < 0 || corefSpan.getLength() <= this.spanMaxLength)
							&& (this.spanMinLength < 0 || corefSpan.getLength() >= this.spanMinLength)) {
						output.add(corefSpan);
					}
				}
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
