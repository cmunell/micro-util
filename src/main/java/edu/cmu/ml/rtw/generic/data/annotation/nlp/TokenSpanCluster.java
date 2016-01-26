package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import edu.cmu.ml.rtw.generic.util.JSONSerializable;

/**
 * TokenSpanCluster represents a cluster of token spans
 * in a natural language text document---for example
 * to represent co-reference resolution output.
 * 
 * @author Bill McDowell
 *
 */
public class TokenSpanCluster implements JSONSerializable {
	private DocumentNLP document;
	private int id;
	private List<TokenSpan> tokenSpans;
	private TokenSpan representative;
	
	public TokenSpanCluster(DocumentNLP document) {
		this.document = document;
	}
	
	public TokenSpanCluster(int id) {
		this(id, null, new ArrayList<TokenSpan>(1));
	}
	
	public TokenSpanCluster(int id, List<TokenSpan> tokenSpans) {
		this(id, null, tokenSpans);
	}
	
	public TokenSpanCluster(int id, TokenSpan representative, List<TokenSpan> tokenSpans) {
		this(representative != null ? representative.getDocument() 
				: tokenSpans.size() > 0 ? tokenSpans.get(0).getDocument() : null);
		
		this.id = id;
		this.tokenSpans = tokenSpans;
		this.representative = representative;
	}
	
	public TokenSpan getRepresentative() {
		return this.representative;
	}
	
	public List<TokenSpan> getTokenSpans() {
		return this.tokenSpans;
	}
	
	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		
		try {
			json.put("id", this.id);
			
			if (this.representative != null)
				json.put("rep", this.representative.toJSON(true, false));
			
			JSONArray tokenSpansJson = new JSONArray();
			for (TokenSpan tokenSpan : this.tokenSpans)
				tokenSpansJson.put(tokenSpan.toJSON(true, false));
			
			json.put("spans", tokenSpansJson);
		} catch (JSONException e) {
			return null;
		}
		
		return json;
	}

	@Override
	public boolean fromJSON(JSONObject json) {
		try {
			this.id = json.getInt("id");
			
			if (json.has("rep"))
				this.representative = TokenSpan.fromJSON(json.getJSONObject("rep"), this.document);
		
			JSONArray spansJson = json.getJSONArray("spans");
			this.tokenSpans = new ArrayList<TokenSpan>(spansJson.length() * 2);
			for (int i = 0; i < spansJson.length(); i++) {
				this.tokenSpans.add(TokenSpan.fromJSON(spansJson.getJSONObject(i), this.document));
			}
		} catch (JSONException e) {
			return false;
		}

		return true;
	}

}
