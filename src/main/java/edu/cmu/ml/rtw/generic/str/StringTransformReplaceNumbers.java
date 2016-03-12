package edu.cmu.ml.rtw.generic.str;

public class StringTransformReplaceNumbers implements StringTransform {
	public StringTransformReplaceNumbers() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.replaceAll("\\d+", "[D]");
	}
	
	@Override
	public String toString() {
		return "ReplaceNumbers";
	}
}
