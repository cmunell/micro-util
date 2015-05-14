package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.annotation.DocumentSet;

/**
 * TokenSpan represents a contiguous span of tokens in a document.  
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
	
	private DocumentNLP document;
	private int sentenceIndex;
	private int startTokenIndex; // 0-based token index (inclusive)
	private int endTokenIndex; // 0-based token index (exclusive)
	
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
	
	public int getStartTokenIndex() {
		return this.startTokenIndex;
	}
	
	public int getEndTokenIndex() {
		return this.endTokenIndex;
	}
	
	public int getLength() {
		return this.endTokenIndex - this.startTokenIndex;
	}
	
	public TokenSpan getSubspan(int startIndex, int endIndex) {
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
		else if (this.startTokenIndex < tokenSpan.startTokenIndex && this.endTokenIndex >= tokenSpan.startTokenIndex)
			return Relation.OVERLAPS;
		else if (this.startTokenIndex < tokenSpan.endTokenIndex && this.endTokenIndex > tokenSpan.endTokenIndex)
			return Relation.OVERLAPS;
		else if (this.sentenceIndex == tokenSpan.sentenceIndex)
			return Relation.SAME_SENTENCE;
		else
			return Relation.NONE;
	}
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		
		for (int i = this.startTokenIndex; i < this.endTokenIndex; i++)
			str.append(getDocument().getTokenStr(this.sentenceIndex, i)).append(" ");
		
		return str.toString().trim();
	}
	
	public JSONObject toJSON() {
		return toJSON(false);
	}
		
	public JSONObject toJSON(boolean includeSentence) {
		JSONObject json = new JSONObject();
		
		try {
			json.put("document", this.document.getName());
			
			if (includeSentence)
				json.put("sentenceIndex", this.sentenceIndex);
			json.put("startTokenIndex", this.startTokenIndex);
			json.put("endTokenIndex", this.endTokenIndex);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentSet<DocumentNLP> documentSet, int sentenceIndex) {
		try {
			DocumentNLP document = documentSet.getDocumentByName(json.getString("document"));
			if (document == null)
				return null;
			
			return new TokenSpan(
				document,
				(sentenceIndex < 0) ? json.getInt("sentenceIndex") : sentenceIndex,
				json.getInt("startTokenIndex"),
				json.getInt("endTokenIndex")
			);
		} catch (JSONException e) {
		
		}
		
		return null;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentSet<DocumentNLP> documentSet) {
		return fromJSON(json, documentSet, -1);
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentNLP document, int sentenceIndex) {
		try {
			return new TokenSpan(
				document,
				(sentenceIndex < 0) ? json.getInt("sentenceIndex") : sentenceIndex,
				json.getInt("startTokenIndex"),
				json.getInt("endTokenIndex")
			);
		} catch (JSONException e) {
		
		}
		
		return null;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentNLP document) {
		return fromJSON(json, document, -1);
	}
}

