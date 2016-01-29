package edu.cmu.ml.rtw.generic.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.platanios.learn.math.matrix.Vector;
import org.platanios.learn.math.matrix.Vector.VectorElement;

import YADLL.Data.FMatrix;
import YADLL.Data.Matrix;
import YADLL.Estimators.BP;
import YADLL.Estimators.Estimator;
import YADLL.FunctionGraphs.FunctionGraph;
import YADLL.FunctionGraphs.Functions.Function;
import YADLL.FunctionGraphs.Functions.Variable;
import YADLL.Optimizers.GradOpt;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;

public class SupervisedModelYADLL <D extends Datum<L>, L> extends SupervisedModel<D, L> {
	/*private double l1;
	private double l2;
	private double convergenceEpsilon = -1.0;
	private int maxEvaluationConstantIterations = 100000;
	private double maxTrainingExamples = 10000;
	private int batchSize = 100;
	private int evaluationIterations = 500;
	private boolean weightedLabels = false;
	private double classificationThreshold = 0.5;
	private boolean computeTestEvaluations = true;*/
	private int numEpochs;
	
	private String[] hyperParameterNames = { "numEpochs"/*"l1", "l2", "convergenceEpsilon", "maxEvaluationConstantIterations", "maxTrainingExamples", "batchSize", "evaluationIterations", "weightedLabels", "classificationThreshold", "computeTestEvaluations" */};
	
	private FunctionGraph model;
	/*private LogisticRegressionAdaGrad classifier;
	private Vector classifierWeights;
	
	private Map<Integer, String> nonZeroFeatureNames;*/
	
	public SupervisedModelYADLL() {
		
	}
	
