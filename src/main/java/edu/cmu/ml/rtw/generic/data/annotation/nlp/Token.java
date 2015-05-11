package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.Document;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public class Token implements JSONSerializable {
	private Document document;
	private int charSpanStart;
	private int charSpanEnd;
	private String str;
	
	public Token(Document document) {
		this(document, "", -1, -1);
	}
	
	public Token(Document document, String str) {
		this(document, str, -1, -1);
	}
	
	public Token(Document document, String str, int charSpanStart, int charSpanEnd) {
		this.document = document;
		this.str = str;
		this.charSpanStart = charSpanStart;
		this.charSpanEnd = charSpanEnd;
	}
	
	public Document getDocument() {
		return this.document;
	}
	
	public int getCharSpanStart() {
		return this.charSpanStart;
	}
	
	public int getCharSpanEnd() {
		return this.charSpanEnd;
	}
	
	public String getStr() {
		return this.str;
	}
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("str", this.str);
			json.put("s", this.charSpanStart);
			json.put("e", this.charSpanEnd);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}
	
	public boolean fromJSON(JSONObject json) {
		try {
			this.str = json.getString("str");
			this.charSpanStart = json.has("s") ? json.getInt("s") : -1;
			this.charSpanEnd = json.has("e") ? json.getInt("e") : -1;
			
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
}
