package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.Document;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.Relation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

/**
 * DocumentInMemory represents a text document with various 
 * NLP annotations (e.g. PoS tags, parses, etc) kept in 
 * memory.  
 * 
 * @author Bill McDowell
 * 
 */
public class DocumentNLPInMemory extends DocumentNLP {
	private static final AnnotationTypeNLP<?>[] FIRST_CLASS_ANNOTATIONS = new AnnotationTypeNLP<?>[] {
		AnnotationTypeNLP.ORIGINAL_TEXT,
		AnnotationTypeNLP.LANGUAGE,
		AnnotationTypeNLP.TOKEN,
		AnnotationTypeNLP.SENTENCE,
		AnnotationTypeNLP.POS,
		AnnotationTypeNLP.DEPENDENCY_PARSE,
		AnnotationTypeNLP.CONSTITUENCY_PARSE,
		AnnotationTypeNLP.NER,
		AnnotationTypeNLP.COREF
	};
	
	protected String languageAnnotatorName;
	protected String originalTextAnnotatorName;
	protected String tokenAnnotatorName;
	protected String posAnnotatorName;
	protected String dependencyParseAnnotatorName;
	protected String constituencyParseAnnotatorName;
	protected String nerAnnotatorName;
	protected String corefAnnotatorName;
	protected Map<AnnotationTypeNLP<?>, String> otherAnnotatorNames;
	
	protected Double languageConf;
	protected Double originalTextConf;
	protected double[][] tokensConf;
	protected double[][] posTagsConf;
	protected double[] dependencyParsesConf; 
	protected double[] constituencyParsesConf;
	
	protected Language language;
	protected String originalText;
	protected Token[][] tokens;
	protected PoSTag[][] posTags;
	protected DependencyParse[] dependencyParses; 
	protected ConstituencyParse[] constituencyParses;
	protected Map<Integer, List<Triple<TokenSpan, String, Double>>> ner;
	protected Map<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>> coref;
	
	protected Map<AnnotationTypeNLP<?>, Pair<?, Double>> otherDocumentAnnotations;
        protected Map<AnnotationTypeNLP<?>, Map<Integer, ?>> otherSentenceAnnotations;  // Should actually be  Map<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> but this fails to type check for some reason
	protected Map<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>> otherTokenSpanAnnotations;
	protected Map<AnnotationTypeNLP<?>, Pair<?, Double>[][]> otherTokenAnnotations;
	
	public DocumentNLPInMemory(DataTools dataTools) {
		super(dataTools);
	}

	public DocumentNLPInMemory(DataTools dataTools, JSONObject json) {
		super(dataTools, json);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, DocumentAnnotation documentAnnotation) {
		super(dataTools, documentAnnotation);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String jsonPath) {
		super(dataTools, jsonPath);
	}

	public DocumentNLPInMemory(DataTools dataTools, DocumentAnnotation documentAnnotation, PipelineNLP pipeline) {
		this(dataTools, documentAnnotation, pipeline, new ArrayList<AnnotationTypeNLP<?>>());
	}
	
	public DocumentNLPInMemory(DataTools dataTools, DocumentAnnotation documentAnnotation, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators) {
		this(dataTools, documentAnnotation);
	
		if (pipeline == null)
			return;
		
		runThroughPipeline(pipeline, skipAnnotators);
	}

	public DocumentNLPInMemory(DataTools dataTools, String name, String text, Language language, PipelineNLP pipeline) {
		this(dataTools, name, text, language, pipeline, new ArrayList<AnnotationTypeNLP<?>>(), false);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String name, String text, Language language, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators, boolean keepOriginalText) {
		super(dataTools);
		
		this.name = name;
		this.language = language;
		this.languageConf = 1.0;
		this.languageAnnotatorName = "cmunell_0.0.1";
		
		this.originalText = text;
		
		if (pipeline == null)
			return;
		
		runThroughPipeline(pipeline, skipAnnotators);
		
		if (!keepOriginalText)
			this.originalText = null;
	}
	
	@SuppressWarnings("unchecked")
	private void runThroughPipeline(PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators) {
		if (!pipeline.setDocument(this))
			throw new IllegalArgumentException();

		for (int annotatorIndex = 0; annotatorIndex < pipeline.getAnnotatorCount(); annotatorIndex++) {
			AnnotationTypeNLP<?> annotationType = (AnnotationTypeNLP<?>)pipeline.getAnnotationType(annotatorIndex);
			if (skipAnnotators != null && skipAnnotators.contains(annotationType)) {
				continue;
			}

			if (!pipeline.meetsAnnotatorRequirements(annotationType, this)) {
				throw new UnsupportedOperationException("Document does not meet annotation type requirements for " + annotationType.getType() + " annotator.");
			}
			
			if (annotationType.equals(AnnotationTypeNLP.TOKEN)) {
				this.tokenAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.TOKEN);
				Pair<Token, Double>[][] tokens = pipeline.annotateTokens(AnnotationTypeNLP.TOKEN);
				boolean tokenConf = pipeline.annotatorMeasuresConfidence(AnnotationTypeNLP.TOKEN);
				if (tokenConf) {
					this.tokensConf = new double[tokens.length][];
				}
				
				this.tokens = new Token[tokens.length][];
				for (int i = 0; i < tokens.length; i++) {
					this.tokens[i] = new Token[tokens[i].length];
					if (tokenConf)
						this.tokensConf[i] = new double[tokens[i].length];
					for (int j = 0; j < tokens[i].length; j++) {
						this.tokens[i][j] = tokens[i][j].getFirst();
						if (tokenConf)
							this.tokensConf[i][j] = tokens[i][j].getSecond();
					}
				}
			} else if (annotationType.equals(AnnotationTypeNLP.POS)) {				
				this.posAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.POS);
				Pair<PoSTag, Double>[][] pos = pipeline.annotateTokens(AnnotationTypeNLP.POS);
				boolean posConf = pipeline.annotatorMeasuresConfidence(AnnotationTypeNLP.POS);
				if (posConf) {
					this.posTagsConf = new double[pos.length][];
				}
				
				this.posTags = new PoSTag[pos.length][];
				for (int i = 0; i < pos.length; i++) {
					this.posTags[i] = new PoSTag[pos[i].length];
					if (posConf)
						this.posTagsConf[i] = new double[pos[i].length];
					for (int j = 0; j < pos[i].length; j++) {
						this.posTags[i][j] = pos[i][j].getFirst();
						if (posConf)
							this.posTagsConf[i][j] = pos[i][j].getSecond();
					}
				}
			} else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {			
				this.constituencyParseAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.CONSTITUENCY_PARSE);
				Map<Integer, Pair<ConstituencyParse, Double>> parses = pipeline.annotateSentences(AnnotationTypeNLP.CONSTITUENCY_PARSE);
				this.constituencyParses = new ConstituencyParse[this.tokens.length];
				
				boolean consConf = pipeline.annotatorMeasuresConfidence(AnnotationTypeNLP.CONSTITUENCY_PARSE);
				if (consConf)
					this.constituencyParsesConf = new double[this.tokens.length];
				
