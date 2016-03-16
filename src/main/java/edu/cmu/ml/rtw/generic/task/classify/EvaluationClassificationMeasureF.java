package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationClassificationMeasureF<D extends Datum<L> , L> extends EvaluationClassificationMeasure<D, L> {
	/**
	 * Mode determines whether the F measure should be macro-averaged, micro-averaged,
	 * or macro-averaged weighted by actual label frequencies.
	 *
	 */
	public enum Mode {
		MACRO,
		MACRO_WEIGHTED,
		MICRO
	}
	
	private Mode mode = Mode.MACRO_WEIGHTED;
	private double Beta = 1.0;
	private L filterLabel; 
	private String[] parameterNames = { "mode", "Beta", "filterLabel" };

	public EvaluationClassificationMeasureF() {
		this(null);
	}
	
	public EvaluationClassificationMeasureF(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Double compute() {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method);
		
		double F = 0.0;

		double totalTp = 0.0;
		double totalFp = 0.0;
		double totalTn = 0.0;
		double totalFn = 0.0;
		
		double Beta2 = this.Beta*this.Beta;
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			if (this.filterLabel != null && !this.filterLabel.equals(entry.getKey()))
				continue;
			
			double tp = entry.getValue().get(Stat.TRUE_POSITIVE);
			double fp = entry.getValue().get(Stat.FALSE_POSITIVE);
			double tn = entry.getValue().get(Stat.TRUE_NEGATIVE);
			double fn = entry.getValue().get(Stat.FALSE_NEGATIVE);
			
			double weight = 0.0;
			if (this.filterLabel != null || this.mode == Mode.MICRO) {
				weight = 1.0;
			} else if (this.mode == Mode.MACRO_WEIGHTED) {
				if (Double.compare(tp + fp + tn + fn, 0.0) == 0)
					weight = 1.0;
				else
					weight = (tp+fn)/(tp+fp+tn+fn);
			} else {
				weight = 1.0/stats.size();
			}
			
			if (this.mode == Mode.MICRO) {
				totalTp += tp;
				totalFp += fp;
				totalTn += tn;
				totalFn += fn;
			} else {
				if (Double.compare(tp + fn + fp, 0.0) == 0)
					F += weight;
				else
					F += weight *(1.0+Beta2)*tp/((1.0+Beta2)*tp + Beta2*fn + fp);
			}
		}
		
		if (this.mode == Mode.MICRO) {
			if (Double.compare(totalTp + totalFp, 0.0) == 0 || Double.compare(totalTp + totalFn, 0.0) == 0)
				return 1.0;
			else
				return (1.0+Beta2)*totalTp/((1.0+Beta2)*totalTp + Beta2*totalFp + totalFn);
		} else 
			return F;
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
		if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		else if (parameter.equals("Beta"))
			return Obj.stringValue(String.valueOf(this.Beta));
		else if (parameter.equals("filterLabel"))
			return (this.filterLabel == null) ? null : Obj.stringValue(this.filterLabel.toString());
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("Beta"))
			this.Beta = Double.valueOf(this.context.getMatchValue(parameterValue));
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
		return "F";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasureF<D, L>(this.context);
	}
	
	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationMeasureF<D, L>(context);
	}
}
