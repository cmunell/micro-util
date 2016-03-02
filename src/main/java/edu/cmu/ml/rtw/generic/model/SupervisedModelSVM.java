package edu.cmu.ml.rtw.generic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.BidirectionalLookupTable;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.PlataniosUtil;

/**
 * SupervisedModelSVM represents a multi-class SVM trained with
 * SGD using AdaGrad to determine the learning rate.  The AdaGrad minimization
 * uses sparse updates based on the loss gradients and 'occasional updates'
 * for the regularizer.  It's unclear whether the occasional regularizer
 * gradient updates are theoretically sound when used with AdaGrad (haven't
 * taken the time to think about it), but it seems to work anyway.
 * 
 * Parameters:
 *  l2 - l2 regularization hyper-parameter
 *  
 *  epsilon - if the objective changes less than this value in
 *  an iteration, then the training procedure terminates (although
 *  this functionality may currently be commented out)
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class SupervisedModelSVM<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	protected BidirectionalLookupTable<L, Integer> labelIndices;
	protected int trainingIterations; // number of training iterations for which to run (set through 'extra info')
	protected boolean earlyStopIfNoLabelChange; // whether to have early stopping when no prediction changes on dev set (set through 'extra info')
	protected Map<Integer, String> featureNames; // map from feature indices to their names
	protected int numFeatures; // total number of features
	protected double[] bias_b;
	protected Map<Integer, Double> feature_w; // Labels x (Input features (percepts)) sparse weights mapped from weight indices 
	
	// Adagrad stuff
	protected int t;
	protected Map<Integer, Double> feature_G;  // Just diagonal
	protected double[] bias_G;
	
	protected double l2; // l2 regularizer
	protected double epsilon = 0;
	protected String[] hyperParameterNames = { "l2", "epsilon" };
	
	protected Random random;

	public SupervisedModelSVM() {
		this.featureNames = new HashMap<Integer, String>();
	}
	
	public SupervisedModelSVM(DatumContext<D, L> context) {
		this();
		this.context = context;
	}
	
	protected boolean setLabelIndices() {
		this.labelIndices = new BidirectionalLookupTable<L, Integer>();
		int i = 0;
		for (L label : this.validLabels) {
			this.labelIndices.put(label, i);
			i++;
		}
		return true;
	}

	@Override
	public boolean train(DataFeatureMatrix<D, L> data, DataFeatureMatrix<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();
		
		if (!initializeTraining(data))
			return false;
		
		//double prevObjectiveValue = objectiveValue(data);
		Map<D, L> prevPredictions = classify(testData);
		List<Double> prevEvaluationValues = new ArrayList<Double>();
		for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
			prevEvaluationValues.add(evaluation.evaluate(this, testData, prevPredictions));
		}
		
		output.debugWriteln("Training " + getGenericName() + " for " + this.trainingIterations + " iterations...");
		
		for (int iteration = 0; iteration < this.trainingIterations; iteration++) {
			if (!trainOneIteration(iteration, data)) 
				return false;
			
			if (iteration % 10 == 0) {
				//double objectiveValue = objectiveValue(data);
				//double objectiveValueDiff = objectiveValue - prevObjectiveValue;
				Map<D, L> predictions = classify(testData);
				int labelDifferences = countLabelDifferences(prevPredictions, predictions);
				if (earlyStopIfNoLabelChange && labelDifferences == 0 && iteration > 10)
					break;
			
				List<Double> evaluationValues = new ArrayList<Double>();
				for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
					evaluationValues.add(evaluation.evaluate(this, testData, predictions));
				}
				
				String statusStr = "(l2=" + this.l2 + ") Finished iteration " + iteration + /*" objective diff: " + objectiveValueDiff + " objective: " + objectiveValue + */" prediction-diff: " + labelDifferences + "/" + predictions.size() + " ";
				for (int i = 0; i < evaluations.size(); i++) {
					String evaluationName = evaluations.get(i).toString();
					double evaluationDiff = evaluationValues.get(i) - prevEvaluationValues.get(i);
					statusStr += evaluationName + " diff: " + evaluationDiff + " " + evaluationName + ": " + evaluationValues.get(i) + " ";
				}
					
				output.debugWriteln(statusStr);
				
				/*
				if (iteration > 20 && Math.abs(objectiveValueDiff) < this.epsilon) {
					output.debugWriteln("(l2=" + this.l2 + ") Terminating early at iteration " + iteration);
					break;
				}*/
				
				// prevObjectiveValue = objectiveValue;
				prevPredictions = predictions;
				prevEvaluationValues = evaluationValues;
			} else {
				output.debugWriteln("(l2=" + this.l2 + ") Finished iteration " + iteration);
			}
		}
		
		return true;
	}
	
	protected boolean initializeTraining(DataFeatureMatrix<D, L> data) {
		if (this.feature_w == null) {
			this.t = 1;
			
			this.bias_b = new double[this.validLabels.size()];
			this.numFeatures = data.getFeatures().getFeatureVocabularySize();
			this.feature_w = new HashMap<Integer, Double>(); 	
	
			this.bias_G = new double[this.bias_b.length];
			this.feature_G = new HashMap<Integer, Double>();
		}
		
		this.random = data.getData().getDatumTools().getDataTools().makeLocalRandom();
		
		return true;
	}
	
	/**
	 * @param iteration
	 * @param data
	 * @return true if the model has been trained for a full pass over the
	 * training data set
	 */
	protected boolean trainOneIteration(int iteration, DataFeatureMatrix<D, L> data) {
		List<Integer> dataPermutation = data.getData().constructRandomDataPermutation(this.random);
		
		for (Integer datumId : dataPermutation) {
			D datum = data.getData().getDatumById(datumId);
			L datumLabel = this.mapValidLabel(datum.getLabel());
			L bestLabel = argMaxScoreLabel(data, datum, true);

			if (!trainOneDatum(datum, datumLabel, bestLabel, iteration, data)) {
				return false;
			}
			
			this.t++;
		}
		return true;
	}
	
	/**
	 * 
	 * @param datum
	 * @param datumLabel
	 * @param bestLabel
	 * @param iteration
	 * @param data
	 * @return true if the model has made SGD weight updates from a single datum.
	 */
	protected boolean trainOneDatum(D datum, L datumLabel, L bestLabel, int iteration, DataFeatureMatrix<D, L> data) {
		int N = data.getData().size();
		double K = N/4.0;
		boolean datumLabelBest = datumLabel.equals(bestLabel);
		boolean regularizerUpdate = (this.t % K == 0); // for "occasionality trick"
		
		Map<Integer, Double> datumFeatureValues = PlataniosUtil.vectorToMap(data.getFeatureVocabularyValues(datum));
		
		if (iteration == 0) {
			List<Integer> missingNameKeys = new ArrayList<Integer>();
			for (Integer key : datumFeatureValues.keySet())
				if (!this.featureNames.containsKey(key))
					missingNameKeys.add(key);
			this.featureNames.putAll(data.getFeatures().getFeatureVocabularyNamesForIndices(missingNameKeys));
		}
		
		if (datumLabelBest && !regularizerUpdate) // No update necessary
			return true;
			
		// Update feature weights
		if (!regularizerUpdate) { // Update only for loss function gradients
			for (Entry<Integer, Double> featureValue : datumFeatureValues.entrySet()) {
				int i_datumLabelWeight = getWeightIndex(datumLabel, featureValue.getKey());
				int i_bestLabelWeight = getWeightIndex(bestLabel, featureValue.getKey());
				
				if (!this.feature_w.containsKey(i_datumLabelWeight)) {
					this.feature_w.put(i_datumLabelWeight, 0.0);
					this.feature_G.put(i_datumLabelWeight, 0.0);
				}
				
				if (!this.feature_w.containsKey(i_bestLabelWeight)) {
					this.feature_w.put(i_bestLabelWeight, 0.0);
					this.feature_G.put(i_bestLabelWeight, 0.0);
				}
				
				// Gradients
				double g_datumLabelWeight = -featureValue.getValue();
				double g_bestLabelWeight = featureValue.getValue();
				
				// Adagrad G
				double G_datumLabelWeight = this.feature_G.get(i_datumLabelWeight) + g_datumLabelWeight*g_datumLabelWeight;
				double G_bestLabelWeight = this.feature_G.get(i_bestLabelWeight) + g_bestLabelWeight*g_bestLabelWeight;
				
				this.feature_G.put(i_datumLabelWeight, G_datumLabelWeight);
				this.feature_G.put(i_bestLabelWeight, G_bestLabelWeight);
				
				// Learning rates
				double eta_datumLabelWeight = 1.0/Math.sqrt(G_datumLabelWeight);
				double eta_bestLabelWeight = 1.0/Math.sqrt(G_bestLabelWeight);
				
				// Weight update
				this.feature_w.put(i_datumLabelWeight, this.feature_w.get(i_datumLabelWeight) - eta_datumLabelWeight*g_datumLabelWeight);
				this.feature_w.put(i_bestLabelWeight, this.feature_w.get(i_bestLabelWeight) - eta_bestLabelWeight*g_bestLabelWeight);
			}
		} else { // Full weight update for regularizer
			Map<Integer, Double> g = new HashMap<Integer, Double>(); // gradients
			
			// Gradient update for hinge loss
			for (Entry<Integer, Double> featureValue : datumFeatureValues.entrySet()) {
				int i_datumLabelWeight = getWeightIndex(datumLabel, featureValue.getKey());
				int i_bestLabelWeight = getWeightIndex(bestLabel, featureValue.getKey());
				
				g.put(i_datumLabelWeight, -featureValue.getValue());
				g.put(i_bestLabelWeight, featureValue.getValue());
			}
			
			// Occasional gradient update for regularizer (this happens after every K training datum updates)
			for (Entry<Integer, Double> wEntry : this.feature_w.entrySet()) {
				if (!g.containsKey(wEntry.getKey()))
					g.put(wEntry.getKey(), (K/N)*this.l2*wEntry.getValue());
				else 
					g.put(wEntry.getKey(), g.get(wEntry.getKey()) + (K/N)*this.l2*wEntry.getValue());
			}
			
			// Update weights based on gradients
			for (Entry<Integer, Double> gEntry : g.entrySet()) {
				if (gEntry.getValue() == 0)
					continue;
				
				if (!this.feature_w.containsKey(gEntry.getKey())) {
					this.feature_w.put(gEntry.getKey(), 0.0);
					this.feature_G.put(gEntry.getKey(), 0.0);
				}
				
				// Adagrad G
				double G = this.feature_G.get(gEntry.getKey()) + gEntry.getValue()*gEntry.getValue();
				this.feature_G.put(gEntry.getKey(), G);
				
				double eta = 1.0/Math.sqrt(G);
				this.feature_w.put(gEntry.getKey(), this.feature_w.get(gEntry.getKey()) - eta*gEntry.getValue());
			}
		}
			
		// Update label biases
		for (int i = 0; i < this.bias_b.length; i++) {
			// Bias gradient based on hinge loss
			double g = ((this.labelIndices.get(datumLabel) == i) ? -1.0 : 0.0) +
							(this.labelIndices.get(bestLabel) == i ? 1.0 : 0.0);
			
			if (g == 0)
				continue;
			
			this.bias_G[i] += g*g;
			double eta = 1.0/Math.sqrt(this.bias_G[i]);
			this.bias_b[i] -= eta*g;
		}
		
		return true;
	}
	
	private int countLabelDifferences(Map<D, L> labels1, Map<D, L> labels2) {
		int count = 0;
		for (Entry<D, L> entry: labels1.entrySet()) {
			if (!labels2.containsKey(entry.getKey()) || !entry.getValue().equals(labels2.get(entry.getKey())))
				count++;
		}
		return count;
	}
	
	protected double objectiveValue(DataFeatureMatrix<D, L> data) {
		double value = 0;
		
		if (this.l2 > 0) {
			double l2Norm = 0;
			for (Entry<Integer, Double> wEntry : this.feature_w.entrySet())
				l2Norm += wEntry.getValue()*wEntry.getValue();
			value += l2Norm*this.l2*.5;
		}
		
		for (D datum : data.getData()) {
			double maxScore = maxScoreLabel(data, datum, true);
			double datumScore = scoreLabel(data, datum, datum.getLabel(), false);
			value += maxScore - datumScore;
		}
		
		return value;
	}
	
	protected double maxScoreLabel(DataFeatureMatrix<D, L> data, D datum, boolean includeCost) {
		double maxScore = Double.NEGATIVE_INFINITY;
		for (L label : this.validLabels) {
			double score = scoreLabel(data, datum, label, includeCost);
			if (score >= maxScore) {
				maxScore = score;
			}
		}
		return maxScore;
	}
	
	protected L argMaxScoreLabel(DataFeatureMatrix<D, L> data, D datum, boolean includeCost) {
		double maxScore = Double.NEGATIVE_INFINITY;
		List<L> maxLabels = null; // for breaking ties randomly
		L maxLabel = null;
		for (L label : this.validLabels) {
			double score = scoreLabel(data, datum, label, includeCost);
			
			if (score == maxScore) {
				if (maxLabels == null) {
					maxLabels = new ArrayList<L>();
					if (maxLabel != null) {
						maxLabels.add(maxLabel);
						maxLabel = null;
					}
				}
				maxLabels.add(label);
			} else if (score > maxScore) {
				maxScore = score;
				maxLabel = label;
				maxLabels = null;
			}
		}
		
		if (maxLabels != null)
			return maxLabels.get(this.random.nextInt(maxLabels.size()));
		else
			return maxLabel;
	}
	
	protected double scoreLabel(DataFeatureMatrix<D, L> data, D datum, L label, boolean includeCost) {
		double score = 0;		
		
		Map<Integer, Double> featureValues = PlataniosUtil.vectorToMap(data.getFeatureVocabularyValues(datum));
		int labelIndex = this.labelIndices.get(label);
		for (Entry<Integer, Double> entry : featureValues.entrySet()) {
			int wIndex = this.getWeightIndex(label, entry.getKey());
			if (this.feature_w.containsKey(wIndex))
				score += this.feature_w.get(wIndex)*entry.getValue();
		}
		
		score += this.bias_b[labelIndex];

		if (includeCost) {
			if (!mapValidLabel(datum.getLabel()).equals(label))
				score += 1.0;
		}
		
		return score;
	}
	
	protected int getWeightIndex(L label, int featureIndex) {
		return this.labelIndices.get(label)*this.numFeatures + featureIndex;
	}
	
	protected int getWeightIndex(int labelIndex, int featureIndex) {
		return labelIndex*this.numFeatures + featureIndex;
	}
	
	protected int getFeatureIndex(int weightIndex) {
		return weightIndex % this.numFeatures;
	}
	
	protected int getLabelIndex(int weightIndex) {
		return weightIndex / this.numFeatures;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.hyperParameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("l2"))
			return Obj.stringValue(String.valueOf(this.l2));
		else if (parameter.equals("epsilon"))
			return Obj.stringValue(String.valueOf(this.epsilon));
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("l2"))
			this.l2 = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("epsilon"))
			this.epsilon = Double.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(DatumContext<D, L> context) {
		return new SupervisedModelSVM<D, L>(context);
	}

	@Override
	public String getGenericName() {
		return "SVM";
	}

	@Override
	public Map<D, Map<L, Double>> posterior(DataFeatureMatrix<D, L> data) {
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>(data.getData().size());

		for (D datum : data.getData()) {
			posteriors.put(datum, posteriorForDatum(data, datum));
		}
		
		return posteriors;
	}

	/**
	 * 
	 * @param data
	 * @param datum
	 * @return posterior based on softmax using scores for labels assigned to datum
	 */
	protected Map<L, Double> posteriorForDatum(DataFeatureMatrix<D, L> data, D datum) {
		Map<L, Double> posterior = new HashMap<L, Double>(this.validLabels.size());
		double[] scores = new double[this.validLabels.size()];
		double max = Double.NEGATIVE_INFINITY;
		for (L label : this.validLabels) {
			double score = scoreLabel(data, datum, label, false);
			scores[this.labelIndices.get(label)] = score;
			if (score > max)
				max = score;
		}
		
		double lse = 0;
		for (int i = 0; i < scores.length; i++)
			lse += Math.exp(scores[i] - max);
		lse = max + Math.log(lse);
		
		for (L label : this.validLabels) {
			posterior.put(label, Math.exp(scores[this.labelIndices.get(label)]-lse));
		}
		
		return posterior;
	}
	
	@Override
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		Map<D, L> classifiedData = new HashMap<D, L>();
		
		for (D datum : data.getData()) {
			classifiedData.put(datum, argMaxScoreLabel(data, datum, false));
		}
	
		return classifiedData;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		SupervisedModelSVM<T, Boolean> binaryModelSVM = (SupervisedModelSVM<T, Boolean>)binaryModel;
		
		binaryModelSVM.earlyStopIfNoLabelChange = this.earlyStopIfNoLabelChange;
		binaryModelSVM.trainingIterations = this.trainingIterations;
		binaryModelSVM.setLabelIndices();
		
		return binaryModelSVM;
	}
	
	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		setLabelIndices();
		
		if (internalAssignments.contains("trainingIterations"))
			this.trainingIterations = Integer.valueOf(((Obj.Value)internalAssignments.get("trainingIterations").getValue()).getStr());
		if (internalAssignments.contains("earlyStopIfNoLabelChange"))
			this.earlyStopIfNoLabelChange = Boolean.valueOf(((Obj.Value)internalAssignments.get("earlyStopIfNoLabelChange").getValue()).getStr());
		
		if (!internalAssignments.contains("t") || !internalAssignments.contains("numWeights"))
			return true;
		
		int numWeights = Integer.valueOf(((Obj.Value)internalAssignments.get("numWeights").getValue()).getStr());
		this.numFeatures = numWeights / this.labelIndices.size();
		
		this.t = Integer.valueOf(((Obj.Value)internalAssignments.get("t").getValue()).getStr());
		this.featureNames = new HashMap<Integer, String>();
		
		this.feature_w = new HashMap<Integer, Double>();
		this.feature_G = new HashMap<Integer, Double>();
		
		this.bias_b = new double[this.labelIndices.size()];
		this.bias_G = new double[this.bias_b.length];	
		
		for (int i = 0; i < internalAssignments.size(); i++) {
			Assignment assignment = internalAssignments.get(i);
			if (assignment.getName().startsWith("b_")) {
				Obj.Array bArr = (Obj.Array)assignment.getValue();
				
				double b = Double.valueOf(bArr.getStr(1));
				double G = Double.valueOf(bArr.getStr(2));
				int index = Integer.valueOf(bArr.getStr(3));
				
				this.bias_b[index] = b;
				this.bias_G[index] = G;
			} else if (assignment.getName().startsWith("w_")) {
				Obj.Array wArr = (Obj.Array)assignment.getValue();
				
				String featureName = wArr.getStr(1);
				double w = Double.valueOf(wArr.getStr(2));
				double G = Double.valueOf(wArr.getStr(3));
				int labelIndex = Integer.valueOf(wArr.getStr(4));
				int featureIndex = Integer.valueOf(wArr.getStr(5));
				
				int index = labelIndex*this.numFeatures+featureIndex;
				this.featureNames.put(featureIndex, featureName);
				this.feature_w.put(index, w);
				this.feature_G.put(index, G);
			}
		}
		
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		
		internalAssignments.add(
				Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "trainingIterations", Obj.stringValue(String.valueOf(this.trainingIterations)))
		);
		
		internalAssignments.add(
				Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "earlyStopIfNoLabelChange", Obj.stringValue(String.valueOf(this.earlyStopIfNoLabelChange)))
		);
		
		if (this.numFeatures == 0)
			return internalAssignments;
		
		internalAssignments.add(
			Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "t", Obj.stringValue(String.valueOf(this.t)))
		);
		
		internalAssignments.add(
			Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "numWeights", Obj.stringValue(String.valueOf(this.labelIndices.size()*this.numFeatures)))
		);
		 
		for (int i = 0; i < this.labelIndices.size(); i++) {
			String label = this.labelIndices.reverseGet(i).toString();
			String b = String.valueOf(this.bias_b[i]);
			String G = String.valueOf(this.bias_G[i]);
			String index = String.valueOf(i);
			
			Obj.Array biasArray = Obj.array(new String[] { label, b, G, index });
			internalAssignments.add(
				Assignment.assignmentTyped(null, Context.ObjectType.ARRAY.toString(), "b_" + index, biasArray)
			);
		}
		
		List<Entry<Integer, Double>> wList = new ArrayList<Entry<Integer, Double>>(this.feature_w.entrySet());
		Collections.sort(wList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> e1,
					Entry<Integer, Double> e2) {
				if (Math.abs(e1.getValue()) > Math.abs(e2.getValue()))
					return -1;
				else if (Math.abs(e1.getValue()) < Math.abs(e2.getValue()))
					return 1;
				else
					return 0;
			} });
		
		for (Entry<Integer, Double> weightEntry : wList) {
			if (Double.compare(weightEntry.getValue(), 0.0) == 0)
				continue;
			
			String weightIndex = String.valueOf(weightEntry.getKey());
			int labelIndex = getLabelIndex(weightEntry.getKey());
			String labelIndexStr = String.valueOf(labelIndex);
			int featureIndex = getFeatureIndex(weightEntry.getKey());
			String featureIndexStr = String.valueOf(featureIndex); 
			String label = this.labelIndices.reverseGet(labelIndex).toString();
			String featureName = this.featureNames.get(featureIndex);
			String w = String.valueOf(weightEntry.getValue());
			String G = String.valueOf((this.feature_G.containsKey(weightEntry.getKey())) ? this.feature_G.get(weightEntry.getKey()).doubleValue() : 0.0);
			
			Obj.Array weightArray = Obj.array(new String[] { label, featureName, w, G, labelIndexStr, featureIndexStr });
			internalAssignments.add(
				Assignment.assignmentTyped(null, Context.ObjectType.ARRAY.toString(), "w_" + weightIndex, weightArray)
			);
		}
		
		return internalAssignments;
	}
}
