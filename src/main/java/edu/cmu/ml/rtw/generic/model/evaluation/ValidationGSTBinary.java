package edu.cmu.ml.rtw.generic.model.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.TokenSpanExtractor;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.SupervisedModelCompositeBinary;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.ThreadMapper;
import edu.cmu.ml.rtw.generic.util.ThreadMapper.Fn;
import edu.cmu.ml.rtw.generic.util.Timer;

/**
 * ValidationGSTBinary performs ValidationGST validations on several
 * binary models in parallel.  There is a separate validation for
 * each label indicator stored in the given Datum.Tools object.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 * @param <D>
 * @param <L>
 */
public class ValidationGSTBinary<T extends Datum<Boolean>, D extends Datum<L>, L> extends ValidationGST<D, L> {
	private ConfusionMatrix<T, Boolean> aggregateConfusionMatrix;
	private List<ValidationGST<T, Boolean>> binaryValidations;
	private Map<GridSearch<T, Boolean>.GridPosition, Integer> bestPositionCounts;
	private SupervisedModelCompositeBinary<T, D, L> learnedCompositeModel;
	
	private List<SupervisedModelEvaluation<D, L>> compositeEvaluations;
	private Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator;
	private List<Double> compositeEvaluationValues;
	
	private Map<String, FeaturizedDataSet<D, L>> compositeTestSets;
	private Map<String, List<Double>> compositeTestSetEvaluationValues;
	
	public ValidationGSTBinary(String name, Context<D, L> context, Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator) {
		super(name, context);
		this.inverseLabelIndicator = inverseLabelIndicator;
		this.evaluations = context.getEvaluationsWithoutModifier("composite");
		this.compositeEvaluations =  context.getEvaluationsWithModifier("composite");
		this.compositeEvaluationValues = new ArrayList<Double>();
		initCompositeFeaturizedTestSets(new HashMap<String, DataSet<D, L>>(), context.getFeatures());
	}
	
	public ValidationGSTBinary(String name,
			  int maxThreads,
			  FeaturizedDataSet<D, L> trainData,
			  FeaturizedDataSet<D, L> devData,
			  FeaturizedDataSet<D, L> testData,
			  List<SupervisedModelEvaluation<D, L>> evaluations,
			  TokenSpanExtractor<D, L> errorExampleExtractor,
			  GridSearch<D,L> gridSearch,
			  boolean trainOnDev,
			  List<SupervisedModelEvaluation<D, L>> compositeEvaluations,
			  Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator,
			  Map<String, DataSet<D, L>> compositeTestSets) {
		super(name, 
			  maxThreads,
			  trainData,
			  devData,
			  testData,
			  evaluations,
			  errorExampleExtractor,
			  gridSearch,
			  trainOnDev);
	
		this.inverseLabelIndicator = inverseLabelIndicator;
		this.compositeEvaluations = compositeEvaluations;
		this.compositeEvaluationValues = new ArrayList<Double>();
		initCompositeFeaturizedTestSets(compositeTestSets, trainData.getFeatures());
	}

	public ValidationGSTBinary(String name,
							   int maxThreads,
							   List<Feature<D, L>> features,
							   DataSet<D, L> trainData,
							   DataSet<D, L> devData,
							   DataSet<D, L> testData,
							   List<SupervisedModelEvaluation<D, L>> evaluations,
							   TokenSpanExtractor<D, L> errorExampleExtractor,
							   GridSearch<D, L> gridSearch,
							   boolean trainOnDev,
							   Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator,
							   List<SupervisedModelEvaluation<D, L>> compositeEvaluations,
							   Map<String, DataSet<D, L>> compositeTestSets) {
		super(name, 
			  maxThreads,
			  features,
			  trainData,
			  devData,
			  testData,
			  evaluations,
			  errorExampleExtractor,
			  gridSearch,
			  trainOnDev);
		
		this.inverseLabelIndicator = inverseLabelIndicator;
		this.compositeEvaluations = compositeEvaluations;
		this.compositeEvaluationValues = new ArrayList<Double>();
		initCompositeFeaturizedTestSets(compositeTestSets, features);
	}

