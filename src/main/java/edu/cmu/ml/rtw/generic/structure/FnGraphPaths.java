package edu.cmu.ml.rtw.generic.structure;

import java.util.Collection;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnGraphPaths extends Fn<WeightedStructureGraph, WeightedStructureSequence> {
	private Context context;
	
	private int length = 1;
	private String[] parameterNames = { "length" };
	
	public FnGraphPaths() {
		
	}
	
	public FnGraphPaths(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("length"))
			return Obj.stringValue(String.valueOf(this.length));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("length"))
			this.length = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	protected <C extends Collection<WeightedStructureSequence>> C compute(
			Collection<WeightedStructureGraph> input, C output) {
		for (WeightedStructureGraph g : input)
			output.addAll(g.getEdgePaths(this.length));
		return output;
	}

	@Override
	public Fn<WeightedStructureGraph, WeightedStructureSequence> makeInstance(Context context) {
		return new FnGraphPaths(context);
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
		return "GraphPaths";
	}

}
