package edu.cmu.ml.rtw.generic.model;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.platanios.learn.optimization.AdaptiveGradientSolver;
import org.platanios.learn.optimization.StochasticSolverStepSize;
import org.platanios.learn.optimization.function.AbstractStochasticFunctionUsingDataSet;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeatureTokenSpanFnFilteredVocab;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.data.feature.FilteredVocabFeatureSet;
import edu.cmu.ml.rtw.generic.data.feature.fn.Fn;
import edu.cmu.ml.rtw.generic.data.feature.rule.RuleSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.Assignment.AssignmentTyped;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;

public class SupervisedModelLogistmarGramression<D extends Datum<L>, L> extends SupervisedModel<D, L> {
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
	
	private Vector u;
	private Map<Integer, String> nonZeroFeatureNamesF_0;
	private List<String> featureNamesConstructed;
	private Map<Integer, List<Integer>> childToParentFeatureMap; 
	private FilteredVocabFeatureSet<D, L> constructedFeatures; // Features constructed through heuristic rules
	private int sizeF_0;
	
	protected class Likelihood extends AbstractStochasticFunctionUsingDataSet<LabeledDataInstance<Vector, Double>> {
		private Map<Integer, List<Integer>> expandedFeatures; 
		private FeaturizedDataSet<D, L> arkDataSet;
		private Map<String, Integer> dataInstanceExtendedVectorSizes;
		private Map<String, Integer> featureNames;
		
		@SuppressWarnings("unchecked")
		public Likelihood(Random random, FeaturizedDataSet<D, L> arkDataSet) {
			this.arkDataSet = arkDataSet;
			this.dataSet = 
					(DataSet<LabeledDataInstance<Vector, Double>>)(DataSet<? extends LabeledDataInstance<Vector, Double>>)
					arkDataSet.makePlataniosDataSet(SupervisedModelLogistmarGramression.this.weightedLabels, 1.0/5.0, true, true);
			
			this.random = random;
			// Features that have had heuristic rules applied mapped to the range of children indices (start-inclusive, end-exclusive)
			this.expandedFeatures = new HashMap<Integer, List<Integer>>();
			this.dataInstanceExtendedVectorSizes = new HashMap<String, Integer>();
			
			this.featureNames = new HashMap<String, Integer>();
			List<String> featureNameList = this.arkDataSet.getFeatureVocabularyNames();
			for (int i = 0; i < featureNameList.size(); i++)
				this.featureNames.put(featureNameList.get(i), i + 1); // Add one for bias
		}