				for (Entry<Integer, Pair<ConstituencyParse, Double>> parse : parses.entrySet()) {
					this.constituencyParses[parse.getKey()] = parse.getValue().getFirst();
					if (consConf)
						this.constituencyParsesConf[parse.getKey()] = parse.getValue().getSecond();
				}
			} else if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
				this.dependencyParseAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.DEPENDENCY_PARSE);
				Map<Integer, Pair<DependencyParse, Double>> parses = pipeline.annotateSentences(AnnotationTypeNLP.DEPENDENCY_PARSE);
				
				boolean depConf = pipeline.annotatorMeasuresConfidence(AnnotationTypeNLP.DEPENDENCY_PARSE);
				if (depConf)
					this.dependencyParsesConf = new double[this.tokens.length];
				
				this.dependencyParses = new DependencyParse[this.tokens.length];
				for (Entry<Integer, Pair<DependencyParse, Double>> parse : parses.entrySet()) {
					this.dependencyParses[parse.getKey()] = parse.getValue().getFirst();
					if (depConf) 
						this.dependencyParsesConf[parse.getKey()] = parse.getValue().getSecond();
				}
			} else if (annotationType.equals(AnnotationTypeNLP.NER)) {
				this.nerAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.NER);
				List<Triple<TokenSpan, String, Double>> ner = pipeline.annotateTokenSpans(AnnotationTypeNLP.NER);
				this.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
				for (Triple<TokenSpan, String, Double> nerSpan : ner) {
					if (!this.ner.containsKey(nerSpan.getFirst().getSentenceIndex()))
						this.ner.put(nerSpan.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, String, Double>>());
					this.ner.get(nerSpan.getFirst().getSentenceIndex()).add(nerSpan);
				}
			} else if (annotationType.equals(AnnotationTypeNLP.COREF)) {
				this.corefAnnotatorName = pipeline.getAnnotatorName(AnnotationTypeNLP.COREF);
				List<Triple<TokenSpan, TokenSpanCluster, Double>> coref = pipeline.annotateTokenSpans(AnnotationTypeNLP.COREF);
				this.coref = new HashMap<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>>();
				for (Triple<TokenSpan, TokenSpanCluster, Double> corefSpan : coref) {
					if (!this.coref.containsKey(corefSpan.getFirst().getSentenceIndex()))
						this.coref.put(corefSpan.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>());
					this.coref.get(corefSpan.getFirst().getSentenceIndex()).add(corefSpan);
				}
			} else {
				if (this.otherAnnotatorNames == null)
					this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
				this.otherAnnotatorNames.put(annotationType, pipeline.getAnnotatorName(annotationType));
				
				if (annotationType.getTarget() == AnnotationTypeNLP.Target.DOCUMENT) {
					if (this.otherDocumentAnnotations == null)
						this.otherDocumentAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>>();
					Pair<?, Double> annotation = pipeline.annotateDocument(annotationType);
					this.otherDocumentAnnotations.put(annotationType, new Pair<Object, Double>(annotation.getFirst(), annotation.getSecond()));
				} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE) {
					if (this.otherSentenceAnnotations == null)
						this.otherSentenceAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, ?>>();
					this.otherSentenceAnnotations.put(annotationType, pipeline.annotateSentences(annotationType));
				} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN_SPAN) {
					if (this.otherTokenSpanAnnotations == null)
						this.otherTokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>>();
					List<?> annotations = pipeline.annotateTokenSpans(annotationType);
					 
					Map<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceMap = new HashMap<Integer, List<Triple<TokenSpan, ?, Double>>>();
					for (Object annotation : annotations) {
						Triple<TokenSpan, ?, Double> triple = (Triple<TokenSpan, ?, Double>)annotation;
						if (!sentenceMap.containsKey(triple.getFirst().getSentenceIndex()))
							sentenceMap.put(triple.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, ?, Double>>());
						sentenceMap.get(triple.getFirst().getSentenceIndex()).add(triple);
					}
					
					this.otherTokenSpanAnnotations.put(annotationType, sentenceMap);
				} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN) {
					if (this.otherTokenAnnotations == null)
						this.otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
					this.otherTokenAnnotations.put(annotationType, pipeline.annotateTokens(annotationType));
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject toJSON() {
		// FIXME Add confidences...
		
		JSONObject json = new JSONObject();
		JSONArray sentencesJson = new JSONArray();
		
		try {
			JSONObject annotators = new JSONObject();
			annotators.put(AnnotationTypeNLP.ORIGINAL_TEXT.getType(), this.originalTextAnnotatorName);
			annotators.put(AnnotationTypeNLP.LANGUAGE.getType(), this.languageAnnotatorName);
			annotators.put(AnnotationTypeNLP.TOKEN.getType(), this.tokenAnnotatorName);
			annotators.put(AnnotationTypeNLP.POS.getType(), this.posAnnotatorName);
			annotators.put(AnnotationTypeNLP.CONSTITUENCY_PARSE.getType(), this.constituencyParseAnnotatorName);
			annotators.put(AnnotationTypeNLP.DEPENDENCY_PARSE.getType(), this.dependencyParseAnnotatorName);
			annotators.put(AnnotationTypeNLP.NER.getType(), this.nerAnnotatorName);
			annotators.put(AnnotationTypeNLP.COREF.getType(), this.corefAnnotatorName);
			
			if (this.otherAnnotatorNames != null) {
				for (Entry<AnnotationTypeNLP<?>, String> entry : this.otherAnnotatorNames.entrySet()) {
					annotators.put(entry.getKey().getType(), entry.getValue());
				}
			}
			
			if (annotators.length() > 0)
				json.put("annotators", annotators);
			
			json.put("name", this.name);
			json.put("text", this.originalText);
			
			if (this.language != null)
				json.put("language", this.language.toString());
			
			if (this.otherDocumentAnnotations != null) {
				for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>> entry : this.otherDocumentAnnotations.entrySet()) 
					json.put(entry.getKey().getType(), entry.getKey().serialize(entry.getValue().getFirst()));
			}
			
			if (this.tokens == null)
				return json;
				
			int sentenceCount = getSentenceCount();
			for (int i = 0; i < sentenceCount; i++) {
				int tokenCount = getSentenceTokenCount(i);
				JSONObject sentenceJson = new JSONObject();
				sentenceJson.put("sentence", getSentence(i));
				
				JSONArray tokensJson = new JSONArray();
				JSONArray posTagsJson = new JSONArray();
				
				for (int j = 0; j < tokenCount; j++) {
					Token token = this.tokens[i][j];
					if (token.getCharSpanEnd() < 0 || token.getCharSpanStart() < 0)
						tokensJson.put(getToken(i, j).getStr());
					else
						tokensJson.put(token.toJSON());
					
					if (this.posTags != null) {
						PoSTag posTag = getPoSTag(i, j);
						if (posTag != null)
							posTagsJson.put(posTag.toString());	
					}
				}
				
				sentenceJson.put("tokens", tokensJson);
				if (this.posTags != null)
					sentenceJson.put("posTags", posTagsJson);
				if (this.dependencyParses != null)
					sentenceJson.put("dependencyParse", getDependencyParse(i).toString());
				if (this.constituencyParses != null && getConstituencyParse(i) != null)
					sentenceJson.put("constituencyParse", getConstituencyParse(i).toString());
				
				if (this.otherSentenceAnnotations != null) {
					for (Entry<AnnotationTypeNLP<?>, Map<Integer, ?>> entry : this.otherSentenceAnnotations.entrySet()) {
						if (entry.getValue().containsKey(i)) {
							Pair<?, Double> pair = (Pair<?, Double>)entry.getValue().get(i);
							sentenceJson.put(entry.getKey().getType(), entry.getKey().serialize(pair.getFirst()));
						}
					}
				}
				
				if (this.otherTokenAnnotations != null) {
					for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : this.otherTokenAnnotations.entrySet()) {
						JSONArray jsonObjs = new JSONArray();
						
						for (int j = 0; j < entry.getValue()[i].length; j++)
							jsonObjs.put(entry.getKey().serialize(entry.getValue()[i][j].getFirst()));
						
						sentenceJson.put(entry.getKey().getType() + "s", jsonObjs);
					}
				}
				
				sentencesJson.put(sentenceJson);
			}
			json.put("sentences", sentencesJson);

			if (this.ner != null) {
				JSONArray nerJson = new JSONArray();
				for (Entry<Integer, List<Triple<TokenSpan, String, Double>>> sentenceEntry : this.ner.entrySet()) {
					JSONObject sentenceJson = new JSONObject();
					sentenceJson.put("sentence", sentenceEntry.getKey());
					JSONArray annotationSpansJson = new JSONArray();
					for (Pair<TokenSpan, String> annotationSpan : sentenceEntry.getValue()) {
						JSONObject annotationSpanJson = new JSONObject();
						
						annotationSpanJson.put("tokenSpan", annotationSpan.getFirst().toJSON(false));
						annotationSpanJson.put("type", AnnotationTypeNLP.NER.serialize(annotationSpan.getSecond()));
						annotationSpansJson.put(annotationSpanJson);
					}
					
					sentenceJson.put("nerSpans", annotationSpansJson);
					nerJson.put(sentenceJson);
				}
				json.put(AnnotationTypeNLP.NER.getType(), nerJson);
			}
			
			if (this.coref != null) {
				JSONArray corefJson = new JSONArray();
				for (Entry<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>> sentenceEntry : this.coref.entrySet()) {
					JSONObject sentenceJson = new JSONObject();
					sentenceJson.put("sentence", sentenceEntry.getKey());
					JSONArray annotationSpansJson = new JSONArray();
					for (Pair<TokenSpan, TokenSpanCluster> annotationSpan : sentenceEntry.getValue()) {
						JSONObject annotationSpanJson = new JSONObject();
						
						annotationSpanJson.put("tokenSpan", annotationSpan.getFirst().toJSON(false));
						annotationSpanJson.put("type", AnnotationTypeNLP.COREF.serialize(annotationSpan.getSecond()));
						annotationSpansJson.put(annotationSpanJson);
					}
					
					sentenceJson.put("corefSpans", annotationSpansJson);
					corefJson.put(sentenceJson);
				}
				json.put(AnnotationTypeNLP.COREF.getType(), corefJson);
			}
			
			if (this.otherTokenSpanAnnotations != null) {
				for (Entry<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>> entry : this.otherTokenSpanAnnotations.entrySet()) {
					JSONArray annotationsJson = new JSONArray();
					String spansStr = entry.getKey().toString() + "Spans";
					for (Entry<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceEntry : entry.getValue().entrySet()) {
						JSONObject sentenceJson = new JSONObject();
						sentenceJson.put("sentence", sentenceEntry.getKey());
						JSONArray annotationSpansJson = new JSONArray();
						for (Pair<TokenSpan, ?> annotationSpan : sentenceEntry.getValue()) {
							JSONObject annotationSpanJson = new JSONObject();
							
							annotationSpanJson.put("tokenSpan", annotationSpan.getFirst().toJSON(false));
							annotationSpanJson.put("type", entry.getKey().serialize(annotationSpan.getSecond()));
							annotationSpansJson.put(annotationSpanJson);
						}
						
						sentenceJson.put(spansStr, annotationSpansJson);
						annotationsJson.put(sentenceJson);
					}
					
					json.put(entry.getKey().getType(), annotationsJson);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean fromJSON(JSONObject json) {
		// FIXME Add confidences
		
		try {
			if (json.has("annotators")) {
				JSONObject annotatorsJson = json.getJSONObject("annotators");
				String[] annotatorTypes = JSONObject.getNames(annotatorsJson);
				for (String annotatorType : annotatorTypes) {
					if (annotatorType.equals(AnnotationTypeNLP.ORIGINAL_TEXT.getType()))
						this.originalTextAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.LANGUAGE.getType()))
						this.languageAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.TOKEN.getType()))
						this.tokenAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.POS.getType()))
						this.posAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE.getType()))
						this.constituencyParseAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE.getType()))
						this.dependencyParseAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.NER.getType()))
						this.nerAnnotatorName = annotatorsJson.getString(annotatorType);
					else if (annotatorType.equals(AnnotationTypeNLP.COREF.getType()))
						this.corefAnnotatorName = annotatorsJson.getString(annotatorType);
					else {
						if (this.otherAnnotatorNames == null)
							this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
						this.otherAnnotatorNames.put(this.dataTools.getAnnotationTypeNLP(annotatorType), annotatorsJson.getString(annotatorType));
					}
				}
			}
			
			if (json.has("name"))
				this.name = json.getString("name");
			
			if (json.has("text"))
				this.originalText = json.getString("text");
			
			if (json.has("language"))
				this.language = Language.valueOf(json.getString("language"));
			
			if (this.dataTools != null) {
				for (AnnotationTypeNLP<?> annotationType : this.dataTools.getAnnotationTypesNLP()) {
					if (annotationType.getTarget() != AnnotationTypeNLP.Target.DOCUMENT
							|| !json.has(annotationType.getType()) 
							|| Arrays.asList(FIRST_CLASS_ANNOTATIONS).contains(annotationType))
						continue;
					
					if (this.otherDocumentAnnotations == null)
						this.otherDocumentAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>>();
	
					this.otherDocumentAnnotations.put(annotationType, new Pair<Object, Double>(annotationType.deserialize(this, json.get(annotationType.getType())), null));
				}
			}
			
			if (!json.has("sentences"))
				return true;
			
			JSONArray sentences = json.getJSONArray("sentences");
			this.tokens = new Token[sentences.length()][];
			this.posTags = new PoSTag[sentences.length()][];
			this.dependencyParses = new DependencyParse[sentences.length()];
			this.constituencyParses = new ConstituencyParse[sentences.length()];
			
			int characterOffset = 0;
			for (int i = 0; i < sentences.length(); i++) {
				JSONObject sentenceJson = sentences.getJSONObject(i);
				JSONArray tokensJson = sentenceJson.getJSONArray("tokens");
				JSONArray posTagsJson = (sentenceJson.has("posTags")) ? sentenceJson.getJSONArray("posTags") : null;
				
				this.tokens[i] = new Token[tokensJson.length()];
				for (int j = 0; j < tokensJson.length(); j++) {
					JSONObject tokenJson = tokensJson.optJSONObject(j);
					if (tokenJson == null) {
						String tokenStr = tokensJson.getString(j);
						this.tokens[i][j] = new Token(this, tokenStr, characterOffset, characterOffset + tokenStr.length());
						characterOffset += tokenStr.length() + 1;
					} else {
						Token token = new Token(this);
						if (!token.fromJSON(tokenJson))
							return false;
						this.tokens[i][j] = token;
					}
				}
				
				if (posTagsJson != null) {
					this.posTags[i] = new PoSTag[posTagsJson.length()];
					for (int j = 0; j < posTagsJson.length(); j++)
						this.posTags[i][j] = PoSTag.valueOf(posTagsJson.getString(j));
				}
				
				if (sentenceJson.has("dependencyParse"))
					this.dependencyParses[i] = DependencyParse.fromString(sentenceJson.getString("dependencyParse"), this, i);
				if (sentenceJson.has("constituencyParse"))
					this.constituencyParses[i] = ConstituencyParse.fromString(sentenceJson.getString("constituencyParse"), this, i);
			
				if (this.dataTools != null) {
					for (AnnotationTypeNLP<?> annotationType : this.dataTools.getAnnotationTypesNLP()) {
						if (Arrays.asList(FIRST_CLASS_ANNOTATIONS).contains(annotationType))
							continue;
						
						if (annotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE && sentenceJson.has(annotationType.getType())) {
							if (this.otherSentenceAnnotations == null)
								this.otherSentenceAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, ?>>();
							if (!this.otherSentenceAnnotations.containsKey(annotationType))
								this.otherSentenceAnnotations.put(annotationType, new HashMap<Integer, Pair<Object, Double>>());
							Map<Integer, Pair<Object, Double>> sentenceMap = (Map<Integer, Pair<Object, Double>>)this.otherSentenceAnnotations.get(annotationType);
							sentenceMap.put(i, new Pair<Object, Double>(annotationType.deserialize(this, i, sentenceJson.get(annotationType.getType())), null));
						} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN && sentenceJson.has(annotationType.getType() + "s")) {
							if (this.otherTokenAnnotations == null)
								this.otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
							if (!this.otherTokenAnnotations.containsKey(annotationType))
								this.otherTokenAnnotations.put(annotationType, (Pair<Object, Double>[][])new Pair[sentences.length()][]);
							
							JSONArray jsonAnnotations = sentenceJson.getJSONArray(annotationType.getType() + "s");
							Pair<Object, Double>[] sentenceAnnotations = (Pair<Object, Double>[])new Pair[jsonAnnotations.length()];
							for (int j = 0; j < jsonAnnotations.length(); j++)
								sentenceAnnotations[j] = new Pair<Object, Double>(annotationType.deserialize(this, i, jsonAnnotations.get(j)), null);
							this.otherTokenAnnotations.get(annotationType)[i] = sentenceAnnotations;
						}
					}
				}
			}
		
			if (json.has(AnnotationTypeNLP.NER.getType())) {
				this.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
	
				JSONArray nerJson = json.getJSONArray(AnnotationTypeNLP.NER.getType());
				for (int i = 0; i < nerJson.length(); i++) {
					JSONObject sentenceNerJson = nerJson.getJSONObject(i);  
					JSONArray nerSpansJson = sentenceNerJson.getJSONArray("nerSpans");
					int sentenceIndex = sentenceNerJson.getInt("sentence");
					List<Triple<TokenSpan, String, Double>> nerSpans = new ArrayList<Triple<TokenSpan, String, Double>>();
					for (int j = 0; j < nerSpansJson.length(); j++)
						nerSpans.add(new Triple<TokenSpan, String, Double>(TokenSpan.fromJSON(nerSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), this, sentenceIndex),
																AnnotationTypeNLP.NER.deserialize(this, sentenceIndex, nerSpansJson.getJSONObject(j).get("type")), null));
					
					this.ner.put(sentenceIndex, nerSpans);
				}
			}
			
			if (json.has(AnnotationTypeNLP.COREF.getType())) {
				this.coref = new HashMap<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>>();
	
				JSONArray corefJson = json.getJSONArray(AnnotationTypeNLP.COREF.getType());
				for (int i = 0; i < corefJson.length(); i++) {
					JSONObject sentenceCorefJson = corefJson.getJSONObject(i);  
					JSONArray corefSpansJson = sentenceCorefJson.getJSONArray("corefSpans");
					int sentenceIndex = sentenceCorefJson.getInt("sentence");
					List<Triple<TokenSpan, TokenSpanCluster, Double>> corefSpans = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
					for (int j = 0; j < corefSpansJson.length(); j++)
						corefSpans.add(new Triple<TokenSpan, TokenSpanCluster, Double>(TokenSpan.fromJSON(corefSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), this, sentenceIndex),
																AnnotationTypeNLP.COREF.deserialize(this, sentenceIndex, corefSpansJson.getJSONObject(j).get("type")), null));
					
					this.coref.put(sentenceIndex, corefSpans);
				}
			}

			if (this.dataTools != null) {
				for (AnnotationTypeNLP<?> annotationType : this.dataTools.getAnnotationTypesNLP()) {
					if (annotationType.getTarget() != AnnotationTypeNLP.Target.TOKEN_SPAN
							|| !json.has(annotationType.getType()) 
							|| Arrays.asList(FIRST_CLASS_ANNOTATIONS).contains(annotationType))
						continue;
					
					if (this.otherTokenSpanAnnotations == null)
						this.otherTokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>>();
	
					Map<Integer, List<Triple<TokenSpan, ?, Double>>> tokenSpanAnnotations = new HashMap<Integer, List<Triple<TokenSpan, ?, Double>>>();
					
					JSONArray annotationJson = json.getJSONArray(annotationType.getType());
					for (int i = 0; i < annotationJson.length(); i++) {
						JSONObject sentenceAnnotationJson = annotationJson.getJSONObject(i);  
						String spansStr = annotationType.getType() + "Spans";
						if (!sentenceAnnotationJson.has("sentence") || !sentenceAnnotationJson.has(spansStr)) {
							// This case is here for backward compatability for annotations that aren't
							// stored by sentence
							JSONObject pairJson = annotationJson.getJSONObject(i);
							TokenSpan tokenSpan = TokenSpan.fromJSON(pairJson.getJSONObject("tokenSpan"), this);
							Object annotationObj =  annotationType.deserialize(this, tokenSpan.getSentenceIndex(), pairJson.getJSONObject("annotation"));
							if (!tokenSpanAnnotations.containsKey(tokenSpan.getSentenceIndex()))
								tokenSpanAnnotations.put(tokenSpan.getSentenceIndex(), new ArrayList<Triple<TokenSpan, ?, Double>>());
							tokenSpanAnnotations.get(tokenSpan.getSentenceIndex()).add(new Triple<TokenSpan, Object, Double>(tokenSpan, annotationObj, null));
						} else {
							JSONArray annotationSpansJson = sentenceAnnotationJson.getJSONArray(spansStr);
							int sentenceIndex = sentenceAnnotationJson.getInt("sentence");
							List<Triple<TokenSpan, ?, Double>> annotationSpans = new ArrayList<Triple<TokenSpan, ?, Double>>();
							for (int j = 0; j < annotationSpansJson.length(); j++)
								annotationSpans.add(new Triple<TokenSpan, Object, Double>(TokenSpan.fromJSON(annotationSpansJson.getJSONObject(j).getJSONObject("tokenSpan"), this, sentenceIndex),
																		annotationType.deserialize(this, sentenceIndex, annotationSpansJson.getJSONObject(j).get("type")), null));
							
							tokenSpanAnnotations.put(sentenceIndex, annotationSpans);
						}
					}	
					
					this.otherTokenSpanAnnotations.put(annotationType, tokenSpanAnnotations);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public DocumentNLP makeInstanceFromJSONFile(String path) {
		return new DocumentNLPInMemory(this.dataTools, path);
	}
	
	@Override
	public DocumentNLP makeInstanceFromMicroAnnotation(DocumentAnnotation documentAnnotation, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators) {
		return new DocumentNLPInMemory(this.dataTools, documentAnnotation, pipeline, skipAnnotators);
	}

	private TokenSpan getTokenSpanFromCharSpan(int sentenceIndex, int charSpanStart, int charSpanEnd) {
		int startTokenIndex = -1;
		int endTokenIndex = -1;
		for (int i = 0; i < this.tokens[sentenceIndex].length; i++) {
			if (this.tokens[sentenceIndex][i].getCharSpanStart() == charSpanStart)
				startTokenIndex = i;
			if (this.tokens[sentenceIndex][i].getCharSpanEnd() == charSpanEnd) {
				endTokenIndex = i + 1;
				break;
			}
		}
		
		if (startTokenIndex < 0 || endTokenIndex < 0)
			return null;
		else
			return new TokenSpan(this, sentenceIndex, startTokenIndex, endTokenIndex);
	}
	
	@Override
	public boolean fromMicroAnnotation(DocumentAnnotation documentAnnotation) {
          return fromMicroAnnotation(documentAnnotation, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean fromMicroAnnotation(DocumentAnnotation documentAnnotation, String originalText) {
		if (this.dataTools == null)
			throw new UnsupportedOperationException("Data tools must not be null to deserialize from micro-reading annotations");
		
		this.name = documentAnnotation.getDocumentId();
		this.originalText = originalText;
		
		List<Annotation> annotations = documentAnnotation.getAllAnnotations();
		Map<AnnotationType<?>, TreeMap<Integer, List<Annotation>>> orderedAnnotations = new HashMap<AnnotationType<?>, TreeMap<Integer, List<Annotation>>>();
		boolean hasNonDocumentAnnotations = false;
		for (Annotation annotation : annotations) {
			AnnotationType<?> annotationType = this.dataTools.getAnnotationTypeNLP(annotation.getSlot());
			
			if (((AnnotationTypeNLP<?>)annotationType).getTarget() != AnnotationTypeNLP.Target.DOCUMENT)
				hasNonDocumentAnnotations = true;
			
			if (!orderedAnnotations.containsKey(annotationType))
				orderedAnnotations.put(annotationType, new TreeMap<Integer, List<Annotation>>());
			if (!orderedAnnotations.get(annotationType).containsKey(annotation.getSpanStart()))
				orderedAnnotations.get(annotationType).put(annotation.getSpanStart(),  new ArrayList<Annotation>(3));
			orderedAnnotations.get(annotationType).get(annotation.getSpanStart()).add(annotation);
		}
		
		if (hasNonDocumentAnnotations && (!orderedAnnotations.containsKey(AnnotationTypeNLP.SENTENCE) || !orderedAnnotations.containsKey(AnnotationTypeNLP.TOKEN)))
			throw new UnsupportedOperationException("Document must contain sentence annotations.");
		
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.ORIGINAL_TEXT)) {
			TreeMap<Integer, List<Annotation>> textAnnotations = orderedAnnotations.get(AnnotationTypeNLP.ORIGINAL_TEXT);
			this.originalText = AnnotationTypeNLP.ORIGINAL_TEXT.deserialize(this, textAnnotations.firstEntry().getValue().get(0).getValue());
			this.originalTextAnnotatorName = textAnnotations.firstEntry().getValue().get(0).getAnnotator();
			this.originalTextConf = textAnnotations.firstEntry().getValue().get(0).getConfidence();
		}

		if (orderedAnnotations.containsKey(AnnotationTypeNLP.LANGUAGE)) {
			TreeMap<Integer, List<Annotation>> languageAnnotations = orderedAnnotations.get(AnnotationTypeNLP.LANGUAGE);
			this.language = AnnotationTypeNLP.LANGUAGE.deserialize(this, languageAnnotations.firstEntry().getValue().get(0).getValue());
			this.languageAnnotatorName = languageAnnotations.firstEntry().getValue().get(0).getAnnotator();
			this.languageConf = languageAnnotations.firstEntry().getValue().get(0).getConfidence();
		}
		
		List<AnnotationTypeNLP<?>> otherAnnotationTypes = new ArrayList<AnnotationTypeNLP<?>>(); // Other non-document annotation types
		for (AnnotationTypeNLP<?> annotationType : this.dataTools.getAnnotationTypesNLP()) {
			if (!orderedAnnotations.containsKey(annotationType)
				|| Arrays.asList(FIRST_CLASS_ANNOTATIONS).contains(annotationType))
				continue;
			
			if (this.otherAnnotatorNames == null)
				this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			this.otherAnnotatorNames.put(annotationType, orderedAnnotations.get(annotationType).firstEntry().getValue().get(0).getAnnotator());
			
			if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN) {
				otherAnnotationTypes.add(annotationType);
				if (this.otherTokenAnnotations == null)
					this.otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
				this.otherTokenAnnotations.put(annotationType, new Pair[orderedAnnotations.get(AnnotationTypeNLP.SENTENCE).size()][]);
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN_SPAN) {
				otherAnnotationTypes.add(annotationType);
				if (this.otherTokenSpanAnnotations == null)
					this.otherTokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>>();
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE) {
				otherAnnotationTypes.add(annotationType);
				if (this.otherSentenceAnnotations == null)
					this.otherSentenceAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, ?>>();
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.DOCUMENT) {
				if (this.otherDocumentAnnotations == null)
					this.otherDocumentAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>>();
				Annotation otherDocAnno = orderedAnnotations.get(annotationType).firstEntry().getValue().get(0);
				this.otherDocumentAnnotations.put(annotationType, new Pair<Object, Double>(annotationType.deserialize(this, otherDocAnno.getValue()), otherDocAnno.getConfidence()));			
			}
		}
		
		if (!hasNonDocumentAnnotations)
			return true;
		
		TreeMap<Integer, List<Annotation>> sentenceAnnotations = orderedAnnotations.get(AnnotationTypeNLP.SENTENCE);
		this.tokens = new Token[sentenceAnnotations.size()][];
		this.tokenAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.TOKEN).firstEntry().getValue().get(0).getAnnotator();
		boolean tokenConf = orderedAnnotations.get(AnnotationTypeNLP.TOKEN).firstEntry().getValue().get(0).getConfidence() != null;
		if (tokenConf)
			this.tokensConf = new double[sentenceAnnotations.size()][];
		
		boolean posConf = false;
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.POS)) {
			this.posTags = new PoSTag[sentenceAnnotations.size()][];
			this.posAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.POS).firstEntry().getValue().get(0).getAnnotator();
			posConf = orderedAnnotations.get(AnnotationTypeNLP.POS).firstEntry().getValue().get(0).getConfidence() != null;
			if (posConf)
				this.posTagsConf = new double[sentenceAnnotations.size()][];
		}
		
		boolean consConf = false;
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			this.constituencyParses = new ConstituencyParse[sentenceAnnotations.size()];
			this.constituencyParseAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.CONSTITUENCY_PARSE).firstEntry().getValue().get(0).getAnnotator();
			consConf = orderedAnnotations.get(AnnotationTypeNLP.CONSTITUENCY_PARSE).firstEntry().getValue().get(0).getConfidence() != null;
			if (consConf)
				this.constituencyParsesConf = new double[sentenceAnnotations.size()];
		
		}
		
		boolean depConf = false;
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			this.dependencyParses = new DependencyParse[sentenceAnnotations.size()];	
			this.dependencyParseAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.DEPENDENCY_PARSE).firstEntry().getValue().get(0).getAnnotator();
			depConf = orderedAnnotations.get(AnnotationTypeNLP.DEPENDENCY_PARSE).firstEntry().getValue().get(0).getConfidence() != null;
			if (depConf)
				this.dependencyParsesConf = new double[sentenceAnnotations.size()];	
		}
		
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.NER)) {
			this.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
			this.nerAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.NER).firstEntry().getValue().get(0).getAnnotator();
		}
		
		if (orderedAnnotations.containsKey(AnnotationTypeNLP.COREF)) {
			this.coref = new HashMap<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>>();
			this.corefAnnotatorName = orderedAnnotations.get(AnnotationTypeNLP.COREF).firstEntry().getValue().get(0).getAnnotator();	
		}
		
		int sentenceIndex = 0;
		for (Entry<Integer, List<Annotation>> sentenceEntry : sentenceAnnotations.entrySet()) {
			int sentenceStart = sentenceEntry.getKey();
			int sentenceEnd = sentenceEntry.getValue().get(0).getSpanEnd();
			
			SortedMap<Integer, List<Annotation>> sentenceTokens = orderedAnnotations.get(AnnotationTypeNLP.TOKEN).subMap(sentenceStart, sentenceEnd);
			this.tokens[sentenceIndex] = new Token[sentenceTokens.size()];
			if (tokenConf)
				this.tokensConf[sentenceIndex] = new double[sentenceTokens.size()];
			
			int tokenIndex = 0;
			for (List<Annotation> token : sentenceTokens.values()) {
				this.tokens[sentenceIndex][tokenIndex] = new Token(this, token.get(0).getValue().toString(), token.get(0).getSpanStart(), token.get(0).getSpanEnd());
				if (tokenConf)
					this.tokensConf[sentenceIndex][tokenIndex] = token.get(0).getConfidence();
				tokenIndex++;
			}
			
			if (this.posTags != null) {
				SortedMap<Integer, List<Annotation>> sentencePoS =  orderedAnnotations.get(AnnotationTypeNLP.POS).subMap(sentenceStart, sentenceEnd);
				if (sentencePoS.size() != sentenceTokens.size())
					return false;
				int posIndex = 0;
				this.posTags[sentenceIndex] = new PoSTag[sentencePoS.size()];
				if (posConf)
					this.posTagsConf[sentenceIndex] = new double[sentencePoS.size()];
				for (List<Annotation> pos : sentencePoS.values()) {
					this.posTags[sentenceIndex][posIndex] = AnnotationTypeNLP.POS.deserialize(this, sentenceIndex, pos.get(0).getValue());
					if (posConf)
						this.posTagsConf[sentenceIndex][posIndex] = pos.get(0).getConfidence();
					posIndex++;
				}
			}
			
			if (this.dependencyParses != null) {
				Map<Integer, List<Annotation>> dependencyAnnotations = orderedAnnotations.get(AnnotationTypeNLP.DEPENDENCY_PARSE);
				if (!dependencyAnnotations.containsKey(sentenceStart))
					return false;
				this.dependencyParses[sentenceIndex] = AnnotationTypeNLP.DEPENDENCY_PARSE.deserialize(this, sentenceIndex, dependencyAnnotations.get(sentenceStart).get(0).getValue());
				if (depConf)
					this.dependencyParsesConf[sentenceIndex] = dependencyAnnotations.get(sentenceStart).get(0).getConfidence();
			}
			
			if (this.constituencyParses != null) {
				Map<Integer, List<Annotation>> constituencyAnnotations = orderedAnnotations.get(AnnotationTypeNLP.CONSTITUENCY_PARSE);
				if (!constituencyAnnotations.containsKey(sentenceStart))
					return false;
				this.constituencyParses[sentenceIndex] = AnnotationTypeNLP.CONSTITUENCY_PARSE.deserialize(this, sentenceIndex, constituencyAnnotations.get(sentenceStart).get(0).getValue());
				if (consConf)
					this.constituencyParsesConf[sentenceIndex] = constituencyAnnotations.get(sentenceStart).get(0).getConfidence();
			}
			
			if (this.ner != null) {
				SortedMap<Integer, List<Annotation>> sentenceNerAnnotations = orderedAnnotations.get(AnnotationTypeNLP.NER).subMap(sentenceStart, sentenceEnd);
				if (sentenceNerAnnotations.size() != 0) {
					List<Triple<TokenSpan, String, Double>> sentenceNer = new ArrayList<Triple<TokenSpan, String, Double>>();
					for (List<Annotation> nerAnnotations : sentenceNerAnnotations.values()) {
						for (Annotation annotation : nerAnnotations) {
							if (!annotation.getAnnotator().equals(nerAnnotations.get(0).getAnnotator()))
								continue;
							
							TokenSpan tokenSpan = getTokenSpanFromCharSpan(sentenceIndex, annotation.getSpanStart(), annotation.getSpanEnd());
							sentenceNer.add(new Triple<TokenSpan, String, Double>(tokenSpan, AnnotationTypeNLP.NER.deserialize(this, sentenceIndex, annotation.getValue()), annotation.getConfidence()));
						}
					}
					this.ner.put(sentenceIndex, sentenceNer);
				}
			}
			
			if (this.coref != null) {
				SortedMap<Integer, List<Annotation>> sentenceCorefAnnotations = orderedAnnotations.get(AnnotationTypeNLP.COREF).subMap(sentenceStart, sentenceEnd);
				if (sentenceCorefAnnotations.size() != 0) {
					List<Triple<TokenSpan, TokenSpanCluster, Double>> sentenceCoref = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
					for (List<Annotation> corefAnnotations : sentenceCorefAnnotations.values()) {
						for (Annotation annotation : corefAnnotations) {
							if (!annotation.getAnnotator().equals(corefAnnotations.get(0).getAnnotator()))
								continue;
							
							TokenSpan tokenSpan = getTokenSpanFromCharSpan(sentenceIndex, annotation.getSpanStart(), annotation.getSpanEnd());
							sentenceCoref.add(new Triple<TokenSpan, TokenSpanCluster, Double>(tokenSpan, AnnotationTypeNLP.COREF.deserialize(this, sentenceIndex, annotation.getValue()), annotation.getConfidence()));
						}
					}
					this.coref.put(sentenceIndex, sentenceCoref);
				}
			}
			
			for (AnnotationTypeNLP<?> otherAnnotationType : otherAnnotationTypes) {
				SortedMap<Integer, List<Annotation>> otherAnnotations = orderedAnnotations.get(otherAnnotationType).subMap(sentenceStart, sentenceEnd);
				if (otherAnnotations.size() == 0)
					continue;
				else if (otherAnnotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN) {
					Pair<Object, Double>[] tokenAnnotations = new Pair[otherAnnotations.size()];
					int i = 0;
					for (List<Annotation> annotation : otherAnnotations.values()) {
						tokenAnnotations[i] = new Pair<Object, Double>(otherAnnotationType.deserialize(this, sentenceIndex, annotation.get(0).getValue()), annotation.get(0).getConfidence());
						i++;
					}
					
					this.otherTokenAnnotations.get(otherAnnotationType)[sentenceIndex] = tokenAnnotations;
				} else if (otherAnnotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE) {
					if (otherAnnotations.containsKey(sentenceStart)) {
						Map<Integer, Pair<Object, Double>> sentenceMap = (Map<Integer, Pair<Object, Double>>)this.otherSentenceAnnotations.get(otherAnnotationType);
						
						sentenceMap.put(sentenceIndex, new Pair<Object, Double>(
								otherAnnotationType.deserialize(this, sentenceIndex, otherAnnotations.get(sentenceStart).get(0).getValue()), otherAnnotations.get(sentenceStart).get(0).getConfidence()));
					}
				} else if (otherAnnotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN_SPAN) {
					List<Triple<TokenSpan, ?, Double>> sentenceAnno = new ArrayList<Triple<TokenSpan, ?, Double>>();
					for (List<Annotation> tokenSpanAnnotations : otherAnnotations.values()) {
						for (Annotation annotation : tokenSpanAnnotations) {
							if (!annotation.getAnnotator().equals(this.otherAnnotatorNames.get(otherAnnotationType)))
								continue;
							
							TokenSpan tokenSpan = getTokenSpanFromCharSpan(sentenceIndex, annotation.getSpanStart(), annotation.getSpanEnd());
							sentenceAnno.add(new Triple<TokenSpan, Object, Double>(tokenSpan, otherAnnotationType.deserialize(this, sentenceIndex, annotation.getValue()), annotation.getConfidence()));
						}
					}
					
					if (!this.otherTokenSpanAnnotations.containsKey(otherAnnotationType)) {
						this.otherTokenSpanAnnotations.put(otherAnnotationType, new HashMap<Integer, List<Triple<TokenSpan, ? , Double>>>());
					}
					this.otherTokenSpanAnnotations.get(otherAnnotationType).put(sentenceIndex, sentenceAnno);
				}
			}
			
			sentenceIndex++;
		}
		
		return true;
	}
	
	private Annotation makeMicroAnnotation(int spanStart, int spanEnd, String slot, String annotator, DateTime annotationTime, Object value, Double confidence) {
		if (value instanceof String) {
			return new Annotation(spanStart, 
								  spanEnd,
								  slot,
								  annotator != null ? annotator : "",
								  this.name,
								  value.toString(),
								  confidence,
								  annotationTime,
								  null);
		} else {
			return new Annotation(spanStart, 
					  spanEnd,
					  slot,
					  annotator != null ? annotator : "",
					  this.name,
					  (JSONObject)value,
					  confidence,
					  annotationTime,
					  null);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public DocumentAnnotation toMicroAnnotation(Collection<AnnotationTypeNLP<?>> annotationTypes) {
		DateTime annotationTime = DateTime.now();
		List<Annotation> annotations = new ArrayList<Annotation>();
		
		int lastCharIndex = (this.tokens != null && this.tokens.length > 0) ? this.tokens[this.tokens.length-1][this.tokens[this.tokens.length - 1].length - 1].getCharSpanEnd() : 0;
		if (this.originalText != null && annotationTypes.contains(AnnotationTypeNLP.ORIGINAL_TEXT)) {
			annotations.add(makeMicroAnnotation(0, 
												lastCharIndex, 
												AnnotationTypeNLP.ORIGINAL_TEXT.getType(), 
												this.originalTextAnnotatorName, 
												annotationTime,
												AnnotationTypeNLP.ORIGINAL_TEXT.serialize(this.originalText),
												this.originalTextConf));
		}
		
		if (this.language != null && annotationTypes.contains(AnnotationTypeNLP.LANGUAGE)) {
			annotations.add(makeMicroAnnotation(0, 
					lastCharIndex, 
					AnnotationTypeNLP.LANGUAGE.getType(), 
					this.languageAnnotatorName, 
					annotationTime,
					AnnotationTypeNLP.LANGUAGE.serialize(this.language),
					this.languageConf));
		}
	
		if (this.otherDocumentAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>> entry : this.otherDocumentAnnotations.entrySet()) {
				if (!annotationTypes.contains(entry.getKey()))
					continue;
				annotations.add(makeMicroAnnotation(0, 
						lastCharIndex, 
						entry.getKey().getType(), 
						(this.otherAnnotatorNames != null) ? this.otherAnnotatorNames.get(entry.getKey()) : "", 
						annotationTime,
						entry.getKey().serialize(entry.getValue().getFirst()),
						entry.getValue().getSecond()));
			}
		}
	
		boolean outputTokens = annotationTypes.contains(AnnotationTypeNLP.TOKEN);
		boolean outputPoS = annotationTypes.contains(AnnotationTypeNLP.POS);
		boolean outputSentence = annotationTypes.contains(AnnotationTypeNLP.SENTENCE);
		boolean outputDep = annotationTypes.contains(AnnotationTypeNLP.DEPENDENCY_PARSE);
		boolean outputCon = annotationTypes.contains(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		boolean outputNer = annotationTypes.contains(AnnotationTypeNLP.NER);
		boolean outputCoref = annotationTypes.contains(AnnotationTypeNLP.COREF);
		
		if (this.tokens == null)
			return new DocumentAnnotation(this.name, annotations);
		
		for (int i = 0; i < this.tokens.length; i++) {
			if (outputTokens || outputPoS) {
				for (int j = 0; j < this.tokens[i].length; j++) {
					if (outputTokens) {
						annotations.add(makeMicroAnnotation(this.tokens[i][j].getCharSpanStart(), 
														    this.tokens[i][j].getCharSpanEnd(), 
														    AnnotationTypeNLP.TOKEN.getType(), 
														    this.tokenAnnotatorName,  
														    annotationTime,
														    this.tokens[i][j].getStr(),
														    (this.tokensConf != null) ? this.tokensConf[i][j] : null));
					}
					
					if (outputPoS && this.posTags != null) {
							annotations.add(makeMicroAnnotation(this.tokens[i][j].getCharSpanStart(), 
																this.tokens[i][j].getCharSpanEnd(), 
																AnnotationTypeNLP.POS.getType(), 
																this.posAnnotatorName, 
																annotationTime,
																AnnotationTypeNLP.POS.serialize(this.posTags[i][j]),
																this.posTagsConf != null ? this.posTagsConf[i][j] : null));
					}
				}
			}
			
			if (outputSentence) {
				annotations.add(makeMicroAnnotation(this.tokens[i][0].getCharSpanStart(), 
													this.tokens[i][this.tokens[i].length - 1].getCharSpanEnd(), 
													AnnotationTypeNLP.SENTENCE.getType(), 
													this.tokenAnnotatorName, 
													annotationTime,
													getSentence(i),
													null));
			}
			
			if (this.dependencyParses != null && outputDep) {
				annotations.add(makeMicroAnnotation(this.tokens[i][0].getCharSpanStart(), 
													this.tokens[i][this.tokens[i].length - 1].getCharSpanEnd(), 
													AnnotationTypeNLP.DEPENDENCY_PARSE.getType(), 
													this.dependencyParseAnnotatorName, 
													annotationTime,
													AnnotationTypeNLP.DEPENDENCY_PARSE.serialize(getDependencyParse(i)),
													this.dependencyParsesConf != null ? this.dependencyParsesConf[i] : null));
			}
			
			if (this.constituencyParses != null && outputCon) {
				annotations.add(makeMicroAnnotation(this.tokens[i][0].getCharSpanStart(), 
						this.tokens[i][this.tokens[i].length - 1].getCharSpanEnd(), 
						AnnotationTypeNLP.CONSTITUENCY_PARSE.getType(), 
						this.constituencyParseAnnotatorName, 
						annotationTime,
						AnnotationTypeNLP.CONSTITUENCY_PARSE.serialize(getConstituencyParse(i)),
						this.constituencyParsesConf != null ? this.constituencyParsesConf[i] : null));
			}
		
			if (this.ner != null && outputNer && this.ner.containsKey(i)) {
				List<Triple<TokenSpan, String, Double>> sentenceNer = this.ner.get(i);
				for (Triple<TokenSpan, String, Double> nerSpan : sentenceNer) {
					annotations.add(makeMicroAnnotation(this.tokens[i][nerSpan.getFirst().getStartTokenIndex()].getCharSpanStart(), 
							this.tokens[i][nerSpan.getFirst().getEndTokenIndex()-1].getCharSpanEnd(), 
							AnnotationTypeNLP.NER.getType(), 
							this.nerAnnotatorName, 
							annotationTime,
							AnnotationTypeNLP.NER.serialize(nerSpan.getSecond()),
							nerSpan.getThird()));
				}
			}
			
			if (this.coref != null && outputCoref && this.coref.containsKey(i)) {
				List<Triple<TokenSpan, TokenSpanCluster, Double>> sentenceCoref = this.coref.get(i);
				for (Triple<TokenSpan, TokenSpanCluster, Double> corefSpan : sentenceCoref) {
					annotations.add(makeMicroAnnotation(this.tokens[i][corefSpan.getFirst().getStartTokenIndex()].getCharSpanStart(), 
							this.tokens[i][corefSpan.getFirst().getEndTokenIndex()-1].getCharSpanEnd(), 
							AnnotationTypeNLP.COREF.getType(), 
							this.corefAnnotatorName, 
							annotationTime,
							AnnotationTypeNLP.COREF.serialize(corefSpan.getSecond()),
							corefSpan.getThird()));
				}
			}
		}
		
		if (this.otherSentenceAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, ?>> entry : this.otherSentenceAnnotations.entrySet()) {
				if (!annotationTypes.contains(entry.getKey()))
					continue;
				
				for (Entry<Integer, ?> sentenceEntry : entry.getValue().entrySet()) {
					Pair pair = (Pair)sentenceEntry.getValue();
					annotations.add(makeMicroAnnotation(this.tokens[sentenceEntry.getKey()][0].getCharSpanStart(), 
							this.tokens[sentenceEntry.getKey()][this.tokens[sentenceEntry.getKey()].length - 1].getCharSpanEnd(), 
							entry.getKey().getType(), 
							(this.otherAnnotatorNames != null) ? this.otherAnnotatorNames.get(entry.getKey()) : "", 
							annotationTime,
							entry.getKey().serialize(pair.getFirst()),
							(Double)pair.getSecond()));
				}
			}
		}
		
		if (this.otherTokenSpanAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>> entry : this.otherTokenSpanAnnotations.entrySet()) {
				if (!annotationTypes.contains(entry.getKey())) {
					continue;
				}
				
				for (Entry<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceEntry : entry.getValue().entrySet()) {
					for (Triple<TokenSpan, ?, Double> span : sentenceEntry.getValue()) {
						annotations.add(makeMicroAnnotation(this.tokens[sentenceEntry.getKey()][span.getFirst().getStartTokenIndex()].getCharSpanStart(), 
								this.tokens[sentenceEntry.getKey()][span.getFirst().getEndTokenIndex()-1].getCharSpanEnd(), 
								entry.getKey().getType(), 
								(this.otherAnnotatorNames != null) ? this.otherAnnotatorNames.get(entry.getKey()) : "", 
								annotationTime,
								entry.getKey().serialize(span.getSecond()),
								span.getThird()));	
					}
				}
			}
		}
		
		if (this.otherTokenAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : this.otherTokenAnnotations.entrySet()) {
				if (!annotationTypes.contains(entry.getKey()))
					continue;
				
				Pair<?, Double>[][] anno = entry.getValue();
				for (int i = 0; i < anno.length; i++) {
					for (int j = 0; j < anno[i].length; j++) {
						annotations.add(makeMicroAnnotation(this.tokens[i][j].getCharSpanStart(), 
								this.tokens[i][j].getCharSpanEnd(), 
								entry.getKey().getType(), 
								(this.otherAnnotatorNames != null) ? this.otherAnnotatorNames.get(entry.getKey()) : "", 
								annotationTime,
								entry.getKey().serialize(anno[i][j].getFirst()),
								anno[i][j].getSecond()));
					}
				}
			}
		}
		
		return new DocumentAnnotation(this.name, annotations);
	}

	@Override
	public String toHtmlString(Collection<AnnotationTypeNLP<?>> annotationTypes) {
		StringBuilder htmlBuilder = new StringBuilder();
		beginHtml(htmlBuilder);
		if (this.originalText == null) {
			throw new RuntimeException("You really need to have the original text to output html...");
		}
		
                htmlBuilder.append("<div class=\"document\" style=\"height:29%; overflow-y:scroll\">");
		htmlBuilder.append("<div class=\"textLabel\"><h3>Document Text</h3></div>");
		htmlBuilder.append("<div class=\"text\">");
		htmlBuilder.append(AnnotationTypeNLP.ORIGINAL_TEXT.toHtml(originalText));
		htmlBuilder.append("</div>");
                htmlBuilder.append("</div>");

		htmlBuilder.append("<div class=\"annotations\" style=\"height:69%; overflow-y:scroll\">");
		htmlBuilder.append("<h3>Annotations</h3>");
		htmlBuilder.append("<ul>");
		
		if (this.language != null && annotationTypes != null && annotationTypes.contains(AnnotationTypeNLP.LANGUAGE)) {
			htmlBuilder.append("<div class=\"annotation\">\n");
			htmlBuilder.append(AnnotationTypeNLP.LANGUAGE.toHtml(language));
			htmlBuilder.append("</div>\n");
		}
		
		if (this.otherDocumentAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>> entry : this.otherDocumentAnnotations.entrySet()) {
				if (!annotationTypes.contains(entry.getKey())) 
					continue;
				htmlBuilder.append("<div class=\"annotation\">\n");
				htmlBuilder.append(entry.getKey().toHtml(entry.getValue().getFirst()));
				htmlBuilder.append("</div>\n");
			}
		}

		boolean outputPoS = annotationTypes == null || annotationTypes.contains(AnnotationTypeNLP.POS);
		boolean outputDep = annotationTypes == null || annotationTypes.contains(AnnotationTypeNLP.DEPENDENCY_PARSE);
		boolean outputCon = annotationTypes == null || annotationTypes.contains(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		boolean outputNer = annotationTypes == null || annotationTypes.contains(AnnotationTypeNLP.NER);
		boolean outputCoref = annotationTypes == null || annotationTypes.contains(AnnotationTypeNLP.COREF);

		if (this.tokens == null) {
			endHtml(htmlBuilder);
			return htmlBuilder.toString();
		}

		for (int i = 0; i < this.tokens.length; i++) {
			if (outputPoS && this.posTags != null) {
				for (int j = 0; j < this.tokens[i].length; j++) {
					htmlBuilder.append("<div class=\"annotation\">\n");
					htmlBuilder.append(AnnotationTypeNLP.POS.toHtml(this.posTags[i][j]));
					htmlBuilder.append("</div>\n");
				}
			}

			if (this.dependencyParses != null && outputDep) {
				htmlBuilder.append("<div class=\"annotation\">\n");
				htmlBuilder.append(AnnotationTypeNLP.DEPENDENCY_PARSE.toHtml(getDependencyParse(i)));
				htmlBuilder.append("</div>\n");
			}

			if (this.constituencyParses != null && outputCon) {
				htmlBuilder.append("<div class=\"annotation\">\n");
				htmlBuilder.append(AnnotationTypeNLP.CONSTITUENCY_PARSE.toHtml(getConstituencyParse(i)));
				htmlBuilder.append("</div>\n");
			}

			if (this.ner != null && outputNer && this.ner.containsKey(i)) {
				List<Triple<TokenSpan, String, Double>> sentenceNer = this.ner.get(i);
				for (Triple<TokenSpan, String, Double> nerSpan : sentenceNer) {
					htmlBuilder.append("<div class=\"annotation\">\n");
					htmlBuilder.append(AnnotationTypeNLP.NER.toHtml(nerSpan.getSecond()));
					htmlBuilder.append("</div>\n");
				}
			}

			if (this.coref != null && outputCoref && this.coref.containsKey(i)) {
				List<Triple<TokenSpan, TokenSpanCluster, Double>> sentenceCoref = this.coref.get(i);
				for (Triple<TokenSpan, TokenSpanCluster, Double> corefSpan : sentenceCoref) {
					htmlBuilder.append("<div class=\"annotation\">\n");
					htmlBuilder.append(AnnotationTypeNLP.COREF.toHtml(corefSpan.getSecond()));
					htmlBuilder.append("</div>\n");
				}
			}
		}

		if (this.otherSentenceAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, ?>> entry : this.otherSentenceAnnotations.entrySet()) {
				if (annotationTypes != null && !annotationTypes.contains(entry.getKey()))
					continue;
			
				for (Entry<Integer, ?> sentenceEntry : entry.getValue().entrySet()) {
					@SuppressWarnings("rawtypes")
					Pair pair = (Pair)sentenceEntry.getValue();
					
					int sentenceId = sentenceEntry.getKey();
					int sentenceStart = tokens[sentenceId][0].getCharSpanStart();
					int sentenceEnd = tokens[sentenceId][tokens[sentenceId].length - 1].getCharSpanEnd();
					htmlBuilder.append("<div class=\"annotation\"");
					htmlBuilder.append(" spanStart=\"" + sentenceStart + "\"");
					htmlBuilder.append(" spanEnd=\"" + sentenceEnd + "\"");
					htmlBuilder.append(">\n");
					htmlBuilder.append("Type: " + entry.getKey().toString());
                                        htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", pair.getSecond()));
					htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + entry.getKey().toHtml(pair.getFirst()));
					htmlBuilder.append("</div>\n");
				}
			}
		}

		if (this.otherTokenSpanAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>> entry : this.otherTokenSpanAnnotations.entrySet()) {
				if (annotationTypes != null && !annotationTypes.contains(entry.getKey())) {
					continue;
				}
		
				for (Entry<Integer, List<Triple<TokenSpan, ?, Double>>> sentenceEntry : entry.getValue().entrySet()) {
					for (Triple<TokenSpan, ?, Double> span : sentenceEntry.getValue()) {
						int sentenceId = sentenceEntry.getKey();
						int spanStart = tokens[sentenceId][span.getFirst().getStartTokenIndex()].getCharSpanStart();
						int spanEnd = tokens[sentenceId][span.getFirst().getEndTokenIndex()-1].getCharSpanEnd(); 
				
						htmlBuilder.append("<div class=\"annotation\"");
						htmlBuilder.append(" spanStart=\"" + spanStart + "\"");
						htmlBuilder.append(" spanEnd=\"" + spanEnd + "\"");
						htmlBuilder.append(">\n");
						htmlBuilder.append("Type: " + entry.getKey().toString());
                                                htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", span.getThird()));
						htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + entry.getKey().toHtml(span.getSecond()));
						htmlBuilder.append("</div>\n");
					}
				}
			}
		}

		if (this.otherTokenAnnotations != null) {
			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : this.otherTokenAnnotations.entrySet()) {
				if (annotationTypes != null && !annotationTypes.contains(entry.getKey()))
					continue;

				Pair<?, Double>[][] anno = entry.getValue();
				for (int i = 0; i < anno.length; i++) {
					for (int j = 0; j < anno[i].length; j++) {
						int spanStart = tokens[i][j].getCharSpanStart();
						int spanEnd = tokens[i][j].getCharSpanEnd();
						
						htmlBuilder.append("<div class=\"annotation\"");
						htmlBuilder.append(" spanStart=\"" + spanStart + "\"");
						htmlBuilder.append(" spanEnd=\"" + spanEnd + "\"");
						htmlBuilder.append(">\n");
						htmlBuilder.append("Type: " + entry.getKey().toString());
                                                htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Score: " + String.format("%.04f", anno[i][j].getSecond()));
						htmlBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;Value: " + entry.getKey().toHtml(anno[i][j].getFirst()));
						htmlBuilder.append("</div>\n");
					}
				}
			}
		}
		htmlBuilder.append("</ul></div>\n");
		endHtml(htmlBuilder);
		htmlBuilder.append("</body></html>");

		return htmlBuilder.toString();
	}

	private void beginHtml(StringBuilder htmlBuilder) {
		htmlBuilder.append("<html>");
		htmlBuilder.append("<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.11.2/jquery.min.js\"></script>");
		htmlBuilder.append("<body>");
	}

	private void endHtml(StringBuilder htmlBuilder) {
		// Add some javascript at the end of the body.  Pretty ugly to be developing javascript here,
		// but it's either this or making an html page that isn't stand-alone...
		htmlBuilder.append("<script type=\"text/javascript\">\n");
		htmlBuilder.append("//# sourceURL=annotations.js\n");
		htmlBuilder.append("var text = $(\".text\").html()\n");
		htmlBuilder.append("$(document).ready(function() {\n");
		htmlBuilder.append("  $(\".annotation\").hover(function() {\n");
		htmlBuilder.append("    var spanStart = $(this).attr(\"spanStart\");\n");
		htmlBuilder.append("    var spanEnd = $(this).attr(\"spanEnd\");\n");
		htmlBuilder.append("    highlightSpan([spanStart, spanEnd]);\n");
		htmlBuilder.append("  },\n");
		htmlBuilder.append("  removeHighlight);\n");
		htmlBuilder.append("});\n");
		htmlBuilder.append("function highlightSpan(span) {\n");
		htmlBuilder.append("  var before = text.slice(0, span[0]);\n");
		htmlBuilder.append("  var after = text.slice(span[1]);\n");
		htmlBuilder.append("  var to_highlight = text.slice(span[0], span[1]);\n");
		htmlBuilder.append("  var highlighted = before + '<span class=\"highlight\">' + to_highlight + '</span>' + after;\n");
		htmlBuilder.append("  $(\".text\").html(highlighted);\n");
		htmlBuilder.append("}\n");
		htmlBuilder.append("function removeHighlight() {\n");
		htmlBuilder.append("  $(\".text\").html(text);\n");
		htmlBuilder.append("}\n");
		htmlBuilder.append("</script>\n");
		// And some simple CSS so that the highlight shows up.
		htmlBuilder.append("<style>.highlight { background-color: red }</style>\n");
		// And then close the html document.
		htmlBuilder.append("</body></html>");
	}

	@Override
	public int getSentenceCount() {
		return this.tokens.length;
	}
	
	@Override
	public int getSentenceTokenCount(int sentenceIndex) {
		return this.tokens[sentenceIndex].length;
	}
	
	@Override
	public String getOriginalText() {
		return this.originalText;
	}
	
	@Override
	public String getText() {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < getSentenceCount(); i++)
			text = text.append(getSentence(i)).append(" ");
		return text.toString().trim();
	}
	
	@Override
	public String getSentence(int sentenceIndex) {
		StringBuilder sentenceStr = new StringBuilder();
		
		for (int i = 0; i < this.tokens[sentenceIndex].length; i++) {
			sentenceStr = sentenceStr.append(this.tokens[sentenceIndex][i].getStr()).append(" ");
		}
		return sentenceStr.toString().trim();
	}
	
	@Override
	public Token getToken(int sentenceIndex, int tokenIndex) {
		if (tokenIndex < 0)
			return new Token(this, "ROOT");
		else 
			return this.tokens[sentenceIndex][tokenIndex];
	}
	
	@Override
	public PoSTag getPoSTag(int sentenceIndex, int tokenIndex) {
		return this.posTags[sentenceIndex][tokenIndex];
	}
	
	@Override
	public ConstituencyParse getConstituencyParse(int sentenceIndex) {
		return this.constituencyParses[sentenceIndex];
	}

	@Override
	public DependencyParse getDependencyParse(int sentenceIndex) {
		return this.dependencyParses[sentenceIndex];
	}
	
	@Override
	public Language getLanguage() {
		return this.language;
	}

	@Override
	public List<Pair<TokenSpan, String>> getNer(TokenSpan tokenSpan,
			Relation[] relationToAnnotations) {
		List<Triple<TokenSpan, String, Double>> sentenceNer = this.ner.get(tokenSpan.getSentenceIndex());
		List<Pair<TokenSpan, String>> retNer = new ArrayList<Pair<TokenSpan, String>>();
		if (sentenceNer == null)
			return retNer;
		
		for (Pair<TokenSpan, String> span : sentenceNer) {
			TokenSpan.Relation relation = tokenSpan.getRelationTo(span.getFirst());
			if (Arrays.asList(relationToAnnotations).contains(relation))
				retNer.add(span);
		}
		
		return retNer;
	}

	@Override
	public List<Pair<TokenSpan, TokenSpanCluster>> getCoref(
			TokenSpan tokenSpan, Relation[] relationToAnnotations) {
		List<Triple<TokenSpan, TokenSpanCluster, Double>> sentenceCoref = this.coref.get(tokenSpan.getSentenceIndex());
		List<Pair<TokenSpan, TokenSpanCluster>> retCoref = new ArrayList<Pair<TokenSpan, TokenSpanCluster>>();
		if (sentenceCoref == null)
			return retCoref;
		
		for (Pair<TokenSpan, TokenSpanCluster> span : sentenceCoref) {
			TokenSpan.Relation relation = tokenSpan.getRelationTo(span.getFirst());
			if (Arrays.asList(relationToAnnotations).contains(relation))
				retCoref.add(span);
		}
		
		return retCoref;
	}

	@Override
	public String getAnnotatorName(AnnotationType<?> annotationType) {
		if (this.otherAnnotatorNames != null && this.otherAnnotatorNames.containsKey(annotationType))
			return this.otherAnnotatorNames.get(annotationType);
		
		if (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT))
			return this.originalTextAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.LANGUAGE))
			return this.languageAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.TOKEN))
			return this.tokenAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.SENTENCE))
			return this.tokenAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.POS))
			return this.posAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE))
			return this.dependencyParseAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE))
			return this.constituencyParseAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.NER))
			return this.nerAnnotatorName;
		else if (annotationType.equals(AnnotationTypeNLP.COREF))
			return this.corefAnnotatorName;
		
		return null;
	}

	@Override
	public boolean hasAnnotationType(AnnotationType<?> annotationType) {
		return ((this.otherDocumentAnnotations != null) && this.otherDocumentAnnotations.containsKey(annotationType))
				|| ((this.otherSentenceAnnotations != null) && this.otherSentenceAnnotations.containsKey(annotationType))
				|| ((this.otherTokenSpanAnnotations != null) && this.otherTokenSpanAnnotations.containsKey(annotationType))
				|| ((this.otherTokenAnnotations != null) && this.otherTokenAnnotations.containsKey(annotationType))
				|| (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT) && this.originalText != null)
				|| (annotationType.equals(AnnotationTypeNLP.LANGUAGE) && this.language != null)
				|| (annotationType.equals(AnnotationTypeNLP.TOKEN) && this.tokens != null)
				|| (annotationType.equals(AnnotationTypeNLP.SENTENCE) && this.tokens != null)
				|| (annotationType.equals(AnnotationTypeNLP.POS) && this.posTags != null)
				|| (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE) && this.dependencyParses != null)
				|| (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE) && this.constituencyParses != null)
				|| (annotationType.equals(AnnotationTypeNLP.NER) && this.ner != null)
				|| (annotationType.equals(AnnotationTypeNLP.COREF) && this.coref != null);
	}

	@Override
	public Collection<AnnotationType<?>> getAnnotationTypes() {
		List<AnnotationType<?>> annotationTypes = new ArrayList<AnnotationType<?>>();
		
		if (this.otherDocumentAnnotations != null)
			annotationTypes.addAll(this.otherDocumentAnnotations.keySet());
		if (this.otherSentenceAnnotations != null)
			annotationTypes.addAll(this.otherSentenceAnnotations.keySet());
		if (this.otherTokenSpanAnnotations != null)
			annotationTypes.addAll(this.otherTokenSpanAnnotations.keySet());
		if (this.otherTokenAnnotations != null)
			annotationTypes.addAll(this.otherTokenAnnotations.keySet());
		
		if (this.tokens != null) {
			annotationTypes.add(AnnotationTypeNLP.TOKEN);
			annotationTypes.add(AnnotationTypeNLP.SENTENCE);
		}
		
		if (this.originalText != null)
			annotationTypes.add(AnnotationTypeNLP.ORIGINAL_TEXT);
		if (this.language != null)
			annotationTypes.add(AnnotationTypeNLP.LANGUAGE);
		if (this.posTags != null)
			annotationTypes.add(AnnotationTypeNLP.POS);
		if (this.constituencyParses != null)
			annotationTypes.add(AnnotationTypeNLP.CONSTITUENCY_PARSE);
		if (this.dependencyParses != null)
			annotationTypes.add(AnnotationTypeNLP.DEPENDENCY_PARSE);
		if (this.ner != null)
			annotationTypes.add(AnnotationTypeNLP.NER);
		if (this.coref != null)
			annotationTypes.add(AnnotationTypeNLP.COREF);
		
		return annotationTypes;
	}
	
	@Override
	public <T> T getDocumentAnnotation(AnnotationTypeNLP<T> annotationType) {		
		T anno = super.getDocumentAnnotation(annotationType);
		if (anno != null)
			return anno;
		return annotationType.getAnnotationClass().cast(this.otherDocumentAnnotations.get(annotationType).getFirst());
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public <T> T getSentenceAnnotation(AnnotationTypeNLP<T> annotationType, int sentenceIndex) {
		T anno = super.getSentenceAnnotation(annotationType, sentenceIndex);
		if (anno != null)
			return anno;
		
		Map<Integer, ?> sentenceAnnotation = this.otherSentenceAnnotations.get(annotationType);	
		if (!sentenceAnnotation.containsKey(sentenceIndex))
			return null;
	
		return annotationType.getAnnotationClass().cast(((Pair)sentenceAnnotation.get(sentenceIndex)).getFirst());
	}
	
	@Override
	public <T> List<Pair<TokenSpan, T>> getTokenSpanAnnotations(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations) {
		List<Pair<TokenSpan, T>> anno = super.getTokenSpanAnnotations(annotationType, tokenSpan, relationsToAnnotations);
		if (anno != null)
			return anno;
		List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotation = this.otherTokenSpanAnnotations.get(annotationType).get(tokenSpan.getSentenceIndex());
		if (tokenSpanAnnotation == null)
			return new ArrayList<Pair<TokenSpan, T>>();
		anno = new ArrayList<Pair<TokenSpan, T>>();
		for (Pair<TokenSpan, ?> span : tokenSpanAnnotation)
			anno.add(new Pair<TokenSpan, T>(span.getFirst(), annotationType.getAnnotationClass().cast(span.getSecond())));
		return anno;
	}
	
	@Override
	public <T> T getTokenAnnotation(AnnotationTypeNLP<T> annotationType, int sentenceIndex, int tokenIndex) {	
		T anno = super.getTokenAnnotation(annotationType, sentenceIndex, tokenIndex);
		if (anno != null)
			return anno;
		
		return annotationType.getAnnotationClass().cast(this.otherTokenAnnotations.get(annotationType)[sentenceIndex][tokenIndex].getFirst());
	}
	
	@Override
	public Double getDocumentAnnotationConfidence(AnnotationTypeNLP<?> annotationType) {		
		Double annoConf = super.getDocumentAnnotationConfidence(annotationType);
		if (annoConf != null)
			return annoConf;
		return this.otherDocumentAnnotations.get(annotationType).getSecond();
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Double getSentenceAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex) {
		Double annoConf = super.getSentenceAnnotationConfidence(annotationType, sentenceIndex);
		if (annoConf != null)
			return annoConf;
		
		Map<Integer, ?> sentenceAnnotation = this.otherSentenceAnnotations.get(annotationType);	
		if (!sentenceAnnotation.containsKey(sentenceIndex))
			return null;
		
		return (Double)(((Pair)sentenceAnnotation.get(sentenceIndex)).getSecond());
	}
	
	@Override
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations) {
		List<Triple<TokenSpan, T, Double>> anno = super.getTokenSpanAnnotationConfidences(annotationType, tokenSpan, relationsToAnnotations);
		if (anno != null)
			return anno;
		List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotation = this.otherTokenSpanAnnotations.get(annotationType).get(tokenSpan.getSentenceIndex());
		if (tokenSpanAnnotation == null)
			return new ArrayList<Triple<TokenSpan, T, Double>>(); 
		
		anno = new ArrayList<Triple<TokenSpan, T, Double>>();
		for (Triple<TokenSpan, ?, Double> span : tokenSpanAnnotation)
			anno.add(new Triple<TokenSpan, T, Double>(span.getFirst(), annotationType.getAnnotationClass().cast(span.getSecond()), span.getThird()));
		return anno;
	}
	
	@Override
	public Double getTokenAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex, int tokenIndex) {	
		Double anno = super.getTokenAnnotationConfidence(annotationType, sentenceIndex, tokenIndex);
		if (anno != null)
			return anno;

		return this.otherTokenAnnotations.get(annotationType)[sentenceIndex][tokenIndex].getSecond();
	}

	@Override
	public Double getOriginalTextConfidence() {
		return this.originalTextConf;
	}

	@Override
	public Double getLanguageConfidence() {
		return this.languageConf;
	}

	@Override
	public Double getSentenceConfidence(int sentenceIndex) {
		return null;
	}

	@Override
	public Double getTokenConfidence(int sentenceIndex, int tokenIndex) {
		return this.tokensConf[sentenceIndex][tokenIndex];
	}

	@Override
	public Double getPoSTagConfidence(int sentenceIndex, int tokenIndex) {
		return this.posTagsConf[sentenceIndex][tokenIndex];
	}

	@Override
	public Double getConstituencyParseConfidence(int sentenceIndex) {
		return this.constituencyParsesConf[sentenceIndex];
	}

	@Override
	public Double getDependencyParseConfidence(int sentenceIndex) {
		return this.dependencyParsesConf[sentenceIndex];
	}

	@Override
	public List<Triple<TokenSpan, String, Double>> getNerWithConfidence(
			TokenSpan tokenSpan, Relation[] relationsToAnnotations) {
		List<Triple<TokenSpan, String, Double>> sentenceNer = this.ner.get(tokenSpan.getSentenceIndex());
		List<Triple<TokenSpan, String, Double>> retNer = new ArrayList<Triple<TokenSpan, String, Double>>();
		if (sentenceNer == null)
			return retNer;
		
		for (Triple<TokenSpan, String, Double> span : sentenceNer) {
			TokenSpan.Relation relation = tokenSpan.getRelationTo(span.getFirst());
			if (Arrays.asList(relationsToAnnotations).contains(relation))
				retNer.add(span);
		}
		
		return retNer;
	}

	@Override
	public List<Triple<TokenSpan, TokenSpanCluster, Double>> getCorefWithConfidence(
			TokenSpan tokenSpan, Relation[] relationsToAnnotations) {
		List<Triple<TokenSpan, TokenSpanCluster, Double>> sentenceCoref = this.coref.get(tokenSpan.getSentenceIndex());
		List<Triple<TokenSpan, TokenSpanCluster, Double>> retCoref = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
		if (sentenceCoref == null)
			return retCoref;
		
		for (Triple<TokenSpan, TokenSpanCluster, Double> span : sentenceCoref) {
			TokenSpan.Relation relation = tokenSpan.getRelationTo(span.getFirst());
			if (Arrays.asList(relationsToAnnotations).contains(relation))
				retCoref.add(span);
		}
		
		return retCoref;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean hasConfidence(AnnotationType<?> annotationType) {
		return (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT) && this.originalTextConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.LANGUAGE) && this.languageConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.TOKEN) && this.tokensConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.POS) && this.posTagsConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE) && this.dependencyParsesConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE) && this.constituencyParsesConf != null)
			|| (annotationType.equals(AnnotationTypeNLP.NER) && (this.ner.size() == 0 || this.ner.values().iterator().next().get(0).getThird() != null))
			|| (annotationType.equals(AnnotationTypeNLP.COREF) && (this.coref.size() == 0 || this.coref.values().iterator().next().get(0).getThird() != null))
			|| (this.otherDocumentAnnotations != null && this.otherDocumentAnnotations.containsKey(annotationType) && this.otherDocumentAnnotations.get(annotationType).getSecond() != null)
			|| (this.otherSentenceAnnotations != null && this.otherSentenceAnnotations.containsKey(annotationType) && this.otherSentenceAnnotations.get(annotationType).size() > 0 && ((Pair)this.otherSentenceAnnotations.get(annotationType).values().iterator().next()).getSecond() != null)
			|| (this.otherTokenSpanAnnotations != null && this.otherTokenSpanAnnotations.containsKey(annotationType) && this.otherTokenSpanAnnotations.get(annotationType).size() > 0 && this.otherTokenSpanAnnotations.get(annotationType).values().iterator().next().get(0).getThird() != null)
			|| (this.otherTokenAnnotations != null && this.otherTokenAnnotations.containsKey(annotationType) && this.otherTokenAnnotations.get(annotationType).length > 0 && this.otherTokenAnnotations.get(annotationType)[0][0].getSecond() != null);

	}

	@Override
	public Document makeInstanceFromText(String name, String text, Language language, PipelineNLP pipeline,
			Collection<AnnotationTypeNLP<?>> skipAnnotators, boolean keepOriginalText) {
		return new DocumentNLPInMemory(this.dataTools, name, text, language, pipeline, skipAnnotators, keepOriginalText);
	}
}
