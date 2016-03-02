package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

public class SerializerDocumentNLPJSONLegacy extends SerializerDocument<DocumentNLPMutable, JSONObject> {
	public SerializerDocumentNLPJSONLegacy() {
		this(new DocumentNLPInMemory(new DataTools()), null);
	}
	
	public SerializerDocumentNLPJSONLegacy(DataTools dataTools) {
		this(new DocumentNLPInMemory(dataTools), null);
	}
	
	public SerializerDocumentNLPJSONLegacy(DocumentNLPMutable genericDocument) {
		this(genericDocument, null);
	}
	
	public SerializerDocumentNLPJSONLegacy(DocumentNLPMutable genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		super(genericDocument, annotationTypes);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject serialize(DocumentNLPMutable document) {
		JSONObject json = new JSONObject();
		JSONArray sentencesJson = new JSONArray();
		
		try {
			JSONObject annotators = new JSONObject();
			
			Collection<AnnotationType<?>> docAnnotationTypes = document.getAnnotationTypes();
			Collection<AnnotationType<?>> annotationTypes = null;
			if (this.annotationTypes == null) {
				annotationTypes = docAnnotationTypes;
			} else {
				annotationTypes = this.annotationTypes;
			}
			
			if (annotationTypes == null) {
				annotationTypes = docAnnotationTypes;
			}
			
			json.put("name", document.getName());
			
			for (AnnotationType<?> annotationType : docAnnotationTypes) {
				if (!annotationTypes.contains(annotationType))
					continue;
				
				AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
				annotators.put(annotationType.getType(), document.getAnnotatorName(annotationType));
			
				if (annotationTypeNLP.getTarget() == Target.DOCUMENT) {
					json.put(annotationTypeNLP.getType(), annotationTypeNLP.serialize(document.getDocumentAnnotation(annotationTypeNLP)));
				}
			}	
			
			if (annotators.length() > 0)
				json.put("annotators", annotators);
			
			if (!document.hasAnnotationType(AnnotationTypeNLP.TOKEN))
				return json;
				
			int sentenceCount = document.getSentenceCount();
			for (int i = 0; i < sentenceCount; i++) {
				int tokenCount = document.getSentenceTokenCount(i);
				JSONObject sentenceJson = new JSONObject();
				
				JSONArray posTagsJson = new JSONArray();
				if (annotationTypes.contains(AnnotationTypeNLP.TOKEN)) {
					sentenceJson.put("sentence", document.getSentence(i));
					
					JSONArray tokensJson = new JSONArray();
					
					for (int j = 0; j < tokenCount; j++) {
						Token token = document.getToken(i, j);
						if (token.getCharSpanEnd() < 0 || token.getCharSpanStart() < 0)
							tokensJson.put(token.getStr());
						else
							tokensJson.put(token.toJSON());
						
						if (document.hasAnnotationType(AnnotationTypeNLP.POS)) {
							PoSTag posTag = document.getPoSTag(i, j);
							if (posTag != null)
								posTagsJson.put(posTag.toString());	
						}
					}
					
					sentenceJson.put("tokens", tokensJson);
				}
				
				if (annotationTypes.contains(AnnotationTypeNLP.POS) && document.hasAnnotationType(AnnotationTypeNLP.POS))
					sentenceJson.put("posTags", posTagsJson);
				if (annotationTypes.contains(AnnotationTypeNLP.DEPENDENCY_PARSE) && document.hasAnnotationType(AnnotationTypeNLP.DEPENDENCY_PARSE))
					sentenceJson.put("dependencyParse", document.getDependencyParse(i).toString());
				if (annotationTypes.contains(AnnotationTypeNLP.CONSTITUENCY_PARSE) && document.hasAnnotationType(AnnotationTypeNLP.CONSTITUENCY_PARSE) && document.getConstituencyParse(i) != null)
					sentenceJson.put("constituencyParse", document.getConstituencyParse(i).toString());
				
				for (AnnotationType<?> annotationType : docAnnotationTypes) {
					AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
					if (annotationTypeNLP.getTarget() == Target.SENTENCE 
							&& !annotationTypeNLP.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)
							&& !annotationTypeNLP.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)
							&& annotationTypes.contains(annotationType)) {
						sentenceJson.put(annotationTypeNLP.getType(), annotationTypeNLP.serialize(document.getSentenceAnnotation(annotationTypeNLP, i)));
					}
				}
				
				for (AnnotationType<?> annotationType : docAnnotationTypes) {
					AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
					if (annotationTypeNLP.getTarget() == Target.TOKEN
							&& !annotationTypeNLP.equals(AnnotationTypeNLP.TOKEN)
							&& !annotationTypeNLP.equals(AnnotationTypeNLP.POS)
							&& annotationTypes.contains(annotationType)) {	
						JSONArray jsonObjs = new JSONArray();
						
						for (int j = 0; j < document.getSentenceTokenCount(i); j++)
							jsonObjs.put(annotationTypeNLP.serialize(document.getTokenAnnotation(annotationTypeNLP, i, j)));
						
						sentenceJson.put(annotationType.getType() + "s", jsonObjs);
					}
				}
				
				sentencesJson.put(sentenceJson);
			}
			json.put("sentences", sentencesJson);

			for (AnnotationType<?> annotationType : docAnnotationTypes) {
				AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
				if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN && annotationTypes.contains(annotationType)) {
					JSONArray annotationsJson = new JSONArray();
					String spansStr = annotationTypeNLP.getType() + "Spans";
					
					for (int i = 0; i < document.getSentenceCount(); i++) {
						List<?> tokenSpanAnnotations = document.getTokenSpanAnnotations(annotationTypeNLP, i);
						JSONObject sentenceJson = new JSONObject();
						sentenceJson.put("sentence", i);
						JSONArray annotationSpansJson = new JSONArray();
						
						if (tokenSpanAnnotations != null) {
							for (Object tokenSpanAnnotation : tokenSpanAnnotations) {
								JSONObject annotationSpanJson = new JSONObject();
								Pair<TokenSpan, ?> tsAnnotation = (Pair<TokenSpan, ?>)tokenSpanAnnotation;
								
								annotationSpanJson.put("tokenSpan", tsAnnotation.getFirst().toJSON());
								annotationSpanJson.put("type", annotationTypeNLP.serialize(tsAnnotation.getSecond()));
								annotationSpansJson.put(annotationSpanJson);
							}
							
							sentenceJson.put(spansStr, annotationSpansJson);
							annotationsJson.put(sentenceJson);
						}
					}
					
					json.put(annotationTypeNLP.getType(), annotationsJson);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		
		return json;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DocumentNLPMutable deserialize(JSONObject json, StoreReference storeReference) {
		try {
			DocumentNLPMutable document = null;
			if (storeReference != null)
				document = this.genericDocument.makeInstance(storeReference);
			else
				document =this.genericDocument.makeInstance(json.getString("name"));
			
			
			Collection<AnnotationType<?>> annotationTypes = null;
			if (this.annotationTypes == null) {
				annotationTypes = new ArrayList<AnnotationType<?>>();
				annotationTypes.addAll(document.getDataTools().getAnnotationTypesNLP());
			} else {
				annotationTypes = this.annotationTypes;
			}
			
			
			Map<AnnotationTypeNLP<?>, String> annotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			if (json.has("annotators")) {
				JSONObject annotatorsJson = json.getJSONObject("annotators");
				String[] annotatorTypes = JSONObject.getNames(annotatorsJson);
				for (String annotatorType : annotatorTypes) {
					AnnotationTypeNLP<?> annotationType = document.getDataTools().getAnnotationTypeNLP(annotatorType);
					if (annotationTypes.contains(annotationType) && annotatorsJson.has(annotationType.getType()))
						annotatorNames.put(annotationType, annotatorsJson.getString(annotatorType));
				}
			}
			
			
			if (json.has("text") && (annotationTypes.contains(AnnotationTypeNLP.ORIGINAL_TEXT)))
				document.setDocumentAnnotation(annotatorNames.get(AnnotationTypeNLP.ORIGINAL_TEXT), 
																  AnnotationTypeNLP.ORIGINAL_TEXT, 
																  new Pair<String, Double>(json.getString("text"), null));
			
			if (json.has("language") && (annotationTypes.contains(AnnotationTypeNLP.LANGUAGE)))
				document.setDocumentAnnotation(annotatorNames.get(AnnotationTypeNLP.LANGUAGE), 
																  AnnotationTypeNLP.LANGUAGE, 
																  new Pair<Language, Double>(Language.valueOf(json.getString("language")), null));
			
			for (AnnotationTypeNLP<?> annotationType : document.getDataTools().getAnnotationTypesNLP()) {
				if (annotationType.getTarget() != AnnotationTypeNLP.Target.DOCUMENT
						|| !json.has(annotationType.getType()) 
						|| annotationType.equals(AnnotationTypeNLP.LANGUAGE)
						|| annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT)
						|| (annotationTypes != null && !annotationTypes.contains(annotationType)))
					continue;
				
				document.setDocumentAnnotation(annotatorNames.get(annotationType), 
						  					   annotationType, 
						  					  new Pair<Object, Double>(annotationType.deserialize(document, json.get(annotationType.getType())), null));
			}
			
			
			if (!json.has("sentences"))
				return document;
			
			JSONArray sentences = json.getJSONArray("sentences");
			Pair<Token, Double>[][] tokens = new Pair[sentences.length()][];
			Pair<PoSTag, Double>[][] posTags = new Pair[sentences.length()][];
			Map<Integer, Pair<DependencyParse, Double>> dependencyParses = new HashMap<Integer, Pair<DependencyParse, Double>>();
			Map<Integer, Pair<ConstituencyParse, Double>> constituencyParses = new HashMap<Integer, Pair<ConstituencyParse, Double>>();	
			Map<AnnotationTypeNLP<?>, Pair<?, Double>[][]> otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
			Map<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> otherSentenceAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>>();
			
			int characterOffset = 0;
			for (int i = 0; i < sentences.length(); i++) {
				JSONObject sentenceJson = sentences.getJSONObject(i);
				JSONArray tokensJson = sentenceJson.getJSONArray("tokens");
				JSONArray posTagsJson = (sentenceJson.has("posTags")) ? sentenceJson.getJSONArray("posTags") : null;
				
				tokens[i] = new Pair[tokensJson.length()];
				for (int j = 0; j < tokensJson.length(); j++) {
					JSONObject tokenJson = tokensJson.optJSONObject(j);
					if (tokenJson == null) {
						String tokenStr = tokensJson.getString(j);
						tokens[i][j] = new Pair<Token, Double>(new Token(document, tokenStr, characterOffset, characterOffset + tokenStr.length()), null);
						characterOffset += tokenStr.length() + 1;
					} else {
						Token token = new Token(document);
						if (!token.fromJSON(tokenJson))
							return null;
						tokens[i][j] = new Pair<Token, Double>(token, null);
					}
				}
				
				if (posTagsJson != null) {
					posTags[i] = new Pair[posTagsJson.length()];
					for (int j = 0; j < posTagsJson.length(); j++)
						posTags[i][j] = new Pair<PoSTag, Double>(PoSTag.valueOf(posTagsJson.getString(j)), null);
				}
				
				if (sentenceJson.has("dependencyParse"))
					dependencyParses.put(i, new Pair<DependencyParse, Double>(DependencyParse.fromString(sentenceJson.getString("dependencyParse"), document, i), null));
				if (sentenceJson.has("constituencyParse"))
					constituencyParses.put(i, new Pair<ConstituencyParse, Double>(ConstituencyParse.fromString(sentenceJson.getString("constituencyParse"), document, i), null));
			
				for (AnnotationTypeNLP<?> annotationType : document.getDataTools().getAnnotationTypesNLP()) {
					if (annotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE 
							&& sentenceJson.has(annotationType.getType())
							&& !annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)
							&& !annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
					
						if (!otherSentenceAnnotations.containsKey(annotationType))
							otherSentenceAnnotations.put(annotationType, new HashMap<Integer, Pair<?, Double>>());
						Map<Integer, Pair<?, Double>> sentenceMap = otherSentenceAnnotations.get(annotationType);
						sentenceMap.put(i, new Pair<Object, Double>(annotationType.deserialize(document, i, sentenceJson.get(annotationType.getType())), null));
					} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN 
							&& sentenceJson.has(annotationType.getType() + "s")
							&& !annotationType.equals(AnnotationTypeNLP.TOKEN)
							&& !annotationType.equals(AnnotationTypeNLP.POS)) {
						
						if (!otherTokenAnnotations.containsKey(annotationType))
							otherTokenAnnotations.put(annotationType, (Pair<Object, Double>[][])new Pair[sentences.length()][]);
						
						JSONArray jsonAnnotations = sentenceJson.getJSONArray(annotationType.getType() + "s");
						Pair<Object, Double>[] sentenceAnnotations = (Pair<Object, Double>[])new Pair[jsonAnnotations.length()];
						for (int j = 0; j < jsonAnnotations.length(); j++)
							sentenceAnnotations[j] = new Pair<Object, Double>(annotationType.deserialize(document, i, jsonAnnotations.get(j)), null);
						otherTokenAnnotations.get(annotationType)[i] = sentenceAnnotations;
					}
				}
			}
		
			if (annotationTypes.contains(AnnotationTypeNLP.TOKEN)  && (tokens.length == 0 || tokens[0] != null))
				document.setTokenAnnotation(annotatorNames.get(AnnotationTypeNLP.TOKEN), AnnotationTypeNLP.TOKEN, tokens);
			if (annotationTypes.contains(AnnotationTypeNLP.POS) && posTags != null && (posTags.length == 0 || posTags[0] != null))
				document.setTokenAnnotation(annotatorNames.get(AnnotationTypeNLP.POS), AnnotationTypeNLP.POS, posTags);
			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : otherTokenAnnotations.entrySet()) {
				if (annotationTypes.contains(entry.getKey()) && (entry.getValue().length == 0 || entry.getValue() != null))
					document.setTokenAnnotation(annotatorNames.get(entry.getKey()), entry.getKey(), entry.getValue());
			}
			
