package edu.cmu.ml.rtw.generic.model.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.TokenSpanExtractor;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Timer;

/**
 * ValidationTrainTest trains a model on a training data
 * set and evaluates it on a test data set by given 
 * evaluation metrics
 * 
 * @author Bill McDowell
 *
 */
public class ValidationTrainTest<D extends Datum<L>, L> extends Validation<D, L> {
	private FeaturizedDataSet<D, L> trainData;
	private FeaturizedDataSet<D, L> testData;
	private Map<D, L> classifiedData;
	
	/**
	 * 
	 * @param name
	 * @param maxThreads
	 * @param model
	 * @param trainData 
	 * @param testData
	 * @param evaluations - Measures by which to evaluate the model
	 * @param errorExampleExtractor - Token span extractor used to generate error descriptions
	 *
	 */
	public ValidationTrainTest(String name,
							  int maxThreads,
							  SupervisedModel<D, L> model, 
							  FeaturizedDataSet<D, L> trainData,
							  FeaturizedDataSet<D, L> testData,
							  List<SupervisedModelEvaluation<D, L>> evaluations,
							  TokenSpanExtractor<D, L> errorExampleExtractor) {	
		super(name, trainData.getDatumTools(), maxThreads, model, evaluations, errorExampleExtractor);
		this.trainData = trainData;
		this.testData = testData;
	}
	
	public ValidationTrainTest(String name,
			  int maxThreads,
			  SupervisedModel<D, L> model, 
			  DataSet<D, L> trainData,
			  DataSet<D, L> testData,
			  List<SupervisedModelEvaluation<D, L>> evaluations,
			  TokenSpanExtractor<D, L> errorExampleExtractor) {	
		super(name, trainData.getDatumTools(), maxThreads, model, evaluations, errorExampleExtractor);
		this.trainData = new FeaturizedDataSet<D, L>(this.name + " Training", this.maxThreads, trainData.getDatumTools(), trainData.getLabelMapping());
		this.testData = new FeaturizedDataSet<D, L>(this.name + " Test", this.maxThreads, trainData.getDatumTools(), trainData.getLabelMapping());
	}
	
	public ValidationTrainTest(String name, Datum.Tools<D, L> datumTools, DataSet<D, L> trainData, DataSet<D, L> testData) {
		this(name, 1, null, trainData, testData, new ArrayList<SupervisedModelEvaluation<D, L>>(), null);
		
	}
	
	public ValidationTrainTest(String name, Datum.Tools<D, L> datumTools, FeaturizedDataSet<D, L> trainData, FeaturizedDataSet<D, L> testData) {
		this(name, 1, null, trainData, testData, new ArrayList<SupervisedModelEvaluation<D, L>>(), null);		
	}
	
	@Override
	public List<Double> run() {
		return run(false);
	}
	
	/**
	 * @param skipTraining indicates whether to skip model training
	 * @return trains and evaluates the model, returning a list of values as results
	 * of the evaluations
	 */
	public List<Double> run(boolean skipTraining) {
		OutputWriter output = this.trainData.getDatumTools().getDataTools().getOutputWriter();
		Timer timer = this.trainData.getDatumTools().getDataTools().getTimer();
		
		this.evaluationValues = new ArrayList<Double>(this.evaluations.size());
		for (int i = 0; i < this.evaluations.size(); i++)
			this.evaluationValues.add(-1.0);

		DataFeatureMatrix<D, L> testMatrix = this.testData.toDataFeatureMatrix(this.model.getContext()); 

		if (!skipTraining) {
			timer.startClock(this.name + " Train/Test (Training)");
			output.debugWriteln("Training model (" + this.name + ")");
			DataFeatureMatrix<D, L> trainMatrix = this.trainData.toDataFeatureMatrix(this.model.getContext()); 
			if (!this.model.train(trainMatrix, testMatrix, this.evaluations))
				return this.evaluationValues;
			
			timer.stopClock(this.name + " Train/Test (Training)");
		}
		output.debugWriteln("Classifying data (" + this.name + ")");
		
		timer.startClock(this.name + " Train/Test (Testing)");
		Map<D, L> classifiedData = this.model.classify(testMatrix);
		if (classifiedData == null)
			return this.evaluationValues;
		this.classifiedData = classifiedData;
		
		output.debugWriteln("Computing model score (" + this.name + ")");
		
		for (int i = 0; i < this.evaluations.size(); i++)
			this.evaluationValues.set(i, this.evaluations.get(i).evaluate(this.model, testMatrix, classifiedData));
		
		this.confusionMatrix = new ConfusionMatrix<D, L>(this.model.getValidLabels(), this.model.getLabelMapping());
		this.confusionMatrix.addData(classifiedData);
		
		timer.stopClock(this.name + " Train/Test (Testing)");
		
		return this.evaluationValues;
	}
	
	public Map<D, L> getClassifiedData(){
		return this.classifiedData;
	}
}
