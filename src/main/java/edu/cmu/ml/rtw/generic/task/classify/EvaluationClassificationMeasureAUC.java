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

public class EvaluationClassificationMeasureAUC<D extends Datum<L> , L> extends EvaluationClassificationMeasure<D, L> {
	private L filterLabel; 	
	private String[] parameterNames = { "filterLabel" };

	public EvaluationClassificationMeasureAUC() {
		this(null);
	}
	
	public EvaluationClassificationMeasureAUC(DatumContext<D, L> context) {
		super(context);
	}

	private double trapezoidArea(double x1, double x2, double y1, double y2) {
		double base = Math.abs(x1 - x2);
		double height_avg = (y1 + y2)/2.0;
		return base*height_avg;
	}
	
	/*
	 * From http://www.cs.ru.nl/~tomh/onderwijs/dm/dm_files/ROC101.pdf
	 */
	@Override
	public Double compute(boolean forceRecompute) {
		Map<D, Double> scoreMap = this.method.score(this.task, this.filterLabel);
		List<Entry<D, Double>> scores = new ArrayList<>(scoreMap.entrySet());
		scores.sort(new Comparator<Entry<D, Double>>() {
			public int compare(Entry<D, Double> o1, Entry<D, Double> o2) {
				return Double.compare(o2.getValue(), o1.getValue());
			}
		});
		
		int tp = 0, fp = 0;
		int tp_prev = 0, fp_prev = 0;
		double A = 0.0;
		double score_prev = Double.NEGATIVE_INFINITY;
		
		for (Entry<D, Double> entry : scores) {
			if (entry.getKey().getLabel() == null)
				continue;
			
			Double score = entry.getValue();
			if (!score.equals(score_prev)) {
				A += trapezoidArea(fp, fp_prev, tp, tp_prev);
				score_prev = score;
				fp_prev = fp;
				tp_prev = tp;
			}
			
			if (entry.getKey().getLabel().equals(this.filterLabel)) 
				tp++;
			else
				fp++;
		}
		
		if (tp == 0)
			return 0.0;
		else if (fp == 0)
			return 1.0;
		
		A += trapezoidArea(1, fp_prev, 1, tp_prev);
		A /= (tp*fp);
		
		return A;
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
			return (this.filterLabel == null) ? null : Obj.stringValue(this.filterLabel.toString());
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
		return "AUC";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasureAUC<D, L>(this.context);
	}
	
	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationMeasureAUC<D, L>(context);
	}
}
