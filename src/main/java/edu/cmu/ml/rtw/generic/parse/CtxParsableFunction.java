package edu.cmu.ml.rtw.generic.parse;

import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;

/**
 * 
 * CtxParsableFunction represents an object that is 
 * parsable through an Obj.Function object from a ctx
 * scrpt using the CtxParser. For example,
 * edu.cmu.ml.rtw.generic.data.feature.Feature 
 * and edu.cmu.ml.rtw.generic.model.SupervisedModel
 * are objects that can be parsed from or converted
 * back into part of a ctx script through Obj.Function.
 * 
 * @author Bill McDowell
 *
 */
public abstract class CtxParsableFunction extends CtxParsable implements Parameterizable {	
	@Override
	public Obj toParse() {
		return toParse(true);
	}
	
	public Obj toParse(boolean includeInternal) {
		String[] parameters = getParameterNames();
		AssignmentList parameterList = new AssignmentList();
		for (String parameterName : parameters)
			parameterList.add(Assignment.assignmentUntyped(parameterName, getParameterValue(parameterName)));
		
		AssignmentList internal = null;
		if (includeInternal) {
			internal = toParseInternal();
			if (internal == null)
				internal = new AssignmentList();
			if (this.referenceName != null)
				internal.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "referenceName", Obj.stringValue(this.referenceName)));
		}
		
		return Obj.function(getGenericName(), parameterList, internal);
	}

	@Override
	protected boolean fromParseHelper(Obj obj) {
		Obj.Function function = (Obj.Function)obj;
		
		AssignmentList parameterList = function.getParameters();
		if (parameterList.size() > 0) {
			if (parameterList.get(0).getName() != null) {
				for (int i = 0; i < parameterList.size(); i++) {
					if (!setParameterValue(parameterList.get(i).getName(), parameterList.get(i).getValue()))
						return false;
				}
			} else {
				String[] parameters = getParameterNames();
				for (int i = 0; i < parameterList.size(); i++)
					if (!setParameterValue(parameters[i], parameterList.get(i).getValue()))
						return false;
			}
		}
		
		if (this.referenceName == null
				&& function.getInternalAssignments() != null
				&& function.getInternalAssignments().contains("referenceName")) {
			// FIXME Handle type errors
			this.referenceName = ((Obj.Value)function.getInternalAssignments().get("referenceName").getValue()).getStr();
		}
		
		return fromParseInternal(function.getInternalAssignments());
	}
	
	public boolean setParameterValues(Map<String, Obj> parameterValues) {
		for (Entry<String, Obj> entry : parameterValues.entrySet())
			if (!setParameterValue(entry.getKey(), entry.getValue()))
				return false;
		
		return true;
	}
	
	protected abstract boolean fromParseInternal(AssignmentList internalAssignments);
	protected abstract AssignmentList toParseInternal();
	public abstract String getGenericName();
}