	public ValidationGSTBinary(String name, 
				 Context<D, L> context, 
				 DataSet<D, L> trainData, 
				 DataSet<D, L> devData, 
				 DataSet<D, L> testData,
				 Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator,
				 Map<String, DataSet<D, L>> compositeTestSets) {
		super(name, context, trainData, devData, testData);
		this.inverseLabelIndicator = inverseLabelIndicator;
		this.evaluations = context.getEvaluationsWithoutModifier("composite");
		this.compositeEvaluations =  context.getEvaluationsWithModifier("composite");
		this.compositeEvaluationValues = new ArrayList<Double>();
		initCompositeFeaturizedTestSets(compositeTestSets, context.getFeatures());
	}
	
	public ValidationGSTBinary(String name, 
			 Context<D, L> context, 
			 DataSet<D, L> trainData, 
			 DataSet<D, L> devData, 
			 DataSet<D, L> testData,
			 Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator) {
		this(name, context, trainData, devData, testData, inverseLabelIndicator, null);
		this.evaluations = context.getEvaluationsWithoutModifier("composite");
		initCompositeFeaturizedTestSets(new HashMap<String, DataSet<D, L>>(), context.getFeatures());
	}

	public ValidationGSTBinary(String name, 
						 Context<D, L> context, 
						 FeaturizedDataSet<D, L> trainData, 
						 FeaturizedDataSet<D, L> devData, 
						 FeaturizedDataSet<D, L> testData,
						 Datum.Tools.InverseLabelIndicator<L> inverseLabelIndicator,
						 Map<String, DataSet<D, L>> compositeTestSets) {
		super(name, context, trainData, devData, testData);
		this.inverseLabelIndicator = inverseLabelIndicator;
		this.evaluations = context.getEvaluationsWithoutModifier("composite");
		this.compositeEvaluations = context.getEvaluationsWithModifier("composite");
		this.compositeEvaluationValues = new ArrayList<Double>();
		initCompositeFeaturizedTestSets(compositeTestSets, context.getFeatures());
	}
	
	private boolean initCompositeFeaturizedTestSets(Map<String, DataSet<D, L>> compositeTestSets, List<Feature<D, L>> features) {
		this.compositeTestSets = new HashMap<String, FeaturizedDataSet<D, L>>();
		this.compositeTestSetEvaluationValues = new HashMap<String, List<Double>>();
		if (compositeTestSets == null) {
			return true;
		}
		
		for (Entry<String, DataSet<D, L>> entry : compositeTestSets.entrySet()) {
			FeaturizedDataSet<D, L> featurizedData = new FeaturizedDataSet<D, L>(entry.getKey(), maxThreads, entry.getValue().getDatumTools(), trainData.getLabelMapping()); 
			featurizedData.addAll(entry.getValue());
			this.compositeTestSets.put(entry.getKey(), featurizedData);
			this.compositeTestSetEvaluationValues.put(entry.getKey(), new ArrayList<Double>());
		
			for (Feature<D, L> feature : features) {
				featurizedData.addFeature(feature, false);
			}
		}
		
		return true;
	}
	
	@Override
	public boolean reset(FeaturizedDataSet<D, L> trainData, FeaturizedDataSet<D, L> devData, FeaturizedDataSet<D, L> testData) {
		if (!super.reset(trainData, devData, testData))
			return false;
	
		this.aggregateConfusionMatrix = null;
		this.binaryValidations = null;
		this.bestPositionCounts = null;
		this.learnedCompositeModel = null;
		this.compositeEvaluationValues = new ArrayList<Double>();
		this.compositeTestSetEvaluationValues = new HashMap<String, List<Double>>();
		
		return true;
	}
	
