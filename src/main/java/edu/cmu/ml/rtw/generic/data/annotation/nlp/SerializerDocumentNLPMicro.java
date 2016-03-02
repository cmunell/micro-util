package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.joda.time.DateTime;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.Annotation;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.micro.DocumentAnnotation;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

public class SerializerDocumentNLPMicro extends SerializerDocument<DocumentNLPMutable, DocumentAnnotation> {
	public SerializerDocumentNLPMicro() {
		this(new DocumentNLPInMemory(new DataTools()), null);
	}
	
	public SerializerDocumentNLPMicro(DataTools dataTools) {
		this(new DocumentNLPInMemory(dataTools), null);
	}
	
	public SerializerDocumentNLPMicro(DocumentNLPMutable genericDocument) {
		this(genericDocument, null);
	}
	
	public SerializerDocumentNLPMicro(DocumentNLPMutable genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		super(genericDocument, annotationTypes);
	}

	@Override
	public DocumentAnnotation serialize(DocumentNLPMutable document) {
		DateTime annotationTime = DateTime.now();
		List<Annotation> annotations = new ArrayList<Annotation>();
		
		Collection<AnnotationType<?>> docAnnotationTypes = document.getAnnotationTypes();
		
		Collection<AnnotationType<?>> annotationTypes = null;
		if (this.annotationTypes == null) {
			annotationTypes = docAnnotationTypes;
		} else {
			annotationTypes = this.annotationTypes;
		}
		
		int lastCharIndex = 0;
		if (document.hasAnnotationType(AnnotationTypeNLP.TOKEN) 
				&& document.getSentenceCount() > 0 
				&& document.getSentenceTokenCount(document.getSentenceCount() - 1) > 0)
			lastCharIndex = document.getToken(document.getSentenceCount() - 1, document.getSentenceTokenCount(document.getSentenceCount() - 1)-1).getCharSpanEnd();
		
		for (AnnotationType<?> annotationType : annotationTypes) {
			if (!document.hasAnnotationType(annotationType))
				continue;
			
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			String annotatorName = document.getAnnotatorName(annotationTypeNLP);
			if (annotatorName == null)
				annotatorName = "";
			
			if (annotationTypeNLP.getTarget() == Target.DOCUMENT) {
				annotations.add(makeMicroAnnotation(
						document,
						0, 
						lastCharIndex, 
						annotationTypeNLP.getType(), 
						annotatorName, 
						annotationTime,
						annotationTypeNLP.serialize(document.getDocumentAnnotation(annotationTypeNLP)),
						document.getDocumentAnnotationConfidence(annotationTypeNLP)));
			} else if (annotationTypeNLP.getTarget() == Target.SENTENCE && document.hasAnnotationType(AnnotationTypeNLP.TOKEN)) {
				for (int i = 0; i < document.getSentenceCount(); i++) {
					Object annotationObj = document.getSentenceAnnotation(annotationTypeNLP, i);
					if (annotationObj != null) {
						Double confidence = document.getSentenceAnnotationConfidence(annotationTypeNLP, i);
						int spanStart = (document.getSentenceTokenCount(i) > 0) ? document.getToken(i, 0).getCharSpanStart() : 0;
						int spanEnd = (document.getSentenceTokenCount(i) > 0) ? document.getToken(i, document.getSentenceTokenCount(i) - 1).getCharSpanEnd() : 0;
						
						annotations.add(makeMicroAnnotation(
								document,
								spanStart, 
								spanEnd, 
								annotationType.getType(), 
								annotatorName, 
								annotationTime,
								annotationTypeNLP.serialize(annotationObj),
								confidence));
					}
				}
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN && document.hasAnnotationType(AnnotationTypeNLP.TOKEN)) {
				List<?> spanAnnotations = document.getTokenSpanAnnotationConfidences(annotationTypeNLP);
				for (Object spanObj : spanAnnotations) {
					@SuppressWarnings("unchecked")
					Triple<TokenSpan, ?, Double> annotation = (Triple<TokenSpan, ?, Double>)spanObj;
					int sentenceIndex = annotation.getFirst().getSentenceIndex();
					int startTokenIndex = annotation.getFirst().getStartTokenIndex();
					int endTokenIndex = annotation.getFirst().getEndTokenIndex() - 1;
					annotations.add(makeMicroAnnotation(
							document,
							document.getToken(sentenceIndex, startTokenIndex).getCharSpanStart(), 
							document.getToken(sentenceIndex, endTokenIndex).getCharSpanEnd(), 
							annotationType.getType(), 
							annotatorName, 
							annotationTime,
							annotationTypeNLP.serialize(annotation.getSecond()),
							annotation.getThird()));	
				}
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN && document.hasAnnotationType(AnnotationTypeNLP.TOKEN)) {
				for (int i = 0; i < document.getSentenceCount(); i++) {
					for (int j = 0; j < document.getSentenceTokenCount(i); j++) {
						annotations.add(makeMicroAnnotation(document,
															document.getToken(i,j).getCharSpanStart(), 
															document.getToken(i,j).getCharSpanEnd(), 
															annotationType.getType(),
															annotatorName, 
															annotationTime,
															annotationTypeNLP.serialize(document.getTokenAnnotation(annotationTypeNLP, i, j)),
															document.getTokenAnnotationConfidence(annotationTypeNLP, i, j)));
					}
				}
			}
		}
		
		return new DocumentAnnotation(document.getName(), annotations);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DocumentNLPMutable deserialize(DocumentAnnotation documentAnnotation, StoreReference storeReference) {
		DocumentNLPMutable document = null;
		if (storeReference != null)
			document = this.genericDocument.makeInstance(storeReference);
		else
			document = this.genericDocument.makeInstance(documentAnnotation.getDocumentId());
		
		Collection<AnnotationType<?>> annotationTypes = null;
		if (this.annotationTypes == null) {
			annotationTypes = new ArrayList<AnnotationType<?>>();
			annotationTypes.addAll(document.getDataTools().getAnnotationTypesNLP());
		} else {
			annotationTypes = this.annotationTypes;
		}
		
		List<Annotation> annotations = documentAnnotation.getAllAnnotations();
		Map<AnnotationType<?>, TreeMap<Integer, List<Annotation>>> orderedAnnotations = new HashMap<AnnotationType<?>, TreeMap<Integer, List<Annotation>>>();
		boolean hasNonDocumentAnnotations = false;
		for (Annotation annotation : annotations) {
			AnnotationType<?> annotationType = document.getDataTools().getAnnotationTypeNLP(annotation.getSlot());
			if (annotationTypes != null && !annotationTypes.contains(annotationType))
				continue;
			
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
		
		Map<AnnotationTypeNLP<?>, String> annotators = new HashMap<AnnotationTypeNLP<?>, String>();
		Map<AnnotationTypeNLP<?>, Pair<?, Double>[][]> tokenAnnotations = new HashMap<AnnotationTypeNLP<?>, Pair<?, Double>[][]>();
		Map<AnnotationTypeNLP<?>, List<Triple<TokenSpan, ?, Double>>> tokenSpanAnnotations = new HashMap<AnnotationTypeNLP<?>, List<Triple<TokenSpan, ?, Double>>>();
		Map<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> sentenceAnnotations =  new HashMap<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>>();
		
		for (AnnotationType<?> annotationType : orderedAnnotations.keySet()) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			
			annotators.put(annotationTypeNLP, orderedAnnotations.get(annotationType).firstEntry().getValue().get(0).getAnnotator());
			
			if (annotationTypeNLP.getTarget() == AnnotationTypeNLP.Target.TOKEN) {
				tokenAnnotations.put(annotationTypeNLP, new Pair[orderedAnnotations.get(AnnotationTypeNLP.SENTENCE).size()][]);
			} else if (annotationTypeNLP.getTarget() == AnnotationTypeNLP.Target.TOKEN_SPAN) {
				tokenSpanAnnotations.put(annotationTypeNLP, new ArrayList<Triple<TokenSpan, ?, Double>>());
			} else if (annotationTypeNLP.getTarget() == AnnotationTypeNLP.Target.SENTENCE) {
				sentenceAnnotations.put(annotationTypeNLP, new HashMap<Integer, Pair<?, Double>>());
			} else if (annotationTypeNLP.getTarget() == AnnotationTypeNLP.Target.DOCUMENT) {
				Annotation annotation = orderedAnnotations.get(annotationType).firstEntry().getValue().get(0);
				document.setDocumentAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, new Pair<Object, Double>(annotationTypeNLP.deserialize(document, annotation.getValue()), annotation.getConfidence()));			
			}
		}
		
		if (!hasNonDocumentAnnotations)
			return document;
		
		TreeMap<Integer, List<Annotation>> sentences = orderedAnnotations.get(AnnotationTypeNLP.SENTENCE);

		int sentenceIndex = 0;
		for (Entry<Integer, List<Annotation>> sentenceEntry : sentences.entrySet()) {
			int sentenceStart = sentenceEntry.getKey();
			int sentenceEnd = sentenceEntry.getValue().get(0).getSpanEnd();
			
			SortedMap<Integer, List<Annotation>> sentenceTokenAnnotations = orderedAnnotations.get(AnnotationTypeNLP.TOKEN).subMap(sentenceStart, sentenceEnd);

			for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : tokenAnnotations.entrySet()) {
				SortedMap<Integer, List<Annotation>> sentTokenAnnotationMap = orderedAnnotations.get(entry.getKey()).subMap(sentenceStart, sentenceEnd);
				if (sentTokenAnnotationMap.size() == 0)
					continue;
				
				entry.getValue()[sentenceIndex] = new Pair[sentenceTokenAnnotations.size()];
				int i = 0;
				for (List<Annotation> annotation : sentTokenAnnotationMap.values()) {
					entry.getValue()[sentenceIndex][i] = new Pair<Object, Double>(entry.getKey().deserialize(document, sentenceIndex, annotation.get(0).getValue()), annotation.get(0).getConfidence());
					i++;
				}
			}
			
			Pair<?, Double>[] sentenceTokens = tokenAnnotations.get(AnnotationTypeNLP.TOKEN)[sentenceIndex];
			
			for (Entry<AnnotationTypeNLP<?>, List<Triple<TokenSpan, ?, Double>>> entry : tokenSpanAnnotations.entrySet()) {
				SortedMap<Integer, List<Annotation>> sentTokenSpanAnnotationMap = orderedAnnotations.get(entry.getKey()).subMap(sentenceStart, sentenceEnd);
				if (sentTokenSpanAnnotationMap.size() == 0)
					continue; 
				
				for (List<Annotation> sentTokenSpanAnnotations : sentTokenSpanAnnotationMap.values()) {
					for (Annotation annotation : sentTokenSpanAnnotations) {
						if (!annotation.getAnnotator().equals(annotators.get(entry.getKey())))
							continue;
						
						TokenSpan tokenSpan = getTokenSpanFromCharSpan(document, sentenceTokens, sentenceIndex, annotation.getSpanStart(), annotation.getSpanEnd());
						entry.getValue().add(new Triple<TokenSpan, Object, Double>(tokenSpan, entry.getKey().deserialize(document, sentenceIndex, annotation.getValue()), annotation.getConfidence()));
					}
				}
			}
			
			for (Entry<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> entry : sentenceAnnotations.entrySet()) {
				SortedMap<Integer, List<Annotation>> sentSentenceAnnotationMap = orderedAnnotations.get(entry.getKey()).subMap(sentenceStart, sentenceEnd);
				if (sentSentenceAnnotationMap.size() == 0)
					continue; 
				
				if (sentSentenceAnnotationMap.containsKey(sentenceStart)) {
					Annotation annotation = sentSentenceAnnotationMap.get(sentenceStart).get(0);
					entry.getValue().put(sentenceIndex, new Pair<Object, Double>(
							entry.getKey().deserialize(document, sentenceIndex, annotation.getValue()), annotation.getConfidence()));
				}
			}
			
			sentenceIndex++;
		}

