package edu.cmu.ml.rtw.generic.model.evaluation.metric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * SupervisedModelEvaluationF computes an F measure
 * (http://en.wikipedia.org/wiki/F1_score)
 * for a supervised classification model on a data set.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class SupervisedModelEvaluationF<D extends Datum<L>, L> extends SupervisedModelEvaluation<D, L> {
	/**
	 * Mode determines whether the F measure should be macro-averaged, micro-averaged,
	 * or macro-averaged weighted by actual label frequencies.
	 *
	 */
	public enum Mode {
		MACRO,
		MACRO_WEIGHTED,
		MICRO
	}
	
	private Mode mode = Mode.MACRO_WEIGHTED;
	private double Beta = 1.0;
	private String filterLabel; // Must be stored as string to work with ValidationGSTBinary
	private String[] parameterNames = { "mode", "Beta", "filterLabel" };
	
	public SupervisedModelEvaluationF() {
		
	}
	
	public SupervisedModelEvaluationF(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	protected double compute(SupervisedModel<D, L> model, FeaturizedDataSet<D, L> data, Map<D, L> predictions) {
		if (this.mode == Mode.MICRO)
			return computeMicro(model, data, predictions);
		else
			return computeMacro(model, data, predictions);
	}

	// Equal to micro accuracy...
	private double computeMicro(SupervisedModel<D, L> model, FeaturizedDataSet<D, L> data, Map<D, L> predictions) {
		List<Pair<L, L>> actualAndPredicted = this.getMappedActualAndPredictedLabels(predictions);
		double tp = 0;
		double f = 0; // fp or fn
		
		for (Pair<L, L> pair : actualAndPredicted) {
			if (pair.getFirst().equals(pair.getSecond()))
				tp++;
			else {
				f++;
			}
		}
		
		double pr = tp/(tp + f);
		return pr;//2*pr*pr/(pr+pr);
	}
	
	private double computeMacro(SupervisedModel<D, L> model, FeaturizedDataSet<D, L> data, Map<D, L> predictions) {
		List<Pair<L, L>> actualAndPredicted = this.getMappedActualAndPredictedLabels(predictions);
		Map<L, Double> weights = new HashMap<L, Double>();
		Map<L, Double> tps = new HashMap<L, Double>();
		Map<L, Double> fps = new HashMap<L, Double>();
		Map<L, Double> fns = new HashMap<L, Double>();
		
		for (L label : model.getValidLabels()) {
			if (!weights.containsKey(label)) {
				weights.put(label, 0.0); 
				tps.put(label, 0.0);
				fps.put(label, 0.0);
				fns.put(label, 0.0);
			}
		}
		
		for (Pair<L, L> pair : actualAndPredicted) {
			L actual = pair.getFirst();
			weights.put(actual, weights.get(actual) + 1.0);
		}
		
		L filterLabelTyped = data.getDatumTools().labelFromString(this.filterLabel);
		for (Entry<L, Double> entry : weights.entrySet()) {
			if (filterLabelTyped != null) {
				if (entry.getKey().equals(filterLabelTyped))
					entry.setValue(1.0);
				else
					entry.setValue(0.0);
			} else if (this.mode == Mode.MACRO_WEIGHTED)
				entry.setValue(entry.getValue()/actualAndPredicted.size());
			else
				entry.setValue(1.0/weights.size());
		}
		
		for (Pair<L, L> pair : actualAndPredicted) {
			L actual = pair.getFirst();
			L predicted = pair.getSecond();
			
			if (actual.equals(predicted)) {
				tps.put(predicted, tps.get(predicted) + 1.0);
			} else {
				fps.put(predicted, fps.get(predicted) + 1.0);
				fns.put(actual, fns.get(actual) + 1.0);
			}
		}
		
		double F = 0.0;
		double Beta2 = this.Beta*this.Beta;
		for (Entry<L, Double> weightEntry : weights.entrySet()) {
			L label = weightEntry.getKey();
			double weight = weightEntry.getValue();
			double tp = tps.get(label);
			double fp = fps.get(label);
			double fn = fns.get(label);
			
			if (tp == 0.0 && fn == 0.0 && fp == 0.0)
				F += weight;
			else
				F += weight*(1.0+Beta2)*tp/((1.0+Beta2)*tp + Beta2*fn + fp);
		}
		
		return F;
	}
	
	@Override
	public String getGenericName() {
		return "F";
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("mode"))
			return Obj.stringValue(this.mode.toString());
		else if (parameter.equals("Beta"))
			return Obj.stringValue(String.valueOf(this.Beta));
		else if (parameter.equals("filterLabel"))
			return Obj.stringValue((this.filterLabel == null) ? "" : this.filterLabel);
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("mode"))
			this.mode = Mode.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("Beta"))
			this.Beta = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("filterLabel"))
			this.filterLabel = this.context.getMatchValue(parameterValue);
		else
			return false;
		return true;
	}

	@Override
	public SupervisedModelEvaluation<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelEvaluationF<D, L>(context);
	}
}
