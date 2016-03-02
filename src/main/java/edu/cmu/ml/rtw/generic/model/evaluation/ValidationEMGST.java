package edu.cmu.ml.rtw.generic.model.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet.DataFilter;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.Timer;

/**
 * ValidationEMGST runs several successive ValidationGST
 * validations, relabeling data using the models that 
 * are trained by each validation, and retraining on
 * that relabeled data in the next iteration (EM).
 * 
 * FIXME: Note that this code was refactored quite a bit,
 * and the newest version has not been tested.
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class ValidationEMGST<D extends Datum<L>, L> extends Validation<D, L> {
	private int iterations;
	private boolean firstIterationOnlyLabeled;
	private boolean relabelLabeledData;
	private List<SupervisedModelEvaluation<D, L>> unlabeledEvaluations;
	
	private ValidationGST<D, L> validationGST;
	private FeaturizedDataSet<D, L> trainData;
	private FeaturizedDataSet<D, L> devData;
	private FeaturizedDataSet<D, L> testData;
	
	public ValidationEMGST(ValidationGST<D, L> validationGST,
			DatumContext<D, L> context,
			DataSet<D, L> trainData, 
			DataSet<D, L> devData,
			DataSet<D, L> testData,
			boolean relabelLabeledData) {
		super(validationGST.name, context);
	
		
		this.validationGST = validationGST;
		this.trainData = new FeaturizedDataSet<D, L>(name + " Training", this.maxThreads, trainData.getDatumTools(), trainData.getLabelMapping()); 
		this.devData = new FeaturizedDataSet<D, L>(name + " Dev", this.maxThreads, devData.getDatumTools(), devData.getLabelMapping());
		this.testData = new FeaturizedDataSet<D, L>(name + " Test", this.maxThreads, testData.getDatumTools(), testData.getLabelMapping()); 
		
		this.validationGST.evaluations = context.getEvaluationsWithoutModifier("unlabeled");
		this.unlabeledEvaluations = context.getEvaluationsWithModifier("labeled");
		this.relabelLabeledData = relabelLabeledData;
		
		this.trainData.addAll(trainData);
		this.devData.addAll(devData);
		this.testData.addAll(testData);
	
		for (Feature<D, L> feature : context.getFeatures()) {
			context.getDatumTools().getDataTools().getOutputWriter().debugWriteln("Initializing feature " + feature.toString() + "...");
			this.trainData.addFeature(feature, true);
			this.devData.addFeature(feature, false);
			this.testData.addFeature(feature, false);
		}
	}

	@Override
	public List<Double> run() {
		Timer timer = this.trainData.getDatumTools().getDataTools().getTimer();
		OutputWriter output = this.trainData.getDatumTools().getDataTools().getOutputWriter();
		
		if (!copyIntoValidationGST())
			return null;
		
		timer.startClock(this.name + " Feature Computation");
		output.debugWriteln("Computing features...");
		if (!this.trainData.precomputeFeatures()
				|| !this.devData.precomputeFeatures()
				|| (this.testData != null && !this.testData.precomputeFeatures()))
			return null;
		output.debugWriteln("Finished computing features.");
		timer.stopClock(this.name + " Feature Computation");
		
		this.evaluationValues = new ArrayList<Double>();
		for (int i = 0; i < this.unlabeledEvaluations.size(); i++)
			this.evaluationValues.add(0.0);
		
		FeaturizedDataSet<D, L> labeledTrainData = (FeaturizedDataSet<D, L>)this.trainData.getSubset(DataFilter.OnlyLabeled);
		FeaturizedDataSet<D, L> labeledDevData = (FeaturizedDataSet<D, L>)this.devData.getSubset(DataFilter.OnlyLabeled);
		FeaturizedDataSet<D, L> labeledTestData = (FeaturizedDataSet<D, L>)this.testData.getSubset(DataFilter.OnlyLabeled);
		
		// First iteration only trains on labeled data?
		FeaturizedDataSet<D, L> trainData = (this.firstIterationOnlyLabeled) ? labeledTrainData : this.trainData;
		FeaturizedDataSet<D, L> devData = (this.firstIterationOnlyLabeled) ? labeledDevData : this.devData;
		FeaturizedDataSet<D, L> testData = (this.firstIterationOnlyLabeled) ? labeledTestData : this.testData;
		
		String initDebugFilePath = output.getDebugFilePath();
		String initResultsFilePath = output.getResultsFilePath();
		String initDataFilePath = output.getDataFilePath();
		String initModelFilePath = output.getModelFilePath();
		
		// Evaluate evaluation data with unlabeled evaluations (iteration 0)
		final FeaturizedDataSet<D, L> unlabeledEvaluationDataOnlyLabeled = labeledTestData;
		final Map<D, L> classifiedDataOnlyLabeled = new HashMap<D, L>();
		for (D datum : unlabeledEvaluationDataOnlyLabeled)
			classifiedDataOnlyLabeled.put(datum, datum.getLabel());
		ThreadMapper<SupervisedModelEvaluation<D, L>, Double> threadMapper = new ThreadMapper<SupervisedModelEvaluation<D, L>, Double>(
				new ThreadMapper.Fn<SupervisedModelEvaluation<D, L>, Double>() {
					@Override
					public Double apply(SupervisedModelEvaluation<D, L> evaluation) {
						return evaluation.evaluate(null, unlabeledEvaluationDataOnlyLabeled.toDataFeatureMatrix(model.getContext()), classifiedDataOnlyLabeled);
					}
				});
		this.evaluationValues = threadMapper.run(this.unlabeledEvaluations, this.maxThreads);
		output.resultsWriteln("Iteration 0 (evaluated labeled data with unlabeled evaluations)");
		if (!outputResults())
			return null;
		//
		
		int i = 1;
		while (i <= this.iterations) {
			output.setDebugFile(new File(initDebugFilePath + "." + i), false);
			output.setResultsFile(new File(initResultsFilePath + "." + i), false);
			output.setDataFile(new File(initDataFilePath + "." + i), false);
			output.setModelFile(new File(initModelFilePath + "." + i), false);
			
			if (!this.validationGST.reset(trainData, devData, testData)
					|| this.validationGST.run() == null
					|| !this.validationGST.outputAll())
				return null;
		
			// Unlabeled evaluations
			final FeaturizedDataSet<D, L> unlabeledEvaluationData = this.testData;
			final Map<D, L> classifiedData = this.validationGST.getModel().classify(unlabeledEvaluationData.toDataFeatureMatrix(this.model.getContext()));
			threadMapper = new ThreadMapper<SupervisedModelEvaluation<D, L>, Double>(
					new ThreadMapper.Fn<SupervisedModelEvaluation<D, L>, Double>() {
						@Override
						public Double apply(SupervisedModelEvaluation<D, L> evaluation) {
							return evaluation.evaluate(validationGST.getModel(), unlabeledEvaluationData.toDataFeatureMatrix(model.getContext()), classifiedData);
						}
					});
			this.evaluationValues = threadMapper.run(this.unlabeledEvaluations, this.maxThreads);
			
			// Relabel training data
			Map<D, Map<L, Double>> trainP = this.validationGST.getModel().posterior(this.trainData.toDataFeatureMatrix(this.model.getContext()));
			Map<D, L> trainC = this.validationGST.getModel().classify(this.trainData.toDataFeatureMatrix(this.model.getContext()));
			for (Entry<D, Map<L, Double>> entry : trainP.entrySet()) {
				if (!this.relabelLabeledData && labeledTrainData.contains(entry.getKey())) {
					continue;
				}
				for (Entry<L, Double> labelEntry : entry.getValue().entrySet()) {
					entry.getKey().setLabelWeight(labelEntry.getKey(), labelEntry.getValue());
				}
				
				this.trainData.setDatumLabel(entry.getKey(), trainC.get(entry.getKey()));
			}
			
			output.setDebugFile(new File(initDebugFilePath), true);
			output.setResultsFile(new File(initResultsFilePath), true);
			output.setDataFile(new File(initDataFilePath), true);
			output.setModelFile(new File(initModelFilePath), true);
			output.resultsWriteln("Iteration " + i);
			if (!outputResults())
				return null;
				
			trainData = this.trainData;
			devData = labeledDevData;
			testData = labeledTestData;
			i++;
		}
		
		this.model = this.validationGST.getModel();
		this.evaluationValues = this.validationGST.evaluationValues;
		this.confusionMatrix = this.validationGST.confusionMatrix;
		
		return this.evaluationValues;
	}
	
	private boolean copyIntoValidationGST() {
		this.validationGST.model = this.model;
		this.validationGST.errorExampleExtractor = this.errorExampleExtractor;
		this.validationGST.evaluations = this.evaluations;
		
		return true;
	}
	
	@Override
	public boolean outputResults() {
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		
		output.resultsWriteln("\nUnlabeled data evaluation results: ");
		for (int i = 0; i < this.unlabeledEvaluations.size(); i++)
			output.resultsWriteln(this.unlabeledEvaluations.get(i).toString() + ": " + this.evaluationValues.get(i));
		
		return true;
	}
	
	@Override
	public boolean outputModel() {
		return this.validationGST.outputModel();
	}
	
	@Override
	public boolean outputData() {
		return this.validationGST.outputData();
	}
}
