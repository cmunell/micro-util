package edu.cmu.ml.rtw.generic.parse;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

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
