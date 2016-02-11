package edu.cmu.ml.rtw.generic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import YADLL.Data.FMatrix;
import YADLL.Data.Matrix;
import YADLL.Data.SMatrix;
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
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.OutputWriter;
import edu.cmu.ml.rtw.generic.util.Pair;

public class SupervisedModelYADLL <D extends Datum<L>, L> extends SupervisedModel<D, L> {
	public static enum YADLLTrainingEstimator {
		BACK_PROPAGATION
	}
	
	public static enum YADLLFunctionType {
		LOSS,
		ACTIVATION,
		PARAMETER_INITIALIZATION
	}
	
	public static enum YADLLFunctionPrototype {
		GAUSSIAN("Gaussian", "gaussian", YADLLFunctionType.PARAMETER_INITIALIZATION, new String[] { "mean", "variance"}),
		FAN_IN("FanIn", "fan_in", YADLLFunctionType.PARAMETER_INITIALIZATION, new String[0]),
		ZEROS("Zeros", "zeros", YADLLFunctionType.PARAMETER_INITIALIZATION, new String[0]),
		ONES("Ones", "ones", YADLLFunctionType.PARAMETER_INITIALIZATION, new String[0]),
		NEGATIVE_LOG_LOSS("NegativeLogLoss", "neg_log_loss", YADLLFunctionType.LOSS, new String[] { "input" }),
		CROSS_ENTROPY_LOSS("CrossEntropyLoss", "cross_entropy_loss", YADLLFunctionType.LOSS, new String[] { "input" }),
		QUADRATIC_LOSS("QuadraticLoss", "quadratic_loss", YADLLFunctionType.LOSS, new String[] { "input" }),
		RELU("Relu", "relu", YADLLFunctionType.ACTIVATION, new String[] { "input" }),
		SIGMOID("Sigmoid", "sigmoid", YADLLFunctionType.ACTIVATION, new String[] { "input" }),
		SOFTMAX("Softmax", "softmax", YADLLFunctionType.ACTIVATION, new String[] { "input" }),
		SOFTSIGN("Softsign", "softsign", YADLLFunctionType.ACTIVATION, new String[] { "input" });
	
		private String name;
		private String yadllName;
		private YADLLFunctionType type;
		private String[] argNames;
		
