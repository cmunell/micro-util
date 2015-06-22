package edu.cmu.ml.rtw.generic.data.feature.fn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FnFilter filters a collection of strings
 * down to those that are equal to, suffixed,
 * prefixed, or extensions of a given filter
 * string.
 * 
 * Parameters:
 *  filter - The string by which to filter the
 *  input collection
 *  
 *  type - Determines how to use the filter
 *  
 *  filterTransform - Function by which to transform
 *  the filter before applying it
 * 
 * @author Bill McDowell
 *
 */
public class FnFilter extends Fn<String, String> {
	public enum Type {
		SUFFIX,
		PREFIX,
		SUBSTRING,
		EQUAL
	}
	
	private String[] parameterNames = { "filter", "type", "filterTransform" };
	private String filter = "";
	private Type type = Type.SUFFIX;
	private Fn<String, String> filterTransform = null;
	
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
		else if (parameter.equals("filterTransform"))
			return this.filterTransform.toParse();
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("type"))
			this.type = Type.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filter"))
			this.filter = this.context.getMatchValue(parameterValue);
		else if (parameter.equals("filterTransform"))
			this.filterTransform = this.context.getMatchOrConstructStrFn(parameterValue); 
		else
			return false;
		return true;
	}
	
	@Override
	public <C extends Collection<String>> C compute(Collection<String> input, C output) {
		List<String> transFilterInput  = new ArrayList<String>();
		transFilterInput.add(this.filter);
		List<String> transFilter = new ArrayList<String>();
		
		if (this.filterTransform != null) {
			transFilter = this.filterTransform.compute(transFilterInput, transFilter);
		} else {
			transFilter.add(this.filter);
		}
			
		if (transFilter.size() == 1 && transFilter.get(0).length() == 0 && this.type != Type.EQUAL) {
			output.addAll(input);
		} else if (this.type == Type.EQUAL) {
			for (String filter : transFilter)
				if (input.contains(filter))
					output.add(filter);
		} else {
			for (String str : input) {
				if (matchesFilter(str, transFilter))
					output.add(str);
			}
		}
			
		return output;
	}
	
	private boolean matchesFilter(String str, List<String> filters) {
		for (String filter : filters) {
			if (this.type == Type.SUFFIX)
				return str.endsWith(filter);
			else if (this.type == Type.PREFIX)
				return str.startsWith(filter);
			else if (this.type == Type.SUBSTRING)
				return str.contains(filter) && !str.equals(filter);
			else
				return str.equals(filter);
		}
		
		return false;
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