	@Override 
	public SupervisedModel<D, L> getModel() {
		return this.learnedCompositeModel;
	}
	
	@Override
	public List<Double> run() { 
		Timer timer = this.trainData.getDatumTools().getDataTools().getTimer();
		final OutputWriter output = this.trainData.getDatumTools().getDataTools().getOutputWriter();
		final Set<Boolean> binaryLabels = new HashSet<Boolean>();
		binaryLabels.add(true);
		binaryLabels.add(false);
		
		output.debugWriteln("Running GSTBinary with max threads " + this.maxThreads + "...");

		if (!this.trainData.getPrecomputedFeatures() 
				|| !this.devData.getPrecomputedFeatures() 
				|| (this.testData != null && !this.testData.getPrecomputedFeatures())) {
			timer.startClock(this.name + " Feature Computation");
			output.debugWriteln("Computing features...");
			if (!this.trainData.precomputeFeatures()
					|| !this.devData.precomputeFeatures()
					|| (this.testData != null && !this.testData.precomputeFeatures()))
				return null;
			
			for (FeaturizedDataSet<D, L> compositeTestSet : this.compositeTestSets.values()) {
				if (!compositeTestSet.precomputeFeatures())
					return null;
			}
			
			output.debugWriteln("Finished computing features.");
			timer.stopClock(this.name + " Feature Computation");
		}
		
		output.resultsWriteln("Training data examples: " + this.trainData.size());
		output.resultsWriteln("Dev data examples: " + this.devData.size());
		output.resultsWriteln("Test data examples: " + this.testData.size());
		output.resultsWriteln("Feature vocabulary size: " + this.trainData.getFeatureVocabularySize() + "\n");
		
		ThreadMapper<LabelIndicator<L>, ValidationGST<T, Boolean>> threads = new ThreadMapper<LabelIndicator<L>, ValidationGST<T, Boolean>>(new Fn<LabelIndicator<L>, ValidationGST<T, Boolean>>() {
			public ValidationGST<T, Boolean> apply(LabelIndicator<L> labelIndicator) {
				Datum.Tools<T, Boolean> binaryTools = trainData.getDatumTools().makeBinaryDatumTools(labelIndicator);
				Context<T, Boolean> binaryContext = gridSearch.getContext().makeBinary(binaryTools, labelIndicator);
				
				List<SupervisedModelEvaluation<T, Boolean>> binaryEvaluations = new ArrayList<SupervisedModelEvaluation<T, Boolean>>();
				for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
					binaryEvaluations.add(evaluation.makeBinary(binaryContext, labelIndicator).clone());
				}
				
				FeaturizedDataSet<T, Boolean> binaryTrainData = (FeaturizedDataSet<T, Boolean>)trainData.makeBinary(labelIndicator, binaryContext);
				FeaturizedDataSet<T, Boolean> binaryDevData = (FeaturizedDataSet<T, Boolean>)devData.makeBinary(labelIndicator, binaryContext);
				FeaturizedDataSet<T, Boolean> binaryTestData = (testData == null) ? null : (FeaturizedDataSet<T, Boolean>)testData.makeBinary(labelIndicator, binaryContext);
				
				ValidationGST<T, Boolean> binaryValidation = new ValidationGST<T, Boolean>(
						labelIndicator.toString(),
						(int)Math.ceil(maxThreads / (double)trainData.getDatumTools().getLabelIndicators().size()),
						binaryTrainData,
						binaryDevData,
						binaryTestData,			
						binaryEvaluations,
						binaryTools.getTokenSpanExtractor(errorExampleExtractor.toString()),
						gridSearch.makeBinary(binaryContext, labelIndicator),
						trainOnDev);
				
				if (binaryTrainData.getDataSizeForLabel(true) == 0
						/*|| binaryDevData.getDataSizeForLabel(true) == 0 
						|| (binaryTestData != null && binaryTestData.getDataSizeForLabel(true) == 0)*/) {
					output.debugWriteln("Skipping " + labelIndicator.toString() + ".  Not enough positive examples.");
					return binaryValidation;
				}
					
				if (!binaryValidation.runAndOutput()) {
					return null;
				}
				
				return binaryValidation;
			}
		});
		