		@Override
		public Vector estimateGradient(Vector weights, List<LabeledDataInstance<Vector, Double>> dataBatch) {
			double l2 = SupervisedModelLogistmarGramression.this.l2;
			
			Pair<Vector, Vector> uPosNeg = splitPosNeg(weights);
			Vector u_p = uPosNeg.getFirst();
			Vector u_n = uPosNeg.getSecond();
	
			Pair<Vector, Vector> cPosNeg = c(u_p, u_n);
			Vector c_p = cPosNeg.getFirst();
			Vector c_n = cPosNeg.getSecond();
			
			extendDataSet(c_p, c_n);
			
			Map<Integer, Double> mapG_n = new HashMap<Integer, Double>();
			Map<Integer, Double> mapG_p = new HashMap<Integer, Double>();
			
			// NOTE Non-zero elements of gc/gf's' are at f=f' and f\in H^*(f') (if f' is unexpanded, then gc/gf' is 
			// not relevant to element of gradient
			
			Map<Integer, Vector> gC_pp = new HashMap<Integer, Vector>(this.expandedFeatures.size() * 2);
			Map<Integer, Vector> gC_np = new HashMap<Integer, Vector>(this.expandedFeatures.size() * 2);
			Map<Integer, Vector> gC_pn = new HashMap<Integer, Vector>(this.expandedFeatures.size() * 2);
			Map<Integer, Vector> gC_nn = new HashMap<Integer, Vector>(this.expandedFeatures.size() * 2);
			for (Integer expandedFeatureIndex : this.expandedFeatures.keySet()) {
				Pair<Vector, Vector> gCPosNeg_p = gC(expandedFeatureIndex, true, c_p, c_n, u_p, u_n);
				Pair<Vector, Vector> gCPosNeg_n = gC(expandedFeatureIndex, false, c_p, c_n, u_p, u_n);
				gC_pp.put(expandedFeatureIndex, gCPosNeg_p.getFirst());
				gC_np.put(expandedFeatureIndex, gCPosNeg_p.getSecond());
				gC_pn.put(expandedFeatureIndex, gCPosNeg_n.getFirst());
				gC_nn.put(expandedFeatureIndex, gCPosNeg_n.getSecond());
				
				mapG_p.put(expandedFeatureIndex, l2*(c_p.dot(gCPosNeg_p.getFirst())+c_n.dot(gCPosNeg_p.getSecond())));
				mapG_n.put(expandedFeatureIndex, l2*(c_p.dot(gCPosNeg_n.getFirst())+c_n.dot(gCPosNeg_n.getSecond())));
			}
			
			for (VectorElement e_c_p : c_p) {
				if (!this.expandedFeatures.containsKey(e_c_p.index())) {
					mapG_p.put(e_c_p.index(), l2*e_c_p.value());
				}
			}
			
			for (VectorElement e_c_n : c_n) {
				if (!this.expandedFeatures.containsKey(e_c_n.index())) {
					mapG_n.put(e_c_n.index(), l2*e_c_n.value());
				}
			}
			
			for (LabeledDataInstance<Vector, Double> dataInstance : dataBatch) {
				extendDataInstance(dataInstance);
				
				Vector f = dataInstance.features();
				double r = posteriorForDatum(dataInstance, c_p, c_n);
				double y = dataInstance.label();
			
				for (VectorElement e_f : f) {
					if (!this.expandedFeatures.containsKey(e_f.index())) {
						if (!mapG_n.containsKey(e_f.index())) {
							mapG_n.put(e_f.index(), 0.0);
						}
						
						if (!mapG_p.containsKey(e_f.index())) {
							mapG_p.put(e_f.index(), 0.0);
						}
						
						mapG_n.put(e_f.index(), mapG_n.get(e_f.index()) + e_f.value()*(y-r));
						mapG_p.put(e_f.index(), mapG_p.get(e_f.index()) + e_f.value()*(r-y));
					} 
				}
				
				for (Integer expandedFeatureIndex : this.expandedFeatures.keySet()) {
					Vector gC_pp_fI = gC_pp.get(expandedFeatureIndex);
					Vector gC_np_fI = gC_np.get(expandedFeatureIndex);
					Vector gC_pn_fI = gC_pp.get(expandedFeatureIndex);
					Vector gC_nn_fI = gC_nn.get(expandedFeatureIndex);
				
					if (!mapG_n.containsKey(expandedFeatureIndex)) {
						mapG_n.put(expandedFeatureIndex, 0.0);
					}
					
					if (!mapG_p.containsKey(expandedFeatureIndex)) {
						mapG_p.put(expandedFeatureIndex, 0.0);
					}

					mapG_p.put(expandedFeatureIndex, mapG_p.get(expandedFeatureIndex) + f.dot(gC_pp_fI.mult(r-y).addInPlace(gC_np_fI.mult(y-r))));
					mapG_n.put(expandedFeatureIndex, mapG_n.get(expandedFeatureIndex) + f.dot(gC_pn_fI.mult(r-y).addInPlace(gC_nn_fI.mult(y-r))));
				}
			}
			
			return joinPosNeg(new SparseVector(Integer.MAX_VALUE, mapG_p), new SparseVector(Integer.MAX_VALUE, mapG_n));
		}
		
		private Pair<Vector, Vector> gC(int wrt_fI, boolean wrt_p, Vector c_p, Vector c_n, Vector u_p, Vector u_n) {
			Map<Integer, Double> gC_p = new HashMap<Integer, Double>();
			Map<Integer, Double> gC_n = new HashMap<Integer, Double>();
			
			// f = f', s = s'
			if (wrt_fI < SupervisedModelLogistmarGramression.this.sizeF_0) {
				// f in F_0
				if (wrt_p) {
					gC_p.put(wrt_fI, 1.0);
					gC_n.put(wrt_fI, 0.0);
				} else {
					gC_n.put(wrt_fI, 1.0);
					gC_p.put(wrt_fI, 0.0);
				}
			} else {
				// f not in F_0
				List<Integer> parentFeatureIndices = SupervisedModelLogistmarGramression.this.childToParentFeatureMap.get(wrt_fI);
				double maxValue = 0.0;
				for (Integer parentFeatureIndex : parentFeatureIndices) {
					maxValue = Math.max(maxValue, c_p.get(parentFeatureIndex) - SupervisedModelLogistmarGramression.this.t);
					maxValue = Math.max(maxValue, c_n.get(parentFeatureIndex) - SupervisedModelLogistmarGramression.this.t);
				}
				
				if (Double.compare(maxValue, 0.0) != 0) {
					if (wrt_p) {
						gC_p.put(wrt_fI, maxValue);
						gC_n.put(wrt_fI, 0.0);
					} else {
						gC_n.put(wrt_fI, maxValue);
						gC_p.put(wrt_fI, 0.0);
					}
				}
			}
			
			// f != f', f\in H^*(f')
			Queue<Integer> toVisit = new LinkedList<Integer>();
			if (this.expandedFeatures.containsKey(wrt_fI))
				toVisit.addAll(this.expandedFeatures.get(wrt_fI));
			
			while (!toVisit.isEmpty()) {
				Integer next_fI = toVisit.remove();
				
				gCHelper(next_fI, wrt_fI, wrt_p, c_p, c_n, u_p, u_n, gC_p, gC_n);
	
				if (this.expandedFeatures.containsKey(next_fI)) {
					toVisit.addAll(this.expandedFeatures.get(next_fI));
				}
			}
			
			return new Pair<Vector, Vector>(new SparseVector(Integer.MAX_VALUE, gC_p), new SparseVector(Integer.MAX_VALUE, gC_n));
		}

