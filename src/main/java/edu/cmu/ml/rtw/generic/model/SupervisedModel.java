package edu.cmu.ml.rtw.generic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.CtxParsableFunction;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * SupervisedModel represents a supervised classification model
 * that can be trained and evaluated using 
 * edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSets.
 * 
 * Implementations of particular supervised models derive from the 
 * SupervisedModel class,
 * and the SupervisedModel class is primarily responsible for 
 * providing the
 * methods necessary for deserializing models from configuration files.
 * Models are defined in the configuration files by strings of
 * the form:
 * 
 * model_[modelReferenceName]=[genericModelName (e.g. SVM)]([hyper-parameter 1]=[hyper-parameter value 1],...)
 * {
 *    [extra info 1]=[extra info value 1]
 *    [...]
 * }
 * 
 * Where strings in square brackets are replaced by model specific info.
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public abstract class SupervisedModel<D extends Datum<L>, L> extends CtxParsableFunction {
	protected Context<D, L> context;
	
	protected Set<L> validLabels; // Labels that this model can assign
	protected LabelMapping<L> labelMapping; // Mapping from actual labels into valid labels
	
	// Labels that the model should assign regardless of its training
	protected Map<D, L> fixedDatumLabels = new HashMap<D, L>(); 
	
	/**
	 * @return a generic instance of some model that can be used
	 * when deserializing from an experiment configuration file
	 */
	public abstract SupervisedModel<D, L> makeInstance(Context<D, L> context);
	
	protected abstract boolean fromParseInternalHelper(AssignmentList internalAssignments);
	
	protected abstract AssignmentList toParseInternalHelper(AssignmentList internalAssignments);
	
	protected abstract <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(Context<T, Boolean> context, LabelIndicator<L> labelIndicator, SupervisedModel<T, Boolean> binaryModel);
	
	/**
	 * @return the generic name of the model in the configuration files.  For
	 * model class SupervisedModel[X], the generic name should usually be X.
	 */
	public abstract String getGenericName();
	
	public abstract boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations);
	
	/**
	 * @param data
	 * @return a map from datums to distributions over labels for the datums
	 */
	public abstract Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data);
	
	public boolean setLabelMapping(LabelMapping<L> labelMapping) {
		this.labelMapping = labelMapping;
		return true;
	}
	
	public boolean fixDatumLabels(Map<D, L> fixedDatumLabels) {
		this.fixedDatumLabels = fixedDatumLabels;
		return true;
	}
	
	public Set<L> getValidLabels() {
		return this.validLabels;
	}
	
	public LabelMapping<L> getLabelMapping() {
		return this.labelMapping;
	}
	
	public L mapValidLabel(L label) {
		if (label == null)
			return null;
		if (this.labelMapping != null)
			label = this.labelMapping.map(label);
		if (this.validLabels.contains(label))
			return label;
		else
			return null;
	}
	
	public Map<D, L> classify(FeaturizedDataSet<D, L> data) {
		Map<D, L> classifiedData = new HashMap<D, L>();
		Map<D, Map<L, Double>> posterior = posterior(data);
	
		for (Entry<D, Map<L, Double>> datumPosterior : posterior.entrySet()) {
			if (this.fixedDatumLabels.containsKey(datumPosterior.getKey())) {
				classifiedData.put(datumPosterior.getKey(), this.fixedDatumLabels.get(datumPosterior.getKey()));
				continue;
			}
			
			Map<L, Double> p = datumPosterior.getValue();
			double max = Double.NEGATIVE_INFINITY;
			L argMax = null;
			for (Entry<L, Double> entry : p.entrySet()) {
				if (entry.getValue() > max) {
					max = entry.getValue();
					argMax = entry.getKey();
				}
			}
			classifiedData.put(datumPosterior.getKey(), argMax);
		}
	
		return classifiedData;
	}
	
	//
	
	public SupervisedModel<D, L> clone() {
		SupervisedModel<D, L> clone = this.context.getDatumTools().makeModelInstance(getGenericName(), this.context);
		if (!clone.fromParse(getModifiers(), getReferenceName(), toParse()))
			return null;
		return clone;
	}
	
	public <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinary(Context<T, Boolean> context, LabelIndicator<L> labelIndicator) {
		SupervisedModel<T, Boolean> binaryModel = context.getDatumTools().makeModelInstance(getGenericName(), context);
		
		binaryModel.referenceName = this.referenceName;
		binaryModel.modifiers = this.modifiers;
		binaryModel.validLabels = new HashSet<Boolean>();
		binaryModel.validLabels.add(true);
		binaryModel.validLabels.add(false);
		
		String[] parameterNames = getParameterNames();
		for (int i = 0; i < parameterNames.length; i++)
			binaryModel.setParameterValue(parameterNames[i], getParameterValue(parameterNames[i]));
		
		return makeBinaryHelper(context, labelIndicator, binaryModel);
	}
	
	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		if (!internalAssignments.contains("validLabels"))
			return false;
		
		this.validLabels = new HashSet<L>();
		List<String> validLabels = this.context.getMatchArray(internalAssignments.get("validLabels").getValue());
		
		for (int i = 0; i < validLabels.size(); i++) {
			this.validLabels.add(this.context.getDatumTools().labelFromString(validLabels.get(i)));
		}
		
		if (internalAssignments.contains("labelMapping")) {
			this.labelMapping = this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(internalAssignments.get("labelMapping").getValue()));
		}
		
		return fromParseInternalHelper(internalAssignments);
	}
	
	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList internalAssignments = new AssignmentList();
		
		Obj.Array validLabels = new Obj.Array();
		for (L validLabel : this.validLabels) {
			validLabels.add(Obj.stringValue(validLabel.toString()));
		}
		
		internalAssignments.add(
				Assignment.assignmentTyped(new ArrayList<String>(), Context.ARRAY_STR, "validLabels", validLabels)
		);
		
		if (this.labelMapping != null) {
			internalAssignments.add(
					Assignment.assignmentTyped(new ArrayList<String>(), Context.VALUE_STR, "labelMapping", Obj.stringValue(this.labelMapping.toString()))
			);	
		}
		
		return toParseInternalHelper(internalAssignments);
	}	
}
