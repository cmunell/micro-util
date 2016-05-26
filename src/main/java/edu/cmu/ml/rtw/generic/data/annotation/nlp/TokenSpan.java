package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.StoredItemSetManager;
import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;

/**
 * TokenSpan represents a contiguous span of tokens in a 
 * natural language text document.  
 * 
 * @author Bill
 */
public class TokenSpan {
	public static final Relation[] ANY_SHARING_RELATION = new Relation[] { Relation.CONTAINS, Relation.CONTAINED_BY, Relation.EQUAL, Relation.OVERLAPS };
	public static final Relation[] ANY_CLOSE_RELATION = new Relation[] { Relation.CONTAINS, Relation.CONTAINED_BY, Relation.EQUAL, Relation.OVERLAPS, Relation.SAME_SENTENCE };

	public enum Relation {
		SAME_SENTENCE,
		CONTAINS,
		CONTAINED_BY,
		EQUAL,
		OVERLAPS,
		NONE
	}
	
	public enum SerializationType {
		ONLY_TOKENS,
		SENTENCE,
		SENTENCE_AND_DOCUMENT,
		STORE_REFERENCE;
		
		public static SerializationType getTypeFromJSON(JSONObject json) {
			if (json.has("d") || json.has("document"))
				return SerializationType.SENTENCE_AND_DOCUMENT;
			else if (json.has("sI") || json.has("sentenceIndex"))
				return SerializationType.SENTENCE;
			else if (json.has("r"))
				return SerializationType.STORE_REFERENCE;
			else
				return SerializationType.ONLY_TOKENS;
		}
	}
	
	private DocumentNLP document;
	private int sentenceIndex;
	private int startTokenIndex; // 0-based token index (inclusive)
	private int endTokenIndex; // 0-based token index (exclusive)
	
	/**
	 * 
	 * @param document
	 * @param sentenceIndex
	 * @param startTokenIndex (inclusive)
	 * @param endTokenIndex (exclusive)
	 */
	public TokenSpan(DocumentNLP document, int sentenceIndex, int startTokenIndex, int endTokenIndex) {
		this.document = document;
		this.sentenceIndex = sentenceIndex;
		this.startTokenIndex = startTokenIndex;
		this.endTokenIndex = endTokenIndex;
	}
	
	@Override
	public boolean equals(Object o) {
		TokenSpan t = (TokenSpan)o;
		
		return this.getDocument().getName().equals(t.getDocument().getName()) 
				&& this.sentenceIndex == t.sentenceIndex
				&& this.startTokenIndex == t.startTokenIndex
				&& this.endTokenIndex == t.endTokenIndex;
	}
	
	@Override
	public int hashCode() {
		return this.getDocument().getName().hashCode() 
				^ this.sentenceIndex
				^ this.startTokenIndex
				^ this.endTokenIndex;
	}
	
	public boolean containsToken(int sentenceIndex, int tokenIndex) {
		return this.sentenceIndex == sentenceIndex
				&& this.startTokenIndex <= tokenIndex
				&& this.endTokenIndex > tokenIndex;
	}
	
	public boolean containsTokenSpan(TokenSpan tokenSpan) {
		if (tokenSpan.sentenceIndex != this.sentenceIndex)
			return false;
		for (int i = tokenSpan.getStartTokenIndex(); i < tokenSpan.getEndTokenIndex(); i++)
			if (!containsToken(tokenSpan.sentenceIndex, i))
				return false;
		return true;
	}
	
	public DocumentNLP getDocument() {
		return this.document;
	}
	
	public int getSentenceIndex() {
		return this.sentenceIndex;
	}
	
	/**
	 * @return first token index in the token span (inclusive)
	 */
	public int getStartTokenIndex() {
		return this.startTokenIndex;
	}
	
	/**
	 * 
	 * @return last token index of the token span (exclusive)
	 */
	public int getEndTokenIndex() {
		return this.endTokenIndex;
	}
	
	public int getLength() {
		return this.endTokenIndex - this.startTokenIndex;
	}
	
	public TokenSpan getSubspan(int startIndex, int endIndex) {
		if (this.startTokenIndex + endIndex > this.endTokenIndex)
			return null;
		else
			return new TokenSpan(this.document, this.sentenceIndex, this.startTokenIndex + startIndex, this.startTokenIndex + endIndex);
	}
	