		private void gCHelper(int fI, int wrt_fI, boolean wrt_p, Vector c_p, Vector c_n, Vector u_p, Vector u_n, Map<Integer, Double> gC_p, Map<Integer, Double> gC_n) {
			int sizeF_0 = SupervisedModelLogistmarGramression.this.sizeF_0;
			if (fI < sizeF_0)
				return;
			if (gC_p.containsKey(fI) || gC_n.containsKey(fI))
				return;
			
			double u_p_i = u_p.get(fI);
			double u_n_i = u_n.get(fI);
			
			if (Double.compare(u_p_i, 0.0) == 0 && Double.compare(u_n_i, 0.0) == 0)
				return;
			
			List<Integer> parentFeatureIndices = SupervisedModelLogistmarGramression.this.childToParentFeatureMap.get(fI);
			double maxValue = 0.0;
			int maxParentFeatureIndex = -1;
			boolean maxP = true;

			for (Integer parentFeatureIndex : parentFeatureIndices) {
				double value_p = c_p.get(parentFeatureIndex) - SupervisedModelLogistmarGramression.this.t;
				if (value_p > maxValue) {
					maxValue = value_p;
					maxParentFeatureIndex = parentFeatureIndex;
					maxP = true;
				}
				
				double value_n = c_n.get(parentFeatureIndex) - SupervisedModelLogistmarGramression.this.t;
				if (value_n > maxValue) {
					maxValue = value_n;
					maxParentFeatureIndex = parentFeatureIndex;
					maxP = false;
				}
			}
			
			if (maxParentFeatureIndex >= 0) {
				gCHelper(maxParentFeatureIndex, wrt_fI, wrt_p, c_p, c_n, u_p, u_n, gC_p, gC_n);
				
				double parentDerivative = 0.0;
				if (maxP && gC_p.containsKey(maxParentFeatureIndex)) {
					parentDerivative = gC_p.get(maxParentFeatureIndex);
				} else if (gC_n.containsKey(maxParentFeatureIndex)) {
					parentDerivative = gC_n.get(maxParentFeatureIndex);
				}
				
				if (Double.compare(parentDerivative, 0.0) != 0) {
					gC_p.put(fI, u_p_i*parentDerivative);
					gC_n.put(fI, u_n_i*parentDerivative);
				}
			}
		}
		
