package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnClean computes a list of cleaned strings from
 * a list of strings.
 * 
 * Parameters:
 *  cleanFn - the string transformation function to 
 *  use for cleaning.
 * 
 * @author Bill McDowell
 *
 */
public class FnClean extends Fn<String, String> {
	private DataTools.StringTransform cleanFn;
	private String[] parameterNames = { "cleanFn" };
	
	private Context<?, ?> context;
	
	public FnClean() {
		
	}
	
	public FnClean(Context<?, ?> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("cleanFn"))
			return Obj.stringValue((this.cleanFn == null) ? "" : this.cleanFn.toString());
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("cleanFn"))
			this.cleanFn = this.context.getDatumTools().getDataTools().getCleanFn(this.context.getMatchValue(parameterValue));
		else 
			return false;
		
		return true;
	}

	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		for (String str : input) {
			output.add(this.cleanFn.transform(str));
		}
		
		return output;
	}

	@Override
	public Fn<String, String> makeInstance(Context<?, ?> context) {
		return new FnClean(context);
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
		return "Clean";
	}
}
