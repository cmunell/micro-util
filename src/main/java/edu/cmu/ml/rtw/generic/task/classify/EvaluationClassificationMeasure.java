package edu.cmu.ml.rtw.generic.task.classify;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.opt.search.ParameterSearchable;

public abstract class EvaluationClassificationMeasure<D extends Datum<L>, L> extends EvaluationClassification<D, L, Double> implements ParameterSearchable {
	public EvaluationClassificationMeasure() {
		this(null);
	}
	
	public EvaluationClassificationMeasure(DatumContext<D, L> context) {
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
			return this.task.computeActualToPredictedData(this.method, true) != null;
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
		EvaluationClassificationMeasure<D, L> clone = makeInstance();
		
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
	
	protected abstract EvaluationClassificationMeasure<D, L> makeInstance();
}