	public SupervisedModelYADLL(Context<D, L> context) {
		this.context = context;
	}
	
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		
		// FIXME Hyper-parameters, and eval on test data
		
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, false);
		Matrix X = dataMatrices.getFirst();
		Matrix Y = dataMatrices.getSecond();
		//Compose a 2 hidden-layer rectifier MLP with softmax outputs
		Variable x_0 = new Variable("x0", data.getFeatureVocabularySize());
		Variable y_0 = new Variable("y0", this.validLabels.size());
		Function f_0 = new Function("f0",5,"relu(W0 * x0 + b0)");
		Function f_1 = new Function("f1",4,"relu(W1 * f0 + b1)");
		Function f_2 = new Function("f2", this.validLabels.size(), "softmax(U0 * f1 + d0)");
		Function L_0 = new Function("L0",1,"neg_log_loss(f2, y0)");//loss function
		    
		//Generate the function-graph that uses these functionals/variables
		this.model = new FunctionGraph();
		this.model.setSeed(1);
		this.model.defineVariable(x_0);
		this.model.defineLabel(y_0);
		this.model.defineFunction(f_0);
		this.model.defineFunction(f_1);
		this.model.defineFunction(f_2);
		this.model.defineFunction(L_0);
		this.model.defineParamInit_("W0","gaussian(0,1)");
		this.model.defineParamInit_("W1","gaussian(0,1)");
		this.model.defineParamInit_("U0","gaussian(0,1)");
		this.model.compile();
		    
		//Now set up an parameter-update estimator and an optimizer
		Estimator estimator = new BP(this.model);
		GradOpt optimizer = new GradOpt(estimator);
		optimizer.setStepSize(0.1f);
		optimizer.setOptType("descent");
		
		int epoch = 0;
		while(epoch < this.numEpochs){
			//NOTE: to get validation error, simply perform a separate call to .eval()
			//e.g.:
			// graph.clamp(x_0, Validation_Set)
			// graph.eval()
			// println(epoch + " Valid.Error = " + graph.getStat("L0"))
			// graph.flush_stats()
		  
			//Clamp training data set to model
			this.model.clamp_("x0", X);
			this.model.clamp_("y0", Y);
			this.model.eval();
			
			//Grab value of loss function to get training error...
			//System.out.println(epoch + " Training.Error = " + graph.getStat_("L0")[1]);
			optimizer.accum_grad(1f);// get the gradient for X
			//Perform a step of batch gradient descent     
			optimizer.update_graph();
			this.model.flush_stats(false);
			
			epoch = epoch + 1;
		}
		
		//this.nonZeroFeatureNames = data.getFeatureVocabularyNamesForIndices(nonZeroWeightIndices);
		
		output.debugWriteln("YADLL finished training"); 
		
		return true;
	}
	
	private Pair<Matrix, Matrix> buildMatricesFromData(FeaturizedDataSet<D, L> data, boolean onlyX) {
		Map<L, Integer> labelMap = getLabelIndices();
		
		int datumFeatureCount = data.getFeatureVocabularySize();
		int datumCount = data.size();
		int labelCount = labelMap.size();
		float[] X = new float[datumCount*datumFeatureCount];
		float[] Y = (onlyX) ? null : new float[datumCount*labelCount];
		int i = 0;
		for (D datum : data) {
			int labelIndex = labelMap.get(mapValidLabel(datum.getLabel()));
			
			Vector datumFeatures = data.getFeatureVocabularyValues(datum);
			for (VectorElement feature : datumFeatures)
				X[i*datumFeatureCount + feature.index()] = (float)feature.value();
			
			if (!onlyX)
				Y[i*labelCount + labelIndex] = 1f;
			
			i++;
		}
		
		return new Pair<Matrix, Matrix>(new FMatrix(datumFeatureCount, datumCount, X),
										(onlyX) ? null : new FMatrix(labelCount, datumCount, Y)); 
	}
	
	@SuppressWarnings("unchecked")
	private Map<L, Integer> getLabelIndices() {
		Map<L, Integer> labelMap = new HashMap<L, Integer>();

		L[] labels = (L[])this.validLabels.toArray();
		for (int i = 0; i < labels.length; i++)
			labelMap.put(labels[i], i);
		
		return labelMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, true);
		Matrix X = dataMatrices.getFirst();
		L[] labels = (L[])this.validLabels.toArray();
		
		this.model.clamp_("x0", X);
		this.model.eval();
		float[] outputY = this.model.getOutput("f2").data();
		
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		int i = 0;
		for (D datum : data) {
			Map<L, Double> p = new HashMap<L, Double>();
			double norm = 0.0;
			for (int j = 0; j < outputY.length; j++) {
				p.put(labels[j], new Double(outputY[i*labels.length + j]));
				norm += outputY[i*labels.length + j];
			}
			
			for (int j = 0; j < outputY.length; j++) {
				p.put(labels[j], p.get(labels[j])/norm);
			}
			
			posteriors.put(datum, p);
			i++;
		}
		
		return posteriors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<D, L> classify(FeaturizedDataSet<D, L> data) {
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, true);
		Matrix X = dataMatrices.getFirst();
		L[] labels = (L[])this.validLabels.toArray();
		
		this.model.clamp_("x0", X);
		this.model.eval();
		float[] outputY = this.model.getOutput("f2").data();
		
		Map<D, L> classifications = new HashMap<D, L>();
		int i = 0;
		for (D datum : data) {
			double maxValue = 0.0;
			int maxIndex = 0;
			for (int j = 0; j < outputY.length; j++) {
				if (Double.compare(outputY[i*labels.length + j], maxValue) >= 0) {
					maxIndex = j;
					maxValue = outputY[i*labels.length + j];
				}
			}
			
			classifications.put(datum, labels[maxIndex]);
	
			i++;
		}
		
		return classifications;
	}

	@Override
	public String getGenericName() {
		return "YADLL";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("numEpochs"))
			return Obj.stringValue(String.valueOf(this.numEpochs));
		/* FIXME if (parameter.equals("l1"))
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
			return Obj.stringValue(String.valueOf(this.computeTestEvaluations)); */
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("numEpochs"))
			this.numEpochs = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
		/* FIXME if (parameter.equals("l1"))
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
			return false;*/
	}
	
	@Override
	public String[] getParameterNames() {
		return this.hyperParameterNames;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelYADLL<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		return true;
		/* FIXME if (!internalAssignments.contains("featureVocabularySize")
				|| !internalAssignments.contains("nonZeroWeights")
				|| !internalAssignments.contains("bias"))
			return true;
		
		int weightVectorSize = Integer.valueOf(((Obj.Value)internalAssignments.get("featureVocabularySize").getValue()).getStr());
		double bias = Double.valueOf(((Obj.Value)internalAssignments.get("bias").getValue()).getStr());
		
		TreeMap<Integer, Double> nonZeroWeights = new TreeMap<Integer, Double>();
		
		for (int i = 0; i < internalAssignments.size(); i++) {
			Assignment assignment = internalAssignments.get(i);
			if (assignment.getName().startsWith("w_")) {
				Obj.Array wArray = (Obj.Array)assignment.getValue();
				int index = Integer.valueOf(wArray.getStr(2));
				double w = Double.valueOf(wArray.getStr(1));
				
				nonZeroWeights.put(index, w);
			}
		} 
		
		nonZeroWeights.put(weightVectorSize-1, bias);
		
		Vector weights = new SparseVector(weightVectorSize, nonZeroWeights);
		
		this.classifier = new LogisticRegressionAdaGrad.Builder(weightVectorSize - 1, weights)
			.sparse(true)
			.useBiasTerm(true)
			.build();
		
		return true; */
	}
	
	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		/* FIXME 
		if (this.classifierWeights == null)
			return internalAssignments;
		
		double[] weightArray = this.classifierWeights.getDenseArray(); 
		internalAssignments.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "featureVocabularySize", Obj.stringValue(String.valueOf(weightArray.length))));
		
		List<Pair<Integer, Double>> sortedWeights = new ArrayList<Pair<Integer, Double>>();
		for (Integer index : this.nonZeroFeatureNames.keySet()) {
			sortedWeights.add(new Pair<Integer, Double>(index, weightArray[index]));
		}
		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "nonZeroWeights", Obj.stringValue(String.valueOf(this.nonZeroFeatureNames.size()))));
		
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
		
		internalAssignments.add(Assignment.assignmentTyped(null, Context.VALUE_STR, "bias", Obj.stringValue(String.valueOf(weightArray[weightArray.length - 1]))));
		
		for (Pair<Integer, Double> weight : sortedWeights) {
			double w = weight.getSecond();
			int index = weight.getFirst();
			String featureName = this.nonZeroFeatureNames.get(index);
			Obj.Array weightArr = Obj.array(new String[]{ featureName, String.valueOf(w), String.valueOf(index) });
			internalAssignments.add(Assignment.assignmentTyped(null, Context.ARRAY_STR, "w_" + index, weightArr));
		}
		
		// this.nonZeroFeatureNames = null; Assumes convert toParse only once... add back in if memory issues
		
		return internalAssignments;
		*/
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}

}

