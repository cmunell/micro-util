package edu.cmu.ml.rtw.generic.data.annotation.nlp;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.DataTools;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

public abstract class DocumentNLPMutable extends DocumentNLP {
	public DocumentNLPMutable(DataTools dataTools) {
		super(dataTools);
	}

	public boolean setDocumentAnnotation(AnnotationTypeNLP<?> annotationType, Pair<?, Double> annotation) {
		return setDocumentAnnotation(null, annotationType, annotation);
	}
	
	public boolean setSentenceAnnotation(AnnotationTypeNLP<?> annotationType, Map<Integer, ?> annotation) {
		return setSentenceAnnotation(null, annotationType, annotation);
	}
	
	public boolean setTokenSpanAnnotation(AnnotationTypeNLP<?> annotationType, List<Triple<TokenSpan, ?, Double>> annotation) {
		return setTokenSpanAnnotation(null, annotationType, annotation);
	}
	
	public boolean setTokenAnnotation(AnnotationTypeNLP<?> annotationType, Pair<?, Double>[][] annotation) {
		return setTokenAnnotation(null, annotationType, annotation);
	}

	public abstract DocumentNLPMutable makeInstance(String name);
	public abstract boolean setDocumentAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Pair<?, Double> annotation);
	public abstract boolean setSentenceAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Map<Integer, ?> annotation);
	public abstract boolean setTokenSpanAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, List<Triple<TokenSpan, ?, Double>> annotation);
	public abstract boolean setTokenAnnotation(String annotator, AnnotationTypeNLP<?> annotationType, Pair<?, Double>[][] annotation);
	
}
