package edu.cmu.ml.rtw.generic.task.classify.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.task.classify.EvaluationClassificationMeasure;
import edu.cmu.ml.rtw.generic.task.classify.MethodClassification;
import edu.cmu.ml.rtw.generic.task.classify.Trainable;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationMetaEvaluation extends MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> {
	private List<EvaluationClassificationMeasure<?, ?>> evaluations;
	private boolean weightByScores = false;
	private String[] parameterNames = { "evaluations", "weightByScores" };
	
	public MethodClassificationMetaEvaluation() {
		this(null);
	}
	
	public MethodClassificationMetaEvaluation(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("evaluations")) {
			if (this.evaluations == null)
				return null;
			Obj.Array array = Obj.array();
			for (EvaluationClassificationMeasure<?, ?> measure : this.evaluations)
				array.add(Obj.curlyBracedValue(measure.getReferenceName()));
			return array;
		} else if (parameter.equals("weightByScores")) {
			return Obj.stringValue(String.valueOf(this.weightByScores));
		} else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("evaluations")) {
			if (parameterValue != null) {
				this.evaluations = new ArrayList<EvaluationClassificationMeasure<?, ?>>();
				Obj.Array array = (Obj.Array)parameterValue;
				for (int i = 0; i < array.size(); i++)
					this.evaluations.add((EvaluationClassificationMeasure<?, ?>)this.context.getAssignedMatches(array.get(i)).get(0));
			}
		} else if (parameter.equals("weightByScores")) {
			this.weightByScores = Boolean.valueOf(this.context.getMatchValue(parameterValue));
		} else
			return false;
		
		return true;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Double getScoreForDatum(PredictionClassificationDatum<Boolean> datum) {
		for (EvaluationClassificationMeasure<?, ?> measure : this.evaluations) {
			if (measure.getMethod().equals(datum.getPrediction().getMethod())) {
				double score = measure.compute();
				if (this.weightByScores)
					score *= ((MethodClassification)datum.getPrediction().getMethod()).score(datum.getPrediction().getDatum(), datum.getPrediction().getLabel());
				return score;
			}
		}
		
		return null;
	}
	
	@Override
	public Map<PredictionClassificationDatum<Boolean>, Boolean> classify(DataSet<PredictionClassificationDatum<Boolean>, Boolean> data) {
		Map<PredictionClassificationDatum<Boolean>, Boolean> map = new HashMap<PredictionClassificationDatum<Boolean>, Boolean>();
		
		for (PredictionClassificationDatum<Boolean> datum : data) {
			map.put(datum, true);
		}
		
		return map;
	}
	
	@Override
	public Map<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>> classifyWithScore(DataSet<PredictionClassificationDatum<Boolean>, Boolean> data) {
		Map<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>> map = new HashMap<PredictionClassificationDatum<Boolean>, Pair<Boolean, Double>>();
		
		for (PredictionClassificationDatum<Boolean> datum : data) {
			map.put(datum, new Pair<Boolean, Double>(true, getScoreForDatum(datum)));
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
		return "MetaEvaluation";
	}

	@Override
	public MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> clone(String referenceName) {
		MethodClassificationMetaEvaluation clone = new MethodClassificationMetaEvaluation(this.context);
		if (!clone.fromParse(this.getModifiers(), referenceName, toParse()))
			return null;
		return clone;
	}

	@Override
	public MethodClassification<PredictionClassificationDatum<Boolean>, Boolean> makeInstance(DatumContext<PredictionClassificationDatum<Boolean>, Boolean> context) {
		return new MethodClassificationMetaEvaluation(context);
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
		return new Pair<Boolean, Double>(true, getScoreForDatum(datum));
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
		if (label)
			return getScoreForDatum(datum);
		else
			return 1.0 - getScoreForDatum(datum);
	}

}
