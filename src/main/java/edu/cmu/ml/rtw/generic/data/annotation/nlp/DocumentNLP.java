package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.Document;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.model.annotator.nlp.PipelineNLP;
import edu.cmu.ml.rtw.generic.util.FileUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

/**
 * 
 * DocumentNLP represents a JSON-serializable text document with 
 * various NLP annotations (e.g. PoS tags, parses, etc).  
 * The methods
 * for getting the NLP annotations are kept abstract so 
 * that they can be implemented in ways that allow for
 * caching in cases when all of the documents don't fit
 * in memory.  In-memory implementations of these methods
 * are given by the 
 * edu.cmu.ml.rtw.generic.data.annotation.DocumentNLPInMemory 
 * class.
 * 
 * All lists of sentences, tokens, and token spans in a 
 * document are 0-indexed.
 * 
 * @author Bill McDowell
 *
 */
public abstract class DocumentNLP extends Document {
	public DocumentNLP(DataTools dataTools) {
		super(dataTools);
	}
	
	public DocumentNLP(DataTools dataTools, JSONObject json) {
		this(dataTools);
		fromJSON(json);
	}
	
	public DocumentNLP(DataTools dataTools, DocumentAnnotation documentAnnotation) {
		this(dataTools);
		fromMicroAnnotation(documentAnnotation);
	}
	
	public DocumentNLP(DataTools dataTools, String jsonPath) {
		this(dataTools);
		BufferedReader r = FileUtil.getFileReader(jsonPath);
		String line = null;
		StringBuffer lines = new StringBuffer();
		try {
			while ((line = r.readLine()) != null) {
				lines.append(line).append("\n");
			}
			
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			if (!fromJSON(new JSONObject(lines.toString())))
				throw new IllegalArgumentException();
		} catch (JSONException e) {
			throw new IllegalArgumentException();
		}
	}
	
	public List<String> getSentenceTokenStrs(int sentenceIndex) {
		int sentenceTokenCount = getSentenceTokenCount(sentenceIndex);
		List<String> sentenceTokens = new ArrayList<String>(sentenceTokenCount);
		for (int i = 0; i < sentenceTokenCount; i++)
			sentenceTokens.add(getTokenStr(sentenceIndex, i));
		return sentenceTokens;
	}
	
	public List<PoSTag> getSentencePoSTags(int sentenceIndex) {
		int sentenceTokenCount = getSentenceTokenCount(sentenceIndex);
		List<PoSTag> sentencePoSTags = new ArrayList<PoSTag>(sentenceTokenCount);
		for (int i = 0; i < sentenceTokenCount; i++)
			sentencePoSTags.add(getPoSTag(sentenceIndex, i));
		return sentencePoSTags;
	}
	
	public String getTokenStr(int sentenceIndex, int tokenIndex) {
		return getToken(sentenceIndex, tokenIndex).getStr();
	}
	
	public List<Pair<TokenSpan, String>> getNer(TokenSpan tokenSpan) {
		return getNer(tokenSpan, TokenSpan.ANY_SHARING_RELATION);
	}
	
