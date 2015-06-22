package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import java.util.List;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.data.annotation.nlp.TokenSpan;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Triple;

/**
 * AnnotatorTokenSpan produces annotations of some
 * token spans within a document.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public interface AnnotatorTokenSpan<T> extends Annotator<T> {
	/**
	 * @param document
	 * @return a list of token spans with their annotations and
	 * confidences
	 */
	List<Triple<TokenSpan, T, Double>> annotate(DocumentNLP document);
}