		YADLLFunctionPrototype(String name, String yadllName, YADLLFunctionType type, String[] argNames) {
			this.name = name;
			this.yadllName = yadllName;
			this.type = type;
			this.argNames = argNames;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String getYADLLName() {
			return this.yadllName;
		}
		
		public YADLLFunctionType getType() {
			return this.type;
		}
		
		public String[] getArgNames() {
			return this.argNames;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		public Obj.Function makeGenericParse() {
			AssignmentList parameters = new AssignmentList();
			for (String argName : this.argNames)
				parameters.add(Assignment.assignmentUntyped(argName, Obj.curlyBracedValue(argName)));
			return Obj.function(this.name, parameters);
		}
		
		public static YADLLFunctionPrototype fromName(String name) {
			YADLLFunctionPrototype[] types = YADLLFunctionPrototype.values();
			for (YADLLFunctionPrototype type : types)
				if (type.getName().equals(name))
					return type;
			return null;
		}
	}
	
	private static class YADLLFunction extends CtxParsableFunction {
		private YADLLFunctionPrototype fnType;
		private Map<String, String> parameterValues;
		
		private Context<?, ?> context;
		
		public YADLLFunction(Context<?, ?> context) {
			this.parameterValues = new HashMap<String, String>();
			this.context = context;
		}
		
		@Override
		protected boolean fromParseHelper(Obj obj) {
			Obj.Function fnObj = (Obj.Function)obj;
			this.fnType = YADLLFunctionPrototype.fromName(fnObj.getName());
			
			if (!super.fromParseHelper(obj))
				return false;
			
			return true;
		}
		
		@Override
		public String[] getParameterNames() {
			return this.fnType.getArgNames();
		}

		@Override
		public Obj getParameterValue(String parameter) {
			return Obj.stringValue(this.parameterValues.get(parameter));
		}

		@Override
		public boolean setParameterValue(String parameter, Obj parameterValue) {
			boolean hasParameter = false;
			for (String argName : this.fnType.argNames)
				if (argName.equals(parameter)) {
					hasParameter = true;
					break;
				}
			if (!hasParameter)
				return false;
			
			this.parameterValues.put(parameter, this.context.getMatchValue(parameterValue));
			return true;
		}

		@Override
		protected boolean fromParseInternal(AssignmentList internalAssignments) {
			return true;
		}

		@Override
		protected AssignmentList toParseInternal() {
			return null;
		}

		@Override
		public String getGenericName() {
			return this.fnType.toString();
		}
		
		public boolean replaceParameterValues(String findStr, String replacementStr) {
			for (Entry<String, String> entry : this.parameterValues.entrySet()) {
				entry.setValue(entry.getValue().replace(findStr, replacementStr));
			}
			return true;
		}
		
		public String getYADLLFunctionString() {
			StringBuilder str = new StringBuilder();
			
			str.append(this.fnType.getYADLLName());
			str.append("(");
			for (String paramName : this.fnType.getArgNames())
				str.append(this.parameterValues.get(paramName)).append(",");
			str.delete(str.length() - 1, str.length());
			str.append(")");
			
			return str.toString();
		}
	}
	
	private static class YADLLFunctionNode extends CtxParsableFunction {
		private int size;
		private YADLLFunction fn;
		
		private String[] parameterNames = { "size", "fn" };
		
		private Context<?, ?> context;
		
		public YADLLFunctionNode(Context<?,?> context) {
			this.context = context;
		}
		
		@Override
		public String[] getParameterNames() {
			return this.parameterNames;
		}

		@Override
		public Obj getParameterValue(String parameter) {
			if (parameter.equals("size"))
				return Obj.stringValue(String.valueOf(this.size));
			else if (parameter.equals("fn"))
				return this.fn.toParse();
			return null;
		}

		@Override
		public boolean setParameterValue(String parameter, Obj parameterValue) {
			if (parameter.equals("size")) {
				this.size = Integer.valueOf(this.context.getMatchValue(parameterValue));
			} else if (parameter.equals("fn")) {
				this.fn = new YADLLFunction(this.context);
				if (!this.fn.fromParse(parameterValue))
					return false;
			} else {
				return false;
			}
			
			return true;
		}

		@Override
		protected boolean fromParseInternal(AssignmentList internalAssignments) {
			return true;
		}

		@Override
		protected AssignmentList toParseInternal() {
			return null;
		}

		@Override
		public String getGenericName() {
			return "FunctionNode";
		}
		
		public boolean replaceFnParameterValues(String findStr, String replacementStr) {
			return this.fn.replaceParameterValues(findStr, replacementStr);
		}
		
		public Function getYADLLModelFunctionObject(String name) {
			return new Function(name, this.size, this.fn.getYADLLFunctionString());
		}
		
		public static Obj.Function makeGenericParse(YADLLFunctionPrototype initFunctionType) {
			if (initFunctionType.getType() == YADLLFunctionType.PARAMETER_INITIALIZATION)
				return null;
			
			AssignmentList parameters = new AssignmentList();
			parameters.add(Assignment.assignmentUntyped("size", Obj.curlyBracedValue("size")));
			parameters.add(Assignment.assignmentUntyped("fn", initFunctionType.makeGenericParse()));
			return Obj.function("FunctionNode", parameters);
		}
	}
	
	private static class YADLLParameter extends CtxParsableFunction {
		private YADLLFunction initFn;
		
		private String[] parameterNames = { "initFn" };
		
		private Context<?, ?> context;
		
		public YADLLParameter(Context<?,?> context) {
			this.context = context;
		}
		
		@Override
		public String[] getParameterNames() {
			return this.parameterNames;
		}

		@Override
		public Obj getParameterValue(String parameter) {
			if (parameter.equals("initFn"))
				return this.initFn.toParse();
			return null;
		}

		@Override
		public boolean setParameterValue(String parameter, Obj parameterValue) {
			if (parameter.equals("initFn")) {
				this.initFn = new YADLLFunction(this.context);
				if (!this.initFn.fromParse(parameterValue))
					return false;
			} else {
				return false;
			}
			
			return true;
		}

		@Override
		protected boolean fromParseInternal(AssignmentList internalAssignments) {
			return true;
		}

		@Override
		protected AssignmentList toParseInternal() {
			return null;
		}

		@Override
		public String getGenericName() {
			return "Parameter";
		}
		
		public boolean replaceInitFnParameterValues(String findStr, String replacementStr) {
			return this.initFn.replaceParameterValues(findStr, replacementStr);
		}
		
		public String getYADLLInitFnString() {
			return this.initFn.getYADLLFunctionString();
		}
		
		public static Obj.Function makeGenericParse(YADLLFunctionPrototype initFunctionType) {
			if (initFunctionType.getType() != YADLLFunctionType.PARAMETER_INITIALIZATION)
				return null;
			
			AssignmentList parameters = new AssignmentList();
			parameters.add(Assignment.assignmentUntyped("initFn", initFunctionType.makeGenericParse()));
			return Obj.function("Parameter", parameters);
		}
	}
	
	private int numEpochs = 1;
	private float stepSize = .1f;
	private YADLLTrainingEstimator trainingEstimator = YADLLTrainingEstimator.BACK_PROPAGATION;
	private List<String> fnNodes; // Values point to possibleFnNodes
	private List<String> fnParameters;
	private String targetFnNode;
	private Map<String, Obj> additionalParameters;
	
	private String[] defaultParameterNames = { "numEpochs", "stepSize", "trainingEstimator", "fnNodes", "fnParameters", "targetFnNode" };
	
	private FunctionGraph model;
	private Map<String, Obj.Function> possibleFnNodes;
	private Map<String, Obj.Function> possibleFnParameters;
	
	public SupervisedModelYADLL() {
		this(null);
	}
	
	public SupervisedModelYADLL(Context<D, L> context) {
		this.context = context;
		this.fnNodes = new ArrayList<String>();
		this.fnParameters = new ArrayList<String>();
		this.targetFnNode = "";
		this.additionalParameters = new HashMap<String, Obj>();
		this.possibleFnNodes = new HashMap<String, Obj.Function>();
		this.possibleFnParameters = new HashMap<String, Obj.Function>();
		
		for (YADLLFunctionPrototype type : YADLLFunctionPrototype.values()) {
			if (type.getType() == YADLLFunctionType.PARAMETER_INITIALIZATION) {
				this.possibleFnParameters.put(type.getName(), YADLLParameter.makeGenericParse(type));
			} else {
				this.possibleFnNodes.put(type.getName(), YADLLFunctionNode.makeGenericParse(type));
			}
		}
	}
	
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		OutputWriter output = data.getDatumTools().getDataTools().getOutputWriter();
		
		if (this.model == null && !buildModelFromParameters(data.getFeatureVocabularySize()))
				return false;
		
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, false);
		Matrix X = dataMatrices.getFirst();
		Matrix Y = dataMatrices.getSecond();
		
