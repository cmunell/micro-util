package edu.cmu.ml.rtw.generic.str;

public class StringTransformUnderscoreToSpace implements StringTransform {
	public StringTransformUnderscoreToSpace() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.replaceAll("_", " ");
	}
	
	@Override
	public String toString() {
		return "UnderscoreToSpace";
	}
}
