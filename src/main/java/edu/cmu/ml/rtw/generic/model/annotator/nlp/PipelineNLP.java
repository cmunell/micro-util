package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.Pipeline;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

/**
 * PipelineNLP represents a pipeline of NLP annotators through
 * which a document can be passed.  A pipeline can be
 * given to an
 * edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP
 * with some text upon construction, and the DocumentNLP
 * will be filled with annotations of the text generated
 * from annotators in the pipeline.
 * 
 * @author Bill McDowell
 *
 */
public abstract class PipelineNLP extends Pipeline<DocumentNLPMutable> {
	public PipelineNLP() {
		super();
	}
	
	@SuppressWarnings("unchecked")
	public <T> Pair<T, Double> annotateDocument(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		AnnotatorDocument<T> annotator = (AnnotatorDocument<T>)this.annotators.get(annotationType);
		return annotator.annotate(document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Map<Integer, Pair<T, Double>> annotateSentences(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		AnnotatorSentence<T> annotator = (AnnotatorSentence<T>)this.annotators.get(annotationType);
		return annotator.annotate(document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<Triple<TokenSpan, T, Double>> annotateTokenSpans(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		AnnotatorTokenSpan<T> annotator = (AnnotatorTokenSpan<T>)this.annotators.get(annotationType);
		return annotator.annotate(document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Pair<T, Double>[][] annotateTokens(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		AnnotatorToken<T> annotator = (AnnotatorToken<T>)this.annotators.get(annotationType);
		return annotator.annotate(document);
	}
	
	public PipelineNLP weld(PipelineNLP pipeline) {
		return new PipelineNLPWelded(this, pipeline);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public DocumentNLPMutable run(DocumentNLPMutable document, Collection<AnnotationType<?>> skipAnnotators) {
		for (int annotatorIndex = 0; annotatorIndex < getAnnotatorCount(); annotatorIndex++) {
			AnnotationTypeNLP<?> annotationType = (AnnotationTypeNLP<?>)getAnnotationType(annotatorIndex);
			if (skipAnnotators != null && skipAnnotators.contains(annotationType)) {
				continue;
			}

			if (!meetsAnnotatorRequirements(annotationType, document)) {
				throw new UnsupportedOperationException("Document does not meet annotation type requirements for " + annotationType.getType() + " annotator.");
			}
				
			if (annotationType.getTarget() == AnnotationTypeNLP.Target.DOCUMENT) {
				document.setDocumentAnnotation(this.annotators.get(annotationType).getName(), annotationType, annotateDocument(annotationType, document));
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.SENTENCE) {
				document.setSentenceAnnotation(this.annotators.get(annotationType).getName(), annotationType, annotateSentences(annotationType, document));
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN_SPAN) {
				document.setTokenSpanAnnotation(this.annotators.get(annotationType).getName(), annotationType, (List<Triple<TokenSpan, ?, Double>>)(List<?>)annotateTokenSpans(annotationType, document));			
			} else if (annotationType.getTarget() == AnnotationTypeNLP.Target.TOKEN) {
				document.setTokenAnnotation(this.annotators.get(annotationType).getName(), annotationType, annotateTokens(annotationType, document));
			}
		}
		
		return document;
	}
}