		Estimator estimator = new BP(this.model); // FIXME: Add other estimators
		GradOpt optimizer = new GradOpt(estimator);
		optimizer.setStepSize(this.stepSize);
		optimizer.setOptType("descent");
		
		int epoch = 0;
		while(epoch < this.numEpochs) {
			this.model.clamp_("x", X);
			this.model.clamp_("y", Y);
			this.model.eval();
			
			optimizer.accum_grad(1f); 
			optimizer.update_graph();
			this.model.flush_stats(false);
			
			output.debugWriteln(this.toParse(false) + 
					" Epoch: " + epoch + " " + 
					evaluations.get(0).getReferenceName() + ": " + 
						evaluations.get(0).evaluate(this, testData, classify(testData)));

			epoch = epoch + 1;
		}

		output.debugWriteln("YADLL finished training"); 
		
		return true;
	}
	
	private Pair<Matrix, Matrix> buildMatricesFromData(FeaturizedDataSet<D, L> data, boolean onlyX) {
		Map<L, Integer> labelMap = getLabelIndices();
		
		int datumFeatureCount = data.getFeatureVocabularySize();
		int datumCount = data.size();
		int labelCount = labelMap.size();
		
		float[] Y = (onlyX) ? null : new float[datumCount*labelCount];
		int i = 0;
		List<Map<Integer, Double>> featureMaps = new ArrayList<Map<Integer, Double>>();
		int numNonZeroFeatures = 0;
		for (D datum : data) {
			Map<Integer, Double> datumFeatureMap = data.getFeatureVocabularyValuesAsMap(datum);
			featureMaps.add(datumFeatureMap);
			numNonZeroFeatures += datumFeatureMap.size();
			
			if (!onlyX) {
				int labelIndex = labelMap.get(mapValidLabel(datum.getLabel()));
				Y[i*labelCount + labelIndex] = 1f;
			}
			
			i++;
		}
		
		int[] featureIndices = new int[numNonZeroFeatures];
		int[] datumIndices = new int[numNonZeroFeatures];
		float[] values = new float[numNonZeroFeatures];
		
		i = 0;
		for (int datumIndex = 0; datumIndex < featureMaps.size(); datumIndex++) {
			for (Entry<Integer, Double> entry : featureMaps.get(datumIndex).entrySet()) {
				featureIndices[i] = entry.getKey();
				datumIndices[i] = datumIndex;
				values[i] = entry.getValue().floatValue();
				
				i++;
			}
		}
		
		return new Pair<Matrix, Matrix>(new SMatrix(datumFeatureCount, datumCount, featureIndices, datumIndices, values),
										(onlyX) ? null : new FMatrix(labelCount, datumCount, Y)); 
	}
	
