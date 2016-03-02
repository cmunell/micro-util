package edu.cmu.ml.rtw.generic.model.evaluation.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * SupervisedModelEvaluation represents evaluation measure
 * for a supervised classification model.  
 * 
 * Implementations of particular evaluation measures derive
 * from SupervisedModelEvaluation, and SupervisedModelEvaluation
 * is primarily responsible for providing the methods necessary
 * for deserializing these evaluation measures from configuration
 * files.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public abstract class SupervisedModelEvaluation<D extends Datum<L>, L> extends CtxParsableFunction {
	protected DatumContext<D, L> context;
	protected LabelMapping<L> labelMapping;
	
	/**
	 * @return a generic name by which to refer to the evaluation
	 */
	public abstract String getGenericName();
	
	/**
	 * 
	 * @param model
	 * @param data
	 * @param predictions
	 * @return the value of the evaluation measure for the model with predictions on the
	 * given data set.  This method should generally start by calling 
	 * 'getMappedActualAndPredictedLabels' to get the labels according to the 'labelMapping'
	 * function, and then compute the measure using those returned labels.
	 * 
	 */
	protected abstract double compute(SupervisedModel<D, L> model, DataFeatureMatrix<D, L> data, Map<D, L> predictions);
	
	/**
	 * @return a generic instance of the evaluation measure.  This is used when deserializing
	 * the parameters for the measure from a configuration file
	 */
	public abstract SupervisedModelEvaluation<D, L> makeInstance(DatumContext<D, L> context);
	
	/**
	 * @param predictions
	 * @return a list of pairs of actual predicted labels that are mapped by
	 * the label mapping (labelMapping) from the given predictions map
	 */
	protected List<Pair<L, L>> getMappedActualAndPredictedLabels(Map<D, L> predictions) {
		List<Pair<L, L>> mappedLabels = new ArrayList<Pair<L, L>>(predictions.size());
		for (Entry<D, L> prediction : predictions.entrySet()) {
			L actual = prediction.getKey().getLabel();
			L predicted = prediction.getValue();
			if (this.labelMapping != null) {
				actual = this.labelMapping.map(actual);
				predicted = this.labelMapping.map(predicted);
			}
			
			Pair<L, L> mappedPair = new Pair<L, L>(actual, predicted);
			mappedLabels.add(mappedPair);
		}
		
		return mappedLabels;
	}
	
	/**
	 * 
	 * @param model
	 * @param data
	 * @param predictions
	 * @return the value of the evaluation measure for the model with predictions on the
	 * given data set.  If the model has a label mapping, then it is used when 
	 * computing the evaluation measure.
	 */
	public double evaluate(SupervisedModel<D, L> model, DataFeatureMatrix<D, L> data, Map<D, L> predictions) {
		LabelMapping<L> modelLabelMapping = null;
		if (model != null)
			model.getLabelMapping();
		if (this.labelMapping != null)
			model.setLabelMapping(this.labelMapping);
		
		double evaluation = compute(model, data, predictions);
		
		if (model != null)
			model.setLabelMapping(modelLabelMapping);
		
		return evaluation;
	}
		
	public SupervisedModelEvaluation<D, L> clone() {
		SupervisedModelEvaluation<D, L> clone = this.context.getDatumTools().makeEvaluationInstance(getGenericName(), this.context);
		if (!clone.fromParse(getModifiers(), getReferenceName(), toParse()))
			return null;
		return clone;
	}
	
	public <T extends Datum<Boolean>> SupervisedModelEvaluation<T, Boolean> makeBinary(DatumContext<T, Boolean> context, LabelIndicator<L> labelIndicator) {
		SupervisedModelEvaluation<T, Boolean> binaryEvaluation = context.getDatumTools().makeEvaluationInstance(getGenericName(), context);
		
		if (binaryEvaluation == null)
			return null;
		
		binaryEvaluation.referenceName = this.referenceName;
		binaryEvaluation.modifiers = this.modifiers;
		binaryEvaluation.labelMapping = null;
		
		String[] parameterNames = getParameterNames();
		for (int i = 0; i < parameterNames.length; i++)
			binaryEvaluation.setParameterValue(parameterNames[i], getParameterValue(parameterNames[i]));
		
		return binaryEvaluation;
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (internalAssignments == null)
			return true;
		
		if (internalAssignments.contains("labelMapping")) {
			Obj.Value labelMapping = (Obj.Value)internalAssignments.get("labelMapping").getValue();
			this.labelMapping = this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(labelMapping));
		}
		
		return true;
	}
	
	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList internalAssignments = new AssignmentList();
		
		if (this.labelMapping != null) {
			Obj.Value labelMapping = Obj.stringValue(this.labelMapping.toString());
			internalAssignments.add(Assignment.assignmentTyped(new ArrayList<String>(), Context.ObjectType.VALUE.toString(), "labelMapping", labelMapping));
		}
		
		return (internalAssignments.size() == 0) ? null : internalAssignments;
	}
}
