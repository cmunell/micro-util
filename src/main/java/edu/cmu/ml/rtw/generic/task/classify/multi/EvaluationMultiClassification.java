package edu.cmu.ml.rtw.generic.task.classify.multi;

import java.util.Arrays;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

public abstract class EvaluationMultiClassification<E> extends CtxParsableFunction {
	public enum Type {
		MEASURE,
		CONFUSION_DATA,
		CONFUSION_MATRIX;
	}
	
	protected Context context;
	
	protected TaskMultiClassification task;
	protected MethodMultiClassification method;
	private String[] parameterNames = { "task", "method" };
	
	public EvaluationMultiClassification() {
		this(null);
	}
	
	public EvaluationMultiClassification(Context context) {
		this.context = context;
	}
	
	@Override
	public String[] getParameterNames() {
		if (this.method != null) {
			String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + this.method.getParameterNames().length);
			String[] methodParameterNames = this.method.getParameterNames();
			for (int i = 0; i < methodParameterNames.length; i++)
				parameterNames[this.parameterNames.length + i] = methodParameterNames[i];
			return parameterNames;
		} else 
			return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("task"))
			return (this.task == null) ? null : Obj.curlyBracedValue(this.task.getReferenceName());
		else if (parameter.equals("method"))
			return (this.method == null) ? null : Obj.curlyBracedValue(this.method.getReferenceName());
		else
			return this.method.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("task"))
			this.task = (parameterValue == null) ? null : this.context.getMatchMultiClassifyTask(parameterValue);
		else if (parameter.equals("method"))
			this.method = (parameterValue == null) ? null : this.context.getMatchMultiClassifyMethod(parameterValue);
		else
			return this.method.setParameterValue(parameter, parameterValue);
		return true;
	}
	
	public MethodMultiClassification getMethod() {
		return this.method;
	}
	
	public abstract Type getType();
	public abstract E compute();
	public abstract EvaluationMultiClassification<E> makeInstance(Context context);
}
