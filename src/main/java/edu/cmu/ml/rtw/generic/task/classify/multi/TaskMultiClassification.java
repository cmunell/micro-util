package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification.Stat;

public class TaskMultiClassification extends CtxParsableFunction {
	protected List<TaskClassification<?, ?>> tasks;
	protected String[] parameterNames = { "tasks" };
	
	protected Map<MethodMultiClassification, List<Map<?, Map<?, List<?>>>>> methodsActualToPredicted;
	protected boolean initialized = false;
	protected Context context;
	
	public TaskMultiClassification(Context context) {
		this.context = context;
		this.methodsActualToPredicted = new HashMap<MethodMultiClassification, List<Map<?, Map<?, List<?>>>>>();
	}
	
	public boolean isInitialized() {
		return this.initialized;
	}
	
	public boolean init() {
		if (this.initialized)
			return true;
		
		for (TaskClassification<?, ?> task : this.tasks)
			if (!task.init())
				return false;
		
		this.initialized = true;
		return true;
	}
	
	public int getTaskCount() {
		return this.tasks.size();
	}
	
	public TaskClassification<?, ?> getTask(int index) {
		return this.tasks.get(index);
	}
	
	public List<DataSet<?, ?>> getData() {
		if (!init())
			return null;
		List<DataSet<?, ?>> data = new ArrayList<>();
		for (TaskClassification<?, ?> task : this.tasks)
			data.add(task.getData());
		return data;
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<?, Map<?, List<?>>>> computeActualToPredictedData(MethodMultiClassification method) {
		if (!this.initialized && !init())
			return null;
		if (this.methodsActualToPredicted.containsKey(method))
			return this.methodsActualToPredicted.get(method);
		
		List<Map<Datum<?>, ?>> predictionMaps = method.classify(this);
		List<Map<?, Map<?, List<?>>>> actualToPredictedMaps = new ArrayList<Map<?, Map<?, List<?>>>>();
		for (int i = 0; i < predictionMaps.size(); i++) {
			Map<?, ?> predictions = predictionMaps.get(i);
			DataSet<?, ?> data = this.tasks.get(i).getData();
			Map<Object, Map<?, List<?>>> actualToPredicted = new HashMap<Object, Map<?, List<?>>>();
			for (Datum<?> datum : data) {
				Object actual = datum.getLabel();
				Object predicted = predictions.get(datum);
				
				if (!actualToPredicted.containsKey(actual))
					actualToPredicted.put(datum.getLabel(), (Map<?, List<?>>)(new HashMap<Object, List<?>>()));
				if (!actualToPredicted.get(actual).containsKey(predicted)) {
					Map<Object, List<?>> predictedMap = (Map<Object, List<?>>)actualToPredicted.get(actual);
					predictedMap.put(predicted, new ArrayList<Object>());
				}
				
				List<Object> datumList = (List<Object>)actualToPredicted.get(actual).get(predicted);
				datumList.add(datum);
			}
			
			actualToPredictedMaps.add((Map<?, Map<?, List<?>>>)actualToPredicted);
		}
		
		this.methodsActualToPredicted.put(method, actualToPredictedMaps);
		
		return actualToPredictedMaps;
	}
	
	public List<Map<?, Map<Stat, Integer>>> computeStats(MethodMultiClassification method) {
		List<Map<?, Map<?, List<?>>>> actualToPredictedMaps = computeActualToPredictedData(method);
		List<Map<?, Map<Stat, Integer>>> statsMaps = new ArrayList<Map<?, Map<Stat, Integer>>>();
		
		for (Map<?, Map<?, List<?>>> actualToPredicted : actualToPredictedMaps) {
			Map<Object, Map<Stat, Integer>> stats = new HashMap<Object, Map<Stat, Integer>>();
			for (Entry<?, Map<?, List<?>>> entry : actualToPredicted.entrySet()) {
				Object actual = entry.getKey();
				for (Entry<?, List<?>> entry2 : entry.getValue().entrySet()) {
					Object predicted = entry2.getKey();
					int count = entry2.getValue().size();
					
					if (actual.equals(predicted)) {
						incrementStat(stats, predicted, Stat.TRUE_POSITIVE, count);
						for (Object label : actualToPredicted.keySet())
							if (!label.equals(predicted))
								incrementStat(stats, label, Stat.TRUE_NEGATIVE, count);
					} else {
						if (predicted != null)
							incrementStat(stats, predicted, Stat.FALSE_POSITIVE, count);
						incrementStat(stats, actual, Stat.FALSE_NEGATIVE, count);
					}
				}
			}
			
			statsMaps.add(stats);
		}
	
		return statsMaps;
	}
	
	private void incrementStat(Map<Object, Map<Stat, Integer>> stats, Object label, Stat stat, int count) {
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
		if (parameter.equals("tasks")) {
			if (this.tasks == null)
				return null;
			Obj.Array array = Obj.array();
			for (TaskClassification<?, ?> task : this.tasks)
				array.add(Obj.curlyBracedValue(task.getReferenceName()));
			return array;
		}
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("tasks")) {
			if (parameterValue != null) {
				this.tasks = new ArrayList<TaskClassification<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.tasks.add((TaskClassification<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else 
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
		return "MultiClassification";
	}
}
