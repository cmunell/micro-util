package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationClassificationMeasureAccuracy<D extends Datum<L>, L> extends EvaluationClassificationMeasure<D, L> {
	public EvaluationClassificationMeasureAccuracy() {
		this(null);
	}
	
	public EvaluationClassificationMeasureAccuracy(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Double compute(boolean forceRecompute) {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method, forceRecompute);
		int trueCount = 0;
		int falseCount = 0;
		
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			trueCount += entry.getValue().get(Stat.TRUE_POSITIVE);
			falseCount += entry.getValue().get(Stat.FALSE_POSITIVE);
		}
		
		if (trueCount + falseCount == 0)
			return 1.0;
		else
			return trueCount/(double)(trueCount + falseCount);
	}
	
	@Override
	public int computeSampleSize(boolean forceRecompute) {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method, forceRecompute);
		int size = 0;
		
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			size += entry.getValue().get(Stat.TRUE_POSITIVE);
			size += entry.getValue().get(Stat.FALSE_POSITIVE);
		}
		
		return size;
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
		return "Accuracy";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasureAccuracy<D, L>(this.context);
	}

	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationMeasureAccuracy<D, L>(context);
	}
}
