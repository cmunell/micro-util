package edu.cmu.ml.rtw.generic.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelIndicator;
import edu.cmu.ml.rtw.generic.data.feature.Feature;
import edu.cmu.ml.rtw.generic.data.feature.FeaturizedDataSet;
import edu.cmu.ml.rtw.generic.model.constraint.Constraint;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

/**
 * FIXME Needs refactoring...
 * 
 * SupervisedModelPartition partitions the training and evaluation
 * according to a set of constraints so that all data in a single
 * part of the partition satisfy the corresponding constraint.  The
 * parts of the partition are given to separate models, and the predictions
 * of these models are used to constrain the predictions of the 
 * later models if the later models are structured.
 * 
 * The 'defaultLabel' hyper-parameter determines the default label
 * that SupervisedModelPartition predicts for datums that do not
 * satisfy any of the partition constraints (and so have no corresponding
 * model).
 * 
 * @author Bill McDowell
 *
 * @param <D> datum type
 * @param <L> datum label type
 */
public class SupervisedModelPartition<D extends Datum<L>, L> extends SupervisedModel<D, L> {
	private L defaultLabel;
	private String[] hyperParameterNames = { "defaultLabel" };
	
	// List of partition model names in the order in which they should
	// run.  If a model occurs prior to another in this list, then its
	// predictions take precedence (especially when structured prediction is involved)
	private List<String> orderedModels;
	
	/// Map model names to constraints, models, and feature sets
	private Map<String, Constraint<D, L>> constraints;
	private Map<String, SupervisedModel<D, L>> models;
	private Map<String, List<Feature<D, L>>> features;
	
	public SupervisedModelPartition() {
		this.orderedModels = new ArrayList<String>();
		this.constraints = new HashMap<String, Constraint<D, L>>();
		this.models = new HashMap<String, SupervisedModel<D, L>>();
		this.features = new HashMap<String, List<Feature<D, L>>>();
	}
	
	public SupervisedModelPartition(Context<D, L> context) {
		this();
		this.context = context;
	}
	
	/**
	 * @param data
	 * @param testData
	 * @param evaluations
	 * @return true if the partitioned models have been trained on the data.  The
	 * predictions of the earlier trained models are used to constrain the training
	 * of the later partitioned models (using the 'fixDatumLabels' method) for
	 * when the later models are structured and the structures overlap. 
	 */
	@Override
	public boolean train(FeaturizedDataSet<D, L> data, FeaturizedDataSet<D, L> testData, List<SupervisedModelEvaluation<D, L>> evaluations) {
		Map<D, L> fixedLabels = new HashMap<D, L>();
		for (int i = 0; i < this.orderedModels.size(); i++) {
			String modelName = this.orderedModels.get(i);
			FeaturizedDataSet<D, L> modelData = this.constraints.get(modelName).getSatisfyingSubset(data, this.labelMapping);
			FeaturizedDataSet<D, L> modelTestData = this.constraints.get(modelName).getSatisfyingSubset(testData, this.labelMapping);
		
			for (Feature<D, L> feature : this.features.get(modelName)) {
				if (!feature.init(modelData) || !modelData.addFeature(feature))
					return false;
			}
			
			if (!this.models.get(modelName).train(modelData, modelTestData, evaluations))
				return false;
			
			fixedLabels.putAll(this.models.get(modelName).classify(modelData));
			for (int j = i + 1; j < this.orderedModels.size(); j++)
				this.models.get(this.orderedModels.get(j)).fixDatumLabels(fixedLabels);
		}
		
		return true;
	}

