package edu.cmu.ml.rtw.generic.task.classify;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.task.classify.meta.PredictionClassification;
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
	
	public PredictionClassification<D, L> predict(D datum) {
		Pair<L, Double> scoredLabel = classifyWithScore(datum);
		return new PredictionClassification<D, L>(datum, scoredLabel.getFirst(), scoredLabel.getSecond(), this);
	}
	
	public Map<D, PredictionClassification<D, L>> predict(TaskClassification<D, L> task) {
		return predict(task.getData());
	}
	
	public Map<D, PredictionClassification<D, L>> predict(DataSet<D, L> data) {
		Map<D, Pair<L, Double>> scoredLabels = classifyWithScore(data);
		Map<D, PredictionClassification<D, L>> predictions = new HashMap<>();
		
		for (Entry<D, Pair<L, Double>> entry : scoredLabels.entrySet()) {
			predictions.put(entry.getKey(), 
				new PredictionClassification<D, L>(entry.getKey(), entry.getValue().getFirst(), entry.getValue().getSecond(), this));
		}
		
		return predictions;
	}
		
	public Map<D, L> classify(TaskClassification<D, L> task) {
		return classify(task.getData());
	}
	
	public Map<D, Pair<L, Double>> classifyWithScore(TaskClassification<D, L> task) {
		return classifyWithScore(task.getData());
	}
	
	public Map<D, Double> score(TaskClassification<D, L> task, L label) {
		return score(task.getData(), label);
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
	public abstract Map<D, Double> score(DataSet<D, L> data, L label);
	public abstract L classify(D datum);
	public abstract Pair<L, Double> classifyWithScore(D datum);
	public abstract double score(D datum, L label);
	public abstract boolean init(DataSet<D, L> testData);
	public abstract MethodClassification<D, L> clone(String referenceName);
	public abstract MethodClassification<D, L> makeInstance(DatumContext<D, L> context);
	public abstract boolean hasTrainable();
	public abstract Trainable<D, L> getTrainable();
}
