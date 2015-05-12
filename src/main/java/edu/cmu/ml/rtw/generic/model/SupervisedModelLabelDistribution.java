package edu.cmu.ml.rtw.generic.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * SupervisedModelLabelDistribution learns a single posterior
 * for all datums according to the label distribution in the
 * training data.  During classification, this leads the 
 * model to pick the majority baseline label.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class SupervisedModelLabelDistribution<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private Map<L, Double> labelDistribution;
	
	public SupervisedModelLabelDistribution() {
		this.labelDistribution = new HashMap<L, Double>();
	}
	
	public SupervisedModelLabelDistribution(Context<D, L> context) {
		this();
		this.context = context;
	}
	
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		double total = 0;
		
		for (L label : this.validLabels)
			this.labelDistribution.put(label, 0.0);
		
		for (D datum : data) {
			L label = mapValidLabel(datum.getLabel());
			if (label == null)
				continue;
			
			this.labelDistribution.put(label, this.labelDistribution.get(label) + 1.0);
			total += 1.0;
		}
		
		for (Entry<L, Double> entry : this.labelDistribution.entrySet()) {
			entry.setValue(entry.getValue() / total);
		}

		return true;
	}

	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		Map<D, Map<L, Double>> posterior = new HashMap<D, Map<L, Double>>();
		for (D datum : data) {
			posterior.put(datum, this.labelDistribution);
		}
		return posterior;
	}
	
	@Override
	public String[] getParameterNames() {
		return new String[0];
	}

	@Override
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelLabelDistribution<D, L>(context);
	}

	@Override
	public String getGenericName() {
		return "LabelDistribution";
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return true;
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments;
	}

	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}
}