		this.binaryValidations = threads.run(this.trainData.getDatumTools().getLabelIndicators(), this.maxThreads);
		this.aggregateConfusionMatrix = new ConfusionMatrix<T, Boolean>(binaryLabels);
		this.evaluationValues = new ArrayList<Double>();
		List<Double> evaluationCounts = new ArrayList<Double>();
		this.bestPositionCounts = new HashMap<GridSearch<T, Boolean>.GridPosition, Integer>();
		for (int i = 0; i < this.evaluations.size(); i++) {
			this.evaluationValues.add(0.0);
			evaluationCounts.add(0.0);
		}
		
		List<SupervisedModel<T, Boolean>> trainedModels = new ArrayList<SupervisedModel<T, Boolean>>();
		List<LabelIndicator<L>> trainedLabelIndicators = new ArrayList<LabelIndicator<L>>();
		for (int i = 0; i < this.binaryValidations.size(); i++) {
			ValidationGST<T, Boolean> validation = this.binaryValidations.get(i);
			if (this.binaryValidations.get(i).trainData.getDataSizeForLabel(true) == 0
					/*|| this.binaryValidations.get(i).devData.getDataSizeForLabel(true) == 0
					|| (this.binaryValidations.get(i).testData != null && this.binaryValidations.get(i).testData.getDataSizeForLabel(true) == 0)*/) {
				output.resultsWriteln("Ignored " + this.trainData.getDatumTools().getLabelIndicators().get(i).toString() + " (lacking positive examples)");
				continue;
			}
			
			if (validation == null) {
				output.debugWriteln("ERROR: Validation thread failed.");
				return null;
			}
			
			ConfusionMatrix<T, Boolean> confusions = validation.getConfusionMatrix();
			
			for (int j = 0; j < this.evaluations.size(); j++) {
				this.evaluationValues.set(j, this.evaluationValues.get(j) + validation.getEvaluationValues().get(j));
				evaluationCounts.set(j, evaluationCounts.get(j) + 1); // FIXME: This is no longer necessary
			}
			
			this.aggregateConfusionMatrix.add(confusions);
			
			GridSearch<T, Boolean>.EvaluatedGridPosition bestGridPosition = validation.getBestGridPosition();
			
			if (!this.bestPositionCounts.containsKey(bestGridPosition))
				this.bestPositionCounts.put(bestGridPosition, 0);
			this.bestPositionCounts.put(bestGridPosition, this.bestPositionCounts.get(bestGridPosition) + 1);
		
			trainedModels.add(validation.getModel());
			trainedLabelIndicators.add(this.trainData.getDatumTools().getLabelIndicators().get(i));
		}
		
		for (int j = 0; j < this.evaluations.size(); j++) {
			this.evaluationValues.set(j, this.evaluationValues.get(j) / evaluationCounts.get(j));
		}
		
		this.learnedCompositeModel = new SupervisedModelCompositeBinary<T, D, L>(
				trainedModels, 
				trainedLabelIndicators, 
				this.gridSearch.getContext().makeBinary(this.binaryValidations.get(0).datumTools, null), 
				this.inverseLabelIndicator);
		
		output.debugWriteln("Composite model classifying test data...");
		final Map<D, L> classifiedData = this.learnedCompositeModel.classify(this.testData);
		output.debugWriteln("Finished classifying test data.");
		
