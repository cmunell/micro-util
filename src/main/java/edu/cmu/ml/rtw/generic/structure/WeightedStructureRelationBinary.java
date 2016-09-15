package edu.cmu.ml.rtw.generic.structure;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class WeightedStructureRelationBinary extends WeightedStructureRelation {
	private WeightedStructureRelation r1;
	private WeightedStructureRelation r2;
	
	private Double w1;
	private Double w2;
	private Object s1; 
	private Object s2;
	
	private String[] parameterNames = { "r1", "r2" };

	public WeightedStructureRelationBinary(String type, boolean ordered) {
		this(type, ordered, null);
	}
	
	public WeightedStructureRelationBinary(String type, boolean ordered, Context context) {
		super(type, ordered, context);
		
		String[] extendedParameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + super.getParameterNames().length);
		for (int i = 0; i < super.getParameterNames().length; i++)
			extendedParameterNames[this.parameterNames.length + i] = super.getParameterNames()[i];
		this.parameterNames = extendedParameterNames; 
	}
	
	public WeightedStructureRelationBinary(String type, Context context, String id, WeightedStructureRelation r1, WeightedStructureRelation r2, boolean ordered) {
		this(type, ordered, context);
		this.id = id;
		this.r1 = r1;
		this.r2 = r2;
	}
		
	public WeightedStructureRelation getFirst() {
		return this.r1;
	}
	
	public WeightedStructureRelation getSecond() {
		return this.r2;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("r1"))
			return this.r1.toParse(false);
		else if (parameter.equals("r2"))
			return this.r2.toParse(false);
		else 
			return super.getParameterValue(parameter);
	}
	
	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("r1")) {			
			this.r1 = (WeightedStructureRelation)this.context.constructMatchWeightedStructure(parameterValue);
		} else if (parameter.equals("r2")) {
			this.r2 = (WeightedStructureRelation)this.context.constructMatchWeightedStructure(parameterValue);
		} else
			return super.setParameterValue(parameter, parameterValue);
		return true;
	}
	
	@Override
	public boolean equals(Object o) {
		WeightedStructureRelationBinary rel = (WeightedStructureRelationBinary)o;
		return super.equals(rel) && this.r1.equals(rel.r1) && this.r2.equals(rel.r2)
				|| (!this.ordered && this.r1.equals(rel.r2) && this.r2.equals(rel.r1));
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() ^ this.r1.hashCode() ^ this.r2.hashCode();
	}
	
	public WeightedStructureRelationBinary getReverse() {
		WeightedStructureRelationBinary rel = new WeightedStructureRelationBinary(this.type, this.ordered, this.context);
		rel.r1 = this.r2;
		rel.r2 = this.r1;
		rel.id = this.id;
		rel.w1 = this.w2;
		rel.w2 = this.w1;
		rel.s1 = this.s2;
		rel.s2 = this.s1;
		
		return rel;
	}
	
	@Override
	public WeightedStructure makeInstance(Context context) {
		return new WeightedStructureRelationBinary(this.type, this.ordered, context);
	}

	@Override
	public boolean remove(CtxParsable item) {
		throw new UnsupportedOperationException(); // FIXME Add this later (weighted nodes)
	}

	@Override
	public WeightedStructure add(CtxParsable item, double w, Object source, Collection<?> changes) {
		throw new UnsupportedOperationException(); // FIXME Add this later (weighted nodes)
	}

	@Override
	public double getWeight(CtxParsable item) {
		throw new UnsupportedOperationException(); // FIXME Add this later (weighted nodes)
	}

	@Override
	public WeightedStructure merge(WeightedStructure s) {
		throw new UnsupportedOperationException(); // FIXME Add this later (weighted nodes)
	}

	@Override
	public List<CtxParsable> toList() {
		throw new UnsupportedOperationException(); // FIXME Add this later (weighted nodes)
	}

	@Override
	public int getItemCount() {
		throw new UnsupportedOperationException(); // FIXME Add this later
	}
	
	@Override
	public double getTotalWeight() {
		throw new UnsupportedOperationException(); // FIXME Add this later
	}

	@Override
	public Object getSource(CtxParsable item) {
		throw new UnsupportedOperationException(); // FIXME Add this later
	}
}
