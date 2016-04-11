package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnCat extends Fn<String, String> {
	private boolean sorted = false;
	private String[] parameterNames = { "sorted" };
	
	private Context context;
	
	public FnCat() {
		
	}
	
	public FnCat(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("sorted"))
			return Obj.stringValue(String.valueOf(this.sorted));
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("sorted"))
			this.sorted = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else 
			return false;
		
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		List<String> toCat = new ArrayList<String>();
		toCat.addAll(input);
		if (this.sorted) {
			Collections.sort(toCat);
		} 
		
		StringBuilder str = new StringBuilder();
		for (String cat : toCat)
			str.append(cat).append("_");
		if (str.length() > 0)
			str.delete(str.length() - 1, str.length());
		output.add(str.toString());
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context context) {
		return new FnCat(context);
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
		return "Cat";
	}

}
