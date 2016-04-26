package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.util.Pair;

public abstract class MethodClassification<D extends Datum<L>, L> extends CtxParsableFunction {
	protected DatumContext<D, L> context;
	
	public MethodClassification() {
		this(null);
	}
	
	public MethodClassification(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		return this.referenceName.equals(((MethodClassification<D, L>)o).getReferenceName());
	}
	
	@Override
	public int hashCode() {
		return this.referenceName.hashCode();
	}
	
	public Map<D, L> classify(TaskClassification<D, L> task) {
		return classify(task.getData());
	}
	
	public Map<D, Pair<L, Double>> classifyWithScore(TaskClassification<D, L> task) {
		return classifyWithScore(task.getData());
	}
	
	public boolean init() {
		return init(null);
	}
	
	public boolean matchesData(DataSet<?, ?> data) {
		return data.getDatumTools().equals(this.context.getDatumTools());
	}
	
	public MethodClassification<D, L> clone() {
		return this.clone(this.referenceName);
	}
	
	public abstract Map<D, L> classify(DataSet<D, L> data);
	public abstract Map<D, Pair<L, Double>> classifyWithScore(DataSet<D, L> data);
	public abstract boolean init(DataSet<D, L> testData);
	public abstract MethodClassification<D, L> clone(String referenceName);
	public abstract MethodClassification<D, L> makeInstance(DatumContext<D, L> context);
	public abstract boolean hasTrainable();
	public abstract Trainable<D, L> getTrainable();
}
