package edu.cmu.ml.rtw.generic.task.classify.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassification;
import edu.cmu.ml.rtw.generic.util.Pair;

public class EvaluationClassificationMetaROC<L> extends EvaluationClassification<PredictionClassificationDatum<L>, L, List<Pair<Double, Double>>> {
	private L filterLabel;
	private String[] parameterNames = { "filterLabel" };
	
	public EvaluationClassificationMetaROC() {
		this(null);
	}
	
	public EvaluationClassificationMetaROC(DatumContext<PredictionClassificationDatum<L>, L> context) {
		super(context);
	}

	@Override
	public Type getType() {
		return Type.ROC;
	}

	@SuppressWarnings("rawtypes")
	private Map<PredictionClassificationDatum<L>, Double> filterMaxDatums(Map<PredictionClassificationDatum<L>, Double> scoreMap, boolean onlyWithAlts) {
		Map<Datum, List<Entry<PredictionClassificationDatum<L>, Double>>> internalDatumMap = new HashMap<>();
		for (Entry<PredictionClassificationDatum<L>, Double> entry : scoreMap.entrySet()) {
			Datum internalDatum = entry.getKey().getPrediction().getDatum();
			if (!internalDatumMap.containsKey(internalDatum))
				internalDatumMap.put(internalDatum, new ArrayList<>());
			internalDatumMap.get(internalDatum).add(entry);
		}
		
		Map<PredictionClassificationDatum<L>, Double> filteredMap = new HashMap<>();
		for (List<Entry<PredictionClassificationDatum<L>, Double>> scoreEntries : internalDatumMap.values()) {
			Entry<PredictionClassificationDatum<L>, Double> maxEntry = null;
			boolean goodAlt = false;
			boolean badAlt = false;
			for (Entry<PredictionClassificationDatum<L>, Double> entry : scoreEntries) {
				if (maxEntry == null || Double.compare(entry.getValue(), maxEntry.getValue()) > 0)
					maxEntry = entry;
				if (!entry.getKey().getLabel().equals(this.filterLabel))
					badAlt = true;
				else
					goodAlt = true;
			}
			
			if (!onlyWithAlts || (goodAlt && badAlt))
				filteredMap.put(maxEntry.getKey(), maxEntry.getValue());
		}
		
		return filteredMap;
	}
	
	@Override
	public List<Pair<Double, Double>> compute(boolean forceRecompute) {
		Map<PredictionClassificationDatum<L>, Double> scoreMap = filterMaxDatums(this.method.score(this.task, this.filterLabel), false);
		List<Entry<PredictionClassificationDatum<L>, Double>> scores = new ArrayList<>(scoreMap.entrySet());
		scores.sort(new Comparator<Entry<PredictionClassificationDatum<L>, Double>>() {
			public int compare(Entry<PredictionClassificationDatum<L>, Double> o1, Entry<PredictionClassificationDatum<L>, Double> o2) {
				return Double.compare(o2.getValue(), o1.getValue());
			}
		});
		
		int tp = 0, fp = 0;
		List<Pair<Double, Double>> points = new ArrayList<>();
		double score_prev = Double.NEGATIVE_INFINITY;
		
		for (Entry<PredictionClassificationDatum<L>, Double> entry : scores) {
			if (entry.getKey().getLabel() == null)
				continue;
			
			Double score = entry.getValue();
			if (!score.equals(score_prev)) {
				points.add(new Pair<Double, Double>(Double.valueOf(fp), Double.valueOf(tp)));
			}
			
			if (entry.getKey().getLabel().equals(this.filterLabel)) 
				tp++;
			else
				fp++;
		}
		
		if (tp == 0 || fp == 0)
			return points;
		
		points.add(new Pair<Double, Double>(Double.valueOf(fp), Double.valueOf(tp)));
		
		for (Pair<Double, Double> point : points) {
			point.setFirst(point.getFirst() / fp);
			point.setSecond(point.getSecond() / tp);
		}
		
		return points;
	}
	
	@Override
	public int computeSampleSize(boolean forceRecompute) {
		throw new UnsupportedOperationException();
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
		if (parameter.equals("filterLabel"))
			return (this.filterLabel == null) ? null : Obj.stringValue(String.valueOf(this.filterLabel.toString()));
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("filterLabel"))
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
		return "MetaROC";
	}

	@Override
	public String toString() {
		List<Pair<Double, Double>> points = compute();
		
		StringBuilder str = new StringBuilder();
		
		str.append(getReferenceName());
		str.append(":\t");
		
		for (Pair<Double, Double> point : points) {
			str.append("(")
				.append(point.getFirst())
				.append(", ")
				.append(point.getSecond())
				.append(");");
		}
		
		return str.toString();
	}

	@Override
	public EvaluationClassification<PredictionClassificationDatum<L>, L, ?> makeInstance(
			DatumContext<PredictionClassificationDatum<L>, L> context) {
		return new EvaluationClassificationMetaROC<L>(context);
	}
}
