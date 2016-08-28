package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

public abstract class EvaluationClassification<D extends Datum<L>, L, E> extends CtxParsableFunction {
	public enum Type {
		MEASURE,
		CONFUSION_DATA,
		CONFUSION_MATRIX;
	}
	
	protected DatumContext<D, L> context;
	
	private Obj taskRef;
	private Obj methodRef;

	private String[] parameterNames = { "task", "method" };
	
	protected TaskClassification<D, L> task;
	protected MethodClassification<D, L> method;
	
	public EvaluationClassification() {
		this(null);
	}
	
	public EvaluationClassification(DatumContext<D, L> context) {
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
	
	public MethodClassification<D, L> getMethod() {
		return this.method;
	}
	
	public E compute() {
		return compute(false);
	}
	
	public abstract Type getType();
	public abstract E compute(boolean forceRecompute);
	public abstract int computeSampleSize(boolean forceRecompute);
	public abstract EvaluationClassification<D, L, ?> makeInstance(DatumContext<D, L> context);
}