	private boolean buildModelFromParameters(int inputVectorSize) {
		
		Variable x = new Variable("x", inputVectorSize, null);
		Variable y = new Variable("y", this.validLabels.size(), null);

		this.model = new FunctionGraph();
		this.model.setSeed(1);
		this.model.defineVariable(x);
		this.model.defineLabel(y);
		
		for (int i = 0; i < this.fnNodes.size(); i++) {
			String fnNodeStr = this.fnNodes.get(i);
			String fnNodeStrAndIndex = fnNodeStr + "_" + i;
			Map<String, Obj> fnNodeParamMap = new HashMap<String, Obj>();
			
			// FIXME This is slow, but okay for now
			for (Entry<String, Obj> entry : this.additionalParameters.entrySet()) {
				if (!entry.getKey().startsWith(fnNodeStrAndIndex))
					continue;
				fnNodeParamMap.put(entry.getKey().substring(fnNodeStrAndIndex.length() + 1), entry.getValue());
			}
			
			System.out.println("PARAM MAP " + fnNodeParamMap);
			
			Obj fnNodeObj = this.possibleFnNodes.get(fnNodeStr).clone();
			if (!fnNodeObj.resolveValues(fnNodeParamMap)) {
				System.out.println("FAILED TO RESOLVE VALUES " + fnNodeParamMap);
				return false;
			}
			
			YADLLFunctionNode fnNode = new YADLLFunctionNode(this.context);
			if (!fnNode.fromParse(fnNodeObj))
				return false;
			
			if (!fnNode.replaceFnParameterValues("%i",String.valueOf(i))
					|| !fnNode.replaceFnParameterValues("%(i-1)", String.valueOf(i-1))
					|| !fnNode.replaceFnParameterValues("%(i+1)", String.valueOf(i-1)))
				return false;
			
			this.model.defineFunction(fnNode.getYADLLModelFunctionObject(fnNodeStrAndIndex));
		}
		
		for (int i = 0; i < this.fnParameters.size(); i++) {
			String fnParameterStr = this.fnParameters.get(i);
			String fnParameterStrAndIndex = fnParameterStr + "_" + i;
			Map<String, Obj> fnParameterParamMap = new HashMap<String, Obj>();
			
			// FIXME This is slow, but okay for now
			for (Entry<String, Obj> entry : this.additionalParameters.entrySet()) {
				if (!entry.getKey().startsWith(fnParameterStrAndIndex))
					continue;
				fnParameterParamMap.put(entry.getKey().substring(fnParameterStrAndIndex.length() + 1), entry.getValue());
			}
			
			Obj fnParameterObj = this.possibleFnParameters.get(fnParameterStr).clone();
			if (!fnParameterObj.resolveValues(fnParameterParamMap))
				return false;
			
			YADLLParameter fnParameter = new YADLLParameter(this.context);
			if (!fnParameter.fromParse(fnParameterObj))
				return false;
			
			if (!fnParameter.replaceInitFnParameterValues("%i",String.valueOf(i))
					|| !fnParameter.replaceInitFnParameterValues("%(i-1)", String.valueOf(i-1))
					|| !fnParameter.replaceInitFnParameterValues("%(i+1)", String.valueOf(i-1)))
				return false;
			
			this.model.defineParamInit_(fnParameterStrAndIndex, fnParameter.getYADLLInitFnString());
			// FIXME Build model from existing weights when available from deserialization
		}
		
		this.model.compile();
		
		return true;
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
		if (this.model == null && !buildModelFromParameters(data.getFeatureVocabularySize()))
			return null;
	
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, true);
		Matrix X = dataMatrices.getFirst();
		L[] labels = (L[])this.validLabels.toArray();
		
		this.model.flush_stats(false);
		this.model.clamp_("x", X);
		this.model.eval();
		
		float[] outputY = this.model.getOutput(this.targetFnNode).getData();
		