		output.debugWriteln("Evaluating composite model on test data...");
		ThreadMapper<SupervisedModelEvaluation<D, L>, Double> evaluationMapper = new ThreadMapper<SupervisedModelEvaluation<D, L>, Double>(new Fn<SupervisedModelEvaluation<D, L>, Double>() {
			@Override
			public Double apply(SupervisedModelEvaluation<D, L> evaluation) {
				return evaluation.evaluate(learnedCompositeModel, testData, classifiedData);
			}
		});
		this.compositeEvaluationValues = evaluationMapper.run(this.compositeEvaluations, this.maxThreads);
		output.debugWriteln("Finished evaluating composite model on test data...");
		
		
		for (Entry<String, FeaturizedDataSet<D, L>> entry : this.compositeTestSets.entrySet()) {
			output.debugWriteln("Composite model classifying " + entry.getKey() + " data...");
			final Map<D, L> compositeTestSetClassifiedData = this.learnedCompositeModel.classify(entry.getValue());
			output.debugWriteln("Composite model finished classifying " + entry.getKey() + " data.");
			
			output.debugWriteln("Evaluating composite model on " + entry.getKey() + " data...");
			evaluationMapper = new ThreadMapper<SupervisedModelEvaluation<D, L>, Double>(new Fn<SupervisedModelEvaluation<D, L>, Double>() {
				@Override
				public Double apply(SupervisedModelEvaluation<D, L> evaluation) {
					return evaluation.evaluate(learnedCompositeModel, entry.getValue(), compositeTestSetClassifiedData);
				}
			});
			this.compositeTestSetEvaluationValues.put(entry.getKey(), evaluationMapper.run(this.compositeEvaluations, this.maxThreads));
			output.debugWriteln("Finished evaluating composite model on " + entry.getKey() + " data.");
		}
		
		return this.evaluationValues;
	}
	
	@Override
	public boolean outputModel() {
		return true;
	}
	
	@Override
	public boolean outputData() { 
		return true;
	}
	
	@Override
	public boolean outputResults() {
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		DecimalFormat cleanDouble = new DecimalFormat("0.00000");
		
		output.resultsWrite("\nMeasures:\t");
		for (SupervisedModelEvaluation<D, L> evaluation : this.evaluations)
			output.resultsWrite(evaluation.toString() + "\t");
		
		for (ValidationGST<T, Boolean> validation : this.binaryValidations) {
			if (validation.getEvaluationValues() == null)
				continue;
			
			output.resultsWrite("\n" + validation.name + ":\t");
			
			for (Double value : validation.getEvaluationValues())
				output.resultsWrite(cleanDouble.format(value) + "\t");
		}
		
		output.resultsWrite("\nAverages:\t");
		for (Double value : this.evaluationValues) 
			output.resultsWrite(cleanDouble.format(value) + "\t");
		
		output.resultsWriteln("\n\nConfusion matrix:\n" + this.aggregateConfusionMatrix.toString());
		
		output.resultsWriteln("\nBest grid search position counts:");
		for (Entry<GridSearch<T, Boolean>.GridPosition, Integer> entry : this.bestPositionCounts.entrySet())
			output.resultsWriteln(entry.getKey().toString() + "\t" + entry.getValue());	
		
		output.resultsWriteln("\nComposite evaluations (on Test data):");
		for (int i = 0; i < this.compositeEvaluations.size(); i++) {
			output.resultsWriteln(this.compositeEvaluations.get(i).toString() + "\t" + cleanDouble.format(this.compositeEvaluationValues.get(i)));
		}
		
		for (Entry<String, FeaturizedDataSet<D, L>> entry : this.compositeTestSets.entrySet()) {
			output.resultsWriteln("\nComposite evaluations (on " + entry.getKey() + " data):");
			for (int j = 0; j < this.compositeEvaluations.size(); j++)
				output.resultsWriteln(this.compositeEvaluations.get(j).toString() + "\t" + cleanDouble.format(this.compositeTestSetEvaluationValues.get(entry.getKey()).get(j)));	
		}
		
		output.resultsWriteln("\nTime:\n" + this.datumTools.getDataTools().getTimer().toString());
	
		return true;
	}
}
