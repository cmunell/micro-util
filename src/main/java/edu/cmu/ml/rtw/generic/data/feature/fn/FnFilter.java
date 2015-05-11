package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnFilter extends Fn<String, String> {
	public enum Type {
		SUFFIX,
		PREFIX,
		SUBSTRING,
		EQUAL
	}
	
	private String[] parameterNames = { "filter", "type" };
	private String filter = "";
	private Type type = Type.SUFFIX;
	
	private Context<?, ?> context;

	public FnFilter() {
		
	}
	
	public FnFilter(Context<?, ?> context) {
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
		else if (parameter.equals("filter"))
			return Obj.stringValue(this.filter);
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filter"))
			this.filter = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}
	
	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		if (this.filter.length() == 0 && this.type != Type.EQUAL) {
			output.addAll(input);
		} else if (this.type == Type.EQUAL) {
			if (input.contains(this.filter))
				output.add(this.filter);
		} else {
			for (String str : input) {
				if (matchesFilter(str))
					output.add(str);
			}
		}
			
		return output;
	}
	
	private boolean matchesFilter(String str) {
		if (this.type == Type.SUFFIX)
			return str.endsWith(this.filter);
		else if (this.type == Type.PREFIX)
			return str.startsWith(this.filter);
		else if (this.type == Type.SUBSTRING)
			return str.contains(this.filter) && !str.equals(this.filter);
		else
			return str.equals(this.filter);
	}

	@Override
	public Fn<String, String> makeInstance(Context<?, ?> context) {
		return new FnFilter(context);
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
		return "Filter";
	}

}
