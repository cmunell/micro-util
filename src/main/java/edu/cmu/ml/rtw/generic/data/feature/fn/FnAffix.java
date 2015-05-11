package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnAffix extends Fn<String, String> {
	public enum Type {
		SUFFIX,
		PREFIX
	}
	
	private Context<?, ?> context;
	private Type type = Type.SUFFIX;
	private int nMin = 3;
	private int nMax = 3;
	private String[] parameterNames = { "type", "nMin", "nMax" };
	
	public FnAffix() {
		
	}
	
	public FnAffix(Context<?, ?> context) {
		this.context = context;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("nMin"))
			return Obj.stringValue(String.valueOf(this.nMin));
		else if (parameter.equals("nMax"))
			return Obj.stringValue(String.valueOf(this.nMax));
		else if (parameter.equals("type"))
			return Obj.stringValue(this.type.toString());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("nMin"))
			this.nMin = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("nMax"))
			this.nMax = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {
			for (int i = this.nMin; i <= this.nMax && str.length() > i; i++) {
				String affix = (this.type == Type.SUFFIX) ? 
						str.substring(str.length() - i, str.length()) 
						: str.substring(0, i);
				
				output.add(affix);
			}
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context<?, ?> context) {
		return new FnAffix(context);
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
		return "Affix";
	}

}
