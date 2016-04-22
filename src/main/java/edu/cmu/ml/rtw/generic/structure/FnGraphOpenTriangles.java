package edu.cmu.ml.rtw.generic.structure;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class FnGraphOpenTriangles extends Fn<WeightedStructureGraph, WeightedStructureSequence> {
	private Context context;
	
	private Set<String> ignoreTypes;
	private String[] parameterNames = { "ignoreTypes" };
	
	public FnGraphOpenTriangles() {
		
	}
	
	public FnGraphOpenTriangles(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("ignoreTypes")) {
			if (this.ignoreTypes == null)
				return null;
			Obj.Array array = Obj.array();
			for (String ignoreType : this.ignoreTypes)
				array.add(Obj.stringValue(ignoreType));
			return array;
		}
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("ignoreTypes")) {
			if (parameterValue == null)
				this.ignoreTypes = null;
			this.ignoreTypes = new HashSet<String>();
			this.ignoreTypes.addAll(this.context.getMatchArray(parameterValue));
		} else
			return false;
		return true;
	}

	@Override
	protected <C extends Collection<WeightedStructureSequence>> C compute(
			Collection<WeightedStructureGraph> input, C output) {
		for (WeightedStructureGraph g : input) {
			output.addAll(g.getOpenTriangles(this.ignoreTypes));
		}
		return output;
	}

	@Override
	public Fn<WeightedStructureGraph, WeightedStructureSequence> makeInstance(Context context) {
		return new FnGraphOpenTriangles(context);
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
		return "GraphOpenTriangles";
	}

}
