package edu.cmu.ml.rtw.generic.str;

public class StringTransformRemoveLongTokens implements StringTransform {

	public StringTransformRemoveLongTokens() {
		
	}
	
	@Override
	public String transform(String str) {
		String[] parts = str.split("\\s+");
		StringBuilder retStr = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() > 30) // remove long tokens
				continue;
			retStr = retStr.append(parts[i]).append(" ");
		}
		
		return retStr.toString().trim();
	}
	
	@Override
	public String toString() {
		return "RemoveLongTokens";
	}
}
