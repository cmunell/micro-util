package edu.cmu.ml.rtw.generic.opt.search;

import java.util.Arrays;

import edu.cmu.ml.rtw.generic.data.Context;
import edu.cmu.ml.rtw.generic.parse.Obj;

public class DimensionEnumerated extends Dimension {
	private Obj.Array values = new Obj.Array();
	
	public DimensionEnumerated(Context context) {
		super(context);
		this.parameterNames = Arrays.copyOf(this.parameterNames, this.parameterNames.length + 1);
		this.parameterNames[this.parameterNames.length - 1] = "values";
	}
	
	@Override
	public Obj getParameterValue(String parameter) {
		if (parameter.equals("values"))
			return this.values;
		else
			return super.getParameterValue(parameter);
	}

	@Override
	public boolean setParameterValue(String parameter, Obj parameterValue) {
		if (parameter.equals("values"))
			this.values = (Obj.Array)parameterValue;
		else
			return super.setParameterValue(parameter, parameterValue);
		
		return true;
	}
	
	public Obj.Array getValues() {
		return this.values;
	}

	@Override
	public String getGenericName() {
		return "Enumerated";
	}
	
	@Override 
	public Type getType() {
		return Type.ENUMERATED;
	}
}
