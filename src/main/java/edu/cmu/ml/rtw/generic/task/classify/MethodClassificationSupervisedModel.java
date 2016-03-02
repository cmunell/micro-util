package edu.cmu.ml.rtw.generic.task.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class MethodClassificationSupervisedModel<D extends Datum<L>, L> extends MethodClassification<D, L> {
	private DataFeatureMatrix<D, L> data;
	private SupervisedModel<D, L> model;
	private SupervisedModelEvaluation<D, L> trainEvaluation; // FIXME Switch this to classification evaluation 
	private String[] parameterNames = { "data", "model", "trainEvaluation" };
	
	public MethodClassificationSupervisedModel() {
		this(null);
	}
	
	public MethodClassificationSupervisedModel(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		if (this.model != null) {
			String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + this.model.getParameterNames().length);
			for (int i = 0; i < this.model.getParameterNames().length; i++)
				parameterNames[this.parameterNames.length + i] = this.model.getParameterNames()[i];
			return parameterNames;
		} else 
			return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("data"))
			return (this.data == null) ? null : Obj.curlyBracedValue(this.data.getReferenceName());
		else if (parameter.equals("model"))
			return (this.model == null) ? null :  Obj.curlyBracedValue(this.model.getReferenceName());
		else if (parameter.equals("trainEvaluation"))
			return (this.trainEvaluation == null) ? null : Obj.curlyBracedValue(this.trainEvaluation.getReferenceName());
		else if (parameter.equals("modelInternal")) // FIXME This is a hack to allow for outputting trained model.  It's intentionally left out of parameterNames
			return (this.model == null) ? null : this.model.toParse();
		else if (this.model != null)
			return this.model.getParameterValue(parameter);
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("data"))
			this.data = (parameterValue == null) ? null : this.context.getMatchDataFeatures(parameterValue);
		else if (parameter.equals("model"))
			this.model = (parameterValue == null) ? null : this.context.getMatchModel(parameterValue);
		else if (parameter.equals("trainEvaluation"))
			this.trainEvaluation = (parameterValue == null) ? null : this.context.getMatchEvaluation(parameterValue);
		else if (this.model != null)
			return this.model.setParameterValue(parameter, parameterValue);
		else
			return false;
		
		return true;
	}

	@Override
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		return this.model.classify(data);
	}

	@Override
	public boolean init(DataFeatureMatrix<D, L> testData) {
		if (!this.data.isInitialized() && !this.data.init())
			return false;
		
		if (!testData.isInitialized() && !testData.init())
			return false;
		
		List<SupervisedModelEvaluation<D, L>> evals = new ArrayList<SupervisedModelEvaluation<D, L>>();
		evals.add(this.trainEvaluation);
		return this.model.train(this.data, testData, evals);
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
		return "SupervisedModel";
	}

	@Override
	public MethodClassification<D, L> clone() {
		MethodClassificationSupervisedModel<D, L> clone = new MethodClassificationSupervisedModel<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.model = this.model.clone();
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationSupervisedModel<D, L>(context);
	}
}
