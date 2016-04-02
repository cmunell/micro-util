package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.SerializationType;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

public class Predicate implements JSONSerializable {
	private DocumentNLP document;
	private String sense;
	private TokenSpan span;
	private Map<String, TokenSpan[]> arguments;
	
	public Predicate(DocumentNLP document) {
		this.document = document;
	}
	
	public Predicate(String sense, TokenSpan span, Map<String, TokenSpan[]> arguments) {
		this.document = span.getDocument();
		this.sense = sense;
		this.span = span;
		this.arguments = arguments;
	}
	
	public String getSense() {
		return this.sense;
	}
	
	public TokenSpan getSpan() {
		return this.span;
	}
	
	public TokenSpan[] getArgument(String tag) {
		return this.arguments.get(tag);
	}
	
	public Set<String> getArgumentTags() {
		return this.arguments.keySet();
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("sense", this.sense);
			json.put("span", this.span.toJSON(SerializationType.SENTENCE));
			
			JSONArray argumentsJson = new JSONArray();
			for (Entry<String, TokenSpan[]> entry : this.arguments.entrySet()) {
				JSONArray argumentSpansJson = new JSONArray();
				for (TokenSpan argumentSpan : entry.getValue()) {
					argumentSpansJson.put(argumentSpan.toJSON(SerializationType.SENTENCE));			
				}
				
				JSONObject argumentJson = new JSONObject();
				argumentJson.put("tag", entry.getKey());
				argumentJson.put("spans", argumentSpansJson);
				
				argumentsJson.put(argumentJson);
			}
			
			json.put("arguments", argumentsJson);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}

	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			this.sense = json.getString("sense");
			this.span = TokenSpan.fromJSON(json.getJSONObject("span"), this.document);
			this.arguments = new HashMap<String, TokenSpan[]>();
			JSONArray argumentsJson = json.getJSONArray("arguments");
			for (int i = 0; i < argumentsJson.length(); i++) {
				JSONObject argumentJson = argumentsJson.getJSONObject(i);
				String tag = argumentJson.getString("tag");
				JSONArray spansJson = argumentJson.getJSONArray("spans");
				TokenSpan[] spans = new TokenSpan[spansJson.length()];
				
				for (int j = 0; j < spansJson.length(); j++) {
					TokenSpan span = TokenSpan.fromJSON(spansJson.getJSONObject(j), this.document);
					spans[j] = span;
				}
				
				this.arguments.put(tag, spans);
			}
			
			return true;
		} catch (JSONException e) {
			return false;
		}
	}
	
	
}
