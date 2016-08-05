package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.annotation.TernaryLabel;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.MathUtil;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationTernaryTest<D extends Datum<TernaryLabel>> extends MethodClassification<D, TernaryLabel> {
	private double correct;
	private double incorrect;
	private String[] parameterNames = { "correct", "incorrect" };
	
	public MethodClassificationTernaryTest() {
		this(null);
	}
	
	public MethodClassificationTernaryTest(DatumContext<D, TernaryLabel> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("correct"))
			return Obj.stringValue(String.valueOf(this.correct));
		else if (parameter.equals("incorrect"))
			return Obj.stringValue(String.valueOf(this.incorrect));
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("correct"))
			this.correct = Double.valueOf(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("incorrect"))
			this.incorrect = Double.valueOf(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Map<D, TernaryLabel> classify(DataSet<D, TernaryLabel> data) {
		int numCorrect = (int)(data.size() * this.correct);
		int numIncorrect = (int)(data.size() * this.incorrect);
		
		Map<D, TernaryLabel> map = new HashMap<D, TernaryLabel>();
		List<D> dataPerm = (List)MathUtil.randomPermutation(this.context.getDataTools().getGlobalRandom(), Arrays.asList(data.toArray()));
		
		for (int i = 0; i < numCorrect+numIncorrect; i++) {
			if (i < numCorrect) {
				map.put(dataPerm.get(i), dataPerm.get(i).getLabel());
			} else {
				if (dataPerm.get(i).getLabel() == TernaryLabel.FIRST)
					map.put(dataPerm.get(i), TernaryLabel.SECOND);
				else if (dataPerm.get(i).getLabel() == TernaryLabel.SECOND)
					map.put(dataPerm.get(i), TernaryLabel.THIRD);
				else if (dataPerm.get(i).getLabel() == TernaryLabel.THIRD)
					map.put(dataPerm.get(i), TernaryLabel.FIRST);
			}
		}
		
		return map;
	}

	@Override
	public boolean init(DataSet<D, TernaryLabel> testData) {
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
		return "TernaryTest";
	}

	@Override
	public MethodClassification<D, TernaryLabel> clone(String referenceName) {
		MethodClassificationTernaryTest<D> clone = new MethodClassificationTernaryTest<D>(this.context);
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<D, TernaryLabel> makeInstance(DatumContext<D, TernaryLabel> context) {
		return new MethodClassificationTernaryTest<D>(context);
	}

	@Override
	public Map<D, Pair<TernaryLabel, Double>> classifyWithScore(DataSet<D, TernaryLabel> data) {
		Map<D, TernaryLabel> classification = classify(data);
		Map<D, Pair<TernaryLabel, Double>> scores = new HashMap<>();
		for (Entry<D, TernaryLabel> entry : classification.entrySet()) {
			scores.put(entry.getKey(), new Pair<TernaryLabel, Double>(entry.getValue(), 1.0));
		}
		return scores;
	}

	@Override
	public boolean hasTrainable() {
		return false;
	}

	@Override
	public Trainable<D, TernaryLabel> getTrainable() {
		return null;
	}

	@Override
	public TernaryLabel classify(D datum) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Pair<TernaryLabel, Double> classifyWithScore(D datum) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<D, Double> score(DataSet<D, TernaryLabel> data,
			TernaryLabel label) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double score(D datum, TernaryLabel label) {
		throw new UnsupportedOperationException();
	}
}