		private void extendDataSet(Vector c_p, Vector c_n) {
			Set<Integer> featuresToExpand = getFeaturesToExpand(c_p, c_n);
			RuleSet<D, L> rules = SupervisedModelLogistmarGramression.this.rules;
			int sizeF_0 = SupervisedModelLogistmarGramression.this.sizeF_0;
			FilteredVocabFeatureSet<D, L> constructedFeatures = SupervisedModelLogistmarGramression.this.constructedFeatures;
			Map<Integer, List<Integer>> childToParentFeatureMap = SupervisedModelLogistmarGramression.this.childToParentFeatureMap;
			
			int startVocabularyIndexBeforeExpansion = sizeF_0 + constructedFeatures.getFeatureVocabularySize();
			int endVocabularyIndex = startVocabularyIndexBeforeExpansion;
			for (Integer featureToExpand : featuresToExpand) {
				this.expandedFeatures.put(featureToExpand, new LinkedList<Integer>());
				
				int dataSetIndex = 0;
				Feature<D, L> featureObj = null;
				int featureStartIndex = 0;
				if (featureToExpand < sizeF_0) {
					dataSetIndex = featureToExpand - 1;
					featureObj = this.arkDataSet.getFeatureByVocabularyIndex(dataSetIndex);
					featureStartIndex = this.arkDataSet.getFeatureStartVocabularyIndex(dataSetIndex);
				} else {
					dataSetIndex = featureToExpand - sizeF_0;
					featureObj = constructedFeatures.getFeatureByVocabularyIndex(dataSetIndex);
					featureStartIndex = constructedFeatures.getFeatureStartVocabularyIndex(dataSetIndex);
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
					FeatureTokenSpanFnFilteredVocab<D, L> featureChild = (FeatureTokenSpanFnFilteredVocab<D, L>)this.arkDataSet.getDatumTools().makeFeatureInstance(featureChildFunction.getName(), SupervisedModelLogistmarGramression.this.context);
					
					if (!featureChild.fromParse(new ArrayList<String>(), null, featureChildFunction))
						throw new UnsupportedOperationException(); // FIXME Throw better exception on return false
					
					featureChild.setFnCacheMode(Fn.CacheMode.ON); // Sets fn results to be cached for all training datums
					
					if (!featureChild.init(this.arkDataSet))
						throw new UnsupportedOperationException(); // FIXME Throw better exception
					
					List<String> featureChildNames = featureChild.getSpecificShortNames(new ArrayList<String>());
					Set<Integer> featureChildIndicesToRemove = new HashSet<Integer>();
					for (int i = 0; i < featureChildNames.size(); i++) {
						String featureChildName = featureChildNames.get(i);
						if (this.featureNames.containsKey(featureChildName)) {
							featureChildIndicesToRemove.add(i);
							int existingFeatureIndex = this.featureNames.get(featureChildName);
							if (existingFeatureIndex >= sizeF_0 && !pathExists(existingFeatureIndex, featureToExpand)) {
								this.expandedFeatures.get(featureToExpand).add(existingFeatureIndex);
								childToParentFeatureMap.get(existingFeatureIndex).add(featureToExpand);
							}
						} else {
							this.expandedFeatures.get(featureToExpand).add(endVocabularyIndex);
							if (!childToParentFeatureMap.containsKey(endVocabularyIndex))
								childToParentFeatureMap.put(endVocabularyIndex, new LinkedList<Integer>());
							childToParentFeatureMap.get(endVocabularyIndex).add(featureToExpand);
							this.featureNames.put(featureChildName, endVocabularyIndex);
							SupervisedModelLogistmarGramression.this.featureNamesConstructed.add(featureChildName);
							
							endVocabularyIndex++;
						}
					}
					
					featureChild = new FeatureTokenSpanFnFilteredVocab<D, L>(featureChild, featureChildIndicesToRemove);
					
					if (featureChild.getVocabularySize() == 0)
						continue;
					
					constructedFeatures.addFeature(featureChild); 
				
					SupervisedModelLogistmarGramression.this.context.getDatumTools().getDataTools().getOutputWriter()
					.debugWriteln("(" + this.arkDataSet.getName() + ") Feature " + featureToExpand + " (" + featureObj.getReferenceName() + "-" + featureVocabStr + ") c=(" + c_p.get(featureToExpand) + "," + c_n.get(featureToExpand) + ") expanded with rule " + entry.getKey() + "...");
				}
			}
		}
		
		private boolean pathExists(int startIndex, int endIndex) {
			Queue<Integer> toVisit = new LinkedList<Integer>();
			toVisit.add(startIndex);
			
			while (toVisit.size() > 0) {
				int nextIndex = toVisit.remove();
				
				if (nextIndex == endIndex)
					return true;
				
				if (this.expandedFeatures.containsKey(nextIndex))
					toVisit.addAll(this.expandedFeatures.get(nextIndex));
			}
			
			return false;
		}
		
		private Set<Integer> getFeaturesToExpand(Vector c_p, Vector c_n) {
			Set<Integer> featuresToExpand = new HashSet<Integer>();
			
			for (VectorElement e_p : c_p) {
				if (e_p.index() != 0 && e_p.value() > SupervisedModelLogistmarGramression.this.t && !this.expandedFeatures.containsKey(e_p.index()))
					featuresToExpand.add(e_p.index());
			}
			
			for (VectorElement e_n : c_n) {
				if (e_n.index() != 0 && e_n.value() > SupervisedModelLogistmarGramression.this.t && !this.expandedFeatures.containsKey(e_n.index()))
					featuresToExpand.add(e_n.index());
			}
			
			return featuresToExpand;
		}
		
