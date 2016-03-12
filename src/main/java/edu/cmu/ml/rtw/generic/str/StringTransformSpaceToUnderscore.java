package edu.cmu.ml.rtw.generic.str;

public class StringTransformSpaceToUnderscore implements StringTransform {
	public StringTransformSpaceToUnderscore() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.replaceAll(" ", "_");
	}
	
	@Override
	public String toString() {
		return "SpaceToUnderscore";
	}
}
