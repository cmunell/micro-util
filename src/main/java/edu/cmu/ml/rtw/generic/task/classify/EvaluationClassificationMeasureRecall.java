package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationClassificationMeasureRecall<D extends Datum<L> , L> extends EvaluationClassificationMeasure<D, L> {
	private boolean weighted;
	private L filterLabel; 
	private String[] parameterNames = { "weighted", "filterLabel" };
	
	public EvaluationClassificationMeasureRecall() {
		this(null);
	}
	
	public EvaluationClassificationMeasureRecall(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Double compute() {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method);
		
		double r = 0.0;
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			if (this.filterLabel != null && !entry.getKey().equals(this.filterLabel))
				continue;
			
			double tp = entry.getValue().get(Stat.TRUE_POSITIVE);
			double fp = entry.getValue().get(Stat.FALSE_POSITIVE);
			double tn = entry.getValue().get(Stat.TRUE_NEGATIVE);
			double fn = entry.getValue().get(Stat.FALSE_NEGATIVE);
			
			double weight = 0.0;
			if (this.filterLabel != null) {
				weight = 1.0;
			} else if (this.weighted) {
				if (Double.compare(tp + fp + tn + fn, 0.0) == 0)
					weight = 1.0;
				else
					weight = (tp+fn)/(tp+fp+tn+fn);
			} else {
				weight = 1.0/stats.size();
			}
			
			if (Double.compare(tp + fn, 0.0) == 0)
				r += weight;
			else
				r += weight * tp/(tp+fn);
		}
		
		return r;
	}

	@Override
	public String[] getParameterNames() {
		String[] parentParameterNames = super.getParameterNames();
		String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + parentParameterNames.length);
		for (int i = 0; i < parentParameterNames.length; i++)
			parameterNames[this.parameterNames.length + i] = parentParameterNames[i];
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("weighted"))
			return Obj.stringValue(String.valueOf(this.weighted));
		else if (parameter.equals("filterLabel"))
			return (this.filterLabel == null) ? null : Obj.stringValue(this.filterLabel.toString());
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("weighted"))
			this.weighted = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filterLabel"))
			this.filterLabel = (parameterValue == null) ? null : this.context.getDatumTools().labelFromString(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
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
		return "Recall";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasureRecall<D, L>(this.context);
	}
	
	@Override
	public EvaluationClassification<D, L, ?> makeInstance(DatumContext<D, L> context) {
		return new EvaluationClassificationMeasureRecall<D, L>(context);
	}
}
