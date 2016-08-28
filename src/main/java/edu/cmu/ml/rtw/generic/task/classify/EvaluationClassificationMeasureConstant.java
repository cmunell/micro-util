package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class EvaluationClassificationMeasureConstant<D extends Datum<L> , L> extends EvaluationClassificationMeasure<D, L> {
	private double value = 0.0;
	private String[] parameterNames = { "value" };
	
	public EvaluationClassificationMeasureConstant() {
		this(null);
	}
	
	public EvaluationClassificationMeasureConstant(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Double compute(boolean forceRecompute) {
		return this.value;
	}
	
	@Override
	public int computeSampleSize(boolean forceRecompute) {
		return this.task.getData().size();
	}

	@Override
	public String[] getParameterNames() {
		String[] parentParameterNames = super.getParameterNames();
		String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + parentParameterNames.length);
		for (int i = 0; i < parentParameterNames.length; i++)
			parameterNames[this.parameterNames.length + i] = parentParameterNames[i];
		return parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("value"))
			return Obj.stringValue(String.valueOf(this.value));
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("value"))
			this.value = Double.valueOf(this.context.getMatchValue(parameterValue));
		else
			return super.setParameterValue(parameter, parameterValue);
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
		return "Constant";
	}

	@Override
	protected EvaluationClassificationMeasure<D, L> makeInstance() {
		return new EvaluationClassificationMeasureConstant<D, L>(this.context);
	}
	
	@Override
	public EvaluationClassification<D, L, ?> makeInstance(DatumContext<D, L> context) {
		return new EvaluationClassificationMeasureConstant<D, L>(context);
	}
}
