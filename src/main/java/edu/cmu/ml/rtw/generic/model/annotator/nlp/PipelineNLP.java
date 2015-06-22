package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
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
public abstract class PipelineNLP extends Pipeline {
	protected DocumentNLP document;
	
	public PipelineNLP() {
		super();
	}
	
	public boolean setDocument(DocumentNLP document) {
		this.document = document;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Pair<T, Double> annotateDocument(AnnotationTypeNLP<T> annotationType) {
		AnnotatorDocument<T> annotator = (AnnotatorDocument<T>)this.annotators.get(annotationType);
		return annotator.annotate(this.document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Map<Integer, Pair<T, Double>> annotateSentences(AnnotationTypeNLP<T> annotationType) {
		AnnotatorSentence<T> annotator = (AnnotatorSentence<T>)this.annotators.get(annotationType);
		return annotator.annotate(this.document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<Triple<TokenSpan, T, Double>> annotateTokenSpans(AnnotationTypeNLP<T> annotationType) {
		AnnotatorTokenSpan<T> annotator = (AnnotatorTokenSpan<T>)this.annotators.get(annotationType);
		return annotator.annotate(this.document);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Pair<T, Double>[][] annotateTokens(AnnotationTypeNLP<T> annotationType) {
		AnnotatorToken<T> annotator = (AnnotatorToken<T>)this.annotators.get(annotationType);
		return annotator.annotate(this.document);
	}
	
	public PipelineNLP weld(PipelineNLP pipeline) {
		return new PipelineNLPWelded(this, pipeline);
	}
}
