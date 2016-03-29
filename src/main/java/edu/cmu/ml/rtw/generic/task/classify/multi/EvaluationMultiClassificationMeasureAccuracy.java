package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationMultiClassificationMeasureAccuracy extends EvaluationMultiClassificationMeasure {
	public EvaluationMultiClassificationMeasureAccuracy() {
		this(null);
	}
	
	public EvaluationMultiClassificationMeasureAccuracy(Context context) {
		super(context);
	}

	@Override
	public Double compute() {
		List<Map<?, Map<Stat, Integer>>> stats =  this.task.computeStats(this.method);
		int trueCount = 0;
		int falseCount = 0;
		
		for (Map<?, Map<Stat, Integer>> stat : stats) {
			for (Entry<?, Map<Stat, Integer>> entry : stat.entrySet()) {
				trueCount += entry.getValue().get(Stat.TRUE_POSITIVE);
				falseCount += entry.getValue().get(Stat.FALSE_POSITIVE);
			}
		}
		
		if (trueCount + falseCount == 0)
			return 1.0;
		else
			return trueCount/(double)(trueCount + falseCount);
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
	protected EvaluationMultiClassificationMeasure makeInstance() {
		return new EvaluationMultiClassificationMeasureAccuracy(this.context);
	}

	@Override
	public EvaluationMultiClassification<Double> makeInstance(Context context) {
		return new EvaluationMultiClassificationMeasureAccuracy(context);
	}

}
