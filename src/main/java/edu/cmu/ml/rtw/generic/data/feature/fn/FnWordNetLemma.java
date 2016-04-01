package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnWordNetLemma extends Fn<TokenSpan, String> {
	private String[] parameterNames = { };
	
	private Context context;
	
	public FnWordNetLemma() {
		
	}
	
	public FnWordNetLemma(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			if (tokenSpan.getSentenceIndex() < 0)
				continue;
			
			String tokenSpanStr = tokenSpan.toString();
			PoSTag tag = tokenSpan.getDocument().getPoSTag(tokenSpan.getSentenceIndex(), tokenSpan.getEndTokenIndex() - 1);
			String lemma = this.context.getDataTools().getWordNet().getLemma(tokenSpanStr, tag);
			if (lemma != null)
				output.add(lemma);
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnWordNetLemma(context);
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
		return "WordNetLemma";
	}
}