			if (annotationTypes.contains(AnnotationTypeNLP.DEPENDENCY_PARSE) && dependencyParses != null)
				document.setSentenceAnnotation(annotatorNames.get(AnnotationTypeNLP.DEPENDENCY_PARSE), AnnotationTypeNLP.DEPENDENCY_PARSE, dependencyParses);
			if (annotationTypes.contains(AnnotationTypeNLP.CONSTITUENCY_PARSE) && constituencyParses != null)
				document.setSentenceAnnotation(annotatorNames.get(AnnotationTypeNLP.CONSTITUENCY_PARSE), AnnotationTypeNLP.CONSTITUENCY_PARSE, constituencyParses);
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> entry : otherSentenceAnnotations.entrySet()) {
				if (annotationTypes.contains(entry.getKey()) && entry.getValue() != null)
					document.setSentenceAnnotation(annotatorNames.get(entry.getKey()), entry.getKey(), entry.getValue());
			}
			
			
			if (json.has(AnnotationTypeNLP.NER.getType()) && (annotationTypes.contains(AnnotationTypeNLP.NER))) {
				List<Triple<TokenSpan, ?, Double>> ner = new ArrayList<Triple<TokenSpan, ?, Double>>();
	
				JSONArray nerJson = json.getJSONArray(AnnotationTypeNLP.NER.getType());
				for (int i = 0; i < nerJson.length(); i++) {
					JSONObject sentenceNerJson = nerJson.getJSONObject(i);  
					JSONArray nerSpansJson = sentenceNerJson.getJSONArray("nerSpans");
					int sentenceIndex = sentenceNerJson.getInt("sentence");
					List<Triple<TokenSpan, String, Double>> nerSpans = new ArrayList<Triple<TokenSpan, String, Double>>();
					for (int j = 0; j < nerSpansJson.length(); j++)
						nerSpans.add(new Triple<TokenSpan, String, Double>(TokenSpan.fromJSON(nerSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), document, sentenceIndex),
																AnnotationTypeNLP.NER.deserialize(document, sentenceIndex, nerSpansJson.getJSONObject(j).get("type")), null));
					
					ner.addAll(nerSpans);
				}
				
				document.setTokenSpanAnnotation(annotatorNames.get(AnnotationTypeNLP.NER), AnnotationTypeNLP.NER, ner);
			}
			
			if (json.has(AnnotationTypeNLP.COREF.getType()) && (annotationTypes.contains(AnnotationTypeNLP.COREF))) {
				List<Triple<TokenSpan, ?, Double>> coref = new ArrayList<Triple<TokenSpan, ?, Double>>();
	
				JSONArray corefJson = json.getJSONArray(AnnotationTypeNLP.COREF.getType());
				for (int i = 0; i < corefJson.length(); i++) {
					JSONObject sentenceCorefJson = corefJson.getJSONObject(i);  
					JSONArray corefSpansJson = sentenceCorefJson.getJSONArray("corefSpans");
					int sentenceIndex = sentenceCorefJson.getInt("sentence");
					List<Triple<TokenSpan, TokenSpanCluster, Double>> corefSpans = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
					for (int j = 0; j < corefSpansJson.length(); j++)
						corefSpans.add(new Triple<TokenSpan, TokenSpanCluster, Double>(TokenSpan.fromJSON(corefSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), document, sentenceIndex),
																AnnotationTypeNLP.COREF.deserialize(document, sentenceIndex, corefSpansJson.getJSONObject(j).get("type")), null));
					
					coref.addAll(corefSpans);
				}
				
				document.setTokenSpanAnnotation(annotatorNames.get(AnnotationTypeNLP.COREF), AnnotationTypeNLP.COREF, coref);
			}

			for (AnnotationTypeNLP<?> annotationType : document.getDataTools().getAnnotationTypesNLP()) {
				if (annotationType.getTarget() != AnnotationTypeNLP.Target.TOKEN_SPAN
						|| !json.has(annotationType.getType()) 
						|| annotationType.equals(AnnotationTypeNLP.NER)
						|| annotationType.equals(AnnotationTypeNLP.COREF)
						|| (!annotationTypes.contains(annotationType)))
					continue;
				
				List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotations = new ArrayList<Triple<TokenSpan, ?, Double>>();
				
				JSONArray annotationJson = json.getJSONArray(annotationType.getType());
				for (int i = 0; i < annotationJson.length(); i++) {
					JSONObject sentenceAnnotationJson = annotationJson.getJSONObject(i);  
					String spansStr = annotationType.getType() + "Spans";
					if (!sentenceAnnotationJson.has("sentence") || !sentenceAnnotationJson.has(spansStr)) {
						// This case is here for backward compatability for annotations that aren't
						// stored by sentence
						JSONObject pairJson = annotationJson.getJSONObject(i);
						TokenSpan tokenSpan = TokenSpan.fromJSON(pairJson.getJSONObject("tokenSpan"), document);
						Object annotationObj =  annotationType.deserialize(document, tokenSpan.getSentenceIndex(), pairJson.getJSONObject("annotation"));
						tokenSpanAnnotations.add(new Triple<TokenSpan, Object, Double>(tokenSpan, annotationObj, null));
					} else {
						JSONArray annotationSpansJson = sentenceAnnotationJson.getJSONArray(spansStr);
						int sentenceIndex = sentenceAnnotationJson.getInt("sentence");
						List<Triple<TokenSpan, ?, Double>> annotationSpans = new ArrayList<Triple<TokenSpan, ?, Double>>();
						for (int j = 0; j < annotationSpansJson.length(); j++)
							annotationSpans.add(new Triple<TokenSpan, Object, Double>(TokenSpan.fromJSON(annotationSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), document, sentenceIndex),
																	annotationType.deserialize(document, sentenceIndex, annotationSpansJson.getJSONObject(j).get("type")), null));
						
						tokenSpanAnnotations.addAll(annotationSpans);
					}
				}	
				
				document.setTokenSpanAnnotation(annotatorNames.get(annotationType), annotationType, tokenSpanAnnotations);
			}
			

			return document;
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getName() {
		return "DocumentNLPJSONLegacy";
	}

	@Override
	public String serializeToString(DocumentNLPMutable item) {
		return serialize(item).toString();
	}

	@Override
	public DocumentNLPMutable deserializeFromString(String str, StoreReference storeReference) {
		try {
			return deserialize(new JSONObject(str), storeReference);
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public SerializerDocument<DocumentNLPMutable, JSONObject> makeInstance(
			DocumentNLPMutable genericDocument,
			Collection<AnnotationType<?>> annotationTypes) {
		return new SerializerDocumentNLPJSONLegacy(genericDocument, annotationTypes);
	}

}
