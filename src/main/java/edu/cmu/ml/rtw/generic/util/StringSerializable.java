package edu.cmu.ml.rtw.generic.util;

/**
 * JSONSerializable represents an object that 
 * can be converted to or from a string.
 * 
 * @author Bill McDowell
 *
 */
public interface StringSerializable {
	public String toString();
	public boolean fromString(String str);
}
