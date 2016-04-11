package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.ConstituencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DependencyParse;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.PoSTag;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.Token;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan.Relation;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
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
public class DocumentNLPInMemory extends DocumentNLPMutable {
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
		this(dataTools, null, null);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String name) {
		super(dataTools, name);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String name, String storageName, String collectionName) {
		super(dataTools, name, storageName, collectionName);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String name, String originalText) {
		this(dataTools, name, null, null, originalText);
	}
	
	public DocumentNLPInMemory(DataTools dataTools, String name, String storageName, String collectionName, String originalText) {
		super(dataTools, name, storageName, collectionName);
		this.originalText = originalText;
	}

	@SuppressWarnings("unchecked")
	public DocumentNLPInMemory(DocumentNLP document) {
		this(document.getDataTools(), 
			document.getName(), 
			(document.getStoreReference() != null) ? document.getStoreReference().getStorageName() : null, 
			(document.getStoreReference() != null) ? document.getStoreReference().getCollectionName() : null, 
			document.getOriginalText());
		
		Collection<AnnotationType<?>> annotationTypes = document.getAnnotationTypes();
		for (AnnotationType<?> annotationType : annotationTypes) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			if (annotationTypeNLP.getTarget() == Target.DOCUMENT) {
				setDocumentAnnotation(document.getAnnotatorName(annotationTypeNLP), 
											annotationTypeNLP, 
											new Pair<Object, Double>(document.getDocumentAnnotation(annotationTypeNLP), 
																	document.getDocumentAnnotationConfidence(annotationTypeNLP)));
			} else if (annotationTypeNLP.getTarget() == Target.SENTENCE) {
				Map<Integer, Pair<?, Double>> annotation = new HashMap<Integer, Pair<?, Double>>();
				int sentenceCount = document.getSentenceCount();
				for (int i = 0; i < sentenceCount; i++) {
					Object obj = document.getSentenceAnnotation(annotationTypeNLP, i);
					if (obj != null)
						annotation.put(i, new Pair<Object, Double>(obj, document.getSentenceAnnotationConfidence(annotationTypeNLP, i)));
				}
				
				setSentenceAnnotation(document.getAnnotatorName(annotationTypeNLP), annotationTypeNLP, annotation);
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN) {
				List<?> annotation = document.getTokenSpanAnnotationConfidences(annotationTypeNLP);
				setTokenSpanAnnotation(document.getAnnotatorName(annotationTypeNLP), 
						annotationTypeNLP, (List<Triple<TokenSpan, ?, Double>>)annotation);

			} else if (annotationTypeNLP.getTarget() == Target.TOKEN) {
				Pair<?, Double>[][] annotation = new Pair[document.getSentenceCount()][];
				int sentenceCount = document.getSentenceCount();
				for (int i = 0; i < sentenceCount; i++) {
					int sentenceTokenCount = document.getSentenceTokenCount(i);
					annotation[i] = new Pair[sentenceTokenCount];
					for (int j = 0; j < sentenceTokenCount; j++) {
						annotation[i][j] = new Pair<Object, Double>(
								document.getTokenAnnotation(annotationTypeNLP, i, j),
								document.getTokenAnnotationConfidence(annotationTypeNLP, i, j)
						);
					}
				}
				
				
				setTokenAnnotation(document.getAnnotatorName(annotationTypeNLP), 
						annotationTypeNLP, annotation);
			}
		}	
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
		if (this.otherTokenSpanAnnotations != null && this.otherTokenSpanAnnotations.containsKey(annotationType)) {
			List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotation = this.otherTokenSpanAnnotations.get(annotationType).get(tokenSpan.getSentenceIndex());
			if (tokenSpanAnnotation == null) 
				return Collections.emptyList();
			anno = new ArrayList<Pair<TokenSpan, T>>();
			for (Pair<TokenSpan, ?> span : tokenSpanAnnotation)
				anno.add(new Pair<TokenSpan, T>(span.getFirst(), annotationType.getAnnotationClass().cast(span.getSecond())));
			return anno;
		} else {
			return Collections.emptyList();
		}
	}
	
	@Override
	public <T> T getTokenAnnotation(AnnotationTypeNLP<T> annotationType, int sentenceIndex, int tokenIndex) {	
		T anno = super.getTokenAnnotation(annotationType, sentenceIndex, tokenIndex);
		if (anno != null)
			return anno;
		Pair<?, Double>[][] annos = this.otherTokenAnnotations.get(annotationType);
		if (annos.length <= sentenceIndex || annos[sentenceIndex].length <= tokenIndex) {
			throw new IndexOutOfBoundsException("Failed to get " + annotationType.getType() + " in " + this.getName() + " at (" + sentenceIndex + ", " + tokenIndex + ")");
		}
		
		return annotationType.getAnnotationClass().cast(annos[sentenceIndex][tokenIndex].getFirst());
	}
	
	@Override
	public Double getDocumentAnnotationConfidence(AnnotationTypeNLP<?> annotationType) {		
		Double annoConf = super.getDocumentAnnotationConfidence(annotationType);
		if (annoConf != null)
			return annoConf;
		if (this.otherDocumentAnnotations != null && this.otherDocumentAnnotations.containsKey(annotationType))
			return this.otherDocumentAnnotations.get(annotationType).getSecond();
		else 
			return null;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Double getSentenceAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex) {
		Double annoConf = super.getSentenceAnnotationConfidence(annotationType, sentenceIndex);
		if (annoConf != null)
			return annoConf;
		
		if (this.otherSentenceAnnotations != null && this.otherSentenceAnnotations.containsKey(annotationType)) {
			Map<Integer, ?> sentenceAnnotation = this.otherSentenceAnnotations.get(annotationType);	
			if (!sentenceAnnotation.containsKey(sentenceIndex))
				return null;
			
			return (Double)(((Pair)sentenceAnnotation.get(sentenceIndex)).getSecond());
		} else {
			return null;
		}
	}
	
	@Override
	public <T> List<Triple<TokenSpan, T, Double>> getTokenSpanAnnotationConfidences(AnnotationTypeNLP<T> annotationType, TokenSpan tokenSpan, TokenSpan.Relation[] relationsToAnnotations) {
		List<Triple<TokenSpan, T, Double>> anno = super.getTokenSpanAnnotationConfidences(annotationType, tokenSpan, relationsToAnnotations);
		if (anno != null)
			return anno;
		
		if (this.otherTokenSpanAnnotations != null && this.otherTokenSpanAnnotations.containsKey(annotationType)) {
			List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotation = this.otherTokenSpanAnnotations.get(annotationType).get(tokenSpan.getSentenceIndex());
			if (tokenSpanAnnotation == null)
				return new ArrayList<Triple<TokenSpan, T, Double>>(); 
			
			anno = new ArrayList<Triple<TokenSpan, T, Double>>();
			for (Triple<TokenSpan, ?, Double> span : tokenSpanAnnotation)
				anno.add(new Triple<TokenSpan, T, Double>(span.getFirst(), annotationType.getAnnotationClass().cast(span.getSecond()), span.getThird()));
			return anno;
		} else {
			return null;
		}
	}
	
	@Override
	public Double getTokenAnnotationConfidence(AnnotationTypeNLP<?> annotationType, int sentenceIndex, int tokenIndex) {	
		Double anno = super.getTokenAnnotationConfidence(annotationType, sentenceIndex, tokenIndex);
		if (anno != null)
			return anno;
		
		if (this.otherTokenAnnotations != null && this.otherTokenAnnotations.containsKey(annotationType)) {
			return this.otherTokenAnnotations.get(annotationType)[sentenceIndex][tokenIndex].getSecond();
		} else {
			return null;
		}
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
		if (this.tokensConf == null)
			return null;
		return this.tokensConf[sentenceIndex][tokenIndex];
	}

	@Override
	public Double getPoSTagConfidence(int sentenceIndex, int tokenIndex) {
		if (this.posTagsConf == null)
			return null;
		return this.posTagsConf[sentenceIndex][tokenIndex];
	}

	@Override
	public Double getConstituencyParseConfidence(int sentenceIndex) {
		if (this.constituencyParsesConf == null)
			return null;
		return this.constituencyParsesConf[sentenceIndex];
	}

	@Override
	public Double getDependencyParseConfidence(int sentenceIndex) {
		if (this.dependencyParsesConf == null)
			return null;
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
	public DocumentNLPMutable makeInstance(String name) {
		return new DocumentNLPInMemory(this.dataTools, name);
	}
	
	@Override
	public DocumentNLPMutable makeInstance(StoreReference storeReference) {
		return new DocumentNLPInMemory(this.dataTools, 
									   storeReference.getIndexValues().get(0).toString(),
									   storeReference.getStorageName(), 
									   storeReference.getCollectionName()
									   );
	}

	@Override
	public boolean setDocumentAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Pair<?, Double> annotation) {
		if (annotationType.equals(AnnotationTypeNLP.LANGUAGE)) {
			this.languageAnnotatorName = annotator;
			this.language = (Language)annotation.getFirst();
			this.languageConf = annotation.getSecond();
		} else if (annotationType.equals(AnnotationTypeNLP.ORIGINAL_TEXT)) {
			this.originalTextAnnotatorName = annotator;
			this.originalText = annotation.getFirst().toString();
			this.originalTextConf = annotation.getSecond();
		} else {
			if (this.otherAnnotatorNames == null)
				this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			this.otherAnnotatorNames.put(annotationType, annotator);
			if (this.otherDocumentAnnotations == null)
				this.otherDocumentAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>>();
			this.otherDocumentAnnotations.put(annotationType, annotation);
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean setSentenceAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Map<Integer, ?> annotation) {
		if (annotationType.equals(AnnotationTypeNLP.DEPENDENCY_PARSE)) {
			this.dependencyParseAnnotatorName = annotator;
			this.dependencyParses = new DependencyParse[this.tokens.length];
			for (int i = 0; i < this.tokens.length; i++)
				if (annotation.containsKey(i)) {
					Pair<DependencyParse, Double> depAnno = (Pair<DependencyParse, Double>)annotation.get(i);
					this.dependencyParses[i] = depAnno.getFirst();
					if (depAnno.getSecond() != null) {
						if (this.dependencyParsesConf == null)
							this.dependencyParsesConf = new double[this.tokens.length];
						this.dependencyParsesConf[i] = depAnno.getSecond();
					}
				}
		} else if (annotationType.equals(AnnotationTypeNLP.CONSTITUENCY_PARSE)) {
			this.constituencyParseAnnotatorName = annotator;
			this.constituencyParses = new ConstituencyParse[this.tokens.length];
			this.constituencyParsesConf = new double[this.tokens.length];
			for (int i = 0; i < this.tokens.length; i++)
				if (annotation.containsKey(i)) {
					Pair<ConstituencyParse, Double> conAnno = (Pair<ConstituencyParse, Double>)annotation.get(i);
					this.constituencyParses[i] = conAnno.getFirst();
					if (conAnno.getSecond() != null) {
						if (this.constituencyParsesConf == null)
							this.constituencyParsesConf = new double[this.tokens.length];
						this.constituencyParsesConf[i] = conAnno.getSecond();
					}
				}
		} else if (!annotationType.equals(AnnotationTypeNLP.SENTENCE)) {
			if (this.otherAnnotatorNames == null)
				this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			this.otherAnnotatorNames.put(annotationType, annotator);
			if (this.otherSentenceAnnotations == null)
				this.otherSentenceAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, ?>>();
			this.otherSentenceAnnotations.put(annotationType, annotation);
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean setTokenSpanAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, List<Triple<TokenSpan, ?, Double>> annotation) {
		if (annotationType.equals(AnnotationTypeNLP.NER)) {
			this.nerAnnotatorName = annotator;
			this.ner = new HashMap<Integer, List<Triple<TokenSpan, String, Double>>>();
			for (Triple<TokenSpan, ?, Double> span : annotation) {
				if (!this.ner.containsKey(span.getFirst().getSentenceIndex()))
					this.ner.put(span.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, String, Double>>());
				this.ner.get(span.getFirst().getSentenceIndex()).add((Triple<TokenSpan, String, Double>)span);
			}
		} else if (annotationType.equals(AnnotationTypeNLP.COREF)) {
			this.corefAnnotatorName = annotator;
			this.coref = new HashMap<Integer, List<Triple<TokenSpan, TokenSpanCluster, Double>>>();
			for (Triple<TokenSpan, ?, Double> span : annotation) {
				if (!this.coref.containsKey(span.getFirst().getSentenceIndex()))
					this.coref.put(span.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, TokenSpanCluster, Double>>());
				this.coref.get(span.getFirst().getSentenceIndex()).add((Triple<TokenSpan, TokenSpanCluster, Double>)span);
			}
		} else {
			if (this.otherAnnotatorNames == null)
				this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			this.otherAnnotatorNames.put(annotationType, annotator);
			
			if (this.otherTokenSpanAnnotations == null)
				this.otherTokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, Map<Integer, List<Triple<TokenSpan, ?, Double>>>>();
			Map<Integer, List<Triple<TokenSpan, ?, Double>>> annotationMap = new HashMap<Integer, List<Triple<TokenSpan, ?, Double>>>();
			for (Triple<TokenSpan, ?, Double> span : annotation) {
				if (!annotationMap.containsKey(span.getFirst().getSentenceIndex()))
					annotationMap.put(span.getFirst().getSentenceIndex(), new ArrayList<Triple<TokenSpan, ?, Double>>());
				annotationMap.get(span.getFirst().getSentenceIndex()).add(span);
			}
			this.otherTokenSpanAnnotations.put(annotationType, annotationMap);
		}
		
		return true;
	}

	@Override
	public boolean setTokenAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Pair<?, Double>[][] annotation) {
		if (annotationType.equals(AnnotationTypeNLP.TOKEN)) {
			this.tokenAnnotatorName = annotator;
			this.tokens = new Token[annotation.length][];
			boolean hasConf = false;
			if (annotation.length > 0 && annotation[0].length > 0 && annotation[0][0].getSecond() != null) {
				hasConf = true; 
				this.tokensConf = new double[annotation.length][];
			}
			
			for (int i = 0; i < this.tokens.length; i++) {
				this.tokens[i] = new Token[annotation[i].length];
				if (hasConf)
					this.tokensConf[i] = new double[annotation[i].length];
				for (int j = 0; j < this.tokens[i].length; j++) {
					this.tokens[i][j] = (Token)annotation[i][j].getFirst();
					if (hasConf)
						this.tokensConf[i][j] = annotation[i][j].getSecond();
				}
			}
		} else if (annotationType.equals(AnnotationTypeNLP.POS)) {
			this.posAnnotatorName = annotator;
			this.posTags = new PoSTag[annotation.length][];
			boolean hasConf = false;
			if (annotation.length > 0&& annotation[0].length > 0 && annotation[0][0].getSecond() != null) {
				hasConf = true; 
				this.posTagsConf = new double[annotation.length][];
			}
			
			for (int i = 0; i < this.posTags.length; i++) {
				this.posTags[i] = new PoSTag[annotation[i].length];
				if (hasConf)
					this.posTagsConf[i] = new double[annotation[i].length];
				for (int j = 0; j < this.posTags[i].length; j++) {
					this.posTags[i][j] = (PoSTag)annotation[i][j].getFirst();
					if (hasConf)
						this.posTagsConf[i][j] = annotation[i][j].getSecond();
				}
			}
		} else {
			if (this.otherAnnotatorNames == null)
				this.otherAnnotatorNames = new HashMap<AnnotationTypeNLP<?>, String>();
			
			this.otherAnnotatorNames.put(annotationType, annotator);
			
			if (this.otherTokenAnnotations == null)
				this.otherTokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
			this.otherTokenAnnotations.put(annotationType, annotation);
		}
		
		return true;
	}
}
