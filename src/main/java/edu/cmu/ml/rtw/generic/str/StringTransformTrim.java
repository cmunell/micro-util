package edu.cmu.ml.rtw.generic.str;

public class StringTransformTrim implements StringTransform {
	public StringTransformTrim() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.trim();
	}
	
	@Override
	public String toString() {
		return "Trim";
	}

}
