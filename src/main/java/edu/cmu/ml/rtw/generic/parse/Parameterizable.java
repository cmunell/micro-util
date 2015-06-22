package edu.cmu.ml.rtw.generic.parse;

/**
 * Parameterizable represents an object whose
 * behavior can be determined by assigning some
 * values to parameters. This is convenient
 * for configuring objects (particularly 
 * CtxParsableFunctions... see the fromParse method
 * there) that are being constructed
 * through a ctx script.
 * 
 * @author Bill McDowell
 *
 */
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
