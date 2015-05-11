package edu.cmu.ml.rtw.generic.model.evaluation;

/*import java.io.BufferedReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;*/
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
/*import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;*/

/**
 * FIXME Needs refactoring
 * ValidationKCV performs a k-fold cross validation with 
 * a given model on set of annotated 
 * organization mentions 
 * (http://en.wikipedia.org/wiki/Cross-validation_(statistics)#k-fold_cross-validation).  
 * For each fold, there is an  
 * optional grid-search for hyper-parameter values using
 * edu.cmu.ml.rtw.generic.model.evaluation.GridSearch with
 * (k-2) parts as training, one part as dev, and one part as
 * test data.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class ValidationKCV<D extends Datum<L>, L>/* extends Validation<D, L>*/ {
/*	private List<Feature<D, L>> features;
	private List<DataSet<D, L>> folds;
	private List<GridSearch.GridDimension> gridDimensions; 
	private DataSet<D, L> data;
	
	private List<Pair<GridSearch<D, L>.GridPosition, List<Double>>> gridFoldResults;
	private List<ValidationResult> validationResults;
	
	private int k;
	private boolean trainOnDev;
	private String[] parameters = new String[] { "trainOnDev", "k"};*/
	
	/**
	 * @param name
	 * @param data - Dataset to randomly partition into folds
	 */
/*	public ValidationKCV(String name, DataSet<D, L> data) {
		super(name, data.getDatumTools());
		this.gridDimensions = new ArrayList<GridSearch.GridDimension>();
		this.data = data;
		this.features = new ArrayList<Feature<D, L>>();
	}
	
	@Override
	public List<Double> run() {
		this.folds = this.data.makePartition(this.k, this.data.getDatumTools().getDataTools().getGlobalRandom());
		
		ExecutorService threadPool = Executors.newFixedThreadPool(maxThreads);
		List<ValidationThread> tasks = new ArrayList<ValidationThread>();
		this.validationResults = new ArrayList<ValidationResult>(this.folds.size());
 		for (int i = 0; i < this.folds.size(); i++) {
 			validationResults.add(null);
			tasks.add(new ValidationThread(i, 1));
		}
		
		try {
			List<Future<ValidationResult>> results = threadPool.invokeAll(tasks);
			threadPool.shutdown();
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			for (Future<ValidationResult> futureResult : results) {
				ValidationResult result = futureResult.get();
				if (result == null)
					return null;
				this.validationResults.set(result.getFoldIndex(), result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		this.confusionMatrix = new ConfusionMatrix<D, L>(this.model.getValidLabels(), this.model.getLabelMapping());
		this.evaluationValues = new ArrayList<Double>(this.evaluations.size());
		for (int i = 0; i < this.evaluations.size(); i++) {
			this.evaluationValues.add(0.0);
		}
		
		for (int i = 0; i < validationResults.size(); i++) {
			List<Double> evaluationValues = validationResults.get(i).getEvaluationValues();
			for (int j = 0; j < evaluationValues.size(); j++) {
				this.evaluationValues.set(j , this.evaluationValues.get(j) + evaluationValues.get(j));
			}
			
			this.confusionMatrix.add(validationResults.get(i).getConfusionMatrix());
		}
		
		for (int i = 0; i < this.evaluationValues.size(); i++) {
			this.evaluationValues.set(i, this.evaluationValues.get(i)/this.folds.size());
		}
		
		if (this.gridDimensions.size() > 0) {
			this.gridFoldResults = new ArrayList<Pair<GridSearch<D, L>.GridPosition, List<Double>>>();
			for (int i = 0; i < validationResults.size(); i++) {
				List<GridSearch<D, L>.EvaluatedGridPosition> gridEvaluation = validationResults.get(i).getGridEvaluation();
				for (int j = 0; j < gridEvaluation.size(); j++) {
					if (this.gridFoldResults.size() <= j)
						this.gridFoldResults.add(new Pair<GridSearch<D, L>.GridPosition, List<Double>>(gridEvaluation.get(j), new ArrayList<Double>()));
					this.gridFoldResults.get(j).getSecond().add(gridEvaluation.get(j).getPositionValue());
				}
			}
		}
		
		return this.evaluationValues;
	}
	
	@Override
	public boolean outputResults() {
		DecimalFormat cleanDouble = new DecimalFormat("0.00000");
		OutputWriter output = this.folds.get(0).getDatumTools().getDataTools().getOutputWriter();

		String gridSearchParameters = ((this.gridDimensions.size() > 0) ? this.validationResults.get(0).getBestParameters().toKeyString("\t") + "\t" : "");
		String evaluationsStr = "";
		
		for (int i = 0; i < this.evaluations.size(); i++) {
			evaluationsStr += this.evaluations.get(i).toString() + "\t";
		
		}
		
		output.resultsWriteln("Fold\t" + gridSearchParameters + evaluationsStr);
		
		for (int i = 0; i < this.validationResults.size(); i++) {
			String gridSearchParameterValues = ((this.gridDimensions.size() > 0) ? validationResults.get(i).getBestParameters().toValueString("\t") + "\t" : "");
			String evaluationValuesStr = "";
			for (int j = 0; j < this.validationResults.get(i).evaluationValues.size(); j++) {
				evaluationValuesStr += cleanDouble.format(this.validationResults.get(i).evaluationValues.get(j)) + "\t";
			}
			
			output.resultsWriteln(i + "\t" + gridSearchParameterValues + evaluationValuesStr);
		}
		
		output.resultsWrite("Averages:\t");
		for (int i = 0; i < this.gridDimensions.size(); i++)
			output.resultsWrite("\t");
		for (int i = 0; i < this.evaluationValues.size(); i++) {
			output.resultsWrite(cleanDouble.format(this.evaluationValues.get(i)) + "\t");
		}
		output.resultsWriteln("");
		
		output.resultsWriteln("\nTotal Confusion Matrix:\n " + this.confusionMatrix.toString());
		
		if (this.gridDimensions.size() > 0) {
			output.resultsWriteln("\nGrid search results:");
			output.resultsWrite(this.validationResults.get(0).getGridEvaluation().get(0).toKeyString("\t") + "\t");
			for (int i = 0; i < this.folds.size(); i++)
				output.resultsWrite("Fold " + i + "\t");
			output.resultsWrite("\n");
			
			for (Pair<GridSearch<D, L>.GridPosition, List<Double>> gridFoldResult : this.gridFoldResults) {
				output.resultsWrite(gridFoldResult.getFirst().toValueString("\t") + "\t");
				for (int i = 0; i < gridFoldResult.getSecond().size(); i++) {
					output.resultsWrite(cleanDouble.format(gridFoldResult.getSecond().get(i)) + "\t");
				}
				output.resultsWrite("\n");
			}
		}
		
		return true;
	}
	
	/**
	 * ValidationResult stores the results of training and evaluating 
	 * the model on a single fold
	 * 
	 * @author Bill McDowell
	 *
	 */
/*	private class ValidationResult  {
		private int foldIndex;
		private List<Double> evaluationValues;
		private ConfusionMatrix<D, L> confusionMatrix;
		private List<GridSearch<D, L>.EvaluatedGridPosition> gridEvaluation;
		private GridSearch<D, L>.GridPosition bestParameters;
		
		public ValidationResult(int foldIndex, List<Double> evaluationValues, ConfusionMatrix<D, L> confusionMatrix, List<GridSearch<D, L>.EvaluatedGridPosition> gridEvaluation, GridSearch<D, L>.GridPosition bestParameters) {
			this.foldIndex = foldIndex;
			this.evaluationValues = evaluationValues;
			this.confusionMatrix = confusionMatrix;
			this.gridEvaluation = gridEvaluation;
			this.bestParameters = bestParameters;
		}
		
		public int getFoldIndex() {
			return this.foldIndex;
		}
		
		public List<Double> getEvaluationValues() {
			return this.evaluationValues;
		}
		
		public ConfusionMatrix<D, L> getConfusionMatrix() {
			return this.confusionMatrix;
		}
		
		public List<GridSearch<D, L>.EvaluatedGridPosition> getGridEvaluation() {
			return this.gridEvaluation;
		}
		
		public GridSearch<D, L>.GridPosition getBestParameters() {
			return this.bestParameters;
		}
	}
	
	/**
	 * ValidationThread trains and evaluates the model on a single
	 * fold (with optional single-threaded grid search).
	 * 
	 * @author Bill McDowell
	 *
	 */
/*	private class ValidationThread implements Callable<ValidationResult> {
		private int foldIndex;
		private int maxThreads;
		// fold-specific environment variables that can be referenced by
		// experiment configuration files
		private Map<String, String> parameterEnvironment; 
		
		public ValidationThread(int foldIndex, int maxThreads) {
			this.foldIndex = foldIndex;
			this.maxThreads = maxThreads;
	
			this.parameterEnvironment = new HashMap<String, String>();
			this.parameterEnvironment.putAll(folds.get(foldIndex).getDatumTools().getDataTools().getParameterEnvironment());
			this.parameterEnvironment.put("FOLD", String.valueOf(this.foldIndex));
		}
		
		public ValidationResult call() {
			OutputWriter output = folds.get(foldIndex).getDatumTools().getDataTools().getOutputWriter();
			String namePrefix = name + " Fold " + foldIndex;
			
			/*
			 * Initialize training, dev, and test sets
			 */
/*			output.debugWriteln("Initializing CV data sets for " + name);
			Datum.Tools<D, L> datumTools = folds.get(this.foldIndex).getDatumTools();
			Datum.Tools.LabelMapping<L> labelMapping = folds.get(this.foldIndex).getLabelMapping();
			FeaturizedDataSet<D, L> testData = new FeaturizedDataSet<D, L>(namePrefix + " Test", this.maxThreads, datumTools, labelMapping);
			FeaturizedDataSet<D, L> trainData = new FeaturizedDataSet<D, L>(namePrefix + " Training", this.maxThreads, datumTools, labelMapping);
			FeaturizedDataSet<D, L> devData = new FeaturizedDataSet<D, L>(namePrefix + " Dev", this.maxThreads, datumTools, labelMapping);
			for (int j = 0; j < folds.size(); j++) {
				if (j == this.foldIndex) {
					testData.addAll(folds.get(j));
				} else if (gridDimensions.size() > 0 && j == ((foldIndex + 1) % folds.size())) {
					devData.addAll(folds.get(j));
				} else {
					trainData.addAll(folds.get(j));	
				}
			}
			
			/* Need cloned bunch of features for the fold so that they can be 
			 * reinitialized without affecting other folds' results */
/*			output.debugWriteln("Initializing features for CV fold " + this.foldIndex);
			for (Feature<D, L> feature : features) {
				Feature<D, L> foldFeature = feature.clone(datumTools, this.parameterEnvironment);
				if (!foldFeature.init(trainData))
					return null;
				
				trainData.addFeature(foldFeature);
				devData.addFeature(foldFeature);
				testData.addFeature(foldFeature);
			}
			
			SupervisedModel<D, L> foldModel = model.clone(datumTools, this.parameterEnvironment, true);
			
			output.dataWriteln("--------------- Fold: " + this.foldIndex + " ---------------");
			output.modelWriteln("--------------- Fold: " + this.foldIndex + " ---------------");
			
			/*
			 *  Run either TrainTestValidation or GridSearchTestValidation on the fold
			 */
/*			ValidationResult result = null;
			List<Double> evaluationValues = null;
			if (gridDimensions.size() > 0) {
				ValidationGST<D, L> gridSearchValidation = new ValidationGST<D, L>(namePrefix, this.maxThreads, foldModel, trainData, devData, testData, evaluations, errorExampleExtractor, gridDimensions, true);
				evaluationValues = gridSearchValidation.run();
				result = new ValidationResult(foldIndex, evaluationValues, gridSearchValidation.getConfusionMatrix(), gridSearchValidation.getGridEvaluation(), gridSearchValidation.getBestGridPosition());
			} else {
				ValidationTrainTest<D, L> accuracyValidation = new ValidationTrainTest<D, L>(namePrefix, this.maxThreads, foldModel, trainData, testData, evaluations, errorExampleExtractor);
				evaluationValues = accuracyValidation.run();
				output.modelWriteln(accuracyValidation.getModel().toString());
				result = new ValidationResult(foldIndex, evaluationValues, accuracyValidation.getConfusionMatrix(), null, null);
			}
			
			if (evaluationValues.get(0) < 0)
				output.debugWriteln("Error: Validation failed on fold " + this.foldIndex);
			
			return result;
		}
	}
		
	@Override
	protected boolean setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return true;
	}
	
	@Override
	protected boolean addFeature(Feature<D, L> feature) {
		this.features.add(feature);
		return true;
	}

	@Override
	public String[] getParameterNames() {
		return this.parameters;
	}

	@Override
	public String getParameterValue(String parameter) {
		if (parameter.equals("trainOnDev")) {
			return String.valueOf(this.trainOnDev);
		} else if (parameter.equals("k"))
			return String.valueOf(this.k);
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, String parameterValue,
			Tools<D, L> datumTools) {
		if (parameter.equals("trainOnDev")) {
			this.trainOnDev = Boolean.valueOf(parameterValue);
		} else if (parameter.equals("k")) {
			this.k = Integer.valueOf(parameterValue);
		} else
			return false;
		
		return true;
	}
	
	@Override
	public boolean deserializeNext(BufferedReader reader, String nextName) throws IOException {
		if (nextName.equals("gridDimension")) {
			GridSearch.GridDimension gridDimension = new GridSearch.GridDimension();
			if (!gridDimension.deserialize(reader))
				return false;
			
			this.gridDimensions.add(gridDimension);
		} else {
			return super.deserializeNext(reader, nextName);
		}
		
		return true;
	}*/
}
