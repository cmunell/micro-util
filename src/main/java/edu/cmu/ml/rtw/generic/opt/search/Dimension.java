package edu.cmu.ml.rtw.generic.opt.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;

public abstract class Dimension extends CtxParsableFunction {
	public static final String DIMENSION_STR = "dimension"; // FIXME Move this later
	
	public enum Type {
		ENUMERATED
	}
	
	private Integer parentValueIndex = null;
	private int stageIndex = 0;
	protected String[] parameterNames = new String[] { "stageIndex", "parentValueIndex" };
	
	private Map<Integer, List<Dimension>> subDimensions = null;
	
	private Context context;
	
	public Dimension(Context context) {
		this.context = context;
		this.subDimensions = new HashMap<Integer, List<Dimension>>();
	}
	
	public int getStageIndex() {
		return this.stageIndex;
	}
	
	public List<Dimension> getSubDimensions(int valueIndex) {
		return this.subDimensions.get(valueIndex);
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("stageIndex"))
			return Obj.stringValue(String.valueOf(this.stageIndex));
		else if (parameter.equals("parentValueIndex") && this.parentValueIndex != null)
			return Obj.stringValue(String.valueOf(this.parentValueIndex));
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("stageIndex"))
			this.stageIndex = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("parentValueIndex"))
			this.parentValueIndex = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;
		
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			
			if (assignment.getType().equals(DIMENSION_STR)) {
				Dimension subDimension = new DimensionEnumerated(this.context); // FIXME Other dimensions supported later
				if (!subDimension.fromParse(null, assignment.getName(), assignment.getValue()))
					return false;
				if (!this.subDimensions.containsKey(subDimension.parentValueIndex))
					this.subDimensions.put(subDimension.parentValueIndex, new ArrayList<Dimension>());
				this.subDimensions.get(subDimension.parentValueIndex).add(subDimension);
			}
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList internalAssignments = new AssignmentList();
		
		for (Entry<Integer, List<Dimension>> entry : this.subDimensions.entrySet()) {
			for (Dimension dimension : entry.getValue()) {
				internalAssignments.add(Assignment.assignmentTyped(null, DIMENSION_STR, dimension.getReferenceName(), dimension.toParse(true)));
			}
		}
		
		return internalAssignments;
	}
	
	@Override
	public boolean equals(Object o) {
		Dimension d = (Dimension)o;
		return d.getReferenceName().equals(this.getReferenceName());

	}
	
	@Override
	public int hashCode() {
		return this.getReferenceName().hashCode();
	}
	
	public String toString() {
		return this.referenceName;
	}
	
	public abstract Type getType();
}
