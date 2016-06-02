package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;


public class FnTrim extends Fn<String, String> {		
	public FnTrim() {
		
	}
	
	public FnTrim(Context context) {
		
	}

	@Override
	public String[] getParameterNames() {
		return new String[0];
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
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {	
			output.add(str.trim());
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnTrim(context);
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
		return "Trim";
	}

}
