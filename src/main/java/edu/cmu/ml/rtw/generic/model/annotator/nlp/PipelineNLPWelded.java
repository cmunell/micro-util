package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.rtw.generic.data.annotation.AnnotationType;
import edu.cmu.ml.rtw.generic.data.annotation.Document;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.AnnotationTypeNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLPMutable;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;
import edu.cmu.ml.rtw.generic.util.Triple;

/**
 * 
 * PipelineNLPWelded represents two NLP pipelines that
 * have been welded together (typically through the PipelineNLP
 * weld method).  This is useful if you want to construct
 * a DocumentNLP pipeline by running text through two consecutive
 * pipelines.  For example,
 * you might want to extend the Stanford CoreNLP (PipelineNLPStanford)
 * pipeline with additional annotators from another pipeline,
 * and use it to construct annotated text documents.
 * 
 * @author Bill McDowell
 *
 */
public class PipelineNLPWelded extends PipelineNLP {
	private PipelineNLP first;
	private PipelineNLP second;
	
	public PipelineNLPWelded(PipelineNLP first, PipelineNLP second) {
		super();
		
		for (int i = 0; i < first.getAnnotatorCount(); i++) {
			if (second.hasAnnotator(first.getAnnotationType(i)))
				throw new IllegalArgumentException("Cannot weld pipelines containing annotators for the same annotation types");
		}
		
		this.first = first;
		this.second = second;
	}
	
	public String getAnnotatorName(AnnotationType<?> annotationType) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.getAnnotatorName(annotationType);
		else
			return this.second.getAnnotatorName(annotationType);
	}
	
	public boolean hasAnnotator(AnnotationType<?> annotationType) {
		return this.first.hasAnnotator(annotationType) || this.second.hasAnnotator(annotationType);
	}
	
	public boolean annotatorMeasuresConfidence(AnnotationType<?> annotationType) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.annotatorMeasuresConfidence(annotationType);
		else
			return this.second.annotatorMeasuresConfidence(annotationType);
	}
	
	public boolean meetsAnnotatorRequirements(AnnotationType<?> annotationType, Document document) {
		if (!hasAnnotator(annotationType))
			return false;
		
		if (this.first.hasAnnotator(annotationType))
			return this.first.meetsAnnotatorRequirements(annotationType, document);
		else
			return this.second.meetsAnnotatorRequirements(annotationType, document);
	}
	
	public int getAnnotatorCount() {
		return this.first.getAnnotatorCount() + this.second.getAnnotatorCount();
	}
	
	public AnnotationType<?> getAnnotationType(int index) {
		if (index < this.first.getAnnotatorCount())
			return this.first.getAnnotationType(index);
		else 
			return this.second.getAnnotationType(index - this.first.getAnnotatorCount());
	}
	
	protected void addAnnotator(AnnotationType<?> annotationType, Annotator<?> annotator) {
		throw new UnsupportedOperationException();
	}
	
	protected void clearAnnotators() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public <T> Pair<T, Double> annotateDocument(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.annotateDocument(annotationType, document);
		else
			return this.second.annotateDocument(annotationType, document);
	}
	
	@Override
	public <T> Map<Integer, Pair<T, Double>> annotateSentences(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.annotateSentences(annotationType, document);
		else
			return this.second.annotateSentences(annotationType, document);
	}
	
	@Override
	public <T> List<Triple<TokenSpan, T, Double>> annotateTokenSpans(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.annotateTokenSpans(annotationType, document);
		else
			return this.second.annotateTokenSpans(annotationType, document);
	}
	
	@Override
	public <T> Pair<T, Double>[][] annotateTokens(AnnotationTypeNLP<T> annotationType, DocumentNLPMutable document) {
		if (this.first.hasAnnotator(annotationType))
			return this.first.annotateTokens(annotationType, document);
		else
			return this.second.annotateTokens(annotationType, document);
	}
	
	@Override
	public DocumentNLPMutable run(DocumentNLPMutable document, Collection<AnnotationType<?>> skipAnnotators) {
		return this.second.run(this.first.run(document, skipAnnotators), skipAnnotators);
	}
}