	/**
	 * @param data - data set for which to compute posteriors
	 * @return a map from datums to their posterior distributions. The 
	 * data is partitioned by the constraints, and each part of the partition
	 * is given a posterior according to a model corresponding to that 
	 * partition.  The maximum posterior labels of the parts that are
	 * processed first are used to constrain the posteriors for later parts'
	 * models.
	 */
	@Override
	public Map<D, Map<L, Double>> posterior(FeaturizedDataSet<D, L> data) {
		Map<D, Map<L, Double>> posterior = new HashMap<D, Map<L, Double>>();
		Map<D, L> fixedLabels = new HashMap<D, L>(); // Labels that are fixed by the first models for later models
		for (int i = 0; i < this.orderedModels.size(); i++) {
			String modelName = this.orderedModels.get(i);
			FeaturizedDataSet<D, L> modelData = this.constraints.get(modelName).getSatisfyingSubset(data, this.labelMapping);
			for (Feature<D, L> feature : this.features.get(modelName))
				if (!modelData.addFeature(feature))
					return null;
			
			Map<D, Map<L, Double>> modelPosterior = this.models.get(modelName).posterior(modelData);
			for (Entry<D, Map<L, Double>> pEntry : modelPosterior.entrySet()) {
				if (posterior.containsKey(pEntry.getKey()))
					continue;
				Map<L, Double> p = new HashMap<L, Double>();
				L bestLabel = this.defaultLabel;
				double bestLabelValue = 0.0;
				for (L validLabel : this.validLabels) {
					if (!pEntry.getValue().containsKey(validLabel)) {
						p.put(validLabel, 0.0);
					} else {
						double labelValue = pEntry.getValue().get(validLabel);
						p.put(validLabel, labelValue);
						if (labelValue >= bestLabelValue) {
							bestLabel = validLabel;
							bestLabelValue = labelValue;
						}
					}
				}
				posterior.put(pEntry.getKey(), p);
				fixedLabels.put(pEntry.getKey(), bestLabel);
			}
			
			// Fix labels for later models
			for (int j = i + 1; j < this.orderedModels.size(); j++)
				this.models.get(this.orderedModels.get(j)).fixDatumLabels(fixedLabels);
		}
		
		for (D datum : data) { // Mark remaining data with default label
			Map<L, Double> p = new HashMap<L, Double>();
			for (L validLabel : this.validLabels)
				p.put(validLabel, 0.0);
			p.put(this.defaultLabel, 1.0);
			
			if (!posterior.containsKey(datum))
				posterior.put(datum, p);
		}
		
		return posterior;
	}

	/**
	 * @param name - variable name on the current line of extra info
	 * @param reader
	 * @param datumTools
	 * @return true if a partition constraint, model, or feature has been
	 * deserialized from the current line using reader.  The models in
	 * the extra info should be specified using the same syntax as models
	 * specified in experiments.  See the classes under edu.cmu.ml.rtw.generic.experiment for
	 * documentation.
	 *  
	 */
	/*@Override
	protected boolean deserializeExtraInfo(String name, BufferedReader reader,
			Tools<D, L> datumTools) throws IOException {
		String[] nameParts = name.split("_");
		String type = nameParts[0];
		String modelReference = nameParts[1];
		
		if (type.equals("model")) {
			String modelName = SerializationUtil.deserializeGenericName(reader);
			SupervisedModel<D, L> model = datumTools.makeModelInstance(modelName);
			if (!model.deserialize(reader, false, false, datumTools, modelReference))
				return false;
			this.orderedModels.add(modelReference);
			this.models.put(modelReference, model);
		} else if (type.equals("feature")) {
			String featureReference = null;
			boolean ignored = false;
			if (nameParts.length > 2)
				featureReference = nameParts[2];
			if (nameParts.length > 3)
				ignored = true;
			
			String featureName = SerializationUtil.deserializeGenericName(reader);
			Feature<D, L> feature = datumTools.makeFeatureInstance(featureName);
			if (!feature.deserialize(reader, false, false, datumTools, featureReference, ignored))
				return false;
			
			if (!this.features.containsKey(modelReference))
				this.features.put(modelReference, new ArrayList<Feature<D, L>>());
			this.features.get(modelReference).add(feature);
		} else if (type.equals("constraint")) {
			this.constraints.put(modelReference, Constraint.<D,L>fromString(reader.readLine()));
		}
		
		return true;
	}

	@Override
	protected boolean deserializeParameters(BufferedReader reader,
			Tools<D, L> datumTools) throws IOException {
		// FIXME Do this later when necessary
		return true;
	}
	
	@Override
	protected boolean serializeExtraInfo(Writer writer) throws IOException {
		for (String modelName : this.orderedModels) {
			writer.write("\tmodel" + "_" + modelName + "=" + this.models.get(modelName).toString(false) + "\n");
			// FIXME Include extra info in model output
			
			writer.write("\tconstraint_" + modelName + "=" + this.constraints.get(modelName).toString() + "\n");
			List<Feature<D, L>> features = this.features.get(modelName);
			for (int i = 0; i < features.size(); i++) {
				writer.write("\tfeature_" + modelName);
				if (features.get(i).getReferenceName() != null)
					writer.write("_" + features.get(i).getReferenceName());
				if (features.get(i).isIgnored())
					writer.write("_ignored");
				writer.write("=" + features.get(i).toString(false) + "\n");
			}
		}
		
		return true;
	}

	@Override
	protected boolean serializeParameters(Writer writer) throws IOException {
		for (Entry<String, SupervisedModel<D, L>> entry : this.models.entrySet()) {
			writer.write("BEGIN PARAMETERS " + entry.getKey() + "\n\n");
			entry.getValue().serializeParameters(writer);
			writer.write("\nEND PARAMETERS " +  entry.getKey() + "\n\n");
			
			// Write features (that have been initialized)
			writer.write("BEGIN FEATURES " +  entry.getKey() + "\n\n");
			for (int j = 0; j < this.features.get(entry.getKey()).size(); j++) {
				this.features.get(entry.getKey()).get(j).serialize(writer, true);
				writer.write("\n\n");
			}
			writer.write("END FEATURES " + entry.getKey() + "\n\n");
		}
		
		return true;
	}*/

