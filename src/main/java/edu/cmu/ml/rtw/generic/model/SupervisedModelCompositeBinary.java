package edu.cmu.ml.rtw.generic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;

public class SupervisedModelCompositeBinary<T extends Datum<Boolean>, D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private List<SupervisedModel<T, Boolean>> binaryModels;
	private List<LabelIndicator<L>> labelIndicators;
	private Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator;
	private Context<T, Boolean> binaryContext;
	
	public SupervisedModelCompositeBinary(List<SupervisedModel<T, Boolean>> binaryModels, List<LabelIndicator<L>> labelIndicators, Context<T, Boolean> binaryContext, Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator) {
		this.binaryModels = binaryModels;
		this.labelIndicators = labelIndicators;
		this.binaryContext = binaryContext;
		this.inverseLabelIndicator = inverseLabelIndicator;
	}
	
	public SupervisedModel<T, Boolean> getModelForIndicator(String indicatorStr) {
		for (int i = 0; i < this.labelIndicators.size(); i++) {
			if (this.labelIndicators.get(i).toString().equals(indicatorStr))
				return this.binaryModels.get(i);
		}
		
		return null;
	}

	@Override
	public boolean train(FeaturizedDataSet<D, L> data,
			FeaturizedDataSet<D, L> testData,
			List<SupervisedModelEvaluation<D, L>> evaluations) {
		return true;
	}

	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		Map<D, Map<L, Double>> p = new HashMap<D, Map<L, Double>>();
		final FeaturizedDataSet<T, Boolean> binaryData = (FeaturizedDataSet<T, Boolean>)data.makeBinary(this.binaryContext);
		List<Map<T, Map<Boolean, Double>>> binaryP = computeBinaryModelPosteriors(binaryData, data.getMaxThreads());
		
		// Generate label for each datum (in parallel)
		binaryData.map(new Fn<T, T>() {
			@Override
			public T apply(T datum) {
				L label = computeLabel(binaryP, datum);
				D unlabeledDatum = data.getDatumById(datum.getId());
				Map<L, Double> datumP = new HashMap<L, Double>();
				datumP.put(label, 1.0);
				synchronized (p) {
					p.put(unlabeledDatum, datumP);
				}
				
				return datum;
			}
		}, data.getMaxThreads());
		
		return p;
	}
	
	@Override
	public Map<D, L> classify(FeaturizedDataSet<D, L> data) {
		Map<D, L> classifications = new HashMap<D, L>();
		final FeaturizedDataSet<T, Boolean> binaryData = (FeaturizedDataSet<T, Boolean>)data.makeBinary(this.binaryContext);
		List<Map<T, Map<Boolean, Double>>> binaryP = computeBinaryModelPosteriors(binaryData, data.getMaxThreads());
			
		// Generate label for each datum (in parallel)
		binaryData.map(new Fn<T, T>() {
			@Override
			public T apply(T datum) {
				L label = computeLabel(binaryP, datum);
				D unlabeledDatum = data.getDatumById(datum.getId());
				
				synchronized (classifications) {
					classifications.put(unlabeledDatum, label);
				}
				
				return datum;
			}
		}, data.getMaxThreads());
		
		return classifications;
	}
	
	private L computeLabel(List<Map<T, Map<Boolean, Double>>> binaryP, T datum) {
		Map<String, Double> indicatorWeights = new HashMap<String, Double>();
		List<String> positiveIndicators = new ArrayList<String>();
		for (int i = 0; i < binaryP.size(); i++) { // For each label indicator
			String label = this.labelIndicators.get(i).toString();
			double weight = binaryP.get(i).get(datum).get(true);
			indicatorWeights.put(label, weight);
			if (weight >= 0.5)
				positiveIndicators.add(label);
		}
		
		return this.inverseLabelIndicator.label(indicatorWeights, positiveIndicators);
	}
	
	private List<Map<T, Map<Boolean, Double>>> computeBinaryModelPosteriors(FeaturizedDataSet<T, Boolean> binaryData, int maxThreads) {
		ThreadMapper<SupervisedModel<T, Boolean>, Map<T, Map<Boolean, Double>>> pThreads 
		= new ThreadMapper<SupervisedModel<T, Boolean>, Map<T, Map<Boolean, Double>>>(
				new Fn<SupervisedModel<T, Boolean>, Map<T, Map<Boolean, Double>>>() {
					@Override
					public Map<T, Map<Boolean, Double>> apply(
							SupervisedModel<T, Boolean> model) {
						return model.posterior(binaryData);
					}
				}
			);
		
		return pThreads.run(this.binaryModels, maxThreads);
	}
	
	@Override
	public String[] getParameterNames() {
		return new String[0];
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
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return null;
	}

	@Override
	public String getGenericName() {
		return "CompositeBinary";
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true; // TODO Implement later if necessary
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		return internalAssignments; // TODO Implement later if necessary
	}

	@Override
	protected <U extends Datum<Boolean>> SupervisedModel<U, Boolean> makeBinaryHelper(
			Context<U, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<U, Boolean> binaryModel) {
		return binaryModel; // TODO Implement later if necessary
	}
}
