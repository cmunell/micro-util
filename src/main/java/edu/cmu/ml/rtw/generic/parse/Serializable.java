package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Serializable represents an object that
 * can be serialized.
 * 
 * FIXME: This class is somewhat unnecessary.  It's here
 * mainly for historical reasons... so maybe just
 * get rid of it soon.
 * 
 * @author Bill McDowell
 *
 */
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
