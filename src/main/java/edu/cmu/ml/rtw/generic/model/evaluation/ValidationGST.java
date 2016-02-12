package edu.cmu.ml.rtw.generic.model.evaluation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.TokenSpanExtractor;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Timer;

/**
 * ValidationGST (grid-search-test) performs a grid search for model
 * hyper-parameter values using a training and dev data sets.
 * Then, it sets the hyper-parameters to the best values from
 * the grid search, retrains on the train+dev data, and
 * gives a final evaluation on the test data. 
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class ValidationGST<D extends Datum<L>, L> extends Validation<D, L> {
	protected FeaturizedDataSet<D, L> trainData;
	protected FeaturizedDataSet<D, L> devData;
	protected FeaturizedDataSet<D, L> testData;
	protected GridSearch<D, L> gridSearch;
	protected boolean trainOnDev;
	
	public ValidationGST(String name, Context<D, L> context) {
		super(name, context);
		
		this.gridSearch = context.getGridSearches().get(0);
		this.trainOnDev = context.getBooleanValue("trainOnDev");
	}
	
	public ValidationGST(String name,
						  int maxThreads,
						  FeaturizedDataSet<D, L> trainData,
						  FeaturizedDataSet<D, L> devData,
						  FeaturizedDataSet<D, L> testData,
						  List<SupervisedModelEvaluation<D, L>> evaluations,
						  TokenSpanExtractor<D, L> errorExampleExtractor,
						  GridSearch<D, L> gridSearch,
						  boolean trainOnDev) {
		super(name, trainData.getDatumTools(), maxThreads, null, evaluations, errorExampleExtractor);
		this.trainData = trainData;
		this.devData = devData;
		this.testData = testData;
		this.gridSearch = gridSearch;
		this.trainOnDev = trainOnDev;
		this.gridSearch.init(trainData, devData);
	}
	
	public ValidationGST(String name,
			  int maxThreads,
			  List<Feature<D, L>> features,
			  DataSet<D, L> trainData,
			  DataSet<D, L> devData,
			  DataSet<D, L> testData,
			  List<SupervisedModelEvaluation<D, L>> evaluations,
			  TokenSpanExtractor<D, L> errorExampleExtractor,
			  GridSearch<D, L> gridSearch,
			  boolean trainOnDev) {
		this(name, 
			 maxThreads, 
			 new FeaturizedDataSet<D, L>(name + " Training", maxThreads, trainData.getDatumTools(), trainData.getLabelMapping()), 
			 new FeaturizedDataSet<D, L>(name + " Dev", maxThreads, devData.getDatumTools(), devData.getLabelMapping()), 
			 (testData == null) ? null : new FeaturizedDataSet<D, L>(name + " Test", maxThreads, testData.getDatumTools(), testData.getLabelMapping()), 
			 evaluations,
			 errorExampleExtractor,
			 gridSearch,
			 trainOnDev);
		
		this.trainData.addAll(trainData);
		this.devData.addAll(devData);
		if (this.testData != null)
			this.testData.addAll(testData);
		
		for (Feature<D, L> feature : features)
			addFeature(feature);
		
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		Context<D, L> featureContext = new Context<D, L>(this.datumTools, features);
		output.debugWriteln(this.name + " serializing features");
		output.modelWriteln(featureContext.toString()); 
		output.debugWriteln(this.name + " finished serializing features");
	}
	
	public ValidationGST(String name, 
						 Context<D, L> context, 
						 DataSet<D, L> trainData, 
						 DataSet<D, L> devData, 
						 DataSet<D, L> testData) {
		this(name, 
				context.getIntValue("maxThreads"), 
				context.getFeatures(),
				trainData, 
				devData, 
				testData, 
				context.getEvaluations(), 
				context.getDatumTools().getTokenSpanExtractor(context.getStringValue("errorExampleExtractor")), 
				context.getGridSearches().get(0),
				context.getBooleanValue("trainOnDev"));
		
	}
	
	public ValidationGST(String name, 
			 Context<D, L> context, 
			 FeaturizedDataSet<D, L> trainData, 
			 FeaturizedDataSet<D, L> devData, 
			 FeaturizedDataSet<D, L> testData) {
		this(name, 
			context.getIntValue("maxThreads"), 
			trainData, 
			devData, 
			testData, 
			context.getEvaluations(), 
			context.getDatumTools().getTokenSpanExtractor(context.getStringValue("errorExampleExtractor")), 
			context.getGridSearches().get(0), 
			context.getBooleanValue("trainOnDev"));	
	}
	
	public List<GridSearch<D, L>.EvaluatedGridPosition> getGridEvaluation() {
		return this.gridSearch.getGridEvaluation();
	}
	
	public GridSearch<D, L>.EvaluatedGridPosition getBestGridPosition() {
		return this.gridSearch.getBestPosition(this.maxThreads);
	}
	
	public boolean reset(FeaturizedDataSet<D, L> trainData, FeaturizedDataSet<D, L> devData, FeaturizedDataSet<D, L> testData) {
		this.trainData = trainData;
		this.devData = devData;
		this.testData = testData;
		
		this.evaluationValues = new ArrayList<Double>();
		this.confusionMatrix = null;
		this.gridSearch.init(trainData, devData);
		
		return true;
	}
	
	@Override
	public List<Double> run() {
		Timer timer = this.datumTools.getDataTools().getTimer();
		OutputWriter output = this.trainData.getDatumTools().getDataTools().getOutputWriter();
		DecimalFormat cleanDouble = new DecimalFormat("0.00000");
		GridSearch<D,L>.EvaluatedGridPosition bestGridPosition = null;
		
		timer.startClock(this.name + " GST (Total)");
		timer.startClock(this.name + " Grid Search");
		
		if (this.gridSearch.getDimensions().size() > 0) {
			bestGridPosition = this.gridSearch.getBestPosition(this.maxThreads);
				
			output.debugWriteln("Grid search (" + this.name + "): \n" + this.gridSearch.toString());
				
			if (bestGridPosition != null) {
				if (!this.trainOnDev) 
					this.model = bestGridPosition.getValidation().getModel();
				else {
					this.model = this.model.clone();
					this.model.setParameterValues(bestGridPosition.getCoordinates());
					
				}
			}
		}
		
		timer.stopClock(this.name + " Grid Search");
		
		output.debugWriteln("Train and/or evaluating model with best parameters (" + this.name + ")");
		
		int evaluationIndex = getEvaluationIndexByReferenceName(bestGridPosition.getValidation().getEvaluations().get(0).getReferenceName());
		this.evaluationValues = null;
		if (this.testData != null) {
			if (this.trainOnDev) 
				this.trainData.addAll(this.devData); // FIXME Reinitialize features on train+dev?
	
			ValidationTrainTest<D, L> validation = new ValidationTrainTest<D, L>(this.name, 1, this.model, this.trainData, this.testData, this.evaluations, this.errorExampleExtractor);
			
			this.evaluationValues = validation.run(!this.trainOnDev);
			if (this.evaluationValues.get(evaluationIndex) < 0) {
				output.debugWriteln("Error: Validation failed (" + this.name + ")");
				return null;
			} 
			
			
			this.confusionMatrix = validation.getConfusionMatrix();
			output.debugWriteln("Test " + this.evaluations.get(0).toString() + " (" + this.name + "): " + cleanDouble.format(this.evaluationValues.get(evaluationIndex)));
			
		} else {
			this.evaluationValues = bestGridPosition.getValidation().getEvaluationValues();
			this.confusionMatrix = bestGridPosition.getValidation().getConfusionMatrix();
			output.debugWriteln("Dev best " + this.evaluations.get(evaluationIndex).toString() + " (" + this.name + "): " + cleanDouble.format(this.evaluationValues.get(evaluationIndex)));
			this.model = bestGridPosition.getValidation().getModel();
		}
		
		timer.stopClock(this.name + " GST (Total)");
		
		return evaluationValues;
	}
	
	public boolean outputResults() {
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		
		GridSearch<D,L>.EvaluatedGridPosition bestGridPosition = (this.gridSearch.getDimensions().size() > 0) ? this.gridSearch.getBestPosition(this.maxThreads) : null;
		List<GridSearch<D, L>.EvaluatedGridPosition> gridEvaluation = (this.gridSearch.getDimensions().size() > 0) ? this.gridSearch.getGridEvaluation(this.maxThreads) : null;
		
		int evaluationIndex = getEvaluationIndexByReferenceName(bestGridPosition.getValidation().getEvaluations().get(0).getReferenceName());
		
		if (bestGridPosition != null) {
			Map<String, Obj> parameters = bestGridPosition.getCoordinates();
			output.resultsWriteln("Best parameters from grid search:");
			for (Entry<String, Obj> entry : parameters.entrySet())
				output.resultsWriteln(entry.getKey() + ": " + entry.getValue());
		}
		
		if (this.testData != null) {
			output.resultsWriteln("\nTest set evaluation results: ");
		} else {
			output.resultsWriteln("\nDev set best evaluation results: ");
		}
		
		for (int i = 0; i < this.evaluations.size(); i++)
			output.resultsWriteln(this.evaluations.get(i).toString() + ": " + evaluationValues.get(i));
		
		output.resultsWriteln("\nConfusion matrix:\n" + this.confusionMatrix.toString());
		
		if (gridEvaluation != null && gridEvaluation.size() > 0) {
			output.resultsWriteln("\nGrid search on " + this.evaluations.get(evaluationIndex).toString() + ":");
			output.resultsWriteln(gridEvaluation.get(0).toKeyString("\t") + "\t" + this.evaluations.get(evaluationIndex).toString());
			for (GridSearch<D, L>.EvaluatedGridPosition gridPosition : gridEvaluation) {
				output.resultsWriteln(gridPosition.toValueString("\t") + "\t" + gridPosition.getPositionValue());
			}
		}		
	
		return true;
	}
	
	protected boolean addFeature(Feature<D, L> feature) {
		OutputWriter output = this.datumTools.getDataTools().getOutputWriter();
		Timer timer = this.datumTools.getDataTools().getTimer();
		String featureStr = feature.getReferenceName();
		
		output.debugWriteln(this.name + " initializing feature (" + featureStr + ")...");
		timer.startClock(featureStr + " Initialization");
		if (!this.trainData.addFeature(feature, true))
			return false;
		timer.stopClock(featureStr + " Initialization");
		output.debugWriteln(this.name + " finished initializing feature (" + featureStr + ").");
	
		if (!this.devData.addFeature(feature, false) || (this.testData != null && !this.testData.addFeature(feature, false)))
			return false;
		
		return true;
	}
	
	private int getEvaluationIndexByReferenceName(String referenceName) {
		for (int i = 0; i < this.evaluations.size(); i++) {
			if (this.evaluations.get(i).getReferenceName().equals(referenceName))
				return i;
		}
		return -1;
	}
}
