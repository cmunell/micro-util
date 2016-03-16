package edu.cmu.ml.rtw.generic.task.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
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
	
	protected DataFeatureMatrix<D, L> data;
	protected boolean precomputeFeatures = false;
	protected String[] parameterNames = {"data", "precomputeFeatures"};
	
	protected Map<MethodClassification<D, L>, Map<L, Map<L, List<D>>>> methodsActualToPredicted;
	protected boolean initialized = false;
	protected DatumContext<D, L> context;
	
	public TaskClassification(DatumContext<D, L> context) {
		this.context = context;
		this.methodsActualToPredicted = new HashMap<MethodClassification<D, L>, Map<L, Map<L, List<D>>>>();
	}
	
	public boolean init() {
		if (!this.data.isInitialized() && !this.data.init())
			return false;
		
		if (this.precomputeFeatures && !this.data.isPrecomputed())
			if (!this.data.precompute())
				return false;
		
		this.initialized = true;
		return true;
	}
	
	public DataFeatureMatrix<D, L> getData() {
		if (!this.initialized && !init())
			return null;
		return this.data;
	}
	
	public Map<L, Map<L, List<D>>> computeActualToPredictedData(MethodClassification<D, L> method) {
		if (!this.initialized && !init())
			return null;
		
		if (this.methodsActualToPredicted.containsKey(method))
			return this.methodsActualToPredicted.get(method);
		
		Map<L, Map<L, List<D>>> actualToPredicted = new HashMap<L, Map<L, List<D>>>();
		Map<D, L> predictions = method.classify(getData());
		
		for (D datum : this.data.getData()) {
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
					if (predicted != null) {
						for (L label : actualToPredicted.keySet())
							if (!label.equals(predicted))
								incrementStat(stats, label, Stat.TRUE_NEGATIVE, count);
					}
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
		else if (parameter.equals("precomputeFeatures"))
			return Obj.stringValue(String.valueOf(this.precomputeFeatures));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("data"))
			this.data = (parameterValue == null) ? null : this.context.getMatchDataFeatures(parameterValue);
		else if (parameter.equals("precomputeFeatures"))
			return Boolean.valueOf(this.context.getMatchValue(parameterValue));
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
