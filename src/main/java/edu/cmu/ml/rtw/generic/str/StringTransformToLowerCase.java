package edu.cmu.ml.rtw.generic.str;

public class StringTransformToLowerCase implements StringTransform {
	public StringTransformToLowerCase() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.toLowerCase();
	}
	
	@Override
	public String toString() {
		return "ToLowerCase";
	}
}
