package edu.cmu.ml.rtw.generic.task.classify;

import java.util.Arrays;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.Datum.Tools.DatumIndicator;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationFilterDatumIndicator<D extends Datum<L>, L> extends MethodClassification<D, L> {
	private DatumIndicator<D> datumIndicator;
	private MethodClassification<D, L> method;
	private String[] parameterNames = { "datumIndicator", "method" };
	
	public MethodClassificationFilterDatumIndicator() {
		this(null);
	}
	
	public MethodClassificationFilterDatumIndicator(DatumContext<D, L> context) {
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
		if (parameter.equals("datumIndicator"))
			return (this.datumIndicator == null) ? null : Obj.stringValue(this.datumIndicator.toString());
		else if (parameter.equals("method"))
			return (this.method == null) ? null :  Obj.curlyBracedValue(this.method.getReferenceName());
		else if (this.method != null)
			return this.method.getParameterValue(parameter);
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("datumIndicator")) {
			this.datumIndicator = (parameterValue == null) ? null : this.context.getDatumTools().getDatumIndicator(this.context.getMatchValue(parameterValue));
		} else if (parameter.equals("method"))
			this.method = (parameterValue == null) ? null : this.context.getMatchClassifyMethod(parameterValue);
		else if (this.method != null)
			return this.method.setParameterValue(parameter, parameterValue);
		else
			return false;
		
		return true;
	}

	@Override
	public Map<D, L> classify(DataSet<D, L> data) {
		DataSet<D, L> filteredData = data.filter(this.datumIndicator, this.context.getMaxThreads());
		return this.method.classify(filteredData);
	}
	
	@Override
	public Map<D, Pair<L, Double>> classifyWithScore(DataSet<D, L> data) {
		DataSet<D, L> filteredData = data.filter(this.datumIndicator, this.context.getMaxThreads());
		return this.method.classifyWithScore(filteredData);
	}

	@Override
	public boolean init(DataSet<D, L> testData) {
		DataSet<D, L> filteredData = testData.filter(this.datumIndicator, this.context.getMaxThreads());
		return this.method.init(filteredData);
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
		return "FilterDatumIndicator";
	}

	@Override
	public MethodClassification<D, L> clone() {
		MethodClassificationFilterDatumIndicator<D, L> clone = new MethodClassificationFilterDatumIndicator<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), this.getReferenceName(), toParse()))
			return null;
		clone.method = this.method.clone();
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationFilterDatumIndicator<D, L>(context);
	}
}