		private void extendDataInstance(LabeledDataInstance<Vector, Double> dataInstance) {
			FilteredVocabFeatureSet<D, L> constructedFeatures = SupervisedModelLogistmarGramression.this.constructedFeatures;
			if (constructedFeatures.getFeatureVocabularySize() == 0)
				return;
			if (this.dataInstanceExtendedVectorSizes.containsKey(dataInstance.name()) &&
					this.dataInstanceExtendedVectorSizes.get(dataInstance.name()) == constructedFeatures.getFeatureVocabularySize())
				return;
			
			int dataInstanceExtendedVectorSize = (this.dataInstanceExtendedVectorSizes.containsKey(dataInstance.name())) ? 
													this.dataInstanceExtendedVectorSizes.get(dataInstance.name()) : 0;
													
			int sizeF_0 = SupervisedModelLogistmarGramression.this.sizeF_0;
			D arkDatum = this.arkDataSet.getDatumById(Integer.valueOf(dataInstance.name()));
			
			Vector extendedDatumValues = constructedFeatures.computeFeatureVocabularyRange(arkDatum, 
					  dataInstanceExtendedVectorSize, 
					  constructedFeatures.getFeatureVocabularySize());

			dataInstance.features().set(sizeF_0 + dataInstanceExtendedVectorSize, 
										sizeF_0 + constructedFeatures.getFeatureVocabularySize() - 1, extendedDatumValues);
			
			this.dataInstanceExtendedVectorSizes.put(dataInstance.name(), constructedFeatures.getFeatureVocabularySize());
		}
	}
	
	public SupervisedModelLogistmarGramression() {
		
	}
	
	public SupervisedModelLogistmarGramression(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		
		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: LogistmarGramression only supports binary classification.");
			return false;
		}
		
		this.u = new SparseVector(Integer.MAX_VALUE);
		this.childToParentFeatureMap = new HashMap<Integer, List<Integer>>();
		this.sizeF_0 = data.getFeatureVocabularySize() + 1; // Add 1 for bias term
		this.constructedFeatures = new FilteredVocabFeatureSet<D, L>();
		this.featureNamesConstructed = new ArrayList<String>();
		
		List<Double> initEvaluationValues = new ArrayList<Double>();
		for (int i = 0; i < evaluations.size(); i++) {
			initEvaluationValues.add(0.0);
		}
		
		int maximumIterations =  (int)(this.maxTrainingExamples/this.batchSize);	
		
		NumberFormat format = new DecimalFormat("#0.000000");
		
		output.debugWriteln("Logistmar gramression (" + data.getName() + ") training for at most " + maximumIterations + 
				" iterations (maximum " + this.maxTrainingExamples + " examples over size " + this.batchSize + " batches."/*from " + plataniosData.size() + " examples)"*/);
				
		SupervisedModel<D, L> thisModel = this;
		this.u = new AdaptiveGradientSolver.Builder(new Likelihood(data.getDatumTools().getDataTools().makeLocalRandom(), data), this.u)
						.lowerBound(0)
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
							Vector prevU = null;
							
							@Override
							public Boolean apply(Vector weights) {
								this.iterations++;
								
								if (this.iterations % evaluationIterations != 0) {
									this.prevU = u;
									u = weights;
									return false;
								}
								
								double pointChange = weights.sub(this.prevU).norm(VectorNorm.L2_FAST);
								
								String amountDoneStr = format.format(this.iterations/(double)maximumIterations);
								String pointChangeStr = format.format(pointChange);
								String statusStr = data.getName() + " (t=" + t + ", l2=" + l2 + ") #" + iterations + 
										" [" + amountDoneStr + "] -- point-change: " + pointChangeStr + " ";
								
								if (!computeTestEvaluations) {
									output.debugWriteln(statusStr);
									return false;
								}

								this.prevU = u;
								u = weights;
								
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
						.useL2Regularization(false)
						.l2RegularizationWeight(0.0)
						.loggingLevel(0)
						.build()
						.solve(); 

		Pair<Vector, Vector> uPosNeg = splitPosNeg(this.u);
		Vector u_p = uPosNeg.getFirst();
		Vector u_n = uPosNeg.getSecond();
		Set<Integer> nonZeroWeightIndices = new HashSet<Integer>();
		for (VectorElement e_p : u_p) {
			if (e_p.index() == 0 || e_p.index() >= this.sizeF_0)
				continue;
			nonZeroWeightIndices.add(e_p.index() - 1);
		}
		
		for (VectorElement e_n : u_n) {
			if (e_n.index() == 0 || e_n.index() >= this.sizeF_0)
				continue;
			nonZeroWeightIndices.add(e_n.index() - 1);
		}
		
		this.nonZeroFeatureNamesF_0 = data.getFeatureVocabularyNamesForIndices(nonZeroWeightIndices);
		
		output.debugWriteln("Logistmar gramression (" + data.getName() + ") finished training."); 
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();

		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: Logistmar Gramression only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = data.makePlataniosDataSet(this.weightedLabels, 0.0, false, true);
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		
		Pair<Vector, Vector> uPosNeg = splitPosNeg(this.u);
		Pair<Vector, Vector> cPosNeg = c(uPosNeg.getFirst(), uPosNeg.getSecond());
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.constructedFeatures.getFeature(i);
			feature.setFnCacheMode(Fn.CacheMode.ON);
		}
		
