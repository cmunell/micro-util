package edu.cmu.ml.rtw.generic.str;

// FIXME In the future, this should be 
// a CtxParsableFunction
public interface StringTransform {
	String transform(String str);
	// FIXME This should really be replaced with something
	// like "getName", but it's "toString" for stupid
	// historical reasons
	String toString();
}
