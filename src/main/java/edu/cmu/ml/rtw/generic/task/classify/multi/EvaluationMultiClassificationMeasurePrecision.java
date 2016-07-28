package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class EvaluationMultiClassificationMeasurePrecision extends EvaluationMultiClassificationMeasure {
	public enum Mode {
		MACRO,
		MACRO_WEIGHTED,
		MICRO
	}
	
	private Mode mode = Mode.MACRO_WEIGHTED;
	private List<String> filterLabels;
	private List<String> filterTasks;
	private String[] parameterNames = { "mode", "filterLabels", "filterTasks" };
	
	private int microNumerator;
	private int microDenominator;
	
	public EvaluationMultiClassificationMeasurePrecision() {
		this(null);
	}
	
	public EvaluationMultiClassificationMeasurePrecision(Context context) {
		super(context);
	}

	@Override
	public Double compute(boolean forceRecompute) {
		List<Map<?, Map<Stat, Integer>>> stats =  this.task.computeStats(this.method, forceRecompute);
		
		double p = 0.0;
		double num = 0.0;
		double den = 0.0;
		int i = 0;
		for (Map<?, Map<Stat, Integer>> stat : stats) {
			String filterLabel = null;
			if (this.filterLabels != null)
				if (this.filterLabels.get(i).length() > 0)
					filterLabel = this.filterLabels.get(i);
			
			if (this.filterTasks != null)
				if (!this.filterTasks.contains(String.valueOf(i))) {
					i++;
					continue;
				}
			
			for (Entry<?, Map<Stat, Integer>> entry : stat.entrySet()) {		
				if (filterLabel != null && !filterLabel.equals(entry.getKey().toString()))
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
					
					if (filterLabel != null) {
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
			
			i++;
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
		else if (parameter.equals("filterLabels"))
			return this.filterLabels == null ? null : Obj.array(this.filterLabels);
		else if (parameter.equals("filterTasks"))
			return this.filterTasks == null ? null : Obj.array(this.filterTasks);
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filterLabels"))
			this.filterLabels = (parameterValue == null) ? null : this.context.getMatchArray(parameterValue);
		else if (parameter.equals("filterTasks"))
			this.filterTasks = (parameterValue == null) ? null : this.context.getMatchArray(parameterValue);
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
		return "MultiPrecision";
	}

	@Override
	protected EvaluationMultiClassificationMeasure makeInstance() {
		return new EvaluationMultiClassificationMeasurePrecision(this.context);
	}
	
	@Override
	public EvaluationMultiClassification<Double> makeInstance(Context context) {
		return new EvaluationMultiClassificationMeasurePrecision(context);
	}
	
	@Override
	public String toString() {
		return getReferenceName() + ":\t" + compute() + ((this.mode == Mode.MICRO) ? "\t(" + this.microNumerator + "/" + this.microDenominator + ")" : "");
	}
}