		Map<D, Map<L, Double>> posteriors = new HashMap<D, Map<L, Double>>();
		int i = 0;
		for (D datum : data) {
			Map<L, Double> p = new HashMap<L, Double>();
			double norm = 0.0;
			for (int j = 0; j < labels.length; j++) {
				p.put(labels[j], new Double(outputY[i*labels.length + j]));
				norm += outputY[i*labels.length + j];
			}
			
			for (int j = 0; j < labels.length; j++) {
				p.put(labels[j], p.get(labels[j])/norm);
			}
			
			posteriors.put(datum, p);
			i++;
		}
		
		this.model.flush_stats(false);
		
		return posteriors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<D, L> classify(FeaturizedDataSet<D, L> data) {
		if (this.model == null && !buildModelFromParameters(data.getFeatureVocabularySize()))
				return null;
		
		Pair<Matrix, Matrix> dataMatrices = buildMatricesFromData(data, true);
		Matrix X = dataMatrices.getFirst();
		L[] labels = (L[])this.validLabels.toArray();
		
		this.model.flush_stats(false);
		this.model.clamp_("x", X);
		this.model.eval();
		
		float[] outputY = this.model.getOutput(this.targetFnNode).getData();
		
		Map<D, L> classifications = new HashMap<D, L>();
		int i = 0;
		for (D datum : data) {
			double maxValue = 0.0;
			int maxIndex = 0;
			for (int j = 0; j < labels.length; j++) {
				if (Double.compare(outputY[i*labels.length + j], maxValue) >= 0) {
					maxIndex = j;
					maxValue = outputY[i*labels.length + j];
				}
			}
			
			classifications.put(datum, labels[maxIndex]);
	
			i++;
		}
		
		this.model.flush_stats(false);
		
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
		else if (parameter.equals("stepSize"))
			return Obj.stringValue(String.valueOf(this.stepSize));
		else if (parameter.equals("trainingEstimator"))
			return Obj.stringValue(this.trainingEstimator.toString());
		else if (parameter.equals("fnNodes"))
			return Obj.array(this.fnNodes); 
		else if (parameter.equals("fnParameters"))
			return Obj.array(this.fnParameters);
		else if (parameter.equals("targetFnNode"))
			return Obj.stringValue(this.targetFnNode);
		else if (this.additionalParameters.containsKey(parameter))
			return this.additionalParameters.get(parameter);
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("numEpochs"))
			this.numEpochs = Integer.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("stepSize"))
			this.stepSize = Float.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("trainingExtimator"))
			this.trainingEstimator = YADLLTrainingEstimator.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("fnNodes"))
			this.fnNodes = this.context.getMatchArray(parameterValue);
		else if (parameter.equals("fnParameters"))
			this.fnParameters = this.context.getMatchArray(parameterValue);
		else if (parameter.equals("targetFnNode")) {
			this.targetFnNode = this.context.getMatchValue(parameterValue);
		} else 
			this.additionalParameters.put(parameter, parameterValue);
		
		return true;
	}
	
	private List<String> getParameterNameList() {
		List<String> parameterNameList = new ArrayList<String>();
		parameterNameList.addAll(Arrays.asList(this.defaultParameterNames));
		
		for (int i = 0; i < this.fnNodes.size(); i++) {
			String fnNodeStr = this.fnNodes.get(i) + "_" + i;
			Set<String> fnNodeParams = this.possibleFnNodes.get(this.fnNodes.get(i)).getCurlyBracedValueStrs();
			for (String fnNodeParam : fnNodeParams)
				parameterNameList.add(fnNodeStr + "_" + fnNodeParam);
		}
		
		for (int i = 0; i < this.fnParameters.size(); i++) {
			String fnParameterStr = this.fnParameters.get(i) + "_" + i;
			Set<String> fnParameterParams = this.possibleFnParameters.get(this.fnParameters.get(i)).getCurlyBracedValueStrs();
			for (String fnParameterParam : fnParameterParams)
				parameterNameList.add(fnParameterStr + "_" + fnParameterParam);
		}
		
		return parameterNameList;
	}
	
	@Override
	public String[] getParameterNames() {
		return getParameterNameList().toArray(new String[0]);
	}

	@Override
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelYADLL<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {		
		// FIXME Deserialize weights if available
		
		return true;
	}
	
	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		
		if (this.model == null)
			return internalAssignments;
		
		// FIXME Serialize model (need a way to turn matrix into a string or write to disk)
		// How to get biases?
		return internalAssignments;
	}
	
	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}

}

