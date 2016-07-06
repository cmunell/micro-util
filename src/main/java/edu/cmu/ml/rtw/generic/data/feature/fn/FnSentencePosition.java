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
		ALMOST_START,
		END,
		ALMOST_END,
		MIDDLE,
		ALL,
		NONE
	}
	
	private Position filter = Position.NONE;
	private String[] parameterNames = { "filter" };
	
	private Context context;
	
	public FnSentencePosition() {
		
	}
	
	public FnSentencePosition(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("filter"))
			return Obj.stringValue(this.filter.toString());
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("filter"))
			this.filter = Position.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan tokenSpan : input) {
			if (tokenSpan.getSentenceIndex() < 0 && this.filter == Position.NONE) {
				output.add(Position.NONE.toString());
				continue;
			}
				
			int sentenceTokenCount = tokenSpan.getDocument().getSentenceTokenCount(tokenSpan.getSentenceIndex());
			if (tokenSpan.getStartTokenIndex() == 0 
					&& tokenSpan.getEndTokenIndex() == sentenceTokenCount && (this.filter == Position.NONE || this.filter == Position.ALL))
				output.add(Position.ALL.toString());
			
			if (tokenSpan.getStartTokenIndex() == 0 && (this.filter == Position.NONE || this.filter == Position.START))
				output.add(Position.START.toString());
			else if (tokenSpan.getEndTokenIndex() == sentenceTokenCount && (this.filter == Position.NONE || this.filter == Position.END))
				output.add(Position.END.toString());
			else {
				if (tokenSpan.getStartTokenIndex() == 1 && (this.filter == Position.NONE || this.filter == Position.ALMOST_START))
					output.add(Position.ALMOST_START.toString());
				if (tokenSpan.getEndTokenIndex() == sentenceTokenCount - 1  && (this.filter == Position.NONE || this.filter == Position.ALMOST_END))
					output.add(Position.ALMOST_END.toString());
				if (this.filter == Position.NONE || this.filter == Position.MIDDLE)
					output.add(Position.MIDDLE.toString());
			}
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
		return "SentencePosition";
	}

}
