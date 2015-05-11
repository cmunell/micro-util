/**
 * Copyright 2014 Bill McDowell 
 *
 * This file is part of theMess (https://github.com/forkunited/theMess)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 */

package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TokenSpan represents a contiguous span of tokens in a document.  
 * 
 * @author Bill
 */
public class TokenSpan {
	public static final Relation[] ANY_RELATION = new Relation[] { Relation.CONTAINS, Relation.CONTAINED_BY, Relation.EQUAL, Relation.OVERLAPS };
	
	public enum Relation {
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
			if (includeSentence)
				json.put("sentenceIndex", this.sentenceIndex);
			json.put("startTokenIndex", this.startTokenIndex);
			json.put("endTokenIndex", this.endTokenIndex);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return json;
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
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static TokenSpan fromJSON(JSONObject json, DocumentNLP document) {
		return fromJSON(json, document, -1);
	}
}

