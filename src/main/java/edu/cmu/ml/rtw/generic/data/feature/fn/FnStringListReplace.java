package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnStringListReplace extends Fn<String, String> {	
	public enum Mode {
		LITERAL,
		REGEX
	}
	
	private Context context;
	private List<String> target = new ArrayList<String>();
	private String replace = " ";
	private Mode mode = Mode.LITERAL;
	private boolean ignoreCase = false;
	private String[] parameterNames = { "target", "replace", "mode", "ignoreCase" };
	
	public FnStringListReplace() {

	}
	
	public FnStringListReplace(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("target"))
			return Obj.array(this.target);
		else if (parameter.equals("replace"))
			return Obj.stringValue(this.replace.toString());
		else if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		else if (parameter.equals("ignoreCase"))
			return Obj.stringValue(String.valueOf(this.ignoreCase));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("target"))
			this.target = this.context.getMatchArray(parameterValue);
		else if (parameter.equals("replace"))
			this.replace = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("ignoreCase"))
			this.ignoreCase = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {	
			for (String target : this.target) {		
				str = strReplace(str, target);
			}
			
			output.add(str);
		}
		
		return output;
	}

	private String strReplace(String str, String target) {
		int flags = 0;
		if (this.ignoreCase)
			flags = Pattern.CASE_INSENSITIVE;
		if (this.mode == Mode.LITERAL)
			flags |= Pattern.LITERAL;
		
		return Pattern.compile(target, flags).matcher(str).replaceAll(this.replace);
	}
	
	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnStringListReplace(context);
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
		return "StringListReplace";
	}

}
