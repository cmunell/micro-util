package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.structure.FnStructure;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.Trainable;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Triple;

public class MethodMultiClassificationPrecedenceScore extends MethodMultiClassification implements MultiTrainable {
	private List<MethodClassification<?, ?>> methods;
	private List<Structurizer<?, ?, ?>> structurizers;
	private Fn<?, ?> structureTransformFn;
	private boolean trainOnInit = false;
	private int trainIters = 10;
	private boolean trainStructured = false;
	private boolean threadStructure = true;
	private String[] parameterNames = { "methods", "structurizers", "structureTransformFn", "trainOnInit", "trainIters", "trainStructured", "threadStructure" };
	
	private boolean initialized = false;
	
	public MethodMultiClassificationPrecedenceScore() {
		
	}
	
	public MethodMultiClassificationPrecedenceScore(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("methods")) {
			if (this.methods == null)
				return null;
			Obj.Array array = Obj.array();
			for (MethodClassification<?, ?> method : this.methods)
				array.add(Obj.curlyBracedValue(method.getReferenceName()));
			return array;
		} else if (parameter.equals("structurizers")) {
			if (this.structurizers == null)
				return null;
			Obj.Array array = Obj.array();
			for (Structurizer<?, ?, ?> structurizer : this.structurizers)
				array.add(Obj.curlyBracedValue(structurizer.getReferenceName()));
			return array;
		} else if (parameter.equals("structureTransformFn")) {
			return (this.structureTransformFn == null) ? null : this.structureTransformFn.toParse();
		} else if (parameter.equals("trainOnInit")) {
			return Obj.stringValue(String.valueOf(this.trainOnInit));
		} else if (parameter.equals("trainIters")) {
			return Obj.stringValue(String.valueOf(this.trainIters));
		} else if (parameter.equals("trainStructured")) {
			return Obj.stringValue(String.valueOf(this.trainStructured));
		} else if (parameter.equals("threadStructure")) {
			return Obj.stringValue(String.valueOf(this.threadStructure));
		}
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("methods")) {
			if (parameterValue != null) {
				this.methods = new ArrayList<MethodClassification<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.methods.add((MethodClassification<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("structurizers")) {
			if (parameterValue != null) {
				this.structurizers = new ArrayList<Structurizer<?, ?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.structurizers.add((Structurizer<?, ?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("structureTransformFn")) {
			this.structureTransformFn = (parameterValue == null) ? null : this.context.getMatchOrConstructStructureFn(parameterValue);
		} else if (parameter.equals("trainOnInit")) {
			this.trainOnInit = (parameterValue == null) ? null : Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("trainIters")) {
			this.trainIters = (parameterValue == null) ? this.trainIters : Integer.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("trainStructured")) {
			this.trainStructured = (parameterValue == null) ? false : Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("threadStructure")) {
			this.threadStructure = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else {
			return false;
		}
		return true;
	}

	@Override
	public List<Map<Datum<?>, ?>> classify(List<DataSet<?, ?>> data) {
		return classify(data, false);
	}
	
	// FIXME Extra pass over the data is unnecessary
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Map<Datum<?>, ?>> classify(List<DataSet<?, ?>> data, boolean recomputeOrderingMeasures) {
		List<Map<Datum<?>, Pair<?, Double>>> labelsWithScores = classifyWithScore(data, recomputeOrderingMeasures);
		List<Map<Datum<?>, ?>> labels = new ArrayList<>();
		for (Map<Datum<?>, Pair<?, Double>> map : labelsWithScores) {
			Map labelMap = new HashMap();
			for (Entry<Datum<?>, Pair<?, Double>> entry : map.entrySet()) {
				labelMap.put(entry.getKey(), entry.getValue().getFirst());
			}
			labels.add(labelMap);
		}
		
		return labels;
	}
	
	public List<Map<Datum<?>, Pair<?, Double>>> classifyWithScore(List<DataSet<?, ?>> data) {
		return classifyWithScore(data, false);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List<Map<Datum<?>, Pair<?, Double>>> classifyWithScore(List<DataSet<?, ?>> data, boolean recomputeOrderingMeasures) {
		Map<String, ?> structures = null;
		if (this.structurizers.size() > 0)
			structures = this.structurizers.get(0).makeStructures();
		
		List<Pair<Integer, Triple<Datum, Object, Double>>> orderedPredictions = new ArrayList<>();
		for (int i = 0; i < this.methods.size(); i++) {
			MethodClassification<?, ?> method = this.methods.get(i);
			this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) classifying with method " + method.getReferenceName());
			
			for (int j = 0; j < data.size(); j++) {
				if (!method.matchesData(data.get(j)))
					continue;
				Map<?, Pair<?, Double>> scoredDatums = (Map)method.classifyWithScore((DataSet)data.get(j));
				for (Entry<?, Pair<?, Double>> entry : scoredDatums.entrySet())
					orderedPredictions.add(
							new Pair<>(i,
							new Triple<Datum, Object, Double>((Datum)entry.getKey(), entry.getValue().getFirst(), entry.getValue().getSecond())));
			}
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) sorting predictions");
		
		Collections.sort(orderedPredictions, new Comparator<Pair<Integer, Triple<Datum, Object, Double>>>() {
			@Override
			public int compare(Pair<Integer, Triple<Datum, Object, Double>> o1, Pair<Integer, Triple<Datum, Object, Double>> o2) {
				return Double.compare(o2.getSecond().getThird(), o1.getSecond().getThird());
			}
		});
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) adding predictions to structures");
		
		Map<String, Collection<WeightedStructure>> changes = new HashMap<String, Collection<WeightedStructure>>();
		double transformCount = 0;
		for (Pair<Integer, Triple<Datum, Object, Double>> pair : orderedPredictions) {
			int methodIndex = pair.getFirst();
			Triple<Datum, Object, Double> prediction = pair.getSecond();
			Datum datum = prediction.getFirst();
			Object label = prediction.getSecond();
			Double score = prediction.getThird();
			Structurizer structurizer = this.structurizers.get(methodIndex);
			
			int changePartitionSize = changes.size();
			boolean changed = structurizer.addToStructures(datum, label, score, structures, changes);

			// Change made in same part, so run transformations
			if (changed && changes.size() == changePartitionSize) {
				if (this.threadStructure) {
					final Map<String, Collection<WeightedStructure>> finalChanges = changes;
					ThreadMapper<Entry, Boolean> threads = new ThreadMapper<Entry, Boolean>(new ThreadMapper.Fn<Entry, Boolean>() {
						@Override
						public Boolean apply(Entry entry) {
							List transformedStructures = ((FnStructure)structureTransformFn).listCompute((WeightedStructure)entry.getValue(), finalChanges.get(entry.getKey()));
							WeightedStructure firstTransformed = (WeightedStructure)transformedStructures.get(0);
							for (int j = 1; j < transformedStructures.size(); j++)
								firstTransformed = firstTransformed.merge((WeightedStructure)transformedStructures.get(j));		
							entry.setValue(firstTransformed);
							return true;
						}
						
					});
					threads.run((Set)structures.entrySet(), this.context.getMaxThreads());
				} else {
					for (Entry entry : structures.entrySet()) {
						List transformedStructures = ((FnStructure)structureTransformFn).listCompute((WeightedStructure)entry.getValue(), changes.get(entry.getKey()));
						WeightedStructure firstTransformed = (WeightedStructure)transformedStructures.get(0);
						for (int j = 1; j < transformedStructures.size(); j++)
							firstTransformed = firstTransformed.merge((WeightedStructure)transformedStructures.get(j));		
						entry.setValue(firstTransformed);
					}
				}
				
				transformCount++;
				changes = new HashMap<String, Collection<WeightedStructure>>();
			}
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) finished structuring data (ran " + transformCount + " transforms (" + (orderedPredictions.size()/transformCount) + " predictions per transform))");
		this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) pulling labeled data out of structures...");

		List<Map<Datum<?>, Pair<?, Double>>> classifications = new ArrayList<Map<Datum<?>, Pair<?, Double>>>();
		for (int i = 0; i < data.size(); i++) {
			Structurizer structurizer = null;
			for (int j = 0; j < this.structurizers.size(); j++) {
				if (this.structurizers.get(j).matchesData(data.get(i))) {
					structurizer = this.structurizers.get(j);
					break;
				}
			}
			
			DataSet dataSet = data.get(i);
			Map labeledData = new HashMap();
			for (Object o : dataSet) {
				Datum d = (Datum)o;
				
				Map<Object, Double> labelsWeighted = structurizer.getLabels(d, structures);
				if (labelsWeighted == null)
					continue;
				
				Object maxLabel = null;
				double maxWeight = Double.NEGATIVE_INFINITY;
				for (Entry<Object, Double> entry : labelsWeighted.entrySet()) {
					if (Double.compare(entry.getValue(), maxWeight) >= 0) {
						maxLabel = entry.getKey();
						maxWeight = entry.getValue();
					}
				}
				
				if (maxLabel != null)
					labeledData.put(d, new Pair(maxLabel, maxWeight));
			}
			classifications.add(labeledData);
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Multi-method (precedence score) finished classifying data");
		
		return classifications;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean init(List<DataSet<?, ?>> testData) {
		if (this.initialized)
			return true;
		
		for (int i = 0; i < testData.size(); i++) {
			this.methods.get(i).init((DataSet)testData.get(i));
		}
	
		this.initialized = true;
		
		if (this.trainOnInit)
			return train();
		else
			return true;
	}

	@Override
	public MethodMultiClassification clone() {
		MethodMultiClassificationPrecedenceScore clone = new MethodMultiClassificationPrecedenceScore(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		
		clone.methods = new ArrayList<MethodClassification<?, ?>>();
		
		for (MethodClassification<?, ?> method : this.methods) {
			clone.methods.add(method.clone());
		}
		
		return clone;
	}

	@Override
	public MethodMultiClassification makeInstance(Context context) {
		return new MethodMultiClassificationPrecedenceScore(context);
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
		return "PrecedenceScore";
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean train() {
		if (!this.trainStructured) {
			List<Trainable<?, ?>> trainables = new ArrayList<>(getTrainableMethods().keySet()); 
			for (Trainable<?, ?> trainable : trainables)
				if (!trainable.train())
					return false;
		} else {
			List<Trainable<?, ?>> trainables = new ArrayList<>(getTrainableMethods().keySet()); 
			List<DataSet<?, ?>> data = new ArrayList<>();
			for (Trainable<?, ?> trainable : trainables)
				data.add(trainable.getTrainData());
			
			
			for (int i = 0; i < this.trainIters; i++) {
				List<Map<Datum<?>, ?>> classifiedData = classify(data, true);
				
				for (int j = 0; j < trainables.size(); j++) {
					Map<Datum<?>, ?> c = classifiedData.get(j);
					if (!trainables.get(j).iterateTraining((Map)c))
						return false;
				}
			}
		}
		
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean setTrainData(List<DataSet<?, ?>> data) {
		Map<Trainable<?, ?>, Integer> trainables = getTrainableToDataIndices(data);
		for (Entry<Trainable<? ,?>, Integer> entry : trainables.entrySet())
			if (!entry.getKey().setTrainData((DataSet)data.get(entry.getValue())))
				return false;
		return true;
	}

	@Override
	public List<DataSet<?, ?>> getTrainData() {
		List<DataSet<?, ?>> data = new ArrayList<>();
		Map<Trainable<?, ?>, List<MethodClassification<?, ?>>> trainables = getTrainableMethods();
		for (Entry<Trainable<?, ?>, List<MethodClassification<?, ?>>> entry : trainables.entrySet()) {
			data.add(entry.getKey().getTrainData());
		}
		
		return data;
	}
	
	private Map<Trainable<?, ?>, Integer> getTrainableToDataIndices(List<DataSet<?, ?>> data) {
		Map<Trainable<?, ?>, List<MethodClassification<?, ?>>> trainables = getTrainableMethods();
		Map<Trainable<?, ?>, Integer> indices = new HashMap<>();
		
		for (Entry<Trainable<?, ?>, List<MethodClassification<?, ?>>> entry : trainables.entrySet()) {
			for (MethodClassification<?, ?> method : entry.getValue()) {
				boolean foundIndex = false;
				for (int i = 0; i < data.size(); i++) {
					if (method.matchesData(data.get(i))) {
						indices.put(entry.getKey(), i);
						foundIndex = true;
						break;
					}
				}
				
				if (foundIndex)
					break;
			}
		}
		
		return indices;
	}
	
	// Map from trainable to list of methods that use that trainable
	private Map<Trainable<?, ?>, List<MethodClassification<?, ?>>> getTrainableMethods() {
		Map<Trainable<?, ?>, List<MethodClassification<?, ?>>> trainableMethods = new HashMap<>();
		
		for (int i = 0; i < this.methods.size(); i++) {
			if (this.methods.get(i).hasTrainable()) {
				if (!trainableMethods.containsKey(this.methods.get(i).getTrainable()))
					trainableMethods.put(this.methods.get(i).getTrainable(), new ArrayList<>());
				trainableMethods.get(this.methods.get(i).getTrainable()).add(this.methods.get(i));
			}
		}
		
		return trainableMethods;
	}

	@Override
	public boolean hasTrainable() {
		return true;
	}

	@Override
	public MultiTrainable getTrainable() {
		return this;
	}
}
