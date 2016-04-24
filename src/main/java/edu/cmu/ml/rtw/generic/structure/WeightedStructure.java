package edu.cmu.ml.rtw.generic.structure;

import java.util.Collection;
import java.util.List;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.CtxParsable;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;

public abstract class WeightedStructure extends CtxParsableFunction {
	public abstract boolean remove(CtxParsable item);
	public abstract WeightedStructure add(CtxParsable item, double w, Collection<?> changes);
	public abstract double getWeight(CtxParsable item);
	public abstract WeightedStructure merge(WeightedStructure s);
	public abstract WeightedStructure makeInstance(Context context);
	public abstract List<CtxParsable> toList();
	public abstract int getItemCount();
	public abstract double getTotalWeight();
	
	public WeightedStructure add(CtxParsable item, double w) {
		return add(item, w, null);
	}
}