		for (PredictedDataInstance<Vector, Double> plataniosDatum : plataniosData) {
			int datumId = Integer.parseInt(plataniosDatum.name());
			D datum = data.getDatumById(datumId);
			Vector constructedF = this.constructedFeatures.computeFeatureVocabularyRange(datum, 0, this.constructedFeatures.getFeatureVocabularySize());
			plataniosDatum.features().set(this.sizeF_0, this.sizeF_0 + this.constructedFeatures.getFeatureVocabularySize() - 1, constructedF);
			double p = posteriorForDatum(plataniosDatum, cPosNeg.getFirst(), cPosNeg.getSecond());
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
	public Map<D, L> classify(FeaturizedDataSet<D, L> data) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();

		if (this.validLabels.size() > 2 || !this.validLabels.contains(true)) {
			output.debugWriteln("ERROR: Areg only supports binary classification.");
			return null;
		}
		
		DataSet<PredictedDataInstance<Vector, Double>> plataniosData = data.makePlataniosDataSet(this.weightedLabels, 0.0, false, true);
		Map<D, Boolean> predictions = new HashMap<D, Boolean>();
		
		Pair<Vector, Vector> uPosNeg = splitPosNeg(this.u);
		Pair<Vector, Vector> cPosNeg = c(uPosNeg.getFirst(), uPosNeg.getSecond());
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.constructedFeatures.getFeature(i);
			feature.setFnCacheMode(Fn.CacheMode.ON);
		}
		
		for (PredictedDataInstance<Vector, Double> plataniosDatum : plataniosData) {
			int datumId = Integer.parseInt(plataniosDatum.name());
			D datum = data.getDatumById(datumId);
			
			if (this.fixedDatumLabels.containsKey(datum)) {
				predictions.put(datum, (Boolean)this.fixedDatumLabels.get(datum));
				continue;
			}
			
			Vector constructedF = this.constructedFeatures.computeFeatureVocabularyRange(datum, 0, this.constructedFeatures.getFeatureVocabularySize());
			
			if (this.constructedFeatures.getFeatureVocabularySize() != 0)
				plataniosDatum.features().set(this.sizeF_0, this.sizeF_0 + this.constructedFeatures.getFeatureVocabularySize() - 1, constructedF);
			
			double p = posteriorForDatum(plataniosDatum, cPosNeg.getFirst(), cPosNeg.getSecond());
			
			if (p >= this.classificationThreshold)
				predictions.put(datum, true);
			else 
				predictions.put(datum, false);
		}
		
