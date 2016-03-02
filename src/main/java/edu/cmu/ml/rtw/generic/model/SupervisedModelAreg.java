package edu.cmu.ml.rtw.generic.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;

import org.platanios.learn.classification.LogisticRegressionAdaGrad;
import org.platanios.learn.data.DataSet;
import org.platanios.learn.data.PredictedDataInstance;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.VectorNorm;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.PlataniosUtil;

/**
 * SupervisedModelAreg is a wrapper around Platanios'
 * 'learn' library AdaGrad implementation of binary
 * logistic regression.
 * 
 * Parameters:
 *  l1 - l1-regularization hyper parameter
 *  
 *  l2 - l2-regularization hyper parameter
 *  
 *  convergenceEpsilon - training procedure terminates if the weight
 *  change in an iteration is below this value
 *  
 *  maxEvaluationConstantIterations - maximum number of training iterations during 
 *  which the test evaluations can be constant without stopping the training procedure
 *  (only works if computeTestEvaluations is true)
 *  
 *  maxTrainingExamples - maximum number of examples that are passed over by AdaGrad
 *  
 *  batchSize - number of examples in each AdaGrad mini-batch iteration
 *  
 *  evaluationIterations - Number of iterations after which evaluations are computed
 *  
 *  weightedLabels - indicates whether weighted labels should be used (for EM... but from
 *  the theory, it seems to me that logistic regression cannot be used as part
 *  of an EM procedure in this way because it is not a generative model.  Should
 *  use CEM with non-weighted labels instead).
 *  
 *  classificationThreshold - posterior threshold above which an example is labeled
 *  as positive
 *  
 *  computeTestEvaluations - indicates whether test evaluations should be computed
 *  after every evaluationIterations iterations.
 *  
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class SupervisedModelAreg<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private double l1;
	private double l2;
	private double convergenceEpsilon = -1.0;
	private int maxEvaluationConstantIterations = 100000;
	private double maxTrainingExamples = 10000;
	private int batchSize = 100;
	private int evaluationIterations = 500;
	private boolean weightedLabels = false;
	private double classificationThreshold = 0.5;
	private boolean computeTestEvaluations = true;
	private String[] hyperParameterNames = { "l1", "l2", "convergenceEpsilon", "maxEvaluationConstantIterations", "maxTrainingExamples", "batchSize", "evaluationIterations", "weightedLabels", "classificationThreshold", "computeTestEvaluations" };
	
	private LogisticRegressionAdaGrad classifier;
	private Vector classifierWeights;
	
	private Map<Integer, String> nonZeroFeatureNames;
	
	public SupervisedModelAreg() {
		
	}
	
	public SupervisedModelAreg(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(DataFeatureMatrix<D, L> data, DataFeatureMatrix<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();
		
		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: Areg only supports binary classification.");
			return false;
		}
		
		@SuppressWarnings("unchecked")
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)data, this.weightedLabels, 1.0/5.0, true, false);
		
		List<Double> initEvaluationValues = new ArrayList<Double>();
		for (int i = 0; i < evaluations.size(); i++) {
			initEvaluationValues.add(0.0);
		}
		
		//int iterationsPerDataSet = plataniosData.size()/this.batchSize;
		int maximumIterations =  (int)(this.maxTrainingExamples/this.batchSize);// (int)Math.floor(this.maxDataSetRuns*iterationsPerDataSet);		
		
		NumberFormat format = new DecimalFormat("#0.000000");
		
		output.debugWriteln("Areg training platanios model...");
		
		SupervisedModel<D, L> thisModel = this;
		this.classifier =
				new LogisticRegressionAdaGrad.Builder(plataniosData.get(0).features().size())
					.sparse(true)
					.useBiasTerm(true)
					.useL1Regularization(this.l1 > 0)
					.useL2Regularization(this.l2 > 0)
					.l1RegularizationWeight(this.l1)
					.l2RegularizationWeight(this.l2)
					.batchSize(this.batchSize)
					.maximumNumberOfIterations(maximumIterations)
					.pointChangeTolerance(this.convergenceEpsilon)
					.additionalCustomConvergenceCriterion(new Function<Vector, Boolean>() {
						int iterations = 0;
						int evaluationConstantIterations = 0;
						Map<D, L> prevPredictions = null;
						List<Double> prevEvaluationValues = initEvaluationValues;
						SupervisedModel<D, L> model = thisModel;
						Vector prevWeights = null;
						
						@Override
						public Boolean apply(Vector weights) {
							this.iterations++;
							
							if (this.iterations % evaluationIterations != 0) {
								this.prevWeights = weights;
								classifierWeights = weights;
								return false;
							}
							
							double pointChange = weights.sub(this.prevWeights).norm(VectorNorm.L2_FAST);
							
							String amountDoneStr = format.format(this.iterations/(double)maximumIterations);
							String pointChangeStr = format.format(pointChange);
							String statusStr = data.getReferenceName() + " (l1=" + l1 + ", l2=" + l2 + ") #" + iterations + 
									" [" + amountDoneStr + "] -- point-change: " + pointChangeStr + " ";
							
							if (!computeTestEvaluations) {
								output.debugWriteln(statusStr);
								return false;
							}
								
							LogisticRegressionAdaGrad tempClassifier = classifier;
							classifier = new LogisticRegressionAdaGrad.Builder(plataniosData.get(0).features().size(), weights)
								.sparse(true)
								.useBiasTerm(true)
								.build();
							
							Map<D, L> predictions = classify(testData);
							int labelDifferences = countLabelDifferences(prevPredictions, predictions);
							List<Double> evaluationValues = new ArrayList<Double>();
							for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
								evaluationValues.add(evaluation.evaluate(model, testData, predictions));
							}
							
							classifier = tempClassifier;
							
							statusStr += " predict-diff: " + labelDifferences + "/" + predictions.size() + " ";
							for (int i = 0; i < evaluations.size(); i++) {
								String evaluationName = evaluations.get(i).getGenericName();
								String evaluationDiffStr = format.format(evaluationValues.get(i) - this.prevEvaluationValues.get(i));
								String evaluationValueStr= format.format(evaluationValues.get(i));
								statusStr += evaluationName + " diff: " + evaluationDiffStr + " " + evaluationName + ": " + evaluationValueStr + " ";
							}
							output.debugWriteln(statusStr);
							
							double evaluationDiff = evaluationValues.get(0) - this.prevEvaluationValues.get(0);
							if (Double.compare(evaluationDiff, 0.0) == 0) {
								this.evaluationConstantIterations += evaluationIterations;
							} else {
								this.evaluationConstantIterations = 0;
							}
								
							this.prevPredictions = predictions;
							this.prevEvaluationValues = evaluationValues;
							this.prevWeights = weights;
							classifierWeights = weights;
							
							if (maxEvaluationConstantIterations < this.evaluationConstantIterations)
								return true;
							
							return false;
						}
						
						private int countLabelDifferences(Map<D, L> labels1, Map<D, L> labels2) {
							if (labels1 == null && labels2 != null)
								return labels2.size();
							if (labels1 != null && labels2 == null)
								return labels1.size();
							if (labels1 == null && labels2 == null)
								return 0;
							
							int count = 0;
							for (Entry<D, L> entry: labels1.entrySet()) {
								if (!labels2.containsKey(entry.getKey()) || !entry.getValue().equals(labels2.get(entry.getKey())))
									count++;
							}
							return count;
						}
						
					})
					.loggingLevel(0)
					.random(data.getData().getDatumTools().getDataTools().makeLocalRandom())
					.build();

		output.debugWriteln(data.getReferenceName() + " training for at most " + maximumIterations + 
				" iterations (maximum " + this.maxTrainingExamples + " examples over size " + this.batchSize + " batches from " + plataniosData.size() + " examples)");
		
		if (!this.classifier.train(plataniosData)) {
			output.debugWriteln("ERROR: Areg failed to train platanios model.");
			return false;
		}
		
		double[] weightsArray = this.classifierWeights.getDenseArray();
		List<Integer> nonZeroWeightIndices = new ArrayList<Integer>();
		for (int i = 0; i < weightsArray.length - 1; i++) {
			if (Double.compare(weightsArray[i], 0) != 0) {
				nonZeroWeightIndices.add(i);
			}
		}
		
		this.nonZeroFeatureNames = data.getFeatures().getFeatureVocabularyNamesForIndices(nonZeroWeightIndices);
		
		output.debugWriteln("Areg finished training platanios model."); 
		
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<D, Map<L, Double>> posterior(DataFeatureMatrix<D, L> data) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();

		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: Areg only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)data, this.weightedLabels, 0.0, false, false);

		plataniosData = this.classifier.predict(plataniosData);
		if (plataniosData == null) {
			output.debugWriteln("ERROR: Areg failed to compute data posteriors.");
			return null;
		}
		
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		for (PredictedDataInstance<Vector, Double> prediction : plataniosData) {
			int datumId = Integer.parseInt(prediction.name());
			D datum = data.getData().getDatumById(datumId);
			
			Map<L, Double> posterior = new HashMap<L, Double>();
			double p = (prediction.label() == 1) ? prediction.probability() : 1.0 - prediction.probability();
			
			// Offset bias term according to classification threshold so that
			// output posterior p >= 0.5 iff model_p >= classification threshold
			// i.e. log (p/(1-p)) = log (model_p/(1-model_p)) - log (threshold/(1-threshold))
			p = p*(1.0-this.classificationThreshold)/(this.classificationThreshold*(1.0-p)+p*(1.0-this.classificationThreshold));
			
			posterior.put((L)(new Boolean(true)), p);
			posterior.put((L)(new Boolean(false)), 1.0-p);
			
			posteriors.put(datum, posterior);
		}

		return posteriors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();

		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: Areg only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)data, this.weightedLabels, 0.0, false, false);

		plataniosData = this.classifier.predict(plataniosData);
		if (plataniosData == null) {
			output.debugWriteln("ERROR: Areg failed to compute data classifications.");
			return null;
		}
		
		Map<D, Boolean> predictions = new HashMap<D, Boolean>();
		for (PredictedDataInstance<Vector, Double> prediction : plataniosData) {
			int datumId = Integer.parseInt(prediction.name());
			D datum = data.getData().getDatumById(datumId);
		
			if (this.fixedDatumLabels.containsKey(datum)) {
				predictions.put(datum, (Boolean)this.fixedDatumLabels.get(datum));
				continue;
			}
			
			double p = (prediction.label() == 1) ? prediction.probability() : 1.0 - prediction.probability();
			if (p >= this.classificationThreshold)
				predictions.put(datum, true);
			else 
				predictions.put(datum, false);
		}
		
		return (Map<D, L>)predictions;
	}

	@Override
	public String getGenericName() {
		return "Areg";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("l1"))
			return Obj.stringValue(String.valueOf(this.l1));
		else if (parameter.equals("l2"))
			return Obj.stringValue(String.valueOf(this.l2));
		else if (parameter.equals("convergenceEpsilon"))
			return Obj.stringValue(String.valueOf(this.convergenceEpsilon));
		else if (parameter.equals("maxTrainingExamples"))
			return Obj.stringValue(String.valueOf(this.maxTrainingExamples));
		else if (parameter.equals("batchSize"))
			return Obj.stringValue(String.valueOf(this.batchSize));
		else if (parameter.equals("evaluationIterations"))
			return Obj.stringValue(String.valueOf(this.evaluationIterations));
		else if (parameter.equals("maxEvaluationConstantIterations"))
			return Obj.stringValue(String.valueOf(this.maxEvaluationConstantIterations));
		else if (parameter.equals("weightedLabels"))
			return Obj.stringValue(String.valueOf(this.weightedLabels));
		else if (parameter.equals("classificationThreshold"))
			return Obj.stringValue(String.valueOf(this.classificationThreshold));
		else if (parameter.equals("computeTestEvaluations"))
			return Obj.stringValue(String.valueOf(this.computeTestEvaluations));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("l1"))
			this.l1 = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("l2"))
			this.l2 = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("convergenceEpsilon"))
			this.convergenceEpsilon = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("maxTrainingExamples"))
			this.maxTrainingExamples = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("batchSize"))
			this.batchSize = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("evaluationIterations"))
			this.evaluationIterations = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("maxEvaluationConstantIterations"))
			this.maxEvaluationConstantIterations = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("weightedLabels"))
			this.weightedLabels = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("classificationThreshold"))
			this.classificationThreshold = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("computeTestEvaluations"))
			this.computeTestEvaluations = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.hyperParameterNames;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(DatumContext<D, L> context) {
		return new SupervisedModelAreg<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (!internalAssignments.contains("featureVocabularySize")
				|| !internalAssignments.contains("nonZeroWeights")
				|| !internalAssignments.contains("bias"))
			return true;
		
		int weightVectorSize = Integer.valueOf(((Obj.Value)internalAssignments.get("featureVocabularySize").getValue()).getStr());
		double bias = Double.valueOf(((Obj.Value)internalAssignments.get("bias").getValue()).getStr());
		
		TreeMap<Integer, Double> nonZeroWeights = new TreeMap<Integer, Double>();
		this.nonZeroFeatureNames = new HashMap<Integer, String>();
		for (int i = 0; i < internalAssignments.size(); i++) {
			Assignment assignment = internalAssignments.get(i);
			if (assignment.getName().startsWith("w_")) {
				Obj.Array wArray = (Obj.Array)assignment.getValue();
				String name = wArray.getStr(0);
				int index = Integer.valueOf(wArray.getStr(2));
				double w = Double.valueOf(wArray.getStr(1));
				
				this.nonZeroFeatureNames.put(index, name);
				nonZeroWeights.put(index, w);
			}
		} 
		
		nonZeroWeights.put(weightVectorSize-1, bias);
		
		this.classifierWeights = new SparseVector(weightVectorSize, nonZeroWeights);
		this.classifier = new LogisticRegressionAdaGrad.Builder(weightVectorSize - 1, this.classifierWeights)
			.sparse(true)
			.useBiasTerm(true)
			.build();
		
		return true;
	}
	
	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (this.classifierWeights == null)
			return internalAssignments;
		
		double[] weightArray = this.classifierWeights.getDenseArray(); 
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "featureVocabularySize", Obj.stringValue(String.valueOf(weightArray.length))));
		
		List<Pair<Integer, Double>> sortedWeights = new ArrayList<Pair<Integer, Double>>();
		for (Integer index : this.nonZeroFeatureNames.keySet()) {
			sortedWeights.add(new Pair<Integer, Double>(index, weightArray[index]));
		}
		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "nonZeroWeights", Obj.stringValue(String.valueOf(this.nonZeroFeatureNames.size()))));
		
		Collections.sort(sortedWeights, new Comparator<Pair<Integer, Double>>() {
			@Override
			public int compare(Pair<Integer, Double> w1,
					Pair<Integer, Double> w2) {
				if (Math.abs(w1.getSecond()) > Math.abs(w2.getSecond()))
					return -1;
				else if (Math.abs(w1.getSecond()) < Math.abs(w2.getSecond()))
					return 1;
				else
					return 0;
			} });
		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "bias", Obj.stringValue(String.valueOf(weightArray[weightArray.length - 1]))));
		
		for (Pair<Integer, Double> weight : sortedWeights) {
			double w = weight.getSecond();
			int index = weight.getFirst();
			String featureName = this.nonZeroFeatureNames.get(index);
			Obj.Array weightArr = Obj.array(new String[]{ featureName, String.valueOf(w), String.valueOf(index) });
			internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.ARRAY.toString(), "w_" + index, weightArr));
		}
		
		// this.nonZeroFeatureNames = null; Assumes convert toParse only once... add back in if memory issues
		
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}
}
