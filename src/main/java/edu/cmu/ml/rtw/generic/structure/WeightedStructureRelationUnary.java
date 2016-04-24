package edu.cmu.ml.rtw.generic.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;

public class WeightedStructureRelationUnary extends WeightedStructureRelation {

	public WeightedStructureRelationUnary(String type) {
		this(type, null);
	}
	
	public WeightedStructureRelationUnary(String type, Context context) {
		super(type, false, context);
	}
	
	public WeightedStructureRelationUnary(String type, Context context, String id) {
		this(type, context);
		this.id = id;
	}

	@Override
	public boolean remove(CtxParsable item) {
		throw new UnsupportedOperationException(); // FIXME Implement later
	}

	@Override
	public WeightedStructure add(CtxParsable item, double w, Collection<?> changes) {
		throw new UnsupportedOperationException(); // FIXME Implement later
	}

	@Override
	public double getWeight(CtxParsable item) {
		throw new UnsupportedOperationException(); // FIXME Implement later
	}

	@Override
	public WeightedStructure merge(WeightedStructure s) {
		throw new UnsupportedOperationException(); // FIXME Implement later
	}

	@Override
	public WeightedStructure makeInstance(Context context) {
		return new WeightedStructureRelationUnary(this.type, context);
	}

	@Override
	public List<CtxParsable> toList() {
		List<CtxParsable> list = new ArrayList<CtxParsable>();
		list.add(this);
		return list;
	}
	
	@Override
	public int getItemCount() {
		throw new UnsupportedOperationException(); // FIXME Add this later
	}
}
