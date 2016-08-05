package edu.cmu.ml.rtw.generic.task.classify.meta;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.Trainable;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationMetaPerfect extends MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> {
	private String[] parameterNames = { };
	
	public MethodClassificationMetaPerfect() {
		this(null);
	}
	
	public MethodClassificationMetaPerfect(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		return false;
	}
	
	@Override
	public Map<PredictionClassificationDatum<Boolean>, Boolean> classify(DataSet<PredictionClassificationDatum<Boolean>, Boolean> data) {
		Map<PredictionClassificationDatum<Boolean>, Boolean> map = new HashMap<PredictionClassificationDatum<Boolean>, Boolean>();
	
		for (PredictionClassificationDatum<Boolean> datum : data) {
			map.put(datum, classify(datum));
		}
		
		return map;
	}
	
	@Override
	public Map<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>> classifyWithScore(DataSet<PredictionClassificationDatum<Boolean>, Boolean> data) {
		Map<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>> map = new HashMap<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>>();
		
		for (PredictionClassificationDatum<Boolean> datum : data) {
			map.put(datum, classifyWithScore(datum));
		}
		
		return map;
	}

	@Override
	public boolean init(DataSet<PredictionClassificationDatum<Boolean>, Boolean> testData) {
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
		return "MetaPerfect";
	}

	@Override
	public MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> clone(String referenceName) {
		MethodClassificationMetaPerfect clone = new MethodClassificationMetaPerfect(this.context);
		if (!clone.fromParse(this.getModifiers(), referenceName, toParse()))
			return null;
		return clone;
	}

	@Override
	public MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> makeInstance(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		return new MethodClassificationMetaPerfect(context);
	}

	@Override
	public boolean hasTrainable() {
		return false;
	}

	@Override
	public Trainable<PredictionClassificationDatum<Boolean>, Boolean> getTrainable() {
		return null;
	}

	@Override
	public Boolean classify(PredictionClassificationDatum<Boolean> datum) {
		return true;
	}

	@Override
	public Pair<Boolean, Double> classifyWithScore(PredictionClassificationDatum<Boolean> datum) {
		return new Pair<Boolean, Double>(datum.getPrediction().getLabel().equals(datum.getPrediction().getDatum().getLabel()), 1.0);
	}

	@Override
	public Map<PredictionClassificationDatum<Boolean>, Double> score(DataSet<PredictionClassificationDatum<Boolean>, Boolean> data, Boolean label) {
		Map<PredictionClassificationDatum<Boolean>, Double> map = new HashMap<>();
		
		for (PredictionClassificationDatum<Boolean> datum : data) {
			map.put(datum, score(datum, label));
		}
		
		return map;
	}

	@Override
	public double score(PredictionClassificationDatum<Boolean> datum, Boolean label) {
		Pair<Boolean, Double> scoredPrediction = classifyWithScore(datum);
		if (label.equals(scoredPrediction.getFirst()))
			return 1.0;
		else
			return 0.0;
	}

}
