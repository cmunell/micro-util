package edu.cmu.ml.rtw.generic.task.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class TaskClassification<D extends Datum<L>, L> extends CtxParsableFunction {
	public enum Stat {
		TRUE_POSITIVE,
		FALSE_POSITIVE,
		TRUE_NEGATIVE,
		FALSE_NEGATIVE,
	}
	
	protected DataSet<D, L> data;
	protected String[] parameterNames = { "data" };
	
	protected Map<MethodClassification<D, L>, Map<L, Map<L, List<D>>>> methodsActualToPredicted;
	protected boolean initialized = false;
	protected DatumContext<D, L> context;
	
	public TaskClassification(DatumContext<D, L> context) {
		this.context = context;
		this.methodsActualToPredicted = new HashMap<MethodClassification<D, L>, Map<L, Map<L, List<D>>>>();
	}
	
	public boolean init() {
		if (this.initialized)
			return true;
		
		if (this.data.isBuildable() && !this.data.isBuilt() && !this.data.build())
			return false;
		
		this.initialized = true;
		return true;
	}
	
	public DataSet<D, L> getData() {
		if (!init())
			return null;
		return this.data;
	}
	
	public Map<L, Map<L, List<D>>> computeActualToPredictedData(MethodClassification<D, L> method) {
		if (!init())
			return null;
		
		if (this.methodsActualToPredicted.containsKey(method))
			return this.methodsActualToPredicted.get(method);
		
		Map<L, Map<L, List<D>>> actualToPredicted = new HashMap<L, Map<L, List<D>>>();
		Map<D, L> predictions = method.classify(getData());
		
		for (D datum : this.data) {
			L actual = datum.getLabel();
			L predicted = predictions.get(datum);
			
			if (!actualToPredicted.containsKey(actual))
				actualToPredicted.put(actual, new HashMap<L, List<D>>());
			if (!actualToPredicted.get(actual).containsKey(predicted))
				actualToPredicted.get(actual).put(predicted, new ArrayList<D>());
			actualToPredicted.get(actual).get(predicted).add(datum);
		}
		
		this.methodsActualToPredicted.put(method, actualToPredicted);
		return actualToPredicted;
	}
	
	public Map<L, Map<Stat, Integer>> computeStats(MethodClassification<D, L> method) {
		Map<L, Map<L, List<D>>> actualToPredicted = computeActualToPredictedData(method);
		Map<L, Map<Stat, Integer>> stats = new HashMap<L, Map<Stat, Integer>>();

		for (Entry<L, Map<L, List<D>>> entry : actualToPredicted.entrySet()) {
			L actual = entry.getKey();
			
			for (Entry<L, List<D>> entry2 : entry.getValue().entrySet()) {
				L predicted = entry2.getKey();
				int count = entry2.getValue().size();
				
				if (actual.equals(predicted)) {
					incrementStat(stats, predicted, Stat.TRUE_POSITIVE, count);
					for (L label : actualToPredicted.keySet())
						if (!label.equals(predicted))
							incrementStat(stats, label, Stat.TRUE_NEGATIVE, count);
				} else {
					if (predicted != null)
						incrementStat(stats, predicted, Stat.FALSE_POSITIVE, count);
					incrementStat(stats, actual, Stat.FALSE_NEGATIVE, count);
				}
			}
		}

		return stats;
	}
	
	private void incrementStat(Map<L, Map<Stat, Integer>> stats, L label, Stat stat, int count) {
		if (!stats.containsKey(label)) {
			stats.put(label, new HashMap<Stat, Integer>());
			for (Stat s : Stat.values())
				stats.get(label).put(s, 0);	
		}
		
		stats.get(label).put(stat, stats.get(label).get(stat) + count);
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("data")) 
			return (this.data == null) ? null : Obj.curlyBracedValue(this.data.getReferenceName());
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("data"))
			this.data = (parameterValue == null) ? null : this.context.getMatchDataSet(parameterValue);
		else 
			return false;
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
		return "Classification";
	}
}
