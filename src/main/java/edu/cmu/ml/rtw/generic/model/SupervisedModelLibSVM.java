package edu.cmu.ml.rtw.generic.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.platanios.learn.math.matrix.Vector;
import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.StringUtil;

public class SupervisedModelLibSVM<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	public enum KernelType {
		LINEAR(svm_parameter.LINEAR),
		POLYNOMIAL(svm_parameter.POLY),
		RBF(svm_parameter.RBF),
		SIGMOID(svm_parameter.SIGMOID);

		private int index;
		
		KernelType(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return this.index;
		}
		
		public static KernelType fromIndex(int index) {
			if (index == 0)
				return LINEAR;
			else if (index == 1)
				return POLYNOMIAL;
			else if (index == 2)
				return RBF;
			else 
				return SIGMOID;
		}
	}
	
	public enum SVMType {
		C_SVC(svm_parameter.C_SVC),
		ONE_CLASS(svm_parameter.ONE_CLASS);
		
		private int index;
		
		SVMType(int index) {
			this.index = index;
		}
		
		public int getIndex() {
			return this.index;
		}
		
		public static SVMType fromIndex(int index) {
			if (index == svm_parameter.C_SVC)
				return C_SVC;
			else if (index == svm_parameter.ONE_CLASS)
				return ONE_CLASS;
			else 
				return null;
		}
	}
	
	private L targetLabel;
	private L outlierLabel;
	private SVMType svmType = SVMType.ONE_CLASS;
	private KernelType kernelType = KernelType.RBF;
	private Double gamma = null;
	private double nu = 0.5;
	private double C = 1;
	private String[] parameterNames = { "targetLabel", "outlierLabel", "svmType", "kernelType", "gamma", "nu", "C" };
	
	private svm_model model;
	
	public SupervisedModelLibSVM() {
		
	}
	
	public SupervisedModelLibSVM(DatumContext<D, L> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("targetLabel"))
			return Obj.stringValue(String.valueOf(this.targetLabel.toString()));
		else if (parameter.equals("outlierLabel"))
			return Obj.stringValue(String.valueOf(this.outlierLabel.toString()));
		else if (parameter.equals("svmType"))
			return Obj.stringValue(this.svmType.toString());
		else if (parameter.equals("kernelType"))
			return Obj.stringValue(this.kernelType.toString());
		else if (parameter.equals("gamma"))
			return (this.gamma == null) ? null : Obj.stringValue(String.valueOf(this.gamma));
		else if (parameter.equals("nu"))
			return Obj.stringValue(String.valueOf(this.nu));
		else if (parameter.equals("C"))
			return Obj.stringValue(String.valueOf(this.C));
		else
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("targetLabel"))
			this.targetLabel = this.context.getDatumTools().labelFromString(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("outlierLabel"))
			this.outlierLabel = this.context.getDatumTools().labelFromString(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("svmType"))
			this.svmType = SVMType.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("kernelType"))
			this.kernelType = KernelType.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("gamma"))
			this.gamma = parameterValue == null ? null : Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("nu"))
			this.nu = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("C"))
			this.C = Double.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		return true;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(DatumContext<D, L> context) {
		return new SupervisedModelLibSVM<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		if (internalAssignments == null || !internalAssignments.contains("classifier"))
			return true;
		
		/* FIXME Be more verbose?
		this.model.l;
		this.model.label;
		this.model.nr_class;
		this.model.nSV;
		this.model.param.C;
		this.model.param.cache_size;
		this.model.param.coef0;
		this.model.param.degree;
		this.model.param.eps;
		this.model.param.gamma;
		this.model.param.kernel_type;
		this.model.param.nr_weight;
		this.model.param.nu;
		this.model.param.p;
		this.model.param.probability;
		this.model.param.shrinking;
		this.model.param.svm_type;
		this.model.param.weight;
		this.model.param.weight_label;
		this.model.param.
		this.model.probA;
		this.model.probB;
		this.model.rho;
		this.model.SV;
		this.model.sv_coef;
		this.model.sv_indices*/
		
		try {
			this.model = (svm_model)StringUtil.deserializeFromBase64String(((Obj.Value)internalAssignments.get("classifier").getValue()).getStr());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	protected AssignmentList toParseInternalHelper(AssignmentList internalAssignments) {
		if (this.model == null) 
			return internalAssignments;
		
		try {
			String classifier = StringUtil.serializeToBase64String(this.model);
			internalAssignments.add(
					Assignment.assignmentTyped(null, 
					Context.ObjectType.VALUE.toString(), "classifier", Obj.stringValue(classifier)));
		} catch (IOException e) {
			return null;
		}
		
		return internalAssignments;
	}

	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		return binaryModel;
	}

	@Override
	public String getGenericName() {
		return "LibSVM";
	}

	@Override
	public boolean train(DataFeatureMatrix<D, L> data,
			DataFeatureMatrix<D, L> testData,
			List<SupervisedModelEvaluation<D, L>> evaluations) {		
		this.context.getDataTools().getOutputWriter().debugWriteln("LibSVM constructing data set... ");

		svm_problem problem = constructProblem(data, true);
		
		svm_parameter param = new svm_parameter();
	    param.probability = 1; // Determines whether p is estimated
	    if (this.gamma != null)
	    	param.gamma = this.gamma;
	    param.nu = this.nu;
	    param.C = this.C;
	    param.svm_type = this.svmType.getIndex();
	    param.kernel_type = svm_parameter.LINEAR;      
	    param.cache_size = 20000;
	    param.eps = 0.001; 
		
	    this.model = svm.svm_train(problem, param);
		
		return true;
	}

	@Override
	public Map<D, Map<L, Double>> posterior(DataFeatureMatrix<D, L> data) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		Map<D, L> out = new HashMap<D, L>();
		int totalClasses = 2;       
		int[] labels = new int[totalClasses];
		svm.svm_get_labels(this.model,labels);

		for (D datum : data.getData()) {
			svm_node[] svmDatum = constructSVMDatumVector(data, datum);
			double[] prob_estimates = new double[totalClasses];
			double v = svm.svm_predict_probability(this.model, svmDatum, prob_estimates);
			L label = null;
			if (Double.compare(v, 0) >= 0)
				label = this.targetLabel;
			else
				label = this.outlierLabel;
			out.put(datum, label);
		}
		
		return out;
	}
	
	private svm_node[] constructSVMDatumVector(DataFeatureMatrix<D, L> data, D datum) {
		Vector v = data.getFeatureVocabularyValues(datum, false);
		svm_node[] svm_v = new svm_node[v.size()];
		
		for (int i = 0; i < v.size(); i++) {
			svm_node n = new svm_node();
			n.index = i;
			n.value = v.get(i);
			svm_v[i] = n;
		}
		
		return svm_v;
	}

	private svm_problem constructProblem(DataFeatureMatrix<D, L> data, boolean includeLabels) {
		svm_problem prob = new svm_problem();
		int dataCount = data.getData().size();
		prob.y = new double[dataCount];
		prob.l = dataCount;
		prob.x = new svm_node[dataCount][];     
		
		int i = 0;
		for (D datum : data.getData()){  
			prob.x[i] = constructSVMDatumVector(data, datum);
			if (includeLabels) {
				if (this.svmType == SVMType.ONE_CLASS) {
					if (this.targetLabel.equals(datum.getLabel()))
						prob.y[i] = 1.0;
				} else {
					if (this.targetLabel.equals(datum.getLabel()))
						prob.y[i] = 1.0;
					else if (this.outlierLabel.equals(datum.getLabel()))
						prob.y[i] = 0.0;
				}
			}
			i++;
		}
		
		return prob;
	}
}
