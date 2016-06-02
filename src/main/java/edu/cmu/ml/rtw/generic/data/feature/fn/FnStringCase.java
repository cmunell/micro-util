package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnStringCase extends Fn<String, String> {
	public enum Type {
		UPPER,
		LOWER
	}
	
	private Context context;
	private Type type = Type.LOWER;
	private String[] parameterNames = { "type" };
	
	public FnStringCase() {
		
	}
	
	public FnStringCase(Context context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("type"))
			return Obj.stringValue(this.type.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {	
			output.add(this.type == Type.LOWER ? str.toLowerCase() : str.toUpperCase());
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnStringCase(context);
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
		return "StringCase";
	}

}