		return (Map<D, L>)predictions;
	}

	@Override
	public String getGenericName() {
		return "LogistmarGramression";
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
			this.rules = this.context.getMatchRuleSet(parameterValue);
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
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelLogistmarGramression<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (!internalAssignments.contains("bias") || !internalAssignments.contains("sizeF_0"))
			return true;
		
		Map<Integer, Double> u_pMap = new HashMap<Integer, Double>(); 
		Map<Integer, Double> u_nMap = new HashMap<Integer, Double>();
		
		this.sizeF_0 = Integer.valueOf(internalAssignments.get("sizeF_0").getValue().toString());
		
		Obj.Array biasArray = (Obj.Array)internalAssignments.get("bias").getValue();
		u_pMap.put(0, Double.valueOf(biasArray.getStr(0)));
		u_nMap.put(0, Double.valueOf(biasArray.getStr(1)));
		
		this.childToParentFeatureMap = new HashMap<Integer, List<Integer>>();
		this.constructedFeatures = new FilteredVocabFeatureSet<D, L>(); 
		for (int i = 0; i < internalAssignments.size(); i++) {
			AssignmentTyped assignment = (AssignmentTyped)internalAssignments.get(i);
			if (assignment.getType().equals(Context.FEATURE_STR)) {
				Obj.Function fnObj = (Obj.Function)assignment.getValue();
				FeatureTokenSpanFnFilteredVocab<D, L> feature = (FeatureTokenSpanFnFilteredVocab<D, L>)this.context.getDatumTools().makeFeatureInstance(fnObj.getName(), this.context);
				String referenceName = assignment.getName();
				if (!feature.fromParse(null, referenceName, fnObj))
					return false;
				this.constructedFeatures.addFeature(feature);
			} else if (assignment.getName().startsWith("u-")) {
				Obj.Array uArr = (Obj.Array)assignment.getValue();
				int uIndex = Integer.valueOf(uArr.getStr(5));
				double u_p = Double.valueOf(uArr.getStr(3));
				double u_n = Double.valueOf(uArr.getStr(4));
				u_pMap.put(uIndex, u_p);
				u_nMap.put(uIndex, u_n);
			} else if (assignment.getName().startsWith("cToP-")) {
				int childIndex = Integer.valueOf(assignment.getName().substring(5));
				List<Integer> parentIndices = new ArrayList<Integer>();
				
				Obj.Array array = (Obj.Array)assignment.getValue();
				for (int j = 0; j < array.size(); j++)
					parentIndices.add(Integer.valueOf(array.getStr(j)));
					
				this.childToParentFeatureMap.put(childIndex, parentIndices);
			}
		}
		
		Vector u_p = new SparseVector(Integer.MAX_VALUE, u_pMap);
		Vector u_n = new SparseVector(Integer.MAX_VALUE, u_nMap);
		this.u = joinPosNeg(u_p, u_n);
		
		return true;
	}
	
	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		if (this.u == null)
			return internalAssignments;
		
		Pair<Vector, Vector> uPosNeg = splitPosNeg(this.u);
		Vector u_p = uPosNeg.getFirst();
		Vector u_n = uPosNeg.getSecond();
		
		Pair<Vector, Vector> cPosNeg = c(uPosNeg.getFirst(), uPosNeg.getSecond());
		Vector c_p = cPosNeg.getFirst();
		Vector c_n = cPosNeg.getSecond();
		
		Map<Integer, Pair<Double, Double>> cMap = new HashMap<Integer, Pair<Double, Double>>();
		for (VectorElement e_p : u_p) {
			if (e_p.index() == 0) // Skip bias
				continue;
			cMap.put(e_p.index(), new Pair<Double,Double>(c_p.get(e_p.index()), c_n.get(e_p.index())));
		}
		
		for (VectorElement e_n : u_n) {
			if (e_n.index() == 0) // Skip bias
				continue;
			cMap.put(e_n.index(), new Pair<Double,Double>(c_p.get(e_n.index()), c_n.get(e_n.index())));
		}
		
		List<Entry<Integer, Pair<Double, Double>>> cList = new ArrayList<Entry<Integer, Pair<Double, Double>>>(cMap.entrySet());
		Collections.sort(cList, new Comparator<Entry<Integer, Pair<Double, Double>>>() {
			@Override
			public int compare(Entry<Integer, Pair<Double, Double>> c1Entry,
					Entry<Integer, Pair<Double, Double>> c2Entry) {
				double c1 = Math.max(c1Entry.getValue().getFirst(), c1Entry.getValue().getSecond());
				double c2 = Math.max(c2Entry.getValue().getFirst(), c2Entry.getValue().getSecond());
				
				if (c1 > c2)
					return -1;
				else if (c1 < c2)
					return 1;
				else
					return 0;
			} });

		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "sizeF_0", Obj.stringValue(String.valueOf(this.sizeF_0))));
		
		Obj.Array bias = Obj.array(new String[] { String.valueOf(u_p.get(0)), String.valueOf(u_n.get(0)) });
		internalAssignments.add(Assignment.assignmentTyped(null, Context.ARRAY_STR, "bias", bias));
		
		for (Entry<Integer, Pair<Double, Double>> cEntry : cList) {
			boolean constructedFeature = cEntry.getKey() >= this.sizeF_0;
			String feature_i = (!constructedFeature) ? this.nonZeroFeatureNamesF_0.get(cEntry.getKey() - 1) : this.featureNamesConstructed.get(cEntry.getKey() - this.sizeF_0);
			String i = String.valueOf(cEntry.getKey());
			String u_p_i = String.valueOf(u_p.get(cEntry.getKey()));
			String u_n_i = String.valueOf(u_n.get(cEntry.getKey()));
			String c_p_i = String.valueOf(cEntry.getValue().getFirst());
			String c_n_i = String.valueOf(cEntry.getValue().getSecond());
			
			Obj.Array weight = Obj.array(new String[] { feature_i, c_p_i, c_n_i, u_p_i, u_n_i, i });
			internalAssignments.add(Assignment.assignmentTyped(null, Context.ARRAY_STR, "u-" + cEntry.getKey() + ((constructedFeature) ? "-c" : ""), weight));
		}
		
		for (int i = 0; i < this.constructedFeatures.getFeatureCount(); i++) {
			Feature<D, L> feature = this.constructedFeatures.getFeature(i);
			internalAssignments.add(Assignment.assignmentTyped(null, Context.FEATURE_STR, feature.getReferenceName(), feature.toParse()));
		}
	
		for (Entry<Integer, List<Integer>> entry : this.childToParentFeatureMap.entrySet()) {
			Obj.Array array = Obj.array();
			for (Integer parent : entry.getValue())
				array.add(Obj.stringValue(parent.toString()));
			
			internalAssignments.add(Assignment.assignmentTyped(null, Context.ARRAY_STR, "cToP-" + entry.getKey(), array));
		}
		
		this.nonZeroFeatureNamesF_0 = null; // Assumes convert toParse only once... add back in if memory issues
		
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}
	
	private Vector joinPosNeg(Vector v_p, Vector v_n) {
		int[] indices = new int[v_p.cardinality() + v_n.cardinality()];
		double[] values = new double[v_p.cardinality() + v_n.cardinality()];
		
		Iterator<VectorElement> i_p = v_p.iterator();
		Iterator<VectorElement> i_n = v_n.iterator();
		
		VectorElement e_p = null;
		VectorElement e_n = null;
		for (int i = 0; i_p.hasNext() || i_n.hasNext() || e_p != null || e_n != null; i++) {
			if (e_p == null && i_p.hasNext())
				e_p = i_p.next();
			if (e_n == null && i_n.hasNext())
				e_n = i_n.next();
			
			// Indicates whether to take next element from e_n or e_p next to keep indices in order
			boolean next_p = (e_n == null) || (e_p != null && e_p.index() <= e_n.index());
			
			if (next_p) {
				indices[i] = e_p.index()*2;
				values[i] = e_p.value();
				e_p = null;
			} else {
				indices[i] = e_n.index()*2+1;
				values[i] = e_n.value();
				e_n = null;
			}		
		}

		return new SparseVector(Integer.MAX_VALUE, indices, values);
	}
	
	private Pair<Vector, Vector> splitPosNeg(Vector v) {
		Map<Integer, Double> map_p = new HashMap<Integer, Double>(v.cardinality());
		Map<Integer, Double> map_n = new HashMap<Integer, Double>(v.cardinality());
		
		for (VectorElement e : v) {
			if (e.index() % 2 == 0)
				map_p.put(e.index() / 2, e.value());
			else
				map_n.put((e.index() - 1) / 2 , e.value());
		}
		
		return new Pair<Vector, Vector>(new SparseVector(Integer.MAX_VALUE, map_p), new SparseVector(Integer.MAX_VALUE, map_n));
	}
	
	private Pair<Vector, Vector> c(Vector u_p, Vector u_n) {
		Map<Integer, Double> c_p = new HashMap<Integer, Double>(2*u_p.cardinality());
		Map<Integer, Double> c_n = new HashMap<Integer, Double>(2*u_n.cardinality());
		
		for (VectorElement e_p : u_p) {
			if (e_p.index() >= this.sizeF_0)
				break;
			c_p.put(e_p.index(), e_p.value());
		}
		
		for (VectorElement e_n : u_n) {
			if (e_n.index() >= this.sizeF_0)
				break;
			c_n.put(e_n.index(), e_n.value());
		}
		
		for (Integer childIndex : this.childToParentFeatureMap.keySet()) {
			cHelper(childIndex, c_p, c_n, u_p, u_n);
		}
	
		return new Pair<Vector, Vector>(new SparseVector(Integer.MAX_VALUE, c_p), new SparseVector(Integer.MAX_VALUE, c_n));
	}
	
	private void cHelper(Integer index, Map<Integer, Double> c_p, Map<Integer, Double> c_n, Vector u_p, Vector u_n) {
		if (index < this.sizeF_0)
			return;
		if (c_p.containsKey(index) || c_n.containsKey(index))
			return;
		
		double u_p_i = u_p.get(index);
		double u_n_i = u_n.get(index);
		
		if (Double.compare(u_p_i, 0.0) == 0 && Double.compare(u_n_i, 0.0) == 0)
			return;
		
		double maxValue = 0.0;
		for (Integer parentIndex : this.childToParentFeatureMap.get(index)) {
			if (parentIndex >= this.sizeF_0)
				cHelper(parentIndex, c_p, c_n, u_p, u_n);
			
			if (c_p.containsKey(parentIndex))
				maxValue = Math.max(c_p.get(parentIndex) - this.t, maxValue);
			if (c_n.containsKey(parentIndex))
				maxValue = Math.max(c_n.get(parentIndex) - this.t, maxValue);
		}
		
		if (Double.compare(maxValue, 0.0) != 0) {
			if (Double.compare(u_p_i, 0.0) != 0)
				c_p.put(index, u_p_i*maxValue);
			if (Double.compare(u_n_i, 0.0) != 0)
				c_n.put(index, u_n_i*maxValue);
		}
	}
	
	private double posteriorForDatum(LabeledDataInstance<Vector, Double> dataInstance, Vector c_p, Vector c_n) {
		Vector f = dataInstance.features();	
		double c_pDotF = Math.exp(c_p.dot(f));
		return c_pDotF/(c_pDotF + Math.exp(c_n.dot(f)));
	}
}
