package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnTokenSpanLengthFilter extends Fn<TokenSpan, TokenSpan> {
	private int min = -1;
	private int max = -1;
	private String[] parameterNames = { "min", "max"};
	
	private Context context;
	
	public FnTokenSpanLengthFilter() {
		
	}
	
	public FnTokenSpanLengthFilter(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("min"))
			return Obj.stringValue(String.valueOf(this.min));
		else if (parameter.equals("max"))
			return Obj.stringValue(String.valueOf(this.max));
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("min"))
			this.min = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("max"))
			this.max = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<TokenSpan>> C compute(Collection<TokenSpan> input, C output) {
		for (TokenSpan span : input) {
			if ((this.min < 0 || span.getLength() >= this.min) && (this.max < 0 || span.getLength() <= this.max))
				output.add(span);
		}
		
		return output;
	}

	@Override
	public Fn<TokenSpan, TokenSpan> makeInstance(
			Context context) {
		return new FnTokenSpanLengthFilter(context);
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
		return "TokenSpanLengthFilter";
	}
}
