package edu.cmu.ml.rtw.generic.str;

import edu.cmu.ml.rtw.generic.util.Stemmer;

public class StringTransformStem implements StringTransform {
	public StringTransformStem() {
		
	}
	
	@Override
	public String transform(String str) {
		String[] parts = str.split("\\s+");
		StringBuilder retStr = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			retStr = retStr.append(Stemmer.stem(parts[i])).append(" ");
		}
		
		return retStr.toString().trim();
	}
	
	@Override
	public String toString() {
		return "Stem";
	}
}
