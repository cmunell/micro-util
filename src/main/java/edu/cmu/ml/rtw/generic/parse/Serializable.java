package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public abstract class Serializable {
	public String toString() {
		StringWriter writer = new StringWriter();
		
		try {
			if (!serialize(writer))
				return null;
		} catch (IOException e) {
			return null;
		}
		
		return writer.toString();
	}
	
	public abstract boolean serialize(Writer writer) throws IOException;
}
