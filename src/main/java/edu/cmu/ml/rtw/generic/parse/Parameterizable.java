package edu.cmu.ml.rtw.generic.parse;

public interface Parameterizable {
	/**
	 * @return parameters that can be set
	 */
	String[] getParameterNames();
	
	/**
	 * @param parameter
	 * @return the value of the given parameter
	 */
	Obj getParameterValue(String parameter);
	
	/**
	 * 
	 * @param parameter
	 * @param parameterValue
	 * @return true if the parameter has been set to parameterValue.
	 */
	boolean setParameterValue(String parameter, Obj parameterValue);
}
