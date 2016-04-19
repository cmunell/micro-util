package edu.cmu.ml.rtw.generic.task.classify.multi;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.opt.search.ParameterSearchable;

public abstract class EvaluationMultiClassificationMeasure extends EvaluationMultiClassification<Double> implements ParameterSearchable {
	public EvaluationMultiClassificationMeasure() {
		this(null);
	}
	
	public EvaluationMultiClassificationMeasure(Context context) {
		super(context);
	}

	@Override
	public int getStageCount() {
		return 2;
	}

	@Override
	public boolean runStage(int stageIndex) {
		if (stageIndex == 0)
			return this.method.init(this.task.getData());
		else
			return this.task.computeActualToPredictedData(this.method) != null;
	}

	@Override
	public double evaluate() {
		return this.compute();
	}

	@Override
	public boolean lastStageRequiresCloning() {
		return true;
	}

	@Override
	public Type getType() {
		return Type.MEASURE;
	}
	
	@Override
	public ParameterSearchable clone() {
		EvaluationMultiClassificationMeasure clone = makeInstance();
		
		if (!clone.fromParse(this.modifiers, this.referenceName, this.toParse()))
			return null;
		
		clone.method = this.method.clone();
		clone.task = this.task.clone();
		
		return clone;
	}
	
	@Override
	public String toString() {
		return getReferenceName() + ":\t" + compute();
	}
	
	protected abstract EvaluationMultiClassificationMeasure makeInstance();
}
