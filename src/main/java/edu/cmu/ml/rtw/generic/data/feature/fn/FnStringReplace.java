package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnStringReplace extends Fn<String, String> {	
	public enum Mode {
		LITERAL,
		REGEX
	}
	
	private Context context;
	private String target = "_";
	private String replace = " ";
	private Mode mode = Mode.LITERAL;
	private String[] parameterNames = { "target", "replace", "mode" };
	
	public FnStringReplace() {

	}
	
	public FnStringReplace(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("target"))
			return Obj.stringValue(this.target.toString());
		else if (parameter.equals("replace"))
			return Obj.stringValue(this.replace.toString());
		else if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("target"))
			this.target = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("replace"))
			this.replace = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {	
			output.add((this.mode == Mode.LITERAL) ? str.replace(this.target, this.replace) : str.replaceAll(this.target, this.replace));
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnStringReplace(context);
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
		return "StringReplace";
	}

}
