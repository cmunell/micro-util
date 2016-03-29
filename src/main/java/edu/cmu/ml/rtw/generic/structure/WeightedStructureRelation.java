package edu.cmu.ml.rtw.generic.structure;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public abstract class WeightedStructureRelation extends WeightedStructure {	
	protected boolean ordered = true;
	protected String id;
	protected String type;

	private static String[] parameterNames = { "ordered", "id" };
	
	protected Context context;
	
	public WeightedStructureRelation(String type) {
		this(type, null);
	}
	
	public WeightedStructureRelation(String type, Context context) {
		this.type = type;
		this.context = context;
	}
	
	public String getType() {
		return this.type;
	}
	
	public String getId() {
		return this.id;
	}
	
	public boolean isOrdered() {
		return this.ordered;
	}
	
	@Override
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("ordered"))
			return Obj.stringValue(String.valueOf(this.ordered));
		else if (parameter.equals("id"))
			return Obj.stringValue(this.id);
		return null;
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("ordered"))
			this.ordered = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("id"))
			this.id = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
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
		return this.type;
	}
	
	@Override
	public boolean equals(Object o) {
		WeightedStructureRelation rel = (WeightedStructureRelation)o;
		return this.ordered == rel.ordered
				&& this.type.equals(rel.type)
				&& ((this.id == null && rel.id == null) || (this.id != null && this.id.equals(rel.id)));
	}
	
	@Override
	public int hashCode() {
		int h = this.type.hashCode();
		
		if (this.id != null)
			h ^= this.id.hashCode();
		
		return h;
	}
}
