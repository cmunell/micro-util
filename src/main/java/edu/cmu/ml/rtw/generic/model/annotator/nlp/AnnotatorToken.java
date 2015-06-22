package edu.cmu.ml.rtw.generic.model.annotator.nlp;

import edu.cmu.ml.rtw.generic.data.annotation.nlp.DocumentNLP;
import edu.cmu.ml.rtw.generic.model.annotator.Annotator;
import edu.cmu.ml.rtw.generic.util.Pair;

/**
 * AnnotatorSentence produces a single annotation per 
 * token within an NLP document.
 * 
 * @author Bill McDowell
 *
 * @param <T>
 */
public interface AnnotatorToken<T> extends Annotator<T> {
	/**
	 * @param document
	 * @return a two dimensional array of annotations and
	 * their confidences corresponding to a two dimensional
	 * array of tokens within sentences of the document.
	 */
	Pair<T, Double>[][] annotate(DocumentNLP document);
}