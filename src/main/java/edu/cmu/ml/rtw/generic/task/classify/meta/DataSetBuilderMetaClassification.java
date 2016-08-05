package edu.cmu.ml.rtw.generic.task.classify.meta;

import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DataSetBuilder;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.TaskClassification;

public class DataSetBuilderMetaClassification extends DataSetBuilder<PredictionClassificationDatum<Boolean>, Boolean> {
	private TaskClassification<?, ?> task;
	private MethodClassification<?, ?> method;
	private String[] parameterNames = { "task", "method" };
	
	private Obj methodRef;
	private Obj taskRef;
	
	public DataSetBuilderMetaClassification() {
		
	}
	
	public DataSetBuilderMetaClassification(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("task")) {
			return (this.task == null) ? null : this.taskRef;
		} else if (parameter.equals("method"))
			return (this.method == null) ? null : this.methodRef;
		else
			return this.method.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("task")) {
			this.taskRef = parameterValue;
			this.task = (parameterValue == null) ? null : this.context.getMatchClassifyTask(parameterValue);
		} else if (parameter.equals("method")) {
			this.methodRef = parameterValue;
			this.method = (parameterValue == null) ? null : this.context.getMatchClassifyMethod(parameterValue);
		} else
			return this.method.setParameterValue(parameter, parameterValue);
		return true;
	}

	@Override
	public DataSetBuilder<PredictionClassificationDatum<Boolean>, Boolean> makeInstance(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		return new DataSetBuilderMetaClassification(context);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public DataSet<PredictionClassificationDatum<Boolean>, Boolean> build() {
		DataSet<PredictionClassificationDatum<Boolean>, Boolean> data = new DataSet<PredictionClassificationDatum<Boolean>, Boolean>(this.context.getDatumTools());
		
		Map<?, PredictionClassification> predictions = ((MethodClassification)this.method).predict(this.task);
		for (PredictionClassification prediction : predictions.values()) {
			data.add(new PredictionClassificationDatum<Boolean>(
					this.context.getDataTools().getIncrementId(),
					prediction,
					prediction.getLabel().equals(prediction.getDatum().getLabel())
					));
		}
		
		return data;
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
		return "MetaClassification";
	}

}
