package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodMultiClassificationSieve extends MethodMultiClassification {
	private List<MethodClassification<?, ?>> methods;
	private List<Structurizer<?, ?, ?>> structurizers;
	private List<EvaluationClassificationMeasure<?, ?>> permutationMeasures;
	private Fn<?, ?> structureTransformFn;
	private String[] parameterNames = { "methods", "structurizers", "permutationMeasures", "structureTransformFn" };
	
	public MethodMultiClassificationSieve() {
		
	}
	
	public MethodMultiClassificationSieve(Context context) {
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
		} else if (parameter.equals("orderingMeasure")) {
			if (this.permutationMeasures == null)
				return null;
			Obj.Array array = Obj.array();
			for (EvaluationClassificationMeasure<?, ?> measure : this.permutationMeasures)
				array.add(Obj.curlyBracedValue(measure.getReferenceName()));
			return array;
		} else if (parameter.equals("structureTransformFn")) {
			return (this.structureTransformFn == null) ? null : this.structureTransformFn.toParse();
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
		} else if (parameter.equals("permutationMeasures")) {
			if (parameterValue != null) {
				this.permutationMeasures = new ArrayList<EvaluationClassificationMeasure<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.permutationMeasures.add((EvaluationClassificationMeasure<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("structureTransformFn")) {
			this.structureTransformFn = (parameterValue == null) ? null : this.context.getMatchOrConstructStructureFn(parameterValue);
		} else 
			return false;
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public List<Map<Datum<?>, ?>> classify(List<DataSet<?, ?>> data) {
		Map<String, ?> structures = null;
		if (this.structurizers.size() > 0)
			structures = this.structurizers.get(0).makeStructures();
		
		List<Pair<Integer, Double>> classifierOrdering = getClassifierOrdering();
		
		for (int o = 0; o < classifierOrdering.size(); o++) {
			Pair<Integer, Double> indexAndWeight = classifierOrdering.get(o);
			int i = indexAndWeight.getFirst();
			Structurizer structurizer = this.structurizers.get(i);
			MethodClassification<?, ?> method = this.methods.get(i);
			this.context.getDataTools().getOutputWriter().debugWriteln("Sieve classifying with method " + method.getReferenceName());
			int addedLinks = 0;
			Map<String, Collection<WeightedStructure>> changes = new HashMap<String, Collection<WeightedStructure>>();
			for (int j = 0; j < data.size(); j++) {
				if (!method.matchesData(data.get(j)))
					continue;
				Map<?, Pair<?, Double>> scoredDatums = (Map)method.classifyWithScore((DataSet)data.get(j));
				for (Entry<?, Pair<?, Double>> entry : scoredDatums.entrySet()) {
					double weight = (indexAndWeight.getSecond() != null) ? indexAndWeight.getSecond() : entry.getValue().getSecond();
					structures = structurizer.addToStructures((Datum)entry.getKey(), entry.getValue().getFirst(), weight, structures, changes);
					addedLinks++;
				}
			}
			
			this.context.getDataTools().getOutputWriter().debugWriteln(method.getReferenceName() + " tried to add " + addedLinks + " to structures");
			
			int startStructureItems = 0;
			int addedStructureItems = 0;
			for (Entry entry : structures.entrySet()) {
				startStructureItems += ((WeightedStructure)entry.getValue()).getItemCount();
				//System.out.println("BEFORE TRANSFORM:\n " + entry.getValue());
				List transformedStructures = ((FnStructure)this.structureTransformFn).listCompute((WeightedStructure)entry.getValue(), changes.get(entry.getKey()));
				WeightedStructure firstTransformed = (WeightedStructure)transformedStructures.get(0);
				for (int j = 1; j < transformedStructures.size(); j++)
					firstTransformed = firstTransformed.merge((WeightedStructure)transformedStructures.get(j));		
				entry.setValue(firstTransformed);
				//System.out.println("AFTER TRANSFORM:\n " + entry.getValue());
				addedStructureItems += firstTransformed.getItemCount();
			}
			
			this.context.getDataTools().getOutputWriter().debugWriteln("Structure transform changed item count from " + startStructureItems + " to " + addedStructureItems);
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Sieve pulling labeled data out of structures...");

		List<Map<Datum<?>, ?>> classifications = new ArrayList<Map<Datum<?>, ?>>();
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
					labeledData.put(d, maxLabel);
			}
			classifications.add(labeledData);
		}
		
		this.context.getDataTools().getOutputWriter().debugWriteln("Sieve finished classifying data");
		
		return classifications;
	}

	private List<Pair<Integer, Double>> getClassifierOrdering() {
		List<Pair<Integer, Double>> ordering = new ArrayList<>();
		
		if (this.permutationMeasures == null) {
			this.context.getDataTools().getOutputWriter().debugWriteln("Ordering methods by default: ");
			for (int i = 0; i < this.methods.size(); i++) {
				this.context.getDataTools().getOutputWriter().debugWriteln(this.methods.get(i).getReferenceName());
				ordering.add(new Pair<Integer, Double>(i, null));
			}
		} else {
			List<Pair<Integer, Double>> measures = new ArrayList<>();
			for (int i = 0; i < this.methods.size(); i++)
				measures.add(new Pair<>(i, this.permutationMeasures.get(i).compute()));
			Collections.sort(measures, new Comparator<Pair<Integer, Double>>() {
				@Override
				public int compare(Pair<Integer, Double> o1,
						Pair<Integer, Double> o2) {
					return o2.getSecond().compareTo(o1.getSecond());
				}
			});
			
			this.context.getDataTools().getOutputWriter().debugWriteln("Ordering methods by measures: ");
			for (int i = 0; i < this.permutationMeasures.size(); i++) {
				int orderIndex = measures.get(i).getFirst();
				
				this.context.getDataTools().getOutputWriter().debugWriteln(this.methods.get(orderIndex).getReferenceName() + " " + measures.get(i).getSecond());
				ordering.add(new Pair<Integer, Double>(orderIndex, measures.get(i).getSecond()));
			}
		}
		
		return ordering;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean init(List<DataSet<?, ?>> testData) {
		for (int i = 0; i < testData.size(); i++) {
			this.methods.get(i).init((DataSet)testData.get(i));
		}
		return true;
	}

	@Override
	public MethodMultiClassification clone() {
		MethodMultiClassificationSieve clone = new MethodMultiClassificationSieve(this.context);
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
		return new MethodMultiClassificationSieve(context);
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
		return "Sieve";
	}

}
