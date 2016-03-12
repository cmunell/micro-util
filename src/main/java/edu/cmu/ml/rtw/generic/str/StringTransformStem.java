package edu.cmu.ml.rtw.generic.str;

import edu.cmu.ml.rtw.generic.util.Stemmer;

public class StringTransformStem implements StringTransform {
	public StringTransformStem() {
		
	}
	
	@Override
	public String transform(String str) {
		return Stemmer.stem(str);
	}
	
	@Override
	public String toString() {
		return "Stem";
	}
}
