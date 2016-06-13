package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.json.JSONObject;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.SerializerDocument;
import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType.SerializationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP.Target;
import edu.cmu.ml.rtw.generic.data.store.StoreReference;
import edu.cmu.ml.rtw.generic.util.JSONUtil;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

public class SerializerDocumentNLPBSON extends SerializerDocument<DocumentNLPMutable, Document> {	
	public SerializerDocumentNLPBSON() {
		this(new DocumentNLPInMemory(new DataTools()), null);
	}
	
	public SerializerDocumentNLPBSON(DataTools dataTools) {
		this(new DocumentNLPInMemory(dataTools), null);
	}
	
	public SerializerDocumentNLPBSON(DocumentNLPMutable genericDocument) {
		this(genericDocument, null);
	}
	
	public SerializerDocumentNLPBSON(DocumentNLPMutable genericDocument, Collection<AnnotationType<?>> annotationTypes) {
		super(genericDocument, annotationTypes);
	}
	
	@Override
	public Document serialize(DocumentNLPMutable document) {
		Collection<AnnotationType<?>> docAnnotationTypes = document.getAnnotationTypes();
		
		Collection<AnnotationType<?>> annotationTypes = null;
		if (this.annotationTypes == null) {
			annotationTypes = docAnnotationTypes;
		} else {
			annotationTypes = this.annotationTypes;
		}
		
		Document bson = new Document();
		bson.append("name", document.getName());
		
		Document annotatorsBson = serializeAnnotators(annotationTypes, document, false);
		if (annotatorsBson != null)
			bson.append("annotators", annotatorsBson);
		
		for (AnnotationType<?> annotationType : docAnnotationTypes) {
			if (!annotationTypes.contains(annotationType))
				continue;
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			
			if (annotationTypeNLP.getTarget() == Target.DOCUMENT) {
				bson.append(annotationTypeNLP.getType(), 
						serializeDocumentAnnotation(annotationTypeNLP, document, false));
			} else if (!document.hasAnnotationType(AnnotationTypeNLP.TOKEN)) {
				throw new UnsupportedOperationException("Can't serialize non-document annotations without token annotations.");
			} else if (annotationTypeNLP.getTarget() == Target.SENTENCE) {
				List<Document> sentenceBsons = new ArrayList<Document>();
				for (int i = 0; i < document.getSentenceCount(); i++) {
					Document sentenceBson = serializeSentenceAnnotation(annotationTypeNLP, document, i, false);
					if (sentenceBson != null)
						sentenceBsons.add(sentenceBson);
				}
				bson.append(annotationTypeNLP.getType(), sentenceBsons);
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN) {
				List<Document> sentenceBsons = new ArrayList<Document>();
				for (int i = 0; i < document.getSentenceCount(); i++) {
					Document sentenceBson = serializeTokenSpanAnnotation(annotationTypeNLP, document, i, false);
					if (sentenceBson != null)
						sentenceBsons.add(sentenceBson);
				}
				bson.append(annotationTypeNLP.getType(), sentenceBsons);
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN) {
				List<Document> sentenceBsons = new ArrayList<Document>();
				for (int i = 0; i < document.getSentenceCount(); i++) {
					Document sentenceBson = serializeTokenAnnotation(annotationTypeNLP, document, i, false);
					if (sentenceBson != null)
						sentenceBsons.add(sentenceBson);
				}
				bson.append(annotationTypeNLP.getType(), sentenceBsons);
			} 
		}
		
		return bson;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DocumentNLPMutable deserialize(Document bson, StoreReference storeReference) {
		DocumentNLPMutable document = null;
		if (storeReference != null)
			document = this.genericDocument.makeInstance(storeReference);
		else
			document = this.genericDocument.makeInstance(bson.getString("name"));
		
		Collection<AnnotationType<?>> annotationTypes = null;
		if (this.annotationTypes == null) {
			annotationTypes = new ArrayList<AnnotationType<?>>();
			annotationTypes.addAll(document.getDataTools().getAnnotationTypesNLP());
		} else {
			annotationTypes = this.annotationTypes;
		}
		
		Map<AnnotationType<?>, String> annotators = null;
		if (bson.containsKey("annotators")) {
			annotators = deserializeAnnotators(annotationTypes, (Document)bson.get("annotators"));
		} else {
			annotators = new HashMap<AnnotationType<?>, String>();
		}
		
		boolean hasTokens = false;
		for (AnnotationType<?> annotationType : annotationTypes) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;
			
			if (annotationTypeNLP.getTarget() == Target.DOCUMENT && bson.containsKey(annotationTypeNLP.getType())) {
				Pair<?, Double> annotation = deserializeDocumentAnnotation(annotationTypeNLP, document, (Document)bson.get(annotationTypeNLP.getType()));
				document.setDocumentAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, annotation);
			} else if (annotationTypeNLP.equals(AnnotationTypeNLP.TOKEN) && bson.containsKey(AnnotationTypeNLP.TOKEN.getType())) {
				List<Document> tokenBson = (List<Document>)bson.get(AnnotationTypeNLP.TOKEN.getType());
				Pair<?, Double>[][] tokenAnnotation = new Pair[tokenBson.size()][];
				for (int i = 0; i < tokenBson.size(); i++) {
					tokenAnnotation[i] = deserializeTokenAnnotation(annotationTypeNLP, document, tokenBson.get(i));
				}
				
				document.setTokenAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, tokenAnnotation);
				hasTokens = true;
			}
		}
		
		if (!hasTokens)
			return document;
			
		for (AnnotationType<?> annotationType : annotationTypes) {
			AnnotationTypeNLP<?> annotationTypeNLP = (AnnotationTypeNLP<?>)annotationType;

			if (annotationTypeNLP.getTarget() == Target.SENTENCE && bson.containsKey(annotationTypeNLP.getType())) {
				List<Document> sentenceBson = (List<Document>)bson.get(annotationTypeNLP.getType());
				Map<Integer, Pair<?, Double>> sentenceAnnotations = new HashMap<Integer, Pair<?, Double>>();
				for (int i = 0; i < sentenceBson.size(); i++) {
					Triple<Integer, ? , Double> sentenceAnnotation = deserializeSentenceAnnotation(annotationTypeNLP, document, sentenceBson.get(i));
					sentenceAnnotations.put(sentenceAnnotation.getFirst(), new Pair<Object, Double>(sentenceAnnotation.getSecond(), sentenceAnnotation.getThird()));
				}
				
				document.setSentenceAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, sentenceAnnotations);
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN_SPAN && bson.containsKey(annotationTypeNLP.getType())) {
				List<Document> tokenSpanBson = (List<Document>)bson.get(annotationTypeNLP.getType());
				List<Triple<TokenSpan, ?, Double>> tokenSpanAnnotations = new ArrayList<Triple<TokenSpan, ?, Double>>();
				for (int i = 0; i < tokenSpanBson.size(); i++) {
					tokenSpanAnnotations.addAll(
							deserializeTokenSpanAnnotation(annotationTypeNLP, document, tokenSpanBson.get(i)));
				}
				
				document.setTokenSpanAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, tokenSpanAnnotations);
			} else if (annotationTypeNLP.getTarget() == Target.TOKEN && !annotationTypeNLP.equals(AnnotationTypeNLP.TOKEN) && bson.containsKey(annotationTypeNLP.getType())) {
				List<Document> tokenBson = (List<Document>)bson.get(annotationTypeNLP.getType());
				Pair<?, Double>[][] tokenAnnotation = new Pair[tokenBson.size()][];
				for (int i = 0; i < tokenBson.size(); i++) {
					tokenAnnotation[i] = deserializeTokenAnnotation(annotationTypeNLP, document, tokenBson.get(i));
				}
				
				document.setTokenAnnotation(annotators.get(annotationTypeNLP), annotationTypeNLP, tokenAnnotation);
			}
		}

		return document;
	}
	
	private Document serializeAnnotators(Collection<AnnotationType<?>> annotationTypes, DocumentNLPMutable document, boolean includeName) {
		Document bson = new Document();
		if (includeName) {
			bson.append("name", document.getName());
			bson.append("anno-type", "annotators");
		}
		
		for (AnnotationType<?> annotationType : document.getAnnotationTypes()) {
			if (!annotationTypes.contains(annotationType))
				continue;
			
			String annotatorName = document.getAnnotatorName(annotationType);
			if (annotatorName != null)
				bson.append(annotationType.getType(), annotatorName);
		}	
		
		if (bson.size() == 0)
			return null;
		else
			return bson;
	}
	
	private Map<AnnotationType<?>, String> deserializeAnnotators(Collection<AnnotationType<?>> annotationTypes, Document bson) {
		Map<AnnotationType<?>, String> annotators = new HashMap<AnnotationType<?>, String>();
		
		for (AnnotationType<?> annotationType : annotationTypes) {
			if (bson.containsKey(annotationType.getType()))
				annotators.put(annotationType, bson.getString(annotationType.getType()));
		}
		
		return annotators;
	}
	
	private Document serializeDocumentAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, boolean includeName) {
		Document bson = new Document();
		if (includeName) {
			bson.append("name", document.getName());
			bson.append("anno-type", annotationType.getType());
		}
		
		bson.append(annotationType.getType(), serializeAnnotation(annotationType, document.getDocumentAnnotation(annotationType)));
		if (document.hasConfidence(annotationType))
			bson.append("conf", document.getDocumentAnnotationConfidence(annotationType));
		
		return bson;
	}
	
	private Pair<?, Double> deserializeDocumentAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, Document bson) {
		Object annotation = deserializeAnnotation(annotationType, document, bson.get(annotationType.getType()), -1);
		Double confidence = null;
		if (bson.containsKey("conf"))
			confidence = bson.getDouble("conf");
		
		return new Pair<Object, Double>(annotation, confidence);
	}
	
	private Document serializeSentenceAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, int sentenceIndex, boolean includeName) {
		Object annotation = document.getSentenceAnnotation(annotationType, sentenceIndex);
		if (annotation == null)
			return null;
		
		Document bson = new Document();
		if (includeName) {
			bson.append("name", document.getName());
			bson.append("anno-type", annotationType.getType());
		}
		
		bson.append("sent", sentenceIndex);
		bson.append(annotationType.getType(), serializeAnnotation(annotationType, annotation));
		
		if (document.hasConfidence(annotationType))
			bson.append("conf", document.getSentenceAnnotationConfidence(annotationType, sentenceIndex));
		
		return bson;
	}
	
	private Triple<Integer, ? , Double> deserializeSentenceAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, Document bson) {
		Integer sentenceIndex = bson.getInteger("sent");
		Object annotation = deserializeAnnotation(annotationType, document, bson.get(annotationType.getType()), sentenceIndex);		
	
		Double confidence = null;
		if (bson.containsKey("conf"))
			confidence = bson.getDouble("conf");
		
		return new Triple<Integer, Object, Double>(sentenceIndex, annotation, confidence);
	}

	@SuppressWarnings("unchecked")
	private Document serializeTokenSpanAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, int sentenceIndex, boolean includeName) {
		List<?> tokenSpanAnnotations = document.getTokenSpanAnnotationConfidences(annotationType, sentenceIndex);
		if (tokenSpanAnnotations == null || tokenSpanAnnotations.size() == 0)
			return null;
		
		Document bson = new Document();
		if (includeName) {
			bson.append("name", document.getName());
			bson.append("anno-type", annotationType.getType());
		}
		
		bson.append("sent", sentenceIndex);
		
		List<Document> bsonAnnotations = new ArrayList<Document>();
		
		for (Object tokenSpanAnnotationObj : tokenSpanAnnotations) {
			Triple<TokenSpan, ?, Double> annotation = (Triple<TokenSpan, ?, Double>)tokenSpanAnnotationObj;
			Document bsonAnnotation = new Document();
			bsonAnnotation.append("span", JSONUtil.convertJSONToBSON(annotation.getFirst().toJSON()));
			bsonAnnotation.append(annotationType.getType(), serializeAnnotation(annotationType, annotation.getSecond()));
			
			if (annotation.getThird() != null)
				bsonAnnotation.append("conf", annotation.getThird());
			
			bsonAnnotations.add(bsonAnnotation);
			
		}
		
		bson.append(annotationType.getType(), bsonAnnotations);	
		
		return bson;
	}
	
	@SuppressWarnings("unchecked")
	private List<Triple<TokenSpan, ?, Double>> deserializeTokenSpanAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, Document bson) {
		List<Triple<TokenSpan, ?, Double>> annotations = new ArrayList<Triple<TokenSpan, ?, Double>>();
		List<Document> bsonAnnotations = (List<Document>)bson.get(annotationType.getType());
		Integer sentenceIndex = bson.getInteger("sent");
		
		for (Document bsonAnnotation : bsonAnnotations) {		
			TokenSpan span = TokenSpan.fromJSON(JSONUtil.convertBSONToJSON((Document)bsonAnnotation.get("span")), document, sentenceIndex);
			Object annotation = deserializeAnnotation(annotationType, document, bsonAnnotation.get(annotationType.getType()), sentenceIndex);
			
			Double confidence = null;
			if (bsonAnnotation.containsKey("conf"))
				confidence = bsonAnnotation.getDouble("conf");
		
			annotations.add(new Triple<TokenSpan, Object, Double>(span, annotation, confidence));
		}
		
		return annotations;
	}
	
	private Document serializeTokenAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, int sentenceIndex, boolean includeDocumentName) {
		Document bson = new Document();
		if (includeDocumentName) {
			bson.append("name", document.getName());
			bson.append("anno-type", annotationType.getType());
		}
		
		bson.append("sent", sentenceIndex);
		
		List<Document> bsonAnnotations = new ArrayList<Document>();
		for (int j = 0; j < document.getSentenceTokenCount(sentenceIndex); j++) {
			Object annotation = document.getTokenAnnotation(annotationType, sentenceIndex, j);
			if (annotation == null)
				return null;
			Document bsonAnnotation = new Document();
			bsonAnnotation.append(annotationType.getType(), serializeAnnotation(annotationType, annotation));
			
			Double conf = document.getTokenAnnotationConfidence(annotationType, sentenceIndex, j);
			if (conf != null)
				bsonAnnotation.append("conf", conf);
			bsonAnnotations.add(bsonAnnotation);
		}
		
		bson.append(annotationType.getType(), bsonAnnotations);	
		
		return bson;
	}

	@SuppressWarnings("unchecked")
	private Pair<?, Double>[] deserializeTokenAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, Document bson) {
		List<Document> bsonAnnotations = (List<Document>)bson.get(annotationType.getType());
		Pair<?, Double>[] annotations = new Pair[bsonAnnotations.size()];
				
		for (int i = 0; i < bsonAnnotations.size(); i++) {
			Document bsonAnnotation = bsonAnnotations.get(i);
			Object annotation = deserializeAnnotation(annotationType, document, bsonAnnotation.get(annotationType.getType()), i);

			Double confidence = null;
			if (bsonAnnotation.containsKey("conf"))
				confidence = bsonAnnotation.getDouble("conf");
		
			annotations[i] = new Pair<Object, Double>(annotation, confidence);
		}
		
		return annotations;
	}
	
	private Object serializeAnnotation(AnnotationTypeNLP<?> annotationType, Object object) {
		if (annotationType.getSerializationType() == SerializationType.JSON || annotationType.getSerializationType() == SerializationType.STORED) {
			return JSONUtil.convertJSONToBSON((JSONObject)annotationType.serialize(object));
		} else {
			return annotationType.serialize(object);
		}
	}
	
	private Object deserializeAnnotation(AnnotationTypeNLP<?> annotationType, DocumentNLPMutable document, Object bsonAnnotation, int sentenceIndex) {
		Object annotation = null;
		
		if (bsonAnnotation == null) {
			throw new UnsupportedOperationException("Missing annotation of " + annotationType.getType() + " in document " + document.getName() + " during BSON deserialization.");
		} else if (annotationType.getSerializationType() == SerializationType.JSON || annotationType.getSerializationType() == SerializationType.STORED) {
			if (annotationType.getTarget() == Target.SENTENCE)
				annotation = annotationType.deserialize(document, sentenceIndex, JSONUtil.convertBSONToJSON((Document)bsonAnnotation));
			else
				annotation = annotationType.deserialize(document, JSONUtil.convertBSONToJSON((Document)bsonAnnotation));
		} else {
			if (annotationType.getTarget() == Target.SENTENCE)
				annotation = annotationType.deserialize(document, sentenceIndex, bsonAnnotation);				
			else 
				annotation = annotationType.deserialize(document, bsonAnnotation);
		}
		
		return annotation;
	}

	@Override
	public String getName() {
		return "DocumentNLPBSON";
	}

	@Override
	public String serializeToString(DocumentNLPMutable item) {
		return serialize(item).toJson();
	}

	@Override
	public DocumentNLPMutable deserializeFromString(String str, StoreReference storeReference) {
		return deserialize(Document.parse(str), storeReference);
	}

	@Override
	public SerializerDocument<DocumentNLPMutable, Document> makeInstance(DocumentNLPMutable genericDocument,
			Collection<AnnotationType<?>> annotationTypes) {
		return new SerializerDocumentNLPBSON(genericDocument, annotationTypes);
	}
}
