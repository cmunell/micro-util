package edu.cmu.ml.rtw.generic.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Function;

import org.platanios.learn.data.DataSet;
import org.platanios.learn.data.LabeledDataInstance;
import org.platanios.learn.data.PredictedDataInstance;
import org.platanios.learn.math.matrix.SparseVector;
import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;
import org.platanios.learn.math.matrix.VectorNorm;
import org.platanios.learn.math.matrix.Vectors;
import org.platanios.learn.optimization.AdaptiveGradientSolver;
import org.platanios.learn.optimization.StochasticSolverStepSize;
import org.platanios.learn.optimization.function.AbstractStochasticFunctionUsingDataSet;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnFilteredVocab;
import edu.cmu.ml.rtw.generic.data.feature.FilteredVocabFeatureSet;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.data.feature.rule.RuleSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.PlataniosUtil;

/**
 * 
 * SupervisedModelLGApproximation implements an approximation to the 
 * 'feature grammar' model described in 
 * src/main/resources/docs/feature-grammar/FeatureGrammarNotes.pdf.
 * 
 * Parameters:
 *  t - weight threshold hyper-parameter above which child features are
 *  added to the vocabulary
 *  
 *  rules - set of rules for expanding the feature set
 *  
 *  weightedLabels - indicates whether weighted labels should be used (for EM... but from
 *  the theory, it seems to me that logistic regression cannot be used as part
 *  of an EM procedure in this way because it is not a generative model.  Should
 *  use CEM with non-weighted labels instead).
 *  
 *  l2 - l2-regularization hyper-parameter
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
 *  classificationThreshold - posterior threshold above which an example is labeled
 *  as positive
 *  
 *  computeTestEvaluations - indicates whether test evaluations should be computed
 *  after every evaluationIterations iterations.
 * 
 * 
 * @author Bill McDowell
 *
 * @param <D>
 * @param <L>
 */