		for (Entry<AnnotationTypeNLP<?>, Pair<?, Double>[][]> entry : tokenAnnotations.entrySet()) {
			document.setTokenAnnotation(annotators.get(entry.getKey()), entry.getKey(), entry.getValue());
		}
		
		for (Entry<AnnotationTypeNLP<?>, List<Triple<TokenSpan, ?, Double>>> entry : tokenSpanAnnotations.entrySet()) {
			document.setTokenSpanAnnotation(annotators.get(entry.getKey()), entry.getKey(), entry.getValue());
		}
		
		for (Entry<AnnotationTypeNLP<?>, Map<Integer, Pair<?, Double>>> entry : sentenceAnnotations.entrySet()) {
			document.setSentenceAnnotation(annotators.get(entry.getKey()), entry.getKey(), entry.getValue());
		}
		
		return document;
	}
	
	private Annotation makeMicroAnnotation(DocumentNLPMutable document, int spanStart, int spanEnd, String slot, String annotator, DateTime annotationTime, Object value, Double confidence) {
		if (value instanceof String) {
			return new Annotation(spanStart, 
								  spanEnd,
								  slot,
								  annotator != null ? annotator : "",
								  document.getName(),
								  value.toString(),
								  confidence,
								  annotationTime,
								  null);
		} else {
			return new Annotation(spanStart, 
					  spanEnd,
					  slot,
					  annotator != null ? annotator : "",
					  document.getName(),
					  (JSONObject)value,
					  confidence,
					  annotationTime,
					  null);
		}
	}

	private TokenSpan getTokenSpanFromCharSpan(DocumentNLPMutable document, Pair<?, Double>[] sentenceTokens, int sentenceIndex, int charSpanStart, int charSpanEnd) {
		int startTokenIndex = -1;
		int endTokenIndex = -1;
		for (int i = 0; i < sentenceTokens.length; i++) {
			Token token = (Token)sentenceTokens[i].getFirst();
			if (token.getCharSpanStart() == charSpanStart)
				startTokenIndex = i;
			if (token.getCharSpanEnd() == charSpanEnd) {
				endTokenIndex = i + 1;
				break;
			}
		}
		
		if (startTokenIndex < 0 || endTokenIndex < 0)
			return null;
		else
			return new TokenSpan(document, sentenceIndex, startTokenIndex, endTokenIndex);
	}

	@Override
	public String getName() {
		return "DocumentNLPMicro";
	}

	@Override
	public String serializeToString(DocumentNLPMutable item) {
		return serialize(item).toString();
	}

	@Override
	public DocumentNLPMutable deserializeFromString(String str, StoreReference storeReference) {
		return deserialize(DocumentAnnotation.fromString(str).get(0), storeReference);
	}

	@Override
	public SerializerDocument<DocumentNLPMutable, DocumentAnnotation> makeInstance(
			DocumentNLPMutable genericDocument,
			Collection<AnnotationType<?>> annotationTypes) {
		return new SerializerDocumentNLPMicro(genericDocument, annotationTypes);
	}
}
