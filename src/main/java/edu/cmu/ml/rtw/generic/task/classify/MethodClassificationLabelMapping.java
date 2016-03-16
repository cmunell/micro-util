package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.LabelMapping;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.data.feature.DataFeatureMatrix;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;

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
	public Map<D, L> classify(DataFeatureMatrix<D, L> data) {
		Map<D, L> map = this.method.classify(data);
		for (Entry<D, L> entry : map.entrySet())
			entry.setValue(this.labelMapping.map(entry.getValue()));
		return map;
	}

	@Override
	public boolean init(DataFeatureMatrix<D, L> testData) {
		return this.method.init(testData);
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
		return "LabelMapping";
	}

	@Override
	public MethodClassification<D, L> clone() {
		MethodClassificationLabelMapping<D, L> clone = new MethodClassificationLabelMapping<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.method = this.method.clone();
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationLabelMapping<D, L>(context);
	}
}
