package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationClassificationMeasurePrecision<D extends Datum<L> , L> extends EvaluationClassificationMeasure<D, L> {
	public enum Mode {
		MACRO,
		MACRO_WEIGHTED,
		MICRO
	}
	
	private Mode mode = Mode.MACRO_WEIGHTED;
	private L filterLabel; 
	private String[] parameterNames = { "filterLabel", "mode" };
	
	private int microNumerator = 0;
	private int microDenominator = 0;
	
	public EvaluationClassificationMeasurePrecision() {
		this(null);
	}
	
	public EvaluationClassificationMeasurePrecision(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Double compute(boolean forceRecompute) {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method, forceRecompute);
		
		double p = 0.0;
		double num = 0.0;
		double den = 0.0;
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			if (this.filterLabel != null && !this.filterLabel.equals(entry.getKey()))
				continue;
			
			double tp = entry.getValue().get(Stat.TRUE_POSITIVE);
			double fp = entry.getValue().get(Stat.FALSE_POSITIVE);
			double tn = entry.getValue().get(Stat.TRUE_NEGATIVE);
			double fn = entry.getValue().get(Stat.FALSE_NEGATIVE);
			
			if (this.mode.equals(Mode.MICRO)) {
				num += tp;
				den += (tp + fp);
			} else {
				double weight = 0.0;
				if (this.filterLabel != null) {
					weight = 1.0;
				} else if (this.mode == Mode.MACRO_WEIGHTED) {
					if (Double.compare(tp + fp + tn + fn, 0.0) == 0)
						weight = 1.0;
					else
						weight = (tp+fn)/(tp+fp+tn+fn);
				} else {
					weight = 1.0/stats.size();
				}
				
				if (Double.compare(tp + fp, 0.0) == 0)
					p += weight;
				else
					p += weight * tp/(tp+fp);
			}
		}
		
		if (this.mode == Mode.MICRO) {
			this.microNumerator = (int)num;
			this.microDenominator = (int)den;
		}
		
		if (den == 0.0) {
			num = 1.0;
			den = 1.0;
		} 
		
		return (this.mode == Mode.MICRO) ? num/den : p;
	}
	
	@Override
	public int computeSampleSize(boolean forceRecompute) {
		Map<L, Map<Stat, Integer>> stats =  this.task.computeStats(this.method, forceRecompute);

		int n = 0;
		
		for (Entry<L, Map<Stat, Integer>> entry : stats.entrySet()) {
			if (this.filterLabel != null && !this.filterLabel.equals(entry.getKey()))
				continue;
			
			n += entry.getValue().get(Stat.TRUE_POSITIVE);
			n += entry.getValue().get(Stat.FALSE_POSITIVE);
		}
		
		return n;
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
		else if (parameter.equals("filterLabel"))
			return (this.filterLabel == null) ? null : Obj.stringValue(this.filterLabel.toString());
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
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
		return "Precision";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasurePrecision<D, L>(this.context);
	}
	
	@Override
	public EvaluationClassification<D, L, ?> makeInstance(DatumContext<D, L> context) {
		return new EvaluationClassificationMeasurePrecision<D, L>(context);
	}
	
	@Override
	public String toString() {
		return getReferenceName() + ":\t" + compute() + ((this.mode == Mode.MICRO) ? "\t(" + this.microNumerator + "/" + this.microDenominator + ")" : "");
	}
}