	public List<Pair<TokenSpan, String>> getNer() {
		List<Pair<TokenSpan, String>> ner = new ArrayList<Pair<TokenSpan, String>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			ner.addAll(getNer(i));
		}
		return ner;
	}
	
	public List<Pair<TokenSpan, String>> getNer(int sentenceIndex) {
		return getNer(new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}
	
	public List<Triple<TokenSpan, String, Double>> getNerWithConfidence() {
		List<Triple<TokenSpan, String, Double>> ner = new ArrayList<Triple<TokenSpan, String, Double>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			ner.addAll(getNerWithConfidence(i));
		}
		return ner;
	}
	
	public List<Triple<TokenSpan, String, Double>> getNerWithConfidence(int sentenceIndex) {
		return getNerWithConfidence(new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}
	
	public List<Pair<TokenSpan, TokenSpanCluster>> getCoref(TokenSpan tokenSpan) {
		return getCoref(tokenSpan, TokenSpan.ANY_SHARING_RELATION);
	}
	
	public List<Pair<TokenSpan, TokenSpanCluster>> getCoref() {
		List<Pair<TokenSpan, TokenSpanCluster>> coref = new ArrayList<Pair<TokenSpan, TokenSpanCluster>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			coref.addAll(getCoref(i));
		}
		return coref;
	}
	
	public List<Pair<TokenSpan, TokenSpanCluster>> getCoref(int sentenceIndex) {
		return getCoref(new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}
	
	public List<Triple<TokenSpan, TokenSpanCluster, Double>> getCorefWithConfidence() {
		List<Triple<TokenSpan, TokenSpanCluster, Double>> coref = new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			coref.addAll(getCorefWithConfidence(i));
		}
		return coref;
	}
	
	public List<Triple<TokenSpan, TokenSpanCluster, Double>> getCorefWithConfidence(int sentenceIndex) {
		return getCorefWithConfidence(new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}

	public Double getDocumentAnnotationConfidence(AnnotationTypeNLP<?> annotationType) {
		if (annotationType.equals(AnnotationTypeNLP.LANGUAGE))
			return getLanguageConfidence();
		else if (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT))
			return getOriginalTextConfidence();
		else
			return null;
	}
	
	public <T> T getDocumentAnnotation(AnnotationTypeNLP<T> annotationType) {
		if (annotationType.equals(AnnotationTypeNLP.LANGUAGE))
			return annotationType.getAnnotationClass().cast(getLanguage());
		else if (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT))
			return annotationType.getAnnotationClass().cast(getOriginalText());
		else
			return null;
	}
	
	public Double getSentenceAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex) {
		if (annotationType.equals(AnnotationTypeNLP.SENTENCE)) {
			return getSentenceConfidence(sentenceIndex);
		} else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			return getConstituencyParseConfidence(sentenceIndex);
		} else if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			return getDependencyParseConfidence(sentenceIndex);
		} else {
			return null;
		}
	}
	
	public <T> T getSentenceAnnotation(AnnotationTypeNLP<T> annotationType, int sentenceIndex) {
		if (annotationType.equals(AnnotationTypeNLP.SENTENCE)) {
			return annotationType.getAnnotationClass().cast(getSentence(sentenceIndex));
		} else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			return annotationType.getAnnotationClass().cast(getConstituencyParse(sentenceIndex));
		} else if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			return annotationType.getAnnotationClass().cast(getDependencyParse(sentenceIndex));
		} else {
			return null;
		}
	}

	public <T> List<Pair<TokenSpan, T>> getTokenSpanAnnotations(AnnotationTypeNLP<T> annotationType) {
		List<Pair<TokenSpan, T>> annotations = new ArrayList<Pair<TokenSpan, T>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			
			List<Pair<TokenSpan, T>> sentenceAnnotations = getTokenSpanAnnotations(annotationType, i);
			if (sentenceAnnotations != null)
				annotations.addAll(sentenceAnnotations);
		}
		return annotations;
	}
	
	public <T> List<Pair<TokenSpan, T>> getTokenSpanAnnotations(AnnotationTypeNLP<T> annotationType, int sentenceIndex) {
		return getTokenSpanAnnotations(annotationType, new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}
	
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType) {
		List<Triple<TokenSpan, T, Double>> annotations = new ArrayList<Triple<TokenSpan, T, Double>>();
		for (int i = 0; i < getSentenceCount(); i++) {
			if (getSentenceTokenCount(i) == 0)
				continue;
			List<Triple<TokenSpan, T, Double>> sentenceAnnotations = getTokenSpanAnnotationConfidences(annotationType, i);
			if (sentenceAnnotations != null)
				annotations.addAll(sentenceAnnotations);
		}
		return annotations;
	}
	
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType, int sentenceIndex) {
		return getTokenSpanAnnotationConfidences(annotationType, new TokenSpan(this, sentenceIndex, 0, 1), TokenSpan.ANY_CLOSE_RELATION);
	}
	
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan) {
		return getTokenSpanAnnotationConfidences(annotationType, tokenSpan, TokenSpan.ANY_SHARING_RELATION);
	}
	
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations) {
		if (annotationType.equals(AnnotationTypeNLP.NER)) {
			List<Triple<TokenSpan, String, Double>> ner = getNerWithConfidence(tokenSpan, relationsToAnnotations);
			List<Triple<TokenSpan, T, Double>> cast = new ArrayList<Triple<TokenSpan, T, Double>>(ner.size() * 2);
			for (Triple<TokenSpan, String, Double> nerSpan : ner)
				cast.add(new Triple<TokenSpan, T, Double>(nerSpan.getFirst(), annotationType.getAnnotationClass().cast(nerSpan.getSecond()), nerSpan.getThird()));
			return cast;
		} else if (annotationType.equals(AnnotationTypeNLP.COREF)) {
			List<Triple<TokenSpan, TokenSpanCluster, Double>> coref = getCorefWithConfidence(tokenSpan, relationsToAnnotations);
			List<Triple<TokenSpan, T, Double>> cast = new ArrayList<Triple<TokenSpan, T, Double>>(coref.size() * 2);
			for (Triple<TokenSpan, TokenSpanCluster, Double> corefSpan : coref)
				cast.add(new Triple<TokenSpan, T, Double>(corefSpan.getFirst(), annotationType.getAnnotationClass().cast(corefSpan.getSecond()), corefSpan.getThird()));
			return cast;
		} else {
			return null;
		}
	}
	
	public <T> List<Pair<TokenSpan, T>> getTokenSpanAnnotations(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan) {
		return getTokenSpanAnnotations(annotationType, tokenSpan, TokenSpan.ANY_SHARING_RELATION);
	}
	
	public <T> List<Pair<TokenSpan, T>> getTokenSpanAnnotations(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations) {
		if (annotationType.equals(AnnotationTypeNLP.NER)) {
			List<Pair<TokenSpan, String>> ner = getNer(tokenSpan, relationsToAnnotations);
			List<Pair<TokenSpan, T>> cast = new ArrayList<Pair<TokenSpan, T>>(ner.size() * 2);
			for (Pair<TokenSpan, String> nerSpan : ner)
				cast.add(new Pair<TokenSpan, T>(nerSpan.getFirst(), annotationType.getAnnotationClass().cast(nerSpan.getSecond())));
			return cast;
		} else if (annotationType.equals(AnnotationTypeNLP.COREF)) {
			List<Pair<TokenSpan, TokenSpanCluster>> coref = getCoref(tokenSpan, relationsToAnnotations);
			List<Pair<TokenSpan, T>> cast = new ArrayList<Pair<TokenSpan, T>>(coref.size() * 2);
			for (Pair<TokenSpan, TokenSpanCluster> corefSpan : coref)
				cast.add(new Pair<TokenSpan, T>(corefSpan.getFirst(), annotationType.getAnnotationClass().cast(corefSpan.getSecond())));
			return cast;
		} else {
			return null;
		}
	}
	
	public Double getTokenAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex, int tokenIndex) {	
		if (annotationType.equals(AnnotationTypeNLP.TOKEN)) {
			return getTokenConfidence(sentenceIndex, tokenIndex);
		} else if (annotationType.equals(AnnotationTypeNLP.POS)) {
			return getPoSTagConfidence(sentenceIndex, tokenIndex);
		} else {
			return null;
		}
	}
	
	public <T> T getTokenAnnotation(AnnotationTypeNLP<T> annotationType, int sentenceIndex, int tokenIndex) {	
		if (annotationType.equals(AnnotationTypeNLP.TOKEN)) {
			return annotationType.getAnnotationClass().cast(getToken(sentenceIndex, tokenIndex));
		} else if (annotationType.equals(AnnotationTypeNLP.POS)) {
			return annotationType.getAnnotationClass().cast(getPoSTag(sentenceIndex, tokenIndex));
		} else {
			return null;
		}
	}
	
	public DocumentAnnotation toMicroAnnotation() {
		return toMicroAnnotation(this.dataTools.getAnnotationTypesNLP());
	}
	
	public abstract String getOriginalText();
	public abstract Language getLanguage();
	public abstract int getSentenceCount();
	public abstract int getSentenceTokenCount(int sentenceIndex);
	public abstract String getText();
	public abstract String getSentence(int sentenceIndex);
	public abstract Token getToken(int sentenceIndex, int tokenIndex);
	public abstract PoSTag getPoSTag(int sentenceIndex, int tokenIndex);
	public abstract ConstituencyParse getConstituencyParse(int sentenceIndex);
	public abstract DependencyParse getDependencyParse(int sentenceIndex);
	public abstract List<Pair<TokenSpan, String>> getNer(TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations);
	public abstract List<Pair<TokenSpan, TokenSpanCluster>> getCoref(TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations);
	
	public abstract Double getOriginalTextConfidence();
	public abstract Double getLanguageConfidence();
	public abstract Double getSentenceConfidence(int sentenceIndex);
	public abstract Double getTokenConfidence(int sentenceIndex, int tokenIndex);
	public abstract Double getPoSTagConfidence(int sentenceIndex, int tokenIndex);
	public abstract Double getConstituencyParseConfidence(int sentenceIndex);
	public abstract Double getDependencyParseConfidence(int sentenceIndex);
	public abstract List<Triple<TokenSpan, String, Double>> getNerWithConfidence(TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations);
	public abstract List<Triple<TokenSpan, TokenSpanCluster, Double>> getCorefWithConfidence(TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations);
	
	public abstract boolean fromMicroAnnotation(DocumentAnnotation documentAnnotation);
	public abstract Document makeInstanceFromMicroAnnotation(DocumentAnnotation documentAnnotation, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators);
	public abstract DocumentAnnotation toMicroAnnotation(Collection<AnnotationTypeNLP<?>> annotationTypes);

	public abstract Document makeInstanceFromText(String name, String text, Language language, PipelineNLP pipeline, Collection<AnnotationTypeNLP<?>> skipAnnotators);
}
