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
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasure;

public class EvaluationClassificationMeasureMetaAUC<L> extends EvaluationClassificationMeasure<PredictionClassificationDatum<L>, L> {
	public enum Mode {
		WITHIN_DATUM,
		WITHOUT_DATUM
	}
	
	private Mode mode = Mode.WITHIN_DATUM;
	private L filterLabel;
	private String[] parameterNames = { "mode", "filterLabel" };

	public EvaluationClassificationMeasureMetaAUC() {
		this(null);
	}
	
	public EvaluationClassificationMeasureMetaAUC(DatumContext<PredictionClassificationDatum<L>, L> context) {
		super(context);
	}
	
	@Override
	public Double compute(boolean forceRecompute) {
		Map<PredictionClassificationDatum<L>, Double> scoreMap = this.method.score(this.task, this.filterLabel);
		if (this.mode == Mode.WITHIN_DATUM) {
			return computeWithin(filterMaxDatums(scoreMap, true));
		} else if (this.mode == Mode.WITHOUT_DATUM) {
			return computeWithout(filterMaxDatums(scoreMap, false));
		} else {
			return null;
		}
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
	
	private double trapezoidArea(double x1, double x2, double y1, double y2) {
		double base = Math.abs(x1 - x2);
		double height_avg = (y1 + y2)/2.0;
		return base*height_avg;
	}
	
	private Double computeWithin(Map<PredictionClassificationDatum<L>, Double> scoreMap) {
		double sum = 0.0;
		for (Entry<PredictionClassificationDatum<L>, Double> entry : scoreMap.entrySet()) {
			sum += entry.getKey().getLabel().equals(this.filterLabel) ? 1.0 : 0.0;
		}
		
		return sum / scoreMap.size();
	}
	
	/*
	 * From http://www.cs.ru.nl/~tomh/onderwijs/dm/dm_files/ROC101.pdf
	 */
	private Double computeWithout(Map<PredictionClassificationDatum<L>, Double> scoreMap) {
		List<Entry<PredictionClassificationDatum<L>, Double>> scores = new ArrayList<>(scoreMap.entrySet());
		scores.sort(new Comparator<Entry<PredictionClassificationDatum<L>, Double>>() {
			public int compare(Entry<PredictionClassificationDatum<L>, Double> o1, Entry<PredictionClassificationDatum<L>, Double> o2) {
				return Double.compare(o2.getValue(), o1.getValue());
			}
		});
		
		int tp = 0, fp = 0;
		int tp_prev = 0, fp_prev = 0;
		double A = 0.0;
		double score_prev = Double.NEGATIVE_INFINITY;
		
		for (Entry<PredictionClassificationDatum<L>, Double> entry : scores) {
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
		
		A += trapezoidArea(fp, fp_prev, tp, tp_prev);
		A /= (tp*fp);
		
		return A;
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
		if (parameter.equals("mode"))
			return (this.mode == null) ? null : Obj.stringValue(this.mode.toString());
		else if (parameter.equals("filterLabel"))
			return (this.filterLabel == null) ? null : Obj.stringValue(this.filterLabel.toString());
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = (parameterValue == null) ? null : Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filterLabel"))
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
		return "MetaAUC";
	}

	@Override
	protected EvaluationClassificationMeasure<PredictionClassificationDatum<L>, L> makeInstance() {
		return new EvaluationClassificationMeasureMetaAUC<L>(this.context);
	}
	
	@Override
	public EvaluationClassification<PredictionClassificationDatum<L>, L, ?> makeInstance(
			DatumContext<PredictionClassificationDatum<L>, L> context) {
		return new EvaluationClassificationMeasureMetaAUC<L>(context);
	}
}
