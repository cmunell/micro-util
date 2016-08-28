package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.Structurizer;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.structure.FnStructure;
import edu.cmu.ml.rtw.generic.structure.WeightedStructure;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasure;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.Trainable;
import edu.cmu.ml.rtw.generic.task.classify.meta.PredictionClassification;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Singleton;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Triple;

public class MethodMultiClassificationRandomSieve extends MethodMultiClassification implements MultiTrainable {
	private List<MethodClassification<?, ?>> methods;
	private List<Structurizer<?, ?, ?>> structurizers;
	private List<EvaluationClassificationMeasure<?, ?>> permutationMeasures;
	private Fn<?, ?> structureTransformFn;
	private boolean trainOnInit = false;
	private boolean threadStructure = true;
	private double z = 1.0;
	private int inferenceIters = 15;
	private String[] parameterNames = { "methods", "structurizers", "permutationMeasures", "structureTransformFn", "trainOnInit", "threadStructure", "z", "inferenceIters" };
	
	private boolean initialized = false;
	
	public MethodMultiClassificationRandomSieve() {
		
	}
	
	public MethodMultiClassificationRandomSieve(Context context) {
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
		} else if (parameter.equals("permutationMeasures")) {
			if (this.permutationMeasures == null)
				return null;
			Obj.Array array = Obj.array();
			for (EvaluationClassificationMeasure<?, ?> measure : this.permutationMeasures)
				array.add(Obj.curlyBracedValue(measure.getReferenceName()));
			return array;
		} else if (parameter.equals("structureTransformFn")) {
			return (this.structureTransformFn == null) ? null : this.structureTransformFn.toParse();
		} else if (parameter.equals("trainOnInit")) {
			return Obj.stringValue(String.valueOf(this.trainOnInit));
		} else if (parameter.equals("threadStructure")) {
			return Obj.stringValue(String.valueOf(this.threadStructure));
		} else if (parameter.equals("z")) {
			return Obj.stringValue(String.valueOf(this.z));
		} else if (parameter.equals("inferenceIters")) {
			return Obj.stringValue(String.valueOf(this.inferenceIters));
		}
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("methods")) {
			if (parameterValue != null) {
				this.methods = new ArrayList<MethodClassification<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++) {
					this.methods.add((MethodClassification<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
				}
			}
		} else if (parameter.equals("structurizers")) {
			if (parameterValue != null) {
				this.structurizers = new ArrayList<Structurizer<?, ?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.structurizers.add((Structurizer<?, ?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("permutationMeasures")) {
			if (parameterValue != null) {
				this.permutationMeasures = new ArrayList<EvaluationClassificationMeasure<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.permutationMeasures.add((EvaluationClassificationMeasure<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("structureTransformFn")) {
			this.structureTransformFn = (parameterValue == null) ? null : this.context.getMatchOrConstructStructureFn(parameterValue);
		} else if (parameter.equals("trainOnInit")) {
			this.trainOnInit = (parameterValue == null) ? null : Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("threadStructure")) {
			this.threadStructure = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("z")) {
			this.z = Double.valueOf(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("inferenceIters")) {
			this.inferenceIters = Integer.valueOf(this.context.getMatchValue(parameterValue));
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
	
	
	public List<Map<Datum<?>, Pair<?, Double>>> classifyWithScore(List<DataSet<?, ?>> data, boolean recomputeOrderingMeasures) {
		double maxWeightedPredictionSum = Double.NEGATIVE_INFINITY;
		List<Map<Datum<?>, Pair<?, Double>>> bestClassifications = null;
		List<Triple<Integer, Double, Double>> stats = new ArrayList<>();
 		for (int i = 0; i < this.inferenceIters; i++) {
			Triple<Integer, Double, List<Map<Datum<?>, Pair<?, Double>>>> possibleClassifications = computePossibleClassificationWithScore(data, recomputeOrderingMeasures);
			int transformPredictions = possibleClassifications.getFirst();
			double weightedPredictionSum = possibleClassifications.getSecond();
			
			double correct = 0.0;
			double total = 0.0;
			for (Map<Datum<?>, Pair<?, Double>> predictions : possibleClassifications.getThird()) {
				for (Entry<Datum<?>, Pair<?, Double>> entry : predictions.entrySet()) {
					if (entry.getKey().getLabel() != null) {
						correct += entry.getValue().getFirst().equals(entry.getKey().getLabel()) ? 1.0 : 0.0;
						total++;
					}
				}
			}
			
			if (weightedPredictionSum >= maxWeightedPredictionSum) {
				maxWeightedPredictionSum = weightedPredictionSum;
				bestClassifications = possibleClassifications.getThird();
			}
			
			stats.add(new Triple<>(transformPredictions, weightedPredictionSum, Double.compare(total, 0.0) != 0 ? correct/total : 0.0));

			this.context.getDataTools().getOutputWriter().debugWriteln(
					"RandomSieve iteration " + i + 
					" (" + transformPredictions + 
					", " + weightedPredictionSum + 
					", " + (Double.compare(total, 0.0) != 0 ? correct/total : 0.0) +
					")");
			
			recomputeOrderingMeasures = false;
		}
		
 		stats.sort(new Comparator<Triple<Integer, Double, Double>>() {
			@Override
			public int compare(Triple<Integer, Double, Double> o1,
					Triple<Integer, Double, Double> o2) {
				return o1.getThird().compareTo(o2.getThird());
			}
 		});
 		
		context.getDataTools().getOutputWriter().debugWriteln("Sieve Stats");
		context.getDataTools().getOutputWriter().debugWriteln("Transformations\tExpectation\tPrecision");
		for (Triple<Integer, Double, Double> stat : stats) {
			context.getDataTools().getOutputWriter().debugWriteln(stat.getFirst() + "\t" + stat.getSecond() + "\t" + stat.getThird());
		}
 		
		return bestClassifications;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Triple<Integer, Double, List<Map<Datum<?>, Pair<?, Double>>>> computePossibleClassificationWithScore(List<DataSet<?, ?>> data, boolean recomputeOrderingMeasures) {
		final Map<String, ?> structures = this.structurizers.get(0).makeStructures();
		
		// Map structures to lists of predictions
		List<Triple<Integer, PredictionClassification, Double>> scoredPredictions = getPossiblePredictionOrdering(this.context.getDataTools().getGlobalRandom(), data, recomputeOrderingMeasures);
		Map<String, List<Triple<Integer, PredictionClassification, Double>>> structurePredictions = new HashMap<>();
		for (Triple<Integer, PredictionClassification, Double> scoredPrediction : scoredPredictions) {
			int methodIndex = scoredPrediction.getFirst();
			PredictionClassification prediction = scoredPrediction.getSecond();
			Structurizer structurizer = this.structurizers.get(methodIndex);
			String structureId = structurizer.getStructureId(prediction.getDatum(), prediction.getLabel(), structures);
			
			if (!structurePredictions.containsKey(structureId))
				structurePredictions.put(structureId, new ArrayList<>());
			structurePredictions.get(structureId).add(scoredPrediction);
		}
		
		int transformPredictions = 0;
		double weightedPredictionSum = 0.0;
		if (this.threadStructure) {
			Singleton<Integer> sCount = new Singleton<>(0);
			Singleton<Integer> sTransformPredictions = new Singleton<>(0);
			Singleton<Double> sWeightedPredictionSum = new Singleton<>(0.0);
			ThreadMapper<Entry<String, List<Triple<Integer, PredictionClassification, Double>>>, Boolean> threads = 
					new ThreadMapper<Entry<String, List<Triple<Integer, PredictionClassification, Double>>>, Boolean>(
						new ThreadMapper.Fn<Entry<String, List<Triple<Integer, PredictionClassification, Double>>>, Boolean>() {
				@Override
				public Boolean apply(Entry<String, List<Triple<Integer, PredictionClassification, Double>>> entry) {
					Pair<Integer, Double> result = structurePredictions(entry.getValue(), (Map<String, WeightedStructure>)structures);
					synchronized (sCount) {
						sCount.set(sCount.get() + 1);
						sTransformPredictions.set(sTransformPredictions.get() + result.getFirst());
						sWeightedPredictionSum.set(sWeightedPredictionSum.get() + result.getSecond());
					}
					return true;
				}	
			});
			
			threads.run(structurePredictions.entrySet(), this.context.getMaxThreads());
			
			transformPredictions = sTransformPredictions.get();
			weightedPredictionSum = sWeightedPredictionSum.get();
		} else {
			for (Entry<String, List<Triple<Integer, PredictionClassification, Double>>> entry : structurePredictions.entrySet()) {
				Pair<Integer, Double> result = structurePredictions(entry.getValue(), (Map<String, WeightedStructure>)structures);
				transformPredictions += result.getFirst();
				weightedPredictionSum += result.getSecond();
			}
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Sieve pulling labeled data out of structures...");

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
		
		return new Triple<>(transformPredictions, weightedPredictionSum, classifications);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List<Triple<Integer, PredictionClassification, Double>> getPossiblePredictionOrdering(Random r, List<DataSet<?, ?>> data, boolean recomputeMeasures) {	
		List<Triple<Integer, PredictionClassification, Double>> ordering = new ArrayList<>();
		for (int i = 0; i < this.methods.size(); i++) {
			double methodScore = this.permutationMeasures.get(i).compute(recomputeMeasures);
			MethodClassification<?, ?> method = this.methods.get(i);
			
			for (int j = 0; j < data.size(); j++) {
				if (!method.matchesData(data.get(j)))
					continue;
				Map<?, PredictionClassification> predictions = (Map)method.predict((DataSet)data.get(j));
				double n = this.permutationMeasures.get(i).computeSampleSize(false);
				
				if (n == 0) {
					for (PredictionClassification prediction : predictions.values()) {
						double score = r.nextDouble();
						ordering.add(new Triple<Integer,PredictionClassification, Double>(i, prediction, score));
					}
				} else {
					for (PredictionClassification prediction : predictions.values()) {
						double halfInterval = this.z * Math.sqrt(methodScore * (1.0 - methodScore)/n);
						double score = methodScore - halfInterval + 2.0 * halfInterval * r.nextDouble();
						score = Math.max(Math.min(score, 1.0), 0.0);
						ordering.add(new Triple<Integer, PredictionClassification, Double>(i, prediction, score));
					}
				}
			}
		}
		
		ordering.sort(new Comparator<Triple<Integer, PredictionClassification, Double>>() {
			@Override
			public int compare(Triple<Integer, PredictionClassification, Double> o1,
					Triple<Integer, PredictionClassification, Double> o2) {
				return Double.compare(o2.getThird(), o1.getThird());
			}
		});
		
		return ordering;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Pair<Integer, Double> structurePredictions(List<Triple<Integer, PredictionClassification, Double>> predictions, Map<String, WeightedStructure> structures) {
		boolean skippedStructure = false;
		double prevScore = Double.POSITIVE_INFINITY;
		Map<String, Collection<WeightedStructure>> changes = new HashMap<String, Collection<WeightedStructure>>();
		int transformPredictions = 0;
		double weightedPredictionSum = 0.0;
		for (Triple<Integer, PredictionClassification, Double> prediction : predictions) {
			int methodIndex = prediction.getFirst();
			Datum datum = prediction.getSecond().getDatum();
			Object label = prediction.getSecond().getLabel();
			Double score = prediction.getThird();
			Structurizer structurizer = this.structurizers.get(methodIndex);
			
			// FIXME This is stupid... but an artifact of the way the other sieve method is designed
			
			synchronized (structures) {
				structurizer.addToStructures(datum, label, score, structures, changes);
				weightedPredictionSum += score;
			}
			
			if (Double.compare(prevScore, score) != 0) {
				List<String> structureIds = new ArrayList<String>(changes.keySet());
				for (String structureId : structureIds) {
					int startStructureSize = structures.get(structureId).getItemCount();
					List transformedStructures = ((FnStructure)structureTransformFn).listCompute(structures.get(structureId), changes.get(structureId));
					WeightedStructure firstTransformed = (WeightedStructure)transformedStructures.get(0);
					for (int j = 1; j < transformedStructures.size(); j++)
						firstTransformed = firstTransformed.merge((WeightedStructure)transformedStructures.get(j));		
					
					synchronized (structures) {
						structures.put(structureId, firstTransformed);
					}
					
					transformPredictions += structures.get(structureId).getItemCount() - startStructureSize;
					weightedPredictionSum += score * (structures.get(structureId).getItemCount() - startStructureSize);
				}
				
				changes = new HashMap<String, Collection<WeightedStructure>>();
				skippedStructure = false;
			} else {
				skippedStructure = true;
			}
			
			prevScore = score;
		}
		
		if (skippedStructure) {
			List<String> structureIds = new ArrayList<String>(changes.keySet());
			for (String structureId : structureIds) {
				int startStructureSize = structures.get(structureId).getItemCount();
				List transformedStructures = ((FnStructure)structureTransformFn).listCompute(structures.get(structureId), changes.get(structureId));
				WeightedStructure firstTransformed = (WeightedStructure)transformedStructures.get(0);
				for (int j = 1; j < transformedStructures.size(); j++)
					firstTransformed = firstTransformed.merge((WeightedStructure)transformedStructures.get(j));		
				
				synchronized (structures) {
					structures.put(structureId, firstTransformed);
				}

				transformPredictions += structures.get(structureId).getItemCount() - startStructureSize;
				weightedPredictionSum += prevScore * (structures.get(structureId).getItemCount() - startStructureSize);

			}
		}
		
		return new Pair<Integer, Double>(transformPredictions, weightedPredictionSum);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean init(List<DataSet<?, ?>> testData) {
		if (this.initialized)
			return true;
		
		for (MethodClassification<?, ?> method : this.methods) {
			boolean init = false;
			for (int j = 0; j < testData.size(); j++) {
				if (!method.matchesData(testData.get(j)))
					continue;
				if (method.init((DataSet)testData.get(j))) {
					init = true;
					break;
				}
			}
			
			if (!init)
				method.init();
		}
	
		this.initialized = true;
		
		if (this.trainOnInit)
			return train();
		else
			return true;
	}

	@Override
	public MethodMultiClassification clone() {
		MethodMultiClassificationRandomSieve clone = new MethodMultiClassificationRandomSieve(this.context);
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
		return new MethodMultiClassificationRandomSieve(context);
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
		return "RandomSieve";
	}

	@Override
	public boolean train() {
		List<Trainable<?, ?>> trainables = new ArrayList<>(getTrainableMethods().keySet()); 
		for (Trainable<?, ?> trainable : trainables)
			if (!trainable.train())
				return false;
		
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
	
	// Map from trainable  to list of methods that use that trainable
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
