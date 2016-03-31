package edu.cmu.ml.rtw.generic.task.classify;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.DataSet;
import edu.cmu.ml.rtw.generic.data.annotation.Datum;
import edu.cmu.ml.rtw.generic.data.annotation.DatumContext;
import edu.cmu.ml.rtw.generic.parse.AssignmentList;
import edu.cmu.ml.rtw.generic.parse.Obj;
import edu.cmu.ml.rtw.generic.util.Pair;

public class MethodClassificationConstant<D extends Datum<L>, L> extends MethodClassification<D, L> {
	private L label;
	private String[] parameterNames = { "label" };
	
	public MethodClassificationConstant() {
		this(null);
	}
	
	public MethodClassificationConstant(DatumContext<D, L> context) {
		super(context);
	}

	@Override
	public String[] getParameterNames() {
		return this.parameterNames;
	}

	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("label"))
			return Obj.stringValue(this.label.toString());
		else 
			return null;
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("label"))
			this.label = (parameterValue == null) ? null : this.context.getDatumTools().labelFromString(this.context.getMatchValue(parameterValue));
		else
			return false;
		
		return true;
	}

	@Override
	public Map<D, L> classify(DataSet<D, L> data) {
		Map<D, L> map = new HashMap<D, L>();
		
		for (D datum : data) {
			map.put(datum, this.label);
		}
		
		return map;
	}
	
	@Override
	public Map<D, Pair<L, Double>> classifyWithScore(DataSet<D, L> data) {
		Map<D, Pair<L, Double>> map = new HashMap<D, Pair<L, Double>>();
		
		for (D datum : data) {
			map.put(datum, new Pair<L, Double>(this.label, 1.0));
		}
		
		return map;
	}

	@Override
	public boolean init(DataSet<D, L> testData) {
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
		return "Constant";
	}

	@Override
	public MethodClassification<D, L> clone(String referenceName) {
		MethodClassificationFilterDatumIndicator<D, L> clone = new MethodClassificationFilterDatumIndicator<D, L>(this.context);
		if (!clone.fromParse(this.getModifiers(), referenceName, toParse()))
			return null;
		return clone;
	}

	@Override
	public MethodClassification<D, L> makeInstance(DatumContext<D, L> context) {
		return new MethodClassificationConstant<D, L>(context);
	}
}
