package edu.cmu.ml.rtw.generic.task.classify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.model.SupervisedModel;
import edu.cmu.ml.rtw.generic.model.evaluation.metric.SupervisedModelEvaluation;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationSupervisedModel<D extends Datum<L>, L> extends MethodClassification<D, L> {
	private DataFeatureMatrix<D, L> data;
	private SupervisedModel<D, L> model;
	private SupervisedModelEvaluation<D, L> trainEvaluation; // FIXME Switch this to classification evaluation 
	private String[] parameterNames = { "data", "model", "trainEvaluation" };
	
	private boolean initialized = false;
	
	public MethodClassificationSupervisedModel() {
		this(null);
	}
	
	public MethodClassificationSupervisedModel(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		if (this.model != null) {
			String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + this.model.getParameterNames().length);
			for (int i = 0; i < this.model.getParameterNames().length; i++)
				parameterNames[this.parameterNames.length + i] = this.model.getParameterNames()[i];
			return parameterNames;
		} else 
			return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("data"))
			return (this.data == null) ? null : Obj.curlyBracedValue(this.data.getReferenceName());
		else if (parameter.equals("model"))
			return (this.model == null) ? null :  Obj.curlyBracedValue(this.model.getReferenceName());
		else if (parameter.equals("trainEvaluation"))
			return (this.trainEvaluation == null) ? null : Obj.curlyBracedValue(this.trainEvaluation.getReferenceName());
		else if (parameter.equals("modelInternal")) // FIXME This is a hack to allow for outputting trained model.  It's intentionally left out of parameterNames
			return (this.model == null) ? null : this.model.toParse();
		else if (this.model != null)
			return this.model.getParameterValue(parameter);
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("data"))
			this.data = (parameterValue == null) ? null : this.context.getMatchDataFeatures(parameterValue);
		else if (parameter.equals("model")) {
			this.model = (parameterValue == null) ? null : this.context.getMatchModel(parameterValue);
		} else if (parameter.equals("trainEvaluation"))
			this.trainEvaluation = (parameterValue == null) ? null : this.context.getMatchEvaluation(parameterValue);
		else if (this.model != null)
			return this.model.setParameterValue(parameter, parameterValue);
		else
			return false;
		
		return true;
	}

	@Override
	public Map<D, L> classify(DataSet<D, L> data) {
		DataFeatureMatrix<D, L> mat = new DataFeatureMatrix<D, L>(this.context, 
																  data.getReferenceName() + "_" + this.data.getFeatures().getReferenceName(), 
																  data,
																  this.data.getFeatures());
		
		return this.model.classify(mat);
	}
	
	@Override
	public Map<D, Pair<L, Double>> classifyWithScore(DataSet<D, L> data) {
		DataFeatureMatrix<D, L> mat = new DataFeatureMatrix<D, L>(this.context, 
				  data.getReferenceName() + "_" + this.data.getFeatures().getReferenceName(), 
				  data,
				  this.data.getFeatures());
		
		Map<D, Pair<L, Double>> scores = new HashMap<D, Pair<L, Double>>();
		Map<D, Map<L, Double>> p = this.model.posterior(mat);
		for (Entry<D, Map<L, Double>> entry : p.entrySet()) {
			if (entry.getValue().size() == 0)
				continue;
			double maxValue = Double.NEGATIVE_INFINITY;
			L maxLabel = null;
			for (Entry<L, Double> labelEntry : entry.getValue().entrySet()) {
				if (Double.compare(labelEntry.getValue(), maxValue) > 0) {
					maxLabel = labelEntry.getKey();
					maxValue = labelEntry.getValue();
				}
			}
			
			scores.put(entry.getKey(), new Pair<L, Double>(maxLabel, maxValue));
		}
		
		return scores;
	}

	@Override
	public boolean init(DataSet<D, L> testData) {
		if (this.initialized)
			return true;
		
		if (!this.data.isInitialized() && !this.data.init())
			return false;
		
		if (testData.isBuildable() && !testData.isBuilt() && !testData.build())
			return false;
		
		DataFeatureMatrix<D, L> testMat = new DataFeatureMatrix<D, L>(this.context, 
				  testData.getReferenceName() + "_" + this.data.getFeatures().getReferenceName(), 
				  testData,
				  this.data.getFeatures());
		
		List<SupervisedModelEvaluation<D, L>> evals = new ArrayList<SupervisedModelEvaluation<D, L>>();
		evals.add(this.trainEvaluation);
		
		this.initialized = this.model.train(this.data, testMat, evals);
		
		return this.initialized;
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
		return "SupervisedModel";
	}

	@Override
	public MethodClassification<D, L> clone(String referenceName) {
		MethodClassificationSupervisedModel<D, L> clone = new MethodClassificationSupervisedModel<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.model = this.model.clone();
		clone.initialized = this.initialized;
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationSupervisedModel<D, L>(context);
	}
}
