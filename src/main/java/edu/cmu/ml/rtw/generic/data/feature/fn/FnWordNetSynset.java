package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnWordNetSynset extends Fn<TokenSpan, String> {
	private boolean firstOnly = false;
	private String[] parameterNames = { "firstOnly" };
	
	private Context context;
	
	public FnWordNetSynset() {
		
	}
	
	public FnWordNetSynset(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("firstOnly"))
			return Obj.stringValue(String.valueOf(this.firstOnly));
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("firstOnly"))
			this.firstOnly = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			if (tokenSpan.getSentenceIndex() < 0)
				continue;
			
			String tokenSpanStr = tokenSpan.toString();
			PoSTag tag = tokenSpan.getDocument().getPoSTag(tokenSpan.getSentenceIndex(), tokenSpan.getEndTokenIndex() - 1);
			if (this.firstOnly) {
				String synset = this.context.getDataTools().getWordNet().getFirstImmediateSynsetName(tokenSpanStr, tag);
				if (synset != null)
					output.add(synset);
			} else {
				Set<String> synsets = this.context.getDataTools().getWordNet().getImmediateSynsetNames(tokenSpanStr, tag);
				if (synsets != null) {
					for (String synset : synsets) {
						output.add(synset);
					}
				}
			}
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnWordNetSynset(context);
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
		return "WordNetSynset";
	}
}