	@Override
	public String getGenericName() {
		return "Partition";
	}

	@Override
	public Obj getParameterValue(String parameter) {
		/* FIXME 
		int firstUnderscoreIndex = parameter.indexOf("_");
		if (parameter.equals("defaultLabel"))
			return (this.defaultLabel == null) ? null : this.defaultLabel.toString();
		else if (firstUnderscoreIndex >= 0) {
			String modelReference = parameter.substring(0, firstUnderscoreIndex);
			String modelParameter = parameter.substring(firstUnderscoreIndex + 1);
			this.models.get(modelReference).getParameterValue(modelParameter);
		}*/
		
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		/* FIXME int firstUnderscoreIndex = parameter.indexOf("_");
		if (parameter.equals("defaultLabel")) {
			this.defaultLabel = (parameterValue == null) ? null : datumTools.labelFromString(parameterValue);
			return true;
		} else if (firstUnderscoreIndex >= 0) {
			String modelReference = parameter.substring(0, firstUnderscoreIndex);
			String modelParameter = parameter.substring(firstUnderscoreIndex + 1);
			this.models.get(modelReference).setParameterValue(modelParameter, parameterValue, datumTools);
		}*/
		return false;
	}

	@Override
	public String[] getParameterNames() {
		List<String> hyperParameterNames = new ArrayList<String>();
		for (String parameterName : this.hyperParameterNames) {
			hyperParameterNames.add(parameterName);
		}
		
		if (this.models != null) {
			for (SupervisedModel<D, L> model : this.models.values()) {
				String[] modelParameterNames = model.getParameterNames();
				for (String parameterName : modelParameterNames) {
					hyperParameterNames.add(model.getReferenceName() + "_" + parameterName);
				}
			}
		}
		
		return this.hyperParameterNames;
	}

	@Override
	public SupervisedModel<D, L> makeInstance(Context<D, L> context) {
		return new SupervisedModelPartition<D, L>(context);
	}

	@Override
	protected boolean fromParseInternalHelper(AssignmentList internalAssignments) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected AssignmentList toParseInternalHelper(
			AssignmentList internalAssignments) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected <T extends Datum<Boolean>> SupervisedModel<T, Boolean> makeBinaryHelper(
			Context<T, Boolean> context, LabelIndicator<L> labelIndicator,
			SupervisedModel<T, Boolean> binaryModel) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*@SuppressWarnings("unchecked")
	public <D1 extends Datum<L1>, L1> SupervisedModel<D1, L1> clone(Datum.Tools<D1, L1> datumTools, Map<String, String> environment, boolean copyLabelObjects) {
		SupervisedModelPartition<D1, L1> clone = (SupervisedModelPartition<D1, L1>)super.clone(datumTools, environment, copyLabelObjects);
		
		// FIXME
		// Need to clone constraints, but this works for now as long as D == D1
		if (copyLabelObjects) {
			clone.constraints = new HashMap<String, Constraint<D1, L1>>();
			for (Entry<String, Constraint<D, L>> entry : this.constraints.entrySet())
				clone.constraints.put(entry.getKey(), (Constraint<D1, L1>)entry.getValue());
		}
			
		clone.features = new HashMap<String, List<Feature<D1, L1>>>();
		clone.models = new HashMap<String, SupervisedModel<D1, L1>>();
		clone.orderedModels = new ArrayList<String>();

		for (String model : this.orderedModels) {
			clone.orderedModels.add(model);
			clone.models.put(model, this.models.get(model).clone(datumTools, environment, copyLabelObjects));
			clone.features.put(model, new ArrayList<Feature<D1, L1>>());
			for (int j = 0; j < this.features.get(model).size(); j++) {
				clone.features.get(model).add(this.features.get(model).get(j).clone(datumTools, environment));
			}
		}
		
		return clone;
	}*/

}
