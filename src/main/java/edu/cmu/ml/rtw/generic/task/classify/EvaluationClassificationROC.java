package edu.cmu.ml.rtw.generic.task.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class EvaluationClassificationROC<D extends Datum<L>, L> extends EvaluationClassification<D, L, List<Pair<Double, Double>>> {
	private L filterLabel; 
	private String[] parameterNames = { "filterLabel" };
	
	public EvaluationClassificationROC() {
		this(null);
	}
	
	public EvaluationClassificationROC(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Type getType() {
		return Type.ROC;
	}

	@Override
	public List<Pair<Double, Double>> compute(boolean forceRecompute) {
		Map<D, Double> scoreMap = this.method.score(this.task, this.filterLabel);
		List<Entry<D, Double>> scores = new ArrayList<>(scoreMap.entrySet());
		scores.sort(new Comparator<Entry<D, Double>>() {
			public int compare(Entry<D, Double> o1, Entry<D, Double> o2) {
				return Double.compare(o2.getValue(), o1.getValue());
			}
		});
		
		int tp = 0, fp = 0;
		List<Pair<Double, Double>> points = new ArrayList<>();
		double score_prev = Double.NEGATIVE_INFINITY;
		
		for (Entry<D, Double> entry : scores) {
			if (entry.getKey().getLabel() == null)
				continue;
			
			Double score = entry.getValue();
			if (!score.equals(score_prev)) {
				points.add(new Pair<Double, Double>(Double.valueOf(fp), Double.valueOf(tp)));
				score_prev = score;
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
		return "ROC";
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
				.append(")\n");
		}
		
		str.append("\n");
		
		return str.toString();
	}

	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationROC<D, L>(context);
	}
}