	public Relation getRelationTo(TokenSpan tokenSpan) {
		if (!this.document.getName().equals(tokenSpan.getDocument().getName())
				|| getSentenceIndex() != tokenSpan.getSentenceIndex())
			return Relation.NONE;
		
		if (this.startTokenIndex == tokenSpan.startTokenIndex && this.endTokenIndex == tokenSpan.endTokenIndex)
			return Relation.EQUAL;
		else if (this.startTokenIndex <= tokenSpan.startTokenIndex && this.endTokenIndex >= tokenSpan.endTokenIndex)
			return Relation.CONTAINS;
		else if (this.startTokenIndex >= tokenSpan.startTokenIndex && this.endTokenIndex <= tokenSpan.endTokenIndex)
			return Relation.CONTAINED_BY;
		else if (this.startTokenIndex < tokenSpan.startTokenIndex && this.endTokenIndex > tokenSpan.startTokenIndex)
			return Relation.OVERLAPS;
		else if (this.startTokenIndex < tokenSpan.endTokenIndex && this.endTokenIndex > tokenSpan.endTokenIndex)
			return Relation.OVERLAPS;
		else if (this.sentenceIndex == tokenSpan.sentenceIndex)
			return Relation.SAME_SENTENCE;
		else
			return Relation.NONE;
	}
	
	public boolean hasRelationTo(TokenSpan tokenSpan, Relation[] relations) {
		Relation r = getRelationTo(tokenSpan);
		for (int i = 0; i < relations.length; i++)
			if (relations[i] == r)
				return true;
		return false;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		for (int i = this.startTokenIndex; i < this.endTokenIndex; i++)
			str.append(getDocument().getTokenStr(this.sentenceIndex, i)).append(" ");
		
		return str.toString().trim();
	}
	
	public JSONObject toJSON() {
		return toJSON(SerializationType.ONLY_TOKENS);
	}
		
	public JSONObject toJSON(SerializationType serializationType) {	
		JSONObject json = new JSONObject();
		
		try {
			if (serializationType == SerializationType.SENTENCE_AND_DOCUMENT) {
				json.put("d", this.document.getName());
				json.put("sI", this.sentenceIndex);
			} else if (serializationType == SerializationType.SENTENCE) {
				json.put("sI", this.sentenceIndex);
			} else if (serializationType == SerializationType.STORE_REFERENCE) {
				json.put("r", this.document.getStoreReference().toJSON());
				json.put("sI", this.sentenceIndex);
			}
			
			json.put("s", this.startTokenIndex);
			json.put("e", this.endTokenIndex);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
		return json;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentSet<DocumentNLP, DocumentNLPMutable> documentSet, int sentenceIndex) {
		try {
			DocumentNLP document = documentSet.getDocumentByName(json.has("d") ? json.getString("d") : json.getString("document"));
			if (document == null)
				return null;
			
			return TokenSpan.fromJSON(json, document, sentenceIndex);
		} catch (JSONException e) {
		
		}
		
		return null;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentSet<DocumentNLP, DocumentNLPMutable> documentSet) {
		return fromJSON(json, documentSet, -1);
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentNLP document, int sentenceIndex) {
		try {
			if (sentenceIndex < 0)
				sentenceIndex = json.has("sI") ? json.getInt("sI") : json.getInt("sentenceIndex");
			int startTokenIndex = json.has("s") ? json.getInt("s") : json.getInt("startTokenIndex");
			int endTokenIndex = json.has("e") ? json.getInt("e") : json.getInt("endTokenIndex");
				
			return new TokenSpan(document, sentenceIndex, startTokenIndex, endTokenIndex);
		} catch (JSONException e) { 
			return null;
		}
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentNLP document) {
		return fromJSON(json, document, -1);
	}
	
	public static TokenSpan fromJSON(JSONObject json, StoredItemSetManager itemSets) {
		StoreReference reference = new StoreReference();
		try {
			if (!reference.fromJSON(json.getJSONObject("r"))) {
				return null;
			}
			
			DocumentNLP document = itemSets.resolveStoreReference(reference, true);
			int sentenceIndex = json.has("sI") ? json.getInt("sI") : json.getInt("sentenceIndex");
			int startTokenIndex = json.has("s") ? json.getInt("s") : json.getInt("startTokenIndex");
			int endTokenIndex = json.has("e") ? json.getInt("e") : json.getInt("endTokenIndex");
			
			return new TokenSpan(document, sentenceIndex, startTokenIndex, endTokenIndex);
		} catch (JSONException e) {
			return null;
		}
	}
}

