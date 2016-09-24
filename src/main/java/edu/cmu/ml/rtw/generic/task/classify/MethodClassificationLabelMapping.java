package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.Assignment;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationLabelMapping<D extends Datum<L>, L> extends MethodClassification<D, L> {
	private LabelMapping<L> labelMapping;
	private MethodClassification<D, L> method;
	private String[] parameterNames = { "labelMapping", "method" };
	
	public MethodClassificationLabelMapping() {
		this(null);
	}
	
	public MethodClassificationLabelMapping(DatumContext<D, L> context) {
		super(context);
	}
	
	public MethodClassification<D, L> getInnerMethod() {
		return this.method;
	}

	@Override
	public String[] getParameterNames() {
		if (this.method != null) {
			String[] parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + this.method.getParameterNames().length);
			for (int i = 0; i < this.method.getParameterNames().length; i++)
				parameterNames[this.parameterNames.length + i] = this.method.getParameterNames()[i];
			return parameterNames;
		} else 
			return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("labelMapping"))
			return (this.labelMapping == null) ? null : Obj.curlyBracedValue(this.labelMapping.toString());
		else if (parameter.equals("method"))
			return (this.method == null) ? null :  Obj.curlyBracedValue(this.method.getReferenceName());
		else if (this.method != null)
			return this.method.getParameterValue(parameter);
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("labelMapping"))
			this.labelMapping = (parameterValue == null) ? null : this.context.getDatumTools().getLabelMapping(this.context.getMatchValue(parameterValue));
		else if (parameter.equals("method"))
			this.method = (parameterValue == null) ? null : this.context.getMatchClassifyMethod(parameterValue);
		else if (this.method != null)
			return this.method.setParameterValue(parameter, parameterValue);
		else
			return false;
		
		return true;
	}

	@Override
	public Map<D, L> classify(DataSet<D, L> data) {
		Map<D, L> map = this.method.classify(data);
		for (Entry<D, L> entry : map.entrySet())
			entry.setValue(this.labelMapping.map(entry.getValue()));
		return map;
	}
	
	@Override
	public Map<D, Pair<L, Double>> classifyWithScore(DataSet<D, L> data) {
		Map<D, Pair<L, Double>> map = this.method.classifyWithScore(data);
		for (Entry<D, Pair<L, Double>> entry : map.entrySet())
			entry.setValue(new Pair<>(this.labelMapping.map(entry.getValue().getFirst()), entry.getValue().getSecond()));
		return map;
	}

	@Override
	public boolean init(DataSet<D, L> testData) {
		return this.method.init(testData);
	}

	@Override
	protected boolean fromParseInternal(AssignmentList internalAssignments) {
		return true;
	}

	@Override
	protected AssignmentList toParseInternal() {
		AssignmentList assignments = new AssignmentList();
		if (this.method != null)
			assignments.add(Assignment.assignmentTyped(null, "classify_method", "method", this.method.toParse(true)));
		return assignments;
	}

	@Override
	public String getGenericName() {
		return "LabelMapping";
	}

	@Override
	public MethodClassification<D, L> clone(String referenceName) {
		MethodClassificationLabelMapping<D, L> clone = new MethodClassificationLabelMapping<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.method = this.method.clone();
		clone.referenceName = referenceName;
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationLabelMapping<D, L>(context);
	}

	@Override
	public boolean hasTrainable() {
		return this.method.hasTrainable();
	}

	@Override
	public Trainable<D, L> getTrainable() {
		return this.method.getTrainable();
	}

	@Override
	public L classify(D datum) {
		return this.labelMapping.map(this.method.classify(datum));
	}

	@Override
	public Pair<L, Double> classifyWithScore(D datum) {
		Pair<L, Double> scoredClass = this.method.classifyWithScore(datum);
		scoredClass.setFirst(this.labelMapping.map(scoredClass.getFirst()));
		return scoredClass;
	}

	@Override
	public Map<D, Double> score(DataSet<D, L> data, L label) {
		return this.method.score(data, this.labelMapping.map(label));
	}

	@Override
	public double score(D datum, L label) {
		return this.method.score(datum, this.labelMapping.map(label));
	}
}
