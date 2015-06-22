package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * CtxParsable represents an object that is parsable
 * as part of ctx script using the CtxParser.  For example
 * edu.cmu.ml.rtw.generic.data.Context,
 * edu.cmu.ml.rtw.generic.data.feature.Feature,
 * and edu.cmu.ml.rtw.generic.model.SupervisedModel
 * are all objects that can be parsed from or converted
 * back into part of a ctx script.
 *  
 * @author Bill McDowell
 *
 */
public abstract class CtxParsable extends Serializable {
	protected String referenceName;
	protected List<String> modifiers;
	
	public String getReferenceName() {
		return this.referenceName;
	}

	public boolean fromParse(Obj obj) {
		return fromParse(null, null, obj);
	}
	
	public boolean fromParse(List<String> modifiers, String referenceName, Obj obj) {
		this.modifiers = modifiers;
		this.referenceName = referenceName;
		return fromParseHelper(obj);
	}
	
	public boolean serialize(Writer writer) throws IOException {
		return toParse().serialize(writer);
	}
	
	public Map<String, Obj> match(Obj obj) {
		return toParse().match(obj);
	}
	
	public List<String> getModifiers() {
		return this.modifiers;
	}
	
	public abstract Obj toParse();
	protected abstract boolean fromParseHelper(Obj obj);
}