public class SupervisedModelLGApproximation<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private boolean weightedLabels = false;
	private double l2 = .00001;
	private double convergenceEpsilon = -1.0;
	private int maxEvaluationConstantIterations = 100000;
	private double maxTrainingExamples = 10000;
	private int batchSize = 100;
	private int evaluationIterations = 500;
	private double classificationThreshold = 0.5;
	private boolean computeTestEvaluations = true;
	private double t = 0.75;
	private RuleSet<D, L> rules;
	private String[] hyperParameterNames = { "t", "rules", "weightedLabels", "l2", "convergenceEpsilon", "maxEvaluationConstantIterations", "maxTrainingExamples", "batchSize", "evaluationIterations", "classificationThreshold", "computeTestEvaluations" };
	
	private Vector w;
	private Map<Integer, String> nonZeroFeatureNamesF_0;
	private List<String> featureNamesConstructed;
	private FilteredVocabFeatureSet<D, L> constructedFeatures; // Features constructed through heuristic rules
	private int sizeF_0;
	
	protected class Likelihood extends AbstractStochasticFunctionUsingDataSet<LabeledDataInstance<Vector, Double>> {
		private Set<Integer> expandedFeatures; 
		private DataFeatureMatrix<D, L> arkDataSet;
		private Map<String, Integer> dataInstanceExtendedVectorSizes;
		private Map<String, Integer> featureNames;
		
		@SuppressWarnings("unchecked")
		public Likelihood(Random random, DataFeatureMatrix<D, L> arkDataSet) {
			this.arkDataSet = arkDataSet;
			this.dataSet = 
					(DataSet<LabeledDataInstance<Vector, Double>>)(DataSet<? extends LabeledDataInstance<Vector, Double>>)
					PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)arkDataSet, SupervisedModelLGApproximation.this.weightedLabels, 1.0/5.0, true, true);
			
			this.random = random;
			// Features that have had heuristic rules applied mapped to the range of children indices (start-inclusive, end-exclusive)
			this.expandedFeatures = new HashSet<Integer>();
			this.dataInstanceExtendedVectorSizes = new HashMap<String, Integer>();
			
			this.featureNames = new HashMap<String, Integer>();
			List<String> featureNameList = this.arkDataSet.getFeatures().getFeatureVocabularyNames();
			for (int i = 0; i < featureNameList.size(); i++)
				this.featureNames.put(featureNameList.get(i), i + 1); // Add one for bias
		}

		@Override
		public Vector estimateGradient(Vector weights, List<LabeledDataInstance<Vector, Double>> dataBatch) {			
			extendDataSet(weights);
			
			Vector gradient = Vectors.build(weights.size(), weights.type());
			for (LabeledDataInstance<Vector, Double> dataInstance : dataBatch) {
				extendDataInstance(dataInstance);
				gradient.saxpyInPlace((1.0 / (1.0 + Math.exp(-weights.dot(dataInstance.features())))) - dataInstance.label(), dataInstance.features());
			}
			
			return gradient;
		}
		
		private void extendDataSet(Vector weights) {
			Set<Integer> featuresToExpand = getFeaturesToExpand(weights);
			RuleSet<D, L> rules = SupervisedModelLGApproximation.this.rules;
			int sizeF_0 = SupervisedModelLGApproximation.this.sizeF_0;
			FilteredVocabFeatureSet<D, L> constructedFeatures = SupervisedModelLGApproximation.this.constructedFeatures;
			
			int startVocabularyIndexBeforeExpansion = sizeF_0 + constructedFeatures.getFeatureVocabularySize();
			int endVocabularyIndex = startVocabularyIndexBeforeExpansion;
			for (Integer featureToExpand : featuresToExpand) {
				this.expandedFeatures.add(featureToExpand);
				
				int dataSetIndex = 0;
				Feature<D, L> featureObj = null;
				int featureStartIndex = 0;
				if (featureToExpand < sizeF_0) {
					dataSetIndex = featureToExpand - 1;
					featureObj = this.arkDataSet.getFeatures().getFeatureByVocabularyIndex(dataSetIndex);
					featureStartIndex = this.arkDataSet.getFeatures().getFeatureStartVocabularyIndex(dataSetIndex);
				} else {
					dataSetIndex = featureToExpand - sizeF_0;
					featureObj = constructedFeatures.getFeatureByVocabularyIndex(dataSetIndex);
					featureStartIndex = constructedFeatures.getFeatureStartVocabularyIndex(dataSetIndex);
					
					if (featureStartIndex < 0) {
						throw new UnsupportedOperationException("ERROR: Failed to find constructed feature by index " + dataSetIndex);
					}
				}
				
				String featureVocabStr = featureObj.getVocabularyTerm(dataSetIndex - featureStartIndex);
				
				Map<String, Obj> featureStrAssignments = new TreeMap<String, Obj>();
				featureStrAssignments.put("SRC_FEATURE", Obj.stringValue(featureObj.getReferenceName()));
				featureStrAssignments.put("SRC_TERM", Obj.stringValue(featureVocabStr));
				featureStrAssignments.put("SRC_FEATURE_TERM", Obj.stringValue(featureObj.getReferenceName() + "_" + featureVocabStr));
				
				Map<String, Obj> featureChildObjs = rules.applyRules(featureObj, featureStrAssignments);
				int startVocabularyIndex = sizeF_0 + constructedFeatures.getFeatureVocabularySize();
				endVocabularyIndex = startVocabularyIndex;
				for (Entry<String, Obj> entry : featureChildObjs.entrySet()) {
					Obj.Function featureChildFunction = (Obj.Function)entry.getValue();
					FeatureTokenSpanFnFilteredVocab<D, L> featureChild = (FeatureTokenSpanFnFilteredVocab<D, L>)this.arkDataSet.getData().getDatumTools().makeFeatureInstance(featureChildFunction.getName(), SupervisedModelLGApproximation.this.context);
					
					if (!featureChild.fromParse(new ArrayList<String>(), null, featureChildFunction))
						throw new UnsupportedOperationException(); // FIXME Throw better exception on return false
					
					featureChild.setFnCacheMode(Fn.CacheMode.ON); // Sets fn results to be cached for all training datums
					
					if (!featureChild.init(this.arkDataSet.getData()))
						throw new UnsupportedOperationException(); // FIXME Throw better exception
					
					List<String> featureChildNames = featureChild.getSpecificShortNames(new ArrayList<String>());
					Set<Integer> featureChildIndicesToRemove = new HashSet<Integer>();
					for (int i = 0; i < featureChildNames.size(); i++) {
						String featureChildName = featureChildNames.get(i);
						
						if (this.featureNames.containsKey(featureChildName)) {
							featureChildIndicesToRemove.add(i);
						} else {
							this.featureNames.put(featureChildName, endVocabularyIndex);
							SupervisedModelLGApproximation.this.featureNamesConstructed.add(featureChildName);
							
							endVocabularyIndex++;
						}
					}
					
					featureChild = new FeatureTokenSpanFnFilteredVocab<D, L>(featureChild, featureChildIndicesToRemove);
					
					if (featureChild.getVocabularySize() == 0)
						continue;
					
					constructedFeatures.addFeature(featureChild); 
					
					SupervisedModelLGApproximation.this.context.getDatumTools().getDataTools().getOutputWriter()
					.debugWriteln("(" + this.arkDataSet.getReferenceName() + ") Feature " + featureToExpand + " (" + featureObj.getReferenceName() + "-" + featureVocabStr + ") w=" + weights.get(featureToExpand) + " expanded with rule " + entry.getKey() + " (" + featureChild.getVocabularySize() + ")...");
				}
			}
		}
		
		private Set<Integer> getFeaturesToExpand(Vector weights) {
			Set<Integer> featuresToExpand = new HashSet<Integer>();
			
			for (VectorElement e_w : weights) {
				if (e_w.index() != 0 &&  Double.compare(Math.abs(e_w.value()), SupervisedModelLGApproximation.this.t) > 0 && !this.expandedFeatures.contains(e_w.index())) {
					featuresToExpand.add(e_w.index());
				}
			}
			
			return featuresToExpand;
		}
		
		private void extendDataInstance(LabeledDataInstance<Vector, Double> dataInstance) {
			FilteredVocabFeatureSet<D, L> constructedFeatures = SupervisedModelLGApproximation.this.constructedFeatures;
			if (constructedFeatures.getFeatureVocabularySize() == 0)
				return;
			if (this.dataInstanceExtendedVectorSizes.containsKey(dataInstance.name()) &&
					this.dataInstanceExtendedVectorSizes.get(dataInstance.name()) == constructedFeatures.getFeatureVocabularySize())
				return;
			
			int dataInstanceExtendedVectorSize = (this.dataInstanceExtendedVectorSizes.containsKey(dataInstance.name())) ? 
													this.dataInstanceExtendedVectorSizes.get(dataInstance.name()) : 0;
													
			int sizeF_0 = SupervisedModelLGApproximation.this.sizeF_0;
			D arkDatum = this.arkDataSet.getData().getDatumById(Integer.valueOf(dataInstance.name()));
			
			Vector extendedDatumValues = constructedFeatures.computeFeatureVocabularyRange(arkDatum, 
					  dataInstanceExtendedVectorSize, 
					  constructedFeatures.getFeatureVocabularySize());

			dataInstance.features().set(sizeF_0 + dataInstanceExtendedVectorSize, 
										sizeF_0 + constructedFeatures.getFeatureVocabularySize() - 1, extendedDatumValues);
			
			this.dataInstanceExtendedVectorSizes.put(dataInstance.name(), constructedFeatures.getFeatureVocabularySize());
		}
	}
	
	public SupervisedModelLGApproximation() {
		
	}
	
	public SupervisedModelLGApproximation(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(DataFeatureMatrix<D, L> data, DataFeatureMatrix<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();
		
		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: LG approximation only supports binary classification.");
			return false;
		}
		
		this.w = new SparseVector(Integer.MAX_VALUE);
		this.sizeF_0 = data.getFeatures().getFeatureVocabularySize() + 1; // Add 1 for bias term
		this.constructedFeatures = new FilteredVocabFeatureSet<D, L>();
		this.featureNamesConstructed = new ArrayList<String>();
		
		List<Double> initEvaluationValues = new ArrayList<Double>();
		for (int i = 0; i < evaluations.size(); i++) {
			initEvaluationValues.add(0.0);
		}
		
		int maximumIterations =  (int)(this.maxTrainingExamples/this.batchSize);	
		
		NumberFormat format = new DecimalFormat("#0.000000");
		
		output.debugWriteln("LG approximation (" + data.getReferenceName() + ") training for at most " + maximumIterations + 
				" iterations (maximum " + this.maxTrainingExamples + " examples over size " + this.batchSize + " batches."/*from " + plataniosData.size() + " examples)"*/);
				
		SupervisedModel<D, L> thisModel = this;
		this.w = new AdaptiveGradientSolver.Builder(new Likelihood(data.getData().getDatumTools().getDataTools().makeLocalRandom(), data), this.w)
						.sampleWithReplacement(false)
						.maximumNumberOfIterations(maximumIterations)
						.maximumNumberOfIterationsWithNoPointChange(5)
						.pointChangeTolerance(this.convergenceEpsilon)
						.checkForPointConvergence(true)
						.additionalCustomConvergenceCriterion(new Function<Vector, Boolean>() {
							int iterations = 0;
							int evaluationConstantIterations = 0;
							Map<D, L> prevPredictions = null;
							List<Double> prevEvaluationValues = initEvaluationValues;
							SupervisedModel<D, L> model = thisModel;
							Vector prevW = null;
							
							@Override
							public Boolean apply(Vector weights) {
								this.iterations++;
								
								if (this.iterations % evaluationIterations != 0) {
									this.prevW = w;
									w = weights;
									return false;
								}
								
								double pointChange = weights.sub(this.prevW).norm(VectorNorm.L2_FAST);
								
								String amountDoneStr = format.format(this.iterations/(double)maximumIterations);
								String pointChangeStr = format.format(pointChange);
								String statusStr = data.getReferenceName() + " (t=" + t + ", l2=" + l2 + ") #" + iterations + 
										" [" + amountDoneStr + "] -- point-change: " + pointChangeStr + " ";
								
								if (!computeTestEvaluations) {
									output.debugWriteln(statusStr);
									return false;
								}

								this.prevW = w;
								w = weights;
								
								Map<D, L> predictions = classify(testData);
								int labelDifferences = countLabelDifferences(prevPredictions, predictions);
								List<Double> evaluationValues = new ArrayList<Double>();
								for (SupervisedModelEvaluation<D, L> evaluation : evaluations) {
									evaluationValues.add(evaluation.evaluate(model, testData, predictions));
								}
								
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
						.batchSize(this.batchSize)
						.stepSize(StochasticSolverStepSize.SCALED)
						.stepSizeParameters(new double[] { 10, 0.75 })
						.useL1Regularization(false)
						.l1RegularizationWeight(0.0)
						.useL2Regularization(true)
						.l2RegularizationWeight(this.l2)
						.loggingLevel(0)
						.build()
						.solve(); 

		Set<Integer> nonZeroWeightIndices = new HashSet<Integer>();
		for (VectorElement e_w : w) {
			if (e_w.index() == 0 || e_w.index() >= this.sizeF_0)
				continue;
			nonZeroWeightIndices.add(e_w.index() - 1);
		}

		this.nonZeroFeatureNamesF_0 = data.getFeatures().getFeatureVocabularyNamesForIndices(nonZeroWeightIndices);
		
		output.debugWriteln("LG approximation (" + data.getReferenceName() + ") finished training."); 
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<D, Map<L, Double>> posterior(DataFeatureMatrix<D, L> data) {
		OutputWriter output = data.getData().getDatumTools().getDataTools().getOutputWriter();

		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: LG approximation only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)data, this.weightedLabels, 0.0, false, true);
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.constructedFeatures.getFeature(i);
			feature.setFnCacheMode(Fn.CacheMode.ON);
		}
		
		for (PredictedDataInstance<Vector, Double> plataniosDatum : plataniosData) {
			int datumId = Integer.parseInt(plataniosDatum.name());
			D datum = data.getData().getDatumById(datumId);
			Vector constructedF = this.constructedFeatures.computeFeatureVocabularyRange(datum, 0, this.constructedFeatures.getFeatureVocabularySize());
			
			if (this.constructedFeatures.getFeatureVocabularySize() != 0)
				plataniosDatum.features().set(this.sizeF_0, this.sizeF_0 + this.constructedFeatures.getFeatureVocabularySize() - 1, constructedF);
			double p = posteriorForDatum(plataniosDatum);
			Map<L, Double> posterior = new HashMap<L, Double>();
			
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
			output.debugWriteln("ERROR: LG approximation only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = PlataniosUtil.makePlataniosDataSet((DataFeatureMatrix<Datum<Boolean>, Boolean>)data, this.weightedLabels, 0.0, false, true);
		Map<D, Boolean> predictions = new HashMap<D, Boolean>();
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.constructedFeatures.getFeature(i);
			feature.setFnCacheMode(Fn.CacheMode.ON);
		}
		
		for (PredictedDataInstance<Vector, Double> plataniosDatum : plataniosData) {
			int datumId = Integer.parseInt(plataniosDatum.name());
			D datum = data.getData().getDatumById(datumId);
			
			if (this.fixedDatumLabels.containsKey(datum)) {
				predictions.put(datum, (Boolean)this.fixedDatumLabels.get(datum));
				continue;
			}
			
			Vector constructedF = this.constructedFeatures.computeFeatureVocabularyRange(datum, 0, this.constructedFeatures.getFeatureVocabularySize());
			
			if (this.constructedFeatures.getFeatureVocabularySize() != 0)
				plataniosDatum.features().set(this.sizeF_0, this.sizeF_0 + this.constructedFeatures.getFeatureVocabularySize() - 1, constructedF);
			
			double p = posteriorForDatum(plataniosDatum);
			
			if (p >= this.classificationThreshold)
				predictions.put(datum, true);
			else 
				predictions.put(datum, false);
		}
		
		return (Map<D, L>)predictions;
	}

	@Override
	public String getGenericName() {
		return "LGApproximation";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("rules"))
			return this.rules.toParse();
		else if (parameter.equals("t"))
			return Obj.stringValue(String.valueOf(this.t));
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
		if (parameter.equals("rules"))
			this.rules = this.context.getMatchRules(parameterValue);
		else if (parameter.equals("t"))
			this.t = Double.valueOf(this.context.getMatchValue(parameterValue));
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
		return new SupervisedModelLGApproximation<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (!internalAssignments.contains("bias") || !internalAssignments.contains("sizeF_0"))
			return true;
		
		Map<Integer, Double> wMap = new HashMap<Integer, Double>();
		
		this.sizeF_0 = Integer.valueOf(((Obj.Value)internalAssignments.get("sizeF_0").getValue()).getStr());
		wMap.put(0, Double.valueOf(((Obj.Value)internalAssignments.get("bias").getValue()).getStr()));
		
		this.constructedFeatures = new FilteredVocabFeatureSet<D, L>(); 
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (assignment.getType().equals(DatumContext.ObjectType.FEATURE.toString())) {
				Obj.Function fnObj = (Obj.Function)assignment.getValue();
				FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.context.getDatumTools().makeFeatureInstance(fnObj.getName(), this.context);
				String referenceName = assignment.getName();
				if (!feature.fromParse(null, referenceName, fnObj))
					return false;
				this.constructedFeatures.addFeature(feature);
			} else if (assignment.getName().startsWith("w_")) {
				Obj.Array wArr = (Obj.Array)assignment.getValue();
				int wIndex = Integer.valueOf(wArr.getStr(2));
				
				double w = Double.valueOf(wArr.getStr(1));
				wMap.put(wIndex, w);
			}
		}
		
		this.w = new SparseVector(Integer.MAX_VALUE, wMap);
		
		return true;
	}
	
	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (this.w == null)
			return internalAssignments;
		
		Map<Integer, Double> wMap = new HashMap<Integer, Double>();
		for (VectorElement e_w : this.w) {
			if (e_w.index() == 0) // Skip bias
				continue;
			wMap.put(e_w.index(), e_w.value());
		}
		
		List<Entry<Integer, Double>> wList = new ArrayList<Entry<Integer, Double>>(wMap.entrySet());
		Collections.sort(wList, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> entry1,
					Entry<Integer, Double> entry2) {
				if (entry1.getValue() > entry2.getValue())
					return -1;
				else if (entry2.getValue() > entry1.getValue())
					return 1;
				else
					return 0;
			} });

		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "sizeF_0", Obj.stringValue(String.valueOf(this.sizeF_0))));
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.VALUE.toString(), "bias", Obj.stringValue(String.valueOf(this.w.get(0)))));
		
		for (Entry<Integer, Double> entry : wList) {
			boolean constructedFeature = entry.getKey() >= this.sizeF_0;
			String feature_i = (!constructedFeature) ? this.nonZeroFeatureNamesF_0.get(entry.getKey() - 1) : this.featureNamesConstructed.get(entry.getKey() - this.sizeF_0);
			String i = String.valueOf(entry.getKey());
			String w_i = String.valueOf(entry.getValue());
			
			Obj.Array weight = Obj.array(new String[] { feature_i, w_i, i });
			internalAssignments.add(Assignment.assignmentTyped(null, Context.ObjectType.ARRAY.toString(), "w_" + entry.getKey() + ((constructedFeature) ? "_c" : ""), weight));
		}
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			Feature<D, L> feature = this.constructedFeatures.getFeature(i);
			internalAssignments.add(Assignment.assignmentTyped(null, DatumContext.ObjectType.FEATURE.toString(), feature.getReferenceName(), feature.toParse()));
		}
		
		this.nonZeroFeatureNamesF_0 = null; // Assumes convert toParse only once... add back in if memory issues
		
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}
	
	private double posteriorForDatum(LabeledDataInstance<Vector, Double> dataInstance) {
		Vector f = dataInstance.features();	
		return 1.0/(1.0 + Math.exp(-this.w.dot(f)));
	}
	
	@Override
	public boolean iterateTraining(DataFeatureMatrix<D, L> data,
			DataFeatureMatrix<D, L> testData,
			List<SupervisedModelEvaluation<D, L>> evaluations,
			Map<D, L> constrainedData) {
		throw new UnsupportedOperationException();
	}
}
