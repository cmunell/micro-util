package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class EvaluationClassificationConfusionMatrix<D extends Datum<L>, L> extends EvaluationClassification<D, L, ConfusionMatrix<D, L>> {
	private LabelMapping<L> labelMapping; 
	private String[] parameterNames = { "labelMapping" };
	
	public EvaluationClassificationConfusionMatrix() {
		this(null);
	}
	
	public EvaluationClassificationConfusionMatrix(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public Type getType() {
		return Type.CONFUSION_MATRIX;
	}

	@Override
	public ConfusionMatrix<D, L> compute(boolean forceRecompute) {
		Map<L, Map<L, List<D>>> actualToPredictedData = this.task.computeActualToPredictedData(this.method, forceRecompute);
		ConfusionMatrix<D, L> matrix = new ConfusionMatrix<D, L>(actualToPredictedData, this.labelMapping);
		return matrix;
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
		if (parameter.equals("labelMapping"))
			return (this.labelMapping == null) ? null : Obj.stringValue(String.valueOf(this.labelMapping.toString()));
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("labelMapping"))
			this.labelMapping = (parameterValue == null) ? null : this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(parameterValue));
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
		return "ConfusionMatrix";
	}

	@Override
	public String toString() {
		return getReferenceName() + ":\t" + compute().toString();
	}

	@Override
	public EvaluationClassification<D, L, ?> makeInstance(
			DatumContext<D, L> context) {
		return new EvaluationClassificationConfusionMatrix<D, L>(context);
	}
}
