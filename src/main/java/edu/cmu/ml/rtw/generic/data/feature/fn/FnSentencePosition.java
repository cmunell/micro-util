package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnSentencePosition takes a collection of token spans and computes
 * their positions within their sentences
 * 
 * @author Bill McDowell
 *
 */
public class FnSentencePosition extends Fn<TokenSpan, String> {
	public enum Position {
		START,
		END,
		MIDDLE,
		ALL
	}
	
	private String[] parameterNames = {  };
	
	public FnSentencePosition() {
		
	}
	
	public FnSentencePosition(Context context) {
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
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			int sentenceTokenCount = tokenSpan.getDocument().getSentenceTokenCount(tokenSpan.getSentenceIndex());
			if (tokenSpan.getStartTokenIndex() == 0 
					&& tokenSpan.getEndTokenIndex() == sentenceTokenCount)
				output.add(Position.ALL.toString());
			else if (tokenSpan.getStartTokenIndex() == 0)
				output.add(Position.START.toString());
			else if (tokenSpan.getEndTokenIndex() == sentenceTokenCount)
				output.add(Position.END.toString());
			else
				output.add(Position.MIDDLE.toString());
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, String> makeInstance(Context context) {
		return new FnSentencePosition(context);
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
		return "PoS";
	}

}
