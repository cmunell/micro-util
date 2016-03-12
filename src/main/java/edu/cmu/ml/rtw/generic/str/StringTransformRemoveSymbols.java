package edu.cmu.ml.rtw.generic.str;

public class StringTransformRemoveSymbols implements StringTransform {
	
	public StringTransformRemoveSymbols() {
		
	}
	
	@Override
	public String transform(String str) {
		return str.replaceAll("[\\W&&[^\\s]]+", " ");
	}
	
	@Override
	public String toString() {
		return "RemoveSymbols";
	}
}
